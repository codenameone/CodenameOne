#!/usr/bin/env python3
"""Determine the current Codename One release version for documentation builds."""
from __future__ import annotations

import json
import os
import re
import subprocess
import sys
import xml.etree.ElementTree as ET
from pathlib import Path
from typing import Iterable, List
from urllib.error import HTTPError, URLError
from urllib.request import Request, urlopen


def _sanitize_tag(value: str) -> str:
    value = value.strip()
    if value.lower().startswith("refs/tags/"):
        value = value[10:]
    if value.lower().startswith("tags/"):
        value = value[5:]
    if value and value[0] in {"v", "V"} and value[1:2].isdigit():
        value = value[1:]
    return value.strip()


def _parse_version_components(version: str) -> List[int]:
    return [int(part) for part in version.split(".")]


def release_tag_from_event() -> str:
    event_path = os.environ.get("GITHUB_EVENT_PATH")
    if event_path:
        event_file = Path(event_path)
        if event_file.is_file():
            try:
                data = json.loads(event_file.read_text())
            except json.JSONDecodeError:
                data = {}
            release = data.get("release") or {}
            tag = release.get("tag_name") or release.get("target_commitish") or ""
            if tag:
                return _sanitize_tag(tag)
    for key in ("GITHUB_REF_NAME", "GITHUB_REF"):
        value = os.environ.get(key)
        if value:
            sanitized = _sanitize_tag(value)
            if sanitized:
                return sanitized
    return ""


def latest_release_from_api() -> str:
    repository = os.environ.get("GITHUB_REPOSITORY")
    if not repository:
        return ""
    api_base = os.environ.get("GITHUB_API_URL", "https://api.github.com")
    url = f"{api_base.rstrip('/')}/repos/{repository}/releases/latest"
    headers = {
        "Accept": "application/vnd.github+json",
        "User-Agent": "codenameone-docs-release-version",
    }
    token = os.environ.get("GITHUB_TOKEN") or os.environ.get("GH_TOKEN")
    if token:
        headers["Authorization"] = f"Bearer {token}"
    request = Request(url, headers=headers)
    try:
        with urlopen(request, timeout=10) as response:
            if response.status != 200:
                return ""
            try:
                payload = json.load(response)
            except json.JSONDecodeError:
                return ""
    except (HTTPError, URLError, TimeoutError):
        return ""
    tag = payload.get("tag_name")
    if not tag:
        return ""
    return _sanitize_tag(str(tag))


def latest_git_tag() -> str:
    try:
        subprocess.run(
            ["git", "fetch", "--tags", "--force"],
            check=True,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
        )
    except (subprocess.CalledProcessError, FileNotFoundError):
        pass
    try:
        result = subprocess.run(
            ["git", "tag", "--list", "v*"],
            check=True,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True,
        )
    except (subprocess.CalledProcessError, FileNotFoundError):
        return ""
    tags: Iterable[str] = (_sanitize_tag(line) for line in result.stdout.splitlines())
    numeric_tags = [tag for tag in tags if re.fullmatch(r"\d+(?:\.\d+)*", tag)]
    if not numeric_tags:
        return ""
    numeric_tags.sort(key=_parse_version_components)
    return numeric_tags[-1]


def version_from_pom(root: Path) -> str:
    pom_path = root / "maven" / "pom.xml"
    if not pom_path.is_file():
        return ""
    try:
        tree = ET.parse(pom_path)
    except ET.ParseError:
        return ""
    namespace = {"mvn": "http://maven.apache.org/POM/4.0.0"}
    version_element = tree.getroot().find("mvn:version", namespace)
    if version_element is None:
        return ""
    version = (version_element.text or "").strip()
    if not version:
        return ""
    if version.endswith("-SNAPSHOT"):
        version = version[: -len("-SNAPSHOT")]
    return version


def main() -> int:
    repo_root = Path(__file__).resolve().parents[2]

    for candidate in (release_tag_from_event, latest_release_from_api, latest_git_tag):
        version = candidate()
        if version:
            print(version)
            return 0

    version = version_from_pom(repo_root)
    if version:
        print(version)
        return 0

    return 1


if __name__ == "__main__":
    sys.exit(main())
