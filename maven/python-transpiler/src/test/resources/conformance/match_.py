
class Point:
    __match_args__ = ("x", "y")

    def __init__(self, x, y):
        self.x = x
        self.y = y


RED = "red"


class Colors:
    BLUE = "blue"


def describe(v):
    match v:
        case None:
            return "none"
        case True:
            return "true"
        case 0 | 1 as small:
            return "small " + str(small)
        case int(n) if n < 0:
            return "negative " + str(n)
        case int() | float():
            return "number " + str(v)
        case Colors.BLUE:
            return "the blue"
        case "go" | "run":
            return "verb"
        case str(s):
            return "str " + s
        case []:
            return "empty"
        case [x]:
            return "one " + repr(x)
        case [first, *rest] if len(rest) > 2:
            return "long " + repr(first) + " " + repr(rest)
        case [a, b, *_, c]:
            return "3+ " + repr((a, b, c))
        case (a, b):
            return "pair " + repr((a, b))
        case {"kind": "circle", "r": r}:
            return "circle " + str(r)
        case {"kind": kind, **others}:
            return "shape " + kind + " " + repr(others)
        case Point(0, 0):
            return "origin"
        case Point(x=0, y=yy):
            return "on y " + str(yy)
        case Point(xx, yy) if xx == yy:
            return "diag " + str(xx)
        case Point():
            return "point"
        case _:
            return "other " + type(v).__name__


for v in [None, True, 0, 1, -5, 7, 2.5, "blue", "go", "hi", [], [9], [1, 2, 3, 4], [1, 2, 3], (1, 2), {"kind": "circle", "r": 2},
          {"kind": "sq", "w": 1, "h": 2}, Point(0, 0), Point(0, 5), Point(3, 3), Point(1, 2), {1, 2}]:
    print(describe(v))


def gen(items):
    for it in items:
        match it:
            case ("add", n):
                yield n
            case ("skip",):
                continue
            case ("stop",):
                return
            case _:
                yield -1


print(list(gen([("add", 1), ("skip",), ("bad",), ("add", 2), ("stop",), ("add", 3)])))

cmd = "load file.txt"
match cmd.split():
    case ["load", fn]:
        print("loading", fn)
    case _:
        print("?")
x = 5
match x:
    case 1:
        print("one")
print("done", x)
