"""Compress oversized images under public/images in place.

- Backs up originals to E:/alfy-projects/image-backup-20260903/ first
- JPEG/JPEG: resize to max 1920px on the long edge, quality=82, progressive
- PNG: resize to max 1920px, optimized save
- Only touches files larger than 500KB
"""
import shutil
import sys
from pathlib import Path

from PIL import Image

IMAGES_DIR = Path(r"E:/alfy-projects/alfy-web/public/images")
BACKUP_DIR = Path(r"E:/alfy-projects/image-backup-20260903")
MAX_EDGE = 1920
MIN_SIZE = 500 * 1024


def compress(path: Path) -> tuple[int, int]:
    original_size = path.stat().st_size
    backup_path = BACKUP_DIR / path.name
    if not backup_path.exists():
        shutil.copy2(path, backup_path)

    img = Image.open(path)
    img.load()
    if img.mode not in ("RGB", "L"):
        img = img.convert("RGB")

    w, h = img.size
    if max(w, h) > MAX_EDGE:
        scale = MAX_EDGE / max(w, h)
        img = img.resize((round(w * scale), round(h * scale)), Image.LANCZOS)

    suffix = path.suffix.lower()
    if suffix in (".jpg", ".jpeg"):
        img.save(path, "JPEG", quality=82, optimize=True, progressive=True)
    elif suffix == ".png":
        img.save(path, "PNG", optimize=True)
    elif suffix == ".webp":
        img.save(path, "WEBP", quality=82, method=6)
    else:
        return original_size, original_size
    return original_size, path.stat().st_size


def main() -> int:
    BACKUP_DIR.mkdir(parents=True, exist_ok=True)
    total_before = total_after = 0
    for path in sorted(IMAGES_DIR.iterdir()):
        if path.suffix.lower() not in (".jpg", ".jpeg", ".png", ".webp"):
            continue
        if path.stat().st_size <= MIN_SIZE:
            continue
        before, after = compress(path)
        total_before += before
        total_after += after
        print(f"{path.name}: {before/1024:.0f}KB -> {after/1024:.0f}KB")
    print(f"TOTAL: {total_before/1024/1024:.1f}MB -> {total_after/1024/1024:.1f}MB")
    return 0


if __name__ == "__main__":
    sys.exit(main())
