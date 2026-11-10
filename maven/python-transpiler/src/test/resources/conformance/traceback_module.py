import traceback
import sys

def fail():
    raise ValueError("bad value")

try:
    fail()
except ValueError:
    text = traceback.format_exc()
    lines = text.splitlines()
    print(lines[0])
    print(lines[-1])

print(traceback.format_exc(), end="")
print(traceback.format_exception_only(KeyError("k")))
print(traceback.format_exception_only(ValueError, ValueError("v")))
print(traceback.format_exception_only(RuntimeError()))

class MyErr(Exception):
    pass

e = MyErr("custom")
print(traceback.format_exception(e)[-1], end="")
print(traceback.format_exception(e)[0], end="")

try:
    try:
        1 / 0
    except ZeroDivisionError as z:
        raise KeyError("x") from z
except KeyError as k:
    out = traceback.format_exception(k)
    print([l for l in out if not l.startswith("  ")])

try:
    try:
        int("q")
    except ValueError:
        raise TypeError("t")
except TypeError as t:
    out = traceback.format_exception(t)
    print([l for l in out if not l.startswith("  ")])

te = traceback.TracebackException.from_exception(ValueError("z"))
print(str(te), list(te.format_exception_only()))
print(te.exc_type_str)
print(isinstance(traceback.extract_stack(), list))
traceback.print_exception(ValueError("to stdout"), file=sys.stdout)
