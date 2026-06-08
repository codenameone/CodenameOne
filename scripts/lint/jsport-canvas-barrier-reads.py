#!/usr/bin/env python3
"""Fail the build on worker->host barrier READS of canvas/context host-refs in
the JavaScript port.

The JS port runs translated Java in a Web Worker; the canvas / 2D context live
on the main thread and are reached over a postMessage bridge. Reading a value
back off one of those host-refs (``canvas.getWidth()``, ``ctx.getImageData()``,
``ctx.measureText()`` ...) is a round-trip that, under load, can return a
degraded value and livelock ``invokeJsoBridge``'s retry -- wedging the worker
(observed: the EDT parked on ``getter.width@HTMLCanvasElement`` during a form
transition). The surface-id render model keeps every such dimension/pixel value
Java-side, so these reads must NOT exist.

This linter is deliberately conservative: it tracks identifiers DECLARED as
``HTMLCanvasElement`` / ``CanvasRenderingContext2D`` (fields, locals, params)
within each file and flags the forbidden read accessors invoked on them. Writes
(``setWidth``, ``drawImage``, ``fillRect`` ...) are fire-and-forget and allowed.
A genuinely necessary read can be opted out with a trailing
``// LINT-ALLOW-CANVAS-BARRIER-READ: <reason>`` on the same line.

Usage:  python3 scripts/lint/jsport-canvas-barrier-reads.py
Exit 0 = clean, 1 = violations found.
"""
import os
import re
import sys

ROOT = os.path.join("Ports", "JavaScriptPort", "src", "main", "java")

# Read accessors that cross the barrier and can degrade. Keyed by the host-ref
# type they're forbidden on. (Writes/draws are fire-and-forget -> not listed.)
FORBIDDEN = {
    "HTMLCanvasElement": ("getWidth", "getHeight", "getContext"),
    "CanvasRenderingContext2D": ("getImageData", "measureText", "getCanvas",
                                 "getFont", "getFillStyle", "getStrokeStyle",
                                 "getGlobalAlpha", "isPointInPath", "getLineDash"),
}
ALLOW_MARKER = "LINT-ALLOW-CANVAS-BARRIER-READ"
TYPES = tuple(FORBIDDEN.keys())


def canvas_identifiers(text):
    """Names declared as a canvas/context host-ref type in this file."""
    names = {}
    for t in TYPES:
        # `HTMLCanvasElement foo`, `final CanvasRenderingContext2D bar`,
        # `(HTMLCanvasElement) x` casts excluded (no following identifier decl).
        for m in re.finditer(r"\b" + t + r"\s+([A-Za-z_]\w*)\b", text):
            names[m.group(1)] = t
    return names


def main():
    if not os.path.isdir(ROOT):
        print("jsport-canvas-barrier-reads: %s not found (run from repo root)" % ROOT,
              file=sys.stderr)
        return 2
    violations = []
    for dirpath, _dirs, files in os.walk(ROOT):
        for fn in files:
            if not fn.endswith(".java"):
                continue
            path = os.path.join(dirpath, fn)
            with open(path, encoding="utf-8", errors="replace") as f:
                lines = f.readlines()
            names = canvas_identifiers("".join(lines))
            if not names:
                continue
            for i, line in enumerate(lines, 1):
                if ALLOW_MARKER in line:
                    continue
                stripped = line.lstrip()
                # Skip comment lines (line comments, javadoc/block-comment
                # continuations) -- a real call wouldn't start with these.
                if stripped.startswith("//") or stripped.startswith("*") or stripped.startswith("/*"):
                    continue
                # Drop any trailing line-comment so `code(); // canvas.getWidth()`
                # doesn't false-positive on the comment.
                code = line.split("//", 1)[0]
                for name, t in names.items():
                    for method in FORBIDDEN[t]:
                        # `name.method(`  (allow an arbitrary receiver chain prefix
                        # like `instance.canvas.getWidth()` -> the `canvas.getWidth`
                        # tail still matches).
                        if re.search(r"\b" + re.escape(name) + r"\." + method + r"\s*\(", code):
                            violations.append((path, i, name, t, method, line.strip()))
    if violations:
        print("Forbidden canvas/context host-ref READS over the worker<->host "
              "barrier (JS port). Keep these values Java-side, or opt out with "
              "`// %s: <reason>`:\n" % ALLOW_MARKER)
        for path, i, name, t, method, src in sorted(violations):
            print("  %s:%d  %s.%s()  [%s]\n      %s" % (path, i, name, method, t, src))
        print("\n%d violation(s)." % len(violations))
        return 1
    print("jsport-canvas-barrier-reads: clean (no forbidden canvas/context reads).")
    return 0


if __name__ == "__main__":
    sys.exit(main())
