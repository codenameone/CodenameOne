# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""socket: the names, the errors and host lookups; no raw sockets.

A Codename One application talks to the network through the platform's HTTP
and WebSocket stacks (urllib, http.client, requests, httpx), which handle
proxies, certificates and radio state the way the device expects. A socket
object can be created and configured, so code that builds one up front
imports and runs; connecting it raises OSError.
"""

import enum as _enum


class AddressFamily(_enum.IntEnum):
    AF_UNSPEC = 0
    AF_UNIX = 1
    AF_INET = 2
    AF_INET6 = 10


class SocketKind(_enum.IntEnum):
    SOCK_STREAM = 1
    SOCK_DGRAM = 2
    SOCK_RAW = 3


class AddressInfo(_enum.IntFlag):
    AI_PASSIVE = 1
    AI_CANONNAME = 2
    AI_NUMERICHOST = 4
    AI_ADDRCONFIG = 32


class MsgFlag(_enum.IntFlag):
    MSG_OOB = 1
    MSG_PEEK = 2
    MSG_DONTWAIT = 64


AF_UNSPEC = AddressFamily.AF_UNSPEC
AF_UNIX = AddressFamily.AF_UNIX
AF_INET = AddressFamily.AF_INET
AF_INET6 = AddressFamily.AF_INET6
SOCK_STREAM = SocketKind.SOCK_STREAM
SOCK_DGRAM = SocketKind.SOCK_DGRAM
SOCK_RAW = SocketKind.SOCK_RAW
AI_PASSIVE = AddressInfo.AI_PASSIVE
AI_CANONNAME = AddressInfo.AI_CANONNAME
AI_NUMERICHOST = AddressInfo.AI_NUMERICHOST
AI_ADDRCONFIG = AddressInfo.AI_ADDRCONFIG
MSG_OOB = MsgFlag.MSG_OOB
MSG_PEEK = MsgFlag.MSG_PEEK
MSG_DONTWAIT = MsgFlag.MSG_DONTWAIT

SOL_SOCKET = 1
SO_REUSEADDR = 2
SO_KEEPALIVE = 9
SO_RCVBUF = 8
SO_SNDBUF = 7
IPPROTO_TCP = 6
IPPROTO_UDP = 17
IPPROTO_IPV6 = 41
TCP_NODELAY = 1
NI_NUMERICHOST = 1
NI_NUMERICSERV = 2
SHUT_RD = 0
SHUT_WR = 1
SHUT_RDWR = 2
SOMAXCONN = 4096
has_ipv6 = True

error = OSError


class herror(OSError):
    pass


class gaierror(OSError):
    pass


timeout = TimeoutError

_default_timeout = None


def getdefaulttimeout():
    return _default_timeout


def setdefaulttimeout(t):
    global _default_timeout
    _default_timeout = t


def _unavailable():
    raise OSError(95, "Raw sockets are not available; use the HTTP client")


class socket:
    def __init__(self, family=AF_INET, type=SOCK_STREAM, proto=0, fileno=None):
        self.family = family
        self.type = type
        self.proto = proto
        self._timeout = _default_timeout
        self._closed = False

    def settimeout(self, t):
        self._timeout = t

    def gettimeout(self):
        return self._timeout

    def setblocking(self, flag):
        self._timeout = None if flag else 0.0

    def setsockopt(self, *args):
        pass

    def getsockopt(self, *args):
        return 0

    def connect(self, address):
        _unavailable()

    connect_ex = connect

    def bind(self, address):
        _unavailable()

    def listen(self, backlog=0):
        _unavailable()

    def accept(self):
        _unavailable()

    def send(self, data, flags=0):
        _unavailable()

    sendall = send
    sendto = send

    def recv(self, n, flags=0):
        _unavailable()

    recvfrom = recv
    recv_into = recv

    def fileno(self):
        return -1

    def shutdown(self, how):
        pass

    def close(self):
        self._closed = True

    def detach(self):
        return -1

    def __enter__(self):
        return self

    def __exit__(self, *exc):
        self.close()


SocketType = socket


def create_connection(address, timeout=None, source_address=None, *, all_errors=False):
    _unavailable()


def create_server(address, *, family=AF_INET, backlog=None, reuse_port=False, dualstack_ipv6=False):
    _unavailable()


def socketpair(family=AF_UNIX, type=SOCK_STREAM, proto=0):
    _unavailable()


def gethostname():
    return "localhost"


def getfqdn(name=""):
    return name or "localhost"


def _numeric(host):
    import ipaddress
    try:
        return ipaddress.ip_address(host)
    except ValueError:
        return None


def gethostbyname(host):
    if host in ("localhost", ""):
        return "127.0.0.1"
    a = _numeric(host)
    if a is not None and a.version == 4:
        return str(a)
    raise gaierror(-2, "Name or service not known")


def getaddrinfo(host, port, family=0, type=0, proto=0, flags=0):
    """Numeric hosts and localhost only: names resolve inside the HTTP stack."""
    if host is None or host == "localhost":
        host = "127.0.0.1"
    a = _numeric(host)
    if a is None:
        raise gaierror(-2, "Name or service not known")
    fam = AF_INET if a.version == 4 else AF_INET6
    if family not in (0, fam):
        raise gaierror(-9, "Address family for hostname not supported")
    addr = (str(a), int(port or 0)) if fam == AF_INET else (str(a), int(port or 0), 0, 0)
    return [(fam, type or SOCK_STREAM, proto or IPPROTO_TCP, "", addr)]


def inet_aton(s):
    import ipaddress
    try:
        return ipaddress.IPv4Address(s).packed
    except ValueError:
        raise OSError("illegal IP address string passed to inet_aton")


def inet_ntoa(b):
    import ipaddress
    return str(ipaddress.IPv4Address(bytes(b)))


def inet_pton(family, s):
    import ipaddress
    return (ipaddress.IPv4Address(s) if family == AF_INET else ipaddress.IPv6Address(s)).packed


def inet_ntop(family, b):
    import ipaddress
    return str(ipaddress.IPv4Address(bytes(b)) if family == AF_INET else ipaddress.IPv6Address(bytes(b)))


def htons(x):
    return ((x & 0xFF) << 8) | ((x >> 8) & 0xFF)


ntohs = htons


def htonl(x):
    return int.from_bytes(int(x).to_bytes(4, "little"), "big")


ntohl = htonl
