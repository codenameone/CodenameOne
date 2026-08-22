#!/usr/bin/env python3
"""Emit BuildHintsExternal: hints the developer guide documents that no code in
this repository reads. Most are consumed by build-daemon lanes whose source is
not mirrored here, so their absence is not evidence they are dead -- which is
exactly why they are recorded rather than enforced."""
import json, sys, os, re
SC = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, SC)
import gen_catalog as G

ROOT = "/Users/shai/dev/cn6/CodenameOne"
OUT = os.path.join(ROOT, "maven/build-hint-catalog/src/main/java/com/codename1/build/shared")
LICENSE = G.load_license()

with open(SC + "/mined.json", encoding="utf-8") as _fh:
    mined = set(json.load(_fh))
DOCS = G.load_docs()
PLACEHOLDER = re.compile(r'PERMISSION_NAME|[A-Z_]{4,}$|[<>]')

names = sorted(k for k in DOCS
               if k not in mined and not PLACEHOLDER.search(k) and "." in k or
                  (k not in mined and not PLACEHOLDER.search(k) and k.islower()))
names = sorted(set(n for n in names if not PLACEHOLDER.search(n)))

body = []
for n in names:
    doc = G.clean_doc(DOCS[n])
    htype, lit, sep = G.infer(n, [], doc)
    parts = ['        h.add(new Hint("%s")' % G.jesc(n)]
    parts.append('                .group(HintGroup.%s)' % G.group_of(n))
    parts.append('                .type(HintType.%s)' % htype)
    if sep is not None:
        parts.append('                .separator("%s")' % G.jesc(sep))
    parts.append('                .platform("%s")' % G.platform_of(n))
    parts.append('                .external()')
    if doc:
        parts.append('                .doc(%s)' % G.wrap(doc, 24))
    body.append("\n".join(parts) + ");")

src = LICENSE + '''package com.codename1.build.shared;

import com.codename1.build.shared.BuildHints.Hint;

import java.util.List;

/**
 * Hints the developer guide documents that nothing in this repository reads.
 *
 * <p>Most are consumed by build-daemon lanes whose source is not mirrored here,
 * so having no in-repo consumer is not evidence that a hint is dead. A few are
 * probably genuinely obsolete. Recording the distinction as
 * {@link Hint#isExternal()} keeps both the drift gate and the Settings tool
 * honest: the gate does not demand a consumer for these, and the tool still
 * offers them for editing.</p>
 *
 * <p>They are deliberately not annotated. Exposing a hint as a typed attribute
 * is a promise that setting it does something, and for these that promise
 * cannot be checked from this repository.</p>
 */
final class BuildHintsExternal {

    private BuildHintsExternal() {
    }

    static void register(List<Hint> h) {
''' + "\n\n".join(body) + "\n    }\n}\n"
with open(os.path.join(OUT, "BuildHintsExternal.java"), "w", encoding="utf-8") as _fh:
    _fh.write(src)
print("external entries:", len(names), file=sys.stderr)
