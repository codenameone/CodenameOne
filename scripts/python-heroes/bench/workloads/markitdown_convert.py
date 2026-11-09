# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""MarkItDown converting one document of each kind, the same files under
CPython and transpiled. The fixtures are MarkItDown's own test files, read
from the directory named by MARKITDOWN_FIXTURES.

Prints the lines run_heroes.py reads: IMPORT (importing markitdown, once),
RESULT (a digest of every Markdown produced, which must be identical on
both sides), BENCH (the fastest round), COLD (the first) and TOTAL. With
MARKITDOWN_DUMP set, each document's Markdown is also written to that
directory, so a digest that differs can be traced to the file."""
import hashlib
import os
import sys
import time

t0 = time.perf_counter()
from markitdown import MarkItDown  # noqa: E402
IMPORT_US = int((time.perf_counter() - t0) * 1000000)

FILES = ["test.pdf", "test.docx", "test.pptx", "test.xlsx", "test.epub", "test_wikipedia.html",
         "test_blog.html", "test_rss.xml", "test_notebook.ipynb", "test.json", "test_files.zip"]


PH = {}


def work(fixtures, dump=None):
    md = MarkItDown(enable_plugins=False)
    digest = hashlib.sha256()
    for name in FILES:
        t = time.perf_counter()
        # The ZIP converter names the archive's path, which is not the same
        # string on the two sides.
        text = md.convert(os.path.join(fixtures, name)).markdown.replace(fixtures, "<fixtures>")
        if dump:
            with open(os.path.join(dump, name + ".md"), "w", encoding="utf-8") as f:
                f.write(text)
        digest.update(name.encode("utf-8"))
        digest.update(text.encode("utf-8"))
        PH[name] = PH.get(name, 0.0) + time.perf_counter() - t
    return digest.hexdigest()[:16]


def main():
    fixtures = os.environ["MARKITDOWN_FIXTURES"]
    rounds = int(sys.argv[1]) if len(sys.argv) > 1 else 5
    best = first = None
    total = 0.0
    result = None
    for _ in range(rounds):
        t = time.perf_counter()
        r = work(fixtures, os.environ.get("MARKITDOWN_DUMP"))
        dt = time.perf_counter() - t
        if result is not None and r != result:
            raise RuntimeError("rounds disagree: %r != %r" % (r, result))
        result = r
        total += dt
        first = dt if first is None else first
        best = dt if best is None or dt < best else best
    for k in PH:
        print("PHASE %s %d" % (k, int(PH[k] * 1000000 / rounds)))
    print("IMPORT markitdown", IMPORT_US)
    print("RESULT markitdown", result)
    print("BENCH markitdown", int(best * 1000000))
    print("COLD markitdown", int(first * 1000000))
    print("TOTAL markitdown", int(total * 1000000))


main()
