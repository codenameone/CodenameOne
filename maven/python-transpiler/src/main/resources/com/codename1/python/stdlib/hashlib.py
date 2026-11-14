# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""Message digests: MD5 (RFC 1321), SHA-1 and the SHA-2 family (FIPS 180-4) and
BLAKE2 (RFC 7693), computed by the runtime's _cn1hash."""

__all__ = ["md5", "sha1", "sha224", "sha256", "sha384", "sha512", "blake2b", "blake2s", "new", "algorithms_available",
           "algorithms_guaranteed", "pbkdf2_hmac", "file_digest"]

algorithms_guaranteed = frozenset(["md5", "sha1", "sha224", "sha256", "sha384", "sha512", "blake2b",
                                   "blake2s"])
algorithms_available = algorithms_guaranteed

import _cn1hash


def _as_bytes(data):
    if isinstance(data, str):
        raise TypeError("Strings must be encoded before hashing")
    return bytes(data)


class _Hash:
    """A digest whose compression function runs in the runtime (_cn1hash)."""

    def __init__(self, data=b"", *, usedforsecurity=True):
        self._s = _cn1hash.new(self.name, 0, b"", b"", b"")
        if data:
            self.update(data)

    def update(self, data):
        _cn1hash.update(self._s, _as_bytes(data))

    def copy(self):
        c = type(self).__new__(type(self))
        c._s = _cn1hash.copy(self._s)
        if "digest_size" in self.__dict__:
            c.digest_size = self.digest_size
        return c

    def digest(self):
        return _cn1hash.digest(self._s)

    def hexdigest(self):
        return self.digest().hex()


class _MD5(_Hash):
    name = "md5"
    digest_size = 16
    block_size = 64


class _SHA1(_Hash):
    name = "sha1"
    digest_size = 20
    block_size = 64


class _SHA224(_Hash):
    name = "sha224"
    digest_size = 28
    block_size = 64


class _SHA256(_Hash):
    name = "sha256"
    digest_size = 32
    block_size = 64


class _SHA384(_Hash):
    name = "sha384"
    digest_size = 48
    block_size = 128


class _SHA512(_Hash):
    name = "sha512"
    digest_size = 64
    block_size = 128


class _Blake2(_Hash):
    """BLAKE2 (RFC 7693), sequential mode: key, salt, person and digest size.
    The tree-hashing parameters are accepted at their defaults only."""

    def __init__(self, data=b"", *, digest_size=None, key=b"", salt=b"", person=b"", fanout=1, depth=1,
                 leaf_size=0, node_offset=0, node_depth=0, inner_size=0, last_node=False,
                 usedforsecurity=True):
        if digest_size is None:
            digest_size = self.MAX_DIGEST_SIZE
        if not 1 <= digest_size <= self.MAX_DIGEST_SIZE:
            raise ValueError("digest_size must be between 1 and %d bytes" % self.MAX_DIGEST_SIZE)
        key = _as_bytes(key)
        salt = _as_bytes(salt)
        person = _as_bytes(person)
        if len(key) > self.MAX_KEY_SIZE:
            raise ValueError("maximum key length is %d bytes" % self.MAX_KEY_SIZE)
        if len(salt) > self.SALT_SIZE:
            raise ValueError("maximum salt length is %d bytes" % self.SALT_SIZE)
        if len(person) > self.PERSON_SIZE:
            raise ValueError("maximum person length is %d bytes" % self.PERSON_SIZE)
        if (fanout, depth, leaf_size, node_offset, node_depth, inner_size, last_node) != (1, 1, 0, 0, 0, 0, False):
            raise ValueError("tree hashing is not supported")
        self.digest_size = digest_size
        self._s = _cn1hash.new(self.name, digest_size, key, salt, person)
        if data:
            self.update(data)


class blake2b(_Blake2):
    name = "blake2b"
    block_size = 128
    MAX_DIGEST_SIZE = 64
    MAX_KEY_SIZE = 64
    SALT_SIZE = 16
    PERSON_SIZE = 16


class blake2s(_Blake2):
    name = "blake2s"
    block_size = 64
    MAX_DIGEST_SIZE = 32
    MAX_KEY_SIZE = 32
    SALT_SIZE = 8
    PERSON_SIZE = 8


_BY_NAME = {"md5": _MD5, "sha1": _SHA1, "sha224": _SHA224, "sha256": _SHA256,
            "sha384": _SHA384, "sha512": _SHA512, "blake2b": blake2b,
            "blake2s": blake2s}


def md5(data=b"", *, usedforsecurity=True):
    return _MD5(data)


def sha1(data=b"", *, usedforsecurity=True):
    return _SHA1(data)


def sha224(data=b"", *, usedforsecurity=True):
    return _SHA224(data)


def sha256(data=b"", *, usedforsecurity=True):
    return _SHA256(data)


def sha384(data=b"", *, usedforsecurity=True):
    return _SHA384(data)


def sha512(data=b"", *, usedforsecurity=True):
    return _SHA512(data)


def new(name, data=b"", **kwargs):
    cls = _BY_NAME.get(name.lower().replace("-", "")) if isinstance(name, str) else None
    if cls is None:
        raise ValueError("unsupported hash type " + str(name))
    return cls(data, **kwargs) if kwargs else cls(data)


def file_digest(fileobj, digest, /):
    h = new(digest) if isinstance(digest, str) else digest()
    while True:
        chunk = fileobj.read(65536)
        if not chunk:
            return h
        h.update(chunk)


def pbkdf2_hmac(hash_name, password, salt, iterations, dklen=None):
    password = _as_bytes(password)
    salt = _as_bytes(salt)
    if iterations < 1:
        raise ValueError("iteration value must be greater than 0.")
    if dklen is None:
        dklen = new(hash_name).digest_size
    if dklen < 1:
        raise ValueError("key length must be greater than 0.")
    return _cn1hash.pbkdf2(hash_name, password, salt, iterations, dklen)
