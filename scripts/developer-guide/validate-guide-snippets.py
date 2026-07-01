#!/usr/bin/env python3
import argparse
import re
import sys
from pathlib import Path


SOURCE_RE = re.compile(r"^\[source,([A-Za-z0-9_#+. -]+)(?:,.*)?\]\s*$")
INCLUDE_RE = re.compile(r"^include::([^\[]+)\[(.*)\]\s*$")
TAG_RE = re.compile(r"(?:^|,)tag=([^,\]]+)")
LISTING_DELIMITER_RE = re.compile(r"^-{4,}\s*$")


def parse_args():
    parser = argparse.ArgumentParser(
        description="Validate developer-guide source blocks against demo-backed includes."
    )
    parser.add_argument(
        "--docs-dir",
        default="docs/developer-guide",
        help="Directory containing guide .adoc/.asciidoc files.",
    )
    return parser.parse_args()


def guide_files(docs_dir):
    return sorted(
        p for p in Path(docs_dir).iterdir()
        if p.is_file() and p.suffix in (".adoc", ".asciidoc")
    )


def extract_source_blocks(docs_dir):
    docs_dir = Path(docs_dir)
    inline = []
    includes = []
    errors = []

    for source in guide_files(docs_dir):
        rel_source = str(source)
        lines = source.read_text(encoding="utf-8", errors="replace").splitlines()
        i = 0
        while i < len(lines):
            source_match = SOURCE_RE.match(lines[i])
            if not source_match:
                i += 1
                continue

            language = source_match.group(1)
            start_line = i + 1
            j = i + 1
            while j < len(lines) and not lines[j].strip():
                j += 1

            if j >= len(lines):
                errors.append(f"{rel_source}:{start_line}: source block has no body")
                i += 1
                continue

            include_match = INCLUDE_RE.match(lines[j].strip())
            if include_match:
                target = include_match.group(1)
                attrs = include_match.group(2)
                includes.append(
                    {
                        "sourceFile": rel_source,
                        "line": j + 1,
                        "language": language,
                        "target": target,
                        "attrs": attrs,
                    }
                )
                i = j + 1
                continue

            delimiter = lines[j].strip()
            if not LISTING_DELIMITER_RE.match(delimiter):
                errors.append(
                    f"{rel_source}:{start_line}: source block is neither include-backed nor fenced with ----"
                )
                i = j + 1
                continue

            body_start = j + 1
            k = body_start
            while k < len(lines) and lines[k].strip() != delimiter:
                k += 1
            if k >= len(lines):
                errors.append(f"{rel_source}:{start_line}: source block is missing closing ----")
                i = j + 1
                continue

            code_lines = lines[body_start:k]
            nonempty_code_lines = [line.strip() for line in code_lines if line.strip()]
            if len(nonempty_code_lines) == 1:
                include_match = INCLUDE_RE.match(nonempty_code_lines[0])
                if include_match:
                    target = include_match.group(1)
                    attrs = include_match.group(2)
                    includes.append(
                        {
                            "sourceFile": rel_source,
                            "line": body_start + 1,
                            "language": language,
                            "target": target,
                            "attrs": attrs,
                        }
                    )
                    i = k + 1
                    continue

            code = "\n".join(code_lines)
            inline.append(
                {
                    "sourceFile": rel_source,
                    "line": start_line,
                    "language": language,
                    "preview": " ".join(code.strip().split())[:120],
                }
            )
            i = k + 1

    return inline, includes, errors


def validate_includes(includes):
    errors = []
    cache = {}
    for item in includes:
        if not item["target"].startswith("../demos/"):
            errors.append(
                f"{item['sourceFile']}:{item['line']}: source include must target ../demos/: {item['target']}"
            )
            continue

        source_path = Path(item["sourceFile"])
        target_path = (source_path.parent / item["target"]).resolve()
        if not target_path.exists():
            errors.append(
                f"{item['sourceFile']}:{item['line']}: include target not found: {item['target']}"
            )
            continue

        tag_match = TAG_RE.search(item["attrs"])
        if not tag_match:
            continue

        tag = tag_match.group(1)
        text = cache.get(target_path)
        if text is None:
            text = target_path.read_text(encoding="utf-8", errors="replace")
            cache[target_path] = text
        if f"tag::{tag}[]" not in text:
            errors.append(
                f"{item['sourceFile']}:{item['line']}: include tag not found in {item['target']}: {tag}"
            )
        if f"end::{tag}[]" not in text:
            errors.append(
                f"{item['sourceFile']}:{item['line']}: include end tag not found in {item['target']}: {tag}"
            )
    return errors


def main():
    args = parse_args()
    inline, includes, errors = extract_source_blocks(args.docs_dir)
    errors.extend(validate_includes(includes))

    if inline:
        errors.append(
            f"{len(inline)} inline source block(s) found. "
            "Move all guide snippets into docs/demos and include them by tag."
        )
        for record in inline[:50]:
            errors.append(
                f"{record['sourceFile']}:{record['line']}: [{record['language']}] {record['preview']}"
            )

    print(f"Validated {len(includes)} include-backed source block(s).")

    if errors:
        for error in errors:
            print(error, file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
