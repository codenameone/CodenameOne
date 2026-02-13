#!/usr/bin/env python3
"""Generate Hugo data file for cn1libs from CN1Libs.xml."""

from __future__ import annotations

import json
import os
import re
import sys
from datetime import datetime, timezone
from pathlib import Path
from typing import Dict, List, Optional
from urllib.error import URLError, HTTPError
from urllib.request import Request, urlopen
import xml.etree.ElementTree as ET

SOURCE_URL = "https://raw.githubusercontent.com/codenameone/CodenameOneLibs/refs/heads/master/CN1Libs.xml"
ROOT = Path(__file__).resolve().parents[1]
OUT_PATH = ROOT / "data" / "cn1libs.json"

URL_RE = re.compile(r"https?://[^\s<>\"']+")


def _strip_ns(tag: str) -> str:
    return tag.split("}", 1)[-1].lower()


def _clean(text: Optional[str]) -> str:
    if not text:
        return ""
    text = re.sub(r"\s+", " ", text).strip()
    return text


def _text_map(elem: ET.Element) -> Dict[str, str]:
    fields: Dict[str, str] = {}
    for child in list(elem):
        key = _strip_ns(child.tag)
        value = _clean(child.text)
        if key and value:
            fields[key] = value
    return fields


def _find_first_url(fields: Dict[str, str], elem: ET.Element) -> str:
    preferred = (
        "url",
        "source",
        "repo",
        "repository",
        "github",
        "projecturl",
        "project_url",
        "website",
        "homepage",
        "download",
    )
    for key in preferred:
        value = fields.get(key, "")
        if value.startswith("http"):
            return value

    for value in fields.values():
        match = URL_RE.search(value)
        if match:
            return match.group(0)

    # Fallback: search inside XML fragment
    xml_text = ET.tostring(elem, encoding="unicode")
    match = URL_RE.search(xml_text)
    return match.group(0) if match else ""


def _candidate_nodes(root: ET.Element) -> List[ET.Element]:
    candidates: List[ET.Element] = []
    for elem in root.iter():
        if elem is root:
            continue
        children = list(elem)
        if not children:
            continue

        fields = _text_map(elem)
        has_name = any(k in fields for k in ("name", "title", "library", "id", "artifactid", "artifact_id"))
        has_url = bool(_find_first_url(fields, elem))

        if has_name or has_url:
            candidates.append(elem)

    # Prefer shallow candidates if overly noisy
    if len(candidates) > 300:
        shallow = [c for c in candidates if len(list(c)) >= 2 and c in list(root)]
        if shallow:
            return shallow
    return candidates


def _extract_lib(elem: ET.Element) -> Optional[Dict[str, str]]:
    fields = _text_map(elem)
    name = (
        fields.get("name")
        or fields.get("title")
        or fields.get("library")
        or fields.get("artifactid")
        or fields.get("artifact_id")
        or fields.get("id")
        or ""
    )
    name = _clean(name)

    url = _find_first_url(fields, elem)
    description = (
        fields.get("description")
        or fields.get("desc")
        or fields.get("summary")
        or fields.get("details")
        or ""
    )
    description = _clean(description)

    author = _clean(
        fields.get("author")
        or fields.get("maintainer")
        or fields.get("owner")
        or fields.get("publisher")
        or ""
    )
    version = _clean(fields.get("version") or fields.get("latestversion") or fields.get("latest_version") or "")
    category = _clean(fields.get("category") or fields.get("type") or fields.get("group") or "")

    if not name and url:
        name = url.rstrip("/").split("/")[-1]

    if not name:
        return None

    return {
        "name": name,
        "url": url,
        "description": description,
        "author": author,
        "version": version,
        "category": category,
    }


def parse_xml(xml_text: str) -> List[Dict[str, str]]:
    root = ET.fromstring(xml_text)
    nodes = _candidate_nodes(root)

    libs: List[Dict[str, str]] = []
    seen = set()

    for node in nodes:
        item = _extract_lib(node)
        if not item:
            continue

        key = (item["name"].lower(), item["url"].lower())
        if key in seen:
            continue
        seen.add(key)
        libs.append(item)

    libs.sort(key=lambda x: x["name"].lower())
    return libs


def fetch_xml(url: str) -> str:
    req = Request(
        url,
        headers={
            "User-Agent": "cn1-website-generator/1.0",
            "Accept": "application/xml,text/xml,*/*",
        },
    )
    with urlopen(req, timeout=20) as resp:  # nosec B310 - fixed trusted URL
        return resp.read().decode("utf-8", errors="replace")


def write_data(libs: List[Dict[str, str]], status: str, error: str = "") -> None:
    payload = {
        "source_url": SOURCE_URL,
        "generated_at_utc": datetime.now(timezone.utc).isoformat(),
        "status": status,
        "error": error,
        "count": len(libs),
        "libs": libs,
    }
    OUT_PATH.parent.mkdir(parents=True, exist_ok=True)
    OUT_PATH.write_text(json.dumps(payload, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def main() -> int:
    try:
        xml_text = fetch_xml(SOURCE_URL)
        libs = parse_xml(xml_text)
        write_data(libs, status="ok")
        print(f"Generated {OUT_PATH} with {len(libs)} cn1libs entries")
        return 0
    except (URLError, HTTPError, TimeoutError, ET.ParseError) as err:
        if OUT_PATH.exists():
            print(f"Warning: failed to refresh cn1libs ({err}); keeping existing data at {OUT_PATH}", file=sys.stderr)
            return 0

        write_data([], status="error", error=str(err))
        print(f"Warning: failed to refresh cn1libs ({err}); wrote empty fallback data", file=sys.stderr)
        return 0


if __name__ == "__main__":
    raise SystemExit(main())
