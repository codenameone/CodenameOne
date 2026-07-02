#!/usr/bin/env python3
import argparse
import re
import shutil
from collections import defaultdict
from pathlib import Path


SOURCE_RE = re.compile(r"^\[source,([A-Za-z0-9_#+. -]+)(?:,.*)?\]\s*$")
INCLUDE_RE = re.compile(r"^include::([^\[]+)\[(.*)\]\s*$")
LISTING_DELIMITER_RE = re.compile(r"^-{4,}\s*$")

EXTENSIONS = {
    "bash": "sh",
    "sh": "sh",
    "shell": "sh",
    "java": "java",
    "kotlin": "kt",
    "xml": "xml",
    "css": "css",
    "javascript": "js",
    "json": "json",
    "properties": "properties",
    "html": "html",
    "objective-c": "m",
    "c": "c",
    "csharp": "cs",
    "sql": "sql",
    "text": "txt",
    "listing": "txt",
    "strings": "strings",
    "bytecode": "txt",
    "rpf": "txt",
}


def parse_args():
    parser = argparse.ArgumentParser(
        description="Move inline developer-guide source blocks into docs/demos tagged snippet fixtures."
    )
    parser.add_argument("--docs-dir", default="docs/developer-guide")
    parser.add_argument(
        "--snippets-dir",
        default="docs/demos/common/src/main/snippets/developer-guide",
    )
    parser.add_argument(
        "--clean",
        action="store_true",
        help="Remove generated snippet fixture files before migrating.",
    )
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="Report the planned migration without writing files.",
    )
    return parser.parse_args()


def guide_files(docs_dir):
    return sorted(
        p for p in Path(docs_dir).iterdir()
        if p.is_file() and p.suffix in (".adoc", ".asciidoc")
    )


def normalize_language(language):
    return " ".join(language.strip().split()).lower()


def snippet_extension(language):
    normalized = normalize_language(language)
    return EXTENSIONS.get(normalized, re.sub(r"[^a-z0-9]+", "-", normalized).strip("-") or "txt")


def tag_prefix(source):
    return re.sub(r"[^A-Za-z0-9]+", "-", source.stem).strip("-").lower()


def java_snippet_file(source):
    words = {
        "3d": "ThreeD",
        "ai": "Ai",
        "ios": "Ios",
        "nfc": "Nfc",
        "junit": "Junit",
        "svg": "Svg",
        "css": "Css",
        "io": "Io",
        "tvplatforms": "TvPlatforms",
    }
    parts = re.split(r"[^A-Za-z0-9]+", source.stem)
    name = "".join(words.get(part.lower(), part[:1].upper() + part[1:]) for part in parts if part)
    return f"{name}Snippets.java"


def is_include_body(lines):
    nonempty = [line.strip() for line in lines if line.strip()]
    return len(nonempty) == 1 and INCLUDE_RE.match(nonempty[0]) is not None


def migrate_file(source, snippets_dir, dry_run):
    original_lines = source.read_text(encoding="utf-8", errors="replace").splitlines()
    output_lines = []
    generated = defaultdict(list)
    migrated = 0
    counters = defaultdict(int)
    prefix = tag_prefix(source)

    i = 0
    while i < len(original_lines):
        source_match = SOURCE_RE.match(original_lines[i])
        if not source_match:
            output_lines.append(original_lines[i])
            i += 1
            continue

        language = source_match.group(1)
        j = i + 1
        blank_lines = []
        while j < len(original_lines) and not original_lines[j].strip():
            blank_lines.append(original_lines[j])
            j += 1

        if j >= len(original_lines):
            output_lines.append(original_lines[i])
            output_lines.extend(blank_lines)
            i = j
            continue

        delimiter = original_lines[j].strip()
        if not LISTING_DELIMITER_RE.match(delimiter):
            output_lines.append(original_lines[i])
            output_lines.extend(blank_lines)
            output_lines.append(original_lines[j])
            i = j + 1
            continue

        k = j + 1
        while k < len(original_lines) and original_lines[k].strip() != delimiter:
            k += 1
        if k >= len(original_lines):
            output_lines.extend(original_lines[i:])
            break

        code_lines = original_lines[j + 1:k]
        if is_include_body(code_lines):
            output_lines.extend(original_lines[i:k + 1])
            i = k + 1
            continue

        normalized_language = normalize_language(language)
        counters[normalized_language] += 1
        tag = f"{prefix}-{normalized_language.replace(' ', '-')}-{counters[normalized_language]:03d}"
        extension = snippet_extension(language)
        snippet_file = java_snippet_file(source) if extension == "java" else f"{prefix}.{extension}"
        include_path = f"../demos/common/src/main/snippets/developer-guide/{snippet_file}"
        generated[snippet_file].append((tag, code_lines))

        output_lines.append(original_lines[i])
        output_lines.extend(blank_lines)
        output_lines.append(original_lines[j])
        output_lines.append(f"include::{include_path}[tag={tag},indent=0]")
        output_lines.append(original_lines[k])
        migrated += 1
        i = k + 1

    if not dry_run and migrated:
        source.write_text("\n".join(output_lines) + "\n", encoding="utf-8")

    if not dry_run:
        snippets_dir.mkdir(parents=True, exist_ok=True)
        for snippet_file, entries in generated.items():
            target = snippets_dir / snippet_file
            chunks = [
                "// Generated from docs/developer-guide source blocks. Edit the guide snippets here, not inline.",
                "",
            ]
            for tag, code_lines in entries:
                chunks.append(f"// tag::{tag}[]")
                chunks.extend(code_lines)
                chunks.append(f"// end::{tag}[]")
                chunks.append("")
            target.write_text("\n".join(chunks), encoding="utf-8")

    return migrated


def main():
    args = parse_args()
    docs_dir = Path(args.docs_dir)
    snippets_dir = Path(args.snippets_dir)

    if args.clean and snippets_dir.exists() and not args.dry_run:
        shutil.rmtree(snippets_dir)

    total = 0
    by_file = {}
    for source in guide_files(docs_dir):
        count = migrate_file(source, snippets_dir, args.dry_run)
        if count:
            by_file[str(source)] = count
            total += count

    for source, count in sorted(by_file.items()):
        print(f"{source}: migrated {count}")
    print(f"Total migrated inline source blocks: {total}")


if __name__ == "__main__":
    main()
