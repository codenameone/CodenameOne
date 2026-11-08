# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.

"""shutil: the parts of CPython's module that make sense here."""
import os

__all__ = ["get_terminal_size", "copyfileobj", "copyfile", "copy"]


def get_terminal_size(fallback=(80, 24)):
    """COLUMNS and LINES when set, else the terminal on standard output,
    else `fallback` -- in that order, as CPython does."""
    try:
        columns = int(os.getenv("COLUMNS", ""))
    except ValueError:
        columns = 0
    try:
        lines = int(os.getenv("LINES", ""))
    except ValueError:
        lines = 0
    if columns <= 0 or lines <= 0:
        try:
            size = os.get_terminal_size(1)
        except OSError:
            size = os.terminal_size(fallback)
        if columns <= 0:
            columns = size.columns or fallback[0]
        if lines <= 0:
            lines = size.lines or fallback[1]
    return os.terminal_size((columns, lines))


def copyfileobj(fsrc, fdst, length=0):
    size = length if length and length > 0 else 65536
    while True:
        buf = fsrc.read(size)
        if not buf:
            break
        fdst.write(buf)


def copyfile(src, dst, *, follow_symlinks=True):
    with open(src, "rb") as fsrc, open(dst, "wb") as fdst:
        copyfileobj(fsrc, fdst)
    return dst


def copy(src, dst, *, follow_symlinks=True):
    if os.path.isdir(dst):
        dst = os.path.join(dst, os.path.basename(src))
    return copyfile(src, dst)

