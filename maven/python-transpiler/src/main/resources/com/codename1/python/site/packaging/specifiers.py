# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""PEP 440 version specifiers: Specifier and SpecifierSet."""

from .version import Version, InvalidVersion

__all__ = ["InvalidSpecifier", "Specifier", "SpecifierSet"]

_OPS = ("===", "~=", "==", "!=", "<=", ">=", "<", ">")


class InvalidSpecifier(ValueError):
    """Raised when a specifier string is not valid."""


def _to_version(v):
    if isinstance(v, Version):
        return v
    return Version(v)


class Specifier:

    def __init__(self, spec="", prereleases=None):
        s = spec.strip()
        op = None
        for o in _OPS:
            if s.startswith(o):
                op = o
                break
        if op is None:
            raise InvalidSpecifier("Invalid specifier: " + repr(spec))
        ver = s[len(op):].strip()
        if ver == "":
            raise InvalidSpecifier("Invalid specifier: " + repr(spec))
        wildcard = ver.endswith(".*")
        if op == "===":
            pass
        else:
            base = ver[:-2] if wildcard else ver
            if wildcard and op not in ("==", "!="):
                raise InvalidSpecifier("Invalid specifier: " + repr(spec))
            try:
                v = Version(base)
            except InvalidVersion:
                raise InvalidSpecifier("Invalid specifier: " + repr(spec)) from None
            if wildcard and (v.local is not None or v.dev is not None):
                raise InvalidSpecifier("Invalid specifier: " + repr(spec))
            if op == "~=" and len(v.release) < 2:
                raise InvalidSpecifier("Invalid specifier: " + repr(spec))
            if op != "==" and op != "!=" and op != "===" and v.local is not None:
                raise InvalidSpecifier("Invalid specifier: " + repr(spec))
        self._op = op
        self._ver = ver
        self._prereleases = prereleases

    @property
    def operator(self):
        return self._op

    @property
    def version(self):
        return self._ver

    @property
    def prereleases(self):
        if self._prereleases is not None:
            return self._prereleases
        if self._op in ("==", ">=", "<=", "~=", ">", "<"):
            base = self._ver[:-2] if self._ver.endswith(".*") else self._ver
            return Version(base).is_prerelease
        return False

    @prereleases.setter
    def prereleases(self, value):
        self._prereleases = value

    def __str__(self):
        return self._op + self._ver

    def __repr__(self):
        return "<Specifier(" + repr(str(self)) + ")>"

    def __hash__(self):
        return hash((self._op, self._canon()))

    def _canon(self):
        if self._op == "===":
            return self._ver
        wildcard = self._ver.endswith(".*")
        base = self._ver[:-2] if wildcard else self._ver
        return str(Version(base)) + (".*" if wildcard else "")

    def __eq__(self, other):
        if isinstance(other, str):
            try:
                other = Specifier(other)
            except InvalidSpecifier:
                return NotImplemented
        if not isinstance(other, Specifier):
            return NotImplemented
        return self._op == other._op and self._canon() == other._canon()

    def __ne__(self, other):
        r = self.__eq__(other)
        if r is NotImplemented:
            return r
        return not r

    def __contains__(self, item):
        return self.contains(item)

    def contains(self, item, prereleases=None):
        if prereleases is None:
            prereleases = self._prereleases
        if self._op == "===":
            return str(item).lower() == self._ver.lower()
        try:
            v = _to_version(item)
        except InvalidVersion:
            return False
        if v.is_prerelease and prereleases is False:
            return False
        return self._match(v)

    def _match(self, v):
        op = self._op
        ver = self._ver
        if op == "==" or op == "!=":
            if ver.endswith(".*"):
                spec = Version(ver[:-2])
                ok = self._prefix_match(v, spec)
            else:
                spec = Version(ver)
                cand = v
                if spec.local is None:
                    cand = Version(v.public)
                ok = cand == spec
            return ok if op == "==" else not ok
        spec = Version(ver)
        if op == "~=":
            rel = list(spec.release[:-1])
            prefix = Version(("" if spec.epoch == 0 else str(spec.epoch) + "!")
                             + ".".join([str(x) for x in rel]))
            return Version(v.public) >= spec and self._prefix_match(v, prefix)
        if op == "<=":
            return Version(v.public) <= spec
        if op == ">=":
            return Version(v.public) >= spec
        if op == "<":
            if not v < spec:
                return False
            if not spec.is_prerelease and v.is_prerelease and Version(v.base_version) == Version(spec.base_version):
                return False
            return True
        if op == ">":
            if not v > spec:
                return False
            if not spec.is_postrelease and v.is_postrelease and Version(v.base_version) == Version(spec.base_version):
                return False
            if v.local is not None and Version(v.base_version) == Version(spec.base_version):
                return False
            return True
        return False

    @staticmethod
    def _prefix_match(v, spec):
        if v.epoch != spec.epoch:
            return False
        a = list(v.release)
        b = list(spec.release)
        while len(a) < len(b):
            a.append(0)
        return a[:len(b)] == b

    def filter(self, iterable, prereleases=None):
        out = []
        for item in iterable:
            if self.contains(item, prereleases=prereleases):
                out.append(item)
        return iter(out)


class SpecifierSet:

    def __init__(self, specifiers="", prereleases=None):
        parsed = []
        if isinstance(specifiers, str):
            for part in specifiers.split(","):
                part = part.strip()
                if part:
                    parsed.append(Specifier(part))
        else:
            for sp in specifiers:
                parsed.append(sp if isinstance(sp, Specifier) else Specifier(str(sp)))
        self._specs = parsed
        self._prereleases = prereleases

    @property
    def prereleases(self):
        if self._prereleases is not None:
            return self._prereleases
        if not self._specs:
            return None
        for s in self._specs:
            if s.prereleases:
                return True
        return None

    @prereleases.setter
    def prereleases(self, value):
        self._prereleases = value

    def __str__(self):
        return ",".join(sorted([str(s) for s in self._specs]))

    def __repr__(self):
        pre = ""
        if self._prereleases is not None:
            pre = ", prereleases=" + repr(self._prereleases)
        return "<SpecifierSet(" + repr(str(self)) + pre + ")>"

    def __hash__(self):
        return hash(frozenset(self._specs))

    def __and__(self, other):
        if isinstance(other, str):
            other = SpecifierSet(other)
        if not isinstance(other, SpecifierSet):
            return NotImplemented
        combined = SpecifierSet()
        combined._specs = list(self._specs) + list(other._specs)
        if self._prereleases is None and other._prereleases is not None:
            combined._prereleases = other._prereleases
        else:
            combined._prereleases = self._prereleases
        return combined

    def __eq__(self, other):
        if isinstance(other, (str, Specifier)):
            other = SpecifierSet(str(other))
        if not isinstance(other, SpecifierSet):
            return NotImplemented
        return set(self._specs) == set(other._specs)

    def __ne__(self, other):
        r = self.__eq__(other)
        if r is NotImplemented:
            return r
        return not r

    def __len__(self):
        return len(self._specs)

    def __iter__(self):
        return iter(self._specs)

    def __contains__(self, item):
        return self.contains(item)

    def contains(self, item, prereleases=None, installed=None):
        if not self._specs:
            return True
        v = item
        if not isinstance(v, Version):
            try:
                v = Version(item)
            except InvalidVersion:
                return False
        if installed and v.is_prerelease:
            item = Version(v.base_version)
            v = item
        if prereleases is None:
            prereleases = self._prereleases
        if prereleases is False and v.is_prerelease:
            return False
        for s in self._specs:
            if not s.contains(v, prereleases=True):
                return False
        return True

    def filter(self, iterable, prereleases=None):
        if prereleases is None:
            prereleases = self.prereleases
        found = []
        pres = []
        for item in iterable:
            try:
                v = _to_version(item)
            except InvalidVersion:
                continue
            ok = True
            for s in self._specs:
                if not s.contains(v, prereleases=True):
                    ok = False
                    break
            if not ok:
                continue
            if v.is_prerelease and not prereleases:
                pres.append(item)
            else:
                found.append(item)
        if not found and prereleases is None:
            return iter(pres)
        return iter(found)
