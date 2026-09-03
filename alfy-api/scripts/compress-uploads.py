"""Compress oversized images inside the alfy-api uploads storage in place.

- Scans the uploads root (skips .thumbnails cache and video files)
- Files larger than 500KB are resized to max 1920px on the long edge
- Effectively opaque images are re-encoded as JPEG q85 (keeps the original
  file name/extension; storage_key stays unchanged)
- Images with real transparency stay PNG (optimized)
- Originals are backed up BEFORE overwriting, relative paths preserved
- Emits compress-uploads-sync.sql so media_asset rows (mime_type, file_size,
  sha256) can be updated on the same database that serves these files

Usage:
  python compress-uploads.py [uploads_root] [backup_dir]
Defaults match the local dev layout. Run the generated SQL afterwards:
  mysql -uroot -p alfy_site < compress-uploads-sync.sql
"""
import hashlib
import shutil
import sys
from pathlib import Path

from PIL import Image

UPLOADS_ROOT = Path(sys.argv[1]) if len(sys.argv) > 1 else Path(
    r"E:/alfy-projects/alfy-api/data/alfy/uploads"
)
BACKUP_DIR = Path(sys.argv[2]) if len(sys.argv) > 2 else Path(
    r"E:/alfy-projects/uploads-backup-20260903"
)
SQL_OUT = Path(__file__).with_name("compress-uploads-sync.sql")
MAX_EDGE = 1920
MIN_SIZE = 500 * 1024
VIDEO_SUFFIXES = {".mp4", ".mpeg", ".mpg", ".mov", ".m4v", ".mkv", ".ogg",
                  ".ogv", ".webm", ".avi"}


def is_effectively_opaque(img: Image.Image) -> bool:
    if img.mode in ("RGB", "L"):
        return True
    if img.mode == "P":
        return "transparency" not in img.info
    if img.mode in ("RGBA", "LA"):
        alpha = img.getchannel("A")
        lo, _ = alpha.getextrema()
        return lo >= 255
    return True


def compress(path: Path) -> dict | None:
    original_size = path.stat().st_size
    try:
        img = Image.open(path)
        img.load()
    except Exception:
        return None

    w, h = img.size
    if max(w, h) > MAX_EDGE:
        scale = MAX_EDGE / max(w, h)
        img = img.resize((round(w * scale), round(h * scale)), Image.LANCZOS)

    if is_effectively_opaque(img):
        encode, fmt, mime = "JPEG", ("JPEG",), "image/jpeg"
        quality = dict(quality=85, optimize=True, progressive=True)
        payload = img.convert("RGB")
    else:
        encode, fmt, mime = "PNG", ("PNG",), "image/png"
        quality = dict(optimize=True)
        payload = img

    tmp = path.with_name(path.name + ".tmp-optimize")
    try:
        payload.save(tmp, encode, **quality)
        new_size = tmp.stat().st_size
        if new_size >= original_size:
            tmp.unlink(missing_ok=True)
            return None
        rel = path.relative_to(UPLOADS_ROOT).as_posix()
        backup = BACKUP_DIR / rel
        if not backup.exists():
            backup.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(path, backup)
        tmp.replace(path)
        digest = hashlib.sha256(path.read_bytes()).hexdigest()
        return {
            "key": rel,
            "before": original_size,
            "after": new_size,
            "mime": mime,
            "sha256": digest,
        }
    except Exception as exc:
        tmp.unlink(missing_ok=True)
        print(f"SKIP {path.name}: {exc}")
        return None


def main() -> int:
    if not UPLOADS_ROOT.is_dir():
        print(f"uploads root not found: {UPLOADS_ROOT}")
        return 1
    results = []
    for path in sorted(UPLOADS_ROOT.rglob("*")):
        if not path.is_file():
            continue
        if ".thumbnails" in path.parts or path.suffix.lower() in VIDEO_SUFFIXES:
            continue
        if path.name.endswith(".tmp-optimize"):
            continue
        if path.stat().st_size <= MIN_SIZE:
            continue
        outcome = compress(path)
        if outcome:
            results.append(outcome)
            print(f"{outcome['key']}: {outcome['before']//1024}KB -> "
                  f"{outcome['after']//1024}KB ({outcome['mime']})")

    total_before = sum(r["before"] for r in results)
    total_after = sum(r["after"] for r in results)
    print(f"\nCompressed {len(results)} files: "
          f"{total_before/1024/1024:.1f}MB -> {total_after/1024/1024:.1f}MB")

    if results:
        lines = ["-- Apply to the database that owns these media_asset rows",
                 "START TRANSACTION;"]
        for r in results:
            key = r["key"].replace("'", "''")
            lines.append(
                "UPDATE media_asset SET mime_type='{}', file_size={}, sha256='{}' "
                "WHERE storage_key='{}';".format(r["mime"], r["after"], r["sha256"], key)
            )
        lines.append("COMMIT;")
        SQL_OUT.write_text("\n".join(lines) + "\n", encoding="utf-8")
        print(f"SQL sync written: {SQL_OUT} "
              f"({sum(1 for r in results)} candidate rows)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
