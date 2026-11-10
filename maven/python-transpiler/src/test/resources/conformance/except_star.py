def show(e, depth=0):
    pad = "  " * depth
    if isinstance(e, BaseExceptionGroup):
        print(pad + type(e).__name__ + ": " + str(e))
        for x in e.exceptions:
            show(x, depth + 1)
    else:
        print(pad + repr(e))


eg = ExceptionGroup("many", [ValueError(1), TypeError(2), ExceptionGroup("inner", [ValueError(3), KeyError(4)])])
print(repr(eg))
show(eg)
m, r = eg.split(ValueError)
show(m)
show(r)
show(eg.subgroup(lambda e: isinstance(e, KeyError)))
print(eg.subgroup(OSError))
print(type(BaseExceptionGroup("x", [ValueError()])).__name__, type(BaseExceptionGroup("x", [KeyboardInterrupt()])).__name__)

try:
    raise eg
except* ValueError as e:
    print("values:")
    show(e)
except* TypeError as e:
    print("types:")
    show(e)
except* KeyError:
    print("keys")

try:
    try:
        raise eg
    except* ValueError:
        print("only values")
except ExceptionGroup as e:
    print("left:")
    show(e)

try:
    raise OSError("naked")
except* OSError as e:
    print("naked caught:")
    show(e)

try:
    try:
        raise OSError("plain")
    except* ValueError:
        print("no")
except OSError as e:
    print("unchanged", repr(e))

try:
    try:
        raise ExceptionGroup("g", [ValueError(1)])
    except* ValueError:
        raise RuntimeError("from handler")
except Exception as e:
    print("raised in handler:")
    show(e)

try:
    try:
        raise ExceptionGroup("g", [ValueError(1), TypeError(2)])
    except* ValueError:
        raise RuntimeError("from handler 2")
except Exception as e:
    print("raised with rest:")
    show(e)

try:
    try:
        raise ExceptionGroup("g", [ValueError(1), TypeError(2)])
    except* ValueError:
        raise
except ExceptionGroup as e:
    print("bare raise:")
    show(e)

try:
    try:
        raise ValueError(5)
    except* ExceptionGroup:
        pass
except TypeError as e:
    print("TypeError", e)


def f():
    try:
        raise ExceptionGroup("g", [ValueError(1)])
    except* ValueError:
        print("in f")
    else:
        print("no else")
    finally:
        print("finally")
    return "ok"


print(f())
