# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""Symbolic error numbers. The values are Linux's on every target: no
translated program receives a raw errno from the platform, so they only have
to agree with the OSError numbers the runtime itself raises."""

EPERM = 1
ENOENT = 2
ESRCH = 3
EINTR = 4
EIO = 5
ENXIO = 6
E2BIG = 7
ENOEXEC = 8
EBADF = 9
ECHILD = 10
EAGAIN = 11
ENOMEM = 12
EACCES = 13
EFAULT = 14
ENOTBLK = 15
EBUSY = 16
EEXIST = 17
EXDEV = 18
ENODEV = 19
ENOTDIR = 20
EISDIR = 21
EINVAL = 22
ENFILE = 23
EMFILE = 24
ENOTTY = 25
ETXTBSY = 26
EFBIG = 27
ENOSPC = 28
ESPIPE = 29
EROFS = 30
EMLINK = 31
EPIPE = 32
EDOM = 33
ERANGE = 34
EDEADLK = 35
ENAMETOOLONG = 36
ENOLCK = 37
ENOSYS = 38
ENOTEMPTY = 39
ELOOP = 40
ENOMSG = 42
ENOTSUP = 95
EOPNOTSUPP = 95
EADDRINUSE = 98
EADDRNOTAVAIL = 99
ENETDOWN = 100
ENETUNREACH = 101
ECONNABORTED = 103
ECONNRESET = 104
ENOBUFS = 105
EISCONN = 106
ENOTCONN = 107
ETIMEDOUT = 110
ECONNREFUSED = 111
EHOSTUNREACH = 113
EALREADY = 114
EINPROGRESS = 115
ECANCELED = 125
EWOULDBLOCK = 11
EDEADLOCK = 35

errorcode = {
    1: "EPERM",
    2: "ENOENT",
    3: "ESRCH",
    4: "EINTR",
    5: "EIO",
    6: "ENXIO",
    7: "E2BIG",
    8: "ENOEXEC",
    9: "EBADF",
    10: "ECHILD",
    11: "EAGAIN",
    12: "ENOMEM",
    13: "EACCES",
    14: "EFAULT",
    15: "ENOTBLK",
    16: "EBUSY",
    17: "EEXIST",
    18: "EXDEV",
    19: "ENODEV",
    20: "ENOTDIR",
    21: "EISDIR",
    22: "EINVAL",
    23: "ENFILE",
    24: "EMFILE",
    25: "ENOTTY",
    26: "ETXTBSY",
    27: "EFBIG",
    28: "ENOSPC",
    29: "ESPIPE",
    30: "EROFS",
    31: "EMLINK",
    32: "EPIPE",
    33: "EDOM",
    34: "ERANGE",
    35: "EDEADLK",
    36: "ENAMETOOLONG",
    37: "ENOLCK",
    38: "ENOSYS",
    39: "ENOTEMPTY",
    40: "ELOOP",
    42: "ENOMSG",
    95: "ENOTSUP",
    98: "EADDRINUSE",
    99: "EADDRNOTAVAIL",
    100: "ENETDOWN",
    101: "ENETUNREACH",
    103: "ECONNABORTED",
    104: "ECONNRESET",
    105: "ENOBUFS",
    106: "EISCONN",
    107: "ENOTCONN",
    110: "ETIMEDOUT",
    111: "ECONNREFUSED",
    113: "EHOSTUNREACH",
    114: "EALREADY",
    115: "EINPROGRESS",
    125: "ECANCELED",
}
