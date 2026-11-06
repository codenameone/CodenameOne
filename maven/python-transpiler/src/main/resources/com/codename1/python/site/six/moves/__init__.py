# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""six.moves: the renamed Python 2 modules and names, as Python 3 ones."""

import sys as _sys
import functools as _functools
import itertools as _itertools
import io as _io
import builtins
import collections as _collections

range = range
xrange = range
zip = zip
map = map
filter = filter
input = input
intern = _sys.intern
reduce = _functools.reduce
zip_longest = _itertools.zip_longest
filterfalse = _itertools.filterfalse
StringIO = _io.StringIO
cStringIO = _io.StringIO
UserDict = _collections.UserDict
UserList = _collections.UserList
UserString = _collections.UserString
reload_module = None

import os as _os
getcwd = _os.getcwd
import shlex as _shlex
shlex_quote = _shlex.quote
import queue
import urllib.parse as _urllib_parse
urllib_parse = _urllib_parse


class _Urllib:
    parse = _urllib_parse


urllib = _Urllib()
