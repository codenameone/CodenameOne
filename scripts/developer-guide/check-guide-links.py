#!/usr/bin/env python3
"""Check the developer guide's links against the site that has to serve them.

Three failures the guide shipped, none of which any existing gate could see:

* Fifteen ``codenameone.com/manual/<chapter>.html`` deep links. ``_redirects``
  covers ``/manual`` and ``/manual/`` exactly and has no splat, so every one of
  them 404s -- and every one points at a section inside this same book, which an
  internal cross-reference would have reached.
* ``https://stackoverflow/tags/codenameone/`` -- a host with no dot in it, which
  resolves nowhere.
* Plain ``http://`` links to hosts that have not answered on port 80 for years.

Rather than curate a list of good links, this derives the set of paths the site
actually serves -- the redirect table plus the Hugo content tree -- and reports
any codenameone.com link that lands outside it. The baseline is a ratchet: it may
shrink, never grow.
"""
from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path
from urllib.parse import urlsplit

ASCIIDOC_EXTENSIONS = {".adoc", ".asciidoc"}
URL_RE = re.compile(r"\bhttps?://[^\s\[\]<>\"'`)]+")
# Served by the site outside the content tree.
STATIC_PREFIXES = ("/javadoc/", "/files/", "/developer-guide/", "/images/", "/img/", "/blog/")
# http:// is correct for these: RFC 3161 timestamping servers reject TLS, and
# example.com URLs are illustrative rather than fetched.
TLS_EXEMPT_HOSTS = {"timestamp.digicert.com", "example.com", "www.example.com"}
# Hosts with no dot that are still real destinations.
LOCAL_HOSTS = {"localhost", "127.0.0.1", "0.0.0.0"}
# Only the marketing site is served from the redirect table and the Hugo content
# tree. cloud.codenameone.com and friends are separate services.
SITE_HOSTS = {"codenameone.com", "www.codenameone.com"}


def site_paths(repo_root: Path) -> set[str]:
    """Every path the website is known to answer on."""
    paths: set[str] = set()
    redirects = repo_root / "docs/website/static/_redirects"
    if redirects.exists():
        for line in redirects.read_text(encoding="utf-8").split("\n"):
            parts = line.split()
            if not parts or parts[0].startswith("#"):
                continue
            paths.add(parts[0].rstrip("/") or "/")
            if len(parts) > 1 and parts[1].startswith("/"):
                paths.add(parts[1].rstrip("/") or "/")
    content = repo_root / "docs/website/content"
    if content.exists():
        for page in content.rglob("*.md"):
            stem = page.relative_to(content).with_suffix("").as_posix()
            stem = stem[: -len("/_index")] if stem.endswith("/_index") else stem
            if stem in {"_index", "index"}:
                paths.add("/")
                continue
            paths.add("/" + stem)
            paths.add("/" + stem + ".html")
            # front matter may override the permalink
            head = page.read_text(encoding="utf-8", errors="ignore")[:600]
            match = re.search(r'^url:\s*"?([^"\n]+)"?', head, re.M)
            if match:
                paths.add(match.group(1).strip().rstrip("/") or "/")
    return paths


def findings_for(path: Path, known: set[str]) -> list[tuple[str, str]]:
    out: list[tuple[str, str]] = []
    for number, line in enumerate(path.read_text(encoding="utf-8").split("\n"), 1):
        for url in URL_RE.findall(line):
            url = url.rstrip(".,;:")
            split = urlsplit(url)
            host = split.hostname or ""
            if "." not in host and host not in LOCAL_HOSTS:
                out.append((url, f"host '{host}' has no dot in it and resolves nowhere"))
                continue
            if split.scheme == "http" and host not in TLS_EXEMPT_HOSTS:
                out.append((url, "plain http, not https"))
            if host in SITE_HOSTS:
                target = split.path.rstrip("/") or "/"
                if target.startswith(STATIC_PREFIXES) or target + "/" in STATIC_PREFIXES:
                    continue
                if target not in known:
                    out.append((url, "the website serves no such path (checked _redirects and the content tree)"))
    return out


def load_baseline(path: Path) -> set[str]:
    if not path.exists():
        return set()
    return {
        line.rstrip("\n")
        for line in path.read_text(encoding="utf-8").split("\n")
        if line.strip() and not line.startswith("#")
    }


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--guide-dir", default="docs/developer-guide", type=Path)
    parser.add_argument("--repo-root", default=".", type=Path)
    parser.add_argument(
        "--baseline", default="scripts/developer-guide/guide-links-baseline.txt", type=Path
    )
    parser.add_argument("--write-baseline", action="store_true")
    args = parser.parse_args()

    guide_dir = args.guide_dir.resolve()
    known = site_paths(args.repo_root.resolve())
    if not known:
        raise SystemExit("could not derive any site paths; is --repo-root correct?")

    current: dict[str, str] = {}
    for path in sorted(guide_dir.rglob("*")):
        if path.suffix not in ASCIIDOC_EXTENSIONS or not path.is_file():
            continue
        name = path.relative_to(guide_dir).as_posix()
        for url, reason in findings_for(path, known):
            current[f"{name}\t{url}"] = reason

    if args.write_baseline:
        args.baseline.write_text(
            "\n".join(
                [
                    "# Developer guide links that do not resolve, or are not TLS.",
                    "# A ratchet: entries may be removed as links are fixed, never added.",
                    "# Regenerate with check-guide-links.py --write-baseline.",
                ]
                + sorted(current)
            )
            + "\n",
            encoding="utf-8",
        )
        print(f"Wrote baseline with {len(current)} entr(ies).")
        return 0

    baseline = load_baseline(args.baseline)
    new = sorted(set(current) - baseline)
    if new:
        for entry in new:
            name, _, url = entry.partition("\t")
            print(f"{name}: {url} -- {current[entry]}", file=sys.stderr)
        print(f"\n{len(new)} new broken or insecure link(s).", file=sys.stderr)
        return 1

    fixed = len(baseline) - len(current)
    print(
        f"Links: {len(current)} known bad link(s) against {len(known)} known site paths, none new"
        + (f", {fixed} fixed since the baseline was written." if fixed > 0 else ".")
    )
    if fixed > 0:
        print("Run --write-baseline to bank the progress.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
