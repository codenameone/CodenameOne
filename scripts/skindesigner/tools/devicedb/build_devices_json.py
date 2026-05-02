#!/usr/bin/env python3
"""
Build the bundled devices.json catalog used by the Skin Designer.

Scrapes GSMArena directly so we always get the freshest specs (third-party
GitHub mirrors lag months behind). The scraper:

  * walks the brand pages we care about,
  * follows each phone link to its detail page,
  * extracts resolution / PPI / OS / form factor from the data-spec table,
  * normalises everything, and
  * writes a compact JSON file the CN1 app loads at startup.

This script is designed to run from CI on a schedule. To stay polite to
GSMArena and avoid IP bans we:
  * sleep --delay seconds between requests (default 1.0s),
  * cache fetched HTML to disk so repeated runs only re-fetch what changed,
  * retry with exponential backoff on transient failures,
  * merge new records into the existing devices.json so partial scrapes
    never lose the existing catalog.

Usage:
    python3 build_devices_json.py [--brands BRAND_SLUG,...] [--full]
                                  [--max-pages N] [--delay SECONDS]
                                  [--out PATH]

Default brand list focuses on devices likely to be skinned (~12 brands).
Pass `--full` to walk every brand on makers.php3. Pass an explicit
`--brands` to override.

No third-party packages required; everything uses the stdlib.
"""
from __future__ import annotations

import argparse
import gzip
import hashlib
import json
import os
import random
import re
import sys
import time
import urllib.error
import urllib.request
from html import unescape
from typing import Iterable, Optional

BASE = "https://www.gsmarena.com/"
USER_AGENT = (
    "Mozilla/5.0 (Macintosh; Intel Mac OS X 14_0) AppleWebKit/605.1.15 "
    "(KHTML, like Gecko) Version/17.1 Safari/605.1.15 "
    "(skin-designer-devicedb-builder; https://github.com/codenameone/CodenameOne)"
)

DEFAULT_OUT = os.path.normpath(
    os.path.join(
        os.path.dirname(__file__),
        "..", "..", "common", "src", "main", "resources", "devices.json"
    )
)
CACHE_DIR = os.path.normpath(os.path.join(os.path.dirname(__file__), "cache"))

# Curated brand slugs — everything users are likely to skin. Slug format
# matches the URLs at https://www.gsmarena.com/makers.php3.
DEFAULT_BRANDS = [
    "apple-phones-48",
    "samsung-phones-9",
    "google-phones-107",
    "huawei-phones-58",
    "xiaomi-phones-80",
    "oneplus-phones-95",
    "oppo-phones-82",
    "vivo-phones-98",
    "motorola-phones-4",
    "sony-phones-7",
    "nokia-phones-1",
    "asus-phones-46",
    "lg-phones-20",
    "htc-phones-45",
    "blackberry-phones-36",
    "honor-phones-121",
    "realme-phones-118",
    "nothing-phones-128",
]

RE_RES = re.compile(r"(\d{3,5})\s*[x×]\s*(\d{3,5})")
RE_PPI = re.compile(r"~?\s*(\d{2,4})\s*ppi")
RE_SIZE = re.compile(r"([\d.]+)\s*inches?")
RE_YEAR = re.compile(r"\b(19|20)(\d{2})\b")
RE_PHONE_LINK = re.compile(r'<li><a href="([a-z0-9_()]+-\d+\.php)"[^>]*>.*?<strong[^>]*><span>([^<]+)</span></strong></a></li>',
                            re.S | re.I)
RE_NAV_PAGE = re.compile(r'href="([a-z0-9_-]+-p\d+\.php)"', re.I)
RE_DATASPEC = re.compile(r'data-spec="([a-z\-]+)"[^>]*>([^<]+)<', re.I)
RE_TITLE = re.compile(r"<title>([^<]+?)\s*-\s*Full phone specifications</title>", re.I)
RE_BRAND_FROM_SLUG = re.compile(r"^([a-z0-9]+)-phones-\d+$", re.I)
RE_BRAND_LINK = re.compile(r'<a href="([a-z0-9-]+-phones-\d+\.php)"[^>]*>\s*<strong[^>]*>([^<]+)</strong>',
                            re.I)

IPHONE_ISLAND_NAMES = re.compile(
    r"iphone\s+(14\s*pro|1[5-9](\s|$|\s*(plus|pro|pro\s*max|mini|air|ultra)))",
    re.I,
)
IPHONE_NOTCH_NAMES = re.compile(
    r"iphone\s+(x[srm]?(\s|$)|1[1-3](\s|$|\s*(pro|pro\s*max|mini|plus))|14(\s|$|\s*plus))",
    re.I,
)


def cache_path(url: str) -> str:
    h = hashlib.sha1(url.encode("utf-8")).hexdigest()[:16]
    return os.path.join(CACHE_DIR, h + ".html.gz")


def http_get(url: str, *, delay: float = 1.0, retries: int = 4, use_cache: bool = True) -> str:
    """Fetch a URL with caching, polite delay and retry/backoff."""
    if use_cache:
        os.makedirs(CACHE_DIR, exist_ok=True)
        cp = cache_path(url)
        if os.path.exists(cp):
            with gzip.open(cp, "rt", encoding="utf-8") as f:
                return f.read()
    last_err: Optional[Exception] = None
    for attempt in range(retries):
        try:
            req = urllib.request.Request(url, headers={
                "User-Agent": USER_AGENT,
                "Accept-Encoding": "gzip",
                "Accept": "text/html,application/xhtml+xml",
                "Accept-Language": "en-US,en;q=0.9",
            })
            with urllib.request.urlopen(req, timeout=30) as resp:
                raw = resp.read()
                if resp.headers.get("Content-Encoding") == "gzip":
                    raw = gzip.decompress(raw)
            html = raw.decode("utf-8", "replace")
            if use_cache:
                with gzip.open(cache_path(url), "wt", encoding="utf-8") as f:
                    f.write(html)
            time.sleep(delay + random.uniform(0, 0.4))
            return html
        except (urllib.error.HTTPError, urllib.error.URLError, TimeoutError) as e:
            last_err = e
            wait = (2 ** attempt) + random.uniform(0, 1.5)
            sys.stderr.write(f"  ! {url} -> {e}; backoff {wait:.1f}s\n")
            time.sleep(wait)
    raise RuntimeError(f"Failed to fetch {url}: {last_err}")


def discover_brands() -> list[tuple[str, str]]:
    """Returns [(brand_label, brand_slug_with_id), ...] from makers.php3."""
    html = http_get(BASE + "makers.php3", delay=0.0)
    seen: dict[str, str] = {}
    for m in RE_BRAND_LINK.finditer(html):
        page = m.group(1)
        label = unescape(m.group(2)).strip()
        slug = page.replace(".php", "")
        seen.setdefault(slug, label)
    return sorted(seen.items())


def walk_brand(slug: str, *, max_pages: int = 999, delay: float = 1.0) -> Iterable[tuple[str, str]]:
    """Yields (phone_url, model_name) for one brand, walking pagination."""
    seen_pages: set[str] = set()
    queue: list[str] = [slug + ".php"]
    yielded_slugs: set[str] = set()
    pages_fetched = 0
    while queue and pages_fetched < max_pages:
        page = queue.pop(0)
        if page in seen_pages:
            continue
        seen_pages.add(page)
        pages_fetched += 1
        try:
            html = http_get(BASE + page, delay=delay)
        except RuntimeError as err:
            sys.stderr.write(f"  ! brand page {page} failed: {err}\n")
            continue
        for m in RE_PHONE_LINK.finditer(html):
            phone_url = m.group(1)
            if phone_url.endswith("-review.php"):
                continue
            if phone_url in yielded_slugs:
                continue
            yielded_slugs.add(phone_url)
            yield phone_url, unescape(m.group(2)).strip()
        for m in RE_NAV_PAGE.finditer(html):
            np = m.group(1)
            if np not in seen_pages:
                queue.append(np)


def parse_phone_page(html: str) -> dict:
    """Pulls every data-spec field out of a phone detail page."""
    out = {}
    for m in RE_DATASPEC.finditer(html):
        key = m.group(1).lower()
        # Skip the keys we don't care about — keeps memory tiny on huge pages.
        if key not in ("displaytype", "displaysize", "displayresolution",
                        "os", "year", "modelname", "dimensions", "status"):
            continue
        out[key] = unescape(m.group(2)).strip()
    tm = RE_TITLE.search(html)
    if tm:
        out["title"] = unescape(tm.group(1)).strip()
    return out


def detect_platform(os_str: str) -> str:
    s = os_str.lower()
    if "ios" in s or "ipados" in s:
        return "ios"
    if "android" in s:
        return "and"
    return ""


def has_notch(model: str, plat: str) -> bool:
    if plat != "ios":
        return False
    return bool(IPHONE_NOTCH_NAMES.search(model)) and not IPHONE_ISLAND_NAMES.search(model)


def has_island(model: str, plat: str) -> bool:
    if plat != "ios":
        return False
    return bool(IPHONE_ISLAND_NAMES.search(model))


def has_hole(plat: str, year: Optional[int]) -> bool:
    return plat == "and" and year is not None and year >= 2019


def is_foldable(model: str, display_type: str) -> bool:
    blob = (model + " " + display_type).lower()
    return any(n in blob for n in (
        "fold", "flip", "razr 5g", "razr 40", "razr+", "razr 2024",
        "x fold", "magic v", "mate x", "tri-fold"
    ))


def normalise(brand: str, model: str, specs: dict) -> Optional[dict]:
    title = specs.get("title", brand + " " + model)
    name = title.replace(brand + " ", "", 1) if title.startswith(brand + " ") else (model or title)
    name = name.strip()
    if not name:
        return None

    res = specs.get("displayresolution", "")
    rm = RE_RES.search(res)
    if not rm:
        return None
    w, h = int(rm.group(1)), int(rm.group(2))
    if w < 320 or h < 480 or w * h < 320 * 480 or w > 4500 or h > 4500:
        return None

    pm = RE_PPI.search(res)
    ppi = int(pm.group(1)) if pm else None
    if not ppi:
        return None

    sm = RE_SIZE.search(specs.get("displaysize", ""))
    inches = float(sm.group(1)) if sm else None

    ym = RE_YEAR.search(specs.get("year", "") or specs.get("status", ""))
    year = int(ym.group(1) + ym.group(2)) if ym else None
    if year is None:
        return None

    plat = detect_platform(specs.get("os", ""))
    if plat not in ("ios", "and"):
        return None

    tablet = inches is not None and inches >= 7.0
    foldable = is_foldable(name, specs.get("displaytype", ""))

    rec = {
        "id": (brand + "_" + name).lower().replace(" ", "_").replace("/", "_").replace("(", "").replace(")", ""),
        "brand": brand,
        "name": name,
        "os": (specs.get("os", "") or "")[:60],
        "year": year,
        "form": "Foldable" if foldable else ("Tablet" if tablet else "Phone"),
        "tablet": tablet,
        "w": w,
        "h": h,
        "ppi": ppi,
        "inches": inches,
        "platform": plat,
        "hasNotch": has_notch(name, plat),
        "hasIsland": has_island(name, plat),
        "hasHole": has_hole(plat, year),
    }
    rec["hasHome"] = (plat == "ios" and (rec["hasNotch"] or rec["hasIsland"])) or (
        plat == "ios" and tablet and year >= 2018
    )
    if rec["hasIsland"]:
        rec["safeTop"], rec["safeBottom"] = 59, 34
    elif rec["hasNotch"]:
        rec["safeTop"], rec["safeBottom"] = 47, 34
    elif rec["hasHole"]:
        rec["safeTop"], rec["safeBottom"] = 40, 0
    elif tablet:
        rec["safeTop"], rec["safeBottom"] = 24, 24
    else:
        rec["safeTop"], rec["safeBottom"] = 20, 0
    if plat == "ios":
        rec["fonts"] = {"system": "SF Pro", "prop": "SF Pro", "mono": "SF Mono",
                         "small": 13 if tablet else 12,
                         "medium": 17 if tablet else 15,
                         "large": 24 if tablet else 22}
    else:
        rec["fonts"] = {"system": "Roboto", "prop": "Roboto", "mono": "Roboto Mono",
                         "small": 12, "medium": 14, "large": 20}
    return rec


def brand_label_from_slug(slug: str, label_map: dict[str, str]) -> str:
    if slug in label_map:
        return label_map[slug]
    m = RE_BRAND_FROM_SLUG.match(slug)
    return m.group(1).title() if m else slug


def merge(existing: list[dict], fresh: list[dict]) -> list[dict]:
    by_id: dict[str, dict] = {r["id"]: r for r in existing}
    for r in fresh:
        prev = by_id.get(r["id"])
        if prev is None or (r.get("year") or 0) >= (prev.get("year") or 0):
            by_id[r["id"]] = r
    return list(by_id.values())


def load_existing(path: str) -> list[dict]:
    if not os.path.exists(path):
        return []
    try:
        with open(path, "r", encoding="utf-8") as f:
            payload = json.load(f)
        return payload.get("devices", [])
    except (OSError, json.JSONDecodeError):
        return []


RE_LATEST_LINK = re.compile(
    r'<a href="([a-z0-9_()]+-\d+\.php)"[^>]*>\s*<img[^>]*alt="([^"]+)"[^>]*>\s*</a>',
    re.I,
)


def walk_latest(*, max_pages: int = 1, delay: float = 1.0) -> Iterable[tuple[str, str, str]]:
    """Yields (phone_url, brand, model) from latest-mobiles.php3.

    The "latest" page lists newly-added devices across every brand. It's the
    cheapest source of fresh data — one or two listing pages cover the last
    few weeks of releases.
    """
    seen_pages: set[str] = set()
    queue: list[str] = ["latest-mobiles.php3"]
    pages_fetched = 0
    while queue and pages_fetched < max_pages:
        page = queue.pop(0)
        if page in seen_pages:
            continue
        seen_pages.add(page)
        pages_fetched += 1
        try:
            html = http_get(BASE + page, delay=delay)
        except RuntimeError as err:
            sys.stderr.write(f"  ! latest page {page} failed: {err}\n")
            continue
        for m in RE_LATEST_LINK.finditer(html):
            phone_url = m.group(1)
            if phone_url.endswith("-review.php"):
                continue
            alt = unescape(m.group(2)).strip()
            # alt is "<Brand> <Model>"; split on first space to recover.
            parts = alt.split(" ", 1)
            brand = parts[0] if parts else "Unknown"
            model = parts[1] if len(parts) > 1 else alt
            yield phone_url, brand, model
        # Pagination: latest-mobiles-pN.php
        for m in RE_NAV_PAGE.finditer(html):
            np = m.group(1)
            if np.startswith("latest-mobiles") and np not in seen_pages:
                queue.append(np)


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__,
                                  formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--mode", choices=("brands", "latest"), default="brands",
                     help="brands = walk per-brand listings (large/slow); "
                          "latest = scrape recent additions only (fast/trickle)")
    ap.add_argument("--brands", default=",".join(DEFAULT_BRANDS),
                     help="Comma-separated brand slugs (no .php). Use --full to walk all brands instead.")
    ap.add_argument("--full", action="store_true",
                     help="Discover and walk every brand on makers.php3 (only with --mode brands)")
    ap.add_argument("--max-pages", type=int, default=10,
                     help="Cap on listing pages walked per brand (or per latest run)")
    ap.add_argument("--min-year", type=int, default=2014,
                     help="Drop devices announced before this year")
    ap.add_argument("--delay", type=float, default=1.0,
                     help="Polite delay between HTTP requests, seconds")
    ap.add_argument("--out", default=DEFAULT_OUT, help="Output devices.json path")
    ap.add_argument("--no-cache", action="store_true",
                     help="Disable on-disk HTML cache")
    ap.add_argument("--limit", type=int, default=0,
                     help="Stop after this many phones (0 = no limit)")
    args = ap.parse_args()

    use_cache = not args.no_cache
    existing = load_existing(args.out)
    fresh: list[dict] = []
    seen: set[str] = set()
    total_phones = 0

    if args.mode == "latest":
        sys.stderr.write(f"Trickle-scraping latest-mobiles "
                          f"(max-pages={args.max_pages}, limit={args.limit or 'none'}, "
                          f"delay={args.delay}s)…\n")
        for url, brand, model in walk_latest(max_pages=args.max_pages, delay=args.delay):
            if url in seen:
                continue
            seen.add(url)
            try:
                html = http_get(BASE + url, delay=args.delay, use_cache=use_cache)
            except RuntimeError as err:
                sys.stderr.write(f"  ! skip {url}: {err}\n")
                continue
            specs = parse_phone_page(html)
            rec = normalise(brand, model, specs)
            if rec is not None and rec["year"] >= args.min_year:
                fresh.append(rec)
                total_phones += 1
                if args.limit and total_phones >= args.limit:
                    sys.stderr.write(f"  · hit --limit {args.limit}, stopping\n")
                    break
    else:
        if args.full:
            sys.stderr.write("Discovering all brands…\n")
            all_brands = discover_brands()
            target = [(s, l) for s, l in all_brands if s.endswith(tuple(f"-{i}" for i in range(1000)))]
            if not target:
                target = all_brands
        else:
            slugs = [b.strip() for b in args.brands.split(",") if b.strip()]
            try:
                label_map = dict(discover_brands())
            except Exception:
                label_map = {}
            target = [(s, brand_label_from_slug(s, label_map)) for s in slugs]

        sys.stderr.write(f"Scraping {len(target)} brand(s) at {args.delay}s/request…\n")

        for slug, label in target:
            sys.stderr.write(f"\n=== {label} [{slug}] ===\n")
            for url, model in walk_brand(slug, max_pages=args.max_pages, delay=args.delay):
                if url in seen:
                    continue
                seen.add(url)
                try:
                    html = http_get(BASE + url, delay=args.delay, use_cache=use_cache)
                except RuntimeError as err:
                    sys.stderr.write(f"  ! skip {url}: {err}\n")
                    continue
                specs = parse_phone_page(html)
                rec = normalise(label, model, specs)
                if rec is not None and rec["year"] >= args.min_year:
                    fresh.append(rec)
                    total_phones += 1
                    if args.limit and total_phones >= args.limit:
                        sys.stderr.write(f"  · hit --limit {args.limit}, stopping\n")
                        break
            if args.limit and total_phones >= args.limit:
                break

    merged = merge(existing, fresh)
    merged.sort(key=lambda r: (-(r["year"] or 0), r["brand"].lower(), r["name"].lower()))

    payload = {
        "version": 2,
        "generator": "build_devices_json.py",
        "source": BASE,
        "fresh_count": len(fresh),
        "count": len(merged),
        "devices": merged,
    }
    out_dir = os.path.dirname(args.out)
    if out_dir and not os.path.isdir(out_dir):
        os.makedirs(out_dir, exist_ok=True)
    with open(args.out, "w", encoding="utf-8") as f:
        json.dump(payload, f, separators=(",", ":"), ensure_ascii=False)
    sys.stderr.write(f"\nWrote {len(merged)} devices "
                      f"({len(fresh)} from this run) to {args.out}\n")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
