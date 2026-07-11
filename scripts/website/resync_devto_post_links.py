#!/usr/bin/env python3
"""Re-render and update already-syndicated dev.to articles in place.

Posts published before the ``post-link`` shortcode was resolved during
syndication went out to dev.to with the shortcode -- link *and* its visible
text -- stripped out (e.g. "the walkthrough is in ."). This one-shot repair
re-renders each affected post with the current (fixed) ``render_syndicated_body``
and PUTs the corrected Markdown back to the existing dev.to article, using the
article id recorded in ``syndication-state.json``.

It is idempotent and conservative: it only touches articles whose freshly
rendered body differs from what the fix would produce, and it never creates,
deletes, or unpublishes anything -- a PUT with only ``body_markdown`` leaves
title, tags, canonical URL, and cover image untouched.

Dry-run by default. Pass ``--apply`` (and DEVTO_API_KEY in the environment) to
actually push the updates.

    # preview what would change
    python3 resync_devto_post_links.py

    # push the corrections
    DEVTO_API_KEY=xxxx python3 resync_devto_post_links.py --apply
"""

from __future__ import annotations

import argparse
import datetime as dt
import json
import os
import sys
import urllib.error
import urllib.request
from pathlib import Path

from syndicate_blog_posts import (  # noqa: E402  (sibling module on sys.path)
    BLOG_DIR,
    STATE_FILE,
    USER_AGENT,
    Post,
    State,
    _POST_LINK_RE,
    _shortcode_attr,
    discover_posts,
    render_post_links,
    render_syndicated_body,
)

DEVTO_API_URL = "https://dev.to/api/articles"


def preview_post_links(post: Post, posts: list[Post], today: dt.date) -> list[str]:
    """Show, per post-link shortcode, the ``before -> after`` of the repair."""
    lines: list[str] = []
    for match in _POST_LINK_RE.finditer(post.body):
        attrs = match.group(1) if match.group(1) is not None else match.group(2)
        text = _shortcode_attr(attrs, "text") or ""
        after = render_post_links(match.group(0), posts, today)
        # The old renderer stripped the shortcode entirely -> empty string.
        lines.append(f'    "" (was stripped)  ->  {after!r}')
        if not after:
            lines[-1] = f'    still plain text (target unpublished): {text!r}'
    return lines


def http_put_json(url: str, api_key: str, payload: dict) -> dict:
    data = json.dumps(payload).encode("utf-8")
    request = urllib.request.Request(url, data=data, method="PUT")
    request.add_header("Content-Type", "application/json")
    request.add_header("User-Agent", USER_AGENT)
    request.add_header("Accept", "application/vnd.forem.api-v1+json")
    request.add_header("api-key", api_key)
    try:
        with urllib.request.urlopen(request, timeout=60) as response:
            body = response.read().decode("utf-8")
    except urllib.error.HTTPError as err:
        detail = err.read().decode("utf-8", errors="replace")
        raise RuntimeError(f"{url} returned HTTP {err.code}: {detail}") from err
    return json.loads(body) if body else {}


def parse_args(argv: list[str]) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--apply", action="store_true",
                        help="Actually PUT updates to dev.to (default: dry-run preview).")
    parser.add_argument("--today", default=None,
                        help="Override today's date (YYYY-MM-DD) for link resolution.")
    parser.add_argument("--state-file", default=str(STATE_FILE))
    parser.add_argument("--blog-dir", default=str(BLOG_DIR))
    return parser.parse_args(argv)


def main(argv: list[str]) -> int:
    args = parse_args(argv)
    today = dt.date.fromisoformat(args.today) if args.today else dt.date.today()

    posts = discover_posts(Path(args.blog_dir))
    by_slug = {p.slug: p for p in posts}
    state = State.load(Path(args.state_file))

    api_key = os.environ.get("DEVTO_API_KEY")
    if args.apply and not api_key:
        print("ERROR: --apply requires DEVTO_API_KEY in the environment.", file=sys.stderr)
        return 2

    # Only posts that actually use the post-link shortcode could have been
    # damaged; re-rendering the rest would be a no-op PUT with no benefit.
    candidates: list[tuple[str, int]] = []
    for slug, platforms in state.raw.get("posts", {}).items():
        entry = platforms.get("devto")
        if not (entry and entry.get("id")):
            continue
        post = by_slug.get(slug)
        if post is None or "post-link" not in post.body:
            continue
        candidates.append((slug, entry["id"]))

    if not candidates:
        print("No dev.to articles use post-link; nothing to repair.")
        return 0

    updated = 0
    for slug, article_id in candidates:
        post = by_slug[slug]
        body = render_syndicated_body(post, posts, today)
        url = f"{DEVTO_API_URL}/{article_id}"
        print(f"[{slug}] dev.to id={article_id}")
        if not args.apply:
            for ln in preview_post_links(post, posts, today):
                print(ln)
            print("    (dry-run — pass --apply to push)")
            continue
        http_put_json(url, api_key, {"article": {"body_markdown": body}})
        print("    updated.")
        updated += 1

    if args.apply:
        print(f"\nDone. Updated {updated} dev.to article(s).")
    else:
        print(f"\nDry-run complete. {len(candidates)} article(s) would be re-pushed with --apply.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
