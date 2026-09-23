#!/usr/bin/env python3
"""Validate rendered guide routes, assets, cross references and website links."""
import argparse
from html.parser import HTMLParser
import json
from pathlib import Path
from urllib.parse import unquote, urljoin, urlsplit


class Document(HTMLParser):
    def __init__(self, text):
        super().__init__()
        self.ids = set()
        self.links = []
        self.images = []
        self.feed(text)

    def handle_starttag(self, tag, attrs):
        attrs = dict(attrs)
        if 'id' in attrs:
            self.ids.add(attrs['id'])
        if tag == 'a' and 'href' in attrs:
            self.links.append(attrs['href'])
        if tag == 'img' and 'src' in attrs:
            self.images.append(attrs['src'])


def validate(root):
    cache, errors = {}, set()

    def document(path):
        if path not in cache:
            cache[path] = Document(path.read_text(encoding='utf-8'))
        return cache[path]

    def check(source, link, image=False):
        try:
            url = urlsplit(urljoin('/' + str(source.relative_to(root)), link))
        except ValueError:
            return  # Unrelated example URLs may deliberately contain placeholders.
        if url.netloc not in ('', 'www.codenameone.com', 'codenameone.com'):
            return
        if not url.path.startswith('/developer-guide/'):
            return
        path = root / unquote(url.path.lstrip('/'))
        if not image:
            path = path / 'index.html' if path.suffix == '' else path
        if not path.is_file():
            errors.add((str(source.relative_to(root)), link, 'missing file'))
        elif not image and url.fragment and unquote(url.fragment) not in document(path).ids:
            errors.add((str(source.relative_to(root)), link, 'missing anchor'))

    for page in root.rglob('*.html'):
        doc = document(page)
        for link in doc.links:
            check(page, link)
        if 'developer-guide' in page.relative_to(root).parts:
            for image in doc.images:
                check(page, image, image=True)

    routes = json.loads((root / 'developer-guide/anchors.json').read_text())
    for target in routes.values():
        check(root / 'developer-guide/anchors.json', target)
    legacy = json.loads(Path(__file__).with_name('guide-legacy-links.json').read_text())
    redirects = {}
    for line in (root / '_redirects').read_text().splitlines():
        fields = line.split()
        if len(fields) == 3 and not line.startswith('#'):
            redirects[fields[0]] = fields[1:]
    for old, current in legacy['chapters'].items():
        target = f'/developer-guide/{current}/'
        check(root / '_redirects', target)
        for suffix in ('', '/', '.html', '.html/'):
            source = f'/manual/{old}{suffix}'
            if redirects.get(source) != [target, '301']:
                errors.add(('_redirects', source, f'expected 301 to {target}'))
    index_path = root / 'lunr-index.json'
    if index_path.exists():
        indexed = {entry['url'] for entry in json.loads(index_path.read_text())['docs']}
        chapters = {url.split('#')[0] for url in routes.values()} - {'/developer-guide/'}
        for url in chapters - indexed:
            errors.add(('lunr-index.json', url, 'chapter is not searchable'))
        if '/developer-guide/single-page/' in indexed:
            errors.add(('lunr-index.json', '/developer-guide/single-page/', 'duplicate full book is indexed'))
    for error in sorted(errors):
        print(': '.join(error))
    if errors:
        raise SystemExit(f'{len(errors)} broken guide links or assets')
    print(f'Guide links and assets verified, including {len(routes)} bookmark routes.')


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('public', type=Path)
    validate(parser.parse_args().public.resolve())
