import base64
import json
import subprocess
import unittest
from unittest.mock import patch

import publish_port_status as publisher


def response(status, body, rc=0, stderr=""):
    return subprocess.CompletedProcess([], rc, f"HTTP/2.0 {status} OK\nContent-Type: application/json\n\n{body}", stderr)


class ApiTests(unittest.TestCase):
    def test_transient_transport_and_empty_json_recover(self):
        failures = [subprocess.CompletedProcess([], 1, "", "connection timed out"),
                    response(200, ""), response(503, "unavailable", 1)]
        for failure in failures:
            with self.subTest(failure=failure), patch.object(publisher.subprocess, "run", side_effect=[failure, response(200, '{"ok":true}')]) as run, patch.object(publisher.time, "sleep"):
                self.assertEqual({"ok": True}, publisher.api("endpoint"))
                self.assertEqual(2, run.call_count)

    def test_auth_and_validation_errors_are_not_transport_retries(self):
        for status in (401, 403, 404, 409, 422):
            with self.subTest(status=status), patch.object(publisher.subprocess, "run", return_value=response(status, '{}', 1, "denied")) as run:
                with self.assertRaises(publisher.ApiError) as error:
                    publisher.api("endpoint")
                self.assertEqual(status, error.exception.status)
                self.assertEqual(1, run.call_count)

    def test_exhausted_transient_error_stays_fatal(self):
        with patch.object(publisher.subprocess, "run", return_value=response(502, "", 1)) as run, patch.object(publisher.time, "sleep"):
            with self.assertRaises(publisher.ApiError):
                publisher.api("endpoint")
            self.assertEqual(3, run.call_count)

    def test_hung_request_has_a_bounded_retry(self):
        with patch.object(publisher.subprocess, "run", side_effect=[subprocess.TimeoutExpired("gh", 60), response(200, '{}')]) as run, patch.object(publisher.time, "sleep"):
            self.assertEqual({}, publisher.api("endpoint"))
            self.assertEqual(60, run.call_args.kwargs["timeout"])


class PublishTests(unittest.TestCase):
    report = {"port": "windows-x64", "generated_at": "2026-09-06T07:00:00Z"}

    def stored(self, timestamp, sha="old"):
        return {"sha": sha, "content": base64.b64encode(json.dumps({"generated_at": timestamp}).encode()).decode()}

    def test_failed_read_never_becomes_blind_write(self):
        with patch.object(publisher, "api", side_effect=[{}, publisher.ApiError("unavailable", 503)]) as api:
            with self.assertRaises(publisher.ApiError):
                publisher.publish(self.report, "owner/repo")
            self.assertEqual(2, api.call_count)

    def test_conflict_rechecks_timestamp_and_preserves_newer_report(self):
        with patch.object(publisher, "api", side_effect=[{}, self.stored("2026-09-06T06:00:00Z"), publisher.ApiError("conflict", 409), self.stored("2026-09-06T08:00:00Z")]) as api, patch.object(publisher.time, "sleep"):
            publisher.publish(self.report, "owner/repo")
            self.assertEqual(4, api.call_count)
            self.assertEqual("old", api.call_args_list[2].args[2]["sha"])

    def test_conflict_uses_fresh_sha_when_ours_is_still_newer(self):
        with patch.object(publisher, "api", side_effect=[{}, self.stored("2026-09-06T05:00:00Z"), publisher.ApiError("conflict", 409), self.stored("2026-09-06T06:00:00Z", "fresh"), {}]) as api, patch.object(publisher.time, "sleep"):
            publisher.publish(self.report, "owner/repo")
            self.assertEqual("fresh", api.call_args.args[2]["sha"])

    def test_only_404_allows_creating_a_report(self):
        with patch.object(publisher, "api", side_effect=[{}, publisher.ApiError("missing", 404), {}]) as api:
            publisher.publish(self.report, "owner/repo")
            self.assertNotIn("sha", api.call_args.args[2])

    def test_corrupt_existing_report_stays_fatal(self):
        with patch.object(publisher, "api", side_effect=[{}, {"sha": "old", "content": ""}]) as api:
            with self.assertRaises(ValueError):
                publisher.publish(self.report, "owner/repo")
            self.assertEqual(2, api.call_count)

    def test_permission_failure_is_not_a_conflict(self):
        with patch.object(publisher, "api", side_effect=[{}, self.stored("2026-09-06T06:00:00Z"), publisher.ApiError("denied", 403)]) as api:
            with self.assertRaises(publisher.ApiError):
                publisher.publish(self.report, "owner/repo")
            self.assertEqual(3, api.call_count)

    def test_repeated_conflicts_stay_fatal(self):
        outcomes = [{}]
        for _ in range(3):
            outcomes += [self.stored("2026-09-06T06:00:00Z"), publisher.ApiError("conflict", 409)]
        with patch.object(publisher, "api", side_effect=outcomes), patch.object(publisher.time, "sleep"):
            with self.assertRaises(publisher.ApiError):
                publisher.publish(self.report, "owner/repo")

    def test_branch_creation_race_requires_a_real_branch(self):
        outcomes = [publisher.ApiError("missing", 404), {"default_branch": "master"},
                    {"object": {"sha": "base"}}, publisher.ApiError("already exists", 422),
                    {}, self.stored("2026-09-06T08:00:00Z")]
        with patch.object(publisher, "api", side_effect=outcomes) as api:
            publisher.publish(self.report, "owner/repo")
            self.assertEqual(6, api.call_count)

    def test_branch_read_permission_failure_does_not_create_a_branch(self):
        with patch.object(publisher, "api", side_effect=publisher.ApiError("denied", 403)) as api:
            with self.assertRaises(publisher.ApiError):
                publisher.publish(self.report, "owner/repo")
            self.assertEqual(1, api.call_count)


if __name__ == "__main__":
    unittest.main()
