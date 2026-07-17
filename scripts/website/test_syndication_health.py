#!/usr/bin/env python3

from __future__ import annotations

import datetime as dt
import unittest
from types import SimpleNamespace

from check_syndication_health import inspect_platform_state, inspect_queue


UTC = dt.timezone.utc


class SyndicationHealthTest(unittest.TestCase):
    def inspect(self, tasks: list[object], now: dt.datetime) -> dict:
        return inspect_queue(
            {"tasks": tasks},
            now,
            social_grace=dt.timedelta(hours=2),
            browser_grace=dt.timedelta(hours=8),
        )

    def test_future_linkedin_task_is_healthy(self) -> None:
        report = self.inspect(
            [
                {
                    "id": "linkedin:future",
                    "site": "linkedin",
                    "kind": "social",
                    "scheduled_at": "2026-07-18T09:00:00+00:00",
                }
            ],
            dt.datetime(2026, 7, 18, 8, 0, tzinfo=UTC),
        )
        self.assertTrue(report["healthy"])
        self.assertEqual([], report["overdue"])

    def test_linkedin_task_is_flagged_after_grace_period(self) -> None:
        report = self.inspect(
            [
                {
                    "id": "linkedin:stuck",
                    "site": "linkedin",
                    "kind": "social",
                    "scheduled_at": "2026-07-18T09:00:00Z",
                }
            ],
            dt.datetime(2026, 7, 18, 12, 30, tzinfo=UTC),
        )
        self.assertFalse(report["healthy"])
        self.assertEqual("linkedin:stuck", report["overdue"][0]["id"])
        self.assertEqual(1.5, report["overdue"][0]["overdue_hours"])

    def test_browser_task_is_flagged_when_local_runner_does_not_drain_it(self) -> None:
        report = self.inspect(
            [
                {
                    "id": "medium:stuck",
                    "site": "medium",
                    "queued_at": "2026-07-17T13:00:00+00:00",
                }
            ],
            dt.datetime(2026, 7, 17, 22, 0, tzinfo=UTC),
        )
        self.assertFalse(report["healthy"])
        self.assertEqual("medium:stuck", report["overdue"][0]["id"])

    def test_missing_or_naive_timestamp_is_invalid(self) -> None:
        report = self.inspect(
            [
                {"id": "linkedin:missing", "site": "linkedin", "kind": "social"},
                {
                    "id": "medium:naive",
                    "site": "medium",
                    "queued_at": "2026-07-17T13:00:00",
                },
            ],
            dt.datetime(2026, 7, 17, 14, 0, tzinfo=UTC),
        )
        self.assertFalse(report["healthy"])
        self.assertEqual(2, len(report["invalid"]))

    def test_missing_direct_platform_is_flagged_after_daily_grace(self) -> None:
        post = SimpleNamespace(slug="stuck-post", date=dt.date(2026, 7, 8))
        state = {
            "posts": {
                "stuck-post": {
                    "devto": {"url": "https://dev.to/example"},
                    "hashnode": {},
                }
            }
        }
        overdue = inspect_platform_state(
            [post],
            state,
            dt.datetime(2026, 7, 17, 13, 0, tzinfo=UTC),
            grace=dt.timedelta(hours=36),
        )
        self.assertEqual(["hashnode:stuck-post"], [item["id"] for item in overdue])

    def test_foojay_is_expected_only_for_friday_posts(self) -> None:
        thursday = SimpleNamespace(slug="thursday", date=dt.date(2026, 7, 9))
        friday = SimpleNamespace(slug="friday", date=dt.date(2026, 7, 10))
        state = {
            "posts": {
                slug: {
                    "devto": {"url": "https://dev.to/example"},
                    "hashnode": {"url": "https://hashnode.com/example"},
                }
                for slug in ("thursday", "friday")
            }
        }
        overdue = inspect_platform_state(
            [thursday, friday],
            state,
            dt.datetime(2026, 7, 20, 0, 0, tzinfo=UTC),
            grace=dt.timedelta(hours=36),
        )
        self.assertEqual(["foojay:friday"], [item["id"] for item in overdue])


if __name__ == "__main__":
    unittest.main()
