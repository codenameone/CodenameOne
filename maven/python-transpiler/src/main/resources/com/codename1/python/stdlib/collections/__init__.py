# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""Container datatypes."""

from operator import itemgetter as _itemgetter
from itertools import chain as _chain, repeat as _repeat, starmap as _starmap
import heapq as _heapq

__all__ = ["ChainMap", "Counter", "OrderedDict", "UserDict", "UserList", "UserString", "defaultdict", "deque",
           "namedtuple"]


class deque:
    """A double-ended queue over a circular buffer."""

    def __init__(self, iterable=(), maxlen=None):
        if maxlen is not None and maxlen < 0:
            raise ValueError("maxlen must be non-negative")
        self._buf = [None] * 8
        self._head = 0
        self._size = 0
        self._maxlen = maxlen
        for x in iterable:
            self.append(x)

    @property
    def maxlen(self):
        return self._maxlen

    def _grow(self):
        old = self._buf
        n = len(old)
        self._buf = [old[(self._head + i) % n] for i in range(self._size)] + [None] * n
        self._head = 0

    def _index(self, i):
        n = self._size
        if i < 0:
            i += n
        if i < 0 or i >= n:
            raise IndexError("deque index out of range")
        return (self._head + i) % len(self._buf)

    def append(self, x):
        if self._maxlen == 0:
            return
        if self._size == self._maxlen:
            self.popleft()
        if self._size == len(self._buf):
            self._grow()
        self._buf[(self._head + self._size) % len(self._buf)] = x
        self._size += 1

    def appendleft(self, x):
        if self._maxlen == 0:
            return
        if self._size == self._maxlen:
            self.pop()
        if self._size == len(self._buf):
            self._grow()
        self._head = (self._head - 1) % len(self._buf)
        self._buf[self._head] = x
        self._size += 1

    def pop(self):
        if not self._size:
            raise IndexError("pop from an empty deque")
        self._size -= 1
        i = (self._head + self._size) % len(self._buf)
        x = self._buf[i]
        self._buf[i] = None
        return x

    def popleft(self):
        if not self._size:
            raise IndexError("pop from an empty deque")
        x = self._buf[self._head]
        self._buf[self._head] = None
        self._head = (self._head + 1) % len(self._buf)
        self._size -= 1
        return x

    def extend(self, iterable):
        if iterable is self:
            iterable = list(iterable)
        for x in iterable:
            self.append(x)

    def extendleft(self, iterable):
        if iterable is self:
            iterable = list(iterable)
        for x in iterable:
            self.appendleft(x)

    def clear(self):
        self._buf = [None] * 8
        self._head = 0
        self._size = 0

    def copy(self):
        return deque(self, self._maxlen)

    __copy__ = copy

    def count(self, x):
        return sum(1 for v in self if v is x or v == x)

    def index(self, x, start=0, stop=None):
        n = self._size
        stop = n if stop is None else stop
        for i in range(start, min(stop, n)):
            v = self[i]
            if v is x or v == x:
                return i
        raise ValueError(repr(x) + " is not in deque")

    def insert(self, i, x):
        if self._maxlen is not None and self._size == self._maxlen:
            raise IndexError("deque already at its maximum size")
        items = list(self)
        items.insert(i, x)
        self.clear()
        self.extend(items)

    def remove(self, value):
        for i in range(self._size):
            v = self[i]
            if v is value or v == value:
                del self[i]
                return
        raise ValueError(repr(value) + " is not in deque")

    def reverse(self):
        items = list(self)
        items.reverse()
        self.clear()
        self.extend(items)

    def rotate(self, n=1):
        size = self._size
        if not size:
            return
        n %= size
        for _ in range(n):
            self.appendleft(self.pop())

    def __len__(self):
        return self._size

    def __bool__(self):
        return self._size > 0

    def __iter__(self):
        buf = self._buf
        head = self._head
        for i in range(self._size):
            yield buf[(head + i) % len(buf)]

    def __reversed__(self):
        for i in range(self._size - 1, -1, -1):
            yield self[i]

    def __getitem__(self, i):
        return self._buf[self._index(i)]

    def __setitem__(self, i, x):
        self._buf[self._index(i)] = x

    def __delitem__(self, i):
        items = list(self)
        del items[i]
        self.clear()
        self.extend(items)

    def __contains__(self, x):
        for v in self:
            if v is x or v == x:
                return True
        return False

    def __eq__(self, other):
        if not isinstance(other, deque):
            return NotImplemented
        return list(self) == list(other)

    def __lt__(self, other):
        return list(self) < list(other)

    def __le__(self, other):
        return list(self) <= list(other)

    def __gt__(self, other):
        return list(self) > list(other)

    def __ge__(self, other):
        return list(self) >= list(other)

    __hash__ = None

    def __add__(self, other):
        r = deque(self, self._maxlen)
        r.extend(other)
        return r

    def __iadd__(self, other):
        self.extend(other)
        return self

    def __mul__(self, n):
        r = deque((), self._maxlen)
        for _ in range(n):
            r.extend(list(self))
        return r

    def __repr__(self):
        body = "[" + ", ".join(repr(x) for x in self) + "]"
        if self._maxlen is None:
            return "deque(" + body + ")"
        return "deque(" + body + ", maxlen=" + str(self._maxlen) + ")"


class defaultdict(dict):
    def __init__(self, default_factory=None, *args, **kwargs):
        if default_factory is not None and not callable(default_factory):
            raise TypeError("first argument must be callable or None")
        super().__init__(*args, **kwargs)
        self.default_factory = default_factory

    def __missing__(self, key):
        if self.default_factory is None:
            raise KeyError(key)
        value = self.default_factory()
        self[key] = value
        return value

    def copy(self):
        return defaultdict(self.default_factory, self)

    __copy__ = copy

    def __repr__(self):
        f = self.default_factory
        name = "None" if f is None else (getattr(f, "__qualname__", None) or repr(f))
        if isinstance(f, type):
            name = repr(f)
        return "defaultdict(" + name + ", " + dict.__repr__(self) + ")"

    def __or__(self, other):
        if not isinstance(other, dict):
            return NotImplemented
        r = self.copy()
        r.update(other)
        return r


class OrderedDict(dict):
    def move_to_end(self, key, last=True):
        value = self.pop(key)
        if last:
            self[key] = value
        else:
            rest = list(self.items())
            self.clear()
            self[key] = value
            for k, v in rest:
                self[k] = v

    def popitem(self, last=True):
        if not self:
            raise KeyError("dictionary is empty")
        key = next(reversed(self)) if last else next(iter(self))
        return key, self.pop(key)

    def copy(self):
        return OrderedDict(self)

    __copy__ = copy

    def __eq__(self, other):
        if isinstance(other, OrderedDict):
            return list(self.items()) == list(other.items())
        return dict.__eq__(self, other)

    def __ne__(self, other):
        r = self.__eq__(other)
        return r if r is NotImplemented else not r

    __hash__ = None

    def __repr__(self):
        if not self:
            return "OrderedDict()"
        return "OrderedDict(" + dict.__repr__(self) + ")"

    @classmethod
    def fromkeys(cls, iterable, value=None):
        r = cls()
        for k in iterable:
            r[k] = value
        return r


class Counter(dict):
    def __init__(self, iterable=None, /, **kwds):
        super().__init__()
        self.update(iterable, **kwds)

    def __missing__(self, key):
        return 0

    def total(self):
        return sum(self.values())

    def most_common(self, n=None):
        if n is None:
            return sorted(self.items(), key=_itemgetter(1), reverse=True)
        return _heapq.nlargest(n, self.items(), key=_itemgetter(1))

    def elements(self):
        return _chain.from_iterable(_starmap(_repeat, self.items()))

    @classmethod
    def fromkeys(cls, iterable, v=None):
        raise NotImplementedError("Counter.fromkeys() is undefined.  Use Counter(iterable) instead.")

    def update(self, iterable=None, /, **kwds):
        if iterable is not None:
            if isinstance(iterable, dict):
                for elem, count in iterable.items():
                    self[elem] = self[elem] + count
            else:
                for elem in iterable:
                    self[elem] = self[elem] + 1
        if kwds:
            self.update(kwds)

    def subtract(self, iterable=None, /, **kwds):
        if iterable is not None:
            if isinstance(iterable, dict):
                for elem, count in iterable.items():
                    self[elem] = self[elem] - count
            else:
                for elem in iterable:
                    self[elem] = self[elem] - 1
        if kwds:
            self.subtract(kwds)

    def copy(self):
        return Counter(self)

    __copy__ = copy

    def __delitem__(self, elem):
        if elem in self:
            dict.__delitem__(self, elem)

    def __repr__(self):
        if not self:
            return type(self).__name__ + "()"
        items = ", ".join(repr(k) + ": " + repr(v) for k, v in self.most_common())
        return type(self).__name__ + "({" + items + "})"

    def __eq__(self, other):
        if not isinstance(other, Counter):
            return NotImplemented
        return all(self[e] == other[e] for c in (self, other) for e in c)

    def __ne__(self, other):
        r = self.__eq__(other)
        return r if r is NotImplemented else not r

    __hash__ = None

    def __add__(self, other):
        if not isinstance(other, Counter):
            return NotImplemented
        result = Counter()
        for elem, count in self.items():
            newcount = count + other[elem]
            if newcount > 0:
                result[elem] = newcount
        for elem, count in other.items():
            if elem not in self and count > 0:
                result[elem] = count
        return result

    def __sub__(self, other):
        if not isinstance(other, Counter):
            return NotImplemented
        result = Counter()
        for elem, count in self.items():
            newcount = count - other[elem]
            if newcount > 0:
                result[elem] = newcount
        for elem, count in other.items():
            if elem not in self and count < 0:
                result[elem] = 0 - count
        return result

    def __or__(self, other):
        if not isinstance(other, Counter):
            return NotImplemented
        result = Counter()
        for elem, count in self.items():
            other_count = other[elem]
            newcount = other_count if count < other_count else count
            if newcount > 0:
                result[elem] = newcount
        for elem, count in other.items():
            if elem not in self and count > 0:
                result[elem] = count
        return result

    def __and__(self, other):
        if not isinstance(other, Counter):
            return NotImplemented
        result = Counter()
        for elem, count in self.items():
            other_count = other[elem]
            newcount = count if count < other_count else other_count
            if newcount > 0:
                result[elem] = newcount
        return result

    def __pos__(self):
        result = Counter()
        for elem, count in self.items():
            if count > 0:
                result[elem] = count
        return result

    def __neg__(self):
        result = Counter()
        for elem, count in self.items():
            if count < 0:
                result[elem] = 0 - count
        return result


def namedtuple(typename, field_names, *, rename=False, defaults=None, module=None):
    if isinstance(field_names, str):
        field_names = field_names.replace(",", " ").split()
    field_names = tuple(str(f) for f in field_names)
    if rename:
        seen = set()
        names = []
        for i, name in enumerate(field_names):
            if not name.isidentifier() or name.startswith("_") or name in seen:
                name = "_" + str(i)
            seen.add(name)
            names.append(name)
        field_names = tuple(names)
    for name in (typename,) + field_names:
        if not name.isidentifier():
            raise ValueError("Type names and field names must be valid identifiers: " + repr(name))
    seen = set()
    for name in field_names:
        if name.startswith("_") and not rename:
            raise ValueError("Field names cannot start with an underscore: " + repr(name))
        if name in seen:
            raise ValueError("Encountered duplicate field name: " + repr(name))
        seen.add(name)
    field_defaults = {}
    if defaults is not None:
        defaults = tuple(defaults)
        if len(defaults) > len(field_names):
            raise TypeError("Got more default values than field names")
        field_defaults = dict(zip(field_names[len(field_names) - len(defaults):], defaults))
    n = len(field_names)

    def __new__(cls, *args, **kwargs):
        if len(args) > n:
            raise TypeError("%s.__new__() takes %d positional arguments but %d were given" % (typename, n + 1, len(args) + 1))
        values = list(args)
        for name in field_names[len(args):]:
            if name in kwargs:
                values.append(kwargs.pop(name))
            elif name in field_defaults:
                values.append(field_defaults[name])
            else:
                raise TypeError("%s.__new__() missing 1 required positional argument: %r" % (typename, name))
        if kwargs:
            name = next(iter(kwargs))
            if name in field_names:
                raise TypeError("%s.__new__() got multiple values for argument %r" % (typename, name))
            raise TypeError("%s.__new__() got an unexpected keyword argument %r" % (typename, name))
        return tuple.__new__(cls, values)

    def _make(cls, iterable):
        result = tuple.__new__(cls, iterable)
        if len(result) != n:
            raise TypeError("Expected %d arguments, got %d" % (n, len(result)))
        return result

    def _replace(self, **kwds):
        result = self._make([kwds.pop(name) if name in kwds else v for name, v in zip(field_names, self)])
        if kwds:
            raise ValueError("Got unexpected field names: %r" % list(kwds))
        return result

    def __repr__(self):
        return typename + "(" + ", ".join(name + "=" + repr(v) for name, v in zip(field_names, self)) + ")"

    def _asdict(self):
        return dict(zip(field_names, self))

    def __getnewargs__(self):
        return tuple(self)

    namespace = {
        "__doc__": typename + "(" + ", ".join(field_names) + ")",
        "__slots__": (),
        "_fields": field_names,
        "_field_defaults": field_defaults,
        "__new__": __new__,
        "_make": classmethod(_make),
        "_replace": _replace,
        "__repr__": __repr__,
        "_asdict": _asdict,
        "__getnewargs__": __getnewargs__,
        "__match_args__": field_names,
    }
    for index, name in enumerate(field_names):
        namespace[name] = property(_itemgetter(index), doc="Alias for field number " + str(index))
    result = type(typename, (tuple,), namespace)
    # CPython reads the caller's module from its frame; there are no frames
    # here, so a caller outside __main__ passes module= (the stdlib does).
    result.__module__ = "__main__" if module is None else module
    return result


class ChainMap(dict):
    """A view of several mappings searched in order; writes go to the first."""

    def __init__(self, *maps):
        self.maps = list(maps) or [{}]

    def __missing__(self, key):
        raise KeyError(key)

    def __getitem__(self, key):
        for mapping in self.maps:
            if key in mapping:
                return mapping[key]
        return self.__missing__(key)

    def get(self, key, default=None):
        return self[key] if key in self else default

    def __len__(self):
        return len(set().union(*self.maps))

    def __iter__(self):
        d = {}
        for mapping in reversed(self.maps):
            for k in mapping:
                d[k] = None
        return iter(d)

    def __contains__(self, key):
        return any(key in m for m in self.maps)

    def __bool__(self):
        return any(self.maps)

    def keys(self):
        return list(iter(self))

    def items(self):
        return [(k, self[k]) for k in self]

    def values(self):
        return [self[k] for k in self]

    def __repr__(self):
        return "ChainMap(" + ", ".join(repr(m) for m in self.maps) + ")"

    def new_child(self, m=None, **kwargs):
        if m is None:
            m = kwargs
        elif kwargs:
            m.update(kwargs)
        return ChainMap(m, *self.maps)

    @property
    def parents(self):
        return ChainMap(*self.maps[1:])

    def __setitem__(self, key, value):
        self.maps[0][key] = value

    def __delitem__(self, key):
        try:
            del self.maps[0][key]
        except KeyError:
            raise KeyError("Key not found in the first mapping: " + repr(key)) from None

    def pop(self, key, *args):
        try:
            return self.maps[0].pop(key, *args)
        except KeyError:
            raise KeyError("Key not found in the first mapping: " + repr(key)) from None

    def clear(self):
        self.maps[0].clear()

    def copy(self):
        return ChainMap(self.maps[0].copy(), *self.maps[1:])

    def __eq__(self, other):
        return dict(self.items()) == dict(other.items())


class UserDict(dict):
    def __init__(self, dict=None, /, **kwargs):
        super().__init__()
        self.data = self
        if dict is not None:
            self.update(dict)
        if kwargs:
            self.update(kwargs)


class UserList(list):
    def __init__(self, initlist=None):
        super().__init__()
        self.data = self
        if initlist is not None:
            self.extend(initlist)


class UserString(str):
    pass
