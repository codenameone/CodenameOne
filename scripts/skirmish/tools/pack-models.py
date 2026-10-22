#!/usr/bin/env python3
# Copyright (c) 2026, Codename One and/or its affiliates.
#
# Permission is hereby granted, free of charge, to any person obtaining a copy
# of this software and associated documentation files (the "Software"), to
# deal in the Software without restriction, including without limitation the
# rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
# sell copies of the Software, and to permit persons to whom the Software is
# furnished to do so.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
# IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
# FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
# AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
# LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
# FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
# IN THE SOFTWARE.
"""Fetches the KayKit models Skirmish uses and packs each into one small .glb.

The KayKit packs (CC0, by Kay Lousberg, www.kaylousberg.com) ship every
character with ~75 animation clips and a full set of props, and the building
packs as .gltf files with external .bin and texture side files. The game needs
a handful of clips and one self-contained file per model, so this script:

- downloads each source at a pinned commit (so a re-run is reproducible),
- keeps only the animation clips listed below,
- drops the meshes of props no unit carries,
- inlines external buffers and images, shrinking textures to MAX_TEXTURE,
- and rewrites the binary chunk with only the bytes still referenced.

Output goes to common/src/main/resources/model_<name>.glb -- Codename One
resources sit at the root, because iOS flattens them. Re-run it after changing the
tables below; the results are committed so a build never needs the network.

    tools/pack-models.py            # every model
    tools/pack-models.py knight     # just these
"""
import io
import json
import os
import struct
import sys
import urllib.request

from PIL import Image

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "common", "src", "main", "resources")
CACHE = os.environ.get("SKIRMISH_MODEL_CACHE", os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "target", "model-cache"))
MAX_TEXTURE = 512

REPOS = {
    "adventurers": ("KayKit-Character-Pack-Adventures-1.0", "672074b73ba276876a19e8816ecdc5241817ab47",
                    "addons/kaykit_character_pack_adventures/Characters/gltf/"),
    "skeletons": ("KayKit-Character-Pack-Skeletons-1.0", "15b62b9bad122f72926c10fb14d622c73819fa54",
                  "addons/kaykit_character_pack_skeletons/Characters/gltf/"),
    "skeleton_props": ("KayKit-Character-Pack-Skeletons-1.0", "15b62b9bad122f72926c10fb14d622c73819fa54",
                       "addons/kaykit_character_pack_skeletons/Assets/gltf/"),
    "hexagon": ("KayKit-Medieval-Hexagon-Pack-1.0", "84fa4e91af6a88989be7c99e0891cede11f2ca38",
                "addons/kaykit_medieval_hexagon_pack/Assets/gltf/"),
    "halloween": ("KayKit-Halloween-Bits-1.0", "6dc69bf6b2fa766a985754f35ec6a0324090e6c6",
                  "addons/kaykit_halloween_bits/Assets/gltf/"),
}

BASE_CLIPS = ["Idle", "Running_A", "Death_A", "Hit_A", "Cheer"]

# name -> (repo, path under the repo's model dir, clips to keep,
#          rigid props to keep (None keeps all), [(prop repo, prop file, parent node)])
MODELS = {
    "knight": ("adventurers", "Knight.glb", BASE_CLIPS + ["1H_Melee_Attack_Chop"],
               ["1H_Sword", "Round_Shield", "Knight_Helmet", "Knight_Cape"], []),
    "barbarian": ("adventurers", "Barbarian.glb", BASE_CLIPS + ["2H_Melee_Attack_Chop"],
                  ["2H_Axe", "Barbarian_Hat", "Barbarian_Cape"], []),
    "mage": ("adventurers", "Mage.glb", BASE_CLIPS + ["Spellcast_Shoot"],
             ["2H_Staff", "Mage_Hat", "Mage_Cape"], []),
    "rogue": ("adventurers", "Rogue.glb", BASE_CLIPS + ["2H_Ranged_Shoot"],
              ["2H_Crossbow", "Rogue_Cape"], []),
    "rogue_hooded": ("adventurers", "Rogue_Hooded.glb", BASE_CLIPS + ["1H_Ranged_Shoot"],
                     ["1H_Crossbow", "Rogue_Cape"], []),
    "skeleton_warrior": ("skeletons", "Skeleton_Warrior.glb", BASE_CLIPS + ["1H_Melee_Attack_Chop"], None,
                         [("skeleton_props", "Skeleton_Blade.gltf", "handslot.r"),
                          ("skeleton_props", "Skeleton_Shield_Small_A.gltf", "handslot.l")]),
    "skeleton_minion": ("skeletons", "Skeleton_Minion.glb",
                        BASE_CLIPS + ["Unarmed_Melee_Attack_Punch_A", "Skeletons_Awaken_Floor"], None, []),
    "skeleton_rogue": ("skeletons", "Skeleton_Rogue.glb", BASE_CLIPS + ["1H_Melee_Attack_Chop"], None,
                       [("skeleton_props", "Skeleton_Blade.gltf", "handslot.r")]),
    "tower_blue": ("hexagon", "buildings/blue/building_tower_A_blue.gltf", [], None, []),
    "tower_red": ("hexagon", "buildings/red/building_tower_A_red.gltf", [], None, []),
    "castle_blue": ("hexagon", "buildings/blue/building_castle_blue.gltf", [], None, []),
    "castle_red": ("hexagon", "buildings/red/building_castle_red.gltf", [], None, []),
    "ruin": ("hexagon", "buildings/neutral/building_destroyed.gltf", [], None, []),
    "tree": ("hexagon", "decoration/nature/tree_single_A.gltf", [], None, []),
    "trees": ("hexagon", "decoration/nature/trees_A_medium.gltf", [], None, []),
    "rock": ("hexagon", "decoration/nature/rock_single_A.gltf", [], None, []),
    "hill": ("hexagon", "decoration/nature/hills_A_trees.gltf", [], None, []),
    "crypt": ("halloween", "crypt.gltf", [], None, []),
}


def fetch(repo_key, rel):
    repo, sha, base = REPOS[repo_key]
    path = os.path.join(CACHE, repo, rel)
    if not os.path.exists(path):
        os.makedirs(os.path.dirname(path), exist_ok=True)
        url = "https://raw.githubusercontent.com/KayKit-Game-Assets/%s/%s/%s%s" % (repo, sha, base, rel)
        with urllib.request.urlopen(url) as r:
            data = r.read()
        with open(path, "wb") as f:
            f.write(data)
    with open(path, "rb") as f:
        return f.read()


def read_source(repo_key, rel):
    data = fetch(repo_key, rel)
    if data[:4] == b"glTF":
        json_len = struct.unpack_from("<I", data, 12)[0]
        doc = json.loads(data[20:20 + json_len])
        p = 20 + json_len
        bin_chunk = b""
        if p + 8 <= len(data):
            n = struct.unpack_from("<I", data, p)[0]
            bin_chunk = data[p + 8:p + 8 + n]
        buffers = [bin_chunk]
    else:
        doc = json.loads(data)
        folder = os.path.dirname(rel)
        buffers = [fetch(repo_key, os.path.join(folder, b["uri"]).replace("\\", "/")) for b in doc["buffers"]]
        for img in doc.get("images", []):
            if "uri" in img:
                img["_bytes"] = fetch(repo_key, os.path.join(folder, img["uri"]).replace("\\", "/"))
    return doc, buffers


def attach(doc, buffers, prop, parent_name):
    """Merges the first mesh node of `prop` into `doc` as a child of the node
    named `parent_name` (a hand slot), keeping its own material and image."""
    pdoc, pbuffers = prop
    base_buffer = len(buffers)
    buffers.extend(pbuffers)
    off = {k: len(doc.setdefault(k, [])) for k in
           ("bufferViews", "accessors", "meshes", "materials", "textures", "images", "samplers", "nodes")}
    for v in pdoc["bufferViews"]:
        v = dict(v)
        v["buffer"] += base_buffer
        doc["bufferViews"].append(v)
    for a in pdoc["accessors"]:
        a = dict(a)
        if "bufferView" in a:
            a["bufferView"] += off["bufferViews"]
        doc["accessors"].append(a)
    for img in pdoc.get("images", []):
        img = dict(img)
        if "bufferView" in img:
            img["bufferView"] += off["bufferViews"]
        doc["images"].append(img)
    for t in pdoc.get("textures", []):
        t = dict(t)
        if "source" in t:
            t["source"] += off["images"]
        if "sampler" in t:
            t["sampler"] += off["samplers"]
        doc["textures"].append(t)
    doc["samplers"].extend(pdoc.get("samplers", []))
    for m in pdoc.get("materials", []):
        m = json.loads(json.dumps(m))
        tex = m.get("pbrMetallicRoughness", {}).get("baseColorTexture")
        if tex is not None:
            tex["index"] += off["textures"]
        doc["materials"].append(m)
    for m in pdoc["meshes"]:
        m = json.loads(json.dumps(m))
        for p in m["primitives"]:
            p["attributes"] = {k: v + off["accessors"] for k, v in p["attributes"].items()}
            if "indices" in p:
                p["indices"] += off["accessors"]
            if "material" in p:
                p["material"] += off["materials"]
        doc["meshes"].append(m)
    src = next(n for n in pdoc["nodes"] if "mesh" in n)
    node = {k: v for k, v in src.items() if k in ("name", "rotation", "scale")}
    node["mesh"] = src["mesh"] + off["meshes"]
    doc["nodes"].append(node)
    parent = next(n for n in doc["nodes"] if n.get("name") == parent_name)
    parent.setdefault("children", []).append(len(doc["nodes"]) - 1)


def view_bytes(doc, buffers, vi):
    v = doc["bufferViews"][vi]
    off = v.get("byteOffset", 0)
    return buffers[v["buffer"]][off:off + v["byteLength"]]


SIZES = {5120: 1, 5121: 1, 5122: 2, 5123: 2, 5125: 4, 5126: 4}
WIDTH = {"SCALAR": 1, "VEC2": 2, "VEC3": 3, "VEC4": 4, "MAT4": 16}


def accessor_bytes(doc, buffers, ai):
    a = doc["accessors"][ai]
    n = a["count"] * SIZES[a["componentType"]] * WIDTH[a["type"]]
    data = view_bytes(doc, buffers, a["bufferView"])
    off = a.get("byteOffset", 0)
    return data[off:off + n]


def floats(doc, buffers, ai):
    a = doc["accessors"][ai]
    raw = accessor_bytes(doc, buffers, ai)
    return list(struct.unpack("<%df" % (len(raw) // 4), raw)), WIDTH[a["type"]]


def prune_channels(doc, buffers):
    """Drops channels that hold a node at its rest value for the whole clip,
    which is most of them: a KayKit clip animates every bone of the rig."""
    nodes = doc.get("nodes", [])
    rest_of = {"translation": [0, 0, 0], "rotation": [0, 0, 0, 1], "scale": [1, 1, 1]}
    for anim in doc.get("animations", []):
        kept = []
        for ch in anim["channels"]:
            tgt = ch["target"]
            path = tgt.get("path")
            sampler = anim["samplers"][ch["sampler"]]
            if path not in rest_of or doc["accessors"][sampler["output"]]["componentType"] != 5126:
                kept.append(ch)
                continue
            vals, w = floats(doc, buffers, sampler["output"])
            first = vals[:w]
            constant = all(abs(vals[i] - first[i % w]) < 1e-5 for i in range(len(vals)))
            rest = nodes[tgt["node"]].get(path, rest_of[path])
            if constant and all(abs(first[i] - rest[i]) < 1e-5 for i in range(w)):
                continue
            kept.append(ch)
        used = sorted({ch["sampler"] for ch in kept})
        remap = {old: i for i, old in enumerate(used)}
        anim["samplers"] = [anim["samplers"][i] for i in used]
        for ch in kept:
            ch["sampler"] = remap[ch["sampler"]]
        anim["channels"] = kept


def shrink(png):
    img = Image.open(io.BytesIO(png))
    if max(img.size) > MAX_TEXTURE:
        scale = MAX_TEXTURE / max(img.size)
        img = img.resize((max(1, int(img.size[0] * scale)), max(1, int(img.size[1] * scale))), Image.LANCZOS)
    out = io.BytesIO()
    img.save(out, "PNG", optimize=True)
    return out.getvalue()


def pack(name):
    repo_key, rel, clips, keep, attachments = MODELS[name]
    doc, buffers = read_source(repo_key, rel)
    for prop_repo, prop_rel, parent in attachments:
        attach(doc, buffers, read_source(prop_repo, prop_rel), parent)

    doc["animations"] = [a for a in doc.get("animations", []) if a.get("name") in clips]
    if not doc["animations"]:
        doc.pop("animations")
    for node in doc.get("nodes", []):
        if keep is not None and "mesh" in node and "skin" not in node and node.get("name") not in keep:
            node.pop("mesh", None)

    prune_channels(doc, buffers)

    used_meshes = sorted({n["mesh"] for n in doc.get("nodes", []) if "mesh" in n})
    mesh_map = {old: i for i, old in enumerate(used_meshes)}
    doc["meshes"] = [doc["meshes"][i] for i in used_meshes]
    for n in doc.get("nodes", []):
        if "mesh" in n:
            n["mesh"] = mesh_map[n["mesh"]]

    used_accessors = []
    for m in doc["meshes"]:
        for p in m["primitives"]:
            p.pop("targets", None)
            used_accessors.extend(p["attributes"].values())
            if "indices" in p:
                used_accessors.append(p["indices"])
    for s in doc.get("skins", []):
        if "inverseBindMatrices" in s:
            used_accessors.append(s["inverseBindMatrices"])
    for a in doc.get("animations", []):
        for s in a["samplers"]:
            used_accessors += [s["input"], s["output"]]
    used_accessors = sorted(set(used_accessors))
    acc_map = {old: i for i, old in enumerate(used_accessors)}

    out_bin = bytearray()
    out_views = []
    view_map = {}

    def add_view(data, extra):
        while len(out_bin) % 4:
            out_bin.append(0)
        v = {"buffer": 0, "byteOffset": len(out_bin), "byteLength": len(data)}
        v.update(extra)
        out_bin.extend(data)
        out_views.append(v)
        return len(out_views) - 1

    shared = bytearray()
    shared_users = []
    accessors = []
    for old in used_accessors:
        a = dict(doc["accessors"][old])
        vi = a.get("bufferView")
        if vi is not None and "byteStride" not in doc["bufferViews"][vi]:
            # tightly packed data (keys, matrices, indices): one shared view
            raw = accessor_bytes(doc, buffers, old)
            while len(shared) % 4:
                shared.append(0)
            a["byteOffset"] = len(shared)
            shared.extend(raw)
            shared_users.append(a)
            if a.get("componentType") != 5126 or a.get("type") != "VEC3":
                a.pop("min", None)
                a.pop("max", None)
            a.pop("name", None)
        elif vi is not None:
            if vi not in view_map:
                src = doc["bufferViews"][vi]
                extra = {k: src[k] for k in ("byteStride", "target") if k in src}
                view_map[vi] = add_view(view_bytes(doc, buffers, vi), extra)
            a["bufferView"] = view_map[vi]
        accessors.append(a)

    if shared:
        sv = add_view(bytes(shared), {})
        for a in shared_users:
            a["bufferView"] = sv

    for img in doc.get("images", []):
        if "_bytes" in img:
            png = img.pop("_bytes")
            img.pop("uri", None)
        elif "bufferView" in img:
            png = view_bytes(doc, buffers, img["bufferView"])
        else:
            continue
        img["bufferView"] = add_view(shrink(png), {})
        img["mimeType"] = "image/png"

    def remap(prim):
        prim["attributes"] = {k: acc_map[v] for k, v in prim["attributes"].items()}
        if "indices" in prim:
            prim["indices"] = acc_map[prim["indices"]]

    for m in doc["meshes"]:
        for p in m["primitives"]:
            remap(p)
    for s in doc.get("skins", []):
        if "inverseBindMatrices" in s:
            s["inverseBindMatrices"] = acc_map[s["inverseBindMatrices"]]
    for a in doc.get("animations", []):
        for s in a["samplers"]:
            s["input"] = acc_map[s["input"]]
            s["output"] = acc_map[s["output"]]

    doc["accessors"] = accessors
    doc["bufferViews"] = out_views
    doc["buffers"] = [{"byteLength": len(out_bin)}]
    for k in ("extras", "extensionsUsed", "extensionsRequired"):
        doc.pop(k, None)
    doc["asset"] = {"version": "2.0", "generator": "skirmish pack-models.py",
                    "copyright": "KayKit by Kay Lousberg (www.kaylousberg.com), CC0 1.0"}

    js = json.dumps(doc, separators=(",", ":")).encode()
    js += b" " * ((4 - len(js) % 4) % 4)
    while len(out_bin) % 4:
        out_bin.append(0)
    total = 12 + 8 + len(js) + 8 + len(out_bin)
    glb = struct.pack("<III", 0x46546C67, 2, total) + struct.pack("<II", len(js), 0x4E4F534A) + js \
        + struct.pack("<II", len(out_bin), 0x004E4942) + bytes(out_bin)
    os.makedirs(OUT, exist_ok=True)
    with open(os.path.join(OUT, "model_" + name + ".glb"), "wb") as f:
        f.write(glb)
    clip_names = [a["name"] for a in doc.get("animations", [])]
    print("%-18s %7d bytes  clips: %s" % (name, len(glb), ", ".join(clip_names)))


def main():
    names = sys.argv[1:] or list(MODELS)
    for n in names:
        pack(n)


if __name__ == "__main__":
    main()
