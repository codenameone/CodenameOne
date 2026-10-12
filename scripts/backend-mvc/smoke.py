#!/usr/bin/env python3
"""Exercise the running demo over HTTP, with ordinary forms and htmx requests."""
import html.parser
import http.cookiejar
import json
import sys
import urllib.error
import urllib.parse
import urllib.request
import uuid


class Page(html.parser.HTMLParser):
    def __init__(self, text):
        super().__init__()
        self.token = None
        self.csrf_headers = None
        self.links = []
        self.feed(text)

    def handle_starttag(self, tag, attrs):
        attrs = dict(attrs)
        if "hx-headers" in attrs:
            self.csrf_headers = json.loads(attrs["hx-headers"])
        if tag == "input" and attrs.get("name") == "_csrf":
            self.token = attrs["value"]
        if tag == "a" and attrs.get("href", "").startswith("/products/"):
            self.links.append(attrs["href"])


base = sys.argv[1] if len(sys.argv) > 1 else "http://127.0.0.1:8080"
client = urllib.request.build_opener(
    urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()))


def request(path, data=None, hx=False, csrf_headers=None):
    headers = {"HX-Request": "true"} if hx else {}
    headers.update(csrf_headers or {})
    payload = urllib.parse.urlencode(data).encode() if data is not None else None
    req = urllib.request.Request(base + path, data=payload, headers=headers)
    try:
        response = client.open(req)
    except urllib.error.HTTPError as error:
        response = error
    return response.status, response.read().decode(), response.headers


def submit(path, data, page, hx):
    security = Page(page)
    if hx:
        assert security.csrf_headers, page
        assert security.csrf_headers.get("X-CSRF-TOKEN") == security.token, page
        # Exercise header-only CSRF, as standalone htmx controls need it.
        return request(path, data, hx, security.csrf_headers)
    data["_csrf"] = security.token
    return request(path, data, hx)


for hx in (False, True):
    status, page, _ = request("/products/new", hx=hx)
    assert status == 200 and Page(page).token, (status, page)
    assert ("<html" not in page) if hx else ("<html" in page)
    status, _, _ = request("/products", {"name": "forbidden", "quantity": "2"}, hx)
    assert status == 403, status
    status, invalid, _ = submit("/products", {"name": "", "quantity": "bad"}, page, hx)
    assert status == 200 and 'value="bad"' in invalid and "Invalid value" in invalid, invalid
    assert "Enter a product name" in invalid
    name = "MVC-" + uuid.uuid4().hex[:8] + " <&😀>"
    status, listing, _ = submit("/products", {
        "name": name, "quantity": "3", "_active": "on", "active": "true"}, invalid, hx)
    assert status == 200 and "&lt;&amp;😀&gt;" in listing, listing
    assert ("<html" not in listing) if hx else ("<html" in listing)
    edit = [link for link in Page(listing).links if link != "/products/new"][-1]
    status, page, _ = request(edit, hx=hx)
    assert status == 200 and "Edit product" in page
    status, listing, _ = submit(edit, {
        "name": "Updated " + name, "quantity": "4", "_active": "on"}, page, hx)
    assert status == 200 and "Updated MVC-" in listing and "Inactive" in listing
    status, listing, _ = submit(edit + "/delete", {}, listing, hx)
    assert status == 200 and "Updated " + name.split(" ")[0] not in listing
    assert request(edit)[0] == 404
    print("PASS", "htmx fragments" if hx else "ordinary forms", "CRUD, validation, Unicode, CSRF")

status, javascript, headers = request("/static/htmx-2.0.11.min.js")
assert status == 200 and len(javascript) > 50000 and "javascript" in headers["Content-Type"]
assert request("/templates/edit.html")[0] == 404
assert request("/static/../templates/edit.html")[0] in (400, 404)
print("PASS packaged assets and private templates")
