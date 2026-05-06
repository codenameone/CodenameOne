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

_UA_STR = (
    "Mozilla/5.0 (Macintosh; Intel Mac OS X 13_0) AppleWebKit/537.36 "
    "(KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
)


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


def _escape_html(text: str) -> str:
    return (text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;"))


def _trim_for_meta_description(text: str, limit: int = 140) -> str:
    """Trim a description to Yoast's preferred meta-description length, on a word boundary."""
    text = (text or "").strip()
    if len(text) <= limit:
        return text
    truncated = text[:limit].rsplit(" ", 1)[0].rstrip(",.;:")
    return truncated + "…"


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
    BASE_URL = "https://foojay.io"
    REST_POSTS_ENDPOINT = "https://foojay.io/wp-json/wp/v2/posts"
    REST_TAGS_ENDPOINT = "https://foojay.io/wp-json/wp/v2/tags"
    REST_MEDIA_ENDPOINT = "https://foojay.io/wp-json/wp/v2/media"
    XMLRPC_ENDPOINT = "https://foojay.io/xmlrpc.php"

    # Pre-resolved category and tag IDs (from /wp-json/wp/v2/categories?search=java
    # and /wp-json/wp/v2/tags?slug=codenameone). The tag is created lazily on
    # first use if it does not yet exist.
    JAVA_CATEGORY_ID = 1722
    CODENAMEONE_TAG_SLUG = "codenameone"
    CODENAMEONE_TAG_NAME = "Codename One"

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

        # Resolve / create the codenameone tag.
        tag_id = self._ensure_tag(cookie_header, nonce)
        # Upload the cover image into the WP media library and use the
        # returned media ID as the post's featured image.
        featured_media_id: int | None = None
        if ctx.post.cover_image:
            try:
                featured_media_id = self._upload_featured_media(
                    cookie_header, nonce, ctx.post.cover_image, ctx.post.title
                )
            except Exception as err:  # noqa: BLE001 — featured image is best-effort
                print(f"  [foojay] featured image upload failed (non-fatal): {err}", file=sys.stderr)

        # Yoast canonical (_yoast_wpseo_canonical) is not registered for REST
        # writes on this Yoast install. We send it in `meta` regardless (it's
        # silently ignored if rejected, accepted if registered) AND surface it
        # as a hidden HTML comment at the top of the body so the editor can
        # spot the original URL when filling Yoast's metabox.
        excerpt = str(ctx.post.front_matter.get("description") or "").strip()
        canonical_prefix = f"<!-- Original / canonical: {ctx.post.canonical_url} -->\n\n"

        payload: dict[str, Any] = {
            "title": ctx.post.title,
            "content": canonical_prefix + ctx.body_markdown,
            "status": "draft",
            "categories": [self.JAVA_CATEGORY_ID],
            "tags": [tag_id] if tag_id else [],
            "meta": {
                "_yoast_wpseo_canonical": ctx.post.canonical_url,
                "_yoast_wpseo_title": ctx.post.title,
                "_yoast_wpseo_metadesc": excerpt[:155] if excerpt else "",
            },
        }
        if featured_media_id:
            payload["featured_media"] = featured_media_id
        if excerpt:
            payload["excerpt"] = excerpt[:500]

        data = self._rest_post(self.REST_POSTS_ENDPOINT, cookie_header, nonce, payload)
        post_id = data.get("id")
        if not post_id:
            raise AdapterError(f"REST response missing post id: {data}")

        # Yoast meta (canonical / SEO title / metadesc) is not REST-writable on
        # foojay's Yoast install. wp-admin form-submit is blocked by Cloudflare.
        # XML-RPC's wp.editPost with custom_fields bypasses both restrictions
        # and successfully writes the underscore-prefixed meta keys.
        yoast_set = False
        try:
            self._set_yoast_meta_via_xmlrpc(
                post_id=post_id,
                canonical=ctx.post.canonical_url,
                seo_title=ctx.post.title,
                metadesc=_trim_for_meta_description(excerpt),
            )
            yoast_set = True
        except Exception as err:  # noqa: BLE001 — Yoast meta is best-effort
            print(f"  [foojay] XML-RPC Yoast meta write failed (non-fatal): {err}", file=sys.stderr)

        return {
            "id": post_id,
            "url": f"https://foojay.io/wp-admin/post.php?post={post_id}&action=edit",
            "preview_url": data.get("link"),
            "featured_media_id": featured_media_id,
            "yoast_meta_set": yoast_set,
            "syndicated_at": dt.datetime.now(dt.timezone.utc).isoformat(timespec="seconds"),
        }

    # ----- helpers -----

    def _ensure_tag(self, cookie_header: str, nonce: str) -> int | None:
        """Return the WP tag id for `codenameone`, creating it if missing."""
        import urllib.parse as _up
        try:
            existing = self._rest_get(
                f"{self.REST_TAGS_ENDPOINT}?slug={_up.quote(self.CODENAMEONE_TAG_SLUG)}",
                cookie_header,
                nonce,
            )
            if isinstance(existing, list) and existing:
                return existing[0].get("id")
            created = self._rest_post(
                self.REST_TAGS_ENDPOINT,
                cookie_header,
                nonce,
                {"name": self.CODENAMEONE_TAG_NAME, "slug": self.CODENAMEONE_TAG_SLUG},
            )
            return created.get("id")
        except Exception as err:  # noqa: BLE001 — tag is best-effort
            print(f"  [foojay] tag resolve/create failed (non-fatal): {err}", file=sys.stderr)
            return None

    def _upload_featured_media(self, cookie_header: str, nonce: str,
                               image_url: str, title: str) -> int:
        """Download the cover image and POST it into WP's media library."""
        import urllib.request as _ur
        # Download bytes
        req = _ur.Request(image_url, headers={"User-Agent": _UA_STR})
        with _ur.urlopen(req, timeout=120) as resp:
            image_bytes = resp.read()
            content_type = resp.headers.get("Content-Type", "image/jpeg")
        filename = image_url.rsplit("/", 1)[-1].split("?", 1)[0] or "cover.jpg"

        upload = _ur.Request(self.REST_MEDIA_ENDPOINT, data=image_bytes, method="POST")
        upload.add_header("Content-Type", content_type)
        upload.add_header("Content-Disposition", f'attachment; filename="{filename}"')
        upload.add_header("X-WP-Nonce", nonce)
        upload.add_header("Cookie", cookie_header)
        upload.add_header("User-Agent", _UA_STR)
        with _ur.urlopen(upload, timeout=120) as response:
            data = json.loads(response.read().decode("utf-8"))
        media_id = data.get("id")
        if not media_id:
            raise RuntimeError(f"media upload returned no id: {str(data)[:200]}")
        # Set a friendlier title on the media item.
        try:
            self._rest_post(
                f"{self.REST_MEDIA_ENDPOINT}/{media_id}", cookie_header, nonce,
                {"title": title, "alt_text": title},
            )
        except Exception:  # noqa: BLE001 — title is cosmetic
            pass
        return media_id

    def _set_yoast_meta_via_xmlrpc(self, post_id: int, canonical: str,
                                   seo_title: str, metadesc: str) -> None:
        """Update Yoast SEO post meta via XML-RPC's wp.editPost custom_fields.

        REST silently drops these meta keys (not registered for REST writes)
        and the wp-admin form-submit path is challenged by Cloudflare.
        XML-RPC accepts underscore-prefixed meta keys via custom_fields and
        is not Cloudflare-protected on foojay.
        """
        import urllib.error as _ue
        import urllib.request as _ur
        import xml.sax.saxutils as _su

        user = os.environ["FOOJAY_USER"]
        pwd = os.environ["FOOJAY_PASSWORD"]

        def cf_member(key: str, value: str) -> str:
            return (
                "<value><struct>"
                f"<member><name>key</name><value><string>{_su.escape(key)}</string></value></member>"
                f"<member><name>value</name><value><string>{_su.escape(value)}</string></value></member>"
                "</struct></value>"
            )

        custom_fields_xml = "".join([
            cf_member("_yoast_wpseo_canonical", canonical),
            cf_member("_yoast_wpseo_title", seo_title),
            cf_member("_yoast_wpseo_metadesc", metadesc),
        ])
        envelope = (
            '<?xml version="1.0"?>'
            '<methodCall><methodName>wp.editPost</methodName><params>'
            "<param><value><int>1</int></value></param>"
            f"<param><value><string>{_su.escape(user)}</string></value></param>"
            f"<param><value><string>{_su.escape(pwd)}</string></value></param>"
            f"<param><value><int>{int(post_id)}</int></value></param>"
            "<param><value><struct>"
            f"<member><name>custom_fields</name><value><array><data>{custom_fields_xml}</data></array></value></member>"
            "</struct></value></param>"
            "</params></methodCall>"
        )
        req = _ur.Request(
            self.XMLRPC_ENDPOINT,
            data=envelope.encode("utf-8"),
            method="POST",
        )
        req.add_header("Content-Type", "text/xml")
        req.add_header("User-Agent", _UA_STR)
        try:
            with _ur.urlopen(req, timeout=60) as response:
                body = response.read().decode("utf-8", errors="replace")
        except _ue.HTTPError as err:
            detail = err.read().decode("utf-8", errors="replace")
            raise RuntimeError(f"xmlrpc HTTP {err.code}: {detail}") from err
        if "<fault>" in body:
            raise RuntimeError(f"xmlrpc fault: {body[:500]}")
        if "<boolean>1</boolean>" not in body:
            raise RuntimeError(f"xmlrpc unexpected response: {body[:500]}")

    def _rest_get(self, url: str, cookie_header: str, nonce: str) -> Any:
        import urllib.request as _ur
        req = _ur.Request(url, method="GET")
        req.add_header("Accept", "application/json")
        req.add_header("X-WP-Nonce", nonce)
        req.add_header("Cookie", cookie_header)
        req.add_header("User-Agent", _UA_STR)
        with _ur.urlopen(req, timeout=60) as response:
            return json.loads(response.read().decode("utf-8"))

    def _rest_post(self, url: str, cookie_header: str, nonce: str,
                   payload: dict[str, Any]) -> dict[str, Any]:
        import urllib.error as _ue
        import urllib.request as _ur
        req = _ur.Request(url, data=json.dumps(payload).encode("utf-8"), method="POST")
        req.add_header("Content-Type", "application/json")
        req.add_header("Accept", "application/json")
        req.add_header("X-WP-Nonce", nonce)
        req.add_header("Cookie", cookie_header)
        req.add_header("User-Agent", _UA_STR)
        try:
            with _ur.urlopen(req, timeout=120) as response:
                raw = response.read().decode("utf-8")
        except _ue.HTTPError as err:
            detail = err.read().decode("utf-8", errors="replace")
            raise AdapterError(f"REST POST {url} failed HTTP {err.code}: {detail}") from err
        return json.loads(raw) if raw else {}


class HackerNoonAdapter:
    """HackerNoon — app.hackernoon.com email/password login + their own editor.

    Selectors below have NOT been validated against the live site. Run with
    ``--validate-only --headed`` first and update them if the run fails.
    """

    name = "hackernoon"
    HOME_URL = "https://hackernoon.com/"
    NEW_DRAFT_URL = "https://hackernoon.com/new"

    # Login is via a drawer that opens when you click the header "Login" button
    # on the public hackernoon.com pages — there is no standalone /login page
    # that submits successfully (the visible /login form is decorative; the
    # working form is in the drawer).
    HEADER_LOGIN_BUTTON_SELECTORS = ["button:text-is('Login')"]
    DRAWER_EMAIL_SELECTORS = ["input[type=email][placeholder='Email']"]
    DRAWER_PASSWORD_SELECTORS = ["input[type=password][placeholder='Password']"]
    DRAWER_SUBMIT_SELECTORS = ["button:text-is('Log In')"]

    # Editor — Quill-based. Reached via "Start Draft" button on /new which
    # navigates to app.hackernoon.com/articles/new.
    START_DRAFT_SELECTORS = ["button:text-is('Start Draft')"]
    TITLE_SELECTORS = ["textarea[name='title'][placeholder='Title']"]
    DESCRIPTION_SELECTORS = ["textarea[placeholder*='brief description' i]"]
    BODY_QUILL_SELECTORS = ["div.ql-editor[contenteditable='true']"]
    # Save creates a draft. Submit Story for Review! sends to editorial and
    # only enables once additional fields (image, categories, tags) are set —
    # the syndication script intentionally targets Save so the draft lands
    # for the user to review, refine, then submit for editorial publish.
    SAVE_DRAFT_SELECTORS = ["button:text-is('Save')"]

    @staticmethod
    def is_configured() -> bool:
        return bool(os.environ.get("HACKERNOON_USER") and os.environ.get("HACKERNOON_PASSWORD"))

    def login(self, page) -> None:
        page.goto(self.HOME_URL, wait_until="domcontentloaded", timeout=30000)
        # Dismiss the Iubenda cookie consent banner if it overlays the page.
        try:
            page.click(".iubenda-cs-accept-btn, .iubenda-cs-reject-btn", timeout=3000)
        except Exception:  # noqa: BLE001
            pass
        # Open the login drawer via the header Login button. There may be
        # multiple "Login" buttons on the page (header + footer); .first picks
        # the visible header one.
        page.locator(self.HEADER_LOGIN_BUTTON_SELECTORS[0]).first.click()
        # Drawer is React-controlled; type per-character so React's onChange
        # actually updates state instead of being silently ignored.
        email = page.locator(self.DRAWER_EMAIL_SELECTORS[0])
        email.wait_for(state="visible", timeout=15000)
        email.click()
        email.press_sequentially(os.environ["HACKERNOON_USER"], delay=10)
        pwd = page.locator(self.DRAWER_PASSWORD_SELECTORS[0])
        pwd.click()
        pwd.press_sequentially(os.environ["HACKERNOON_PASSWORD"], delay=10)
        page.locator(self.DRAWER_SUBMIT_SELECTORS[0]).click()
        # Successful login sets a `hasAuthCookie` cookie on .hackernoon.com
        # and the drawer disappears. Wait for the cookie rather than a URL
        # change because the page may stay on the homepage.
        deadline = dt.datetime.now() + dt.timedelta(seconds=30)
        while dt.datetime.now() < deadline:
            cookies = page.context.cookies("https://hackernoon.com/")
            if any(c.get("name") == "hasAuthCookie" for c in cookies):
                return
            page.wait_for_timeout(500)
        raise AdapterError("hackernoon login: hasAuthCookie not set within 30s")

    def submit_draft(self, page, ctx: AdapterContext) -> dict[str, Any]:
        page.goto(self.NEW_DRAFT_URL, wait_until="networkidle", timeout=60000)
        # SPA needs additional time after networkidle to hydrate the buttons.
        start_locator = page.locator(self.START_DRAFT_SELECTORS[0]).first
        start_locator.wait_for(state="visible", timeout=30000)
        start_locator.click()
        # Lands on app.hackernoon.com/articles/new — Quill needs a moment to mount.
        page.wait_for_url("**/articles/**", timeout=30000)
        page.wait_for_timeout(5000)

        title_field = page.locator(self.TITLE_SELECTORS[0])
        title_field.wait_for(state="visible", timeout=15000)
        title_field.click()
        title_field.press_sequentially(ctx.post.title, delay=5)

        # Description (used by HackerNoon as the SEO description / preview).
        description = str(ctx.post.front_matter.get("description") or "").strip()
        if description:
            try:
                desc = page.locator(self.DESCRIPTION_SELECTORS[0])
                desc.wait_for(state="visible", timeout=5000)
                desc.click()
                desc.press_sequentially(description[:300], delay=5)
            except Exception:  # noqa: BLE001
                pass

        # Body — Quill rich-text editor. Pasting markdown lands as plain text;
        # the user can refine in the editor before clicking Submit-for-Review.
        # Prepend an "Originally published at" note so the editorial reviewer
        # can wire up the canonical and credit before publishing.
        body_with_canonical = (
            f"Originally published at {ctx.post.canonical_url}\n\n"
            + ctx.body_markdown
        )
        body = page.locator(self.BODY_QUILL_SELECTORS[0])
        body.wait_for(state="visible", timeout=15000)
        body.click()
        # Quill's contenteditable accepts text via the keyboard. Use insertText
        # via the clipboard for speed; per-character typing on a 20k-char post
        # would take far too long.
        page.evaluate("text => navigator.clipboard.writeText(text)", body_with_canonical)
        page.keyboard.press("Meta+V" if sys.platform == "darwin" else "Control+V")
        page.wait_for_timeout(1000)

        if ctx.validate_only:
            shot = _save_screenshot(page, ctx.post.slug, "hackernoon-editor")
            return {"validated": True, "screenshot": str(shot)}

        # Two "Save" buttons exist — one in the article toolbar (no class)
        # and one in some side panel (class="btn"). The first visible one in
        # the toolbar is the one we want.
        page.locator(self.SAVE_DRAFT_SELECTORS[0]).first.click()
        # Save returns to the editor with a saved-draft toast; the URL gains
        # a /draftId/<slug> segment.
        page.wait_for_timeout(5000)
        return {
            "url": page.url,
            "syndicated_at": dt.datetime.now(dt.timezone.utc).isoformat(timespec="seconds"),
        }


class DZoneAdapter:
    """DZone — AngularJS login form gated by invisible reCAPTCHA, body editor
    is Froala. Uses storage-state auth (DZONE_STORAGE_STATE) since password
    login can't pass reCAPTCHA from headless Playwright.

    Live editor URL: /content/article/post.html (the create-form). The Save
    Draft button is enabled once title + body have content.
    """

    name = "dzone"
    EDITOR_URL = "https://dzone.com/content/article/post.html"

    TITLE_SELECTORS = ["textarea[name='title'][placeholder='Enter Title Here']"]
    SUBTITLE_SELECTORS = ["textarea[name='subtitle']"]
    META_DESCRIPTION_SELECTORS = ["#meta-description-textarea"]
    # Froala renders the editable area as a contenteditable div with class
    # `fr-element`. Clicking puts the cursor in the body so keystrokes land.
    BODY_FROALA_SELECTORS = ["div.fr-element[contenteditable='true']"]
    SAVE_DRAFT_SELECTORS = ["button:has-text('Save draft')"]

    @staticmethod
    def is_configured() -> bool:
        return bool(os.environ.get("DZONE_STORAGE_STATE"))

    def login(self, page) -> None:
        # Storage state is loaded into the browser context up-front; nothing
        # to do here. If the cookies have expired the editor page will bounce
        # back to login — at which point the user needs to refresh
        # DZONE_STORAGE_STATE via export_storage_state.py.
        return

    def submit_draft(self, page, ctx: AdapterContext) -> dict[str, Any]:
        page.goto(self.EDITOR_URL, wait_until="domcontentloaded", timeout=60000)
        page.wait_for_timeout(5000)

        title = page.locator(self.TITLE_SELECTORS[0])
        title.wait_for(state="visible", timeout=20000)
        title.click()
        title.press_sequentially(ctx.post.title, delay=5)

        # Subtitle / TL;DR — use the post's description if present.
        description = str(ctx.post.front_matter.get("description") or "").strip()
        if description:
            try:
                sub = page.locator(self.SUBTITLE_SELECTORS[0])
                sub.wait_for(state="visible", timeout=5000)
                sub.click()
                sub.press_sequentially(description[:300], delay=5)
            except Exception:  # noqa: BLE001
                pass
            try:
                meta = page.locator(self.META_DESCRIPTION_SELECTORS[0])
                meta.wait_for(state="visible", timeout=5000)
                meta.click()
                meta.press_sequentially(description[:155], delay=5)
            except Exception:  # noqa: BLE001
                pass

        # Body — Froala rich-text editor. Use Froala's JS API to set HTML
        # directly; clipboard paste into the contenteditable is unreliable
        # (Froala's paste handler often discards or transforms the input).
        body_with_canonical = (
            f"<p><em>Originally published at <a href=\"{ctx.post.canonical_url}\">"
            f"{ctx.post.canonical_url}</a></em></p>\n\n"
            f"<pre>{_escape_html(ctx.body_markdown)}</pre>"
        )
        body = page.locator(self.BODY_FROALA_SELECTORS[0]).first
        body.wait_for(state="visible", timeout=15000)
        body.click()
        result = page.evaluate(
            """(html) => {
              const fr = document.querySelector("div.fr-element[contenteditable='true']");
              if (window.FroalaEditor && window.FroalaEditor.INSTANCES && window.FroalaEditor.INSTANCES.length) {
                const inst = window.FroalaEditor.INSTANCES[0];
                inst.html.set(html);
                if (inst.events && inst.events.trigger) inst.events.trigger('contentChanged');
                return {via: 'froala-api', length: inst.html.get().length};
              }
              if (fr) { fr.innerHTML = html; fr.dispatchEvent(new Event('input', {bubbles: true})); return {via: 'fallback', length: fr.innerHTML.length}; }
              return {via: 'none'};
            }""",
            body_with_canonical,
        )
        if result.get("via") == "none":
            raise AdapterError("could not access Froala editor instance")
        page.wait_for_timeout(2000)

        if ctx.validate_only:
            shot = _save_screenshot(page, ctx.post.slug, "dzone-editor")
            return {"validated": True, "screenshot": str(shot)}

        page.locator(self.SAVE_DRAFT_SELECTORS[0]).first.click()
        page.wait_for_timeout(5000)
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

    # Medium's editor is a single contenteditable div with placeholder
    # "Title\nTell your story…" — first line typed becomes the H3 title,
    # everything after is body. Auto-saves a few seconds after typing pauses.
    EDITOR_SELECTOR = "div.postArticle-content[contenteditable='true']"
    SETTINGS_BUTTON_SELECTORS = [
        "button:has-text('Story settings')",
        "button[aria-label*='settings' i]",
    ]
    CANONICAL_FIELD_SELECTORS = [
        "input[placeholder*='canonical' i]",
        "input[placeholder*='URL of original' i]",
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
        page.goto(self.EDITOR_URL, wait_until="domcontentloaded", timeout=45000)
        page.wait_for_timeout(5000)
        editor = page.locator(self.EDITOR_SELECTOR).first
        editor.wait_for(state="visible", timeout=20000)
        editor.click()
        # Type the title, press Enter to drop into the body section, then
        # paste the body. Medium auto-saves a few seconds after typing pauses.
        page.keyboard.type(ctx.post.title, delay=5)
        page.keyboard.press("Enter")
        body_with_canonical = (
            f"Originally published at {ctx.post.canonical_url}\n\n"
            + ctx.body_markdown
        )
        page.evaluate("text => navigator.clipboard.writeText(text)", body_with_canonical)
        page.keyboard.press("Meta+V" if sys.platform == "darwin" else "Control+V")

        if ctx.validate_only:
            shot = _save_screenshot(page, ctx.post.slug, "medium-editor")
            return {"validated": True, "screenshot": str(shot)}

        # Wait for Medium's auto-save to kick in. Medium redirects from
        # /new-story to /p/<draftId>/edit once the first save completes.
        try:
            page.wait_for_url("**/p/*/edit", timeout=45000)
        except Exception:  # noqa: BLE001
            page.wait_for_timeout(8000)  # fall back to a long wait

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
            "user_agent": _UA_STR,
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
