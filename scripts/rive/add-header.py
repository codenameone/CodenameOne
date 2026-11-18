#!/usr/bin/env python3
# Replaces a first line of "//@RIVE-HEADER" in each named Java file with the
# rive-runtime MIT notice the generator writes (hand-written Rive runtime files
# are a Java rewrite of rive-runtime and keep its notice).
import re, sys, os
here = os.path.dirname(os.path.abspath(__file__))
src = open(os.path.join(here, "gen-rive-schema.py")).read()
header = src[src.index('HEADER = """') + len('HEADER = """'): src.index('"""\n\nJAVA_TYPE')]
for path in sys.argv[1:]:
    text = open(path).read()
    if text.startswith("//@RIVE-HEADER\n"):
        open(path, "w").write(header + text[len("//@RIVE-HEADER\n"):])
