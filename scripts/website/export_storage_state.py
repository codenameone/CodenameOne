#!/usr/bin/env python3
"""Export a logged-in browser session for syndication targets that block
password-based automation (Medium has no password login at all; DZone
gates its login form behind invisible reCAPTCHA).

Two paths:

    --from-firefox-profile   read cookies straight from your existing Firefox
                             profile's cookies.sqlite (no second login)
    --browser {chrome,...}   launch Playwright with the chosen browser, open
                             the site's signin page, poll for auth cookies

Output is a Playwright storageState JSON written to disk and (unless
--no-base64) a base64 blob ready to paste as the {SITE}_STORAGE_STATE
repo secret consumed by syndicate_browser_posts.py.

Examples:

    python3 scripts/website/export_storage_state.py --site medium --from-firefox-profile
    python3 scripts/website/export_storage_state.py --site dzone --browser firefox
"""

from __future__ import annotations

import argparse
import base64
import glob
import json
import shutil
import sqlite3
import sys
import tempfile
import time
from pathlib import Path


DEFAULT_OUTPUT = Path("medium-storage-state.json")
DEFAULT_TIMEOUT_SECONDS = 600  # 10 minutes for the user to complete login

# Per-target site profile. Each entry knows where to land in a launched browser,
# which cookie domain to filter from a Firefox profile, and how to recognize
# a logged-in session (a function over the captured cookie list).
SITE_PROFILES: dict[str, dict] = {
    "medium": {
        "signin_url": "https://medium.com/m/signin",
        "cookie_host_glob": "%medium.com",
        # Medium assigns every visitor a `uid` cookie. Anonymous visitors get a
        # value prefixed with `lo_`; a signed-in user gets one without it.
        "is_logged_in": lambda cookies: any(
            c.get("name") == "uid" and not (c.get("value") or "").startswith("lo_")
            for c in cookies
        ),
    },
    "dzone": {
        "signin_url": "https://dzone.com/users/login.html",
        "cookie_host_glob": "%dzone.com",
        # DZone uses Spring Security's `remember-me` cookie for long-lived auth
        # plus a per-session `dz<hash>` cookie. Either one signals a logged-in
        # session.
        "is_logged_in": lambda cookies: any(
            c.get("name") == "remember-me" or (c.get("name") or "").startswith("dz")
            and (c.get("name") or "") not in ("dzuuid",)  # dzuuid is anonymous
            for c in cookies
        ),
    },
}


def parse_args(argv: list[str]) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--site", choices=sorted(SITE_PROFILES), default="medium",
                        help="Which target site to capture a session for (default: medium).")
    parser.add_argument("--output", default=None,
                        help="Path to write the storage state JSON (default: <site>-storage-state.json)")
    parser.add_argument("--no-base64", action="store_true",
                        help="Skip printing the base64 blob (just write the JSON file).")
    parser.add_argument("--timeout", type=int, default=DEFAULT_TIMEOUT_SECONDS,
                        help="Maximum seconds to wait for login completion (default: 600).")
    parser.add_argument("--interactive", action="store_true",
                        help="Wait for Enter on stdin instead of polling for auth cookies.")
    parser.add_argument("--browser", default="chrome", choices=["chrome", "chromium", "firefox", "msedge"],
                        help="Which Playwright browser to launch (default: chrome).")
    parser.add_argument("--from-firefox-profile", nargs="?", const="auto", default=None,
                        help=("Skip launching a browser and instead read medium.com cookies from an "
                              "existing Firefox profile's cookies.sqlite. Pass a path or omit for auto-detect."))
    return parser.parse_args(argv)


def _locate_firefox_profile(explicit: str | None) -> Path:
    if explicit and explicit != "auto":
        path = Path(explicit).expanduser()
        if path.is_file():
            return path
        if path.is_dir():
            candidate = path / "cookies.sqlite"
            if candidate.is_file():
                return candidate
        raise RuntimeError(f"Firefox cookies.sqlite not found at {path}")
    # Auto-detect macOS Firefox profile.
    base = Path.home() / "Library" / "Application Support" / "Firefox" / "Profiles"
    if not base.exists():
        # Linux / other-OS fallbacks.
        for guess in (Path.home() / ".mozilla" / "firefox", Path.home() / "snap" / "firefox" / "common" / ".mozilla" / "firefox"):
            if guess.exists():
                base = guess
                break
    if not base.exists():
        raise RuntimeError("Could not locate a Firefox profiles directory.")
    candidates = sorted(glob.glob(str(base / "*default*" / "cookies.sqlite"))) or sorted(
        glob.glob(str(base / "*" / "cookies.sqlite"))
    )
    if not candidates:
        raise RuntimeError(f"No cookies.sqlite found under {base}")
    # Prefer the most recently modified profile.
    return Path(max(candidates, key=lambda p: Path(p).stat().st_mtime))


def _firefox_storage_state(cookies_db: Path, host_glob: str) -> dict:
    # Copy to a temp file because Firefox holds a write lock on the live DB.
    with tempfile.NamedTemporaryFile(suffix=".sqlite", delete=False) as tmp:
        tmp_path = Path(tmp.name)
    shutil.copy2(cookies_db, tmp_path)
    try:
        conn = sqlite3.connect(f"file:{tmp_path}?mode=ro", uri=True)
        cur = conn.execute(
            "SELECT name, value, host, path, expiry, isSecure, isHttpOnly, sameSite "
            "FROM moz_cookies WHERE host LIKE ?",
            (host_glob,),
        )
        rows = cur.fetchall()
        conn.close()
    finally:
        tmp_path.unlink(missing_ok=True)
    samesite_map = {0: "None", 1: "Lax", 2: "Strict"}
    cookies = []
    for name, value, host, path, expiry, is_secure, is_http_only, same_site in rows:
        cookies.append({
            "name": name,
            "value": value,
            "domain": host if host.startswith(".") else "." + host,
            "path": path or "/",
            "expires": _normalize_expiry(expiry),
            "httpOnly": bool(is_http_only),
            "secure": bool(is_secure),
            "sameSite": samesite_map.get(int(same_site or 0), "None"),
        })
    return {"cookies": cookies, "origins": []}


def _normalize_expiry(raw: float | int | None) -> float:
    """Coerce a Firefox cookies.sqlite expiry into a Playwright-acceptable value.

    Playwright wants seconds-since-epoch (positive number) or -1 for session.
    Firefox stores `expiry` in seconds in older code but in milliseconds in
    newer entries. Anything past ~year 5138 must be milliseconds — divide.
    """
    if not raw:
        return -1.0
    value = float(raw)
    if value > 1e11:  # > ~year 5138 in seconds; treat as milliseconds.
        value = value / 1000.0
    return value


def main(argv: list[str]) -> int:
    args = parse_args(argv)
    profile = SITE_PROFILES[args.site]
    output_path = Path(args.output or f"{args.site}-storage-state.json").resolve()
    secret_name = f"{args.site.upper()}_STORAGE_STATE"

    if args.from_firefox_profile is not None:
        try:
            cookies_db = _locate_firefox_profile(args.from_firefox_profile)
        except RuntimeError as err:
            print(f"Error: {err}", file=sys.stderr)
            return 1
        print(f"Reading {args.site} cookies from Firefox profile: {cookies_db}")
        state = _firefox_storage_state(cookies_db, profile["cookie_host_glob"])
        if not profile["is_logged_in"](state["cookies"]):
            print(f"Error: this Firefox profile does not appear to be logged in to {args.site}.",
                  file=sys.stderr)
            return 1
        output_path.write_text(json.dumps(state), encoding="utf-8")
        print(f"Wrote storage state: {output_path}")
        print(f"  cookies captured: {len(state['cookies'])}")
        if not args.no_base64:
            encoded = base64.b64encode(output_path.read_bytes()).decode("ascii")
            print()
            print(f"Paste the following as the {secret_name} repository secret:")
            print("-" * 72)
            print(encoded)
            print("-" * 72)
        return 0

    try:
        from playwright.sync_api import sync_playwright
    except ImportError:
        print("Playwright is not installed. In a venv, run: pip install playwright && playwright install chromium",
              file=sys.stderr)
        return 1

    with sync_playwright() as pw:
        launch_kwargs: dict = {"headless": False}
        # The args namespace renamed channel to browser to allow Firefox.
        browser_choice = args.browser
        if browser_choice == "firefox":
            try:
                browser = pw.firefox.launch(headless=False)
            except Exception as err:  # noqa: BLE001
                print(f"Could not launch Playwright Firefox ({err}). "
                      "Run `playwright install firefox` and retry.", file=sys.stderr)
                return 1
        else:
            if browser_choice and browser_choice != "chromium":
                launch_kwargs["channel"] = browser_choice
            try:
                browser = pw.chromium.launch(**launch_kwargs)
            except Exception as err:  # noqa: BLE001 — channel may not be installed
                print(f"Could not launch with browser='{browser_choice}' ({err}); falling back to bundled Chromium.",
                      file=sys.stderr)
                browser = pw.chromium.launch(headless=False)

        context = browser.new_context(
            viewport={"width": 1280, "height": 900},
            user_agent=(
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 13_0) AppleWebKit/537.36 "
                "(KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
            ),
        )
        page = context.new_page()
        page.goto(profile["signin_url"])

        print()
        print("=" * 72)
        print(f"A browser window has opened on {args.site}'s sign-in page.")
        print("Log in (Google / email / whatever you normally use).")
        if args.interactive:
            print("When you can see your Medium home or profile, return here and press Enter.")
        else:
            print(f"The script will detect login automatically (waiting up to {args.timeout}s).")
        print("=" * 72)
        sys.stdout.flush()

        if args.interactive:
            try:
                input("Press Enter once you are logged in… ")
            except (KeyboardInterrupt, EOFError):
                print("Aborted.", file=sys.stderr)
                browser.close()
                return 1
        else:
            deadline = time.time() + args.timeout
            detected = False
            while time.time() < deadline:
                if profile["is_logged_in"](context.cookies(profile["signin_url"])):
                    detected = True
                    break
                time.sleep(3)
            if not detected:
                print(f"Timed out waiting for {args.site} login — auth cookies not detected.",
                      file=sys.stderr)
                browser.close()
                return 1
            print("Logged-in cookies detected — capturing session state…")
            # Give the site a couple seconds to finish setting localStorage.
            time.sleep(3)

        state = context.storage_state()
        output_path.write_text(json.dumps(state), encoding="utf-8")
        browser.close()

    print()
    print(f"Wrote storage state: {output_path}")
    print(f"  cookies captured: {len(state.get('cookies', []))}")
    print(f"  origins with localStorage: {len(state.get('origins', []))}")

    if args.no_base64:
        return 0

    encoded = base64.b64encode(output_path.read_bytes()).decode("ascii")
    print()
    print(f"Paste the following as the {secret_name} repository secret:")
    print("-" * 72)
    print(encoded)
    print("-" * 72)
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
