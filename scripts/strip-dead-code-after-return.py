#!/usr/bin/env python3
"""Strip dead ``{ <single-stmt>; break; }`` blocks that follow a
``return`` or ``throw`` in worker-side translated JS.

The bytecode translator already strips these in its per-method post-emit
pipeline (see ``stripDeadCodeAfterTerminator`` in
``JavascriptMethodGenerator.java``), but ``esbuild --minify-syntax``
reorders and merges adjacent blocks inside switch cases without realising
that a trailing ``{pc=N;break}`` block becomes unreachable after the
preceding statements collapse into a return / throw. Esbuild's own
dead-code elimination is conservative inside switch cases and leaves
these blocks intact -- so we strip them here, post-esbuild.

Conservative: only drops a balanced ``{...}`` block whose contents are
exactly ``<ident> = <RHS>; break;``. Anything more complex (function
calls, multiple statements, control flow) is left untouched.

Usage:
    python3 strip-dead-code-after-return.py <file-or-dir> [...]
"""
from __future__ import annotations

import os
import sys
from typing import List, Tuple


def _skip_string(src: str, i: int) -> int:
    """Skip a JS string starting at ``src[i]``. Handles ``"..."``,
    ``'...'`` and template literals ```...``` (without ``${...}``
    substitution -- the translator's output uses backticks only for
    simple multi-glyph token strings). Returns the index of the closing
    quote, or -1 if unterminated."""
    quote = src[i]
    n = len(src)
    i += 1
    while i < n:
        c = src[i]
        if c == "\\":
            i += 2
            continue
        if c == quote:
            return i
        i += 1
    return -1


def _is_ident_part(c: str) -> bool:
    return c.isalnum() or c == "_" or c == "$"


def _find_expression_statement_end(src: str, i: int) -> int:
    """Find the end of a return/throw expression (top-level ``;``)."""
    n = len(src)
    paren = brace = bracket = 0
    while i < n:
        c = src[i]
        if c == '"' or c == "'" or c == "`":
            end = _skip_string(src, i)
            if end < 0:
                return -1
            i = end + 1
            continue
        if c == "(":
            paren += 1
        elif c == ")":
            if paren == 0:
                return i
            paren -= 1
        elif c == "{":
            brace += 1
        elif c == "}":
            if brace == 0:
                return i
            brace -= 1
        elif c == "[":
            bracket += 1
        elif c == "]":
            bracket -= 1
        elif c == ";" and paren == 0 and brace == 0 and bracket == 0:
            return i
        i += 1
    return n


def _match_brace(src: str, open_idx: int) -> int:
    n = len(src)
    depth = 0
    i = open_idx
    while i < n:
        c = src[i]
        if c == '"' or c == "'" or c == "`":
            end = _skip_string(src, i)
            if end < 0:
                return -1
            i = end + 1
            continue
        if c == "{":
            depth += 1
        elif c == "}":
            depth -= 1
            if depth == 0:
                return i
        i += 1
    return -1


def _find_statement_end_with_comma(src: str, i: int) -> int:
    """Find the end of an assignment statement -- stops at top-level
    ``;`` (NOT ``,``: ``a = b, c = d`` is two comma-expressions but the
    ``break`` follows the top-level ``;``)."""
    return _find_expression_statement_end(src, i)


def _is_dead_pc_bump_block(inner: str) -> bool:
    """True when ``inner`` is exactly ``<ident> = <RHS>; break;?`` --
    the shape a pc-bump trailer takes in worker-side translated JS."""
    n = len(inner)
    i = 0
    while i < n and inner[i].isspace():
        i += 1
    # Identifier (assignment LHS)
    ident_start = i
    while i < n and _is_ident_part(inner[i]):
        i += 1
    if i == ident_start:
        return False
    while i < n and inner[i].isspace():
        i += 1
    if i >= n or inner[i] != "=":
        return False
    # Avoid ``==`` / ``=>``
    if i + 1 < n and inner[i + 1] in ("=", ">"):
        return False
    rhs_end = _find_statement_end_with_comma(inner, i + 1)
    if rhs_end < 0 or rhs_end >= n or inner[rhs_end] != ";":
        return False
    p = rhs_end + 1
    while p < n and inner[p].isspace():
        p += 1
    if not inner.startswith("break", p):
        return False
    after_break = p + 5
    if after_break < n and _is_ident_part(inner[after_break]):
        return False
    while after_break < n and inner[after_break].isspace():
        after_break += 1
    if after_break < n and inner[after_break] == ";":
        after_break += 1
    while after_break < n and inner[after_break].isspace():
        after_break += 1
    return after_break == n


def strip_file(path: str) -> Tuple[int, int]:
    """Process a JS file in place. Returns (bytes_before, bytes_after)."""
    with open(path, "rb") as f:
        raw = f.read()
    src = raw.decode("utf-8")
    before = len(src)
    out: List[str] = []
    n = len(src)
    i = 0
    while i < n:
        c = src[i]
        if c == '"' or c == "'" or c == "`":
            end = _skip_string(src, i)
            if end < 0:
                out.append(src[i:])
                break
            out.append(src[i : end + 1])
            i = end + 1
            continue
        kw_len = 0
        if c == "r" and src.startswith("return", i):
            if (i == 0 or not _is_ident_part(src[i - 1])) and (
                i + 6 >= n or not _is_ident_part(src[i + 6])
            ):
                kw_len = 6
        elif c == "t" and src.startswith("throw", i):
            if (i == 0 or not _is_ident_part(src[i - 1])) and (
                i + 5 >= n or not _is_ident_part(src[i + 5])
            ):
                kw_len = 5
        if kw_len == 0:
            out.append(c)
            i += 1
            continue
        out.append(src[i : i + kw_len])
        i += kw_len
        stmt_end = _find_expression_statement_end(src, i)
        if stmt_end < 0 or stmt_end >= n:
            out.append(src[i:])
            break
        out.append(src[i : stmt_end + 1])
        i = stmt_end + 1
        if src[stmt_end] != ";":
            continue
        peek = i
        while peek < n and src[peek].isspace():
            peek += 1
        if peek >= n or src[peek] != "{":
            continue
        close_brace = _match_brace(src, peek)
        if close_brace < 0 or close_brace >= n:
            continue
        inner = src[peek + 1 : close_brace]
        if not _is_dead_pc_bump_block(inner):
            continue
        i = close_brace + 1
    result = "".join(out)
    after = len(result)
    if after != before:
        with open(path, "wb") as f:
            f.write(result.encode("utf-8"))
    return before, after


def iter_targets(args: List[str]):
    for arg in args:
        if os.path.isfile(arg):
            yield arg
        elif os.path.isdir(arg):
            for name in sorted(os.listdir(arg)):
                if not name.endswith(".js"):
                    continue
                # Skip vendor / loader chunks
                if name in ("browser_bridge.js", "port.js", "worker.js", "sw.js"):
                    continue
                if name.endswith("_native_handlers.js"):
                    continue
                yield os.path.join(arg, name)


def main(argv: List[str]) -> int:
    if len(argv) < 2:
        print("usage: strip-dead-code-after-return.py <file-or-dir> ...", file=sys.stderr)
        return 2
    total_before = 0
    total_after = 0
    for path in iter_targets(argv[1:]):
        before, after = strip_file(path)
        total_before += before
        total_after += after
        if before != after:
            print(
                f"[strip-dead-code] {os.path.basename(path)}: "
                f"{before} -> {after} (-{before - after} bytes)"
            )
    if total_before:
        saved = total_before - total_after
        print(
            f"[strip-dead-code] total saved: {saved} bytes "
            f"({100.0 * saved / total_before:.2f}%)"
        )
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
