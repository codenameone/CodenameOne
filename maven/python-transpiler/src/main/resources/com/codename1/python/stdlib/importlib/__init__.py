# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""import_module over the translated program's module registry.

Every module is compiled into the program and registered by name before
__main__ runs, so importing by a computed name is a lookup. A name that was
not part of the build raises ModuleNotFoundError, as a missing module does in
CPython; nothing is ever loaded from a path.
"""
import sys
import _cn1imp


def _resolve_name(name, package, level):
    bits = package.rsplit(".", level - 1)
    if len(bits) < level:
        raise ImportError("attempted relative import beyond top-level package")
    base = bits[0]
    return f"{base}.{name}" if name else base


def import_module(name, package=None):
    level = 0
    if name.startswith("."):
        if not package:
            raise TypeError(
                f"the 'package' argument is required to perform a relative import for {name!r}")
        for character in name:
            if character != ".":
                break
            level += 1
        name = _resolve_name(name[level:], package, level)
    return _cn1imp.load(name)


def invalidate_caches():
    pass


def reload(module):
    # A module body runs once; there is no source to read again.
    return module
