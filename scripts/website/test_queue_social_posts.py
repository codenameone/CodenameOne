#!/usr/bin/env python3
from __future__ import annotations

import datetime as dt
import json
import sys
import tempfile
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import queue_social_posts as social
from syndicate_blog_posts import State


BLOG = """---
title: "Source Post"
slug: source-post
url: /blog/source-post/
date: '2026-07-20'
author: Shai Almog
description: Test source.
---

![Source](/blog/source-post.jpg)

Body.
"""


def artifact(
    *,
    status: str | None = None,
    publish_at: str = "2026-07-20T03:00:00",
) -> str:
    status_field = f"status: {status}\n" if status else ""
    return f"""---
title: "Personal performance lesson"
slug: 2026-07-20-0300-shai-source-post
platform: linkedin
account: shai
source_slug: source-post
publish_at: '{publish_at}'
timezone: Asia/Jerusalem
image: /blog/source-post.jpg
{status_field}---

The useful part of this test is the queue contract.

Full write-up: {{{{canonical}}}}
"""


class SocialQueueTest(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        root = Path(self.tmp.name)
        self.blog_dir = root / "blog"
        self.social_dir = root / "social"
        self.static_dir = root / "static"
        self.blog_dir.mkdir()
        self.social_dir.mkdir()
        (self.static_dir / "blog").mkdir(parents=True)
        (self.blog_dir / "source-post.md").write_text(
            BLOG, encoding="utf-8"
        )
        (self.static_dir / "blog" / "source-post.jpg").write_bytes(
            b"image"
        )

    def tearDown(self):
        self.tmp.cleanup()

    def write_artifact(self, text: str) -> Path:
        path = (
            self.social_dir
            / "2026-07-20-0300-shai-source-post.md"
        )
        path.write_text(text, encoding="utf-8")
        return path

    def parse(self, text: str) -> social.SocialPost:
        return social.parse_social_post(
            self.write_artifact(text),
            self.blog_dir,
            self.static_dir,
        )

    def test_post_queues_with_time_link_and_image(self):
        post = self.parse(artifact())
        now = dt.datetime(
            2026, 7, 17, 9, tzinfo=dt.timezone.utc
        )
        tasks = social.queue_posts(
            [post],
            {"tasks": []},
            State(raw={"posts": {}}),
            queued_at=now,
        )
        self.assertEqual(1, len(tasks))
        task = tasks[0]
        self.assertEqual(
            "2026-07-20T00:00:00+00:00",
            task["scheduled_at"],
        )
        self.assertEqual(
            "2026-07-20T03:00+03:00",
            task["scheduled_local"],
        )
        self.assertEqual(
            "https://www.codenameone.com/blog/source-post/",
            task["canonical"],
        )
        self.assertIn(task["canonical"], task["body_text"])
        self.assertEqual(
            "https://www.codenameone.com/blog/source-post.jpg",
            task["cover_image_url"],
        )

    def test_legacy_draft_status_does_not_block_queueing(self):
        post = self.parse(artifact(status="draft"))
        tasks = social.queue_posts(
            [post],
            {"tasks": []},
            State(raw={"posts": {}}),
        )
        self.assertEqual(1, len(tasks))

    def test_publish_date_must_match_source_blog_date(self):
        with self.assertRaisesRegex(
            social.SocialPostError, "source blog date"
        ):
            self.parse(
                artifact(publish_at="2026-07-21T03:00:00")
            )

    def test_queue_and_state_deduplicate(self):
        post = self.parse(artifact())
        existing = {"tasks": [{"id": post.task_id}]}
        state = State(raw={"posts": {}})
        self.assertEqual(
            [], social.queue_posts([post], existing, state)
        )
        state = State(
            raw={
                "posts": {
                    post.slug: {
                        "linkedin": {
                            "url": "https://linkedin.example/post"
                        }
                    }
                }
            }
        )
        self.assertEqual(
            [],
            social.queue_posts([post], {"tasks": []}, state),
        )

    def test_image_must_exist(self):
        (
            self.static_dir / "blog" / "source-post.jpg"
        ).unlink()
        with self.assertRaisesRegex(
            social.SocialPostError,
            "image attachment does not exist",
        ):
            self.parse(artifact())

    def test_body_requires_one_canonical_reference(self):
        with self.assertRaisesRegex(
            social.SocialPostError,
            "exactly one canonical",
        ):
            self.parse(
                artifact().replace("{{canonical}}", "no-link")
            )

    def test_existing_canonical_url_is_accepted(self):
        post = self.parse(
            artifact().replace(
                "{{canonical}}",
                "https://www.codenameone.com/blog/source-post/",
            )
        )
        self.assertEqual(
            "https://www.codenameone.com/blog/source-post/",
            post.canonical,
        )

    def test_main_dry_run_does_not_write_queue(self):
        self.write_artifact(artifact())
        root = Path(self.tmp.name)
        queue = root / "queue.json"
        state = root / "state.json"
        queue.write_text(
            json.dumps({"tasks": []}), encoding="utf-8"
        )
        state.write_text(
            json.dumps({"posts": {}}), encoding="utf-8"
        )
        rc = social.main(
            [
                "--social-dir",
                str(self.social_dir),
                "--blog-dir",
                str(self.blog_dir),
                "--static-dir",
                str(self.static_dir),
                "--queue-file",
                str(queue),
                "--state-file",
                str(state),
                "--dry-run",
            ]
        )
        self.assertEqual(0, rc)
        self.assertEqual(
            [],
            json.loads(
                queue.read_text(encoding="utf-8")
            )["tasks"],
        )


if __name__ == "__main__":
    unittest.main()
