#!/usr/bin/env python3
"""Generate a reviewable manifest before importing legacy Alfy blog DOCX files.

The script is intentionally read-only: it does not copy assets, write to MySQL,
or alter the source documents.  It only scans the agreed content directories and
writes a JSON report for human review before a batch import is enabled.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import re
import sys
import zipfile
from collections import Counter, defaultdict
from datetime import datetime, timezone
from pathlib import Path
from typing import Any
from xml.etree import ElementTree


WORD_NAMESPACE = "{http://schemas.openxmlformats.org/wordprocessingml/2006/main}"
URL_PATTERN = re.compile(r"https?://[^\s<>\"']+", re.IGNORECASE)
DATE_PATTERN = re.compile(r"(?<!\d)(?:20\d{2}-\d{2}-\d{2}|20\d{2}年\d{1,2}月\d{1,2}日)(?!\d)")
LEGACY_FILENAME_SUFFIX = re.compile(r"\s*-\s*湖南奥飞新材料有限公司(?:_\d{8})?$")

# Only these paths are in scope.  In particular, “新建文件夹” is deliberately
# absent until its business meaning is confirmed.
CATEGORY_PATHS: dict[tuple[str, ...], dict[str, str]] = {
    ("新闻动态", "公司新闻"): {"code": "company_news", "name": "公司新闻"},
    ("新闻动态", "行业新闻"): {"code": "industry_news", "name": "行业新闻"},
    ("相关案例",): {"code": "case", "name": "相关案例"},
    ("研发成果",): {"code": "research", "name": "研发成果"},
    ("应用技术",): {"code": "application_technology", "name": "应用技术"},
}
IGNORED_TOP_LEVEL_DIRECTORIES = ("新建文件夹",)


def sha256_bytes(value: bytes) -> str:
    return hashlib.sha256(value).hexdigest()


def normalize_text(value: str) -> str:
    return re.sub(r"\s+", " ", value).strip()


def title_from_filename(path: Path) -> str:
    stem = re.sub(r"_\d{8}$", "", path.stem)
    return LEGACY_FILENAME_SUFFIX.sub("", stem).strip()


def ordered_unique(values: list[str]) -> list[str]:
    seen: set[str] = set()
    result: list[str] = []
    for value in values:
        if value not in seen:
            seen.add(value)
            result.append(value)
    return result


def read_docx(path: Path) -> tuple[list[str], list[str], str, int]:
    """Return paragraphs, embedded media file names, DOCX hash and byte size."""
    raw_docx = path.read_bytes()
    with zipfile.ZipFile(path) as archive:
        root = ElementTree.fromstring(archive.read("word/document.xml"))
        paragraphs: list[str] = []
        for paragraph in root.iter(f"{WORD_NAMESPACE}p"):
            text = "".join(node.text or "" for node in paragraph.iter(f"{WORD_NAMESPACE}t"))
            text = normalize_text(text)
            if text:
                paragraphs.append(text)
        media_files = sorted(
            Path(name).name
            for name in archive.namelist()
            if name.startswith("word/media/") and not name.endswith("/")
        )
    return paragraphs, media_files, sha256_bytes(raw_docx), len(raw_docx)


def clean_url(url: str) -> str:
    return url.rstrip("。，、；;）)]}》〉")


def captured_dates_in_export(paragraphs: list[str]) -> list[str]:
    """Identify WebToOffice capture dates; they must not become publish dates."""
    captured: list[str] = []
    for paragraph in paragraphs:
        if "收藏时间" in paragraph:
            captured.extend(match.group(0) for match in DATE_PATTERN.finditer(paragraph))
    return ordered_unique(captured)


def discover_documents(source_root: Path) -> list[tuple[Path, dict[str, str]]]:
    selected: list[tuple[Path, dict[str, str]]] = []
    for category_path, category in CATEGORY_PATHS.items():
        directory = source_root.joinpath(*category_path)
        if not directory.is_dir():
            raise FileNotFoundError(f"缺少约定内容目录：{directory}")
        for document in sorted(directory.rglob("*.docx"), key=lambda item: str(item).casefold()):
            # Microsoft Word creates transient lock files beginning with "~$".
            # They are not DOCX archives and must never enter an import batch.
            if document.name.startswith("~$"):
                continue
            selected.append((document, category))
    return selected


def load_legacy_publication_dates(path: Path) -> dict[str, dict[str, str]]:
    """Load only successfully extracted, externally verifiable legacy dates."""
    if not path.is_file():
        return {}
    raw = json.loads(path.read_text(encoding="utf-8"))
    return {
        item["source_file"]: item
        for item in raw.get("records", [])
        if item.get("status") == "FOUND" and item.get("published_at")
    }


def preflight_document(
    document: Path,
    source_root: Path,
    category: dict[str, str],
    videos: list[Path],
    legacy_dates: dict[str, dict[str, str]],
) -> dict[str, Any]:
    paragraphs, media_files, source_hash, source_size = read_docx(document)
    content_text = "\n".join(paragraphs)
    source_urls = ordered_unique([clean_url(item) for item in URL_PATTERN.findall(content_text)])
    dates = ordered_unique([match.group(0) for match in DATE_PATTERN.finditer(content_text)])
    captured_dates = captured_dates_in_export(paragraphs)
    candidate_source_dates = [date for date in dates if date not in captured_dates]
    source_file = document.relative_to(source_root).as_posix()
    legacy_date = legacy_dates.get(source_file, {})
    lower_content = content_text.casefold()
    referenced_videos = [video.name for video in videos if video.name.casefold() in lower_content]

    warnings: list[str] = []
    if not paragraphs:
        warnings.append("未提取到正文文本")
    if not source_urls:
        warnings.append("未识别到旧站原文链接，需人工补充来源")
    if not candidate_source_dates and not legacy_date:
        warnings.append("未识别到候选原发布时间，需人工确认")

    return {
        "source_file": source_file,
        "category_code": category["code"],
        "category_name": category["name"],
        "title": title_from_filename(document),
        "source_urls": source_urls,
        "detected_dates": dates,
        "export_captured_dates": captured_dates,
        "candidate_source_dates": candidate_source_dates,
        "legacy_page_published_at": legacy_date.get("published_at"),
        "legacy_page_published_at_source": legacy_date.get("published_at_source"),
        "legacy_page_published_at_url": legacy_date.get("url"),
        "docx_sha256": source_hash,
        "docx_size": source_size,
        "content_sha256": sha256_bytes(normalize_text(content_text).encode("utf-8")),
        "paragraph_count": len(paragraphs),
        "character_count": len(content_text),
        "embedded_media_files": media_files,
        "embedded_media_count": len(media_files),
        "referenced_videos": referenced_videos,
        "content_preview": content_text[:500],
        "warnings": warnings,
    }


def build_report(source_root: Path, legacy_date_file: Path) -> dict[str, Any]:
    source_root = source_root.resolve()
    videos = sorted(source_root.glob("*.mp4"), key=lambda item: item.name.casefold())
    documents = discover_documents(source_root)
    legacy_dates = load_legacy_publication_dates(legacy_date_file)
    records: list[dict[str, Any]] = []
    failures: list[dict[str, str]] = []

    for document, category in documents:
        try:
            records.append(preflight_document(document, source_root, category, videos, legacy_dates))
        except (OSError, zipfile.BadZipFile, KeyError, ElementTree.ParseError) as error:
            failures.append(
                {
                    "source_file": document.relative_to(source_root).as_posix(),
                    "error": str(error),
                }
            )

    title_groups: dict[str, list[dict[str, str]]] = defaultdict(list)
    display_titles: dict[str, str] = {}
    for record in records:
        normalized_title = normalize_text(record["title"]).casefold()
        display_titles.setdefault(normalized_title, record["title"])
        title_groups[normalized_title].append(
            {"source_file": record["source_file"], "category_code": record["category_code"]}
        )
    duplicate_titles = [
        {"title": display_titles[normalized_title], "occurrences": records_for_title}
        for normalized_title, records_for_title in title_groups.items()
        if len(records_for_title) > 1
    ]

    video_usage = {
        video.name: [record["source_file"] for record in records if video.name in record["referenced_videos"]]
        for video in videos
    }
    warnings = Counter(warning for record in records for warning in record["warnings"])
    by_category = Counter(record["category_code"] for record in records)

    return {
        "generated_at": datetime.now(timezone.utc).isoformat(),
        "source_root": str(source_root),
        "scope": {
            "included_category_codes": [category["code"] for category in CATEGORY_PATHS.values()],
            "ignored_top_level_directories": list(IGNORED_TOP_LEVEL_DIRECTORIES),
            "mode": "preflight_only_no_database_write_no_asset_move",
        },
        "summary": {
            "document_count": len(records),
            "failed_document_count": len(failures),
            "documents_by_category": dict(sorted(by_category.items())),
            "documents_with_embedded_media": sum(bool(record["embedded_media_count"]) for record in records),
            "documents_with_source_url": sum(bool(record["source_urls"]) for record in records),
            "documents_with_candidate_source_dates": sum(bool(record["candidate_source_dates"]) for record in records),
            "documents_with_verified_legacy_page_published_at": sum(
                bool(record["legacy_page_published_at"]) for record in records
            ),
            "duplicate_title_group_count": len(duplicate_titles),
            "warning_counts": dict(sorted(warnings.items())),
        },
        "video_assets": [
            {
                "filename": video.name,
                "size": video.stat().st_size,
                "sha256": sha256_bytes(video.read_bytes()),
                "referenced_by": video_usage[video.name],
            }
            for video in videos
        ],
        "unmatched_video_filenames": [name for name, usages in video_usage.items() if not usages],
        "duplicate_titles": duplicate_titles,
        "failures": failures,
        "documents": records,
    }


def markdown_cell(value: str) -> str:
    return value.replace("|", "\\|").replace("\n", "<br>")


def write_review_list(report: dict[str, Any], output: Path) -> None:
    """Write only the records that need a person to complete source metadata."""
    records = report["documents"]
    missing_dates = [
        record
        for record in records
        if not record["candidate_source_dates"] and not record["legacy_page_published_at"]
    ]
    missing_urls = [record for record in records if not record["source_urls"]]

    lines = [
        "# 旧站博客待补信息清单",
        "",
        f"生成时间：{report['generated_at']}",
        "",
        "本清单只包含已纳入导入范围、但缺少原发布时间或来源链接的文章。",
        "“收藏时间”是抓取时间，不能作为原发布时间填写。",
        "",
        f"## 待确认原发布时间（{len(missing_dates)} 篇）",
        "",
        "| 分类 | 标题 | 源文件 | 已识别旧站链接 |",
        "| --- | --- | --- | --- |",
    ]
    for record in missing_dates:
        source_url = "<br>".join(record["source_urls"]) or "—"
        lines.append(
            "| {category} | {title} | `{source_file}` | {source_url} |".format(
                category=markdown_cell(record["category_name"]),
                title=markdown_cell(record["title"]),
                source_file=markdown_cell(record["source_file"]),
                source_url=markdown_cell(source_url),
            )
        )

    lines.extend(
        [
            "",
            f"## 待补旧站来源链接（{len(missing_urls)} 篇）",
            "",
            "| 分类 | 标题 | 源文件 | 候选原发布时间（仍需确认） |",
            "| --- | --- | --- | --- |",
        ]
    )
    for record in missing_urls:
        candidate_dates = "、".join(record["candidate_source_dates"]) or "—"
        lines.append(
            "| {category} | {title} | `{source_file}` | {candidate_dates} |".format(
                category=markdown_cell(record["category_name"]),
                title=markdown_cell(record["title"]),
                source_file=markdown_cell(record["source_file"]),
                candidate_dates=markdown_cell(candidate_dates),
            )
        )
    lines.extend(
        [
            "",
            "## 填写规则",
            "",
            "- 找不到可靠信息时，可暂时保留为空；后续后台可补录。",
            "- 发现文章有多个分类时，保留所有分类关联；不因多分类而删除文章。",
            "- 不要将 WebToOffice 的“收藏时间”填写为原发布时间。",
        ]
    )
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text("\n".join(lines) + "\n", encoding="utf-8")


def main() -> int:
    parser = argparse.ArgumentParser(description="预检奥飞旧站博客 DOCX，不写入数据库。")
    parser.add_argument("--source", type=Path, default=Path("docs/奥飞网站"), help="旧站抓取内容根目录")
    parser.add_argument(
        "--output",
        type=Path,
        default=Path("docs/content-import-preflight.json"),
        help="预检 JSON 报告输出路径",
    )
    parser.add_argument(
        "--review-output",
        type=Path,
        default=Path("docs/content-import-review-list.md"),
        help="待人工补充信息的 Markdown 清单输出路径",
    )
    parser.add_argument(
        "--legacy-date-file",
        type=Path,
        default=Path("docs/legacy-publication-date-fetch.json"),
        help="旧站页面发布时间抓取结果；仅采纳 status 为 FOUND 的记录",
    )
    arguments = parser.parse_args()

    try:
        report = build_report(arguments.source, arguments.legacy_date_file)
    except FileNotFoundError as error:
        print(f"预检失败：{error}", file=sys.stderr)
        return 2

    arguments.output.parent.mkdir(parents=True, exist_ok=True)
    arguments.output.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    write_review_list(report, arguments.review_output)
    summary = report["summary"]
    print(
        "预检完成："
        f"{summary['document_count']} 篇文章，"
        f"{summary['failed_document_count']} 篇失败，"
        f"报告：{arguments.output}；待补清单：{arguments.review_output}"
    )
    return 1 if report["failures"] else 0


if __name__ == "__main__":
    raise SystemExit(main())
