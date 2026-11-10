# A try whose body raises only by its own raise statements.
def classify(n):
    out = 0
    for i in range(n):
        try:
            if i % 3 == 0:
                raise ValueError(i)
            elif i % 5 == 0:
                raise KeyError(i * 2)
            elif i > 1000 and i < 0:
                raise IndexError()
        except ValueError as e:
            out += e.args[0]
        except KeyError as e:
            out -= e.args[0]
        finally:
            out += 1
    return out


def escapes(n):
    try:
        try:
            if n > 2:
                raise TypeError(n)
        except ValueError:
            return "wrong handler"
    except TypeError as e:
        return "outer " + str(e.args[0])
    return "none"


def chained(flag):
    try:
        if flag:
            raise RuntimeError(7)
    except RuntimeError as e:
        try:
            raise ValueError(8)
        except ValueError as f:
            return (e.args[0], f.args[0], type(f.__context__).__name__)
    return None


print(classify(40))
print(escapes(1), escapes(5))
print(chained(True), chained(False))

