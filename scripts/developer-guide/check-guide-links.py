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
# Writing a scheme's own default port changes nothing about where the request goes.
DEFAULT_PORTS = {"http": 80, "https": 443}
# Only the marketing site is served from the redirect table and the Hugo content
# tree. cloud.codenameone.com and friends are separate services.
SITE_HOSTS = {"codenameone.com", "www.codenameone.com"}
# The routes that serve this very book. A link from inside the guide to one of
# these, carrying a fragment, is a cross-reference wearing a URL: it leaves the
# reader's PDF or offline copy to fetch a page they are already reading, and no
# gate can tell that a renamed section broke the fragment, because the anchor
# lives in the rendered book rather than in the site tree. Written as `<<id>>`
# instead, check-guide-xrefs.py resolves it against the rendered anchors.
SELF_PATHS = {"/developer-guide", "/developer-guide.html", "/manual", "/manual.html"}


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


def site_paths(repo_root: Path) -> tuple[set[str], list, set[str]]:
    """Every path the website is known to answer on, derived rather than listed.

    Returns the literal paths and every redirect rule, each as a matcher, its
    capture names and its destination, so a link can be followed rather than
    accepted for merely matching.
    """
    paths: set[str] = set()
    patterns: list = []
    # The sources exactly as written. Every rule is compiled slash-insensitively
    # ("^...$/?"), which is right for matching but loses the distinction the site
    # actually draws, so the trailing-slash rule needs the raw spelling.
    declared: set[str] = set()

    redirects = repo_root / "docs/website/static/_redirects"
    if redirects.exists():
        for line in redirects.read_text(encoding="utf-8").split("\n"):
            parts = line.split()
            if not parts or parts[0].startswith("#"):
                continue
            destination = parts[1] if len(parts) > 1 else ""
            declared.add(parts[0])
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

    # Cloudflare Pages Functions serve a fallback the redirect table does not
    # mention: docs/website/functions/[[path]].js runs only after context.next()
    # has already 404ed, and then sends anything under /files/ or /demos/ to
    # download.codenameone.com. Those paths are therefore served, and this was
    # recording a real one -- /files/iOS_UI-Kit.psd -- as a broken link. The
    # destination is off-site, so it lands in the same bucket as every other
    # off-site redirect: reachable, and not verifiable from this repository.
    # Appended AFTER the _redirects rules because the function is a fallback and
    # the first matching rule wins, mirroring the order the host evaluates.
    for prefix in ("files", "demos"):
        patterns.append(
            (
                re.compile(rf"^/{prefix}(/.*)?$"),
                [],
                "https://download.codenameone.com/",
            )
        )

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

    # This derives a Hugo route as "section path + slug", which is true only while
    # the site leaves routing alone. A [permalinks] rule or uglyURLs would rewrite
    # every route underneath and this would keep accepting links to paths Hugo no
    # longer publishes -- accepting a dead link is exactly the failure this script
    # exists to prevent. hugo.toml declares neither today, so rather than model a
    # configuration that is not there, notice when it appears.
    hugo_config = repo_root / "docs/website/hugo.toml"
    if hugo_config.exists():
        config = hugo_config.read_text(encoding="utf-8", errors="ignore")
        overrides = [
            name
            for name, probe in (("[permalinks]", r"^\s*\[permalinks\]"), ("uglyURLs", r"^\s*uglyURLs\s*="))
            if re.search(probe, config, re.M)
        ]
        if overrides:
            raise SystemExit(
                f"hugo.toml now sets {', '.join(overrides)}, which rewrites the routes "
                f"this script derives from the content tree. Derive them from the built "
                f"docs/website/public tree instead, or teach this function the rule -- "
                f"until then every link it accepts is unverified."
            )

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

    return paths, patterns, declared


def findings_for(path: Path, known: set[str], patterns: list, declared: set[str]) -> list[tuple[str, str]]:
    # Only http:// and https:// are extracted. Protocol-relative links were raised
    # as a gap; measured, the guide contains no `link://` macro at all, and its one
    # bare `//host/path` is a JavaScript string inside a source block, so widening
    # URL_RE to match `//` would start reporting code as a broken link. The scheme
    # requirement is what keeps this off code.
    #
    # Fragments are checked only against this book's own routes. On an ordinary
    # same-site page the anchors live in Hugo's rendered output, which this does
    # not build, so a fragment there cannot be resolved from the repository.
    # Measured: of the 42 same-site URLs carrying a fragment, 38 are /javadoc/ --
    # generated at build time and exempt for the same reason -- and the other four
    # pointed into this book and are now xrefs, which check-guide-xrefs.py resolves
    # against the rendered anchors. That leaves nothing this could check today.
    #
    # Every URL in the source is checked, including any inside an AsciiDoc `//`
    # line comment or `////` block. That is deliberate. Across the guide's 120
    # files there is not one commented-out URL and not one `////` block, so
    # tracking comment state would buy nothing today -- and it would hand the
    # gate a way to be silenced: comment the line out, the finding disappears,
    # the ratchet shrinks, and the dead link is still sitting in the source
    # waiting to be uncommented. Deleting the link is the fix. (Ordinary `//`
    # comments do exist here, for editorial notes; none carries a URL.)
    out: list[tuple[str, str]] = []
    for number, line in enumerate(path.read_text(encoding="utf-8").split("\n"), 1):
        for url in URL_RE.findall(line):
            url = url.rstrip(".,;:")
            split = urlsplit(url)
            # urlsplit lowercases the host but keeps the root label's trailing dot,
            # so the fully qualified spelling "www.codenameone.com." misses
            # SITE_HOSTS and skips route validation entirely -- the same path that
            # is rejected without the dot sails through with it. DNS treats the two
            # as the same name, so strip it before classifying.
            host = (split.hostname or "").rstrip(".")
            # hostname strips the port whether or not it is a number, so a typo in
            # the authority hides behind an otherwise correct host and every check
            # below passes on a URL no browser can open. Reading .port is what
            # surfaces it: urlsplit defers the parse until then and raises.
            try:
                port = split.port
            except ValueError:
                out.append((url, "the port is not a number, so this cannot be opened at all"))
                continue
            if "." not in host and host not in LOCAL_HOSTS:
                out.append((url, f"host '{host}' has no dot in it and resolves nowhere"))
                continue
            if split.scheme == "http" and host not in TLS_EXEMPT_HOSTS | LOCAL_HOSTS:
                out.append((url, "plain http, not https"))
            if (
                host in SITE_HOSTS
                and port is not None
                and port != DEFAULT_PORTS.get(split.scheme)
            ):
                # The route model below describes the site on its default port. A
                # NONSTANDARD port is a different endpoint that model says nothing
                # about, so accepting the path would be accepting an unchecked URL.
                # Spelling out the scheme's own default (":443" under https) is
                # redundant but reaches the identical endpoint, so it is allowed.
                # Local services keep their ports either way: http://localhost:11434
                # is the Ollama endpoint the AI chapter documents on purpose.
                out.append((url, f"port {port} is not where the site is served"))
                continue
            if host in SITE_HOSTS:
                target = split.path.rstrip("/") or "/"
                if split.fragment and target in SELF_PATHS:
                    out.append((url, "links into this book's own body; use an xref so the anchor is checked"))
                elif (
                    split.path.endswith("/")
                    and "." in split.path.rstrip("/").rsplit("/", 1)[-1]
                    and split.path not in declared
                ):
                    # The site treats "/x.html" and "/x.html/" as separate routes and
                    # spells both out where both work -- 32 such pairs in _redirects.
                    # Every rule here compiles slash-insensitively, so the normalised
                    # lookup below would silently validate the variant that was NOT
                    # asked for. Hence: a file path wearing a trailing slash is
                    # reported, UNLESS _redirects declares that exact spelling, which
                    # is the site saying it serves it -- "/videos.html/" is declared
                    # and must not be reported. Directory routes such as /blog/ and
                    # /javadoc/com/codename1/io/ have no dot in the last segment and
                    # never reach this branch.
                    out.append((url, "a file path with a trailing slash that _redirects does not declare"))
                elif not resolves(target, known, patterns):
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
    known, patterns, declared = site_paths(args.repo_root.resolve())
    if not known:
        raise SystemExit("could not derive any site paths; is --repo-root correct?")

    current: collections.Counter = collections.Counter()
    reasons: dict[str, str] = {}
    for path in sorted(guide_dir.rglob("*")):
        if path.suffix not in ASCIIDOC_EXTENSIONS or not path.is_file():
            continue
        name = path.relative_to(guide_dir).as_posix()
        for url, reason in findings_for(path, known, patterns, declared):
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
