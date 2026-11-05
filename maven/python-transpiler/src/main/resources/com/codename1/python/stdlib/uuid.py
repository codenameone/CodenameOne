# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""UUID objects (RFC 9562) and the functions that make each version.

There is no hardware address to read, so getnode() answers a random
48-bit number with the multicast bit set, as the RFC asks of a host
without one.
"""

import os
import time

__all__ = ["UUID", "SafeUUID", "getnode", "uuid1", "uuid3", "uuid4", "uuid5", "uuid6", "uuid7", "uuid8",
           "NAMESPACE_DNS", "NAMESPACE_URL", "NAMESPACE_OID", "NAMESPACE_X500", "NIL", "MAX",
           "RESERVED_NCS", "RFC_4122", "RESERVED_MICROSOFT", "RESERVED_FUTURE"]

RESERVED_NCS, RFC_4122, RESERVED_MICROSOFT, RESERVED_FUTURE = [
    "reserved for NCS compatibility", "specified in RFC 4122",
    "reserved for Microsoft compatibility", "reserved for future definition"]

_UINT_128_MAX = (1 << 128) - 1
_RFC_4122_CLEARFLAGS_MASK = ~((0xf000 << 64) | (0xc000 << 48))
_RFC_4122_VERSION_1_FLAGS = ((1 << 76) | (0x8000 << 48))
_RFC_4122_VERSION_3_FLAGS = ((3 << 76) | (0x8000 << 48))
_RFC_4122_VERSION_4_FLAGS = ((4 << 76) | (0x8000 << 48))
_RFC_4122_VERSION_5_FLAGS = ((5 << 76) | (0x8000 << 48))
_RFC_4122_VERSION_6_FLAGS = ((6 << 76) | (0x8000 << 48))
_RFC_4122_VERSION_7_FLAGS = ((7 << 76) | (0x8000 << 48))
_RFC_4122_VERSION_8_FLAGS = ((8 << 76) | (0x8000 << 48))


class SafeUUID:
    safe = 0
    unsafe = -1
    unknown = None


class UUID:
    __slots__ = ("int", "is_safe", "__weakref__")

    def __init__(self, hex=None, bytes=None, bytes_le=None, fields=None, int=None, version=None, *,
                 is_safe=SafeUUID.unknown):
        if [hex, bytes, bytes_le, fields, int].count(None) != 4:
            raise TypeError("one of the hex, bytes, bytes_le, fields, or int arguments must be given")
        if hex is not None:
            hex = hex.replace("urn:", "").replace("uuid:", "")
            hex = hex.strip("{}").replace("-", "")
            if len(hex) != 32:
                raise ValueError("badly formed hexadecimal UUID string")
            int = _int(hex, 16)
        elif bytes_le is not None:
            if len(bytes_le) != 16:
                raise ValueError("bytes_le is not a 16-char string")
            bytes = (bytes_le[4 - 1::-1] + bytes_le[6 - 1:4 - 1:-1] + bytes_le[8 - 1:6 - 1:-1] + bytes_le[8:])
        if bytes is not None:
            if len(bytes) != 16:
                raise ValueError("bytes is not a 16-char string")
            int = _int.from_bytes(bytes, "big")
        elif fields is not None:
            if len(fields) != 6:
                raise ValueError("fields is not a 6-tuple")
            (time_low, time_mid, time_hi_version, clock_seq_hi_variant, clock_seq_low, node) = fields
            if not 0 <= time_low < (1 << 32):
                raise ValueError("field 1 out of range (need a 32-bit value)")
            if not 0 <= time_mid < (1 << 16):
                raise ValueError("field 2 out of range (need a 16-bit value)")
            if not 0 <= time_hi_version < (1 << 16):
                raise ValueError("field 3 out of range (need a 16-bit value)")
            if not 0 <= clock_seq_hi_variant < (1 << 8):
                raise ValueError("field 4 out of range (need an 8-bit value)")
            if not 0 <= clock_seq_low < (1 << 8):
                raise ValueError("field 5 out of range (need an 8-bit value)")
            if not 0 <= node < (1 << 48):
                raise ValueError("field 6 out of range (need a 48-bit value)")
            clock_seq = (clock_seq_hi_variant << 8) | clock_seq_low
            int = ((time_low << 96) | (time_mid << 80) | (time_hi_version << 64) | (clock_seq << 48) | node)
        if not 0 <= int <= _UINT_128_MAX:
            raise ValueError("int is out of range (need a 128-bit value)")
        if version is not None:
            if not 1 <= version <= 8:
                raise ValueError("illegal version number")
            int &= _RFC_4122_CLEARFLAGS_MASK
            int |= 0x8000 << 48
            int |= version << 76
        object.__setattr__(self, "int", int)
        object.__setattr__(self, "is_safe", is_safe)

    @classmethod
    def _from_int(cls, value):
        self = object.__new__(cls)
        object.__setattr__(self, "int", value)
        object.__setattr__(self, "is_safe", SafeUUID.unknown)
        return self

    def __getstate__(self):
        d = {"int": self.int}
        if self.is_safe != SafeUUID.unknown:
            d["is_safe"] = self.is_safe
        return d

    def __eq__(self, other):
        if isinstance(other, UUID):
            return self.int == other.int
        return NotImplemented

    def __lt__(self, other):
        if isinstance(other, UUID):
            return self.int < other.int
        return NotImplemented

    def __gt__(self, other):
        if isinstance(other, UUID):
            return self.int > other.int
        return NotImplemented

    def __le__(self, other):
        if isinstance(other, UUID):
            return self.int <= other.int
        return NotImplemented

    def __ge__(self, other):
        if isinstance(other, UUID):
            return self.int >= other.int
        return NotImplemented

    def __hash__(self):
        return hash(self.int)

    def __int__(self):
        return self.int

    def __repr__(self):
        return "%s(%r)" % (self.__class__.__name__, str(self))

    def __setattr__(self, name, value):
        raise TypeError("UUID objects are immutable")

    def __str__(self):
        x = self.hex
        return "%s-%s-%s-%s-%s" % (x[:8], x[8:12], x[12:16], x[16:20], x[20:])

    @property
    def bytes(self):
        return self.int.to_bytes(16, "big")

    @property
    def bytes_le(self):
        bytes = self.bytes
        return (bytes[4 - 1::-1] + bytes[6 - 1:4 - 1:-1] + bytes[8 - 1:6 - 1:-1] + bytes[8:])

    @property
    def fields(self):
        return (self.time_low, self.time_mid, self.time_hi_version, self.clock_seq_hi_variant, self.clock_seq_low,
                self.node)

    @property
    def time_low(self):
        return self.int >> 96

    @property
    def time_mid(self):
        return (self.int >> 80) & 0xffff

    @property
    def time_hi_version(self):
        return (self.int >> 64) & 0xffff

    @property
    def clock_seq_hi_variant(self):
        return (self.int >> 56) & 0xff

    @property
    def clock_seq_low(self):
        return (self.int >> 48) & 0xff

    @property
    def time(self):
        if self.version == 6:
            time_hi = self.int >> 96
            time_lo = (self.int >> 64) & 0x0fff
            return time_hi << 28 | (self.time_mid << 12) | time_lo
        elif self.version == 7:
            return self.int >> 80
        else:
            time_hi = self.time_hi_version & 0x0fff
            time_lo = self.int >> 96
            return time_hi << 48 | (self.time_mid << 32) | time_lo

    @property
    def clock_seq(self):
        return (((self.clock_seq_hi_variant & 0x3f) << 8) | self.clock_seq_low)

    @property
    def node(self):
        return self.int & 0xffffffffffff

    @property
    def hex(self):
        return "%032x" % self.int

    @property
    def urn(self):
        return "urn:uuid:" + str(self)

    @property
    def variant(self):
        if not self.int & (0x8000 << 48):
            return RESERVED_NCS
        elif not self.int & (0x4000 << 48):
            return RFC_4122
        elif not self.int & (0x2000 << 48):
            return RESERVED_MICROSOFT
        else:
            return RESERVED_FUTURE

    @property
    def version(self):
        if self.variant == RFC_4122:
            return int((self.int >> 76) & 0xf)


_int = int

_node = None


def getnode():
    global _node
    if _node is None:
        _node = _int.from_bytes(os.urandom(6), "big") | (1 << 40)
    return _node


_last_timestamp = None


def uuid1(node=None, clock_seq=None):
    global _last_timestamp
    nanoseconds = time.time_ns()
    timestamp = nanoseconds // 100 + 0x01b21dd213814000
    if _last_timestamp is not None and timestamp <= _last_timestamp:
        timestamp = _last_timestamp + 1
    _last_timestamp = timestamp
    if clock_seq is None:
        clock_seq = _int.from_bytes(os.urandom(2), "big") & 0x3fff
    time_low = timestamp & 0xffffffff
    time_mid = (timestamp >> 32) & 0xffff
    time_hi_version = (timestamp >> 48) & 0x0fff
    clock_seq_low = clock_seq & 0xff
    clock_seq_hi_variant = (clock_seq >> 8) & 0x3f
    if node is None:
        node = getnode()
    return UUID(fields=(time_low, time_mid, time_hi_version, clock_seq_hi_variant, clock_seq_low, node),
                version=1)


def uuid3(namespace, name):
    if isinstance(name, str):
        name = bytes(name, "utf-8")
    import hashlib
    h = hashlib.md5(namespace.bytes + name, usedforsecurity=False)
    int_uuid_3 = _int.from_bytes(h.digest())
    int_uuid_3 &= _RFC_4122_CLEARFLAGS_MASK
    int_uuid_3 |= _RFC_4122_VERSION_3_FLAGS
    return UUID._from_int(int_uuid_3)


def uuid4():
    int_uuid_4 = _int.from_bytes(os.urandom(16))
    int_uuid_4 &= _RFC_4122_CLEARFLAGS_MASK
    int_uuid_4 |= _RFC_4122_VERSION_4_FLAGS
    return UUID._from_int(int_uuid_4)


def uuid5(namespace, name):
    if isinstance(name, str):
        name = bytes(name, "utf-8")
    import hashlib
    h = hashlib.sha1(namespace.bytes + name, usedforsecurity=False)
    int_uuid_5 = _int.from_bytes(h.digest()[:16])
    int_uuid_5 &= _RFC_4122_CLEARFLAGS_MASK
    int_uuid_5 |= _RFC_4122_VERSION_5_FLAGS
    return UUID._from_int(int_uuid_5)


_last_timestamp_v6 = None


def uuid6(node=None, clock_seq=None):
    global _last_timestamp_v6
    nanoseconds = time.time_ns()
    timestamp = nanoseconds // 100 + 0x01b21dd213814000
    if _last_timestamp_v6 is not None and timestamp <= _last_timestamp_v6:
        timestamp = _last_timestamp_v6 + 1
    _last_timestamp_v6 = timestamp
    if clock_seq is None:
        clock_seq = _int.from_bytes(os.urandom(2), "big") & 0x3fff
    time_hi_and_mid = (timestamp >> 12) & 0xffff_ffff_ffff
    time_lo = timestamp & 0x0fff
    clock_s = clock_seq & 0x3fff
    if node is None:
        node = _int.from_bytes(os.urandom(6), "big")
    int_uuid_6 = time_hi_and_mid << 80
    int_uuid_6 |= time_lo << 64
    int_uuid_6 |= clock_s << 48
    int_uuid_6 |= node & 0xffff_ffff_ffff
    int_uuid_6 |= _RFC_4122_VERSION_6_FLAGS
    return UUID._from_int(int_uuid_6)


_last_timestamp_v7 = None
_last_counter_v7 = 0


def _uuid7_get_counter_and_tail():
    rand = _int.from_bytes(os.urandom(10))
    counter = (rand >> 32) & 0x1ff_ffff_ffff
    tail = rand & 0xffff_ffff
    return counter, tail


def uuid7():
    global _last_timestamp_v7, _last_counter_v7
    nanoseconds = time.time_ns()
    timestamp_ms = nanoseconds // 1_000_000
    if _last_timestamp_v7 is None or timestamp_ms > _last_timestamp_v7:
        counter, tail = _uuid7_get_counter_and_tail()
    else:
        if timestamp_ms < _last_timestamp_v7:
            timestamp_ms = _last_timestamp_v7 + 1
        counter = _last_counter_v7 + 1
        if counter > 0x3ff_ffff_ffff:
            timestamp_ms += 1
            counter, tail = _uuid7_get_counter_and_tail()
        else:
            tail = _int.from_bytes(os.urandom(4))
    unix_ts_ms = timestamp_ms & 0xffff_ffff_ffff
    counter_msbs = counter >> 30
    counter_hi = counter_msbs & 0x0fff
    counter_lo = counter & 0x3fff_ffff
    tail &= 0xffff_ffff
    int_uuid_7 = unix_ts_ms << 80
    int_uuid_7 |= counter_hi << 64
    int_uuid_7 |= counter_lo << 32
    int_uuid_7 |= tail
    int_uuid_7 |= _RFC_4122_VERSION_7_FLAGS
    _last_timestamp_v7 = timestamp_ms
    _last_counter_v7 = counter
    return UUID._from_int(int_uuid_7)


def uuid8(a=None, b=None, c=None):
    if a is None:
        import random
        a = random.getrandbits(48)
    if b is None:
        import random
        b = random.getrandbits(12)
    if c is None:
        import random
        c = random.getrandbits(62)
    int_uuid_8 = (a & 0xffff_ffff_ffff) << 80
    int_uuid_8 |= (b & 0xfff) << 64
    int_uuid_8 |= c & 0x3fff_ffff_ffff_ffff
    int_uuid_8 |= _RFC_4122_VERSION_8_FLAGS
    return UUID._from_int(int_uuid_8)


NAMESPACE_DNS = UUID("6ba7b810-9dad-11d1-80b4-00c04fd430c8")
NAMESPACE_URL = UUID("6ba7b811-9dad-11d1-80b4-00c04fd430c8")
NAMESPACE_OID = UUID("6ba7b812-9dad-11d1-80b4-00c04fd430c8")
NAMESPACE_X500 = UUID("6ba7b814-9dad-11d1-80b4-00c04fd430c8")
NIL = UUID("00000000-0000-0000-0000-000000000000")
MAX = UUID("ffffffff-ffff-ffff-ffff-ffffffffffff")
