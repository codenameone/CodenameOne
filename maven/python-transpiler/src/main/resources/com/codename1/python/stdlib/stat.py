# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""Constants and tests for the st_mode bits os.stat() reports."""

S_IFMT_MASK = 0o170000
S_IFDIR = 0o040000
S_IFCHR = 0o020000
S_IFBLK = 0o060000
S_IFREG = 0o100000
S_IFIFO = 0o010000
S_IFLNK = 0o120000
S_IFSOCK = 0o140000
S_IFDOOR = 0
S_IFPORT = 0
S_IFWHT = 0

S_ISUID = 0o4000
S_ISGID = 0o2000
S_ENFMT = S_ISGID
S_ISVTX = 0o1000
S_IREAD = 0o0400
S_IWRITE = 0o0200
S_IEXEC = 0o0100
S_IRWXU = 0o0700
S_IRUSR = 0o0400
S_IWUSR = 0o0200
S_IXUSR = 0o0100
S_IRWXG = 0o0070
S_IRGRP = 0o0040
S_IWGRP = 0o0020
S_IXGRP = 0o0010
S_IRWXO = 0o0007
S_IROTH = 0o0004
S_IWOTH = 0o0002
S_IXOTH = 0o0001

ST_MODE, ST_INO, ST_DEV, ST_NLINK, ST_UID, ST_GID, ST_SIZE, ST_ATIME, ST_MTIME, ST_CTIME = range(10)


def S_IMODE(mode):
    return mode & 0o7777


def S_IFMT(mode):
    return mode & S_IFMT_MASK


def S_ISDIR(mode):
    return S_IFMT(mode) == S_IFDIR


def S_ISCHR(mode):
    return S_IFMT(mode) == S_IFCHR


def S_ISBLK(mode):
    return S_IFMT(mode) == S_IFBLK


def S_ISREG(mode):
    return S_IFMT(mode) == S_IFREG


def S_ISFIFO(mode):
    return S_IFMT(mode) == S_IFIFO


def S_ISLNK(mode):
    return S_IFMT(mode) == S_IFLNK


def S_ISSOCK(mode):
    return S_IFMT(mode) == S_IFSOCK


def S_ISDOOR(mode):
    return False


def S_ISPORT(mode):
    return False


def S_ISWHT(mode):
    return False


_filemode_table = (
    ((S_IFLNK, "l"), (S_IFSOCK, "s"), (S_IFREG, "-"), (S_IFBLK, "b"), (S_IFDIR, "d"), (S_IFCHR, "c"),
     (S_IFIFO, "p")),
    ((S_IRUSR, "r"),),
    ((S_IWUSR, "w"),),
    ((S_IXUSR | S_ISUID, "s"), (S_ISUID, "S"), (S_IXUSR, "x")),
    ((S_IRGRP, "r"),),
    ((S_IWGRP, "w"),),
    ((S_IXGRP | S_ISGID, "s"), (S_ISGID, "S"), (S_IXGRP, "x")),
    ((S_IROTH, "r"),),
    ((S_IWOTH, "w"),),
    ((S_IXOTH | S_ISVTX, "t"), (S_ISVTX, "T"), (S_IXOTH, "x")),
)


def filemode(mode):
    perm = []
    for index, table in enumerate(_filemode_table):
        for bit, char in table:
            if index == 0:
                if S_IFMT(mode) == bit:
                    perm.append(char)
                    break
            elif mode & bit == bit:
                perm.append(char)
                break
        else:
            perm.append("?" if index == 0 else "-")
    return "".join(perm)
