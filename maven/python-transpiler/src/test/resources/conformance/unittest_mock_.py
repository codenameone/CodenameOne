import unittest
from unittest import mock
from unittest.mock import Mock, MagicMock, patch, call, ANY
import textwrap


def greet(name):
    return "hello " + name


class Service:
    def fetch(self, key):
        return "real " + key


def use_greet():
    return greet("x")


m = Mock()
print(m.called, m.call_count, m.call_args)
m(1, 2, a=3)
m("again")
print(m.called, m.call_count)
print(m.call_args)
print(m.call_args_list)
print(m.call_args == call("again"), m.call_args.args, m.call_args.kwargs)
args, kwargs = m.call_args_list[0]
print(args, kwargs)
m.assert_called()
m.assert_called_with("again")
m.assert_any_call(1, 2, a=3)
for check in [lambda: m.assert_called_once(), lambda: m.assert_called_with("nope"),
              lambda: m.assert_called_once_with("again"), lambda: m.assert_not_called(),
              lambda: Mock().assert_called(), lambda: Mock().assert_called_with(1),
              lambda: m.assert_any_call(9)]:
    try:
        check()
    except AssertionError as e:
        print("AssertionError:", str(e))

one = Mock()
one(5)
one.assert_called_once_with(5)
one.assert_called_once()
one.reset_mock()
print(one.called, one.call_count, one.call_args_list)

r = Mock(return_value=42)
print(r(), r(1), r.call_count)
r.return_value = 7
print(r())
print(Mock().return_value is not None)

e = Mock(side_effect=ValueError("bad"))
try:
    e()
except ValueError as ex:
    print("raised", ex)
e2 = Mock(side_effect=KeyError)
try:
    e2()
except KeyError:
    print("raised KeyError")
it = Mock(side_effect=[1, 2, 3])
print(it(), it(), it())
try:
    it()
except StopIteration:
    print("exhausted")
f = Mock(side_effect=lambda x: x * 2)
print(f(4))
mixed = Mock(side_effect=[1, ValueError("two")])
print(mixed())
try:
    mixed()
except ValueError as ex:
    print("second raised", ex)

parent = Mock()
parent.child.method(1)
parent.child.method(2)
print(parent.child.method.call_count, parent.child.method.call_args)
parent.other(3)
print(parent.method_calls)
print(parent.mock_calls)
parent.child.method.assert_called_with(2)
parent.assert_has_calls([call.child.method(1), call.child.method(2)])
parent.assert_has_calls([call.other(3), call.child.method(2)], any_order=True)
print(parent.child is parent.child)
parent.attr = 5
print(parent.attr)
parent.configure_mock(**{"x.return_value": 9})
print(parent.x())
cfg = Mock(a=1, b="two")
print(cfg.a, cfg.b)
print(call(1, k=2), call.foo(3), call.foo.bar())
print(call(1, 2) == call(1, 2), call(1) == call(2), m.call_args == ANY, ANY == 3)

mm = MagicMock()
mm.__len__ = Mock(return_value=3)
print(len(mm), bool(mm), list(MagicMock()), 1 in MagicMock())
getitem = Mock(return_value="item")
mm.__getitem__ = getitem
print(mm["k"], mm[0])
getitem.assert_called_with(0)
with MagicMock() as ctx:
    print(ctx is not None)
mm2 = MagicMock()
mm2.method.return_value = [1, 2]
print(mm2.method("q"), mm2.method.call_args)
print(MagicMock(return_value=5)())


# patch
with patch("__main__.greet") as g:
    g.return_value = "patched"
    print(use_greet(), g.call_args)
print(use_greet())

with patch("__main__.greet", lambda n: "lambda " + n):
    print(use_greet())
print(use_greet())

with patch.object(Service, "fetch", return_value="fake") as fetch:
    print(Service().fetch("k"), fetch.call_count)
print(Service().fetch("k"))

svc = Service()
with patch.object(svc, "fetch") as fm:
    fm.return_value = "inst"
    print(svc.fetch("z"))
    fm.assert_called_once_with("z")
print(svc.fetch("z"))

with patch.object(textwrap, "dedent", return_value="D") as d:
    print(textwrap.dedent("   x"))
print(textwrap.dedent("   x"))


@patch("__main__.greet")
def decorated(extra, g):
    g.return_value = "dec"
    return use_greet() + extra


print(decorated("!"), use_greet())


@patch.object(Service, "fetch", new=lambda self, k: "new " + k)
def decorated2():
    return Service().fetch("a")


print(decorated2(), Service().fetch("a"))

config = {"a": 1}
with patch.dict(config, {"b": 2}):
    print(sorted(config))
print(sorted(config))
with patch.dict(config, {"c": 3}, clear=True):
    print(sorted(config))
print(sorted(config))

p = patch("__main__.greet")
started = p.start()
print(greet("q") is started.return_value)
p.stop()
print(greet("q"))

try:
    with patch.object(Service, "missing"):
        pass
except AttributeError:
    print("missing attribute")
with patch.object(Service, "missing", create=True) as c:
    print(hasattr(Service, "missing"))
print(hasattr(Service, "missing"))


class T(unittest.TestCase):
    @patch("__main__.greet")
    def test_patched(self, g):
        g.return_value = "tc"
        self.assertEqual(use_greet(), "tc")
        g.assert_called_once_with("x")

    def test_mock(self):
        mk = Mock()
        mk.go(1)
        mk.go.assert_called_once_with(1)
        with self.assertRaises(AssertionError):
            mk.go.assert_called_once_with(2)


res = unittest.TestResult()
unittest.defaultTestLoader.loadTestsFromTestCase(T).run(res)
print(res.testsRun, res.wasSuccessful())
