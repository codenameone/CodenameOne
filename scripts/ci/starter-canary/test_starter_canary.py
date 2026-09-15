#!/usr/bin/env python3
"""Self-test for the starter canary's assertions.

The canary itself talks to production, so it cannot run on a PR. These tests
run anywhere and prove the assertions still fire for the three regressions that
actually reached users -- otherwise the canary could quietly degrade into a
green check that verifies nothing, which is the failure mode it exists to end.
"""
import contextlib
import io
import sys
import tempfile
import unittest
import zipfile
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import starter_canary as canary

GOOD_POM = """<project>
  <properties><cn1.version>7.0.269</cn1.version></properties>
  <repositories>
    <repository><id>cn1</id><url>https://repo.codenameone.com/maven2</url></repository>
  </repositories>
  <pluginRepositories>
    <pluginRepository><id>cn1p</id><url>https://repo.codenameone.com/maven2</url></pluginRepository>
  </pluginRepositories>
</project>
"""

LAUNCHERS = ("build.sh", "run.sh", "mvnw")


@contextlib.contextmanager
def fake_builds(rows):
    """Stand in for the console build list so the assertions can be tested offline."""
    original = canary.list_builds
    canary.list_builds = lambda opener, base: list(rows)
    try:
        yield
    finally:
        canary.list_builds = original


def make_zip(pom=GOOD_POM, executable=True, root="my-first-app"):
    buffer = io.BytesIO()
    with zipfile.ZipFile(buffer, "w") as zf:
        info = zipfile.ZipInfo(f"{root}/pom.xml")
        info.external_attr = 0o644 << 16
        zf.writestr(info, pom)
        for name in LAUNCHERS:
            info = zipfile.ZipInfo(f"{root}/{name}")
            info.external_attr = (0o755 if executable else 0o644) << 16
            zf.writestr(info, "#!/bin/sh\necho hi\n")
    buffer.seek(0)
    return buffer


class StarterAssertions(unittest.TestCase):
    def unpack(self, buffer):
        directory = Path(tempfile.mkdtemp())
        archive = directory / "starter.zip"
        archive.write_bytes(buffer.read())
        return canary.unpack(archive, directory / canary.AWKWARD)

    def test_healthy_starter_passes(self):
        project, modes = self.unpack(make_zip())
        canary.check_launcher_bits(project, modes)
        self.assertEqual(canary.check_repositories(project), "7.0.269")

    def test_non_executable_launchers_fail(self):
        """BuildCloud #144 -- permission denied on the first documented command."""
        project, modes = self.unpack(make_zip(executable=False))
        with self.assertRaises(canary.CanaryFailure) as caught:
            canary.check_launcher_bits(project, modes)
        self.assertIn("non-executable", str(caught.exception))
        self.assertIn("build.sh", str(caught.exception))

    def test_missing_repositories_fail(self):
        """BuildCloud #139 -- a pin past 7.0.267 resolves nothing."""
        pom = GOOD_POM.replace("<repositories>", "<nope>").replace("</repositories>", "</nope>")
        project, _ = self.unpack(make_zip(pom=pom))
        with self.assertRaises(canary.CanaryFailure) as caught:
            canary.check_repositories(project)
        self.assertIn("repositories", str(caught.exception))

    def test_missing_plugin_repositories_fail(self):
        pom = GOOD_POM.replace("<pluginRepositories>", "<nope>").replace("</pluginRepositories>", "</nope>")
        project, _ = self.unpack(make_zip(pom=pom))
        with self.assertRaises(canary.CanaryFailure):
            canary.check_repositories(project)

    def test_repositories_pointing_elsewhere_fail(self):
        pom = GOOD_POM.replace("https://repo.codenameone.com/maven2", "https://repo.maven.apache.org/maven2")
        project, _ = self.unpack(make_zip(pom=pom))
        with self.assertRaises(canary.CanaryFailure) as caught:
            canary.check_repositories(project)
        self.assertIn("repo.codenameone.com", str(caught.exception))

    def test_local_jar_without_submission_fails(self):
        """BuildCloud #146 -- build.bat produced a local jar and said nothing."""
        with fake_builds([]):
            with self.assertRaises(canary.CanaryFailure) as caught:
                canary.await_cloud_build(
                    None, "https://x", set(), "win32", 0,
                    "BUILD SUCCESS\nBuilding jar: target/app.jar\n",
                    timeout=0, interval=0)
        self.assertIn("never created a build on the server", str(caught.exception))

    def test_failed_cloud_build_fails(self):
        with fake_builds([{"id": "b2", "status": "failed", "submittedAt": 2,
                           "message": "compilation error"}]):
            with self.assertRaises(canary.CanaryFailure) as caught:
                canary.await_cloud_build(None, "https://x", {"b1"}, "javascript", 0, "",
                                         timeout=1, interval=0)
        self.assertIn("finished as 'failed'", str(caught.exception))
        self.assertIn("compilation error", str(caught.exception))

    def test_successful_cloud_build_passes(self):
        with fake_builds([{"id": "b1", "status": "success", "submittedAt": 1},
                          {"id": "b2", "status": "success", "submittedAt": 2}]):
            build = canary.await_cloud_build(None, "https://x", {"b1"}, "javascript", 0, "",
                                             timeout=5, interval=0)
        self.assertEqual(build["id"], "b2")

    def test_build_stuck_in_queue_fails(self):
        with fake_builds([{"id": "b9", "status": "queued", "submittedAt": 9}]):
            with self.assertRaises(canary.CanaryFailure) as caught:
                canary.await_cloud_build(None, "https://x", set(), "javascript", 0, "",
                                         timeout=0.2, interval=0)
        self.assertIn("still 'queued'", str(caught.exception))

    def test_pre_existing_build_is_not_mistaken_for_ours(self):
        """A free account keeps only its latest build; ids, not counts, decide."""
        with fake_builds([{"id": "old", "status": "success", "submittedAt": 1}]):
            with self.assertRaises(canary.CanaryFailure) as caught:
                canary.await_cloud_build(None, "https://x", {"old"}, "javascript", 0, "",
                                         timeout=0, interval=0)
        self.assertIn("never created a build on the server", str(caught.exception))

    def test_corrupt_archive_is_reported_clearly(self):
        """An error page served as starter.zip must not surface as a stack trace."""
        directory = Path(tempfile.mkdtemp())
        archive = directory / "starter.zip"
        archive.write_bytes(b"<html>502 Bad Gateway</html>")
        with self.assertRaises(canary.CanaryFailure) as caught:
            canary.unpack(archive, directory / "out")
        self.assertIn("could not be unpacked", str(caught.exception))

    def test_multiple_roots_rejected(self):
        buffer = io.BytesIO()
        with zipfile.ZipFile(buffer, "w") as zf:
            zf.writestr("a/pom.xml", GOOD_POM)
            zf.writestr("b/pom.xml", GOOD_POM)
        buffer.seek(0)
        with self.assertRaises(canary.CanaryFailure):
            self.unpack(buffer)


if __name__ == "__main__":
    unittest.main(verbosity=2)
