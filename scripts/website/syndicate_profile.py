#!/usr/bin/env python3
"""Profile-bootstrap helpers shared by syndicate_login.py and
syndicate_browser.py.

The syndication scripts drive a separate Firefox process than the user's
day-to-day browser. To avoid making the user re-authenticate (and re-pass
Cloudflare) for that process, we bootstrap each per-site Playwright
profile by copying state from the user's real Firefox profile on first
run. The copy excludes caches and lock files so it stays small and
doesn't fight Firefox's own per-profile lock.
"""

from __future__ import annotations

import os
import platform
import shutil
import sys
from pathlib import Path

# Surgical bootstrap: copy ONLY auth-state files from the user's real
# Firefox profile. We deliberately skip prefs.js, compatibility.ini,
# extensions.json, and similar version-bound files because Playwright
# launches its own bundled Nightly build, which refuses to start
# against a profile annotated for a different Firefox version (the
# previous full-copy attempt failed for exactly this reason — Nightly
# read compatibility.ini, decided the profile was for "another
# Firefox", and exited 0 without showing a window).
_ESSENTIAL_FILES = (
    "cookies.sqlite",
    "cookies.sqlite-wal",
    "cookies.sqlite-shm",
    "key4.db",          # encrypted credential keystore
    "logins.json",      # saved password/login form data
    "permissions.sqlite",  # per-site permission grants
    "cert9.db",         # cert / TLS exception store
)
# Storage subdirs that hold per-origin localStorage / IndexedDB. Copy
# the whole tree (small, no version metadata).
_ESSENTIAL_DIRS = ("storage",)


def find_user_firefox_profile() -> Path | None:
    """Best-effort locate the user's primary Firefox profile dir.

    Prefers ``*.default-release`` (modern Firefox), falls back to
    ``*.default``. Returns ``None`` if no profile dir is found.
    """
    system = platform.system()
    if system == "Darwin":
        base = Path.home() / "Library" / "Application Support" / "Firefox" / "Profiles"
    elif system == "Linux":
        base = Path.home() / ".mozilla" / "firefox"
    elif system == "Windows":
        appdata = os.environ.get("APPDATA")
        if not appdata:
            return None
        base = Path(appdata) / "Mozilla" / "Firefox" / "Profiles"
    else:
        return None
    if not base.exists():
        return None
    for pattern in ("*.default-release", "*.default"):
        for candidate in sorted(base.glob(pattern)):
            if candidate.is_dir():
                return candidate
    return None


def is_profile_initialized(profile_dir: Path) -> bool:
    """A Playwright-managed profile is "initialized" once Firefox has
    written cookies into it. Use that as the trigger for skipping the
    one-time copy."""
    return (profile_dir / "cookies.sqlite").exists()


def bootstrap_profile_from_user(
    profile_dir: Path,
    user_profile: Path | None = None,
    *,
    log: callable = print,
) -> bool:
    """Copy auth-state files from the user's real Firefox profile into
    ``profile_dir`` if the latter is empty. Returns ``True`` if any
    copy happened, ``False`` otherwise.

    Surgical: only copies cookies, saved logins, permissions, certs,
    and per-origin storage — never prefs.js / compatibility.ini /
    extensions.json (which would version-conflict with Playwright's
    bundled Firefox build and make it exit on launch).
    """
    if is_profile_initialized(profile_dir):
        return False
    src = user_profile or find_user_firefox_profile()
    if src is None:
        log("No user Firefox profile found; starting with an empty profile.")
        return False
    if not src.exists():
        log(f"User Firefox profile {src} does not exist; starting empty.")
        return False
    log(f"Bootstrapping {profile_dir} from your Firefox profile at {src}")
    log("(Copying cookies + logins only — prefs/extensions stay fresh "
        "to avoid Firefox version-mismatch refusal.)")
    profile_dir.mkdir(parents=True, exist_ok=True)
    copied = 0
    for fname in _ESSENTIAL_FILES:
        sf = src / fname
        if not sf.exists():
            continue
        try:
            shutil.copy2(sf, profile_dir / fname)
            copied += 1
        except Exception as err:
            log(f"  failed to copy {fname}: {err}")
    for dname in _ESSENTIAL_DIRS:
        sd = src / dname
        if not sd.is_dir():
            continue
        try:
            shutil.copytree(
                sd, profile_dir / dname,
                ignore=shutil.ignore_patterns("*.tmp", "*.lock"),
                dirs_exist_ok=True,
                symlinks=True,
            )
            copied += 1
        except Exception as err:
            log(f"  failed to copy {dname}/: {err}")
    log(f"Copied {copied} item(s).")
    return copied > 0


def default_firefox_executable() -> str | None:
    """Best-guess path to the user's installed Firefox binary."""
    system = platform.system()
    candidates: list[str] = []
    if system == "Darwin":
        candidates = [
            "/Applications/Firefox.app/Contents/MacOS/firefox",
            "/Applications/Firefox Developer Edition.app/Contents/MacOS/firefox",
        ]
    elif system == "Linux":
        candidates = ["/usr/bin/firefox", "/usr/local/bin/firefox", "/snap/bin/firefox"]
    elif system == "Windows":
        candidates = [
            r"C:\Program Files\Mozilla Firefox\firefox.exe",
            r"C:\Program Files (x86)\Mozilla Firefox\firefox.exe",
        ]
    for c in candidates:
        if os.path.exists(c):
            return c
    return None
