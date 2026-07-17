#!/usr/bin/env python3

import datetime as dt
import importlib.util
import os
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path
from unittest import mock


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


class StateCommitRaceTest(unittest.TestCase):
    def git(self, cwd, *args):
        return subprocess.run(
            ["git", *args],
            cwd=cwd,
            check=True,
            text=True,
            capture_output=True,
        )

    def write_state_files(self, repo, state_text="{}\n"):
        website = repo / "scripts" / "website"
        website.mkdir(parents=True, exist_ok=True)
        (website / "syndication-state.json").write_text(state_text, encoding="utf-8")
        (website / "syndication-queue.json").write_text("{}\n", encoding="utf-8")

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
                '{"recorded": true}\n',
                (verify / "scripts" / "website" / "syndication-state.json").read_text(
                    encoding="utf-8"
                ),
            )


if __name__ == "__main__":
    unittest.main()
