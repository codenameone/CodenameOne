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

DZone and Medium are NOT driven from this Playwright script — both sit
behind aggressive Cloudflare bot detection that cannot be bypassed
reliably from headless automation. They are queued to the Codename One
Syndicator Firefox extension instead, which runs inside the user's
already-trusted browser session. See scripts/syndication-extension/.
"""

from __future__ import annotations

import argparse
import base64
import datetime as dt
import json
import os
import re
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
# DZone and Medium are no longer driven from this Playwright script — both
# are gated by Cloudflare bot detection that headless browsers cannot pass
# reliably. Their syndication is queued to the Codename One Syndicator
# Firefox extension via scripts/website/queue_browser_syndication.py.
DEFAULT_PLATFORMS = "foojay,hackernoon"

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


def _download_to_temp(url: str) -> Path:
    """Download a remote file to a tempfile and return the local path."""
    import tempfile
    import urllib.request as _ur
    req = _ur.Request(url, headers={"User-Agent": _UA_STR})
    with _ur.urlopen(req, timeout=120) as resp:
        data = resp.read()
    suffix = Path(url.split("?", 1)[0]).suffix or ".jpg"
    fd, name = tempfile.mkstemp(suffix=suffix)
    with os.fdopen(fd, "wb") as out:
        out.write(data)
    return Path(name)


_CODE_BLOCK_RE = re.compile(
    r'<pre><code class="language-(?P<lang>[\w+-]+)">(?P<body>.*?)</code></pre>',
    re.DOTALL,
)


def _retag_code_blocks_for_quill(html: str) -> str:
    """Convert python-markdown's <pre><code class="language-X"> to a form
    Quill's syntax module recognises. Quill exposes the language on the
    <pre> via data-language; without it, Quill's auto-detect frequently
    mis-tags Java as JavaScript.
    """
    def replace(match: re.Match) -> str:
        lang = match.group("lang")
        body = match.group("body")
        return (
            f'<pre class="ql-syntax language-{lang}" data-language="{lang}" '
            f'spellcheck="false">{body}</pre>'
        )
    return _CODE_BLOCK_RE.sub(replace, html)


def _markdown_to_html(text: str) -> str:
    """Render a Hugo-flavoured markdown post to HTML for paste-into-editor.

    Falls back to <pre>-wrapped escaped text if python-markdown is unavailable.
    """
    try:
        import markdown as _md
    except ImportError:
        return f"<pre>{_escape_html(text)}</pre>"
    return _md.markdown(text, extensions=["extra", "fenced_code", "sane_lists"], output_format="html5")


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
    COVER_IMAGE_FILE_INPUT_SELECTORS = ["input[type=file][accept*='image']"]
    # Story Settings drawer — for canonical / non-original-story flag. The
    # `css-p9s3bq` class is shared by both Yes and No buttons in the drawer;
    # the corresponding modal also has Yes/No buttons (class="negative")
    # which would be ambiguous, so we restrict to the drawer styling.
    NOT_ORIGINAL_NO_SELECTOR = "button.css-p9s3bq:text-is('No')"
    CANONICAL_INPUT_SELECTORS = ["input.firstSeenAt", "input[placeholder='www.example.com/yourstory']"]
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

        # Title is a React-controlled textarea in the viewport. .fill() leaves
        # it empty, and press_sequentially with a small delay (5-10ms) drops
        # leading chars because HN's React onChange debounces faster than the
        # keystrokes. An 80ms-per-key delay is slow enough that every key
        # registers.
        title_field = page.locator(self.TITLE_SELECTORS[0])
        title_field.wait_for(state="visible", timeout=15000)
        title_field.click()
        title_field.press_sequentially(ctx.post.title, delay=80)

        # Body — Quill rich-text editor in the viewport. Convert markdown to
        # HTML so headings, images, links, and code fences render. Inject via
        # Quill's clipboard API which translates HTML into Quill's Delta.
        body_html = _markdown_to_html(ctx.body_markdown)
        body_html = _retag_code_blocks_for_quill(body_html)
        body = page.locator(self.BODY_QUILL_SELECTORS[0])
        body.wait_for(state="visible", timeout=15000)
        body.click()
        result = page.evaluate(
            """(html) => {
              const ce = document.querySelector("div.ql-editor[contenteditable='true']");
              if (!ce) return {via: 'none'};
              if (window.Quill && window.Quill.find) {
                let container = ce.parentElement;
                let q = null;
                for (let i = 0; i < 5 && container; i++) {
                  q = window.Quill.find(container);
                  if (q) break;
                  container = container.parentElement;
                }
                if (q && q.clipboard && q.clipboard.dangerouslyPasteHTML) {
                  q.setText('');
                  q.clipboard.dangerouslyPasteHTML(0, html, 'api');
                  return {via: 'quill', length: q.getLength()};
                }
              }
              ce.innerHTML = html;
              ce.dispatchEvent(new Event('input', {bubbles: true}));
              return {via: 'fallback', length: ce.innerHTML.length};
            }""",
            body_html,
        )
        if result.get("via") == "none":
            raise AdapterError("could not access Quill editor instance")
        page.wait_for_timeout(1500)

        # Description and Story Settings (No, canonical) live in HN's right
        # drawer with its own internal scroll container. scroll_into_view
        # only scrolls the page, leaving these elements outside the viewport
        # and unclickable. Set them via JS instead — value via React's native
        # property setter (so React's onChange fires), and click() called
        # directly on the button element.
        description = str(ctx.post.front_matter.get("description") or "").strip()
        if description:
            page.evaluate(
                """({sel, value}) => {
                  const el = document.querySelector(sel);
                  if (!el) return false;
                  const setter = Object.getOwnPropertyDescriptor(Object.getPrototypeOf(el), 'value').set;
                  setter.call(el, value);
                  el.dispatchEvent(new Event('input', {bubbles: true}));
                  return true;
                }""",
                {"sel": self.DESCRIPTION_SELECTORS[0], "value": description[:300]},
            )

        # Cover image — download from canonical to a temp file, upload via
        # the file input that accepts image/*. set_input_files works without
        # the file picker dialog opening and ignores viewport position.
        if ctx.post.cover_image:
            try:
                cover_path = _download_to_temp(ctx.post.cover_image)
                page.locator(self.COVER_IMAGE_FILE_INPUT_SELECTORS[0]).first.set_input_files(str(cover_path))
                page.wait_for_timeout(6000)  # let HN process and crop the upload
            except Exception as err:  # noqa: BLE001
                print(f"  [hackernoon] cover image upload failed (non-fatal): {err}", file=sys.stderr)

        # "Is this story original on HackerNoon?" → No  +  canonical URL
        try:
            page.evaluate(
                """() => {
                  const no = Array.from(document.querySelectorAll('button.css-p9s3bq'))
                    .find(b => /^no$/i.test((b.textContent||'').trim()));
                  if (no) no.click();
                }"""
            )
            page.wait_for_timeout(1500)
            page.evaluate(
                """(url) => {
                  const el = document.querySelector('input.firstSeenAt');
                  if (!el) return false;
                  const setter = Object.getOwnPropertyDescriptor(Object.getPrototypeOf(el), 'value').set;
                  setter.call(el, url);
                  el.dispatchEvent(new Event('input', {bubbles: true}));
                  return true;
                }""",
                ctx.post.canonical_url,
            )
            page.wait_for_timeout(500)
        except Exception as err:  # noqa: BLE001
            print(f"  [hackernoon] canonical setup failed (non-fatal): {err}", file=sys.stderr)

        if ctx.validate_only:
            shot = _save_screenshot(page, ctx.post.slug, "hackernoon-editor")
            return {"validated": True, "screenshot": str(shot)}

        page.locator(self.SAVE_DRAFT_SELECTORS[0]).first.click()
        page.wait_for_timeout(5000)
        return {
            "url": page.url,
            "syndicated_at": dt.datetime.now(dt.timezone.utc).isoformat(timespec="seconds"),
        }


ADAPTERS: dict[str, Callable[[], Any]] = {
    "foojay": FoojayAdapter,
    "hackernoon": HackerNoonAdapter,
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
