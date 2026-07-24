#!/usr/bin/env python3
"""Validate LinkedIn social posts and append them to the browser queue.

Social posts live under ``docs/website/social/linkedin`` as Markdown with
YAML-style front matter. Every valid artifact on the default branch enters the
shared queue. The local browser runner gates publication on ``scheduled_at``.
"""

from __future__ import annotations

import argparse
import datetime as dt
import json
import sys
from dataclasses import dataclass
from pathlib import Path
from typing import Any
from zoneinfo import ZoneInfo, ZoneInfoNotFoundError

sys.path.insert(0, str(Path(__file__).resolve().parent))
from syndicate_blog_posts import (  # noqa: E402
    BLOG_DIR,
    REPO_ROOT,
    SITE_BASE_URL,
    STATE_FILE,
    State,
    parse_front_matter,
    parse_post,
)


SOCIAL_DIR = REPO_ROOT / "docs" / "website" / "social" / "linkedin"
QUEUE_FILE = REPO_ROOT / "scripts" / "website" / "syndication-queue.json"
STATIC_DIR = REPO_ROOT / "docs" / "website" / "static"
SUPPORTED_ACCOUNTS = {"codenameone", "shai"}
CANONICAL_TOKEN = "{{canonical}}"


class SocialPostError(ValueError):
    """Raised when a social-post artifact violates the queue contract."""


@dataclass(frozen=True)
class SocialPost:
    path: Path
    slug: str
    title: str
    account: str
    source_slug: str
    publish_local: dt.datetime
    publish_utc: dt.datetime
    timezone: str
    image_path: str
    body: str
    canonical: str

    @property
    def task_id(self) -> str:
        return f"linkedin:{self.slug}"

    @property
    def image_url(self) -> str:
        return f"{SITE_BASE_URL}{self.image_path}"

    @property
    def body_text(self) -> str:
        return self.body.replace(CANONICAL_TOKEN, self.canonical).strip()


def _required(fm: dict[str, Any], key: str, path: Path) -> str:
    value = fm.get(key)
    if value is None or not str(value).strip():
        raise SocialPostError(
            f"{path}: missing required frontmatter field {key!r}"
        )
    return str(value).strip()


def _parse_local_datetime(
    value: str, timezone: str, path: Path
) -> tuple[dt.datetime, dt.datetime]:
    try:
        parsed = dt.datetime.fromisoformat(value)
    except ValueError as err:
        raise SocialPostError(
            f"{path}: publish_at must be an ISO date/time: {value!r}"
        ) from err
    try:
        zone = ZoneInfo(timezone)
    except ZoneInfoNotFoundError as err:
        raise SocialPostError(
            f"{path}: unknown IANA timezone: {timezone!r}"
        ) from err

    if parsed.tzinfo is None:
        local = parsed.replace(tzinfo=zone)
    else:
        local = parsed.astimezone(zone)
    return local, local.astimezone(dt.timezone.utc)


def parse_social_post(
    path: Path,
    blog_dir: Path = BLOG_DIR,
    static_dir: Path = STATIC_DIR,
) -> SocialPost:
    raw = path.read_text(encoding="utf-8")
    try:
        fm, body = parse_front_matter(raw)
    except ValueError as err:
        raise SocialPostError(f"{path}: {err}") from err

    slug = _required(fm, "slug", path)
    if slug != path.stem:
        raise SocialPostError(
            f"{path}: slug must match filename stem ({path.stem!r})"
        )

    platform = _required(fm, "platform", path).lower()
    if platform != "linkedin":
        raise SocialPostError(
            f"{path}: platform must be 'linkedin' for this queue"
        )

    account = _required(fm, "account", path).lower()
    if account not in SUPPORTED_ACCOUNTS:
        raise SocialPostError(
            f"{path}: account must be one of "
            f"{sorted(SUPPORTED_ACCOUNTS)}, got {account!r}"
        )

    source_slug = _required(fm, "source_slug", path)
    source_path = blog_dir / f"{source_slug}.md"
    if not source_path.exists():
        raise SocialPostError(
            f"{path}: source blog post does not exist: {source_path}"
        )
    source = parse_post(source_path)
    if source is None:
        raise SocialPostError(
            f"{path}: could not parse source blog post: {source_path}"
        )

    timezone = _required(fm, "timezone", path)
    publish_local, publish_utc = _parse_local_datetime(
        _required(fm, "publish_at", path), timezone, path
    )
    if publish_local.date() != source.date:
        raise SocialPostError(
            f"{path}: publish_at must use the source blog date "
            f"{source.date.isoformat()}"
        )

    image_path = _required(fm, "image", path)
    if not image_path.startswith("/blog/"):
        raise SocialPostError(f"{path}: image must be a /blog/... path")
    image_file = static_dir / image_path.lstrip("/")
    if not image_file.exists():
        raise SocialPostError(
            f"{path}: image attachment does not exist: {image_file}"
        )

    body = body.strip()
    if not body:
        raise SocialPostError(f"{path}: LinkedIn post body is empty")
    canonical = source.canonical_url
    canonical_references = (
        body.count(CANONICAL_TOKEN) + body.count(canonical)
    )
    if canonical_references != 1:
        raise SocialPostError(
            f"{path}: body must contain exactly one canonical link or "
            f"{CANONICAL_TOKEN} placeholder"
        )
    body_text = body.replace(CANONICAL_TOKEN, canonical)
    if len(body_text) > 3000:
        raise SocialPostError(
            f"{path}: LinkedIn body is {len(body_text)} characters; "
            "maximum is 3000"
        )

    return SocialPost(
        path=path,
        slug=slug,
        title=_required(fm, "title", path),
        account=account,
        source_slug=source_slug,
        publish_local=publish_local,
        publish_utc=publish_utc,
        timezone=timezone,
        image_path=image_path,
        body=body,
        canonical=canonical,
    )


def discover_social_posts(
    social_dir: Path = SOCIAL_DIR,
    blog_dir: Path = BLOG_DIR,
    static_dir: Path = STATIC_DIR,
) -> list[SocialPost]:
    if not social_dir.exists():
        return []
    posts = [
        parse_social_post(path, blog_dir=blog_dir, static_dir=static_dir)
        for path in sorted(social_dir.glob("*.md"))
        if not path.name.startswith("_")
    ]
    posts.sort(
        key=lambda post: (post.publish_utc, post.account, post.slug)
    )
    return posts


def build_task(
    post: SocialPost, queued_at: dt.datetime | None = None
) -> dict[str, Any]:
    queued_at = queued_at or dt.datetime.now(dt.timezone.utc)
    return {
        "id": post.task_id,
        "site": "linkedin",
        "kind": "social",
        "slug": post.slug,
        "source_slug": post.source_slug,
        "account": post.account,
        "title": post.title,
        "canonical": post.canonical,
        "cover_image_url": post.image_url,
        "body_text": post.body_text,
        "scheduled_at": post.publish_utc.isoformat(timespec="seconds"),
        "scheduled_local": post.publish_local.isoformat(
            timespec="minutes"
        ),
        "timezone": post.timezone,
        "queued_at": queued_at.isoformat(timespec="seconds"),
    }


def _load_queue(path: Path) -> dict[str, Any]:
    if not path.exists():
        return {"tasks": []}
    data = json.loads(path.read_text(encoding="utf-8"))
    if not isinstance(data.get("tasks"), list):
        raise SocialPostError(f"{path}: queue must contain a tasks list")
    return data


def queue_posts(
    posts: list[SocialPost],
    queue: dict[str, Any],
    state: State,
    queued_at: dt.datetime | None = None,
) -> list[dict[str, Any]]:
    existing_ids = {
        task.get("id") for task in queue.get("tasks", [])
    }
    new_tasks: list[dict[str, Any]] = []
    for post in posts:
        if post.task_id in existing_ids:
            continue
        if state.is_syndicated(post.slug, "linkedin"):
            continue
        task = build_task(post, queued_at=queued_at)
        new_tasks.append(task)
        existing_ids.add(post.task_id)
    return new_tasks


def parse_args(argv: list[str]) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--social-dir", default=str(SOCIAL_DIR))
    parser.add_argument("--blog-dir", default=str(BLOG_DIR))
    parser.add_argument("--static-dir", default=str(STATIC_DIR))
    parser.add_argument("--state-file", default=str(STATE_FILE))
    parser.add_argument("--queue-file", default=str(QUEUE_FILE))
    parser.add_argument("--validate-only", action="store_true")
    parser.add_argument("--dry-run", action="store_true")
    return parser.parse_args(argv)


def main(argv: list[str]) -> int:
    args = parse_args(argv)
    try:
        posts = discover_social_posts(
            Path(args.social_dir),
            Path(args.blog_dir),
            Path(args.static_dir),
        )
    except (SocialPostError, json.JSONDecodeError) as err:
        print(err, file=sys.stderr)
        return 2

    print(f"Validated {len(posts)} LinkedIn social post artifact(s).")
    if args.validate_only:
        return 0

    queue_path = Path(args.queue_file)
    try:
        queue = _load_queue(queue_path)
        state = State.load(Path(args.state_file))
    except (SocialPostError, json.JSONDecodeError) as err:
        print(err, file=sys.stderr)
        return 2

    new_tasks = queue_posts(posts, queue, state)
    if not new_tasks:
        print("No new LinkedIn tasks to queue.")
        return 0

    print(f"Queueing {len(new_tasks)} LinkedIn task(s):")
    for task in new_tasks:
        print(
            f"  + {task['id']} "
            f"({task['account']}, {task['scheduled_local']})"
        )
    if not args.dry_run:
        queue.setdefault("tasks", []).extend(new_tasks)
        queue_path.write_text(
            json.dumps(queue, indent=2) + "\n", encoding="utf-8"
        )
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
