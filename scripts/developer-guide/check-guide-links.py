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
import datetime
import re
import sys
from pathlib import Path
from urllib.parse import urlsplit

ASCIIDOC_EXTENSIONS = {".adoc", ".asciidoc"}
URL_RE = re.compile(r"\bhttps?://[^\s\[\]<>\"'`)]+")
# Trees the site serves that are produced by a build rather than by a file in
# this repository, so nothing here can be enumerated. Everything else --
# including /blog/ and every static asset -- is derived, because whitelisting a
# prefix silently exempts every path under it from the check.
GENERATED_PREFIXES = ("/javadoc/", "/developer-guide/")
# http:// is correct for these: RFC 3161 timestamping servers reject TLS, and
# example.com URLs are illustrative rather than fetched.
TLS_EXEMPT_HOSTS = {"timestamp.digicert.com", "example.com", "www.example.com"}
# Hosts with no dot that are still real destinations.
LOCAL_HOSTS = {"localhost", "127.0.0.1", "0.0.0.0"}
# Only the marketing site is served from the redirect table and the Hugo content
# tree. cloud.codenameone.com and friends are separate services.
SITE_HOSTS = {"codenameone.com", "www.codenameone.com"}


def front_matter(page: Path) -> dict[str, object]:
    """Pull the few front-matter keys that decide a page's published route.

    Deliberately not a YAML parse: the tree mixes YAML and TOML front matter and
    only three keys matter here.
    """
    text = page.read_text(encoding="utf-8", errors="ignore")
    lines = text.split("\n")
    if not lines or lines[0].strip() not in {"---", "+++"}:
        return {}
    fence = lines[0].strip()
    out: dict[str, object] = {}
    aliases: list[str] = []
    in_aliases = False
    for line in lines[1:]:
        if line.strip() == fence:
            break
        if in_aliases:
            stripped = line.strip()
            if stripped.startswith("-"):
                aliases.append(stripped.lstrip("- ").strip().strip("\"'"))
                continue
            in_aliases = False
        match = re.match(r'^(url|slug|aliases|draft|date)\s*[:=]\s*(.*)$', line)
        if not match:
            continue
        key, raw = match.group(1), match.group(2).strip()
        if key == "aliases":
            if raw in {"", "["}:
                in_aliases = True
            else:
                aliases.extend(v.strip().strip("\"'") for v in raw.strip("[]").split(",") if v.strip())
            continue
        out[key] = raw.strip("\"'")
    if aliases:
        out["aliases"] = aliases
    return out


def is_published(meta: dict[str, object], today: str) -> bool:
    """Hugo defaults buildDrafts and buildFuture to false, so neither reaches the site.

    The date test makes the result depend on the day it runs, which is not ideal in
    a gate. It is kept because it mirrors what the site actually serves: a link to a
    post that has not been published yet is genuinely broken until it is.
    """
    if str(meta.get("draft", "")).strip().strip("\"'").lower() in {"true", "yes"}:
        return False
    date = str(meta.get("date", "")).strip().strip("\"'")
    return not (re.match(r"^\d{4}-\d{2}-\d{2}", date) and date[:10] > today)


def normalize_path(value: str) -> str:
    value = value.strip()
    if not value:
        return ""
    if not value.startswith("/"):
        value = "/" + value
    return value.rstrip("/") or "/"


def redirect_pattern(source: str) -> re.Pattern[str] | None:
    """Compile a _redirects source that is not a literal path.

    Netlify sources may end in a `*` splat or contain `:placeholder` segments, and
    21 of the rules in this file do. Recording `/files/cn1libs/*` as a literal
    string means a real link to `/files/cn1libs/foo.cn1lib` matches nothing and is
    reported as broken.
    """
    if "*" not in source and ":" not in source:
        return None
    pattern = "".join(
        ".*" if part == "*" else (r"[^/]+" if part.startswith(":") else re.escape(part))
        for part in re.split(r"(\*|:[A-Za-z_][A-Za-z0-9_]*)", source)
        if part
    )
    return re.compile("^" + pattern + "/?$")


def site_paths(repo_root: Path) -> tuple[set[str], list[re.Pattern[str]]]:
    """Every path the website is known to answer on, derived rather than listed.

    Returns the literal paths and the patterns for the wildcard redirect rules.
    """
    paths: set[str] = set()
    patterns: list[re.Pattern[str]] = []

    redirects = repo_root / "docs/website/static/_redirects"
    if redirects.exists():
        for line in redirects.read_text(encoding="utf-8").split("\n"):
            parts = line.split()
            if not parts or parts[0].startswith("#"):
                continue
            compiled = redirect_pattern(parts[0])
            if compiled is not None:
                patterns.append(compiled)
            else:
                paths.add(normalize_path(parts[0]))
            # Only the SOURCE counts. A rule whose destination was deleted still
            # sits in this file, so trusting destinations would accept a guide
            # link to a page that no longer exists.

    # Hugo's published route is the section path plus the page's slug, which
    # 1055 of the content pages override; deriving it from the filename instead
    # both invents routes that are never generated and rejects real ones.
    paths.add("/")  # Hugo always renders the home page, _index.md or not

    today = datetime.date.today().isoformat()
    content = repo_root / "docs/website/content"
    if content.exists():
        for page in content.rglob("*.md"):
            relative = page.relative_to(content).with_suffix("")
            meta = front_matter(page)
            if not is_published(meta, today):
                continue
            for alias in meta.get("aliases", []) or []:
                if isinstance(alias, str):
                    paths.add(normalize_path(alias))
            if meta.get("url"):
                paths.add(normalize_path(str(meta["url"])))
                continue
            parts = list(relative.parts)
            if parts and parts[-1] in {"_index", "index"}:
                parts.pop()
            if meta.get("slug"):
                parts = parts[:-1] + [str(meta["slug"])] if parts else [str(meta["slug"])]
            paths.add(normalize_path("/".join(parts)) if parts else "/")

    # Some redirects are written into _redirects at deploy time rather than
    # committed, so the file in the tree does not list them. Read the paths out
    # of the script that emits them instead of assuming a prefix is safe.
    for emitter in sorted((repo_root / "scripts/website").glob("*redirect*.sh")):
        for match in re.finditer(
            r"printf\s+'(/[^\s']+)\s+%s[^']*'", emitter.read_text(encoding="utf-8")
        ):
            paths.add(normalize_path(match.group(1)))

    # Anything committed under static/ is served at its own path.
    static = repo_root / "docs/website/static"
    if static.exists():
        for asset in static.rglob("*"):
            relative = asset.relative_to(static).as_posix()
            paths.add(normalize_path(relative))
            if asset.is_file() and asset.name == "index.html":
                paths.add(normalize_path(asset.parent.relative_to(static).as_posix()))

    return paths, patterns


def findings_for(path: Path, known: set[str], patterns: list[re.Pattern[str]]) -> list[tuple[str, str]]:
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
                if target.startswith(GENERATED_PREFIXES) or target + "/" in GENERATED_PREFIXES:
                    continue
                if target not in known and not any(p.match(target) for p in patterns):
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
    known, patterns = site_paths(args.repo_root.resolve())
    if not known:
        raise SystemExit("could not derive any site paths; is --repo-root correct?")

    current: dict[str, str] = {}
    for path in sorted(guide_dir.rglob("*")):
        if path.suffix not in ASCIIDOC_EXTENSIONS or not path.is_file():
            continue
        name = path.relative_to(guide_dir).as_posix()
        for url, reason in findings_for(path, known, patterns):
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
    # A baselined entry that no longer reproduces has to leave the file. Leaving it
    # keeps a slot open: a later change can restore that exact file+URL and
    # `current - baseline` stays empty, so the regression sails through. The
    # ratchet only ratchets if fixes are banked.
    stale = sorted(baseline - set(current))
    if new or stale:
        for entry in new:
            name, _, url = entry.partition("\t")
            print(f"{name}: {url} -- {current[entry]}", file=sys.stderr)
        for entry in stale:
            name, _, url = entry.partition("\t")
            print(
                f"{name}: {url} -- no longer broken, but still in the baseline. Run "
                f"check-guide-links.py --write-baseline to bank the fix.",
                file=sys.stderr,
            )
        print(
            f"\n{len(new)} new broken or insecure link(s), {len(stale)} stale baseline entr(ies).",
            file=sys.stderr,
        )
        return 1

    print(
        f"Links: {len(current)} known bad link(s) against {len(known)} known site paths "
        f"and {len(patterns)} wildcard rule(s); none new, none stale."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
