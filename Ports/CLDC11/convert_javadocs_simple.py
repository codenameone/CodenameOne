#!/usr/bin/env python3
from __future__ import annotations

import argparse
import fnmatch
import html
import re
from pathlib import Path
from typing import Dict, Iterable, List, Tuple


# -----------------------------
# File walking
# -----------------------------

def iter_files(root: Path, includes: List[str], excludes: List[str]) -> Iterable[Path]:
    root = root.resolve()
    if root.is_file():
        yield root
        return

    for p in root.rglob("*"):
        if not p.is_file():
            continue
        rel = p.relative_to(root).as_posix()
        if includes and not any(fnmatch.fnmatch(rel, pat) for pat in includes):
            continue
        if any(fnmatch.fnmatch(rel, pat) for pat in excludes):
            continue
        yield p


# -----------------------------
# Legacy JavaDoc block detection
# -----------------------------

def find_javadoc_blocks(source: str) -> List[Tuple[int, int]]:
    blocks: List[Tuple[int, int]] = []

    i = 0
    n = len(source)
    in_string = False
    in_char = False
    in_line_comment = False
    in_block_comment = False

    def peek(k: int) -> str:
        return source[i + k] if i + k < n else ""

    while i < n:
        c = source[i]

        if in_line_comment:
            if c == "\n":
                in_line_comment = False
            i += 1
            continue

        if in_block_comment:
            if c == "*" and peek(1) == "/":
                in_block_comment = False
                i += 2
            else:
                i += 1
            continue

        if in_string:
            if c == "\\":
                i += 2
                continue
            if c == '"':
                in_string = False
            i += 1
            continue

        if in_char:
            if c == "\\":
                i += 2
                continue
            if c == "'":
                in_char = False
            i += 1
            continue

        if c == "/" and peek(1) == "/":
            in_line_comment = True
            i += 2
            continue

        if c == "/" and peek(1) == "*":
            if peek(2) == "*":
                start = i
                j = i + 3
                while j < n - 1:
                    if source[j] == "*" and source[j + 1] == "/":
                        end = j + 2
                        blocks.append((start, end))
                        i = end
                        break
                    j += 1
                else:
                    break
                continue
            else:
                in_block_comment = True
                i += 2
                continue

        if c == '"':
            in_string = True
            i += 1
            continue

        if c == "'":
            in_char = True
            i += 1
            continue

        i += 1

    return blocks


def strip_javadoc_stars(block_body: str) -> str:
    lines = block_body.splitlines()
    out: List[str] = []
    for line in lines:
        m = re.match(r"^\s*\*\s?(.*)$", line)
        out.append(m.group(1) if m else line.rstrip("\r"))

    while out and out[0].strip() == "":
        out.pop(0)
    while out and out[-1].strip() == "":
        out.pop()
    return "\n".join(out)


def line_start_and_indent(text: str, slash_index: int) -> Tuple[int, str]:
    line_start = text.rfind("\n", 0, slash_index) + 1
    prefix = text[line_start:slash_index]
    if prefix and any(ch not in (" ", "\t") for ch in prefix):
        return slash_index, ""
    return line_start, prefix


# -----------------------------
# JavaDoc -> Markdown conversion
# -----------------------------

LINK_RE = re.compile(r"\{@link\s+([^}]+)\}")
CODE_RE = re.compile(r"\{@code\s+([^}]+)\}")
LITERAL_RE = re.compile(r"\{@literal\s+([^}]+)\}")

A_TAG_RE = re.compile(r'<a\s+[^>]*href="([^"]+)"[^>]*>(.*?)</a>', re.IGNORECASE | re.DOTALL)
PRE_RE = re.compile(r"<pre\b[^>]*>(.*?)</pre>", re.IGNORECASE | re.DOTALL)
CODE_TAG_RE = re.compile(r"<code\b[^>]*>(.*?)</code>", re.IGNORECASE | re.DOTALL)

BR_RE = re.compile(r"<br\s*/?>", re.IGNORECASE)
P_OPEN_RE = re.compile(r"<p\b[^>]*>", re.IGNORECASE)
P_CLOSE_RE = re.compile(r"</p\s*>", re.IGNORECASE)

B_OPEN_RE = re.compile(r"<(b|strong)\b[^>]*>", re.IGNORECASE)
B_CLOSE_RE = re.compile(r"</(b|strong)\s*>", re.IGNORECASE)
I_OPEN_RE = re.compile(r"<(i|em)\b[^>]*>", re.IGNORECASE)
I_CLOSE_RE = re.compile(r"</(i|em)\s*>", re.IGNORECASE)

UL_OPEN_RE = re.compile(r"<ul\b[^>]*>", re.IGNORECASE)
UL_CLOSE_RE = re.compile(r"</ul\s*>", re.IGNORECASE)
OL_OPEN_RE = re.compile(r"<ol\b[^>]*>", re.IGNORECASE)
OL_CLOSE_RE = re.compile(r"</ol\s*>", re.IGNORECASE)
LI_OPEN_RE = re.compile(r"<li\b[^>]*>", re.IGNORECASE)
LI_CLOSE_RE = re.compile(r"</li\s*>", re.IGNORECASE)

ANY_TAG_RE = re.compile(r"</?[^>]+>")

PARAM_RE = re.compile(r"^@param\s+(\S+)\s*(.*)$")
RET_RE = re.compile(r"^@return\s*(.*)$")
THROWS_RE = re.compile(r"^@(throws|exception)\s+(\S+)\s*(.*)$")
SEE_RE = re.compile(r"^@see\s*(.*)$")
SINCE_RE = re.compile(r"^@since\s*(.*)$")
DEPR_RE = re.compile(r"^@deprecated\s*(.*)$")


def escape_backticks(s: str) -> str:
    return f"`{s}`" if "`" not in s else f"``{s}``"


def replace_inline_javadoc_tags(text: str) -> str:
    def repl_code(m: re.Match) -> str:
        return escape_backticks(html.unescape(m.group(1).strip()))

    def repl_literal(m: re.Match) -> str:
        return html.unescape(m.group(1).strip())

    def repl_link(m: re.Match) -> str:
        inner = m.group(1).strip()
        parts = inner.split()
        if len(parts) >= 2:
            label = " ".join(parts[1:])
            return escape_backticks(html.unescape(label))
        return escape_backticks(html.unescape(parts[0]))

    text = CODE_RE.sub(repl_code, text)
    text = LITERAL_RE.sub(repl_literal, text)
    text = LINK_RE.sub(repl_link, text)
    return text


def convert_html_to_markdown(text: str) -> str:
    def repl_pre(m: re.Match) -> str:
        inner = m.group(1).strip("\n\r")
        inner = CODE_TAG_RE.sub(lambda mm: mm.group(1), inner)
        inner = html.unescape(inner).strip("\n\r")
        return f"\n```java\n{inner}\n```\n"

    text = PRE_RE.sub(repl_pre, text)

    def repl_code_tag(m: re.Match) -> str:
        inner = html.unescape(m.group(1).strip())
        inner = re.sub(r"\s+", " ", inner).strip()
        return escape_backticks(inner)

    text = CODE_TAG_RE.sub(repl_code_tag, text)

    def repl_a(m: re.Match) -> str:
        href = m.group(1).strip()
        label = re.sub(r"\s+", " ", html.unescape(m.group(2)).strip())
        return f"[{label}]({href})"

    text = A_TAG_RE.sub(repl_a, text)

    text = B_OPEN_RE.sub("**", text)
    text = B_CLOSE_RE.sub("**", text)
    text = I_OPEN_RE.sub("*", text)
    text = I_CLOSE_RE.sub("*", text)

    text = BR_RE.sub("\n", text)
    text = P_OPEN_RE.sub("\n\n", text)
    text = P_CLOSE_RE.sub("\n\n", text)

    text = UL_OPEN_RE.sub("\n", text)
    text = UL_CLOSE_RE.sub("\n", text)
    text = OL_OPEN_RE.sub("\n", text)
    text = OL_CLOSE_RE.sub("\n", text)
    text = LI_OPEN_RE.sub("\n- ", text)
    text = LI_CLOSE_RE.sub("", text)

    text = ANY_TAG_RE.sub("", text)
    text = html.unescape(text)
    text = re.sub(r"\n{3,}", "\n\n", text)
    return text.strip()


def parse_block_tags(lines: List[str]) -> Tuple[List[str], Dict[str, List[Tuple[str, str]]], Dict[str, str]]:
    main: List[str] = []
    params: List[Tuple[str, str]] = []
    throws: List[Tuple[str, str]] = []
    see: List[Tuple[str, str]] = []
    singles: Dict[str, str] = {}

    i = 0
    n = len(lines)

    def read_continuation(start_i: int) -> Tuple[str, int]:
        parts: List[str] = []
        j = start_i
        while j < n:
            s = lines[j]
            if j == start_i:
                parts.append(s)
                j += 1
                continue
            if s.strip() == "":
                parts.append(s)
                j += 1
                continue
            if s.lstrip().startswith("@"):
                break
            parts.append(s)
            j += 1
        joined = "\n".join(parts).strip()
        joined = re.sub(r"\n{3,}", "\n\n", joined)
        return joined, j

    while i < n:
        line = lines[i]
        stripped = line.lstrip()

        if stripped.startswith("@"):
            if PARAM_RE.match(stripped):
                name = PARAM_RE.match(stripped).group(1)
                blob, i2 = read_continuation(i)
                desc = PARAM_RE.sub(r"\2", blob, count=1).strip()
                params.append((name, desc))
                i = i2
                continue

            if THROWS_RE.match(stripped):
                exc = THROWS_RE.match(stripped).group(2)
                blob, i2 = read_continuation(i)
                desc = THROWS_RE.sub(r"\3", blob, count=1).strip()
                throws.append((exc, desc))
                i = i2
                continue

            if RET_RE.match(stripped):
                blob, i2 = read_continuation(i)
                singles["return"] = RET_RE.sub(r"\1", blob, count=1).strip()
                i = i2
                continue

            if SEE_RE.match(stripped):
                blob, i2 = read_continuation(i)
                see.append((SEE_RE.sub(r"\1", blob, count=1).strip(), ""))
                i = i2
                continue

            if SINCE_RE.match(stripped):
                blob, i2 = read_continuation(i)
                singles["since"] = SINCE_RE.sub(r"\1", blob, count=1).strip()
                i = i2
                continue

            if DEPR_RE.match(stripped):
                blob, i2 = read_continuation(i)
                singles["deprecated"] = DEPR_RE.sub(r"\1", blob, count=1).strip()
                i = i2
                continue

            blob, i2 = read_continuation(i)
            main.append(blob)
            i = i2
            continue

        main.append(line)
        i += 1

    return main, {"params": params, "throws": throws, "see": see}, singles


def build_markdown(main_text: str, lists: Dict[str, List[Tuple[str, str]]], singles: Dict[str, str]) -> str:
    parts: List[str] = []
    if main_text.strip():
        parts.append(main_text.strip())

    params = lists.get("params") or []
    throws = lists.get("throws") or []
    see = lists.get("see") or []

    if params:
        parts.append("#### Parameters")
        for name, desc in params:
            parts.append(f"- `{name}`: {desc.strip()}" if desc.strip() else f"- `{name}`")

    if "return" in singles and singles["return"].strip():
        parts.append("#### Returns")
        parts.append(singles["return"].strip())

    if throws:
        parts.append("#### Throws")
        for exc, desc in throws:
            parts.append(f"- `{exc}`: {desc.strip()}" if desc.strip() else f"- `{exc}`")

    if "since" in singles and singles["since"].strip():
        parts.append("#### Since")
        parts.append(singles["since"].strip())

    if "deprecated" in singles and singles["deprecated"].strip():
        parts.append("#### Deprecated")
        parts.append(singles["deprecated"].strip())

    if see:
        parts.append("#### See also")
        for item, _ in see:
            item = item.strip()
            if item:
                parts.append(f"- {item}")

    out = "\n\n".join(p.strip() for p in parts if p.strip())
    out = re.sub(r"\n{3,}", "\n\n", out).strip()
    return out


def cleanup_markdown_indentation(md: str) -> str:
    """
    Remove accidental 4+ leading spaces on prose lines outside fenced code blocks.
    This fixes cases where old JavaDoc indentation becomes Markdown code blocks.
    """
    lines = md.replace("\r\n", "\n").replace("\r", "\n").split("\n")
    out: List[str] = []
    in_fence = False

    for line in lines:
        stripped = line.lstrip()

        if stripped.startswith("```"):
            in_fence = not in_fence
            out.append(line)
            continue

        if in_fence:
            out.append(line)
            continue

        if line.startswith("    "):
            out.append(line.lstrip())
        else:
            out.append(line)

    text = "\n".join(out)
    text = re.sub(r"\n{3,}", "\n\n", text)
    return text.strip()


def convert_legacy_javadoc_to_md(raw: str) -> str:
    raw = replace_inline_javadoc_tags(raw)
    raw = convert_html_to_markdown(raw)

    lines = raw.splitlines()
    main_lines, lists, singles = parse_block_tags(lines)

    main_text = "\n".join(main_lines).strip()
    out = build_markdown(main_text, lists, singles)
    out = cleanup_markdown_indentation(out)
    return "\n".join(ln.rstrip() for ln in out.splitlines()).strip()


# -----------------------------
# Emit Java 25 /// comments
# -----------------------------

def render_triple_slash_doc(md: str, indent: str) -> str:
    lines = md.replace("\r\n", "\n").replace("\r", "\n").split("\n")
    if lines == [""]:
        return f"{indent}///"

    out: List[str] = []
    for ln in lines:
        if ln == "":
            out.append(f"{indent}///")
        else:
            out.append(f"{indent}/// {ln}")
    return "\n".join(out)


# -----------------------------
# File conversion
# -----------------------------

def convert_file_in_place(path: Path, dry_run: bool = False) -> bool:
    try:
        src = path.read_text(encoding="utf-8")
    except UnicodeDecodeError:
        return False

    blocks = find_javadoc_blocks(src)
    if not blocks:
        return False

    out = src

    for start, end in reversed(blocks):
        replace_start, indent = line_start_and_indent(out, start)

        legacy_block = out[start:end]
        normalized = legacy_block.replace("\r\n", "\n").replace("\r", "\n")

        header_idx = normalized.find("/**")
        if header_idx == -1:
            continue

        inner_body = normalized[header_idx + 3 : -2]
        raw_content = strip_javadoc_stars(inner_body)

        md = convert_legacy_javadoc_to_md(raw_content)
        triple = render_triple_slash_doc(md, indent)

        out = out[:replace_start] + triple + out[end:]

    if out != src:
        if not dry_run:
            path.write_text(out, encoding="utf-8", newline="\n")
        return True

    return False


# -----------------------------
# Main
# -----------------------------

def main() -> None:
    ap = argparse.ArgumentParser(
        description="Convert legacy /** ... */ JavaDoc to Java 25 /// Markdown doc comments."
    )
    ap.add_argument("root", help="Root directory or single .java file")
    ap.add_argument("--include", action="append", default=["**/*.java"], help='Include glob(s), default: "**/*.java"')
    ap.add_argument("--exclude", action="append", default=[], help='Exclude glob(s), e.g. "**/build/**"')
    ap.add_argument("--dry-run", action="store_true", help="Report changes but do not write files")
    args = ap.parse_args()

    root = Path(args.root)

    scanned = 0
    changed = 0

    for f in iter_files(root, args.include, args.exclude):
        scanned += 1
        if convert_file_in_place(f, args.dry_run):
            changed += 1
            print(f"CHANGED: {f}")

    if args.dry_run:
        print(f"\nDry run complete. Scanned {scanned} file(s), would change {changed}.")
    else:
        print(f"\nDone. Scanned {scanned} file(s), changed {changed}.")


if __name__ == "__main__":
    main()