# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""A pandas subset: Series and DataFrame over plain Python lists.

A column is a list of Python values plus a dtype name -- int64, float64,
bool, str or object -- inferred the way pandas infers it: a missing value in
a numeric column promotes it to float64 and is stored as float('nan').
Conversion to numpy happens only at the edges (`values`, `to_numpy`).

Formatting follows pandas' default repr for frames that fit the screen.
Not provided: datetime indexes, MultiIndex, pivot, categoricals, rolling
windows, and the truncated repr of long frames.
"""

import math as _math
import re as _re
import json as _json
import csv as _csv
import io as _io
import collections as _collections
import builtins as _b
import numpy as _np

__version__ = "3.1.0"

nan = float("nan")
NA = nan


# ---- values and dtypes ----------------------------------------------------

def _isna(v):
    return v is None or (isinstance(v, float) and v != v)


def isna(v):
    if isinstance(v, (Series, DataFrame)):
        return v.isna()
    return _isna(v)


isnull = isna


def notna(v):
    if isinstance(v, (Series, DataFrame)):
        return v.notna()
    return not _isna(v)


notnull = notna


class _Dtype:
    def __init__(self, name):
        self.name = name

    def __str__(self):
        return self.name

    def __repr__(self):
        if self.name in ("str", "object"):
            return self.name
        return "dtype('" + self.name + "')"

    def __eq__(self, other):
        if isinstance(other, _Dtype):
            return other.name == self.name
        return _dtype_name(other) == self.name

    def __ne__(self, other):
        return not self.__eq__(other)

    def __hash__(self):
        return hash(self.name)

    @property
    def kind(self):
        return {"int64": "i", "float64": "f", "bool": "b"}.get(self.name, "O")


def _dtype_name(t):
    if isinstance(t, _Dtype):
        return t.name
    if t is int or t == "int" or t == "int64" or t == "int32":
        return "int64"
    if t is float or t == "float" or t == "float64" or t == "float32":
        return "float64"
    if t is bool or t == "bool":
        return "bool"
    if t is str or t == "str" or t == "string":
        return "str"
    return "object"


def _is_num(v):
    return isinstance(v, (int, float)) and not isinstance(v, bool)


def _infer(values):
    """Returns (dtype name, values converted to that dtype's storage)."""
    if not values:
        return "object", []
    kinds = set()
    for v in values:
        if v is None or (isinstance(v, float) and v != v):
            kinds.add("na")
        elif isinstance(v, bool):
            kinds.add("bool")
        elif isinstance(v, int):
            kinds.add("int")
        elif isinstance(v, float):
            kinds.add("float")
        elif isinstance(v, str):
            kinds.add("str")
        else:
            kinds.add("obj")
    if kinds == {"bool"}:
        return "bool", list(values)
    if kinds == {"int"}:
        return "int64", list(values)
    if kinds <= {"int", "float", "na"}:
        return "float64", [nan if _isna(v) else float(v) for v in values]
    if kinds <= {"str", "na"} and "str" in kinds:
        return "str", [nan if _isna(v) else v for v in values]
    return "object", list(values)


def _cast(values, dt):
    if dt == "float64":
        return [nan if _isna(v) else float(v) for v in values]
    if dt == "int64":
        out = []
        for v in values:
            if _isna(v):
                raise ValueError("cannot convert NA to integer")
            out.append(int(v))
        return out
    if dt == "bool":
        return [bool(v) for v in values]
    if dt == "str":
        return [v if _isna(v) else str(v) for v in values]
    return list(values)


def _numeric(dt):
    return dt in ("int64", "float64", "bool")


# ---- formatting -----------------------------------------------------------

def _fmt_float_list(vals):
    finite = [abs(v) for v in vals if not _isna(v) and v not in (float("inf"), float("-inf"))]
    sci = any(v > 1e6 for v in finite) or any(0 < v < 1e-6 for v in finite)
    out = []
    for v in vals:
        if _isna(v):
            out.append("NaN")
        elif v == float("inf"):
            out.append(" inf")
        elif v == float("-inf"):
            out.append("-inf")
        elif sci:
            out.append("%.6e" % v if v < 0 else " %.6e" % v)
        else:
            out.append("%.6f" % v if v < 0 else " %.6f" % v)
    if sci:
        return out

    def num(x):
        return "." in x and x.strip() not in ("NaN", "inf", "-inf")
    while True:
        nums = [x for x in out if num(x)]
        if not nums or not all(x.endswith("0") for x in nums):
            break
        out = [x[:-1] if num(x) else x for x in out]
    return [x + "0" if num(x) and x.endswith(".") else x for x in out]


def _fmt_obj(v):
    if v is None:
        return "None"
    if isinstance(v, float) and v != v:
        return "NaN"
    return str(v)


def _fmt_values(values, dt):
    if dt == "float64":
        return _fmt_float_list(values)
    if dt == "int64":
        return [str(v) if v < 0 else " " + str(v) for v in values]
    return [" " + _fmt_obj(v) for v in values]


def _rjust_all(strs):
    w = max([len(x) for x in strs] + [0])
    return [x.rjust(w) for x in strs]


def _label(v):
    return _fmt_obj(v)


# ---- Index ----------------------------------------------------------------

class Index:
    def __init__(self, data=None, name=None, dtype=None):
        if isinstance(data, Index):
            data = data._v
        self._v = list(data) if data is not None else []
        self.name = name
        self._range = False
        self._pos = None

    @classmethod
    def _rng(cls, n):
        ix = cls(range(n))
        ix._range = True
        return ix

    def _loc(self):
        if self._pos is None:
            d = {}
            for i, v in enumerate(self._v):
                if v not in d:
                    d[v] = i
            self._pos = d
        return self._pos

    def get_loc(self, key):
        d = self._loc()
        if key in d:
            return d[key]
        raise KeyError(key)

    def __len__(self):
        return len(self._v)

    def __iter__(self):
        return iter(self._v)

    def __contains__(self, key):
        return key in self._loc()

    def __getitem__(self, i):
        if isinstance(i, slice):
            return Index(self._v[i], self.name)
        return self._v[i]

    def tolist(self):
        return list(self._v)

    to_list = tolist

    @property
    def values(self):
        return _to_np(self._v, self.dtype.name)

    @property
    def dtype(self):
        return _Dtype(_infer(self._v)[0] if self._v else "object")

    def equals(self, other):
        return list(other) == self._v

    def __eq__(self, other):
        if isinstance(other, (list, Index)):
            return [a == b for a, b in zip(self._v, other)]
        return [a == other for a in self._v]

    def __hash__(self):
        return id(self)

    def __repr__(self):
        if self._range:
            return "RangeIndex(start=0, stop=%d, step=1)" % len(self._v)
        items = ", ".join(repr(v) for v in self._v)
        s = "Index([" + items + "], dtype='" + self.dtype.name + "'"
        if self.name is not None:
            s += ", name=" + repr(self.name)
        return s + ")"

    __str__ = __repr__


RangeIndex = Index


def _to_np(values, dt):
    if dt in ("int64", "float64", "bool"):
        return _np.array(values)
    return list(values)


# ---- Series ---------------------------------------------------------------

def _same_index(a, b):
    return a._v == b._v


def _union(a, b):
    seen = set(a._v)
    out = list(a._v)
    for v in b._v:
        if v not in seen:
            seen.add(v)
            out.append(v)
    try:
        out.sort()
    except TypeError:
        pass
    return out


class Series:
    def __init__(self, data=None, index=None, dtype=None, name=None, copy=False):
        if isinstance(data, Series):
            if name is None:
                name = data.name
            if index is None:
                index = data._index
            data = data._v
        if isinstance(data, dict):
            if index is None:
                index = list(data.keys())
            data = list(data.values())
        elif data is None:
            data = []
        elif isinstance(data, _np.ndarray):
            data = data.tolist()
        elif isinstance(data, (str, int, float, bool)):
            n = len(index) if index is not None else 1
            data = [data] * n
        else:
            data = list(data)
        if dtype is not None:
            self._dt = _dtype_name(dtype)
            self._v = _cast(data, self._dt)
        else:
            self._dt, self._v = _infer(data)
        if index is None:
            self._index = Index._rng(len(self._v))
        elif isinstance(index, Index):
            self._index = index
        else:
            self._index = Index(index)
        self.name = name

    @classmethod
    def _make(cls, values, index, name=None, dt=None):
        s = cls.__new__(cls)
        if dt is None:
            s._dt, s._v = _infer(values)
        else:
            s._dt, s._v = dt, values
        s._index = index if isinstance(index, Index) else Index(index)
        s.name = name
        return s

    # attributes
    @property
    def index(self):
        return self._index

    @index.setter
    def index(self, value):
        self._index = value if isinstance(value, Index) else Index(value)

    @property
    def dtype(self):
        return _Dtype(self._dt)

    dtypes = dtype

    @property
    def values(self):
        return _to_np(self._v, self._dt)

    def to_numpy(self):
        return _to_np(self._v, self._dt)

    def tolist(self):
        return list(self._v)

    to_list = tolist

    @property
    def shape(self):
        return (len(self._v),)

    @property
    def size(self):
        return len(self._v)

    @property
    def empty(self):
        return len(self._v) == 0

    @property
    def loc(self):
        return _SLoc(self)

    @property
    def iloc(self):
        return _SILoc(self)

    @property
    def str(self):
        return _StrAccessor(self)

    def __len__(self):
        return len(self._v)

    def __iter__(self):
        return iter(self._v)

    def __contains__(self, key):
        return key in self._index

    def keys(self):
        return self._index

    def items(self):
        return zip(self._index._v, self._v)

    def copy(self):
        return Series._make(list(self._v), Index(self._index._v, self._index.name), self.name, self._dt)

    def _take(self, positions):
        ix = Index([self._index._v[i] for i in positions], self._index.name)
        return Series._make([self._v[i] for i in positions], ix, self.name, self._dt)

    def _mask_positions(self, mask):
        m = mask._v if isinstance(mask, Series) else list(mask)
        return [i for i, b in enumerate(m) if b is True or (b is not False and not _isna(b) and b)]

    def __getitem__(self, key):
        if isinstance(key, Series) and key._dt == "bool":
            return self._take(self._mask_positions(key))
        if isinstance(key, slice):
            return self._take(range(len(self._v))[key])
        if isinstance(key, list):
            if key and isinstance(key[0], bool):
                return self._take(self._mask_positions(key))
            return self._take([self._index.get_loc(k) for k in key])
        if key in self._index:
            return self._v[self._index.get_loc(key)]
        if isinstance(key, int) and not isinstance(key, bool):
            return self._v[key]
        raise KeyError(key)

    def __setitem__(self, key, value):
        if isinstance(key, Series) and key._dt == "bool":
            pos = self._mask_positions(key)
        elif key in self._index:
            pos = [self._index.get_loc(key)]
        else:
            self._index = Index(self._index._v + [key], self._index.name)
            self._v.append(value)
            self._dt, self._v = _infer(self._v)
            return
        vals = list(self._v)
        for p in pos:
            vals[p] = value
        self._dt, self._v = _infer(vals)

    def get(self, key, default=None):
        if key in self._index:
            return self._v[self._index.get_loc(key)]
        return default

    # arithmetic
    def _binop(self, other, op, name=None):
        if isinstance(other, DataFrame):
            return NotImplemented
        if isinstance(other, Series):
            if _same_index(self._index, other._index):
                a, b, ix = self._v, other._v, self._index
            else:
                labels = _union(self._index, other._index)
                a = [self._v[self._index.get_loc(k)] if k in self._index else nan for k in labels]
                b = [other._v[other._index.get_loc(k)] if k in other._index else nan for k in labels]
                ix = Index(labels)
            nm = self.name if self.name == other.name else None
            out = [_apply2(op, x, y) for x, y in zip(a, b)]
            return Series._make(out, ix, nm)
        if isinstance(other, (list, tuple, _np.ndarray)):
            o = list(other)
            return Series._make([_apply2(op, x, y) for x, y in zip(self._v, o)], self._index, self.name)
        return Series._make([_apply2(op, x, other) for x in self._v], self._index, self.name)

    def __add__(self, o): return self._binop(o, "add")
    def __radd__(self, o): return self._binop(o, "radd")
    def __sub__(self, o): return self._binop(o, "sub")
    def __rsub__(self, o): return self._binop(o, "rsub")
    def __mul__(self, o): return self._binop(o, "mul")
    def __rmul__(self, o): return self._binop(o, "mul")
    def __truediv__(self, o): return self._binop(o, "div")
    def __rtruediv__(self, o): return self._binop(o, "rdiv")
    def __floordiv__(self, o): return self._binop(o, "fdiv")
    def __mod__(self, o): return self._binop(o, "mod")
    def __pow__(self, o): return self._binop(o, "pow")
    def __eq__(self, o): return self._binop(o, "eq")
    def __ne__(self, o): return self._binop(o, "ne")
    def __lt__(self, o): return self._binop(o, "lt")
    def __le__(self, o): return self._binop(o, "le")
    def __gt__(self, o): return self._binop(o, "gt")
    def __ge__(self, o): return self._binop(o, "ge")
    def __and__(self, o): return self._binop(o, "and")
    def __or__(self, o): return self._binop(o, "or")
    def __xor__(self, o): return self._binop(o, "xor")

    def __hash__(self):
        return id(self)

    add = __add__
    sub = __sub__
    mul = __mul__
    div = truediv = __truediv__

    def __neg__(self):
        return Series._make([-v for v in self._v], self._index, self.name)

    def __abs__(self):
        return Series._make([abs(v) for v in self._v], self._index, self.name)

    def abs(self):
        return self.__abs__()

    def __invert__(self):
        return Series._make([not v for v in self._v], self._index, self.name, "bool")

    def __bool__(self):
        raise ValueError("The truth value of a Series is ambiguous. "
                         "Use a.empty, a.bool(), a.item(), a.any() or a.all().")

    def round(self, decimals=0):
        if self._dt != "float64":
            return self.copy()
        return Series._make([v if _isna(v) else round(v, decimals) for v in self._v],
                            self._index, self.name, "float64")

    def between(self, left, right):
        return (self >= left) & (self <= right)

    def isin(self, values):
        vs = list(values)
        return Series._make([v in vs for v in self._v], self._index, self.name, "bool")

    # missing data
    def isna(self):
        return Series._make([_isna(v) for v in self._v], self._index, self.name, "bool")

    isnull = isna

    def notna(self):
        return Series._make([not _isna(v) for v in self._v], self._index, self.name, "bool")

    notnull = notna

    def fillna(self, value):
        return Series._make([value if _isna(v) else v for v in self._v], self._index, self.name)

    def dropna(self):
        return self._take([i for i, v in enumerate(self._v) if not _isna(v)])

    # reductions
    def _valid(self):
        return [v for v in self._v if not _isna(v)]

    def sum(self):
        vs = self._valid()
        if self._dt == "str":
            return "".join(vs)
        total = 0
        for v in vs:
            total = total + v
        if self._dt == "bool":
            return int(total)
        return total

    def prod(self):
        p = 1
        for v in self._valid():
            p = p * v
        return p

    def count(self):
        return len(self._valid())

    def mean(self):
        vs = self._valid()
        if not vs:
            return nan
        return float(sum(vs)) / len(vs)

    def var(self, ddof=1):
        vs = self._valid()
        if len(vs) <= ddof:
            return nan
        m = float(sum(vs)) / len(vs)
        return sum((v - m) * (v - m) for v in vs) / (len(vs) - ddof)

    def std(self, ddof=1):
        v = self.var(ddof)
        return v if v != v else _math.sqrt(v)

    def min(self):
        vs = self._valid()
        return min(vs) if vs else nan

    def max(self):
        vs = self._valid()
        return max(vs) if vs else nan

    def idxmax(self):
        best = None
        for k, v in zip(self._index._v, self._v):
            if not _isna(v) and (best is None or v > best[1]):
                best = (k, v)
        return best[0]

    def idxmin(self):
        best = None
        for k, v in zip(self._index._v, self._v):
            if not _isna(v) and (best is None or v < best[1]):
                best = (k, v)
        return best[0]

    def quantile(self, q=0.5):
        return _quantile(sorted(self._valid()), q)

    def median(self):
        return self.quantile(0.5)

    def any(self):
        return any(self._valid())

    def all(self):
        return all(self._valid())

    def nunique(self):
        return len(set(self._valid()))

    def unique(self):
        seen = []
        s = set()
        for v in self._v:
            key = "__nan__" if _isna(v) else v
            if key not in s:
                s.add(key)
                seen.append(v)
        return _to_np(seen, self._dt)

    def value_counts(self, ascending=False):
        counts = {}
        order = []
        for v in self._valid():
            if v not in counts:
                counts[v] = 0
                order.append(v)
            counts[v] += 1
        order.sort(key=lambda k: counts[k], reverse=not ascending)
        return Series._make([counts[k] for k in order], Index(order, self.name), "count", "int64")

    def describe(self):
        vals = [float(self.count()), self.mean(), self.std(), float(self.min()),
                self.quantile(0.25), self.quantile(0.5), self.quantile(0.75), float(self.max())]
        return Series._make(vals, Index(_DESCRIBE), self.name, "float64")

    def cumsum(self):
        out = []
        total = 0
        for v in self._v:
            if _isna(v):
                out.append(nan)
            else:
                total = total + v
                out.append(total)
        return Series._make(out, self._index, self.name)

    def cumprod(self):
        out = []
        total = 1
        for v in self._v:
            if _isna(v):
                out.append(nan)
            else:
                total = total * v
                out.append(total)
        return Series._make(out, self._index, self.name)

    def diff(self):
        out = [nan]
        for i in range(1, len(self._v)):
            out.append(self._v[i] - self._v[i - 1])
        return Series._make(out, self._index, self.name, "float64" if self._v else None)

    # transforms
    def apply(self, func):
        return Series._make([func(v) for v in self._v], self._index, self.name)

    def map(self, arg):
        if isinstance(arg, dict):
            return Series._make([arg.get(v, nan) for v in self._v], self._index, self.name)
        if isinstance(arg, Series):
            return Series._make([arg.get(v, nan) for v in self._v], self._index, self.name)
        return Series._make([v if _isna(v) else arg(v) for v in self._v], self._index, self.name)

    def astype(self, dtype):
        dt = _dtype_name(dtype)
        return Series._make(_cast(self._v, dt), self._index, self.name, dt)

    def rename(self, name):
        s = self.copy()
        s.name = name
        return s

    def reset_index(self, drop=False):
        if drop:
            return Series._make(list(self._v), Index._rng(len(self._v)), self.name, self._dt)
        return DataFrame({self._index.name or "index": self._index._v,
                          (self.name if self.name is not None else 0): self._v})

    def to_frame(self, name=None):
        n = name if name is not None else (self.name if self.name is not None else 0)
        return DataFrame._from_series([Series._make(self._v, self._index, n, self._dt)], self._index)

    def to_dict(self):
        return dict(zip(self._index._v, self._v))

    def head(self, n=5):
        return self._take(range(min(n, len(self._v))))

    def tail(self, n=5):
        return self._take(range(max(0, len(self._v) - n), len(self._v)))

    def sort_values(self, ascending=True):
        return self._take(_order(self._v, ascending))

    def sort_index(self, ascending=True):
        return self._take(_order(self._index._v, ascending))

    def equals(self, other):
        return isinstance(other, Series) and _eq_lists(self._v, other._v) and self._dt == other._dt

    # repr
    def __repr__(self):
        if not self._v:
            return "Series([], " + ("Name: %s, " % self.name if self.name is not None else "") + \
                "dtype: " + self._dt + ")"
        n = len(self._v)
        shown = self
        if n > _MAX_ROWS:
            # display.max_rows exceeded: min_rows of them, half from each end
            shown = self._take(list(range(_MIN_ROWS // 2)) + list(range(n - _MIN_ROWS // 2, n)))
        labels = [_label(v) for v in shown._index._v]
        vals = _rjust_all(_fmt_values(shown._v, shown._dt))
        if shown is not self:
            # SeriesFormatter: dots centred (str.center) in the value width
            dots = ".." if len(vals[_MIN_ROWS // 2 - 1]) <= 3 else "..."
            vals.insert(_MIN_ROWS // 2, dots.center(len(vals[_MIN_ROWS // 2 - 1])))
            labels.insert(_MIN_ROWS // 2, "")
        w = max(len(x) for x in labels)
        lines = []
        if self._index.name is not None:
            lines.append(str(self._index.name))
        for lab, v in zip(labels, vals):
            lines.append(lab.ljust(w) + "   " + v)
        foot = []
        if self.name is not None:
            foot.append("Name: " + str(self.name))
        if shown is not self:
            foot.append("Length: %d" % n)
        foot.append("dtype: " + self._dt)
        lines.append(", ".join(foot))
        return "\n".join(lines)

    __str__ = __repr__


_DESCRIBE = ["count", "mean", "std", "min", "25%", "50%", "75%", "max"]


def _eq_lists(a, b):
    if len(a) != len(b):
        return False
    for x, y in zip(a, b):
        if _isna(x) and _isna(y):
            continue
        if x != y:
            return False
    return True


def _quantile(vs, q):
    if not vs:
        return nan
    pos = (len(vs) - 1) * q
    lo = int(_math.floor(pos))
    hi = min(lo + 1, len(vs) - 1)
    frac = pos - lo
    return float(vs[lo]) + (float(vs[hi]) - float(vs[lo])) * frac


def _order(values, ascending=True):
    good = [i for i, v in enumerate(values) if not _isna(v)]
    bad = [i for i, v in enumerate(values) if _isna(v)]
    good.sort(key=lambda i: values[i], reverse=not ascending)
    return good + bad


def _apply2(op, x, y):
    if op == "eq":
        return x == y
    if op == "ne":
        return x != y
    if _isna(x) or _isna(y):
        if op in ("lt", "le", "gt", "ge"):
            return False
        if op in ("and", "or", "xor"):
            return False
        return nan
    if op == "add":
        return x + y
    if op == "radd":
        return y + x
    if op == "sub":
        return x - y
    if op == "rsub":
        return y - x
    if op == "mul":
        return x * y
    if op == "div":
        if y == 0:
            return nan if x == 0 else (float("inf") if x > 0 else float("-inf"))
        return x / y
    if op == "rdiv":
        if x == 0:
            return nan if y == 0 else (float("inf") if y > 0 else float("-inf"))
        return y / x
    if op == "fdiv":
        return x // y
    if op == "mod":
        return x % y
    if op == "pow":
        return x ** y
    if op == "lt":
        return x < y
    if op == "le":
        return x <= y
    if op == "gt":
        return x > y
    if op == "ge":
        return x >= y
    if op == "and":
        return bool(x and y)
    if op == "or":
        return bool(x or y)
    if op == "xor":
        return bool(x) != bool(y)
    raise ValueError(op)


class _StrAccessor:
    def __init__(self, s):
        self._s = s

    def _map(self, f, dt=None):
        s = self._s
        return Series._make([nan if _isna(v) else f(v) for v in s._v], s._index, s.name, dt)

    def lower(self): return self._map(lambda v: v.lower())
    def upper(self): return self._map(lambda v: v.upper())
    def title(self): return self._map(lambda v: v.title())
    def capitalize(self): return self._map(lambda v: v.capitalize())
    def strip(self, chars=None): return self._map(lambda v: v.strip(chars))
    def lstrip(self, chars=None): return self._map(lambda v: v.lstrip(chars))
    def rstrip(self, chars=None): return self._map(lambda v: v.rstrip(chars))
    def len(self): return self._map(lambda v: len(v))
    def startswith(self, p): return self._map(lambda v: v.startswith(p))
    def endswith(self, p): return self._map(lambda v: v.endswith(p))
    def split(self, sep=None): return self._map(lambda v: v.split(sep), "object")

    def replace(self, pat, repl, regex=False):
        if regex:
            return self._map(lambda v: _re.sub(pat, repl, v))
        return self._map(lambda v: v.replace(pat, repl))

    def contains(self, pat, case=True, regex=True):
        if not regex:
            if case:
                return self._map(lambda v: pat in v)
            return self._map(lambda v: pat.lower() in v.lower())
        flags = 0 if case else _re.IGNORECASE
        return self._map(lambda v: _re.search(pat, v, flags) is not None)

    def __getitem__(self, i):
        return self._map(lambda v: v[i])


class _SLoc:
    def __init__(self, s):
        self._s = s

    def __getitem__(self, key):
        s = self._s
        if isinstance(key, Series) or (isinstance(key, list) and key and isinstance(key[0], bool)):
            return s._take(s._mask_positions(key))
        if isinstance(key, list):
            return s._take([s._index.get_loc(k) for k in key])
        if isinstance(key, slice):
            a = 0 if key.start is None else s._index.get_loc(key.start)
            b = len(s._v) if key.stop is None else s._index.get_loc(key.stop) + 1
            return s._take(range(a, b))
        return s._v[s._index.get_loc(key)]

    def __setitem__(self, key, value):
        self._s[key] = value


class _SILoc:
    def __init__(self, s):
        self._s = s

    def __getitem__(self, key):
        s = self._s
        if isinstance(key, slice):
            return s._take(range(len(s._v))[key])
        if isinstance(key, list):
            return s._take(key)
        return s._v[key]


# ---- DataFrame ------------------------------------------------------------

_MAX_ROWS = 60
_MIN_ROWS = 10
_WIDTH = 80


def _adjoin(strcols):
    return [" ".join(cells) for cells in zip(*strcols)]


def _fit_columns(strcols, max_len):
    """StringFormatter._fit_strcols_to_terminal_width: drop middle columns
    until the repr fits, and answer how many data columns remain."""
    adj_dif = max_len - _WIDTH + 1
    lens = [max(len(x) for x in col) for col in strcols]
    while adj_dif > 0 and len(lens) > 1:
        mid = round(len(lens) / 2)
        adj_dif -= lens[mid] + 1
        del lens[mid]
    return max(len(lens) - 1, 2)


class DataFrame:
    def __init__(self, data=None, index=None, columns=None, dtype=None):
        cols = []
        if data is None:
            data = {}
        if isinstance(data, DataFrame):
            cols = [c.copy() for c in data._cols]
            index = data._index
        elif isinstance(data, dict):
            n = None
            for v in data.values():
                if isinstance(v, Series):
                    if index is None:
                        index = v._index
                    n = len(v)
                elif isinstance(v, (list, tuple, range, _np.ndarray)):
                    n = len(v)
            if n is None:
                n = len(index) if index is not None else 1
            for k, v in data.items():
                if columns is not None and k not in columns:
                    continue
                if isinstance(v, Series):
                    vals = v._v
                elif isinstance(v, (list, tuple, range)):
                    vals = list(v)
                elif isinstance(v, _np.ndarray):
                    vals = v.tolist()
                else:
                    vals = [v] * n
                cols.append(Series(vals, dtype=dtype, name=k))
        elif isinstance(data, (list, tuple)):
            rows = list(data)
            if rows and isinstance(rows[0], dict):
                names = []
                for r in rows:
                    for k in r:
                        if k not in names:
                            names.append(k)
                if columns is not None:
                    names = list(columns)
                for k in names:
                    cols.append(Series([r.get(k) for r in rows], dtype=dtype, name=k))
            elif rows and isinstance(rows[0], Series):
                names = list(rows[0]._index._v)
                for j, k in enumerate(names):
                    cols.append(Series([r._v[j] for r in rows], dtype=dtype, name=k))
                if index is None:
                    index = [r.name for r in rows]
            else:
                width = len(rows[0]) if rows else 0
                names = list(columns) if columns is not None else list(range(width))
                for j, k in enumerate(names):
                    cols.append(Series([r[j] for r in rows], dtype=dtype, name=k))
        elif isinstance(data, _np.ndarray):
            DataFrame.__init__(self, data.tolist(), index, columns, dtype)
            return
        n = len(cols[0]) if cols else (len(index) if index is not None else 0)
        if index is None:
            ix = Index._rng(n)
        elif isinstance(index, Index):
            ix = index
        else:
            ix = Index(index)
        for c in cols:
            c._index = ix
        self._cols = cols
        self._index = ix

    @classmethod
    def _from_series(cls, cols, index):
        df = cls.__new__(cls)
        for c in cols:
            c._index = index
        df._cols = cols
        df._index = index
        return df

    def _col(self, name):
        for c in self._cols:
            if c.name == name:
                return c
        raise KeyError(name)

    def _names(self):
        return [c.name for c in self._cols]

    # attributes
    @property
    def columns(self):
        return Index(self._names())

    @columns.setter
    def columns(self, names):
        for c, n in zip(self._cols, list(names)):
            c.name = n

    @property
    def index(self):
        return self._index

    @index.setter
    def index(self, value):
        ix = value if isinstance(value, Index) else Index(value)
        self._index = ix
        for c in self._cols:
            c._index = ix

    @property
    def shape(self):
        return (len(self._index), len(self._cols))

    @property
    def size(self):
        return len(self._index) * len(self._cols)

    @property
    def empty(self):
        return len(self._index) == 0 or len(self._cols) == 0

    @property
    def dtypes(self):
        return Series._make([_Dtype(c._dt) for c in self._cols], Index(self._names()), None, "object")

    @property
    def values(self):
        return self.to_numpy()

    def to_numpy(self):
        rows = [[c._v[i] for c in self._cols] for i in range(len(self._index))]
        if all(_numeric(c._dt) for c in self._cols):
            return _np.array(rows)
        return rows

    @property
    def loc(self):
        return _DLoc(self, False)

    @property
    def iloc(self):
        return _DLoc(self, True)

    @property
    def T(self):
        rows = [self._row(i) for i in range(len(self._index))]
        return DataFrame({lab: r._v for lab, r in zip(self._index._v, rows)}, index=self._names())

    def __len__(self):
        return len(self._index)

    def __iter__(self):
        return iter(self._names())

    def __contains__(self, name):
        return name in self._names()

    def keys(self):
        return self.columns

    def items(self):
        return [(c.name, c.copy()) for c in self._cols]

    def copy(self):
        ix = Index(self._index._v, self._index.name)
        ix._range = self._index._range
        return DataFrame._from_series([c.copy() for c in self._cols], ix)

    def _take(self, positions, names=None):
        positions = list(positions)
        ix = Index([self._index._v[i] for i in positions], self._index.name)
        if self._index._range and positions == list(range(positions[0] if positions else 0,
                                                          (positions[0] if positions else 0) + len(positions))) \
                and (not positions or positions[0] == 0):
            ix._range = True
        cols = self._cols if names is None else [self._col(n) for n in names]
        out = [Series._make([c._v[i] for i in positions], ix, c.name, c._dt) for c in cols]
        return DataFrame._from_series(out, ix)

    def _row(self, i):
        vals = [c._v[i] for c in self._cols]
        dts = set(c._dt for c in self._cols)
        if len(dts) == 1:
            dt = dts.pop()
        elif dts <= {"int64", "float64"}:
            dt = "float64"
            vals = [float(v) for v in vals]
        else:
            dt = "object"
        return Series._make(vals, Index(self._names()), self._index._v[i], dt)

    def _mask(self, key):
        return self._cols[0]._mask_positions(key) if self._cols else []

    def __getitem__(self, key):
        if isinstance(key, Series) and key._dt == "bool":
            return self._take(self._mask(key))
        if isinstance(key, slice):
            return self._take(range(len(self._index))[key])
        if isinstance(key, (list, Index)):
            key = list(key)
            if key and isinstance(key[0], bool):
                return self._take(self._mask(key))
            return self._take(range(len(self._index)), key)
        c = self._col(key)
        return Series._make(c._v, self._index, c.name, c._dt)

    def __getattr__(self, name):
        if name.startswith("_"):
            raise AttributeError(name)
        for c in self.__dict__.get("_cols", []):
            if c.name == name:
                return self[name]
        raise AttributeError("'DataFrame' object has no attribute '" + name + "'")

    def _as_column(self, value, name):
        n = len(self._index)
        if isinstance(value, Series):
            if _same_index(value._index, self._index):
                vals = list(value._v)
            else:
                vals = [value._v[value._index.get_loc(k)] if k in value._index else nan
                        for k in self._index._v]
        elif isinstance(value, _np.ndarray):
            vals = value.tolist()
        elif isinstance(value, (list, tuple)):
            vals = list(value)
        else:
            vals = [value] * n
        return Series(vals, index=self._index, name=name)

    def __setitem__(self, key, value):
        if isinstance(key, list):
            for k, v in zip(key, value._cols if isinstance(value, DataFrame) else value):
                self[k] = v
            return
        col = self._as_column(value, key)
        for i, c in enumerate(self._cols):
            if c.name == key:
                self._cols[i] = col
                return
        if not self._cols and len(self._index) == 0:
            self._index = Index._rng(len(col))
            col._index = self._index
        self._cols.append(col)

    def __delitem__(self, key):
        self._cols = [c for c in self._cols if c.name != key]

    def insert(self, loc, column, value):
        self._cols.insert(loc, self._as_column(value, column))

    def pop(self, key):
        s = self[key]
        del self[key]
        return s

    def get(self, key, default=None):
        return self[key] if key in self._names() else default

    def head(self, n=5):
        return self._take(range(min(n, len(self._index))))

    def tail(self, n=5):
        return self._take(range(max(0, len(self._index) - n), len(self._index)))

    # missing data
    def isna(self):
        return DataFrame._from_series([c.isna() for c in self._cols], self._index)

    isnull = isna

    def notna(self):
        return DataFrame._from_series([c.notna() for c in self._cols], self._index)

    notnull = notna

    def fillna(self, value):
        out = []
        for c in self._cols:
            v = value.get(c.name, nan) if isinstance(value, dict) else value
            out.append(c.fillna(v))
        return DataFrame._from_series(out, self._index)

    def dropna(self, how="any", subset=None):
        cols = self._cols if subset is None else [self._col(n) for n in subset]
        keep = []
        for i in range(len(self._index)):
            miss = [_isna(c._v[i]) for c in cols]
            if (how == "any" and not any(miss)) or (how == "all" and not all(miss)):
                keep.append(i)
        return self._take(keep)

    # reductions over columns
    def _reduce(self, name, numeric_only=False):
        names, vals = [], []
        for c in self._cols:
            if numeric_only and not _numeric(c._dt):
                continue
            names.append(c.name)
            vals.append(getattr(c, name)())
        return Series._make(vals, Index(names))

    def sum(self, numeric_only=False): return self._reduce("sum", numeric_only)
    def count(self): return self._reduce("count")
    def mean(self, numeric_only=False): return self._reduce("mean", numeric_only)
    def median(self, numeric_only=False): return self._reduce("median", numeric_only)
    def std(self, numeric_only=False): return self._reduce("std", numeric_only)
    def min(self, numeric_only=False): return self._reduce("min", numeric_only)
    def max(self, numeric_only=False): return self._reduce("max", numeric_only)
    def nunique(self): return self._reduce("nunique")

    def describe(self):
        num = [c for c in self._cols if c._dt in ("int64", "float64")]
        out = [c.describe() for c in num]
        ix = Index(_DESCRIBE)
        return DataFrame._from_series([Series._make(s._v, ix, s.name, "float64") for s in out], ix)

    # reshaping
    def sort_values(self, by, ascending=True):
        keys = by if isinstance(by, list) else [by]
        asc = ascending if isinstance(ascending, list) else [ascending] * len(keys)
        pos = list(range(len(self._index)))
        for k, a in reversed(list(zip(keys, asc))):
            vals = self._col(k)._v
            good = [i for i in pos if not _isna(vals[i])]
            bad = [i for i in pos if _isna(vals[i])]
            good.sort(key=lambda i: vals[i], reverse=not a)
            pos = good + bad
        return self._take(pos)

    def sort_index(self, ascending=True):
        return self._take(_order(self._index._v, ascending))

    def reset_index(self, drop=False):
        df = self.copy()
        df.index = Index._rng(len(self._index))
        if not drop:
            name = self._index.name if self._index.name is not None else "index"
            df._cols.insert(0, Series(list(self._index._v), index=df._index, name=name))
        return df

    def set_index(self, key):
        c = self._col(key)
        df = self.drop(columns=[key])
        df.index = Index(list(c._v), key)
        return df

    def groupby(self, by):
        return GroupBy(self, by)

    def merge(self, right, on=None, how="inner", left_on=None, right_on=None, suffixes=("_x", "_y")):
        return merge(self, right, on=on, how=how, left_on=left_on, right_on=right_on, suffixes=suffixes)

    def assign(self, **kwargs):
        df = self.copy()
        for k, v in kwargs.items():
            df[k] = v(df) if callable(v) else v
        return df

    def rename(self, columns=None, index=None):
        df = self.copy()
        if columns:
            for c in df._cols:
                if c.name in columns:
                    c.name = columns[c.name]
        if index:
            df.index = Index([index.get(v, v) for v in df._index._v], df._index.name)
        return df

    def drop(self, labels=None, axis=0, index=None, columns=None):
        if labels is not None:
            if axis in (1, "columns"):
                columns = labels
            else:
                index = labels
        df = self
        if columns is not None:
            gone = columns if isinstance(columns, list) else [columns]
            df = df._take(range(len(df._index)), [n for n in df._names() if n not in gone])
        if index is not None:
            gone = index if isinstance(index, list) else [index]
            df = df._take([i for i, v in enumerate(df._index._v) if v not in gone])
        return df

    def drop_duplicates(self, subset=None):
        names = subset or self._names()
        seen = set()
        keep = []
        for i in range(len(self._index)):
            key = tuple(self._col(n)._v[i] for n in names)
            if key not in seen:
                seen.add(key)
                keep.append(i)
        return self._take(keep)

    def apply(self, func, axis=0):
        if axis in (1, "columns"):
            res = [func(self._row(i)) for i in range(len(self._index))]
            return Series._make(res, self._index)
        res = [func(self[c.name]) for c in self._cols]
        if res and isinstance(res[0], Series):
            return DataFrame._from_series([Series._make(r._v, r._index, c.name, r._dt)
                                           for r, c in zip(res, self._cols)], res[0]._index)
        return Series._make(res, Index(self._names()))

    def applymap(self, func):
        return DataFrame._from_series([c.apply(func) for c in self._cols], self._index)

    map = applymap

    def equals(self, other):
        if not isinstance(other, DataFrame) or self._names() != other._names():
            return False
        return all(a.equals(b) for a, b in zip(self._cols, other._cols))

    # iteration
    def iterrows(self):
        for i in range(len(self._index)):
            yield self._index._v[i], self._row(i)

    def itertuples(self, index=True, name="Pandas"):
        fields = (["Index"] if index else []) + [str(n) for n in self._names()]
        T = _collections.namedtuple(name, fields, rename=True)
        for i in range(len(self._index)):
            vals = ([self._index._v[i]] if index else []) + [c._v[i] for c in self._cols]
            yield T(*vals)

    # conversion
    def to_dict(self, orient="dict"):
        if orient == "records":
            return [{c.name: c._v[i] for c in self._cols} for i in range(len(self._index))]
        if orient == "list":
            return {c.name: list(c._v) for c in self._cols}
        if orient == "index":
            return {self._index._v[i]: {c.name: c._v[i] for c in self._cols}
                    for i in range(len(self._index))}
        return {c.name: dict(zip(self._index._v, c._v)) for c in self._cols}

    def to_csv(self, path_or_buf=None, index=True, sep=",", header=True):
        out = []

        def cell(v):
            if _isna(v):
                return ""
            s = str(v)
            if sep in s or '"' in s or "\n" in s:
                s = '"' + s.replace('"', '""') + '"'
            return s
        if header:
            hdr = ([cell(self._index.name) if self._index.name is not None else ""] if index else [])
            out.append(sep.join(hdr + [cell(n) for n in self._names()]))
        for i in range(len(self._index)):
            row = ([cell(self._index._v[i])] if index else []) + [cell(c._v[i]) for c in self._cols]
            out.append(sep.join(row))
        text = "\n".join(out) + "\n"
        if path_or_buf is None:
            return text
        if isinstance(path_or_buf, str):
            with open(path_or_buf, "w") as f:
                f.write(text)
        else:
            path_or_buf.write(text)
        return None

    def to_json(self, path_or_buf=None, orient="columns"):
        def clean(v):
            return None if _isna(v) else v
        if orient == "records":
            obj = [{str(c.name): clean(c._v[i]) for c in self._cols} for i in range(len(self._index))]
        elif orient == "list" or orient == "values":
            obj = [[clean(c._v[i]) for c in self._cols] for i in range(len(self._index))]
        elif orient == "index":
            obj = {str(self._index._v[i]): {str(c.name): clean(c._v[i]) for c in self._cols}
                   for i in range(len(self._index))}
        else:
            obj = {str(c.name): {str(k): clean(v) for k, v in zip(self._index._v, c._v)}
                   for c in self._cols}
        text = _json.dumps(obj, separators=(",", ":"))
        if path_or_buf is None:
            return text
        if isinstance(path_or_buf, str):
            with open(path_or_buf, "w") as f:
                f.write(text)
        else:
            path_or_buf.write(text)
        return None

    # repr
    def __repr__(self):
        names = self._names()
        if not self._cols or not len(self._index):
            return "Empty DataFrame\nColumns: [" + ", ".join(_label(n) for n in names) + \
                "]\nIndex: [" + ", ".join(_label(v) for v in self._index._v) + "]"
        # pandas' DataFrameFormatter with the defaults of a run that is not
        # an interactive terminal: max_rows 60 (then min_rows 10), and
        # max_columns 0, which fits the columns to a width of 80
        nrows = len(self._index)
        ncols = len(self._cols)
        rows = list(range(nrows))
        vtrunc = nrows > _MAX_ROWS
        if vtrunc:
            half = _MIN_ROWS // 2
            rows = rows[:half] + rows[-half:]
        strcols = self._strcols(rows, None, vtrunc, -1)
        lines = _adjoin(strcols)
        htrunc = False
        if max(len(x) for x in lines) > _WIDTH:
            fitted = _fit_columns(strcols, max(len(x) for x in lines))
            if ncols > fitted:
                htrunc = True
                keep = fitted // 2
                names = self._names()
                strcols = self._strcols(rows, names[:keep] + names[ncols - keep:], vtrunc, keep)
                lines = _adjoin(strcols)
        if vtrunc or htrunc:
            lines.append("")
            lines.append("[%d rows x %d columns]" % (nrows, ncols))
        return "\n".join(lines)

    def _strcols(self, rows, names, vtrunc, dotcol):
        """The index and every column as lists of equal-width strings."""
        frame = self._take(rows, names)
        named = self._index.name is not None
        idx = [""] + ([str(self._index.name)] if named else []) + [_label(v) for v in frame._index._v]
        iw = max(len(x) for x in idx)
        strcols = [[x.ljust(iw) for x in idx]]
        for c in frame._cols:
            head = (" " if _numeric(c._dt) else "") + _label(c.name)
            cells = _fmt_values(c._v, c._dt)
            w = max([len(head)] + [len(x) for x in cells])
            strcols.append([head.rjust(w)] + ([" " * w] if named else []) + [x.rjust(w) for x in cells])
        if dotcol >= 0:
            strcols.insert(dotcol + 1, [" ..."] * len(idx))
        if vtrunc:
            at = _MIN_ROWS // 2 + 1 + (1 if named else 0)
            for ix, col in enumerate(strcols):
                width = len(col[at - 1])
                dot_col = ix == dotcol + 1 and dotcol >= 0
                dots = "..." if width > 3 or dot_col else ".."
                if ix == 0:
                    col.insert(at, dots.ljust(width))
                else:
                    col.insert(at, dots.rjust(4 if dot_col else width))
        return strcols

    __str__ = __repr__


class _DLoc:
    def __init__(self, df, positional):
        self._df = df
        self._pos = positional

    def _rows(self, key):
        df = self._df
        n = len(df._index)
        if isinstance(key, Series) and key._dt == "bool":
            return df._mask(key), False
        if isinstance(key, slice):
            if self._pos or (key.start is None and key.stop is None):
                return list(range(n)[key]), False
            a = 0 if key.start is None else df._index.get_loc(key.start)
            b = n if key.stop is None else df._index.get_loc(key.stop) + 1
            return list(range(a, b)), False
        if isinstance(key, list):
            if key and isinstance(key[0], bool):
                return df._mask(key), False
            return (list(key) if self._pos else [df._index.get_loc(k) for k in key]), False
        return [key if self._pos else df._index.get_loc(key)], True

    def _colnames(self, key):
        names = self._df._names()
        if isinstance(key, slice):
            if self._pos:
                return names[key], False
            a = 0 if key.start is None else names.index(key.start)
            b = len(names) if key.stop is None else names.index(key.stop) + 1
            return names[a:b], False
        if isinstance(key, list):
            return ([names[k] for k in key] if self._pos else list(key)), False
        return [names[key] if self._pos else key], True

    def __getitem__(self, key):
        df = self._df
        if isinstance(key, tuple):
            rk, ck = key
        else:
            rk, ck = key, slice(None)
        rows, one_row = self._rows(rk)
        cols, one_col = self._colnames(ck)
        if one_row and one_col:
            return df._col(cols[0])._v[rows[0]]
        if one_col:
            c = df._col(cols[0])
            return c._take(rows)
        sub = df._take(rows, cols)
        if one_row:
            return sub._row(0)
        return sub

    def __setitem__(self, key, value):
        df = self._df
        if isinstance(key, tuple):
            rk, ck = key
        else:
            rk, ck = key, slice(None)
        cols, _ = self._colnames(ck)
        if not self._pos and not isinstance(rk, (Series, slice, list)) and rk not in df._index:
            df._index = Index(df._index._v + [rk], df._index.name)
            for c in df._cols:
                c._v = c._v + [nan]
                c._index = df._index
        rows, _ = self._rows(rk)
        for name in cols:
            if name not in df._names():
                df[name] = nan
            c = df._col(name)
            vals = list(c._v)
            for r in rows:
                vals[r] = value
            dt, vals = _infer(vals)
            c._dt, c._v = dt, vals


# ---- groupby --------------------------------------------------------------

def _agg_one(series, how):
    if callable(how):
        return how(series)
    if how == "size":
        return len(series)
    if how == "first":
        return series._v[0]
    if how == "last":
        return series._v[-1]
    return getattr(series, how)()


class GroupBy:
    def __init__(self, df, by, cols=None):
        self._df = df
        self._by = by
        keys = df._col(by)._v
        groups = {}
        for i, k in enumerate(keys):
            if _isna(k):
                continue
            if k not in groups:
                groups[k] = []
            groups[k].append(i)
        self._keys = sorted(groups.keys())
        self._groups = groups
        self._cols = cols

    def _value_cols(self):
        if self._cols is not None:
            return self._cols
        return [n for n in self._df._names() if n != self._by]

    def _index(self):
        return Index(list(self._keys), self._by)

    def __getitem__(self, key):
        if isinstance(key, list):
            return GroupBy(self._df, self._by, key)
        return SeriesGroupBy(self, key)

    def __iter__(self):
        for k in self._keys:
            yield k, self._df._take(self._groups[k])

    @property
    def groups(self):
        return {k: self._df._index[0:0].__class__([self._df._index._v[i] for i in self._groups[k]])
                for k in self._keys}

    def get_group(self, key):
        return self._df._take(self._groups[key])

    def _agg(self, spec, numeric_only=False):
        ix = self._index()
        out = []
        for name in self._value_cols():
            how = spec.get(name) if isinstance(spec, dict) else spec
            if how is None:
                continue
            col = self._df._col(name)
            if numeric_only and not _numeric(col._dt):
                continue
            hows = how if isinstance(how, list) else [how]
            for h in hows:
                vals = [_agg_one(col._take(self._groups[k]), h) for k in self._keys]
                out.append(Series._make(vals, ix, name))
        return DataFrame._from_series(out, ix)

    def agg(self, spec):
        return self._agg(spec)

    aggregate = agg

    def sum(self): return self._agg("sum")
    def mean(self): return self._agg("mean", True)
    def median(self): return self._agg("median", True)
    def std(self): return self._agg("std", True)
    def min(self): return self._agg("min")
    def max(self): return self._agg("max")
    def first(self): return self._agg("first")
    def last(self): return self._agg("last")
    def nunique(self): return self._agg("nunique")

    def count(self):
        df = self._agg("count")
        for c in df._cols:
            c._dt = "int64"
        return df

    def size(self):
        return Series._make([len(self._groups[k]) for k in self._keys], self._index(), None, "int64")

    def apply(self, func):
        return Series._make([func(self._df._take(self._groups[k])) for k in self._keys], self._index())


class SeriesGroupBy:
    def __init__(self, gb, name):
        self._gb = gb
        self._name = name

    def agg(self, how):
        gb = self._gb
        col = gb._df._col(self._name)
        if isinstance(how, list):
            return DataFrame._from_series(
                [Series._make([_agg_one(col._take(gb._groups[k]), h) for k in gb._keys], gb._index(),
                              h if isinstance(h, str) else h.__name__) for h in how], gb._index())
        vals = [_agg_one(col._take(gb._groups[k]), how) for k in gb._keys]
        return Series._make(vals, gb._index(), self._name)

    aggregate = agg
    apply = agg

    def sum(self): return self.agg("sum")
    def mean(self): return self.agg("mean")
    def median(self): return self.agg("median")
    def std(self): return self.agg("std")
    def min(self): return self.agg("min")
    def max(self): return self.agg("max")
    def count(self): return self.agg("count")
    def nunique(self): return self.agg("nunique")
    def first(self): return self.agg("first")
    def last(self): return self.agg("last")

    def size(self):
        return self._gb.size()


# ---- combining ------------------------------------------------------------

def merge(left, right, on=None, how="inner", left_on=None, right_on=None, suffixes=("_x", "_y")):
    lk = left_on or on
    rk = right_on or on
    lkeys = left._col(lk)._v
    rkeys = right._col(rk)._v
    rpos = {}
    for j, k in enumerate(rkeys):
        rpos.setdefault(k, []).append(j)
    pairs = []
    for i, k in enumerate(lkeys):
        if k in rpos:
            for j in rpos[k]:
                pairs.append((i, j))
        elif how in ("left", "outer"):
            pairs.append((i, None))
    if how == "outer":
        matched = set(j for _, j in pairs if j is not None)
        for j in range(len(rkeys)):
            if j not in matched:
                pairs.append((None, j))
    lnames = left._names()
    rnames = [n for n in right._names() if not (n == rk and lk == rk)]
    data = {}
    order = []
    for n in lnames:
        name = n + suffixes[0] if (n in rnames and n != lk) else n
        col = left._col(n)._v
        if n == lk:
            data[name] = [col[i] if i is not None else rkeys[j] for i, j in pairs]
        else:
            data[name] = [col[i] if i is not None else None for i, j in pairs]
        order.append(name)
    for n in rnames:
        name = n + suffixes[1] if n in lnames else n
        col = right._col(n)._v
        data[name] = [col[j] if j is not None else None for i, j in pairs]
        order.append(name)
    return DataFrame({k: data[k] for k in order})


def concat(objs, axis=0, ignore_index=False):
    objs = list(objs)
    if objs and isinstance(objs[0], Series) and axis in (1, "columns"):
        return DataFrame({s.name: s for s in objs})
    if objs and isinstance(objs[0], Series):
        vals, labels = [], []
        for s in objs:
            vals.extend(s._v)
            labels.extend(s._index._v)
        ix = Index._rng(len(vals)) if ignore_index else Index(labels)
        return Series._make(vals, ix, objs[0].name)
    if axis in (1, "columns"):
        cols = []
        for d in objs:
            cols.extend(c.copy() for c in d._cols)
        return DataFrame._from_series(cols, objs[0]._index)
    names = []
    for d in objs:
        for n in d._names():
            if n not in names:
                names.append(n)
    labels = []
    for d in objs:
        labels.extend(d._index._v)
    ix = Index._rng(len(labels)) if ignore_index else Index(labels)
    cols = []
    for n in names:
        vals = []
        for d in objs:
            if n in d._names():
                vals.extend(d._col(n)._v)
            else:
                vals.extend([None] * len(d._index))
        cols.append(Series._make(vals, ix, n))
    return DataFrame._from_series(cols, ix)


# ---- I/O ------------------------------------------------------------------

_NA_STRINGS = ("", "NaN", "nan", "NA", "N/A", "null", "NULL", "None", "n/a", "<NA>")


def _parse_column(cells):
    vals = []
    kind = "int"
    for s in cells:
        if s in _NA_STRINGS:
            vals.append(None)
            continue
        vals.append(s)
    present = [v for v in vals if v is not None]
    if present and all(v in ("True", "False", "TRUE", "FALSE", "true", "false") for v in present) \
            and len(present) == len(vals):
        return [v.lower() == "true" for v in vals]
    for v in present:
        try:
            int(v)
        except ValueError:
            kind = "float"
            break
    if kind == "float":
        for v in present:
            try:
                float(v)
            except ValueError:
                kind = "str"
                break
    if kind == "int":
        return [int(v) if v is not None else None for v in vals]
    if kind == "float":
        return [float(v) if v is not None else None for v in vals]
    return vals


def read_csv(filepath_or_buffer, sep=",", header=0, names=None, index_col=None, usecols=None):
    if isinstance(filepath_or_buffer, str):
        with open(filepath_or_buffer) as f:
            text = f.read()
    else:
        text = filepath_or_buffer.read()
    rows = [r for r in _csv.reader(_io.StringIO(text), delimiter=sep) if r]
    if names is not None:
        cols = list(names)
        body = rows[1:] if header == 0 and header is not None and names is None else rows
        if header == 0 and names is not None and header is not None and header != "infer":
            body = rows
    elif header is None:
        cols = list(range(len(rows[0]))) if rows else []
        body = rows
    else:
        cols = rows[0] if rows else []
        body = rows[1:]
    data = {}
    for j, n in enumerate(cols):
        if usecols is not None and n not in usecols:
            continue
        data[n] = _parse_column([r[j] if j < len(r) else "" for r in body])
    df = DataFrame(data)
    if index_col is not None:
        key = cols[index_col] if isinstance(index_col, int) else index_col
        df = df.set_index(key)
    return df


def read_json(path_or_buf, orient=None):
    if isinstance(path_or_buf, str):
        if path_or_buf.lstrip()[:1] in ("[", "{"):
            text = path_or_buf
        else:
            with open(path_or_buf) as f:
                text = f.read()
    else:
        text = path_or_buf.read()
    obj = _json.loads(text)
    if isinstance(obj, list):
        if obj and isinstance(obj[0], list):
            return DataFrame(obj)
        return DataFrame(obj)
    if orient == "index":
        labels = list(obj.keys())
        return DataFrame([obj[k] for k in labels], index=labels)
    labels = []
    for col in obj.values():
        for k in col:
            if k not in labels:
                labels.append(k)
    data = {c: [obj[c].get(k) for k in labels] for c in obj}
    try:
        labels = [int(k) for k in labels]
    except ValueError:
        pass
    return DataFrame(data, index=labels)


def to_numeric(arg, errors="raise"):
    def conv(v):
        if _is_num(v) or _isna(v):
            return v
        try:
            return int(v)
        except (ValueError, TypeError):
            try:
                return float(v)
            except (ValueError, TypeError):
                if errors == "coerce":
                    return nan
                raise
    if isinstance(arg, Series):
        return Series._make([conv(v) for v in arg._v], arg._index, arg.name)
    if isinstance(arg, list):
        return _np.array([conv(v) for v in arg])
    return conv(arg)


def unique(values):
    return Series(values).unique()
