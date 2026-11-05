# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""Internationalized Domain Names in Applications (IDNA 2008): encode and decode.

Code points are accepted when they are lowercase letters, digits or hyphens;
the full IDNA tables are not carried, so the contextual rules (joiners, bidi,
combining marks) are not enforced.
"""

__version__ = "3.10"

_ACE_PREFIX = "xn--"
_DOTS = (".", "\u3002", "\uff0e", "\uff61")

_BASE = 36
_TMIN = 1
_TMAX = 26
_SKEW = 38
_DAMP = 700
_INITIAL_BIAS = 72
_INITIAL_N = 128


class IDNAError(UnicodeError):
    """Base class for IDNA failures."""


class IDNABidiError(IDNAError):
    """A bidi rule was violated."""


class InvalidCodepoint(IDNAError):
    """A code point is not allowed in a label."""


class InvalidCodepointContext(IDNAError):
    """A code point is not allowed in its context."""


def _adapt(delta, numpoints, first):
    delta = delta // _DAMP if first else delta // 2
    delta += delta // numpoints
    k = 0
    while delta > ((_BASE - _TMIN) * _TMAX) // 2:
        delta //= _BASE - _TMIN
        k += _BASE
    return k + (_BASE - _TMIN + 1) * delta // (delta + _SKEW)


def _digit(d):
    if d < 26:
        return chr(97 + d)
    return chr(22 + d)


def punycode_encode(text):
    """RFC 3492 encoding of one label (no prefix)."""
    cps = [ord(c) for c in text]
    out = [c for c in text if ord(c) < 128]
    h = len(out)
    b = h
    if b > 0:
        out.append("-")
    n = _INITIAL_N
    delta = 0
    bias = _INITIAL_BIAS
    while h < len(cps):
        m = min([c for c in cps if c >= n])
        delta += (m - n) * (h + 1)
        n = m
        for c in cps:
            if c < n:
                delta += 1
            if c == n:
                q = delta
                k = _BASE
                while True:
                    t = _TMIN if k <= bias else (_TMAX if k >= bias + _TMAX else k - bias)
                    if q < t:
                        break
                    out.append(_digit(t + (q - t) % (_BASE - t)))
                    q = (q - t) // (_BASE - t)
                    k += _BASE
                out.append(_digit(q))
                bias = _adapt(delta, h + 1, h == b)
                delta = 0
                h += 1
        delta += 1
        n += 1
    return "".join(out)


def punycode_decode(text):
    """RFC 3492 decoding of one label (no prefix)."""
    pos = text.rfind("-")
    if pos >= 0:
        out = [ord(c) for c in text[:pos]]
        rest = text[pos + 1:]
    else:
        out = []
        rest = text
    n = _INITIAL_N
    i = 0
    bias = _INITIAL_BIAS
    idx = 0
    while idx < len(rest):
        oldi = i
        w = 1
        k = _BASE
        while True:
            if idx >= len(rest):
                raise IDNAError("Invalid A-label")
            c = rest[idx]
            idx += 1
            o = ord(c)
            if 48 <= o <= 57:
                d = o - 22
            elif 65 <= o <= 90:
                d = o - 65
            elif 97 <= o <= 122:
                d = o - 97
            else:
                raise IDNAError("Invalid A-label")
            i += d * w
            t = _TMIN if k <= bias else (_TMAX if k >= bias + _TMAX else k - bias)
            if d < t:
                break
            w *= _BASE - t
            k += _BASE
        bias = _adapt(i - oldi, len(out) + 1, oldi == 0)
        n += i // (len(out) + 1)
        i = i % (len(out) + 1)
        if n > 0x10FFFF:
            raise IDNAError("Invalid A-label")
        out.insert(i, n)
        i += 1
    return "".join([chr(c) for c in out])


def _is_mark(o):
    """Combining marks and Indic or Thai vowel signs (no Unicode tables here)."""
    if 0x0300 <= o <= 0x036F or 0x1AB0 <= o <= 0x1AFF or 0x20D0 <= o <= 0x20FF:
        return True
    if 0x0900 <= o <= 0x0DFF and o != 0x0964 and o != 0x0965:
        return True
    return 0x0E31 <= o <= 0x0E3A or 0x0E47 <= o <= 0x0E4E


def check_label(label):
    if isinstance(label, (bytes, bytearray)):
        label = label.decode("utf-8")
    if len(label) == 0:
        raise IDNAError("Empty Label")
    if label[2:4] == "--":
        raise IDNAError("Label has disallowed hyphens in 3rd and 4th position")
    if label[0] == "-" or label[-1] == "-":
        raise IDNAError("Label must not start or end with a hyphen")
    pos = 0
    for ch in label:
        pos += 1
        o = ord(ch)
        if o < 128:
            ok = ch == "-" or ("a" <= ch <= "z") or ("0" <= ch <= "9")
        else:
            ok = (ch.isalpha() or ch.isdigit()) and ch == ch.lower() and not ch.isupper()
            if not ok and _is_mark(o):
                ok = True
        if not ok:
            raise InvalidCodepoint("Codepoint U+" + format(o, "04X") + " at position " + str(pos)
                                   + " of " + repr(label) + " not allowed")
    return True


def valid_label_length(label):
    return len(label) <= 63


def valid_string_length(label, trailing_dot):
    return len(label) <= (254 if trailing_dot else 253)


def _is_ascii(s):
    for ch in s:
        if ord(ch) >= 128:
            return False
    return True


def ulabel(label):
    if isinstance(label, (bytes, bytearray)):
        label = label.decode("ascii")
    if not _is_ascii(label):
        check_label(label)
        return label
    label = label.lower()
    if label.startswith(_ACE_PREFIX):
        label = label[4:]
        if not label:
            raise IDNAError("Malformed A-label, no Punycode eligible content found")
        if label[-1] == "-":
            raise IDNAError("A-label must not end with a hyphen")
    else:
        check_label(label)
        return label
    decoded = punycode_decode(label)
    check_label(decoded)
    return decoded


def alabel(label):
    if isinstance(label, (bytes, bytearray)):
        label = label.decode("ascii")
    if _is_ascii(label):
        ulabel(label)
        if not valid_label_length(label):
            raise IDNAError("Label too long")
        return label.encode("ascii")
    check_label(label)
    enc = _ACE_PREFIX + punycode_encode(label)
    if not valid_label_length(enc):
        raise IDNAError("Label too long")
    return enc.encode("ascii")


def uts46_remap(domain, std3_rules=True, transitional=False):
    """A reduced UTS 46 mapping: case folding and the alternative dots."""
    out = domain.lower()
    for d in _DOTS[1:]:
        out = out.replace(d, ".")
    return out


def _split(s):
    for d in _DOTS[1:]:
        s = s.replace(d, ".")
    return s.split(".")


def encode(s, strict=False, uts46=False, std3_rules=False, transitional=False):
    if not isinstance(s, str):
        try:
            s = str(s, "ascii")
        except UnicodeDecodeError:
            raise IDNAError("should pass a unicode string to the function rather than a byte string.")
    if uts46:
        s = uts46_remap(s, std3_rules, transitional)
    trailing_dot = False
    result = []
    labels = s.split(".") if strict else _split(s)
    if not labels or labels == [""]:
        raise IDNAError("Empty domain")
    if labels[-1] == "":
        del labels[-1]
        trailing_dot = True
    for label in labels:
        s = alabel(label)
        if s:
            result.append(s)
        else:
            raise IDNAError("Empty Label")
    if trailing_dot:
        result.append(b"")
    s = b".".join(result)
    if not valid_string_length(s, trailing_dot):
        raise IDNAError("Domain too long")
    return s


def decode(s, strict=False, uts46=False, std3_rules=False):
    if isinstance(s, (bytes, bytearray)):
        try:
            s = s.decode("ascii")
        except UnicodeDecodeError:
            raise IDNAError("Invalid ASCII in A-label")
    if uts46:
        s = uts46_remap(s, std3_rules, False)
    trailing_dot = False
    result = []
    labels = s.split(".") if strict else _split(s)
    if not labels or labels == [""]:
        raise IDNAError("Empty domain")
    if labels[-1] == "":
        del labels[-1]
        trailing_dot = True
    for label in labels:
        s = ulabel(label)
        if s:
            result.append(s)
        else:
            raise IDNAError("Empty Label")
    if trailing_dot:
        result.append("")
    return ".".join(result)
