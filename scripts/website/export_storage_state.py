#!/usr/bin/env python3
"""Export a Medium login session as a base64 string for the MEDIUM_STORAGE_STATE secret.

Medium has no password-based REST API and no password-based login form —
only OAuth (Google/Apple/Facebook) and email magic-link. The browser-based
syndicator therefore needs a saved Playwright storageState (cookies +
localStorage) instead of credentials.

Usage:

    python3 scripts/website/export_medium_storage.py
        # opens a headed browser; you log in; press Enter when done
        # script writes ./medium-storage-state.json AND prints the base64
        # representation that you paste as the MEDIUM_STORAGE_STATE secret

If the user has Google Chrome installed, the script uses Chrome (channel=
"chrome") so the browser the user sees is the same brand they are used to.
Otherwise it falls back to Playwright's bundled Chromium.
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
SIGNIN_URL = "https://medium.com/m/signin"
# Medium assigns every visitor a `uid` cookie. Anonymous visitors get a value
# prefixed with `lo_` ("logged-out"); a real signed-in user gets a value
# without that prefix. We use that distinction to detect login completion.
ANON_UID_PREFIX = "lo_"
DEFAULT_TIMEOUT_SECONDS = 600  # 10 minutes for the user to complete login


def _is_logged_in(cookies: list[dict]) -> bool:
    for c in cookies:
        if c.get("name") == "uid":
            value = c.get("value") or ""
            if value and not value.startswith(ANON_UID_PREFIX):
                return True
    return False


def parse_args(argv: list[str]) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output", default=str(DEFAULT_OUTPUT),
                        help=f"Path to write the storage state JSON (default: {DEFAULT_OUTPUT})")
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


def _firefox_storage_state(cookies_db: Path) -> dict:
    # Copy to a temp file because Firefox holds a write lock on the live DB.
    with tempfile.NamedTemporaryFile(suffix=".sqlite", delete=False) as tmp:
        tmp_path = Path(tmp.name)
    shutil.copy2(cookies_db, tmp_path)
    try:
        conn = sqlite3.connect(f"file:{tmp_path}?mode=ro", uri=True)
        cur = conn.execute(
            "SELECT name, value, host, path, expiry, isSecure, isHttpOnly, sameSite "
            "FROM moz_cookies WHERE host LIKE '%medium.com'"
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
            "expires": float(expiry) if expiry else -1.0,
            "httpOnly": bool(is_http_only),
            "secure": bool(is_secure),
            "sameSite": samesite_map.get(int(same_site or 0), "None"),
        })
    return {"cookies": cookies, "origins": []}


def main(argv: list[str]) -> int:
    args = parse_args(argv)
    output_path = Path(args.output).resolve()

    if args.from_firefox_profile is not None:
        try:
            cookies_db = _locate_firefox_profile(args.from_firefox_profile)
        except RuntimeError as err:
            print(f"Error: {err}", file=sys.stderr)
            return 1
        print(f"Reading Medium cookies from Firefox profile: {cookies_db}")
        state = _firefox_storage_state(cookies_db)
        if not _is_logged_in(state["cookies"]):
            print("Error: this Firefox profile does not appear to be logged in to Medium "
                  "(no `uid` cookie without the `lo_` prefix).", file=sys.stderr)
            return 1
        output_path.write_text(json.dumps(state), encoding="utf-8")
        print(f"Wrote storage state: {output_path}")
        print(f"  cookies captured: {len(state['cookies'])}")
        if not args.no_base64:
            encoded = base64.b64encode(output_path.read_bytes()).decode("ascii")
            print()
            print("Paste the following as the MEDIUM_STORAGE_STATE repository secret:")
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
        page.goto(SIGNIN_URL)

        print()
        print("=" * 72)
        print("A browser window has opened on Medium's sign-in page.")
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
                if _is_logged_in(context.cookies("https://medium.com/")):
                    detected = True
                    break
                time.sleep(3)
            if not detected:
                print("Timed out waiting for Medium login — no `uid` cookie without the `lo_` prefix.",
                      file=sys.stderr)
                browser.close()
                return 1
            print("Logged-in `uid` cookie detected — capturing session state…")
            # Give Medium a couple seconds to finish setting localStorage.
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
    print("Paste the following as the MEDIUM_STORAGE_STATE repository secret:")
    print("-" * 72)
    print(encoded)
    print("-" * 72)
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
