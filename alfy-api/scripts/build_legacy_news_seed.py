#!/usr/bin/env python3
"""Build the deployable legacy-news seed from a folder of WebToOffice DOCX files.

The generated SQL is intentionally idempotent so it can safely coexist with a
database that has already received part of the seed during local development.
Media binaries are written as classpath resources; the Spring bootstrap runner
copies them to the configured upload storage on startup.
"""

from __future__ import annotations

import argparse
import hashlib
import html
import json
import mimetypes
import re
import shutil
import zipfile
from collections import OrderedDict
from dataclasses import dataclass
from datetime import datetime
from pathlib import Path, PurePosixPath

from lxml import etree


W_NS = "http://schemas.openxmlformats.org/wordprocessingml/2006/main"
R_NS = "http://schemas.openxmlformats.org/officeDocument/2006/relationships"
REL_NS = "http://schemas.openxmlformats.org/package/2006/relationships"
NS = {"w": W_NS, "a": "http://schemas.openxmlformats.org/drawingml/2006/main", "r": R_NS}
SITE_SUFFIX = " - 湖南奥飞新材料有限公司"
VIDEO_BY_MARKER = {
    "263_1751102553.mp4": "外墙隔热保温改造完成，奥飞气凝胶涂料显著提升节能性能.docx",
    "368_1751498938.mp4": "长沙屋顶隔热保温望城区山与墅项目圆满完工，奥飞涂料助力清凉过夏.docx",
    "edada97c88aa6232739a7d5c6d63b966.mp4": "为迎接93大阅兵，停工半日，邀请全员工停下脚步，缅怀历史 - 湖南奥飞新材料有限公司_20260713.docx",
}
CATEGORY_BY_PARTS = {
    ("新闻动态", "公司新闻"): "company_news",
    ("新闻动态", "行业新闻"): "industry_news",
    ("研发成果",): "research",
    ("应用技术",): "application_technology",
    ("相关案例",): "case",
}


@dataclass
class Asset:
    sha256: str
    extension: str
    mime_type: str
    file_size: int
    original_filename: str
    storage_key: str
    resource_path: str
    display_name_assigned: bool = False


@dataclass
class Article:
    title: str
    slug: str
    category_codes: list[str]
    source_file: str
    source_url: str | None
    author_name: str | None
    published_at: str
    content_html: str
    content_text: str
    source_hash: str
    content_hash: str
    assets: list[Asset]
    videos: list[Asset]
    source_records: list[tuple[str, str, str, str | None]]


def qname(namespace: str, tag: str) -> str:
    return f"{{{namespace}}}{tag}"


def normalized_title(value: str) -> str:
    value = re.sub(r"\s+", " ", value).strip()
    return value[:-len(SITE_SUFFIX)].rstrip() if value.endswith(SITE_SUFFIX) else value


def text_of(node: etree._Element) -> str:
    return "".join(node.xpath(".//w:t/text()", namespaces=NS)).replace("\u00a0", " ").strip()


def paragraph_style(node: etree._Element) -> str:
    values = node.xpath("./w:pPr/w:pStyle/@w:val", namespaces=NS)
    return values[0] if values else ""


def is_list_item(node: etree._Element) -> bool:
    return bool(node.xpath("./w:pPr/w:numPr", namespaces=NS))


def sql_literal(value: object | None) -> str:
    if value is None:
        return "NULL"
    text = str(value).replace("\\", "\\\\").replace("'", "''")
    return "'" + text + "'"


def media_type(extension: str) -> tuple[str, str]:
    mime = mimetypes.types_map.get(extension.lower())
    if extension.lower() in {".jpg", ".jpeg"}:
        return "image/jpeg", "IMAGE"
    if extension.lower() == ".webp":
        return "image/webp", "IMAGE"
    if extension.lower() == ".png":
        return "image/png", "IMAGE"
    if extension.lower() == ".gif":
        return "image/gif", "IMAGE"
    if extension.lower() == ".mp4":
        return "video/mp4", "VIDEO"
    raise ValueError(f"Unsupported legacy media type: {extension} ({mime})")


def relative_category(path: Path) -> str:
    parts = path.parts[:-1]
    for prefix, code in CATEGORY_BY_PARTS.items():
        if parts[: len(prefix)] == prefix:
            return code
    raise ValueError(f"Cannot map article category for {path}")


def parse_collection_time(lines: list[str], fallback: datetime) -> str:
    for line in lines[:5]:
        match = re.search(r"(20\d{2})年(\d{1,2})月(\d{1,2})日(?:\s*(\d{1,2}):(\d{2})(?::(\d{2}))?)?", line)
        if match:
            year, month, day, hour, minute, second = match.groups()
            return datetime(int(year), int(month), int(day), int(hour or 0), int(minute or 0), int(second or 0)).strftime("%Y-%m-%d %H:%M:%S")
    return fallback.strftime("%Y-%m-%d 00:00:00")


def source_url_from(lines: list[str]) -> str | None:
    for line in lines[:6]:
        match = re.search(r"https?://\S+", line)
        if match:
            return match.group(0).rstrip("。；，,)")
    return None


def author_from(lines: list[str]) -> str | None:
    for line in lines:
        match = re.search(r"(?:作者|author)\s*[:：]?\s*([^\s|｜]+)", line, re.I)
        if match:
            candidate = match.group(1).strip()
            return candidate if len(candidate) <= 100 else None
    return None


def stable_slug(source_url: str | None, source_hash: str) -> str:
    if source_url:
        match = re.search(r"/(\d+)\.html(?:$|[?#])", source_url)
        if match:
            return "legacy-" + match.group(1)
    return "legacy-" + source_hash[:20]


def display_filename(title: str, label: str, index: int, extension: str) -> str:
    normalized = re.sub(r"[\\/:*?\"<>|]", "-", title)
    normalized = re.sub(r"\s+", " ", normalized).strip(" .-")
    return f"{normalized[:96]}-{label}-{index}{extension}"


def asset_from_bytes(data: bytes, original_filename: str, output_media: Path, assets: OrderedDict[str, Asset]) -> Asset:
    digest = hashlib.sha256(data).hexdigest()
    extension = Path(original_filename).suffix.lower() or ".jpg"
    mime_type, _ = media_type(extension)
    storage_key = f"bootstrap/legacy-news/{digest}{extension}"
    resource_path = f"media/{digest}{extension}"
    asset = assets.get(digest)
    if asset is None:
        output_path = output_media / f"{digest}{extension}"
        output_path.parent.mkdir(parents=True, exist_ok=True)
        output_path.write_bytes(data)
        asset = Asset(digest, extension, mime_type, len(data), original_filename, storage_key, resource_path)
        assets[digest] = asset
    return asset


def relationship_targets(zf: zipfile.ZipFile) -> dict[str, str]:
    root = etree.fromstring(zf.read("word/_rels/document.xml.rels"))
    return {
        item.get("Id"): item.get("Target")
        for item in root.findall(qname(REL_NS, "Relationship"))
        if item.get("Target", "").startswith("media/")
    }


def image_assets_in(node: etree._Element, targets: dict[str, str], zf: zipfile.ZipFile, output_media: Path, assets: OrderedDict[str, Asset]) -> list[Asset]:
    result: list[Asset] = []
    for embed_id in node.xpath(".//a:blip/@r:embed", namespaces=NS):
        target = targets.get(embed_id)
        if not target:
            continue
        archive_path = "word/" + target.replace("\\", "/")
        data = zf.read(archive_path)
        result.append(asset_from_bytes(data, Path(target).name, output_media, assets))
    return result


def table_html(node: etree._Element) -> str:
    rows: list[str] = []
    for row in node.xpath("./w:tr", namespaces=NS):
        cells = [f"<td>{html.escape(text_of(cell))}</td>" for cell in row.xpath("./w:tc", namespaces=NS)]
        rows.append("<tr>" + "".join(cells) + "</tr>")
    return "<table><tbody>" + "".join(rows) + "</tbody></table>" if rows else ""


def parse_docx(path: Path, root: Path, output_media: Path, assets: OrderedDict[str, Asset], video_assets: dict[str, Asset]) -> Article:
    source_hash = hashlib.sha256(path.read_bytes()).hexdigest()
    with zipfile.ZipFile(path) as zf:
        document = etree.fromstring(zf.read("word/document.xml"))
        targets = relationship_targets(zf)
        paragraphs = document.xpath("/w:document/w:body/w:p", namespaces=NS)
        lines = [text_of(paragraph) for paragraph in paragraphs if text_of(paragraph)]
        raw_title = next((text for paragraph in paragraphs if paragraph_style(paragraph) == "Title" for text in [text_of(paragraph)] if text), "")
        if not raw_title:
            raw_title = next((text_of(paragraph) for paragraph in paragraphs if text_of(paragraph)), path.stem)
        title = normalized_title(raw_title)
        source_url = source_url_from(lines)
        published_at = parse_collection_time(lines, datetime.fromtimestamp(path.stat().st_mtime))
        source_file = path.relative_to(root).as_posix()
        category_code = relative_category(path.relative_to(root))
        body = document.xpath("/w:document/w:body/*[self::w:p or self::w:tbl]", namespaces=NS)
        html_parts: list[str] = []
        content_parts: list[str] = []
        article_assets: list[Asset] = []
        seen_assets: set[str] = set()
        image_index = 0
        skipped_metadata = 0
        list_open = False
        for node in body:
            if node.tag == qname(W_NS, "tbl"):
                if list_open:
                    html_parts.append("</ul>")
                    list_open = False
                rendered = table_html(node)
                if rendered:
                    html_parts.append(rendered)
                    content_parts.append(" ".join(node.xpath(".//w:t/text()", namespaces=NS)))
                continue
            value = text_of(node)
            style = paragraph_style(node)
            images = image_assets_in(node, targets, zf, output_media, assets)
            is_metadata = skipped_metadata < 4 and (
                style == "Title" or value.startswith(("☆", "⏱", "📄")) or value == title or normalized_title(value) == title
            )
            if is_metadata:
                skipped_metadata += 1
                continue
            if list_open and not is_list_item(node):
                html_parts.append("</ul>")
                list_open = False
            if value:
                escaped = html.escape(value)
                if style.lower().startswith("heading"):
                    level_match = re.search(r"(\d+)", style)
                    level = min(4, max(2, int(level_match.group(1)) + 1 if level_match else 2))
                    html_parts.append(f"<h{level}>{escaped}</h{level}>")
                elif is_list_item(node):
                    if not list_open:
                        html_parts.append("<ul>")
                        list_open = True
                    html_parts.append(f"<li>{escaped}</li>")
                elif value.lower().endswith(".mp4") or "\\" in value and value.lower().endswith("mp4"):
                    pass
                else:
                    html_parts.append(f"<p>{escaped}</p>")
                    content_parts.append(value)
            for asset in images:
                image_index += 1
                if not asset.display_name_assigned:
                    asset.original_filename = display_filename(title, "图片", image_index, asset.extension)
                    asset.display_name_assigned = True
                if asset.sha256 not in seen_assets:
                    article_assets.append(asset)
                    seen_assets.add(asset.sha256)
                html_parts.append(f'<figure><img src="{asset.storage_key}" alt="{html.escape(title)}" loading="lazy"></figure>')
        if list_open:
            html_parts.append("</ul>")
        videos = [
            asset
            for filename, asset in video_assets.items()
            if VIDEO_BY_MARKER.get(filename) == path.name
        ]
        for video_index, asset in enumerate(videos, start=1):
            if not asset.display_name_assigned:
                asset.original_filename = display_filename(title, "视频", video_index, asset.extension)
                asset.display_name_assigned = True
            html_parts.append(f'<figure><video controls preload="metadata" src="{asset.storage_key}"></video></figure>')
        content_text = re.sub(r"\s+", " ", " ".join(content_parts)).strip()
        return Article(
            title=title,
            slug=stable_slug(source_url, source_hash),
            category_codes=[category_code],
            source_file=source_file,
            source_url=source_url,
            author_name=author_from(lines),
            published_at=published_at,
            content_html="\n".join(html_parts),
            content_text=content_text,
            source_hash=source_hash,
            content_hash=hashlib.sha256(content_text.encode("utf-8")).hexdigest(),
            assets=article_assets,
            videos=videos,
            source_records=[(source_file, source_hash, category_code, source_url)],
        )


def emit_sql(articles: list[Article], assets: OrderedDict[str, Asset], output_file: Path) -> None:
    lines = [
        "-- Legacy news archive, generated by scripts/build_legacy_news_seed.py. Do not edit by hand.",
        "-- Media files are packaged under classpath:/bootstrap/legacy-news and copied by BundledMediaBootstrapRunner.",
        "SET NAMES utf8mb4;",
        "START TRANSACTION;",
        "",
    ]
    all_assets = list(assets.values())
    for asset in all_assets:
        media_type = "VIDEO" if asset.mime_type == "video/mp4" else "IMAGE"
        lines += [
            "INSERT INTO media_asset (media_type, storage_key, original_filename, mime_type, file_size, sha256, deleted)",
            "SELECT " + ", ".join([sql_literal(media_type), sql_literal(asset.storage_key), sql_literal(asset.original_filename), sql_literal(asset.mime_type), str(asset.file_size), sql_literal(asset.sha256), "0"]),
            f"WHERE NOT EXISTS (SELECT 1 FROM media_asset WHERE sha256 = {sql_literal(asset.sha256)});",
            "",
        ]
    for article in articles:
        summary = article.content_text[:300]
        primary_category = article.category_codes[0]
        category_id = f"(SELECT id FROM article_category WHERE code = {sql_literal(primary_category)} LIMIT 1)"
        lines += [
            "INSERT INTO article (category_id, title, slug, summary, content_html, content_text, author_name, source_url, source_file, content_hash, source_published_at, published_at, status, sort_order, is_featured, home_sort_order, seo_title, seo_description, seo_keywords, deleted, version)",
            "SELECT " + ", ".join([
                category_id, sql_literal(article.title), sql_literal(article.slug), sql_literal(summary), sql_literal(article.content_html), sql_literal(article.content_text), sql_literal(article.author_name), sql_literal(article.source_url), sql_literal(article.source_file), sql_literal(article.content_hash), sql_literal(article.published_at), sql_literal(article.published_at), sql_literal("PUBLISHED"), "0", "0", "0", sql_literal(article.title), sql_literal(summary), sql_literal(",".join(article.category_codes)), "0", "0",
            ]),
            f"WHERE NOT EXISTS (SELECT 1 FROM article WHERE slug = {sql_literal(article.slug)});",
            "",
        ]
        for category_code in article.category_codes:
            relation_category_id = f"(SELECT id FROM article_category WHERE code = {sql_literal(category_code)} LIMIT 1)"
            lines += [
                "INSERT INTO article_category_relation (article_id, category_id, sort_order, deleted)",
                f"SELECT a.id, {relation_category_id}, 0, 0 FROM article a WHERE a.slug = {sql_literal(article.slug)}",
                f"AND NOT EXISTS (SELECT 1 FROM article_category_relation r WHERE r.article_id = a.id AND r.category_id = {relation_category_id});",
                "",
            ]
        cover = article.assets[0] if article.assets else None
        if cover:
            lines += [
                "UPDATE article SET cover_media_id = (SELECT id FROM media_asset WHERE sha256 = " + sql_literal(cover.sha256) + " LIMIT 1)",
                f"WHERE slug = {sql_literal(article.slug)} AND cover_media_id IS NULL;",
                "",
            ]
        for asset in article.assets:
            for usage in (["COVER", "INLINE"] if cover and asset.sha256 == cover.sha256 else ["INLINE"]):
                lines += [
                    "INSERT INTO article_media (article_id, media_id, usage_type, sort_order, deleted)",
                    f"SELECT a.id, m.id, {sql_literal(usage)}, 0, 0 FROM article a JOIN media_asset m ON m.sha256 = {sql_literal(asset.sha256)} WHERE a.slug = {sql_literal(article.slug)}",
                    f"AND NOT EXISTS (SELECT 1 FROM article_media am WHERE am.article_id = a.id AND am.media_id = m.id AND am.usage_type = {sql_literal(usage)});",
                    "",
                ]
        for asset in article.videos:
            lines += [
                "INSERT INTO article_media (article_id, media_id, usage_type, sort_order, deleted)",
                f"SELECT a.id, m.id, 'VIDEO', 0, 0 FROM article a JOIN media_asset m ON m.sha256 = {sql_literal(asset.sha256)} WHERE a.slug = {sql_literal(article.slug)}",
                "AND NOT EXISTS (SELECT 1 FROM article_media am WHERE am.article_id = a.id AND am.media_id = m.id AND am.usage_type = 'VIDEO');",
                "",
            ]
        for source_file, source_hash, category_code, source_url in article.source_records:
            lines += [
                "INSERT INTO content_import_record (article_id, category_code, source_file, source_url, source_hash, content_hash, import_status, imported_at, deleted)",
                f"SELECT a.id, {sql_literal(category_code)}, {sql_literal(source_file)}, {sql_literal(source_url)}, {sql_literal(source_hash)}, {sql_literal(article.content_hash)}, 'IMPORTED', NOW(), 0 FROM article a WHERE a.slug = {sql_literal(article.slug)}",
                f"AND NOT EXISTS (SELECT 1 FROM content_import_record c WHERE c.source_hash = {sql_literal(source_hash)});",
                "",
            ]
    lines += ["COMMIT;", ""]
    output_file.parent.mkdir(parents=True, exist_ok=True)
    output_file.write_text("\n".join(lines), encoding="utf-8")


def emit_filename_normalization(assets: OrderedDict[str, Asset], output_file: Path) -> None:
    lines = [
        "-- Normalize legacy-news media display filenames. Generated by scripts/build_legacy_news_seed.py.",
        "SET NAMES utf8mb4;",
        "START TRANSACTION;",
        "",
    ]
    for asset in assets.values():
        lines += [
            f"UPDATE media_asset SET original_filename = {sql_literal(asset.original_filename)} WHERE sha256 = {sql_literal(asset.sha256)}",
            f"AND original_filename <> {sql_literal(asset.original_filename)};",
            "",
        ]
    lines += ["COMMIT;", ""]
    output_file.parent.mkdir(parents=True, exist_ok=True)
    output_file.write_text("\n".join(lines), encoding="utf-8")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("source_root", type=Path, help="Folder containing the legacy DOCX files and MP4 files")
    parser.add_argument("--resource-root", type=Path, required=True, help="Output directory for bootstrap classpath resources")
    parser.add_argument("--migration", type=Path, required=True, help="Output Flyway SQL migration")
    parser.add_argument("--filename-migration", type=Path, required=True, help="Output Flyway SQL migration for media display names")
    args = parser.parse_args()
    source_root = args.source_root.resolve()
    media_root = args.resource_root / "media"
    if args.resource_root.exists():
        shutil.rmtree(args.resource_root)
    media_root.mkdir(parents=True, exist_ok=True)
    assets: OrderedDict[str, Asset] = OrderedDict()
    video_assets: dict[str, Asset] = {}
    for video in sorted(source_root.glob("*.mp4")):
        video_assets[video.name] = asset_from_bytes(video.read_bytes(), video.name, media_root, assets)
    parsed_articles = [parse_docx(path, source_root, media_root, assets, video_assets) for path in sorted(source_root.rglob("*.docx"))]
    by_slug: OrderedDict[str, Article] = OrderedDict()
    for article in parsed_articles:
        existing = by_slug.get(article.slug)
        if existing is None:
            by_slug[article.slug] = article
            continue
        for category_code in article.category_codes:
            if category_code not in existing.category_codes:
                existing.category_codes.append(category_code)
        existing.source_records.extend(article.source_records)
        for asset in article.assets:
            if asset.sha256 not in {item.sha256 for item in existing.assets}:
                existing.assets.append(asset)
        for video in article.videos:
            if video.sha256 not in {item.sha256 for item in existing.videos}:
                existing.videos.append(video)
    articles = list(by_slug.values())
    emit_sql(articles, assets, args.migration)
    emit_filename_normalization(assets, args.filename_migration)
    manifest = {
        "version": 1,
        "assets": [
            {"storage_key": asset.storage_key, "resource_path": asset.resource_path, "sha256": asset.sha256, "file_size": asset.file_size}
            for asset in assets.values()
        ],
    }
    (args.resource_root / "manifest.json").write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"Generated {len(articles)} articles and {len(assets)} unique media assets.")


if __name__ == "__main__":
    main()
