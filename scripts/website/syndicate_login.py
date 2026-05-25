#!/usr/bin/env python3
"""One-time login helper for the syndication runner.

Launches an `undetected-chromedriver`-managed Chrome (a stealth
Chrome/Chromium build that strips the JS fingerprints Cloudflare uses
to detect Selenium/Playwright). Opens the target site's verify URL
and watches the page URL — when you finish signing in (and complete
any Cloudflare challenge that does still appear) and arrive at the
verify URL, the script auto-exits. A persistent Chrome user-data-dir
under ``scripts/website/syndication-auth/chrome-profile/`` retains
cookies across runs, so subsequent logins to other sites and runs of
``syndicate_browser.py`` skip the login step.

Usage::

    pip install undetected-chromedriver selenium
    python scripts/website/syndicate_login.py --site medium
    python scripts/website/syndicate_login.py --site dzone

The auth directory is gitignored — never commit it.
"""

from __future__ import annotations

import argparse
import os
import platform
import re
import subprocess
import sys
import time
from pathlib import Path
from urllib.parse import urlparse


def detect_chrome_major_version() -> int | None:
    """Read the installed Chrome's major version so undetected-chromedriver
    fetches a matching chromedriver. The default behaviour fetches the
    *latest* chromedriver, which fails when the local Chrome is one
    release behind (very common — Chrome auto-updates lag).
    """
    candidates = []
    if platform.system() == "Darwin":
        candidates = [
            "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome",
            "/Applications/Chromium.app/Contents/MacOS/Chromium",
        ]
    elif platform.system() == "Linux":
        candidates = ["/usr/bin/google-chrome", "/usr/bin/chromium", "/usr/bin/chromium-browser"]
    elif platform.system() == "Windows":
        candidates = [
            r"C:\Program Files\Google\Chrome\Application\chrome.exe",
            r"C:\Program Files (x86)\Google\Chrome\Application\chrome.exe",
        ]
    for c in candidates:
        if not os.path.exists(c):
            continue
        try:
            out = subprocess.check_output([c, "--version"], stderr=subprocess.DEVNULL, timeout=5).decode()
            m = re.search(r"(\d+)\.\d+\.\d+", out)
            if m:
                return int(m.group(1))
        except Exception:
            continue
    return None


AUTH_DIR = Path(__file__).resolve().parent / "syndication-auth"
PROFILE_DIR = AUTH_DIR / "chrome-profile"

SITES: dict[str, dict[str, str]] = {
    "medium": {
        # /me/stories/drafts redirects to /me/stories on modern Medium
        # (drafts + published share one view). Use the post-redirect
        # path so the URL match fires.
        "verify_url": "https://medium.com/me/stories",
        "verify_hint": "Stories page should list your drafts/published.",
    },
    "dzone": {
        # /articles/post.html silently lands on dzone.com without an
        # editor; the actual editor is at /content/article/post.html.
        "verify_url": "https://dzone.com/content/article/post.html",
        "verify_hint": "Article editor should load with the title field visible.",
    },
}


def parse_args(argv: list[str]) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__,
                                     formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--site", required=True, choices=sorted(SITES))
    parser.add_argument("--profile-dir", default=str(PROFILE_DIR),
                        help=f"Chrome user-data-dir (default: {PROFILE_DIR}).")
    parser.add_argument("--chrome-version", type=int, default=None,
                        help="Chrome major version (default: auto-detect from installed Chrome).")
    parser.add_argument("--timeout-seconds", type=int, default=900,
                        help="Max seconds to wait for the verify URL (default: 900).")
    return parser.parse_args(argv)


def main(argv: list[str]) -> int:
    args = parse_args(argv)
    try:
        import undetected_chromedriver as uc
    except ImportError:
        print("undetected-chromedriver not installed. Run: pip install undetected-chromedriver", file=sys.stderr)
        return 2

    site = args.site
    info = SITES[site]
    profile_dir = Path(args.profile_dir)
    profile_dir.mkdir(parents=True, exist_ok=True)

    verify_path = urlparse(info["verify_url"]).path or "/"

    print(f"=== {site.upper()} LOGIN ===", flush=True)
    print(f"Profile dir (persists cookies/login across all sites): {profile_dir}", flush=True)

    options = uc.ChromeOptions()
    options.add_argument(f"--user-data-dir={profile_dir}")
    options.add_argument("--profile-directory=Default")
    # Reasonable defaults; let undetected-chromedriver handle the
    # stealth tweaks (navigator.webdriver, plugins, etc.).
    options.add_argument("--no-first-run")
    options.add_argument("--no-default-browser-check")

    chrome_version = args.chrome_version or detect_chrome_major_version()
    if chrome_version:
        print(f"Detected Chrome major version: {chrome_version}", flush=True)
    else:
        print("Could not detect Chrome version; using undetected-chromedriver default (latest).", flush=True)

    driver = uc.Chrome(
        options=options,
        headless=False,
        use_subprocess=True,
        version_main=chrome_version,
    )
    try:
        driver.get(info["verify_url"])
        print(f"Chrome window opened at: {info['verify_url']}", flush=True)
        print("If you see the page load directly, you're already signed in.", flush=True)
        print("Otherwise sign in (and complete any Cloudflare challenge),", flush=True)
        print(f"then navigate back to {info['verify_url']}.", flush=True)
        print(f"({info['verify_hint']})", flush=True)
        print(f"This script auto-exits when it sees you reach path '{verify_path}'.", flush=True)
        print(f"Times out after {args.timeout_seconds // 60} minutes.", flush=True)
        print(flush=True)

        deadline = time.time() + args.timeout_seconds
        ok = False
        while time.time() < deadline:
            try:
                current = driver.current_url
                current_path = urlparse(current).path or "/"
                if current_path == verify_path or current_path.startswith(verify_path.rstrip("/") + "/"):
                    time.sleep(3)  # let cookies settle
                    ok = True
                    print(f"Detected verify URL ({current}); profile saved at {profile_dir}.", flush=True)
                    break
            except Exception:
                # Driver may briefly error during navigation; retry.
                pass
            time.sleep(1)

        if not ok:
            print(f"Timed out after {args.timeout_seconds}s waiting for {info['verify_url']}.", file=sys.stderr)
            return 1
    finally:
        try:
            driver.quit()
        except Exception:
            pass

    print(f"Logged in to {site}. You can now run: python scripts/website/syndicate_browser.py --site {site}", flush=True)
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
