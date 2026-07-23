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
STATE_RELEASE_TAG = "oss-traction-state"
STATE_RELEASE_NAME = "OSS traction aggregate state"


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
        raise GitHubApiError(
            f"GitHub returned HTTP {error.code} for {url}: {detail}"
        ) from error
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


def load_snapshot_history(repository: str, token: str) -> tuple[int | None, list[dict]]:
    releases = paged_rest(f"/repos/{repository}/releases", token)
    matches = [
        release
        for release in releases
        if release.get("draft") and release.get("tag_name") == STATE_RELEASE_TAG
    ]
    if not matches:
        return None, []
    if len(matches) > 1:
        raise GitHubApiError("Found multiple OSS traction state draft releases")

    release = matches[0]
    try:
        state = json.loads(release.get("body") or "{}")
        if state.get("schema_version") != 1:
            raise ValueError("unsupported state schema")
        snapshots = state["snapshots"]
        release_id = int(release["id"])
    except (KeyError, TypeError, ValueError, json.JSONDecodeError) as error:
        raise GitHubApiError(
            f"Unable to read OSS traction draft state: {error}"
        ) from error
    if not isinstance(snapshots, list):
        raise GitHubApiError("OSS traction draft state snapshots must be a list")
    return release_id, snapshots


def update_snapshot_history(
    history: list[dict], report: dict, now: dt.datetime
) -> list[dict]:
    earliest = now.astimezone(dt.timezone.utc) - dt.timedelta(days=100)
    retained: dict[str, dict] = {}
    for snapshot in history:
        try:
            captured_at = parse_timestamp(snapshot["captured_at"])
            stars = int(snapshot["repository_metrics"]["stars"])
        except (KeyError, TypeError, ValueError):
            continue
        if earliest <= captured_at < now:
            timestamp = captured_at.isoformat(timespec="seconds")
            retained[timestamp] = {
                "captured_at": timestamp,
                "repository_metrics": {"stars": stars},
            }

    current_timestamp = parse_timestamp(report["captured_at"]).isoformat(
        timespec="seconds"
    )
    retained[current_timestamp] = {
        "captured_at": current_timestamp,
        "repository_metrics": {
            "stars": int(report["repository_metrics"]["stars"])
        },
    }
    return [retained[key] for key in sorted(retained)]


def save_snapshot_history(
    repository: str,
    token: str,
    release_id: int | None,
    history: list[dict],
) -> None:
    body = json.dumps(
        {"schema_version": 1, "snapshots": history},
        indent=2,
        sort_keys=True,
    )
    if release_id is None:
        api_request(
            f"{API_ROOT}/repos/{repository}/releases",
            token,
            method="POST",
            payload={
                "tag_name": STATE_RELEASE_TAG,
                "name": STATE_RELEASE_NAME,
                "body": body,
                "draft": True,
                "prerelease": False,
            },
        )
        return
    api_request(
        f"{API_ROOT}/repos/{repository}/releases/{release_id}",
        token,
        method="PATCH",
        payload={"body": body},
    )


def calculate_star_growth(
    current_stars: int,
    now: dt.datetime,
    snapshots: list[dict],
    windows: tuple[int, ...] = (7, 28, 90),
) -> dict[str, dict]:
    now = now.astimezone(dt.timezone.utc)
    baselines: list[tuple[dt.datetime, int]] = []
    for snapshot in snapshots:
        try:
            captured_at = parse_timestamp(snapshot["captured_at"])
            stars = int(snapshot["repository_metrics"]["stars"])
        except (KeyError, TypeError, ValueError):
            continue
        if captured_at < now:
            baselines.append((captured_at, stars))

    growth: dict[str, dict] = {}
    schedule_tolerance = dt.timedelta(hours=6)
    for days in windows:
        target = now - dt.timedelta(days=days)
        eligible = [item for item in baselines if item[0] <= target + schedule_tolerance]
        key = f"last_{days}_days"
        if not eligible:
            growth[key] = {"available": False}
            continue
        captured_at, baseline_stars = max(eligible, key=lambda item: item[0])
        elapsed_days = (now - captured_at).total_seconds() / 86400
        growth[key] = {
            "available": True,
            "change": current_stars - baseline_stars,
            "baseline_stars": baseline_stars,
            "baseline_at": captured_at.isoformat(timespec="seconds"),
            "period_days": round(elapsed_days, 1),
        }
    return growth


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
    history: list[dict] | None = None,
    history_available: bool = True,
    history_errors: list[str] | None = None,
) -> dict:
    try:
        owner, name = repository.split("/", 1)
    except ValueError as error:
        raise ValueError("repository must use owner/name format") from error
    cutoff = now - dt.timedelta(days=28)
    metadata = api_request(f"{API_ROOT}/repos/{repository}", repository_token)

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
        "schema_version": 2,
        "captured_at": now.astimezone(dt.timezone.utc).isoformat(timespec="seconds"),
        "repository": repository,
        "repository_metrics": {
            "stars": metadata["stargazers_count"],
            "forks": metadata["forks_count"],
            "subscribers": metadata["subscribers_count"],
            "open_issues": metadata["open_issues_count"],
            "pushed_at": metadata["pushed_at"],
        },
        "star_growth": {
            "source": "aggregate_snapshots",
            "history_available": history_available,
            "history_errors": history_errors or [],
            "windows": calculate_star_growth(
                metadata["stargazers_count"], now, history or []
            ),
        },
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
    growth = report["star_growth"]
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
    ]
    for days in (7, 28, 90):
        window = growth["windows"][f"last_{days}_days"]
        if window["available"]:
            change = f"{window['change']:+d} over {window['period_days']:.1f} days"
        else:
            change = "N/A (baseline not available yet)"
        lines.append(f"| Aggregate star growth, {days}-day target | {change} |")
    lines.extend(
        [
            f"| New issue authors, last 28 days | {engagement['unique_issue_authors']} |",
            f"| New discussion authors, last 28 days | {engagement['unique_discussion_authors']} |",
            "",
        ]
    )
    if not growth["history_available"]:
        lines.extend(
            [
                "Prior snapshot history was unavailable, so this run could not calculate star growth.",
                "",
            ]
        )
    elif not any(window["available"] for window in growth["windows"].values()):
        lines.extend(
            [
                "This snapshot establishes the aggregate star baseline; growth appears after enough history accumulates.",
                "",
            ]
        )
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
        release_id, history = load_snapshot_history(args.repository, repository_token)
        report = collect_snapshot(
            args.repository,
            repository_token,
            now,
            traffic_token=traffic_token,
            history=history,
        )
        updated_history = update_snapshot_history(history, report, now)
        save_snapshot_history(
            args.repository, repository_token, release_id, updated_history
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
