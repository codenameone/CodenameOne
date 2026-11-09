# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""A small pytest: the API tests are written against, and a runner.

Supported: raises (as context manager with match=, excinfo.value / .type /
.typename, and as a call), approx, fail, skip, xfail, importorskip (for modules already imported),
mark.parametrize / skip / skipif / xfail (other marks are accepted and
ignored), param(..., id=, marks=), fixture (function scope, found by parameter name from the test's class,
the module or the fixtures registered so far; yield fixtures run their teardown;
fixtures may use other fixtures; autouse is honoured, and a conftest module's
autouse fixtures apply to every module once it is imported; params= runs a
test once per value), async def tests (each on a fresh asyncio loop), the
built-in monkeypatch, tmp_path, tmpdir and request (param, addfinalizer,
getfixturevalue) fixtures (capsys and the rest are absent), the httpx_mock
fixture of the bundled pytest_httpx2, and run_module(module) /
main([...]) which run every test_* function and Test* class of a module
object and print a summary such as "3 passed, 1 failed".

Not supported: collecting files from disk (a conftest module is imported by
the test runner's code like any other), plugin entry points, async fixtures, fixture
scopes above function (accepted, treated as function), assertion rewriting (a failed assert reports its message only), capture of
stdout, and the long traceback report. Plain `assert` is Python's assert.
"""

import sys
import re
import inspect
import math

__all__ = ["raises", "approx", "fail", "skip", "xfail", "mark", "fixture",
           "main", "run_module", "param", "Failed", "Skipped", "XFailed",
           "ExceptionInfo", "warns", "importorskip", "FixtureRequest", "MonkeyPatch"]


class Failed(BaseException):
    pass


class Skipped(BaseException):
    pass


class XFailed(BaseException):
    pass


def fail(reason="", pytrace=True):
    raise Failed(reason)


def skip(reason="", allow_module_level=False):
    raise Skipped(reason)


def xfail(reason=""):
    raise XFailed(reason)


def importorskip(modname, minversion=None, reason=None):
    mod = sys.modules.get(modname)
    if mod is None:
        raise Skipped(reason or "could not import %r" % modname)
    return mod


class ExceptionInfo:
    def __init__(self):
        self.value = None
        self.type = None
        self.tb = None

    @property
    def typename(self):
        return self.type.__name__

    def match(self, regexp):
        if not re.search(regexp, str(self.value)):
            raise AssertionError("Regex pattern did not match.\n Regex: %r\n Input: %r" % (
                regexp, str(self.value)))
        return True

    def errisinstance(self, exc):
        return isinstance(self.value, exc)

    def __repr__(self):
        if self.type is None:
            return "<ExceptionInfo for raises contextmanager>"
        return "<ExceptionInfo %s(%r) tblen=0>" % (self.type.__name__, str(self.value))


class _RaisesContext:
    def __init__(self, expected, match):
        self.expected = expected
        self.match_expr = match
        self.excinfo = ExceptionInfo()

    def __enter__(self):
        return self.excinfo

    def __exit__(self, exc_type, exc, tb):
        if exc_type is None:
            raise Failed("DID NOT RAISE %s" % _expected_repr(self.expected))
        if not issubclass(exc_type, self.expected):
            return False
        self.excinfo.value = exc
        self.excinfo.type = exc_type
        if self.match_expr is not None:
            self.excinfo.match(self.match_expr)
        return True


def _expected_repr(expected):
    if isinstance(expected, tuple):
        return "any of (" + ", ".join(_expected_repr(e) for e in expected) + ")"
    return getattr(expected, "__name__", repr(expected))


def raises(expected_exception, *args, match=None, **kwargs):
    ctx = _RaisesContext(expected_exception, match)
    if not args:
        return ctx
    func = args[0]
    with ctx:
        func(*args[1:], **kwargs)
    return ctx.excinfo


class _WarnsContext:
    def __init__(self, expected):
        self.expected = expected

    def __enter__(self):
        import warnings
        self._cm = warnings.catch_warnings(record=True)
        self.list = self._cm.__enter__()
        warnings.simplefilter("always")
        return self

    def __exit__(self, exc_type, exc, tb):
        self._cm.__exit__(exc_type, exc, tb)
        if exc_type is not None:
            return False
        for w in self.list:
            if isinstance(w.message, self.expected):
                return True
        raise Failed("DID NOT WARN. No warnings of type %s were emitted." % (self.expected,))


def warns(expected_warning=Warning, *args, **kwargs):
    return _WarnsContext(expected_warning)


# -- approx ---------------------------------------------------------------

class _Approx:
    def __init__(self, expected, rel=None, abs=None, nan_ok=False):
        self.expected = expected
        self.rel = rel
        self.abs = abs
        self.nan_ok = nan_ok

    def _tolerance(self, exp):
        mag = exp if exp >= 0 else -exp
        if self.rel is None and self.abs is None:
            tol = 1e-6 * mag
            return tol if tol > 1e-12 else 1e-12
        if self.abs is None:
            return self.rel * mag
        if self.rel is None:
            return self.abs
        tol = self.rel * mag
        return tol if tol > self.abs else self.abs

    def _close(self, actual, exp):
        if isinstance(exp, bool) or not isinstance(exp, (int, float)):
            return actual == exp
        if isinstance(actual, bool) or not isinstance(actual, (int, float)):
            return False
        if actual == exp:
            return True
        if math.isnan(exp):
            return self.nan_ok and math.isnan(actual)
        if math.isinf(exp) or math.isinf(actual):
            return False
        diff = actual - exp
        if diff < 0:
            diff = -diff
        return diff <= self._tolerance(exp)

    def __eq__(self, actual):
        exp = self.expected
        if isinstance(exp, dict):
            if not isinstance(actual, dict) or set(actual) != set(exp):
                return False
            return all(self._close(actual[k], exp[k]) for k in exp)
        if isinstance(exp, (list, tuple)):
            if not isinstance(actual, (list, tuple)) or len(actual) != len(exp):
                return False
            return all(self._close(a, e) for a, e in zip(actual, exp))
        return self._close(actual, exp)

    def __ne__(self, actual):
        return not self.__eq__(actual)

    def __hash__(self):
        return id(self)

    def __repr__(self):
        exp = self.expected
        if isinstance(exp, (int, float)) and not isinstance(exp, bool):
            tol = self._tolerance(exp)
            if math.isnan(exp) or math.isinf(exp):
                return "%s" % (exp,)
            return "%s ± %.1e" % (exp, tol)
        if isinstance(exp, dict):
            return "approx({" + ", ".join("%r: %r" % (k, _Approx(v, self.rel, self.abs, self.nan_ok))
                                         for k, v in exp.items()) + "})"
        if isinstance(exp, (list, tuple)):
            body = ", ".join(repr(_Approx(v, self.rel, self.abs, self.nan_ok)) for v in exp)
            return "approx([" + body + "])" if isinstance(exp, list) else "approx((" + body + ",))"
        return "approx(%r)" % (exp,)


def approx(expected, rel=None, abs=None, nan_ok=False):
    return _Approx(expected, rel, abs, nan_ok)


# -- marks ------------------------------------------------------------------

class _ParamSet:
    def __init__(self, values, marks, id):
        self.values = values
        self.marks = marks
        self.id = id


def param(*values, marks=(), id=None):
    if not isinstance(marks, (list, tuple)):
        marks = (marks,)
    return _ParamSet(values, [m.mark if isinstance(m, _MarkDecorator) else m for m in marks], id)


class _Mark:
    def __init__(self, name, args, kwargs):
        self.name = name
        self.args = args
        self.kwargs = kwargs

    def _apply(self, func):
        marks = list(getattr(func, "pytestmark", []))
        marks.append(self)
        func.pytestmark = marks
        return func

    def __repr__(self):
        return "Mark(name=%r, args=%r, kwargs=%r)" % (self.name, self.args, self.kwargs)


class _MarkGenerator:
    def __getattr__(self, name):
        if name.startswith("__"):
            raise AttributeError(name)
        return _MarkMaker(name)


class _MarkMaker:
    """mark.name(...) returns a decorator that attaches the mark."""

    def __init__(self, name):
        self.name = name

    def __call__(self, *args, **kwargs):
        if len(args) == 1 and not kwargs and callable(args[0]) and self.name not in ("parametrize", "skipif"):
            return _Mark(self.name, (), {})._apply(args[0])
        return _MarkDecorator(_Mark(self.name, args, kwargs))


class _MarkDecorator:
    def __init__(self, mark):
        self.mark = mark

    def __call__(self, func):
        return self.mark._apply(func)


mark = _MarkGenerator()


# -- fixtures -----------------------------------------------------------------

_registry = {}


class _FixtureDef:
    def __init__(self, func, name, autouse, params=None, ids=None):
        self.func = func
        self.name = name
        self.autouse = autouse
        self.params = params
        self.ids = ids


class FixtureRequest:
    """The `request` a fixture or test asks for: `param` (a parametrized
    fixture's current value), `fixturename`, `addfinalizer` and
    `getfixturevalue`. The node, config and scope objects are absent."""

    def __init__(self, param=None, fixturename=None, session=None, instance=None, cache=None):
        self.param = param
        self.fixturename = fixturename
        self._session = session
        self._instance = instance
        self._cache = cache

    def addfinalizer(self, finalizer):
        self._session.teardowns.append(finalizer)

    def getfixturevalue(self, argname):
        return self._session.make(argname, self._instance, self._cache, [])


class _LocalPath:
    """tmpdir's value: the py.path.local subset tests use, over a string
    path. `/` and join give another, str() and os.fspath the path."""

    def __init__(self, path):
        import os
        self.strpath = os.fspath(path)

    def __fspath__(self):
        return self.strpath

    def __str__(self):
        return self.strpath

    def __repr__(self):
        return "local(%r)" % self.strpath

    def __eq__(self, other):
        import os
        try:
            return self.strpath == os.fspath(other)
        except TypeError:
            return NotImplemented

    def __hash__(self):
        return hash(self.strpath)

    def __truediv__(self, other):
        return self.join(other)

    def join(self, *parts):
        import os
        return _LocalPath(os.path.join(self.strpath, *[str(p) for p in parts]))

    @property
    def basename(self):
        import os
        return os.path.basename(self.strpath)

    @property
    def dirname(self):
        import os
        return os.path.dirname(self.strpath)

    def dirpath(self):
        return _LocalPath(self.dirname)

    def mkdir(self, *parts):
        import os
        target = self.join(*parts) if parts else self
        os.mkdir(target.strpath)
        return target

    def ensure(self, *parts, dir=False):
        import os
        target = self.join(*parts) if parts else self
        if dir:
            os.makedirs(target.strpath, exist_ok=True)
        else:
            os.makedirs(os.path.dirname(target.strpath), exist_ok=True)
            if not os.path.exists(target.strpath):
                open(target.strpath, "a").close()
        return target

    def exists(self):
        import os
        return os.path.exists(self.strpath)

    def check(self, file=None, dir=None, exists=None):
        import os
        if file:
            return os.path.isfile(self.strpath)
        if dir:
            return os.path.isdir(self.strpath)
        return os.path.exists(self.strpath)

    def isdir(self):
        import os
        return os.path.isdir(self.strpath)

    def isfile(self):
        import os
        return os.path.isfile(self.strpath)

    def listdir(self):
        import os
        return [self.join(n) for n in sorted(os.listdir(self.strpath))]

    def read(self, mode="r"):
        with open(self.strpath, mode) as f:
            return f.read()

    def read_text(self, encoding="utf-8"):
        with open(self.strpath, encoding=encoding) as f:
            return f.read()

    def read_binary(self):
        with open(self.strpath, "rb") as f:
            return f.read()

    def write(self, data, mode="w", ensure=False):
        if ensure:
            self.dirpath().ensure(dir=True)
        if isinstance(data, bytes) and "b" not in mode:
            mode += "b"
        with open(self.strpath, mode) as f:
            f.write(data)

    def write_text(self, data, encoding="utf-8", ensure=False):
        self.write(data.encode(encoding), "wb", ensure)

    def write_binary(self, data, ensure=False):
        self.write(data, "wb", ensure)

    def remove(self):
        import os
        import shutil
        if os.path.isdir(self.strpath):
            shutil.rmtree(self.strpath)
        else:
            os.remove(self.strpath)


def fixture(func=None, *, scope="function", params=None, autouse=False, ids=None, name=None):
    def register(f):
        fname = name or f.__name__
        f.__cn1_fixture__ = _FixtureDef(f, fname, autouse, params, ids)
        _registry[fname] = f.__cn1_fixture__
        return f
    if func is not None:
        return register(func)
    return register


_MISSING = object()
_NOTSET = object()
_ENV = object()
_CWD = object()
_SYSPATH = object()


class _MonkeyPatch:
    def __init__(self):
        self._undo = []

    def setattr(self, target, name, value=_NOTSET, raising=True):
        if isinstance(target, str):
            # "package.module.attr": the module must already be imported, as
            # nothing can be imported by a name built at run time
            if value is not _NOTSET:
                raise TypeError("use setattr(target, name, value) or setattr(target, value) with target "
                                "being a dotted import string")
            value = name
            path, _, name = target.rpartition(".")
            target = _resolve_dotted(path)
        old = getattr(target, name, _MISSING)
        if old is _MISSING and raising:
            raise AttributeError("%r has no attribute %r" % (target, name))
        self._undo.append((target, name, old))
        setattr(target, name, value)

    def delattr(self, target, name, raising=True):
        old = getattr(target, name, _MISSING)
        if old is _MISSING:
            if raising:
                raise AttributeError(name)
            return
        self._undo.append((target, name, old))
        delattr(target, name)

    def setenv(self, name, value, prepend=None):
        import os
        value = str(value)
        if prepend and name in os.environ:
            value = value + prepend + os.environ[name]
        self._undo.append((_ENV, name, os.environ.get(name, _MISSING)))
        os.environ[name] = value

    def delenv(self, name, raising=True):
        import os
        if name not in os.environ:
            if raising:
                raise KeyError(name)
            return
        self._undo.append((_ENV, name, os.environ[name]))
        del os.environ[name]

    def chdir(self, path):
        import os
        self._undo.append((_CWD, None, os.getcwd()))
        os.chdir(os.fspath(path))

    def syspath_prepend(self, path):
        self._undo.append((_SYSPATH, None, list(sys.path)))
        sys.path.insert(0, str(path))

    def context(self):
        import contextlib

        @contextlib.contextmanager
        def ctx():
            m = _MonkeyPatch()
            try:
                yield m
            finally:
                m.undo()
        return ctx()

    def setitem(self, dic, name, value):
        self._undo.append((dic, name, dic.get(name, _MISSING)))
        dic[name] = value

    def delitem(self, dic, name, raising=True):
        if name not in dic:
            if raising:
                raise KeyError(name)
            return
        self._undo.append((dic, name, dic[name]))
        del dic[name]

    def undo(self):
        while self._undo:
            target, name, old = self._undo.pop()
            if target is _ENV:
                import os
                if old is _MISSING:
                    os.environ.pop(name, None)
                else:
                    os.environ[name] = old
            elif target is _CWD:
                import os
                os.chdir(old)
            elif target is _SYSPATH:
                sys.path[:] = old
            elif isinstance(target, dict):
                if old is _MISSING:
                    target.pop(name, None)
                else:
                    target[name] = old
            elif old is _MISSING:
                delattr(target, name)
            else:
                setattr(target, name, old)


MonkeyPatch = _MonkeyPatch


def _resolve_dotted(path):
    parts = path.split(".")
    for i in range(len(parts), 0, -1):
        mod = sys.modules.get(".".join(parts[:i]))
        if mod is not None:
            obj = mod
            for attr in parts[i:]:
                obj = getattr(obj, attr)
            return obj
    raise ImportError("monkeypatch target %r is not imported" % path)


# -- running ----------------------------------------------------------------

def _param_names(func, skip_self):
    names = list(inspect.signature(func).parameters)
    if skip_self and names:
        names = names[1:]
    return names


def _split_argnames(argnames):
    if isinstance(argnames, str):
        return [a.strip() for a in argnames.split(",") if a.strip()]
    return list(argnames)


def _marks_of(func):
    return list(getattr(func, "pytestmark", []))


def _expand_params(func):
    """The (id, {name: value}, marks) combinations parametrize marks give."""
    combos = [("", {}, [])]
    for m in reversed(_marks_of(func)):
        if m.name != "parametrize":
            continue
        names = _split_argnames(m.args[0])
        values = m.args[1]
        ids = m.kwargs.get("ids")
        nxt = []
        for base_id, base_vals, base_marks in combos:
            for i, v in enumerate(values):
                marks = []
                if isinstance(v, _ParamSet):
                    marks = v.marks
                    tup = v.values
                    pid = v.id
                elif len(names) == 1:
                    tup = (v,)
                    pid = None
                else:
                    tup = tuple(v)
                    pid = None
                if pid is None:
                    if ids is not None and i < len(ids):
                        pid = str(ids[i])
                    else:
                        pid = "-".join(_idpart(x, names[j] if j < len(names) else "", i)
                                       for j, x in enumerate(tup))
                vals = dict(base_vals)
                for j, n in enumerate(names):
                    vals[n] = tup[j]
                nid = pid if not base_id else base_id + "-" + pid
                nxt.append((nid, vals, base_marks + marks))
        combos = nxt
    return combos


def _idpart(value, argname, index):
    if isinstance(value, bool) or value is None:
        return str(value)
    if isinstance(value, (int, float)):
        return str(value)
    if isinstance(value, str):
        return value
    return argname + str(index)


class _Result:
    def __init__(self):
        self.passed = 0
        self.failed = 0
        self.skipped = 0
        self.xfailed = 0
        self.xpassed = 0
        self.errors = 0
        self.outcomes = []

    def record(self, nodeid, outcome, detail=""):
        self.outcomes.append((nodeid, outcome, detail))
        if outcome == "passed":
            self.passed += 1
        elif outcome == "failed":
            self.failed += 1
        elif outcome == "skipped":
            self.skipped += 1
        elif outcome == "xfailed":
            self.xfailed += 1
        elif outcome == "xpassed":
            self.xpassed += 1
        elif outcome == "error":
            self.errors += 1

    def summary(self):
        parts = []
        if self.failed:
            parts.append("%d failed" % self.failed)
        if self.passed:
            parts.append("%d passed" % self.passed)
        if self.skipped:
            parts.append("%d skipped" % self.skipped)
        if self.xfailed:
            parts.append("%d xfailed" % self.xfailed)
        if self.xpassed:
            parts.append("%d xpassed" % self.xpassed)
        if self.errors:
            parts.append("%d error%s" % (self.errors, "s" if self.errors != 1 else ""))
        return ", ".join(parts) if parts else "no tests ran"


class _Session:
    def __init__(self, module, quiet):
        self.module = module
        self.quiet = quiet
        self.result = _Result()
        self.teardowns = []
        self.fixture_params = {}

    def param_fixtures(self, func, instance, is_method):
        """The parametrized fixtures a test reaches, directly or through
        other fixtures, in the order first met."""
        found = []
        seen = set()
        todo = list(_param_names(func, is_method))
        while todo:
            n = todo.pop(0)
            if n in seen:
                continue
            seen.add(n)
            f, m = self.find_fixture(n, instance)
            if f is None:
                continue
            d = f.__cn1_fixture__
            if d.params is not None:
                found.append(d)
            todo.extend(_param_names(f, m))
        return found

    def expand(self, func, instance, is_method):
        """_expand_params, multiplied by each parametrized fixture's params;
        the fixture values ride in the dict under a ("fixture", name) key."""
        combos = _expand_params(func)
        for d in self.param_fixtures(func, instance, is_method):
            nxt = []
            for base_id, vals, marks in combos:
                for i, p in enumerate(d.params):
                    pid = str(d.ids[i]) if d.ids is not None and i < len(d.ids) \
                        else _idpart(p, d.name, i)
                    v = dict(vals)
                    v[("fixture", d.name)] = p
                    nxt.append((pid if not base_id else base_id + "-" + pid, v, marks))
            combos = nxt
        return combos

    # fixture lookup: class attribute, module attribute, then the registry
    def find_fixture(self, name, instance):
        if instance is not None:
            f = getattr(type(instance), name, None)
            if f is not None and hasattr(f, "__cn1_fixture__"):
                return f, True
        f = getattr(self.module, name, None)
        if f is not None and hasattr(f, "__cn1_fixture__"):
            return f, False
        d = _registry.get(name)
        if d is None and name in ("httpx_mock", "httpx2_mock"):
            # A plugin's fixture, used without importing the plugin: pytest
            # would have loaded it through its entry point.
            import pytest_httpx2  # noqa: F401
            d = _registry.get(name)
        if d is not None:
            return d.func, False
        return None, False

    def make(self, name, instance, cache, stack):
        if name in cache and name != "request":
            return cache[name]
        if name == "monkeypatch":
            mp = _MonkeyPatch()
            self.teardowns.append(mp.undo)
            cache[name] = mp
            return mp
        if name == "request":
            req = FixtureRequest(self.fixture_params.get(stack[-1]) if stack else None,
                                 stack[-1] if stack else None, self, instance, cache)
            cache[name] = req
            return req
        if name == "tmpdir":
            # tmp_path's directory, as py.path.local: pytest gives both the same one
            value = _LocalPath(self.make("tmp_path", instance, cache, stack))
            cache[name] = value
            return value
        if name == "tmp_path":
            # A fresh directory per test under the system temporary directory,
            # as pytest's tmp_path is; it is left in place, as pytest keeps
            # the last few runs' directories.
            import tempfile
            import pathlib
            path = pathlib.Path(tempfile.mkdtemp(prefix="pytest-"))
            cache[name] = path
            return path
        func, is_method = self.find_fixture(name, instance)
        if func is None:
            raise Failed("fixture '%s' not found" % name)
        if name in stack:
            raise Failed("recursive dependency involving fixture '%s'" % name)
        stack.append(name)
        names = _param_names(func, is_method)
        kwargs = {}
        for n in names:
            kwargs[n] = self.make(n, instance, cache, stack)
        stack.pop()
        if is_method:
            bound = getattr(instance, func.__name__)
            value = bound(**kwargs)
        else:
            value = func(**kwargs)
        if _is_generator(value):
            gen = value
            value = next(gen)
            self.teardowns.append(_finish(gen))
        cache[name] = value
        return value

    def run_teardowns(self):
        errors = []
        while self.teardowns:
            t = self.teardowns.pop()
            try:
                t()
            except Exception as e:
                errors.append(e)
        return errors

    def autouse_names(self, instance):
        # conftest's autouse fixtures apply to every module, as pytest's do
        names = [d.name for d in _registry.values()
                 if d.autouse and getattr(d.func, "__module__", "").rpartition(".")[2] == "conftest"]
        for n in dir(self.module):
            f = getattr(self.module, n)
            d = getattr(f, "__cn1_fixture__", None)
            if d is not None and d.autouse and d.name not in names:
                names.append(d.name)
        if instance is not None:
            for n in dir(type(instance)):
                f = getattr(type(instance), n)
                d = getattr(f, "__cn1_fixture__", None)
                if d is not None and d.autouse and d.name not in names:
                    names.append(d.name)
        return names

    def run_one(self, nodeid, func, instance, is_method, extra_marks, values):
        marks = _marks_of(func) + extra_marks
        if instance is not None:
            marks = marks + _marks_of(type(instance))
        xfail_mark = None
        for m in marks:
            if m.name == "skip":
                reason = m.kwargs.get("reason", m.args[0] if m.args else "")
                self.result.record(nodeid, "skipped", reason)
                return "s"
            if m.name == "skipif":
                cond = m.args[0] if m.args else True
                if cond:
                    self.result.record(nodeid, "skipped", m.kwargs.get("reason", ""))
                    return "s"
            if m.name == "xfail":
                cond = m.args[0] if m.args else True
                if cond:
                    xfail_mark = m
        cache = {}
        self.fixture_params = dict((k[1], v) for k, v in values.items() if isinstance(k, tuple))
        outcome = None
        detail = ""
        try:
            try:
                for n in self.autouse_names(instance):
                    self.make(n, instance, cache, [])
                names = _param_names(func, is_method)
                kwargs = {}
                for n in names:
                    if n in values:
                        kwargs[n] = values[n]
                    else:
                        kwargs[n] = self.make(n, instance, cache, [])
                if is_method:
                    ret = getattr(instance, func.__name__)(**kwargs)
                else:
                    ret = func(**kwargs)
                if type(ret).__name__ == "coroutine":
                    # An async test runs to completion on a fresh loop, as
                    # pytest-asyncio's function-scoped loop runs it.
                    import asyncio
                    asyncio.run(ret)
                outcome = "passed"
            except Skipped as e:
                outcome = "skipped"
                detail = str(e)
            except XFailed as e:
                outcome = "xfailed"
                detail = str(e)
            except BaseException as e:
                if xfail_mark is not None:
                    want = xfail_mark.kwargs.get("raises")
                    if want is None or isinstance(e, want):
                        outcome = "xfailed"
                    else:
                        outcome = "failed"
                else:
                    outcome = "failed"
                detail = _describe(e)
        finally:
            errs = self.run_teardowns()
        if outcome == "passed" and xfail_mark is not None:
            if xfail_mark.kwargs.get("strict"):
                outcome = "failed"
                detail = "[XPASS(strict)] " + str(xfail_mark.kwargs.get("reason", ""))
            else:
                outcome = "xpassed"
        if errs and outcome in ("passed", "xpassed"):
            self.result.record(nodeid, outcome, detail)
            self.result.record(nodeid, "error", _describe(errs[0]))
            return "E"
        self.result.record(nodeid, outcome, detail)
        return {"passed": ".", "failed": "F", "skipped": "s", "xfailed": "x", "xpassed": "X"}[outcome]

    def run_function(self, prefix, func, instance, is_method, extra_marks):
        out = ""
        for pid, values, pmarks in self.expand(func, instance, is_method):
            nodeid = prefix + func.__name__ + ("[" + pid + "]" if pid else "")
            out += self.run_one(nodeid, func, instance, is_method, extra_marks + pmarks, values)
        return out

    def run_class(self, name, cls):
        out = ""
        for attr in dir(cls):
            if not attr.startswith("test"):
                continue
            member = getattr(cls, attr)
            if not callable(member):
                continue
            for pid, values, pmarks in self.expand(member, cls(), True):
                nodeid = "%s::%s::%s%s" % (self.module_name(), name, attr, "[" + pid + "]" if pid else "")
                instance = cls()
                if hasattr(instance, "setup_method"):
                    instance.setup_method(getattr(instance, attr))
                out += self.run_one(nodeid, member, instance, True, pmarks, values)
                if hasattr(instance, "teardown_method"):
                    instance.teardown_method(getattr(instance, attr))
        return out

    def module_name(self):
        return getattr(self.module, "__name__", "module")


def _is_generator(value):
    return type(value).__name__ == "generator"


def _finish(gen):
    def run():
        try:
            next(gen)
        except StopIteration:
            return
        raise Failed("fixture function has more than one 'yield'")
    return run


def _describe(e):
    text = str(e)
    name = type(e).__name__
    return "%s: %s" % (name, text) if text else name


def run_module(module, quiet=False):
    """Run every test_* function and Test* class of a module object. Prints
    one line per test when not quiet, then the summary; returns the exit code
    pytest would use (0 all passed, 1 failures)."""
    session = _Session(module, quiet)
    prefix = session.module_name() + "::"
    line = ""
    for name in dir(module):
        obj = getattr(module, name)
        if name.startswith("test") and callable(obj) and not isinstance(obj, type) and \
                not hasattr(obj, "__cn1_fixture__"):
            line += session.run_function(prefix, obj, None, False, [])
        elif isinstance(obj, type) and name.startswith("Test"):
            line += session.run_class(name, obj)
    result = session.result
    if not quiet:
        print(line)
        for nodeid, outcome, detail in result.outcomes:
            if outcome in ("failed", "error"):
                print("%s %s - %s" % (outcome.upper(), nodeid, detail))
    print("=== %s ===" % result.summary())
    run_module.last = result
    return 0 if result.failed == 0 and result.errors == 0 else 1


def main(args=None, plugins=None, module=None):
    """main([...]): the module to run is the keyword `module`, or __main__.
    Paths on the command line cannot be loaded from disk, so `args` is read
    only for -q, which prints the summary line alone."""
    if module is None:
        module = sys.modules["__main__"]
    return run_module(module, "-q" in (args or []))
