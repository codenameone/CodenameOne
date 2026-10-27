# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""Parse URLs into components, and quote and unquote their parts."""

from collections import namedtuple

__all__ = ["urlparse", "urlunparse", "urljoin", "urldefrag", "urlsplit", "urlunsplit", "urlencode",
           "parse_qs", "parse_qsl", "quote", "quote_plus", "quote_from_bytes", "unquote", "unquote_plus",
           "unquote_to_bytes", "DefragResult", "ParseResult", "SplitResult", "DefragResultBytes",
           "ParseResultBytes", "SplitResultBytes"]

uses_relative = ["", "ftp", "http", "gopher", "nntp", "imap", "wais", "file", "https", "shttp", "mms",
                 "prospero", "rtsp", "rtsps", "rtspu", "sftp", "svn", "svn+ssh", "ws", "wss"]
uses_netloc = ["", "ftp", "http", "gopher", "nntp", "telnet", "imap", "wais", "file", "mms", "https", "shttp",
               "snews", "prospero", "rtsp", "rtsps", "rtspu", "rsync", "svn", "svn+ssh", "sftp", "nfs", "git",
               "git+ssh", "ws", "wss", "itms-services"]
uses_params = ["", "ftp", "hdl", "prospero", "http", "imap", "https", "shttp", "rtsp", "rtsps", "rtspu", "sip",
               "sips", "mms", "sftp", "tel"]

scheme_chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789+-."

_UNSAFE_URL_BYTES_TO_REMOVE = ["\t", "\r", "\n"]
_WHATWG_C0_CONTROL_OR_SPACE = "".join(chr(c) for c in range(33))


def _coerce_args(*args):
    str_input = isinstance(args[0], str)
    for arg in args[1:]:
        if arg and isinstance(arg, str) != str_input:
            raise TypeError("Cannot mix str and non-str arguments")
    if str_input:
        return args + (_noop,)
    return tuple(_decode(x) for x in args) + (_encode_result,)


def _noop(obj):
    return obj


def _decode(x):
    if x is None or x == b"":
        return ""
    return bytes(x).decode("ascii", "strict")


def _encode_result(obj, encoding="ascii", errors="strict"):
    if isinstance(obj, tuple):
        enc = type(obj)._encoded
        return enc(*(x.encode(encoding, errors) if isinstance(x, str) else x for x in obj))
    return obj.encode(encoding, errors)


class _NetlocResultMixinBase:
    __slots__ = ()

    @property
    def username(self):
        return self._userinfo[0]

    @property
    def password(self):
        return self._userinfo[1]

    @property
    def hostname(self):
        hostname = self._hostinfo[0]
        if not hostname:
            return None
        separator = "%" if isinstance(hostname, str) else b"%"
        hostname, percent, zone = hostname.partition(separator)
        return hostname.lower() + percent + zone

    @property
    def port(self):
        port = self._hostinfo[1]
        if port is not None:
            if port.isdigit() and port.isascii():
                port = int(port)
            else:
                raise ValueError("Port could not be cast to integer value as %r" % (port,))
            if not (0 <= port <= 65535):
                raise ValueError("Port out of range 0-65535")
        return port

    @property
    def _userinfo(self):
        netloc = self.netloc
        userinfo, have_info, hostinfo = netloc.rpartition("@" if isinstance(netloc, str) else b"@")
        if have_info:
            username, have_password, password = userinfo.partition(":" if isinstance(netloc, str) else b":")
            if not have_password:
                password = None
        else:
            username = password = None
        return username, password

    @property
    def _hostinfo(self):
        netloc = self.netloc
        s = isinstance(netloc, str)
        _, _, hostinfo = netloc.rpartition("@" if s else b"@")
        _, have_open_br, bracketed = hostinfo.partition("[" if s else b"[")
        if have_open_br:
            hostname, _, port = bracketed.partition("]" if s else b"]")
            _, _, port = port.partition(":" if s else b":")
        else:
            hostname, _, port = hostinfo.partition(":" if s else b":")
        if not port:
            port = None
        return hostname, port


_DefragResultBase = namedtuple("DefragResult", "url fragment")
_SplitResultBase = namedtuple("SplitResult", "scheme netloc path query fragment")
_ParseResultBase = namedtuple("ParseResult", "scheme netloc path params query fragment")


class DefragResult(_DefragResultBase):
    __slots__ = ()

    def geturl(self):
        if self.fragment:
            return self.url + "#" + self.fragment
        return self.url

    def encode(self, encoding="ascii", errors="strict"):
        return DefragResultBytes(*(x.encode(encoding, errors) for x in self))


class SplitResult(_SplitResultBase, _NetlocResultMixinBase):
    __slots__ = ()

    def geturl(self):
        return urlunsplit(self)

    def encode(self, encoding="ascii", errors="strict"):
        return SplitResultBytes(*(x.encode(encoding, errors) for x in self))


class ParseResult(_ParseResultBase, _NetlocResultMixinBase):
    __slots__ = ()

    def geturl(self):
        return urlunparse(self)

    def encode(self, encoding="ascii", errors="strict"):
        return ParseResultBytes(*(x.encode(encoding, errors) for x in self))


class DefragResultBytes(_DefragResultBase):
    __slots__ = ()

    def geturl(self):
        if self.fragment:
            return self.url + b"#" + self.fragment
        return self.url

    def decode(self, encoding="ascii", errors="strict"):
        return DefragResult(*(x.decode(encoding, errors) for x in self))


class SplitResultBytes(_SplitResultBase, _NetlocResultMixinBase):
    __slots__ = ()

    def geturl(self):
        return urlunsplit(self)

    def decode(self, encoding="ascii", errors="strict"):
        return SplitResult(*(x.decode(encoding, errors) for x in self))


class ParseResultBytes(_ParseResultBase, _NetlocResultMixinBase):
    __slots__ = ()

    def geturl(self):
        return urlunparse(self)

    def decode(self, encoding="ascii", errors="strict"):
        return ParseResult(*(x.decode(encoding, errors) for x in self))


DefragResult._encoded = DefragResultBytes
SplitResult._encoded = SplitResultBytes
ParseResult._encoded = ParseResultBytes


def urlparse(url, scheme="", allow_fragments=True):
    url, scheme, _coerce_result = _coerce_args(url, scheme)
    scheme, netloc, url, query, fragment = _urlsplit(url, scheme, allow_fragments)
    if scheme in uses_params and ";" in url:
        url, params = _splitparams(url)
    else:
        params = ""
    return _coerce_result(ParseResult(scheme, netloc, url, params, query, fragment))


def _splitparams(url):
    if "/" in url:
        i = url.find(";", url.rfind("/"))
        if i < 0:
            return url, ""
    else:
        i = url.find(";")
    return url[:i], url[i + 1:]


def _splitnetloc(url, start=0):
    delim = len(url)
    for c in "/?#":
        wdelim = url.find(c, start)
        if wdelim >= 0:
            delim = min(delim, wdelim)
    return url[start:delim], url[delim:]


def _check_bracketed_netloc(netloc):
    hostname_and_port = netloc.rpartition("@")[2]
    before_bracket, have_open_br, bracketed = hostname_and_port.partition("[")
    if have_open_br:
        if before_bracket:
            raise ValueError("Invalid IPv6 URL")
        hostname, _, port = bracketed.partition("]")
        if port and not port.startswith(":"):
            raise ValueError("Invalid IPv6 URL")


def _urlsplit(url, scheme, allow_fragments):
    url = url.lstrip(_WHATWG_C0_CONTROL_OR_SPACE)
    scheme = scheme.strip(_WHATWG_C0_CONTROL_OR_SPACE)
    for b in _UNSAFE_URL_BYTES_TO_REMOVE:
        url = url.replace(b, "")
        scheme = scheme.replace(b, "")
    allow_fragments = bool(allow_fragments)
    netloc = query = fragment = ""
    i = url.find(":")
    if i > 0 and url[0].isascii() and url[0].isalpha():
        for c in url[:i]:
            if c not in scheme_chars:
                break
        else:
            scheme, url = url[:i].lower(), url[i + 1:]
    if url[:2] == "//":
        netloc, url = _splitnetloc(url, 2)
        if (("[" in netloc and "]" not in netloc) or ("]" in netloc and "[" not in netloc)):
            raise ValueError("Invalid IPv6 URL")
        if "[" in netloc and "]" in netloc:
            _check_bracketed_netloc(netloc)
    if allow_fragments and "#" in url:
        url, fragment = url.split("#", 1)
    if "?" in url:
        url, query = url.split("?", 1)
    return scheme, netloc, url, query, fragment


def urlsplit(url, scheme="", allow_fragments=True):
    url, scheme, _coerce_result = _coerce_args(url, scheme)
    return _coerce_result(SplitResult(*_urlsplit(url, scheme, allow_fragments)))


def urlunparse(components):
    scheme, netloc, url, params, query, fragment, _coerce_result = _coerce_args(*components)
    if params:
        url = "%s;%s" % (url, params)
    return _coerce_result(_urlunsplit(scheme, netloc, url, query, fragment))


def urlunsplit(components):
    scheme, netloc, url, query, fragment, _coerce_result = _coerce_args(*components)
    return _coerce_result(_urlunsplit(scheme, netloc, url, query, fragment))


def _urlunsplit(scheme, netloc, url, query, fragment):
    if netloc:
        if url and url[:1] != "/":
            url = "/" + url
        url = "//" + netloc + url
    elif url[:2] == "//":
        url = "//" + url
    elif scheme and scheme in uses_netloc and (not url or url[:1] == "/"):
        url = "//" + url
    if scheme:
        url = scheme + ":" + url
    if query:
        url = url + "?" + query
    if fragment:
        url = url + "#" + fragment
    return url


def urljoin(base, url, allow_fragments=True):
    if not base:
        return url
    if not url:
        return base
    base, url, _coerce_result = _coerce_args(base, url)
    bscheme, bnetloc, bpath, bquery, bfragment = _urlsplit(base, "", allow_fragments)
    scheme, netloc, path, query, fragment = _urlsplit(url, bscheme, allow_fragments)
    if scheme != bscheme or scheme not in uses_relative:
        return _coerce_result(url)
    if scheme in uses_netloc:
        if netloc:
            return _coerce_result(_urlunsplit(scheme, netloc, path, query, fragment))
        netloc = bnetloc
    if not path:
        path = bpath
        if not query:
            query = bquery
            if not fragment:
                fragment = bfragment
        return _coerce_result(_urlunsplit(scheme, netloc, path, query, fragment))
    base_parts = bpath.split("/")
    if base_parts[-1] != "":
        del base_parts[-1]
    if path[:1] == "/":
        segments = path.split("/")
    else:
        segments = base_parts + path.split("/")
        segments[1:-1] = filter(None, segments[1:-1])
    resolved_path = []
    for seg in segments:
        if seg == "..":
            try:
                resolved_path.pop()
            except IndexError:
                pass
        elif seg == ".":
            continue
        else:
            resolved_path.append(seg)
    if segments[-1] in (".", ".."):
        resolved_path.append("")
    return _coerce_result(_urlunsplit(scheme, netloc, "/".join(resolved_path) or "/", query, fragment))


def urldefrag(url):
    url, _coerce_result = _coerce_args(url)
    if "#" in url:
        s, n, p, a, q, frag = urlparse(url)
        defrag = urlunparse((s, n, p, a, q, ""))
    else:
        frag = ""
        defrag = url
    return _coerce_result(DefragResult(defrag, frag))


_hexdig = "0123456789ABCDEFabcdef"
_hextobyte = None


def unquote_to_bytes(string):
    if not string:
        string.split
        return b""
    if isinstance(string, str):
        string = string.encode("utf-8")
    bits = string.split(b"%")
    if len(bits) == 1:
        return bytes(string)
    res = bytearray(bits[0])
    for item in bits[1:]:
        h = item[:2]
        if len(h) == 2 and all(c in b"0123456789ABCDEFabcdef" for c in h):
            res.append(int(h, 16))
            res += item[2:]
        else:
            res += b"%"
            res += item
    return bytes(res)


def unquote(string, encoding="utf-8", errors="replace"):
    if isinstance(string, bytes):
        return unquote_to_bytes(string).decode(encoding, errors)
    if "%" not in string:
        string.split
        return string
    if encoding is None:
        encoding = "utf-8"
    if errors is None:
        errors = "replace"
    out = []
    i = 0
    n = len(string)
    while i < n:
        j = string.find("%", i)
        if j < 0:
            out.append(string[i:])
            break
        out.append(string[i:j])
        k = j
        raw = bytearray()
        while k + 2 < n + 0 and string[k] == "%" and string[k + 1] in _hexdig and string[k + 2] in _hexdig:
            raw.append(int(string[k + 1:k + 3], 16))
            k += 3
            if k >= n:
                break
        if raw:
            out.append(bytes(raw).decode(encoding, errors))
            i = k
        else:
            out.append("%")
            i = j + 1
    return "".join(out)


def parse_qs(qs, keep_blank_values=False, strict_parsing=False, encoding="utf-8", errors="replace",
             max_num_fields=None, separator="&"):
    parsed_result = {}
    pairs = parse_qsl(qs, keep_blank_values, strict_parsing, encoding=encoding, errors=errors,
                      max_num_fields=max_num_fields, separator=separator)
    for name, value in pairs:
        if name in parsed_result:
            parsed_result[name].append(value)
        else:
            parsed_result[name] = [value]
    return parsed_result


def parse_qsl(qs, keep_blank_values=False, strict_parsing=False, encoding="utf-8", errors="replace",
              max_num_fields=None, separator="&"):
    if not qs:
        return []
    if isinstance(qs, (bytes, bytearray, memoryview)):
        qs = bytes(qs).decode("latin-1")
        is_bytes = True
    else:
        is_bytes = False
    if not separator or not isinstance(separator, (str, bytes)):
        raise ValueError("Separator must be of type string or bytes.")
    if isinstance(separator, bytes):
        separator = separator.decode("ascii")
    if max_num_fields is not None:
        num_fields = 1 + qs.count(separator)
        if max_num_fields < num_fields:
            raise ValueError("Max number of fields exceeded")
    r = []
    for name_value in qs.split(separator):
        if not name_value and not strict_parsing:
            continue
        nv = name_value.split("=", 1)
        if len(nv) != 2:
            if strict_parsing:
                raise ValueError("bad query field: %r" % (name_value,))
            if keep_blank_values:
                nv.append("")
            else:
                continue
        if len(nv[1]) or keep_blank_values:
            name = nv[0].replace("+", " ")
            value = nv[1].replace("+", " ")
            if is_bytes:
                name = unquote_to_bytes(name.encode("latin-1"))
                value = unquote_to_bytes(value.encode("latin-1"))
            else:
                name = unquote(name, encoding=encoding, errors=errors)
                value = unquote(value, encoding=encoding, errors=errors)
            r.append((name, value))
    return r


def unquote_plus(string, encoding="utf-8", errors="replace"):
    string = string.replace("+", " ")
    return unquote(string, encoding, errors)


_ALWAYS_SAFE = frozenset(b"ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789_.-~")
_safe_cache = {}


def quote(string, safe="/", encoding=None, errors=None):
    if isinstance(string, str):
        if not string:
            return string
        if encoding is None:
            encoding = "utf-8"
        if errors is None:
            errors = "strict"
        string = string.encode(encoding, errors)
    else:
        if encoding is not None:
            raise TypeError("quote() doesn't support 'encoding' for bytes")
        if errors is not None:
            raise TypeError("quote() doesn't support 'errors' for bytes")
    return quote_from_bytes(string, safe)


def quote_plus(string, safe="", encoding=None, errors=None):
    if ((isinstance(string, str) and " " not in string) or (isinstance(string, bytes) and b" " not in string)):
        return quote(string, safe, encoding, errors)
    if isinstance(safe, str):
        space = " "
    else:
        space = b" "
    string = quote(string, safe + space, encoding, errors)
    return string.replace(" ", "+")


def quote_from_bytes(bs, safe="/"):
    if not isinstance(bs, (bytes, bytearray)):
        raise TypeError("quote_from_bytes() expected bytes")
    if not bs:
        return ""
    if isinstance(safe, str):
        safe = safe.encode("ascii", "ignore")
    else:
        safe = bytes([c for c in safe if c < 128])
    allowed = _ALWAYS_SAFE.union(safe)
    out = []
    for b in bs:
        if b in allowed:
            out.append(chr(b))
        else:
            out.append("%{:02X}".format(b))
    return "".join(out)


def urlencode(query, doseq=False, safe="", encoding=None, errors=None, quote_via=quote_plus):
    if hasattr(query, "items"):
        query = query.items()
    else:
        try:
            if len(query) and not isinstance(query[0], tuple):
                raise TypeError
        except TypeError:
            raise TypeError("not a valid non-string sequence or mapping object")
    l = []
    if not doseq:
        for k, v in query:
            if isinstance(k, bytes):
                k = quote_via(k, safe)
            else:
                k = quote_via(str(k), safe, encoding, errors)
            if isinstance(v, bytes):
                v = quote_via(v, safe)
            else:
                v = quote_via(str(v), safe, encoding, errors)
            l.append(k + "=" + v)
    else:
        for k, v in query:
            if isinstance(k, bytes):
                k = quote_via(k, safe)
            else:
                k = quote_via(str(k), safe, encoding, errors)
            if isinstance(v, bytes):
                v = quote_via(v, safe)
                l.append(k + "=" + v)
            elif isinstance(v, str):
                v = quote_via(v, safe, encoding, errors)
                l.append(k + "=" + v)
            else:
                try:
                    len(v)
                except TypeError:
                    v = quote_via(str(v), safe, encoding, errors)
                    l.append(k + "=" + v)
                else:
                    for elt in v:
                        if isinstance(elt, bytes):
                            elt = quote_via(elt, safe)
                        else:
                            elt = quote_via(str(elt), safe, encoding, errors)
                        l.append(k + "=" + elt)
    return "&".join(l)
