#!/usr/bin/env python3
"""Mines every build hint the Maven plugin and the builders read.

Three accessor shapes reach a hint; a getArg() grep alone misses the whole
@Desktop group, which is read only by a private arg() helper in
GenerateDesktopAppWrapperMojo.

Arguments are split with a paren/quote-balanced scan rather than a regex,
because calls nest: getArg("ios.urlSchemes", getArg("ios.urlScheme", "")).
A regex that stops at the first comma both mis-reads the outer default and
consumes the inner call, silently dropping a hint from the catalog.
"""
import re, os, sys, json, collections

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, "maven/codenameone-maven-plugin/src/main/java/com/codename1")

OPENERS = [
    (re.compile(r'\bgetArg\(\s*"'), False),
    (re.compile(r'(?<![A-Za-z0-9_.])arg\(\s*"'), False),
    (re.compile(r'\bbooleanArg\(\s*"'), False),
    (re.compile(r'\bgetProperty\(\s*"codename1\.arg\.'), True),
]

def read_literal(text, i):
    """Read a Java string literal body starting just after the opening quote."""
    out = []
    while i < len(text):
        c = text[i]
        if c == '\\':
            out.append(text[i:i+2]); i += 2; continue
        if c == '"':
            return "".join(out), i + 1
        out.append(c); i += 1
    return None, i

def split_args(text, i):
    """Split the argument list; i points just after the opening paren."""
    args, depth, cur = [], 0, []
    while i < len(text):
        c = text[i]
        if c == '"':
            lit, j = read_literal(text, i + 1)
            cur.append('"' + (lit or "") + '"'); i = j; continue
        if c == "'":
            j = i + 1
            while j < len(text) and text[j] != "'":
                j += 2 if text[j] == '\\' else 1
            cur.append(text[i:j+1]); i = j + 1; continue
        if c in "([":
            depth += 1
        elif c in ")]":
            if depth == 0:
                args.append("".join(cur).strip()); return args
            depth -= 1
        elif c == ',' and depth == 0:
            args.append("".join(cur).strip()); cur = []; i += 1; continue
        cur.append(c); i += 1
    return args

LITERAL_DEFAULT = re.compile(r'null|true|false|-?\d+|"(?:[^"\\]|\\.)*"')

hits = collections.defaultdict(list)

for dirpath, _, files in os.walk(SRC):
    for fn in sorted(files):
        if not fn.endswith(".java"):
            continue
        path = os.path.join(dirpath, fn)
        text = open(path, encoding="utf-8", errors="replace").read()
        rel = os.path.relpath(path, ROOT)
        for pat, prefixed in OPENERS:
            for m in pat.finditer(text):
                # position of the char just after the opening quote of arg 1
                key, after = read_literal(text, m.end())
                if key is None:
                    continue
                if prefixed:
                    if not key:
                        continue
                else:
                    if not re.fullmatch(r'[A-Za-z][A-Za-z0-9_.!]*', key):
                        continue
                # find the call's opening paren to split the whole arg list
                open_paren = text.rindex('(', m.start(), m.end())
                args = split_args(text, open_paren + 1)
                default = args[1].strip() if len(args) > 1 else ""
                if not LITERAL_DEFAULT.fullmatch(default or "null"):
                    default = "<expr>"
                line = text.count("\n", 0, m.start()) + 1
                hits[key].append((default or "null", rel, line))

if __name__ == "__main__":
    print(f"distinct keys mined: {len(hits)}", file=sys.stderr)
    out = sys.argv[1] if len(sys.argv) > 1 else "-"
    payload = {k: v for k, v in sorted(hits.items())}
    if out == "-":
        json.dump(payload, sys.stdout, indent=1)
    else:
        json.dump(payload, open(out, "w"), indent=1)
