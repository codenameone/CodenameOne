#!/usr/bin/env python3
#
# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
# This code is free software; you can redistribute it and/or modify it
# under the terms of the GNU General Public License version 2 only, as
# published by the Free Software Foundation.  Codename One designates this
# particular file as subject to the "Classpath" exception as provided
# by Oracle in the LICENSE file that accompanied this code.
#
# This code is distributed in the hope that it will be useful, but WITHOUT
# ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
# FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
# version 2 for more details (a copy is included in the LICENSE file that
# accompanied this code).
#
# You should have received a copy of the GNU General Public License version
# 2 along with this work; if not, write to the Free Software Foundation,
# Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
#
# Please contact Codename One through http://www.codenameone.com/ if you
# need additional information or have any questions.
"""Rejects @author and @version tags in Java documentation comments.

Neither says anything about the API, and both do damage. A block tag's body
runs to the next tag or to the end of the comment, so documentation written
after an @author line became part of the tag -- and the website's doclet drops
@author, so it vanished: 90 published comments had lost a "See also" or a
"Deprecated" section that way. The version is what git is for.

Doc comments only, in both styles: /// lines, and lines inside /** */. A plain
/* */ comment is left alone, because that is where vendored code keeps its
license header, and those headers (MiG Layout's BSD notice, for one) carry an
@author line that is part of the notice rather than documentation.

No baseline and no exclusions: there is no reason to keep one.

Usage:
  scripts/check-author-tags.py            # every tracked .java file
  scripts/check-author-tags.py PATH ...   # just these
"""
import re
import subprocess
import sys

TAG = re.compile(r'@(author|version)\b')
MARKDOWN_TAG = re.compile(r'^\s*///\s*@(author|version)\b')
DOC_BLOCK = re.compile(r'/\*\*(?!/)(?:(?!\*/).)*\*/', re.S)
DOC_LINE_TAG = re.compile(r'^\s*(?:/\*\*|\*)?\s*@(author|version)\b')


def findings(path):
    try:
        with open(path, 'rb') as source:
            text = source.read().decode('latin-1')
    except OSError as err:
        return [(0, f'unreadable: {err}')]
    out = []
    for number, line in enumerate(text.split('\n'), 1):
        if MARKDOWN_TAG.match(line):
            out.append((number, line.strip()))
    for block in DOC_BLOCK.finditer(text):
        if not TAG.search(block.group(0)):
            continue
        first = text.count('\n', 0, block.start()) + 1
        for offset, line in enumerate(block.group(0).split('\n')):
            if DOC_LINE_TAG.match(line):
                out.append((first + offset, line.strip()))
    return sorted(out)


def main(argv):
    paths = argv[1:] or subprocess.check_output(
        ['git', 'ls-files', '*.java'], text=True).split()
    paths = [p for p in paths if p.endswith('.java')]
    total = 0
    for path in paths:
        for number, line in findings(path):
            print(f'{path}:{number}: {line}')
            total += 1
    if total:
        print(f'\ncheck-author-tags: {total} @author/@version tag(s) in documentation '
              'comments. Delete them; history is in git, and text written after one '
              'is swallowed into it and never published.', file=sys.stderr)
        return 1
    print(f'check-author-tags: {len(paths)} file(s) clean.')
    return 0


if __name__ == '__main__':
    sys.exit(main(sys.argv))
