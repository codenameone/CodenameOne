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
"""Builds CodenameOne/src/com/codename1/impl/pdf/cid2unicode.bin, the CID to
Unicode tables of the Adobe character collections the PDF text extractor uses
for CID fonts that carry no ToUnicode map.

Input: the cidToUnicode directory of poppler-data (Adobe's CMap resources,
BSD 3-clause; see NOTICE), one line per CID with the Unicode value in hex,
0000 for none. Usage:

    scripts/pdf/build-cid-to-unicode.py /opt/homebrew/share/poppler/cidToUnicode

Format, zlib compressed: a byte with the collection count, then per
collection its name as a byte length plus ASCII, the CID count and the byte
length of its run data as 4-byte big-endian integers, then the runs. A run is
two varints (7 bits a byte, low first): the zigzag-coded difference between
its first Unicode value and the end of the previous run, and its length. Each
run maps consecutive CIDs to consecutive code points; a value of 0 maps
nothing.
"""
import os
import struct
import sys
import zlib

COLLECTIONS = ["Adobe-Japan1", "Adobe-GB1", "Adobe-CNS1", "Adobe-Korea1"]


def varint(b, x):
    while x >= 0x80:
        b.append((x & 0x7F) | 0x80)
        x >>= 7
    b.append(x)


def main():
    src = sys.argv[1]
    here = os.path.dirname(os.path.abspath(__file__))
    out = os.path.join(here, "..", "..", "CodenameOne", "src", "com", "codename1", "impl", "pdf",
                       "cid2unicode.bin")
    body = bytearray([len(COLLECTIONS)])
    for name in COLLECTIONS:
        values = []
        with open(os.path.join(src, name)) as f:
            for line in f:
                line = line.strip()
                values.append(int(line.split()[0], 16) if line else 0)
        runs = bytearray()
        prev = 0
        i = 0
        while i < len(values):
            j = i
            while values[i] != 0 and j + 1 < len(values) and values[j + 1] == values[j] + 1:
                j += 1
            delta = values[i] - prev
            varint(runs, (delta << 1) if delta >= 0 else ((-delta << 1) - 1))
            varint(runs, j - i + 1)
            prev = values[i] + (j - i + 1)
            i = j + 1
        body.append(len(name))
        body += name.encode("ascii")
        body += struct.pack(">II", len(values), len(runs))
        body += runs
    with open(out, "wb") as f:
        f.write(zlib.compress(bytes(body), 9))
    print(out, os.path.getsize(out))


if __name__ == "__main__":
    main()
