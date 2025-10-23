#!/usr/bin/env python3
"""Identify unreferenced images in the developer guide."""
from __future__ import annotations

import argparse
import json
from pathlib import Path
from typing import Iterable, List

ASCIIDOC_EXTENSIONS = {".adoc", ".asciidoc"}


def iter_text_files(root: Path) -> Iterable[Path]:
    for path in root.rglob("*"):
        if path.is_file() and path.suffix.lower() in ASCIIDOC_EXTENSIONS:
            yield path


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("doc_root", type=Path, help="Path to the developer guide root directory")
    parser.add_argument(
        "--image-dir",
        type=Path,
        default=None,
        help="Directory containing images (defaults to <doc_root>/img)",
    )
    parser.add_argument(
        "--output",
        type=Path,
        default=None,
        help="Optional path to write a JSON report",
    )
    args = parser.parse_args()

    doc_root = args.doc_root.resolve()
    image_dir = (args.image_dir or (doc_root / "img")).resolve()

    if not image_dir.exists():
        raise SystemExit(f"Image directory '{image_dir}' does not exist")

    adoc_files = list(iter_text_files(doc_root))
    contents = [path.read_text(encoding="utf-8", errors="ignore") for path in adoc_files]

    unused: List[str] = []
    for image_path in sorted(image_dir.rglob("*")):
        if not image_path.is_file():
            continue
        rel_path = image_path.relative_to(doc_root).as_posix()
        if any(rel_path in text for text in contents):
            continue
        # Also fall back to checking just the file name to catch references that rely on imagesdir.
        filename = image_path.name
        if any(filename in text for text in contents):
            continue
        unused.append(rel_path)

    report = {"unused_images": unused}

    if args.output:
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(json.dumps(report, indent=2), encoding="utf-8")

    if unused:
        print("Unused images detected:")
        for rel_path in unused:
            print(f" - {rel_path}")
    else:
        print("No unused images found.")


if __name__ == "__main__":
    main()
