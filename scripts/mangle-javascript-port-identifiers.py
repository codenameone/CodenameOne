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
      * browser_bridge.js — main-thread host-bridge dispatcher. Uses
        plain ``cn1HostBridge.register`` symbol strings that the worker
        side sends via ``jvm.invokeHostNative(...)``; those symbols are
        application-chosen identifiers, not translator-owned names.
      * worker.js / sw.js — tiny shells that just ``importScripts`` the
        mangled files; no identifiers of their own worth mangling.

    We *do* mangle:
      * translated_app*.js — raw translator output.
      * parparvm_runtime.js — hosts ``resolveVirtual``, ``classes``, the
        cn1_iv* helpers, and a handful of inline method-name literals
        (e.g. ``"cn1_java_lang_Object_toString_R_java_lang_String"``)
        that must match the mangled identifier on the worker.
      * port.js — imported by worker.js. Contains 300+ cn1_* /
        class-name literals passed to ``bindCiFallback`` / ``bindNative``
        to install method overrides by name. These names are ONLY
        meaningful against the translator's emitted symbols, so they
        must move in lockstep with the mangler's output.
      * App-supplied ``*_native_bindings.js`` — worker-side
        ``bindNative([...])`` calls that register overrides on the
        generated WebsiteThemeNativeImpl static natives; their string
        arguments must match the mangled identifier.

    App-supplied main-thread ``*_native_handlers.js`` are skipped — they
    pair with files under native/ which we never mangle (their JS-visible
    keys in ``cn1_get_native_interfaces()`` are public API).
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


_CLASSDEF_NAME_PATTERN = re.compile(
    r'jvm\.defineClass\(\{\s*name:\s*"([A-Za-z0-9_]+)"'
)
_CLASSDEF_ASSIGNABLE_TAIL_PATTERN = re.compile(
    r'assignableTo:\s*\{([^}]*)\}'
)
_JSO_BRIDGE_MARKER = "com_codename1_html5_js_JSObject"


def _collect_jso_bridge_class_names(files: list[Path]) -> set[str]:
    """Find every class whose ``assignableTo`` set contains the JSO bridge
    marker. These classes go through ``jvm.invokeJsoBridge`` at runtime,
    which uses ``parseJsoBridgeMethod(className, methodId)`` — an explicit
    string split of ``methodId`` against ``"cn1_" + className + "_"`` — to
    recover the DOM member name the call is targeting (getter / setter /
    method). That split ONLY works when the method id is the unmangled
    ``cn1_<class>_<member>_<sig>`` form, because the host receiver has
    real JS properties named ``createElement`` / ``appendChild`` / etc.
    Mangling those ids to ``$a`` makes the runtime pass ``$a`` as the
    member name and the host throws "Missing JS member $a for host
    receiver". Returning the class names here lets the caller exclude
    every ``cn1_<jsoClass>_*`` identifier from the mangle pass.
    """
    jso_classes: set[str] = set()
    for path in files:
        data = path.read_text(encoding="utf-8")
        for match in _CLASSDEF_NAME_PATTERN.finditer(data):
            class_name = match.group(1)
            # Peek ahead at the assignableTo block for this defineClass
            # call. We bound the search to a reasonable window so runaway
            # scans on giant one-line-minified output don't degrade.
            window = data[match.end(): match.end() + 4096]
            tail = _CLASSDEF_ASSIGNABLE_TAIL_PATTERN.search(window)
            if tail and _JSO_BRIDGE_MARKER in tail.group(1):
                jso_classes.add(class_name)
    return jso_classes


def collect_counts(files: list[Path]) -> tuple[Counter, frozenset[str]]:
    counts: Counter = Counter()
    for path in files:
        data = path.read_text(encoding="utf-8")
        for match in IDENTIFIER_PATTERN.finditer(data):
            counts[match.group(0)] += 1
    for name in EXCLUDE:
        counts.pop(name, None)

    jso_bridge_classes = _collect_jso_bridge_class_names(files)
    # Exclude every ``cn1_<jsoClass>_*`` method id so ``parseJsoBridgeMethod``
    # keeps working against host DOM receivers. Also exclude the class name
    # itself, both because it flows through the runtime as a plain string
    # (the ``className`` argument of ``invokeJsoBridge`` / ``isJsoBridgeClass``
    # / ``classes[...]`` lookup) and so runtime-built ``"cn1_" + className +
    # "_"`` prefixes still match the unmangled method ids we just excluded.
    to_exclude: set[str] = set()
    for name in list(counts.keys()):
        for cls in jso_bridge_classes:
            if name.startswith("cn1_" + cls + "_") or name.startswith("cn1_" + cls + "__"):
                to_exclude.add(name)
                break
        if name in jso_bridge_classes:
            to_exclude.add(name)
    for name in to_exclude:
        counts.pop(name, None)

    preserved = frozenset(to_exclude | set(EXCLUDE) | jso_bridge_classes)
    return counts, preserved


_IMPL_SUFFIX = "__impl"


def build_mapping(counts: Counter) -> dict[str, str]:
    """Assign short symbols to the most frequent identifiers first.

    Identifiers with an ``X``/``X__impl`` twin are kept in lockstep:
    when ``X`` is mapped to ``$a``, ``X__impl`` is mapped to
    ``$a__impl``. This preserves runtime ``methodId + "__impl"``
    concatenation patterns in port.js (e.g. ctor lookup, CN1SS hooks)
    without requiring a lookup table.

    We only mangle when the mangled form is strictly shorter than the
    original — otherwise skipping leaves the source slightly larger but
    avoids bloating identifiers whose original name was already short.
    """
    names = set(counts.keys())
    # Bases: names without __impl suffix (plus __impl names whose base is
    # not present — orphans that can mangle freely).
    pairs: dict[str, str | None] = {}
    for name in sorted(names):
        if name.endswith(_IMPL_SUFFIX):
            base = name[: -len(_IMPL_SUFFIX)]
            if base in names:
                # handled via its base entry below
                continue
            # Orphan impl — mangle on its own; no twin exists in this bundle
            pairs[name] = None
            continue
        impl = name + _IMPL_SUFFIX
        pairs[name] = impl if impl in names else None

    def rank_key(base: str) -> tuple[int, str]:
        impl = pairs.get(base)
        total = counts[base]
        if impl:
            total += counts.get(impl, 0)
        return (-total, base)

    mapping: dict[str, str] = {}
    rank = 0
    for base in sorted(pairs.keys(), key=rank_key):
        impl = pairs[base]
        short = symbol_for(rank)
        rank += 1
        base_saves = len(short) < len(base)
        if impl:
            impl_short = short + _IMPL_SUFFIX
            impl_saves = len(impl_short) < len(impl)
        else:
            impl_short = None
            impl_saves = False
        # Mangle the pair atomically: either both move to the short form
        # or neither does. Splitting would break ``X + "__impl"`` lookups
        # at runtime (the mangled base would resolve but the appended
        # suffix would name a non-existent global).
        if impl is not None and not (base_saves and impl_saves):
            continue
        if impl is None and not base_saves:
            continue
        mapping[base] = short
        if impl is not None:
            mapping[impl] = impl_short  # type: ignore[assignment]
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

    counts, preserved = collect_counts(files)
    # An identifier that appears once at all can't be shrunk (the one
    # definition site is its one use; mangling makes the file bigger by
    # the length of the mapping entry unless we're willing to write a
    # runtime lookup table — which we aren't).
    if args.min_occurrences > 1:
        counts = Counter({k: v for k, v in counts.items() if v >= args.min_occurrences})

    mapping = build_mapping(counts)
    saved = rewrite(files, mapping)

    total_bytes = sum(path.stat().st_size for path in files)
    preserved_count = len(preserved)
    print(
        f"[mangle] {len(mapping):,} identifiers mangled across {len(files)} files; "
        f"saved ~{saved / (1024 * 1024):.1f} MiB "
        f"(total after: {total_bytes / (1024 * 1024):.1f} MiB; "
        f"preserved {preserved_count} JSO-bridge / excluded names)"
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
