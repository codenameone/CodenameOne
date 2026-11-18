#!/usr/bin/env python3
# Reads the generated C++ base headers of rive-runtime (MIT) and returns the
# object schema of the .riv format: every type key with its parent and its
# properties (key, field type, default). Used by gen-rive-schema.py.
import os
import re

FIELD_NAMES = {0: "uint", 1: "string", 2: "double", 3: "color", 4: "bool"}

CLASS_RE = re.compile(r"^class (\w+)Base\s*:\s*public (\w+)(?:,\s*public (\w+))?", re.M)
TYPEKEY_RE = re.compile(r"static const uint16_t typeKey = (\d+);")
PROP_RE = re.compile(r"static const uint16_t (\w+)PropertyKey = (\d+);")
MEMBER_RE = re.compile(r"^  ([A-Za-z_][\w:<> ]*?) m_(\w+)(?: = ([^;]+))?;", re.M)
# A property name that differs from its member only in case of first letter.


def _registry_field_ids(registry_text):
    start = registry_text.index("static int propertyFieldId(int propertyKey)")
    body = registry_text[start:]
    body = body[: body.index("\n  }\n")]
    ids = {}
    pending = []
    for line in body.splitlines():
        m = re.search(r"case (\w+)Base::(\w+)PropertyKey:", line)
        if m:
            pending.append((m.group(1), m.group(2)))
            continue
        m = re.search(r"return (\w+)Type::id;", line)
        if m:
            kind = {"CoreUint": 0, "CoreId": 0, "CoreInt": 0, "CoreString": 1,
                    "CoreBytes": 1, "CoreDouble": 2, "CoreColor": 3,
                    "CoreBool": 4}[m.group(1)]
            for cls, prop in pending:
                ids[(cls, prop)] = (kind, m.group(1))
            pending = []
    return ids


def load(runtime_dir):
    gen = os.path.join(runtime_dir, "include", "rive", "generated")
    with open(os.path.join(gen, "core_registry.hpp")) as f:
        field_ids = _registry_field_ids(f.read())
    types = {}
    for root, _dirs, files in os.walk(gen):
        for name in files:
            if not name.endswith("_base.hpp"):
                continue
            path = os.path.join(root, name)
            with open(path) as f:
                text = f.read()
            cm = CLASS_RE.search(text)
            if not cm:
                continue
            cls = cm.group(1)
            parent = cm.group(2)
            mixin = cm.group(3)
            tk = TYPEKEY_RE.search(text)
            members = {}
            for mm in MEMBER_RE.finditer(text):
                members[mm.group(2)] = (mm.group(1).strip(), (mm.group(3) or "").strip())
            props = []
            for pm in PROP_RE.finditer(text):
                pname = pm.group(1)
                key = int(pm.group(2))
                ctype, default = members.get(pname[0].upper() + pname[1:], ("", ""))
                fid = field_ids.get((cls, pname))
                props.append({
                    "name": pname,
                    "key": key,
                    "field": fid[0] if fid else None,
                    "coreType": fid[1] if fid else None,
                    "ctype": ctype,
                    "default": default,
                })
            types[cls] = {
                "name": cls,
                "typeKey": int(tk.group(1)) if tk else None,
                "parent": parent.replace("Base", "") if parent.endswith("Base") and parent != "ColorChannelsBase" else parent,
                "mixin": mixin,
                "props": props,
                "dir": os.path.relpath(root, gen),
            }
    return types
