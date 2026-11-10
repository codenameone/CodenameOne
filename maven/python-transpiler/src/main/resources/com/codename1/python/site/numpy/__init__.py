# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""A numpy subset: n-dimensional arrays of float64, int64 and bool.

The array keeps its shape, strides (in elements) and offset here; every loop
over elements is in the runtime's `_cn1np` module, over a flat primitive
buffer. Views share their buffer, as numpy's do. Other dtypes are accepted by
name and stored as the nearest of the three.
"""

import builtins as _b
import math as _math
import _cn1np

__version__ = "2.0.0"

pi = _math.pi
e = _math.e
inf = float("inf")
nan = float("nan")
newaxis = None


# ---- dtypes ---------------------------------------------------------------

class dtype:
    def __init__(self, spec="float64"):
        if isinstance(spec, dtype):
            self.kind, self.name = spec.kind, spec.name
            return
        key = spec
        if isinstance(spec, type):
            key = _TYPE_NAMES.get(spec, getattr(spec, "__name__", "float64"))
        if key not in _KINDS:
            raise TypeError("data type %r not understood" % (spec,))
        self.kind = _KINDS[key]
        self.name = _NAMES[self.kind] if key in _ALIASES else key

    @property
    def type(self):
        return {"f": float64, "i": int64, "b": bool_}[self.kind]

    @property
    def itemsize(self):
        return 1 if self.kind == "b" else 8

    @property
    def char(self):
        return {"f": "d", "i": "l", "b": "?"}[self.kind]

    def __repr__(self):
        return "dtype('%s')" % self.name

    def __str__(self):
        return self.name

    def __eq__(self, other):
        try:
            return dtype(other).kind == self.kind
        except TypeError:
            return False

    def __hash__(self):
        return hash(self.kind)


_KINDS = {"float64": "f", "float32": "f", "float16": "f", "float": "f", "f8": "f", "f4": "f", "d": "f",
          "double": "f", "int64": "i", "int32": "i", "int16": "i", "int8": "i", "uint8": "i", "uint16": "i",
          "uint32": "i", "uint64": "i", "int": "i", "i8": "i", "i4": "i", "l": "i", "intp": "i",
          "bool": "b", "bool_": "b", "?": "b"}
_ALIASES = {"float", "f8", "d", "double", "int", "i8", "l", "intp", "bool_", "?"}
_NAMES = {"f": "float64", "i": "int64", "b": "bool"}
_ORDER = {"b": 0, "i": 1, "f": 2}


class float64(float):
    def __repr__(self):
        return "np.float64(" + float.__repr__(self) + ")"

    def __str__(self):
        return float.__repr__(self)

    def item(self):
        return float(self)


class int64(int):
    def __repr__(self):
        return "np.int64(" + int.__repr__(self) + ")"

    def __str__(self):
        return int.__repr__(self)

    def item(self):
        return int(self)


class bool_:
    def __init__(self, v=False):
        self._v = _b.bool(v)

    def __bool__(self):
        return self._v

    def __eq__(self, other):
        return self._v == _b.bool(other)

    def __hash__(self):
        return hash(self._v)

    def __repr__(self):
        return "np.True_" if self._v else "np.False_"

    def __str__(self):
        return "True" if self._v else "False"

    def __and__(self, o):
        return bool_(self._v and _b.bool(o))

    def __or__(self, o):
        return bool_(self._v or _b.bool(o))

    def __invert__(self):
        return bool_(not self._v)

    def __int__(self):
        return 1 if self._v else 0

    def __index__(self):
        return 1 if self._v else 0

    def item(self):
        return self._v


float32 = float16 = float_ = double = float64
int32 = int16 = int8 = uint8 = uint16 = uint32 = uint64 = intp = int_ = int64
_TYPE_NAMES = {float: "float64", int: "int64", _b.bool: "bool", float64: "float64", int64: "int64", bool_: "bool"}
number = object
floating = float64
integer = int64


def _scalar(v, kind):
    if kind == "f":
        return float64(v)
    if kind == "i":
        return int64(v)
    return bool_(v)


def _kind_of_value(v):
    if isinstance(v, (_b.bool, bool_)):
        return "b"
    if isinstance(v, int):
        return "i"
    if isinstance(v, float):
        return "f"
    raise TypeError("unsupported value %r" % (v,))


# ---- the array ------------------------------------------------------------

def _contig(shape):
    st = []
    s = 1
    for n in reversed(shape):
        st.append(s)
        s *= n
    return tuple(reversed(st))


def _prod(shape):
    n = 1
    for s in shape:
        n *= s
    return n


class ndarray:
    __array_priority__ = 0

    def __init__(self, shape, dtype_=None, buffer=None, offset=0, strides=None):
        if isinstance(shape, int):
            shape = (shape,)
        self._shape = tuple(shape)
        self._dtype = dtype(dtype_ if dtype_ is not None else "float64")
        self._buf = buffer if buffer is not None else _cn1np.alloc(self._dtype.kind, _prod(self._shape))
        self._off = offset
        self._st = tuple(strides) if strides is not None else _contig(self._shape)

    @classmethod
    def _wrap(cls, buf, shape, kind=None):
        a = ndarray.__new__(ndarray)
        a._shape = tuple(shape)
        a._dtype = dtype(_NAMES[kind or _cn1np.kind(buf)])
        a._buf = buf
        a._off = 0
        a._st = _contig(a._shape)
        return a

    def _view(self, shape, strides, off):
        a = ndarray.__new__(ndarray)
        a._shape = tuple(shape)
        a._dtype = self._dtype
        a._buf = self._buf
        a._off = off
        a._st = tuple(strides)
        return a

    # attributes
    @property
    def shape(self):
        return self._shape

    @shape.setter
    def shape(self, value):
        r = self.reshape(value)
        self._shape, self._st, self._buf, self._off = r._shape, r._st, r._buf, r._off

    @property
    def ndim(self):
        return len(self._shape)

    @property
    def size(self):
        return _prod(self._shape)

    @property
    def dtype(self):
        return self._dtype

    @property
    def itemsize(self):
        return self._dtype.itemsize

    @property
    def nbytes(self):
        return self.size * self._dtype.itemsize

    @property
    def strides(self):
        return tuple(s * self._dtype.itemsize for s in self._st)

    @property
    def T(self):
        return self.transpose()

    @property
    def flat(self):
        return iter(self.ravel().tolist())

    @property
    def real(self):
        return self

    def _contiguous(self):
        return self._off == 0 and self._st == _contig(self._shape) and _cn1np.size(self._buf) == self.size

    def _flatbuf(self):
        if self._contiguous():
            return self._buf
        return _cn1np.copy(self._dtype.kind, self._shape, self._buf, self._off, self._st)

    # conversion
    def tolist(self):
        return _cn1np.tolist(self._shape, self._buf, self._off, self._st)

    def item(self, *index):
        if not index:
            if self.size != 1:
                raise ValueError("can only convert an array of size 1 to a Python scalar")
            return _cn1np.item(self._flatbuf(), 0)
        if len(index) == 1 and isinstance(index[0], int) and self.ndim != 1:
            return _cn1np.item(self._flatbuf(), index[0] % self.size if index[0] < 0 else index[0])
        r = self[index if len(index) > 1 else index[0]]
        return r.item() if isinstance(r, ndarray) else r.item()

    def copy(self):
        return ndarray._wrap(_cn1np.copy(self._dtype.kind, self._shape, self._buf, self._off, self._st),
                             self._shape)

    def astype(self, dt):
        k = dtype(dt).kind
        if k == "b":
            return self != 0
        if k == "i" and self._dtype.kind == "f":
            return ndarray._wrap(_cn1np.unop(28, "i", True, self._shape, self._buf, self._off, self._st), self._shape)
        return ndarray._wrap(_cn1np.copy(k, self._shape, self._buf, self._off, self._st), self._shape)

    def __array__(self):
        return self

    # shape
    def reshape(self, *shape, order="C"):
        if len(shape) == 1 and isinstance(shape[0], (tuple, list)):
            shape = tuple(shape[0])
        shape = list(shape)
        n = self.size
        if shape.count(-1) > 1:
            raise ValueError("can only specify one unknown dimension")
        if -1 in shape:
            known = _prod([s for s in shape if s != -1])
            if known == 0 or n % known:
                raise ValueError("cannot reshape array of size %d into shape %s" % (n, _tight(shape)))
            shape[shape.index(-1)] = n // known
        if _prod(shape) != n:
            raise ValueError("cannot reshape array of size %d into shape %s" % (n, _tight(shape)))
        if self._contiguous():
            return self._view(shape, _contig(shape), 0)
        return ndarray._wrap(self._flatbuf(), shape)

    def ravel(self):
        return self.reshape(-1)

    def flatten(self):
        return self.copy().reshape(-1)

    def transpose(self, *axes):
        if not axes:
            axes = tuple(range(self.ndim - 1, -1, -1))
        elif len(axes) == 1 and isinstance(axes[0], (tuple, list)):
            axes = tuple(axes[0])
        axes = [_axis(a, self.ndim) for a in axes]
        return self._view([self._shape[a] for a in axes], [self._st[a] for a in axes], self._off)

    def swapaxes(self, a, b):
        axes = list(range(self.ndim))
        a, b = _axis(a, self.ndim), _axis(b, self.ndim)
        axes[a], axes[b] = axes[b], axes[a]
        return self.transpose(axes)

    def squeeze(self, axis=None):
        keep = [i for i, s in enumerate(self._shape) if s != 1 or (axis is not None and i != _axis(axis, self.ndim))]
        return self._view([self._shape[i] for i in keep], [self._st[i] for i in keep], self._off)

    def fill(self, value):
        _cn1np.fill(self._shape, self._buf, self._off, self._st, value)

    # indexing
    def _resolve(self, key):
        if not isinstance(key, tuple):
            key = (key,)
        if _b.any(isinstance(k, (ndarray, list)) for k in key):
            return None
        n_real = _b.sum(1 for k in key if k is not None and k is not Ellipsis)
        if n_real > self.ndim:
            raise IndexError("too many indices for array: array is %d-dimensional, but %d were indexed"
                             % (self.ndim, n_real))
        if Ellipsis in key:
            i = key.index(Ellipsis)
            key = key[:i] + (slice(None),) * (self.ndim - n_real) + key[i + 1:]
        else:
            key = key + (slice(None),) * (self.ndim - n_real)
        shape, st, off, dim = [], [], self._off, 0
        for k in key:
            if k is None:
                shape.append(1)
                st.append(0)
                continue
            n = self._shape[dim]
            if isinstance(k, slice):
                start, stop, step = k.indices(n)
                shape.append(len(range(start, stop, step)))
                st.append(self._st[dim] * step)
                off += start * self._st[dim]
            else:
                i = int(k)
                if i < -n or i >= n:
                    raise IndexError("index %d is out of bounds for axis %d with size %d" % (i, dim, n))
                off += (i % n) * self._st[dim]
            dim += 1
        return shape, st, off

    def _positions(self, key):
        """The buffer positions an advanced index selects, and the result shape."""
        if not isinstance(key, tuple):
            key = (key,)
        adv = [i for i, k in enumerate(key) if isinstance(k, (ndarray, list))]
        k0 = key[adv[0]]
        k0 = k0 if isinstance(k0, ndarray) else array(k0)
        if len(key) == 1 and k0._dtype.kind == "b":
            if k0._shape != self._shape[:k0.ndim]:
                raise IndexError("boolean index did not match indexed array")
            sub = self._shape[k0.ndim:]
            pos = _cn1np.positions(self._shape[:k0.ndim], self._off, self._st[:k0.ndim])
            pos = _cn1np.compress(pos, k0._flatbuf())
            if not sub:
                return pos, (_cn1np.size(pos),)
            parts = [_cn1np.positions(sub, _cn1np.item(pos, i), self._st[k0.ndim:]) for i in range(_cn1np.size(pos))]
            return _cn1np.concat("i", parts) if parts else _cn1np.alloc("i", 0), (_cn1np.size(pos),) + tuple(sub)
        if len(adv) > 1:
            arrs = [key[i] if isinstance(key[i], ndarray) else array(key[i]) for i in adv]
            if len(adv) != len(key) or len(key) != self.ndim:
                raise IndexError("only one advanced index per selection is supported")
            bshape = _broadcast_shapes(*[a._shape for a in arrs])
            flat = [broadcast_to(a, bshape).ravel().tolist() for a in arrs]
            pos = []
            for idx in zip(*flat):
                p = self._off
                for d, i in enumerate(idx):
                    n = self._shape[d]
                    if i < -n or i >= n:
                        raise IndexError("index %d is out of bounds for axis %d with size %d" % (i, d, n))
                    p += (i % n) * self._st[d]
                pos.append(p)
            return _cn1np.fromflat("i", pos), bshape
        d = adv[0]
        rest = key[:d] + (slice(None),) + key[d + 1:]
        shape, st, off = self._resolve(rest)
        # The advanced axis is the d-th surviving one only when nothing before
        # it was an integer; this subset keeps basic keys before it as slices.
        n = self._shape[d]
        idxs = k0.ravel().tolist()
        parts = []
        for i in idxs:
            if i < -n or i >= n:
                raise IndexError("index %d is out of bounds for axis %d with size %d" % (i, d, n))
            parts.append(_cn1np.positions(shape[:d] + shape[d + 1:], off + (i % n) * st[d], st[:d] + st[d + 1:]))
        pos = _cn1np.concat("i", parts) if parts else _cn1np.alloc("i", 0)
        outer = tuple(shape[:d])
        inner = tuple(shape[d + 1:])
        if outer:
            # positions came out index-major; reorder to the array's order.
            res = ndarray._wrap(pos, k0._shape + outer + inner)
            res = res.transpose(list(range(k0.ndim, k0.ndim + len(outer))) + list(range(k0.ndim)) +
                                list(range(k0.ndim + len(outer), res.ndim)))
            return res._flatbuf(), tuple(outer) + k0._shape + inner
        return pos, k0._shape + inner

    def __getitem__(self, key):
        r = self._resolve(key)
        if r is None:
            pos, shape = self._positions(key)
            return ndarray._wrap(_cn1np.gather(self._buf, pos), shape, self._dtype.kind)
        shape, st, off = r
        if not shape and not (isinstance(key, tuple) and None in key):
            return _scalar(_cn1np.item(self._buf, off), self._dtype.kind)
        return self._view(shape, st, off)

    def __setitem__(self, key, value):
        r = self._resolve(key)
        src = value if isinstance(value, ndarray) else array(value)
        if r is None:
            pos, shape = self._positions(key)
            src = src if src.size == 1 else broadcast_to(src, shape)
            _cn1np.scatter(self._buf, pos, src._flatbuf())
            return
        shape, st, off = r
        src = broadcast_to(src, shape)
        _cn1np.assign(shape, self._buf, off, st, src._buf, src._off, src._st)

    def __len__(self):
        if not self._shape:
            raise TypeError("len() of unsized object")
        return self._shape[0]

    def __iter__(self):
        if not self._shape:
            raise TypeError("iteration over a 0-d array")
        for i in range(self._shape[0]):
            yield self[i]

    def __contains__(self, v):
        return _b.bool((self == v).any())

    def __bool__(self):
        if self.size != 1:
            raise ValueError("The truth value of an array with more than one element is ambiguous. "
                             "Use a.any() or a.all()")
        return _b.bool(self.item())

    def __float__(self):
        return float(self.item())

    def __int__(self):
        return int(self.item())

    def __index__(self):
        if self._dtype.kind == "f":
            raise TypeError("only integer scalar arrays can be converted to a scalar index")
        return int(self.item())

    def __hash__(self):
        raise TypeError("unhashable type: 'numpy.ndarray'")

    # arithmetic
    def __add__(self, o):
        return _binop(0, self, o)

    def __radd__(self, o):
        return _binop(0, o, self)

    def __sub__(self, o):
        return _binop(1, self, o)

    def __rsub__(self, o):
        return _binop(1, o, self)

    def __mul__(self, o):
        return _binop(2, self, o)

    def __rmul__(self, o):
        return _binop(2, o, self)

    def __truediv__(self, o):
        return _binop(3, self, o)

    def __rtruediv__(self, o):
        return _binop(3, o, self)

    def __floordiv__(self, o):
        return _binop(4, self, o)

    def __rfloordiv__(self, o):
        return _binop(4, o, self)

    def __mod__(self, o):
        return _binop(5, self, o)

    def __rmod__(self, o):
        return _binop(5, o, self)

    def __pow__(self, o):
        return _binop(6, self, o)

    def __rpow__(self, o):
        return _binop(6, o, self)

    def __eq__(self, o):
        return _binop(7, self, o)

    def __ne__(self, o):
        return _binop(8, self, o)

    def __lt__(self, o):
        return _binop(9, self, o)

    def __le__(self, o):
        return _binop(10, self, o)

    def __gt__(self, o):
        return _binop(11, self, o)

    def __ge__(self, o):
        return _binop(12, self, o)

    def __and__(self, o):
        return _binop(13, self, o)

    def __rand__(self, o):
        return _binop(13, o, self)

    def __or__(self, o):
        return _binop(14, self, o)

    def __ror__(self, o):
        return _binop(14, o, self)

    def __xor__(self, o):
        return _binop(15, self, o)

    def __rxor__(self, o):
        return _binop(15, o, self)

    def __lshift__(self, o):
        return _binop(20, self, o)

    def __rshift__(self, o):
        return _binop(21, self, o)

    def __neg__(self):
        return _unop(0, self)

    def __pos__(self):
        return self.copy()

    def __abs__(self):
        return _unop(1, self)

    def __invert__(self):
        return _unop(11, self)

    def __matmul__(self, o):
        return matmul(self, o)

    def __rmatmul__(self, o):
        return matmul(o, self)

    def _inplace(self, op, o):
        r = _binop(op, self, o)
        if _ORDER[r._dtype.kind] > _ORDER[self._dtype.kind]:
            raise TypeError("Cannot cast ufunc '%s' output from dtype('%s') to dtype('%s') with casting rule "
                            "'same_kind'" % (_BIN_NAMES[op], r._dtype.name, self._dtype.name))
        self[...] = r
        return self

    def __iadd__(self, o):
        return self._inplace(0, o)

    def __isub__(self, o):
        return self._inplace(1, o)

    def __imul__(self, o):
        return self._inplace(2, o)

    def __itruediv__(self, o):
        return self._inplace(3, o)

    def __ifloordiv__(self, o):
        return self._inplace(4, o)

    def __imod__(self, o):
        return self._inplace(5, o)

    def __ipow__(self, o):
        return self._inplace(6, o)

    # methods that mirror functions
    def sum(self, axis=None, dtype=None, keepdims=False):
        return sum(self, axis=axis, keepdims=keepdims)

    def prod(self, axis=None, keepdims=False):
        return prod(self, axis=axis, keepdims=keepdims)

    def mean(self, axis=None, keepdims=False):
        return mean(self, axis=axis, keepdims=keepdims)

    def std(self, axis=None, ddof=0, keepdims=False):
        return std(self, axis=axis, ddof=ddof, keepdims=keepdims)

    def var(self, axis=None, ddof=0, keepdims=False):
        return var(self, axis=axis, ddof=ddof, keepdims=keepdims)

    def min(self, axis=None, keepdims=False):
        return amin(self, axis=axis, keepdims=keepdims)

    def max(self, axis=None, keepdims=False):
        return amax(self, axis=axis, keepdims=keepdims)

    def argmin(self, axis=None):
        return argmin(self, axis)

    def argmax(self, axis=None):
        return argmax(self, axis)

    def any(self, axis=None, keepdims=False):
        return any(self, axis=axis, keepdims=keepdims)

    def all(self, axis=None, keepdims=False):
        return all(self, axis=axis, keepdims=keepdims)

    def cumsum(self, axis=None):
        return cumsum(self, axis)

    def cumprod(self, axis=None):
        return cumprod(self, axis)

    def dot(self, o):
        return dot(self, o)

    def clip(self, a_min=None, a_max=None):
        return clip(self, a_min, a_max)

    def round(self, decimals=0):
        return round(self, decimals)

    def sort(self, axis=-1):
        self[...] = sort(self, axis)

    def argsort(self, axis=-1):
        return argsort(self, axis)

    def nonzero(self):
        return nonzero(self)

    def __repr__(self):
        return _repr(self)

    def __str__(self):
        return _str(self)

    def __format__(self, spec):
        if self.ndim == 0:
            return format(self.item(), spec)
        return str(self)


def _shape_str(shape):
    shape = tuple(shape)
    return "(%d,)" % shape[0] if len(shape) == 1 else "(" + ", ".join(str(s) for s in shape) + ")"


def _tight(shape):
    return "(" + ",".join(str(s) for s in shape) + ("," if len(shape) == 1 else "") + ")"


def _axis(a, ndim):
    if a < -ndim or a >= ndim:
        raise AxisError("axis %d is out of bounds for array of dimension %d" % (a, ndim))
    return a % ndim


class AxisError(ValueError, IndexError):
    pass


# ---- construction ---------------------------------------------------------

def _flatten_nested(obj, shape, out, depth):
    if isinstance(obj, ndarray):
        obj = obj.tolist()
    if isinstance(obj, (list, tuple)):
        if len(shape) <= depth:
            shape.append(len(obj))
        elif shape[depth] != len(obj):
            raise ValueError("setting an array element with a sequence. The requested array has an "
                             "inhomogeneous shape after %d dimensions." % depth)
        for x in obj:
            _flatten_nested(x, shape, out, depth + 1)
    else:
        if len(shape) != depth and out:
            raise ValueError("setting an array element with a sequence. The requested array has an "
                             "inhomogeneous shape after %d dimensions." % len(shape))
        out.append(obj)


def array(obj, dtype=None, copy=True, ndmin=0):
    if isinstance(obj, ndarray):
        r = obj.astype(dtype) if dtype is not None else (obj.copy() if copy else obj)
    else:
        if isinstance(obj, (float64, int64, bool_)):
            obj = obj.item()
        shape, flat = [], []
        _flatten_nested(obj, shape, flat, 0)
        if dtype is not None:
            kind = globals()["dtype"](dtype).kind
        elif flat:
            kind = "b"
            for v in flat:
                if isinstance(v, (float64, int64, bool_)):
                    v = v.item()
                k = _kind_of_value(v)
                if _ORDER[k] > _ORDER[kind]:
                    kind = k
        else:
            kind = "f"
        flat = [v.item() if isinstance(v, (float64, int64, bool_)) else v for v in flat]
        r = ndarray._wrap(_cn1np.fromflat(kind, flat), shape)
    while r.ndim < ndmin:
        r = r.reshape((1,) + r._shape)
    return r


def asarray(obj, dtype=None):
    if isinstance(obj, ndarray) and dtype is None:
        return obj
    return array(obj, dtype)


asanyarray = asarray


def _shape_arg(shape):
    return (shape,) if isinstance(shape, int) else tuple(shape)


def zeros(shape, dtype=float):
    shape = _shape_arg(shape)
    return ndarray._wrap(_cn1np.alloc(globals()["dtype"](dtype).kind, _prod(shape)), shape)


def empty(shape, dtype=float):
    return zeros(shape, dtype)


def ones(shape, dtype=float):
    return full(shape, 1, dtype)


def full(shape, fill_value, dtype=None):
    if dtype is None:
        dtype = _NAMES[_kind_of_value(fill_value)]
    r = zeros(shape, dtype)
    r.fill(fill_value)
    return r


def zeros_like(a, dtype=None):
    a = asarray(a)
    return zeros(a.shape, dtype or a.dtype)


def ones_like(a, dtype=None):
    a = asarray(a)
    return ones(a.shape, dtype or a.dtype)


def full_like(a, fill_value, dtype=None):
    a = asarray(a)
    return full(a.shape, fill_value, dtype or a.dtype)


empty_like = zeros_like


def arange(start, stop=None, step=1, dtype=None):
    if stop is None:
        start, stop = 0, start
    kind = "f" if _b.any(isinstance(v, float) for v in (start, stop, step)) else "i"
    if dtype is not None:
        kind = globals()["dtype"](dtype).kind
    n = _b.max(0, int(_math.ceil((stop - start) / step)))
    return ndarray._wrap(_cn1np.arange(kind, start, step, n), (n,))


def linspace(start, stop, num=50, endpoint=True, retstep=False, dtype=None):
    div = (num - 1) if endpoint else num
    step = (stop - start) / div if div > 0 else nan
    vals = [start + i * step for i in range(num)]
    if endpoint and num > 1:
        vals[-1] = stop
    r = array([float(v) for v in vals], dtype=dtype or float)
    return (r, step) if retstep else r


def eye(n, m=None, k=0, dtype=float):
    r = zeros((n, m if m is not None else n), dtype)
    for i in range(n):
        j = i + k
        if 0 <= j < r.shape[1]:
            r[i, j] = 1
    return r


def identity(n, dtype=float):
    return eye(n, dtype=dtype)


def diag(v, k=0):
    v = asarray(v)
    if v.ndim == 1:
        n = v.shape[0] + _b.abs(k)
        r = zeros((n, n), v.dtype)
        for i in range(v.shape[0]):
            r[i + _b.max(-k, 0), i + _b.max(k, 0)] = v[i]
        return r
    n = _b.min(v.shape[0] - _b.max(-k, 0), v.shape[1] - _b.max(k, 0))
    return array([v[i + _b.max(-k, 0), i + _b.max(k, 0)] for i in range(_b.max(n, 0))], dtype=v.dtype)


def meshgrid(*xs, indexing="xy"):
    xs = [asarray(x).ravel() for x in xs]
    if len(xs) == 2 and indexing == "xy":
        x, y = xs
        return [broadcast_to(x.reshape(1, -1), (y.size, x.size)).copy(),
                broadcast_to(y.reshape(-1, 1), (y.size, x.size)).copy()]
    shape = tuple(x.size for x in xs)
    out = []
    for i, x in enumerate(xs):
        s = [1] * len(xs)
        s[i] = x.size
        out.append(broadcast_to(x.reshape(s), shape).copy())
    return out


# ---- broadcasting ---------------------------------------------------------

def _broadcast_shapes(*shapes):
    nd = _b.max(len(s) for s in shapes) if shapes else 0
    out = []
    for i in range(nd):
        n = 1
        for s in shapes:
            j = len(s) - nd + i
            d = s[j] if j >= 0 else 1
            if d != 1:
                if n != 1 and n != d:
                    raise ValueError("operands could not be broadcast together with shapes " +
                                     " ".join(_shape_str(x) for x in shapes) + " ")
                n = d
        out.append(n)
    return tuple(out)


broadcast_shapes = _broadcast_shapes


def _bstrides(a, shape):
    nd = len(shape)
    st = []
    for i in range(nd):
        j = a.ndim - nd + i
        if j < 0 or a._shape[j] == 1 and shape[i] != 1:
            st.append(0)
        else:
            st.append(a._st[j])
    return tuple(st)


def broadcast_to(a, shape):
    a = asarray(a)
    shape = _shape_arg(shape)
    if _broadcast_shapes(a._shape, shape) != tuple(shape):
        raise ValueError("operands could not be broadcast together with remapped shapes [original->remapped]: "
                         + _shape_str(a._shape) + " and requested shape " + _shape_str(shape))
    return a._view(shape, _bstrides(a, shape), a._off)


def broadcast_arrays(*arrs):
    arrs = [asarray(a) for a in arrs]
    shape = _broadcast_shapes(*[a._shape for a in arrs])
    return [broadcast_to(a, shape) for a in arrs]


# ---- ufuncs ---------------------------------------------------------------

_BIN_NAMES = {0: "add", 1: "subtract", 2: "multiply", 3: "divide", 4: "floor_divide", 5: "remainder",
              6: "power", 13: "bitwise_and", 14: "bitwise_or", 15: "bitwise_xor"}


def _operand(x):
    """An operand and whether it is a weak Python scalar (NEP 50)."""
    if isinstance(x, ndarray):
        return x, False
    if isinstance(x, (float64, int64, bool_)):
        return array(x.item()), False
    if isinstance(x, (_b.bool, int, float)):
        return array(x), True
    return array(x), False


def _result_kind(a, wa, b, wb):
    ka, kb = a._dtype.kind, b._dtype.kind
    if wa and not wb:
        return kb if _ORDER[kb] >= _ORDER[ka] else ka
    if wb and not wa:
        return ka if _ORDER[ka] >= _ORDER[kb] else kb
    return ka if _ORDER[ka] >= _ORDER[kb] else kb


def _binop(op, x, y):
    if not isinstance(y, (ndarray, list, tuple, int, float, float64, int64, bool_)) and \
            not isinstance(x, (ndarray, list, tuple, int, float, float64, int64, bool_)):
        return NotImplemented
    a, wa = _operand(x)
    b, wb = _operand(y)
    kind = _result_kind(a, wa, b, wb)
    if op in (13, 14, 15, 20, 21) and kind == "f":
        raise TypeError("ufunc '%s' not supported for the input types, and the inputs could not be safely "
                        "coerced to any supported types according to the casting rule ''safe''"
                        % _BIN_NAMES.get(op, "shift"))
    fp = kind == "f"
    if op == 3 or op in (18, 19):
        kind, fp = "f", True
    elif 7 <= op <= 12:
        kind = "b"
    elif kind == "b" and op in (0, 2):
        op = 14 if op == 0 else 13
    elif kind == "b" and op in (1, 4, 5, 6):
        kind = "i"
    shape = _broadcast_shapes(a._shape, b._shape)
    buf = _cn1np.binop(op, kind, fp, shape, a._buf, a._off, _bstrides(a, shape), b._buf, b._off, _bstrides(b, shape))
    r = ndarray._wrap(buf, shape, kind)
    return r if r.ndim or isinstance(x, ndarray) or isinstance(y, ndarray) else r[()]


def _unop(op, x, kind=None, fp=None):
    a = asarray(x)
    k = a._dtype.kind
    if kind is None:
        kind = k
    if fp is None:
        fp = kind == "f" or k == "f"
    buf = _cn1np.unop(op, kind, fp, a._shape, a._buf, a._off, a._st)
    r = ndarray._wrap(buf, a._shape, kind)
    return r if isinstance(x, ndarray) else r[()]


def _float_unop(op):
    def f(x):
        return _unop(op, x, "f", True)
    return f


sqrt = _float_unop(2)
exp = _float_unop(3)
log = _float_unop(4)
sin = _float_unop(5)
cos = _float_unop(6)
tan = _float_unop(7)
log10 = _float_unop(14)
log2 = _float_unop(15)
tanh = _float_unop(16)
arcsin = _float_unop(17)
arccos = _float_unop(18)
arctan = _float_unop(19)
sinh = _float_unop(24)
cosh = _float_unop(25)
expm1 = _float_unop(26)
log1p = _float_unop(27)


def _keep_kind(op):
    def f(x):
        a = asarray(x)
        if a._dtype.kind == "f" or op in (0, 1, 11, 12, 13):
            return _unop(op, x)
        return _unop(op, x, "f", True)
    return f


def floor(x):
    return _unop(8, x, "f", True)


def ceil(x):
    return _unop(9, x, "f", True)


def trunc(x):
    return _unop(28, x, "f", True)


def rint(x):
    return _unop(20, x, "f", True)


negative = _keep_kind(0)
absolute = abs_ = _keep_kind(1)
fabs = _float_unop(1)
sign = _keep_kind(12)
square = _keep_kind(13)
invert = bitwise_not = _keep_kind(11)


def logical_not(x):
    return _unop(10, x, "b")


def isnan(x):
    return _unop(21, x, "b")


def isinf(x):
    return _unop(22, x, "b")


def isfinite(x):
    return _unop(23, x, "b")


def _bin(op):
    def f(x, y):
        return _binop(op, x, y)
    return f


add = _bin(0)
subtract = _bin(1)
multiply = _bin(2)
divide = true_divide = _bin(3)
floor_divide = _bin(4)
mod = remainder = _bin(5)
power = _bin(6)
equal = _bin(7)
not_equal = _bin(8)
less = _bin(9)
less_equal = _bin(10)
greater = _bin(11)
greater_equal = _bin(12)
bitwise_and = _bin(13)
bitwise_or = _bin(14)
bitwise_xor = _bin(15)
maximum = _bin(17)
minimum = _bin(16)
arctan2 = _bin(18)
hypot = _bin(19)
left_shift = _bin(20)
right_shift = _bin(21)


def logical_and(x, y):
    return _binop(13, asarray(x).astype(bool), asarray(y).astype(bool))


def logical_or(x, y):
    return _binop(14, asarray(x).astype(bool), asarray(y).astype(bool))


def logical_xor(x, y):
    return _binop(15, asarray(x).astype(bool), asarray(y).astype(bool))


def round(a, decimals=0):
    a = asarray(a)
    if a._dtype.kind != "f":
        return a.copy()
    if decimals == 0:
        return _unop(20, a)
    f = 10.0 ** decimals
    return _unop(20, a * f) / f


around = round


def clip(a, a_min, a_max):
    r = asarray(a)
    if a_min is not None:
        r = maximum(r, a_min)
    if a_max is not None:
        r = minimum(r, a_max)
    return r


def where(cond, x=None, y=None):
    c = asarray(cond)
    if x is None and y is None:
        return nonzero(c)
    a, wa = _operand(x)
    b, wb = _operand(y)
    kind = _result_kind(a, wa, b, wb)
    shape = _broadcast_shapes(c._shape, a._shape, b._shape)
    buf = _cn1np.where(kind, shape, c._buf, c._off, _bstrides(c, shape), a._buf, a._off, _bstrides(a, shape),
                       b._buf, b._off, _bstrides(b, shape))
    return ndarray._wrap(buf, shape, kind)


def nonzero(a):
    a = asarray(a)
    mask = a != 0
    out = []
    for d in range(a.ndim):
        idx = arange(a.shape[d]).reshape([a.shape[d] if i == d else 1 for i in range(a.ndim)])
        out.append(broadcast_to(idx, a.shape)[mask])
    return tuple(out)


def argwhere(a):
    nz = nonzero(a)
    return stack(nz, axis=1) if nz else zeros((0, 0), int)


def count_nonzero(a, axis=None):
    return sum(asarray(a) != 0, axis=axis)


# ---- reductions -----------------------------------------------------------

def _reduce(op, a, axis, kind, keepdims=False, fp=None):
    a = asarray(a)
    if fp is None:
        fp = a._dtype.kind == "f"
    if axis is None:
        buf = _cn1np.reduce(op, kind, fp, a._shape, a._buf, a._off, a._st, -1)
        r = ndarray._wrap(buf, (), kind)
        if keepdims:
            return r.reshape((1,) * a.ndim)
        return r[()]
    if isinstance(axis, tuple):
        r = a
        for ax in sorted((_axis(x, a.ndim) for x in axis), reverse=True):
            r = _reduce(op, r, ax, kind, True, fp)
        return r if keepdims else r.reshape([s for i, s in enumerate(r.shape)
                                             if i not in [_axis(x, a.ndim) for x in axis]])
    ax = _axis(axis, a.ndim)
    buf = _cn1np.reduce(op, kind, fp, a._shape, a._buf, a._off, a._st, ax)
    shape = a._shape[:ax] + ((1,) if keepdims else ()) + a._shape[ax + 1:]
    return ndarray._wrap(buf, shape, kind)


def _sum_kind(a):
    k = asarray(a)._dtype.kind
    return "i" if k == "b" else k


def sum(a, axis=None, dtype=None, keepdims=False):
    a = asarray(a)
    return _reduce(0, a, axis, _sum_kind(a), keepdims)


def prod(a, axis=None, keepdims=False):
    a = asarray(a)
    return _reduce(1, a, axis, _sum_kind(a), keepdims)


def amin(a, axis=None, keepdims=False):
    a = asarray(a)
    return _reduce(2, a, axis, a._dtype.kind, keepdims)


def amax(a, axis=None, keepdims=False):
    a = asarray(a)
    return _reduce(3, a, axis, a._dtype.kind, keepdims)


min = amin
max = amax


def argmin(a, axis=None):
    return _reduce(4, a, axis, "i")


def argmax(a, axis=None):
    return _reduce(5, a, axis, "i")


def any(a, axis=None, keepdims=False):
    return _reduce(6, a, axis, "b", keepdims)


def all(a, axis=None, keepdims=False):
    return _reduce(7, a, axis, "b", keepdims)


def mean(a, axis=None, keepdims=False):
    a = asarray(a)
    n = a.size if axis is None else _prod([a.shape[_axis(x, a.ndim)] for x in
                                           (axis if isinstance(axis, tuple) else (axis,))])
    s = _reduce(0, a, axis, "f", keepdims, True)
    r = s / n if n else s * nan
    return r if isinstance(r, ndarray) else float64(r)


def var(a, axis=None, ddof=0, keepdims=False):
    a = asarray(a)
    m = mean(a, axis=axis, keepdims=True)
    d = a - m
    n = a.size if axis is None else _prod([a.shape[_axis(x, a.ndim)] for x in
                                           (axis if isinstance(axis, tuple) else (axis,))])
    r = sum(d * d, axis=axis, keepdims=keepdims) / (n - ddof)
    return r if isinstance(r, ndarray) else float64(r)


def std(a, axis=None, ddof=0, keepdims=False):
    return sqrt(var(a, axis=axis, ddof=ddof, keepdims=keepdims))


def median(a, axis=None):
    a = asarray(a)
    if axis is None:
        a, axis = a.ravel(), 0
    s = sort(a, axis)
    n = s.shape[axis]
    lo = take(s, (n - 1) // 2, axis)
    hi = take(s, n // 2, axis)
    return (lo + hi) / 2.0


def percentile(a, q, axis=None):
    a = asarray(a)
    if axis is None:
        a, axis = a.ravel(), 0
    s = sort(a, axis)
    n = s.shape[axis]
    pos = (n - 1) * q / 100.0
    lo = int(_math.floor(pos))
    hi = _b.min(lo + 1, n - 1)
    frac = pos - lo
    vlo = take(s, lo, axis)
    vhi = take(s, hi, axis)
    return vlo + (vhi - vlo) * frac


def quantile(a, q, axis=None):
    return percentile(a, q * 100.0, axis)


def _cumulate(op, a, axis):
    a = asarray(a)
    if axis is None:
        a, axis = a.ravel(), 0
    ax = _axis(axis, a.ndim)
    kind = _sum_kind(a)
    return ndarray._wrap(_cn1np.cumulate(op, kind, a._shape, a._buf, a._off, a._st, ax), a._shape, kind)


def cumsum(a, axis=None):
    return _cumulate(0, a, axis)


def cumprod(a, axis=None):
    return _cumulate(1, a, axis)


def diff(a, n=1, axis=-1):
    a = asarray(a)
    for _ in range(n):
        ax = _axis(axis, a.ndim)
        hi = [slice(None)] * a.ndim
        lo = [slice(None)] * a.ndim
        hi[ax] = slice(1, None)
        lo[ax] = slice(None, -1)
        a = a[tuple(hi)] - a[tuple(lo)]
    return a


def take(a, indices, axis=None):
    a = asarray(a)
    if axis is None:
        a, axis = a.ravel(), 0
    key = [slice(None)] * a.ndim
    key[_axis(axis, a.ndim)] = indices
    return a[tuple(key)]


# ---- sorting and sets -----------------------------------------------------

def _along_last(a, axis):
    a = asarray(a)
    if axis is None:
        return a.ravel(), 0, None
    ax = _axis(axis, a.ndim)
    return a.swapaxes(ax, -1), ax, a.ndim - 1


def sort(a, axis=-1):
    m, ax, last = _along_last(a, axis)
    if m.ndim == 0:
        return m.copy()
    buf = _cn1np.sortrows(m._flatbuf(), m.shape[-1], False)
    r = ndarray._wrap(buf, m.shape, m._dtype.kind)
    return r.swapaxes(ax, -1).copy() if last is not None else r


def argsort(a, axis=-1, kind=None):
    m, ax, last = _along_last(a, axis)
    buf = _cn1np.sortrows(m._flatbuf(), m.shape[-1], True)
    r = ndarray._wrap(buf, m.shape, "i")
    return r.swapaxes(ax, -1).copy() if last is not None else r


def unique(a, return_counts=False, return_index=False):
    s = sort(asarray(a).ravel())
    vals = s.tolist()
    out, counts = [], []
    for v in vals:
        if out and (out[-1] == v or (out[-1] != out[-1] and v != v)):
            counts[-1] += 1
        else:
            out.append(v)
            counts.append(1)
    r = array(out, dtype=s.dtype)
    if return_counts:
        return r, array(counts)
    return r


def searchsorted(a, v, side="left"):
    a = asarray(a).tolist()
    import bisect
    f = bisect.bisect_left if side == "left" else bisect.bisect_right
    if isinstance(v, (int, float)):
        return int64(f(a, v))
    return array([f(a, x) for x in asarray(v).ravel().tolist()]).reshape(asarray(v).shape)


def isin(element, test_elements):
    tests = set(asarray(test_elements).ravel().tolist())
    e = asarray(element)
    return array([x in tests for x in e.ravel().tolist()], dtype=bool).reshape(e.shape)


def bincount(x, minlength=0):
    vals = asarray(x).ravel().tolist()
    n = _b.max([minlength] + [v + 1 for v in vals])
    out = [0] * n
    for v in vals:
        out[v] += 1
    return array(out, dtype=int)


def histogram(a, bins=10, range=None):
    a = asarray(a).ravel()
    lo, hi = (float(a.min()), float(a.max())) if range is None else range
    if lo == hi:
        lo, hi = lo - 0.5, hi + 0.5
    edges = linspace(lo, hi, bins + 1)
    counts = [0] * bins
    width = (hi - lo) / bins
    for v in a.tolist():
        if lo <= v <= hi:
            i = _b.min(int((v - lo) / width), bins - 1)
            counts[i] += 1
    return array(counts), edges


# ---- joining --------------------------------------------------------------

def concatenate(arrays, axis=0):
    arrays = [asarray(a) for a in arrays]
    if axis is None:
        arrays, axis = [a.ravel() for a in arrays], 0
    first = arrays[0]
    ax = _axis(axis, first.ndim)
    kind = "b"
    for a in arrays:
        if a.ndim != first.ndim:
            raise ValueError("all the input array dimensions except for the concatenation axis must match exactly")
        if _ORDER[a._dtype.kind] > _ORDER[kind]:
            kind = a._dtype.kind
    shape = list(first.shape)
    shape[ax] = _b.sum(a.shape[ax] for a in arrays)
    r = zeros(shape, _NAMES[kind])
    at = 0
    for a in arrays:
        key = [slice(None)] * len(shape)
        key[ax] = slice(at, at + a.shape[ax])
        r[tuple(key)] = a
        at += a.shape[ax]
    return r


def stack(arrays, axis=0):
    arrays = [asarray(a) for a in arrays]
    nd = arrays[0].ndim + 1
    ax = _axis(axis, nd)
    return concatenate([a.reshape(a.shape[:ax] + (1,) + a.shape[ax:]) for a in arrays], axis=ax)


def vstack(arrays):
    return concatenate([atleast_2d(a) for a in arrays], axis=0)


def hstack(arrays):
    arrays = [atleast_1d(a) for a in arrays]
    return concatenate(arrays, axis=0 if arrays[0].ndim == 1 else 1)


def column_stack(arrays):
    return hstack([a.reshape(-1, 1) if asarray(a).ndim == 1 else a for a in [asarray(x) for x in arrays]])


def atleast_1d(a):
    a = asarray(a)
    return a.reshape(1) if a.ndim == 0 else a


def atleast_2d(a):
    a = asarray(a)
    return a.reshape(1, -1) if a.ndim < 2 else a


def split(a, sections, axis=0):
    a = asarray(a)
    ax = _axis(axis, a.ndim)
    n = a.shape[ax]
    if isinstance(sections, int):
        if n % sections:
            raise ValueError("array split does not result in an equal division")
        cuts = [n // sections * i for i in range(1, sections)]
    else:
        cuts = list(sections)
    out = []
    prev = 0
    for c in cuts + [n]:
        key = [slice(None)] * a.ndim
        key[ax] = slice(prev, c)
        out.append(a[tuple(key)])
        prev = c
    return out


def reshape(a, shape):
    return asarray(a).reshape(shape)


def ravel(a):
    return asarray(a).ravel()


def transpose(a, axes=None):
    return asarray(a).transpose(*(() if axes is None else (axes,)))


def squeeze(a, axis=None):
    return asarray(a).squeeze(axis)


def expand_dims(a, axis):
    a = asarray(a)
    ax = _axis(axis, a.ndim + 1)
    return a.reshape(a.shape[:ax] + (1,) + a.shape[ax:])


def flip(a, axis=None):
    a = asarray(a)
    axes = range(a.ndim) if axis is None else [_axis(axis, a.ndim)]
    key = [slice(None)] * a.ndim
    for ax in axes:
        key[ax] = slice(None, None, -1)
    return a[tuple(key)]


def tile(a, reps):
    a = asarray(a)
    if isinstance(reps, int):
        reps = (reps,)
    r = a
    while r.ndim < len(reps):
        r = r.reshape((1,) + r.shape)
    reps = (1,) * (r.ndim - len(reps)) + tuple(reps)
    for ax, n in enumerate(reps):
        r = concatenate([r] * n, axis=ax)
    return r


def repeat(a, repeats, axis=None):
    a = asarray(a)
    if axis is None:
        a, axis = a.ravel(), 0
    ax = _axis(axis, a.ndim)
    idx = []
    for i in range(a.shape[ax]):
        idx.extend([i] * repeats)
    return take(a, idx, ax)


def copy(a):
    return asarray(a).copy()


def shape(a):
    return asarray(a).shape


def ndim(a):
    return asarray(a).ndim


def size(a):
    return asarray(a).size


# ---- linear algebra -------------------------------------------------------

def matmul(a, b):
    a, b = asarray(a), asarray(b)
    if a.ndim == 0 or b.ndim == 0:
        raise ValueError("matmul: Input operand does not have enough dimensions")
    va, vb = a.ndim == 1, b.ndim == 1
    if va:
        a = a.reshape(1, -1)
    if vb:
        b = b.reshape(-1, 1)
    if a.ndim > 2 or b.ndim > 2:
        return stack([matmul(a[i], b[i] if b.ndim > 2 else b) for i in range(a.shape[0])])
    n, k = a.shape
    k2, m = b.shape
    if k != k2:
        raise ValueError("matmul: Input operand 1 has a mismatch in its core dimension 0, with gufunc signature "
                         "(n?,k),(k,m?)->(n?,m?) (size %d is different from %d)" % (k2, k))
    kind = a._dtype.kind if _ORDER[a._dtype.kind] >= _ORDER[b._dtype.kind] else b._dtype.kind
    kind = "i" if kind == "b" else kind
    buf = _cn1np.matmul(kind, n, k, m, a._buf, a._off, a._st, b._buf, b._off, b._st)
    r = ndarray._wrap(buf, (n, m), kind)
    if va and vb:
        return r[0, 0]
    if va:
        return r.reshape(m)
    if vb:
        return r.reshape(n)
    return r


def dot(a, b):
    a, b = asarray(a), asarray(b)
    if a.ndim == 0 or b.ndim == 0:
        return a * b
    return matmul(a, b)


def vdot(a, b):
    return dot(asarray(a).ravel(), asarray(b).ravel())


def inner(a, b):
    return dot(a, b)


def outer(a, b):
    return asarray(a).reshape(-1, 1) * asarray(b).reshape(1, -1)


def cross(a, b):
    a, b = asarray(a), asarray(b)
    return array([a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]])


def trace(a):
    return sum(diag(a))


def allclose(a, b, rtol=1e-05, atol=1e-08):
    return _b.bool(all(isclose(a, b, rtol, atol)))


def isclose(a, b, rtol=1e-05, atol=1e-08):
    a, b = asarray(a).astype(float), asarray(b).astype(float)
    return absolute(a - b) <= atol + rtol * absolute(b)


def array_equal(a, b):
    a, b = asarray(a), asarray(b)
    return a.shape == b.shape and _b.bool(all(a == b))


def interp(x, xp, fp):
    xp = asarray(xp).tolist()
    fp = asarray(fp).tolist()
    import bisect

    def one(v):
        if v <= xp[0]:
            return float(fp[0])
        if v >= xp[-1]:
            return float(fp[-1])
        i = bisect.bisect_right(xp, v) - 1
        t = (v - xp[i]) / (xp[i + 1] - xp[i])
        return fp[i] + t * (fp[i + 1] - fp[i])
    if isinstance(x, (int, float)):
        return float64(one(x))
    return array([one(v) for v in asarray(x).ravel().tolist()]).reshape(asarray(x).shape)


def polyval(p, x):
    r = 0.0 * asarray(x)
    for c in p:
        r = r * x + c
    return r


def convolve(a, v, mode="full"):
    a = asarray(a).tolist()
    v = asarray(v).tolist()
    n = len(a) + len(v) - 1
    out = [0] * n
    for i, x in enumerate(a):
        for j, y in enumerate(v):
            out[i + j] += x * y
    r = array(out)
    if mode == "same":
        m = _b.max(len(a), len(v))
        s = (n - m) // 2
        return r[s:s + m]
    if mode == "valid":
        m = _b.max(len(a), len(v)) - _b.min(len(a), len(v)) + 1
        s = _b.min(len(a), len(v)) - 1
        return r[s:s + m]
    return r


# ---- printing -------------------------------------------------------------

def _fmt_positional(x):
    if x != x:
        return "nan"
    if x in (inf, -inf):
        return "inf" if x > 0 else "-inf"
    s = repr(float(x))
    if "e" in s or len(s.split(".")[1]) > 8:
        s = "%.8f" % x
        s = s.rstrip("0")
        if s == "-0.":
            s = "-0."
    if s.endswith(".0"):
        s = s[:-1]
    return s


def _fmt_scientific(x):
    if x != x:
        return "nan"
    if x in (inf, -inf):
        return "inf" if x > 0 else "-inf"
    s = "%.8e" % x
    mant, ex = s.split("e")
    # the shortest mantissa that reads back as x at 8 digits
    for p in range(0, 9):
        cand = "%.*e" % (p, x)
        if float(cand) == float(s) or float(cand) == x:
            mant, ex = cand.split("e")
            break
    if "." not in mant:
        mant += "."
    else:
        mant = mant.rstrip("0")
    sign = ex[0]
    digits = ex[1:].lstrip("0")
    return mant + "e" + sign + digits.rjust(2, "0")


def _float_formatter(vals):
    finite = [_b.abs(v) for v in vals if v == v and v not in (inf, -inf)]
    nonzero_ = [v for v in finite if v != 0]
    exp_mode = False
    if nonzero_:
        mx, mn = _b.max(nonzero_), _b.min(nonzero_)
        exp_mode = mx >= 1e8 or mn < 0.0001 or mx / mn > 1000.0
    if exp_mode:
        strs = [_fmt_scientific(v) for v in vals]
        parts = [s.split("e") if "e" in s else (s, None) for s in strs]
        int_w = _b.max(len(p[0].split(".")[0]) for p in parts if p[1] is not None)
        frac_w = _b.max(len(p[0].split(".")[1]) for p in parts if p[1] is not None)
        exp_w = _b.max(len(p[1]) - 1 for p in parts if p[1] is not None)
        width = int_w + 1 + frac_w + 2 + exp_w

        def fmt(v):
            s = _fmt_scientific(v)
            if "e" not in s:
                return s.rjust(width)
            m, x = s.split("e")
            i, f = m.split(".")
            return i.rjust(int_w) + "." + f.ljust(frac_w) + "e" + x[0] + x[1:].rjust(exp_w, "0")
        return fmt
    strs = [_fmt_positional(v) for v in vals]
    num = [s for s in strs if "." in s]
    int_w = _b.max([len(s.split(".")[0]) for s in num] or [0])
    frac_w = _b.max([len(s.split(".")[1]) for s in num] or [0])
    width = _b.max([int_w + 1 + frac_w] + [len(s) for s in strs])

    def fmt(v):
        s = _fmt_positional(v)
        if "." not in s:
            return s.rjust(width)
        i, f = s.split(".")
        return (i.rjust(int_w) + "." + f.ljust(frac_w)).rjust(width)
    return fmt


def _formatter(a, flat):
    k = a._dtype.kind
    if k == "f":
        return _float_formatter(flat)
    if k == "b":
        w = 5 if _b.any(not v for v in flat) else 4
        return lambda v: ("True" if v else "False").rjust(w)
    w = _b.max([len(str(v)) for v in flat] or [1])
    return lambda v: str(v).rjust(w)


def _summarize(a):
    return a.size > 1000


def _extend_line(s, line, word, width, prefix):
    if len(line) + len(word) > width and len(line) > len(prefix):
        s += line.rstrip() + "\n"
        line = prefix
    return s, line + word


def _format_array(nested, ndim, fmt, sep, prefix, width, summarize):
    """numpy's layout: rows wrap at `width`, nested rows hang under the
    opening bracket, and blank lines separate the blocks of 3-d and up."""
    edge = 3

    def rec(x, depth, hanging, curr):
        left = ndim - depth
        if left == 0:
            return fmt(x)
        n = len(x)
        show = summarize and n > 2 * edge
        lead = edge if show else n
        trail = edge if show else 0
        nxt = hanging + " "
        nwidth = curr - 1
        if left == 1:
            elem_w = curr - len(sep.rstrip())
            s, line = "", hanging
            for i in range(lead if show else n - 1):
                s, line = _extend_line(s, line, rec(x[i], depth + 1, nxt, nwidth), elem_w, hanging)
                line += sep
            if show:
                s, line = _extend_line(s, line, "...", elem_w, hanging)
                line += sep
                for i in range(trail, 1, -1):
                    s, line = _extend_line(s, line, rec(x[n - i], depth + 1, nxt, nwidth), elem_w, hanging)
                    line += sep
            s, line = _extend_line(s, line, rec(x[n - 1], depth + 1, nxt, nwidth), curr, hanging)
            s += line
        else:
            s = ""
            line_sep = sep.rstrip() + "\n" * (left - 1)
            for i in range(lead if show else n - 1):
                s += hanging + rec(x[i], depth + 1, nxt, nwidth) + line_sep
            if show:
                s += hanging + "..." + line_sep
                for i in range(trail, 1, -1):
                    s += hanging + rec(x[n - i], depth + 1, nxt, nwidth) + line_sep
            s += hanging + rec(x[n - 1], depth + 1, nxt, nwidth)
        return "[" + s[len(hanging):] + "]"

    return rec(nested, 0, prefix, width)


def _flat_for_format(a):
    if _summarize(a) and a.ndim:
        key_parts = []
        r = a
        for d in range(a.ndim):
            if a.shape[d] > 6:
                idx = list(range(3)) + list(range(a.shape[d] - 3, a.shape[d]))
                r = take(r, idx, d)
        return r.ravel().tolist()
    return a.ravel().tolist()


def _repr(a):
    suffix = ""
    if a.size == 0:
        suffix = ", dtype=" + a._dtype.name
    if a.ndim == 0:
        fmt = _formatter(a, [a.item()])
        return "array(" + fmt(a.item()).strip() + suffix + ")"
    if a.size == 0:
        return "array(" + "[" * a.ndim + "]" * a.ndim + ", shape=" + _shape_str(a.shape) + suffix + ")" \
            if a.shape != (0,) else "array([]" + suffix + ")"
    fmt = _formatter(a, _flat_for_format(a))
    body = _format_array(a.tolist(), a.ndim, fmt, ", ", " " * 7, 74, _summarize(a))
    return "array(" + body + suffix + ")"


def _str(a):
    if a.ndim == 0:
        return str(a.item())
    if a.size == 0:
        return "[" * a.ndim + "]" * a.ndim
    fmt = _formatter(a, _flat_for_format(a))
    return _format_array(a.tolist(), a.ndim, fmt, " ", " ", 75, _summarize(a))


def set_printoptions(**kwargs):
    pass


from . import linalg  # noqa: E402
from . import random  # noqa: E402
