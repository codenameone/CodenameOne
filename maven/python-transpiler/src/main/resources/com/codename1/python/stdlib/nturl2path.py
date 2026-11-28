# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""nturl2path: the Windows forms of url2pathname and pathname2url.

Present so code that converts a Windows file URL by name (and tests that
simulate Windows on another host) can import it; urllib.request uses the
POSIX forms on every target Codename One runs on."""

from urllib.parse import quote, unquote


def url2pathname(url):
    """The Windows path of a file URL's path: an empty or localhost
    authority is dropped, a drive's '|' becomes ':', '/' becomes '\\', and
    the result is percent-decoded after that, as CPython's is."""
    if url[:3] == "///":
        url = url[2:]
    elif url[:12] == "//localhost/":
        url = url[11:]
    if url[:3] == "///":
        url = url[1:]
    else:
        if url[:1] == "/" and url[2:3] in (":", "|"):
            url = url[1:]
        if url[1:2] == "|":
            url = url[:1] + ":" + url[2:]
    return unquote(url.replace("/", "\\"))


def pathname2url(p):
    p = p.replace("\\", "/")
    if p[:4] == "//?/":
        p = p[4:]
        if p[:4].upper() == "UNC/":
            p = "//" + p[4:]
    if p[1:2] == ":":
        return quote("///" + p[:2], safe="/:") + quote(p[2:])
    if p[:1] == "/" and p[:2] != "//":
        return "//" + quote(p)
    return quote(p)
