# Copyright (c) 2001 Python Software Foundation; All Rights Reserved
# Licensed under the Python Software Foundation License Version 2; the full
# text is in maven/python-transpiler/third-party/cpython/LICENSE.txt.
#
# Derived from CPython's Lib/ipaddress.py. Changes: rewritten for the Codename One
# Python runtime, trimmed to what the transpiler supports and reorganised;
# the C accelerators and platform-specific branches are left out.
"""ipaddress: IPv4 and IPv6 addresses, networks and interfaces -- parsing,
comparison, membership, packing and the common address classes."""

import functools as _functools

IPV4LENGTH = 32
IPV6LENGTH = 128


class AddressValueError(ValueError):
    pass


class NetmaskValueError(ValueError):
    pass


def _parse_v4(s):
    parts = s.split(".")
    if len(parts) != 4:
        raise AddressValueError("Expected 4 octets in %r" % (s,))
    n = 0
    for p in parts:
        if not p or not p.isdigit() or len(p) > 3 or (len(p) > 1 and p[0] == "0"):
            raise AddressValueError("Invalid octet %r in %r" % (p, s))
        v = int(p)
        if v > 255:
            raise AddressValueError("Octet %d (> 255) not permitted in %r" % (v, s))
        n = (n << 8) | v
    return n


def _parse_v6(s):
    if "%" in s:
        s = s.split("%", 1)[0]
    tail = []
    if "." in s:
        head, _, v4 = s.rpartition(":")
        n4 = _parse_v4(v4)
        s = head + ":" + "%x:%x" % (n4 >> 16, n4 & 0xFFFF)
    if s.count("::") > 1:
        raise AddressValueError("At most one '::' permitted in %r" % (s,))
    if "::" in s:
        a, b = s.split("::")
        left = a.split(":") if a else []
        right = b.split(":") if b else []
        missing = 8 - len(left) - len(right)
        if missing < 1:
            raise AddressValueError("Too many parts in %r" % (s,))
        groups = left + ["0"] * missing + right
    else:
        groups = s.split(":")
    if len(groups) != 8:
        raise AddressValueError("Exactly 8 parts expected in %r" % (s,))
    n = 0
    for g in groups:
        if not g or len(g) > 4:
            raise AddressValueError("Invalid part %r in %r" % (g, s))
        try:
            v = int(g, 16)
        except ValueError:
            raise AddressValueError("Invalid part %r in %r" % (g, s))
        n = (n << 16) | v
    return n


@_functools.total_ordering
class _BaseAddress:
    __slots__ = ()

    def __int__(self):
        return self._ip

    def __index__(self):
        return self._ip

    def __eq__(self, other):
        return type(other) is type(self) and other._ip == self._ip

    def __lt__(self, other):
        if not isinstance(other, _BaseAddress) or other.version != self.version:
            return NotImplemented
        return self._ip < other._ip

    def __hash__(self):
        return hash((self.version, self._ip))

    def __add__(self, n):
        return type(self)(self._ip + int(n))

    def __sub__(self, n):
        return type(self)(self._ip - int(n))

    def __repr__(self):
        return "%s(%r)" % (type(self).__name__, str(self))

    def __format__(self, spec):
        return format(str(self), spec)

    @property
    def packed(self):
        return self._ip.to_bytes(self.max_prefixlen // 8, "big")

    def _in(self, nets):
        for n in nets:
            if self in ip_network(n):
                return True
        return False


class IPv4Address(_BaseAddress):
    __slots__ = ("_ip",)
    version = 4
    max_prefixlen = IPV4LENGTH

    def __init__(self, address):
        if isinstance(address, int):
            if not 0 <= address < 1 << 32:
                raise AddressValueError("%d out of range" % address)
            self._ip = address
        elif isinstance(address, (bytes, bytearray)):
            if len(address) != 4:
                raise AddressValueError("%r (len %d != 4) is not permitted as an IPv4 address"
                                        % (address, len(address)))
            self._ip = int.from_bytes(bytes(address), "big")
        else:
            s = str(address)
            if "/" in s:
                raise AddressValueError("Unexpected '/' in %r" % (s,))
            self._ip = _parse_v4(s)

    def __str__(self):
        n = self._ip
        return "%d.%d.%d.%d" % ((n >> 24) & 255, (n >> 16) & 255, (n >> 8) & 255, n & 255)

    @property
    def is_private(self):
        return self._in(("0.0.0.0/8", "10.0.0.0/8", "127.0.0.0/8", "169.254.0.0/16", "172.16.0.0/12",
                         "192.0.0.0/29", "192.0.0.170/31", "192.0.2.0/24", "192.168.0.0/16",
                         "198.18.0.0/15", "198.51.100.0/24", "203.0.113.0/24", "240.0.0.0/4",
                         "255.255.255.255/32"))

    @property
    def is_global(self):
        return not self.is_private and not self._in(("100.64.0.0/10",))

    @property
    def is_loopback(self):
        return self._in(("127.0.0.0/8",))

    @property
    def is_link_local(self):
        return self._in(("169.254.0.0/16",))

    @property
    def is_multicast(self):
        return self._in(("224.0.0.0/4",))

    @property
    def is_reserved(self):
        return self._in(("240.0.0.0/4",))

    @property
    def is_unspecified(self):
        return self._ip == 0

    @property
    def reverse_pointer(self):
        return ".".join(reversed(str(self).split("."))) + ".in-addr.arpa"


class IPv6Address(_BaseAddress):
    __slots__ = ("_ip", "_scope")
    version = 6
    max_prefixlen = IPV6LENGTH

    def __init__(self, address):
        self._scope = None
        if isinstance(address, int):
            if not 0 <= address < 1 << 128:
                raise AddressValueError("%d out of range" % address)
            self._ip = address
        elif isinstance(address, (bytes, bytearray)):
            if len(address) != 16:
                raise AddressValueError("%r (len %d != 16) is not permitted as an IPv6 address"
                                        % (address, len(address)))
            self._ip = int.from_bytes(bytes(address), "big")
        else:
            s = str(address)
            if "/" in s:
                raise AddressValueError("Unexpected '/' in %r" % (s,))
            if "%" in s:
                s, self._scope = s.split("%", 1)
            self._ip = _parse_v6(s)

    def __str__(self):
        if (self._ip >> 32) == 0xFFFF:
            return "::ffff:" + str(IPv4Address(self._ip & 0xFFFFFFFF)) + ("%" + self._scope if self._scope else "")
        groups = [(self._ip >> (16 * (7 - i))) & 0xFFFF for i in range(8)]
        best, best_len, cur, cur_len = -1, 0, -1, 0
        for i, g in enumerate(groups):
            if g == 0:
                if cur < 0:
                    cur, cur_len = i, 1
                else:
                    cur_len += 1
                if cur_len > best_len:
                    best, best_len = cur, cur_len
            else:
                cur, cur_len = -1, 0
        hexes = ["%x" % g for g in groups]
        if best_len > 1:
            hexes[best:best + best_len] = [""]
            if best == 0:
                hexes.insert(0, "")
            if best + best_len == 8:
                hexes.append("")
        s = ":".join(hexes)
        return s + ("%" + self._scope if self._scope else "")

    @property
    def scope_id(self):
        return self._scope

    @property
    def ipv4_mapped(self):
        if (self._ip >> 32) != 0xFFFF:
            return None
        return IPv4Address(self._ip & 0xFFFFFFFF)

    @property
    def is_loopback(self):
        return self._ip == 1

    @property
    def is_unspecified(self):
        return self._ip == 0

    @property
    def is_link_local(self):
        return self._in(("fe80::/10",))

    @property
    def is_multicast(self):
        return self._in(("ff00::/8",))

    @property
    def is_private(self):
        m = self.ipv4_mapped
        if m is not None:
            return m.is_private
        return self._in(("::1/128", "::/128", "::ffff:0:0/96", "100::/64", "2001::/23", "2001:db8::/32",
                         "2001:10::/28", "fc00::/7", "fe80::/10"))

    @property
    def is_global(self):
        return not self.is_private

    @property
    def is_reserved(self):
        return self._in(("::/8", "100::/8", "200::/7", "400::/6", "800::/5", "1000::/4", "4000::/3",
                         "6000::/3", "8000::/3", "a000::/3", "c000::/3", "e000::/4", "f000::/5",
                         "f800::/6", "fe00::/9"))

    @property
    def is_site_local(self):
        return self._in(("fec0::/10",))


@_functools.total_ordering
class _BaseNetwork:
    def __init__(self, address, strict=True):
        if isinstance(address, tuple):
            addr, prefix = address
            addr = self._address_class(addr)
            prefix = str(prefix)
        else:
            s = str(address) if not isinstance(address, (int, bytes, bytearray)) else None
            if s is None:
                addr, prefix = self._address_class(address), str(self._address_class.max_prefixlen)
            else:
                a, _, prefix = s.partition("/")
                addr = self._address_class(a)
                prefix = prefix or str(self._address_class.max_prefixlen)
        bits = self._address_class.max_prefixlen
        if prefix.isdigit():
            plen = int(prefix)
        elif self._address_class.version == 4:
            # A netmask (contiguous ones from the top) or a hostmask
            # (contiguous ones from the bottom), as CPython accepts.
            m = _parse_v4(prefix)
            inv = m ^ ((1 << 32) - 1)
            if (inv & (inv + 1)) == 0:
                plen = bits - inv.bit_length()
            elif (m & (m + 1)) == 0:
                plen = bits - m.bit_length()
            else:
                raise NetmaskValueError("%r is not a valid netmask" % (prefix,))
        else:
            raise NetmaskValueError("%r is not a valid netmask" % (prefix,))
        if not 0 <= plen <= bits:
            raise NetmaskValueError("%r is not a valid netmask" % (prefix,))
        self._prefixlen = plen
        mask = ((1 << bits) - 1) ^ ((1 << (bits - plen)) - 1)
        net = int(addr) & mask
        if strict and net != int(addr):
            raise ValueError("%s has host bits set" % (address,))
        self._net = net
        self._mask = mask

    @property
    def network_address(self):
        return self._address_class(self._net)

    @property
    def broadcast_address(self):
        bits = self._address_class.max_prefixlen
        return self._address_class(self._net | (((1 << bits) - 1) ^ self._mask))

    @property
    def netmask(self):
        return self._address_class(self._mask)

    @property
    def hostmask(self):
        bits = self._address_class.max_prefixlen
        return self._address_class(((1 << bits) - 1) ^ self._mask)

    @property
    def prefixlen(self):
        return self._prefixlen

    @property
    def num_addresses(self):
        return 1 << (self._address_class.max_prefixlen - self._prefixlen)

    @property
    def with_prefixlen(self):
        return "%s/%d" % (self.network_address, self._prefixlen)

    @property
    def with_netmask(self):
        return "%s/%s" % (self.network_address, self.netmask)

    def __contains__(self, other):
        if not isinstance(other, _BaseAddress) or other.version != self.version:
            return False
        return (int(other) & self._mask) == self._net

    def overlaps(self, other):
        return self.network_address in other or other.network_address in self

    def subnet_of(self, other):
        return self._prefixlen >= other._prefixlen and self.network_address in other

    def supernet_of(self, other):
        return other.subnet_of(self)

    def __iter__(self):
        for i in range(self.num_addresses):
            yield self._address_class(self._net + i)

    def hosts(self):
        n = self.num_addresses
        if n <= 2:
            return iter(self)
        return (self._address_class(self._net + i) for i in range(1, n - 1))

    def __getitem__(self, i):
        n = self.num_addresses
        if i < 0:
            i += n
        if not 0 <= i < n:
            raise IndexError("address out of range")
        return self._address_class(self._net + i)

    def __eq__(self, other):
        return type(other) is type(self) and (self._net, self._prefixlen) == (other._net, other._prefixlen)

    def __lt__(self, other):
        if type(other) is not type(self):
            return NotImplemented
        return (self._net, self._prefixlen) < (other._net, other._prefixlen)

    def __hash__(self):
        return hash((self.version, self._net, self._prefixlen))

    def __str__(self):
        return self.with_prefixlen

    def __repr__(self):
        return "%s(%r)" % (type(self).__name__, str(self))

    @property
    def is_private(self):
        return self.network_address.is_private and self.broadcast_address.is_private

    @property
    def is_loopback(self):
        return self.network_address.is_loopback and self.broadcast_address.is_loopback

    @property
    def is_global(self):
        return not self.is_private


class IPv4Network(_BaseNetwork):
    _address_class = IPv4Address
    version = 4
    max_prefixlen = IPV4LENGTH


class IPv6Network(_BaseNetwork):
    _address_class = IPv6Address
    version = 6
    max_prefixlen = IPV6LENGTH


class _BaseInterface:
    def __init__(self, address):
        s = str(address) if not isinstance(address, tuple) else "%s/%s" % address
        a, _, p = s.partition("/")
        self._addr = self._address_class(a)
        self.network = self._network_class(s, strict=False)

    @property
    def ip(self):
        return self._addr

    def __str__(self):
        return "%s/%d" % (self._addr, self.network.prefixlen)

    def __repr__(self):
        return "%s(%r)" % (type(self).__name__, str(self))

    def __eq__(self, other):
        return type(other) is type(self) and str(other) == str(self)

    def __hash__(self):
        return hash(str(self))


class IPv4Interface(_BaseInterface):
    _address_class = IPv4Address
    _network_class = IPv4Network
    version = 4


class IPv6Interface(_BaseInterface):
    _address_class = IPv6Address
    _network_class = IPv6Network
    version = 6


def _either(address, kinds, what):
    for k in kinds:
        try:
            return k(address)
        except (AddressValueError, NetmaskValueError):
            pass
    raise ValueError("%r does not appear to be an IPv4 or IPv6 %s" % (address, what))


def ip_address(address):
    return _either(address, (IPv4Address, IPv6Address), "address")


def ip_network(address, strict=True):
    for k in (IPv4Network, IPv6Network):
        try:
            return k(address, strict)
        except (AddressValueError, NetmaskValueError):
            pass
    raise ValueError("%r does not appear to be an IPv4 or IPv6 network" % (address,))


def ip_interface(address):
    return _either(address, (IPv4Interface, IPv6Interface), "interface")


def v4_int_to_packed(address):
    return int(address).to_bytes(4, "big")


def v6_int_to_packed(address):
    return int(address).to_bytes(16, "big")
