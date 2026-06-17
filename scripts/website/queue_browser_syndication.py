#!/usr/bin/env python3
"""Append browser-only syndication tasks to syndication-queue.json.

This script is the bridge between the daily CI cron (which knows which
posts are eligible for syndication) and a local syndication tool that
runs on the maintainer's machine and drives Medium / DZone / Hashnode
editors from inside a signed-in browser session.

Daily flow:

  1. CI runs the API syndicator (dev.to) and the Playwright syndicator
     (foojay) directly.
  2. CI runs *this* script for `medium,dzone` (or whatever browser
     platforms are configured) — it appends a task entry to
     syndication-queue.json for every eligible post that does not
     already have an entry in syndication-state.json for that platform.
  3. The committed queue file is what the local tool reads. When it
     runs on the maintainer's machine it processes pending tasks
     against the already-signed-in browser session.
  4. The local tool writes the resulting URLs back into
     syndication-state.json.

Hashnode is *not* queued here: their free public GraphQL API shut down
on 2026-05-13, but Hashnode's web editor is reachable from
``syndicate_browser_posts.py`` directly via a saved Playwright
storageState, so it is driven inline by that script (HashnodeAdapter)
instead of going through this queue.

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

# Platforms that only receive the weekly Friday digest post, rather than every
# eligible post. This mirrors FoojayAdapter.accepts in
# syndicate_browser_posts.py (post.date.weekday() == 4 -> Friday); deep-dive
# posts published on other weekdays are not syndicated to these platforms.
WEEKLY_FRIDAY_PLATFORMS = {"dzone"}


def _platform_accepts(platform: str, post: Post) -> bool:
    """Whether ``platform`` wants ``post`` at all. Friday-only platforms take
    just the weekly digest post; everyone else takes every eligible post."""
    if platform in WEEKLY_FRIDAY_PLATFORMS:
        return post.date.weekday() == 4
    return True


def _task_is_allowed(task: dict, dates_by_slug: dict[str, dt.date]) -> bool:
    """Whether an existing queue task is still permitted under the per-platform
    rules. Used to prune tasks that were queued before the Friday-only filter
    existed (or added by hand): the drain tool submits whatever is in the
    queue, so a disallowed task left here would still reach moderation. A task
    whose post date is unknown is kept — we never silently drop something we
    cannot evaluate."""
    platform = task.get("site")
    if platform not in WEEKLY_FRIDAY_PLATFORMS:
        return True
    post_date = dates_by_slug.get(task.get("slug"))
    if post_date is None:
        return True
    return post_date.weekday() == 4


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
    blog_dir = Path(args.blog_dir)
    posts = _eligible_posts(today, floor, args.min_age_days, blog_dir)
    # Slug -> publish date for *every* post, so existing queue tasks can be
    # re-checked against the per-platform rules regardless of eligibility.
    dates_by_slug = {p.slug: p.date for p in discover_posts(blog_dir)}

    queue_path = Path(args.queue_file)
    if queue_path.exists():
        queue = json.loads(queue_path.read_text(encoding="utf-8"))
    else:
        queue = {"tasks": []}

    # Prune any already-queued task that the per-platform rules no longer
    # allow (e.g. a non-Friday DZone task queued before the Friday-only
    # filter landed, or one added by hand). The drain tool submits whatever
    # sits in the queue, so this prune is what actually stops a stray task
    # from reaching moderation.
    original_tasks = queue.get("tasks", [])
    kept_tasks = [t for t in original_tasks if _task_is_allowed(t, dates_by_slug)]
    pruned = [t for t in original_tasks if t not in kept_tasks]
    if pruned:
        print(f"Pruning {len(pruned)} disallowed task(s) from the queue:")
        for t in pruned:
            print(f"  - {t.get('id')}")
    queue["tasks"] = kept_tasks
    existing_ids = {t.get("id") for t in kept_tasks}

    new_tasks: list[dict] = []
    for post in posts:
        for platform in platforms:
            if not _platform_accepts(platform, post):
                continue
            task_id = f"{platform}:{post.slug}"
            if task_id in existing_ids:
                continue
            if state.is_syndicated(post.slug, platform):
                continue
            new_tasks.append(_build_task(post, platform))

    if new_tasks:
        print(f"Queueing {len(new_tasks)} new task(s):")
        for t in new_tasks:
            print(f"  + {t['id']}")
    elif not pruned:
        print("No new browser-syndication tasks to queue.")

    if args.dry_run:
        return 0

    # Write when there is anything to add OR anything was pruned, so the
    # cleaned queue is persisted even on a run that queues nothing new.
    if new_tasks or pruned:
        queue.setdefault("tasks", []).extend(new_tasks)
        queue_path.write_text(json.dumps(queue, indent=2) + "\n", encoding="utf-8")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
