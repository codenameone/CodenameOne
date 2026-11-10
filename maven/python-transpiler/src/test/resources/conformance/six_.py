import six
from six import (PY2, PY3, string_types, integer_types, text_type, binary_type,
                 iteritems, itervalues, iterkeys, with_metaclass, add_metaclass,
                 ensure_str, ensure_binary, ensure_text, b, u, raise_from, reraise)
from six.moves import range, zip, map, filter, reduce, StringIO, zip_longest

print(PY2, PY3, string_types, integer_types, text_type, binary_type)
d = {"a": 1, "b": 2}
print(sorted(iteritems(d)), sorted(itervalues(d)), sorted(iterkeys(d)))
print(list(range(3)), list(zip([1, 2], "ab")), list(map(str, [1, 2])))
print(reduce(lambda a, c: a + c, [1, 2, 3]), list(zip_longest([1], [2, 3])))
print(isinstance("x", string_types), isinstance(3, integer_types), isinstance(b"x", binary_type))
print(b("abc"), u("abc"), type(b("a")).__name__, type(u("a")).__name__)
print(ensure_str(b"abc"), ensure_str("abc"), ensure_binary("abc"), ensure_binary(b"x"), ensure_text(b"q"))
for bad in (3, None):
    try:
        ensure_str(bad)
    except TypeError as e:
        print("TypeError", str(e))
s = StringIO()
s.write("hi")
print(s.getvalue())
print(six.int2byte(65), six.byte2int(b"A"), six.indexbytes(b"AB", 1), list(six.iterbytes(b"AB")))


class Meta(type):
    def __new__(mcs, name, bases, ns):
        ns["tag"] = "meta:" + name
        return super().__new__(mcs, name, bases, ns)


class Base(object):
    pass


class A(with_metaclass(Meta, Base)):
    pass


print(A.tag, type(A).__name__, A.__mro__[1].__name__)


@add_metaclass(Meta)
class B(Base):
    x = 1


print(B.tag, B.x, type(B).__name__)

try:
    raise_from(ValueError("outer"), KeyError("inner"))
except ValueError as e:
    print(type(e.__cause__).__name__, e.__cause__.args)

try:
    reraise(RuntimeError, RuntimeError("boom"))
except RuntimeError as e:
    print("reraised", e)
print(six.moves.range is range, six.callable(len), six.MAXSIZE > 0)
six.print_("a", "b", sep="-")
