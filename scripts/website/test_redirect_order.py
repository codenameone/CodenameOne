#!/usr/bin/env python3
"""Guard the Pages redirect ordering before the expensive website build."""
import pathlib
import tempfile
import unittest

from test_redirects import DEFAULT_REDIRECTS_FILE, parse_redirects_file


class RedirectOrderTest(unittest.TestCase):
    def parse(self, contents):
        with tempfile.TemporaryDirectory() as directory:
            path = pathlib.Path(directory) / '_redirects'
            path.write_text(contents, encoding='utf-8')
            return parse_redirects_file(path)

    def test_repository_rules(self):
        self.assertTrue(parse_redirects_file(DEFAULT_REDIRECTS_FILE))

    def test_static_then_dynamic_rules(self):
        rules = self.parse(
            '# Comment\n'
            '/manual/edt.html /developer-guide/the-edt-event-dispatch-thread/ 301\n'
            '/download https://example.com/file.zip 302\n'
            '\n/manual/* /developer-guide/ 301\n'
            '/blog/:slug /articles/:slug/ 301\n'
        )
        self.assertEqual(len(rules), 4)

    def test_catch_all_before_static_rules(self):
        with self.assertRaisesRegex(ValueError, 'static redirect /videos follows dynamic redirect /manual/\\*'):
            self.parse('/manual/* /developer-guide/ 301\n/videos /videos/ 301\n')

    def test_placeholder_before_static_rules(self):
        with self.assertRaisesRegex(ValueError, 'keep all static redirects before dynamic redirects'):
            self.parse('/blog/:slug /articles/:slug/ 301\n/videos /videos/ 301\n')


if __name__ == '__main__':
    unittest.main()
