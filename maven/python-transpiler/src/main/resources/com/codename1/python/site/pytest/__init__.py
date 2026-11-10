# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""A small pytest: the API tests are written against, and a runner.

Supported: raises (as context manager with match=, excinfo.value / .type /
.typename, and as a call), approx, fail, skip, xfail, importorskip (for modules already imported),
mark.parametrize / skip / skipif / xfail (other marks are accepted and
ignored), param(..., id=, marks=), fixture (function scope, found by parameter name from the test's class,
the module or the fixtures registered so far; yield fixtures run their teardown;
fixtures may use other fixtures; autouse is honoured), the built-in
monkeypatch fixture (capsys, tmp_path and the rest are absent), and run_module(module) /
main([...]) which run every test_* function and Test* class of a module
object and print a summary such as "3 passed, 1 failed".

Not supported: collecting files from disk, conftest.py, plugins, fixture
scopes above function (accepted, treated as function), assertion rewriting (a failed assert reports its message only), capture of
stdout, and the long traceback report. Plain `assert` is Python's assert.
"""

import sys
import re
import inspect
import math

__all__ = ["raises", "approx", "fail", "skip", "xfail", "mark", "fixture",
           "main", "run_module", "param", "Failed", "Skipped", "XFailed",
           "ExceptionInfo", "warns", "importorskip"]


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
    def __init__(self, func, name, autouse):
        self.func = func
        self.name = name
        self.autouse = autouse


def fixture(func=None, *, scope="function", params=None, autouse=False, ids=None, name=None):
    def register(f):
        fname = name or f.__name__
        f.__cn1_fixture__ = _FixtureDef(f, fname, autouse)
        _registry[fname] = f.__cn1_fixture__
        return f
    if func is not None:
        return register(func)
    return register


class _MonkeyPatch:
    def __init__(self):
        self._undo = []

    def setattr(self, target, name, value=None, raising=True):
        if value is None and isinstance(target, str):
            raise TypeError("monkeypatch.setattr with a string target is not supported")
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
            if isinstance(target, dict):
                if old is _MISSING:
                    target.pop(name, None)
                else:
                    target[name] = old
            elif old is _MISSING:
                delattr(target, name)
            else:
                setattr(target, name, old)


_MISSING = object()


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
        if d is not None:
            return d.func, False
        return None, False

    def make(self, name, instance, cache, stack):
        if name in cache:
            return cache[name]
        if name == "monkeypatch":
            mp = _MonkeyPatch()
            self.teardowns.append(mp.undo)
            cache[name] = mp
            return mp
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
        names = []
        for n in dir(self.module):
            f = getattr(self.module, n)
            d = getattr(f, "__cn1_fixture__", None)
            if d is not None and d.autouse:
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
                    getattr(instance, func.__name__)(**kwargs)
                else:
                    func(**kwargs)
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
        for pid, values, pmarks in _expand_params(func):
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
            for pid, values, pmarks in _expand_params(member):
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
