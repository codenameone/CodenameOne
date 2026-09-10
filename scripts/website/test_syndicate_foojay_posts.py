#!/usr/bin/env python3
"""Offline regressions for Foojay bundles and retry-safe editorial submission."""

import datetime as dt
import hashlib
import json
import tempfile
import unittest
from pathlib import Path
from unittest.mock import Mock, patch

from syndicate_blog_posts import Post, State, select_candidate
from syndicate_foojay_posts import (
    UPSTREAM, accepts, build_bundle, main, pr_result, read_image, submit_bundle,
)

FRIDAY = dt.date(2026, 9, 4)
TODAY = dt.date(2026, 9, 11)
JPEG = b"\xff\xd8\xff" + b"test-image"


def post(slug="friday", date=FRIDAY):
    return Post(Path(f"{slug}.md"), slug, 'Java: "A useful feature"', date,
                {"description": "A source-grounded feature description.", "author": "Shai Almog"},
                '![A phone beside a laptop](/blog/cover.jpg)\n\n'
                'Intro with a [guide](/guide/).\n\n'
                '## Example\n\n```java\nString example = "![fake](/blog/missing.png)";\n```\n\n'
                '{{< mermaid >}}\ngraph LR\n A --> B\n{{< /mermaid >}}\n\n'
                '---\n\n## Discussion\nWebsite-only comments.')


def pr(state="open", merged_at=None):
    return {"number": 42, "html_url": "https://github.com/foojayio/website/pull/42",
            "state": state, "merged_at": merged_at,
            "head": {"repo": {"full_name": "writer/website"}}}


class BundleTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        (self.root / "blog").mkdir()
        (self.root / "blog/cover.jpg").write_bytes(JPEG)

    def test_bundle_matches_foojay_template_and_preserves_content(self):
        files = build_bundle(post(), [], TODAY, self.root)
        self.assertEqual(2, len(files))
        text = files["draft/friday/index.md"].decode()
        self.assertIn('authors: ["shai-almog"]', text)
        self.assertIn('categories: ["Java"]', text)
        self.assertIn('date: "2026-09-11"', text)
        self.assertIn('canonical: "https://www.codenameone.com/blog/friday/"', text)
        self.assertIn('title: ' + json.dumps(post().title), text)
        self.assertIn('```mermaid\ngraph LR\n A --> B\n```', text)
        self.assertNotIn('mermaid.ink', text)
        self.assertNotIn('{{<', text)
        self.assertNotIn('Website-only comments', text)
        self.assertIn('[guide](https://www.codenameone.com/guide/)', text)
        self.assertIn('![A phone beside a laptop](cover-', text)
        self.assertIn('String example = "![fake]', text)
        self.assertEqual(JPEG, next(data for path, data in files.items() if path.endswith('.jpg')))

    def test_submission_date_is_stable_across_retries(self):
        self.assertEqual(build_bundle(post(), [], TODAY, self.root),
                         build_bundle(post(), [], TODAY + dt.timedelta(days=1), self.root))

    def test_description_is_short_and_yaml_safe(self):
        p = post()
        p.front_matter['description'] = 'Quoted "words": ' + 'many words ' * 30
        text = build_bundle(p, [], TODAY, self.root)['draft/friday/index.md'].decode()
        description = json.loads(next(line.split(': ', 1)[1] for line in text.splitlines()
                                      if line.startswith('description: ')))
        self.assertLess(len(description), 160)
        self.assertTrue(description.endswith('...'))

    def test_same_filename_different_urls_do_not_collide(self):
        (self.root / 'other').mkdir()
        (self.root / 'other/cover.jpg').write_bytes(JPEG + b'2')
        p = post()
        p.body += '\n![Different diagram](/other/cover.jpg)'
        # Append before the website footer, which is intentionally stripped.
        p.body = p.body.replace('---\n\n## Discussion\nWebsite-only comments.', '')
        files = build_bundle(p, [], TODAY, self.root)
        self.assertEqual(3, len(files))

    def test_invalid_input_fails_before_submission(self):
        for slug in ['../escape', 'bad/slug']:
            with self.subTest(slug=slug), self.assertRaises(ValueError):
                build_bundle(post(slug), [], TODAY, self.root)
        p = post()
        p.front_matter['author'] = 'Someone Else'
        with self.assertRaisesRegex(ValueError, 'author mapping'):
            build_bundle(p, [], TODAY, self.root)
        p = post()
        p.body = 'No preview image'
        with self.assertRaisesRegex(ValueError, 'preview image'):
            build_bundle(p, [], TODAY, self.root)
        (self.root / 'blog/cover.jpg').write_bytes(b'<html>not an image</html>')
        with self.assertRaisesRegex(ValueError, 'still JPEG or PNG'):
            build_bundle(post(), [], TODAY, self.root)

    def test_missing_and_oversize_images_fail(self):
        with self.assertRaises(FileNotFoundError):
            read_image('https://www.codenameone.com/blog/missing.jpg', self.root)
        with patch('syndicate_foojay_posts.MAX_IMAGE_BYTES', 3):
            with self.assertRaisesRegex(ValueError, '4 MB'):
                build_bundle(post(), [], TODAY, self.root)

    def test_image_cannot_escape_static_root(self):
        with self.assertRaisesRegex(ValueError, 'escapes'):
            read_image('https://www.codenameone.com/%2e%2e/secret.png', self.root)

    def test_friday_delay_and_legacy_wordpress_state_are_preserved(self):
        friday = post()
        thursday = post('thursday', FRIDAY - dt.timedelta(days=1))
        state = State.load(self.root / 'state.json')
        args = ([thursday, friday], state, ['foojay'])
        floor = dt.date(2026, 4, 30)
        self.assertIsNone(select_candidate(*args, TODAY - dt.timedelta(days=1), floor, 7,
                                           platform_filters={'foojay': accepts}))
        self.assertEqual(friday, select_candidate(*args, TODAY, floor, 7,
                                                  platform_filters={'foojay': accepts}))
        state.record('friday', 'foojay', {'url': 'https://foojay.io/wp-admin/post.php?post=123'})
        self.assertIsNone(select_candidate(*args, TODAY, floor, 7,
                                           platform_filters={'foojay': accepts}))


class SubmissionTest(unittest.TestCase):
    def fake(self, *, existing=None, duplicate=False, ref=None, branch_tree=None):
        github = Mock()

        def api(endpoint, payload=None, **kwargs):
            if '/pulls?' in endpoint:
                return existing or []
            if payload is not None:
                if endpoint.endswith('/pulls'):
                    return pr()
                return {'sha': 'created'}
            if endpoint == 'repos/' + UPSTREAM:
                return {'default_branch': 'main'}
            if endpoint == 'repos/writer/website':
                return {'permissions': {'push': True}, 'source': {'full_name': UPSTREAM}}
            if '/commits/main' in endpoint:
                return {'sha': 'base', 'commit': {'tree': {'sha': 'tree'}}}
            if '/contents/content/authors/' in endpoint:
                return {'name': '_index.md'}
            if '/git/trees/tree?' in endpoint:
                return {'tree': [{'path': 'content/posts/2026/09/01/friday/index.md'}] if duplicate else []}
            if '/git/ref/heads/' in endpoint:
                return ref
            if '/git/commits/retry' in endpoint:
                return {'tree': {'sha': 'retry'}}
            if '/git/trees/retry?' in endpoint:
                return branch_tree
            if '/compare/' in endpoint:
                return {'files': [{'filename': 'draft/friday/index.md', 'status': 'added'}]}
            raise AssertionError(f'Unexpected API call: {endpoint}')
        github.api.side_effect = api
        return github

    def test_existing_pr_recovers_lost_state_without_writes(self):
        github = self.fake(existing=[pr()])
        result = submit_bundle(post(), {}, 'writer/website', github)
        self.assertEqual('submitted', result['status'])
        self.assertFalse(result['published'])
        self.assertEqual(1, github.api.call_count)

    def test_closed_unmerged_pr_requires_review(self):
        github = self.fake(existing=[pr('closed')])
        with self.assertRaisesRegex(RuntimeError, 'closed without merging'):
            submit_bundle(post(), {}, 'writer/website', github)
        self.assertEqual(1, github.api.call_count)

    def test_merged_draft_is_not_claimed_public(self):
        result = pr_result(pr('closed', '2026-09-11T12:00:00Z'))
        self.assertEqual('merged', result['status'])
        self.assertFalse(result['published'])

    def test_new_submission_uses_atomic_bundle_commit_and_regular_pr(self):
        github = self.fake()
        files = {'draft/friday/index.md': b'article', 'draft/friday/cover.jpg': JPEG}
        result = submit_bundle(post(), files, 'writer/website', github)
        self.assertEqual('submitted', result['status'])
        writes = [call.args for call in github.api.call_args_list if len(call.args) > 1]
        self.assertEqual(['blobs', 'blobs', 'trees', 'commits', 'refs', 'pulls'],
                         [args[0].rsplit('/', 1)[1] for args in writes])
        self.assertEqual('tree', writes[2][1]['base_tree'])
        self.assertEqual(set(files), {entry['path'] for entry in writes[2][1]['tree']})
        self.assertEqual(['base'], writes[3][1]['parents'])
        self.assertEqual('refs/heads/cn1-syndication/friday', writes[4][1]['ref'])
        self.assertFalse(writes[-1][1]['draft'])
        self.assertEqual('writer:cn1-syndication/friday', writes[-1][1]['head'])
        self.assertIn(post().canonical_url, writes[-1][1]['body'])

    def test_duplicate_migrated_article_prevents_writes(self):
        github = self.fake(duplicate=True)
        with self.assertRaisesRegex(RuntimeError, 'already contains'):
            submit_bundle(post(), {}, 'writer/website', github)
        self.assertTrue(all(len(call.args) == 1 for call in github.api.call_args_list))

    def test_partial_branch_is_not_submitted_or_overwritten(self):
        github = self.fake(ref={'object': {'sha': 'retry'}}, branch_tree={'tree': []})
        with self.assertRaisesRegex(RuntimeError, 'differs'):
            submit_bundle(post(), {'draft/friday/index.md': b'article'}, 'writer/website', github)
        self.assertTrue(all(len(call.args) == 1 for call in github.api.call_args_list))

    def test_complete_branch_recovers_crash_before_pr(self):
        data = b'article'
        blob_sha = hashlib.sha1(b'blob 7\0' + data).hexdigest()
        github = self.fake(ref={'object': {'sha': 'retry'}}, branch_tree={'tree': [
            {'path': 'draft/friday/index.md', 'sha': blob_sha, 'type': 'blob'}]})
        submit_bundle(post(), {'draft/friday/index.md': data}, 'writer/website', github)
        writes = [call.args[0] for call in github.api.call_args_list if len(call.args) > 1]
        self.assertEqual(['repos/' + UPSTREAM + '/pulls'], writes)

    def test_branch_with_unrelated_changes_is_not_submitted(self):
        data = b'article'
        blob_sha = hashlib.sha1(b'blob 7\0' + data).hexdigest()
        github = self.fake(ref={'object': {'sha': 'retry'}}, branch_tree={'tree': [
            {'path': 'draft/friday/index.md', 'sha': blob_sha, 'type': 'blob'}]})
        normal = github.api.side_effect
        github.api.side_effect = lambda endpoint, *args, **kwargs: (
            {'files': [{'filename': 'hugo.toml', 'status': 'modified'}]}
            if '/compare/' in endpoint else normal(endpoint, *args, **kwargs))
        with self.assertRaisesRegex(RuntimeError, 'outside the article bundle'):
            submit_bundle(post(), {'draft/friday/index.md': data}, 'writer/website', github)
        self.assertTrue(all(len(call.args) == 1 for call in github.api.call_args_list))

    def test_failed_submission_leaves_state_unchanged(self):
        with tempfile.TemporaryDirectory() as directory:
            state_path = Path(directory) / 'state.json'
            State(raw={'posts': {}}).save(state_path)
            before = state_path.read_bytes()
            with patch('syndicate_foojay_posts.discover_posts', return_value=[post()]), \
                 patch.dict('os.environ', {'GITHUB_ACTIONS': 'false'}), \
                 patch('syndicate_foojay_posts.build_bundle', return_value={}), \
                 patch('syndicate_foojay_posts.submit_bundle', side_effect=RuntimeError('API failure')):
                self.assertEqual(2, main(['--submit', '--today', str(TODAY), '--state-file', str(state_path)]))
            self.assertEqual(before, state_path.read_bytes())

    def test_success_records_pr_and_preserves_other_platforms(self):
        with tempfile.TemporaryDirectory() as directory:
            state_path = Path(directory) / 'state.json'
            State(raw={'posts': {'friday': {'devto': {'url': 'https://dev.to/example'}}}}).save(state_path)
            with patch('syndicate_foojay_posts.discover_posts', return_value=[post()]), \
                 patch.dict('os.environ', {'GITHUB_ACTIONS': 'false'}), \
                 patch('syndicate_foojay_posts.build_bundle', return_value={}), \
                 patch('syndicate_foojay_posts.submit_bundle', return_value=pr_result(pr())):
                self.assertEqual(0, main(['--submit', '--today', str(TODAY), '--state-file', str(state_path)]))
            saved = State.load(state_path).raw['posts']['friday']
            self.assertEqual('https://dev.to/example', saved['devto']['url'])
            self.assertEqual('submitted', saved['foojay']['status'])
            self.assertFalse(saved['foojay']['published'])

    def test_ci_missing_token_fails_before_preparing_or_submitting(self):
        with tempfile.TemporaryDirectory() as directory, \
             patch('syndicate_foojay_posts.discover_posts', return_value=[post()]), \
             patch.dict('os.environ', {'GITHUB_ACTIONS': 'true', 'FOOJAY_GITHUB_TOKEN': ''}), \
             patch('syndicate_foojay_posts.build_bundle') as bundle, \
             patch('syndicate_foojay_posts.GitHub') as github:
            self.assertEqual(2, main(['--submit', '--today', str(TODAY),
                                      '--state-file', str(Path(directory) / 'state.json')]))
            bundle.assert_not_called()
            github.assert_not_called()

    def test_dry_run_never_reads_images_or_calls_github(self):
        with tempfile.TemporaryDirectory() as directory, \
             patch('syndicate_foojay_posts.discover_posts', return_value=[post()]), \
             patch('syndicate_foojay_posts.build_bundle') as bundle, \
             patch('syndicate_foojay_posts.GitHub') as github:
            path = Path(directory) / 'state.json'
            self.assertEqual(0, main(['--dry-run', '--today', str(TODAY), '--state-file', str(path)]))
            bundle.assert_not_called()
            github.assert_not_called()
            self.assertFalse(path.exists())


if __name__ == '__main__':
    unittest.main()
