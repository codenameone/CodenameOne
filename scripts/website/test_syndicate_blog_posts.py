#!/usr/bin/env python3

import datetime as dt
import importlib.util
import json
import os
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path
from unittest import mock

from merge_syndication_json import MergeConflict, merge_value


SCRIPT_DIR = Path(__file__).resolve().parent
MODULE_PATH = SCRIPT_DIR / "syndicate_blog_posts.py"
COMMIT_SCRIPT = SCRIPT_DIR / "commit_syndication_state.sh"

spec = importlib.util.spec_from_file_location("syndicate_blog_posts", MODULE_PATH)
syndicate = importlib.util.module_from_spec(spec)
assert spec.loader is not None
sys.modules[spec.name] = syndicate
spec.loader.exec_module(syndicate)


class DevToRecoveryTest(unittest.TestCase):
    def setUp(self):
        self.post = syndicate.Post(
            path=Path("post.md"),
            slug="lost-state",
            title="Lost State",
            date=dt.date(2026, 7, 8),
            front_matter={"url": "/blog/lost-state/"},
            body="Body",
        )

    @mock.patch.object(syndicate, "find_devto_article_by_canonical")
    @mock.patch.object(syndicate, "http_post_json")
    def test_duplicate_canonical_recovers_existing_article(self, post_json, find_article):
        post_json.side_effect = syndicate.HttpJsonError(
            "https://dev.to/api/articles",
            422,
            '{"error":"Canonical url has already been taken."}',
        )
        recovered = {
            "id": 123,
            "url": "https://dev.to/codenameone/lost-state-123",
            "syndicated_at": "2026-07-15T14:58:28+00:00",
            "recovered": True,
        }
        find_article.return_value = recovered

        result = syndicate.publish_to_devto(self.post, "Body", "api-key")

        self.assertEqual(recovered, result)
        find_article.assert_called_once_with(self.post.canonical_url, "api-key")

    @mock.patch.object(syndicate, "find_devto_article_by_canonical")
    @mock.patch.object(syndicate, "http_post_json")
    def test_unrelated_http_error_is_not_hidden(self, post_json, find_article):
        error = syndicate.HttpJsonError(
            "https://dev.to/api/articles", 401, '{"error":"unauthorized"}'
        )
        post_json.side_effect = error

        with self.assertRaises(syndicate.HttpJsonError) as raised:
            syndicate.publish_to_devto(self.post, "Body", "api-key")

        self.assertIs(error, raised.exception)
        find_article.assert_not_called()

    @mock.patch.object(syndicate, "http_get_json")
    def test_lookup_matches_canonical_url_without_trailing_slash(self, get_json):
        get_json.side_effect = [
            [
                {
                    "id": 123,
                    "url": "https://dev.to/codenameone/lost-state-123",
                    "canonical_url": self.post.canonical_url.rstrip("/"),
                    "published_at": "2026-07-15T14:58:28+00:00",
                }
            ]
        ]

        result = syndicate.find_devto_article_by_canonical(
            self.post.canonical_url, "api-key"
        )

        self.assertEqual(123, result["id"])
        self.assertTrue(result["recovered"])


class RecoveredStateMergeTest(unittest.TestCase):
    def state(self, **fields):
        return {"posts": {"same-post": {"devto": fields}}}

    def test_same_article_keeps_recorded_timestamp_and_recovery_metadata(self):
        remote = self.state(id=123, url="https://dev.to/same-post",
                            syndicated_at="2026-10-03T17:35:08+00:00")
        local = self.state(id=123, url="https://dev.to/same-post",
                           syndicated_at="2026-10-03T17:35:07Z", recovered=True)
        merged = merge_value({"posts": {}}, local, remote)
        self.assertEqual(remote["posts"]["same-post"]["devto"]["syndicated_at"],
                         merged["posts"]["same-post"]["devto"]["syndicated_at"])
        self.assertTrue(merged["posts"]["same-post"]["devto"]["recovered"])

    def test_different_or_missing_article_identity_still_conflicts(self):
        remote = self.state(id=123, url="https://dev.to/same-post", syndicated_at="old")
        for identity in ({"id": 456, "url": "https://dev.to/same-post"},
                         {"id": 123, "url": "https://dev.to/another-post"},
                         {"url": "https://dev.to/same-post"},
                         {"id": 123}):
            with self.subTest(identity=identity), self.assertRaises(MergeConflict):
                merge_value({"posts": {}}, self.state(**identity, syndicated_at="new"), remote)

    def test_other_metadata_conflicts_are_not_hidden(self):
        remote = self.state(id=123, url="https://dev.to/same-post", published=True)
        local = self.state(id=123, url="https://dev.to/same-post", published=False)
        with self.assertRaises(MergeConflict):
            merge_value({"posts": {}}, local, remote)

    def test_unrelated_timestamp_conflicts_are_not_hidden(self):
        with self.assertRaises(MergeConflict):
            merge_value({}, {"id": 123, "url": "same", "syndicated_at": "new"},
                        {"id": 123, "url": "same", "syndicated_at": "old"})


class StateCommitRaceTest(unittest.TestCase):
    def git(self, cwd, *args):
        return subprocess.run(
            ["git", *args],
            cwd=cwd,
            check=True,
            text=True,
            capture_output=True,
        )

    def write_state_files(self, repo, state_text="{}\n", queue_text="{}\n"):
        website = repo / "scripts" / "website"
        website.mkdir(parents=True, exist_ok=True)
        (website / "syndication-state.json").write_text(state_text, encoding="utf-8")
        (website / "syndication-queue.json").write_text(queue_text, encoding="utf-8")

    def test_state_commit_rebases_when_default_branch_advanced(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            root = Path(temp_dir)
            remote = root / "remote.git"
            seed = root / "seed"
            runner = root / "runner"
            concurrent = root / "concurrent"
            verify = root / "verify"

            self.git(root, "init", "--bare", "--initial-branch=master", str(remote))
            self.git(root, "clone", str(remote), str(seed))
            self.git(seed, "config", "user.name", "Test")
            self.git(seed, "config", "user.email", "test@example.com")
            self.write_state_files(seed)
            (seed / "README.md").write_text("initial\n", encoding="utf-8")
            self.git(seed, "add", ".")
            self.git(seed, "commit", "-m", "initial")
            self.git(seed, "push", "origin", "master")

            self.git(root, "clone", str(remote), str(runner))
            self.git(root, "clone", str(remote), str(concurrent))

            (concurrent / "README.md").write_text("concurrent\n", encoding="utf-8")
            self.git(concurrent, "config", "user.name", "Test")
            self.git(concurrent, "config", "user.email", "test@example.com")
            self.git(concurrent, "add", "README.md")
            self.git(concurrent, "commit", "-m", "advance master")
            self.git(concurrent, "push", "origin", "master")

            self.write_state_files(runner, '{"recorded": true}\n')
            subprocess.run(
                ["bash", str(COMMIT_SCRIPT)],
                cwd=runner,
                check=True,
                text=True,
                capture_output=True,
                env={
                    "PATH": os.environ["PATH"],
                    "GITHUB_REF_NAME": "master",
                },
            )

            self.git(root, "clone", str(remote), str(verify))
            self.assertEqual(
                "concurrent\n", (verify / "README.md").read_text(encoding="utf-8")
            )
            self.assertEqual(
                {"recorded": True},
                json.loads(
                    (
                        verify / "scripts" / "website" / "syndication-state.json"
                    ).read_text(encoding="utf-8")
                ),
            )

    def test_state_commit_merges_concurrent_platform_updates(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            root = Path(temp_dir)
            remote = root / "remote.git"
            seed = root / "seed"
            runner = root / "runner"
            concurrent = root / "concurrent"
            verify = root / "verify"

            self.git(root, "init", "--bare", "--initial-branch=master", str(remote))
            self.git(root, "clone", str(remote), str(seed))
            self.git(seed, "config", "user.name", "Test")
            self.git(seed, "config", "user.email", "test@example.com")
            self.write_state_files(
                seed,
                '{"posts": {"same-post": {}}}\n',
                '{"tasks": []}\n',
            )
            self.git(seed, "add", ".")
            self.git(seed, "commit", "-m", "initial")
            self.git(seed, "push", "origin", "master")

            self.git(root, "clone", str(remote), str(runner))
            self.git(root, "clone", str(remote), str(concurrent))

            self.write_state_files(
                concurrent,
                json.dumps(
                    {
                        "posts": {
                            "same-post": {
                                "devto": {
                                    "id": 123,
                                    "url": "https://dev.to/same-post",
                                    "syndicated_at": "2026-10-03T17:35:08+00:00",
                                },
                                "hashnode": {
                                    "url": "https://hashnode.example/same-post"
                                }
                            }
                        }
                    }
                )
                + "\n",
                json.dumps(
                    {
                        "tasks": [
                            {
                                "id": "linkedin:same-post",
                                "site": "linkedin",
                            }
                        ]
                    }
                )
                + "\n",
            )
            self.git(concurrent, "config", "user.name", "Test")
            self.git(concurrent, "config", "user.email", "test@example.com")
            self.git(
                concurrent,
                "add",
                "scripts/website/syndication-state.json",
                "scripts/website/syndication-queue.json",
            )
            self.git(concurrent, "commit", "-m", "record hashnode")
            self.git(concurrent, "push", "origin", "master")

            self.write_state_files(
                runner,
                json.dumps(
                    {
                        "posts": {
                            "same-post": {
                                "devto": {
                                    "id": 123,
                                    "url": "https://dev.to/same-post",
                                    "syndicated_at": "2026-10-03T17:35:07Z",
                                    "recovered": True,
                                },
                                "foojay": {"url": "https://foojay.io/same-post"},
                            }
                        }
                    }
                )
                + "\n",
                json.dumps(
                    {
                        "tasks": [
                            {
                                "id": "medium:same-post",
                                "site": "medium",
                            }
                        ]
                    }
                )
                + "\n",
            )
            subprocess.run(
                ["bash", str(COMMIT_SCRIPT)],
                cwd=runner,
                check=True,
                text=True,
                capture_output=True,
                env={
                    "PATH": os.environ["PATH"],
                    "GITHUB_REF_NAME": "master",
                },
            )

            self.git(root, "clone", str(remote), str(verify))
            state = json.loads(
                (verify / "scripts" / "website" / "syndication-state.json").read_text(
                    encoding="utf-8"
                )
            )
            self.assertEqual(
                {"devto", "foojay", "hashnode"},
                set(state["posts"]["same-post"]),
            )
            self.assertEqual("2026-10-03T17:35:08+00:00",
                             state["posts"]["same-post"]["devto"]["syndicated_at"])
            self.assertTrue(state["posts"]["same-post"]["devto"]["recovered"])
            queue = json.loads(
                (verify / "scripts" / "website" / "syndication-queue.json").read_text(
                    encoding="utf-8"
                )
            )
            self.assertEqual(
                {"linkedin:same-post", "medium:same-post"},
                {task["id"] for task in queue["tasks"]},
            )


if __name__ == "__main__":
    unittest.main()
