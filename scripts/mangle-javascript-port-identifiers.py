#!/usr/bin/env python3
"""
Cross-file identifier mangler for the ParparVM JavaScript port bundle.

The translator emits very long identifiers everywhere:
  - Method names like ``cn1_com_codename1_ui_Form_setTitle_java_lang_String``
  - Class names like ``com_codename1_ui_Form``
  - Instance-field property names like ``cn1_com_codename1_ui_Form_title``

They appear as JS identifiers (function declarations and references), as
string literals (passed to ``jvm.resolveVirtual``, ``cn1_iv*`` helpers,
``jvm.setMain``, ``jvm.defineClass`` ``name`` / ``baseClass``), as object
keys (``{"com_codename1_ui_Form": true}``), and as bracketed property
accesses (``target["cn1_com_codename1_ui_Form_title"]``). Each such
occurrence is the full ~50-character string, and Initializr's output
contained ~525 000 such matches totalling ~28 MiB — 42 % of the bundle.
Esbuild can't help here: these are string literals, not variable names.

This script rewrites every translated_app*.js / parparvm_runtime.js /
supporting glue file in the output directory, assigning short ``$aaa``
symbols to each unique identifier (highest-frequency first so the
commonest names get the shortest mangled forms), and writes a
``mangle-map.json`` alongside for post-hoc demangling of stack traces.

The replacement is purely textual with a word-boundary anchor. The
identifiers we mangle all live in the ``cn1_`` / ``com_codename1_`` /
``java_*_`` / ``org_teavm_`` / ``kotlin_`` namespaces, which are
translator-owned (hand-written runtime and user code never uses those as
public surface), so a raw s/// pass is safe. Native JS shims that live
in a separate ``native/`` directory are NOT mangled — their symbols are
the unmangled ``cn1_get_native_interfaces`` exports consumed by generated
glue that we do patch separately.
"""

from __future__ import annotations

import argparse
import json
import os
import re
import sys
from collections import Counter
from pathlib import Path


# Namespaces owned entirely by the translator output. Hand-written user
# JS lives outside these prefixes, and the one well-known external symbol
# ``cn1_get_native_interfaces`` is added to EXCLUDE below.
IDENTIFIER_PATTERN = re.compile(
    r"\b("
    r"cn1_[A-Za-z0-9_]+"
    r"|com_codename1_[A-Za-z0-9_]+"
    r"|java_lang_[A-Za-z0-9_]+"
    r"|java_util_[A-Za-z0-9_]+"
    r"|java_io_[A-Za-z0-9_]+"
    r"|java_net_[A-Za-z0-9_]+"
    r"|java_nio_[A-Za-z0-9_]+"
    r"|java_math_[A-Za-z0-9_]+"
    r"|java_text_[A-Za-z0-9_]+"
    r"|java_time_[A-Za-z0-9_]+"
    r"|java_security_[A-Za-z0-9_]+"
    r"|java_awt_[A-Za-z0-9_]+"
    r"|org_teavm_[A-Za-z0-9_]+"
    r"|kotlin_[A-Za-z0-9_]+"
    r")\b"
)

# Identifiers that cross into hand-written webapp assets (js/fontmetrics.js,
# native/com_codename1_*.js) and must keep their original spelling so the
# cross-file linkage still works. If you add a new public runtime symbol in
# parparvm_runtime.js whose name matches IDENTIFIER_PATTERN, add it here too.
EXCLUDE = frozenset({
    # Host bridge registry populated by native/* scripts on the main thread
    # and read by worker-side stubs. Shared via window global.
    "cn1_get_native_interfaces",
    "cn1_native_interfaces",
    # Main-thread invocation helpers defined in webapp assets.
    "cn1_escape_single_quotes",
    "cn1_use_baseline_text_rendering",
    "cn1_debug_flags",
    "cn1_registerPush",
    "cn1_get_device_pixel_ratio",
})


# Base62 symbol generator: $a, $b, ... $z, $A, ... $Z, $0, ... $9, $aa, $ab, ...
# Gives us 62 * (1 + 62 + 62^2 + ...) symbols while keeping the common
# set at two bytes ($a = 2 bytes vs 40-80 bytes for the unmangled form).
_SYMBOL_ALPHABET = (
    "abcdefghijklmnopqrstuvwxyz"
    "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    "0123456789"
)


def symbol_for(index: int) -> str:
    """Produce a $-prefixed base62 symbol for the given rank."""
    if index < 0:
        raise ValueError(index)
    chars: list[str] = []
    n = index
    while True:
        chars.append(_SYMBOL_ALPHABET[n % 62])
        n //= 62
        if n == 0:
            break
        n -= 1
    return "$" + "".join(reversed(chars))


def collect_files(out_dir: Path) -> list[Path]:
    """Return the set of JS files that share the translator's worker-side
    identifier namespace.

    The mangler rewrites cn1_* / class-name identifiers in every file that
    the web worker loads against the *same* mapping, because the worker
    links these symbols together (bindNative overrides translated method
    names, translated code calls global ``cn1_iv*`` helpers, etc.).

    We skip:
      * browser_bridge.js and port.js — hand-authored main-thread code
        that talks to the browser DOM and uses public ``cn1_get_native_*``
        helpers whose names must stay stable (see EXCLUDE above).
      * worker.js / sw.js — tiny shells that just ``importScripts`` the
        mangled files; no identifiers of their own worth mangling.
      * Anything under a native/ or js/ subdir — app-provided native
        shims (``native/com_codename1_*``) and vendor runtime (jquery,
        bootstrap, fontmetrics) that the mangler has no business touching.

    App-supplied glue scripts like ``*_native_bindings.js`` are mangled
    because they call ``bindNative([...])`` with string names that must
    match the mangled identifier the translator emitted for the same
    Java static native. That's a worker-side cross-file link.
    """
    keep: list[Path] = []
    for entry in sorted(out_dir.iterdir()):
        if not entry.is_file():
            continue
        if entry.suffix != ".js":
            continue
        name = entry.name
        if name in {
            "browser_bridge.js",
            "port.js",
            "worker.js",
            "sw.js",
        }:
            continue
        # Main-thread host-bridge handlers pair up with files under native/
        # which we don't mangle — their JS-visible keys (e.g.
        # ``com_codename1_initializr_WebsiteThemeNative``) must remain
        # stable so cn1_get_native_interfaces() lookups keep working.
        if name.endswith("_native_handlers.js"):
            continue
        keep.append(entry)
    return keep


def collect_counts(files: list[Path]) -> Counter:
    counts: Counter = Counter()
    for path in files:
        data = path.read_text(encoding="utf-8")
        for match in IDENTIFIER_PATTERN.finditer(data):
            counts[match.group(0)] += 1
    for name in EXCLUDE:
        counts.pop(name, None)
    return counts


def build_mapping(counts: Counter) -> dict[str, str]:
    """Assign short symbols to the most frequent identifiers first.

    We only mangle when the mangled form is strictly shorter than the
    original — otherwise skipping leaves the source slightly larger but
    avoids bloating identifiers whose original name was already short.
    """
    ordered = sorted(counts.items(), key=lambda item: (-item[1], item[0]))
    mapping: dict[str, str] = {}
    for rank, (name, _count) in enumerate(ordered):
        new = symbol_for(rank)
        if len(new) >= len(name):
            continue
        mapping[name] = new
    return mapping


def rewrite(files: list[Path], mapping: dict[str, str]) -> int:
    """Apply the mapping to every file, returning the bytes saved.

    We scan with the single generic ``IDENTIFIER_PATTERN`` and look each
    match up in the mapping dict. Attempting an 80k-way alternation regex
    (one branch per mapped identifier) freezes Python's ``re`` engine for
    minutes — a single pattern + O(1) dict lookup does the same work in
    seconds.
    """
    if not mapping:
        return 0

    def substitute(match: re.Match) -> str:
        return mapping.get(match.group(0), match.group(0))

    before = after = 0
    for path in files:
        data = path.read_text(encoding="utf-8")
        before += len(data.encode("utf-8"))
        replaced = IDENTIFIER_PATTERN.sub(substitute, data)
        path.write_text(replaced, encoding="utf-8")
        after += len(replaced.encode("utf-8"))
    return before - after


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("output_dir", help="Translator output directory (the *-js/ folder)")
    ap.add_argument(
        "--min-occurrences",
        type=int,
        default=2,
        help="Skip identifiers that appear fewer than this many times (net loss to mangle).",
    )
    ap.add_argument(
        "--map-output",
        default=None,
        help="Where to write the reverse mangle map (JSON). Defaults to output_dir/mangle-map.json; "
             "callers that ship the output_dir to users typically redirect this somewhere outside "
             "so the ~6 MiB map doesn't bloat the shipped bundle.",
    )
    args = ap.parse_args()

    out_dir = Path(args.output_dir)
    if not out_dir.is_dir():
        print(f"[mangle] output dir missing: {out_dir}", file=sys.stderr)
        return 2

    files = collect_files(out_dir)
    if not files:
        print("[mangle] no eligible .js files in output dir", file=sys.stderr)
        return 0

    counts = collect_counts(files)
    # An identifier that appears once at all can't be shrunk (the one
    # definition site is its one use; mangling makes the file bigger by
    # the length of the mapping entry unless we're willing to write a
    # runtime lookup table — which we aren't).
    if args.min_occurrences > 1:
        counts = Counter({k: v for k, v in counts.items() if v >= args.min_occurrences})

    mapping = build_mapping(counts)
    saved = rewrite(files, mapping)

    total_bytes = sum(path.stat().st_size for path in files)
    print(
        f"[mangle] {len(mapping):,} identifiers mangled across {len(files)} files; "
        f"saved ~{saved / (1024 * 1024):.1f} MiB "
        f"(total after: {total_bytes / (1024 * 1024):.1f} MiB)"
    )

    # Persist the reverse mapping so stack traces / debugging can demangle
    # symbols after the fact without rebuilding.
    map_path = Path(args.map_output) if args.map_output else out_dir / "mangle-map.json"
    map_path.parent.mkdir(parents=True, exist_ok=True)
    reverse = {short: original for original, short in mapping.items()}
    map_path.write_text(json.dumps(reverse, indent=0, sort_keys=True), encoding="utf-8")
    return 0


if __name__ == "__main__":
    sys.exit(main())
