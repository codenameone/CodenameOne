#!/usr/bin/env python3
# Generates the Rive object model base classes of the Codename One Rive
# runtime (CodenameOne/src/com/codename1/impl/rive) from the generated C++
# base headers of rive-runtime (MIT, https://github.com/rive-app/rive-runtime).
#
#   scripts/rive/gen-rive-schema.py <rive-runtime checkout>          # write
#   scripts/rive/gen-rive-schema.py <rive-runtime checkout> --check  # verify
#
# For every type it writes <Name>Base.java (keys, fields, defaults, generic
# property access) and, when no hand-written <Name>.java exists, a trivial
# <Name>.java placeholder carrying PLACEHOLDER_MARK. Hand-written logic
# replaces a placeholder: delete the marker line and the generator leaves the
# file alone. RiveRegistry.java holds the factory and the field-type table.
import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import rive_schema  # noqa: E402

REPO = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
OUT = os.path.join(REPO, "CodenameOne", "src", "com", "codename1", "impl", "rive")
SCHEMA = os.path.join(REPO, "maven", "rive-transcoder", "src", "main", "java", "com", "codename1", "rive",
                      "transcoder", "RiveSchema.java")
EXCLUSIONS = os.path.join(REPO, "scripts", "copyright-header-exclusions.txt")
PLACEHOLDER_MARK = "// rive-gen: placeholder, replace by deleting this line"
EXCLUSION_RATIONALE = "Rive object model derived from rive-runtime (MIT, Copyright (c) 2020 Rive); keeps the upstream notice"

# Luau scripting is out of scope: those types are read and skipped. The
# exception is ScriptedInterpolator, which upstream's build without
# WITH_RIVE_SCRIPTING still instantiates as a linear interpolator, so a
# keyframe that names one keeps playing instead of being dropped.
SCRIPTED_KEPT = ("ScriptedInterpolator",)

def excluded(name, info):
    if name in SCRIPTED_KEPT:
        return False
    return info["dir"] == "scripted" or name.startswith("Script")

HEADER = """/*
 * MIT License
 *
 * Copyright (c) 2020 Rive
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 * Java rewrite of rive-runtime for Codename One.
 * Codename One modifications Copyright (c) 2026 Codename One.
 */
"""

JAVA_TYPE = {"CoreDouble": "float", "CoreUint": "int", "CoreId": "int", "CoreInt": "int",
             "CoreColor": "int", "CoreBool": "boolean", "CoreString": "String",
             "CoreBytes": "byte[]"}
# generic accessor family per field kind
FAMILY = {"CoreDouble": "Double", "CoreUint": "Uint", "CoreId": "Uint", "CoreInt": "Uint",
          "CoreColor": "Color", "CoreBool": "Bool", "CoreString": "String",
          "CoreBytes": "Bytes"}
FIELD_ID = {"CoreDouble": 2, "CoreUint": 0, "CoreId": 0, "CoreInt": 0, "CoreColor": 3,
            "CoreBool": 4, "CoreString": 1, "CoreBytes": 1}


def snake(name):
    return re.sub(r"(?<=[a-z0-9])([A-Z])", r"_\1", name).upper()


def java_default(prop):
    core = prop["coreType"]
    d = prop["default"]
    jt = JAVA_TYPE[core]
    if not d:
        return {"float": "0f", "int": "0", "boolean": "false", "String": "\"\"", "byte[]": "null"}[jt]
    if d in ("kEmptyId",):
        return "-1"
    if d == "Core::invalidPropertyKey":
        return "0"
    if jt == "String":
        return d.replace("\\x24", "$")
    if jt == "float":
        return d if d.endswith("f") else d + "f"
    return d


def cap(n):
    return n[0].upper() + n[1:]


def hand_methods(cls):
    """Names of methods the hand-written `cls`.java declares, if it exists."""
    path = os.path.join(OUT, cls + ".java")
    if not os.path.exists(path):
        return set()
    return set(re.findall(r"^\s+(?:public|protected)[\w\s<>\[\]]*?\s(\w+)\(", open(path).read(), re.M))


def inherited_hand_methods(name, types):
    """Methods declared by hand in an ancestor of `name`: a generated getter
    or setter with one of these names overrides it."""
    out = set()
    parent = types[name]["parent"] if name in types else None
    while parent and parent in types:
        out |= hand_methods(parent)
        parent = types[parent]["parent"]
    out |= hand_methods("Component") | hand_methods("Core")
    return out


COPY_ROOTS = ("DataConverter", "DataBind", "FormulaToken", "StringTemplateArgument", "DataConverterGroupItem")


def copy_family(name, types):
    n = name
    while n in types:
        if n in COPY_ROOTS:
            return True
        n = types[n]["parent"]
    return False


def gen_copy(name, info, types, props):
    # Upstream's generated XBase::copy and clone(): data binding clones its
    # converters (and their binds, items, tokens, arguments) per bind.
    if not copy_family(name, types):
        return []
    root = name in COPY_ROOTS
    out = []
    # A type with no properties of its own inherits copyFrom: an override
    # that only called super would be PMD's UselessOverridingMethod.
    if root or props:
        out.append("    /// Copies this type's properties from `source`, which is of this type.\n")
        if not root:
            out.append("    @Override\n")
        out.append("    protected void copyFrom(Core source) {\n")
        if not root:
            out.append("        super.copyFrom(source);\n")
        if props:
            out.append("        %sBase o = (%sBase) source;\n" % (name, name))
            for p in props:
                out.append("        m%s = o.m%s;\n" % (cap(p["name"]), cap(p["name"])))
        out.append("    }\n\n")
    if root and name not in COPY_CONCRETE:
        out.append("    /// A copy of this object, or null for a type that cannot be built.\n")
        out.append("    public Core makeCopy() {\n        return null;\n    }\n\n")
    if name in COPY_CONCRETE:
        out.append("    @Override\n" if not root else "    /// A copy of this object.\n")
        out.append("    public Core makeCopy() {\n        %s c = new %s();\n        c.copyFrom(this);\n        return c;\n    }\n\n" % (name, name))
    return out


COPY_CONCRETE = set()


def gen_base(name, info, types):
    inherited = inherited_hand_methods(name, types)
    props = [p for p in info["props"] if p["coreType"]]
    parent = info["parent"]
    out = [HEADER, "package com.codename1.impl.rive;\n\n"]
    out.append("/// Generated from rive-runtime's `%sBase` by `scripts/rive/gen-rive-schema.py`;\n" % name)
    out.append("/// do not edit. Behaviour belongs in `%s`.\n" % name)
    out.append("public abstract class %sBase extends %s {\n" % (name, parent))
    out.append("    public static final int TYPE_KEY = %d;\n" % info["typeKey"])
    for p in props:
        out.append("    public static final int %s_PROPERTY_KEY = %d;\n" % (snake(p["name"]), p["key"]))
    out.append("\n")
    for p in props:
        out.append("    protected %s m%s = %s;\n" % (JAVA_TYPE[p["coreType"]], cap(p["name"]), java_default(p)))
    out.append("\n    @Override\n    public int coreType() {\n        return TYPE_KEY;\n    }\n\n")
    out.extend(gen_copy(name, info, types, props))
    out.append("    @Override\n    public boolean isTypeOf(int typeKey) {\n        return typeKey == TYPE_KEY || super.isTypeOf(typeKey);\n    }\n")
    for p in props:
        jt = JAVA_TYPE[p["coreType"]]
        c = cap(p["name"])
        getter = ("is" if jt == "boolean" else "get") + c
        if jt == "boolean" and c.startswith("Is") and len(c) > 2 and c[2].isupper():
            getter = "is" + c[2:]
        ov = "    @Override\n" if getter in inherited else ""
        out.append("\n%s    public %s %s() {\n        return m%s;\n    }\n" % (ov, jt, getter, c))
        if jt == "byte[]":
            out.append("\n    protected void decode%s(byte[] value) {\n        m%s = value;\n    }\n" % (c, c))
            continue
        # float: compare bits, an exact "unchanged" test (SpotBugs FE_*)
        cmp = ("Float.floatToIntBits(m%s) == Float.floatToIntBits(value)" % c) if jt == "float" else \
            ("m%s == value" % c) if jt != "String" else "(m%s == null ? value == null : m%s.equals(value))" % (c, c)
        ov = "    @Override\n" if ("set" + c) in inherited else ""
        out.append("\n%s    public void set%s(%s value) {\n        if (%s) {\n            return;\n        }\n"
                   "        m%s = value;\n        %sChanged();\n    }\n" % (ov, c, jt, cmp, c, p["name"]))
        out.append("\n    protected void %sChanged() {\n    }\n" % p["name"])
    # deserialize + generic set/get per family
    fams = {}
    for p in props:
        fams.setdefault(FAMILY[p["coreType"]], []).append(p)
    for fam, ps in sorted(fams.items()):
        jt = JAVA_TYPE[ps[0]["coreType"]] if fam != "Uint" else "int"
        out.append("\n    @Override\n    public boolean deserialize%s(int propertyKey, %s value) {\n        switch (propertyKey) {\n" % (fam, jt))
        for p in ps:
            c = cap(p["name"])
            if fam == "Bytes":
                out.append("            case %s_PROPERTY_KEY:\n                decode%s(value);\n                return true;\n" % (snake(p["name"]), c))
            else:
                out.append("            case %s_PROPERTY_KEY:\n                m%s = value;\n                return true;\n" % (snake(p["name"]), c))
        out.append("            default:\n                return super.deserialize%s(propertyKey, value);\n        }\n    }\n" % fam)
        if fam == "Bytes":
            continue
        out.append("\n    @Override\n    public boolean set%sProperty(int propertyKey, %s value) {\n        switch (propertyKey) {\n" % (fam, jt))
        for p in ps:
            out.append("            case %s_PROPERTY_KEY:\n                set%s(value);\n                return true;\n" % (snake(p["name"]), cap(p["name"])))
        out.append("            default:\n                return super.set%sProperty(propertyKey, value);\n        }\n    }\n" % fam)
        out.append("\n    @Override\n    public %s get%sProperty(int propertyKey) {\n        switch (propertyKey) {\n" % (jt, fam))
        for p in ps:
            out.append("            case %s_PROPERTY_KEY:\n                return m%s;\n" % (snake(p["name"]), cap(p["name"])))
        out.append("            default:\n                return super.get%sProperty(propertyKey);\n        }\n    }\n" % fam)
    out.append("}\n")
    return "".join(out)


def gen_placeholder(name):
    return (HEADER + "package com.codename1.impl.rive;\n\n" + PLACEHOLDER_MARK + "\n"
            "/// Rive `%s`, with no behaviour beyond its stored properties.\n"
            "public class %s extends %sBase {\n}\n" % (name, name, name))


def gen_registry(types, concrete, all_types):
    out = [HEADER, "package com.codename1.rive.transcoder;\n\n"]
    out.append("/// Generated by `scripts/rive/gen-rive-schema.py`; do not edit. The runtime\n")
    out.append("/// class of every object type a `.riv` file can hold, and the wire type of\n")
    out.append("/// every property key. Build time only: nothing here ships in an app.\n")
    out.append("public final class RiveSchema {\n")
    out.append("    public static final int FIELD_UINT = 0;\n    public static final int FIELD_STRING = 1;\n")
    out.append("    public static final int FIELD_DOUBLE = 2;\n    public static final int FIELD_COLOR = 3;\n")
    out.append("    public static final int FIELD_BOOL = 4;\n    public static final int FIELD_BYTES = 5;\n")
    out.append("    public static final int FIELD_UNKNOWN = -1;\n\n")
    out.append("    private RiveSchema() {\n    }\n\n")
    out.append("    /// The simple name of the `com.codename1.impl.rive` class for `typeKey`,\n")
    out.append("    /// or null for a type the runtime skips.\n")
    out.append("    public static String className(int typeKey) {\n        switch (typeKey) {\n")
    for name in sorted(concrete, key=lambda n: types[n]["typeKey"]):
        out.append("            case %d:\n                return \"%s\";\n" % (types[name]["typeKey"], name))
    out.append("            default:\n                return null;\n        }\n    }\n\n")
    out.append("    /// The rive-runtime name of `typeKey`, for build notes, or null.\n")
    out.append("    public static String typeName(int typeKey) {\n        switch (typeKey) {\n")
    for name in sorted(all_types, key=lambda n: all_types[n]["typeKey"]):
        out.append("            case %d:\n                return \"%s\";\n" % (all_types[name]["typeKey"], name))
    out.append("            default:\n                return null;\n        }\n    }\n\n")
    def is_asset(n):
        while n:
            if n in ("FileAsset", "FileAssetContents"):
                return True
            n = types[n]["parent"] if n in types else None
        return False
    out.append("    /// Whether `typeKey` is a file asset or an asset's contents, which belong\n")
    out.append("    /// to the whole file rather than to an artboard.\n")
    out.append("    public static boolean isAsset(int typeKey) {\n        switch (typeKey) {\n")
    for name in sorted((n for n in concrete if is_asset(n)), key=lambda n: types[n]["typeKey"]):
        out.append("            case %d:\n" % types[name]["typeKey"])
    out.append("                return true;\n            default:\n                return false;\n        }\n    }\n\n")
    out.append("    /// The wire type of `propertyKey`, or FIELD_UNKNOWN.\n")
    out.append("    public static int propertyFieldId(int propertyKey) {\n        switch (propertyKey) {\n")
    by = {}
    # Every type's keys, skipped ones included: their properties still have
    # to be stepped over, and the header's table of contents does not list
    # keys the editor expects every runtime to know.
    for t in all_types.values():
        for p in t["props"]:
            if p["coreType"]:
                fid = FIELD_ID[p["coreType"]]
                if p["coreType"] == "CoreBytes":
                    fid = 5
                by.setdefault(fid, set()).add(p["key"])
    for fid in sorted(by):
        for k in sorted(by[fid]):
            out.append("            case %d:\n" % k)
        out.append("                return %d;\n" % fid)
    out.append("            default:\n                return FIELD_UNKNOWN;\n        }\n    }\n}\n")
    return "".join(out)


def main():
    runtime = sys.argv[1]
    check = "--check" in sys.argv
    types = rive_schema.load(runtime)
    with open(os.path.join(runtime, "include", "rive", "generated", "core_registry.hpp")) as f:
        reg = f.read()
    make = reg[reg.index("static Core* makeCoreInstance"):]
    make = make[: make.index("\n  }\n")]
    concrete = set(re.findall(r"return new (\w+)\(\);", make))
    keep = {n: i for n, i in types.items() if not excluded(n, i)}
    concrete = {n for n in concrete if n in keep}
    COPY_CONCRETE.update(concrete)
    files = {}
    for name, info in keep.items():
        files["%sBase.java" % name] = gen_base(name, info, keep)
        path = os.path.join(OUT, name + ".java")
        if not os.path.exists(path) or PLACEHOLDER_MARK in open(path).read():
            files[name + ".java"] = gen_placeholder(name)
    schema = gen_registry(keep, concrete, types)
    schema_old = open(SCHEMA).read() if os.path.exists(SCHEMA) else None
    if schema_old != schema and not check:
        os.makedirs(os.path.dirname(SCHEMA), exist_ok=True)
        with open(SCHEMA, "w") as f:
            f.write(schema)
    stale = []
    for fname, text in sorted(files.items()):
        path = os.path.join(OUT, fname)
        old = open(path).read() if os.path.exists(path) else None
        if old != text:
            stale.append(fname)
            if not check:
                os.makedirs(OUT, exist_ok=True)
                with open(path, "w") as f:
                    f.write(text)
    if not check:
        rel = sorted("CodenameOne/src/com/codename1/impl/rive/" + f for f in files)
        with open(EXCLUSIONS) as f:
            lines = f.read().splitlines()
        lines = [l for l in lines if not l.startswith("CodenameOne/src/com/codename1/impl/rive/")]
        hand = sorted(f for f in os.listdir(OUT) if f.endswith(".java")
                      and "Copyright (c) 2020 Rive" in open(os.path.join(OUT, f)).read())
        lines += ["CodenameOne/src/com/codename1/impl/rive/%s | %s" % (f, EXCLUSION_RATIONALE) for f in hand]
        tdir = os.path.dirname(SCHEMA)
        rel_t = os.path.relpath(tdir, REPO) + "/"
        lines = [l for l in lines if not l.startswith(rel_t)]
        lines += ["%s%s | %s" % (rel_t, f, EXCLUSION_RATIONALE) for f in sorted(os.listdir(tdir))
                  if f.endswith(".java") and "Copyright (c) 2020 Rive" in open(os.path.join(tdir, f)).read()]
        with open(EXCLUSIONS, "w") as f:
            f.write("\n".join(lines) + "\n")
    if check and stale:
        print("rive schema is stale: " + ", ".join(stale[:10]))
        sys.exit(1)
    print("%d types, %d concrete, %d files %s" % (len(keep), len(concrete), len(stale), "stale" if check else "written"))


if __name__ == "__main__":
    main()
