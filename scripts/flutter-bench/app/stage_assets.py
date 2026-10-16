#!/usr/bin/env python3
"""Stages the gallery's asset package into a Codename One resource directory.

    stage_assets.py <flutter_gallery_assets/lib> <output-dir>

Codename One resources are FLAT on every port -- there are no directories
inside the bundle -- while Flutter addresses assets by path. The runtime
(`com.codename1.flutter.FlutterAssets`) bridges that by mangling the Flutter
key into a single flat name, and this script has to produce exactly the same
name or the asset is simply not found at runtime:

    packages/flutter_gallery_assets/places/india.png
        -> cn1f_packages_sflutter__gallery__assets_splaces_sindia.png

The rule is: prefix `cn1f_`, then `_` is an escape always followed by one
character -- `__` for an underscore, `_s` for a `/`. That is prefix-free, so no
two keys share a name. (Doubling underscores and using a single `_` for `/` was
not: `a_/b` and `a/_b` both became `a___b`.)

Hard links where the filesystem allows, so the asset pack costs no extra disk;
a stamp file makes a repeat run a no-op, which matters because this runs on
every build of the benchmark project.
"""

import os
import shutil
import sys

STAMP = ".flutter-assets-staged"
PREFIX = "packages/flutter_gallery_assets/"


def flat_name(key):
    """The flat Codename One resource name for a Flutter asset key."""
    out = []
    for ch in key:
        if ch == "_":
            out.append("__")
        elif ch == "/":
            out.append("_s")
        else:
            out.append(ch)
    return "cn1f_" + "".join(out)


def stage_from_flutter_bundle(bundle, out_dir):
    """Stages exactly the assets the Flutter build itself bundled.

    This is the fair set, and neither of the obvious alternatives is.

    Staging the whole `flutter_gallery_assets` package ships files Flutter
    never bundles, which inflates our installed size against a build that
    tree-shook them -- and it overflowed the desktop packaging limit outright.
    Staging only the 1x images ships FEWER: Flutter bundles the 2x and 3x
    resolution variants too, so dropping them would have flattered us by tens
    of megabytes on exactly the metric the benchmark leads with.

    Reading the built bundle removes the judgement call. Whatever Flutter
    decided to carry is what we carry, so "installed size" compares two
    applications shipping the same artwork.
    """
    if not os.path.isdir(bundle):
        raise SystemExit("flutter_assets not found: %s" % bundle)
    os.makedirs(out_dir, exist_ok=True)
    staged = 0
    for root, _dirs, files in os.walk(bundle):
        rel = os.path.relpath(root, bundle).replace(os.sep, "/")
        if not (rel == "packages" or rel.startswith("packages/")):
            continue
        for name in files:
            key = name if rel == "." else rel + "/" + name
            target = os.path.join(out_dir, flat_name(key))
            if os.path.exists(target):
                staged += 1
                continue
            src = os.path.join(root, name)
            try:
                os.link(src, target)
            except OSError:
                shutil.copyfile(src, target)
            staged += 1
    with open(os.path.join(out_dir, STAMP), "w") as handle:
        handle.write("%d\n" % staged)
    return staged


def stage(source, out_dir):
    if not os.path.isdir(source):
        raise SystemExit("asset package not found: %s" % source)
    os.makedirs(out_dir, exist_ok=True)
    if os.path.exists(os.path.join(out_dir, STAMP)):
        return 0
    staged = 0
    for root, _dirs, files in os.walk(source):
        rel = os.path.relpath(root, source)
        for name in files:
            key = name if rel == "." else rel.replace(os.sep, "/") + "/" + name
            target = os.path.join(out_dir, flat_name(PREFIX + key))
            if os.path.exists(target):
                staged += 1
                continue
            src = os.path.join(root, name)
            try:
                os.link(src, target)
            except OSError:
                shutil.copyfile(src, target)
            staged += 1
    with open(os.path.join(out_dir, STAMP), "w") as handle:
        handle.write("%d\n" % staged)
    return staged


def main(argv):
    args = list(argv[1:])
    from_bundle = False
    if args and args[0] == "--from-flutter-bundle":
        from_bundle = True
        args = args[1:]
    if len(args) != 2:
        raise SystemExit(
            "usage: stage_assets.py [--from-flutter-bundle] <source> <output-dir>")
    if from_bundle:
        staged = stage_from_flutter_bundle(args[0], args[1])
    else:
        staged = stage(args[0], args[1])
    print("staged %d asset(s) into %s" % (staged, args[1]))
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
