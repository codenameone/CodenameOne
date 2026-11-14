# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""Installed-distribution metadata, recorded at build time.

A translated program has no site-packages to scan. The build writes the
metadata of every PyPI package it installed into the module
_cn1_distributions (python-packages/_cn1_distributions.py): each one's name,
version, top-level packages, requirements and the text of its
entry_points.txt. Plugin systems that discover their plugins through entry
points (pluggy's load_setuptools_entrypoints, llm's plugin loader) therefore
find every plugin that was installed with the application, and only those.
"""
import re as _re

try:
    from _cn1_distributions import DISTRIBUTIONS as _DISTRIBUTIONS
except ImportError:
    _DISTRIBUTIONS = {}


class PackageNotFoundError(ModuleNotFoundError):
    def __str__(self):
        return f"No package metadata was found for {self.name}"

    @property
    def name(self):
        return self.args[0]


def _normalize(name):
    return _re.sub(r"[-_.]+", "-", name).lower()


class EntryPoint:
    def __init__(self, name, value, group):
        self.name = name
        self.value = value
        self.group = group
        self.dist = None

    @property
    def module(self):
        return self.value.partition(":")[0].strip()

    @property
    def attr(self):
        return self.value.partition(":")[2].partition("[")[0].strip()

    @property
    def extras(self):
        m = _re.search(r"\[(.*)\]", self.value)
        return [x.strip() for x in m.group(1).split(",")] if m else []

    def load(self):
        import importlib
        module = importlib.import_module(self.module)
        value = module
        for part in filter(None, self.attr.split(".")):
            value = getattr(value, part)
        return value

    def matches(self, **params):
        for k, v in params.items():
            if getattr(self, k) != v:
                return False
        return True

    def __iter__(self):
        return iter((self.name, self))

    def __eq__(self, other):
        return isinstance(other, EntryPoint) and (self.name, self.value, self.group) == (
            other.name, other.value, other.group)

    def __hash__(self):
        return hash((self.name, self.value, self.group))

    def __repr__(self):
        return f"EntryPoint(name={self.name!r}, value={self.value!r}, group={self.group!r})"


class EntryPoints(tuple):
    def __getitem__(self, name):
        if isinstance(name, int):
            return tuple.__getitem__(self, name)
        for ep in self:
            if ep.name == name:
                return ep
        raise KeyError(name)

    def select(self, **params):
        return EntryPoints(ep for ep in self if ep.matches(**params))

    @property
    def names(self):
        return {ep.name for ep in self}

    @property
    def groups(self):
        return {ep.group for ep in self}


def _parse_entry_points(text):
    group = None
    out = []
    for raw in (text or "").splitlines():
        line = raw.strip()
        if not line or line.startswith(("#", ";")):
            continue
        if line.startswith("[") and line.endswith("]"):
            group = line[1:-1].strip()
            continue
        name, sep, value = line.partition("=")
        if sep and group is not None:
            out.append(EntryPoint(name.strip(), value.strip(), group))
    return out


class _Metadata(dict):
    def get_all(self, name, failobj=None):
        v = self.get(name)
        if v is None:
            return failobj
        return v if isinstance(v, list) else [v]

    @property
    def json(self):
        return dict(self)


class Distribution:
    def __init__(self, name, record):
        self._name = name
        self._record = record

    @classmethod
    def from_name(cls, name):
        if not name:
            raise ValueError("A distribution name is required.")
        key = _normalize(name)
        for n, record in _DISTRIBUTIONS.items():
            if _normalize(n) == key:
                return cls(n, record)
        raise PackageNotFoundError(name)

    @classmethod
    def discover(cls, **kwargs):
        return [cls(n, r) for n, r in _DISTRIBUTIONS.items()]

    @property
    def name(self):
        return self._name

    @property
    def version(self):
        return self._record.get("version", "")

    @property
    def metadata(self):
        m = _Metadata()
        m["Name"] = self._name
        m["Version"] = self.version
        for k, v in self._record.get("metadata", {}).items():
            m[k] = v
        if self._record.get("requires"):
            m["Requires-Dist"] = list(self._record["requires"])
        return m

    @property
    def entry_points(self):
        eps = _parse_entry_points(self._record.get("entry_points", ""))
        for ep in eps:
            ep.dist = self
        return EntryPoints(eps)

    @property
    def requires(self):
        return list(self._record.get("requires", [])) or None

    @property
    def files(self):
        return None

    def read_text(self, filename):
        if filename == "entry_points.txt":
            return self._record.get("entry_points")
        if filename == "top_level.txt":
            return "\n".join(self._record.get("top_level", []))
        return None

    def locate_file(self, path):
        return path


def distribution(distribution_name):
    return Distribution.from_name(distribution_name)


def distributions(**kwargs):
    return iter(Distribution.discover(**kwargs))


def metadata(distribution_name):
    return Distribution.from_name(distribution_name).metadata


def version(distribution_name):
    return Distribution.from_name(distribution_name).version


def requires(distribution_name):
    return Distribution.from_name(distribution_name).requires


def files(distribution_name):
    return Distribution.from_name(distribution_name).files


def entry_points(**params):
    eps = []
    seen = set()
    for dist in Distribution.discover():
        if _normalize(dist.name) in seen:
            continue
        seen.add(_normalize(dist.name))
        eps.extend(dist.entry_points)
    return EntryPoints(eps).select(**params)


def packages_distributions():
    out = {}
    for n, record in _DISTRIBUTIONS.items():
        for top in record.get("top_level", []):
            out.setdefault(top, []).append(n)
    return out
