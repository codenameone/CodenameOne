#!/usr/bin/env python3
"""Search coverage and content extraction against rendered (including minified) HTML."""
import importlib.util
import json
from pathlib import Path
import tempfile
import unittest

spec = importlib.util.spec_from_file_location('search_index', Path(__file__).resolve().parents[2] / 'docs/website/scripts/generate_lunr_index.py')
search = importlib.util.module_from_spec(spec)
spec.loader.exec_module(search)


class SearchIndexTest(unittest.TestCase):
    def test_minified_chapter_omits_navigation(self):
        source = '''<title>Wrong fallback</title><main><aside>All guide chapters</aside>
        <article class="post-single cn1-guide-chapter"><nav>Previous chapter</nav>
        <h1 class=post-title>Build hints reference</h1>
        <details class=cn1-guide-toc>On this page</details>
        <div class=post-content><table><tr><td>android.xpermissions</td>
        <td>Extra manifest permissions &amp; settings</td></tr></table></div>
        <footer>Edit this chapter</footer></article></main>'''
        self.assertEqual(search.extract_title(source), 'Build hints reference')
        self.assertEqual(search.extract_main_content(source), 'Build hints reference android.xpermissions Extra manifest permissions & settings')

    def test_semantic_asides_are_searchable_but_navigation_is_not(self):
        source = '''<main><article class=post-single><h1 class=post-title>Services</h1>
        <aside class=cn1-pricing-commerce>Commerce is optional.</aside>
        <aside class=cn1-signing-note>Know where the key goes</aside>
        <aside class=cn1-guide-sidebar>Guide menu</aside>
        <aside role=navigation>Related chapters</aside></article></main>'''
        self.assertEqual(search.extract_main_content(source),
                         'Services Commerce is optional. Know where the key goes')

    def test_minified_date_and_redirect(self):
        page = search.PageText('<meta property=article:published_time content=2026-10-10T09:00:00Z><meta http-equiv=refresh content="0; url=/developer-guide/">')
        self.assertEqual(page.date, '2026-10-10T09:00:00Z')
        self.assertTrue(page.redirect)

    def test_scopes_and_duplicate_exclusions(self):
        with tempfile.TemporaryDirectory() as tmp:
            original = search.PUBLIC_DIR
            self.addCleanup(setattr, search, 'PUBLIC_DIR', original)
            search.PUBLIC_DIR = Path(tmp)
            routes = ['developer-guide/build-hints', 'blog/build-hints', 'getting-started',
                      'developer-guide/single-page', 'search', 'javadoc', 'backend/javadoc', 'tags/java']
            for route in routes:
                path = Path(tmp) / route / 'index.html'
                path.parent.mkdir(parents=True)
                path.write_text('<article class=post-single><h1 class=post-title>Build hints</h1><p>' + 'Build arguments for Android and iOS. ' * 5 + '</p></article>')
            for route in ('blog', 'blog/page/2'):
                path = Path(tmp) / route / 'index.html'
                path.parent.mkdir(parents=True, exist_ok=True)
                path.write_text('<article class="post-single cn1-blog-index"><h1 class=post-title>Blog</h1>'
                                '<article class=cn1-blog-index__card>' + 'Build arguments for Android and iOS. ' * 5 + '</article></article>')
            payload = search.build_index()
            self.assertEqual({doc['url']: doc['section'] for doc in payload['docs']}, {
                '/developer-guide/build-hints/': 'guide', '/blog/build-hints/': 'blog', '/getting-started/': 'site'})
            output = Path(tmp) / 'lunr-index.json'
            search.write_index(payload, output, budget=400)
            manifest = json.loads(output.read_text())
            reconstructed = [doc for part in manifest['parts'] for doc in json.loads((Path(tmp) / part.lstrip('/')).read_text())['docs']]
            self.assertEqual(reconstructed, payload['docs'])
            self.assertEqual(len(reconstructed), manifest['count'])


if __name__ == '__main__':
    unittest.main()
