#!/usr/bin/env python3
"""Syndicate Codename One Hugo blog posts to dev.to and Hashnode.

Selects the oldest blog post under ``docs/website/content/blog`` that:

* has a ``date`` strictly after the eligibility floor (default: 2026-04-30),
* is at least ``--min-age-days`` old (default: 7),
* has not yet been syndicated to a given target platform.

For each unsyndicated platform on the chosen post the script POSTs the
content with ``canonical_url`` pointing back at the original on
``www.codenameone.com`` and records the resulting URL / id in
``scripts/website/syndication-state.json``.

Designed to run from a daily GitHub Action with only the Python standard
library available.
"""

from __future__ import annotations

import argparse
import datetime as dt
import json
import os
import re
import sys
import urllib.error
import urllib.request
from dataclasses import dataclass, field
from pathlib import Path
from typing import Any


REPO_ROOT = Path(__file__).resolve().parents[2]
BLOG_DIR = REPO_ROOT / "docs" / "website" / "content" / "blog"
STATE_FILE = REPO_ROOT / "scripts" / "website" / "syndication-state.json"
SITE_BASE_URL = "https://www.codenameone.com"

ELIGIBILITY_FLOOR = dt.date(2026, 4, 30)  # posts must be strictly newer than this
MIN_AGE_DAYS = 7

CN1_BLURB = (
    "> **What is Codename One?** Codename One is an open-source framework for "
    "building native iOS, Android, desktop, and web apps from a single Java "
    "or Kotlin codebase. Learn more at "
    "[codenameone.com](https://www.codenameone.com/)."
)

DEVTO_TAGS = ["java", "mobile", "android", "ios"]
HASHNODE_TAGS = [
    {"slug": "java", "name": "Java"},
    {"slug": "mobile", "name": "Mobile"},
    {"slug": "android", "name": "Android"},
    {"slug": "ios", "name": "iOS"},
]


@dataclass
class Post:
    path: Path
    slug: str
    title: str
    date: dt.date
    front_matter: dict[str, Any]
    body: str

    @property
    def canonical_url(self) -> str:
        url_field = self.front_matter.get("url")
        if isinstance(url_field, str) and url_field.startswith("/"):
            return f"{SITE_BASE_URL}{url_field}"
        return f"{SITE_BASE_URL}/blog/{self.slug}/"

    @property
    def cover_image(self) -> str | None:
        match = re.search(r"!\[[^\]]*\]\((/blog/[^)\s]+)\)", self.body)
        if match:
            return f"{SITE_BASE_URL}{match.group(1)}"
        return None


@dataclass
class State:
    raw: dict[str, Any] = field(default_factory=dict)

    @classmethod
    def load(cls, path: Path) -> "State":
        if not path.exists():
            return cls(raw={"posts": {}})
        with path.open("r", encoding="utf-8") as handle:
            data = json.load(handle)
        if "posts" not in data or not isinstance(data["posts"], dict):
            data["posts"] = {}
        return cls(raw=data)

    def save(self, path: Path) -> None:
        ordered = {key: self.raw[key] for key in ("_comment", "posts") if key in self.raw}
        for key, value in self.raw.items():
            if key not in ordered:
                ordered[key] = value
        with path.open("w", encoding="utf-8") as handle:
            json.dump(ordered, handle, indent=2, sort_keys=False)
            handle.write("\n")

    def is_syndicated(self, slug: str, platform: str) -> bool:
        post = self.raw["posts"].get(slug)
        if not post:
            return False
        entry = post.get(platform)
        return bool(entry and entry.get("url"))

    def record(self, slug: str, platform: str, payload: dict[str, Any]) -> None:
        post = self.raw["posts"].setdefault(slug, {})
        post[platform] = payload


def parse_front_matter(text: str) -> tuple[dict[str, Any], str]:
    """Parse the small subset of YAML front matter the blog uses.

    The site's posts use simple ``key: value`` pairs (no nesting, no lists),
    so a hand-rolled parser keeps this script dependency-free.
    """
    if not text.startswith("---\n"):
        raise ValueError("missing front matter")
    end = text.find("\n---\n", 4)
    if end == -1:
        raise ValueError("unterminated front matter")
    block = text[4:end]
    body = text[end + len("\n---\n") :]

    fm: dict[str, Any] = {}
    current_key: str | None = None
    current_lines: list[str] | None = None

    for raw_line in block.splitlines():
        if current_key is not None and (raw_line.startswith(" ") or raw_line.startswith("\t") or raw_line == ""):
            current_lines.append(raw_line)
            continue
        if current_lines is not None and current_key is not None:
            fm[current_key] = _coerce_scalar("\n".join(current_lines).strip())
            current_key = None
            current_lines = None

        match = re.match(r"^([A-Za-z0-9_]+):\s*(.*)$", raw_line)
        if not match:
            continue
        key, value = match.group(1), match.group(2)
        if value == "":
            current_key = key
            current_lines = []
        else:
            fm[key] = _coerce_scalar(value)

    if current_lines is not None and current_key is not None:
        fm[current_key] = _coerce_scalar("\n".join(current_lines).strip())

    return fm, body


def _coerce_scalar(value: str) -> Any:
    stripped = value.strip()
    if len(stripped) >= 2 and stripped[0] == stripped[-1] and stripped[0] in ("'", '"'):
        inner = stripped[1:-1]
        if stripped[0] == "'":
            inner = inner.replace("''", "'")
        return inner
    if stripped.lower() in ("true", "false"):
        return stripped.lower() == "true"
    return stripped


def parse_post(path: Path) -> Post | None:
    text = path.read_text(encoding="utf-8")
    try:
        fm, body = parse_front_matter(text)
    except ValueError:
        return None
    date_value = fm.get("date")
    if not isinstance(date_value, str):
        return None
    try:
        date = dt.date.fromisoformat(date_value[:10])
    except ValueError:
        return None
    slug = fm.get("slug") or path.stem
    title = fm.get("title") or slug
    return Post(path=path, slug=slug, title=str(title), date=date, front_matter=fm, body=body)


def discover_posts(blog_dir: Path) -> list[Post]:
    posts: list[Post] = []
    for path in sorted(blog_dir.glob("*.md")):
        if path.name.startswith("_"):
            continue
        post = parse_post(path)
        if post is not None:
            posts.append(post)
    posts.sort(key=lambda p: p.date)
    return posts


def select_candidate(
    posts: list[Post],
    state: State,
    platforms: list[str],
    today: dt.date,
    floor: dt.date,
    min_age_days: int,
) -> Post | None:
    cutoff = today - dt.timedelta(days=min_age_days)
    for post in posts:
        if post.date <= floor:
            continue
        if post.date > cutoff:
            continue
        if all(state.is_syndicated(post.slug, p) for p in platforms):
            continue
        return post
    return None


_RELATIVE_LINK_RE = re.compile(r"(\]\()(/[^)\s]+)(\))")
_RELATIVE_IMG_RE = re.compile(r'(<img\s+[^>]*src=["\'])(/[^"\']+)(["\'])', re.IGNORECASE)


def absolutize_links(body: str) -> str:
    body = _RELATIVE_LINK_RE.sub(lambda m: f"{m.group(1)}{SITE_BASE_URL}{m.group(2)}{m.group(3)}", body)
    body = _RELATIVE_IMG_RE.sub(lambda m: f"{m.group(1)}{SITE_BASE_URL}{m.group(2)}{m.group(3)}", body)
    return body


def insert_blurb(body: str, blurb: str) -> str:
    """Insert ``blurb`` after the first non-image paragraph (i.e. after the fold)."""
    lines = body.split("\n")
    n = len(lines)
    i = 0
    # skip leading blank lines
    while i < n and lines[i].strip() == "":
        i += 1
    # skip a leading header image (a paragraph that is just a markdown image)
    if i < n and re.match(r"^!\[[^\]]*\]\([^)]+\)\s*$", lines[i].strip()):
        i += 1
        while i < n and lines[i].strip() == "":
            i += 1
    # skip the first paragraph of body text
    while i < n and lines[i].strip() != "":
        i += 1
    # i now points at the blank line (or EOF) following the first text paragraph
    insertion = ["", blurb, ""]
    return "\n".join(lines[:i] + insertion + lines[i:])


def render_syndicated_body(post: Post) -> str:
    body = post.body.strip("\n")
    body = absolutize_links(body)
    body = insert_blurb(body, CN1_BLURB)
    return body


def http_post_json(url: str, headers: dict[str, str], payload: dict[str, Any]) -> dict[str, Any]:
    data = json.dumps(payload).encode("utf-8")
    request = urllib.request.Request(url, data=data, method="POST")
    request.add_header("Content-Type", "application/json")
    for key, value in headers.items():
        request.add_header(key, value)
    try:
        with urllib.request.urlopen(request, timeout=60) as response:
            body = response.read().decode("utf-8")
    except urllib.error.HTTPError as err:
        detail = err.read().decode("utf-8", errors="replace")
        raise RuntimeError(f"{url} returned HTTP {err.code}: {detail}") from err
    if not body:
        return {}
    return json.loads(body)


def publish_to_devto(post: Post, body_markdown: str, api_key: str) -> dict[str, Any]:
    payload: dict[str, Any] = {
        "article": {
            "title": post.title,
            "body_markdown": body_markdown,
            "published": True,
            "canonical_url": post.canonical_url,
            "tags": DEVTO_TAGS,
            "description": str(post.front_matter.get("description") or "")[:250] or None,
        }
    }
    cover = post.cover_image
    if cover:
        payload["article"]["main_image"] = cover
    payload["article"] = {k: v for k, v in payload["article"].items() if v is not None}

    response = http_post_json(
        "https://dev.to/api/articles",
        headers={"api-key": api_key, "Accept": "application/vnd.forem.api-v1+json"},
        payload=payload,
    )
    return {
        "id": response.get("id"),
        "url": response.get("url") or response.get("canonical_url"),
        "syndicated_at": dt.datetime.now(dt.timezone.utc).isoformat(timespec="seconds"),
    }


def publish_to_hashnode(post: Post, body_markdown: str, token: str, publication_id: str) -> dict[str, Any]:
    mutation = """
    mutation PublishPost($input: PublishPostInput!) {
      publishPost(input: $input) {
        post { id slug url }
      }
    }
    """.strip()

    input_obj: dict[str, Any] = {
        "title": post.title,
        "contentMarkdown": body_markdown,
        "publicationId": publication_id,
        "tags": HASHNODE_TAGS,
        "originalArticleURL": post.canonical_url,
    }
    cover = post.cover_image
    if cover:
        input_obj["coverImageOptions"] = {"coverImageURL": cover}
    subtitle = str(post.front_matter.get("description") or "").strip()
    if subtitle:
        input_obj["subtitle"] = subtitle[:250]

    response = http_post_json(
        "https://gql.hashnode.com",
        headers={"Authorization": token},
        payload={"query": mutation, "variables": {"input": input_obj}},
    )
    if response.get("errors"):
        raise RuntimeError(f"hashnode GraphQL errors: {response['errors']}")
    published = (response.get("data") or {}).get("publishPost", {}).get("post", {})
    return {
        "id": published.get("id"),
        "url": published.get("url"),
        "syndicated_at": dt.datetime.now(dt.timezone.utc).isoformat(timespec="seconds"),
    }


def parse_args(argv: list[str]) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--dry-run", action="store_true", help="Do not call any APIs; print what would happen.")
    parser.add_argument(
        "--platforms",
        default="devto,hashnode",
        help="Comma-separated subset of platforms to consider (default: devto,hashnode).",
    )
    parser.add_argument(
        "--today",
        default=None,
        help="Override today's date (YYYY-MM-DD). Useful for testing.",
    )
    parser.add_argument(
        "--floor",
        default=ELIGIBILITY_FLOOR.isoformat(),
        help=f"Posts must be dated strictly after this date (default: {ELIGIBILITY_FLOOR.isoformat()}).",
    )
    parser.add_argument(
        "--min-age-days",
        type=int,
        default=MIN_AGE_DAYS,
        help=f"Minimum post age in days before syndicating (default: {MIN_AGE_DAYS}).",
    )
    parser.add_argument(
        "--blog-dir",
        default=str(BLOG_DIR),
        help="Directory containing Hugo blog posts.",
    )
    parser.add_argument(
        "--state-file",
        default=str(STATE_FILE),
        help="Path to syndication state JSON.",
    )
    return parser.parse_args(argv)


def main(argv: list[str]) -> int:
    args = parse_args(argv)
    today = dt.date.fromisoformat(args.today) if args.today else dt.date.today()
    floor = dt.date.fromisoformat(args.floor)
    platforms = [p.strip() for p in args.platforms.split(",") if p.strip()]
    blog_dir = Path(args.blog_dir)
    state_file = Path(args.state_file)

    posts = discover_posts(blog_dir)
    state = State.load(state_file)
    candidate = select_candidate(posts, state, platforms, today, floor, args.min_age_days)
    if candidate is None:
        print("No syndication candidate found today.")
        return 0

    print(f"Selected post: {candidate.slug} (date={candidate.date.isoformat()})")
    body_markdown = render_syndicated_body(candidate)

    devto_key = os.environ.get("DEVTO_API_KEY", "")
    hashnode_token = os.environ.get("HASHNODE_TOKEN", "")
    hashnode_publication = os.environ.get("HASHNODE_PUBLICATION_ID", "")

    any_change = False
    failures: list[str] = []

    for platform in platforms:
        if state.is_syndicated(candidate.slug, platform):
            print(f"  [{platform}] already syndicated; skipping.")
            continue
        if args.dry_run:
            print(f"  [{platform}] dry run — would publish {len(body_markdown)} chars, canonical {candidate.canonical_url}")
            continue
        try:
            if platform == "devto":
                if not devto_key:
                    raise RuntimeError("DEVTO_API_KEY is not set")
                result = publish_to_devto(candidate, body_markdown, devto_key)
            elif platform == "hashnode":
                if not hashnode_token:
                    raise RuntimeError("HASHNODE_TOKEN is not set")
                if not hashnode_publication:
                    raise RuntimeError("HASHNODE_PUBLICATION_ID is not set")
                result = publish_to_hashnode(candidate, body_markdown, hashnode_token, hashnode_publication)
            else:
                raise RuntimeError(f"unknown platform: {platform}")
        except Exception as err:  # noqa: BLE001 — surface any failure as per-platform
            print(f"  [{platform}] FAILED: {err}", file=sys.stderr)
            failures.append(platform)
            continue

        if not result.get("url"):
            print(f"  [{platform}] response missing URL: {result}", file=sys.stderr)
            failures.append(platform)
            continue

        state.record(candidate.slug, platform, result)
        any_change = True
        print(f"  [{platform}] published: {result['url']}")

    if any_change:
        state.save(state_file)
        print(f"Updated state file: {state_file}")

    if failures:
        return 2
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
