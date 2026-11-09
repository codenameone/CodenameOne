# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""The module spec type importlib.util hands out."""


class ModuleSpec:
    def __init__(self, name, loader, *, origin=None, loader_state=None, is_package=None):
        self.name = name
        self.loader = loader
        self.origin = origin
        self.loader_state = loader_state
        self.submodule_search_locations = [] if is_package else None
        self.cached = None
        self.has_location = False

    @property
    def parent(self):
        if self.submodule_search_locations is None:
            return self.name.rpartition(".")[0]
        return self.name

    def __repr__(self):
        return f"ModuleSpec(name={self.name!r}, loader={self.loader!r}, origin={self.origin!r})"


SOURCE_SUFFIXES = [".py"]
EXTENSION_SUFFIXES = []
BYTECODE_SUFFIXES = []
