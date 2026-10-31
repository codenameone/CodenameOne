# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""Python 2 and 3 compatibility helpers, over the runtime's Python 3 stdlib."""

import sys as _sys
import functools as _functools
import itertools as _itertools
import operator as _operator
import io as _io

__version__ = "1.16.0"

PY2 = False
PY3 = True
PY34 = True

string_types = (str,)
integer_types = (int,)
class_types = (type,)
text_type = str
binary_type = bytes
MAXSIZE = _sys.maxsize

unichr = chr
long = int
xrange = range
zip = zip
map = map
filter = filter
range = range
reduce = _functools.reduce
zip_longest = _itertools.zip_longest
StringIO = _io.StringIO
BytesIO = _io.BytesIO
intern = _sys.intern
callable = callable
next = next
advance_iterator = next


def get_unbound_function(unbound):
    return unbound


def create_bound_method(func, obj):
    return func.__get__(obj)


def create_unbound_method(func, cls):
    return func


Iterator = object


def iterkeys(d, **kw):
    return iter(d.keys(**kw))


def itervalues(d, **kw):
    return iter(d.values(**kw))


def iteritems(d, **kw):
    return iter(d.items(**kw))


def iterlists(d, **kw):
    return iter(d.lists(**kw))


viewkeys = _operator.methodcaller("keys")
viewvalues = _operator.methodcaller("values")
viewitems = _operator.methodcaller("items")

get_method_function = _operator.attrgetter("__func__")
get_method_self = _operator.attrgetter("__self__")
get_function_closure = _operator.attrgetter("__closure__")
get_function_code = _operator.attrgetter("__code__")
get_function_defaults = _operator.attrgetter("__defaults__")
get_function_globals = _operator.attrgetter("__globals__")


def b(s):
    return s.encode("latin-1")


def u(s):
    return s


def int2byte(i):
    return bytes((i,))


def byte2int(bs):
    return bs[0]


def indexbytes(buf, i):
    return buf[i]


iterbytes = iter


def assertCountEqual(self, *args, **kwargs):
    return self.assertCountEqual(*args, **kwargs)


def assertRaisesRegex(self, *args, **kwargs):
    return self.assertRaisesRegex(*args, **kwargs)


def assertRegex(self, *args, **kwargs):
    return self.assertRegex(*args, **kwargs)


def assertNotRegex(self, *args, **kwargs):
    return self.assertNotRegex(*args, **kwargs)


def reraise(tp, value, tb=None):
    try:
        if value is None:
            value = tp()
        if value.__traceback__ is not tb:
            raise value.with_traceback(tb)
        raise value
    finally:
        value = None
        tb = None


def raise_from(value, from_value):
    try:
        raise value from from_value
    finally:
        value = None


def print_(*args, **kwargs):
    fp = kwargs.pop("file", _sys.stdout)
    if fp is None:
        return
    sep = kwargs.pop("sep", " ")
    end = kwargs.pop("end", "\n")
    flush = kwargs.pop("flush", False)
    fp.write(sep.join([str(a) for a in args]) + end)
    if flush:
        fp.flush()


def wraps(wrapped, assigned=_functools.WRAPPER_ASSIGNMENTS, updated=_functools.WRAPPER_UPDATES):
    def wrapper(f):
        f = _functools.wraps(wrapped, assigned, updated)(f)
        f.__wrapped__ = wrapped
        return f
    return wrapper


def with_metaclass(meta, *bases):
    """Create a base class with a metaclass."""

    class metaclass(type):

        def __new__(cls, name, this_bases, d):
            return meta(name, bases, d)

        @classmethod
        def __prepare__(cls, name, this_bases):
            return meta.__prepare__(name, bases)

    return type.__new__(metaclass, "temporary_class", (), {})


def add_metaclass(metaclass):
    """Class decorator for creating a class with a metaclass."""

    def wrapper(cls):
        orig_vars = cls.__dict__.copy()
        slots = orig_vars.get("__slots__")
        if slots is not None:
            if isinstance(slots, str):
                slots = [slots]
            for slots_var in slots:
                orig_vars.pop(slots_var)
        orig_vars.pop("__dict__", None)
        orig_vars.pop("__weakref__", None)
        if hasattr(cls, "__qualname__"):
            orig_vars["__qualname__"] = cls.__qualname__
        return metaclass(cls.__name__, cls.__bases__, orig_vars)

    return wrapper


def ensure_binary(s, encoding="utf-8", errors="strict"):
    if isinstance(s, bytes):
        return s
    if isinstance(s, str):
        return s.encode(encoding, errors)
    raise TypeError("not expecting type '" + str(type(s)) + "'")


def ensure_str(s, encoding="utf-8", errors="strict"):
    if not isinstance(s, (str, bytes)):
        raise TypeError("not expecting type '" + str(type(s)) + "'")
    if isinstance(s, bytes):
        return s.decode(encoding, errors)
    return s


def ensure_text(s, encoding="utf-8", errors="strict"):
    if isinstance(s, bytes):
        return s.decode(encoding, errors)
    if isinstance(s, str):
        return s
    raise TypeError("not expecting type '" + str(type(s)) + "'")


def python_2_unicode_compatible(klass):
    return klass


from . import moves
