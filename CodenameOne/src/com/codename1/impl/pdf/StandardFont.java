/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codename1.impl.pdf;

import com.codename1.pdf.PdfFont;
import java.io.IOException;

/// One of the 14 standard fonts, drawn in WinAnsiEncoding (Symbol and
/// ZapfDingbats in their built-in encodings).
@com.codename1.impl.SharedWithBackend
public final class StandardFont extends PdfFont {
    /// Unicode of WinAnsiEncoding codes 0x80 to 0x9F; 0 where undefined.
    private static final char[] WIN_ANSI_80 = {
        0x20ac, 0, 0x201a, 0x0192, 0x201e, 0x2026, 0x2020, 0x2021,
        0x02c6, 0x2030, 0x0160, 0x2039, 0x0152, 0, 0x017d, 0,
        0, 0x2018, 0x2019, 0x201c, 0x201d, 0x2022, 0x2013, 0x2014,
        0x02dc, 0x2122, 0x0161, 0x203a, 0x0153, 0, 0x017e, 0x0178};

    private final int index;
    private int[] widths;

    /// Font `index` of [StandardFontMetrics#NAMES].
    public StandardFont(int index) {
        this.index = index;
    }

    private boolean isSymbolic() {
        return index >= 12;
    }

    private int[] widths() {
        int[] w = widths;
        if (w == null) {
            w = widthTable(index);
            widths = w;
        }
        return w;
    }

    private static final int[][] TABLES = new int[14][];

    /// The 256 widths, by WinAnsi code (built-in code for the symbolic
    /// fonts), of standard font `index`. Benign race: every thread
    /// computes the same table.
    static int[] widthTable(int index) {
        int[] w = TABLES[index];
        if (w == null) {
            w = new int[256];
            String s = StandardFontMetrics.WIDTHS[index];
            int pos = 0;
            for (int i = 0; i < 256; i++) {
                int end = s.indexOf(',', pos);
                if (end < 0) {
                    end = s.length();
                }
                w[i] = Integer.parseInt(s.substring(pos, end));
                pos = end + 1;
            }
            TABLES[index] = w;
        }
        return w;
    }

    /// The byte that draws `c`, or -1.
    int code(char c) {
        if (isSymbolic()) {
            return c < 256 && widths()[c] > 0 ? c : -1;
        }
        if (c >= 0x20 && c < 0x7f || c >= 0xa0 && c <= 0xff) {
            return c;
        }
        for (int i = 0; i < WIN_ANSI_80.length; i++) {
            if (WIN_ANSI_80[i] == c) {
                return 0x80 + i;
            }
        }
        return -1;
    }

    private int codeOrFallback(char c) {
        int code = code(c);
        return code >= 0 ? code : (isSymbolic() ? 0x20 : '?');
    }

    @Override
    public String getName() {
        return StandardFontMetrics.NAMES[index];
    }

    @Override
    public float getStringWidth(String text, float size) {
        int[] w = widths();
        int total = 0;
        int len = text.length();
        for (int i = 0; i < len; i++) {
            total += w[codeOrFallback(text.charAt(i))];
        }
        return total * size / 1000f;
    }

    @Override
    public float getAscent(float size) {
        return StandardFontMetrics.HEADER[index][0] * size / 1000f;
    }

    @Override
    public float getDescent(float size) {
        return -StandardFontMetrics.HEADER[index][1] * size / 1000f;
    }

    @Override
    public boolean canDisplay(char c) {
        return code(c) >= 0;
    }

    @Override
    protected FontState createState() {
        return new FontState() {
            @Override
            public void showText(StringBuilder content, String text) {
                StringBuilder bytes = new StringBuilder(text.length());
                int len = text.length();
                for (int i = 0; i < len; i++) {
                    bytes.append((char) codeOrFallback(text.charAt(i)));
                }
                PdfFormat.literal(content, bytes.toString());
            }

            @Override
            public void write(PdfWriter writer) throws IOException {
                StringBuilder sb = new StringBuilder("<</Type /Font /Subtype /Type1 /BaseFont ");
                PdfFormat.name(sb, getName());
                if (!isSymbolic()) {
                    sb.append(" /Encoding /WinAnsiEncoding");
                }
                sb.append(">>");
                writer.object(objectNumber, sb.toString());
            }
        };
    }
}
