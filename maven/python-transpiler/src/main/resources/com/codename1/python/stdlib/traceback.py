"""Formatting exceptions the way CPython's `traceback` module does.

The translated program has no frame objects, so the stack part of a
traceback is empty: `extract_tb` and `extract_stack` answer empty
`StackSummary` lists and a formatted traceback holds the header, the chained
exceptions and each exception's last line. Everything a program usually
reads -- `format_exc().splitlines()[-1]`, `format_exception_only` -- matches
CPython.
"""

import sys

__all__ = ["format_exc", "print_exc", "format_exception", "print_exception",
           "format_exception_only", "extract_tb", "extract_stack", "format_tb",
           "format_stack", "format_list", "print_tb", "print_stack",
           "StackSummary", "FrameSummary", "TracebackException"]

_CAUSE = "\nThe above exception was the direct cause of the following exception:\n\n"
_CONTEXT = "\nDuring handling of the above exception, another exception occurred:\n\n"


class FrameSummary:
    def __init__(self, filename, lineno, name, *, lookup_line=True, locals=None, line=None):
        self.filename = filename
        self.lineno = lineno
        self.name = name
        self.line = line
        self.locals = locals

    def __iter__(self):
        return iter([self.filename, self.lineno, self.name, self.line])

    def __repr__(self):
        return "<FrameSummary file %s, line %s in %s>" % (self.filename, self.lineno, self.name)


class StackSummary(list):
    @classmethod
    def from_list(cls, a_list):
        result = cls()
        for f in a_list:
            if isinstance(f, FrameSummary):
                result.append(f)
            else:
                filename, lineno, name, line = f
                result.append(FrameSummary(filename, lineno, name, line=line))
        return result

    def format(self):
        out = []
        for f in self:
            row = '  File "%s", line %s, in %s\n' % (f.filename, f.lineno, f.name)
            if f.line:
                row += "    %s\n" % f.line.strip()
            out.append(row)
        return out


def extract_tb(tb, limit=None):
    return StackSummary()


def extract_stack(f=None, limit=None):
    return StackSummary()


def format_list(extracted_list):
    return StackSummary.from_list(extracted_list).format()


def format_tb(tb, limit=None):
    return []


def format_stack(f=None, limit=None):
    return []


def print_tb(tb, limit=None, file=None):
    pass


def print_stack(f=None, limit=None, file=None):
    pass


def _type_name(etype):
    name = getattr(etype, "__qualname__", None) or etype.__name__
    mod = getattr(etype, "__module__", "builtins")
    if mod not in ("builtins", "__main__", None):
        name = mod + "." + name
    return name


def _only(exc):
    if exc is None:
        return ["NoneType: None\n"]
    name = _type_name(type(exc))
    try:
        msg = str(exc)
    except Exception:
        msg = "<exception str() failed>"
    line = name + (": " + msg if msg else "") + "\n"
    out = [line]
    for note in getattr(exc, "__notes__", None) or ():
        out.append(str(note) + "\n")
    return out


class TracebackException:
    def __init__(self, exc_type, exc_value, exc_traceback, *, limit=None,
                 lookup_lines=True, capture_locals=False, compact=False,
                 max_group_width=15, max_group_depth=10, _seen=None):
        if _seen is None:
            _seen = set()
        _seen.add(id(exc_value))
        self.exc_type = exc_type
        self._value = exc_value
        self._tb = exc_traceback
        self.stack = StackSummary()
        self.__cause__ = None
        self.__context__ = None
        self.__suppress_context__ = False
        if exc_value is not None:
            cause = getattr(exc_value, "__cause__", None)
            context = getattr(exc_value, "__context__", None)
            self.__suppress_context__ = bool(getattr(exc_value, "__suppress_context__", False))
            if cause is not None and id(cause) not in _seen:
                self.__cause__ = TracebackException(type(cause), cause, getattr(cause, "__traceback__", None), _seen=_seen)
            if context is not None and id(context) not in _seen:
                self.__context__ = TracebackException(type(context), context, getattr(context, "__traceback__", None), _seen=_seen)
        self.exc_type_str = _type_name(exc_type) if exc_type is not None else "NoneType"

    @classmethod
    def from_exception(cls, exc, *args, **kwargs):
        return cls(type(exc), exc, getattr(exc, "__traceback__", None), *args, **kwargs)

    def format_exception_only(self, *, show_group=False):
        return iter(_only(self._value))

    def format(self, *, chain=True):
        out = []
        if chain:
            if self.__cause__ is not None:
                out.extend(self.__cause__.format(chain=chain))
                out.append(_CAUSE)
            elif self.__context__ is not None and not self.__suppress_context__:
                out.extend(self.__context__.format(chain=chain))
                out.append(_CONTEXT)
        if self._tb is not None:
            out.append("Traceback (most recent call last):\n")
            out.extend(self.stack.format())
        out.extend(_only(self._value))
        return iter(out)

    def print(self, *, file=None, chain=True):
        if file is None:
            file = sys.stderr
        for line in self.format(chain=chain):
            print(line, file=file, end="")

    def __eq__(self, other):
        if isinstance(other, TracebackException):
            return self.exc_type is other.exc_type and self._value is other._value
        return NotImplemented

    def __str__(self):
        return str(self._value)


def _value_of(exc, value):
    # format_exception(exc) and format_exception(type, value, tb) both work.
    if value is _SENTINEL:
        return exc
    return value


_SENTINEL = object()


def format_exception(exc, value=_SENTINEL, tb=_SENTINEL, limit=None, chain=True):
    v = _value_of(exc, value)
    return list(TracebackException(type(v), v, getattr(v, "__traceback__", None)).format(chain=chain))


def print_exception(exc, value=_SENTINEL, tb=_SENTINEL, limit=None, file=None, chain=True):
    if file is None:
        file = sys.stderr
    for line in format_exception(exc, value, tb, limit, chain):
        print(line, file=file, end="")


def format_exception_only(exc, value=_SENTINEL, *, show_group=False):
    return _only(_value_of(exc, value))


def format_exc(limit=None, chain=True):
    e = sys.exception()
    if e is None:
        return "NoneType: None\n"
    return "".join(format_exception(e, chain=chain))


def print_exc(limit=None, file=None, chain=True):
    if file is None:
        file = sys.stderr
    print(format_exc(limit, chain), file=file, end="")


def print_last(limit=None, file=None, chain=True):
    exc = getattr(sys, "last_exc", None)
    if exc is not None:
        print_exception(exc, limit=limit, file=file, chain=chain)


def clear_frames(tb):
    pass


def walk_tb(tb):
    return iter(())


def walk_stack(f):
    return iter(())
