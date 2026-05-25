#!/usr/bin/env python3
"""Selenium + undetected-chromedriver runner that drives Medium and
DZone editors locally using your real Chrome.

Architecture:

* ``syndicate_login.py`` captures cookies + Cloudflare clearance into
  a shared Chrome user-data-dir under
  ``scripts/website/syndication-auth/chrome-profile/``.
* This runner reuses that profile, so Cloudflare doesn't re-challenge
  and Medium/DZone recognise you as already-signed-in.
* All input goes through Selenium's ``ActionChains.send_keys`` /
  ``element.send_keys``, which routes through Chrome's real input
  pipeline. The events carry ``isTrusted=true``, so Medium's typing
  handler converts markdown shortcuts (``## `` → heading,
  ``* `` → bullet, ```` ``` ```` → code block, ``**bold**`` → bold,
  ``[text](url)`` → link) on the fly.
* DZone uses simple text inputs + the Froala JS API; ``execute_script``
  drives the Froala instance directly in the page context.

Usage::

    pip install undetected-chromedriver selenium setuptools
    python scripts/website/syndicate_browser.py
    python scripts/website/syndicate_browser.py --task-id medium:my-slug
    python scripts/website/syndicate_browser.py --site dzone --dry-run
"""

from __future__ import annotations

import argparse
import datetime as dt
import json
import os
import platform
import re
import subprocess
import sys
import tempfile
import time
import urllib.error
import urllib.request
from pathlib import Path
from typing import Any, Callable


REPO_ROOT = Path(__file__).resolve().parents[2]
BLOG_DIR = REPO_ROOT / "docs" / "website" / "content" / "blog"
QUEUE_FILE = Path(__file__).resolve().parent / "syndication-queue.json"
STATE_FILE = Path(__file__).resolve().parent / "syndication-state.json"
AUTH_DIR = Path(__file__).resolve().parent / "syndication-auth"
PROFILE_DIR = AUTH_DIR / "chrome-profile"
SITE_BASE = "https://www.codenameone.com"

# Reuse front-matter parsing + Hugo-footer stripping from the existing
# syndication tooling so the markdown handling stays consistent.
sys.path.insert(0, str(Path(__file__).resolve().parent))
from syndicate_blog_posts import (  # noqa: E402
    parse_front_matter,
    _HUGO_FOOTER_RE,
    _HUGO_SHORTCODE_RE,
    absolutize_links,
)


# ---------------------------------------------------------------------
# Queue / state IO
# ---------------------------------------------------------------------

def _load_json(path: Path, default: dict) -> dict:
    if not path.exists():
        return default
    return json.loads(path.read_text(encoding="utf-8"))


def is_already_done(state: dict, task: dict) -> bool:
    return task["site"] in state.get("posts", {}).get(task["slug"], {})


def record_result(state: dict, task: dict, url: str) -> None:
    state.setdefault("posts", {}).setdefault(task["slug"], {})[task["site"]] = {
        "url": url,
        "syndicated_at": dt.datetime.now(dt.timezone.utc).isoformat(timespec="seconds"),
    }


# ---------------------------------------------------------------------
# Markdown prep (shared)
# ---------------------------------------------------------------------

def read_post_markdown(slug: str) -> tuple[dict[str, Any], str]:
    """Read the local blog post markdown for ``slug`` and return
    ``(front_matter, body)`` with the Hugo discussion footer + shortcodes
    stripped and root-relative URLs absolutized."""
    md_path = BLOG_DIR / f"{slug}.md"
    if not md_path.exists():
        raise FileNotFoundError(f"blog post not found: {md_path}")
    raw = md_path.read_text(encoding="utf-8")
    fm, body = parse_front_matter(raw)
    body = body.strip("\n")
    body = _HUGO_FOOTER_RE.sub("", body)
    body = _HUGO_SHORTCODE_RE.sub("", body).rstrip()
    body = absolutize_links(body)
    return fm, body


def download_to_tempfile(url: str) -> Path:
    """Fetch ``url`` and store the bytes in a temp file. Returns the
    path; caller cleans up. Used for cover images.

    Uses a browser-like User-Agent header — codenameone.com (Hugo +
    Cloudflare in front) returns HTTP 403 to python-urllib's default
    ``Python-urllib/3.x`` UA."""
    suffix = Path(urllib.request.urlparse(url).path).suffix or ".bin"
    fd = tempfile.NamedTemporaryFile(delete=False, suffix=suffix)
    try:
        req = urllib.request.Request(
            url,
            headers={
                "User-Agent": "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/147.0.0.0 Safari/537.36",
                "Accept": "image/avif,image/webp,image/png,image/svg+xml,image/*,*/*;q=0.8",
            },
        )
        with urllib.request.urlopen(req) as resp:
            fd.write(resp.read())
        return Path(fd.name)
    finally:
        fd.close()


# ---------------------------------------------------------------------
# Chrome version auto-detection
# ---------------------------------------------------------------------

def detect_chrome_major_version() -> int | None:
    """Read Chrome's major version without subprocess invocation —
    on macOS we read the .app's Info.plist directly. The previous
    subprocess approach would silently fail (and the script would
    fall back to the latest chromedriver, which mismatches Chrome)
    when Chrome was already running with our profile."""
    if platform.system() == "Darwin":
        plist_paths = [
            "/Applications/Google Chrome.app/Contents/Info.plist",
            "/Applications/Chromium.app/Contents/Info.plist",
        ]
        for p in plist_paths:
            if not os.path.exists(p):
                continue
            try:
                import plistlib
                with open(p, "rb") as f:
                    data = plistlib.load(f)
                version = data.get("CFBundleShortVersionString", "")
                m = re.match(r"(\d+)", version)
                if m:
                    return int(m.group(1))
            except Exception:
                continue
        return None
    # Linux / Windows: subprocess --version is reliable enough.
    candidates: list[str] = []
    if platform.system() == "Linux":
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


# ---------------------------------------------------------------------
# Medium driver
# ---------------------------------------------------------------------

def process_medium(driver, task: dict, log: Callable[[str], None]) -> str:
    """Drive Medium's /new-story editor. Returns the resulting draft URL.

    Strategy:
      1. Type the title via real keystrokes (already proven to work and
         to allocate the /p/<id>/edit URL on first input).
      2. Render the markdown body to clean HTML (python-markdown).
      3. Set the macOS clipboard to ``text/html`` via ``osascript``.
      4. Press Cmd+V in the body editor — a real isTrusted=true paste
         event Medium's paste handler accepts and converts into native
         graf elements, including <strong>/<em> for bold/italic,
         <h2>/<h3> for headings, <pre><code> for code blocks,
         <a href> for links, <ul>/<ol>/<li> for lists, and <img src>
         for images (Medium auto-fetches external image URLs and
         rehosts them on its CDN during paste).
    """
    from selenium.webdriver.common.by import By
    from selenium.webdriver.common.keys import Keys
    from selenium.webdriver.common.action_chains import ActionChains
    from selenium.webdriver.support.ui import WebDriverWait
    from selenium.webdriver.support import expected_conditions as EC

    fm, body_md = read_post_markdown(task["slug"])
    title = (fm.get("title") or task.get("title") or "").strip()
    if not title:
        raise RuntimeError("no title in front matter or task")

    body_html = render_markdown_for_medium(body_md)
    log(f"title='{title}', body={len(body_md)} md / {len(body_html)} html chars")

    driver.get("https://medium.com/new-story")
    if "/m/signin" in driver.current_url:
        raise RuntimeError(f"Medium redirected to sign-in (auth state expired?). url={driver.current_url}")

    wait = WebDriverWait(driver, 20)
    title_el = wait.until(EC.presence_of_element_located(
        (By.CSS_SELECTOR, 'h3[data-testid="editorTitleParagraph"], h3.graf--title')
    ))
    log("title element found")

    title_el.click()
    ActionChains(driver).send_keys(title).perform()
    log(f"title typed ({len(title)} chars)")

    # Press Enter to create the body's first paragraph and move cursor
    # into it. Wait for Medium to mount the body graf.
    time.sleep(1)
    ActionChains(driver).send_keys(Keys.ENTER).perform()
    time.sleep(1)
    log(f"url after Enter: {driver.current_url}")

    # Set the clipboard to our rendered HTML, then Cmd+V. Medium's
    # paste handler runs and converts the HTML to its block model.
    macos_set_clipboard_html(body_html)
    log("clipboard set to text/html")

    # Make sure focus is in the body editor. Click the most recently
    # added paragraph (the one Enter just created). The body editor
    # placeholder is "Tell your story…".
    try:
        body_p = driver.find_element(By.CSS_SELECTOR, "p.graf--p, p[data-testid='editorParagraphText']")
        body_p.click()
    except Exception:
        # Fallback: click the editor root.
        editor = driver.find_element(By.CSS_SELECTOR, "div.postArticle-content[contenteditable='true'], div[contenteditable='true']")
        editor.click()

    ActionChains(driver).key_down(Keys.COMMAND).send_keys('v').key_up(Keys.COMMAND).perform()
    log("Cmd+V pasted")

    # Give Medium time to fetch external images and run autosave.
    # Image fetch can take several seconds per image.
    time.sleep(15)

    # Click outside the editor to flush any pending autosave.
    driver.find_element(By.CSS_SELECTOR, "body").click()
    time.sleep(3)

    # Poll up to 90s for the URL to land on /p/<id>/edit (it usually
    # lands within a few seconds of the title typing, so this is mostly
    # a safety net here).
    deadline = time.time() + 90
    draft_url = None
    while time.time() < deadline:
        if re.search(r"medium\.com/p/[A-Za-z0-9]+/edit", driver.current_url):
            draft_url = driver.current_url
            log(f"draft URL: {draft_url}")
            break
        time.sleep(1)
    if not draft_url:
        raise RuntimeError(f"autosave did not produce a /p/<id>/edit URL within 90s (still at {driver.current_url})")

    # Post-paste cleanup: set code-block languages and the canonical URL.
    set_code_block_languages(driver, log, default_lang="java")
    if task.get("canonical"):
        try:
            set_medium_canonical(driver, draft_url, task["canonical"], log)
        except Exception as err:
            log(f"WARNING: could not set canonical URL: {err}")
    # Submit-to-publication flow is implemented but disabled — enable
    # by passing submit_to_publication=True (planned: per-task flag).
    # submit_to_publication(driver, "Javarevisited", log)
    time.sleep(2)
    return draft_url


def set_code_block_languages(driver, log: Callable[[str], None], default_lang: str = "java") -> None:
    """Tag every Medium code block with ``data-code-block-lang``.
    Medium's paste handler may or may not pick up
    ``class="language-java"`` from python-markdown's output (varies by
    paste flow), so we set the attribute directly via JS as a backstop.
    Casts a wide selector net (graf--pre, graf--preV2,
    [data-testid='editorCodeBlockParagraph']) since the exact class
    cocktail Medium emits has changed over time."""
    try:
        diag = driver.execute_script(
            """
            const all = Array.from(document.querySelectorAll(
              'pre.graf--pre, pre.graf--preV2, pre[data-testid="editorCodeBlockParagraph"], pre[class*="graf--pre"]'
            ));
            const dedup = Array.from(new Set(all));
            return dedup.map((p, i) => ({
              idx: i,
              cls: (p.className || '').slice(0, 80),
              lang: p.getAttribute('data-code-block-lang') || '',
              text: (p.innerText || '').slice(0, 60).replace(/\\s+/g, ' '),
            }));
            """
        )
        log(f"pre blocks discovered: {len(diag)}")
        for d in diag:
            log(f"  [{d['idx']}] lang='{d['lang']}' cls='{d['cls'][:60]}' text='{d['text']}'")

        count = driver.execute_script(
            """
            const lang = arguments[0];
            const all = Array.from(document.querySelectorAll(
              'pre.graf--pre, pre.graf--preV2, pre[data-testid="editorCodeBlockParagraph"], pre[class*="graf--pre"]'
            ));
            const dedup = Array.from(new Set(all));
            let count = 0;
            dedup.forEach(pre => {
              const cur = pre.getAttribute('data-code-block-lang');
              if (!cur || cur === '') {
                pre.setAttribute('data-code-block-lang', lang);
                count++;
              }
            });
            return count;
            """,
            default_lang,
        )
        log(f"set data-code-block-lang='{default_lang}' on {count} code block(s)")
    except Exception as err:
        log(f"WARNING: code-block language tagging failed: {err}")


def set_medium_canonical(driver, draft_url: str, canonical_url: str, log: Callable[[str], None]) -> None:
    """Navigate to Medium's per-story settings page, expand Advanced
    Settings, check the "originally published elsewhere" box (which
    enables the canonical-link input), click "Edit canonical link",
    fill the URL, and return to the editor.

    The settings page lives at ``/p/<id>/settings``."""
    from selenium.webdriver.common.by import By
    from selenium.webdriver.common.keys import Keys
    from selenium.webdriver.common.action_chains import ActionChains
    from selenium.webdriver.support.ui import WebDriverWait
    from selenium.webdriver.support import expected_conditions as EC

    settings_url = re.sub(r"/edit/?$", "/settings", draft_url)
    if settings_url == draft_url:
        settings_url = draft_url.rstrip("/") + "/settings"
    driver.get(settings_url)
    log(f"navigated to {settings_url}")
    time.sleep(2)

    wait = WebDriverWait(driver, 20)

    # 1. Expand Advanced Settings.
    try:
        adv_btn = wait.until(EC.element_to_be_clickable(
            (By.XPATH, "//button[.//*[normalize-space(text())='Advanced Settings']]")
        ))
    except Exception as err:
        _dump_settings_page(driver, log, "Advanced Settings button not found")
        raise RuntimeError(f"Advanced Settings button not found: {err}")
    # Always click to expand; if already expanded, the flow below still
    # finds the elements.
    adv_btn.click()
    log("clicked Advanced Settings (expand)")
    time.sleep(1.5)

    # 2. Check the "originally published elsewhere" checkbox. The
    # checkbox itself may be visually hidden (custom-styled label), so
    # click the *label* not the input. This enables the canonical
    # input field.
    try:
        published_label = WebDriverWait(driver, 8).until(EC.presence_of_element_located(
            (By.XPATH, "//label[.//p[contains(normalize-space(.), 'originally published elsewhere')]]")
        ))
        cb = published_label.find_element(By.CSS_SELECTOR, "input[type='checkbox']")
        if not cb.is_selected():
            try:
                published_label.click()
            except Exception:
                # Fallback: JS click on the checkbox itself.
                driver.execute_script("arguments[0].click();", cb)
            log("checked 'originally published elsewhere'")
            time.sleep(0.8)
        else:
            log("'originally published elsewhere' already checked")
    except Exception as err:
        log(f"WARNING: could not toggle 'originally published' checkbox: {err}")

    # 3. Click "Edit canonical link" to enable the input. Use a
    # text-contains XPath so case / whitespace variations don't
    # break us.
    edit_clicked = False
    for sel in (
        "//button[normalize-space(text())='Edit canonical link']",
        "//button[contains(normalize-space(.), 'Edit canonical link')]",
        "//button[contains(., 'Edit canonical')]",
    ):
        try:
            btn = WebDriverWait(driver, 4).until(EC.element_to_be_clickable((By.XPATH, sel)))
            btn.click()
            edit_clicked = True
            log(f"clicked Edit canonical link (selector: {sel})")
            time.sleep(0.6)
            break
        except Exception:
            continue
    if not edit_clicked:
        log("WARNING: 'Edit canonical link' button not found — input may already be enabled by the checkbox check")

    # 4. Find and fill the canonical input.
    canonical_input = None
    for sel in (
        "input[placeholder*='canonical URL' i]",
        "input[placeholder*='canonical' i]",
        "input[placeholder*='original story' i]",
        "input[placeholder*='URL of original' i]",
    ):
        try:
            cand = WebDriverWait(driver, 5).until(EC.presence_of_element_located((By.CSS_SELECTOR, sel)))
            if cand:
                canonical_input = cand
                log(f"found canonical input via selector '{sel}'")
                break
        except Exception:
            continue
    if not canonical_input:
        _dump_settings_page(driver, log, "canonical input not found")
        raise RuntimeError("canonical URL input not found on settings page after enabling")

    # Wait for the input to become enabled (the disabled attribute
    # gets removed asynchronously after Edit is clicked).
    for _ in range(15):
        if canonical_input.is_enabled() and not canonical_input.get_attribute("disabled"):
            break
        time.sleep(0.3)
    if canonical_input.get_attribute("disabled"):
        # Force-enable via JS as a last resort.
        driver.execute_script("arguments[0].removeAttribute('disabled');", canonical_input)
        log("force-enabled canonical input via JS")

    canonical_input.click()
    canonical_input.send_keys(Keys.COMMAND, "a")
    canonical_input.send_keys(Keys.DELETE)
    canonical_input.send_keys(canonical_url)
    log(f"typed canonical URL: {canonical_url}")
    # Blur to commit.
    canonical_input.send_keys(Keys.TAB)
    time.sleep(1)

    # Click any confirm/save button if one exists.
    for label in ("Save canonical link", "Save", "Done", "Apply"):
        try:
            btn = driver.find_element(By.XPATH, f"//button[normalize-space(text())='{label}']")
            if btn.is_displayed() and btn.is_enabled():
                btn.click()
                log(f"clicked '{label}'")
                time.sleep(1)
                break
        except Exception:
            continue

    # Verify the value stuck.
    try:
        final = canonical_input.get_attribute("value") or ""
        if final.strip() == canonical_url.strip():
            log(f"verified canonical URL set to {final}")
        else:
            log(f"WARNING: canonical input value is '{final}', expected '{canonical_url}'")
    except Exception:
        pass

    # Navigate back to the draft editor.
    driver.get(draft_url)
    log("navigated back to draft editor")
    time.sleep(2)


def _dump_settings_page(driver, log: Callable[[str], None], reason: str) -> None:
    """Diagnostic dump of relevant elements on the settings page when
    the canonical-URL flow can't find what it expects."""
    try:
        log(f"--- diagnostic dump ({reason}) ---")
        log(f"current URL: {driver.current_url}")
        snap = driver.execute_script("""
            const buttons = Array.from(document.querySelectorAll('button'))
              .filter(b => /canonical|edit|advanced|save/i.test((b.textContent || '') + (b.getAttribute('aria-label') || '')))
              .slice(0, 20)
              .map(b => `button text="${(b.textContent || '').trim().slice(0, 60)}" aria="${b.getAttribute('aria-label') || ''}" visible=${b.offsetParent !== null}`);
            const inputs = Array.from(document.querySelectorAll('input'))
              .filter(i => /canonical|url|original/i.test((i.placeholder || '') + (i.getAttribute('aria-label') || '') + (i.name || '')))
              .slice(0, 10)
              .map(i => `input type=${i.type} placeholder="${i.placeholder || ''}" disabled=${i.disabled} value="${(i.value || '').slice(0, 40)}"`);
            return {buttons, inputs};
        """)
        for b in snap.get("buttons", []):
            log(f"  {b}")
        for i in snap.get("inputs", []):
            log(f"  {i}")
        log("--- end diagnostic dump ---")
    except Exception as err:
        log(f"diagnostic dump failed: {err}")


# ---------------------------------------------------------------------
# Submit-to-publication flow (commented out — enable per task field
# once the canonical-URL flow is verified).
# ---------------------------------------------------------------------
#
# def submit_to_publication(driver, publication_name: str, log: Callable[[str], None]) -> None:
#     """From the draft editor, open More options → Submit to publication,
#     pick the publication, uncheck "Paywall this story", click Send for
#     review. Selectors based on user-supplied DOM dump (see PR thread)."""
#     from selenium.webdriver.common.by import By
#     from selenium.webdriver.common.keys import Keys
#     from selenium.webdriver.support.ui import WebDriverWait
#     from selenium.webdriver.support import expected_conditions as EC
#
#     # 1. Open the "More options" popover (button containing svgIcon--moreFilled).
#     more_btn = WebDriverWait(driver, 10).until(EC.element_to_be_clickable(
#         (By.XPATH, "//button[.//span[contains(@class, 'svgIcon--moreFilled')]]")
#     ))
#     more_btn.click()
#     log("opened More options popover")
#
#     # 2. Click "Submit to publication" (data-action="show-post-submission").
#     submit_btn = WebDriverWait(driver, 5).until(EC.element_to_be_clickable(
#         (By.CSS_SELECTOR, "button[data-action='show-post-submission']")
#     ))
#     submit_btn.click()
#     log("clicked Submit to publication")
#
#     # 3. On the submission page, click the publication card whose
#     #    visible name matches publication_name (e.g. "Javarevisited").
#     pub_btn = WebDriverWait(driver, 15).until(EC.element_to_be_clickable(
#         (By.XPATH, f"//button[.//h2[normalize-space(text())='{publication_name}']]")
#     ))
#     pub_btn.click()
#     log(f"selected publication: {publication_name}")
#
#     # 4. Uncheck "Paywall this story".
#     try:
#         paywall_label = WebDriverWait(driver, 10).until(EC.presence_of_element_located(
#             (By.XPATH, "//label[.//p[normalize-space(text())='Paywall this story']]")
#         ))
#         paywall_cb = paywall_label.find_element(By.CSS_SELECTOR, "input[type='checkbox']")
#         if paywall_cb.is_selected():
#             paywall_label.click()
#             log("unchecked Paywall this story")
#     except Exception as err:
#         log(f"WARNING: could not toggle paywall checkbox: {err}")
#
#     # 5. Click "Send for review".
#     send_btn = WebDriverWait(driver, 10).until(EC.element_to_be_clickable(
#         (By.XPATH, "//button[normalize-space(text())='Send for review']")
#     ))
#     send_btn.click()
#     log("clicked Send for review")
#     time.sleep(3)


def render_markdown_for_medium(md: str) -> str:
    """Render markdown to a clean HTML subset Medium's paste handler
    accepts cleanly: headings, paragraphs, bold/italic, inline code,
    fenced code blocks, links, lists, blockquotes, images. Strips any
    inline ``style=`` attributes (leftovers from Hugo extensions),
    drops <aside> tags (no aside graf type in Medium), and de-blank-
    lines inside <pre> blocks (Medium splits a single pre into
    multiple blocks at every blank line otherwise)."""
    import markdown as _md
    html = _md.markdown(
        md,
        extensions=["extra", "fenced_code", "sane_lists"],
        output_format="html5",
    )
    html = re.sub(r'\s+style="[^"]*"', "", html)
    html = re.sub(r'<aside[^>]*>(.*?)</aside>', r'<blockquote>\1</blockquote>', html, flags=re.DOTALL)
    html = _fix_pre_blank_lines(html)
    return html


def _fix_pre_blank_lines(html: str) -> str:
    """Inside each <pre>...</pre> block, replace runs of blank lines
    with a single line carrying a non-breaking space. Medium's paste
    handler interprets a literal blank line inside <pre> as a
    paragraph separator and emits multiple ``<pre graf--pre>`` blocks;
    a non-blank line keeps them as one block."""
    def fix_block(m: re.Match) -> str:
        inner = m.group(1)
        inner = re.sub(r'\n[ \t]*\n', '\n \n', inner)
        return f'<pre>{inner}</pre>'
    return re.sub(r'<pre>(.*?)</pre>', fix_block, html, flags=re.DOTALL)


def macos_set_clipboard_html(html: str) -> None:
    """Set the macOS clipboard to HTML content via osascript. Medium's
    paste handler reads ``text/html`` from the clipboard and runs its
    HTML-to-graf converter; if we set only ``text/plain`` (e.g. via
    pbcopy or pyperclip) Medium pastes literal HTML markup as text."""
    import os, tempfile
    fd, path = tempfile.mkstemp(suffix=".html", text=True)
    try:
        with os.fdopen(fd, "w", encoding="utf-8") as f:
            f.write(html)
        # «class HTML» (U+00AB / U+00BB) tells AppleScript to read the
        # file as the HTML clipboard type, not raw text.
        script = f'set the clipboard to (read POSIX file "{path}" as «class HTML»)'
        result = subprocess.run(
            ["osascript", "-e", script],
            capture_output=True, text=True, timeout=15,
        )
        if result.returncode != 0:
            raise RuntimeError(f"osascript clipboard set failed: {result.stderr.strip() or result.stdout.strip()}")
    finally:
        try:
            os.unlink(path)
        except Exception:
            pass


# ---------------------------------------------------------------------
# DZone driver
# ---------------------------------------------------------------------

def process_dzone(driver, task: dict, log: Callable[[str], None]) -> str:
    """Drive DZone's article editor. Returns the resulting draft URL.
    Raises on failure."""
    from selenium.webdriver.common.by import By
    from selenium.webdriver.common.keys import Keys
    from selenium.webdriver.support.ui import WebDriverWait
    from selenium.webdriver.support import expected_conditions as EC

    # Look up author from the post's front matter so we can fill the
    # contributor field DZone requires before Save Draft will commit.
    author_name = ""
    try:
        fm, _ = read_post_markdown(task["slug"])
        author_name = (fm.get("author") or "").strip()
    except Exception:
        pass

    # Try DZone's known editor URLs in order. Modern DZone uses
    # /articles/post.html; older paths /content/article/post.html
    # may still work as redirects.
    editor_urls = [
        "https://dzone.com/articles/post.html",
        "https://dzone.com/content/article/post.html",
    ]
    title_el = None
    last_err = None
    for url in editor_urls:
        driver.get(url)
        time.sleep(2)
        log(f"navigated to {url} -> landed at {driver.current_url} (title: {driver.title[:60]})")
        if "/users/login" in driver.current_url:
            raise RuntimeError(f"DZone redirected to login (auth state expired?). url={driver.current_url}")
        try:
            wait = WebDriverWait(driver, 20)
            title_el = wait.until(EC.presence_of_element_located((By.CSS_SELECTOR, "textarea[name='title']")))
            log(f"title textarea found via {url}")
            break
        except Exception as err:
            last_err = err
            log(f"textarea[name='title'] not found at {url}; trying next URL")
            continue
    if title_el is None:
        # Diagnostic: dump form structure on the page we ended up at.
        try:
            snap = driver.execute_script("""
                const inputs = Array.from(document.querySelectorAll('input, textarea')).slice(0, 30).map(el => ({
                    tag: el.tagName.toLowerCase(),
                    name: el.name || '',
                    id: el.id || '',
                    placeholder: el.placeholder || '',
                    visible: el.offsetParent !== null,
                }));
                return {url: location.href, title: document.title, inputs};
            """)
            log(f"diagnostic: url={snap['url']}, title={snap['title']!r}")
            log(f"  inputs (count={len(snap['inputs'])}):")
            for el in snap['inputs']:
                log(f"    {el}")
        except Exception as derr:
            log(f"diagnostic dump failed: {derr}")
        raise RuntimeError(f"DZone title textarea not found at any known editor URL (last error: {last_err})")

    # Title (Angular ng-model on a textarea — send_keys works).
    title_el.click()
    title_el.clear()
    title_el.send_keys(task["title"])
    log("title filled")

    # Subtitle and meta-description (best-effort).
    # Subtitle (TL;DR) hard-limit ~170 chars on DZone — text past that
    # silently truncates the visible TL;DR in the published view.
    # Meta description ~155 chars (Google's snippet cap).
    desc = (task.get("description") or "").strip()
    if desc:
        try:
            sub = driver.find_element(By.CSS_SELECTOR, "textarea[name='subtitle']")
            sub.clear()
            sub.send_keys(desc[:170])
        except Exception:
            pass
        try:
            meta = driver.find_element(By.ID, "meta-description-textarea")
            meta.clear()
            meta.send_keys(desc[:155])
        except Exception:
            pass
        log(f"subtitle/meta filled (len={len(desc)}, sub-cap=170, meta-cap=155)")

    # Original source URL (canonical).
    if task.get("canonical"):
        try:
            src = driver.find_element(By.CSS_SELECTOR, "input[ng-model='article.originalSource']")
            src.clear()
            src.send_keys(task["canonical"])
            log(f"original source: {task['canonical']}")
        except Exception:
            log("WARNING: original-source input not found")

    # Article type (Angular dropdown). DZone's sticky top header
    # frequently intercepts clicks on header-adjacent elements; use
    # the scroll+JS-click helper to bypass.
    article_type = task.get("article_type") or "News"
    try:
        toggle = driver.find_element(By.CSS_SELECTOR, "div.dropdown a.admin-selector.dropdown-toggle")
        _safe_click(driver, toggle)
        time.sleep(0.5)
        items = driver.find_elements(By.CSS_SELECTOR, "ul.dropdown-menu.portal-menu li a")
        for a in items:
            if (a.text or "").strip().lower() == article_type.lower():
                _safe_click(driver, a)
                log(f"article type: {article_type}")
                break
        else:
            log(f"WARNING: article type '{article_type}' not found")
    except Exception as err:
        log(f"article type set failed: {err}")

    # Featured image upload.
    if task.get("cover_image_url"):
        try:
            tmp = download_to_tempfile(task["cover_image_url"])
            log(f"downloaded cover image -> {tmp}")
            file_input = driver.find_element(
                By.CSS_SELECTOR,
                "input[type='file'][ng-file-change], input[type='file'][ng-file-select], input[type='file'][ng-file-drop]",
            )
            file_input.send_keys(str(tmp))
            log("cover image attached, waiting for upload to settle")
            # Wait for progress bar to hide.
            try:
                wait_short = WebDriverWait(driver, 60)
                wait_short.until(lambda d: d.execute_script(
                    "const p = document.querySelector('.progress-container');"
                    "return !p || (p.getAttribute('style') || '').match(/visibility:\\s*hidden/i) !== null;"
                ))
                log("cover image upload settled")
            except Exception as err:
                log(f"WARNING: upload-progress wait timed out: {err}")
        except Exception as err:
            log(f"cover image upload failed: {err}")

    # Body via Froala JS API (no xray issues in Selenium — execute_script
    # runs in the page context).
    if task.get("body_html"):
        try:
            wait_froala = WebDriverWait(driver, 15)
            wait_froala.until(lambda d: d.execute_script(
                "return !!(window.FroalaEditor && window.FroalaEditor.INSTANCES && window.FroalaEditor.INSTANCES.length);"
            ))
            driver.execute_script(
                "const inst = window.FroalaEditor.INSTANCES[0];"
                "inst.html.set(arguments[0]);"
                "if (inst.events && inst.events.trigger) inst.events.trigger('contentChanged');",
                task["body_html"],
            )
            log(f"body set via FroalaEditor.html.set ({len(task['body_html'])} chars)")
        except Exception as err:
            raise RuntimeError(f"Froala body set failed: {err}") from err

    # Add author/contributor — DZone refuses to save the draft without
    # at least one author entry properly registered via the
    # select-users widget. The "Orig. Poster" entry that's auto-created
    # for the logged-in user isn't enough on its own; we also need to
    # add the author through the search widget.
    if author_name:
        try:
            add_dzone_author(driver, author_name, log)
        except Exception as err:
            log(f"WARNING: could not add author '{author_name}': {err}")

    # Let Angular digest before clicking Save.
    time.sleep(2.5)

    # Save Draft. DZone's top bar overlays clicks here too — use the
    # scroll+JS-click helper.
    save_btns = [
        b for b in driver.find_elements(By.TAG_NAME, "button")
        if re.match(r"^save\s*draft$", (b.text or "").strip(), re.IGNORECASE) and b.is_displayed()
    ]
    if not save_btns:
        raise RuntimeError("Save Draft button not found")
    _safe_click(driver, save_btns[0])
    log("Save Draft clicked")

    time.sleep(6)
    return driver.current_url


def _safe_click(driver, element) -> None:
    """Click an element after scrolling it into view; if a sticky
    header (or other overlay) intercepts the native click, fall back
    to a JS dispatch which bypasses overlay hit-testing.

    DZone's editor pages have a sticky top nav bar that intercepts
    many click targets near the top of the viewport — scrolling first
    is enough most of the time, JS-click covers the residual cases."""
    from selenium.common.exceptions import ElementClickInterceptedException, ElementNotInteractableException
    try:
        driver.execute_script("arguments[0].scrollIntoView({block: 'center'});", element)
        time.sleep(0.2)
    except Exception:
        pass
    try:
        element.click()
    except (ElementClickInterceptedException, ElementNotInteractableException):
        driver.execute_script("arguments[0].click();", element)


def _dzone_search_user(driver, search_input, author_name: str, log: Callable[[str], None]) -> list:
    """Type the author name into DZone's user-select search input and
    wait for popup results. The widget is genuinely flaky — the search
    debounce + async fetch sometimes drops events. Try multiple
    strategies and return the matching `<div class="ui-select-choices-row">`
    elements when results appear."""
    from selenium.webdriver.common.by import By
    from selenium.webdriver.common.keys import Keys

    rows_selector = "div.select-users div.ui-select-choices-row"

    def wait_for_rows(timeout_s: float) -> list:
        deadline = time.time() + timeout_s
        while time.time() < deadline:
            r = driver.find_elements(By.CSS_SELECTOR, rows_selector)
            if r:
                return r
            time.sleep(0.3)
        return []

    # Strategy 1: send_keys directly to the input (focus is automatic
    # since the input was just un-hidden by the toggle click).
    search_input.send_keys(author_name)
    log(f"typed (strategy 1: send_keys all): {author_name}")
    rows = wait_for_rows(8)
    if rows:
        return rows

    # Strategy 2: clear, retype char-by-char with longer delays.
    log("no results yet; retrying char-by-char")
    try:
        search_input.send_keys(Keys.COMMAND, "a")
        search_input.send_keys(Keys.DELETE)
    except Exception:
        pass
    for ch in author_name:
        search_input.send_keys(ch)
        time.sleep(0.12)
    rows = wait_for_rows(8)
    if rows:
        return rows

    # Strategy 3: drive Angular scope directly. Set $select.search and
    # call $apply, which re-runs the watcher → the search() function
    # → the network call. This bypasses any input-event flakiness.
    log("no results yet; trying Angular scope $apply")
    try:
        driver.execute_script(
            """
            const el = document.querySelector('div.select-users input.ui-select-search');
            if (!el) return false;
            const ng = (window.angular && angular.element(el)) || null;
            if (!ng) return false;
            const scope = ng.scope ? ng.scope() : null;
            if (!scope || !scope.$select) return false;
            scope.$select.search = arguments[0];
            scope.$apply();
            return true;
            """,
            author_name,
        )
        time.sleep(2)
    except Exception as err:
        log(f"angular fallback failed: {err}")
    rows = wait_for_rows(8)
    return rows


def add_dzone_author(driver, author_name: str, log: Callable[[str], None]) -> None:
    """Open DZone's contributor user-select widget, search for the
    author by name, and click the matching popup result. DZone's
    "Save Draft" validation rejects with "please add author in the
    contributor field" until at least one author entry is registered
    via this widget (the auto-created "Orig. Poster" entry is not
    enough on its own).

    Widget structure (from user's DOM dump):
      div.select-users
        div.ui-select-container
          div.ui-select-match → span.ui-select-toggle  ← click to open
          input.ui-select-search                       ← type name here
          ul.ui-select-choices > li                    ← popup results
    """
    from selenium.webdriver.common.by import By
    from selenium.webdriver.common.keys import Keys
    from selenium.webdriver.support.ui import WebDriverWait
    from selenium.webdriver.support import expected_conditions as EC

    # Open the user-select dropdown. Try up to 3 times — the toggle
    # is sometimes intercepted briefly by other elements.
    toggle = WebDriverWait(driver, 10).until(EC.element_to_be_clickable(
        (By.CSS_SELECTOR, "div.select-users span.ui-select-toggle")
    ))
    _safe_click(driver, toggle)
    log("opened author user-select dropdown")

    # Wait for the search input to actually become visible (the input
    # has the `ng-hide` class until $select.open flips).
    search = WebDriverWait(driver, 10).until(EC.visibility_of_element_located(
        (By.CSS_SELECTOR, "div.select-users input.ui-select-search")
    ))

    rows = _dzone_search_user(driver, search, author_name, log)
    if not rows:
        raise RuntimeError(f"no search results appeared for '{author_name}' after multiple strategies")

    # Match by exact name in .user-info > span (the displayed name).
    needle = author_name.strip().lower()
    matched = None
    for row in rows:
        try:
            name_span = row.find_element(By.CSS_SELECTOR, ".user-info > span")
            name = (name_span.text or "").strip().lower()
            if name == needle:
                matched = row
                break
        except Exception:
            continue
    if not matched:
        # Fallback to substring match (e.g. for slight name variations).
        for row in rows:
            try:
                name_span = row.find_element(By.CSS_SELECTOR, ".user-info > span")
                if needle in (name_span.text or "").lower():
                    matched = row
                    break
            except Exception:
                continue
    if not matched:
        log(f"WARNING: no exact/substring match for '{author_name}'. Found rows:")
        for row in rows[:5]:
            try:
                name_span = row.find_element(By.CSS_SELECTOR, ".user-info > span")
                log(f"  '{(name_span.text or '').strip()[:50]}'")
            except Exception:
                log(f"  (no name span)")
        raise RuntimeError(f"no DZone user matching '{author_name}'")

    _safe_click(driver, matched)
    try:
        name_text = matched.find_element(By.CSS_SELECTOR, ".user-info > span").text
    except Exception:
        name_text = "(unknown)"
    log(f"selected author: {name_text}")
    time.sleep(1)


# ---------------------------------------------------------------------
# Orchestration
# ---------------------------------------------------------------------

DRIVERS: dict[str, Callable] = {
    "medium": process_medium,
    "dzone": process_dzone,
}


def make_chrome_driver(profile_dir: Path, chrome_version: int | None):
    import undetected_chromedriver as uc
    options = uc.ChromeOptions()
    options.add_argument(f"--user-data-dir={profile_dir}")
    options.add_argument("--profile-directory=Default")
    options.add_argument("--no-first-run")
    options.add_argument("--no-default-browser-check")
    return uc.Chrome(
        options=options,
        headless=False,
        use_subprocess=True,
        version_main=chrome_version,
    )


def process_task(task: dict, log: Callable[[str], None],
                 profile_dir: Path, chrome_version: int | None) -> tuple[bool, str | None, str | None]:
    """Returns (success, url, error)."""
    site = task["site"]
    driver_fn = DRIVERS.get(site)
    if driver_fn is None:
        return False, None, f"unknown site '{site}'"

    if not profile_dir.exists() or not (profile_dir / "Default").exists():
        return False, None, (
            f"profile dir not initialized at {profile_dir}. Run: "
            f"python scripts/website/syndicate_login.py --site {site}"
        )

    driver = make_chrome_driver(profile_dir, chrome_version)
    try:
        url = driver_fn(driver, task, log)
        return True, url, None
    except Exception as err:
        try:
            current = driver.current_url
        except Exception:
            current = None
        return False, current, str(err)
    finally:
        try:
            driver.quit()
        except Exception:
            pass


def parse_args(argv: list[str]) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__,
                                     formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--task-id", help="Process only the task with this id (e.g. medium:my-slug).")
    parser.add_argument("--site", choices=sorted(DRIVERS),
                        help="Process only tasks for this site.")
    parser.add_argument("--dry-run", action="store_true", help="Print what would be processed.")
    parser.add_argument("--queue-file", default=str(QUEUE_FILE))
    parser.add_argument("--state-file", default=str(STATE_FILE))
    parser.add_argument("--profile-dir", default=str(PROFILE_DIR))
    parser.add_argument("--chrome-version", type=int, default=None,
                        help="Chrome major version (default: auto-detect from installed Chrome).")
    return parser.parse_args(argv)


def main(argv: list[str]) -> int:
    args = parse_args(argv)
    try:
        import undetected_chromedriver  # noqa: F401
    except ImportError:
        print("undetected-chromedriver not installed. Run: pip install undetected-chromedriver setuptools selenium", file=sys.stderr)
        return 2

    queue_path = Path(args.queue_file)
    state_path = Path(args.state_file)
    queue = _load_json(queue_path, {"tasks": []})
    state = _load_json(state_path, {"posts": {}})

    tasks: list[dict] = list(queue.get("tasks", []))
    if args.task_id:
        tasks = [t for t in tasks if t.get("id") == args.task_id]
    if args.site:
        tasks = [t for t in tasks if t.get("site") == args.site]

    pending = [t for t in tasks if not is_already_done(state, t)]
    if not pending:
        print("Nothing to do (queue empty or all tasks already in syndication-state.json).")
        return 0

    print(f"{len(pending)} task(s) to process:")
    for t in pending:
        print(f"  - {t['id']}")

    if args.dry_run:
        return 0

    chrome_version = args.chrome_version or detect_chrome_major_version()
    if chrome_version:
        print(f"Detected Chrome major version: {chrome_version}")
    else:
        print("WARNING: could not detect Chrome version; undetected-chromedriver will fetch the latest, which may mismatch your installed Chrome. Pass --chrome-version=NN to override.", file=sys.stderr)
    profile_dir = Path(args.profile_dir)

    failures = 0
    for task in pending:
        print(f"\n=== {task['id']} ===")

        def log(msg: str, _site=task["site"]) -> None:
            print(f"  [{_site}] {msg}")

        ok, url, err = process_task(task, log, profile_dir, chrome_version)
        if ok:
            print(f"  ✓ success: {url}")
            record_result(state, task, url)
            state_path.write_text(json.dumps(state, indent=2) + "\n", encoding="utf-8")
        else:
            print(f"  ✗ failed: {err}")
            failures += 1

    print(f"\nDone. {len(pending) - failures}/{len(pending)} succeeded.")
    return 0 if failures == 0 else 1


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
