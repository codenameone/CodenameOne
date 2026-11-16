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
import sys, os, re
src = sys.argv[1]; out = sys.argv[2]
afmdir = os.path.join(src, 'org/apache/pdfbox/resources/afm')
gl = {}
for line in open(os.path.join(src, 'org/apache/pdfbox/resources/glyphlist/glyphlist.txt')):
    if line.startswith('#') or ';' not in line: continue
    n, c = line.strip().split(';')
    cs = c.split(' ')
    if len(cs) == 1:
        u = int(cs[0], 16)
        gl.setdefault(u, []).append(n)
notice = re.sub(r'<[^>]*>', '', open(os.path.join(afmdir, 'MustRead.html')).read())
notice = ' '.join(notice.split())
m = re.search(r'This file and the 14.*?AFM files\.', notice)
notice = m.group(0)
fonts = ['Helvetica','Helvetica-Bold','Helvetica-Oblique','Helvetica-BoldOblique',
 'Times-Roman','Times-Bold','Times-Italic','Times-BoldItalic',
 'Courier','Courier-Bold','Courier-Oblique','Courier-BoldOblique','Symbol','ZapfDingbats']
rows = []
for f in fonts:
    met = {}; bycode = {}; hdr = {}
    for line in open(os.path.join(afmdir, f + '.afm'), encoding='latin-1'):
        line = line.strip()
        if line.startswith('C '):
            parts = dict(p.strip().split(' ', 1) for p in line.split(';') if p.strip() and ' ' in p.strip())
            code = int(parts['C']); w = int(parts['WX']); n = parts['N'].strip()
            met[n] = w
            if code >= 0: bycode[code] = w
        else:
            k = line.split(' ', 1)
            if len(k) == 2: hdr[k[0]] = k[1].strip()
    widths = []
    for code in range(256):
        if f in ('Symbol', 'ZapfDingbats'):
            widths.append(bycode.get(code, 0)); continue
        try:
            u = ord(bytes([code]).decode('cp1252'))
        except UnicodeDecodeError:
            widths.append(0); continue
        if code < 32:
            widths.append(0); continue
        w = 0
        for n in gl.get(u, []):
            if n in met: w = met[n]; break
        if code == 0xA0: w = met.get('space', 0)
        if code == 0xAD: w = met.get('hyphen', 0)
        widths.append(w)
    bbox = hdr['FontBBox'].split()
    def g(k, d): return hdr.get(k, d)
    rows.append((f, g('Ascender', bbox[3]), g('Descender', bbox[1]), g('CapHeight', bbox[3]),
                 g('XHeight', '0'), g('ItalicAngle', '0'), ' '.join(bbox), widths))
L = []
L.append(open(os.path.join(os.path.dirname(out) if False else sys.argv[3])).read().rstrip())
L.append('package com.codename1.impl.pdf;')
L.append('')
L.append('/// Glyph metrics of the 14 standard PDF fonts, generated from Adobe\'s')
L.append('/// AFM files by `scripts/pdf/afm2java.py`. MODIFIED from the originals:')
L.append('/// only the advance widths (indexed by WinAnsiEncoding code, or by the')
L.append('/// built-in code for Symbol and ZapfDingbats) and the header metrics are')
L.append('/// kept. The AFM notice, which is not modified, reads:')
L.append('///')
L.append('/// Copyright (c) 1985, 1987, 1989, 1990, 1991, 1992, 1993, 1997 Adobe Systems')
L.append('/// Incorporated. All Rights Reserved.')
L.append('///')
words = notice.split(' '); line = '///'
for w in words:
    if len(line) + len(w) + 1 > 78: L.append(line); line = '///'
    line += ' ' + w
L.append(line)
L.append('@com.codename1.impl.SharedWithBackend')
L.append('public final class StandardFontMetrics {')
L.append('    static final String[] NAMES = {')
L.append(',\n'.join('        "%s"' % r[0] for r in rows))
L.append('    };')
L.append('')
L.append('    /// Per font: ascender, descender, cap height, x height, italic angle,')
L.append('    /// then the four bounding box values.')
L.append('    static final int[][] HEADER = {')
L.append(',\n'.join('        {%s, %s, %s, %s, %s, %s}' % (r[1], r[2], r[3], r[4], int(float(r[5])), ', '.join(r[6].split())) for r in rows))
L.append('    };')
L.append('')
L.append('    /// Per font: 256 advance widths in 1/1000 em, comma separated.')
L.append('    static final String[] WIDTHS = {')
L.append(',\n'.join('        "%s"' % ','.join(str(x) for x in r[7]) for r in rows))
L.append('    };')
L.append('')
L.append('    private StandardFontMetrics() {')
L.append('    }')
L.append('}')
open(out, 'w').write('\n'.join(L) + '\n')
