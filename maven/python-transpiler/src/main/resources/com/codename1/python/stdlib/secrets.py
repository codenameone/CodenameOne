# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""Random numbers suitable for secrets: tokens, passwords, keys."""

import base64
import random

__all__ = ["choice", "randbelow", "randbits", "SystemRandom", "token_bytes", "token_hex",
           "token_urlsafe", "compare_digest"]

SystemRandom = random.SystemRandom
_sysrand = SystemRandom()
randbits = _sysrand.getrandbits
choice = _sysrand.choice

DEFAULT_ENTROPY = 32


def randbelow(exclusive_upper_bound):
    if exclusive_upper_bound <= 0:
        raise ValueError("Upper bound must be positive.")
    return _sysrand._randbelow(exclusive_upper_bound)


def token_bytes(nbytes=None):
    if nbytes is None:
        nbytes = DEFAULT_ENTROPY
    return _sysrand.randbytes(nbytes)


def token_hex(nbytes=None):
    return token_bytes(nbytes).hex()


def token_urlsafe(nbytes=None):
    tok = token_bytes(nbytes)
    return base64.urlsafe_b64encode(tok).rstrip(b"=").decode("ascii")


def compare_digest(a, b):
    if isinstance(a, str) and isinstance(b, str):
        try:
            a = a.encode("ascii")
            b = b.encode("ascii")
        except UnicodeEncodeError:
            raise TypeError("comparing strings with non-ASCII characters is not supported")
    elif not (isinstance(a, (bytes, bytearray, memoryview)) and isinstance(b, (bytes, bytearray, memoryview))):
        raise TypeError("unsupported operand types(s) or combination of types: '%s' and '%s'"
                        % (type(a).__name__, type(b).__name__))
    a = bytes(a)
    b = bytes(b)
    result = len(a) ^ len(b)
    if len(a) != len(b):
        b = a
    for x, y in zip(a, b):
        result |= x ^ y
    return result == 0
