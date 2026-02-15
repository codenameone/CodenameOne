#!/usr/bin/env python3
"""Lint internal links, anchors, and asset references in a built static site."""

from __future__ import annotations

import argparse
import re
import sys
from dataclasses import dataclass
from html.parser import HTMLParser
from pathlib import Path
from typing import Dict, Iterable, List, Optional, Set, Tuple
from urllib.parse import unquote, urlsplit


IGNORED_SCHEMES = {
    "http",
    "https",
    "mailto",
    "tel",
    "sms",
    "javascript",
    "data",
    "blob",
    "ws",
    "wss",
}

IGNORED_PATH_PREFIXES = (
    "/cdn-cgi/l/email-protection",
    "/livereload.js",
    "/files",
    "/javadoc",
    "/ota",
    "/demos",
    "/downloads",
)

REF_ATTRS = {
    ("a", "href"),
    ("img", "src"),
    ("img", "srcset"),
    ("source", "src"),
    ("source", "srcset"),
    ("script", "src"),
    ("link", "href"),
    ("video", "poster"),
    ("audio", "src"),
}


@dataclass
class Ref:
    tag: str
    attr: str
    value: str
    line: int


class PageParser(HTMLParser):
    def __init__(self) -> None:
        super().__init__(convert_charrefs=True)
        self.ids: Set[str] = set()
        self.refs: List[Ref] = []

    def handle_starttag(self, tag: str, attrs: List[Tuple[str, Optional[str]]]) -> None:
        self._handle_tag(tag, attrs)

    def handle_startendtag(self, tag: str, attrs: List[Tuple[str, Optional[str]]]) -> None:
        self._handle_tag(tag, attrs)

    def _handle_tag(self, tag: str, attrs: List[Tuple[str, Optional[str]]]) -> None:
        attr_map = {k: v for k, v in attrs if v is not None}
        if "id" in attr_map and attr_map["id"]:
            self.ids.add(attr_map["id"])
        if tag == "a" and "name" in attr_map and attr_map["name"]:
            self.ids.add(attr_map["name"])

        for attr_name, value in attr_map.items():
            if (tag, attr_name) in REF_ATTRS and value:
                self.refs.append(Ref(tag=tag, attr=attr_name, value=value, line=self.getpos()[0]))


def to_route_path(site_dir: Path, file_path: Path) -> str:
    rel = file_path.relative_to(site_dir).as_posix()
    if rel == "index.html":
        return "/"
    if rel.endswith("/index.html"):
        return f"/{rel[:-10]}/"
    return f"/{rel}"


def extract_srcset_urls(srcset: str) -> Iterable[str]:
    for part in srcset.split(","):
        candidate = part.strip()
        if not candidate:
            continue
        yield candidate.split()[0]


def parse_redirect_patterns(redirects_file: Path) -> List[re.Pattern[str]]:
    if not redirects_file.exists():
        return []

    patterns: List[re.Pattern[str]] = []
    for raw in redirects_file.read_text(encoding="utf-8", errors="ignore").splitlines():
        line = raw.strip()
        if not line or line.startswith("#"):
            continue
        parts = line.split()
        if len(parts) < 2:
            continue
        source = parts[0]
        if not source.startswith("/"):
            continue
        parts = re.split(r"(:[A-Za-z_][A-Za-z0-9_]*|\*)", source)
        regex_parts: List[str] = []
        for part in parts:
            if not part:
                continue
            if part == "*":
                regex_parts.append(".*")
            elif part.startswith(":"):
                regex_parts.append("[^/]+")
            else:
                regex_parts.append(re.escape(part))
        patterns.append(re.compile(r"^" + "".join(regex_parts) + r"$"))
    return patterns


def matches_redirect(path: str, patterns: List[re.Pattern[str]]) -> bool:
    for pattern in patterns:
        if pattern.match(path):
            return True
    return False


def is_ignored_path(path: str) -> bool:
    lowered = path.lower()
    for prefix in IGNORED_PATH_PREFIXES:
        p = prefix.lower()
        if lowered == p or lowered.startswith(p + "/"):
            return True
    return False


def candidate_targets(site_dir: Path, current_file: Path, raw_path: str) -> Tuple[str, List[Path]]:
    if raw_path.startswith("/"):
        clean = "/" + unquote(raw_path.lstrip("/"))
        fs_path = site_dir / clean.lstrip("/")
    else:
        clean = "/" + (to_route_path(site_dir, current_file).rsplit("/", 1)[0].lstrip("/") + "/" + unquote(raw_path)).lstrip("/")
        fs_path = (current_file.parent / unquote(raw_path)).resolve()

    if not fs_path.is_relative_to(site_dir.resolve()):
        return clean, []

    candidates: List[Path] = []
    if clean.endswith("/"):
        candidates.append(fs_path / "index.html")
    else:
        candidates.append(fs_path)
        candidates.append(Path(f"{fs_path}.html"))
        candidates.append(fs_path / "index.html")
        # Legacy WordPress-style links often point to /foo.html while Hugo outputs /foo/index.html.
        if clean.endswith(".html"):
            stem_route = clean[:-5]
            stem_path = site_dir / stem_route.lstrip("/")
            candidates.append(stem_path)
            candidates.append(stem_path / "index.html")

    return clean, candidates


def resolve_target(site_dir: Path, current_file: Path, url: str, redirect_patterns: List[re.Pattern[str]]) -> Tuple[Optional[Path], Optional[str], Optional[str]]:
    try:
        split = urlsplit(url)
    except ValueError:
        return None, None, url
    if split.scheme and split.scheme.lower() in IGNORED_SCHEMES:
        return None, split.fragment or None, None
    if split.scheme or split.netloc or url.startswith("//"):
        return None, split.fragment or None, None

    if split.path == "" and split.fragment:
        return current_file, split.fragment, None

    path = split.path or ""
    if is_ignored_path(path):
        return None, split.fragment or None, None

    clean_route, candidates = candidate_targets(site_dir, current_file, path)

    for candidate in candidates:
        if candidate.is_file():
            return candidate, split.fragment or None, None

    if path and not path.startswith("/"):
        # Legacy content often meant site-root-relative paths but omitted leading slash.
        if is_ignored_path("/" + path.lstrip("./")):
            return None, split.fragment or None, None
        root_route, root_candidates = candidate_targets(site_dir, current_file, "/" + path.lstrip("./"))
        for candidate in root_candidates:
            if candidate.is_file():
                return candidate, split.fragment or None, None

    redirect_candidates = {clean_route}
    if clean_route.endswith("/"):
        redirect_candidates.add(clean_route[:-1])
    else:
        redirect_candidates.add(clean_route + "/")
    if clean_route.endswith(".html"):
        redirect_candidates.add(clean_route[:-5])
        redirect_candidates.add(clean_route[:-5] + "/")
    if path and not path.startswith("/"):
        root_route = "/" + unquote(path.lstrip("./")).lstrip("/")
        redirect_candidates.add(root_route)
        if root_route.endswith("/"):
            redirect_candidates.add(root_route[:-1])
        else:
            redirect_candidates.add(root_route + "/")
        if root_route.endswith(".html"):
            redirect_candidates.add(root_route[:-5])
            redirect_candidates.add(root_route[:-5] + "/")

    if any(matches_redirect(candidate, redirect_patterns) for candidate in redirect_candidates):
        return None, split.fragment or None, None

    # Optional docs sections may not be generated in all local builds.
    if (clean_route == "/manual/" or clean_route.startswith("/manual/")) and not (site_dir / "manual").exists():
        return None, split.fragment or None, None
    if (clean_route == "/javadoc/" or clean_route.startswith("/javadoc/")) and not (site_dir / "javadoc").exists():
        return None, split.fragment or None, None

    return None, split.fragment or None, clean_route


def should_ignore_raw_value(raw: str) -> bool:
    # Some legacy imported comments contain malformed pseudo-URLs from stack traces.
    lowered = raw.lower()
    return (
        lowered.startswith("[http://")
        or lowered.startswith("[https://")
        or "]%28http://" in lowered
        or "]%28https://" in lowered
        or "%28http://" in lowered
        or "%29%28http://" in lowered
        or lowered.startswith("http://[")
        or lowered.startswith("https://[")
        or "%29%28[" in lowered
        or lowered.startswith("/***")
        or "//[http" in lowered
        or lowered.startswith("/blog/") and "//[https://" in lowered
    )


def load_pages(site_dir: Path) -> Tuple[Dict[Path, Set[str]], Dict[Path, List[Ref]]]:
    page_ids: Dict[Path, Set[str]] = {}
    page_refs: Dict[Path, List[Ref]] = {}
    for html_file in sorted(site_dir.rglob("*.html")):
        parser = PageParser()
        parser.feed(html_file.read_text(encoding="utf-8", errors="ignore"))
        page_ids[html_file] = parser.ids
        page_refs[html_file] = parser.refs
    return page_ids, page_refs


def lint(site_dir: Path, max_log_errors: int, report_file: Optional[Path]) -> int:
    redirect_patterns = parse_redirect_patterns(site_dir / "_redirects")
    page_ids, page_refs = load_pages(site_dir)

    errors: List[str] = []

    for page, refs in page_refs.items():
        page_route = to_route_path(site_dir, page)
        for ref in refs:
            values = [ref.value]
            if ref.attr == "srcset":
                values = list(extract_srcset_urls(ref.value))

            for value in values:
                stripped = value.strip()
                if not stripped or stripped == "#":
                    continue
                if should_ignore_raw_value(stripped):
                    continue
                target_file, fragment, missing_route = resolve_target(site_dir, page, stripped, redirect_patterns)

                if missing_route is not None:
                    errors.append(
                        f"{page_route}:{ref.line} [{ref.tag}@{ref.attr}] missing target: {stripped} (resolved {missing_route})"
                    )
                    continue

                if fragment and target_file is not None and target_file.suffix.lower() == ".html":
                    target_route = to_route_path(site_dir, target_file)
                    # Javadocs are generated by a different tool and anchor ids are not always stable/portable.
                    if target_route.startswith("/javadoc/"):
                        continue
                    target_ids = page_ids.get(target_file, set())
                    decoded_fragment = unquote(fragment)
                    if fragment.startswith("comment-"):
                        continue
                    if fragment not in target_ids and decoded_fragment not in target_ids:
                        errors.append(
                            f"{page_route}:{ref.line} [{ref.tag}@{ref.attr}] missing anchor #{fragment} in {target_route}"
                        )

    if report_file is not None:
        report_file.parent.mkdir(parents=True, exist_ok=True)
        report_file.write_text("\n".join(errors) + ("\n" if errors else ""), encoding="utf-8")

    if errors:
        total = len(errors)
        shown = min(total, max_log_errors)
        print(f"Broken internal links/assets found: {total}")
        for entry in errors[:shown]:
            print(f"- {entry}")
        if total > shown:
            print(f"... {total - shown} more errors omitted from log output")
            if report_file is not None:
                print(f"Full report: {report_file}")
        return 1

    print("No broken internal links/assets found.")
    return 0


def main() -> int:
    parser = argparse.ArgumentParser(description="Lint internal links/images in a built site directory.")
    parser.add_argument("--site-dir", default="docs/website/public", help="Path to built website root.")
    parser.add_argument("--max-log-errors", type=int, default=300, help="Maximum number of errors to print to stdout.")
    parser.add_argument("--report-file", default="", help="Optional path to write a full error report.")
    args = parser.parse_args()

    site_dir = Path(args.site_dir).resolve()
    if not site_dir.exists():
        print(f"Site directory not found: {site_dir}", file=sys.stderr)
        return 2
    report_file = Path(args.report_file).resolve() if args.report_file else None
    return lint(site_dir, max_log_errors=max(args.max_log_errors, 0), report_file=report_file)


if __name__ == "__main__":
    raise SystemExit(main())
