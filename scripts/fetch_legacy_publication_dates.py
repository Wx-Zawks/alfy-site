#!/usr/bin/env python3
"""Fetch verified publication dates from public legacy Alfy article pages.

Input comes from the read-only DOCX preflight report.  Only records that do
not contain a candidate source date are requested.  No DOCX or database record
is changed; this script produces an auditable metadata cache consumed by the
preflight report generator.
"""

from __future__ import annotations

import argparse
import json
import re
import sys
import time
from datetime import datetime, timezone
from html.parser import HTMLParser
from pathlib import Path
from typing import Any
from urllib.error import HTTPError, URLError
from urllib.request import Request, urlopen


class PublicationDateParser(HTMLParser):
    """Extract WordPress/Rank Math publication metadata without rendering JS."""

    def __init__(self) -> None:
        super().__init__()
        self.meta: dict[str, str] = {}
        self.json_ld_parts: list[str] = []
        self._inside_json_ld = False

    def handle_starttag(self, tag: str, attrs: list[tuple[str, str | None]]) -> None:
        attributes = {key.lower(): value or "" for key, value in attrs}
        if tag.lower() == "meta":
            key = attributes.get("property") or attributes.get("name")
            content = attributes.get("content")
            if key and content and key.lower() in {"article:published_time", "datepublished"}:
                self.meta[key.lower()] = content
        elif tag.lower() == "script" and attributes.get("type", "").lower() == "application/ld+json":
            self._inside_json_ld = True

    def handle_endtag(self, tag: str) -> None:
        if tag.lower() == "script":
            self._inside_json_ld = False

    def handle_data(self, data: str) -> None:
        if self._inside_json_ld:
            self.json_ld_parts.append(data)


def find_date_published(value: Any) -> str | None:
    if isinstance(value, dict):
        date = value.get("datePublished")
        if isinstance(date, str) and date:
            return date
        for child in value.values():
            found = find_date_published(child)
            if found:
                return found
    elif isinstance(value, list):
        for child in value:
            found = find_date_published(child)
            if found:
                return found
    return None


def extract_metadata(html: str) -> tuple[str | None, str | None]:
    parser = PublicationDateParser()
    parser.feed(html)
    meta_date = parser.meta.get("article:published_time") or parser.meta.get("datepublished")
    json_ld_date = None
    for raw_json in parser.json_ld_parts:
        try:
            json_ld_date = find_date_published(json.loads(raw_json))
        except json.JSONDecodeError:
            continue
        if json_ld_date:
            break
    return meta_date or json_ld_date, "article:published_time" if meta_date else ("json_ld.datePublished" if json_ld_date else None)


def fetch_one(url: str, timeout_seconds: int) -> dict[str, str | int | None]:
    request = Request(url, headers={"User-Agent": "AlfyContentMigration/1.0 (+https://alfy.cn/)"})
    with urlopen(request, timeout=timeout_seconds) as response:
        raw = response.read()
        charset = response.headers.get_content_charset() or "utf-8"
        html = raw.decode(charset, errors="replace")
        published_at, source = extract_metadata(html)
        return {
            "http_status": response.status,
            "published_at": published_at,
            "published_at_source": source,
        }


def write_output(
    output_path: Path,
    input_path: Path,
    targets: list[dict[str, Any]],
    records_by_source_file: dict[str, dict[str, Any]],
) -> dict[str, Any]:
    records = [
        records_by_source_file[target["source_file"]]
        for target in targets
        if target["source_file"] in records_by_source_file
    ]
    output = {
        "generated_at": datetime.now(timezone.utc).isoformat(),
        "input_report": str(input_path),
        "scope": "only_articles_without_docx_candidate_source_dates",
        "summary": {
            "target_count": len(targets),
            "processed": len(records),
            "found": sum(item["status"] == "FOUND" for item in records),
            "missing_metadata": sum(item["status"] == "MISSING_METADATA" for item in records),
            "failed": sum(item["status"] == "FAILED" for item in records),
        },
        "records": records,
    }
    output_path.parent.mkdir(parents=True, exist_ok=True)
    output_path.write_text(json.dumps(output, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    return output


def main() -> int:
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
    parser = argparse.ArgumentParser(description="读取旧站公开文章页的发布时间元数据。")
    parser.add_argument("--input", type=Path, default=Path("docs/content-import-preflight.json"))
    parser.add_argument("--output", type=Path, default=Path("docs/legacy-publication-date-fetch.json"))
    parser.add_argument("--timeout", type=int, default=30)
    parser.add_argument("--delay", type=float, default=0.2, help="每次页面请求之间的最小间隔（秒）")
    parser.add_argument("--offset", type=int, default=0, help="从待抓取列表的第几个项目开始，0 表示第一项")
    parser.add_argument("--limit", type=int, default=None, help="本次最多抓取的项目数；省略时抓取全部")
    arguments = parser.parse_args()

    report = json.loads(arguments.input.read_text(encoding="utf-8"))
    targets = [
        record
        for record in report["documents"]
        if not record["candidate_source_dates"] and record["source_urls"]
    ]
    existing_records: dict[str, dict[str, Any]] = {}
    if arguments.output.is_file():
        existing = json.loads(arguments.output.read_text(encoding="utf-8"))
        existing_records = {item["source_file"]: item for item in existing.get("records", [])}

    if arguments.offset < 0:
        parser.error("--offset 不能小于 0")
    batch = targets[arguments.offset : arguments.offset + arguments.limit if arguments.limit else None]
    for index, target in enumerate(batch, start=arguments.offset + 1):
        url = target["source_urls"][0]
        result: dict[str, Any] = {
            "source_file": target["source_file"],
            "title": target["title"],
            "url": url,
            "status": "FAILED",
            "published_at": None,
            "published_at_source": None,
            "http_status": None,
            "error": None,
        }
        try:
            fetched = fetch_one(url, arguments.timeout)
            result.update(fetched)
            if result["published_at"]:
                result["status"] = "FOUND"
            else:
                result["status"] = "MISSING_METADATA"
        except HTTPError as error:
            result["http_status"] = error.code
            result["error"] = f"HTTP {error.code}: {error.reason}"
        except (URLError, TimeoutError, ValueError) as error:
            result["error"] = str(error)
        existing_records[target["source_file"]] = result
        # Persist every result to make a long public-page crawl resumable.
        write_output(arguments.output, arguments.input, targets, existing_records)
        print(f"[{index}/{len(targets)}] {result['status']} {target['source_file']}")
        if index < arguments.offset + len(batch):
            time.sleep(arguments.delay)

    output = write_output(arguments.output, arguments.input, targets, existing_records)
    print(json.dumps(output["summary"], ensure_ascii=False))
    return 1 if any(item["status"] == "FAILED" for item in output["records"]) else 0


if __name__ == "__main__":
    raise SystemExit(main())
