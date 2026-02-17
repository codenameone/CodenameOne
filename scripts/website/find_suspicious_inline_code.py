#!/usr/bin/env python3
"""
Report likely markdown/code formatting issues in blog posts.

This scanner is conservative for auto-fixes: it only auto-wraps simple inline
tokens. Structural issues (split code blocks, broken call chains, etc.) are
reported for manual review.
"""

from __future__ import annotations

import argparse
import re
from pathlib import Path

PATTERNS = [
    ("qualified_method", re.compile(r"\b(?:[A-Za-z_]\w*\.)+[A-Za-z_]\w*\(\)")),
    ("java_like_ref", re.compile(r"\b(?:java|javax|android|com|org|io|net|sun)\.[A-Za-z0-9_$.]+\b")),
    ("filename", re.compile(r"\b[A-Za-z0-9_.-]+\.(?:java|kt|xml|json|gradle|properties|jar|cn1lib|sh)\b")),
    ("xml_tag", re.compile(r"</?[A-Za-z_][A-Za-z0-9_.:-]*(?:\s+[^>]*)?>")),
]
PATTERN_NAMES = {name for name, _ in PATTERNS}

CODE_LINE_HINTS = [
    re.compile(r"^\s*\.\s*[A-Za-z_]\w*\s*\("),  # .method(
    re.compile(r"^\s*(?:public|private|protected|static|final)\b"),
    re.compile(r"^\s*(?:if|for|while|switch|try|catch)\b"),
    re.compile(r"^\s*(?:new\s+[A-Za-z_]\w*|[A-Za-z_]\w*\s+[A-Za-z_]\w*\s*=)"),
    re.compile(r"\b[A-Za-z_]\w*\.[A-Za-z_]\w*\s*\("),
]


def code_spans(line: str) -> list[tuple[int, int]]:
    spans: list[tuple[int, int]] = []
    i = 0
    while i < len(line):
        s = line.find("`", i)
        if s < 0:
            break
        e = line.find("`", s + 1)
        if e < 0:
            break
        spans.append((s, e + 1))
        i = e + 1
    return spans


def in_spans(pos: int, spans: list[tuple[int, int]]) -> bool:
    for s, e in spans:
        if s <= pos < e:
            return True
    return False


def is_markdown_scaffold(stripped: str) -> bool:
    if not stripped:
        return True
    return (
        stripped.startswith("#")
        or stripped.startswith("*")
        or stripped.startswith("-")
        or stripped.startswith(">")  # blockquote
        or stripped.startswith("|")
        or stripped.startswith("```")
        or stripped.startswith("[")
        or stripped.startswith("](")
        or stripped.startswith("![")
    )


def is_codeish_line(line: str) -> bool:
    stripped = line.strip()
    if is_markdown_scaffold(stripped):
        return False
    # Very short prose lines are unlikely to be code.
    if len(stripped) < 4:
        return False
    if stripped.endswith(";") or stripped in ("{", "}", "});", "};", ");"):
        return True
    if "=" in stripped and not stripped.endswith(":"):
        # assignment-like
        return True
    for rx in CODE_LINE_HINTS:
        if rx.search(stripped):
            return True
    return False


def next_non_empty_line(lines: list[str], i0: int) -> tuple[int | None, str | None]:
    for j in range(i0 + 1, len(lines)):
        if lines[j].strip():
            return j, lines[j]
    return None, None


def structural_findings(lines: list[str]):
    in_front_matter = False
    in_fence = False

    # Broken inline chain across lines, e.g. `CloudStorage.getInstance()` + next ".upload..."
    for i, line in enumerate(lines):
        stripped = line.strip()
        if i == 0 and stripped == "---":
            in_front_matter = True
            continue
        if in_front_matter:
            if stripped == "---":
                in_front_matter = False
            continue
        if stripped.startswith("```"):
            in_fence = not in_fence
            continue
        if in_fence:
            continue
        if stripped.startswith(">"):
            continue

        m = re.search(r"`[^`]+`\s*\.?\s*$", line)
        if m:
            j, nxt = next_non_empty_line(lines, i)
            if nxt is not None and re.match(r"^\s*\.\s*[A-Za-z_]\w*\s*\(", nxt):
                yield ("broken_chain", i + 1, f"{line.rstrip()}  <NEXT>  {nxt.rstrip()}")

    # Suspicious split expression block: code-like lines split by spacing, usually
    # created by broken markdown conversions around inline backticks.
    in_front_matter = False
    in_fence = False
    i = 0
    while i < len(lines):
        stripped = lines[i].strip()
        if i == 0 and stripped == "---":
            in_front_matter = True
            i += 1
            continue
        if in_front_matter:
            if stripped == "---":
                in_front_matter = False
            i += 1
            continue
        if stripped.startswith("```"):
            in_fence = not in_fence
            i += 1
            continue
        if in_fence:
            i += 1
            continue
        if stripped.startswith(">"):
            i += 1
            continue

        # Scan a short window for split expressions with clear damage indicators.
        window_end = min(len(lines), i + 14)
        chunk = lines[i:window_end]
        if any(ln.strip().startswith("```") for ln in chunk):
            i += 1
            continue
        code_lines = [k for k, ln in enumerate(chunk) if is_codeish_line(ln)]
        def has_codey_backtick(ln: str) -> bool:
            for m in re.finditer(r"`([^`]{1,120})`", ln):
                token = m.group(1)
                if any(ch in token for ch in (".", "(", ")", "=", ";", "<", ">")):
                    return True
            return False

        has_backticked_line = any(has_codey_backtick(ln) for ln in chunk)
        has_dot_chain = any(re.match(r"^\s*\.\s*[A-Za-z_]\w*\s*\(", ln) for ln in chunk)
        has_fragment_line = any(
            ln.strip() in {"(", ");", "}.start();", ").start();"} or re.match(r"^\s*[A-Za-z_]\w*\s*$", ln.strip())
            for ln in chunk
        )
        # High-signal only: we expect backticks plus fragmented call-chain lines.
        if len(code_lines) >= 2 and has_backticked_line and (has_dot_chain or has_fragment_line):
            first = i + code_lines[0]
            last = i + code_lines[-1]
            # Avoid re-flagging huge ranges repeatedly.
            if last - first <= 18:
                snippet = " | ".join(lines[first:last + 1]).strip()
                yield ("split_expr_block", first + 1, snippet[:240])
                i = last + 1
                continue
        i += 1

    # Multi-line markdown link labels where label is only a backticked token.
    # This tends to be formatting damage from auto-fixes.
    for i in range(0, len(lines) - 2):
        if lines[i].strip() == "[" and re.match(r"^\s*`[^`]+`\s*$", lines[i + 1].strip()) and lines[i + 2].strip().startswith("]("):
            yield ("code_label_link", i + 1, f"{lines[i + 1].strip()} -> {lines[i + 2].strip()}")


def scan_file(path: Path):
    text = path.read_text(encoding="utf-8")
    lines = text.splitlines()

    in_front_matter = False
    in_fence = False

    for i, line in enumerate(lines, start=1):
        stripped = line.strip()

        if i == 1 and stripped == "---":
            in_front_matter = True
            continue
        if in_front_matter:
            if stripped == "---":
                in_front_matter = False
            continue

        if stripped.startswith("```"):
            in_fence = not in_fence
            continue
        if in_fence:
            continue
        if line.startswith("    ") or line.startswith("\t"):
            continue
        if stripped.startswith(">"):
            continue
        if "](" in line or line.lstrip().startswith("!["):
            continue

        spans = code_spans(line)
        found = []
        for kind, rx in PATTERNS:
            for m in rx.finditer(line):
                token = m.group(0)
                if in_spans(m.start(), spans):
                    continue
                # Skip obvious URLs/domains for java_like_ref false positives
                if kind == "java_like_ref" and token.endswith((".com", ".org", ".net", ".io")):
                    continue
                found.append((kind, token, m.start(), m.end()))

        if found:
            yield i, line, found

    # Emit structural findings as pseudo-token rows with line context.
    for kind, line_no, snippet in structural_findings(lines):
        yield line_no, snippet, [(kind, snippet, 0, len(snippet))]


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--blog-dir", default="docs/website/content/blog")
    ap.add_argument("--max-files", type=int, default=0)
    ap.add_argument("--start-index", type=int, default=0, help="Start file index for batch processing")
    ap.add_argument("--apply", action="store_true", help="Apply backtick fixes for reported tokens")
    args = ap.parse_args()

    blog_dir = Path(args.blog_dir)
    files = sorted(blog_dir.rglob("*.md"))
    if args.start_index > 0:
        files = files[args.start_index :]
    if args.max_files > 0:
        files = files[: args.max_files]

    findings = 0
    files_with_findings = 0
    for path in files:
        rows = list(scan_file(path))
        if not rows:
            continue
        files_with_findings += 1
        print(f"\n## {path}")
        if args.apply:
            original = path.read_text(encoding="utf-8").splitlines()
            updated = original[:]
            for line_no, line, found in rows:
                # Wrap from right-to-left to preserve offsets.
                safe = [(k, tok, s, e) for k, tok, s, e in found if k in PATTERN_NAMES]
                repls = sorted(((s, e, tok) for _, tok, s, e in safe), key=lambda x: x[0], reverse=True)
                new_line = updated[line_no - 1]
                for s, e, tok in repls:
                    if s > 0 and new_line[s - 1] == "`":
                        continue
                    if e < len(new_line) and new_line[e] == "`":
                        continue
                    new_line = new_line[:s] + f"`{tok}`" + new_line[e:]
                updated[line_no - 1] = new_line
            path.write_text("\n".join(updated) + "\n", encoding="utf-8")

        for line_no, line, found in rows:
            findings += len(found)
            kinds = ", ".join(f"{k}:{t}" for k, t, _, _ in found)
            print(f"{line_no}: {kinds}")
            print(f"    {line}")

    mode = "Applied" if args.apply else "Reported"
    print(f"\n{mode} scan complete. Scanned {len(files)} files. Files with findings: {files_with_findings}. Tokens flagged: {findings}.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
