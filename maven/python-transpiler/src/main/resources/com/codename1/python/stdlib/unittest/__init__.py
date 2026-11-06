# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""Unit testing framework.

Supported: TestCase with setUp/tearDown/setUpClass/tearDownClass/addCleanup,
the assert* family (including assertRaises, assertRaisesRegex, assertWarns,
assertLogs-free), subTest, skip/skipIf/skipUnless/expectedFailure, TestSuite,
TestLoader (loadTestsFromTestCase, loadTestsFromModule, loadTestsFromName for
a module object), TestResult, TextTestRunner and main().

Not supported: test discovery from the file system, loadTestsFromName with a
dotted string (it names a module by a computed name), assertLogs, and the
difflib-style diff appended to the message of a failed container comparison;
the first line of every message matches CPython's. A failure report has no
traceback lines because the program is compiled ahead of time. main() runs
the module given as its module argument, or __main__.
"""

import sys
import time
import re
import warnings as _warnings

__all__ = ["TestCase", "TestSuite", "TestLoader", "TestResult", "TextTestRunner",
           "TextTestResult", "SkipTest", "skip", "skipIf", "skipUnless",
           "expectedFailure", "main", "defaultTestLoader", "FunctionTestCase",
           "installHandler", "IsolatedAsyncioTestCase", "TestProgram"]

__unittest = True


class SkipTest(Exception):
    pass


class _ShouldStop(Exception):
    pass


def skip(reason):
    def decorator(item):
        if isinstance(item, type):
            item.__unittest_skip__ = True
            item.__unittest_skip_why__ = reason
            return item

        def skip_wrapper(*args, **kwargs):
            raise SkipTest(reason)
        skip_wrapper.__name__ = item.__name__
        skip_wrapper.__unittest_skip__ = True
        skip_wrapper.__unittest_skip_why__ = reason
        return skip_wrapper
    return decorator


def skipIf(condition, reason):
    if condition:
        return skip(reason)
    return _id


def skipUnless(condition, reason):
    if not condition:
        return skip(reason)
    return _id


def _id(obj):
    return obj


def expectedFailure(test_item):
    test_item.__unittest_expecting_failure__ = True
    return test_item


def installHandler():
    pass


class _Outcome:
    pass


class _SubTest:
    def __init__(self, test_case, message, params):
        self.test_case = test_case
        self.message = message
        self.params = params

    def id(self):
        return "%s %s" % (self.test_case.id(), self._desc())

    def _desc(self):
        parts = []
        if self.message is not _marker:
            parts.append("[%s]" % (self.message,))
        if self.params:
            parts.append("(" + ", ".join("%s=%r" % (k, v) for k, v in self.params.items()) + ")")
        return " ".join(parts) or "(<subtest>)"

    def __str__(self):
        return "%s %s" % (self.test_case, self._desc())

    def shortDescription(self):
        return None


_marker = object()


class _SubTestContext:
    def __init__(self, case, msg, params):
        self.case = case
        self.msg = msg
        self.params = params

    def __enter__(self):
        return None

    def __exit__(self, exc_type, exc, tb):
        if exc_type is None:
            return False
        result = self.case._result
        if result is None:
            return False
        sub = _SubTest(self.case, self.msg, self.params)
        if issubclass(exc_type, SkipTest):
            result.addSkip(sub, str(exc))
        elif issubclass(exc_type, self.case.failureException):
            result.addSubTest(self.case, sub, (exc_type, exc, tb))
            self.case._subtest_failed = True
        else:
            result.addSubTest(self.case, sub, (exc_type, exc, tb))
            self.case._subtest_failed = True
        return True


class _AssertRaisesContext:
    def __init__(self, expected, test_case, expected_regex=None):
        self.expected = expected
        self.test_case = test_case
        self.expected_regex = expected_regex
        self.exception = None
        self.obj_name = None

    def __enter__(self):
        return self

    def __exit__(self, exc_type, exc_value, tb):
        if exc_type is None:
            name = getattr(self.expected, "__name__", str(self.expected))
            if self.obj_name:
                raise self.test_case.failureException("%s not raised by %s" % (name, self.obj_name))
            raise self.test_case.failureException("%s not raised" % name)
        if not issubclass(exc_type, self.expected):
            return False
        self.exception = exc_value
        if self.expected_regex is None:
            return True
        expected_regex = self.expected_regex
        if isinstance(expected_regex, str):
            expected_regex = re.compile(expected_regex)
        if not expected_regex.search(str(exc_value)):
            raise self.test_case.failureException(
                '"%s" does not match "%s"' % (expected_regex.pattern, str(exc_value)))
        return True


class _AssertWarnsContext:
    def __init__(self, expected, test_case, expected_regex=None):
        self.expected = expected
        self.test_case = test_case
        self.expected_regex = expected_regex
        self.warning = None
        self.warnings = []
        self.filename = None
        self.lineno = None

    def __enter__(self):
        self._cm = _warnings.catch_warnings(record=True)
        self._log = self._cm.__enter__()
        _warnings.simplefilter("always")
        return self

    def __exit__(self, exc_type, exc_value, tb):
        self._cm.__exit__(exc_type, exc_value, tb)
        if exc_type is not None:
            return False
        first_matching = None
        for m in self._log:
            w = m.message
            if not isinstance(w, self.expected):
                continue
            if first_matching is None:
                first_matching = w
            if self.expected_regex is not None and not re.search(self.expected_regex, str(w)):
                continue
            self.warning = w
            self.warnings = self._log
            return True
        if first_matching is not None:
            raise self.test_case.failureException('"%s" does not match "%s"' % (
                self.expected_regex if isinstance(self.expected_regex, str) else self.expected_regex.pattern,
                str(first_matching)))
        name = getattr(self.expected, "__name__", str(self.expected))
        raise self.test_case.failureException("%s not triggered" % name)


def _safe_repr(obj, short=False):
    try:
        result = repr(obj)
    except Exception:
        result = object.__repr__(obj)
    if not short or len(result) < 80:
        return result
    return result[:80] + " [truncated]..."


def _count_diff_all_purpose(actual, expected):
    s, t = list(actual), list(expected)
    m, n = len(s), len(t)
    NULL = _marker
    result = []
    for i, elem in enumerate(s):
        if elem is NULL:
            continue
        cnt_s = cnt_t = 0
        for j in range(i, m):
            if s[j] == elem:
                cnt_s += 1
                s[j] = NULL
        for j, other_elem in enumerate(t):
            if other_elem == elem:
                cnt_t += 1
                t[j] = NULL
        if cnt_s != cnt_t:
            result.append((cnt_s, cnt_t, elem))
    for i, elem in enumerate(t):
        if elem is NULL:
            continue
        cnt_t = 0
        for j in range(i, n):
            if t[j] == elem:
                cnt_t += 1
                t[j] = NULL
        result.append((0, cnt_t, elem))
    return result


class TestCase:
    failureException = AssertionError
    longMessage = True
    maxDiff = 80 * 8

    def __init__(self, methodName="runTest"):
        self._testMethodName = methodName
        self._testMethodDoc = "No test"
        self._cleanups = []
        self._result = None
        self._subtest_failed = False
        self._type_equality_funcs = {}
        try:
            testMethod = getattr(self, methodName)
        except AttributeError:
            if methodName != "runTest":
                raise ValueError("no such test method in %s: %s" % (self.__class__, methodName))
        else:
            self._testMethodDoc = testMethod.__doc__

    # -- hooks ------------------------------------------------------------
    def setUp(self):
        pass

    def tearDown(self):
        pass

    @classmethod
    def setUpClass(cls):
        pass

    @classmethod
    def tearDownClass(cls):
        pass

    def countTestCases(self):
        return 1

    def defaultTestResult(self):
        return TestResult()

    def shortDescription(self):
        doc = self._testMethodDoc
        return doc.strip().split("\n")[0].strip() if doc else None

    def id(self):
        return "%s.%s.%s" % (self.__class__.__module__, self.__class__.__qualname__, self._testMethodName)

    def __eq__(self, other):
        if type(self) is not type(other):
            return NotImplemented
        return self._testMethodName == other._testMethodName

    def __hash__(self):
        return hash((type(self), self._testMethodName))

    def __str__(self):
        return "%s (%s.%s)" % (self._testMethodName, self.__class__.__module__, self.__class__.__qualname__ + "." + self._testMethodName)

    def __repr__(self):
        return "<%s.%s testMethod=%s>" % (self.__class__.__module__, self.__class__.__qualname__, self._testMethodName)

    def addCleanup(self, function, *args, **kwargs):
        self._cleanups.append((function, args, kwargs))

    def doCleanups(self):
        ok = True
        while self._cleanups:
            function, args, kwargs = self._cleanups.pop()
            try:
                function(*args, **kwargs)
            except BaseException as e:
                ok = False
                if self._result is not None:
                    self._result.addError(self, (type(e), e, None))
        return ok

    def addTypeEqualityFunc(self, typeobj, function):
        self._type_equality_funcs[typeobj] = function

    def subTest(self, msg=_marker, **params):
        return _SubTestContext(self, msg, params)

    # -- running ----------------------------------------------------------
    def run(self, result=None):
        if result is None:
            result = self.defaultTestResult()
        self._result = result
        result.startTest(self)
        testMethod = getattr(self, self._testMethodName)
        try:
            if getattr(self.__class__, "__unittest_skip__", False) or \
                    getattr(testMethod, "__unittest_skip__", False):
                why = getattr(self.__class__, "__unittest_skip_why__", "") or \
                    getattr(testMethod, "__unittest_skip_why__", "")
                result.addSkip(self, why)
                return result
            expecting_failure = getattr(testMethod, "__unittest_expecting_failure__", False) or \
                getattr(self.__class__, "__unittest_expecting_failure__", False)
            self._subtest_failed = False
            outcome = None
            ok = False
            try:
                self.setUp()
            except SkipTest as e:
                result.addSkip(self, str(e))
                self.doCleanups()
                return result
            except BaseException as e:
                result.addError(self, (type(e), e, None))
                self.doCleanups()
                return result
            try:
                testMethod()
                ok = True
            except SkipTest as e:
                result.addSkip(self, str(e))
                outcome = "skip"
            except self.failureException as e:
                if expecting_failure:
                    result.addExpectedFailure(self, (type(e), e, None))
                    outcome = "xfail"
                else:
                    result.addFailure(self, (type(e), e, None))
                    outcome = "fail"
            except BaseException as e:
                if expecting_failure:
                    result.addExpectedFailure(self, (type(e), e, None))
                    outcome = "xfail"
                else:
                    result.addError(self, (type(e), e, None))
                    outcome = "error"
            try:
                self.tearDown()
            except BaseException as e:
                result.addError(self, (type(e), e, None))
                ok = False
                outcome = "error"
            if not self.doCleanups():
                ok = False
            if ok and not self._subtest_failed:
                if expecting_failure:
                    result.addUnexpectedSuccess(self)
                else:
                    result.addSuccess(self)
            return result
        finally:
            result.stopTest(self)
            self._result = None

    def __call__(self, *args, **kwds):
        return self.run(*args, **kwds)

    def debug(self):
        self.setUp()
        getattr(self, self._testMethodName)()
        self.tearDown()
        while self._cleanups:
            function, args, kwargs = self._cleanups.pop()
            function(*args, **kwargs)

    # -- failing ----------------------------------------------------------
    def skipTest(self, reason):
        raise SkipTest(reason)

    def fail(self, msg=None):
        raise self.failureException(msg)

    def _formatMessage(self, msg, standardMsg):
        if not self.longMessage:
            return msg or standardMsg
        if msg is None:
            return standardMsg
        try:
            return "%s : %s" % (standardMsg, msg)
        except UnicodeDecodeError:
            return "%s : %s" % (_safe_repr(standardMsg), _safe_repr(msg))

    # -- simple asserts ---------------------------------------------------
    def assertFalse(self, expr, msg=None):
        if expr:
            msg = self._formatMessage(msg, "%s is not false" % _safe_repr(expr))
            raise self.failureException(msg)

    def assertTrue(self, expr, msg=None):
        if not expr:
            msg = self._formatMessage(msg, "%s is not true" % _safe_repr(expr))
            raise self.failureException(msg)

    def _getAssertEqualityFunc(self, first, second):
        if type(first) == type(second):
            asserter = self._type_equality_funcs.get(type(first))
            if asserter is not None:
                return asserter
            if isinstance(first, str):
                return self.assertMultiLineEqual
            if isinstance(first, dict):
                return self.assertDictEqual
            if isinstance(first, list):
                return self.assertListEqual
            if isinstance(first, tuple):
                return self.assertTupleEqual
            if isinstance(first, (set, frozenset)):
                return self.assertSetEqual
        return self._baseAssertEqual

    def _baseAssertEqual(self, first, second, msg=None):
        if not first == second:
            standardMsg = "%s != %s" % (_safe_repr(first), _safe_repr(second))
            msg = self._formatMessage(msg, standardMsg)
            raise self.failureException(msg)

    def assertEqual(self, first, second, msg=None):
        self._getAssertEqualityFunc(first, second)(first, second, msg)

    def assertNotEqual(self, first, second, msg=None):
        if not first != second:
            msg = self._formatMessage(msg, "%s == %s" % (_safe_repr(first), _safe_repr(second)))
            raise self.failureException(msg)

    def assertAlmostEqual(self, first, second, places=None, msg=None, delta=None):
        if first == second:
            return
        if delta is not None and places is not None:
            raise TypeError("specify delta or places not both")
        diff = abs(first - second)
        if delta is not None:
            if diff <= delta:
                return
            standardMsg = "%s != %s within %s delta (%s difference)" % (
                _safe_repr(first), _safe_repr(second), _safe_repr(delta), _safe_repr(diff))
        else:
            if places is None:
                places = 7
            if round(diff, places) == 0:
                return
            standardMsg = "%s != %s within %r places (%s difference)" % (
                _safe_repr(first), _safe_repr(second), places, _safe_repr(diff))
        raise self.failureException(self._formatMessage(msg, standardMsg))

    def assertNotAlmostEqual(self, first, second, places=None, msg=None, delta=None):
        if delta is not None and places is not None:
            raise TypeError("specify delta or places not both")
        diff = abs(first - second)
        if delta is not None:
            if not (first == second) and diff > delta:
                return
            standardMsg = "%s == %s within %s delta (%s difference)" % (
                _safe_repr(first), _safe_repr(second), _safe_repr(delta), _safe_repr(diff))
        else:
            if places is None:
                places = 7
            if not (first == second) and round(diff, places) != 0:
                return
            standardMsg = "%s == %s within %r places" % (_safe_repr(first), _safe_repr(second), places)
        raise self.failureException(self._formatMessage(msg, standardMsg))

    def assertGreater(self, a, b, msg=None):
        if not a > b:
            raise self.failureException(self._formatMessage(msg, "%s not greater than %s" % (_safe_repr(a), _safe_repr(b))))

    def assertGreaterEqual(self, a, b, msg=None):
        if not a >= b:
            raise self.failureException(self._formatMessage(msg, "%s not greater than or equal to %s" % (_safe_repr(a), _safe_repr(b))))

    def assertLess(self, a, b, msg=None):
        if not a < b:
            raise self.failureException(self._formatMessage(msg, "%s not less than %s" % (_safe_repr(a), _safe_repr(b))))

    def assertLessEqual(self, a, b, msg=None):
        if not a <= b:
            raise self.failureException(self._formatMessage(msg, "%s not less than or equal to %s" % (_safe_repr(a), _safe_repr(b))))

    def assertIs(self, expr1, expr2, msg=None):
        if expr1 is not expr2:
            raise self.failureException(self._formatMessage(msg, "%s is not %s" % (_safe_repr(expr1), _safe_repr(expr2))))

    def assertIsNot(self, expr1, expr2, msg=None):
        if expr1 is expr2:
            raise self.failureException(self._formatMessage(msg, "unexpectedly identical: %s" % (_safe_repr(expr1),)))

    def assertIsNone(self, obj, msg=None):
        if obj is not None:
            raise self.failureException(self._formatMessage(msg, "%s is not None" % (_safe_repr(obj),)))

    def assertIsNotNone(self, obj, msg=None):
        if obj is None:
            raise self.failureException(self._formatMessage(msg, "unexpectedly None"))

    def assertIn(self, member, container, msg=None):
        if member not in container:
            raise self.failureException(self._formatMessage(msg, "%s not found in %s" % (_safe_repr(member), _safe_repr(container))))

    def assertNotIn(self, member, container, msg=None):
        if member in container:
            raise self.failureException(self._formatMessage(msg, "%s unexpectedly found in %s" % (_safe_repr(member), _safe_repr(container))))

    def assertIsInstance(self, obj, cls, msg=None):
        if not isinstance(obj, cls):
            raise self.failureException(self._formatMessage(msg, "%s is not an instance of %r" % (_safe_repr(obj), cls)))

    def assertNotIsInstance(self, obj, cls, msg=None):
        if isinstance(obj, cls):
            raise self.failureException(self._formatMessage(msg, "%s is an instance of %r" % (_safe_repr(obj), cls)))

    def assertIsSubclass(self, cls, superclass, msg=None):
        if not issubclass(cls, superclass):
            raise self.failureException(self._formatMessage(msg, "%r is not a subclass of %r" % (cls, superclass)))

    def assertNotIsSubclass(self, cls, superclass, msg=None):
        if issubclass(cls, superclass):
            raise self.failureException(self._formatMessage(msg, "%r is a subclass of %r" % (cls, superclass)))

    # -- raises / warns ---------------------------------------------------
    def assertRaises(self, expected_exception, *args, **kwargs):
        context = _AssertRaisesContext(expected_exception, self)
        if not args:
            msg = kwargs.pop("msg", None)
            if kwargs:
                raise TypeError("%r is an invalid keyword argument for this function" % (next(iter(kwargs)),))
            return context
        callable_obj = args[0]
        context.obj_name = getattr(callable_obj, "__name__", str(callable_obj))
        with context:
            callable_obj(*args[1:], **kwargs)
        return None

    def assertRaisesRegex(self, expected_exception, expected_regex, *args, **kwargs):
        context = _AssertRaisesContext(expected_exception, self, expected_regex)
        if not args:
            kwargs.pop("msg", None)
            return context
        callable_obj = args[0]
        context.obj_name = getattr(callable_obj, "__name__", str(callable_obj))
        with context:
            callable_obj(*args[1:], **kwargs)
        return None

    def assertWarns(self, expected_warning, *args, **kwargs):
        context = _AssertWarnsContext(expected_warning, self)
        if not args:
            kwargs.pop("msg", None)
            return context
        with context:
            args[0](*args[1:], **kwargs)
        return None

    def assertWarnsRegex(self, expected_warning, expected_regex, *args, **kwargs):
        context = _AssertWarnsContext(expected_warning, self, expected_regex)
        if not args:
            kwargs.pop("msg", None)
            return context
        with context:
            args[0](*args[1:], **kwargs)
        return None

    # -- regex ------------------------------------------------------------
    def assertRegex(self, text, expected_regex, msg=None):
        if isinstance(expected_regex, (str, bytes)):
            expected_regex = re.compile(expected_regex)
        if not expected_regex.search(text):
            standardMsg = "Regex didn't match: %r not found in %r" % (expected_regex.pattern, text)
            raise self.failureException(self._formatMessage(msg, standardMsg))

    def assertNotRegex(self, text, unexpected_regex, msg=None):
        if isinstance(unexpected_regex, (str, bytes)):
            unexpected_regex = re.compile(unexpected_regex)
        match = unexpected_regex.search(text)
        if match:
            standardMsg = "Regex matched: %r matches %r in %r" % (
                text[match.start():match.end()], unexpected_regex.pattern, text)
            raise self.failureException(self._formatMessage(msg, standardMsg))

    # -- containers -------------------------------------------------------
    def assertCountEqual(self, first, second, msg=None):
        differences = []
        for cnt_s, cnt_t, elem in _count_diff_all_purpose(list(first), list(second)):
            differences.append("First has %d, Second has %d:  %r" % (cnt_s, cnt_t, elem))
        if not differences:
            return
        standardMsg = "Element counts were not equal:\n" + "\n".join(differences)
        raise self.failureException(self._formatMessage(msg, standardMsg))

    def _assertSequenceFirstDiff(self, seq1, seq2, seq_type, name):
        len1 = len(seq1)
        len2 = len(seq2)
        differing = "%ss differ: %s != %s\n" % (name.capitalize(), _safe_repr(seq1, True), _safe_repr(seq2, True))
        for i in range(min(len1, len2)):
            try:
                item1 = seq1[i]
                item2 = seq2[i]
            except (TypeError, IndexError, NotImplementedError):
                differing += "\nUnable to index element %d of %s\n" % (i, name)
                break
            if item1 != item2:
                differing += "\nFirst differing element %d:\n%s\n%s\n" % (i, _safe_repr(item1), _safe_repr(item2))
                break
        else:
            if len1 == len2:
                return None
        if len1 > len2:
            differing += "\nFirst %s contains %d additional elements.\n" % (name, len1 - len2)
            try:
                differing += "First extra element %d:\n%s\n" % (len2, _safe_repr(seq1[len2]))
            except (TypeError, IndexError, NotImplementedError):
                pass
        elif len1 < len2:
            differing += "\nSecond %s contains %d additional elements.\n" % (name, len2 - len1)
            try:
                differing += "First extra element %d:\n%s\n" % (len1, _safe_repr(seq2[len1]))
            except (TypeError, IndexError, NotImplementedError):
                pass
        return differing

    def assertSequenceEqual(self, seq1, seq2, msg=None, seq_type=None):
        if seq_type is not None:
            if not isinstance(seq1, seq_type):
                raise self.failureException("First sequence is not a %s: %s" % (seq_type.__name__, _safe_repr(seq1)))
            if not isinstance(seq2, seq_type):
                raise self.failureException("Second sequence is not a %s: %s" % (seq_type.__name__, _safe_repr(seq2)))
            name = seq_type.__name__
        else:
            name = "sequence"
        if seq1 == seq2:
            return
        differing = self._assertSequenceFirstDiff(seq1, seq2, seq_type, name)
        if differing is None:
            differing = "%ss differ: %s != %s\n" % (name.capitalize(), _safe_repr(seq1, True), _safe_repr(seq2, True))
        raise self.failureException(self._formatMessage(msg, differing + "\n- %s\n+ %s" % (_safe_repr(seq1), _safe_repr(seq2))))

    def assertListEqual(self, list1, list2, msg=None):
        self.assertSequenceEqual(list1, list2, msg, seq_type=list)

    def assertTupleEqual(self, tuple1, tuple2, msg=None):
        self.assertSequenceEqual(tuple1, tuple2, msg, seq_type=tuple)

    def assertSetEqual(self, set1, set2, msg=None):
        try:
            difference1 = set1.difference(set2)
        except TypeError as e:
            raise self.failureException("invalid type when attempting set difference: %s" % e)
        try:
            difference2 = set2.difference(set1)
        except TypeError as e:
            raise self.failureException("invalid type when attempting set difference: %s" % e)
        if not (difference1 or difference2):
            return
        lines = []
        if difference1:
            lines.append("Items in the first set but not the second:")
            for item in difference1:
                lines.append(repr(item))
        if difference2:
            lines.append("Items in the second set but not the first:")
            for item in difference2:
                lines.append(repr(item))
        raise self.failureException(self._formatMessage(msg, "\n".join(lines)))

    def assertDictEqual(self, d1, d2, msg=None):
        self.assertIsInstance(d1, dict, "First argument is not a dictionary")
        self.assertIsInstance(d2, dict, "Second argument is not a dictionary")
        if d1 != d2:
            standardMsg = "%s != %s\n- %s\n+ %s" % (_safe_repr(d1, True), _safe_repr(d2, True),
                                                    _safe_repr(d1), _safe_repr(d2))
            raise self.failureException(self._formatMessage(msg, standardMsg))

    def assertMultiLineEqual(self, first, second, msg=None):
        self.assertIsInstance(first, str, "First argument is not a string")
        self.assertIsInstance(second, str, "Second argument is not a string")
        if first != second:
            standardMsg = "%s != %s\n- %s\n+ %s" % (_safe_repr(first, True), _safe_repr(second, True),
                                                    first, second)
            raise self.failureException(self._formatMessage(msg, standardMsg))

    def assertLogs(self, logger=None, level=None):
        raise NotImplementedError("assertLogs is not supported")

    # -- deprecated aliases -------------------------------------------------
    assertEquals = assertEqual
    assertNotEquals = assertNotEqual
    assert_ = assertTrue
    failUnlessEqual = assertEqual
    failUnless = assertTrue
    assertRegexpMatches = assertRegex
    assertRaisesRegexp = assertRaisesRegex
    assertAlmostEquals = assertAlmostEqual


class IsolatedAsyncioTestCase(TestCase):
    pass


class FunctionTestCase(TestCase):
    def __init__(self, testFunc, setUp=None, tearDown=None, description=None):
        TestCase.__init__(self)
        self._testFunc = testFunc
        self._setUpFunc = setUp
        self._tearDownFunc = tearDown
        self._description = description

    def setUp(self):
        if self._setUpFunc is not None:
            self._setUpFunc()

    def tearDown(self):
        if self._tearDownFunc is not None:
            self._tearDownFunc()

    def runTest(self):
        self._testFunc()

    def id(self):
        return self._testFunc.__name__

    def __str__(self):
        return "%s (%s)" % (self.__class__.__name__, self._testFunc.__name__)

    def shortDescription(self):
        if self._description is not None:
            return self._description
        doc = self._testFunc.__doc__
        return doc.split("\n")[0].strip() if doc else None


# -- suites ---------------------------------------------------------------

class TestSuite:
    def __init__(self, tests=()):
        self._tests = []
        self._removed_tests = 0
        self.addTests(tests)

    def __repr__(self):
        return "<%s tests=%s>" % (self.__class__.__name__, list(self._tests))

    def __iter__(self):
        return iter(self._tests)

    def countTestCases(self):
        cases = self._removed_tests
        for test in self:
            if test:
                cases += test.countTestCases()
        return cases

    def addTest(self, test):
        if not callable(test):
            raise TypeError("%r is not callable" % (test,))
        if isinstance(test, type) and issubclass(test, (TestCase, TestSuite)):
            raise TypeError("TestCases and TestSuites must be instantiated before passing them to addTest()")
        self._tests.append(test)

    def addTests(self, tests):
        if isinstance(tests, str):
            raise TypeError("tests must be an iterable of tests, not a string")
        for test in tests:
            self.addTest(test)

    def run(self, result, debug=False):
        topLevel = False
        if getattr(result, "_testRunEntered", False) is False:
            result._testRunEntered = topLevel = True
        for index, test in enumerate(self):
            if result.shouldStop:
                break
            if isinstance(test, TestSuite):
                test.run(result)
            else:
                self._handleClassSetUp(test, result)
                if getattr(result, "_classSetupFailed", False):
                    self._tearDownPrevious(test, result)
                    continue
                test(result)
        if topLevel:
            self._tearDownPreviousClass(None, result)
            result._testRunEntered = False
        return result

    def __call__(self, *args, **kwds):
        return self.run(*args, **kwds)

    def debug(self):
        for test in self:
            test.debug()

    def _handleClassSetUp(self, test, result):
        previousClass = getattr(result, "_previousTestClass", None)
        currentClass = test.__class__
        if currentClass == previousClass:
            return
        self._tearDownPreviousClass(test, result)
        result._previousTestClass = currentClass
        result._classSetupFailed = False
        if getattr(currentClass, "__unittest_skip__", False):
            return
        setUpClass = getattr(currentClass, "setUpClass", None)
        if setUpClass is not None:
            try:
                setUpClass()
            except BaseException as e:
                result._classSetupFailed = True
                if isinstance(e, SkipTest):
                    return
                result.addError(_ErrorHolder("setUpClass (%s.%s)" % (
                    currentClass.__module__, currentClass.__qualname__)), (type(e), e, None))

    def _tearDownPrevious(self, test, result):
        pass

    def _tearDownPreviousClass(self, test, result):
        previousClass = getattr(result, "_previousTestClass", None)
        currentClass = test.__class__ if test is not None else None
        if currentClass == previousClass or previousClass is None:
            return
        if getattr(result, "_classSetupFailed", False):
            return
        if getattr(previousClass, "__unittest_skip__", False):
            return
        tearDownClass = getattr(previousClass, "tearDownClass", None)
        if tearDownClass is not None:
            try:
                tearDownClass()
            except BaseException as e:
                result.addError(_ErrorHolder("tearDownClass (%s.%s)" % (
                    previousClass.__module__, previousClass.__qualname__)), (type(e), e, None))
        result._previousTestClass = None


class _ErrorHolder:
    failureException = None

    def __init__(self, description):
        self.description = description

    def id(self):
        return self.description

    def shortDescription(self):
        return None

    def __repr__(self):
        return "<ErrorHolder description=%r>" % (self.description,)

    def __str__(self):
        return self.id()

    def run(self, result):
        pass

    def __call__(self, result):
        return self.run(result)

    def countTestCases(self):
        return 0


# -- loader ---------------------------------------------------------------

class TestLoader:
    testMethodPrefix = "test"
    sortTestMethodsUsing = None
    suiteClass = TestSuite
    testNamePatterns = None

    def loadTestsFromTestCase(self, testCaseClass):
        if issubclass(testCaseClass, TestSuite):
            raise TypeError("Test cases should not be derived from TestSuite. Maybe you meant to derive from TestCase?")
        names = self.getTestCaseNames(testCaseClass)
        if not names and hasattr(testCaseClass, "runTest"):
            names = ["runTest"]
        return self.suiteClass([testCaseClass(name) for name in names])

    def loadTestsFromModule(self, module, *args, pattern=None, **kws):
        tests = []
        for name in dir(module):
            obj = getattr(module, name)
            if isinstance(obj, type) and issubclass(obj, TestCase):
                tests.append(self.loadTestsFromTestCase(obj))
        load_tests = getattr(module, "load_tests", None)
        tests = self.suiteClass(tests)
        if load_tests is not None:
            return load_tests(self, tests, pattern)
        return tests

    def loadTestsFromName(self, name, module=None):
        if module is None:
            raise ValueError("loadTestsFromName needs a module object: dotted names are resolved at run time")
        obj = module
        for part in name.split("."):
            obj = getattr(obj, part)
        if isinstance(obj, type) and issubclass(obj, TestCase):
            return self.loadTestsFromTestCase(obj)
        if isinstance(obj, TestSuite):
            return obj
        if callable(obj):
            parent = None
            parts = name.split(".")
            if len(parts) > 1:
                holder = module
                for part in parts[:-1]:
                    holder = getattr(holder, part)
                if isinstance(holder, type) and issubclass(holder, TestCase):
                    return self.suiteClass([holder(parts[-1])])
            return self.suiteClass([FunctionTestCase(obj)])
        raise TypeError("don't know how to make test from: %s" % obj)

    def loadTestsFromNames(self, names, module=None):
        return self.suiteClass([self.loadTestsFromName(n, module) for n in names])

    def getTestCaseNames(self, testCaseClass):
        names = []
        for attrname in dir(testCaseClass):
            if not attrname.startswith(self.testMethodPrefix):
                continue
            attr = getattr(testCaseClass, attrname)
            if not callable(attr):
                continue
            if self.testNamePatterns is not None:
                ok = False
                for pat in self.testNamePatterns:
                    if "*" in pat:
                        if _fnmatch(attrname, pat):
                            ok = True
                    elif pat in attrname:
                        ok = True
                if not ok:
                    continue
            names.append(attrname)
        if self.sortTestMethodsUsing is not None:
            names.sort(key=_CmpToKey(self.sortTestMethodsUsing))
        return names


def _fnmatch(name, pat):
    import fnmatch
    return fnmatch.fnmatchcase(name, pat)


class _CmpToKey:
    def __init__(self, cmp):
        self.cmp = cmp

    def __call__(self, obj):
        cmp = self.cmp

        class K:
            def __init__(self, o):
                self.o = o

            def __lt__(self, other):
                return cmp(self.o, other.o) < 0
        return K(obj)


defaultTestLoader = TestLoader()


def findTestCases(module, prefix="test", sortUsing=None, suiteClass=TestSuite):
    loader = TestLoader()
    loader.testMethodPrefix = prefix
    loader.suiteClass = suiteClass
    return loader.loadTestsFromModule(module)


# -- results ----------------------------------------------------------------

class TestResult:
    def __init__(self, stream=None, descriptions=None, verbosity=None):
        self.failfast = False
        self.failures = []
        self.errors = []
        self.testsRun = 0
        self.skipped = []
        self.expectedFailures = []
        self.unexpectedSuccesses = []
        self.shouldStop = False
        self.buffer = False
        self.tb_locals = False
        self._testRunEntered = False
        self._previousTestClass = None
        self._classSetupFailed = False

    def printErrors(self):
        pass

    def startTest(self, test):
        self.testsRun += 1

    def startTestRun(self):
        pass

    def stopTestRun(self):
        pass

    def stopTest(self, test):
        pass

    def _exc_info_to_string(self, err, test):
        exctype, value, tb = err
        name = exctype.__name__
        text = str(value)
        if text:
            line = "%s: %s" % (name, text)
        else:
            line = name
        if exctype.__module__ not in ("builtins", "__main__") and exctype is not AssertionError:
            line = "%s.%s" % (exctype.__module__, line) if "." not in name else line
        return "Traceback (most recent call last):\n" + line + "\n"

    def addError(self, test, err):
        self.errors.append((test, self._exc_info_to_string(err, test)))
        if self.failfast:
            self.stop()

    def addFailure(self, test, err):
        self.failures.append((test, self._exc_info_to_string(err, test)))
        if self.failfast:
            self.stop()

    def addSubTest(self, test, subtest, err):
        if err is not None:
            if issubclass(err[0], test.failureException):
                self.failures.append((subtest, self._exc_info_to_string(err, test)))
            else:
                self.errors.append((subtest, self._exc_info_to_string(err, test)))
            if self.failfast:
                self.stop()

    def addSuccess(self, test):
        pass

    def addSkip(self, test, reason):
        self.skipped.append((test, reason))

    def addExpectedFailure(self, test, err):
        self.expectedFailures.append((test, self._exc_info_to_string(err, test)))

    def addUnexpectedSuccess(self, test):
        self.unexpectedSuccesses.append(test)

    def wasSuccessful(self):
        return len(self.failures) == len(self.errors) == 0 and (
            not hasattr(self, "unexpectedSuccesses") or len(self.unexpectedSuccesses) == 0)

    def stop(self):
        self.shouldStop = True

    def __repr__(self):
        return "<%s run=%i errors=%i failures=%i>" % (
            self.__class__.__name__, self.testsRun, len(self.errors), len(self.failures))


class _WritelnDecorator:
    def __init__(self, stream):
        self.stream = stream

    def __getattr__(self, attr):
        return getattr(self.stream, attr)

    def writeln(self, arg=None):
        if arg:
            self.write(arg)
        self.write("\n")


class TextTestResult(TestResult):
    separator1 = "=" * 70
    separator2 = "-" * 70

    def __init__(self, stream, descriptions, verbosity):
        TestResult.__init__(self, stream, descriptions, verbosity)
        self.stream = stream
        self.showAll = verbosity > 1
        self.dots = verbosity == 1
        self.descriptions = descriptions

    def getDescription(self, test):
        doc_first_line = test.shortDescription()
        if self.descriptions and doc_first_line:
            return "\n".join((str(test), doc_first_line))
        return str(test)

    def startTest(self, test):
        TestResult.startTest(self, test)
        if self.showAll:
            self.stream.write(self.getDescription(test))
            self.stream.write(" ... ")
            self.stream.flush()

    def addSuccess(self, test):
        TestResult.addSuccess(self, test)
        if self.showAll:
            self.stream.writeln("ok")
        elif self.dots:
            self.stream.write(".")
            self.stream.flush()

    def addError(self, test, err):
        TestResult.addError(self, test, err)
        if self.showAll:
            self.stream.writeln("ERROR")
        elif self.dots:
            self.stream.write("E")
            self.stream.flush()

    def addFailure(self, test, err):
        TestResult.addFailure(self, test, err)
        if self.showAll:
            self.stream.writeln("FAIL")
        elif self.dots:
            self.stream.write("F")
            self.stream.flush()

    def addSubTest(self, test, subtest, err):
        if err is not None:
            if self.showAll:
                if issubclass(err[0], subtest.test_case.failureException):
                    self.stream.writeln("FAIL")
                else:
                    self.stream.writeln("ERROR")
            elif self.dots:
                if issubclass(err[0], subtest.test_case.failureException):
                    self.stream.write("F")
                else:
                    self.stream.write("E")
                self.stream.flush()
        TestResult.addSubTest(self, test, subtest, err)

    def addSkip(self, test, reason):
        TestResult.addSkip(self, test, reason)
        if self.showAll:
            self.stream.writeln("skipped {0!r}".format(reason))
        elif self.dots:
            self.stream.write("s")
            self.stream.flush()

    def addExpectedFailure(self, test, err):
        TestResult.addExpectedFailure(self, test, err)
        if self.showAll:
            self.stream.writeln("expected failure")
        elif self.dots:
            self.stream.write("x")
            self.stream.flush()

    def addUnexpectedSuccess(self, test):
        TestResult.addUnexpectedSuccess(self, test)
        if self.showAll:
            self.stream.writeln("unexpected success")
        elif self.dots:
            self.stream.write("u")
            self.stream.flush()

    def printErrors(self):
        if self.dots or self.showAll:
            self.stream.writeln()
            self.stream.flush()
        self.printErrorList("ERROR", self.errors)
        self.printErrorList("FAIL", self.failures)
        unexpectedSuccesses = self.unexpectedSuccesses
        if unexpectedSuccesses:
            self.stream.writeln(self.separator1)
            for test in unexpectedSuccesses:
                self.stream.writeln("UNEXPECTED SUCCESS: %s" % self.getDescription(test))
            self.stream.flush()

    def printErrorList(self, flavour, errors):
        for test, err in errors:
            self.stream.writeln(self.separator1)
            self.stream.writeln("%s: %s" % (flavour, self.getDescription(test)))
            self.stream.writeln(self.separator2)
            self.stream.writeln("%s" % err)
            self.stream.flush()


class TextTestRunner:
    resultclass = TextTestResult

    def __init__(self, stream=None, descriptions=True, verbosity=1, failfast=False, buffer=False,
                 resultclass=None, warnings=None, *, tb_locals=False):
        if stream is None:
            stream = sys.stderr
        self.stream = _WritelnDecorator(stream)
        self.descriptions = descriptions
        self.verbosity = verbosity
        self.failfast = failfast
        self.buffer = buffer
        self.tb_locals = tb_locals
        self.warnings = warnings
        if resultclass is not None:
            self.resultclass = resultclass

    def _makeResult(self):
        return self.resultclass(self.stream, self.descriptions, self.verbosity)

    def run(self, test):
        result = self._makeResult()
        result.failfast = self.failfast
        result.buffer = self.buffer
        result.tb_locals = self.tb_locals
        startTime = time.perf_counter()
        result.startTestRun()
        try:
            test(result)
        finally:
            result.stopTestRun()
        stopTime = time.perf_counter()
        timeTaken = stopTime - startTime
        result.printErrors()
        if hasattr(result, "separator2"):
            self.stream.writeln(result.separator2)
        run = result.testsRun
        self.stream.writeln("Ran %d test%s in %.3fs" % (run, run != 1 and "s" or "", timeTaken))
        self.stream.writeln()
        expectedFails = len(result.expectedFailures)
        unexpectedSuccesses = len(result.unexpectedSuccesses)
        skipped = len(result.skipped)
        infos = []
        if not result.wasSuccessful():
            self.stream.write("FAILED")
            failed = len(result.failures)
            errored = len(result.errors)
            if failed:
                infos.append("failures=%d" % failed)
            if errored:
                infos.append("errors=%d" % errored)
        else:
            self.stream.write("OK")
        if skipped:
            infos.append("skipped=%d" % skipped)
        if expectedFails:
            infos.append("expected failures=%d" % expectedFails)
        if unexpectedSuccesses:
            infos.append("unexpected successes=%d" % unexpectedSuccesses)
        if infos:
            self.stream.writeln(" (%s)" % (", ".join(infos),))
        else:
            self.stream.write("\n")
        self.stream.flush()
        return result


class TestProgram:
    def __init__(self, module="__main__", defaultTest=None, argv=None, testRunner=None,
                 testLoader=defaultTestLoader, exit=True, verbosity=1, failfast=None,
                 catchbreak=None, buffer=None, warnings=None, *, tb_locals=False):
        if module is None or isinstance(module, str):
            module = sys.modules[module or "__main__"]
        self.module = module
        self.exit = exit
        self.verbosity = verbosity
        self.failfast = failfast
        self.testRunner = testRunner
        self.testLoader = testLoader
        if argv is None:
            argv = sys.argv
        self.progName = argv[0] if argv else "python -m unittest"
        names = []
        for arg in argv[1:]:
            if arg == "-v" or arg == "--verbose":
                self.verbosity = 2
            elif arg == "-q" or arg == "--quiet":
                self.verbosity = 0
            elif arg == "-f" or arg == "--failfast":
                self.failfast = True
            elif not arg.startswith("-"):
                names.append(arg)
        if defaultTest is not None and not names:
            names = [defaultTest] if isinstance(defaultTest, str) else list(defaultTest)
        if names:
            self.test = self.testLoader.loadTestsFromNames(names, self.module)
        else:
            self.test = self.testLoader.loadTestsFromModule(self.module)
        self.runTests()

    def runTests(self):
        testRunner = self.testRunner
        if testRunner is None:
            testRunner = TextTestRunner
        if isinstance(testRunner, type):
            kw = {"verbosity": self.verbosity}
            if self.failfast is not None:
                kw["failfast"] = self.failfast
            testRunner = testRunner(**kw)
        self.result = testRunner.run(self.test)
        if self.exit:
            sys.exit(0 if self.result.wasSuccessful() else 1)


main = TestProgram
