# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""llm doing what the mobile application does, offline: a conversation
with the `echo` model (the llm-echo plugin, found through pluggy), every
reply logged to a SQLite database the way the command line logs it, a
full-text search of that log, and similarity search over an embedding
collection. The same program runs under CPython and transpiled.

Prints IMPORT (importing llm and loading its plugins, once), RESULT (a
digest of everything produced, identical on both sides), BENCH (the
fastest round), COLD (the first) and TOTAL."""
import hashlib
import os
import shutil
import sys
import tempfile
import time

t0 = time.perf_counter()
import llm  # noqa: E402
import sqlite_utils  # noqa: E402
from llm.logs import LogStore, log_rows  # noqa: E402
llm.get_model("echo")
IMPORT_US = int((time.perf_counter() - t0) * 1000000)

WORDS = ("alpha bravo charlie delta echo foxtrot golf hotel india juliet kilo lima mike "
         "november oscar papa quebec romeo sierra tango uniform victor whiskey xray").split()


class WordLengths(llm.EmbeddingModel):
    """A deterministic stand-in for a real embedding model: the lengths of
    the first sixteen words, as llm's own test suite uses."""
    model_id = "word-lengths"
    batch_size = 32

    def embed_batch(self, texts):
        for text in texts:
            v = [float(len(w)) for w in text.split()[:16]]
            yield v + [0.0] * (16 - len(v))


PH = {}


def ph(name, t):
    now = time.perf_counter()
    PH[name] = PH.get(name, 0.0) + now - t
    return now


def work(root, turns):
    t = time.perf_counter()
    db = sqlite_utils.Database(os.path.join(root, "logs.db"))
    store = LogStore(db)
    t = ph("open+migrate", t)
    digest = hashlib.sha256()
    conversation = llm.get_model("echo").conversation()
    for i in range(turns):
        prompt = " ".join(WORDS[(i + k) % len(WORDS)] for k in range(1 + i % 7))
        response = conversation.prompt(prompt)
        digest.update(response.text().encode("utf-8"))
        t = ph("prompt", t)
        response.log_to_db(db)
        t = ph("log_to_db", t)
    rows = log_rows(store, count=10, query="bravo")
    digest.update(str(len(rows)).encode("ascii"))
    t = ph("log_rows", t)
    collection = llm.Collection("bench", db, model=WordLengths())
    collection.embed_multi(((str(i), " ".join(WORDS[i:i + 1 + i % 9])) for i in range(len(WORDS))), store=True)
    t = ph("embed_multi", t)
    for entry in collection.similar("hotel india juliet", number=5):
        digest.update(("%s %.6f" % (entry.id, entry.score)).encode("ascii"))
    t = ph("similar", t)
    db.close()
    return digest.hexdigest()[:16]


def main():
    rounds = int(sys.argv[1]) if len(sys.argv) > 1 else 5
    turns = int(sys.argv[2]) if len(sys.argv) > 2 else 40
    best = first = None
    total = 0.0
    result = None
    for _ in range(rounds):
        root = tempfile.mkdtemp(prefix="llm-bench-")
        t = time.perf_counter()
        r = work(root, turns)
        dt = time.perf_counter() - t
        shutil.rmtree(root, ignore_errors=True)
        if result is not None and r != result:
            raise RuntimeError("rounds disagree: %r != %r" % (r, result))
        result = r
        total += dt
        first = dt if first is None else first
        best = dt if best is None or dt < best else best
    for k in PH:
        print("PHASE %s %d" % (k, int(PH[k] * 1000000 / rounds)))
    print("IMPORT llm", IMPORT_US)
    print("RESULT llm", result)
    print("BENCH llm", int(best * 1000000))
    print("COLD llm", int(first * 1000000))
    print("TOTAL llm", int(total * 1000000))


main()
