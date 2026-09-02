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
from pathlib import Path, PurePosixPath
from urllib.parse import urlsplit

ASCIIDOC_EXTENSIONS = {".adoc", ".asciidoc"}
URL_RE = re.compile(r"\bhttps?://[^\s\[\]<>\"'`)]+")
# A URL assembled from an attribute is invisible to the scan above: the
# declaration holds a valid site root and the use site holds only "{name}", so the
# path that actually ships is never checked. Expanding attributes properly means
# reimplementing asciidoctor's attribute resolution, including inheritance through
# includes, which is a lot of machinery for a construct the guide does not use --
# measured: zero URL-valued attribute declarations and zero link:{...} targets.
# So both spellings are refused instead, which keeps the gap from opening quietly.
ATTRIBUTE_URL_DECL_RE = re.compile(r"^:[A-Za-z0-9_-]+:\s*https?://")
ATTRIBUTE_LINK_RE = re.compile(r"\blink:\{[^}]+\}")
# A root-relative target names a website route just as an absolute URL does, but
# it carries no scheme, so URL_RE never sees it -- and check-guide-xrefs.py skips
# hrefs beginning with "/" because they are not same-page anchors. Between the two
# gates the route went unchecked, so it is run through the same route model here.
ROOT_RELATIVE_RE = re.compile(r"""(?:\blink:|\bhref=["'])(/[^\s\[\]"'`>]*)""")
# The one tree that genuinely cannot be enumerated from this repository: the
# Javadoc is produced from the framework sources at build time. Everything else,
# /developer-guide/ included, is derived below -- whitelisting a prefix silently
# exempts every path under it from the check.
GENERATED_PREFIXES = ("/javadoc/",)
_JAVADOC_ROOT: Path | None = None
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

    def day(key: str) -> str | None:
        value = str(meta.get(key, "")).strip().strip("\"'")
        return value[:10] if re.match(r"^\d{4}-\d{2}-\d{2}", value) else None

    # Hugo builds neither a future page nor an expired one. publishDate overrides
    # date for scheduling, and expiryDate withdraws a page that was published; a
    # date-only expiry lapses at midnight, so the day itself is already too late.
    for key in ("date", "publishDate"):
        scheduled = day(key)
        if scheduled and scheduled > today:
            return False
    expiry = day("expiryDate")
    return not (expiry and expiry <= today)


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


# build_javadocs.sh generates the API docs from these two roots, so the published
# tree is derivable from the repository without building it.
JAVADOC_SOURCE_ROOTS = ("CodenameOne/src", "Ports/CLDC11/src")
# Pages javadoc emits for a package rather than for a type.
# build_javadocs.sh filters these out of its source list, passes -exclude for them
# and then guards that they never reached the output. Recording them here would
# accept a link to a page the published tree deliberately does not contain.
JAVADOC_EXCLUDED_PACKAGES = ("com/codename1/impl",)
JAVADOC_PACKAGE_PAGES = {
    "package-summary.html",
    "package-frame.html",
    "package-use.html",
    "package-tree.html",
}
_javadoc_index: tuple[set[str], set[str]] | None = None


def javadoc_index(repo_root: Path) -> tuple[set[str], set[str]]:
    """Package directories and class names the generated Javadoc will contain."""
    global _javadoc_index
    if _javadoc_index is not None:
        return _javadoc_index
    packages: set[str] = set()
    classes: set[str] = set()
    for root_name in JAVADOC_SOURCE_ROOTS:
        root = repo_root / root_name
        if not root.exists():
            continue
        for source in root.rglob("*.java"):
            relative = source.relative_to(root)
            package = relative.parent.as_posix()
            if any(
                package == excluded or package.startswith(excluded + "/")
                for excluded in JAVADOC_EXCLUDED_PACKAGES
            ):
                continue
            packages.add(package)
            classes.add(f"{package}/{relative.stem}")
    _javadoc_index = (packages, classes)
    return _javadoc_index


def javadoc_path_exists(target: str) -> bool:
    """Whether a /javadoc/ path names something the generated tree will hold.

    The prefix used to be accepted wholesale, on the grounds that the tree is
    generated at build time and cannot be enumerated here. It can: the generator
    runs over two fixed source roots, so a package is a directory and a class page
    is a .java file. That distinction matters -- the guide linked three times to
    /javadoc/com/codename1/JavaScript/, and the package is lowercase, so a
    case-sensitive host served 404s while this reported success.

    Anything that is not a recognisable package or class page is still accepted:
    javadoc emits index pages, class-use trees and frames this does not model, and
    reporting those would be a false alarm rather than a finding.
    """
    if _JAVADOC_ROOT is None:
        return True
    path = target[len("/javadoc/"):] if target.startswith("/javadoc/") else target
    if not path.endswith(".html"):
        return True  # a directory or an asset, not a documented type
    packages, classes = javadoc_index(_JAVADOC_ROOT)
    package = str(PurePosixPath(path).parent)
    page = PurePosixPath(path).name
    if package == ".":
        return True  # a top-level index or overview page, outside the model
    # class-use/ holds one page per type, alongside the package it belongs to.
    if package.endswith("/class-use"):
        package = package[: -len("/class-use")]
    if package not in packages:
        # The package itself will not exist in the generated tree. This is the
        # case that mattered: com/codename1/JavaScript is spelt lowercase in the
        # sources, so the published path 404s on a case-sensitive host.
        return False
    if page in JAVADOC_PACKAGE_PAGES:
        return True
    # A nested type is documented as Outer.Inner.html, generated from Outer.java.
    return f"{package}/{page[:-5].split('.')[0]}" in classes


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
        # test belongs here rather than only at the call site. The javadoc tree is
        # not accepted wholesale -- javadoc_path_exists() checks the package and
        # class against the sources it is generated from.
        return javadoc_path_exists(target)
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
    function = repo_root / "docs/website/functions/[[path]].js"
    if function.exists():
        # Read the prefixes out of the Function rather than restating them here,
        # so removing a fallback removes it from the model too. If it is ever
        # rewritten in a shape this cannot read, the derivation yields nothing and
        # links under those prefixes start failing -- loudly, which is the safe
        # direction; a hardcoded pair would have gone on accepting them.
        for prefix in sorted(
            set(
                re.findall(
                    r'path\.startsWith\("/([^/"]+)/"\)',
                    function.read_text(encoding="utf-8"),
                )
            )
        ):
            patterns.append(
                (
                    re.compile(rf"^/{re.escape(prefix)}(/.*)?$"),
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


def bare_authority(split, host: str, port: int | None) -> bool:
    """Whether the authority carries nothing but the host and, at most, its port."""
    netloc = split.netloc.lower()
    if netloc.endswith("."):
        netloc = netloc[:-1]          # the DNS root dot, already stripped from host
    elif ":" in netloc and netloc.rsplit(":", 1)[0].endswith("."):
        netloc = netloc.replace(".:", ":", 1)
    return netloc == (host if port is None else f"{host}:{port}")


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
        if ATTRIBUTE_URL_DECL_RE.match(line.strip()):
            out.append((line.strip().split()[0], "an attribute holding a URL: any link built from it is unchecked, because this does not expand attributes"))
        if ATTRIBUTE_LINK_RE.search(line):
            out.append((ATTRIBUTE_LINK_RE.search(line).group(0), "a link target built from an attribute, which this cannot expand or check"))
        for target in ROOT_RELATIVE_RE.findall(line):
            path = target.split("#", 1)[0].split("?", 1)[0]
            normalized = normalize_path(path)
            if not resolves(normalized, known, patterns):
                out.append((target, "the website serves no such path (checked _redirects and the content tree)"))
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
            if host in SITE_HOSTS and not bare_authority(split, host, port):
                # Everything below identifies the site by hostname alone, and
                # urlsplit is forgiving about what else the authority may carry:
                # userinfo, a bracketed literal, mixed case. Each is a different
                # way of writing something this route model has not been shown to
                # describe, so classify on the bare form only and report the rest,
                # rather than growing one rule per spelling. Measured: the guide
                # has no URL with userinfo, a non-ASCII host, an IPv6 literal or
                # mixed case in the authority.
                out.append((url, f"authority '{split.netloc}' is not a bare hostname"))
                continue
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
    global _JAVADOC_ROOT
    _JAVADOC_ROOT = args.repo_root.resolve()
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
