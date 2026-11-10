# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""Mock objects and patching.

Supported: Mock and MagicMock (child mocks by attribute, return_value,
side_effect as exception, callable or iterable, call recording, call_count,
call_args, call_args_list, mock_calls, method_calls, the assert_called*
family, assert_has_calls, assert_any_call, reset_mock, configure_mock,
common magic methods on MagicMock), call, ANY, DEFAULT, sentinel, and patch
as decorator and context manager: patch.object, patch.dict, and patch with a
dotted name that names a module the program has already imported.

Not supported: mutating a magic method in place (m.__len__.return_value = 3;
assign m.__len__ = Mock(return_value=3) instead), spec and autospec (accepted and ignored), create_autospec,
PropertyMock, AsyncMock, mock_open, and patch of a module that was not
imported, because that would import by a computed name.
"""

import sys

__all__ = ["Mock", "MagicMock", "NonCallableMock", "call", "ANY", "DEFAULT",
           "sentinel", "patch", "PropertyMock"]


class _Sentinel:
    def __init__(self, name):
        self.name = name

    def __repr__(self):
        return "sentinel." + self.name


class _SentinelFactory:
    def __init__(self):
        self._items = {}

    def __getattr__(self, name):
        if name.startswith("__"):
            raise AttributeError(name)
        item = self._items.get(name)
        if item is None:
            item = _Sentinel(name)
            self._items[name] = item
        return item


sentinel = _SentinelFactory()
DEFAULT = sentinel.DEFAULT


class _ANY:
    def __eq__(self, other):
        return True

    def __ne__(self, other):
        return False

    def __repr__(self):
        return "<ANY>"


ANY = _ANY()


def _format_call(name, args, kwargs):
    parts = [repr(a) for a in args]
    for k in kwargs:
        parts.append("%s=%r" % (k, kwargs[k]))
    return "%s(%s)" % (name, ", ".join(parts))


class _Call:
    """A recorded call: (name, args, kwargs). `call(...)` builds one to
    compare with."""

    def __init__(self, args=(), kwargs=None, name="", parent=None):
        self._args = tuple(args)
        self._kwargs = dict(kwargs) if kwargs else {}
        self._name = name
        self._parent = parent

    @property
    def args(self):
        return self._args

    @property
    def kwargs(self):
        return self._kwargs

    def __call__(self, *args, **kwargs):
        return _Call(args, kwargs, self._name, self._parent)

    def __getattr__(self, attr):
        if attr.startswith("__"):
            raise AttributeError(attr)
        name = attr if not self._name else self._name + "." + attr
        return _Call((), {}, name, self)

    def __eq__(self, other):
        if not isinstance(other, _Call):
            if isinstance(other, tuple):
                if len(other) == 2:
                    return self._args == other[0] and self._kwargs == other[1]
                if len(other) == 3:
                    return self._name == other[0] and self._args == other[1] and self._kwargs == other[2]
                return False
            return NotImplemented
        return self._args == other._args and self._kwargs == other._kwargs and \
            (not self._name or not other._name or self._name == other._name)

    def __ne__(self, other):
        r = self.__eq__(other)
        return r if r is NotImplemented else not r

    def __hash__(self):
        return id(self)

    def __iter__(self):
        return iter((self._args, self._kwargs))

    def __len__(self):
        return 2

    def __getitem__(self, i):
        return (self._args, self._kwargs)[i]

    def __repr__(self):
        if not self._name:
            return _format_call("call", self._args, self._kwargs)
        n = "call" + self._name if self._name.startswith("(") else "call." + self._name
        return _format_call(n, self._args, self._kwargs)


call = _Call(name="")
call._name = ""


class NonCallableMock:
    def __init__(self, spec=None, side_effect=None, return_value=DEFAULT, wraps=None,
                 name=None, spec_set=None, parent=None, _new_name="", **kwargs):
        d = self.__dict__
        d["_children"] = {}
        d["_mock_name"] = name
        d["_mock_new_name"] = _new_name
        d["_mock_parent"] = parent
        d["_mock_return_value"] = return_value
        d["side_effect"] = side_effect
        d["_mock_wraps"] = wraps
        d["_mock_spec"] = spec
        d["called"] = False
        d["call_count"] = 0
        d["call_args"] = None
        d["call_args_list"] = []
        d["mock_calls"] = []
        d["method_calls"] = []
        for k in kwargs:
            setattr(self, k, kwargs[k])

    # -- naming -------------------------------------------------------------
    def __repr__(self):
        kind = type(self).__name__
        if self._mock_parent is not None and self._mock_new_name:
            return "<%s name=%r id='%d'>" % (kind, self._full_name(), id(self))
        if self._mock_name is None:
            return "<%s id='%d'>" % (kind, id(self))
        return "<%s name=%r id='%d'>" % (kind, self._mock_name, id(self))

    def _full_name(self):
        parts = []
        node = self
        while node is not None:
            if node._mock_new_name:
                parts.append(node._mock_new_name)
            elif node._mock_name:
                parts.append(node._mock_name)
            node = node._mock_parent
        if not parts:
            return "mock"
        parts.reverse()
        if parts[0] != "mock" and self._root_unnamed():
            parts.insert(0, "mock")
        return ".".join(parts)

    def _root_unnamed(self):
        node = self
        while node._mock_parent is not None:
            node = node._mock_parent
        return node._mock_name is None

    # -- attributes ---------------------------------------------------------
    def _make_child(self, name):
        return self._child_class()(parent=self, _new_name=name)

    def _child_class(self):
        return MagicMock if isinstance(self, MagicMock) else Mock

    def __getattr__(self, name):
        if name == "return_value":
            return self._get_return_value()
        if name.startswith("__") and name.endswith("__") or name.startswith("_mock_"):
            raise AttributeError(name)
        children = self.__dict__["_children"]
        child = children.get(name)
        if child is None:
            if self.__dict__["_mock_spec"] is not None and not hasattr(self.__dict__["_mock_spec"], name):
                raise AttributeError("Mock object has no attribute %r" % name)
            child = self._make_child(name)
            children[name] = child
        return child

    def __setattr__(self, name, value):
        if name == "return_value":
            self.__dict__["_mock_return_value"] = value
            return
        if name in ("side_effect", "called", "call_count", "call_args",
                    "call_args_list", "mock_calls", "method_calls"):
            self.__dict__[name] = value
            return
        if isinstance(value, NonCallableMock) and value._mock_parent is None and \
                not value._mock_new_name:
            value.__dict__["_mock_parent"] = self
            value.__dict__["_mock_new_name"] = name
        self.__dict__["_children"][name] = value

    def __delattr__(self, name):
        children = self.__dict__["_children"]
        if name in children:
            del children[name]
        else:
            raise AttributeError(name)

    def _get_return_value(self):
        rv = self._mock_return_value
        if rv is DEFAULT:
            rv = self._child_class()(parent=self, _new_name="()")
            self.__dict__["_mock_return_value"] = rv
        return rv

    def configure_mock(self, **kwargs):
        for k in kwargs:
            target = self
            parts = k.split(".")
            for p in parts[:-1]:
                target = getattr(target, p)
            setattr(target, parts[-1], kwargs[k])

    def reset_mock(self, visited=None, return_value=False, side_effect=False):
        d = self.__dict__
        d["called"] = False
        d["call_count"] = 0
        d["call_args"] = None
        d["call_args_list"] = []
        d["mock_calls"] = []
        d["method_calls"] = []
        if return_value:
            d["_mock_return_value"] = DEFAULT
        elif isinstance(d["_mock_return_value"], NonCallableMock):
            d["_mock_return_value"].reset_mock()
        if side_effect:
            d["side_effect"] = None
        for c in list(d["_children"].values()):
            if isinstance(c, NonCallableMock):
                c.reset_mock()

    # -- assertions ---------------------------------------------------------
    def _name_for_message(self):
        return self._mock_name or self._mock_new_name or "mock"

    def _call_str(self, args, kwargs):
        return _format_call(self._name_for_message(), args, kwargs)

    def assert_not_called(self):
        if self.call_count != 0:
            raise AssertionError("Expected '%s' to not have been called. Called %d times.%s" % (
                self._name_for_message(), self.call_count, self._calls_repr()))

    def assert_called(self):
        if self.call_count == 0:
            raise AssertionError("Expected '%s' to have been called." % (self._name_for_message(),))

    def assert_called_once(self):
        if not self.call_count == 1:
            raise AssertionError("Expected '%s' to have been called once. Called %d times.%s" % (
                self._name_for_message(), self.call_count, self._calls_repr()))

    def _calls_repr(self):
        if not self.call_args_list:
            return ""
        return "\nCalls: %s." % (self._calls_text(),)

    def _calls_text(self):
        return "[" + ", ".join(self._call_str(c._args, c._kwargs).replace(
            self._name_for_message(), "call", 1) for c in self.call_args_list) + "]"

    def assert_called_with(self, *args, **kwargs):
        if self.call_args is None:
            expected = self._call_str(args, kwargs)
            raise AssertionError("expected call not found.\nExpected: %s\n  Actual: not called." % expected)
        actual = self.call_args
        if not (actual._args == args and actual._kwargs == kwargs):
            raise AssertionError("expected call not found.\nExpected: %s\n  Actual: %s" % (
                self._call_str(args, kwargs), self._call_str(actual._args, actual._kwargs)))

    def assert_called_once_with(self, *args, **kwargs):
        if not self.call_count == 1:
            raise AssertionError("Expected '%s' to be called once. Called %d times.%s" % (
                self._name_for_message(), self.call_count, self._calls_repr()))
        self.assert_called_with(*args, **kwargs)

    def assert_any_call(self, *args, **kwargs):
        for c in self.call_args_list:
            if c._args == args and c._kwargs == kwargs:
                return
        raise AssertionError("%s call not found" % self._call_str(args, kwargs))

    def assert_has_calls(self, calls, any_order=False):
        expected = list(calls)
        actual = list(self.mock_calls)
        if not any_order:
            n = len(expected)
            for i in range(len(actual) - n + 1):
                if all(actual[i + j] == expected[j] for j in range(n)):
                    return
            raise AssertionError("Calls not found.\nExpected: %r\n  Actual: %r" % (expected, actual))
        remaining = list(actual)
        missing = []
        for e in expected:
            for i in range(len(remaining)):
                if remaining[i] == e:
                    del remaining[i]
                    break
            else:
                missing.append(e)
        if missing:
            raise AssertionError("%r not all found in call list" % (missing,))

    # -- calling --------------------------------------------------------------
    def _execute(self, args, kwargs):
        self.__dict__["called"] = True
        self.__dict__["call_count"] = self.call_count + 1
        c = _Call(args, kwargs)
        self.__dict__["call_args"] = c
        self.__dict__["call_args_list"].append(c)
        # mock_calls on this mock and every ancestor, with the dotted path
        node = self
        path = ""
        through_call = False
        while node is not None:
            node.__dict__["mock_calls"].append(_Call(args, kwargs, path))
            parent = node._mock_parent
            nn = node._mock_new_name
            if parent is not None and nn:
                if nn == "()":
                    path = "()" + ("." + path if path else "")
                    through_call = True
                else:
                    if path.startswith("("):
                        path = nn + path
                    elif path:
                        path = nn + "." + path
                    else:
                        path = nn
                if not through_call:
                    parent.__dict__["method_calls"].append(_Call(args, kwargs, path))
            node = parent
        effect = self.side_effect
        if effect is not None:
            if isinstance(effect, BaseException) or (isinstance(effect, type) and issubclass(effect, BaseException)):
                raise effect
            if callable(effect):
                result = effect(*args, **kwargs)
                if result is not DEFAULT:
                    return result
            else:
                try:
                    it = self.__dict__["_effect_iter"]
                except KeyError:
                    it = iter(effect)
                    self.__dict__["_effect_iter"] = it
                value = next(it)
                if isinstance(value, BaseException) or (isinstance(value, type) and issubclass(value, BaseException)):
                    raise value
                if value is not DEFAULT:
                    return value
        if self._mock_return_value is not DEFAULT:
            return self._mock_return_value
        if self._mock_wraps is not None:
            return self._mock_wraps(*args, **kwargs)
        return self._get_return_value()


class Mock(NonCallableMock):
    def __call__(self, *args, **kwargs):
        return self._execute(args, kwargs)


_MAGIC_DEFAULTS = {"__len__": 0, "__bool__": True, "__contains__": False, "__getitem__": DEFAULT,
                   "__setitem__": None, "__delitem__": None, "__enter__": DEFAULT,
                   "__exit__": False, "__int__": 1, "__float__": 1.0, "__lt__": NotImplemented,
                   "__gt__": NotImplemented, "__le__": NotImplemented, "__ge__": NotImplemented}


class MagicMock(Mock):
    def _magic(self, name, default):
        child = self.__dict__["_children"].get(name)
        if child is None:
            child = Mock(return_value=default, parent=self, _new_name=name)
            self.__dict__["_children"][name] = child
        return child

    def __len__(self):
        return self._magic("__len__", 0)()

    def __bool__(self):
        return self._magic("__bool__", True)()

    def __iter__(self):
        return iter(self._magic("__iter__", iter([]))())

    def __contains__(self, item):
        return self._magic("__contains__", False)(item)

    def __getitem__(self, key):
        return self._magic("__getitem__", DEFAULT)(key)

    def __setitem__(self, key, value):
        self._magic("__setitem__", None)(key, value)

    def __delitem__(self, key):
        self._magic("__delitem__", None)(key)

    def __enter__(self):
        return self._magic("__enter__", DEFAULT)()

    def __exit__(self, exc_type, exc, tb):
        return self._magic("__exit__", False)(exc_type, exc, tb)

    def __int__(self):
        return self._magic("__int__", 1)()

    def __float__(self):
        return self._magic("__float__", 1.0)()

    def __str__(self):
        return self._magic("__str__", object.__repr__(self))()

    def __hash__(self):
        return id(self)

    def __eq__(self, other):
        return self is other

    def __ne__(self, other):
        return self is not other

    def __lt__(self, other):
        return self._magic("__lt__", NotImplemented)(other)

    def __gt__(self, other):
        return self._magic("__gt__", NotImplemented)(other)

    def __le__(self, other):
        return self._magic("__le__", NotImplemented)(other)

    def __ge__(self, other):
        return self._magic("__ge__", NotImplemented)(other)


class PropertyMock(Mock):
    pass


# -- patch -------------------------------------------------------------------

def _resolve(target):
    """Find the object a dotted name such as "pkg.mod.attr" starts from:
    the longest prefix that is an imported module, then attribute lookups."""
    parts = target.split(".")
    for n in range(len(parts), 0, -1):
        name = ".".join(parts[:n])
        mod = sys.modules.get(name)
        if mod is not None:
            obj = mod
            for p in parts[n:]:
                obj = getattr(obj, p)
            return obj
    raise ImportError("patch target %r names a module that was not imported" % target)


class _Patch:
    def __init__(self, getter, attribute, new, kwargs, create=False):
        self.getter = getter
        self.attribute = attribute
        self.new = new
        self.kwargs = kwargs
        self.create = create
        self.target = None
        self.original = None
        self.had = False
        self.is_local = False

    def __enter__(self):
        target = self.getter()
        self.target = target
        d = getattr(target, "__dict__", None)
        self.had = hasattr(target, self.attribute)
        if not self.had and not self.create:
            raise AttributeError("%r does not have the attribute %r" % (target, self.attribute))
        self.original = getattr(target, self.attribute) if self.had else None
        self.is_local = d is not None and self.attribute in d
        new = self.new
        if new is DEFAULT:
            new = MagicMock(**self.kwargs)
        setattr(target, self.attribute, new)
        self.active = new
        return new

    def __exit__(self, exc_type, exc, tb):
        if self.is_local or not self.had:
            if self.had:
                setattr(self.target, self.attribute, self.original)
            else:
                delattr(self.target, self.attribute)
        else:
            try:
                delattr(self.target, self.attribute)
            except AttributeError:
                setattr(self.target, self.attribute, self.original)
            if getattr(self.target, self.attribute, None) is not self.original:
                setattr(self.target, self.attribute, self.original)
        return False

    start = __enter__

    def stop(self):
        return self.__exit__(None, None, None)

    def __call__(self, func):
        if isinstance(func, type):
            for name in dir(func):
                if name.startswith("test"):
                    attr = getattr(func, name)
                    if callable(attr):
                        setattr(func, name, self.__class__(self.getter, self.attribute, self.new,
                                                           self.kwargs, self.create)(attr))
            return func
        patcher = self

        def patched(*args, **kw):
            p = patcher.__class__(patcher.getter, patcher.attribute, patcher.new, patcher.kwargs, patcher.create)
            with p as value:
                if patcher.new is DEFAULT:
                    args = args + (value,)
                return func(*args, **kw)
        patched.__name__ = getattr(func, "__name__", "patched")
        patched.__wrapped__ = func
        return patched


class _PatchDict:
    def __init__(self, in_dict, values, clear, kwargs):
        self.in_dict = in_dict
        self.values = dict(values)
        self.values.update(kwargs)
        self.clear = clear
        self.saved = None

    def __enter__(self):
        d = self.in_dict
        if isinstance(d, str):
            d = _resolve(d)
        self._d = d
        self.saved = dict(d)
        if self.clear:
            d.clear()
        d.update(self.values)
        return d

    def __exit__(self, exc_type, exc, tb):
        self._d.clear()
        self._d.update(self.saved)
        return False

    def __call__(self, func):
        pd = self

        def patched(*args, **kw):
            with _PatchDict(pd.in_dict, pd.values, pd.clear, {}):
                return func(*args, **kw)
        patched.__name__ = getattr(func, "__name__", "patched")
        patched.__wrapped__ = func
        return patched


def _patch(target, new=DEFAULT, spec=None, create=False, spec_set=None, autospec=None,
           new_callable=None, **kwargs):
    if not isinstance(target, str):
        raise TypeError("patch target must be a string such as 'module.name'")
    idx = target.rfind(".")
    if idx < 0:
        raise TypeError("Need a valid target to patch. You supplied: %r" % (target,))
    path = target[:idx]
    attribute = target[idx + 1:]
    return _Patch(lambda: _resolve(path), attribute, new, kwargs, create)


def _patch_object(target, attribute, new=DEFAULT, spec=None, create=False, spec_set=None,
                  autospec=None, new_callable=None, **kwargs):
    return _Patch(lambda: target, attribute, new, kwargs, create)


def _patch_multiple(target, spec=None, create=False, spec_set=None, autospec=None,
                    new_callable=None, **kwargs):
    raise NotImplementedError("patch.multiple is not supported")


class _PatchFacade:
    def __call__(self, target, new=DEFAULT, spec=None, create=False, spec_set=None,
                 autospec=None, new_callable=None, **kwargs):
        return _patch(target, new, spec, create, spec_set, autospec, new_callable, **kwargs)

    def object(self, target, attribute, new=DEFAULT, spec=None, create=False, spec_set=None,
               autospec=None, new_callable=None, **kwargs):
        return _patch_object(target, attribute, new, spec, create, spec_set, autospec,
                             new_callable, **kwargs)

    def dict(self, in_dict, values=(), clear=False, **kwargs):
        return _PatchDict(in_dict, values, clear, kwargs)

    def multiple(self, target, spec=None, create=False, spec_set=None, autospec=None,
                 new_callable=None, **kwargs):
        return _patch_multiple(target)


patch = _PatchFacade()
