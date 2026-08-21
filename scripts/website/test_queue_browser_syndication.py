#!/usr/bin/env python3

from __future__ import annotations

import datetime as dt
import json
import tempfile
import unittest
from pathlib import Path

import queue_browser_syndication as queue_syndication


class BrowserSyndicationQueueTest(unittest.TestCase):
    def test_default_platforms_exclude_dzone(self) -> None:
        self.assertEqual("medium", queue_syndication.DEFAULT_PLATFORMS)

    def test_paused_dzone_task_is_never_allowed(self) -> None:
        task = {"id": "dzone:old-post", "site": "dzone", "slug": "old-post"}

        self.assertFalse(queue_syndication._task_is_allowed(task, {}))

    def test_explicit_dzone_request_prunes_stale_task_without_requeueing(self) -> None:
        with tempfile.TemporaryDirectory() as temp_dir:
            root = Path(temp_dir)
            blog_dir = root / "blog"
            blog_dir.mkdir()
            (blog_dir / "friday-post.md").write_text(
                "---\n"
                "title: Friday Post\n"
                "slug: friday-post\n"
                "url: /blog/friday-post/\n"
                "date: '2026-08-07'\n"
                "---\n"
                "Body\n",
                encoding="utf-8",
            )
            state_file = root / "state.json"
            state_file.write_text('{"posts": {}}\n', encoding="utf-8")
            queue_file = root / "queue.json"
            queue_file.write_text(
                json.dumps(
                    {
                        "tasks": [
                            {
                                "id": "dzone:friday-post",
                                "site": "dzone",
                                "slug": "friday-post",
                                "queued_at": "2026-08-14T13:00:00+00:00",
                            },
                            {
                                "id": "medium:existing",
                                "site": "medium",
                                "slug": "existing",
                                "queued_at": "2026-08-14T13:00:00+00:00",
                            },
                        ]
                    }
                )
                + "\n",
                encoding="utf-8",
            )

            result = queue_syndication.main(
                [
                    "--platforms",
                    "dzone",
                    "--today",
                    dt.date(2026, 8, 20).isoformat(),
                    "--blog-dir",
                    str(blog_dir),
                    "--state-file",
                    str(state_file),
                    "--queue-file",
                    str(queue_file),
                ]
            )

            self.assertEqual(0, result)
            tasks = json.loads(queue_file.read_text(encoding="utf-8"))["tasks"]
            self.assertEqual(["medium:existing"], [task["id"] for task in tasks])


if __name__ == "__main__":
    unittest.main()
