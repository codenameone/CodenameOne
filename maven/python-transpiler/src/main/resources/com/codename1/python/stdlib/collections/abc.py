# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""Abstract base classes for containers."""

from abc import ABCMeta, abstractmethod


def _has(c, *methods):
    for m in methods:
        for k in c.__mro__:
            if m in k.__dict__:
                if k.__dict__[m] is None:
                    return NotImplemented
                break
        else:
            return NotImplemented
    return True


class Hashable(metaclass=ABCMeta):
    @abstractmethod
    def __hash__(self):
        return 0

    @classmethod
    def __subclasshook__(cls, c):
        return _has(c, "__hash__") if cls is Hashable else NotImplemented


class Awaitable(metaclass=ABCMeta):
    @abstractmethod
    def __await__(self):
        yield

    @classmethod
    def __subclasshook__(cls, c):
        return _has(c, "__await__") if cls is Awaitable else NotImplemented


class Coroutine(Awaitable):
    pass


class AsyncIterable(metaclass=ABCMeta):
    @abstractmethod
    def __aiter__(self):
        return None

    @classmethod
    def __subclasshook__(cls, c):
        return _has(c, "__aiter__") if cls is AsyncIterable else NotImplemented


class AsyncIterator(AsyncIterable):
    @abstractmethod
    async def __anext__(self):
        raise StopAsyncIteration

    def __aiter__(self):
        return self

    @classmethod
    def __subclasshook__(cls, c):
        return _has(c, "__anext__", "__aiter__") if cls is AsyncIterator else NotImplemented


class AsyncGenerator(AsyncIterator):
    pass


class Iterable(metaclass=ABCMeta):
    @abstractmethod
    def __iter__(self):
        while False:
            yield None

    @classmethod
    def __subclasshook__(cls, c):
        return _has(c, "__iter__") if cls is Iterable else NotImplemented


class Iterator(Iterable):
    @abstractmethod
    def __next__(self):
        raise StopIteration

    def __iter__(self):
        return self

    @classmethod
    def __subclasshook__(cls, c):
        return _has(c, "__iter__", "__next__") if cls is Iterator else NotImplemented


class Reversible(Iterable):
    @abstractmethod
    def __reversed__(self):
        while False:
            yield None

    @classmethod
    def __subclasshook__(cls, c):
        return _has(c, "__reversed__", "__iter__") if cls is Reversible else NotImplemented


class Generator(Iterator):
    @classmethod
    def __subclasshook__(cls, c):
        return _has(c, "__iter__", "__next__", "send", "throw", "close") if cls is Generator else NotImplemented


class Sized(metaclass=ABCMeta):
    @abstractmethod
    def __len__(self):
        return 0

    @classmethod
    def __subclasshook__(cls, c):
        return _has(c, "__len__") if cls is Sized else NotImplemented


class Container(metaclass=ABCMeta):
    @abstractmethod
    def __contains__(self, x):
        return False

    @classmethod
    def __subclasshook__(cls, c):
        return _has(c, "__contains__") if cls is Container else NotImplemented


class Collection(Sized, Iterable, Container):
    @classmethod
    def __subclasshook__(cls, c):
        return _has(c, "__len__", "__iter__", "__contains__") if cls is Collection else NotImplemented


class Callable(metaclass=ABCMeta):
    @abstractmethod
    def __call__(self, *args, **kwds):
        return False

    @classmethod
    def __subclasshook__(cls, c):
        return _has(c, "__call__") if cls is Callable else NotImplemented


class Set(Collection):
    def __le__(self, other):
        if len(self) > len(other):
            return False
        return all(e in other for e in self)

    def __lt__(self, other):
        return len(self) < len(other) and self.__le__(other)

    def __ge__(self, other):
        return len(self) >= len(other) and all(e in self for e in other)

    def __gt__(self, other):
        return len(self) > len(other) and self.__ge__(other)

    def __eq__(self, other):
        if not isinstance(other, Set):
            return NotImplemented
        return len(self) == len(other) and self.__le__(other)

    @classmethod
    def _from_iterable(cls, it):
        return cls(it)

    def __and__(self, other):
        return self._from_iterable(v for v in other if v in self)

    def __or__(self, other):
        return self._from_iterable(e for s in (self, other) for e in s)

    def __sub__(self, other):
        return self._from_iterable(v for v in self if v not in other)

    def __xor__(self, other):
        return (self - other) | (other - self)

    def isdisjoint(self, other):
        return all(v not in self for v in other)


class MutableSet(Set):
    @abstractmethod
    def add(self, value):
        raise NotImplementedError

    @abstractmethod
    def discard(self, value):
        raise NotImplementedError

    def remove(self, value):
        if value not in self:
            raise KeyError(value)
        self.discard(value)

    def pop(self):
        it = iter(self)
        try:
            value = next(it)
        except StopIteration:
            raise KeyError from None
        self.discard(value)
        return value

    def clear(self):
        try:
            while True:
                self.pop()
        except KeyError:
            pass

    def __ior__(self, it):
        for value in it:
            self.add(value)
        return self

    def __iand__(self, it):
        for value in self - it:
            self.discard(value)
        return self

    def __isub__(self, it):
        for value in it:
            self.discard(value)
        return self


class Mapping(Collection):
    @abstractmethod
    def __getitem__(self, key):
        raise KeyError

    def get(self, key, default=None):
        try:
            return self[key]
        except KeyError:
            return default

    def __contains__(self, key):
        try:
            self[key]
        except KeyError:
            return False
        return True

    def keys(self):
        return KeysView(self)

    def items(self):
        return ItemsView(self)

    def values(self):
        return ValuesView(self)

    def __eq__(self, other):
        if not isinstance(other, Mapping):
            return NotImplemented
        return dict(self.items()) == dict(other.items())

    __reversed__ = None


class MappingView(Sized):
    def __init__(self, mapping):
        self._mapping = mapping

    def __len__(self):
        return len(self._mapping)

    def __repr__(self):
        return "%s(%r)" % (type(self).__name__, self._mapping)


class KeysView(MappingView, Set):
    @classmethod
    def _from_iterable(cls, it):
        return set(it)

    def __contains__(self, key):
        return key in self._mapping

    def __iter__(self):
        yield from self._mapping


class ItemsView(MappingView, Set):
    @classmethod
    def _from_iterable(cls, it):
        return set(it)

    def __contains__(self, item):
        key, value = item
        try:
            v = self._mapping[key]
        except KeyError:
            return False
        return v is value or v == value

    def __iter__(self):
        for key in self._mapping:
            yield (key, self._mapping[key])


class ValuesView(MappingView, Collection):
    def __contains__(self, value):
        for key in self._mapping:
            v = self._mapping[key]
            if v is value or v == value:
                return True
        return False

    def __iter__(self):
        for key in self._mapping:
            yield self._mapping[key]


_MARKER = object()


class MutableMapping(Mapping):
    @abstractmethod
    def __setitem__(self, key, value):
        raise KeyError

    @abstractmethod
    def __delitem__(self, key):
        raise KeyError

    def pop(self, key, default=_MARKER):
        try:
            value = self[key]
        except KeyError:
            if default is _MARKER:
                raise
            return default
        del self[key]
        return value

    def popitem(self):
        try:
            key = next(iter(self))
        except StopIteration:
            raise KeyError from None
        value = self[key]
        del self[key]
        return key, value

    def clear(self):
        try:
            while True:
                self.popitem()
        except KeyError:
            pass

    def update(self, other=(), **kwds):
        if isinstance(other, Mapping):
            for key in other:
                self[key] = other[key]
        elif hasattr(other, "keys"):
            for key in other.keys():
                self[key] = other[key]
        else:
            for key, value in other:
                self[key] = value
        for key, value in kwds.items():
            self[key] = value

    def setdefault(self, key, default=None):
        try:
            return self[key]
        except KeyError:
            self[key] = default
        return default


class Sequence(Reversible, Collection):
    @abstractmethod
    def __getitem__(self, index):
        raise IndexError

    def __iter__(self):
        i = 0
        try:
            while True:
                yield self[i]
                i += 1
        except IndexError:
            return

    def __contains__(self, value):
        for v in self:
            if v is value or v == value:
                return True
        return False

    def __reversed__(self):
        for i in reversed(range(len(self))):
            yield self[i]

    def index(self, value, start=0, stop=None):
        i = start
        while stop is None or i < stop:
            try:
                v = self[i]
            except IndexError:
                break
            if v is value or v == value:
                return i
            i += 1
        raise ValueError

    def count(self, value):
        return sum(1 for v in self if v is value or v == value)


class MutableSequence(Sequence):
    @abstractmethod
    def __setitem__(self, index, value):
        raise IndexError

    @abstractmethod
    def __delitem__(self, index):
        raise IndexError

    @abstractmethod
    def insert(self, index, value):
        raise IndexError

    def append(self, value):
        self.insert(len(self), value)

    def clear(self):
        try:
            while True:
                self.pop()
        except IndexError:
            pass

    def reverse(self):
        n = len(self)
        for i in range(n // 2):
            self[i], self[n - i - 1] = self[n - i - 1], self[i]

    def extend(self, values):
        if values is self:
            values = list(values)
        for v in values:
            self.append(v)

    def pop(self, index=-1):
        v = self[index]
        del self[index]
        return v

    def remove(self, value):
        del self[self.index(value)]

    def __iadd__(self, values):
        self.extend(values)
        return self


class ByteString(Sequence):
    pass


Sequence.register(tuple)
Sequence.register(str)
Sequence.register(range)
MutableSequence.register(list)
Set.register(frozenset)
MutableSet.register(set)
Mapping.register(dict)
MutableMapping.register(dict)
