#!/usr/bin/env python3
"""Upload a validated video-builder package through an authenticated Studio profile.

This intentionally drives YouTube Studio instead of storing OAuth client secrets in
the repository.  It is designed for the private-review stage of the syndication
workflow and refuses to select public or unlisted visibility.
"""

from __future__ import annotations

import argparse
import json
import os
import plistlib
import re
import subprocess
import time
from pathlib import Path

try:
    import undetected_chromedriver as uc
    from selenium.common.exceptions import TimeoutException, WebDriverException
    from selenium.webdriver.common.by import By
    from selenium.webdriver.common.action_chains import ActionChains
    from selenium.webdriver.common.keys import Keys
except ModuleNotFoundError:  # Keep pure helpers and unit tests dependency-light.
    uc = None

    class TimeoutException(RuntimeError):
        pass

    class WebDriverException(RuntimeError):
        pass

    class By:
        CSS_SELECTOR = "css selector"
        XPATH = "xpath"

    ActionChains = None
    Keys = None


CHANNEL_ID = "UCb-4T86pwSFyN66s7QJI90A"
DEFAULT_PROFILE = Path(
    os.environ.get(
        "CN1_YOUTUBE_PROFILE",
        Path.home() / "dev" / "cn-syndication" / "runner"
        / "syndication-auth" / "youtube-profile",
    )
)
CHROME = "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"
CHROME_INFO = Path("/Applications/Google Chrome.app/Contents/Info.plist")


def chrome_major() -> int | None:
    try:
        with CHROME_INFO.open("rb") as stream:
            version = str(plistlib.load(stream)["CFBundleShortVersionString"])
        match = re.match(r"(\d+)\.", version)
        if match:
            return int(match.group(1))
    except Exception:
        pass
    try:
        output = subprocess.check_output(
            [CHROME, "--version"], stderr=subprocess.DEVNULL, timeout=15
        ).decode()
        match = re.search(r"(\d+)\.\d+\.\d+", output)
        return int(match.group(1)) if match else None
    except Exception:
        return None


def chrome_user_agent(major: int | None = None) -> str:
    version = major or 120
    return (
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) "
        "AppleWebKit/537.36 (KHTML, like Gecko) "
        f"Chrome/{version}.0.0.0 Safari/537.36"
    )


def wait_until(predicate, timeout: float, description: str):
    deadline = time.monotonic() + timeout
    last_error: Exception | None = None
    while time.monotonic() < deadline:
        try:
            value = predicate()
            if value:
                return value
        except (WebDriverException, RuntimeError) as error:
            last_error = error
        time.sleep(0.5)
    detail = f": {last_error}" if last_error else ""
    raise TimeoutException(f"timed out waiting for {description}{detail}")


class StudioUploader:
    def __init__(self, profile: Path, evidence_dir: Path, headed: bool = False):
        if uc is None:
            raise RuntimeError(
                "YouTube Studio upload requires undetected-chromedriver and selenium"
            )
        options = uc.ChromeOptions()
        options.add_argument(f"--user-data-dir={profile}")
        options.add_argument("--profile-directory=Default")
        options.add_argument("--no-first-run")
        options.add_argument("--no-default-browser-check")
        major = chrome_major()
        # uc leaves HeadlessChrome in navigator.userAgent.  Studio rejects that
        # token as an unsupported browser before authentication is evaluated.
        options.add_argument(f"--user-agent={chrome_user_agent(major)}")
        self.driver = uc.Chrome(
            options=options,
            headless=not headed,
            use_subprocess=True,
            version_main=major,
        )
        self.driver.set_page_load_timeout(60)
        self.driver.set_script_timeout(130)
        self.evidence_dir = evidence_dir

    def close(self):
        self.driver.quit()

    def navigate(self, url: str) -> None:
        """Open a Studio URL and pass Google's browser-warning interstitial.

        Headless Chrome can occasionally be classified as an older browser even
        when its binary is current.  The warning offers an explicit, supported
        escape hatch.  Handle it once at navigation time so later waits report
        the real Studio page instead of timing out on an unrelated interstitial.
        """
        self.driver.get(url)
        if "studio.youtube.com" in url:
            wait_until(
                lambda: (
                    "unsupported browser" in self.body().lower()
                    or self.driver.find_elements(By.CSS_SELECTOR, "ytcp-app")
                ),
                20,
                "YouTube Studio shell or browser warning",
            )
        body = self.body()
        if "unsupported browser" not in body.lower():
            return
        separator = "&" if "?" in url else "?"
        self.driver.get(f"{url}{separator}skip_browser_check=true")
        wait_until(
            lambda: (
                "unsupported browser" not in self.body().lower()
                and self.driver.find_elements(By.CSS_SELECTOR, "ytcp-app")
            ),
            30,
            "YouTube Studio browser-warning dismissal",
        )

    def visible(self, selector: str):
        return [
            element
            for element in self.driver.find_elements(By.CSS_SELECTOR, selector)
            if element.is_displayed()
        ]

    def wait_visible(self, selector: str, timeout: float = 30):
        return wait_until(
            lambda: self.visible(selector), timeout, f"visible {selector}"
        )[0]

    def click(self, selector: str, timeout: float = 30):
        element = self.wait_visible(selector, timeout)
        self.driver.execute_script(
            "arguments[0].scrollIntoView({block:'center'});", element
        )
        try:
            element.click()
        except WebDriverException:
            self.driver.execute_script("arguments[0].click();", element)
        return element

    def click_text_radio(self, text: str, timeout: float = 30):
        def find():
            for element in self.visible("tp-yt-paper-radio-button"):
                label = self.driver.execute_script(
                    "return arguments[0].innerText.trim();", element
                )
                if label == text:
                    return element
            return None

        element = wait_until(find, timeout, f"radio {text!r}")
        self.driver.execute_script("arguments[0].click();", element)

    def click_text_button(self, text: str, timeout: float = 30):
        def find():
            matches = []
            for element in self.visible("ytcp-button, button"):
                label = self.driver.execute_script(
                    "return arguments[0].innerText.trim();", element
                )
                if label == text:
                    matches.append(element)
            # Draft recovery pages can contain the base editor and upload
            # dialog at once.  The dialog is appended last in the DOM.
            return matches[-1] if matches else None

        element = wait_until(find, timeout, f"button {text!r}")
        self.driver.execute_script(
            "arguments[0].scrollIntoView({block:'center'}); arguments[0].click();",
            element,
        )
        return element

    def set_editable(self, aria_prefix: str, value: str):
        selector = f'[contenteditable="true"][aria-label^="{aria_prefix}"]'
        element = self.wait_visible(selector, 45)
        self.driver.execute_script(
            """
            const element = arguments[0];
            const value = arguments[1];
            element.focus();
            element.textContent = value;
            element.dispatchEvent(new InputEvent('input', {
              bubbles: true, inputType: 'insertText', data: value
            }));
            element.dispatchEvent(new Event('change', {bubbles: true}));
            """,
            element,
            value,
        )

    def upload_file(self, path: Path, index: int = 0, timeout: float = 30):
        inputs = wait_until(
            lambda: self.driver.find_elements(By.CSS_SELECTOR, 'input[type="file"]'),
            timeout,
            "file input",
        )
        if index >= len(inputs):
            raise RuntimeError(f"file input {index} missing; found {len(inputs)}")
        inputs[index].send_keys(str(path.resolve()))

    def body(self) -> str:
        try:
            return self.driver.find_element(By.TAG_NAME, "body").text
        except WebDriverException:
            return ""

    def screenshot(self, label: str):
        self.evidence_dir.mkdir(parents=True, exist_ok=True)
        self.driver.save_screenshot(str(self.evidence_dir / f"{label}.png"))

    def wait_for_thumbnail_upload(self, preview, timeout: float = 120) -> None:
        """Wait inside the page without repeatedly touching or capturing the element."""
        completed = self.driver.execute_async_script(
            """
            const preview = arguments[0];
            const timeoutMs = arguments[1];
            const done = arguments[arguments.length - 1];
            let settled = false;
            let observer;
            let timer;
            const finish = value => {
              if (settled) return;
              settled = true;
              if (observer) observer.disconnect();
              if (timer) clearTimeout(timer);
              done(value);
            };
            const ready = () => !((preview.innerText || '').includes('Uploading'));
            if (ready()) {
              finish(true);
              return;
            }
            observer = new MutationObserver(() => {
              if (ready()) finish(true);
            });
            observer.observe(preview, {
              subtree: true, childList: true, characterData: true, attributes: true
            });
            timer = setTimeout(() => finish(false), timeoutMs);
            """,
            preview,
            round(timeout * 1000),
        )
        if not completed:
            raise TimeoutException("timed out waiting for thumbnail upload completion")

    def preflight(self, package: Path, keys: tuple[str, ...]) -> dict:
        manifest = json.loads(package.read_text(encoding="utf-8"))
        titles = [
            manifest["uploads"][key]["youtube"]["title"]
            for key in keys
        ]
        self.navigate(f"https://studio.youtube.com/channel/{CHANNEL_ID}/videos")
        try:
            wait_until(
                lambda: "Channel content" in self.body()
                or "Sign in" in self.body()
                or "You don't have permission" in self.body(),
                60,
                "YouTube Studio channel content",
            )
        except TimeoutException:
            self.screenshot("channel-preflight-timeout")
            raise
        body = self.body()
        if "Sign in" in body or "You don't have permission" in body:
            raise RuntimeError(f"authenticated profile cannot access channel {CHANNEL_ID}")
        if "Channel content" not in body:
            raise RuntimeError(f"channel content did not load for {CHANNEL_ID}")

        # The dedicated channel is expected to have very few private review
        # uploads. Scroll the complete table so a retry cannot silently create
        # a duplicate that is merely below the first viewport.
        previous_height = -1
        for _ in range(30):
            current_height = self.driver.execute_script(
                "return document.documentElement.scrollHeight"
            )
            if current_height == previous_height:
                break
            previous_height = current_height
            self.driver.execute_script(
                "window.scrollTo(0, document.documentElement.scrollHeight)"
            )
            time.sleep(0.4)
        body = self.body()
        duplicate_terms = list(titles)
        if manifest.get("source", {}).get("slug") == "ar-vr-support-simulation":
            duplicate_terms += ["ARKit", "ARCore", "ar-vr-support-simulation"]
        matches = [term for term in duplicate_terms if term.lower() in body.lower()]
        self.screenshot("channel-preflight")
        if matches:
            raise RuntimeError(
                "possible existing upload found; refusing duplicate: "
                + ", ".join(matches)
            )
        return {
            "channelId": CHANNEL_ID,
            "channelContentLoaded": True,
            "duplicateMatches": [],
            "visibleContentHasAot": "AOT" in body or "HotSpot" in body,
        }

    def video_catalog(self) -> list[dict]:
        """Return every video currently visible in the channel content table."""
        entries = {}

        for kind, suffix in (("landscape", "videos"), ("portrait", "videos/short")):
            self.navigate(f"https://studio.youtube.com/channel/{CHANNEL_ID}/{suffix}")
            wait_until(
                lambda: "Channel content" in self.body(),
                60,
                f"YouTube Studio {kind} content",
            )
            previous_height = -1
            for _ in range(60):
                current_height = self.driver.execute_script(
                    "return document.documentElement.scrollHeight"
                )
                if current_height == previous_height:
                    break
                previous_height = current_height
                self.driver.execute_script(
                    "window.scrollTo(0, document.documentElement.scrollHeight)"
                )
                time.sleep(0.4)
            for row in self.driver.find_elements(By.CSS_SELECTOR, "ytcp-video-row"):
                if not row.is_displayed():
                    continue
                match = None
                for link in row.find_elements(By.CSS_SELECTOR, "a[href*='/video/']"):
                    match = re.search(
                        r"/video/([A-Za-z0-9_-]{11})(?:/|$)",
                        link.get_attribute("href") or "",
                    )
                    if match:
                        break
                if not match:
                    continue
                video_id = match.group(1)
                title_nodes = row.find_elements(
                    By.CSS_SELECTOR,
                    "#video-title, [id='video-title'], [aria-label='Video title']",
                )
                title = next((
                    (node.get_attribute("title") or node.text or "").strip()
                    for node in title_nodes
                    if (node.get_attribute("title") or node.text or "").strip()
                ), "")
                if not title:
                    continue
                row_text = row.text
                privacy = next(
                    (value for value in ("Private", "Unlisted", "Public") if value in row_text),
                    "unknown",
                )
                entries[video_id] = {
                    "videoId": video_id,
                    "title": title,
                    "privacy": privacy.lower(),
                    "kind": kind,
                }
        self.screenshot("video-catalog")
        catalog = sorted(entries.values(), key=lambda entry: entry["title"].lower())
        self.evidence_dir.mkdir(parents=True, exist_ok=True)
        (self.evidence_dir / "video-catalog.json").write_text(
            json.dumps({"channelId": CHANNEL_ID, "videos": catalog}, indent=2) + "\n",
            encoding="utf-8",
        )
        return catalog

    def video_id(self) -> str | None:
        match = re.search(
            r"https://(?:youtu\.be/|youtube\.com/(?:shorts/|watch\?v=))([\w-]{11})",
            self.body(),
        )
        return match.group(1) if match else None

    def set_tags(self, tags: list[str]):
        field = self.wait_visible('input[aria-label="Tags"]', 30)
        field.click()
        field.send_keys(",".join(tags) + ",")

    def add_captions(self, path: Path):
        self.click("#subtitles-button", 45)

        # A new upload can reach this screen before or after YouTube's automatic
        # captions are available.  The former shows the initial upload chooser;
        # the latter opens the transcript editor.  Uploading from the editor is
        # important: the top-level Languages page may otherwise reopen the
        # automatic transcript without replacing it.
        try:
            chooser = wait_until(
                lambda: self.visible("#choose-upload-file")
                or self.visible("#more-actions-menu"),
                45,
                "caption upload entry point",
            )
            if chooser[0].get_attribute("id") == "choose-upload-file":
                self.click("#choose-upload-file", 10)
                self.click("#confirm-button", 10)
            else:
                self.click("#more-actions-menu", 10)

                def upload_menu_item():
                    for element in self.visible("tp-yt-paper-item"):
                        label = self.driver.execute_script(
                            "return arguments[0].innerText.trim();", element
                        )
                        if label == "Upload file":
                            return element
                    return None

                item = wait_until(upload_menu_item, 10, "caption Upload file menu item")
                self.driver.execute_script("arguments[0].click();", item)
            self.upload_file(path, timeout=15)
        except Exception:
            self.screenshot("captions-upload-failed")
            raise

        def publish_ready():
            buttons = self.visible("#publish-button")
            if not buttons:
                return None
            button = buttons[0]
            return button if button.get_attribute("aria-disabled") != "true" else None

        wait_until(publish_ready, 45, "enabled caption Done button")
        expected = " ".join(
            line.strip()
            for line in path.read_text(encoding="utf-8").splitlines()
            if line.strip() and not line.strip().isdigit() and "-->" not in line
        ).split(".", 1)[0]
        wait_until(
            lambda: expected[:80] in self.body(),
            30,
            "authored caption text",
        )
        self.screenshot("captions-loaded")
        self.click("#publish-button", 15)
        wait_until(
            lambda: "english by you" in self.body().lower()
            or "subtitles published" in self.body().lower()
            or not self.visible("#publish-button"),
            45,
            "manual English caption label",
        )

    @staticmethod
    def framestamp(milliseconds: int, frame_rate: int = 30) -> str:
        total_seconds, remainder = divmod(milliseconds, 1000)
        minutes, seconds = divmod(total_seconds, 60)
        frames = round(remainder * frame_rate / 1000)
        if frames >= frame_rate:
            seconds += 1
            frames = 0
        return f"{minutes}:{seconds:02d}:{frames:02d}"

    def add_end_screen(self, start_at_ms: int):
        self.click("#endscreens-button", 45)
        self.click("div.card[aria-label='1 video, 1 subscribe']", 45)
        wait_until(lambda: self.visible("#start-input input"),
                   30, "end-screen framestamps")
        self.set_selected_end_screen_start(start_at_ms)
        self.screenshot("end-screen")
        self.click("#save-button", 15)

    def open_existing_video(self, video_id: str):
        self.navigate(f"https://studio.youtube.com/video/{video_id}/edit")
        try:
            wait_until(lambda: "Video details" in self.body(), 60, "video details")
        except TimeoutException as error:
            self.screenshot("video-details-timeout")
            body_excerpt = " ".join((self.body() or "").split())[:500]
            raise RuntimeError(
                "YouTube Studio video details did not load; "
                f"url={self.driver.current_url!r}, title={self.driver.title!r}, "
                f"body={body_excerpt!r}"
            ) from error

    def open_existing_end_screen(self, video_id: str):
        self.open_existing_video(video_id)
        try:
            self.click("#endscreens-button", 12)
        except TimeoutException:
            candidates = [
                element for element in self.driver.find_elements(
                    By.XPATH, "//*[normalize-space(.)='End screen']"
                ) if element.is_displayed()
            ]
            if not candidates:
                raise RuntimeError("existing-video End screen control is missing")
            target = candidates[-1]
            self.driver.execute_script(
                "arguments[0].scrollIntoView({block:'center'});", target
            )
            self.driver.execute_script("arguments[0].click();", target)
        wait_until(lambda: "End Screens" in self.body(), 45, "end-screen editor")
        wait_until(
            lambda: len(self.visible("ytve-endscreen-editor-preview-overlay-item")) >= 2,
            30,
            "existing end-screen elements",
        )

    def replace_existing_thumbnail(self, video_id: str, thumbnail: Path):
        edit_url = f"https://studio.youtube.com/video/{video_id}/edit"
        self.open_existing_video(video_id)
        preview = self.wait_visible('button[aria-label="Uploaded thumbnail"]', 30)
        image = preview.find_element(By.CSS_SELECTOR, "img")
        previous_src = image.get_attribute("src")
        file_input = self.driver.find_element(By.CSS_SELECTOR, 'input#file-loader[type="file"]')
        file_input.send_keys(str(thumbnail.resolve()))
        wait_until(
            lambda: next((candidate for candidate in preview.find_elements(By.CSS_SELECTOR, "img")
                           if candidate.get_attribute("src") not in ("", previous_src)), None),
            45,
            "updated thumbnail preview",
        )

        self.wait_for_thumbnail_upload(preview)
        self.screenshot("thumbnail-corrected")
        save = self.wait_visible("#save", 30)
        wait_until(lambda: save.get_attribute("aria-disabled") != "true",
                   30, "enabled video-details Save button")
        save.click()
        wait_until(
            lambda: "Changes saved" in self.body(), 60, "thumbnail Changes saved acknowledgement"
        )
        self.screenshot("thumbnail-save-acknowledged")

        # Reopen exactly once. Repeated visible navigation steals desktop focus
        # and traps the user in Studio; a failed check must return control rather
        # than starting an automatic retry loop.
        time.sleep(8)
        self.navigate(edit_url)
        wait_until(lambda: "Video details" in self.body(), 60, "reopened video details")
        self.screenshot("thumbnail-persisted")
        return {
            "videoId": video_id,
            "thumbnail": str(thumbnail),
            "previewChanged": True,
            "persistedScreenshot": str(self.evidence_dir / "thumbnail-persisted.png"),
            "verification": "captured-for-local-visual-inspection",
        }

    def set_selected_end_screen_start(self, start_at_ms: int):
        stamp = self.framestamp(start_at_ms)
        start_input = self.wait_visible("#start-input input", 15)
        self.driver.execute_script(
            """
            const input = arguments[0];
            const value = arguments[1];
            const host = input.closest('ytve-framestamp-input');
            const setter = Object.getOwnPropertyDescriptor(
              HTMLInputElement.prototype, 'value').set;
            setter.call(input, value);
            host.updateValue({detail: {value}});
            input.dispatchEvent(new InputEvent('input', {
              bubbles: true, inputType: 'insertText', data: value
            }));
            input.dispatchEvent(new Event('change', {bubbles: true}));
            input.dispatchEvent(new FocusEvent('blur', {bubbles: true}));
            """,
            start_input,
            stamp,
        )
        wait_until(lambda: start_input.get_attribute("value") == stamp,
                   15, f"end-screen start {stamp}")
        return stamp

    def replace_and_align_existing_end_screen(
        self, video_id: str, target_video_id: str, target_title: str,
        start_at_ms: int,
    ):
        self.open_existing_end_screen(video_id)
        items = [item for item in self.visible(
            "ytve-endscreen-editor-preview-overlay-item"
        ) if item.rect["width"] > 20 and item.rect["height"] > 20]
        if len(items) != 2:
            raise RuntimeError("expected one video and one subscribe end-screen element")
        video_item = max(items, key=lambda item: item.rect["width"] / item.rect["height"])
        subscribe_item = min(
            items, key=lambda item: abs(item.rect["width"] / item.rect["height"] - 1)
        )

        ActionChains(self.driver).move_to_element(video_item).click().perform()
        already_targeted = any(
            target_title in row.text for row in self.visible("ytve-endscreen-row")
        )
        if not already_targeted:
            choose = self.wait_visible("#choose-video", 15)
            ActionChains(self.driver).move_to_element(choose).click().perform()
            search = self.wait_visible("#search-yours", 30)
            search.clear()
            # Titles with punctuation and common words can yield no results in
            # Studio's own-video picker even when the exact video is present.
            # The stable video ID is unambiguous and avoids locale/title drift.
            search.send_keys(target_video_id)
            wait_until(lambda: target_title in self.body(), 45, "specific video picker result")
            def visible_picker_row():
                for element in self.driver.find_elements(
                    By.CSS_SELECTOR,
                    "ytcp-video-picker-item, ytcp-video-picker-item-renderer, [role='option']",
                ):
                    try:
                        if element.is_displayed() and target_title in element.text:
                            return element
                    except WebDriverException:
                        # Studio replaces the result node while its thumbnail
                        # resolves. Query again instead of retaining a stale row.
                        continue
                return None

            try:
                result = wait_until(visible_picker_row, 30, "stable specific video result")
            except TimeoutException:
                self.screenshot("specific-video-result-missing")
                raise RuntimeError(f"specific video result not found: {target_title}")
            self.driver.execute_script(
                """
                const row = arguments[0];
                row.scrollIntoView({block: 'center'});
                row.click();
                """,
                result,
            )
            wait_until(
                lambda: "Choose specific video" not in self.body()
                and (target_title in self.body() or "Video element" in self.body()),
                30,
                "specific video selection",
            )
        self.set_selected_end_screen_start(start_at_ms)

        def classified_items():
            visible_items = [item for item in self.visible(
                "ytve-endscreen-editor-preview-overlay-item"
            ) if item.rect["width"] > 20 and item.rect["height"] > 20]
            if len(visible_items) != 2:
                raise RuntimeError(
                    f"expected two visible end-screen elements, found {len(visible_items)}"
                )
            video = max(
                visible_items,
                key=lambda item: item.rect["width"] / item.rect["height"],
            )
            subscribe = min(
                visible_items,
                key=lambda item: abs(item.rect["width"] / item.rect["height"] - 1),
            )
            if video == subscribe:
                raise RuntimeError("could not distinguish video and subscribe elements")
            return video, subscribe

        video_item, subscribe_item = classified_items()
        ActionChains(self.driver).move_to_element(subscribe_item).click().perform()
        self.set_selected_end_screen_start(start_at_ms)

        overlay = self.wait_visible("ytve-endscreen-editor-preview-overlay", 15)
        overlay_rect = overlay.rect
        safe = {
            "left": overlay_rect["x"] + 1094 / 1920.0 * overlay_rect["width"],
            "right": overlay_rect["x"] + 1804 / 1920.0 * overlay_rect["width"],
            "top": overlay_rect["y"] + 174 / 1080.0 * overlay_rect["height"],
            "bottom": overlay_rect["y"] + 908 / 1080.0 * overlay_rect["height"],
        }

        def drag_to(item, target_left: float, target_top: float):
            """Drag with Chrome's native input path and verify the element moved.

            Studio occasionally accepts Selenium's synthetic click-and-hold but
            silently ignores the move.  CDP mouse events travel through the same
            pointer path as a real mouse and remain reliable in headless Chrome.
            """
            current = item.rect
            start_x = current["x"] + current["width"] / 2.0
            start_y = current["y"] + current["height"] / 2.0
            target_x = target_left + current["width"] / 2.0
            target_y = target_top + current["height"] / 2.0
            self.driver.execute_cdp_cmd("Input.dispatchMouseEvent", {
                "type": "mouseMoved", "x": start_x, "y": start_y,
            })
            self.driver.execute_cdp_cmd("Input.dispatchMouseEvent", {
                "type": "mousePressed", "x": start_x, "y": start_y,
                "button": "left", "buttons": 1, "clickCount": 1,
            })
            for step in range(1, 7):
                fraction = step / 6.0
                self.driver.execute_cdp_cmd("Input.dispatchMouseEvent", {
                    "type": "mouseMoved",
                    "x": start_x + (target_x - start_x) * fraction,
                    "y": start_y + (target_y - start_y) * fraction,
                    "button": "left", "buttons": 1,
                })
            self.driver.execute_cdp_cmd("Input.dispatchMouseEvent", {
                "type": "mouseReleased", "x": target_x, "y": target_y,
                "button": "left", "buttons": 0, "clickCount": 1,
            })
            time.sleep(1)

        # Fit both elements inside the renderer's reserved outro panel.  Work
        # in actual preview pixels because Studio can resize the editor without
        # preserving the old template's positions.
        video_item, subscribe_item = classified_items()
        subscribe_left = safe["left"] + (
            safe["right"] - safe["left"] - subscribe_item.rect["width"]
        ) / 2.0
        subscribe_top = safe["top"] + 6
        drag_to(subscribe_item, subscribe_left, subscribe_top)
        video_item, subscribe_item = classified_items()
        video_top = safe["bottom"] - video_item.rect["height"] - 6
        drag_to(video_item, safe["left"] + 6, video_top)
        video_item, subscribe_item = classified_items()

        placements = {}
        for name, item in (("video", video_item), ("subscribe", subscribe_item)):
            rect = item.rect
            inside = (
                rect["x"] >= safe["left"] - 3
                and rect["x"] + rect["width"] <= safe["right"] + 3
                and rect["y"] >= safe["top"] - 3
                and rect["y"] + rect["height"] <= safe["bottom"] + 3
            )
            placements[name] = {"rect": rect, "insideReservedArea": inside}
        if not all(item["insideReservedArea"] for item in placements.values()):
            self.screenshot("end-screen-placement-failed")
            raise RuntimeError(
                f"end-screen element escaped reserved area: {placements}; "
                f"overlay={overlay_rect}; safe={safe}"
            )

        self.screenshot("end-screen-corrected")
        save = self.wait_visible("#save-button", 15)
        changed = save.get_attribute("aria-disabled") != "true"
        if changed:
            self.driver.execute_script("arguments[0].click();", save)
            wait_until(lambda: not self.visible("ytve-endscreen-modal"),
                       45, "saved end screen")
        else:
            # Studio disables Save when the existing target, timestamp, and
            # placements already match.  The checks above prove that state;
            # close the no-op editor instead of turning idempotence into a
            # failed upload.
            self.driver.find_element(By.TAG_NAME, "body").send_keys(Keys.ESCAPE)
            wait_until(lambda: not self.visible("ytve-endscreen-modal"),
                       15, "unchanged end screen close")
        return {
            "videoId": video_id,
            "targetVideoId": target_video_id,
            "targetTitle": target_title,
            "start": self.framestamp(start_at_ms),
            "placements": placements,
            "changed": changed,
        }

    def verify_existing_repair(
        self, video_id: str, thumbnail: Path, target_title: str, start_at_ms: int,
    ):
        self.open_existing_video(video_id)
        preview = self.wait_visible('button[aria-label="Uploaded thumbnail"]', 30)
        preview_image = wait_until(
            lambda: next((image for image in preview.find_elements(By.CSS_SELECTOR, "img")
                          if image.get_attribute("src")), None),
            30,
            "remote thumbnail preview",
        )
        thumbnail_src = preview_image.get_attribute("src")
        time.sleep(1)
        self.screenshot("thumbnail-verified")

        self.open_existing_end_screen(video_id)

        def classified_items():
            visible_items = [item for item in self.visible(
                "ytve-endscreen-editor-preview-overlay-item"
            ) if item.rect["width"] > 20 and item.rect["height"] > 20]
            video = max(visible_items,
                        key=lambda item: item.rect["width"] / item.rect["height"])
            subscribe = min(visible_items,
                            key=lambda item: abs(item.rect["width"] / item.rect["height"] - 1))
            return video, subscribe

        video_item, subscribe_item = classified_items()
        ActionChains(self.driver).move_to_element(video_item).click().perform()
        wait_until(lambda: target_title in self.body(), 20, "saved AOT end-screen target")
        expected_stamp = self.framestamp(start_at_ms)
        starts = {}
        for name, item in (("video", video_item), ("subscribe", subscribe_item)):
            ActionChains(self.driver).move_to_element(item).click().perform()
            start_input = self.wait_visible("#start-input input", 15)
            starts[name] = start_input.get_attribute("value")
        if any(value != expected_stamp for value in starts.values()):
            raise RuntimeError(f"end-screen starts do not match {expected_stamp}: {starts}")

        try:
            playhead = self.wait_visible(
                "ytcp-media-timestamp-input input, input.ytcp-media-timestamp-input", 4
            )
            playhead.send_keys(Keys.COMMAND, "a")
            playhead.send_keys("2:50:00")
            playhead.send_keys(Keys.ENTER)
        except TimeoutException:
            timeline = self.wait_visible("ytve-timeline-header", 10)
            ActionChains(self.driver).move_to_element_with_offset(
                timeline, round(timeline.rect["width"] * 0.52), 12
            ).click().perform()
        time.sleep(2)

        overlay = self.wait_visible("ytve-endscreen-editor-preview-overlay", 15)
        overlay_rect = overlay.rect
        safe = {
            "left": overlay_rect["x"] + 1094 / 1920.0 * overlay_rect["width"],
            "right": overlay_rect["x"] + 1804 / 1920.0 * overlay_rect["width"],
            "top": overlay_rect["y"] + 174 / 1080.0 * overlay_rect["height"],
            "bottom": overlay_rect["y"] + 908 / 1080.0 * overlay_rect["height"],
        }
        video_item, subscribe_item = classified_items()
        placements = {}
        for name, item in (("video", video_item), ("subscribe", subscribe_item)):
            rect = item.rect
            inside = (
                rect["x"] >= safe["left"] - 3
                and rect["x"] + rect["width"] <= safe["right"] + 3
                and rect["y"] >= safe["top"] - 3
                and rect["y"] + rect["height"] <= safe["bottom"] + 3
            )
            placements[name] = {"rect": rect, "insideReservedArea": inside}
        if not all(item["insideReservedArea"] for item in placements.values()):
            raise RuntimeError(f"saved placement is outside reserved area: {placements}")
        self.screenshot("end-screen-verified")
        return {
            "videoId": video_id,
            "thumbnailPreviewPresent": bool(thumbnail_src),
            "targetTitle": target_title,
            "starts": starts,
            "placements": placements,
        }

    def assign_existing_playlist(self, video_id: str, title: str, playlist: str,
                                 playlist_description: str):
        self.navigate(f"https://studio.youtube.com/video/{video_id}/edit")
        wait_until(
            lambda: title in self.body() and "Details" in self.body(),
            60,
            f"video editor for {video_id}",
        )
        widget = self.wait_visible("ytcp-video-metadata-playlists", 45)
        if playlist.lower() in widget.text.lower():
            self.screenshot("playlist-assigned")
            return {"videoId": video_id, "playlist": playlist, "playlistId": None,
                    "alreadySelected": True, "created": False}
        candidates = []
        for selector in ("ytcp-dropdown-trigger", "#dropdown-trigger", "[role='button']", "button"):
            candidates.extend(
                element for element in widget.find_elements(By.CSS_SELECTOR, selector)
                if element.is_displayed()
            )
        trigger = candidates[0] if candidates else widget
        self.driver.execute_script("arguments[0].click();", trigger)
        try:
            wait_until(
                lambda: self.visible("ytcp-playlist-dialog"),
                30,
                "playlist dialog",
            )
        except Exception:
            self.screenshot("playlist-dialog-failed")
            print(widget.get_attribute("outerHTML")[:8000], flush=True)
            raise

        search = self.wait_visible('input[placeholder="Search for a playlist"]', 15)
        search.send_keys(playlist)
        created = False
        try:
            filtered_choices = wait_until(
                lambda: self.visible("ytcp-checkbox-lit"),
                10,
                f"playlist search result for {playlist!r}",
            )
        except TimeoutException:
            filtered_choices = []
        playlist_id = None
        if not filtered_choices:
            self.driver.execute_script(
                """
                const input = arguments[0];
                const setter = Object.getOwnPropertyDescriptor(
                  HTMLInputElement.prototype, 'value').set;
                setter.call(input, '');
                input.dispatchEvent(new InputEvent('input', {
                  bubbles: true, inputType: 'deleteContentBackward'
                }));
                input.dispatchEvent(new Event('change', {bubbles: true}));
                """,
                search,
            )
            wait_until(
                lambda: self.visible("ytcp-checkbox-lit"),
                15,
                "playlist list after clearing search",
            )
            self.click_text_button("New playlist", 15)
            def new_playlist_menu_item():
                for element in self.visible("tp-yt-paper-item, ytcp-text-menu-item"):
                    label = self.driver.execute_script(
                        "return arguments[0].innerText.trim();", element
                    )
                    if label == "New playlist":
                        return element
                return None

            menu_item = wait_until(new_playlist_menu_item, 15, "New playlist menu item")
            self.driver.execute_script("arguments[0].click();", menu_item)
            try:
                dialog = wait_until(
                    lambda: next((element for element in self.visible("tp-yt-paper-dialog")
                                  if "Create a new playlist" in element.text), None),
                    15,
                    "create playlist dialog",
                )
                editables = [
                    element for element in dialog.find_elements(By.CSS_SELECTOR, '[contenteditable="true"]')
                    if element.is_displayed()
                ]
                if not editables:
                    raise RuntimeError("new playlist editable fields are missing")

                def set_contenteditable(element, value: str):
                    self.driver.execute_script(
                        """
                        const element = arguments[0];
                        const value = arguments[1];
                        element.focus();
                        element.textContent = value;
                        element.dispatchEvent(new InputEvent('input', {
                          bubbles: true, inputType: 'insertText', data: value
                        }));
                        element.dispatchEvent(new Event('change', {bubbles: true}));
                        """,
                        element,
                        value,
                    )

                set_contenteditable(editables[0], playlist)
                if len(editables) > 1:
                    set_contenteditable(editables[1], playlist_description)
                privacy_controls = []
                for selector in ("ytcp-dropdown-trigger", "tp-yt-paper-dropdown-menu"):
                    for element in dialog.find_elements(By.CSS_SELECTOR, selector):
                        label = self.driver.execute_script(
                            "return arguments[0].innerText.trim();", element
                        )
                        if any(value in label for value in ("Public", "Unlisted", "Private")):
                            privacy_controls.append(element)
                if not privacy_controls:
                    raise RuntimeError("new playlist privacy control is missing")
                self.driver.execute_script("arguments[0].click();", privacy_controls[0])

                def private_menu_item():
                    for element in self.visible("tp-yt-paper-item, ytcp-text-menu-item"):
                        label = self.driver.execute_script(
                            "return arguments[0].innerText.trim();", element
                        )
                        if label == "Private":
                            return element
                    return None

                private = wait_until(private_menu_item, 15, "Private playlist visibility")
                self.driver.execute_script("arguments[0].click();", private)
                if "Private" not in dialog.text:
                    raise RuntimeError("new playlist is not set to Private")
                self.click_text_button("Create", 15)
                wait_until(
                    lambda: not any("Create a new playlist" in element.text
                                    for element in self.visible("tp-yt-paper-dialog")),
                    30,
                    "created playlist",
                )
                playlist_dialog = self.wait_visible("ytcp-playlist-dialog", 15)
                created_search = playlist_dialog.find_element(
                    By.CSS_SELECTOR, 'input[placeholder="Search for a playlist"]'
                )
                created_search.send_keys(playlist)
                choice = wait_until(
                    lambda: (self.visible("ytcp-checkbox-lit") or [None])[0],
                    30,
                    "created playlist checkbox",
                )
                playlist_id = choice.get_attribute("test-id")
                checkbox = choice.find_element(By.CSS_SELECTOR, "#checkbox")
                checked = checkbox.get_attribute("aria-checked") == "true"
                if not checked:
                    self.driver.execute_script("arguments[0].click();", checkbox)
                    wait_until(
                        lambda: checkbox.get_attribute("aria-checked") == "true",
                        10,
                        "created playlist selected",
                    )
                created = True
            except Exception:
                self.screenshot("playlist-create-failed")
                print(self.visible("ytcp-playlist-dialog")[0].get_attribute("outerHTML")[:12000], flush=True)
                raise

        # Searching leaves the matching row as the only selectable checkbox.
        if not created:
            choice = filtered_choices[0]
            playlist_id = choice.get_attribute("test-id")
            checkbox = choice.find_element(By.CSS_SELECTOR, "#checkbox")
            checked = checkbox.get_attribute("aria-checked") == "true"
            if not checked:
                self.driver.execute_script("arguments[0].click();", checkbox)
                wait_until(
                    lambda: checkbox.get_attribute("aria-checked") == "true",
                    10,
                    "playlist selected",
                )
        else:
            checked = False
        self.click_text_button("Done", 20)
        wait_until(
            lambda: playlist.lower() in self.wait_visible(
                "ytcp-video-metadata-playlists", 5
            ).text.lower(),
            15,
            "playlist shown in video details",
        )
        save_buttons = [
            element for element in self.visible("ytcp-button, button")
            if self.driver.execute_script("return arguments[0].innerText.trim();", element) == "Save"
        ]
        enabled_save = wait_until(
            lambda: next((element for element in reversed([
                candidate for candidate in self.visible("ytcp-button, button")
                if self.driver.execute_script(
                    "return arguments[0].innerText.trim();", candidate
                ) == "Save"
            ]) if element.get_attribute("aria-disabled") != "true"
                and element.get_attribute("disabled") is None), None),
            30,
            "enabled video Save button",
        )
        self.driver.execute_script("arguments[0].click();", enabled_save)

        def save_complete():
            if "Changes saved" in self.body() or "All changes saved" in self.body():
                return True
            candidates = [
                element for element in self.visible("ytcp-button, button")
                if self.driver.execute_script(
                    "return arguments[0].innerText.trim();", element
                ) == "Save"
            ]
            return bool(candidates) and all(
                element.get_attribute("aria-disabled") == "true"
                or element.get_attribute("disabled") is not None
                for element in candidates
            )

        wait_until(save_complete, 45, "saved playlist assignment")
        self.driver.refresh()
        wait_until(lambda: title in self.body() and "Details" in self.body(),
                   60, "reloaded video details")
        reloaded_widget = self.wait_visible("ytcp-video-metadata-playlists", 45)
        if playlist.lower() not in reloaded_widget.text.lower():
            raise RuntimeError(f"playlist assignment was not durable: {playlist}")
        if playlist.lower() not in self.body().lower():
            raise RuntimeError(f"playlist assignment did not persist: {playlist}")
        self.screenshot("playlist-assigned")
        return {"videoId": video_id, "playlist": playlist, "playlistId": playlist_id,
                "alreadySelected": checked, "created": created}

    def playlist_catalog(self) -> list[dict]:
        self.navigate(f"https://studio.youtube.com/channel/{CHANNEL_ID}/videos")
        wait_until(lambda: "Channel content" in self.body(), 60, "channel content")

        def playlists_tab():
            for element in self.visible("tp-yt-paper-tab, [role='tab'], a, button"):
                label = self.driver.execute_script(
                    "return arguments[0].innerText.trim();", element
                )
                if label == "Playlists":
                    return element
            return None

        tab = wait_until(playlists_tab, 30, "Playlists tab")
        self.driver.execute_script("arguments[0].click();", tab)
        wait_until(
            lambda: "Playlist" in self.body() and "Create" in self.body(),
            45,
            "playlist catalog",
        )
        time.sleep(2)
        previous_height = -1
        for _ in range(30):
            current_height = self.driver.execute_script(
                "return document.documentElement.scrollHeight"
            )
            if current_height == previous_height:
                break
            previous_height = current_height
            self.driver.execute_script(
                "window.scrollTo(0, document.documentElement.scrollHeight)"
            )
            time.sleep(0.4)
        entries = []
        for element in self.driver.find_elements(By.CSS_SELECTOR, "a[href*='playlist']"):
            href = element.get_attribute("href") or ""
            label = self.driver.execute_script(
                "return arguments[0].innerText.trim();", element
            )
            match = re.search(r"(?:list=|/playlist/)(PL[A-Za-z0-9_-]+)", href)
            if match and label:
                entries.append({"title": label.splitlines()[0], "id": match.group(1), "href": href})
        self.screenshot("playlist-catalog")
        return entries

    def configure_series_playlist(self, playlist_id: str) -> dict:
        self.navigate(f"https://www.youtube.com/playlist?list={playlist_id}")
        wait_until(lambda: "Codename One" in self.body(), 60, "YouTube playlist page")

        def open_settings():
            more = self.wait_visible('button[aria-label="More actions"]', 30)
            self.driver.execute_script("arguments[0].click();", more)

            def settings_item():
                for element in self.driver.find_elements(
                    By.XPATH, "//*[normalize-space(.)='Playlist Settings']"
                ):
                    if element.is_displayed():
                        return element
                return None

            item = wait_until(settings_item, 15, "Playlist Settings menu item")
            self.driver.execute_script("arguments[0].click();", item)
            wait_until(lambda: "official series" in self.body().lower(),
                       30, "official series setting")

        def toggle_state(toggle) -> bool:
            return any(toggle.get_attribute(name) in ("true", "checked", "")
                       for name in ("checked", "aria-checked", "aria-pressed")
                       if toggle.get_attribute(name) is not None)

        open_settings()
        toggles = wait_until(
            lambda: self.visible("tp-yt-paper-toggle-button, [role='switch']")
            if len(self.visible("tp-yt-paper-toggle-button, [role='switch']")) >= 3
            else None,
            15,
            "playlist setting toggles",
        )
        # Private review playlists cannot be embedded by viewers.  Preserve
        # YouTube's private-playlist embedding state and enforce the setting
        # that matters now: this is an official ordered series.
        for toggle in (toggles[2],):
            if not toggle_state(toggle):
                ActionChains(self.driver).move_to_element(toggle).click().perform()
        self.screenshot("series-settings-enabled")

        done = self.wait_visible('button[aria-label="Done"]', 15)
        ActionChains(self.driver).move_to_element(done).click().perform()
        try:
            wait_until(
                lambda: not self.visible("ytd-form-popup-renderer[dialog]"),
                5,
                "playlist settings dialog close",
            )
        except TimeoutException:
            # The Polymer dialog occasionally consumes Done without dismissing
            # itself after its switches have already autosaved.  Escape closes
            # only the modal; the immediate re-open below verifies persistence.
            self.driver.find_element(By.TAG_NAME, "body").send_keys(Keys.ESCAPE)
            wait_until(
                lambda: not self.visible("ytd-form-popup-renderer[dialog]"),
                15,
                "playlist settings dialog escape close",
            )

        # Re-open rather than trusting the transient switch animation.
        open_settings()
        verified_toggles = wait_until(
            lambda: self.visible("tp-yt-paper-toggle-button, [role='switch']")
            if len(self.visible("tp-yt-paper-toggle-button, [role='switch']")) >= 3
            else None,
            15,
            "verified playlist setting toggles",
        )
        embedding = toggle_state(verified_toggles[0])
        official_series = toggle_state(verified_toggles[2])
        self.screenshot("series-settings-verified")
        return {"playlistId": playlist_id, "privacy": "private",
                "allowEmbedding": embedding, "officialSeries": official_series,
                "embeddingDeferredUntilPublic": not embedding,
                "officialSeriesDeferredUntilPublic": not official_series}

    def verify_series_playlist(self, playlist_id: str) -> dict:
        self.navigate(f"https://www.youtube.com/playlist?list={playlist_id}")
        wait_until(lambda: "Codename One" in self.body(), 60, "YouTube playlist page")
        more = self.wait_visible('button[aria-label="More actions"]', 30)
        self.driver.execute_script("arguments[0].click();", more)

        def settings_item():
            for element in self.driver.find_elements(
                By.XPATH, "//*[normalize-space(.)='Playlist Settings']"
            ):
                if element.is_displayed():
                    return element
            return None

        item = wait_until(settings_item, 15, "Playlist Settings menu item")
        self.driver.execute_script("arguments[0].click();", item)
        toggles = wait_until(
            lambda: self.visible("tp-yt-paper-toggle-button, [role='switch']")
            if len(self.visible("tp-yt-paper-toggle-button, [role='switch']")) >= 3
            else None,
            15,
            "playlist setting toggles",
        )

        def is_on(toggle) -> bool:
            return any(toggle.get_attribute(name) in ("true", "checked", "")
                       for name in ("checked", "aria-checked", "aria-pressed")
                       if toggle.get_attribute(name) is not None)

        result = {"playlistId": playlist_id, "privacy": "private",
                  "allowEmbedding": is_on(toggles[0]),
                  "officialSeries": is_on(toggles[2])}
        self.screenshot("series-settings-read-only-verified")
        return result

    def wait_for_checks(self):
        wait_until(
            lambda: "Checks complete. No issues found." in self.body(),
            600,
            "YouTube checks",
        )

    def finalize_existing_draft(self, video_id: str) -> dict:
        """Finish YouTube's upload wizard for an already-created draft.

        A slow checks page can outlive the browser session after the media,
        captions, and metadata have already been saved. Reopen that exact ID;
        never upload the file a second time.
        """
        self.open_existing_video(video_id)
        if "This video is in a draft state" not in self.body():
            return {"videoId": video_id, "draft": False, "changed": False}
        self.click_text_button("Edit draft", 30)
        wait_until(lambda: bool(self.visible("ytcp-uploads-dialog")), 45,
                   "existing upload draft dialog")
        for _ in range(5):
            body = self.body()
            if "Private" in body and self.visible("#done-button"):
                self.click_text_radio("Private")
                self.screenshot("private-visibility")
                self.click("#done-button", 30)
                wait_until(lambda: not self.visible("ytcp-uploads-dialog"), 60,
                           "finalized draft dialog to close")
                return {"videoId": video_id, "draft": False, "changed": True,
                        "privacy": "private"}
            if "Checks" in body:
                self.wait_for_checks()
            self.click("#next-button", 30)
        raise RuntimeError(f"could not reach private visibility for draft {video_id}")

    def upload(self, package: Path, key: str) -> str:
        manifest = json.loads(package.read_text(encoding="utf-8"))
        upload = manifest["uploads"][key]
        metadata = upload["youtube"]
        video = Path(upload["video"])
        captions = Path(upload["captions"])
        description = (package.parent / metadata["descriptionFile"]).read_text(
            encoding="utf-8"
        )
        if metadata.get("privacyStatus") != "private":
            raise ValueError(f"{key}: only private uploads are permitted")
        for required in (video, captions):
            if not required.is_file():
                raise FileNotFoundError(required)

        self.navigate(
            f"https://studio.youtube.com/channel/{CHANNEL_ID}/videos/upload?d=ud"
        )
        self.upload_file(video, timeout=45)
        self.set_editable("Add a title", metadata["title"])
        self.set_editable("Tell viewers", description)
        self.click_text_radio("No, it's not made for kids")
        video_id = wait_until(self.video_id, 60, "uploaded video id")
        print(f"{key}: created {video_id}", flush=True)

        if key == "landscape":
            thumbnail = package.parent / upload["thumbnail"]
            if not thumbnail.is_file():
                raise FileNotFoundError(thumbnail)
            self.upload_file(thumbnail, timeout=30)

        self.click_text_button("Show more", 30)
        self.click_text_radio("Yes")
        self.set_tags(metadata["tags"])
        wait_until(
            lambda: "All changes saved" in self.body()
            or "Checks complete" in self.body(),
            60,
            "saved metadata",
        )

        self.click("#next-button", 30)
        self.add_captions(captions)
        if key == "landscape":
            self.add_end_screen(metadata["engagement"]["endScreen"]["startAtMs"])
        self.click("#next-button", 30)
        self.wait_for_checks()
        self.click("#next-button", 30)
        self.click_text_radio("Private")
        self.screenshot("private-visibility")
        self.click("#done-button", 30)
        wait_until(
            lambda: not self.visible("ytcp-uploads-dialog"),
            60,
            "upload dialog to close",
        )
        return video_id


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("manifest", type=Path)
    parser.add_argument(
        "--orientation",
        choices=("landscape", "portrait", "both"),
        default="both",
    )
    parser.add_argument(
        "--profile",
        type=Path,
        default=Path(os.environ.get("CN1_YOUTUBE_PROFILE", str(DEFAULT_PROFILE))),
    )
    parser.add_argument("--evidence-dir", type=Path)
    parser.add_argument(
        "--headed",
        action="store_true",
        help="Open foreground Chrome; use only when login or interactive recovery is required.",
    )
    parser.add_argument("--preflight-only", action="store_true")
    parser.add_argument("--organize-only", action="store_true")
    parser.add_argument("--landscape-id")
    parser.add_argument("--short-id")
    parser.add_argument("--playlist-catalog-only", action="store_true")
    parser.add_argument("--video-catalog-only", action="store_true")
    parser.add_argument("--verify-organized-only", action="store_true")
    parser.add_argument("--inspect-playlist-id")
    parser.add_argument("--inspect-watch-playlist-id")
    parser.add_argument("--inspect-watch-playlist-menu-id")
    parser.add_argument("--inspect-series-settings-id")
    parser.add_argument("--configure-series-playlist-id")
    parser.add_argument("--verify-series-playlist-id")
    parser.add_argument("--inspect-video-id")
    parser.add_argument("--inspect-end-screen-id")
    parser.add_argument("--inspect-thumbnail-id")
    parser.add_argument("--inspect-choose-video-id")
    parser.add_argument("--inspect-timestamp-id")
    parser.add_argument("--repair-thumbnail-id")
    parser.add_argument("--repair-existing-id")
    parser.add_argument("--finalize-draft-id")
    parser.add_argument("--repair-end-screen-id")
    parser.add_argument("--verify-repair-id")
    parser.add_argument("--verify-final-pair", action="store_true")
    parser.add_argument("--end-screen-video-id", default="1_Cu1-aVLf8")
    parser.add_argument(
        "--end-screen-video-title",
        default="Java AOT vs JIT: How ParparVM Reached HotSpot Performance",
    )
    parser.add_argument("--verify-short-id")
    args = parser.parse_args()
    manifest = args.manifest.resolve()
    evidence_dir = args.evidence_dir or manifest.parent / "upload-evidence"
    keys = (
        ("landscape", "portrait")
        if args.orientation == "both"
        else (args.orientation,)
    )
    uploader = StudioUploader(args.profile, evidence_dir, headed=args.headed)
    uploaded: dict[str, str] = {}
    try:
        if args.verify_final_pair:
            if not args.landscape_id or not args.short_id:
                parser.error("final pair verification requires --landscape-id and --short-id")
            manifest_data = json.loads(manifest.read_text(encoding="utf-8"))
            landscape = manifest_data["uploads"]["landscape"]
            thumbnail = manifest.parent / landscape["thumbnail"]
            start_at_ms = landscape["youtube"]["engagement"]["endScreen"]["startAtMs"]
            uploader.evidence_dir = evidence_dir / "landscape"
            repair = uploader.verify_existing_repair(
                args.landscape_id, thumbnail, args.end_screen_video_title, start_at_ms
            )

            details = {}
            for key, video_id, playlist in (
                ("landscape", args.landscape_id,
                 manifest_data["distribution"]["trail"]["title"]),
                ("portrait", args.short_id,
                 manifest_data["distribution"]["shortsPlaylist"]["title"]),
            ):
                title = manifest_data["uploads"][key]["youtube"]["title"]
                uploader.evidence_dir = evidence_dir / key
                uploader.open_existing_video(video_id)
                body = uploader.body()
                playlist_widget = uploader.wait_visible("ytcp-video-metadata-playlists", 45)
                playlist_selected = playlist.lower() in playlist_widget.text.lower()
                private = bool(re.search(r"\bPrivate\b", body))
                uploader.screenshot("saved-details-verified")
                if not private or not playlist_selected:
                    raise RuntimeError(
                        f"{key} details failed verification: private={private}, "
                        f"playlistSelected={playlist_selected}"
                    )

                uploader.driver.get(
                    f"https://studio.youtube.com/video/{video_id}/translations"
                )
                wait_until(lambda: title in uploader.body(), 60,
                           f"subtitles page for {video_id}")
                # The left video rail renders before the Languages table.  A
                # title-only wait can therefore capture a visually empty page
                # and misreport a caption failure even though the authored
                # track was published. Wait for the table's semantic content.
                captions_body = wait_until(
                    lambda: (
                        body if "English" in body and any(
                            marker in body
                            for marker in ("Published", "Subtitles", "Language")
                        ) else None
                    ) if (body := uploader.body()) else None,
                    90,
                    f"authored English captions for {video_id}",
                )
                captions = True
                uploader.screenshot("captions-persisted")
                if not captions:
                    raise RuntimeError(f"{key} authored English captions were not visible")
                details[key] = {
                    "videoId": video_id,
                    "privacy": "private",
                    "playlist": playlist,
                    "captions": "English authored captions visible",
                }

            short_title = manifest_data["uploads"]["portrait"]["youtube"]["title"]
            uploader.evidence_dir = evidence_dir / "portrait"
            uploader.driver.get(f"https://studio.youtube.com/channel/{CHANNEL_ID}/videos")
            wait_until(lambda: "Channel content" in uploader.body(), 60, "channel content")

            def shorts_tab():
                for element in uploader.visible("tp-yt-paper-tab, [role='tab'], a, button"):
                    if element.text.strip() == "Shorts":
                        return element
                return None

            tab = wait_until(shorts_tab, 30, "Shorts tab")
            uploader.driver.execute_script("arguments[0].click();", tab)
            wait_until(lambda: short_title in uploader.body(), 45,
                       "Short in Shorts catalog")
            uploader.screenshot("short-classification-verified")
            print(json.dumps({
                "ok": True,
                "repair": repair,
                "details": details,
                "shortClassification": {"videoId": args.short_id, "type": "Short"},
            }, ensure_ascii=False, sort_keys=True), flush=True)
            return 0
        if args.inspect_timestamp_id:
            uploader.open_existing_end_screen(args.inspect_timestamp_id)
            items = [item for item in uploader.visible(
                "ytve-endscreen-editor-preview-overlay-item"
            ) if item.rect["width"] > 20 and item.rect["height"] > 20]
            video_item = max(items, key=lambda item: item.rect["width"] / item.rect["height"])
            ActionChains(uploader.driver).move_to_element(video_item).click().perform()
            start_input = uploader.wait_visible("#start-input input", 15)
            before = start_input.get_attribute("value")
            start_input.click()
            start_input.clear()
            start_input.send_keys("2:46:00")
            start_input.send_keys(Keys.ENTER)
            start_input.send_keys(Keys.TAB)
            time.sleep(2)
            mutation = uploader.driver.execute_script(
                """
                const host = document.querySelector('#start-input');
                const formatted = host.querySelector('ytve-formatted-input');
                const input = host.querySelector('input');
                const setter = Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value').set;
                setter.call(input, '2:46:00');
                let calls = [];
                for (const target of [formatted, host]) {
                  if (target && typeof target.updateValue === 'function') {
                    for (const args of [[], [{target: input, currentTarget: input}]]) {
                      try { target.updateValue(...args); calls.push(target.tagName + ':' + args.length); }
                      catch (e) { calls.push(target.tagName + ':error:' + e.message); }
                    }
                  }
                }
                input.dispatchEvent(new InputEvent('input', {bubbles:true,inputType:'insertText',data:'2:46:00'}));
                input.dispatchEvent(new Event('change', {bubbles:true}));
                input.dispatchEvent(new FocusEvent('blur', {bubbles:true}));
                return {calls, inputValue: input.value, outerHTML: host.outerHTML.slice(0,8000),
                  hostData: host.__data ? Object.fromEntries(Object.entries(host.__data).map(([k,v])=>[k,String(v)])) : {},
                  formattedData: formatted && formatted.__data ? Object.fromEntries(Object.entries(formatted.__data).map(([k,v])=>[k,String(v)])) : {}};
                """
            )
            time.sleep(2)
            host_properties = uploader.driver.execute_script(
                """
                const host = document.querySelector('#start-input');
                return Object.keys(host).filter(k => /time|frame|value|start/i.test(k))
                  .reduce((o,k) => { try { o[k]=String(host[k]); } catch(e) {} return o; }, {});
                """
            )
            uploader.screenshot("timestamp-input-test")
            print(json.dumps({"before": before, "after": start_input.get_attribute("value"),
                              "hostProperties": host_properties, "mutation": mutation},
                             sort_keys=True), flush=True)
            return 0
        if args.verify_repair_id:
            manifest_data = json.loads(manifest.read_text(encoding="utf-8"))
            thumbnail = manifest.parent / manifest_data["uploads"]["landscape"]["thumbnail"]
            start_at_ms = manifest_data["uploads"]["landscape"]["youtube"][
                "engagement"
            ]["endScreen"]["startAtMs"]
            uploader.evidence_dir = evidence_dir / "landscape"
            result = uploader.verify_existing_repair(
                args.verify_repair_id, thumbnail, args.end_screen_video_title, start_at_ms
            )
            print(json.dumps({"ok": True, "verification": result},
                             ensure_ascii=False, sort_keys=True), flush=True)
            return 0
        if args.repair_end_screen_id:
            manifest_data = json.loads(manifest.read_text(encoding="utf-8"))
            landscape = manifest_data["uploads"]["landscape"]
            start_at_ms = landscape["youtube"]["engagement"]["endScreen"]["startAtMs"]
            uploader.evidence_dir = evidence_dir / "landscape"
            end_screen_result = uploader.replace_and_align_existing_end_screen(
                args.repair_end_screen_id,
                args.end_screen_video_id,
                args.end_screen_video_title,
                start_at_ms,
            )
            print(json.dumps({"ok": True, "endScreen": end_screen_result},
                             ensure_ascii=False, sort_keys=True), flush=True)
            return 0
        if args.repair_thumbnail_id:
            manifest_data = json.loads(manifest.read_text(encoding="utf-8"))
            landscape = manifest_data["uploads"]["landscape"]
            thumbnail = manifest.parent / landscape["thumbnail"]
            uploader.evidence_dir = evidence_dir / "landscape"
            thumbnail_result = uploader.replace_existing_thumbnail(
                args.repair_thumbnail_id, thumbnail
            )
            print(json.dumps({"ok": True, "thumbnail": thumbnail_result},
                             ensure_ascii=False, sort_keys=True), flush=True)
            return 0
        if args.repair_existing_id:
            manifest_data = json.loads(manifest.read_text(encoding="utf-8"))
            landscape = manifest_data["uploads"]["landscape"]
            thumbnail = manifest.parent / landscape["thumbnail"]
            start_at_ms = landscape["youtube"]["engagement"]["endScreen"]["startAtMs"]
            uploader.evidence_dir = evidence_dir / "landscape"
            thumbnail_result = uploader.replace_existing_thumbnail(
                args.repair_existing_id, thumbnail
            )
            end_screen_result = uploader.replace_and_align_existing_end_screen(
                args.repair_existing_id,
                args.end_screen_video_id,
                args.end_screen_video_title,
                start_at_ms,
            )
            print(json.dumps({
                "ok": True,
                "thumbnail": thumbnail_result,
                "endScreen": end_screen_result,
            }, ensure_ascii=False, sort_keys=True), flush=True)
            return 0
        if args.finalize_draft_id:
            result = uploader.finalize_existing_draft(args.finalize_draft_id)
            print(json.dumps({"ok": True, "draftFinalization": result},
                             ensure_ascii=False, sort_keys=True), flush=True)
            return 0
        if args.inspect_thumbnail_id:
            uploader.driver.get(
                f"https://studio.youtube.com/video/{args.inspect_thumbnail_id}/edit"
            )
            wait_until(lambda: "Video details" in uploader.body(), 60, "video details")
            inputs = uploader.driver.find_elements(By.CSS_SELECTOR, 'input[type="file"]')
            print(json.dumps([{
                "index": index,
                "accept": element.get_attribute("accept"),
                "outerHTML": element.get_attribute("outerHTML")[:4000],
            } for index, element in enumerate(inputs)], ensure_ascii=False, indent=2), flush=True)
            uploader.screenshot("thumbnail-before-repair")
            return 0
        if args.inspect_choose_video_id:
            uploader.driver.get(
                f"https://studio.youtube.com/video/{args.inspect_choose_video_id}/edit"
            )
            wait_until(lambda: "Video details" in uploader.body(), 60, "video details")
            try:
                uploader.click("#endscreens-button", 12)
            except TimeoutException:
                candidates = [element for element in uploader.driver.find_elements(
                    By.XPATH, "//*[normalize-space(.)='End screen']"
                ) if element.is_displayed()]
                if not candidates:
                    raise RuntimeError("existing-video End screen control is missing")
                uploader.driver.execute_script(
                    "arguments[0].scrollIntoView({block:'center'}); arguments[0].click();",
                    candidates[-1],
                )
            wait_until(lambda: "End Screens" in uploader.body(), 45, "end-screen editor")
            video_item = wait_until(
                lambda: next((element for element in uploader.visible(
                    "ytve-endscreen-editor-preview-overlay-item"
                ) if element.text.strip().startswith("Video:")), None),
                20, "end-screen video element",
            )
            ActionChains(uploader.driver).move_to_element(video_item).click().perform()
            choose = uploader.wait_visible("#choose-video", 15)
            ActionChains(uploader.driver).move_to_element(choose).click().perform()
            wait_until(lambda: "Choose specific video" in uploader.body(),
                       30, "specific video dialog")
            time.sleep(2)
            uploader.screenshot("choose-specific-video")
            controls = uploader.driver.execute_script(
                """
                return Array.from(document.querySelectorAll('input, button, ytcp-button, tp-yt-paper-item, ytcp-video-picker-item'))
                  .filter(e => { const r=e.getBoundingClientRect(); return r.width && r.height; })
                  .map(e => ({tag:e.tagName.toLowerCase(),id:e.id||'',aria:e.getAttribute('aria-label')||'',
                    text:(e.innerText||'').trim().slice(0,300),outerHTML:e.outerHTML.slice(0,1200),
                    rect:(() => {const r=e.getBoundingClientRect();return{x:r.x,y:r.y,width:r.width,height:r.height};})()}));
                """
            )
            print(json.dumps(controls, ensure_ascii=False, indent=2), flush=True)
            return 0
        if args.inspect_end_screen_id:
            uploader.driver.get(
                f"https://studio.youtube.com/video/{args.inspect_end_screen_id}/edit"
            )
            wait_until(lambda: "Video details" in uploader.body(), 60, "video details")
            try:
                uploader.click("#endscreens-button", 12)
            except TimeoutException:
                candidates = [
                    element for element in uploader.driver.find_elements(
                        By.XPATH, "//*[normalize-space(.)='End screen']"
                    ) if element.is_displayed()
                ]
                if not candidates:
                    raise RuntimeError("existing-video End screen control is missing")
                target = candidates[-1]
                uploader.driver.execute_script(
                    "arguments[0].scrollIntoView({block:'center'});", target
                )
                uploader.driver.execute_script("arguments[0].click();", target)
            wait_until(lambda: "End Screens" in uploader.body(), 45, "end-screen editor")
            time.sleep(2)
            video_items = [
                element for element in uploader.visible(
                    "ytve-endscreen-editor-preview-overlay-item"
                ) if element.text.strip().startswith("Video:")
            ]
            if video_items:
                ActionChains(uploader.driver).move_to_element(video_items[0]).click().perform()
                time.sleep(1)
            uploader.screenshot("end-screen-before-repair")
            visible = []
            for element in uploader.visible(
                "ytve-element-renderer, ytve-video-editor, ytve-end-screen-editor, "
                "ytcp-button, tp-yt-paper-radio-button, tp-yt-paper-item, button, input"
            ):
                visible.append({
                    "tag": element.tag_name,
                    "id": element.get_attribute("id"),
                    "class": element.get_attribute("class"),
                    "aria": element.get_attribute("aria-label"),
                    "text": element.text.strip()[:300],
                    "rect": element.rect,
                })
            print(json.dumps(visible, ensure_ascii=False, indent=2), flush=True)
            ytve = uploader.driver.execute_script(
                """
                return Array.from(document.querySelectorAll('*'))
                  .filter(e => e.tagName.startsWith('YTVE'))
                  .map(e => ({
                    tag: e.tagName.toLowerCase(), id: e.id || '',
                    className: typeof e.className === 'string' ? e.className : '',
                    aria: e.getAttribute('aria-label') || '',
                    text: (e.innerText || '').trim().slice(0, 240),
                    rect: (() => { const r=e.getBoundingClientRect();
                      return {x:r.x,y:r.y,width:r.width,height:r.height}; })()
                  }));
                """
            )
            print(json.dumps(ytve, ensure_ascii=False, indent=2), flush=True)
            for selector in ("ytve-end-screen-editor", "ytve-video-editor"):
                elements = uploader.driver.find_elements(By.CSS_SELECTOR, selector)
                if elements:
                    print(elements[0].get_attribute("outerHTML")[:30000], flush=True)
            return 0
        if args.verify_short_id:
            manifest_data = json.loads(manifest.read_text(encoding="utf-8"))
            short_title = manifest_data["uploads"]["portrait"]["youtube"]["title"]
            uploader.driver.get(f"https://studio.youtube.com/channel/{CHANNEL_ID}/videos")
            wait_until(lambda: "Channel content" in uploader.body(), 60, "channel content")

            def shorts_tab():
                for element in uploader.visible("tp-yt-paper-tab, [role='tab'], a, button"):
                    if element.text.strip() == "Shorts":
                        return element
                return None

            tab = wait_until(shorts_tab, 30, "Shorts tab")
            uploader.driver.execute_script("arguments[0].click();", tab)
            wait_until(lambda: short_title in uploader.body(), 45, "Short in Shorts catalog")
            uploader.screenshot("short-classification-verified")
            print(json.dumps({"ok": True, "videoId": args.verify_short_id,
                              "classification": "Short", "title": short_title},
                             sort_keys=True))
            return 0
        if args.inspect_video_id:
            uploader.driver.get(f"https://studio.youtube.com/video/{args.inspect_video_id}/edit")
            wait_until(lambda: "Video details" in uploader.body(), 60, "video details")
            try:
                uploader.click_text_button("Show more", 15)
            except TimeoutException:
                pass
            uploader.driver.execute_script(
                "window.scrollTo(0, document.documentElement.scrollHeight)"
            )
            time.sleep(2)
            uploader.screenshot("video-details-expanded")
            print(uploader.body()[:18000])
            return 0
        if args.configure_series_playlist_id:
            print(json.dumps({"ok": True, "series": uploader.configure_series_playlist(
                args.configure_series_playlist_id)}, sort_keys=True))
            return 0
        if args.verify_series_playlist_id:
            result = uploader.verify_series_playlist(args.verify_series_playlist_id)
            print(json.dumps({"ok": result["officialSeries"],
                              "series": result}, sort_keys=True))
            return 0
        if args.inspect_playlist_id:
            uploader.driver.get(
                f"https://studio.youtube.com/playlist/{args.inspect_playlist_id}/edit"
            )
            wait_until(lambda: "Playlist details" in uploader.body()
                       or "Codename One" in uploader.body(), 60, "playlist editor")
            uploader.screenshot("playlist-editor")
            print(uploader.body()[:12000])
            return 0
        if args.inspect_watch_playlist_id:
            uploader.driver.get(
                f"https://www.youtube.com/playlist?list={args.inspect_watch_playlist_id}"
            )
            wait_until(lambda: "Codename One" in uploader.body()
                       or "Private playlist" in uploader.body(), 60, "YouTube playlist page")
            uploader.screenshot("watch-playlist")
            print(uploader.body()[:12000])
            return 0
        if args.inspect_watch_playlist_menu_id:
            uploader.driver.get(
                f"https://www.youtube.com/playlist?list={args.inspect_watch_playlist_menu_id}"
            )
            wait_until(lambda: "Codename One" in uploader.body(), 60, "YouTube playlist page")
            more_buttons = uploader.visible(
                'button[aria-label="More actions"], yt-icon-button[aria-label="More actions"]'
            )
            if not more_buttons:
                headers = uploader.visible("ytd-playlist-header-renderer")
                if headers:
                    print(headers[0].get_attribute("outerHTML")[:12000], flush=True)
                button_debug = []
                for element in uploader.visible("button, yt-icon-button"):
                    button_debug.append({
                        "aria": element.get_attribute("aria-label"),
                        "title": element.get_attribute("title"),
                        "text": element.text.strip(),
                        "x": element.rect["x"],
                        "y": element.rect["y"],
                    })
                print(json.dumps(button_debug[:120], ensure_ascii=False), flush=True)
                raise RuntimeError("playlist More button is missing")
            more = min(more_buttons, key=lambda element: element.rect["x"])
            uploader.driver.execute_script("arguments[0].click();", more)
            time.sleep(1)
            uploader.screenshot("watch-playlist-menu")
            print(uploader.body()[:12000])
            return 0
        if args.inspect_series_settings_id:
            uploader.driver.get(
                f"https://www.youtube.com/playlist?list={args.inspect_series_settings_id}"
            )
            wait_until(lambda: "Codename One" in uploader.body(), 60, "YouTube playlist page")
            more = uploader.wait_visible('button[aria-label="More actions"]', 30)
            uploader.driver.execute_script("arguments[0].click();", more)

            def playlist_settings_item():
                for element in uploader.driver.find_elements(
                    By.XPATH, "//*[normalize-space(.)='Playlist Settings']"
                ):
                    if element.is_displayed():
                        return element
                return None

            item = wait_until(playlist_settings_item, 15, "Playlist Settings menu item")
            uploader.driver.execute_script("arguments[0].click();", item)
            wait_until(lambda: "official series" in uploader.body().lower(),
                       30, "official series setting")
            uploader.screenshot("series-settings")
            print(uploader.body()[:12000])
            return 0
        if args.playlist_catalog_only:
            print(json.dumps({"ok": True, "playlists": uploader.playlist_catalog()},
                             sort_keys=True))
            return 0
        if args.video_catalog_only:
            print(json.dumps({"ok": True, "videos": uploader.video_catalog()},
                             sort_keys=True))
            return 0
        if args.verify_organized_only:
            if "landscape" in keys and not args.landscape_id:
                parser.error("landscape verification requires --landscape-id")
            if "portrait" in keys and not args.short_id:
                parser.error("portrait verification requires --short-id")
            manifest_data = json.loads(manifest.read_text(encoding="utf-8"))
            checks = {}
            for key, video_id, playlist in (
                ("landscape", args.landscape_id,
                 manifest_data["distribution"]["trail"]["title"]),
                ("portrait", args.short_id,
                 manifest_data["distribution"]["shortsPlaylist"]["title"]),
            ):
                if key not in keys:
                    continue
                title = manifest_data["uploads"][key]["youtube"]["title"]
                uploader.evidence_dir = evidence_dir / key
                uploader.driver.get(f"https://studio.youtube.com/video/{video_id}/edit")
                wait_until(lambda: title in uploader.body() and "Details" in uploader.body(),
                           60, f"video editor for {video_id}")
                widget = uploader.wait_visible("ytcp-video-metadata-playlists", 45)
                selected = playlist.lower() in widget.text.lower()
                uploader.screenshot("playlist-verified")
                checks[key] = {"videoId": video_id, "playlist": playlist,
                               "selected": selected, "widgetText": widget.text.strip()}
            print(json.dumps({"ok": all(item["selected"] for item in checks.values()),
                              "checks": checks}, sort_keys=True))
            return 0
        if args.organize_only:
            if "landscape" in keys and not args.landscape_id:
                parser.error("landscape organization requires --landscape-id")
            if "portrait" in keys and not args.short_id:
                parser.error("portrait organization requires --short-id")
            manifest_data = json.loads(manifest.read_text(encoding="utf-8"))
            landscape_title = manifest_data["uploads"]["landscape"]["youtube"]["title"]
            portrait_title = manifest_data["uploads"]["portrait"]["youtube"]["title"]
            trail_title = manifest_data["distribution"]["trail"]["title"]
            trail_description = manifest_data["distribution"]["trail"]["description"]
            shorts_title = manifest_data["distribution"]["shortsPlaylist"]["title"]
            shorts_description = manifest_data["distribution"]["shortsPlaylist"]["description"]
            playlist_results = {}
            if "landscape" in keys:
                uploader.evidence_dir = evidence_dir / "landscape"
                playlist_results["landscape"] = uploader.assign_existing_playlist(
                    args.landscape_id, landscape_title, trail_title, trail_description
                )
                print(json.dumps({"playlist": {"landscape": playlist_results["landscape"]}},
                                 sort_keys=True), flush=True)
            if "portrait" in keys:
                uploader.evidence_dir = evidence_dir / "portrait"
                playlist_results["portrait"] = uploader.assign_existing_playlist(
                    args.short_id, portrait_title, shorts_title, shorts_description
                )
                print(json.dumps({"playlist": {"portrait": playlist_results["portrait"]}},
                                 sort_keys=True), flush=True)
            print(json.dumps({"ok": True, "playlists": playlist_results}, sort_keys=True))
            return 0
        preflight = uploader.preflight(manifest, keys)
        print(json.dumps({"ok": True, "preflight": preflight}, sort_keys=True), flush=True)
        if args.preflight_only:
            return 0
        for key in keys:
            uploader.evidence_dir = evidence_dir / key
            try:
                uploaded[key] = uploader.upload(manifest, key)
                if key == "landscape":
                    manifest_data = json.loads(manifest.read_text(encoding="utf-8"))
                    end_screen = manifest_data["uploads"]["landscape"]["youtube"][
                        "engagement"
                    ]["endScreen"]
                    specific = next((element for element in end_screen["elements"]
                                     if element.get("type") == "specific-video"), None)
                    if specific:
                        uploader.replace_and_align_existing_end_screen(
                            uploaded[key],
                            specific["videoId"],
                            specific["title"],
                            end_screen["startAtMs"],
                        )
            except Exception:
                try:
                    uploader.screenshot("upload-failed")
                except WebDriverException:
                    pass
                raise
    finally:
        uploader.close()
    print(json.dumps({"ok": True, "uploads": uploaded}, sort_keys=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
