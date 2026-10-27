# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""The backtracking machine a compiled pattern runs on.

Registers hold the group boundaries (2 per group, group 0 first), then
`lastindex`, then one slot per empty-loop guard. Every register write is
logged on a trail, and a choice point remembers the trail's length, so
backtracking restores exactly the captures that were live when the choice
was made. Choice points live on a list, never the call stack.
"""

CHAR = 1
CHARI = 2
ANY = 3
ANYALL = 4
SET = 5
AT = 6
SPLIT = 7
JMP = 8
SAVE = 9
BACKREF = 10
LOOK = 11
ATOMIC = 12
MATCH = 13
MARK = 14
PROGRESS = 15
GROUPREF_EXISTS = 16
REPEAT_ONE = 17
MIN_REPEAT_ONE = 18

_ASCII_WORD = frozenset("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_")
_ASCII_SPACE = frozenset(" \t\n\r\f\v")


def _cat(k, c, unicode):
    lk = k.lower()
    if lk == "d":
        r = c.isdecimal() if unicode else "0" <= c <= "9"
    elif lk == "w":
        r = (c.isalnum() or c == "_") if unicode else c in _ASCII_WORD
    else:
        r = c.isspace() if unicode else c in _ASCII_SPACE
    return r if k == lk else not r


def _one(op, c):
    k = op[0]
    if k == CHAR:
        return c == op[1]
    if k == CHARI:
        return c == op[1] or c == op[2] or c.lower() == op[1]
    if k == ANY:
        return c != "\n"
    if k == ANYALL:
        return True
    chars, ranges, cats, neg, rcase, unicode = op[1], op[2], op[3], op[4], op[5], op[6]
    r = c in chars
    if not r and ranges:
        o = ord(c)
        for lo, hi in ranges:
            if lo <= o <= hi:
                r = True
                break
        if not r and rcase:
            for alt in (c.lower(), c.upper()):
                if len(alt) == 1:
                    o = ord(alt)
                    for lo, hi in ranges:
                        if lo <= o <= hi:
                            r = True
                            break
    if not r and cats:
        for k2 in cats:
            if _cat(k2, c, unicode):
                r = True
                break
    return r != neg


def _word(text, i, endpos, unicode):
    if i < 0 or i >= endpos:
        return False
    c = text[i]
    return (c.isalnum() or c == "_") if unicode else c in _ASCII_WORD


def _at(kind, text, pos, endpos, unicode):
    if kind == "begin":
        return pos == 0
    if kind == "bol":
        return pos == 0 or text[pos - 1] == "\n"
    if kind == "end":
        return pos == endpos
    if kind == "eol":
        return pos == endpos or (pos == endpos - 1 and text[pos] == "\n")
    if kind == "eolm":
        return pos == endpos or text[pos] == "\n"
    b = _word(text, pos - 1, endpos, unicode) != _word(text, pos, endpos, unicode)
    if kind == "boundary":
        return b
    return not b


def _exec(prog, text, start, endpos, regs, full, forbid, li):
    """Runs `prog` from `start`; returns the end position, or -1."""
    stack = []
    trail = []
    pc = 0
    pos = start
    while True:
        op = prog[pc]
        k = op[0]
        ok = True
        if k == CHAR:
            if pos < endpos and text[pos] == op[1]:
                pos += 1
                pc += 1
                continue
            ok = False
        elif k <= SET:
            if pos < endpos and _one(op, text[pos]):
                pos += 1
                pc += 1
                continue
            ok = False
        elif k == SPLIT:
            stack.append((0, op[2], pos, len(trail)))
            pc = op[1]
            continue
        elif k == JMP:
            pc = op[1]
            continue
        elif k == SAVE:
            n = op[1]
            trail.append((n, regs[n]))
            regs[n] = pos
            if n & 1:
                trail.append((li, regs[li]))
                regs[li] = n >> 1
            pc += 1
            continue
        elif k == REPEAT_ONE:
            one = op[1]
            lo = op[2]
            hi = op[3]
            limit = endpos if hi is None else min(endpos, pos + hi)
            j = pos
            while j < limit and _one(one, text[j]):
                j += 1
            if j - pos < lo:
                ok = False
            else:
                if j > pos + lo:
                    stack.append((1, pc + 1, pos + lo, j, len(trail)))
                pos = j
                pc += 1
                continue
        elif k == MIN_REPEAT_ONE:
            one = op[1]
            lo = op[2]
            j = pos
            while j < pos + lo:
                if j >= endpos or not _one(one, text[j]):
                    break
                j += 1
            if j < pos + lo:
                ok = False
            else:
                stack.append((2, pc + 1, j, lo, len(trail), one, op[3]))
                pos = j
                pc += 1
                continue
        elif k == AT:
            if _at(op[1], text, pos, endpos, op[2]):
                pc += 1
                continue
            ok = False
        elif k == MATCH:
            if (full and pos != endpos) or (forbid >= 0 and pos == start == forbid):
                ok = False
            else:
                return pos
        elif k == MARK:
            n = li + 1 + op[1]
            trail.append((n, regs[n]))
            regs[n] = pos
            pc += 1
            continue
        elif k == PROGRESS:
            if regs[li + 1 + op[1]] == pos:
                pc = op[2]
            else:
                pc += 1
            continue
        elif k == BACKREF:
            s = regs[2 * op[1]]
            e = regs[2 * op[1] + 1]
            if s < 0 or e < 0:
                ok = False
            else:
                seg = text[s:e]
                n = e - s
                got = text[pos:pos + n] if pos + n <= endpos else None
                if got is not None and (got == seg or (op[2] and got.lower() == seg.lower())):
                    pos += n
                    pc += 1
                    continue
                ok = False
        elif k == GROUPREF_EXISTS:
            if regs[2 * op[1]] >= 0 and regs[2 * op[1] + 1] >= 0:
                pc += 1
            else:
                pc = op[2]
            continue
        elif k == LOOK:
            sub = op[1]
            inner = list(regs)
            if op[2]:
                s = pos - op[4]
                end = -1 if s < 0 else _exec(sub, text, s, pos, inner, True, -1, li)
            else:
                end = _exec(sub, text, pos, endpos, inner, False, -1, li)
            if op[3]:
                if end < 0:
                    pc += 1
                    continue
                ok = False
            elif end < 0:
                ok = False
            else:
                _adopt(regs, inner, trail)
                pc += 1
                continue
        elif k == ATOMIC:
            inner = list(regs)
            end = _exec(op[1], text, pos, endpos, inner, False, -1, li)
            if end < 0:
                ok = False
            else:
                _adopt(regs, inner, trail)
                pos = end
                pc += 1
                continue
        if ok:
            raise RuntimeError("internal error: opcode %d" % k)
        # Backtrack to the most recent choice point.
        while True:
            if not stack:
                return -1
            e = stack.pop()
            kind = e[0]
            if kind == 0:
                pc, pos, tl = e[1], e[2], e[3]
                break
            if kind == 1:
                nxt, minpos, cur, tl = e[1], e[2], e[3], e[4]
                cur -= 1
                # Only stop where the next literal could match.
                nop = prog[nxt]
                if nop[0] == CHAR:
                    c = nop[1]
                    while cur >= minpos and (cur >= endpos or text[cur] != c):
                        cur -= 1
                    if cur < minpos:
                        continue
                if cur > minpos:
                    stack.append((1, nxt, minpos, cur, tl))
                pc, pos = nxt, cur
                break
            nxt, cur, count, tl, one, hi = e[1], e[2], e[3], e[4], e[5], e[6]
            if (hi is not None and count >= hi) or cur >= endpos or not _one(one, text[cur]):
                continue
            cur += 1
            stack.append((2, nxt, cur, count + 1, tl, one, hi))
            pc, pos = nxt, cur
            break
        while len(trail) > tl:
            n, v = trail.pop()
            regs[n] = v


def _adopt(regs, inner, trail):
    for n in range(len(regs)):
        if regs[n] != inner[n]:
            trail.append((n, regs[n]))
            regs[n] = inner[n]


def run(code, text, pos, endpos, mode, forbid=-1):
    """mode 0 = match, 1 = fullmatch, 2 = search. Returns registers or None."""
    size = 2 * (code.groups + 1)
    prog = code.prog
    full = mode == 1
    start = pos
    prefix = code.prefix
    while start <= endpos:
        if mode == 2 and prefix:
            found = text.find(prefix, start, endpos)
            if found < 0:
                return None
            start = found
        regs = [-1] * (size + 1 + code.marks)
        end = _exec(prog, text, start, endpos, regs, full, forbid, size)
        if end >= 0:
            regs[0] = start
            regs[1] = end
            out = regs[:size]
            out.append(regs[size])
            return out
        if mode != 2 or code.anchored:
            return None
        start += 1
    return None
