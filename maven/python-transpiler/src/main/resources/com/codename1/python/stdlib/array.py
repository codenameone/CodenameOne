# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""array: typed arrays of numbers, stored as a list and checked against
their type code on every store. The byte layout of tobytes/frombytes is the
machine's native one, little-endian on every Codename One target."""

import struct as _struct

typecodes = "bBuhHiIlLqQfd"

_SIZES = {"b": 1, "B": 1, "u": 4, "w": 4, "h": 2, "H": 2, "i": 4, "I": 4, "l": 8, "L": 8, "q": 8, "Q": 8,
          "f": 4, "d": 8}
_FMT = {"b": "b", "B": "B", "h": "h", "H": "H", "i": "i", "I": "I", "l": "q", "L": "Q", "q": "q", "Q": "Q",
        "f": "f", "d": "d"}

__all__ = ["array", "ArrayType", "typecodes"]


class array:
    __slots__ = ("_tc", "_a")

    def __new__(cls, typecode, initializer=None):
        if not isinstance(typecode, str) or len(typecode) != 1:
            raise TypeError("array() argument 1 must be a unicode character, not %s" % type(typecode).__name__)
        if typecode not in _SIZES:
            raise ValueError("bad typecode (must be b, B, u, w, h, H, i, I, l, L, q, Q, f or d)")
        self = object.__new__(cls)
        self._tc = typecode
        self._a = []
        if initializer is not None:
            if isinstance(initializer, (bytes, bytearray)):
                self.frombytes(initializer)
            elif isinstance(initializer, str):
                if typecode not in "uw":
                    raise TypeError("cannot use a str to initialize an array with typecode '%s'" % typecode)
                self._a.extend(initializer)
            elif isinstance(initializer, array) and initializer._tc in "uw" and typecode not in "uw":
                raise TypeError("cannot use a unicode array to initialize an array with typecode '%s'" % typecode)
            else:
                self.extend(initializer)
        return self

    def __init__(self, *args, **kwargs):
        pass

    @property
    def typecode(self):
        return self._tc

    @property
    def itemsize(self):
        return _SIZES[self._tc]

    def _check(self, v):
        tc = self._tc
        if tc in "uw":
            if not isinstance(v, str) or len(v) != 1:
                raise TypeError("array item must be a unicode character, not %s" % type(v).__name__)
            return v
        if tc in "fd":
            if not isinstance(v, (int, float)):
                raise TypeError("must be real number, not %s" % type(v).__name__)
            v = float(v)
            if tc == "f":
                v = _struct.unpack("<f", _struct.pack("<f", v))[0]
            return v
        if not isinstance(v, int):
            if hasattr(v, "__index__"):
                v = v.__index__()
            else:
                raise TypeError("'%s' object cannot be interpreted as an integer" % type(v).__name__)
        bits = _SIZES[tc] * 8
        if tc in "bhilq":
            lo, hi = -(1 << (bits - 1)), (1 << (bits - 1)) - 1
        else:
            lo, hi = 0, (1 << bits) - 1
        if v < lo or v > hi:
            if tc in "bB":
                raise OverflowError("%s is %s than %s" % (
                    "signed char" if tc == "b" else "unsigned byte integer", "less" if v < lo else "greater",
                    "minimum" if v < lo else "maximum"))
            raise OverflowError("%s %s is %s than %s" % (
                "signed" if tc in "bhilq" else "unsigned",
                {"h": "short integer", "H": "short integer", "i": "integer", "I": "integer",
                 "l": "integer", "L": "integer", "q": "integer", "Q": "integer"}[tc],
                "less" if v < lo else "greater", "minimum" if v < lo else "maximum"))
        return v

    def __len__(self):
        return len(self._a)

    def __iter__(self):
        return iter(self._a)

    def __reversed__(self):
        return reversed(self._a)

    def __contains__(self, v):
        return v in self._a

    def __getitem__(self, i):
        if isinstance(i, slice):
            r = array(self._tc)
            r._a = self._a[i]
            return r
        return self._a[i]

    def __setitem__(self, i, v):
        if isinstance(i, slice):
            if not isinstance(v, array):
                raise TypeError("can only assign array (not \"%s\") to array slice" % type(v).__name__)
            if v._tc != self._tc:
                raise TypeError("bad argument type for built-in operation")
            self._a[i] = list(v._a)
        else:
            self._a[i] = self._check(v)

    def __delitem__(self, i):
        del self._a[i]

    def __eq__(self, other):
        if not isinstance(other, array):
            return NotImplemented
        return self._a == other._a

    def __ne__(self, other):
        if not isinstance(other, array):
            return NotImplemented
        return self._a != other._a

    def __lt__(self, other):
        if not isinstance(other, array):
            return NotImplemented
        return self._a < other._a

    def __le__(self, other):
        if not isinstance(other, array):
            return NotImplemented
        return self._a <= other._a

    def __gt__(self, other):
        if not isinstance(other, array):
            return NotImplemented
        return self._a > other._a

    def __ge__(self, other):
        if not isinstance(other, array):
            return NotImplemented
        return self._a >= other._a

    __hash__ = None

    def __add__(self, other):
        if not isinstance(other, array):
            raise TypeError("can only append array (not \"%s\") to array" % type(other).__name__)
        if other._tc != self._tc:
            raise TypeError("bad argument type for built-in operation")
        r = array(self._tc)
        r._a = self._a + other._a
        return r

    def __iadd__(self, other):
        if not isinstance(other, array):
            raise TypeError("can only extend array with array (not \"%s\")" % type(other).__name__)
        self.extend(other)
        return self

    def __mul__(self, n):
        r = array(self._tc)
        r._a = self._a * n
        return r

    __rmul__ = __mul__

    def __imul__(self, n):
        self._a *= n
        return self

    def __copy__(self):
        return array(self._tc, self)

    def __deepcopy__(self, memo):
        return array(self._tc, self)

    def __reduce_ex__(self, protocol):
        return (array, (self._tc, list(self._a)))

    def __repr__(self):
        name = type(self).__name__
        if not self._a:
            return "%s('%s')" % (name, self._tc)
        if self._tc in "uw":
            return "%s('%s', %r)" % (name, self._tc, "".join(self._a))
        return "%s('%s', %r)" % (name, self._tc, self._a)

    def __bytes__(self):
        return self.tobytes()

    def append(self, v):
        self._a.append(self._check(v))

    def extend(self, it):
        if isinstance(it, array):
            if it._tc != self._tc:
                raise TypeError("can only extend with array of same kind")
            self._a.extend(it._a)
            return
        for v in it:
            self._a.append(self._check(v))

    def insert(self, i, v):
        self._a.insert(i, self._check(v))

    def pop(self, i=-1):
        if not self._a:
            raise IndexError("pop from empty array")
        return self._a.pop(i)

    def remove(self, v):
        try:
            self._a.remove(v)
        except ValueError:
            raise ValueError("array.remove(x): x not in array") from None

    def index(self, v, start=0, stop=None):
        a = self._a
        n = len(a)
        if stop is None or stop > n:
            stop = n
        for i in range(start if start >= 0 else max(0, n + start), stop if stop >= 0 else n + stop):
            if a[i] == v:
                return i
        raise ValueError("array.index(x): x not in array")

    def count(self, v):
        return self._a.count(v)

    def reverse(self):
        self._a.reverse()

    def buffer_info(self):
        return (id(self), len(self._a))

    def byteswap(self):
        if self._tc in "uw":
            data = self.tobytes()
            sz = 4
        else:
            data = self.tobytes()
            sz = _SIZES[self._tc]
        out = bytearray()
        for k in range(0, len(data), sz):
            out += data[k:k + sz][::-1]
        self._a = []
        self.frombytes(bytes(out))

    def tobytes(self):
        tc = self._tc
        if tc in "uw":
            return "".join(self._a).encode("utf-32-le") if self._a else b""
        if not self._a:
            return b""
        return _struct.pack("<%d%s" % (len(self._a), _FMT[tc]), *self._a)

    def frombytes(self, b):
        b = bytes(b)
        sz = _SIZES[self._tc]
        if len(b) % sz:
            raise ValueError("bytes length not a multiple of item size")
        if self._tc in "uw":
            self._a.extend(b.decode("utf-32-le"))
            return
        n = len(b) // sz
        if n:
            self._a.extend(_struct.unpack("<%d%s" % (n, _FMT[self._tc]), b))

    def tolist(self):
        return list(self._a)

    def fromlist(self, lst):
        if not isinstance(lst, list):
            raise TypeError("arg must be list")
        vals = [self._check(v) for v in lst]
        self._a.extend(vals)

    def tounicode(self):
        if self._tc not in "uw":
            raise ValueError("tounicode() may only be called on unicode type arrays ('u' or 'w')")
        return "".join(self._a)

    def fromunicode(self, s):
        if self._tc not in "uw":
            raise ValueError("fromunicode() may only be called on unicode type arrays ('u' or 'w')")
        self._a.extend(s)

    def tofile(self, f):
        f.write(self.tobytes())

    def fromfile(self, f, n):
        sz = _SIZES[self._tc]
        data = f.read(n * sz)
        if len(data) < n * sz:
            self.frombytes(data[:len(data) - len(data) % sz])
            raise EOFError("read() didn't return enough bytes")
        self.frombytes(data)


ArrayType = array
