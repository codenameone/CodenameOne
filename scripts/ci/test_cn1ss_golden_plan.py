#!/usr/bin/env python3
"""Tests for cn1ss-golden-plan.py and golden-capture-collect.py.

The real tree must report no gap and no contradiction: its golden set is
complete, so any hit there is a convention or declaration the detector gets
wrong."""
import importlib.util
import json
import os
import tempfile
import unittest

HERE = os.path.dirname(os.path.abspath(__file__))


def load(name, file):
    spec = importlib.util.spec_from_file_location(name, os.path.join(HERE, file))
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    return mod


plan = load("plan", "cn1ss-golden-plan.py")
col = load("collect", "golden-capture-collect.py")

RUNNER = """class Cn1ssDeviceRunner {
  static final BaseTest[] DEFAULT_TEST_CLASSES = new BaseTest[]{
    new FooScreenshotTest(), new BarTest(), new AssertOnlyTest(), new OptOutScreenshotTest(),
    new WatchSkipScreenshotTest(), new ChildTest(), new MappedTest()
  };
}"""
BASE_SOURCES = {
    "FooScreenshotTest.java": "class FooScreenshotTest extends BaseTest {}",
    "BarTest.java": 'class BarTest extends BaseTest { void r(){ createForm("t", null, "bar-image"); } }',
    "AssertOnlyTest.java": "class AssertOnlyTest extends BaseTest { void r(){ assertTrue(true); } }",
    "OptOutScreenshotTest.java": "class OptOutScreenshotTest extends BaseTest {"
                                 " public boolean shouldTakeScreenshot() { return false; } }",
    "WatchSkipScreenshotTest.java": "class WatchSkipScreenshotTest extends BaseTest {\n"
                                    "  // cn1ss-skip: watchos -- why\n"
                                    '  boolean runTest() { if (CN.isWatch()) { log("test=W status=SKIPPED"); done(); return true; } return true; } }',
    "ParentForm.java": "abstract class ParentForm extends BaseTest {\n  // cn1ss-skip: watchos\n"
                       '  boolean runTest() { if (!Desktop.isSupported()) { done(); return true; }'
                       ' emitCurrentFormScreenshot("parent-shot"); return true; } }',
    "ChildTest.java": "class ChildTest extends ParentForm {}",
    "MappedTest.java": "class MappedTest extends BaseTest { void r(){ emitImage(i, n); } }",
    "App.kt": "fun main() { Cn1ssDeviceRunner.addTest(KotlinUiTest()) }",
    "KotlinUiTest.kt": 'class KotlinUiTest : BaseTest() { fun r() { createForm("k", null, "kotlin") } }',
}
DIRS = {"g/phone": {"ios-metal"}, "g/watch": {"watchos"}, "g/win": {"windows-x64", "windows-arm64"}}
MAPPINGS = [{"pattern": "weird-*", "test": "MappedTest"},
            {"pattern": "bar-image", "test": "BarTest"}, {"pattern": "Foo_old", "test": "BarTest"}]
ALL = ["FooScreenshotTest_dark", "bar-image", "WatchSkipScreenshotTest", "parent-shot", "weird-1", "kotlin"]
PHONE_ONLY = ["WatchSkipScreenshotTest", "parent-shot"]


class Fixture:
    def __init__(self, goldens, sources=None, mappings=MAPPINGS):
        self.tmp = tempfile.TemporaryDirectory()
        root = self.root = self.tmp.name
        self.src = os.path.join(root, "src")
        os.makedirs(self.src)
        for name, body in (sources or BASE_SOURCES).items():
            with open(os.path.join(self.src, name), "w") as fh:
                fh.write(body)
        self.runner = os.path.join(self.src, "Cn1ssDeviceRunner.java")
        with open(self.runner, "w") as fh:
            fh.write(RUNNER)
        self.manifest = os.path.join(root, "port_status.json")
        with open(self.manifest, "w") as fh:
            json.dump({"screenshot_mappings": mappings}, fh)
        for d in DIRS:
            os.makedirs(os.path.join(root, d))
            for f in goldens.get(d, []):
                open(os.path.join(root, d, f + ".png"), "w").close()

    def args(self):
        return dict(sources_dir=self.src, runner=self.runner, dir_ports=DIRS, manifest=self.manifest)

    def plan(self):
        return plan.plan(root=self.root, **self.args())


def complete():
    return {"g/phone": list(ALL), "g/win": list(ALL),
            "g/watch": [x for x in ALL if x not in PHONE_ONLY]}


class GoldenPlanTest(unittest.TestCase):
    def test_complete_tree_has_no_gap(self):
        f = Fixture(complete())
        self.assertEqual(f.plan(), ([], []))

    def test_a_golden_missing_in_one_directory_is_a_gap(self):
        g = complete()
        g["g/win"] = [x for x in ALL if x != "bar-image"]
        self.assertEqual(Fixture(g).plan(), ([("BarTest", "g/win")], []))

    def test_new_test_is_missing_everywhere_it_runs(self):
        g = complete()
        for d in g:
            g[d] = [x for x in g[d] if x != "FooScreenshotTest_dark"]
        gaps, problems = Fixture(g).plan()
        self.assertEqual(problems, [])
        self.assertEqual(sorted(gaps), [("FooScreenshotTest", d) for d in sorted(DIRS)])

    def test_declared_skip_is_not_a_gap_but_a_golden_there_is_a_contradiction(self):
        g = complete()
        g["g/watch"].append("WatchSkipScreenshotTest")
        gaps, problems = Fixture(g).plan()
        self.assertEqual(gaps, [])
        self.assertEqual(len(problems), 1)
        self.assertIn("WatchSkipScreenshotTest declares cn1ss-skip for watchos", problems[0])

    def test_guard_without_declaration_is_a_problem(self):
        src = dict(BASE_SOURCES)
        src["WatchSkipScreenshotTest.java"] = src["WatchSkipScreenshotTest.java"].replace("// cn1ss-skip: watchos -- why", "")
        gaps, problems = Fixture(complete(), sources=src).plan()
        self.assertTrue(any("declares no `// cn1ss-skip" in p for p in problems), problems)

    def test_declaration_in_base_class_is_inherited(self):
        g = complete()
        g["g/phone"].remove("parent-shot")
        self.assertEqual(Fixture(g).plan(), ([("ChildTest", "g/phone")], []))

    def test_unknown_port_is_an_error(self):
        src = dict(BASE_SOURCES)
        src["WatchSkipScreenshotTest.java"] = src["WatchSkipScreenshotTest.java"].replace("watchos", "watch")
        with self.assertRaises(plan.PlanError):
            Fixture(complete(), sources=src).plan()

    def test_mapping_owns_images_the_name_rules_would_not(self):
        g = complete()
        g["g/phone"].remove("weird-1")
        self.assertEqual(Fixture(g).plan(), ([("MappedTest", "g/phone")], []))

    def test_image_mapped_to_another_test_does_not_cover_a_new_one(self):
        g = complete()
        for d in g:
            g[d] = [x for x in g[d] if x != "FooScreenshotTest_dark"] + ["Foo_old"]
        gaps, _ = Fixture(g).plan()
        self.assertEqual(len(gaps), len(DIRS))

    def test_empty_listing_is_an_error(self):
        with self.assertRaises(plan.PlanError):
            Fixture({}).plan()

    def test_current_tree_is_complete(self):
        self.assertEqual(plan.plan(), ([], []))
        self.assertEqual(plan.plan(from_git=True), ([], []))


class CollectTest(unittest.TestCase):
    def put(self, dl, run, art, rel, data):
        path = os.path.join(dl, run, art, rel)
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, "w") as fh:
            fh.write(data)

    def test_fills_gaps_only_and_fails_on_an_unfilled_one(self):
        g = complete()
        g["g/win"].remove("bar-image")
        g["g/phone"].remove("bar-image")
        f = Fixture(g)
        dl = os.path.join(f.root, "dl")
        self.put(dl, "1", "cn1ss-capture-a", "g/win/bar-image.png", "x")
        self.put(dl, "1", "cn1ss-capture-a", "g/win/stray.png", "x")
        self.put(dl, "2", "cn1ss-capture-b", "g/win/bar-image.png", "y")
        self.assertFalse(col.collect(dl, f.root, **f.args()))  # g/phone still open
        self.assertTrue(os.path.exists(os.path.join(f.root, "g/win/bar-image.png")))
        self.assertFalse(os.path.exists(os.path.join(f.root, "g/win/stray.png")))
        self.put(dl, "3", "cn1ss-capture-c", "g/phone/bar-image.png", "x")
        self.assertTrue(col.collect(dl, f.root, **f.args()))


if __name__ == "__main__":
    unittest.main()
