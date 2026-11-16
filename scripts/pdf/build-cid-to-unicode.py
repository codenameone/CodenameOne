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
"""Builds CodenameOne/src/com/codename1/impl/pdf/CidToUnicodeData.java, the CID to
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
                       "CidToUnicodeData.java")
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
    with open(out, "w") as f:
        f.write(java_source(zlib.compress(bytes(body), 9)))
    print(out, os.path.getsize(out))


HEADER_FILE = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "CodenameOne", "src",
                           "com", "codename1", "impl", "pdf", "CidToUnicode.java")


def escape(v):
    if v == 0x22:
        return '\\"'
    if v == 0x5C:
        return "\\\\"
    if v == 0x0A:
        return "\\n"
    if v == 0x0D:
        return "\\r"
    # Any other control char as a unicode escape. LF and CR cannot be: javac
    # turns an escape into its char before it reads the string, which would
    # end the literal on a line break.
    if 0x20 <= v < 0x7F:
        return chr(v)
    return "\\u%04x" % v


def java_source(data):
    """The compressed table as string constants, so it is a class: stripped with
    the rest of the PDF code from an application that never reads a PDF, where a
    resource would ship in every application. Each char carries 7 bits of the
    data, most significant first, so nearly every char is one byte in the class
    file's constant pool (a char past 127 would take two). 30000 chars per
    constant stays below the 65535 byte limit even if every one were NUL."""
    with open(HEADER_FILE) as f:
        header = f.read().split("package ")[0]
    bits = "".join("{:08b}".format(b) for b in data)
    bits += "0" * (-len(bits) % 7)
    chars = [int(bits[i:i + 7], 2) for i in range(0, len(bits), 7)]
    step = 30000
    count = (len(chars) + step - 1) // step
    lines = [header + "package com.codename1.impl.pdf;", "",
             "/// Generated by `scripts/pdf/build-cid-to-unicode.py` from Adobe's CMap",
             "/// resources (BSD 3-clause; see the NOTICE file): the deflated CID to",
             "/// Unicode table [CidToUnicode] reads, 7 bits per char, most significant",
             "/// first. Do not edit.",
             "@com.codename1.impl.SharedWithBackend",
             "final class CidToUnicodeData {",
             "    /// The length of the deflated table in bytes.",
             "    static final int LENGTH = %d;" % len(data), "",
             "    private CidToUnicodeData() {",
             "    }", "",
             "    /// The packed table, in pieces to concatenate.",
             "    static String[] chunks() {",
             "        return new String[] {",
             ",\n".join("            chunk%d()" % i for i in range(count)),
             "        };",
             "    }"]
    for i in range(count):
        part = chars[i * step:(i + 1) * step]
        lines += ["", "    private static String chunk%d() {" % i, '        return ""']
        for r in range(0, len(part), 72):
            lines.append('                + "' + "".join(escape(v) for v in part[r:r + 72]) + '"')
        lines[-1] += ";"
        lines.append("    }")
    lines += ["}", ""]
    return "\n".join(lines)

if __name__ == "__main__":
    main()
