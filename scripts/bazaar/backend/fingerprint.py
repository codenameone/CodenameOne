#!/usr/bin/env python3
"""Copies the web app's assets from web-src/ into src/main/resources/static/
under names that carry their content hash, and points the templates at them.

    python3 fingerprint.py

A name like app.1a2b3c4d.css never changes its bytes, so the server sends it
with a year of immutable caching (see web/security/ImmutableAssets.java). Edit
the file in web-src/, run this, rebuild. WebAssetsTest fails when a template
names a file that is missing or whose bytes no longer match its name.
"""
import hashlib
import os
import re
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
SRC = os.path.join(HERE, "web-src")
STATIC = os.path.join(HERE, "src", "main", "resources", "static")
TEMPLATES = os.path.join(HERE, "src", "main", "resources", "templates")
# Source file -> published stem and extension.
ASSETS = [("app.css", "app", "css"), ("app.js", "app", "js"),
          ("htmx.js", "htmx", "js"), ("shop.css", "shop", "css"), ("shop.js", "shop", "js")]

# Java sources that name an asset (pages answered without a view).
REFERENCES = ["src/main/java/com/codenameone/examples/bazaar/web/NotFoundPage.java",
              "src/main/java/com/codenameone/examples/bazaar/web/ErrorPages.java"]


def main():
    renames = {}
    for source, stem, ext in ASSETS:
        path = os.path.join(SRC, source)
        if not os.path.exists(path):
            continue
        data = open(path, "rb").read()
        digest = hashlib.sha256(data).hexdigest()[:8]
        name = "%s.%s.%s" % (stem, digest, ext)
        pattern = re.compile(r"^%s\.[0-9a-f]{8}\.%s$" % (re.escape(stem), ext))
        for old in os.listdir(STATIC):
            if pattern.match(old) and old != name:
                os.remove(os.path.join(STATIC, old))
        with open(os.path.join(STATIC, name), "wb") as out:
            out.write(data)
        renames[(stem, ext)] = name
    files = [os.path.join(TEMPLATES, t) for t in os.listdir(TEMPLATES)]
    files += [os.path.join(HERE, p) for p in REFERENCES]
    for path in files:
        if not os.path.exists(path):
            continue
        text = open(path, encoding="utf-8").read()
        new = text
        for (stem, ext), name in renames.items():
            new = re.sub(r"/static/%s\.[0-9a-f]{8}\.%s" % (re.escape(stem), ext),
                         "/static/" + name, new)
        if new != text:
            with open(path, "w", encoding="utf-8") as out:
                out.write(new)
    for key, name in sorted(renames.items()):
        print(name)
    return 0


if __name__ == "__main__":
    sys.exit(main())
