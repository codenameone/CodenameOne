# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""Headers, query parameters and URLs."""
from urllib.parse import parse_qsl, quote, urlencode


class Headers:
    """Case-insensitive, multi-valued, lower-cased header mapping."""

    def __init__(self, headers=None):
        self._list = []
        if headers is not None:
            items = headers.items() if isinstance(headers, (dict, Headers)) else headers
            for k, v in items:
                self._list.append((str(k).lower(), str(v)))

    @property
    def raw(self):
        return [(k.encode("latin-1"), v.encode("latin-1")) for k, v in self._list]

    def get(self, key, default=None):
        lk = key.lower()
        for k, v in self._list:
            if k == lk:
                return v
        return default

    def getlist(self, key):
        lk = key.lower()
        return [v for k, v in self._list if k == lk]

    def __getitem__(self, key):
        lk = key.lower()
        for k, v in self._list:
            if k == lk:
                return v
        raise KeyError(key)

    def __contains__(self, key):
        lk = str(key).lower()
        for k, v in self._list:
            if k == lk:
                return True
        return False

    def keys(self):
        return [k for k, v in self._list]

    def values(self):
        return [v for k, v in self._list]

    def items(self):
        return list(self._list)

    def multi_items(self):
        return list(self._list)

    def __iter__(self):
        return iter(self.keys())

    def __len__(self):
        return len(self._list)

    def __eq__(self, other):
        if isinstance(other, Headers):
            return sorted(self._list) == sorted(other._list)
        if isinstance(other, dict):
            return sorted(self._list) == sorted((k.lower(), v) for k, v in other.items())
        return False

    def __repr__(self):
        return "Headers(%r)" % (dict(self._list),)


class MutableHeaders(Headers):
    def __setitem__(self, key, value):
        lk = key.lower()
        out = []
        done = False
        for k, v in self._list:
            if k == lk:
                if not done:
                    out.append((lk, str(value)))
                    done = True
            else:
                out.append((k, v))
        if not done:
            out.append((lk, str(value)))
        self._list = out

    def __delitem__(self, key):
        lk = key.lower()
        self._list = [(k, v) for k, v in self._list if k != lk]

    def append(self, key, value):
        self._list.append((key.lower(), str(value)))

    def setdefault(self, key, value):
        if key not in self:
            self[key] = value
        return self[key]

    def update(self, other):
        items = other.items() if hasattr(other, "items") else other
        for k, v in items:
            self[k] = v

    def add_vary_header(self, vary):
        existing = self.get("vary")
        if existing is not None:
            vary = ", ".join([existing, vary])
        self["vary"] = vary


class QueryParams:
    """Immutable multi-dict of decoded query parameters; the last value wins on get."""

    def __init__(self, query=""):
        if isinstance(query, (bytes, bytearray)):
            query = bytes(query).decode("latin-1")
        if isinstance(query, str):
            self._list = list(parse_qsl(query, keep_blank_values=True))
        elif hasattr(query, "multi_items"):
            self._list = list(query.multi_items())
        elif isinstance(query, dict):
            self._list = list(query.items())
        else:
            self._list = list(query)
        self._dict = {}
        for k, v in self._list:
            self._dict[k] = v

    def get(self, key, default=None):
        return self._dict.get(key, default)

    def getlist(self, key):
        return [v for k, v in self._list if k == key]

    def __getitem__(self, key):
        return self._dict[key]

    def __contains__(self, key):
        return key in self._dict

    def keys(self):
        return list(self._dict.keys())

    def values(self):
        return list(self._dict.values())

    def items(self):
        return list(self._dict.items())

    def multi_items(self):
        return list(self._list)

    def __iter__(self):
        return iter(list(self._dict.keys()))

    def __len__(self):
        return len(self._dict)

    def __str__(self):
        return urlencode(self._list)

    def __repr__(self):
        return "QueryParams(%r)" % (str(self),)


class URL:
    def __init__(self, url="", scheme="http", host="testserver", path="/", query=""):
        if url:
            rest = url
            self.scheme = "http"
            if "://" in rest:
                self.scheme, rest = rest.split("://", 1)
                if "/" in rest:
                    self.netloc, rest = rest.split("/", 1)
                    rest = "/" + rest
                else:
                    self.netloc, rest = rest, "/"
            else:
                self.netloc = ""
            self.fragment = ""
            if "#" in rest:
                rest, self.fragment = rest.split("#", 1)
            self.query = ""
            if "?" in rest:
                rest, self.query = rest.split("?", 1)
            self.path = rest
        else:
            self.scheme = scheme
            self.netloc = host
            self.path = path
            self.query = query
            self.fragment = ""

    @property
    def hostname(self):
        return self.netloc.split(":")[0]

    @property
    def port(self):
        if ":" in self.netloc:
            return int(self.netloc.split(":")[1])
        return None

    def __str__(self):
        s = ""
        if self.netloc:
            s = self.scheme + "://" + self.netloc
        s += quote(self.path, safe="/%:@!$&'()*+,;=-._~")
        if self.query:
            s += "?" + self.query
        if self.fragment:
            s += "#" + self.fragment
        return s

    def __repr__(self):
        return "URL(%r)" % (str(self),)

    def __eq__(self, other):
        return str(self) == str(other)

    def __hash__(self):
        return hash(str(self))


class State:
    def __init__(self, state=None):
        self.__dict__["_state"] = dict(state or {})

    def __setattr__(self, key, value):
        self._state[key] = value

    def __getattr__(self, key):
        try:
            return self.__dict__["_state"][key]
        except KeyError:
            raise AttributeError("'State' object has no attribute '%s'" % key)

    def __delattr__(self, key):
        del self._state[key]
