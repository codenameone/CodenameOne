#!/usr/bin/env python3

import datetime as dt
import io
import json
import unittest
import zipfile
from unittest import mock

import capture_oss_traction as traction


class CaptureOssTractionTest(unittest.TestCase):
    def test_star_growth_uses_aggregate_baselines(self):
        now = dt.datetime(2026, 7, 18, 12, tzinfo=dt.timezone.utc)
        snapshots = [
            self.snapshot(now - dt.timedelta(days=8), 100),
            self.snapshot(now - dt.timedelta(days=35), 90),
            self.snapshot(now - dt.timedelta(days=95), 70),
        ]
        growth = traction.calculate_star_growth(110, now, snapshots)

        self.assertEqual(growth["last_7_days"]["change"], 10)
        self.assertEqual(growth["last_28_days"]["change"], 20)
        self.assertEqual(growth["last_90_days"]["change"], 40)
        self.assertEqual(growth["last_7_days"]["period_days"], 8.0)

    def test_star_growth_waits_for_a_full_baseline(self):
        now = dt.datetime(2026, 7, 18, 12, tzinfo=dt.timezone.utc)
        growth = traction.calculate_star_growth(
            110, now, [self.snapshot(now - dt.timedelta(days=3), 108)]
        )

        self.assertFalse(growth["last_7_days"]["available"])
        self.assertFalse(growth["last_28_days"]["available"])
        self.assertFalse(growth["last_90_days"]["available"])

    def test_load_artifact_snapshot_reads_aggregate_report(self):
        report = self.snapshot(
            dt.datetime(2026, 7, 11, 12, tzinfo=dt.timezone.utc), 104
        )
        archive = io.BytesIO()
        with zipfile.ZipFile(archive, "w") as bundle:
            bundle.writestr("oss-traction.json", json.dumps(report))

        with mock.patch.object(
            traction, "api_request_bytes", return_value=archive.getvalue()
        ):
            self.assertEqual(
                traction.load_artifact_snapshot(
                    "https://example.invalid/artifact", "token"
                ),
                report,
            )

    def test_summary_marks_unavailable_traffic(self):
        report = {
            "captured_at": "2026-07-18T12:00:00+00:00",
            "repository_metrics": {"stars": 1854},
            "star_growth": {
                "history_available": True,
                "windows": {
                    "last_7_days": {
                        "available": True,
                        "change": 2,
                        "period_days": 7.0,
                    },
                    "last_28_days": {"available": False},
                    "last_90_days": {"available": False},
                },
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
        self.assertIn("+2 over 7.0 days", summary)
        self.assertIn("baseline not available yet", summary)
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

    @staticmethod
    def snapshot(captured_at: dt.datetime, stars: int) -> dict:
        return {
            "captured_at": captured_at.astimezone(dt.timezone.utc).isoformat(
                timespec="seconds"
            ),
            "repository_metrics": {"stars": stars},
        }


if __name__ == "__main__":
    unittest.main()
