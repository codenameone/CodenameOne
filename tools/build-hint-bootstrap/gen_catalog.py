#!/usr/bin/env python3
"""One-time bootstrap: emit the BuildHints* registration classes."""
import json, re, os, sys, collections

ROOT = "/Users/shai/dev/cn6/CodenameOne"
SC = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(ROOT, "maven/build-hint-catalog/src/main/java/com/codename1/build/shared")
LICENSE = open(os.path.join(SC, "license.txt")).read()

mined = json.load(open(os.path.join(SC, "mined.json")))

sys.path.insert(0, SC)
from curation import CURATED, ENUMS, PRIVACY_PREFIX, DEFAULT_NOTES, DOC_OVERRIDES, TYPE_OVERRIDES


def privacy_attr(name):
    """ios.NSCameraUsageDescription -> cameraUsageDescription."""
    body = name[len(PRIVACY_PREFIX):]
    return body[0].lower() + body[1:]

# ---------------------------------------------------------------- doc prose
def load_docs():
    # The guide's inline table has been replaced by the generated include, so the
    # prose no longer has a live source. Recover the pre-migration copy with
    #   git show <commit-before-this-branch>:docs/developer-guide/Advanced-Topics-Under-The-Hood.asciidoc
    # This bootstrap is a one-off: the catalog is the source of truth now and is
    # edited directly.
    p = os.path.join(SC, "guide_old.asciidoc")
    lines = open(p, encoding="utf-8").read().split("\n")[30:736]
    docs, i = {}, 0
    while i < len(lines):
        ln = lines[i]
        m = re.fullmatch(r'\|([A-Za-z][A-Za-z0-9_.<>\-]*)', ln.strip())
        # a Vale/AsciiDoc // directive may sit between the name and its description
        k = i + 1
        while k < len(lines) and lines[k].lstrip().startswith("//"):
            k += 1
        if m and k < len(lines) and lines[k].startswith("|"):
            body, j = [lines[k][1:]], k + 1
            while j < len(lines) and lines[j].strip() and not lines[j].startswith("|"):
                body.append(lines[j]); j += 1
            docs[m.group(1)] = " ".join(x.strip() for x in body).strip()
            i = j
        else:
            i += 1
    return docs

DOCS = load_docs()
print(f"doc rows parsed: {len(DOCS)}", file=sys.stderr)

def clean_doc(t):
    t = re.sub(r'<<[^,>]*,\s*([^>]*)>>', r'\1', t)   # <<anchor,text>> -> text
    t = re.sub(r'<<([^>]*)>>', r'\1', t)             # <<anchor>> -> anchor
    t = re.sub(r'\s+', ' ', t).strip()
    return t

# separators are authoritative -- read off LibraryHintMerger
SEPARATORS = {
    "android.gradleDep": ";", "gradleDependencies": "\n", "android.topDependency": "\n",
    "android.repositories": "\n", "android.xgradle": "\n", "android.gradle.androidx": "\n",
    "android.xgradle_default_config": "\n", "android.gradlePlugin": "\n",
    "android.supportv4Dep": "\n", "android.proguardKeep": "\n",
    "ios.pods": ",", "ios.applicationQueriesSchemes": ",", "ios.add_libs": ";",
    # joins with a space, but it is an XML attribute fragment rather than a list
    # the user thinks of as items, so it keeps HintType.XML -- see infer().
    "android.xapplication_attr": " ",
}
# Every entry above is a list the user edits as items; this one is not.
SEPARATOR_BUT_NOT_A_LIST = {"android.xapplication_attr"}
XML_HINTS = re.compile(r'^(android\.(xpermissions|xapplication|xmanifest|xactivity|'
                       r'xintent_filter|xqueries|xapplication_attr|xactivity_attr)|ios\.plistInject|'
                       r'ios\.entitlementsInject|.*Inject)$')

ID_NAME = re.compile(r'(^|[._])([a-z]+_)?id$|Id$|_id$', re.I)

def is_int_hint(name, lit):
    """A digit default is only an int if it is arithmetic, not an identifier.

    facebook.appId defaults to a 15-digit Facebook app id: it overflows a Java
    int and nothing ever adds to it. Such a hint is an opaque string.
    """
    if ID_NAME.search(name):
        return False
    try:
        v = int(lit)
    except ValueError:
        return False
    return -2**31 <= v < 2**31


def infer(name, defaults, doc):
    if name in TYPE_OVERRIDES:
        t, sep = TYPE_OVERRIDES[name]
        lit = None
        for x in defaults:
            if x.startswith('"') and x.endswith('"'):
                lit = x[1:-1]; break
            if x in ("true", "false") or re.fullmatch(r'-?\d+', x):
                lit = x; break
        return t, lit, sep
    d = [x for x in defaults if x not in ("<expr>", "null")]
    lit = None
    for x in d:
        if x.startswith('"') and x.endswith('"'):
            lit = x[1:-1]; break
        if x in ("true", "false") or re.fullmatch(r'-?\d+', x):
            lit = x; break
    dl = doc.lower()
    if name in SEPARATORS:
        if name in SEPARATOR_BUT_NOT_A_LIST:
            return "XML", lit, SEPARATORS[name]
        return "STRING_LIST", lit, SEPARATORS[name]
    if XML_HINTS.match(name):
        return "XML", lit, ""
    if lit in ("true", "false") or {x.strip('"') for x in d} <= {"true", "false"} and d:
        return "BOOLEAN", lit, None
    if lit is not None and re.fullmatch(r'-?\d+', lit) and is_int_hint(name, lit):
        return "INT", lit, None
    if "true/false" in dl or dl.startswith("boolean"):
        return "BOOLEAN", lit, None
    if lit is not None and re.fullmatch(r'\d+\.\d+(\.\d+)?', lit):
        return "VERSION", lit, None
    return "STRING", lit, None

PLATFORM = [("android.", "android"), ("and.", "android"), ("ios.", "ios"),
            ("macNative.", "mac"), ("desktop.mac.", "mac"), ("windows.", "windows"),
            ("win.", "windows"), ("linux.", "linux"), ("javascript.", "javascript"),
            ("desktop.", "desktop"), ("tvNative.", "tv"), ("watchNative.", "watch")]

IOS_PRIVACY = re.compile(r'^ios\.NS.*UsageDescription$')
ODD = re.compile(r'^(ios\.onDeviceDebug|android\.onDeviceDebug$)')

def group_of(name):
    if IOS_PRIVACY.match(name):  return "IOS_PRIVACY"
    if ODD.match(name):          return "ON_DEVICE_DEBUG"
    for p, g in [("ios.", "IOS"), ("android.", "ANDROID"), ("and.", "ANDROID"),
                 ("desktop.", "DESKTOP"), ("macNative.", "MAC_NATIVE"),
                 ("windows.", "WINDOWS"), ("linux.", "LINUX"),
                 ("javascript.", "JAVASCRIPT"), ("tvNative.", "TV_NATIVE"),
                 ("watchNative.", "WATCH_NATIVE"), ("harden.", "HARDENING")]:
        if name.startswith(p): return g
    return "GENERAL"

def platform_of(name):
    for p, v in PLATFORM:
        if name.startswith(p): return v
    return "general"

def jesc(s):
    return (s.replace("\\", "\\\\").replace('"', '\\"')
             .replace("\n", "\\n").replace("\t", "\\t").replace("\r", ""))

def wrap(text, indent, width=88):
    """Split a long Java string literal into concatenated chunks."""
    words, lines, cur = text.split(" "), [], ""
    for w in words:
        if len(cur) + len(w) + 1 > width and cur:
            lines.append(cur); cur = w
        else:
            cur = (cur + " " + w).strip()
    if cur: lines.append(cur)
    if not lines: return '""'
    if len(lines) == 1: return '"%s"' % jesc(lines[0])
    sep = "\n" + " " * indent + "+ "
    return sep.join('"%s "' % jesc(l) if i < len(lines) - 1 else '"%s"' % jesc(l)
                    for i, l in enumerate(lines))

FILES = {
    "BuildHintsIos":     lambda n: group_of(n) in ("IOS", "IOS_PRIVACY"),
    "BuildHintsAndroid": lambda n: group_of(n) == "ANDROID",
    "BuildHintsApple":   lambda n: group_of(n) in ("MAC_NATIVE", "TV_NATIVE", "WATCH_NATIVE"),
    "BuildHintsDesktop": lambda n: group_of(n) in ("DESKTOP", "WINDOWS", "LINUX", "JAVASCRIPT"),
    "BuildHintsGeneral": lambda n: group_of(n) in ("GENERAL", "HARDENING", "ON_DEVICE_DEBUG"),
}

BLURB = {
 "BuildHintsIos": "iOS build hints, including the Info.plist privacy strings.",
 "BuildHintsAndroid": "Android build hints, including the {@code and.} override aliases.",
 "BuildHintsApple": "macOS Catalyst, tvOS and watchOS native-slice build hints.",
 "BuildHintsDesktop": "Desktop, native Windows, native Linux and JavaScript build hints.",
 "BuildHintsGeneral": "Hints with no platform prefix, plus hardening and on-device debugging.",
}

# A mined key ending in a dot is the constant half of a concatenation --
# getArg("android.permission." + name) -- not a hint anyone can set. Cataloguing
# it would put a phantom row in the guide and a phantom entry in the Settings
# tool. Each one is covered by a dynamic family instead.
mined = {k: v for k, v in mined.items() if not k.endswith(".")}

counts = collections.Counter()
for fname, pred in FILES.items():
    names = sorted(n for n in mined if pred(n))
    counts[fname] = len(names)
    body = []
    for n in names:
        defaults = [d for d, _, _ in mined[n]]
        sites = sorted({os.path.basename(f)[:-5] for _, f, _ in mined[n]})
        doc = clean_doc(DOCS.get(n, ""))
        if not doc and n in DOC_OVERRIDES:
            doc = DOC_OVERRIDES[n]
        if n in DEFAULT_NOTES:
            if doc and not doc.rstrip().endswith((".", "!", "?")):
                doc = doc.rstrip() + "."
            doc = (doc + " " + DEFAULT_NOTES[n]).strip()
        htype, lit, sep = infer(n, defaults, doc)
        parts = ['        h.add(new Hint("%s")' % jesc(n)]
        g = group_of(n)
        cur = CURATED.get(n)
        enum_name = None
        if g == "IOS_PRIVACY":
            parts.append('                .annotatedAs(HintGroup.IOS_PRIVACY, "%s")' % privacy_attr(n))
            htype = "STRING"
        elif cur:
            cg, attr, enum_name, forced_type, forced_def = cur
            parts.append('                .annotatedAs(HintGroup.%s, "%s")' % (cg, attr))
            if forced_type:
                htype = forced_type
            if forced_def is not None:
                lit = forced_def
        else:
            parts.append('                .group(HintGroup.%s)' % g)
        if enum_name:
            vals = ", ".join('"%s"' % v for v in ENUMS[enum_name])
            parts.append('                .values("%s", %s)' % (enum_name, vals))
            if lit is not None and lit not in ENUMS[enum_name]:
                lit = None
        else:
            parts.append('                .type(HintType.%s)' % htype)
        if lit is not None and lit != "":
            parts.append('                .def("%s")' % jesc(lit))
        if sep is not None:
            parts.append('                .separator("%s")' % jesc(sep))
        parts.append('                .platform("%s")' % platform_of(n))
        parts.append('                .consumedBy(%s)' % ", ".join('"%s"' % s for s in sites))
        if doc:
            parts.append('                .doc(%s)' % wrap(doc, 24))
        body.append("\n".join(parts) + ");")

    src = LICENSE + f'''package com.codename1.build.shared;

import com.codename1.build.shared.BuildHints.Hint;

import java.util.List;

/**
 * {BLURB[fname]}
 *
 * <p>Seeded by mining every {{@code getArg}} call site in the builders, so the
 * name and the default match what the build actually reads. Curated entries
 * carry an annotation attribute and, where the domain is provably closed, an
 * enum; the rest are described but set through
 * {{@code codenameone_settings.properties}}.</p>
 *
 * <p>Split out of {{@link BuildHints}} because a single class initializer
 * holding every entry would exceed the JVM's 64KB per-method limit.</p>
 */
final class {fname} {{

    private {fname}() {{
    }}

    static void register(List<Hint> h) {{
''' + "\n\n".join(body) + "\n    }\n}\n"
    open(os.path.join(OUT, fname + ".java"), "w").write(src)

print("entries per file:", dict(counts), file=sys.stderr)
print("total:", sum(counts.values()), file=sys.stderr)
