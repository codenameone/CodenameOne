#!/usr/bin/env python3
"""Syndicate Codename One Hugo blog posts to sites that have no usable API.

Counterpart to ``syndicate_blog_posts.py``: instead of POSTing to a REST/
GraphQL endpoint, this script drives a real (headless) browser via Playwright
and submits the post through the site's normal authoring UI as a draft for
editorial review. Shares ``Post`` discovery, body rendering, and the
``syndication-state.json`` state file with the API-based script.

Adapters (one class per target site) live at the bottom of this file. Each
adapter exposes a ``login()`` and a ``submit_draft()`` step. Selectors are
kept as constants at the top of each adapter so they are easy to update when
the site changes its UI — which it will, so plan on it.

Usage:

    # First-time setup, watch the browser, take screenshots of the editor:
    python3 scripts/website/syndicate_browser_posts.py \
        --platforms foojay --validate-only --headed --today 2026-05-08

    # Real syndication (headless, daily-cron style):
    python3 scripts/website/syndicate_browser_posts.py --platforms foojay,hackernoon

Required env vars per platform (script auto-skips a platform when its creds
are missing, just like the API script):

    foojay     : FOOJAY_USER, FOOJAY_PASSWORD
    hackernoon : HACKERNOON_USER, HACKERNOON_PASSWORD
    dzone      : DZONE_USER, DZONE_PASSWORD
    medium     : MEDIUM_STORAGE_STATE  (base64-encoded Playwright storageState
                                        JSON exported from a logged-in session
                                        — Medium has no password login flow)
"""

from __future__ import annotations

import argparse
import base64
import datetime as dt
import json
import os
import sys
from dataclasses import dataclass
from pathlib import Path
from typing import Any, Callable

# Reuse the API-based script's discovery, body rendering, and state machinery.
sys.path.insert(0, str(Path(__file__).resolve().parent))
from syndicate_blog_posts import (  # noqa: E402  (intentional path injection)
    BLOG_DIR,
    ELIGIBILITY_FLOOR,
    MIN_AGE_DAYS,
    Post,
    STATE_FILE,
    State,
    discover_posts,
    render_syndicated_body,
    select_candidate,
)


SCREENSHOT_DIR = Path(__file__).resolve().parents[2] / "docs" / "website" / "reports" / "syndication-screenshots"
DEFAULT_PLATFORMS = "foojay,hackernoon,dzone,medium"


@dataclass
class AdapterContext:
    post: Post
    body_markdown: str
    headed: bool
    validate_only: bool


# --------------------------------------------------------------------------- #
# Adapters                                                                     #
# --------------------------------------------------------------------------- #


class AdapterError(RuntimeError):
    """Raised when an adapter cannot complete its flow."""


def _find_first(page, selectors: list[str], *, timeout: int = 15000):
    """Try each selector in turn; return the first that becomes visible.

    Adapters list multiple plausible selectors per field so a small UI tweak
    on the target site does not break the run. The first match wins.
    """
    last_error: Exception | None = None
    for selector in selectors:
        try:
            handle = page.wait_for_selector(selector, timeout=timeout, state="visible")
            if handle:
                return handle
        except Exception as err:  # noqa: BLE001 — Playwright TimeoutError, etc.
            last_error = err
            continue
    raise AdapterError(f"none of the selectors matched: {selectors}: {last_error}")


def _load_base64_storage_state(env_var: str) -> Path:
    """Decode a base64-encoded storage_state JSON from an env var to a temp file."""
    encoded = os.environ[env_var]
    decoded = base64.b64decode(encoded)
    path = Path(f"/tmp/{env_var.lower()}.json")
    path.write_bytes(decoded)
    return path


def _save_screenshot(page, slug: str, label: str) -> Path:
    SCREENSHOT_DIR.mkdir(parents=True, exist_ok=True)
    stamp = dt.datetime.now(dt.timezone.utc).strftime("%Y%m%dT%H%M%SZ")
    path = SCREENSHOT_DIR / f"{slug}-{label}-{stamp}.png"
    try:
        page.screenshot(path=str(path), full_page=True)
    except Exception:  # noqa: BLE001 — never let a screenshot failure mask the real error
        return path
    return path


class FoojayAdapter:
    """foojay.io — Playwright login + REST API draft creation.

    Pure UI submission to foojay does not work reliably: Cloudflare in front
    of foojay challenges form POSTs to /wp-admin/post.php and drops the
    form payload during the challenge, so the draft is never created. The
    REST API is not subject to the same challenge, but Wordfence has
    Application Passwords disabled, so token auth is also out.

    The working hybrid: drive wp-login.php with Playwright to obtain a real
    user session (cookies), pull the WP REST nonce from /wp-admin/, then
    POST the draft through /wp-json/wp/v2/posts with cookie + X-WP-Nonce
    auth. Behaves "as a website user" end-to-end while sidestepping both
    the app-password block and the Cloudflare POST challenge.
    """

    name = "foojay"
    LOGIN_URL = "https://foojay.io/wp-login.php"
    REST_POSTS_ENDPOINT = "https://foojay.io/wp-json/wp/v2/posts"

    USER_SELECTORS = ["#user_login"]
    PASSWORD_SELECTORS = ["#user_pass"]
    SUBMIT_SELECTORS = ["#wp-submit"]

    @staticmethod
    def is_configured() -> bool:
        return bool(os.environ.get("FOOJAY_USER") and os.environ.get("FOOJAY_PASSWORD"))

    def login(self, page) -> None:
        page.goto(self.LOGIN_URL, wait_until="domcontentloaded")
        _find_first(page, self.USER_SELECTORS).fill(os.environ["FOOJAY_USER"])
        _find_first(page, self.PASSWORD_SELECTORS).fill(os.environ["FOOJAY_PASSWORD"])
        _find_first(page, self.SUBMIT_SELECTORS).click()
        try:
            page.wait_for_url("**/wp-admin/**", timeout=90000)
        except Exception:  # noqa: BLE001
            page.wait_for_selector("#wpadminbar", timeout=30000)

    def submit_draft(self, page, ctx: AdapterContext) -> dict[str, Any]:
        # Land on wp-admin so wpApiSettings (which carries the nonce) is in scope.
        page.goto("https://foojay.io/wp-admin/", wait_until="domcontentloaded", timeout=60000)
        nonce = page.evaluate(
            "() => (window.wpApiSettings && window.wpApiSettings.nonce) || null"
        )
        if not nonce:
            raise AdapterError("could not extract wpApiSettings.nonce from /wp-admin/")

        if ctx.validate_only:
            shot = _save_screenshot(page, ctx.post.slug, "foojay-editor")
            return {"validated": True, "screenshot": str(shot), "nonce_acquired": True}

        cookies = page.context.cookies("https://foojay.io/")
        cookie_header = "; ".join(f"{c['name']}={c['value']}" for c in cookies)

        # Yoast canonical (_yoast_wpseo_canonical) is not registered for REST
        # writes on foojay's Yoast install — POSTing it via meta is silently
        # ignored. Surface the canonical as a visible note at the top of the
        # body instead, so the editor reviewer can wire it into Yoast's UI
        # field before publishing.
        canonical_prefix = (
            f"<!-- Canonical: {ctx.post.canonical_url} -->\n\n"
            f"*Originally published on the [Codename One blog]({ctx.post.canonical_url}).*\n\n"
        )

        payload: dict[str, Any] = {
            "title": ctx.post.title,
            "content": canonical_prefix + ctx.body_markdown,
            "status": "draft",
        }
        excerpt = str(ctx.post.front_matter.get("description") or "").strip()
        if excerpt:
            payload["excerpt"] = excerpt[:500]

        import urllib.error as _ue
        import urllib.request as _ur
        request = _ur.Request(
            self.REST_POSTS_ENDPOINT,
            data=json.dumps(payload).encode("utf-8"),
            method="POST",
        )
        request.add_header("Content-Type", "application/json")
        request.add_header("Accept", "application/json")
        request.add_header("X-WP-Nonce", nonce)
        request.add_header("Cookie", cookie_header)
        request.add_header(
            "User-Agent",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 13_0) AppleWebKit/537.36 "
            "(KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36",
        )
        try:
            with _ur.urlopen(request, timeout=60) as response:
                body = response.read().decode("utf-8")
        except _ue.HTTPError as err:
            detail = err.read().decode("utf-8", errors="replace")
            raise AdapterError(f"REST POST failed HTTP {err.code}: {detail}") from err

        data = json.loads(body) if body else {}
        post_id = data.get("id")
        if not post_id:
            raise AdapterError(f"REST response missing post id: {data}")
        return {
            "id": post_id,
            "url": f"https://foojay.io/wp-admin/post.php?post={post_id}&action=edit",
            "preview_url": data.get("link"),
            "syndicated_at": dt.datetime.now(dt.timezone.utc).isoformat(timespec="seconds"),
        }


class HackerNoonAdapter:
    """HackerNoon — app.hackernoon.com email/password login + their own editor.

    Selectors below have NOT been validated against the live site. Run with
    ``--validate-only --headed`` first and update them if the run fails.
    """

    name = "hackernoon"
    LOGIN_URL = "https://hackernoon.com/login"
    EDITOR_URL = "https://app.hackernoon.com/new-story"

    USER_SELECTORS = ["input#email"]
    PASSWORD_SELECTORS = ["input#password"]
    SUBMIT_SELECTORS = [
        "button:has-text('LOG IN')",
        "button:has-text('Log in')",
        "button:has-text('Login')",
    ]
    TITLE_SELECTORS = [
        "[data-placeholder='Title']",
        "[contenteditable='true'][placeholder*='Title']",
        "textarea[placeholder*='Title']",
    ]
    BODY_SELECTORS = [
        "[data-placeholder*='your story' i]",
        "[contenteditable='true'][placeholder*='Tell your story' i]",
    ]
    SAVE_DRAFT_SELECTORS = [
        "button:has-text('Save Draft')",
        "button:has-text('Save as draft')",
    ]
    CANONICAL_FIELD_SELECTORS = [
        "input[name='canonical']",
        "input[placeholder*='canonical' i]",
        "input[placeholder*='original URL' i]",
    ]

    @staticmethod
    def is_configured() -> bool:
        return bool(os.environ.get("HACKERNOON_USER") and os.environ.get("HACKERNOON_PASSWORD"))

    def login(self, page) -> None:
        page.goto(self.LOGIN_URL, wait_until="domcontentloaded")
        # Dismiss the Iubenda cookie consent banner if it overlays the page.
        try:
            page.click(".iubenda-cs-accept-btn, .iubenda-cs-reject-btn", timeout=3000)
        except Exception:  # noqa: BLE001
            pass
        # The form is React-controlled; .fill() sets DOM .value but doesn't
        # update React's internal state, so doLogin() runs with empty fields.
        # Type per-character to dispatch the events React's onChange listens for.
        email = _find_first(page, self.USER_SELECTORS)
        email.click()
        email.press_sequentially(os.environ["HACKERNOON_USER"], delay=10)
        pwd = _find_first(page, self.PASSWORD_SELECTORS)
        pwd.click()
        pwd.press_sequentially(os.environ["HACKERNOON_PASSWORD"], delay=10)
        _find_first(page, self.SUBMIT_SELECTORS).click()
        page.wait_for_load_state("networkidle", timeout=30000)
        # HackerNoon stays on /login if credentials were rejected; raise so the
        # caller surfaces the explicit "Invalid email or password" rather than
        # timing out later in the editor flow.
        if page.url.rstrip("/").endswith("/login"):
            err = page.evaluate(
                "() => { const t = document.body.innerText; "
                "const m = t.match(/Invalid[^\\n]*|Incorrect[^\\n]*/i); "
                "return m ? m[0] : null; }"
            )
            raise AdapterError(err or "login redirected back to /login (auth failed)")

    def submit_draft(self, page, ctx: AdapterContext) -> dict[str, Any]:
        page.goto(self.EDITOR_URL, wait_until="domcontentloaded")
        _find_first(page, self.TITLE_SELECTORS).fill(ctx.post.title)

        body_field = _find_first(page, self.BODY_SELECTORS)
        body_field.click()
        page.evaluate("text => navigator.clipboard.writeText(text)", ctx.body_markdown)
        page.keyboard.press("Meta+V" if sys.platform == "darwin" else "Control+V")

        # Canonical field lives behind a settings panel; selectors here will
        # almost certainly need adjustment. Don't fail the run if it's hidden.
        try:
            field = _find_first(page, self.CANONICAL_FIELD_SELECTORS, timeout=3000)
            field.fill(ctx.post.canonical_url)
        except AdapterError:
            pass

        if ctx.validate_only:
            shot = _save_screenshot(page, ctx.post.slug, "hackernoon-editor")
            return {"validated": True, "screenshot": str(shot)}

        _find_first(page, self.SAVE_DRAFT_SELECTORS).click()
        # Drafts land at /draft/<slug-or-id> on HackerNoon
        page.wait_for_url("**/draft/**", timeout=20000)
        return {
            "url": page.url,
            "syndicated_at": dt.datetime.now(dt.timezone.utc).isoformat(timespec="seconds"),
        }


class DZoneAdapter:
    """DZone — AngularJS login form gated by invisible reCAPTCHA.

    Password-based login does not work from Playwright: DZone's doLogin()
    requires a reCAPTCHA token (visible in scope.credentials.recaptchaToken)
    that Google's invisible reCAPTCHA does not issue to headless browsers.
    The auth request is never sent and login fails silently.

    Use the storage-state path: export your logged-in DZone session from
    a real browser and pass it as DZONE_STORAGE_STATE (base64-encoded JSON,
    same shape as MEDIUM_STORAGE_STATE — generate it with
    scripts/website/export_medium_storage.py --from-firefox-profile after
    swapping the cookie host filter).

    Selectors below for the article editor have NOT been validated against
    the live site.
    """

    name = "dzone"
    EDITOR_URL = "https://dzone.com/articles/new"

    TITLE_SELECTORS = ["input[name='title']", "input#title"]
    BODY_IFRAME_SELECTORS = ["iframe.tox-edit-area__iframe", "iframe[id$='_ifr']"]
    CANONICAL_SELECTORS = [
        "input[name='originalUrl']",
        "input[name='canonicalUrl']",
        "input[placeholder*='canonical' i]",
        "input[placeholder*='original' i]",
    ]
    SAVE_DRAFT_SELECTORS = [
        "button:has-text('Save Draft')",
        "button:has-text('Save as draft')",
        "button[name='saveDraft']",
    ]

    @staticmethod
    def is_configured() -> bool:
        return bool(os.environ.get("DZONE_STORAGE_STATE"))

    def login(self, page) -> None:
        # Storage state is loaded into the browser context up-front; nothing
        # to do here. If the cookies have expired the editor page will bounce
        # back to login and the editor selectors will time out — at which
        # point the user needs to refresh DZONE_STORAGE_STATE.
        return

    def submit_draft(self, page, ctx: AdapterContext) -> dict[str, Any]:
        page.goto(self.EDITOR_URL, wait_until="domcontentloaded")
        _find_first(page, self.TITLE_SELECTORS).fill(ctx.post.title)

        # TinyMCE body lives inside an iframe.
        iframe_handle = _find_first(page, self.BODY_IFRAME_SELECTORS)
        frame = iframe_handle.content_frame()
        if frame is None:
            raise AdapterError("could not access TinyMCE iframe content frame")
        frame.locator("body").click()
        # Paste raw markdown as text — DZone's editor accepts it but renders
        # it as plain text. Editor reviewer will tidy before publishing.
        page.evaluate("text => navigator.clipboard.writeText(text)", ctx.body_markdown)
        frame.locator("body").press("Meta+V" if sys.platform == "darwin" else "Control+V")

        try:
            field = _find_first(page, self.CANONICAL_SELECTORS, timeout=3000)
            field.fill(ctx.post.canonical_url)
        except AdapterError:
            pass

        if ctx.validate_only:
            shot = _save_screenshot(page, ctx.post.slug, "dzone-editor")
            return {"validated": True, "screenshot": str(shot)}

        _find_first(page, self.SAVE_DRAFT_SELECTORS).click()
        page.wait_for_load_state("networkidle", timeout=20000)
        return {
            "url": page.url,
            "syndicated_at": dt.datetime.now(dt.timezone.utc).isoformat(timespec="seconds"),
        }


class MediumAdapter:
    """Medium — no password login flow, so this adapter relies on a saved
    Playwright storageState (cookies + localStorage) loaded from the
    ``MEDIUM_STORAGE_STATE`` env var (base64-encoded JSON).

    To produce one:

        $ python3 -c "from playwright.sync_api import sync_playwright as p; \\
              ctx=p().start().chromium.launch(headless=False).new_context(); \\
              page=ctx.new_page(); page.goto('https://medium.com/m/signin'); \\
              input('Log in then press Enter...'); \\
              ctx.storage_state(path='medium-state.json')"
        $ base64 -i medium-state.json | pbcopy   # paste as MEDIUM_STORAGE_STATE
    """

    name = "medium"
    EDITOR_URL = "https://medium.com/new-story"

    TITLE_SELECTORS = [
        "h3[data-default-value*='Title' i]",
        "[data-default-value*='Title' i]",
        "h3.graf--title",
    ]
    BODY_SELECTORS = [
        "[data-default-value*='your story' i]",
        "p[data-default-value*='Tell your story' i]",
        ".section-content [contenteditable='true']",
    ]
    SETTINGS_BUTTON_SELECTORS = [
        "button:has-text('Story settings')",
        "button[aria-label*='settings' i]",
    ]
    CANONICAL_FIELD_SELECTORS = [
        "input[placeholder*='canonical' i]",
        "input[placeholder*='URL of original' i]",
    ]
    SAVE_AS_DRAFT_SELECTORS = [
        # Medium auto-saves drafts; an explicit Save Draft action isn't
        # always necessary, but the keyboard shortcut Cmd/Ctrl+S works.
    ]

    @staticmethod
    def is_configured() -> bool:
        return bool(os.environ.get("MEDIUM_STORAGE_STATE"))

    @staticmethod
    def storage_state_path() -> Path:
        return _load_base64_storage_state("MEDIUM_STORAGE_STATE")

    def login(self, page) -> None:
        # No-op: storage state was loaded into the browser context already.
        return

    def submit_draft(self, page, ctx: AdapterContext) -> dict[str, Any]:
        page.goto(self.EDITOR_URL, wait_until="domcontentloaded")
        _find_first(page, self.TITLE_SELECTORS).fill(ctx.post.title)
        body_field = _find_first(page, self.BODY_SELECTORS)
        body_field.click()
        page.evaluate("text => navigator.clipboard.writeText(text)", ctx.body_markdown)
        page.keyboard.press("Meta+V" if sys.platform == "darwin" else "Control+V")

        # Set canonical via Story settings panel.
        try:
            _find_first(page, self.SETTINGS_BUTTON_SELECTORS, timeout=3000).click()
            field = _find_first(page, self.CANONICAL_FIELD_SELECTORS, timeout=5000)
            field.fill(ctx.post.canonical_url)
            # Close the settings panel
            page.keyboard.press("Escape")
        except AdapterError:
            pass

        if ctx.validate_only:
            shot = _save_screenshot(page, ctx.post.slug, "medium-editor")
            return {"validated": True, "screenshot": str(shot)}

        # Force-save the draft via keyboard shortcut, then capture the URL.
        page.keyboard.press("Meta+S" if sys.platform == "darwin" else "Control+S")
        page.wait_for_timeout(2000)
        return {
            "url": page.url,
            "syndicated_at": dt.datetime.now(dt.timezone.utc).isoformat(timespec="seconds"),
        }


ADAPTERS: dict[str, Callable[[], Any]] = {
    "foojay": FoojayAdapter,
    "hackernoon": HackerNoonAdapter,
    "dzone": DZoneAdapter,
    "medium": MediumAdapter,
}


# --------------------------------------------------------------------------- #
# Driver                                                                       #
# --------------------------------------------------------------------------- #


def run_adapter(adapter, post: Post, body_markdown: str, headed: bool, validate_only: bool) -> dict[str, Any]:
    from playwright.sync_api import sync_playwright

    with sync_playwright() as pw:
        launch_kwargs: dict[str, Any] = {"headless": not headed}
        browser = pw.chromium.launch(**launch_kwargs)
        context_kwargs: dict[str, Any] = {
            "viewport": {"width": 1400, "height": 900},
            "user_agent": (
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 13_0) AppleWebKit/537.36 "
                "(KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
            ),
        }
        if isinstance(adapter, MediumAdapter):
            context_kwargs["storage_state"] = str(MediumAdapter.storage_state_path())
        elif isinstance(adapter, DZoneAdapter):
            context_kwargs["storage_state"] = str(_load_base64_storage_state("DZONE_STORAGE_STATE"))

        context = browser.new_context(**context_kwargs)
        # Grant clipboard access so navigator.clipboard.writeText() succeeds.
        try:
            context.grant_permissions(["clipboard-read", "clipboard-write"])
        except Exception:  # noqa: BLE001 — Firefox/WebKit don't support all permissions
            pass

        page = context.new_page()
        ctx = AdapterContext(post=post, body_markdown=body_markdown, headed=headed, validate_only=validate_only)

        try:
            adapter.login(page)
            result = adapter.submit_draft(page, ctx)
        except Exception as err:  # noqa: BLE001
            shot = _save_screenshot(page, post.slug, f"{adapter.name}-error")
            raise AdapterError(f"{adapter.name} flow failed (screenshot: {shot}): {err}") from err
        finally:
            context.close()
            browser.close()
        return result


def parse_args(argv: list[str]) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--platforms", default=DEFAULT_PLATFORMS,
                        help=f"Comma-separated platforms (default: {DEFAULT_PLATFORMS}).")
    parser.add_argument("--dry-run", action="store_true",
                        help="No browser launched; just print what would happen.")
    parser.add_argument("--headed", action="store_true",
                        help="Run with a visible browser (for local debugging).")
    parser.add_argument("--validate-only", action="store_true",
                        help="Log in and open the editor, then screenshot and exit without submitting.")
    parser.add_argument("--today", default=None, help="Override today's date (YYYY-MM-DD).")
    parser.add_argument("--floor", default=ELIGIBILITY_FLOOR.isoformat(),
                        help=f"Posts must be dated strictly after this date (default: {ELIGIBILITY_FLOOR.isoformat()}).")
    parser.add_argument("--min-age-days", type=int, default=MIN_AGE_DAYS,
                        help=f"Minimum post age in days (default: {MIN_AGE_DAYS}).")
    parser.add_argument("--blog-dir", default=str(BLOG_DIR))
    parser.add_argument("--state-file", default=str(STATE_FILE))
    return parser.parse_args(argv)


def main(argv: list[str]) -> int:
    args = parse_args(argv)
    today = dt.date.fromisoformat(args.today) if args.today else dt.date.today()
    floor = dt.date.fromisoformat(args.floor)
    requested = [p.strip() for p in args.platforms.split(",") if p.strip()]
    blog_dir = Path(args.blog_dir)
    state_file = Path(args.state_file)

    unknown = [p for p in requested if p not in ADAPTERS]
    if unknown:
        print(f"Unknown platform(s): {unknown}. Known: {sorted(ADAPTERS)}", file=sys.stderr)
        return 1

    adapters: list[Any] = []
    for name in requested:
        adapter = ADAPTERS[name]()
        if args.dry_run or args.validate_only or adapter.is_configured():
            adapters.append(adapter)
        else:
            print(f"[{name}] credentials not configured; skipping platform.")

    if not adapters:
        print("No browser platforms are configured; nothing to do.")
        return 0

    posts = discover_posts(blog_dir)
    state = State.load(state_file)
    platform_names = [a.name for a in adapters]
    candidate = select_candidate(posts, state, platform_names, today, floor, args.min_age_days)
    if candidate is None and not args.validate_only:
        print("No syndication candidate found today.")
        return 0
    if candidate is None and args.validate_only:
        # In validate-only mode, fall back to the newest post so we can still
        # verify selectors even when nothing is technically due.
        candidate = posts[-1]
        print(f"validate-only: using newest post {candidate.slug} for selector verification.")

    print(f"Selected post: {candidate.slug} (date={candidate.date.isoformat()})")
    body_markdown = render_syndicated_body(candidate)

    any_change = False
    failures: list[str] = []

    for adapter in adapters:
        if state.is_syndicated(candidate.slug, adapter.name) and not args.validate_only:
            print(f"  [{adapter.name}] already syndicated; skipping.")
            continue
        if args.dry_run:
            print(f"  [{adapter.name}] dry run — would publish {len(body_markdown)} chars, "
                  f"canonical {candidate.canonical_url}")
            continue
        try:
            result = run_adapter(adapter, candidate, body_markdown, args.headed, args.validate_only)
        except Exception as err:  # noqa: BLE001
            print(f"  [{adapter.name}] FAILED: {err}", file=sys.stderr)
            failures.append(adapter.name)
            continue

        if args.validate_only:
            print(f"  [{adapter.name}] validated. {json.dumps(result)}")
            continue

        if not result.get("url"):
            print(f"  [{adapter.name}] response missing URL: {result}", file=sys.stderr)
            failures.append(adapter.name)
            continue

        state.record(candidate.slug, adapter.name, result)
        any_change = True
        print(f"  [{adapter.name}] published draft: {result['url']}")

    if any_change:
        state.save(state_file)
        print(f"Updated state file: {state_file}")

    if failures:
        return 2
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
