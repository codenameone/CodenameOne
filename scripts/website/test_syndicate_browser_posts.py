#!/usr/bin/env python3

from __future__ import annotations

import inspect
import unittest
from unittest.mock import MagicMock, patch

import json
import tempfile
import time
from pathlib import Path

import syndicate_browser_posts
from export_storage_state import SITE_PROFILES, _cookie_unexpired
from syndicate_blog_posts import State
from syndicate_browser_posts import (
    AdapterError,
    HashnodeAdapter,
    _completed_hashnode_result,
    _retryable_failed_draft,
)


class HashnodeCompletionTest(unittest.TestCase):
    def test_unpublished_draft_is_not_persisted_as_completed(self) -> None:
        with self.assertRaisesRegex(AdapterError, "unpublished draft"):
            _completed_hashnode_result(
                published_url=None,
                draft_url="https://hashnode.com/draft/example",
                cover_set=True,
                subheading_set=True,
                tags_set=False,
                canonical_set=False,
            )

    def test_public_result_is_persisted_as_published(self) -> None:
        result = _completed_hashnode_result(
            published_url="https://debugagent.com/example",
            draft_url="https://hashnode.com/draft/example",
            cover_set=True,
            subheading_set=True,
            tags_set=True,
            canonical_set=True,
        )
        self.assertEqual("https://debugagent.com/example", result["url"])
        self.assertTrue(result["published"])
        self.assertTrue(result["canonical_set"])

    def test_hashnode_edit_redirect_is_not_a_public_result(self) -> None:
        with self.assertRaisesRegex(AdapterError, "unpublished draft"):
            _completed_hashnode_result(
                published_url="https://hashnode.com/edit/post-id",
                draft_url="https://hashnode.com/draft/example",
                cover_set=True,
                subheading_set=True,
                tags_set=True,
                canonical_set=True,
            )

    def test_public_article_verification_accepts_matching_canonical(self) -> None:
        response = MagicMock()
        response.status = 200
        response.read.return_value = (
            b'<link rel="canonical" href="https://www.codenameone.com/blog/example/">'
        )
        response.__enter__.return_value = response

        with patch(
            "syndicate_browser_posts.urllib.request.urlopen",
            return_value=response,
        ):
            self.assertTrue(
                HashnodeAdapter._wait_for_public_article(
                    "https://debugagent.com/example",
                    "https://www.codenameone.com/blog/example/",
                )
            )

    def test_explicit_recovery_accepts_only_failed_drafts(self) -> None:
        failed = State(
            raw={
                "posts": {
                    "example": {
                        "hashnode": {
                            "url": "https://hashnode.com/draft/example",
                            "draft_url": "https://hashnode.com/draft/example",
                            "published": False,
                        }
                    }
                }
            }
        )
        complete = State(
            raw={
                "posts": {
                    "example": {
                        "hashnode": {
                            "url": "https://debugagent.com/example",
                            "draft_url": "https://hashnode.com/draft/example",
                            "published": True,
                        }
                    }
                }
            }
        )
        self.assertTrue(_retryable_failed_draft(failed, "example", "hashnode"))
        self.assertFalse(_retryable_failed_draft(complete, "example", "hashnode"))

    def test_hashnode_publish_sheet_selectors_match_current_controls(self) -> None:
        self.assertIn("button#republish-canonical", HashnodeAdapter.CANONICAL_TOGGLE_SELECTOR)
        self.assertIn("sheet-footer", HashnodeAdapter.DIALOG_PUBLISH_SELECTOR)

    def test_tag_entry_cannot_close_publish_sheet(self) -> None:
        source = inspect.getsource(HashnodeAdapter._set_tags)
        self.assertNotIn('press("Escape")', source)
        self.assertIn("tags_input.fill(tag", source)

    def test_publish_requires_tags_and_canonical(self) -> None:
        source = inspect.getsource(HashnodeAdapter.submit_draft)
        self.assertIn("if tags_set and canonical_set", source)


def _session_cookie(expires: float) -> dict:
    return {"name": "hashnode-session", "value": "x" * 64, "domain": ".hashnode.com", "expires": expires}


class HashnodeSessionExpiryTest(unittest.TestCase):
    def test_expired_session_cookie_is_not_logged_in(self) -> None:
        logged_in = SITE_PROFILES["hashnode"]["is_logged_in"]
        self.assertFalse(logged_in([_session_cookie(time.time() - 60)]))
        self.assertTrue(logged_in([_session_cookie(time.time() + 3600)]))
        # Browser-session cookies carry -1 or no expiry at all.
        self.assertTrue(logged_in([_session_cookie(-1)]))
        self.assertTrue(_cookie_unexpired({"name": "hashnode-session"}))

    def _resolve_with_file(self, cookies: list[dict], firefox: Path | None) -> Path | None:
        with tempfile.TemporaryDirectory() as tmp:
            saved = Path(tmp) / "hashnode-storage-state.json"
            saved.write_text(json.dumps({"cookies": cookies, "origins": []}), encoding="utf-8")
            with patch.object(syndicate_browser_posts, "HASHNODE_STORAGE_FILE", saved), \
                    patch.object(syndicate_browser_posts, "_HASHNODE_STATE_RESOLVED", False), \
                    patch.object(syndicate_browser_posts, "_hashnode_storage_from_firefox",
                                 return_value=firefox), \
                    patch.dict("os.environ", {"HASHNODE_STORAGE_STATE": ""}):
                resolved = syndicate_browser_posts._resolve_hashnode_storage_state()
                return None if resolved is None else (saved if resolved == saved else resolved)

    def test_expired_saved_file_falls_through_to_firefox(self) -> None:
        firefox = Path("/tmp/firefox-hashnode-state.json")
        self.assertEqual(firefox, self._resolve_with_file([_session_cookie(time.time() - 60)], firefox))
        self.assertIsNone(self._resolve_with_file([_session_cookie(time.time() - 60)], None))

    def test_live_saved_file_still_wins(self) -> None:
        resolved = self._resolve_with_file([_session_cookie(time.time() + 3600)],
                                           Path("/tmp/firefox-hashnode-state.json"))
        self.assertEqual("hashnode-storage-state.json", resolved.name)


if __name__ == "__main__":
    unittest.main()
