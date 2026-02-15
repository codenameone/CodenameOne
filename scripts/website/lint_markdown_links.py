#!/usr/bin/env python3
"""Lint internal links in markdown source against content routes and static images."""

from __future__ import annotations

import argparse
import posixpath
import re
import sys
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
    "/files",
    "/javadoc",
    "/manual",
    "/ota",
    "/demos",
    "/downloads",
    "/cdn-cgi",
)

IMAGE_EXTENSIONS = {
    ".png",
    ".jpg",
    ".jpeg",
    ".gif",
    ".svg",
    ".webp",
    ".avif",
    ".bmp",
    ".ico",
}

LINK_PATTERN = re.compile(r"!?\[[^\]]*]\(([^)]+)\)")


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
        tokens = re.split(r"(:[A-Za-z_][A-Za-z0-9_]*|\*)", source)
        regex_parts: List[str] = []
        for token in tokens:
            if not token:
                continue
            if token == "*":
                regex_parts.append(".*")
            elif token.startswith(":"):
                regex_parts.append("[^/]+")
            else:
                regex_parts.append(re.escape(token))
        patterns.append(re.compile(r"^" + "".join(regex_parts) + r"$"))
    return patterns


def matches_redirect(path: str, patterns: List[re.Pattern[str]]) -> bool:
    return any(pattern.match(path) for pattern in patterns)


def should_ignore_path(path: str) -> bool:
    lowered = path.lower()
    for prefix in IGNORED_PATH_PREFIXES:
        p = prefix.lower()
        if lowered == p or lowered.startswith(p + "/"):
            return True
    return False


def should_ignore_raw_target(raw: str) -> bool:
    lowered = raw.lower().strip()
    return (
        lowered.startswith("<http://")
        or lowered.startswith("<https://")
        or lowered.startswith("[http://")
        or lowered.startswith("[https://")
        or "]%28http://" in lowered
        or "]%28https://" in lowered
        or "%29%28http://" in lowered
        or "%29%28[" in lowered
        or lowered.startswith("http://[")
        or lowered.startswith("https://[")
        or lowered.startswith("/***")
        or "//[http" in lowered
    )


def extract_target(raw: str) -> str:
    value = raw.strip()
    if value.startswith("<") and value.endswith(">"):
        value = value[1:-1].strip()
    if " " in value:
        value = value.split(" ", 1)[0].strip()
    return value


def parse_front_matter_value(value: str) -> str:
    value = value.strip()
    if len(value) >= 2 and value[0] == value[-1] and value[0] in {"'", '"'}:
        return value[1:-1]
    return value


def parse_front_matter(text: str) -> Dict[str, object]:
    lines = text.splitlines()
    if not lines:
        return {}

    delimiter = None
    if lines[0].strip() == "---":
        delimiter = "---"
    elif lines[0].strip() == "+++":
        delimiter = "+++"
    else:
        return {}

    out: Dict[str, object] = {}
    i = 1
    while i < len(lines):
        line = lines[i]
        if line.strip() == delimiter:
            break
        if not line.strip() or line.lstrip().startswith("#"):
            i += 1
            continue

        m = re.match(r"^([A-Za-z0-9_-]+)\s*:\s*(.*)$", line)
        if not m:
            i += 1
            continue
        key, value = m.group(1), m.group(2).strip()

        if key == "aliases":
            aliases: List[str] = []
            if value.startswith("[") and value.endswith("]"):
                inner = value[1:-1].strip()
                if inner:
                    for item in inner.split(","):
                        alias = parse_front_matter_value(item)
                        if alias:
                            aliases.append(alias)
                out[key] = aliases
                i += 1
                continue

            i += 1
            while i < len(lines):
                item_line = lines[i]
                if item_line.strip() == delimiter:
                    i -= 1
                    break
                item_match = re.match(r"^\s*-\s*(.+)$", item_line)
                if not item_match:
                    i -= 1
                    break
                alias = parse_front_matter_value(item_match.group(1))
                if alias:
                    aliases.append(alias)
                i += 1
            out[key] = aliases
            i += 1
            continue

        out[key] = parse_front_matter_value(value)
        i += 1

    return out


def normalize_route(route: str) -> str:
    cleaned = "/" + unquote(route).lstrip("/")
    cleaned = re.sub(r"/{2,}", "/", cleaned)
    return cleaned


def build_routes(content_root: Path) -> Dict[Path, str]:
    routes: Dict[Path, str] = {}
    for md_file in sorted(content_root.rglob("*.md")):
        rel = md_file.relative_to(content_root).as_posix()
        rel_no_ext = rel[:-3]
        parent = posixpath.dirname(rel_no_ext)
        filename = posixpath.basename(rel_no_ext)

        text = md_file.read_text(encoding="utf-8", errors="ignore")
        fm = parse_front_matter(text)
        slug = str(fm.get("slug", "")).strip() if "slug" in fm else ""
        explicit_url = str(fm.get("url", "")).strip() if "url" in fm else ""

        if explicit_url.startswith("/"):
            route = normalize_route(explicit_url)
        elif filename == "_index":
            route = "/" if parent in {"", "."} else f"/{parent.strip('/')}/"
        else:
            leaf = slug if slug else filename
            route = f"/{parent.strip('/') + '/' if parent not in {'', '.'} else ''}{leaf}/"
            route = normalize_route(route)

        if not route.endswith("/") and not route.endswith(".html"):
            route = route + "/"
        routes[md_file] = route
    return routes


def build_route_set(content_root: Path) -> Set[str]:
    route_by_file = build_routes(content_root)
    route_set: Set[str] = set()

    for route in route_by_file.values():
        route_set.add(route)
        if route.endswith("/"):
            route_set.add(route[:-1] or "/")
        elif not route.endswith(".html"):
            route_set.add(route + "/")

    for md_file in sorted(content_root.rglob("*.md")):
        text = md_file.read_text(encoding="utf-8", errors="ignore")
        fm = parse_front_matter(text)
        aliases = fm.get("aliases", [])
        if isinstance(aliases, list):
            for alias in aliases:
                if isinstance(alias, str) and alias.strip().startswith("/"):
                    route_set.add(normalize_route(alias.strip()))

    return route_set


def collect_static_files(static_root: Path) -> Set[str]:
    out: Set[str] = set()
    if not static_root.exists():
        return out
    for file in static_root.rglob("*"):
        if file.is_file():
            out.add("/" + file.relative_to(static_root).as_posix())
    return out


def iter_markdown_links(text: str) -> Iterable[Tuple[int, str]]:
    for line_no, line in enumerate(text.splitlines(), start=1):
        for m in LINK_PATTERN.finditer(line):
            yield line_no, m.group(1)


def normalize_candidate_paths(path: str) -> Set[str]:
    path = normalize_route(path)
    candidates = {path}
    if path.endswith("/"):
        candidates.add(path[:-1] or "/")
    else:
        candidates.add(path + "/")
    if path.endswith(".html"):
        base = path[:-5]
        candidates.add(base)
        candidates.add(base + "/")
    return candidates


def should_check_page_path(path: str) -> bool:
    if path == "/":
        return False
    if should_ignore_path(path):
        return False
    if path.startswith("/tags/"):
        # Taxonomy pages are generated by Hugo from content metadata, not standalone markdown files.
        return False
    if path.startswith("/blog/") or path.startswith("/how-do-i/") or path.startswith("/courses/"):
        return True
    # Also check top-level content pages like /about-us/ or legacy /about-us.html.
    return path.count("/") <= 2


def resolve_link_path(page_route: str, raw_path: str) -> str:
    decoded = unquote(raw_path)
    if decoded.startswith("/"):
        return normalize_route(decoded)
    base_dir = page_route if page_route.endswith("/") else posixpath.dirname(page_route) + "/"
    return normalize_route(posixpath.normpath(posixpath.join(base_dir, decoded)))


def lint(content_root: Path, static_root: Path, redirects_file: Path, max_log_errors: int, report_file: Optional[Path]) -> int:
    route_by_file = build_routes(content_root)
    route_set = build_route_set(content_root)
    static_files = collect_static_files(static_root)
    redirect_patterns = parse_redirect_patterns(redirects_file)

    errors: List[str] = []

    for md_file, page_route in route_by_file.items():
        rel = md_file.relative_to(content_root).as_posix()
        text = md_file.read_text(encoding="utf-8", errors="ignore")
        for line_no, raw_target in iter_markdown_links(text):
            target = extract_target(raw_target)
            if not target or target == "#" or should_ignore_raw_target(target):
                continue

            try:
                split = urlsplit(target)
            except ValueError:
                continue

            if split.scheme and split.scheme.lower() in IGNORED_SCHEMES:
                continue
            if split.scheme or split.netloc or target.startswith("//"):
                continue
            if split.path == "" and split.fragment:
                continue

            resolved_path = resolve_link_path(page_route, split.path or "")
            if should_ignore_path(resolved_path):
                continue

            ext = Path(resolved_path).suffix.lower()
            if ext in IMAGE_EXTENSIONS:
                if resolved_path in static_files:
                    continue
                if matches_redirect(resolved_path, redirect_patterns):
                    continue
                errors.append(f"{rel}:{line_no} missing image asset: {target} (resolved {resolved_path})")
                continue

            if ext and ext != ".html":
                # Skip non-image file URL checks (downloads, zips, binaries, etc.).
                continue
            if not should_check_page_path(resolved_path):
                continue

            candidates = normalize_candidate_paths(resolved_path)
            if any(candidate in route_set for candidate in candidates):
                continue
            if any(matches_redirect(candidate, redirect_patterns) for candidate in candidates):
                continue

            errors.append(f"{rel}:{line_no} missing markdown route: {target} (resolved {resolved_path})")

    if report_file is not None:
        report_file.parent.mkdir(parents=True, exist_ok=True)
        report_file.write_text("\n".join(errors) + ("\n" if errors else ""), encoding="utf-8")

    if errors:
        total = len(errors)
        shown = min(total, max_log_errors)
        print(f"Broken markdown links/images found: {total}")
        for entry in errors[:shown]:
            print(f"- {entry}")
        if total > shown:
            print(f"... {total - shown} more errors omitted from log output")
            if report_file is not None:
                print(f"Full report: {report_file}")
        return 1

    print("No broken markdown links/images found.")
    return 0


def main() -> int:
    parser = argparse.ArgumentParser(description="Lint markdown source links against site content/static files.")
    parser.add_argument("--content-root", default="docs/website/content", help="Path to website content markdown root.")
    parser.add_argument("--static-root", default="docs/website/static", help="Path to website static asset root.")
    parser.add_argument("--redirects-file", default="docs/website/static/_redirects", help="Redirects file used for fallback route checks.")
    parser.add_argument("--max-log-errors", type=int, default=300, help="Maximum number of errors to print.")
    parser.add_argument("--report-file", default="", help="Optional path to write full report.")
    args = parser.parse_args()

    content_root = Path(args.content_root).resolve()
    static_root = Path(args.static_root).resolve()
    redirects_file = Path(args.redirects_file).resolve()

    if not content_root.exists():
        print(f"Content root not found: {content_root}", file=sys.stderr)
        return 2
    if not static_root.exists():
        print(f"Static root not found: {static_root}", file=sys.stderr)
        return 2

    report_file = Path(args.report_file).resolve() if args.report_file else None
    return lint(content_root, static_root, redirects_file, max(args.max_log_errors, 0), report_file)


if __name__ == "__main__":
    raise SystemExit(main())
