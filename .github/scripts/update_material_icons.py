#!/usr/bin/env python3
"""Updates Codename One Material icon font and FontImage constants from upstream."""

from __future__ import annotations

import argparse
import hashlib
import pathlib
import re
import sys
import urllib.request

REPO_ROOT = pathlib.Path(__file__).resolve().parents[2]
FONT_PATH = REPO_ROOT / "CodenameOne" / "src" / "material-design-font.ttf"
FONT_IMAGE_PATH = REPO_ROOT / "CodenameOne" / "src" / "com" / "codename1" / "ui" / "FontImage.java"

MATERIAL_FONT_URL = (
    "https://raw.githubusercontent.com/google/material-design-icons/master/font/MaterialIcons-Regular.ttf"
)
MATERIAL_CODEPOINTS_URL = (
    "https://raw.githubusercontent.com/google/material-design-icons/master/font/MaterialIcons-Regular.codepoints"
)
MATERIAL_CSS_URL = "https://fonts.googleapis.com/icon?family=Material+Icons"

CONSTANT_DOC = (
    "    /// Material design icon font character code see\n"
    "    /// https://www.material.io/resources/icons/ for full list\n"
)
CONSTANT_RE = re.compile(r"^\s*public static final char MATERIAL_")
SENTINEL = "    private static Font materialDesignFont;"
MAX_JAVA_CHAR_CODEPOINT = 0xFFFF


def download_text(url: str) -> str:
    with urllib.request.urlopen(url) as response:
        return response.read().decode("utf-8")


def download_bytes(url: str) -> bytes:
    with urllib.request.urlopen(url) as response:
        return response.read()


def hash_bytes(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def material_constant_name(icon_name: str) -> str:
    return "MATERIAL_" + re.sub(r"[^A-Z0-9_]", "_", icon_name.upper())


def parse_codepoints(codepoints_text: str) -> list[tuple[str, str]]:
    out: list[tuple[str, str]] = []
    for line in codepoints_text.splitlines():
        line = line.strip()
        if not line:
            continue
        parts = line.split()
        if len(parts) != 2:
            raise ValueError(f"Unexpected codepoints line format: {line}")
        out.append((parts[0], parts[1]))
    return out


def java_char_literal(codepoint: str) -> str | None:
    value = int(codepoint, 16)
    if value > MAX_JAVA_CHAR_CODEPOINT:
        return None
    return f"\\u{value:04X}"


def generate_constants_block(entries: list[tuple[str, str]]) -> tuple[str, list[tuple[str, str]]]:
    lines: list[str] = []
    skipped: list[tuple[str, str]] = []
    for icon_name, codepoint in entries:
        const_name = material_constant_name(icon_name)
        char_literal = java_char_literal(codepoint)
        if char_literal is None:
            skipped.append((const_name, codepoint.upper()))
            lines.append(
                f"    // {const_name} omitted: U+{codepoint.upper()} is outside the Java char range."
            )
            continue
        lines.append(CONSTANT_DOC.rstrip("\n"))
        lines.append(f"    public static final char {const_name} = '{char_literal}';")
    return "\n".join(lines) + "\n", skipped


def is_material_constant_doc_line(line: str) -> bool:
    return line.lstrip().startswith("///")


def update_fontimage(constants_block: str, check_only: bool) -> bool:
    source = FONT_IMAGE_PATH.read_text(encoding="utf-8")
    lines = source.splitlines(keepends=True)

    start_idx = None
    end_idx = None
    for i, line in enumerate(lines):
        if start_idx is None and CONSTANT_RE.match(line):
            start_idx = i
            while start_idx > 0 and is_material_constant_doc_line(lines[start_idx - 1]):
                start_idx -= 1
        if line.strip("\n") == SENTINEL:
            end_idx = i
            break

    if start_idx is None or end_idx is None or start_idx >= end_idx:
        raise RuntimeError("Couldn't locate material constants block in FontImage.java")

    new_source = "".join(lines[:start_idx]) + constants_block + "".join(lines[end_idx:])
    changed = new_source != source
    if changed and not check_only:
        FONT_IMAGE_PATH.write_text(new_source, encoding="utf-8")
    return changed


def update_font(remote_font_bytes: bytes, check_only: bool) -> bool:
    local_font_bytes = FONT_PATH.read_bytes()
    changed = hash_bytes(local_font_bytes) != hash_bytes(remote_font_bytes)
    if changed and not check_only:
        FONT_PATH.write_bytes(remote_font_bytes)
    return changed


def extract_css_font_version(css: str) -> str | None:
    match = re.search(r"/materialicons/v(\d+)/", css)
    return match.group(1) if match else None


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true", help="Only check if an update is needed")
    args = parser.parse_args()

    print("Fetching Material Icons metadata...")
    css = download_text(MATERIAL_CSS_URL)
    version = extract_css_font_version(css)
    if version:
        print(f"Detected Google Fonts Material Icons version: v{version}")

    remote_font = download_bytes(MATERIAL_FONT_URL)
    codepoints = download_text(MATERIAL_CODEPOINTS_URL)
    entries = parse_codepoints(codepoints)
    constants_block, skipped = generate_constants_block(entries)
    if skipped:
        skipped_names = ", ".join(
            f"{const_name} (U+{codepoint})" for const_name, codepoint in skipped
        )
        print(
            "Skipping Material icons that cannot be represented as Java char constants: "
            f"{skipped_names}"
        )

    font_changed = update_font(remote_font, check_only=args.check)
    constants_changed = update_fontimage(constants_block, check_only=args.check)

    if font_changed or constants_changed:
        print("Material icons update detected.")
        return 0

    print("Material icons are already up to date.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
