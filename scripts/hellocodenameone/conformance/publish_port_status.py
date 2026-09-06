#!/usr/bin/env python3
"""Publish an accepted report, retrying transport failures and CAS conflicts only."""
import base64
from datetime import datetime
import json
from pathlib import Path
import re
import subprocess
import sys
import time


class ApiError(RuntimeError):
    def __init__(self, message, status=None):
        super().__init__(message)
        self.status = status


def api(endpoint, method="GET", payload=None):
    command = ["gh", "api", "--include", "--method", method, endpoint]
    if payload is not None:
        command += ["--input", "-"]
    for attempt in range(3):
        try:
            result = subprocess.run(command, input=json.dumps(payload) if payload is not None else None,
                                    text=True, capture_output=True, timeout=60)
        except subprocess.TimeoutExpired:
            result = subprocess.CompletedProcess(command, 1, "", "API request timed out after 60s")
        # --include preserves the status even when GitHub returns a non-JSON body.
        match = re.match(r"HTTP/\S+ (\d+)[^\n]*\n", result.stdout)
        status = int(match[1]) if match else None
        body = re.split(r"\r?\n\r?\n", result.stdout, maxsplit=1)
        detail = result.stderr.strip() or f"HTTP {status}: invalid or empty API response"
        if result.returncode == 0 and status is not None and 200 <= status < 300:
            try:
                value = json.loads(body[1])
                if not isinstance(value, dict):
                    raise ValueError("expected a JSON object")
                return value
            except (IndexError, ValueError):
                detail = f"HTTP {status}: invalid or empty JSON response"
                transient = True
        else:
            transient = status is None or status in (408, 429, 500, 502, 503, 504)
        if not transient or attempt == 2:
            raise ApiError(f"{method} {endpoint}: {detail}", status)
        print(f"Transient API failure ({detail}); retrying {method} in {2 ** (attempt + 1)}s",
              file=sys.stderr)
        time.sleep(2 ** (attempt + 1))
    raise ApiError(f"{method} {endpoint}: API retry loop exhausted")


def stamp(value):
    parsed = datetime.fromisoformat(value.replace("Z", "+00:00"))
    if parsed.tzinfo is None:
        raise ValueError("report timestamp must include a timezone")
    return parsed


def publish(report, repo):
    branch = "port-status-data"
    root = f"repos/{repo}"
    ref = f"{root}/git/ref/heads/{branch}"
    try:
        api(ref)
    except ApiError as error:
        if error.status != 404:
            raise
        default = api(root)["default_branch"]
        sha = api(f"{root}/git/ref/heads/{default}")["object"]["sha"]
        try:
            api(f"{root}/git/refs", "POST", {"ref": f"refs/heads/{branch}", "sha": sha})
        except ApiError as conflict:
            if conflict.status not in (409, 422):
                raise
            api(ref)  # A concurrent creator must actually have created the branch.

    target = f"ports/{report['port']}.json"
    endpoint = f"{root}/contents/{target}"
    generated = stamp(report["generated_at"])
    for attempt in range(3):
        payload = {"message": f"Update {report['port']} compliance status", "branch": branch,
                   "content": base64.b64encode(json.dumps(report).encode()).decode()}
        try:
            existing = api(f"{endpoint}?ref={branch}")
        except ApiError as error:
            if error.status != 404:
                raise  # Never treat a failed read as an absent report.
        else:
            stored = json.loads(base64.b64decode(existing["content"]))
            if stamp(stored["generated_at"]) >= generated:
                print(f"Not publishing {target}: the branch already holds an equal or newer report.")
                return
            payload["sha"] = existing["sha"]
        try:
            api(endpoint, "PUT", payload)
            print(f"Published {target} on {branch}.")
            return
        except ApiError as error:
            if error.status not in (409, 422) or attempt == 2:
                raise
            # 422 can mean a concurrently created file now requires its SHA.
            # Re-read both SHA and timestamp so a newer report cannot be replaced.
            print(f"Report update conflict (HTTP {error.status}); re-reading {target}.", file=sys.stderr)
            time.sleep(2 ** (attempt + 1))


if __name__ == "__main__":
    try:
        publish(json.loads(Path(sys.argv[1]).read_text()), sys.argv[2])
    except (ApiError, ValueError, KeyError, OSError) as error:
        print(f"Port-status publication failed: {error}", file=sys.stderr)
        sys.exit(1)
