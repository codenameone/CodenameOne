# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""requests.structures: the case-insensitive dictionary of headers."""


class CaseInsensitiveDict:
    def __init__(self, data=None, **kwargs):
        self._store = {}
        if data is None:
            data = {}
        self.update(data, **kwargs)

    def update(self, data=(), **kwargs):
        items = data.items() if hasattr(data, "items") else data
        for k, v in items:
            self[k] = v
        for k, v in kwargs.items():
            self[k] = v

    def __setitem__(self, key, value):
        self._store[key.lower()] = (key, value)

    def __getitem__(self, key):
        return self._store[key.lower()][1]

    def __delitem__(self, key):
        del self._store[key.lower()]

    def __iter__(self):
        return (casedkey for casedkey, _ in self._store.values())

    def __len__(self):
        return len(self._store)

    def __contains__(self, key):
        return key.lower() in self._store

    def get(self, key, default=None):
        return self[key] if key in self else default

    def keys(self):
        return list(self)

    def values(self):
        return [v for _, v in self._store.values()]

    def items(self):
        return [(k, v) for k, v in self._store.values()]

    def lower_items(self):
        return [(k, v[1]) for k, v in self._store.items()]

    def setdefault(self, key, default=None):
        if key not in self:
            self[key] = default
        return self[key]

    def pop(self, key, *default):
        if key in self:
            v = self[key]
            del self[key]
            return v
        if default:
            return default[0]
        raise KeyError(key)

    def __eq__(self, other):
        if hasattr(other, "items"):
            other = CaseInsensitiveDict(other)
        else:
            return NotImplemented
        return dict(self.lower_items()) == dict(other.lower_items())

    def copy(self):
        return CaseInsensitiveDict(self._store.values())

    def __repr__(self):
        return str(dict(self.items()))


class LookupDict(dict):
    def __init__(self, name=None):
        self.name = name
        super().__init__()

    def __repr__(self):
        return f"<lookup '{self.name}'>"

    def __getitem__(self, key):
        return self.__dict__.get(key, None)

    def get(self, key, default=None):
        return self.__dict__.get(key, default)
