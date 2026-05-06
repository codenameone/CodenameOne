#!/usr/bin/env python3
"""Append browser-only syndication tasks to syndication-queue.json.

This script is the bridge between the daily CI cron (which knows which
posts are eligible for syndication) and the Codename One Syndicator
Firefox extension (which runs inside the user's logged-in Firefox to
drive Medium/DZone editors past Cloudflare).

Daily flow:

  1. CI runs the API syndicator (foojay, dev.to, hashnode) directly.
  2. CI runs *this* script for `medium,dzone` (or whatever browser
     platforms are configured) — it appends a task entry to
     syndication-queue.json for every eligible post that does not
     already have an entry in syndication-state.json for that platform.
  3. The committed queue file is what the extension polls. When the
     user's Firefox is online, the extension processes pending tasks.
  4. The extension's popup surfaces a JSON patch the user can paste into
     syndication-state.json (or a small local script can ingest the
     extension's results).

Tasks are deduplicated by id (`<platform>:<slug>`).
"""

from __future__ import annotations

import argparse
import datetime as dt
import json
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from syndicate_blog_posts import (  # noqa: E402
    BLOG_DIR,
    ELIGIBILITY_FLOOR,
    MIN_AGE_DAYS,
    Post,
    STATE_FILE,
    State,
    discover_posts,
    render_syndicated_body,
)

QUEUE_FILE = Path(__file__).resolve().parent / "syndication-queue.json"
DEFAULT_PLATFORMS = "medium,dzone"


def _markdown_to_html(text: str) -> str:
    try:
        import markdown as _md
    except ImportError:
        from html import escape as _esc
        return f"<pre>{_esc(text)}</pre>"
    return _md.markdown(text, extensions=["extra", "fenced_code", "sane_lists"], output_format="html5")


def _eligible_posts(today: dt.date, floor: dt.date, min_age_days: int, blog_dir: Path) -> list[Post]:
    posts = discover_posts(blog_dir)
    cutoff = today - dt.timedelta(days=min_age_days)
    return [p for p in posts if p.date > floor and p.date <= cutoff]


def _build_task(post: Post, platform: str) -> dict:
    body_html = _markdown_to_html(render_syndicated_body(post))
    return {
        "id": f"{platform}:{post.slug}",
        "site": platform,
        "slug": post.slug,
        "title": post.title,
        "canonical": post.canonical_url,
        "description": str(post.front_matter.get("description") or "").strip(),
        "cover_image_url": post.cover_image,
        "body_html": body_html,
        "queued_at": dt.datetime.now(dt.timezone.utc).isoformat(timespec="seconds"),
    }


def parse_args(argv: list[str]) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--platforms", default=DEFAULT_PLATFORMS,
                        help=f"Comma-separated platforms (default: {DEFAULT_PLATFORMS}).")
    parser.add_argument("--today", default=None, help="Override today's date (YYYY-MM-DD).")
    parser.add_argument("--floor", default=ELIGIBILITY_FLOOR.isoformat())
    parser.add_argument("--min-age-days", type=int, default=MIN_AGE_DAYS)
    parser.add_argument("--blog-dir", default=str(BLOG_DIR))
    parser.add_argument("--state-file", default=str(STATE_FILE))
    parser.add_argument("--queue-file", default=str(QUEUE_FILE))
    parser.add_argument("--dry-run", action="store_true",
                        help="Print what would be queued; do not modify the file.")
    return parser.parse_args(argv)


def main(argv: list[str]) -> int:
    args = parse_args(argv)
    today = dt.date.fromisoformat(args.today) if args.today else dt.date.today()
    floor = dt.date.fromisoformat(args.floor)
    platforms = [p.strip() for p in args.platforms.split(",") if p.strip()]
    state = State.load(Path(args.state_file))
    posts = _eligible_posts(today, floor, args.min_age_days, Path(args.blog_dir))

    queue_path = Path(args.queue_file)
    if queue_path.exists():
        queue = json.loads(queue_path.read_text(encoding="utf-8"))
    else:
        queue = {"tasks": []}
    existing_ids = {t.get("id") for t in queue.get("tasks", [])}

    new_tasks: list[dict] = []
    for post in posts:
        for platform in platforms:
            task_id = f"{platform}:{post.slug}"
            if task_id in existing_ids:
                continue
            if state.is_syndicated(post.slug, platform):
                continue
            new_tasks.append(_build_task(post, platform))

    if not new_tasks:
        print("No new browser-syndication tasks to queue.")
        return 0

    print(f"Queueing {len(new_tasks)} new task(s):")
    for t in new_tasks:
        print(f"  + {t['id']}")

    if args.dry_run:
        return 0

    queue.setdefault("tasks", []).extend(new_tasks)
    queue_path.write_text(json.dumps(queue, indent=2) + "\n", encoding="utf-8")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
