#!/usr/bin/env python3
"""Prepare developer guide cover artwork for rasterization."""

from __future__ import annotations

import argparse
import pathlib
import re
import sys


VERSION_PATTERN = re.compile(r"Version\s+[^<]+")
CSS_VARIABLE_DECL_PATTERN = re.compile(r"--([\w-]+):\s*([^;]+);")
CSS_VARIABLE_REF_PATTERN = re.compile(r"var\(--([\w-]+)\)")


def parse_args(argv: list[str]) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("svg_path", help="Path to the SVG file to update")
    parser.add_argument("--rev-number", required=True, help="Revision number to embed")
    parser.add_argument("--rev-human-date", default="", help="Human readable revision date")
    parser.add_argument("--rev-date", default="", help="Fallback machine readable revision date")
    return parser.parse_args(argv)


def inject_version_label(svg_text: str, version_label: str) -> str:
    updated_text, count = VERSION_PATTERN.subn(version_label, svg_text, count=1)
    if count != 1:
        raise RuntimeError("Could not find version text placeholder in cover artwork")
    return updated_text


def resolve_css_variables(svg_text: str) -> tuple[str, int]:
    variables = {name: value.strip() for name, value in CSS_VARIABLE_DECL_PATTERN.findall(svg_text)}
    replacement_count = 0

    def replace(match: re.Match[str]) -> str:
        nonlocal replacement_count
        name = match.group(1)
        value = variables.get(name)
        if value is None:
            return match.group(0)
        replacement_count += 1
        return value

    resolved_text = CSS_VARIABLE_REF_PATTERN.sub(replace, svg_text)
    return resolved_text, replacement_count


def main(argv: list[str]) -> int:
    args = parse_args(argv)

    svg_path = pathlib.Path(args.svg_path)
    svg_text = svg_path.read_text(encoding="utf-8")

    revision_date = args.rev_human_date or args.rev_date
    version_label = f"Version {args.rev_number}".strip()
    if revision_date:
        version_label = f"{version_label} - {revision_date.strip()}"

    svg_text = inject_version_label(svg_text, version_label)
    svg_text, replacement_count = resolve_css_variables(svg_text)

    svg_path.write_text(svg_text, encoding="utf-8")

    print(f"Injected cover legend: {version_label}")
    print(f"Resolved {replacement_count} CSS variable references for rasterization")

    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
