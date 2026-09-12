#!/usr/bin/env python3
"""Prepare or submit Friday digests using Foojay's GitHub article-bundle workflow.

Reuses the existing discovery, seven-day delay, body renderer and state file.
The default is an offline dry run; --submit creates a PR for editorial review.
See foojay-syndication.md for token/fork setup and migration behavior.
"""

from __future__ import annotations

import argparse
import base64
import datetime as dt
import hashlib
import json
import os
import re
import subprocess
import sys
import urllib.parse
import urllib.request
from dataclasses import replace
from pathlib import Path
from typing import Any

from syndicate_blog_posts import (
    BLOG_DIR, ELIGIBILITY_FLOOR, MIN_AGE_DAYS, REPO_ROOT,
    STATE_FILE, USER_AGENT, Post, State, _MERMAID_BLOCK_RE,
    discover_posts, render_syndicated_body, select_candidate,
)

UPSTREAM = "foojayio/website"
AUTHOR = "shai-almog"
MAX_IMAGE_BYTES = 4_000_000  # Foojay scripts/validate/Frontmatter.java
MAX_HERO_BYTES = 800_000
MAX_BUNDLE_BYTES = 15_000_000
STATIC_DIR = REPO_ROOT / "docs/website/static"
IMAGE_RE = re.compile(r'(!\[[^\]]*\]\()([^\s)]+)([^)]*\))')
HTML_IMAGE_RE = re.compile(r'(<img\b[^>]*\bsrc=["\'])([^"\']+)(["\'])', re.I)


def accepts(post: Post) -> bool:
    return post.date.weekday() == 4


def validate_slug(slug: str) -> None:
    if not re.fullmatch(r"[a-z0-9]+(?:-[a-z0-9]+)*", slug):
        raise ValueError(f"Invalid Foojay article slug: {slug!r}")


def render_body(post: Post, posts: list[Post], today: dt.date) -> str:
    # Foojay supports native Mermaid fences. Convert before the shared renderer
    # would replace CN1's shortcode with an externally hosted diagram image.
    body = _MERMAID_BLOCK_RE.sub(
        lambda m: "\n```mermaid\n" + m.group(1).strip() + "\n```\n", post.body,
    )
    return render_syndicated_body(replace(post, body=body), posts, today)


def read_image(url: str, static_dir: Path) -> bytes:
    parsed = urllib.parse.urlsplit(url)
    if parsed.scheme not in {"http", "https"}:
        raise ValueError(f"Unsupported image URL: {url}")
    if parsed.netloc in {"www.codenameone.com", "codenameone.com"}:
        root = static_dir.resolve()
        local = (root / urllib.parse.unquote(parsed.path).lstrip("/")).resolve()
        if not local.is_relative_to(root):
            raise ValueError("Image path escapes the static directory")
        data = local.read_bytes()
    else:
        request = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
        with urllib.request.urlopen(request, timeout=60) as response:
            data = response.read(MAX_IMAGE_BYTES + 1)
    if not data or len(data) > MAX_IMAGE_BYTES:
        raise ValueError(f"Image is empty or exceeds Foojay's 4 MB limit: {url}")
    return data


def build_bundle(post: Post, posts: list[Post], today: dt.date,
                 static_dir: Path = STATIC_DIR) -> dict[str, bytes]:
    validate_slug(post.slug)
    if str(post.front_matter.get("author", "")).strip() != "Shai Almog":
        raise ValueError("Foojay author mapping is only configured for Shai Almog")
    description = " ".join(str(post.front_matter.get("description") or "").split())
    if not description or not post.title.strip():
        raise ValueError("Foojay requires a title and description")
    if len(description) >= 160:
        description = description[:156].rsplit(" ", 1)[0] + "..."
    if not post.cover_image:
        raise ValueError("Foojay requires a preview image")
    files: dict[str, bytes] = {}
    names: dict[str, str] = {}

    def bundle_image(url: str) -> str:
        if url in names:
            return names[url]
        parsed = urllib.parse.urlsplit(url)
        suffix = Path(parsed.path).suffix.lower()
        if suffix not in {".jpg", ".jpeg", ".png", ".gif", ".webp", ".svg", ".avif"}:
            raise ValueError(f"Unsupported image format: {url}")
        # URL digest avoids collisions between different images with the same name.
        stem = re.sub(r"[^a-zA-Z0-9_-]", "-", Path(parsed.path).stem)[:80]
        name = f"{stem}-{hashlib.sha256(url.encode()).hexdigest()[:10]}{suffix}"
        files[name] = read_image(url, static_dir)
        names[url] = name
        return name

    cover = bundle_image(post.cover_image)
    cover_data = files[cover]
    is_jpeg = cover_data.startswith(b"\xff\xd8\xff")
    is_png = cover_data.startswith(b"\x89PNG\r\n\x1a\n")
    if not (is_jpeg or is_png) or (is_png and b"acTL" in cover_data):
        raise ValueError("Foojay preview image must be a still JPEG or PNG")
    if len(cover_data) > MAX_HERO_BYTES:
        raise ValueError("Compress the Foojay preview below 800 KB (recommended: below 300 KB)")
    body = render_body(post, posts, today)
    # Leave code examples alone; URLs there are source code, not image assets.
    fence: str | None = None
    lines = []
    for line in body.splitlines():
        marker = re.match(r"^\s*(`{3,}|~{3,})", line)
        if marker:
            value = marker.group(1)
            if fence is None:
                fence = value
            elif value[0] == fence[0] and len(value) >= len(fence):
                fence = None
            lines.append(line)
            continue
        if fence is None:
            line = IMAGE_RE.sub(lambda m: m[1] + bundle_image(m[2]) + m[3], line)
            line = HTML_IMAGE_RE.sub(lambda m: m[1] + bundle_image(m[2]) + m[3], line)
            if re.match(r"^#\s", line):
                raise ValueError("Foojay body headings must start at H2")
        lines.append(line)
    fields = {
        "title": post.title, "date": (post.date + dt.timedelta(days=MIN_AGE_DAYS)).isoformat(), "description": description,
        "authors": [AUTHOR], "image": cover, "categories": ["Java"],
        "canonical": post.canonical_url,
    }
    # JSON scalars/arrays are valid YAML and correctly escape quotes/newlines.
    frontmatter = "\n".join(f"{key}: {json.dumps(value, ensure_ascii=False)}"
                            for key, value in fields.items())
    files["index.md"] = ("---\n" + frontmatter + "\n---\n\n" +
                         "\n".join(lines) + "\n").encode("utf-8")
    if sum(map(len, files.values())) > MAX_BUNDLE_BYTES:
        raise ValueError("Foojay article bundle exceeds the 15 MB budget")
    return {f"draft/{post.slug}/{name}": data for name, data in files.items()}


class GitHub:
    """Use the authenticated gh CLI; credentials never enter command arguments."""

    def api(self, endpoint: str, payload: dict | None = None,
            *, optional: bool = False) -> Any:
        command = ["gh", "api", "--hostname", "github.com", endpoint]
        if payload is not None:
            command += ["--method", "POST", "--input", "-"]
        env = os.environ.copy()
        if env.get("FOOJAY_GITHUB_TOKEN"):
            env["GH_TOKEN"] = env["FOOJAY_GITHUB_TOKEN"]
        result = subprocess.run(command, input=json.dumps(payload) if payload is not None else None,
                                text=True, capture_output=True, env=env, timeout=120)
        if result.returncode:
            if optional and "HTTP 404" in result.stderr:
                return None
            raise RuntimeError(f"GitHub request failed ({endpoint}): {result.stderr.strip()}")
        return json.loads(result.stdout)


def pr_result(pr: dict) -> dict:
    if pr.get("state") == "closed" and not pr.get("merged_at"):
        raise RuntimeError(f"Foojay PR was closed without merging; review it before retrying: {pr['html_url']}")
    if not pr.get("html_url") or not pr.get("number"):
        raise RuntimeError("GitHub response missing pull-request URL/number")
    return {
        "id": pr["number"], "url": pr["html_url"], "pull_request_url": pr["html_url"],
        "status": "merged" if pr.get("merged_at") else "submitted",
        "published": False,  # merging a draft bundle is not public publication
        "syndicated_at": dt.datetime.now(dt.timezone.utc).isoformat(timespec="seconds"),
    }


def submit_bundle(post: Post, files: dict[str, bytes], head_repo: str,
                  github: GitHub) -> dict:
    validate_slug(post.slug)
    if not re.fullmatch(r"[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+", head_repo):
        raise ValueError("FOOJAY_HEAD_REPO must be owner/repository")
    branch = f"cn1-syndication/{post.slug}"
    owner = head_repo.split("/")[0]
    query = urllib.parse.urlencode({"state": "all", "head": f"{owner}:{branch}", "per_page": 100})
    existing = github.api(f"repos/{UPSTREAM}/pulls?{query}")
    for pr in existing:
        if (pr.get("head", {}).get("repo") or {}).get("full_name", "").lower() == head_repo.lower():
            # Recover a successful submission even if CN1's state commit was lost.
            return pr_result(pr)

    upstream = github.api(f"repos/{UPSTREAM}")
    head = github.api(f"repos/{head_repo}")
    if not head.get("permissions", {}).get("push"):
        raise RuntimeError(f"Token needs Contents write access to {head_repo}; see foojay-syndication.md")
    if head_repo != UPSTREAM and (head.get("source") or {}).get("full_name") != UPSTREAM:
        raise RuntimeError(f"{head_repo} must be a fork of {UPSTREAM}")
    base = upstream["default_branch"]
    commit = github.api(f"repos/{UPSTREAM}/commits/{base}")
    base_sha = commit["sha"]
    base_tree = commit["commit"]["tree"]["sha"]
    github.api(f"repos/{UPSTREAM}/contents/content/authors/{AUTHOR}/_index.md?ref={base_sha}")
    tree = github.api(f"repos/{UPSTREAM}/git/trees/{base_tree}?recursive=1")
    if tree.get("truncated"):
        raise RuntimeError("Foojay tree was truncated; cannot safely check for duplicate article slugs")
    for item in tree["tree"]:
        path = item["path"]
        if (path.startswith("content/posts/") or path.startswith("draft/")) and re.search(
                rf"/{re.escape(post.slug)}/index\.(md|adoc)$", path):
            raise RuntimeError(f"Foojay already contains {path}; reconcile state before submitting")

    # A deterministic branch also recovers a crash after commit but before PR.
    ref = github.api(f"repos/{head_repo}/git/ref/heads/{branch}", optional=True)
    if ref:
        # Never overwrite a branch or silently submit a partial/different bundle.
        branch_commit = github.api(f"repos/{head_repo}/git/commits/{ref['object']['sha']}")
        branch_tree = github.api(f"repos/{head_repo}/git/trees/{branch_commit['tree']['sha']}?recursive=1")
        expected = {path: hashlib.sha1(b"blob " + str(len(data)).encode() + b"\0" + data).hexdigest()
                    for path, data in files.items()}
        actual = {item["path"]: item["sha"] for item in branch_tree["tree"]
                  if item["path"].startswith(f"draft/{post.slug}/") and item["type"] == "blob"}
        if branch_tree.get("truncated") or actual != expected:
            raise RuntimeError(f"Existing {branch} differs from the prepared bundle; review it before retrying")
        comparison = github.api(f"repos/{head_repo}/compare/{base_sha}...{branch}")
        changed = comparison.get("files", [])
        if (len(changed) >= 300 or {item['filename'] for item in changed} != set(files)
                or any(item['status'] != 'added' for item in changed)):
            raise RuntimeError(f"Existing {branch} changes files outside the article bundle; review it before retrying")
    else:
        entries = []
        for path, data in sorted(files.items()):
            blob = github.api(f"repos/{head_repo}/git/blobs", {
                "content": base64.b64encode(data).decode("ascii"), "encoding": "base64",
            })
            entries.append({"path": path, "mode": "100644", "type": "blob", "sha": blob["sha"]})
        new_tree = github.api(f"repos/{head_repo}/git/trees", {"base_tree": base_tree, "tree": entries})
        new_commit = github.api(f"repos/{head_repo}/git/commits", {
            "message": f"Submit article: {post.title}", "tree": new_tree["sha"], "parents": [base_sha],
        })
        github.api(f"repos/{head_repo}/git/refs", {"ref": f"refs/heads/{branch}", "sha": new_commit["sha"]})
    pr = github.api(f"repos/{UPSTREAM}/pulls", {
        "title": post.title, "head": f"{owner}:{branch}", "base": base, "draft": False,
        "body": (f"Submit **{post.title}** by Shai Almog for editorial review.\n\n"
                 f"Originally published at {post.canonical_url}\n\n"
                 f"The article and its images are in `draft/{post.slug}/`. "
                 "Frontmatter includes the original canonical URL and the existing "
                 "`shai-almog` author profile. The date is a suggested day; "
                 "please set it when moving the bundle into the publication folder."),
    })
    return pr_result(pr)


def main(argv: list[str]) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    mode = parser.add_mutually_exclusive_group()
    mode.add_argument("--submit", action="store_true", help="Create a Foojay editorial PR and record it.")
    mode.add_argument("--dry-run", action="store_true", help="Print the candidate without network calls (default).")
    parser.add_argument("--output-dir", type=Path, help="Prepare a reviewable bundle here; may fetch remote body images.")
    parser.add_argument("--today", type=dt.date.fromisoformat, default=dt.date.today())
    parser.add_argument("--blog-dir", type=Path, default=BLOG_DIR)
    parser.add_argument("--static-dir", type=Path, default=STATIC_DIR)
    parser.add_argument("--state-file", type=Path, default=STATE_FILE)
    parser.add_argument("--head-repo", default=os.environ.get("FOOJAY_HEAD_REPO") or UPSTREAM)
    args = parser.parse_args(argv)
    if args.dry_run and args.output_dir:
        parser.error("Use --output-dir by itself to prepare a bundle; --dry-run is offline selection only")
    try:
        posts = discover_posts(args.blog_dir)
        state = State.load(args.state_file)
        post = select_candidate(posts, state, ["foojay"], args.today,
                                ELIGIBILITY_FLOOR, MIN_AGE_DAYS,
                                platform_filters={"foojay": accepts})
        if post is None:
            print("[foojay] No eligible Friday digest awaiting submission.")
            return 0
        print(f"[foojay] Selected {post.slug}; canonical {post.canonical_url}")
        if not args.submit and not args.output_dir:
            print(f"[foojay] Dry run: would prepare draft/{post.slug}/ and open an editorial PR.")
            return 0
        if args.submit and os.environ.get("GITHUB_ACTIONS") == "true" and not os.environ.get("FOOJAY_GITHUB_TOKEN"):
            raise RuntimeError("Set FOOJAY_GITHUB_TOKEN: the workflow GITHUB_TOKEN cannot write to Foojay or a fork")
        files = build_bundle(post, posts, args.today, args.static_dir)
        if args.output_dir:
            for relative, data in files.items():
                target = args.output_dir / relative
                target.parent.mkdir(parents=True, exist_ok=True)
                target.write_bytes(data)
            print(f"[foojay] Prepared {len(files)} files under {args.output_dir / 'draft' / post.slug}")
        if args.submit:
            result = submit_bundle(post, files, args.head_repo, GitHub())
            state.record(post.slug, "foojay", result)
            state.save(args.state_file)
            print(f"[foojay] {result['status']}: {result['url']} (editorial submission, not verified public)")
        return 0
    except (OSError, ValueError, RuntimeError, subprocess.SubprocessError) as error:
        print(f"[foojay] FAILED: {error}", file=sys.stderr)
        return 2


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
