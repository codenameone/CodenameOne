#!/usr/bin/env python3
"""Report syndication work that has not been processed.

The GitHub workflow queues Medium, DZone, and reviewed LinkedIn work, while a
signed-in browser runner on the maintainer's machine drains that queue.  A
successful local run removes completed entries.  Anything left beyond its
grace period therefore needs attention.  DEV, Foojay, and Hashnode do not all
use that queue, so eligible articles are also checked against platform state.
"""

from __future__ import annotations

import argparse
import datetime as dt
import json
import sys
from pathlib import Path

from syndicate_blog_posts import (
    BLOG_DIR,
    ELIGIBILITY_FLOOR,
    MIN_AGE_DAYS,
    STATE_FILE,
    discover_posts,
)


SCRIPT_DIR = Path(__file__).resolve().parent
DEFAULT_QUEUE_FILE = SCRIPT_DIR / "syndication-queue.json"


def parse_timestamp(value: object) -> dt.datetime:
    if not isinstance(value, str) or not value.strip():
        raise ValueError("timestamp is missing")
    parsed = dt.datetime.fromisoformat(value.strip().replace("Z", "+00:00"))
    if parsed.tzinfo is None:
        raise ValueError("timestamp has no timezone")
    return parsed.astimezone(dt.timezone.utc)


def inspect_queue(
    queue: dict,
    now: dt.datetime,
    social_grace: dt.timedelta,
    browser_grace: dt.timedelta,
) -> dict:
    if now.tzinfo is None:
        raise ValueError("now must include a timezone")
    now = now.astimezone(dt.timezone.utc)
    overdue: list[dict] = []
    invalid: list[dict] = []

    tasks = queue.get("tasks", [])
    if not isinstance(tasks, list):
        tasks = []
        invalid.append({"id": "<queue>", "reason": "tasks must be a list"})

    for index, task in enumerate(tasks):
        if not isinstance(task, dict):
            invalid.append({"id": f"<task {index}>", "reason": "task must be an object"})
            continue

        task_id = str(task.get("id") or f"<task {index}>")
        site = str(task.get("site") or "unknown")
        is_social = task.get("kind") == "social" or site == "linkedin"
        timestamp_field = "scheduled_at" if is_social else "queued_at"
        grace = social_grace if is_social else browser_grace

        try:
            base_time = parse_timestamp(task.get(timestamp_field))
        except (TypeError, ValueError) as error:
            invalid.append(
                {
                    "id": task_id,
                    "site": site,
                    "reason": f"invalid {timestamp_field}: {error}",
                }
            )
            continue

        deadline = base_time + grace
        if now > deadline:
            overdue.append(
                {
                    "id": task_id,
                    "site": site,
                    "deadline": deadline.isoformat(timespec="seconds"),
                    "overdue_hours": round((now - deadline).total_seconds() / 3600, 1),
                }
            )

    return {
        "healthy": not overdue and not invalid,
        "checked_at": now.isoformat(timespec="seconds"),
        "pending_count": len(tasks),
        "overdue": overdue,
        "invalid": invalid,
    }


def inspect_platform_state(
    posts: list,
    state: dict,
    now: dt.datetime,
    grace: dt.timedelta,
) -> list[dict]:
    """Find eligible direct-publish articles missing their platform state."""
    if now.tzinfo is None:
        raise ValueError("now must include a timezone")
    now = now.astimezone(dt.timezone.utc)
    state_posts = state.get("posts", {})
    if not isinstance(state_posts, dict):
        state_posts = {}

    overdue: list[dict] = []
    for post in posts:
        eligible_at = dt.datetime.combine(
            post.date + dt.timedelta(days=MIN_AGE_DAYS),
            dt.time.min,
            tzinfo=dt.timezone.utc,
        )
        deadline = eligible_at + grace
        if now <= deadline:
            continue

        expected_platforms = ["devto", "hashnode"]
        if post.date.weekday() == 4:  # Foojay takes the Friday digest only.
            expected_platforms.append("foojay")

        recorded = state_posts.get(post.slug, {})
        if not isinstance(recorded, dict):
            recorded = {}
        for platform in expected_platforms:
            platform_state = recorded.get(platform, {})
            if isinstance(platform_state, dict) and platform_state.get("url"):
                continue
            overdue.append(
                {
                    "id": f"{platform}:{post.slug}",
                    "site": platform,
                    "deadline": deadline.isoformat(timespec="seconds"),
                    "overdue_hours": round((now - deadline).total_seconds() / 3600, 1),
                }
            )
    return overdue


def parse_args(argv: list[str]) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--queue-file", default=str(DEFAULT_QUEUE_FILE))
    parser.add_argument("--state-file", default=str(STATE_FILE))
    parser.add_argument("--blog-dir", default=str(BLOG_DIR))
    parser.add_argument("--output", help="Write the JSON report to this path.")
    parser.add_argument("--now", help="Override the current UTC time for testing.")
    parser.add_argument("--social-grace-hours", type=float, default=2.0)
    parser.add_argument("--browser-grace-hours", type=float, default=8.0)
    parser.add_argument("--platform-grace-hours", type=float, default=36.0)
    return parser.parse_args(argv)


def main(argv: list[str]) -> int:
    args = parse_args(argv)
    queue_path = Path(args.queue_file)
    try:
        queue = json.loads(queue_path.read_text(encoding="utf-8"))
        state = json.loads(Path(args.state_file).read_text(encoding="utf-8"))
        posts = [
            post
            for post in discover_posts(Path(args.blog_dir))
            if post.date > ELIGIBILITY_FLOOR
        ]
    except (OSError, json.JSONDecodeError, ValueError) as error:
        print(f"Unable to read syndication inputs: {error}", file=sys.stderr)
        return 2

    now = parse_timestamp(args.now) if args.now else dt.datetime.now(dt.timezone.utc)
    report = inspect_queue(
        queue,
        now,
        dt.timedelta(hours=args.social_grace_hours),
        dt.timedelta(hours=args.browser_grace_hours),
    )
    report["platform_overdue"] = inspect_platform_state(
        posts,
        state,
        now,
        dt.timedelta(hours=args.platform_grace_hours),
    )
    report["healthy"] = report["healthy"] and not report["platform_overdue"]
    rendered = json.dumps(report, indent=2) + "\n"
    if args.output:
        Path(args.output).write_text(rendered, encoding="utf-8")
    print(rendered, end="")
    return 0 if report["healthy"] else 1


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
