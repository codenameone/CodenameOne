# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""posixpath: POSIX path operations. os.path is the POSIX module on every
Codename One target, so this names the same functions."""

import os.path as _p

sep = _p.sep
altsep = _p.altsep
extsep = _p.extsep
pathsep = _p.pathsep
curdir = _p.curdir
pardir = _p.pardir
defpath = _p.defpath
devnull = _p.devnull
supports_unicode_filenames = _p.supports_unicode_filenames
normcase = _p.normcase
isabs = _p.isabs
join = _p.join
split = _p.split
splitext = _p.splitext
splitdrive = _p.splitdrive
splitroot = _p.splitroot
basename = _p.basename
dirname = _p.dirname
normpath = _p.normpath
abspath = _p.abspath
realpath = _p.realpath
relpath = _p.relpath
commonprefix = _p.commonprefix
commonpath = _p.commonpath
exists = _p.exists
lexists = _p.lexists
isdir = _p.isdir
isfile = _p.isfile
islink = _p.islink
ismount = _p.ismount
getsize = _p.getsize
getmtime = _p.getmtime
getatime = _p.getatime
getctime = _p.getctime
samefile = _p.samefile
expanduser = _p.expanduser
expandvars = _p.expandvars
