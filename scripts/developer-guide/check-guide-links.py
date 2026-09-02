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
import collections
import datetime
import re
import sys
from pathlib import Path
from urllib.parse import urlsplit

ASCIIDOC_EXTENSIONS = {".adoc", ".asciidoc"}
URL_RE = re.compile(r"\bhttps?://[^\s\[\]<>\"'`)]+")
# The one tree that genuinely cannot be enumerated from this repository: the
# Javadoc is produced from the framework sources at build time. Everything else,
# /developer-guide/ included, is derived below -- whitelisting a prefix silently
# exempts every path under it from the check.
GENERATED_PREFIXES = ("/javadoc/",)
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
            if stripped in {"]", "],"}:
                in_aliases = False
                continue
            # YAML block sequence ("- /x") and TOML/flow array ("\"/x\",") both
            # appear in this tree, so accept either continuation shape.
            if stripped.startswith("-") or stripped.startswith(("\"", "'")):
                aliases.append(stripped.lstrip("- ").strip().rstrip(",").strip("\"'"))
                continue
            in_aliases = False
        match = re.match(r'^(url|slug|aliases|draft|date)\s*[:=]\s*(.*)$', line)
        if not match:
            continue
        key, raw = match.group(1), match.group(2).strip()
        if key == "aliases":
            if raw in {"", "[", "[]"}:
                in_aliases = raw != "[]"
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


def redirect_pattern(source: str) -> tuple[re.Pattern[str], list[str]] | None:
    """Compile a _redirects source that is not a literal path.

    Netlify sources may end in a `*` splat or contain `:placeholder` segments, and
    21 of the rules in this file do. Recording `/files/cn1libs/*` as a literal
    string means a real link to `/files/cn1libs/foo.cn1lib` matches nothing and is
    reported as broken. Returns the pattern and the capture names in order, so the
    destination can be reconstructed from a match.
    """
    if "*" not in source and ":" not in source:
        return None
    names: list[str] = []
    pattern = ""
    for part in re.split(r"(\*|:[A-Za-z_][A-Za-z0-9_]*)", source):
        if not part:
            continue
        if part == "*":
            names.append("splat")
            pattern += "(.*)"
        elif part.startswith(":"):
            names.append(part[1:])
            pattern += "([^/]+)"
        else:
            pattern += re.escape(part)
    return re.compile("^" + pattern + "/?$"), names


def resolves(target: str, known: set[str], rules: list, depth: int = 0) -> bool:
    """Whether a path is served, following wildcard redirects to their destination.

    Accepting every path that merely *matches* a wildcard source is the same
    mistake as whitelisting a prefix. `/*.html -> /:splat/ 301` matches any
    root-level .html path at all, so `/does-not-exist.html` would pass while
    redirecting to a page that is not there. Substitute the captures into the
    destination and check that instead.
    """
    if target in known:
        return True
    if target.startswith(GENERATED_PREFIXES) or target + "/" in GENERATED_PREFIXES:
        # Reachable both directly and by following a redirect into it, so the
        # test belongs here rather than only at the call site.
        return True
    if depth > 4:  # a redirect loop in the table should not hang the check
        return False
    for compiled, names, destination in rules:
        match = compiled.match(target)
        if not match:
            continue
        # The FIRST matching rule wins and the others never run, which is how the
        # host evaluates this file. Trying later rules after an early one leads
        # somewhere dead would pass a link whose reader lands on a deleted page.
        if not destination or not destination.startswith("/"):
            return True  # redirects off-site; nothing here can verify it
        resolved = destination
        for name, value in zip(names, match.groups()):
            resolved = resolved.replace(":" + name, value or "")
        resolved = normalize_path(resolved)
        if resolved == target:
            return False
        return resolves(resolved, known, rules, depth + 1)
    return False


def site_paths(repo_root: Path) -> tuple[set[str], list]:
    """Every path the website is known to answer on, derived rather than listed.

    Returns the literal paths and every redirect rule, each as a matcher, its
    capture names and its destination, so a link can be followed rather than
    accepted for merely matching.
    """
    paths: set[str] = set()
    patterns: list = []

    redirects = repo_root / "docs/website/static/_redirects"
    if redirects.exists():
        for line in redirects.read_text(encoding="utf-8").split("\n"):
            parts = line.split()
            if not parts or parts[0].startswith("#"):
                continue
            destination = parts[1] if len(parts) > 1 else ""
            compiled = redirect_pattern(parts[0])
            if compiled is not None:
                patterns.append((compiled[0], compiled[1], destination))
            else:
                # A literal source is not a served route either -- it is a rule,
                # and a rule pointing at a page that was deleted redirects the
                # reader to a 404. Follow it like any other, rather than treating
                # the fact that a rule exists as proof the link works.
                patterns.append(
                    (re.compile("^" + re.escape(normalize_path(parts[0])) + "/?$"), [], destination)
                )
            # Only the SOURCE counts. A rule whose destination was deleted still
            # sits in this file, so trusting destinations would accept a guide
            # link to a page that no longer exists.

    # Hugo's published route is the section path plus the page's slug, which
    # 1055 of the content pages override; deriving it from the filename instead
    # both invents routes that are never generated and rejects real ones.
    #
    # Known gap: taxonomy term pages (/tags/<term>/) are generated by Hugo from
    # front-matter tags rather than from a file, so they are not derived here. No
    # guide link targets one today. If one is ever added it will be reported as
    # broken, which is the safe direction for a gate to be wrong in.
    paths.add("/")  # Hugo always renders the home page, _index.md or not

    # scripts/website/build.sh renders the guide to /developer-guide/ and rsyncs
    # this directory alongside it so relative image links resolve, excluding the
    # Sketch sources and the AsciiDoc itself. That makes every served path under
    # the guide enumerable, so it does not need a blanket exemption.
    guide = repo_root / "docs/developer-guide"
    if guide.exists():
        paths.add("/developer-guide")
        for asset in guide.rglob("*"):
            if not asset.is_file():
                continue
            relative = asset.relative_to(guide)
            if relative.parts[0] == "sketch" or relative.suffix in {".asciidoc", ".adoc"}:
                continue
            paths.add(normalize_path("developer-guide/" + relative.as_posix()))

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
            if not asset.is_file():
                # A directory is not a route. static/uploads holds assets and no
                # index page, so recording the directory itself would accept a
                # link to /uploads that resolves to nothing.
                continue
            paths.add(normalize_path(asset.relative_to(static).as_posix()))
            if asset.name == "index.html":
                paths.add(normalize_path(asset.parent.relative_to(static).as_posix()))

    return paths, patterns


def findings_for(path: Path, known: set[str], patterns: list) -> list[tuple[str, str]]:
    out: list[tuple[str, str]] = []
    for number, line in enumerate(path.read_text(encoding="utf-8").split("\n"), 1):
        for url in URL_RE.findall(line):
            url = url.rstrip(".,;:")
            split = urlsplit(url)
            host = split.hostname or ""
            if "." not in host and host not in LOCAL_HOSTS:
                out.append((url, f"host '{host}' has no dot in it and resolves nowhere"))
                continue
            if split.scheme == "http" and host not in TLS_EXEMPT_HOSTS | LOCAL_HOSTS:
                out.append((url, "plain http, not https"))
            if host in SITE_HOSTS:
                target = split.path.rstrip("/") or "/"
                if not resolves(target, known, patterns):
                    out.append((url, "the website serves no such path (checked _redirects and the content tree)"))
    return out


def load_baseline(path: Path) -> collections.Counter:
    """Read the baseline as a multiset: one line per occurrence.

    A file that mentions the same broken URL twice has two problems, not one.
    Collapsing them into a set understated the real count -- 35 recorded against
    38 occurrences -- and left a second occurrence of an already-baselined link
    free to appear without the check noticing.
    """
    if not path.exists():
        return collections.Counter()
    return collections.Counter(
        line.rstrip("\n")
        for line in path.read_text(encoding="utf-8").split("\n")
        if line.strip() and not line.startswith("#")
    )


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--guide-dir", default="docs/developer-guide", type=Path)
    parser.add_argument("--repo-root", default=".", type=Path)
    parser.add_argument(
        "--baseline", default="scripts/developer-guide/guide-links-baseline.txt", type=Path
    )
    parser.add_argument("--write-baseline", action="store_true")
    parser.add_argument(
        "--allow-new",
        action="store_true",
        help="permit --write-baseline to ADD entries; without it the baseline may only shrink",
    )
    args = parser.parse_args()

    guide_dir = args.guide_dir.resolve()
    known, patterns = site_paths(args.repo_root.resolve())
    if not known:
        raise SystemExit("could not derive any site paths; is --repo-root correct?")

    current: collections.Counter = collections.Counter()
    reasons: dict[str, str] = {}
    for path in sorted(guide_dir.rglob("*")):
        if path.suffix not in ASCIIDOC_EXTENSIONS or not path.is_file():
            continue
        name = path.relative_to(guide_dir).as_posix()
        for url, reason in findings_for(path, known, patterns):
            entry = f"{name}\t{url}"
            current[entry] += 1
            reasons[entry] = reason

    if args.write_baseline:
        # The command that banks a fix is the same command that could bury a new
        # failure. Shrinking is free; growing needs --allow-new, so recording new
        # debt is a deliberate act that shows up in the command as well as in the
        # baseline diff a reviewer reads.
        added = sorted((current - load_baseline(args.baseline)).elements())
        if added and not args.allow_new:
            for entry in added:
                name, _, url = entry.partition("\t")
                print(f"{name}: {url}", file=sys.stderr)
            print(
                f"\nRefusing to add {len(added)} entr(ies) to the baseline. Fix the "
                f"link, or pass --allow-new if this is debt you mean to record.",
                file=sys.stderr,
            )
            return 1
        args.baseline.write_text(
            "\n".join(
                [
                    "# Developer guide links that do not resolve, or are not TLS.",
                    "# One line per occurrence: a file naming the same bad URL twice gets",
                    "# two lines, because that is two things to fix.",
                    "# A ratchet: entries may be removed as links are fixed, never added.",
                    "# Regenerate with check-guide-links.py --write-baseline.",
                ]
                + sorted(current.elements())
            )
            + "\n",
            encoding="utf-8",
        )
        print(f"Wrote baseline with {sum(current.values())} entr(ies).")
        return 0

    baseline = load_baseline(args.baseline)
    new = sorted((current - baseline).elements())
    # A baselined entry that no longer reproduces has to leave the file. Leaving it
    # keeps a slot open: a later change can restore that exact file+URL and
    # `current - baseline` stays empty, so the regression sails through. The
    # ratchet only ratchets if fixes are banked.
    stale = sorted((baseline - current).elements())
    if new or stale:
        for entry in new:
            name, _, url = entry.partition("\t")
            print(f"{name}: {url} -- {reasons[entry]}", file=sys.stderr)
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
        f"Links: {sum(current.values())} known bad link(s) against {len(known)} known site paths "
        f"and {len(patterns)} redirect rule(s); none new, none stale."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
