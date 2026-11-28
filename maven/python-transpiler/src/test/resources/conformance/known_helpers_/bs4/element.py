from bs4._deprecation import (
    _deprecated,
    _deprecated_alias,
    _deprecated_function_alias,
)


class Node:
    def __init__(self, name, children=None):
        self.name = name
        self.next_sibling = None
        self.children = children or []

    nextSibling = _deprecated_alias("nextSibling", "next_sibling", "4.0.0")
    findAll = _deprecated_function_alias(
        "findAll", "find_all", "4.0.0"
    )

    def find_all(self, name, limit=None):
        found = [c for c in self.children if c.name == name]
        return found if limit is None else found[:limit]

    def find(self, name):
        r = self.find_all(name, 1)
        return r[0] if r else None

    @_deprecated("find_all", "4.0.0")
    def fetch(self, name):
        return self.find_all(name)
