#!/usr/bin/env python3
"""Generate a Lunr-compatible JSON search index from rendered Hugo HTML."""

from __future__ import annotations

import json
import re
from datetime import datetime, timezone
from pathlib import Path
from html.parser import HTMLParser
from typing import Dict, List

ROOT = Path(__file__).resolve().parents[1]
PUBLIC_DIR = ROOT / "public"
OUT_FILE = PUBLIC_DIR / "lunr-index.json"

SKIP_PREFIXES = (
    "/tags/",
    "/categories/",
    "/page/",
    "/developer-guide/single-page/",
    "/search/",
    # API types and members have their own identifier index.
    "/javadoc/",
    "/backend/javadoc/",
)

# Cloudflare Pages refuses to deploy a file over 25 MiB, and the index is the text of
# every page: it reached 24.8 MiB as one file, and the next few hundred API pages put it
# over and stopped the deployment. It is therefore written in parts, each far below the
# limit, beside a small lunr-index.json that names them. The search page and
# check_developer_guide.py read the parts through that file.
PAGES_FILE_LIMIT = 25 * 1024 * 1024
PART_BYTES = 8 * 1024 * 1024

WS_RE = re.compile(r"\s+")


class PageText(HTMLParser):
    """Read both Hugo's minified attributes and normal HTML without indexing menus."""

    def __init__(self, source):
        super().__init__()
        self.stack = []
        self.date = ""
        self.redirect = False
        self.title, self.heading, self.article, self.main, self.fallback = [], [], [], [], []
        self.feed(source)

    def handle_starttag(self, tag, attrs):
        attrs = dict(attrs)
        if tag == "meta":
            if attrs.get("property") in ("article:published_time", "article:modified_time") and not self.date:
                self.date = attrs.get("content", "")
            if attrs.get("http-equiv", "").lower() == "refresh":
                self.redirect = True
        classes = attrs.get("class", "").split()
        parent = self.stack[-1][1] if self.stack else set()
        flags = set(parent)
        if tag in ("script", "style", "nav", "footer", "aside") or "cn1-guide-toc" in classes:
            flags.add("skip")
        if tag == "title":
            flags.add("title")
        if tag == "h1" and "post-title" in classes:
            flags.add("heading")
        if tag == "article" and "post-single" in classes:
            flags.add("article")
        if tag == "main":
            flags.add("main")
        if tag not in {"area", "base", "br", "col", "embed", "hr", "img", "input", "link", "meta", "param", "source", "track", "wbr"}:
            self.stack.append((tag, flags))

    def handle_startendtag(self, tag, attrs):
        self.handle_starttag(tag, attrs)
        self.handle_endtag(tag)

    def handle_endtag(self, tag):
        for i in range(len(self.stack) - 1, -1, -1):
            if self.stack[i][0] == tag:
                del self.stack[i:]
                break

    def handle_data(self, data):
        flags = self.stack[-1][1] if self.stack else set()
        if "skip" in flags:
            return
        self.fallback.append(data)
        for name in ("title", "heading", "article", "main"):
            if name in flags:
                getattr(self, name).append(data)

    @staticmethod
    def text(parts):
        return WS_RE.sub(" ", " ".join(parts)).strip()


def extract_title(html_doc: str) -> str:
    page = PageText(html_doc)
    return page.text(page.heading) or re.sub(r"\s*\|\s*Codename One\s*$", "", page.text(page.title)) or "Untitled"


def extract_main_content(html_doc: str) -> str:
    page = PageText(html_doc)
    return page.text(page.article or page.main or page.fallback)


def search_section(url: str) -> str:
    if url.startswith("/developer-guide/"):
        return "guide"
    if url.startswith("/blog/"):
        return "blog"
    return "site"


DATE_META_RE = re.compile(
    r"<meta\s+property=[\"']article:(?:published|modified)_time[\"']\s+content=[\"']([^\"']+)[\"']",
    re.I,
)
DATE_JSONLD_RE = re.compile(r"\"datePublished\"\s*:\s*\"([^\"]+)\"")


def extract_date(html_doc: str) -> str:
    m = DATE_META_RE.search(html_doc)
    if m:
        return m.group(1)
    m = DATE_JSONLD_RE.search(html_doc)
    if m:
        return m.group(1)
    return ""


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

    for html_file in sorted(PUBLIC_DIR.rglob("*.html")):
        if html_file.name != "index.html":
            continue

        url = file_to_url(html_file)
        if should_skip_url(url):
            continue

        doc = html_file.read_text(encoding="utf-8", errors="ignore")
        page = PageText(doc)
        if page.redirect:
            continue
        title = page.text(page.heading) or re.sub(r"\s*\|\s*Codename One\s*$", "", page.text(page.title)) or "Untitled"
        content = page.text(page.article or page.main or page.fallback)
        date = page.date or extract_date(doc)

        if not content or len(content) < 80:
            continue

        docs.append(
            {
                "title": title,
                "url": url,
                "date": date,
                "section": search_section(url),
                "content": content,
            }
        )

    # Newest first; undated pages sort to the end. Tie-break on URL for stable output.
    docs.sort(key=lambda d: (
        1 if not d["date"] else 0,
        -_date_sort_key(d["date"]),
        d["url"],
    ))

    for i, d in enumerate(docs):
        d["id"] = str(i)

    return {
        "generated_at_utc": datetime.now(timezone.utc).isoformat(),
        "count": len(docs),
        "docs": docs,
    }


def _date_sort_key(value: str) -> float:
    """Return a numeric sort key for an ISO-8601 date string; 0 if unparseable."""
    try:
        # Python 3.11+ parses trailing "Z" natively; older parses "+00:00" form.
        normalized = value.replace("Z", "+00:00")
        return datetime.fromisoformat(normalized).timestamp()
    except ValueError:
        return 0.0


def partition(docs: List[Dict[str, str]], budget: int) -> List[List[Dict[str, str]]]:
    """Split docs, in order, into runs whose JSON stays within budget bytes.

    A single document larger than the budget gets a part of its own rather than being
    dropped or cut: write_index is what refuses a part the host would not take.
    """
    parts: List[List[Dict[str, str]]] = []
    current: List[Dict[str, str]] = []
    size = 0
    for doc in docs:
        doc_bytes = len(json.dumps(doc, ensure_ascii=False).encode("utf-8")) + 2
        if current and size + doc_bytes > budget:
            parts.append(current)
            current = []
            size = 0
        current.append(doc)
        size += doc_bytes
    if current:
        parts.append(current)
    return parts


def write_index(payload: Dict[str, object], out_file: Path, budget: int = PART_BYTES) -> List[Path]:
    """Write the parts and the file that names them; returns every file written."""
    for stale in out_file.parent.glob(out_file.stem + "-*.json"):
        stale.unlink()
    written: List[Path] = []
    names: List[str] = []
    for number, docs in enumerate(partition(payload["docs"], budget)):
        part = out_file.with_name(f"{out_file.stem}-{number}.json")
        part.write_text(json.dumps({"docs": docs}, ensure_ascii=False) + "\n", encoding="utf-8")
        written.append(part)
        names.append("/" + part.name)
    manifest = {
        "generated_at_utc": payload["generated_at_utc"],
        "count": payload["count"],
        "parts": names,
    }
    out_file.write_text(json.dumps(manifest, ensure_ascii=False) + "\n", encoding="utf-8")
    written.append(out_file)
    for path in written:
        size = path.stat().st_size
        if size > PAGES_FILE_LIMIT:
            raise SystemExit(
                f"{path.name} is {size} bytes, over the {PAGES_FILE_LIMIT} a deployment accepts: "
                "one page's text is larger than a whole part may be")
    return written


def main() -> int:
    if not PUBLIC_DIR.exists():
        print(f"Public dir not found: {PUBLIC_DIR}")
        return 1

    payload = build_index()
    written = write_index(payload, OUT_FILE)
    largest = max(path.stat().st_size for path in written)
    print(f"Generated {OUT_FILE} with {payload['count']} searchable documents in "
          f"{len(written) - 1} part(s), the largest {largest} bytes")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
