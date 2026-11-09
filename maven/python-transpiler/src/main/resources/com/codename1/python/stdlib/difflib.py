# Copyright (c) 2001 Python Software Foundation; All Rights Reserved
# Licensed under the Python Software Foundation License Version 2; the full
# text is in maven/python-transpiler/third-party/cpython/LICENSE.txt.
#
# Derived from CPython's Lib/difflib.py. Changes: rewritten for the Codename One
# Python runtime, trimmed to what the transpiler supports and reorganised;
# the C accelerators and platform-specific branches are left out.
"""difflib: sequence matching, close matches and unified/context diffs.

SequenceMatcher finds the longest matching block and recurses on both sides
of it (Ratcliff/Obershelp), with CPython's junk and popularity heuristics,
so ratio() and get_opcodes() give the numbers CPython gives.
"""

from collections import namedtuple as _namedtuple

Match = _namedtuple("Match", "a b size")


class SequenceMatcher:
    def __init__(self, isjunk=None, a="", b="", autojunk=True):
        self.isjunk = isjunk
        self.autojunk = autojunk
        self.a = self.b = None
        self.set_seqs(a, b)

    def set_seqs(self, a, b):
        self.set_seq1(a)
        self.set_seq2(b)

    def set_seq1(self, a):
        if a is self.a:
            return
        self.a = a
        self.matching_blocks = self.opcodes = None

    def set_seq2(self, b):
        if b is self.b:
            return
        self.b = b
        self.matching_blocks = self.opcodes = None
        self.fullbcount = None
        self._chain_b()

    def _chain_b(self):
        b = self.b
        self.b2j = b2j = {}
        for i, elt in enumerate(b):
            b2j.setdefault(elt, []).append(i)
        self.bjunk = junk = set()
        if self.isjunk:
            for elt in list(b2j):
                if self.isjunk(elt):
                    junk.add(elt)
            for elt in junk:
                del b2j[elt]
        self.bpopular = popular = set()
        n = len(b)
        if self.autojunk and n >= 200:
            ntest = n // 100 + 1
            for elt, idxs in list(b2j.items()):
                if len(idxs) > ntest:
                    popular.add(elt)
            for elt in popular:
                del b2j[elt]

    def find_longest_match(self, alo=0, ahi=None, blo=0, bhi=None):
        a, b, b2j, isbjunk = self.a, self.b, self.b2j, self.bjunk.__contains__
        if ahi is None:
            ahi = len(a)
        if bhi is None:
            bhi = len(b)
        besti, bestj, bestsize = alo, blo, 0
        j2len = {}
        for i in range(alo, ahi):
            newj2len = {}
            for j in b2j.get(a[i], ()):
                if j < blo:
                    continue
                if j >= bhi:
                    break
                k = newj2len[j] = j2len.get(j - 1, 0) + 1
                if k > bestsize:
                    besti, bestj, bestsize = i - k + 1, j - k + 1, k
            j2len = newj2len
        while besti > alo and bestj > blo and not isbjunk(b[bestj - 1]) and a[besti - 1] == b[bestj - 1]:
            besti, bestj, bestsize = besti - 1, bestj - 1, bestsize + 1
        while (besti + bestsize < ahi and bestj + bestsize < bhi and not isbjunk(b[bestj + bestsize])
               and a[besti + bestsize] == b[bestj + bestsize]):
            bestsize += 1
        while besti > alo and bestj > blo and isbjunk(b[bestj - 1]) and a[besti - 1] == b[bestj - 1]:
            besti, bestj, bestsize = besti - 1, bestj - 1, bestsize + 1
        while (besti + bestsize < ahi and bestj + bestsize < bhi and isbjunk(b[bestj + bestsize])
               and a[besti + bestsize] == b[bestj + bestsize]):
            bestsize = bestsize + 1
        return Match(besti, bestj, bestsize)

    def get_matching_blocks(self):
        if self.matching_blocks is not None:
            return self.matching_blocks
        la, lb = len(self.a), len(self.b)
        queue = [(0, la, 0, lb)]
        blocks = []
        while queue:
            alo, ahi, blo, bhi = queue.pop()
            i, j, k = x = self.find_longest_match(alo, ahi, blo, bhi)
            if k:
                blocks.append(x)
                if alo < i and blo < j:
                    queue.append((alo, i, blo, j))
                if i + k < ahi and j + k < bhi:
                    queue.append((i + k, ahi, j + k, bhi))
        blocks.sort()
        i1 = j1 = k1 = 0
        merged = []
        for i2, j2, k2 in blocks:
            if i1 + k1 == i2 and j1 + k1 == j2:
                k1 += k2
            else:
                if k1:
                    merged.append((i1, j1, k1))
                i1, j1, k1 = i2, j2, k2
        if k1:
            merged.append((i1, j1, k1))
        merged.append((la, lb, 0))
        self.matching_blocks = [Match(*t) for t in merged]
        return self.matching_blocks

    def get_opcodes(self):
        if self.opcodes is not None:
            return self.opcodes
        i = j = 0
        self.opcodes = answer = []
        for ai, bj, size in self.get_matching_blocks():
            tag = ""
            if i < ai and j < bj:
                tag = "replace"
            elif i < ai:
                tag = "delete"
            elif j < bj:
                tag = "insert"
            if tag:
                answer.append((tag, i, ai, j, bj))
            i, j = ai + size, bj + size
            if size:
                answer.append(("equal", ai, i, bj, j))
        return answer

    def get_grouped_opcodes(self, n=3):
        codes = self.get_opcodes()
        if not codes:
            codes = [("equal", 0, 1, 0, 1)]
        if codes[0][0] == "equal":
            tag, i1, i2, j1, j2 = codes[0]
            codes[0] = tag, max(i1, i2 - n), i2, max(j1, j2 - n), j2
        if codes[-1][0] == "equal":
            tag, i1, i2, j1, j2 = codes[-1]
            codes[-1] = tag, i1, min(i2, i1 + n), j1, min(j2, j1 + n)
        nn = n + n
        group = []
        for tag, i1, i2, j1, j2 in codes:
            if tag == "equal" and i2 - i1 > nn:
                group.append((tag, i1, min(i2, i1 + n), j1, min(j2, j1 + n)))
                yield group
                group = []
                i1, j1 = max(i1, i2 - n), max(j1, j2 - n)
            group.append((tag, i1, i2, j1, j2))
        if group and not (len(group) == 1 and group[0][0] == "equal"):
            yield group

    def ratio(self):
        matches = sum(t[-1] for t in self.get_matching_blocks())
        return _ratio(matches, len(self.a) + len(self.b))

    def quick_ratio(self):
        if self.fullbcount is None:
            self.fullbcount = full = {}
            for elt in self.b:
                full[elt] = full.get(elt, 0) + 1
        full = self.fullbcount
        avail = {}
        matches = 0
        for elt in self.a:
            numb = avail[elt] if elt in avail else full.get(elt, 0)
            avail[elt] = numb - 1
            if numb > 0:
                matches += 1
        return _ratio(matches, len(self.a) + len(self.b))

    def real_quick_ratio(self):
        la, lb = len(self.a), len(self.b)
        return _ratio(min(la, lb), la + lb)


def _ratio(matches, length):
    return 2.0 * matches / length if length else 1.0


def get_close_matches(word, possibilities, n=3, cutoff=0.6):
    if not n > 0:
        raise ValueError("n must be > 0: %r" % (n,))
    if not 0.0 <= cutoff <= 1.0:
        raise ValueError("cutoff must be in [0.0, 1.0]: %r" % (cutoff,))
    result = []
    s = SequenceMatcher()
    s.set_seq2(word)
    for x in possibilities:
        s.set_seq1(x)
        if s.real_quick_ratio() >= cutoff and s.quick_ratio() >= cutoff and s.ratio() >= cutoff:
            result.append((s.ratio(), x))
    result.sort(key=lambda t: (t[0], t[1]), reverse=True)
    return [x for score, x in result[:n]]


def _format_range_unified(start, stop):
    beginning = start + 1
    length = stop - start
    if length == 1:
        return "%d" % beginning
    if not length:
        beginning -= 1
    return "%d,%d" % (beginning, length)


def unified_diff(a, b, fromfile="", tofile="", fromfiledate="", tofiledate="", n=3, lineterm="\n"):
    started = False
    for group in SequenceMatcher(None, a, b).get_grouped_opcodes(n):
        if not started:
            started = True
            fromdate = "\t" + fromfiledate if fromfiledate else ""
            todate = "\t" + tofiledate if tofiledate else ""
            yield "--- %s%s%s" % (fromfile, fromdate, lineterm)
            yield "+++ %s%s%s" % (tofile, todate, lineterm)
        first, last = group[0], group[-1]
        yield "@@ -%s +%s @@%s" % (_format_range_unified(first[1], last[2]),
                                   _format_range_unified(first[3], last[4]), lineterm)
        for tag, i1, i2, j1, j2 in group:
            if tag == "equal":
                for line in a[i1:i2]:
                    yield " " + line
                continue
            if tag in ("replace", "delete"):
                for line in a[i1:i2]:
                    yield "-" + line
            if tag in ("replace", "insert"):
                for line in b[j1:j2]:
                    yield "+" + line


def ndiff(a, b, linejunk=None, charjunk=None):
    for tag, i1, i2, j1, j2 in SequenceMatcher(linejunk, a, b).get_opcodes():
        if tag == "equal":
            for line in a[i1:i2]:
                yield "  " + line
            continue
        for line in a[i1:i2]:
            yield "- " + line
        for line in b[j1:j2]:
            yield "+ " + line


def IS_CHARACTER_JUNK(ch, ws=" \t"):
    return ch in ws


def IS_LINE_JUNK(line, pat=None):
    return line.strip() in ("", "#")
