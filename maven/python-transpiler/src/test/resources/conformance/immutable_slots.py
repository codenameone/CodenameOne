# Immutable slot classes in the soupsieve style: the compiler writes each
# constructor and slot reader (ImmutableSlots).
def _repr_value(value: object) -> object:
    return value if isinstance(value, Immutable) else repr(value)


class Immutable:
    __slots__: tuple[str, ...] = ('_hash',)

    _hash: int

    def __init__(self, **kwargs: object) -> None:
        temp = []
        for k, v in kwargs.items():
            temp.append(type(v))
            temp.append(v)
            super().__setattr__(k, v)
        super().__setattr__('_hash', hash(tuple(temp)))

    @classmethod
    def __base__(cls) -> type:
        return cls

    def __eq__(self, other: object) -> bool:
        stack: list[tuple[Immutable, object]] = [(self, other)]
        while stack:
            a, b = stack.pop()
            if a is b:
                continue
            if not isinstance(b, a.__base__()) or a._hash != b._hash:
                return False
            for key in a.__slots__[:-1]:
                x = getattr(a, key)
                y = getattr(b, key)
                if isinstance(x, Immutable):
                    stack.append((x, y))
                elif x.__class__ is tuple and y.__class__ is tuple:
                    if len(x) != len(y):
                        return False
                    for i, j in zip(x, y):
                        if isinstance(i, Immutable):
                            stack.append((i, j))
                        elif i != j:
                            return False
                elif x != y:
                    return False
        return True

    def __ne__(self, other: object) -> bool:
        return not self.__eq__(other)

    def __hash__(self) -> int:
        return self._hash

    def __setattr__(self, name: str, value: object) -> None:
        raise AttributeError(f"'{self.__class__.__name__}' is immutable")

    def __repr__(self) -> str:
        output: list[str] = []
        stack: list[object] = [self]
        while stack:
            item = stack.pop()
            if item.__class__ is str:
                output.append(item)
            elif isinstance(item, Immutable):
                stack.append(')')
                keys = item.__slots__[:-1]
                for index in range(len(keys) - 1, -1, -1):
                    stack.append(_repr_value(getattr(item, keys[index])))
                    stack.append(f'{keys[index]}=' if index == 0 else f', {keys[index]}=')
                stack.append(f'{item.__class__.__name__}(')
            else:
                stack.append(',)' if len(item) == 1 else ')')
                for index in range(len(item) - 1, -1, -1):
                    stack.append(_repr_value(item[index]))
                    if index:
                        stack.append(', ')
                stack.append('(')
        return ''.join(output)

    __str__ = __repr__


class Tag(Immutable):
    __slots__ = ("name", "prefix", "_hash")

    name: str
    prefix: str | None

    def __init__(self, name: str, prefix: str | None) -> None:
        super().__init__(prefix=prefix, name=name)


class Group(Immutable):
    __slots__ = ("tags", "inverse", "_hash")

    tags: tuple[Tag, ...]
    inverse: bool

    def __init__(self, tags: list[Tag], inverse: bool = False) -> None:
        super().__init__(tags=tuple(tags), inverse=inverse)

    def count(self) -> int:
        return len(self.tags)


def values(obj: Immutable) -> list[object]:
    vals = [getattr(obj, s) for s in obj.__slots__[:-1]]
    return vals


a = Tag("div", None)
b = Tag("div", None)
c = Tag("div", "svg")
print(a == b, a != b, a == c, a != c, hash(a) == hash(b))
print(a)
g1 = Group([a, c], True)
g2 = Group([b, c], True)
g3 = Group([b, a])
print(g1 == g2, g1 == g3, g1.count(), g3.inverse)
print(repr(g1))
print(str(Group([a])))
print(values(c), values(g3)[1])
print(len({a, b, c}))
print(a == "div", Tag("p", None) == Group([]))
