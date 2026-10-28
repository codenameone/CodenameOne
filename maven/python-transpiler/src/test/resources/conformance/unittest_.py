import io
import re
import sys
import unittest


class Basics(unittest.TestCase):
    log = []

    @classmethod
    def setUpClass(cls):
        Basics.log.append("setUpClass")

    @classmethod
    def tearDownClass(cls):
        Basics.log.append("tearDownClass")

    def setUp(self):
        self.value = 10
        Basics.log.append("setUp")

    def tearDown(self):
        Basics.log.append("tearDown")

    def test_equal(self):
        self.assertEqual(self.value, 10)
        self.assertNotEqual(self.value, 11)
        self.assertTrue(self.value)
        self.assertFalse(0)
        self.assertIs(None, None)
        self.assertIsNot([], [])
        self.assertIsNone(None)
        self.assertIsNotNone(0)
        self.assertIn(1, [1, 2])
        self.assertNotIn(3, [1, 2])
        self.assertIsInstance(1, int)
        self.assertNotIsInstance("a", int)
        self.assertGreater(2, 1)
        self.assertGreaterEqual(2, 2)
        self.assertLess(1, 2)
        self.assertLessEqual(2, 2)
        self.assertAlmostEqual(1.00000001, 1.0)
        self.assertAlmostEqual(1.5, 1.4, delta=0.2)
        self.assertNotAlmostEqual(1.5, 1.0)
        self.assertCountEqual([1, 2, 2, 3], [3, 2, 1, 2])
        self.assertDictEqual({"a": 1}, {"a": 1})
        self.assertListEqual([1, 2], [1, 2])
        self.assertTupleEqual((1, 2), (1, 2))
        self.assertSetEqual({1, 2}, {2, 1})
        self.assertMultiLineEqual("a\nb", "a\nb")
        self.assertRegex("hello world", r"w.r")
        self.assertNotRegex("hello", r"xyz")

    def test_raises(self):
        with self.assertRaises(ZeroDivisionError) as cm:
            1 / 0
        self.assertIsInstance(cm.exception, ZeroDivisionError)
        self.assertRaises(KeyError, lambda: {}["x"])
        self.assertRaises(ValueError, int, "zz")
        with self.assertRaisesRegex(ValueError, "invalid literal"):
            int("zz")
        self.assertRaisesRegex(ValueError, "^invalid", int, "q")

    def test_subtests(self):
        for i in range(3):
            with self.subTest(i=i):
                self.assertLess(i, 5)

    @unittest.skip("not now")
    def test_skipped(self):
        self.fail("must not run")

    @unittest.skipIf(True, "cond")
    def test_skipped_if(self):
        self.fail("must not run")

    @unittest.skipUnless(False, "cond2")
    def test_skipped_unless(self):
        self.fail("must not run")

    def test_skip_inside(self):
        self.skipTest("inside")

    @unittest.expectedFailure
    def test_xfail(self):
        self.assertEqual(1, 2)

    def helper_not_a_test(self):
        pass


class Failing(unittest.TestCase):
    def test_a_equal(self):
        self.assertEqual(1, 2)

    def test_b_msg(self):
        self.assertEqual("a", "b", "custom")

    def test_c_true(self):
        self.assertTrue(0)

    def test_d_in(self):
        self.assertIn(1, [2])

    def test_e_list(self):
        self.assertEqual([1, 2, 3], [1, 2, 4])

    def test_f_error(self):
        raise KeyError("boom")

    def test_g_raises(self):
        with self.assertRaises(ValueError):
            pass

    def test_h_regex(self):
        with self.assertRaisesRegex(ValueError, "abc"):
            raise ValueError("xyz")

    def test_i_almost(self):
        self.assertAlmostEqual(1.0, 1.5)

    def test_j_less(self):
        self.assertLess(2, 1)

    def test_k_count(self):
        self.assertCountEqual([1, 2], [1, 3])

    def test_l_isnone(self):
        self.assertIsNone(5)

    def test_m_sub(self):
        for i in range(3):
            with self.subTest(i=i):
                self.assertEqual(i % 2, 0)

    def test_n_set(self):
        self.assertSetEqual({1}, {2})

    @unittest.expectedFailure
    def test_o_unexpected(self):
        pass


class Teardown(unittest.TestCase):
    order = []

    def setUp(self):
        self.addCleanup(Teardown.order.append, "cleanup")

    def test_one(self):
        Teardown.order.append("test")

    def tearDown(self):
        Teardown.order.append("tearDown")


def first_line(result_text):
    return result_text


def run(suite, verbosity=1):
    stream = io.StringIO()
    result = unittest.TextTestRunner(stream=stream, verbosity=verbosity).run(suite)
    out = stream.getvalue()
    out = re.sub(r"in \d+\.\d+s", "in X.XXXs", out)
    return result, out


loader = unittest.TestLoader()
print(loader.getTestCaseNames(Basics))
suite = loader.loadTestsFromTestCase(Basics)
print(suite.countTestCases())
result, out = run(suite)
print(out)
print(result.testsRun, len(result.failures), len(result.errors), len(result.skipped),
      len(result.expectedFailures), len(result.unexpectedSuccesses), result.wasSuccessful())
print(Basics.log[0], Basics.log[-1], Basics.log.count("setUp"), Basics.log.count("tearDown"))
print(sorted(r for _, r in result.skipped))

result, out = run(loader.loadTestsFromTestCase(Failing))
print(result.testsRun, len(result.failures), len(result.errors), result.wasSuccessful())
print(out.splitlines()[0])
print(sorted(type(t).__name__ for t, _ in result.failures + result.errors).count("_SubTest"))


def trim(text):
    # The difflib-style diff after the first line block is not supported.
    return re.split(r"\n[-+] ", text)[0]


def message(name, *args):
    case = Failing(name)
    try:
        getattr(case, name)()
    except Exception as e:
        return type(e).__name__ + ": " + trim(str(e))
    return "no exception"


for name in sorted(loader.getTestCaseNames(Failing)):
    if name != "test_m_sub" and name != "test_o_unexpected":
        print(name, "|", message(name))
c = Failing("test_a_equal")
for call in [lambda: c.assertEqual([1, 2, 3], [1, 2]),
             lambda: c.assertEqual({"a": 1}, {"a": 2}),
             lambda: c.assertEqual((1, 2), (1, 3)),
             lambda: c.assertNotEqual(1, 1),
             lambda: c.assertIs(1, 2),
             lambda: c.assertIsNot(None, None),
             lambda: c.assertIsNotNone(None),
             lambda: c.assertNotIn(1, [1]),
             lambda: c.assertIsInstance(1, str),
             lambda: c.assertNotIsInstance(1, int),
             lambda: c.assertGreater(1, 2),
             lambda: c.assertGreaterEqual(1, 2),
             lambda: c.assertLessEqual(2, 1),
             lambda: c.assertFalse(1),
             lambda: c.assertRegex("abc", "x"),
             lambda: c.assertNotRegex("abc", "b"),
             lambda: c.assertNotAlmostEqual(1.0, 1.0),
             lambda: c.assertAlmostEqual(1.0, 1.5, delta=0.1),
             lambda: c.assertEqual(1, 2, msg="why"),
             lambda: c.fail("explicit"),
             lambda: c.assertRaises(ValueError, lambda: None)]:
    try:
        call()
    except AssertionError as e:
        print(repr(trim(str(e))))
print(out.rstrip().splitlines()[-1])

result, out = run(loader.loadTestsFromTestCase(Teardown))
print(Teardown.order, result.wasSuccessful())

# verbose output
result, out = run(loader.loadTestsFromTestCase(Teardown), 2)
print(out.splitlines()[0])

# module loading, suite composition
mod = sys.modules["__main__"]
names = [type(t).__name__ for s in loader.loadTestsFromModule(mod) for t in s]
print(sorted(set(names)))
combined = unittest.TestSuite([loader.loadTestsFromTestCase(Teardown), unittest.TestSuite()])
combined.addTest(Teardown("test_one"))
print(combined.countTestCases())

def plain():
    assert 1 + 1 == 2

result, out = run(unittest.TestSuite([unittest.FunctionTestCase(plain)]))
print(result.testsRun, result.wasSuccessful())

# main() with exit=False
stream = io.StringIO()
prog = unittest.main(module=mod, argv=["x", "Teardown"], exit=False,
                     testRunner=unittest.TextTestRunner(stream=stream))
print(prog.result.testsRun, prog.result.wasSuccessful())
try:
    unittest.main(module=mod, argv=["x", "Teardown"],
                  testRunner=unittest.TextTestRunner(stream=io.StringIO()))
except SystemExit as e:
    print("exit", e.code)
