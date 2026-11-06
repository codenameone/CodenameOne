# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""The backtracking machine a compiled pattern runs on: the opcodes
`_parser` compiles to, and `run`, which hands the program to the machine
in Java (`com.codename1.python.runtime.Regex`, module `_cn1re`). The machine
was Python here once; as transpiled Python every step was generic
arithmetic and a tuple per choice point, which made a URL router's match
most of what a request allocated.

Registers hold the group boundaries (2 per group, group 0 first), then
`lastindex`, then one slot per empty-loop guard. Every register write is
logged on a trail, and a choice point remembers the trail's length, so
backtracking restores exactly the captures that were live when the choice
was made. Choice points live on an int stack, never the call stack.
"""

import _cn1re

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


def run(code, text, pos, endpos, mode, forbid=-1):
    """mode 0 = match, 1 = fullmatch, 2 = search. Returns registers or None."""
    prog = getattr(code, "_native", None)
    if prog is None:
        prog = _cn1re.compile(code.prog, code.groups, code.marks, code.prefix, code.anchored)
        code._native = prog
    return _cn1re.run(prog, text, pos, endpos, mode, forbid)
