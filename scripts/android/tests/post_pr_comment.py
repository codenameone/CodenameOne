#!/usr/bin/env python3
"""Publish screenshot comparison feedback as a pull request comment."""

from __future__ import annotations

import argparse
import json
import os
import pathlib
import re
import shutil
import subprocess
import sys
from typing import Dict, List, Optional
from urllib.request import Request, urlopen

MARKER = "<!-- CN1SS_SCREENSHOT_COMMENT -->"
LOG_PREFIX = "[run-android-instrumentation-tests]"


def log(message: str) -> None:
    print(f"{LOG_PREFIX} {message}", file=sys.stdout)


def err(message: str) -> None:
    print(f"{LOG_PREFIX} {message}", file=sys.stderr)


def load_event(path: pathlib.Path) -> Dict[str, object]:
    return json.loads(path.read_text(encoding="utf-8"))


def find_pr_number(event: Dict[str, object]) -> Optional[int]:
    if "pull_request" in event:
        pr_data = event.get("pull_request")
        if isinstance(pr_data, dict):
            number = pr_data.get("number")
            if isinstance(number, int):
                return number
    issue = event.get("issue")
    if isinstance(issue, dict) and issue.get("pull_request"):
        number = issue.get("number")
        if isinstance(number, int):
            return number
    return None


def next_link(header: Optional[str]) -> Optional[str]:
    if not header:
        return None
    for part in header.split(","):
        segment = part.strip()
        if segment.endswith('rel="next"'):
            url_part = segment.split(";", 1)[0].strip()
            if url_part.startswith("<") and url_part.endswith(">"):
                return url_part[1:-1]
    return None


def publish_previews_to_branch(
    preview_dir: Optional[pathlib.Path],
    repo: str,
    pr_number: int,
    token: str,
    allow_push: bool,
) -> Dict[str, str]:
    """Publish preview images to the cn1ss-previews branch and return name->URL."""

    if not preview_dir or not preview_dir.exists():
        return {}

    image_files = [
        path
        for path in sorted(preview_dir.iterdir())
        if path.is_file() and path.suffix.lower() in {".jpg", ".jpeg", ".png"}
    ]
    if not image_files:
        return {}
    if not allow_push:
        log("Preview publishing skipped for forked PR")
        return {}
    if not repo or not token:
        return {}

    workspace = pathlib.Path(os.environ.get("GITHUB_WORKSPACE", ".")).resolve()
    worktree = workspace / f".cn1ss-previews-pr-{pr_number}"
    if worktree.exists():
        shutil.rmtree(worktree)
    worktree.mkdir(parents=True, exist_ok=True)

    try:
        env = os.environ.copy()
        env.setdefault("GIT_TERMINAL_PROMPT", "0")

        def run_git(args: List[str], check: bool = True) -> subprocess.CompletedProcess[str]:
            result = subprocess.run(
                ["git", *args],
                cwd=worktree,
                env=env,
                capture_output=True,
                text=True,
            )
            if check and result.returncode != 0:
                raise RuntimeError(
                    f"git {' '.join(args)} failed: {result.stderr.strip() or result.stdout.strip()}"
                )
            return result

        run_git(["init"])
        actor = os.environ.get("GITHUB_ACTOR", "github-actions") or "github-actions"
        run_git(["config", "user.name", actor])
        run_git(["config", "user.email", "github-actions@users.noreply.github.com"])
        remote_url = f"https://x-access-token:{token}@github.com/{repo}.git"
        run_git(["remote", "add", "origin", remote_url])

        has_branch = run_git(["ls-remote", "--heads", "origin", "cn1ss-previews"], check=False)
        if has_branch.returncode == 0 and has_branch.stdout.strip():
            run_git(["fetch", "origin", "cn1ss-previews"])
            run_git(["checkout", "cn1ss-previews"])
        else:
            run_git(["checkout", "--orphan", "cn1ss-previews"])

        dest = worktree / f"pr-{pr_number}"
        if dest.exists():
            shutil.rmtree(dest)
        dest.mkdir(parents=True, exist_ok=True)

        for source in image_files:
            shutil.copy2(source, dest / source.name)

        run_git(["add", "-A", "."])
        status = run_git(["status", "--porcelain"])
        if status.stdout.strip():
            run_git(["commit", "-m", f"Add previews for PR #{pr_number}"])
            push = run_git(["push", "origin", "HEAD:cn1ss-previews"], check=False)
            if push.returncode != 0:
                raise RuntimeError(push.stderr.strip() or push.stdout.strip())
            log(f"Published {len(image_files)} preview(s) to cn1ss-previews/pr-{pr_number}")
        else:
            log(f"Preview branch already up-to-date for PR #{pr_number}")

        raw_base = f"https://raw.githubusercontent.com/{repo}/cn1ss-previews/pr-{pr_number}"
        urls: Dict[str, str] = {}
        if dest.exists():
            for file in sorted(dest.iterdir()):
                if file.is_file():
                    urls[file.name] = f"{raw_base}/{file.name}"
        return urls
    finally:
        shutil.rmtree(worktree, ignore_errors=True)


def replace_attachments(body: str, urls: Dict[str, str]) -> tuple[str, List[str]]:
    attachment_pattern = re.compile(r"\(attachment:([^)]+)\)")
    missing: List[str] = []

    def repl(match: re.Match[str]) -> str:
        name = match.group(1)
        url = urls.get(name)
        if url:
            return f"({url})"
        missing.append(name)
        log(f"Preview URL missing for {name}; leaving placeholder")
        return "(#)"

    return attachment_pattern.sub(repl, body), missing


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--body", required=True, help="Path to markdown body to publish")
    parser.add_argument(
        "--preview-dir",
        help="Directory containing preview images referenced by attachment placeholders",
    )
    args = parser.parse_args()

    body_path = pathlib.Path(args.body)
    if not body_path.is_file():
        return 0

    raw_body = body_path.read_text(encoding="utf-8")
    body = raw_body.strip()
    if not body:
        return 0

    if MARKER not in body:
        body = body.rstrip() + "\n\n" + MARKER

    body_without_marker = body.replace(MARKER, "").strip()
    if not body_without_marker:
        return 0

    event_path_env = os.environ.get("GITHUB_EVENT_PATH")
    repo = os.environ.get("GITHUB_REPOSITORY")
    token = os.environ.get("GITHUB_TOKEN")
    if not event_path_env or not repo or not token:
        return 0

    event_path = pathlib.Path(event_path_env)
    if not event_path.is_file():
        return 0

    event = load_event(event_path)
    pr_number = find_pr_number(event)
    if not pr_number:
        return 0

    headers = {
        "Authorization": f"token {token}",
        "Accept": "application/vnd.github+json",
        "Content-Type": "application/json",
    }

    pr_data = event.get("pull_request")
    is_fork_pr = False
    if isinstance(pr_data, dict):
        head = pr_data.get("head")
        if isinstance(head, dict):
            head_repo = head.get("repo")
            if isinstance(head_repo, dict):
                is_fork_pr = bool(head_repo.get("fork"))

    comments_url = f"https://api.github.com/repos/{repo}/issues/{pr_number}/comments?per_page=100"
    existing_comment: Optional[Dict[str, object]] = None
    preferred_comment: Optional[Dict[str, object]] = None
    actor = os.environ.get("GITHUB_ACTOR")
    preferred_logins = {login for login in (actor, "github-actions[bot]") if login}

    while comments_url:
        req = Request(comments_url, headers=headers)
        with urlopen(req) as resp:
            comments = json.load(resp)
            for comment in comments:
                body_text = comment.get("body") or ""
                if MARKER in body_text:
                    existing_comment = comment
                    login = comment.get("user", {}).get("login")
                    if login in preferred_logins:
                        preferred_comment = comment
            comments_url = next_link(resp.headers.get("Link"))

    if preferred_comment is not None:
        existing_comment = preferred_comment

    comment_id: Optional[int] = None
    created_placeholder = False

    if existing_comment is not None:
        cid = existing_comment.get("id")
        if isinstance(cid, int):
            comment_id = cid
    else:
        create_payload = json.dumps({"body": MARKER}).encode("utf-8")
        create_req = Request(
            f"https://api.github.com/repos/{repo}/issues/{pr_number}/comments",
            data=create_payload,
            headers=headers,
            method="POST",
        )
        with urlopen(create_req) as resp:
            created = json.load(resp)
            cid = created.get("id")
            if isinstance(cid, int):
                comment_id = cid
        created_placeholder = comment_id is not None
        if created_placeholder:
            log(f"Created new screenshot comment placeholder (id={comment_id})")

    if comment_id is None:
        return 1

    preview_dir = pathlib.Path(args.preview_dir).resolve() if args.preview_dir else None
    attachment_urls: Dict[str, str] = {}
    if "(attachment:" in body:
        try:
            attachment_urls = publish_previews_to_branch(
                preview_dir,
                repo,
                pr_number,
                token,
                allow_push=not is_fork_pr,
            )
            for name, url in attachment_urls.items():
                log(f"Preview available for {name}: {url}")
        except Exception as exc:  # pragma: no cover - defensive logging
            err(f"Preview publishing failed: {exc}")
            return 1

    final_body, missing = replace_attachments(body, attachment_urls)
    if missing and not is_fork_pr:
        err(f"Failed to resolve preview URLs for: {', '.join(sorted(set(missing)))}")
        return 1
    if missing and is_fork_pr:
        log("Preview URLs unavailable in forked PR context; placeholders left as-is")

    update_payload = json.dumps({"body": final_body}).encode("utf-8")
    update_req = Request(
        f"https://api.github.com/repos/{repo}/issues/comments/{comment_id}",
        data=update_payload,
        headers=headers,
        method="PATCH",
    )

    with urlopen(update_req) as resp:
        resp.read()
        action = "updated"
        if created_placeholder:
            action = "posted"
        log(f"PR comment {action} (status={resp.status}, bytes={len(update_payload)})")

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
