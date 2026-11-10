# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""PEP 440 versions: parsing, normalization and ordering."""

__all__ = ["VERSION_PATTERN", "InvalidVersion", "Version", "parse"]

VERSION_PATTERN = r"v?(?:(?:[0-9]+)!)?[0-9]+(?:\.[0-9]+)*"

_DIGITS = "0123456789"
_SEPS = "-_."
_PRE_WORDS = (("alpha", "a"), ("beta", "b"), ("preview", "rc"), ("pre", "rc"),
              ("a", "a"), ("b", "b"), ("c", "rc"), ("rc", "rc"))
_POST_WORDS = ("post", "rev", "r")


class InvalidVersion(ValueError):
    """Raised when a version string is not a valid PEP 440 version."""


def parse(version):
    return Version(version)


def _digits_at(s, i):
    j = i
    while j < len(s) and s[j] in _DIGITS:
        j += 1
    return j


class Version:

    def __init__(self, version):
        parsed = self._parse(version)
        if parsed is None:
            raise InvalidVersion("Invalid version: " + repr(version))
        self._epoch = parsed[0]
        self._release = parsed[1]
        self._pre = parsed[2]
        self._post = parsed[3]
        self._dev = parsed[4]
        self._local = parsed[5]
        self._key = self._make_key()

    @staticmethod
    def _parse(text):
        s = text.strip().lower()
        i = 0
        n = len(s)
        if s[0:1] == "v":
            i = 1
        epoch = 0
        j = _digits_at(s, i)
        if j > i and s[j:j + 1] == "!":
            epoch = int(s[i:j])
            i = j + 1
        j = _digits_at(s, i)
        if j == i:
            return None
        release = [int(s[i:j])]
        i = j
        while s[i:i + 1] == "." and _digits_at(s, i + 1) > i + 1:
            j = _digits_at(s, i + 1)
            release.append(int(s[i + 1:j]))
            i = j
        pre = None
        post = None
        dev = None
        local = None
        # pre-release
        k = i
        if k < n and s[k] in _SEPS:
            k += 1
        for word, norm in _PRE_WORDS:
            if s.startswith(word, k):
                k2 = k + len(word)
                if k2 < n and s[k2] in _SEPS:
                    k3 = k2 + 1
                else:
                    k3 = k2
                j = _digits_at(s, k3)
                if j > k3:
                    pre = (norm, int(s[k3:j]))
                    i = j
                else:
                    pre = (norm, 0)
                    i = k2
                break
        # post-release
        if s[i:i + 1] == "-" and _digits_at(s, i + 1) > i + 1:
            j = _digits_at(s, i + 1)
            post = int(s[i + 1:j])
            i = j
        else:
            k = i
            if k < n and s[k] in _SEPS:
                k += 1
            for word in _POST_WORDS:
                if s.startswith(word, k):
                    k2 = k + len(word)
                    k3 = k2 + 1 if (k2 < n and s[k2] in _SEPS) else k2
                    j = _digits_at(s, k3)
                    if j > k3:
                        post = int(s[k3:j])
                        i = j
                    else:
                        post = 0
                        i = k2
                    break
        # dev release
        k = i
        if k < n and s[k] in _SEPS:
            k += 1
        if s.startswith("dev", k):
            k2 = k + 3
            k3 = k2 + 1 if (k2 < n and s[k2] in _SEPS) else k2
            j = _digits_at(s, k3)
            if j > k3:
                dev = int(s[k3:j])
                i = j
            else:
                dev = 0
                i = k2
        # local
        if s[i:i + 1] == "+":
            rest = s[i + 1:]
            if rest == "":
                return None
            parts = []
            cur = ""
            for ch in rest:
                if ch in _SEPS:
                    if cur == "":
                        return None
                    parts.append(cur)
                    cur = ""
                elif ch.isalnum() and ord(ch) < 128:
                    cur += ch
                else:
                    return None
            if cur == "":
                return None
            parts.append(cur)
            local = []
            for p in parts:
                if _digits_at(p, 0) == len(p):
                    local.append(int(p))
                else:
                    local.append(p)
            i = n
        if i != n:
            return None
        return (epoch, tuple(release), pre, post, dev, None if local is None else tuple(local))

    def _make_key(self):
        release = list(self._release)
        while len(release) > 1 and release[-1] == 0:
            release.pop()
        if self._pre is None and self._post is None and self._dev is not None:
            pre = (0, "", 0)
        elif self._pre is None:
            pre = (2, "", 0)
        else:
            pre = (1, self._pre[0], self._pre[1])
        post = (0, 0) if self._post is None else (1, self._post)
        dev = (1, 0) if self._dev is None else (0, self._dev)
        if self._local is None:
            local = (0,)
        else:
            items = []
            for p in self._local:
                if isinstance(p, int):
                    items.append((1, p, ""))
                else:
                    items.append((0, 0, p))
            local = (1, tuple(items))
        return (self._epoch, tuple(release), pre, post, dev, local)

    def __repr__(self):
        return "<Version(" + repr(str(self)) + ")>"

    def __str__(self):
        parts = []
        if self._epoch != 0:
            parts.append(str(self._epoch) + "!")
        parts.append(".".join([str(x) for x in self._release]))
        if self._pre is not None:
            parts.append(self._pre[0] + str(self._pre[1]))
        if self._post is not None:
            parts.append(".post" + str(self._post))
        if self._dev is not None:
            parts.append(".dev" + str(self._dev))
        if self._local is not None:
            parts.append("+" + self.local)
        return "".join(parts)

    @property
    def epoch(self):
        return self._epoch

    @property
    def release(self):
        return self._release

    @property
    def pre(self):
        return self._pre

    @property
    def post(self):
        return self._post

    @property
    def dev(self):
        return self._dev

    @property
    def local(self):
        if self._local is None:
            return None
        return ".".join([str(x) for x in self._local])

    @property
    def public(self):
        return str(self).split("+", 1)[0]

    @property
    def base_version(self):
        parts = []
        if self._epoch != 0:
            parts.append(str(self._epoch) + "!")
        parts.append(".".join([str(x) for x in self._release]))
        return "".join(parts)

    @property
    def is_prerelease(self):
        return self._dev is not None or self._pre is not None

    @property
    def is_postrelease(self):
        return self._post is not None

    @property
    def is_devrelease(self):
        return self._dev is not None

    @property
    def major(self):
        return self._release[0] if len(self._release) >= 1 else 0

    @property
    def minor(self):
        return self._release[1] if len(self._release) >= 2 else 0

    @property
    def micro(self):
        return self._release[2] if len(self._release) >= 3 else 0

    def __hash__(self):
        return hash(self._key)

    def __lt__(self, other):
        if not isinstance(other, Version):
            return NotImplemented
        return self._key < other._key

    def __le__(self, other):
        if not isinstance(other, Version):
            return NotImplemented
        return self._key <= other._key

    def __eq__(self, other):
        if not isinstance(other, Version):
            return NotImplemented
        return self._key == other._key

    def __ge__(self, other):
        if not isinstance(other, Version):
            return NotImplemented
        return self._key >= other._key

    def __gt__(self, other):
        if not isinstance(other, Version):
            return NotImplemented
        return self._key > other._key

    def __ne__(self, other):
        if not isinstance(other, Version):
            return NotImplemented
        return self._key != other._key
