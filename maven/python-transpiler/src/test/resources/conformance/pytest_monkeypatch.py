import os.path
import pytest


class Box:
    size = 1


def main():
    mp = pytest.MonkeyPatch()
    mp.setattr(Box, "size", 2)
    print("set", Box.size)
    mp.setattr(Box, "color", "red", raising=False)
    print("new", Box.color)
    mp.setattr("os.path.sep", "!")
    print("dotted", os.path.sep)
    mp.delattr(Box, "size")
    print("deleted", hasattr(Box, "size"))
    try:
        mp.setattr(Box, "missing", 3)
    except AttributeError:
        print("raising", "AttributeError")
    mp.undo()
    print("undone", Box.size, hasattr(Box, "color"), os.path.sep)


main()
