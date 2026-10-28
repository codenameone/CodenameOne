# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""hmac (RFC 2104) over the hashlib in this runtime."""
import hashlib as _hashlib

__all__ = ["new", "compare_digest", "digest", "HMAC"]

digest_size = None


def _ctor(digestmod):
    if digestmod is None:
        raise TypeError("Missing required argument 'digestmod'.")
    if isinstance(digestmod, str):
        name = digestmod
        return lambda d=b"": _hashlib.new(name, d)
    if hasattr(digestmod, "new"):
        return digestmod.new
    return digestmod


def _bytes(b):
    if isinstance(b, str):
        raise TypeError("Strings must be encoded before hashing")
    return bytes(b)


class HMAC:
    def __init__(self, key, msg=None, digestmod=None):
        key = _bytes(key)
        ctor = _ctor(digestmod)
        self._ctor = ctor
        probe = ctor()
        bs = getattr(probe, "block_size", 64)
        self.block_size = bs
        self.digest_size = probe.digest_size
        if len(key) > bs:
            key = ctor(key).digest()
        key = key + b"\x00" * (bs - len(key))
        self._inner = ctor(bytes(b ^ 0x36 for b in key))
        self._outer = ctor(bytes(b ^ 0x5C for b in key))
        self.name = "hmac-" + probe.name
        if msg is not None:
            self.update(msg)

    def update(self, msg):
        self._inner.update(_bytes(msg))

    def copy(self):
        c = HMAC.__new__(HMAC)
        c.__dict__.update(self.__dict__)
        c._inner = self._inner.copy()
        c._outer = self._outer.copy()
        return c

    def digest(self):
        o = self._outer.copy()
        o.update(self._inner.copy().digest())
        return o.digest()

    def hexdigest(self):
        return self.digest().hex()


def new(key, msg=None, digestmod=None):
    return HMAC(key, msg, digestmod)


def digest(key, msg, digest):
    return HMAC(key, msg, digest).digest()


def compare_digest(a, b):
    if isinstance(a, str) and isinstance(b, str):
        try:
            a = a.encode("ascii")
            b = b.encode("ascii")
        except UnicodeEncodeError:
            raise TypeError("comparing strings with non-ASCII characters is not supported")
    elif isinstance(a, str) or isinstance(b, str):
        raise TypeError("unsupported operand types(s) or combination of types")
    a = bytes(a)
    b = bytes(b)
    r = len(a) ^ len(b)
    if len(a) != len(b):
        b = a
    for x, y in zip(a, b):
        r |= x ^ y
    return r == 0
