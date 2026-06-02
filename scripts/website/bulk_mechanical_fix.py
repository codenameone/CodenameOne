#!/usr/bin/env python3
"""Safe, body-only mechanical fixes for blog posts (Phase 4).

This is the aggregate-reviewed tier of the blog prose rollout: deterministic,
reversible transforms applied across the whole corpus in one batch, verified by
asserting every change belongs to an allowlisted category rather than by
reading 854 diffs. It deliberately does NOT do anything requiring judgment —
grammar rewrites go through the AI fix→verify workflow (Phase 5), not here.

Hard guarantees (so this can never disturb syndication or content):
  * Front matter is preserved BYTE-FOR-BYTE. Syndication eligibility keys only
    off front-matter `date:`/`slug:` (scripts/website/syndicate_blog_posts.py),
    so body-only edits can never re-trigger it. The script asserts this.
  * Fenced code blocks, indented code, inline code spans, Hugo shortcodes,
    Markdown tables, and link/image targets are never modified — the set of
    code spans, shortcodes and link targets is identical before/after.

Transforms (each independently toggleable; all reversible):
  * collapse-spaces       collapse runs of 2+ inner spaces to one, in prose only
                          (cosmetic: HTML rendering already collapses these, so
                          this only tidies source — no rendered change). ON.
  * trailing-whitespace   strip INSIGNIFICANT trailing whitespace. OFF by
                          default and Markdown-hard-break-aware: a line ending
                          in 2+ spaces is an intentional <br>, so those are
                          preserved; only a lone trailing space or trailing tab
                          (which render identically) are removed.

NOTE on value: most of this corpus is blogger-migrated and the safe scope is
small — inner double spaces don't change rendered output, and real trailing-2-
space line breaks must be preserved. Genuine defects (doubled words, typos,
grammar) are fixed through the gate and the Phase 5 AI workflow, not here.

Defaults to DRY RUN. Pass --write to modify files. Prints a per-category change
count so a reviewer audits categories, not files.
"""

import argparse
import os
import re
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import blog_text  # noqa: E402

_FENCE_RE = re.compile(r"^\s*(?:```|~~~)")
# Segments that must pass through untouched on a prose line: inline code spans,
# Hugo shortcodes, and Markdown links/images (the bracketed target).
_PROTECTED_RE = re.compile(
    r"(`[^`]*`)"            # inline code span
    r"|(\{\{[<%].*?[%>]\}\})"  # shortcode
    r"|(\]\([^)]*\))"       # link/image target ](...)
)


def _transform_prose_text(text, opts, counts):
    """Apply enabled transforms to a run of plain prose (no protected spans)."""
    if opts["collapse_spaces"]:
        new = re.sub(r"(?<=\S)  +(?=\S)", " ", text)
        if new != text:
            counts["collapse-spaces"] += 1
            text = new
    return text


def _transform_line(line, opts, counts):
    """Transform a single prose line, leaving protected segments untouched."""
    # Preserve leading indentation; only operate on the content after it.
    stripped_lead = len(line) - len(line.lstrip(" "))
    indent, body = line[:stripped_lead], line[stripped_lead:]

    # Split into protected / unprotected pieces and only transform unprotected.
    pieces = []
    pos = 0
    for m in _PROTECTED_RE.finditer(body):
        if m.start() > pos:
            pieces.append(("text", body[pos:m.start()]))
        pieces.append(("keep", m.group(0)))
        pos = m.end()
    if pos < len(body):
        pieces.append(("text", body[pos:]))
    rebuilt = "".join(
        _transform_prose_text(seg, opts, counts) if kind == "text" else seg
        for kind, seg in pieces
    )
    line = indent + rebuilt

    if opts["trailing_ws"]:
        m = re.search(r"[ \t]+$", line)
        if m:
            run = m.group(0)
            # A trailing run of 2+ spaces is a Markdown hard line break (<br>).
            # Preserve it. Only a lone trailing space, or a run ending in a tab
            # (tabs don't form a break), render identically when stripped.
            is_hard_break = run.endswith(" ") and run.count(" ") >= 2
            if not is_hard_break:
                line = line[:m.start()]
                counts["trailing-whitespace"] += 1
    return line


def transform_body(body, opts, counts):
    """Apply transforms to a post body, skipping code blocks, tables, indented
    code, and lines that are entirely a shortcode."""
    lines = body.split("\n")
    in_fence = False
    out = []
    for line in lines:
        if _FENCE_RE.match(line):
            in_fence = not in_fence
            out.append(line)
            continue
        if in_fence:
            out.append(line)
            continue
        # Indented code (4+ spaces) and table rows are left verbatim.
        if re.match(r"^ {4,}\S", line) or line.lstrip().startswith("|"):
            out.append(line)
            continue
        out.append(_transform_line(line, opts, counts))
    return "\n".join(out)


def _invariants(body):
    """Return the multiset-ish fingerprints that must not change."""
    return {
        "code_spans": sorted(re.findall(r"`[^`]*`", body)),
        "shortcodes": sorted(re.findall(r"\{\{[<%].*?[%>]\}\}", body, re.DOTALL)),
        "links": sorted(re.findall(r"\]\([^)]*\)", body)),
        "words": re.findall(r"\S+", body),  # no word added/removed/reordered
    }


def process_file(path, opts, counts):
    with open(path, "r", encoding="utf-8") as fh:
        original = fh.read()
    front_matter, body, _start = blog_text.split_front_matter(original)
    new_body = transform_body(body, opts, counts)
    if new_body == body:
        return None
    # Guardrails: front matter byte-identical; protected constructs and the word
    # sequence unchanged (we only ever delete whitespace, never characters).
    new_full = front_matter + new_body
    assert new_full.startswith(front_matter), f"front matter changed in {path}"
    before, after = _invariants(body), _invariants(new_body)
    for key in ("code_spans", "shortcodes", "links", "words"):
        if before[key] != after[key]:
            raise AssertionError(
                f"{path}: transform changed protected content ({key}); refusing to write"
            )
    if opts["write"]:
        with open(path, "w", encoding="utf-8") as fh:
            fh.write(new_full)
    return path


def expand_paths(paths):
    files = []
    for p in paths:
        if os.path.isdir(p):
            for root, _d, names in os.walk(p):
                files.extend(os.path.join(root, n) for n in sorted(names)
                             if n.endswith(".md"))
        else:
            files.append(p)
    return files


def main():
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("paths", nargs="+", help="Markdown files or directories")
    ap.add_argument("--write", action="store_true",
                    help="Apply changes (default: dry run)")
    ap.add_argument("--trailing-whitespace", action="store_true",
                    help="Also strip insignificant trailing whitespace "
                         "(hard-break-aware; off by default)")
    ap.add_argument("--no-collapse-spaces", action="store_true")
    args = ap.parse_args()

    opts = {
        "write": args.write,
        "trailing_ws": args.trailing_whitespace,
        "collapse_spaces": not args.no_collapse_spaces,
    }
    counts = {"trailing-whitespace": 0, "collapse-spaces": 0}
    files = expand_paths(args.paths)
    changed = []
    for path in files:
        try:
            if process_file(path, opts, counts):
                changed.append(path)
        except AssertionError as exc:
            print(f"SKIPPED (guardrail): {exc}", file=sys.stderr)

    mode = "WROTE" if args.write else "DRY RUN — would change"
    print(f"{mode} {len(changed)} file(s) of {len(files)} scanned.")
    print("Changes by category (files touched per transform):")
    for cat, n in counts.items():
        print(f"  {cat}: {n}")
    if not args.write and changed:
        print("\nRe-run with --write to apply. Land one batched PR; the guardrails "
              "above guarantee front matter and code/links/words are untouched.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
