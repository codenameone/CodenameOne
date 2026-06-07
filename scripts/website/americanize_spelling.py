#!/usr/bin/env python3
"""Convert British spellings to American in the blog's author prose.

The blog is American English (Shai Almog and the other contributors); only
Steve Hannah's posts may use British/Canadian spelling, so they are skipped.
Body-only: front matter, fenced/inline code, Hugo shortcodes, link/image
targets, and the imported "## Archived Comments" section are never touched.

Deterministic and case-preserving (Behaviour->Behavior, BEHAVIOUR->BEHAVIOR).
Defaults to DRY RUN; pass --write to apply. Every change is listed so the diff
can be eyeballed for proper-noun false positives (e.g. a person named "Grey").
"""

import argparse
import os
import re
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import blog_text  # noqa: E402

SKIP_AUTHORS = {"steve hannah"}

# Irregular British -> American words (with the inflections that actually occur).
IRREGULAR = {
    "behaviour": "behavior", "behaviours": "behaviors",
    "colour": "color", "colours": "colors", "coloured": "colored",
    "colouring": "coloring", "colourful": "colorful",
    "favour": "favor", "favours": "favors", "favoured": "favored",
    "favourite": "favorite", "favourites": "favorites",
    "flavour": "flavor", "flavours": "flavors", "flavoured": "flavored",
    "honour": "honor", "honours": "honors", "honoured": "honored",
    "neighbour": "neighbor", "neighbours": "neighbors",
    "labour": "labor", "humour": "humor", "rumour": "rumor",
    "harbour": "harbor", "endeavour": "endeavor", "vapour": "vapor",
    "centre": "center", "centres": "centers", "centred": "centered",
    "centring": "centering",
    "metre": "meter", "metres": "meters",
    "litre": "liter", "litres": "liters",
    "theatre": "theater", "fibre": "fiber", "calibre": "caliber",
    "defence": "defense", "offence": "offense",
    "artefact": "artifact", "artefacts": "artifacts",
    "grey": "gray", "greyed": "grayed",
    "programme": "program", "programmes": "programs",
    "catalogue": "catalog", "catalogues": "catalogs",
    "analogue": "analog", "mould": "mold", "plough": "plow",
    "cancelled": "canceled", "cancelling": "canceling",
    "cancellation": "cancellation",  # same in both; listed for clarity (no-op removed below)
    "travelled": "traveled", "travelling": "traveling",
    "modelling": "modeling", "labelled": "labeled", "labelling": "labeling",
    "signalling": "signaling", "fuelled": "fueled",
    # Prefixed forms not reachable by a bare stem.
    "unrecognised": "unrecognized", "unrecognise": "unrecognize",
    "reinitialised": "reinitialized", "reinitialising": "reinitializing",
}
IRREGULAR = {k: v for k, v in IRREGULAR.items() if k != v}

# British-only -ise/-isation stems (NOT advise/surprise/exercise/comprise/etc.).
IZE_STEMS = [
    "real", "organ", "optim", "recogn", "custom", "initial", "serial",
    "deserial", "normal", "special", "summar", "apolog", "emphas", "priorit",
    "categor", "minim", "maxim", "util", "capital", "standard", "raster",
    "modern", "central", "decentral", "visual", "memor", "synthes", "symbol",
    "stabil", "author", "personal", "parameter", "vector", "local", "context",
    "token", "container", "virtual", "monet",
]
YSE_STEMS = ["anal", "paral", "catal"]  # analyse->analyze, etc.


def _build_mapping():
    m = dict(IRREGULAR)
    for stem in IZE_STEMS:
        m[stem + "ise"] = stem + "ize"
        m[stem + "ised"] = stem + "ized"
        m[stem + "ises"] = stem + "izes"
        m[stem + "ising"] = stem + "izing"
        m[stem + "isation"] = stem + "ization"
        m[stem + "isations"] = stem + "izations"
    for stem in YSE_STEMS:
        m[stem + "yse"] = stem + "yze"
        m[stem + "ysed"] = stem + "yzed"
        m[stem + "yses"] = stem + "yzes"
        m[stem + "ysing"] = stem + "yzing"
    return m


MAPPING = _build_mapping()
_WORD_RE = re.compile(r"[A-Za-z]+")
# Protected inline segments to skip on a prose line.
_PROTECTED_RE = re.compile(r"(`[^`]*`)|(\{\{[<%].*?[%>]\}\})|(\]\([^)]*\))")
_FENCE_RE = re.compile(r"^\s*(?:```|~~~)")


def _apply_case(british, american):
    if british.isupper():
        return american.upper()
    if british[0].isupper():
        return american[0].upper() + american[1:]
    return american


def _convert_text(text, counts):
    def repl(mobj):
        w = mobj.group(0)
        american = MAPPING.get(w.lower())
        if not american:
            return w
        out = _apply_case(w, american)
        if out != w:
            counts[w.lower() + " -> " + american] += 1
        return out
    return _WORD_RE.sub(repl, text)


def _convert_line(line, counts):
    pieces = []
    pos = 0
    for mobj in _PROTECTED_RE.finditer(line):
        if mobj.start() > pos:
            pieces.append(_convert_text(line[pos:mobj.start()], counts))
        pieces.append(mobj.group(0))
        pos = mobj.end()
    if pos < len(line):
        pieces.append(_convert_text(line[pos:], counts))
    return "".join(pieces)


def convert_body(author_body_text, counts):
    out = []
    in_fence = False
    for line in author_body_text.split("\n"):
        if _FENCE_RE.match(line):
            in_fence = not in_fence
            out.append(line)
            continue
        if in_fence or re.match(r"^ {4,}\S", line):
            out.append(line)
            continue
        out.append(_convert_line(line, counts))
    return "\n".join(out)


def post_author(front_matter):
    m = re.search(r"^author:\s*[\"']?([^\"'\n]+)", front_matter, re.M)
    return (m.group(1).strip() if m else "")


def process_file(path, write, counts, skipped):
    with open(path, "r", encoding="utf-8") as fh:
        original = fh.read()
    fm, body, _ = blog_text.split_front_matter(original)
    if post_author(fm).lower() in SKIP_AUTHORS:
        skipped.append(os.path.basename(path))
        return False
    cut = len(blog_text.author_body(body))
    author_part, rest = body[:cut], body[cut:]
    new_author = convert_body(author_part, counts)
    if new_author == author_part:
        return False
    new_full = fm + new_author + rest
    assert new_full.startswith(fm), f"front matter changed in {path}"
    if write:
        with open(path, "w", encoding="utf-8") as fh:
            fh.write(new_full)
    return True


def expand(paths):
    files = []
    for p in paths:
        if os.path.isdir(p):
            for root, _d, names in os.walk(p):
                files.extend(os.path.join(root, n) for n in sorted(names)
                             if n.endswith(".md") and n != "_index.md")
        else:
            files.append(p)
    return files


def main():
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("paths", nargs="+")
    ap.add_argument("--write", action="store_true", help="Apply (default: dry run)")
    args = ap.parse_args()
    import collections
    counts = collections.Counter()
    changed, skipped = [], []
    for path in expand(args.paths):
        if process_file(path, args.write, counts, skipped):
            changed.append(os.path.basename(path))
    mode = "WROTE" if args.write else "DRY RUN — would change"
    print(f"{mode} {len(changed)} post(s); skipped {len(skipped)} Steve-Hannah post(s).")
    print(f"Total substitutions: {sum(counts.values())}")
    for k, n in counts.most_common():
        print(f"  {n:4d}  {k}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
