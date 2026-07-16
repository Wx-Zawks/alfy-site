#!/usr/bin/env python3
"""Convert preflighted Alfy DOCX files into an auditable draft import package.

The package contains HTML generated only from DOCX content, deduplicated image
assets, article/media manifests, and five static review pages.  It deliberately
does not write to MySQL and never changes the source DOCX files.
"""

from __future__ import annotations

import argparse
import hashlib
import html
import json
import re
import shutil
import sys
from collections import OrderedDict
from datetime import datetime, timezone
from pathlib import Path
from typing import Any

from docx import Document
from docx.oxml.ns import qn


BOILERPLATE_MARKERS = ("原文链接：", "收藏时间：", "本文档由：【WebToOffice】")
DATE_ONLY = re.compile(r"^20\d{2}-\d{2}-\d{2}$")
LEADING_DATE_AND_AUTHOR = re.compile(r"^20\d{2}[-年]\d{1,2}(?:[-月]\d{1,2}(?:日)?)?.{0,20}作者")
CHINESE_DATE = re.compile(r"^(20\d{2})年(\d{1,2})月(\d{1,2})日$")
VIDEO_REFERENCE = re.compile(r"(?:^|[\\/])[^\\/]+\.(?:mp4|mov|avi|webm)$", re.IGNORECASE)
IMAGE_EXTENSION_BY_MIME = {
    "image/jpeg": ".jpg",
    "image/png": ".png",
    "image/gif": ".gif",
    "image/webp": ".webp",
    "image/bmp": ".bmp",
    "image/tiff": ".tiff",
}


def sha256(value: bytes) -> str:
    return hashlib.sha256(value).hexdigest()


def normalize_space(value: str) -> str:
    return re.sub(r"\s+", " ", value).strip()


def normalize_candidate_date(value: str | None) -> str | None:
    if not value:
        return None
    if re.fullmatch(r"20\d{2}-\d{2}-\d{2}", value):
        return f"{value}T00:00:00+08:00"
    matched = CHINESE_DATE.fullmatch(value)
    if matched:
        year, month, day = matched.groups()
        return f"{year}-{int(month):02d}-{int(day):02d}T00:00:00+08:00"
    return None


def preferred_published_at(record: dict[str, Any]) -> tuple[str | None, str | None]:
    if record.get("legacy_page_published_at"):
        return record["legacy_page_published_at"], "legacy_page"
    for value in record.get("candidate_source_dates", []):
        normalized = normalize_candidate_date(value)
        if normalized:
            return normalized, "docx_candidate"
    return None, None


def is_initial_metadata(text: str, index: int, title: str) -> bool:
    """Remove export scaffolding only before the first real content paragraph."""
    if not text:
        return True
    if any(marker in text for marker in BOILERPLATE_MARKERS):
        return True
    if text.startswith("http://") or text.startswith("https://"):
        return True
    if DATE_ONLY.fullmatch(text) or LEADING_DATE_AND_AUTHOR.fullmatch(text):
        return True
    if normalize_space(text).startswith(normalize_space(title)):
        return True
    # Case documents use a short category line after title/date before body.
    if index < 5 and len(text) <= 40 and ("," in text or "，" in text):
        return True
    if index < 6 and len(text) <= 24 and not re.search(r"[。！？；;：:]", text):
        return True
    return False


def is_heading(paragraph: Any, text: str) -> int | None:
    style_name = paragraph.style.name if paragraph.style is not None else ""
    if style_name == "Title":
        return 1
    matched = re.fullmatch(r"Heading\s*([1-6])", style_name or "")
    if matched:
        return int(matched.group(1))
    # Scraped case documents often lose heading styles.  Keep the heuristic
    # deliberately conservative so regular prose remains a paragraph.
    if (
        2 <= len(text) <= 36
        and not re.search(r"[。！？；;：:]", text)
        and not re.match(r"^[0-9０-９]+[.、)]", text)
        and " " not in text
    ):
        return 2
    return None


def extension_for_image(content_type: str, fallback_name: str) -> str:
    if content_type in IMAGE_EXTENSION_BY_MIME:
        return IMAGE_EXTENSION_BY_MIME[content_type]
    suffix = Path(fallback_name).suffix.lower()
    return suffix if suffix else ".bin"


def extract_inline_images(
    paragraph: Any,
    document: Document,
    assets_directory: Path,
    assets: dict[str, dict[str, Any]],
) -> list[dict[str, Any]]:
    images: list[dict[str, Any]] = []
    for blip in paragraph._p.xpath(".//a:blip"):
        relationship_id = blip.get(qn("r:embed"))
        if not relationship_id:
            continue
        image_part = document.part.related_parts.get(relationship_id)
        if image_part is None:
            continue
        image_bytes = image_part.blob
        digest = sha256(image_bytes)
        extension = extension_for_image(image_part.content_type, getattr(image_part, "filename", ""))
        filename = f"{digest}{extension}"
        target = assets_directory / filename
        if not target.exists():
            target.write_bytes(image_bytes)
        assets.setdefault(
            digest,
            {
                "sha256": digest,
                "media_type": "IMAGE",
                "mime_type": image_part.content_type,
                "original_filename": getattr(image_part, "filename", filename),
                "storage_key": f"assets/{filename}",
                "file_size": len(image_bytes),
            },
        )
        images.append(assets[digest])
    return images


def convert_docx(
    source_path: Path,
    title: str,
    assets_directory: Path,
    assets: dict[str, dict[str, Any]],
) -> tuple[str, str, list[dict[str, Any]], list[str]]:
    document = Document(source_path)
    html_parts: list[str] = []
    plain_parts: list[str] = []
    media: list[dict[str, Any]] = []
    warnings: list[str] = []
    content_started = False

    for index, paragraph in enumerate(document.paragraphs):
        text = normalize_space(paragraph.text)
        images = extract_inline_images(paragraph, document, assets_directory, assets)
        if not content_started and is_initial_metadata(text, index, title):
            continue
        if text or images:
            content_started = True
        if not content_started:
            continue

        for image in images:
            position = len(media)
            media.append({**image, "usage_type": "INLINE", "sort_order": position})
            html_parts.append(
                '<figure><img src="{src}" alt="{alt}"></figure>'.format(
                    src=html.escape(image["storage_key"], quote=True),
                    alt=html.escape(f"{title} 配图 {position + 1}", quote=True),
                )
            )

        if not text:
            continue
        if VIDEO_REFERENCE.search(text):
            # The original MP4 remains a separately managed media asset and
            # will be attached through the article/video relation on import.
            continue
        plain_parts.append(text)
        escaped = html.escape(text)
        heading_level = is_heading(paragraph, text)
        if heading_level:
            html_parts.append(f"<h{heading_level}>{escaped}</h{heading_level}>")
        elif re.match(r"^(?:[-•●▪]|\d+[.、)])\s*", text):
            html_parts.append(f"<p>{escaped}</p>")
        else:
            html_parts.append(f"<p>{escaped}</p>")

    if not html_parts:
        warnings.append("转换后没有可展示的正文")
    return "\n".join(html_parts), "\n".join(plain_parts), media, warnings


def canonical_key(record: dict[str, Any]) -> str:
    source_urls = record.get("source_urls", [])
    return f"url:{source_urls[0]}" if source_urls else f"file:{record['docx_sha256']}"


def build_article_groups(records: list[dict[str, Any]]) -> list[list[dict[str, Any]]]:
    groups: OrderedDict[str, list[dict[str, Any]]] = OrderedDict()
    for record in records:
        groups.setdefault(canonical_key(record), []).append(record)
    return list(groups.values())


def write_preview(article: dict[str, Any], output: Path) -> None:
    content = article["content_html"].replace('src="assets/', 'src="../assets/')
    source_url = article.get("source_url") or ""
    published_at = article.get("source_published_at") or "未识别"
    category_names = "、".join(article["category_names"])
    video_text = "、".join(article["video_filenames"]) or "无"
    page = f"""<!doctype html>
<html lang="zh-CN"><head><meta charset="utf-8"><title>{html.escape(article['title'])}</title>
<style>body{{font:16px/1.8 system-ui,sans-serif;max-width:860px;margin:40px auto;padding:0 24px;color:#202124}}h1{{line-height:1.35}}img{{max-width:100%;height:auto}}figure{{margin:28px 0}}.meta{{padding:16px;background:#f6f8fa;border-radius:8px;color:#555}}.video{{padding:12px;background:#fff7e6;border-radius:6px}}</style></head>
<body><h1>{html.escape(article['title'])}</h1><div class="meta">分类：{html.escape(category_names)}<br>原发布时间：{html.escape(published_at)}<br>旧站链接：<a href="{html.escape(source_url, quote=True)}">{html.escape(source_url)}</a></div>
{content}<p class="video">关联视频：{html.escape(video_text)}</p></body></html>"""
    output.write_text(page, encoding="utf-8")


def main() -> int:
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
    parser = argparse.ArgumentParser(description="将预检完成的 DOCX 转为草稿导入包，不写入数据库。")
    parser.add_argument("--source", type=Path, default=Path("docs/奥飞网站"))
    parser.add_argument("--preflight", type=Path, default=Path("docs/content-import-preflight.json"))
    parser.add_argument("--output", type=Path, default=Path("docs/content-import-draft"))
    arguments = parser.parse_args()

    preflight = json.loads(arguments.preflight.read_text(encoding="utf-8"))
    source_root = arguments.source.resolve()
    output = arguments.output
    assets_directory = output / "assets"
    previews_directory = output / "preview"
    assets_directory.mkdir(parents=True, exist_ok=True)
    previews_directory.mkdir(parents=True, exist_ok=True)

    assets: dict[str, dict[str, Any]] = {}
    video_assets = [
        {
            "sha256": video["sha256"],
            "media_type": "VIDEO",
            "mime_type": "video/mp4",
            "original_filename": video["filename"],
            "source_file": video["filename"],
            "storage_key": f"content/{video['sha256']}.mp4",
            "file_size": video["size"],
        }
        for video in preflight["video_assets"]
    ]
    articles: list[dict[str, Any]] = []
    for number, group in enumerate(build_article_groups(preflight["documents"]), start=1):
        primary = group[0]
        source_path = source_root / Path(primary["source_file"])
        content_html, content_text, media, warnings = convert_docx(
            source_path, primary["title"], assets_directory, assets
        )
        category_codes = list(OrderedDict((record["category_code"], None) for record in group))
        category_names = list(OrderedDict((record["category_name"], None) for record in group))
        source_published_at, date_provenance = preferred_published_at(primary)
        video_filenames = list(OrderedDict((name, None) for record in group for name in record["referenced_videos"]))
        articles.append(
            {
                "import_key": canonical_key(primary),
                "source_files": [record["source_file"] for record in group],
                "source_records": [
                    {
                        "source_file": record["source_file"],
                        "source_url": record["source_urls"][0] if record["source_urls"] else None,
                        "docx_sha256": record["docx_sha256"],
                        "content_sha256": record["content_sha256"],
                        "category_code": record["category_code"],
                    }
                    for record in group
                ],
                "source_file": primary["source_file"],
                "title": primary["title"],
                "primary_category_code": category_codes[0],
                "category_codes": category_codes,
                "category_names": category_names,
                "source_url": primary["source_urls"][0] if primary["source_urls"] else None,
                "source_published_at": source_published_at,
                "published_at_provenance": date_provenance,
                "status": "DRAFT",
                "content_html": content_html,
                "content_text": content_text,
                "content_hash": sha256(content_text.encode("utf-8")),
                "media": media,
                "video_filenames": video_filenames,
                "docx_sha256": primary["docx_sha256"],
                "warnings": warnings,
            }
        )
        print(f"[{number}] {primary['title']}")

    samples: dict[str, dict[str, Any]] = {}
    for article in articles:
        category = article["primary_category_code"]
        if article["content_html"] and (
            category not in samples or len(article["content_html"]) > len(samples[category]["content_html"])
        ):
            samples[category] = article
    for category, article in samples.items():
        write_preview(article, previews_directory / f"{category}.html")

    manifest = {
        "generated_at": datetime.now(timezone.utc).isoformat(),
        "mode": "draft_package_only_no_database_write",
        "summary": {
            "source_document_count": len(preflight["documents"]),
            "article_count": len(articles),
            "deduplicated_document_count": len(preflight["documents"]) - len(articles),
            "image_asset_count": len(assets),
            "sample_preview_count": len(samples),
            "articles_with_warnings": sum(bool(article["warnings"]) for article in articles),
        },
        "assets": list(assets.values()),
        "video_assets": video_assets,
        "sample_previews": {
            category: {"title": article["title"], "import_key": article["import_key"]}
            for category, article in samples.items()
        },
        "articles": articles,
    }
    (output / "manifest.json").write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(manifest["summary"], ensure_ascii=False))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
