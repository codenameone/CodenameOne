from ._deprecation import (
    _deprecated,
)
from .element import Node


class Soup(Node):
    @_deprecated("find", "4.0.0")
    def findChild(self, name):
        return self.find(name)
