"""Code that walks another module's instances, in the shape of bs4.filter:
annotated parameters of a class `elements` compiles, fields read off
them, a method called through a value as well as directly."""
from elements import Tag, Bag


class Strainer:
    def __init__(self, name):
        self.name = name

    def matches(self, tag: Tag) -> bool:
        if not tag.prefix and tag.name != self.name:
            return False
        return tag.prefix != "skip"


def names(tags):
    out = []
    for t in tags:
        out.append(t.name if t.prefix is None else t.prefix + ":" + t.name)
    return out


def fill(tag: Tag, n):
    for i in range(n):
        tag.append(Tag("c" + str(i)))
    return tag.size


def count(bag: Bag):
    return "full" if bag else "empty"


def check(fn, x):
    return fn(x)
