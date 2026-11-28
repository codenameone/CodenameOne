# Copyright (c) 2001 Python Software Foundation; All Rights Reserved
# Licensed under the Python Software Foundation License Version 2; the full
# text is in maven/python-transpiler/third-party/cpython/LICENSE.txt.
#
# Derived from CPython's Lib/importlib/util.py. Changes: rewritten for the Codename One
# Python runtime, trimmed to what the transpiler supports and reorganised;
# the C accelerators and platform-specific branches are left out.
"""find_spec answers whether a module is part of the program."""
import sys
import _cn1imp
from importlib import _resolve_name
from importlib.machinery import ModuleSpec


class _RegistryLoader:
    def create_module(self, spec):
        return None

    def exec_module(self, module):
        pass


def resolve_name(name, package):
    if not name.startswith("."):
        return name
    if not package:
        raise ImportError(f"no package specified for {name!r} (required for relative module names)")
    level = 0
    for character in name:
        if character != ".":
            break
        level += 1
    return _resolve_name(name[level:], package, level)


def find_spec(name, package=None):
    fullname = resolve_name(name, package) if name.startswith(".") else name
    if fullname in sys.modules:
        module = sys.modules[fullname]
        spec = getattr(module, "__spec__", None)
        if spec is not None:
            return spec
        return ModuleSpec(fullname, _RegistryLoader())
    parent = fullname.rpartition(".")[0]
    if parent and not _cn1imp.available(parent):
        raise ModuleNotFoundError(f"No module named {parent!r}", name=parent)
    if not _cn1imp.available(fullname):
        return None
    return ModuleSpec(fullname, _RegistryLoader())


def module_from_spec(spec):
    return _cn1imp.load(spec.name)
