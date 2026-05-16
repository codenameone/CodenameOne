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
    python3 scripts/website/syndicate_browser_posts.py --platforms foojay

Required env vars per platform (script auto-skips a platform when its creds
are missing, just like the API script):

    foojay     : FOOJAY_USER, FOOJAY_PASSWORD
    hashnode   : HASHNODE_STORAGE_STATE (base64-encoded Playwright
                 storageState JSON — produce with
                 ``scripts/website/export_storage_state.py --site hashnode``)

DZone and Medium are NOT driven from this Playwright script — both sit
behind aggressive Cloudflare bot detection that cannot be bypassed
reliably from headless automation. They are queued by
``queue_browser_syndication.py`` to ``syndication-queue.json`` and
handled manually from an already-signed-in browser session.

Hashnode used to be driven from the API syndicator (gql.hashnode.com
GraphQL) but Hashnode shut down free public GraphQL access on 2026-05-13
and moved it behind a paid / allow-listed offering, so we drive its
web editor here from a signed-in storage state instead. The Hashnode
adapter is intended to be run locally, not from CI — CI does not hold
the storage-state secret.

HackerNoon was previously supported here but removed: HackerNoon
charges business sites for canonical URL support, which makes it
unsuitable for syndication where the canonical link back to the
original is the whole point.
"""

from __future__ import annotations

import argparse
import base64
import datetime as dt
import json
import os
import re
import sys
import tempfile
import urllib.request
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
# DZone and Medium are not driven from this Playwright script — both are
# gated by Cloudflare bot detection that headless browsers cannot pass
# reliably. Their syndication is queued to syndication-queue.json via
# scripts/website/queue_browser_syndication.py and handled manually from
# an already-signed-in browser session.
#
# Hashnode IS driven from here (HashnodeAdapter below) using a saved
# storage state — see the module docstring. CI does not hold the
# HASHNODE_STORAGE_STATE secret so the platform is skipped automatically
# in cron runs; the maintainer runs the script locally to drive it.
DEFAULT_PLATFORMS = "foojay,hashnode"

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


def _download_to_temp(url: str) -> Path:
    """Download ``url`` into a temp file, preserving the URL's basename
    so the upload target sees a friendly filename."""
    basename = url.rsplit("/", 1)[-1].split("?", 1)[0] or "cover"
    suffix = "." + basename.rsplit(".", 1)[-1] if "." in basename else ""
    request = urllib.request.Request(url, headers={"User-Agent": _UA_STR})
    with urllib.request.urlopen(request, timeout=60) as response:
        data = response.read()
    fd, path = tempfile.mkstemp(prefix="cn1-syndic-", suffix=suffix)
    try:
        os.write(fd, data)
    finally:
        os.close(fd)
    return Path(path)


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
        except Exception:  # noqa: BLE001
            # The media-item title and alt text are cosmetic — the upload
            # itself already succeeded and the post will reference the
            # returned media id regardless. Swallow any follow-up rename
            # error so the caller still gets the upload result.
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


class HashnodeAdapter:
    """Hashnode — Playwright + storage-state auth.

    Hashnode shut down free public GraphQL API access on 2026-05-13 and
    moved it behind a paid / allow-listed offering, so we drive the web
    editor directly from a signed-in browser session. Auth is via
    ``HASHNODE_STORAGE_STATE`` (base64-encoded Playwright storageState
    JSON); produce one with::

        python3 scripts/website/export_storage_state.py --site hashnode \\
            --from-firefox-profile

    Flow (verified against the 2026-05 Hashnode UI):

      1. Land on the dashboard and click the "Write" button — Hashnode
         opens its single per-user draft slot and redirects to
         /draft/<id>.
      2. Fill the title textarea (placeholder "Article Title...") and
         paste the markdown body into the contenteditable editor. The
         leading header-image markdown line is stripped from the body
         because it lives separately as the "Cover" image (see step 3).
      3. Click the "Cover" header button to open the cover popover,
         upload the post's cover image into the file input
         (input[type='file'][accept='image/*']), and wait for the
         upload to finish.
      4. Click the "Subheading" header button to reveal the subheading
         textarea, then fill it with the post's description (trimmed).
      5. Open the publish dialog (the top "Publish" button in the
         header bar — there is a second "Publish" button inside the
         dialog that actually publishes; we never click that one).
      6. Switch to the Discovery tab, clear any pre-existing tag pills,
         add the five canonical tags (java, mobile-development, ios,
         android, opensource), toggle the "Add a canonical URL" switch
         on if needed, and fill the canonical URL pointing back at
         www.codenameone.com.
      7. Click "Close" — Hashnode autosaves everything onto the draft.
         The post stays unpublished until a human reviews and clicks
         Publish in the editor UI.

    Known limitation: Hashnode's "Write" button lands on a single
    persistent "current draft" slot per user. Consecutive runs reuse
    the same /draft/<id> URL and overwrite the prior draft's content.
    Publish (or delete) each Hashnode draft before the next weekly
    syndication runs, otherwise the next post will clobber it. This is
    a Hashnode product-design choice, not something we can work around
    from the UI side.
    """

    name = "hashnode"
    # /create/story 404s on the modern Hashnode UI; the "Write" button on
    # the dashboard is the only entry point that creates a fresh draft
    # bound to the user's primary publication.
    DASHBOARD_URL = "https://hashnode.com/"

    TITLE_SELECTOR = "textarea[placeholder='Article Title...']"
    BODY_SELECTOR = "div[contenteditable='true']"
    # Cover popover trigger has two captions: "Cover" when no cover is
    # set, "Change cover" once one is uploaded. Both buttons share
    # data-slot='popover-trigger'.
    COVER_BUTTON_SELECTORS = [
        "button[data-slot='popover-trigger']:has(span:text-is('Cover'))",
        "button[data-slot='popover-trigger']:has-text('Change cover')",
    ]
    COVER_UPLOAD_IMAGE_BUTTON_SELECTOR = "button:has-text('Upload Image')"
    SUBHEADING_BUTTON_SELECTOR = "button:has(span:text-is('Subheading'))"
    SUBHEADING_TEXTAREA_SELECTOR = "textarea[placeholder='Add a subheading']"
    DISCOVERY_TAB_SELECTOR = "button[role='tab']:has-text('Discovery')"
    TAGS_INPUT_SELECTOR = "input#editor-tags"
    CANONICAL_TOGGLE_SELECTOR = "label:has-text('Add a canonical URL')"
    CANONICAL_INPUT_SELECTOR = "input[placeholder='https://example.com/original-article']"
    CLOSE_DIALOG_SELECTOR = "button:has-text('Close')"

    # The five tags every Codename One syndicated post carries on
    # Hashnode. All five exist as canonical Hashnode tags so a
    # type-the-name + Enter sequence resolves to the right pill.
    TAGS = ["java", "mobile-development", "ios", "android", "opensource"]

    # Max subheading length. Hashnode's textarea does not visibly
    # enforce a limit, but long subheadings clip in card previews and
    # social shares.
    SUBHEADING_MAX = 250

    # Strips the first "header image" paragraph from the rendered body
    # so the cover image isn't duplicated (once as the Cover, once
    # inline at the top of the article).
    _LEADING_COVER_IMAGE_RE = re.compile(
        r"\A\s*!\[[^\]]*\]\([^)\s]+\)\s*\n?",
        re.MULTILINE,
    )

    # Stable copy of the JS used to pick the top-bar "Publish" button (the
    # publish dialog also contains a "Publish" button — clicking that
    # one would actually publish, which we never want).
    _CLICK_TOP_PUBLISH_JS = """
    () => {
        const btns = Array.from(document.querySelectorAll('button'))
            .filter(b => b.innerText && b.innerText.trim() === 'Publish');
        btns.sort((a,b) => a.getBoundingClientRect().top - b.getBoundingClientRect().top);
        if (btns[0]) btns[0].click();
    }
    """

    @staticmethod
    def is_configured() -> bool:
        return bool(os.environ.get("HASHNODE_STORAGE_STATE"))

    @staticmethod
    def storage_state_path() -> Path:
        return _load_base64_storage_state("HASHNODE_STORAGE_STATE")

    def login(self, page) -> None:
        # No-op: storage state was loaded into the browser context already.
        return

    def submit_draft(self, page, ctx: AdapterContext) -> dict[str, Any]:
        mod = "Meta" if sys.platform == "darwin" else "Control"

        page.goto(self.DASHBOARD_URL, wait_until="domcontentloaded", timeout=45000)
        page.wait_for_timeout(4000)

        # "Write" opens the user's current draft slot and redirects to
        # /draft/<id>. If the dashboard already auto-redirected us
        # there (Hashnode does this when an in-progress draft is open
        # in another tab), skip the click — wait_for_url would never
        # fire since the URL never changes.
        if "/draft/" not in page.url:
            page.locator("button:has-text('Write')").first.click()
            page.wait_for_url("**/draft/*", timeout=30000)
        draft_url = page.url
        # Let the editor hydrate before typing into it — there are two
        # contenteditables to disambiguate (article body vs Hashnode AI
        # textarea) and the title field is also lazy-mounted.
        page.wait_for_timeout(8000)

        # --- Title + body ---
        title_field = page.locator(self.TITLE_SELECTOR).first
        title_field.wait_for(state="visible", timeout=20000)
        title_field.click()
        title_field.fill(ctx.post.title)

        body_editor = page.locator(self.BODY_SELECTOR).first
        body_editor.click()
        # Hashnode reuses the same auto-saved empty draft across
        # consecutive "Write" clicks, so on the second run the body
        # already has whatever we typed last time. Playwright's
        # locator.fill() does not clear a contenteditable, so we
        # manually select-all + delete before pasting.
        page.keyboard.press(f"{mod}+A")
        page.keyboard.press("Delete")
        page.wait_for_timeout(500)
        # Strip the leading cover-image markdown — Hashnode hosts the
        # cover image via the "Cover" button (see _set_cover_image), so
        # leaving it inline at the top of the body would render it
        # twice.
        body_for_paste = self._LEADING_COVER_IMAGE_RE.sub("", ctx.body_markdown, count=1)
        # Hashnode's editor accepts markdown pasted into the body — it
        # tokenizes headings, code fences, links, images, etc. on paste.
        # Clipboard paste is the most robust way to insert a large body
        # without triggering per-keystroke autocomplete or slash-menus.
        page.evaluate("text => navigator.clipboard.writeText(text)", body_for_paste)
        page.keyboard.press(f"{mod}+V")
        # Hashnode autosaves a few seconds after typing stops.
        page.wait_for_timeout(5000)

        # --- Cover image ---
        cover_set = self._set_cover_image(page, ctx)

        # --- Subheading ---
        subheading_set = self._set_subheading(page, ctx)

        if ctx.validate_only:
            shot = _save_screenshot(page, ctx.post.slug, "hashnode-editor")
            return {
                "validated": True,
                "screenshot": str(shot),
                "draft_url": draft_url,
                "cover_set": cover_set,
                "subheading_set": subheading_set,
            }

        # --- Publish-dialog Discovery tab: tags + canonical URL ---
        canonical_set = False
        tags_set = False
        try:
            self._open_publish_dialog_discovery(page)
            tags_set = self._set_tags(page, mod)
            canonical_set = self._set_canonical_url(page, ctx.post.canonical_url, mod)
            page.locator(self.CLOSE_DIALOG_SELECTOR).first.click(timeout=8000)
            # Let the autosave-on-close round-trip finish before we
            # close the browser context.
            page.wait_for_timeout(4000)
        except Exception as err:  # noqa: BLE001 — surface as non-fatal
            print(f"  [hashnode] publish-dialog flow failed (non-fatal): {err}",
                  file=sys.stderr)

        return {
            "url": draft_url,
            "cover_set": cover_set,
            "subheading_set": subheading_set,
            "tags_set": tags_set,
            "canonical_set": canonical_set,
            "syndicated_at": dt.datetime.now(dt.timezone.utc).isoformat(timespec="seconds"),
        }

    # ------------------------------------------------------------------ #
    # Step helpers — each returns True on success and False on a
    # recoverable failure so the overall flow can keep going.
    # ------------------------------------------------------------------ #

    def _set_cover_image(self, page, ctx: AdapterContext) -> bool:
        cover_url = ctx.post.cover_image
        if not cover_url:
            return False
        try:
            tmp_path = _download_to_temp(cover_url)
        except Exception as err:  # noqa: BLE001 — cover is best-effort
            print(f"  [hashnode] cover image download failed (non-fatal): {err}",
                  file=sys.stderr)
            return False
        try:
            # Try both empty-state ("Cover") and set-state ("Change
            # cover") selectors so re-runs on a draft with an existing
            # cover still resolve the popover trigger.
            cover_btn = _find_first(page, self.COVER_BUTTON_SELECTORS, timeout=15000)
            cover_btn.click()
            page.wait_for_timeout(2000)
            # Hashnode wraps the <input type='file'> in a custom
            # uploader: Playwright's set_input_files dispatches the
            # change event but the wrapper ignores it. Driving the
            # native file chooser through expect_file_chooser is the
            # only reliable way to upload.
            with page.expect_file_chooser(timeout=10000) as fc_info:
                page.locator(self.COVER_UPLOAD_IMAGE_BUTTON_SELECTOR).first.click()
            fc_info.value.set_files(str(tmp_path))
            # Upload + thumbnail render takes ~3-6 seconds on a small
            # JPEG and the popover closes itself on success.
            page.wait_for_timeout(8000)
            return True
        except Exception as err:  # noqa: BLE001
            print(f"  [hashnode] cover image upload failed (non-fatal): {err}",
                  file=sys.stderr)
            return False
        finally:
            try:
                tmp_path.unlink(missing_ok=True)
            except Exception:  # noqa: BLE001
                pass

    def _set_subheading(self, page, ctx: AdapterContext) -> bool:
        subheading = str(ctx.post.front_matter.get("description") or "").strip()
        if not subheading:
            return False
        if len(subheading) > self.SUBHEADING_MAX:
            subheading = subheading[: self.SUBHEADING_MAX].rsplit(" ", 1)[0].rstrip(",.;:") + "…"
        try:
            # The Subheading button only appears when the subheading
            # textarea is not already shown. If it's missing, the
            # textarea is already there from a prior run on this draft.
            if page.locator(self.SUBHEADING_TEXTAREA_SELECTOR).count() == 0:
                page.locator(self.SUBHEADING_BUTTON_SELECTOR).first.click(timeout=8000)
                page.wait_for_timeout(1500)
            field = page.locator(self.SUBHEADING_TEXTAREA_SELECTOR).first
            field.click()
            # Same fill-vs-React quirk as the canonical URL field.
            field.press("Control+A" if sys.platform != "darwin" else "Meta+A")
            page.keyboard.press("Delete")
            page.keyboard.type(subheading, delay=5)
            return True
        except Exception as err:  # noqa: BLE001
            print(f"  [hashnode] subheading fill failed (non-fatal): {err}",
                  file=sys.stderr)
            return False

    def _open_publish_dialog_discovery(self, page) -> None:
        """Open the publish dialog and switch to the Discovery tab.

        The top-bar Publish click is intermittently swallowed when the
        editor hasn't fully hydrated, so retry until the Discovery tab
        is reachable.
        """
        for _ in range(4):
            page.evaluate(self._CLICK_TOP_PUBLISH_JS)
            page.wait_for_timeout(3000)
            if page.locator(self.DISCOVERY_TAB_SELECTOR).count() > 0:
                break
            page.wait_for_timeout(2000)
        page.locator(self.DISCOVERY_TAB_SELECTOR).first.click(timeout=15000)
        page.wait_for_timeout(2000)

    def _set_tags(self, page, mod: str) -> bool:
        try:
            tags_input = page.locator(self.TAGS_INPUT_SELECTOR).first
            tags_input.wait_for(state="visible", timeout=10000)
            # Existing tags render as buttons in a row sibling to the
            # input. Clicking a pill removes it. Synthetic .click() via
            # JS is ignored — Hashnode listens for native pointer
            # events — so we use Playwright's real click and loop on
            # the live count.
            pill_row = tags_input.locator("xpath=ancestor::div[contains(@class,'space-y-3')][1]")
            for _ in range(20):  # hard cap so we never loop forever
                pills = pill_row.locator("button:has-text('#')")
                count = pills.count()
                if count == 0:
                    break
                pills.first.click()
                page.wait_for_timeout(400)
            # Add the canonical tag set. Typing alone + Enter triggers
            # Hashnode's autocomplete and frequently commits a
            # "fuzzy-match" tag (e.g. "java" → "javascript",
            # "opensource" → "opensource-inactive"). Strategy:
            #
            #   1. Type the full tag name.
            #   2. Click the exact-match dropdown suggestion if one is
            #      present (its first line is "#<tag>" followed by a
            #      post-count line).
            #   3. If no exact-match suggestion is available, press
            #      Escape to dismiss the autocomplete dropdown, then
            #      Enter to commit the literal typed value. Escape
            #      before Enter prevents Enter from picking the
            #      currently-highlighted fuzzy suggestion.
            #
            # After each iteration verify the target pill landed; if
            # not, retry once more.
            for tag in self.TAGS:
                pills_before = self._pill_count(pill_row)
                for attempt in range(2):
                    tags_input.click()
                    page.keyboard.press(f"{mod}+A")
                    page.keyboard.press("Delete")
                    page.keyboard.type(tag, delay=15)
                    page.wait_for_timeout(1200)
                    if not self._click_exact_tag_suggestion(page, tag):
                        page.keyboard.press("Escape")
                        page.wait_for_timeout(200)
                        # Escape moves focus off the input on some
                        # builds; re-focus, restore the typed value,
                        # and commit with Enter so Hashnode treats
                        # this as a "create new tag" rather than
                        # selecting an autocomplete option.
                        tags_input.click()
                        if tags_input.input_value() != tag:
                            page.keyboard.press(f"{mod}+A")
                            page.keyboard.press("Delete")
                            page.keyboard.type(tag, delay=15)
                            page.wait_for_timeout(300)
                        page.keyboard.press("Escape")
                        page.wait_for_timeout(200)
                        tags_input.click()
                        page.keyboard.press("Enter")
                    page.wait_for_timeout(1200)
                    if self._pill_count(pill_row) > pills_before:
                        break
                    print(f"  [hashnode] tag '{tag}' did not land (attempt {attempt + 1}); retrying",
                          file=sys.stderr)
                else:
                    print(f"  [hashnode] tag '{tag}' could not be added after retry; skipping",
                          file=sys.stderr)
            return True
        except Exception as err:  # noqa: BLE001
            print(f"  [hashnode] tag set failed (non-fatal): {err}",
                  file=sys.stderr)
            return False

    @staticmethod
    def _pill_count(pill_row) -> int:
        return pill_row.locator("button:has-text('#')").count()

    @staticmethod
    def _click_exact_tag_suggestion(page, tag: str) -> bool:
        """Click the dropdown suggestion whose first line is exactly ``#<tag>``.

        Suggestions render as ``button.flex.w-full.items-start`` and
        their innerText is two lines: ``#<slug>\\n<post-count> posts``.
        """
        clicked = page.evaluate(
            """
            (tag) => {
                const wanted = '#' + tag;
                const buttons = Array.from(document.querySelectorAll(
                    "button.flex.w-full.items-start"
                ));
                const match = buttons.find(b => {
                    const r = b.getBoundingClientRect();
                    if (r.width === 0) return false;
                    const firstLine = (b.innerText || '').split('\\n', 1)[0].trim();
                    return firstLine === wanted;
                });
                if (!match) return null;
                const r = match.getBoundingClientRect();
                return {x: r.left + r.width / 2, y: r.top + r.height / 2};
            }
            """,
            tag,
        )
        if not clicked:
            return False
        # Use a real mouse click — Hashnode's React handlers ignore
        # synthetic button.click() in the same way the pills do.
        page.mouse.click(clicked["x"], clicked["y"])
        return True

    def _set_canonical_url(self, page, canonical_url: str, mod: str) -> bool:
        try:
            # "Add a canonical URL" is a Radix switch label that
            # reveals the input below it. Only click if the input is
            # not already visible (clicking a second time would toggle
            # it back off).
            if page.locator(self.CANONICAL_INPUT_SELECTOR).count() == 0:
                page.locator(self.CANONICAL_TOGGLE_SELECTOR).first.click(timeout=8000)
                page.wait_for_timeout(1500)
            canonical_input = page.locator(self.CANONICAL_INPUT_SELECTOR).first
            # Locator.fill() programmatically replaces the value, but
            # Hashnode's React form does not re-render from the
            # resulting input event when a prior value (from an
            # earlier syndication) is already in the field. Select-all
            # + type generates real key events so React picks up the
            # change.
            canonical_input.click()
            page.keyboard.press(f"{mod}+A")
            page.keyboard.press("Delete")
            page.wait_for_timeout(300)
            page.keyboard.type(canonical_url, delay=5)
            canonical_input.press("Tab")  # blur to flush onBlur handlers
            page.wait_for_timeout(2000)
            return True
        except Exception as err:  # noqa: BLE001
            print(f"  [hashnode] canonical URL set failed (non-fatal): {err}",
                  file=sys.stderr)
            return False


ADAPTERS: dict[str, Callable[[], Any]] = {
    "foojay": FoojayAdapter,
    "hashnode": HashnodeAdapter,
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
        # Adapters that authenticate via a saved Playwright storageState
        # (Hashnode, and historically Medium/DZone) expose a
        # `storage_state_path()` classmethod that returns a path to the
        # decoded JSON. FoojayAdapter logs in with user+password and does
        # not define it.
        storage_state_method = getattr(adapter, "storage_state_path", None)
        if callable(storage_state_method):
            try:
                context_kwargs["storage_state"] = str(storage_state_method())
            except KeyError as err:
                raise AdapterError(
                    f"{adapter.name} needs a storage-state env var: {err}"
                ) from err
        context = browser.new_context(**context_kwargs)
        # Grant clipboard access so navigator.clipboard.writeText() succeeds.
        try:
            context.grant_permissions(["clipboard-read", "clipboard-write"])
        except Exception:  # noqa: BLE001
            # Firefox and WebKit reject the chromium-only clipboard-* perms.
            # Adapters that need the clipboard fall back to other paths
            # (Quill API, Froala API, execCommand insertHTML), so a refusal
            # here is non-fatal.
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
