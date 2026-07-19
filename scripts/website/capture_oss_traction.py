#!/usr/bin/env python3
"""Capture a repeatable GitHub snapshot for the Codename One OSS funnel."""

from __future__ import annotations

import argparse
import datetime as dt
import json
import os
import sys
import urllib.error
import urllib.parse
import urllib.request
from pathlib import Path


API_ROOT = "https://api.github.com"
GRAPHQL_URL = "https://api.github.com/graphql"
STAR_ACCEPT = "application/vnd.github.star+json"


class GitHubApiError(RuntimeError):
    """Raised when GitHub returns a response the snapshot cannot use."""


def parse_timestamp(value: str) -> dt.datetime:
    parsed = dt.datetime.fromisoformat(value.replace("Z", "+00:00"))
    if parsed.tzinfo is None:
        raise ValueError(f"timestamp has no timezone: {value}")
    return parsed.astimezone(dt.timezone.utc)


def api_request(
    url: str,
    token: str,
    *,
    method: str = "GET",
    payload: dict | None = None,
    accept: str = "application/vnd.github+json",
) -> object:
    data = json.dumps(payload).encode("utf-8") if payload is not None else None
    request = urllib.request.Request(
        url,
        data=data,
        method=method,
        headers={
            "Accept": accept,
            "Authorization": f"Bearer {token}",
            "Content-Type": "application/json",
            "User-Agent": "codenameone-oss-traction",
            "X-GitHub-Api-Version": "2022-11-28",
        },
    )
    try:
        with urllib.request.urlopen(request, timeout=30) as response:
            return json.load(response)
    except urllib.error.HTTPError as error:
        detail = error.read().decode("utf-8", errors="replace")
        raise GitHubApiError(f"GitHub returned HTTP {error.code} for {url}: {detail}") from error
    except urllib.error.URLError as error:
        raise GitHubApiError(f"Unable to reach GitHub for {url}: {error}") from error


def paged_rest(path: str, token: str, *, accept: str = "application/vnd.github+json") -> list:
    separator = "&" if "?" in path else "?"
    page = 1
    items: list = []
    while True:
        url = f"{API_ROOT}{path}{separator}per_page=100&page={page}"
        batch = api_request(url, token, accept=accept)
        if not isinstance(batch, list):
            raise GitHubApiError(f"Expected a list from {url}")
        items.extend(batch)
        if len(batch) < 100:
            return items
        page += 1


def count_windows(
    timestamps: list[dt.datetime], now: dt.datetime, windows: tuple[int, ...] = (7, 28, 90)
) -> dict[str, int]:
    now = now.astimezone(dt.timezone.utc)
    return {
        f"last_{days}_days": sum(now - timestamp <= dt.timedelta(days=days) for timestamp in timestamps)
        for days in windows
    }


def unique_logins(items: list[dict], field: str) -> int:
    logins = {
        item.get(field, {}).get("login")
        for item in items
        if isinstance(item.get(field), dict) and item[field].get("login")
    }
    return len(logins)


def optional_rest(path: str, token: str) -> dict:
    try:
        data = api_request(f"{API_ROOT}{path}", token)
        return {"available": True, "data": data}
    except GitHubApiError as error:
        return {"available": False, "error": str(error)}


def collect_traffic(repository: str, token: str) -> dict:
    return {
        "views": optional_rest(f"/repos/{repository}/traffic/views", token),
        "clones": optional_rest(f"/repos/{repository}/traffic/clones", token),
        "referrers": optional_rest(
            f"/repos/{repository}/traffic/popular/referrers", token
        ),
    }


def collect_discussions(owner: str, name: str, token: str, cutoff: dt.datetime) -> list[dict]:
    query = """
      query($owner: String!, $name: String!, $cursor: String) {
        repository(owner: $owner, name: $name) {
          discussions(first: 100, after: $cursor,
            orderBy: {field: CREATED_AT, direction: DESC}) {
            nodes { createdAt author { login } }
            pageInfo { hasNextPage endCursor }
          }
        }
      }
    """
    cursor = None
    collected: list[dict] = []
    while True:
        result = api_request(
            GRAPHQL_URL,
            token,
            method="POST",
            payload={
                "query": query,
                "variables": {"owner": owner, "name": name, "cursor": cursor},
            },
        )
        if not isinstance(result, dict) or result.get("errors"):
            raise GitHubApiError(f"Unable to read discussions: {result}")
        connection = result["data"]["repository"]["discussions"]
        nodes = connection["nodes"]
        for node in nodes:
            if parse_timestamp(node["createdAt"]) >= cutoff:
                collected.append(node)
        if not connection["pageInfo"]["hasNextPage"]:
            return collected
        if nodes and parse_timestamp(nodes[-1]["createdAt"]) < cutoff:
            return collected
        cursor = connection["pageInfo"]["endCursor"]


def collect_snapshot(
    repository: str,
    repository_token: str,
    now: dt.datetime,
    traffic_token: str | None = None,
) -> dict:
    try:
        owner, name = repository.split("/", 1)
    except ValueError as error:
        raise ValueError("repository must use owner/name format") from error
    cutoff = now - dt.timedelta(days=28)
    metadata = api_request(f"{API_ROOT}/repos/{repository}", repository_token)

    stargazers = paged_rest(
        f"/repos/{repository}/stargazers", repository_token, accept=STAR_ACCEPT
    )
    star_times = [
        parse_timestamp(item["starred_at"])
        for item in stargazers
        if isinstance(item, dict) and item.get("starred_at")
    ]

    issue_candidates = paged_rest(
        "/repos/"
        + repository
        + "/issues?state=all&since="
        + urllib.parse.quote(cutoff.isoformat()),
        repository_token,
    )
    new_issues = [
        item
        for item in issue_candidates
        if "pull_request" not in item and parse_timestamp(item["created_at"]) >= cutoff
    ]
    discussions = collect_discussions(owner, name, repository_token, cutoff)
    traffic = collect_traffic(repository, traffic_token or repository_token)

    return {
        "schema_version": 1,
        "captured_at": now.astimezone(dt.timezone.utc).isoformat(timespec="seconds"),
        "repository": repository,
        "repository_metrics": {
            "stars": metadata["stargazers_count"],
            "forks": metadata["forks_count"],
            "subscribers": metadata["subscribers_count"],
            "open_issues": metadata["open_issues_count"],
            "pushed_at": metadata["pushed_at"],
        },
        "star_velocity": count_windows(star_times, now),
        "engagement_last_28_days": {
            "issues_created": len(new_issues),
            "unique_issue_authors": unique_logins(new_issues, "user"),
            "discussions_created": len(discussions),
            "unique_discussion_authors": unique_logins(discussions, "author"),
        },
        "traffic_last_14_days": traffic,
    }


def render_summary(report: dict) -> str:
    repo = report["repository_metrics"]
    stars = report["star_velocity"]
    engagement = report["engagement_last_28_days"]
    traffic = report["traffic_last_14_days"]
    lines = [
        "# OSS traction snapshot",
        "",
        f"Captured: `{report['captured_at']}`",
        "",
        "| Metric | Value |",
        "| --- | ---: |",
        f"| Total stars | {repo['stars']} |",
        f"| Stars, last 7 days | {stars['last_7_days']} |",
        f"| Stars, last 28 days | {stars['last_28_days']} |",
        f"| Stars, last 90 days | {stars['last_90_days']} |",
        f"| New issue authors, last 28 days | {engagement['unique_issue_authors']} |",
        f"| New discussion authors, last 28 days | {engagement['unique_discussion_authors']} |",
        "",
    ]
    views = traffic["views"]
    referrers = traffic["referrers"]
    if views["available"]:
        lines.append(f"GitHub reports {views['data']['uniques']} unique repository visitors in its rolling 14-day window.")
    else:
        lines.append("GitHub traffic was unavailable to this token; configure `OSS_TRACTION_TOKEN` with repository Administration read access.")
    if referrers["available"] and referrers["data"]:
        rendered = ", ".join(
            f"{item['referrer']} ({item['uniques']} unique)" for item in referrers["data"]
        )
        lines.extend(["", f"Top referrers: {rendered}."])
    lines.extend(
        [
            "",
            "> Clone counts are context only. CI and automation can dominate them, so they are not an OSS success metric.",
            "",
        ]
    )
    return "\n".join(lines)


def parse_args(argv: list[str]) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--repository", default="codenameone/CodenameOne")
    parser.add_argument("--output", required=True, help="Path for the JSON snapshot.")
    parser.add_argument("--summary", help="Optional Markdown summary path.")
    parser.add_argument("--now", help="Override the UTC timestamp for testing.")
    return parser.parse_args(argv)


def resolve_tokens(environ: dict[str, str]) -> tuple[str | None, str | None]:
    repository_token = environ.get("GITHUB_TOKEN") or environ.get("GH_TOKEN")
    traffic_token = environ.get("OSS_TRACTION_TOKEN") or repository_token
    return repository_token, traffic_token


def main(argv: list[str]) -> int:
    args = parse_args(argv)
    repository_token, traffic_token = resolve_tokens(os.environ)
    if not repository_token:
        print("Set GH_TOKEN or GITHUB_TOKEN before capturing a snapshot.", file=sys.stderr)
        return 2
    now = parse_timestamp(args.now) if args.now else dt.datetime.now(dt.timezone.utc)
    try:
        report = collect_snapshot(
            args.repository, repository_token, now, traffic_token=traffic_token
        )
    except (GitHubApiError, KeyError, TypeError, ValueError) as error:
        print(f"Unable to capture OSS traction: {error}", file=sys.stderr)
        return 1
    output = Path(args.output)
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
    summary = render_summary(report)
    if args.summary:
        summary_path = Path(args.summary)
        summary_path.parent.mkdir(parents=True, exist_ok=True)
        summary_path.write_text(summary, encoding="utf-8")
    print(summary)
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
