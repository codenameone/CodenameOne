#!/usr/bin/env python3
"""Tests for cn1ss-golden-plan.py and golden-capture-collect.py.

The real tree must report no gap and no contradiction: its golden set is
complete, so any hit there is a convention or declaration the detector gets
wrong."""
import importlib.util
import json
import os
import subprocess
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
            {"pattern": "bar-image", "test": "BarTest"}, {"pattern": "Foo_old", "test": "BarTest"},
            {"pattern": "FooScreenshotTest*", "test": "FooScreenshotTest"},
            {"pattern": "WatchSkipScreenshotTest", "test": "WatchSkipScreenshotTest"},
            {"pattern": "parent-shot", "test": "ChildTest"}, {"pattern": "kotlin", "test": "KotlinUiTest"}]
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

    def plan(self, **kw):
        return plan.plan(root=self.root, **self.args(), **kw)

    def commit(self):
        """Make the current tree a git revision, the PR's base in a test."""
        git = ["git", "-C", self.root, "-c", "user.name=t", "-c", "user.email=t@t"]
        subprocess.run(git[:3] + ["init", "-q"], check=True)
        subprocess.run(git + ["add", "-A"], check=True)
        subprocess.run(git + ["commit", "-qm", "base"], check=True)
        return "HEAD"


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

    def test_one_deleted_image_of_a_multi_image_test_is_a_gap_against_the_base(self):
        g = complete()
        g["g/phone"].append("FooScreenshotTest_light")
        f = Fixture(g)
        base = f.commit()
        os.remove(os.path.join(f.root, "g/phone/FooScreenshotTest_light.png"))
        self.assertEqual(f.plan(), ([], []))  # the tree alone cannot tell
        self.assertEqual(f.plan(base=base), ([("FooScreenshotTest", "g/phone")], []))

    def test_golden_deleted_with_a_change_to_its_test_is_not_a_gap(self):
        g = complete()
        g["g/phone"].append("FooScreenshotTest_light")
        f = Fixture(g)
        base = f.commit()
        os.remove(os.path.join(f.root, "g/phone/FooScreenshotTest_light.png"))
        with open(os.path.join(f.src, "FooScreenshotTest.java"), "a") as fh:
            fh.write("\n// dropped the light variant\n")
        self.assertEqual(f.plan(base=base), ([], []))

    def test_alias_declares_both_metal_directories(self):
        dirs = {"g/phone": {"ios-metal"}, "g/phone27": {"ios-metal-27"}}
        src = dict(BASE_SOURCES)
        src["WatchSkipScreenshotTest.java"] = src["WatchSkipScreenshotTest.java"].replace(
            "cn1ss-skip: watchos", "cn1ss-skip: ios-metal")
        f = Fixture({}, sources=src)
        for d in dirs:
            os.makedirs(os.path.join(f.root, d), exist_ok=True)
            for x in ALL:
                if x not in PHONE_ONLY or x == "parent-shot":
                    open(os.path.join(f.root, d, x + ".png"), "w").close()
        args = f.args()
        args["dir_ports"] = dirs
        self.assertEqual(plan.plan(root=f.root, **args), ([], []))

    def test_empty_listing_is_an_error(self):
        with self.assertRaises(plan.PlanError):
            Fixture({}).plan()

    def test_current_tree_declarations_hold(self):
        # Only the declarations: a PR that adds a screenshot test has gaps
        # until golden capture commits its goldens, and golden-plan.yml is
        # the check that reports them. Failing here as well left such a PR
        # with a second red check that nothing but the capture could clear.
        self.assertEqual(plan.plan()[1], [])
        self.assertEqual(plan.plan(from_git=True)[1], [])


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
        self.put(dl, "2", "cn1ss-capture-b", "g/win/bar-image.png", "x")
        self.assertFalse(col.collect(dl, f.root, **f.args()))  # g/phone still open
        self.assertTrue(os.path.exists(os.path.join(f.root, "g/win/bar-image.png")))
        self.assertFalse(os.path.exists(os.path.join(f.root, "g/win/stray.png")))
        self.put(dl, "3", "cn1ss-capture-c", "g/phone/bar-image.png", "x")
        self.assertTrue(col.collect(dl, f.root, **f.args()))

    def test_legs_sharing_a_directory_that_disagree_fail(self):
        g = complete()
        g["g/win"].remove("bar-image")
        f = Fixture(g)
        dl = os.path.join(f.root, "dl")
        self.put(dl, "1", "cn1ss-capture-a", "g/win/bar-image.png", "x")
        self.put(dl, "2", "cn1ss-capture-b", "g/win/bar-image.png", "y")
        self.assertFalse(col.collect(dl, f.root, **f.args()))
        self.assertFalse(os.path.exists(os.path.join(f.root, "g/win/bar-image.png")))

    @staticmethod
    def png(changed=(), value=0):
        """A 100x100 RGBA PNG, grey except the pixels in `changed` set to `value`."""
        import struct
        import zlib
        rows = b""
        for y in range(100):
            row = bytearray([128] * 400)
            for (cx, cy) in changed:
                if cy == y:
                    row[cx * 4:cx * 4 + 3] = bytes([value] * 3)
            rows += b"\0" + bytes(row)

        def chunk(kind, body):
            return struct.pack(">I", len(body)) + kind + body + struct.pack(">I", zlib.crc32(kind + body))
        return (b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", 100, 100, 8, 6, 0, 0, 0))
                + chunk(b"IDAT", zlib.compress(rows)) + chunk(b"IEND", b""))

    def put_bytes(self, dl, run, art, rel, data):
        path = os.path.join(dl, run, art, rel)
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, "wb") as fh:
            fh.write(data)

    def test_legs_differing_only_by_anti_aliasing_commit_with_a_sidecar(self):
        # #6265: Windows x64 and arm64 differed in one glyph pixel by 9.
        g = complete()
        g["g/win"].remove("bar-image")
        f = Fixture(g)
        dl = os.path.join(f.root, "dl")
        self.put_bytes(dl, "1", "cn1ss-capture-a", "g/win/bar-image.png", self.png())
        self.put_bytes(dl, "2", "cn1ss-capture-b", "g/win/bar-image.png", self.png([(5, 5)], 137))
        self.assertTrue(col.collect(dl, f.root, **f.args()))
        self.assertTrue(os.path.exists(os.path.join(f.root, "g/win/bar-image.png")))
        with open(os.path.join(f.root, "g/win/bar-image.tolerance")) as fh:
            side = fh.read()
        self.assertIn("maxChannelDelta=16", side)
        self.assertIn("maxMismatchPercent=0.1", side)

    def test_legs_differing_beyond_anti_aliasing_still_fail(self):
        for changed, value in (([(5, 5)], 200), ([(x, 5) for x in range(20)], 137)):
            g = complete()
            g["g/win"].remove("bar-image")
            f = Fixture(g)
            dl = os.path.join(f.root, "dl")
            self.put_bytes(dl, "1", "cn1ss-capture-a", "g/win/bar-image.png", self.png())
            self.put_bytes(dl, "2", "cn1ss-capture-b", "g/win/bar-image.png", self.png(changed, value))
            self.assertFalse(col.collect(dl, f.root, **f.args()))
            self.assertFalse(os.path.exists(os.path.join(f.root, "g/win/bar-image.png")))
            self.assertFalse(os.path.exists(os.path.join(f.root, "g/win/bar-image.tolerance")))

    def emit(self, dl, run, art, listing):
        path = os.path.join(dl, run, art, "cn1ss-emitted.json")
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, "w") as fh:
            json.dump(listing, fh)

    def deleted_fixture(self):
        g = complete()
        g["g/phone"].append("FooScreenshotTest_light")
        f = Fixture(g)
        base = f.commit()
        os.remove(os.path.join(f.root, "g/phone/FooScreenshotTest_light.png"))
        return f, base, os.path.join(f.root, "dl")

    def test_deleted_golden_still_rendered_must_come_back(self):
        f, base, dl = self.deleted_fixture()
        self.emit(dl, "1", "cn1ss-capture-p", {"g/phone": ["FooScreenshotTest_light", "bar-image"]})
        self.assertFalse(col.collect(dl, f.root, base=base, **f.args()))
        self.put(dl, "1", "cn1ss-capture-p", "g/phone/FooScreenshotTest_light.png", "x")
        self.assertTrue(col.collect(dl, f.root, base=base, **f.args()))
        self.assertTrue(os.path.exists(os.path.join(f.root, "g/phone/FooScreenshotTest_light.png")))

    def test_deleted_golden_no_longer_rendered_stays_deleted(self):
        f, base, dl = self.deleted_fixture()
        self.emit(dl, "1", "cn1ss-capture-p", {"g/phone": ["FooScreenshotTest_dark"]})
        self.assertTrue(col.collect(dl, f.root, base=base, **f.args()))

    def test_deleted_golden_with_no_report_from_its_leg_fails(self):
        f, base, dl = self.deleted_fixture()
        os.makedirs(dl)
        self.assertFalse(col.collect(dl, f.root, base=base, **f.args()))

    def test_harvest_takes_owned_images_and_rejects_unowned(self):
        f = Fixture(complete())
        dl = os.path.join(f.root, "dl")
        self.put(dl, "1", "cn1ss-capture-a", "g/phone/FooScreenshotTest_new.png", "x")
        self.assertTrue(col.collect(dl, f.root, harvest=True, **f.args()))
        self.assertTrue(os.path.exists(os.path.join(f.root, "g/phone/FooScreenshotTest_new.png")))
        self.put(dl, "2", "cn1ss-capture-b", "g/phone/nobody.png", "x")
        self.assertFalse(col.collect(dl, f.root, harvest=True, **f.args()))
        self.assertFalse(os.path.exists(os.path.join(f.root, "g/phone/nobody.png")))
        self.put(dl, "3", "cn1ss-capture-c", "elsewhere/FooScreenshotTest_x.png", "x")
        self.assertFalse(col.collect(dl, f.root, harvest=True, **f.args()))


if __name__ == "__main__":
    unittest.main()
