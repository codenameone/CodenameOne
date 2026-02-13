#!/usr/bin/env python3
"""Generate a Lunr-compatible JSON search index from rendered Hugo HTML."""

from __future__ import annotations

import html
import json
import re
from datetime import datetime, timezone
from pathlib import Path
from typing import Dict, List

ROOT = Path(__file__).resolve().parents[1]
PUBLIC_DIR = ROOT / "public"
OUT_FILE = PUBLIC_DIR / "lunr-index.json"

SKIP_PREFIXES = (
    "/tags/",
    "/categories/",
    "/page/",
)

WS_RE = re.compile(r"\s+")
TAG_RE = re.compile(r"<[^>]+>")


def clean_html_text(raw: str) -> str:
    raw = re.sub(r"<script\b[^>]*>.*?</script(?:\s+[^>]*)?>", " ", raw, flags=re.I | re.S)
    raw = re.sub(r"<style\b[^>]*>.*?</style(?:\s+[^>]*)?>", " ", raw, flags=re.I | re.S)
    text = TAG_RE.sub(" ", raw)
    text = html.unescape(text)
    text = WS_RE.sub(" ", text).strip()
    return text


def extract_title(html_doc: str) -> str:
    m = re.search(r"<h1\b[^>]*class=\"[^\"]*post-title[^\"]*\"[^>]*>(.*?)</h1>", html_doc, flags=re.I | re.S)
    if m:
        return clean_html_text(m.group(1))

    m = re.search(r"<title>(.*?)</title>", html_doc, flags=re.I | re.S)
    if m:
        title = clean_html_text(m.group(1))
        title = re.sub(r"\s*\|\s*Codename One\s*$", "", title)
        return title

    return "Untitled"


def extract_main_content(html_doc: str) -> str:
    m = re.search(r"<article\b[^>]*class=\"[^\"]*post-single[^\"]*\"[^>]*>(.*?)</article>", html_doc, flags=re.I | re.S)
    chunk = m.group(1) if m else html_doc

    chunk = re.sub(r"<nav\b[^>]*>.*?</nav>", " ", chunk, flags=re.I | re.S)
    chunk = re.sub(r"<footer\b[^>]*>.*?</footer>", " ", chunk, flags=re.I | re.S)
    return clean_html_text(chunk)


def file_to_url(path: Path) -> str:
    rel = path.relative_to(PUBLIC_DIR)
    if rel.name == "index.html":
        base = "/" + str(rel.parent).replace("\\", "/")
        return "/" if base == "/." else base.rstrip("/") + "/"
    return "/" + str(rel).replace("\\", "/")


def should_skip_url(url: str) -> bool:
    return any(url.startswith(prefix) for prefix in SKIP_PREFIXES)


def build_index() -> Dict[str, object]:
    docs: List[Dict[str, str]] = []
    idx = 0

    for html_file in sorted(PUBLIC_DIR.rglob("*.html")):
        if html_file.name != "index.html":
            continue

        url = file_to_url(html_file)
        if should_skip_url(url):
            continue

        doc = html_file.read_text(encoding="utf-8", errors="ignore")
        title = extract_title(doc)
        content = extract_main_content(doc)

        if not content or len(content) < 80:
            continue

        docs.append(
            {
                "id": str(idx),
                "title": title,
                "url": url,
                "content": content,
            }
        )
        idx += 1

    return {
        "generated_at_utc": datetime.now(timezone.utc).isoformat(),
        "count": len(docs),
        "docs": docs,
    }


def main() -> int:
    if not PUBLIC_DIR.exists():
        print(f"Public dir not found: {PUBLIC_DIR}")
        return 1

    payload = build_index()
    OUT_FILE.write_text(json.dumps(payload, ensure_ascii=False) + "\n", encoding="utf-8")
    print(f"Generated {OUT_FILE} with {payload['count']} searchable documents")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
