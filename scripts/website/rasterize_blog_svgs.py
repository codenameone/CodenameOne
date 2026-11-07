#!/usr/bin/env python3
"""Render a PNG beside every SVG a blog post references.

DEV's image proxy (and other platforms) serves an SVG under an image/webp
content type, which no browser draws, so a syndicated chart simply vanishes.
Syndication swaps each SVG for the PNG written here; test_syndicate_blog_posts
fails when a post references an SVG that has no PNG beside it.

Needs Playwright with Chromium (the syndication runner's venv has both):
    python rasterize_blog_svgs.py            # missing PNGs only
    python rasterize_blog_svgs.py --force    # re-render all
"""

from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path

from syndicate_blog_posts import BLOG_DIR, REPO_ROOT, svg_local_path

SVG_REF_RE = re.compile(r'(?:src="|\]\()(/[^")\s]+\.svg)[")]')


def referenced_svgs(blog_dir: Path = BLOG_DIR) -> list[Path]:
    found = set()
    for path in blog_dir.glob("*.md"):
        for ref in SVG_REF_RE.findall(path.read_text(encoding="utf-8")):
            found.add(svg_local_path(ref))
    return sorted(found)


def main(argv: list[str]) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--force", action="store_true")
    args = parser.parse_args(argv)
    todo = [svg for svg in referenced_svgs()
            if svg.exists() and (args.force or not svg.with_suffix(".png").exists())]
    if not todo:
        print("Every referenced SVG has a PNG.")
        return 0
    from playwright.sync_api import sync_playwright
    with sync_playwright() as p:
        browser = p.chromium.launch()
        page = browser.new_page(device_scale_factor=2)
        for svg in todo:
            page.goto(svg.resolve().as_uri())
            box = page.evaluate("""() => { const r = document.documentElement.getBoundingClientRect();
                                         return {w: Math.ceil(r.width), h: Math.ceil(r.height)}; }""")
            page.set_viewport_size({"width": max(box["w"], 1), "height": max(box["h"], 1)})
            page.locator("svg").first.screenshot(path=str(svg.with_suffix(".png")), omit_background=False)
            print(f"wrote {svg.with_suffix('.png').relative_to(REPO_ROOT)}")
        browser.close()
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
