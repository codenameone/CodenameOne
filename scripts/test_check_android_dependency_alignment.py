#!/usr/bin/env python3
"""Pins the verdicts of check-android-dependency-alignment.py.

Gradle is replaced by a stub that writes one synthetic AAR per group, chosen
by the artifact name in the coordinate, so the test needs no network and no
Gradle. What is pinned:

  - a 4 KB aligned library fails, and the failure names the group it came from
  - each group is resolved into its own directory, never pooled
  - a coordinate that does not resolve, or resolves to nothing, fails
  - a run that saw too few groups or libraries is an error, not a pass
  - a malformed coordinates file is an error, not an empty pass
"""

import importlib.util
import io
import os
import shutil
import stat
import sys
import tempfile
import unittest
import zipfile
from contextlib import redirect_stderr, redirect_stdout

HERE = os.path.dirname(os.path.abspath(__file__))


def load(name, file_name):
    spec = importlib.util.spec_from_file_location(name, os.path.join(HERE, file_name))
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


GATE = load("dependency_gate", "check-android-dependency-alignment.py")
ELF = load("page_gate_tests", "test_check_16k_page_alignment.py")

# The stub "Gradle": reads groups.txt from the -p directory and writes, per
# group, an AAR whose arm64 library is aligned when the artifact name says
# "good", misaligned for "bad", a FAILURES.txt for "missing", and nothing at
# all for "empty".
STUB = r'''#!/usr/bin/env python3
import os, sys, zipfile
sys.path.insert(0, %(here)r)
project = sys.argv[sys.argv.index("-p") + 1]
spec = __import__("importlib.util").util.spec_from_file_location(
    "t", os.path.join(%(here)r, "test_check_16k_page_alignment.py"))
t = __import__("importlib.util").util.module_from_spec(spec); spec.loader.exec_module(t)
for index, line in enumerate(open(os.path.join(project, "groups.txt")).read().splitlines()):
    out = os.path.join(project, "out", "g%%d" %% index)
    os.makedirs(out, exist_ok=True)
    for coordinate in line.split("\t")[1].split(" "):
        artifact = coordinate.split(":")[1]
        if artifact.startswith("missing"):
            open(os.path.join(out, "FAILURES.txt"), "a").write("Could not find " + coordinate + "\n")
            continue
        if artifact.startswith("empty"):
            continue
        align = 0x4000 if artifact.startswith("good") else 0x1000
        with zipfile.ZipFile(os.path.join(out, artifact + ".aar"), "w") as z:
            z.writestr("jni/arm64-v8a/lib" + artifact + ".so", t.elf64(align))
''' % {"here": HERE}


class DependencyGate(unittest.TestCase):

    def setUp(self):
        self.tmp = tempfile.mkdtemp()
        self.stub = os.path.join(self.tmp, "gradle")
        with open(self.stub, "w") as f:
            f.write(STUB)
        os.chmod(self.stub, os.stat(self.stub).st_mode | stat.S_IEXEC)
        self.saved = (GATE.MIN_GROUPS, GATE.MIN_LIBRARIES_SCANNED)
        GATE.MIN_GROUPS = 1
        GATE.MIN_LIBRARIES_SCANNED = 1
        self.runs = 0

    def tearDown(self):
        GATE.MIN_GROUPS, GATE.MIN_LIBRARIES_SCANNED = self.saved
        shutil.rmtree(self.tmp)

    def run_gate(self, lines):
        self.runs += 1
        coordinates = os.path.join(self.tmp, "coords%d.txt" % self.runs)
        with open(coordinates, "w") as f:
            f.write("\n".join(lines) + "\n")
        work = os.path.join(self.tmp, "work%d" % self.runs)
        out, err = io.StringIO(), io.StringIO()
        with redirect_stdout(out), redirect_stderr(err):
            code = GATE.main(["--coordinates", coordinates, "--gradle", self.stub,
                              "--work", work])
        return code, out.getvalue() + err.getvalue()

    def test_aligned_dependencies_pass(self):
        code, output = self.run_gate(["a\tcom.x:good-one:1.0", "b\tcom.x:good-two:2.0"])
        self.assertEqual(0, code, output)
        self.assertIn("2 groups, 2 Android 64-bit", output)

    def test_misaligned_dependency_fails_under_its_group(self):
        code, output = self.run_gate(["catalog:com/codename1/camera/\tcom.x:bad-camera:1.3.4",
                                      "other\tcom.x:good-one:1.0"])
        self.assertEqual(1, code, output)
        self.assertIn("[catalog:com/codename1/camera/] bad-camera.aar!jni/arm64-v8a", output)
        self.assertNotIn("[other]", output)

    def test_groups_are_resolved_apart(self):
        # The point of per-group resolution: a group's own artifacts are the
        # only ones scanned under its name. Pooled, "b" would be credited with
        # "a"'s library and the stub would still see one directory.
        code, output = self.run_gate(["a\tcom.x:bad-one:1.0", "b\tcom.x:good-two:1.0"])
        self.assertEqual(1, code, output)
        self.assertIn("[a] bad-one.aar", output)
        self.assertNotIn("[b]", output)
        work = os.path.join(self.tmp, "work%d" % self.runs, "out")
        self.assertEqual(["bad-one.aar"], os.listdir(os.path.join(work, "g0")))
        self.assertEqual(["good-two.aar"], os.listdir(os.path.join(work, "g1")))

    def test_two_coordinates_of_one_group_resolve_together(self):
        code, output = self.run_gate(["a\tcom.x:good-one:1.0", "a\tcom.x:bad-two:1.0"])
        self.assertEqual(1, code, output)
        self.assertIn("[a] bad-two.aar", output)

    def test_unresolved_coordinate_fails(self):
        code, output = self.run_gate(["a\tcom.x:missing-thing:9.9", "b\tcom.x:good-one:1.0"])
        self.assertEqual(1, code, output)
        self.assertIn("a: Could not find com.x:missing-thing:9.9", output)

    def test_group_that_resolves_to_nothing_fails(self):
        code, output = self.run_gate(["a\tcom.x:empty-thing:1.0", "b\tcom.x:good-one:1.0"])
        self.assertEqual(1, code, output)
        self.assertIn("a: resolved to no artifacts", output)

    def test_too_few_groups_is_an_error_not_a_pass(self):
        GATE.MIN_GROUPS = 5
        code, output = self.run_gate(["a\tcom.x:good-one:1.0"])
        self.assertEqual(2, code, output)

    def test_too_few_libraries_is_an_error_not_a_pass(self):
        GATE.MIN_LIBRARIES_SCANNED = 5
        code, output = self.run_gate(["a\tcom.x:good-one:1.0"])
        self.assertEqual(2, code, output)

    def test_malformed_line_is_an_error(self):
        code, output = self.run_gate(["a com.x:good-one:1.0"])
        self.assertEqual(2, code, output)
        code, output = self.run_gate(["a\tnot a coordinate"])
        self.assertEqual(2, code, output)

    def test_comments_and_blank_lines_are_ignored(self):
        code, output = self.run_gate(["# header", "", "a\tcom.x:good-one:1.0@aar"])
        self.assertEqual(0, code, output)

    def test_a_reused_work_directory_is_refused(self):
        code, output = self.run_gate(["a\tcom.x:good-one:1.0"])
        self.assertEqual(0, code, output)
        work = os.path.join(self.tmp, "work%d" % self.runs)
        coordinates = os.path.join(self.tmp, "coords%d.txt" % self.runs)
        err = io.StringIO()
        with redirect_stdout(io.StringIO()), redirect_stderr(err):
            code = GATE.main(["--coordinates", coordinates, "--gradle", self.stub,
                              "--work", work])
        self.assertEqual(2, code, err.getvalue())


if __name__ == "__main__":
    unittest.main()
