#!/usr/bin/env python3
"""Tests for cn1ss-golden-plan.py. The real tree must report nothing: its
golden set is complete, so any hit there is a convention the detector misses."""
import importlib.util
import os
import tempfile
import unittest

HERE = os.path.dirname(os.path.abspath(__file__))
spec = importlib.util.spec_from_file_location("plan", os.path.join(HERE, "cn1ss-golden-plan.py"))
plan = importlib.util.module_from_spec(spec)
spec.loader.exec_module(plan)

RUNNER = """class Cn1ssDeviceRunner {
  static final BaseTest[] DEFAULT_TEST_CLASSES = new BaseTest[]{
    new FooScreenshotTest(), new BarTest(), new AssertOnlyTest(), new OptOutScreenshotTest(),
    new MaybeScreenshotTest(), new ChildTest()
  };
}"""
SOURCES = {
    "FooScreenshotTest.java": "class FooScreenshotTest extends BaseTest {}",
    "BarTest.java": 'class BarTest extends BaseTest { void r(){ createForm("t", null, "bar-image"); } }',
    "AssertOnlyTest.java": "class AssertOnlyTest extends BaseTest { void r(){ assertTrue(true); } }",
    "OptOutScreenshotTest.java": "class OptOutScreenshotTest extends BaseTest {"
                                 " public boolean shouldTakeScreenshot() { return false; } }",
    "MaybeScreenshotTest.java": "class MaybeScreenshotTest extends BaseTest {"
                                " public boolean shouldTakeScreenshot() { return !isHtml5(); } }",
    "ParentForm.java": "abstract class ParentForm extends BaseTest { void r(){ emitCurrentFormScreenshot(name()); } }",
    "ChildTest.java": "class ChildTest extends ParentForm {}",
    "App.kt": "fun main() { Cn1ssDeviceRunner.addTest(KotlinUiTest()) }",
    "KotlinUiTest.kt": 'class KotlinUiTest : BaseTest() { fun r() { createForm("k", null, "kotlin") } }',
}


class GoldenPlanTest(unittest.TestCase):
    def run_plan(self, goldens):
        with tempfile.TemporaryDirectory() as root:
            src = os.path.join(root, "src")
            os.makedirs(src)
            for name, body in SOURCES.items():
                with open(os.path.join(src, name), "w") as fh:
                    fh.write(body)
            runner = os.path.join(src, "Cn1ssDeviceRunner.java")
            with open(runner, "w") as fh:
                fh.write(RUNNER)
            for d, files in goldens.items():
                os.makedirs(os.path.join(root, d))
                for f in files:
                    open(os.path.join(root, d, f + ".png"), "w").close()
            return plan.missing(root=root, sources_dir=src, runner=runner, dirs=list(goldens))

    def test_nothing_captured_lists_every_screenshot_test(self):
        self.assertEqual(self.run_plan({"a": ["unrelated"]}),
                         ["FooScreenshotTest", "BarTest", "MaybeScreenshotTest", "ChildTest", "KotlinUiTest"])

    def test_suffixes_literals_and_any_directory_cover(self):
        self.assertEqual(self.run_plan({"a": ["Foo_dark", "bar-image"], "b": ["MaybeScreenshotTest", "Child", "kotlin"]}), [])

    def test_prefix_needs_a_separator(self):
        self.assertIn("FooScreenshotTest", self.run_plan({"a": ["Foobar"]}))

    def test_empty_listing_is_an_error(self):
        with self.assertRaises(SystemExit):
            self.run_plan({"a": []})

    def test_current_tree_is_complete(self):
        self.assertEqual(plan.missing(), [])
        self.assertEqual(plan.missing(from_git=True), [])


if __name__ == "__main__":
    unittest.main()
