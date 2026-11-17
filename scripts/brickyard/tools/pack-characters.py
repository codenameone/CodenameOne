#!/usr/bin/env python3
"""Packs the Brickyard avatar bodies from KayKit Character Pack: Adventurers
(Kay Lousberg, www.kaylousberg.com, CC0 1.0) into
src/main/resources/com/codenameone/examples/brickyard/rbx/models/avatar_<name>.glb.

It reuses scripts/skirmish/tools/pack-models.py (same pinned source commit,
same texture shrinking) with a different clip set: the locomotion a platformer
character needs -- idle, walk, run, the three jump phases, death and a cheer --
and no weapons, since an avatar carries none until a place hands it a tool.

Run once after editing; the output is checked in. The source files are
cached under target/model-cache.
"""
import importlib.util
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.join(HERE, "..", "..", "..")
spec = importlib.util.spec_from_file_location("packmodels", os.path.join(ROOT, "scripts", "skirmish", "tools", "pack-models.py"))
pm = importlib.util.module_from_spec(spec)
spec.loader.exec_module(pm)

pm.OUT = os.path.join(HERE, "..", "common", "src", "main", "resources", "com", "codenameone", "examples", "brickyard", "rbx", "models")
pm.CACHE = os.path.join(HERE, "..", "target", "model-cache")
CLIPS = ["Idle", "Walking_A", "Running_A", "Jump_Start", "Jump_Idle", "Jump_Land", "Death_A", "Cheer"]
pm.MODELS = {
    "knight": ("adventurers", "Knight.glb", CLIPS, ["Knight_Helmet", "Knight_Cape"], []),
    "barbarian": ("adventurers", "Barbarian.glb", CLIPS, ["Barbarian_Hat", "Barbarian_Cape"], []),
    "mage": ("adventurers", "Mage.glb", CLIPS, ["Mage_Hat", "Mage_Cape"], []),
    "rogue": ("adventurers", "Rogue_Hooded.glb", CLIPS, ["Rogue_Cape"], []),
}


def main():
    for n in sys.argv[1:] or list(pm.MODELS):
        pm.pack(n)
    # pack() names files model_<name>.glb; the avatar bodies are avatar_<name>.glb
    for n in sys.argv[1:] or list(pm.MODELS):
        os.replace(os.path.join(pm.OUT, "model_" + n + ".glb"), os.path.join(pm.OUT, "avatar_" + n + ".glb"))


if __name__ == "__main__":
    main()
