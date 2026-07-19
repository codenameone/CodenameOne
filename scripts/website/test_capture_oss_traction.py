#!/usr/bin/env python3

import datetime as dt
import unittest
from unittest import mock

import capture_oss_traction as traction


class CaptureOssTractionTest(unittest.TestCase):
    def test_count_windows_uses_rolling_boundaries(self):
        now = dt.datetime(2026, 7, 18, 12, tzinfo=dt.timezone.utc)
        timestamps = [
            now - dt.timedelta(days=1),
            now - dt.timedelta(days=7),
            now - dt.timedelta(days=8),
            now - dt.timedelta(days=29),
            now - dt.timedelta(days=91),
        ]
        self.assertEqual(
            traction.count_windows(timestamps, now),
            {"last_7_days": 2, "last_28_days": 3, "last_90_days": 4},
        )

    def test_summary_marks_unavailable_traffic(self):
        report = {
            "captured_at": "2026-07-18T12:00:00+00:00",
            "repository_metrics": {"stars": 1854},
            "star_velocity": {
                "last_7_days": 2,
                "last_28_days": 8,
                "last_90_days": 24,
            },
            "engagement_last_28_days": {
                "unique_issue_authors": 3,
                "unique_discussion_authors": 2,
            },
            "traffic_last_14_days": {
                "views": {"available": False, "error": "forbidden"},
                "referrers": {"available": False, "error": "forbidden"},
            },
        }
        summary = traction.render_summary(report)
        self.assertIn("OSS_TRACTION_TOKEN", summary)
        self.assertIn("CI and automation", summary)

    def test_unique_logins_ignores_missing_authors(self):
        items = [
            {"user": {"login": "alice"}},
            {"user": {"login": "alice"}},
            {"user": {"login": "bob"}},
            {"user": None},
            {},
        ]
        self.assertEqual(traction.unique_logins(items, "user"), 2)

    def test_resolve_tokens_keeps_traffic_token_separate(self):
        self.assertEqual(
            traction.resolve_tokens(
                {
                    "GITHUB_TOKEN": "repository-token",
                    "OSS_TRACTION_TOKEN": "traffic-token",
                }
            ),
            ("repository-token", "traffic-token"),
        )

    def test_resolve_tokens_falls_back_to_repository_token(self):
        self.assertEqual(
            traction.resolve_tokens({"GH_TOKEN": "repository-token"}),
            ("repository-token", "repository-token"),
        )

    def test_collect_traffic_uses_only_traffic_token(self):
        with mock.patch.object(
            traction,
            "optional_rest",
            return_value={"available": True, "data": {}},
        ) as request:
            traction.collect_traffic("owner/repository", "traffic-token")

        self.assertEqual(
            request.call_args_list,
            [
                mock.call("/repos/owner/repository/traffic/views", "traffic-token"),
                mock.call("/repos/owner/repository/traffic/clones", "traffic-token"),
                mock.call(
                    "/repos/owner/repository/traffic/popular/referrers",
                    "traffic-token",
                ),
            ],
        )


if __name__ == "__main__":
    unittest.main()
