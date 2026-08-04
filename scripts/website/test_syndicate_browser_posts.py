#!/usr/bin/env python3

from __future__ import annotations

import unittest

from syndicate_blog_posts import State
from syndicate_browser_posts import (
    AdapterError,
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


if __name__ == "__main__":
    unittest.main()
