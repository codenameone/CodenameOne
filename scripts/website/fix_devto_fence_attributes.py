#!/usr/bin/env python3
"""Strip Hugo code-fence attributes from already-syndicated dev.to articles.

Posts syndicated before ``strip_fence_attributes`` went out with fences like
```` ```java {class="cn1-no-try"} ````, which dev.to takes as the language and
renders unhighlighted. This edits each article's LIVE body_markdown (so manual
edits made on dev.to survive) and PUTs it back only where a fence changed.

Dry-run by default; ``--apply`` needs DEVTO_API_KEY.
"""

from __future__ import annotations

import argparse
import json
import os
import sys
import time
import urllib.request

import urllib.error

from syndicate_blog_posts import BLOG_DIR, STATE_FILE, USER_AGENT, strip_fence_attributes


def request(url: str, api_key: str | None = None, payload: dict | None = None) -> dict:
    headers = {"User-Agent": USER_AGENT, "Accept": "application/vnd.forem.api-v1+json"}
    data = None
    if api_key:
        headers["api-key"] = api_key
    if payload is not None:
        headers["Content-Type"] = "application/json"
        data = json.dumps(payload).encode("utf-8")
    req = urllib.request.Request(url, data=data, headers=headers, method="PUT" if data else "GET")
    for attempt in range(6):
        try:
            with urllib.request.urlopen(req, timeout=30) as resp:
                return json.loads(resp.read().decode("utf-8"))
        except urllib.error.HTTPError as err:
            if err.code != 429 or attempt == 5:
                raise
            time.sleep(int(err.headers.get("Retry-After") or 0) or 10 * (attempt + 1))
    raise AssertionError("unreachable")


def main(argv: list[str]) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--apply", action="store_true")
    args = parser.parse_args(argv)
    api_key = os.environ.get("DEVTO_API_KEY")
    if args.apply and not api_key:
        print("--apply needs DEVTO_API_KEY", file=sys.stderr)
        return 2
    state = json.loads(STATE_FILE.read_text(encoding="utf-8"))
    posts = state.get("posts", state)
    # Only posts whose source has a fence attribute can carry one on dev.to.
    candidates = {p.stem for p in BLOG_DIR.glob("*.md")
                  if strip_fence_attributes(p.read_text(encoding="utf-8")) != p.read_text(encoding="utf-8")}
    changed = 0
    for slug, platforms in sorted(posts.items()):
        devto = platforms.get("devto") if isinstance(platforms, dict) else None
        if not devto or not devto.get("id") or slug not in candidates:
            continue
        url = f"https://dev.to/api/articles/{devto['id']}"
        try:
            body = request(url, api_key).get("body_markdown") or ""
        except Exception as err:  # noqa: BLE001 - report and keep going
            print(f"skip {slug}: {err}")
            continue
        fixed = strip_fence_attributes(body)
        if fixed == body:
            continue
        changed += 1
        print(f"{'fix' if args.apply else 'would fix'} {slug} ({devto['id']})")
        if args.apply:
            request(url, api_key, {"article": {"body_markdown": fixed}})
            time.sleep(3)  # dev.to rate-limits article updates
        else:
            time.sleep(0.3)
    print(f"{changed} article(s) {'fixed' if args.apply else 'affected'}")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
