#!/usr/bin/env python3
"""Self-test for the starter canary's assertions.

The canary itself talks to the live service, so it cannot run on a PR. These
tests run anywhere and prove each assertion still fires for the breakage it was
written for -- otherwise the canary could quietly degrade into a green check
that verifies nothing, which is the failure mode it exists to end.
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
  <properties>
    <cn1.plugin.version>7.0.269</cn1.plugin.version>
    <cn1.version>7.0.269</cn1.version>
  </properties>
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
        self.assertEqual(canary.check_repositories(project), ("7.0.269", "7.0.269"))

    def test_non_executable_launchers_fail(self):
        """Non-executable launchers: permission denied on the first documented command."""
        project, modes = self.unpack(make_zip(executable=False))
        with self.assertRaises(canary.CanaryFailure) as caught:
            canary.check_launcher_bits(project, modes)
        self.assertIn("non-executable", str(caught.exception))
        self.assertIn("build.sh", str(caught.exception))

    def test_missing_repositories_fail(self):
        """No dependency repository: a pinned release resolves nothing."""
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
        """A launcher that produces a local jar and says nothing is not a cloud build."""
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


class Redaction(unittest.TestCase):
    """Failure text reaches a public tracking issue, so it must name no account."""

    def test_login_failure_does_not_name_the_account(self):
        class Response:
            """Spring bounces a rejected sign-in back to /login?error."""

            def __init__(self, url):
                self.status, self.headers, self.url = 200, {}, url

            def read(self):
                return b'<input name="_csrf" value="t">'

            def __enter__(self):
                return self

            def __exit__(self, *exc):
                return False

        class Opener:
            def __init__(self):
                self.calls = 0

            def open(self, request, timeout=0):
                self.calls += 1
                return Response("https://x/login" if self.calls == 1
                                else "https://x/login?error")

        with self.assertRaises(canary.CanaryFailure) as caught:
            canary.login(Opener(), "https://x", "secret-account@example.com", "pw")
        message = str(caught.exception)
        self.assertNotIn("secret-account@example.com", message)
        self.assertIn("could not sign in", message)

    def test_run_redacts_every_supplied_credential(self):
        code, output = canary.run(
            [sys.executable, "-c", "print('tok-abc123 user@example.com ok')"],
            cwd=".", what="probe", secrets=("tok-abc123", "user@example.com"))
        self.assertNotIn("tok-abc123", output)
        self.assertNotIn("user@example.com", output)
        self.assertIn("***", output)


class Budgets(unittest.TestCase):
    def test_internal_budgets_fit_the_documented_job_timeout(self):
        """The workflow allows 70 minutes; the canary must finish inside it."""
        total_minutes = (canary.LAUNCH_TIMEOUT + canary.POLL_TIMEOUT) / 60
        self.assertLess(total_minutes, 70, "job timeout-minutes must exceed this")


class PomProperties(unittest.TestCase):
    """The plugin version is its own property and must not be assumed equal."""

    def test_plugin_version_read_independently(self):
        pom = GOOD_POM.replace("<cn1.plugin.version>7.0.269</cn1.plugin.version>",
                               "<cn1.plugin.version>7.0.271</cn1.plugin.version>")
        directory = Path(tempfile.mkdtemp())
        (directory / "pom.xml").write_text(pom)
        self.assertEqual(canary.check_repositories(directory), ("7.0.269", "7.0.271"))

    def test_plugin_version_falls_back_to_framework_version(self):
        pom = GOOD_POM.replace("<cn1.plugin.version>7.0.269</cn1.plugin.version>", "")
        directory = Path(tempfile.mkdtemp())
        (directory / "pom.xml").write_text(pom)
        self.assertEqual(canary.check_repositories(directory), ("7.0.269", "7.0.269"))


class TargetGuards(unittest.TestCase):
    """Target names are not portable between launchers, so read the served one."""

    CLOUD = 'function javascript {\n  "$MVNW" "package" "-Dcodename1.buildTarget=javascript"\n}\n'
    LOCAL = 'function javascript {\n  "$MVNW" "package" "-Dcodename1.buildTarget=local-javascript"\n}\n'

    def launcher(self, text):
        directory = Path(tempfile.mkdtemp())
        (directory / ("build.bat" if canary.WINDOWS else "build.sh")).write_text(text)
        return directory

    def test_cloud_target_accepted(self):
        canary.check_target_is_cloud(self.launcher(self.CLOUD), "javascript")

    def test_local_target_rejected_before_the_long_poll(self):
        """The archetype maps `javascript` to local-javascript; catch it up front."""
        with self.assertRaises(canary.CanaryFailure) as caught:
            canary.check_target_is_cloud(self.launcher(self.LOCAL), "javascript")
        self.assertIn("local-javascript", str(caught.exception))

    def test_unknown_target_lists_what_is_offered(self):
        with self.assertRaises(canary.CanaryFailure) as caught:
            canary.check_target_is_cloud(self.launcher(self.CLOUD), "javascript_cloud")
        self.assertIn("javascript", str(caught.exception))

    def test_windows_caret_escaped_target_is_parsed(self):
        """build.bat spells it buildTarget^=, which the regex must accept."""
        original = canary.WINDOWS
        canary.WINDOWS = True
        try:
            directory = Path(tempfile.mkdtemp())
            (directory / "build.bat").write_text(
                ':javascript\ncall "%MVNW%" package -Dcodename1.buildTarget^=local-javascript -U -e\n')
            with self.assertRaises(canary.CanaryFailure) as caught:
                canary.check_target_is_cloud(directory, "javascript")
            self.assertIn("local-javascript", str(caught.exception))
        finally:
            canary.WINDOWS = original

    def test_windows_cloud_target_accepted(self):
        original = canary.WINDOWS
        canary.WINDOWS = True
        try:
            directory = Path(tempfile.mkdtemp())
            (directory / "build.bat").write_text(
                ':javascript\ncall "%MVNW%" package -Dcodename1.buildTarget^=javascript -U -e\n')
            canary.check_target_is_cloud(directory, "javascript")
        finally:
            canary.WINDOWS = original

    def test_prefix_named_neighbour_is_not_matched(self):
        """`function ios` must not match inside `function ios_source`."""
        text = ('function ios_source {\n  "$MVNW" "-Dcodename1.buildTarget=ios-source"\n}\n'
                'function ios {\n  "$MVNW" "-Dcodename1.buildTarget=ios-device"\n}\n')
        canary.check_target_is_cloud(self.launcher(text), "ios")

    def test_body_does_not_bleed_into_the_next_target(self):
        """A delegating target must not be judged on its neighbour's buildTarget."""
        text = ('function ios_source {\n  xcode\n}\n'
                'function android_source {\n  "$MVNW" "-Dcodename1.buildTarget=android-source"\n}\n')
        with self.assertRaises(canary.CanaryFailure) as caught:
            canary.check_target_is_cloud(self.launcher(text), "ios_source")
        self.assertIn("could not read the buildTarget", str(caught.exception))
        self.assertNotIn("android-source", str(caught.exception))

    def test_unparseable_target_fails_closed(self):
        """Not being able to read the target is not evidence that it is cloud."""
        text = 'function javascript {\n  "$MVNW" "package" "-DskipTests"\n}\n'
        with self.assertRaises(canary.CanaryFailure) as caught:
            canary.check_target_is_cloud(self.launcher(text), "javascript")
        self.assertIn("could not read the buildTarget", str(caught.exception))

    def test_source_target_rejected_before_the_long_poll(self):
        """*-source generates an IDE project locally and submits nothing."""
        text = ('function android_source {\n'
                '  "$MVNW" "package" "-Dcodename1.buildTarget=android-source"\n}\n')
        with self.assertRaises(canary.CanaryFailure) as caught:
            canary.check_target_is_cloud(self.launcher(text), "android_source")
        self.assertIn("android-source", str(caught.exception))

    def test_source_targets_are_not_in_the_allowlist(self):
        for target in ("android_source", "ios_source", "xcode"):
            self.assertNotIn(target, canary.CHEAP_TARGETS, target)

    def test_apple_launcher_targets_are_not_in_the_allowlist(self):
        for target in ("ios", "ios_release", "ios_source", "xcode",
                       "mac_native", "mac_catalyst"):
            self.assertNotIn(target, canary.CHEAP_TARGETS, target)

    def test_allowlist_holds_only_cheap_targets(self):
        for target in canary.CHEAP_TARGETS:
            self.assertFalse(target.startswith(("ios", "mac", "xcode")), target)


if __name__ == "__main__":
    unittest.main(verbosity=2)
