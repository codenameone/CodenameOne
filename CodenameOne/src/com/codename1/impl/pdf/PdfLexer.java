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

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

/// Reads PDF objects and content-stream tokens from bytes. Malformed input
/// never throws: the lexer skips what it cannot read, since real files are
/// full of small errors that readers are expected to tolerate.
@com.codename1.impl.SharedWithBackend
public final class PdfLexer {
    /// A content-stream operator such as `Tj` or `re`.
    public static final class Op {
        /// The operator text.
        public final String name;

        Op(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    /// Returned by [#next()] at the end of the data.
    public static final Object EOF = new Object();
    private static final Object END_ARRAY = new Object();
    private static final Object END_DICT = new Object();

    private final byte[] d;
    private final int end;
    /// The read position.
    public int pos;
    private final boolean references;

    /// A lexer over `data[start, end)`. With `references`, `n g R` is read
    /// as a [PdfRef], as in a file body; in content streams it must not be.
    public PdfLexer(byte[] data, int start, int end, boolean references) {
        this.d = data;
        this.pos = start;
        this.end = Math.min(end, data.length);
        this.references = references;
    }

    /// The data being read.
    public byte[] data() {
        return d;
    }

    /// The end of the readable range.
    public int end() {
        return end;
    }

    /// Skips white space and comments.
    public void skipWhitespace() {
        while (pos < end) {
            int c = d[pos] & 0xff;
            if (c == '%') {
                while (pos < end && d[pos] != '\n' && d[pos] != '\r') {
                    pos++;
                }
            } else if (PdfFormat.isWhitespace(c)) {
                pos++;
            } else {
                return;
            }
        }
    }

    /// The next complete object or operator, or [#EOF].
    public Object next() {
        Object o = token();
        while (o == END_ARRAY || o == END_DICT) {
            o = token();
        }
        return o;
    }

    // Arrays and dictionaries nest by recursion; past this depth an opening
    // bracket reads as an empty container, so hostile input cannot overflow
    // the stack (a native crash on ParparVM).
    private static final int MAX_NESTING = 64;
    private int nesting;

    private Object token() {
        while (true) {
            Object o = tokenOnce();
            if (o != SKIP) { // NOPMD CompareObjectsWithEquals - identity of a sentinel object is intended
                return o;
            }
        }
    }

    private static final Object SKIP = new Object();

    private Object tokenOnce() {
        skipWhitespace();
        if (pos >= end) {
            return EOF;
        }
        int c = d[pos] & 0xff;
        switch (c) {
            case '/':
                return name();
            case '(':
                return literal();
            case '[': {
                pos++;
                List<Object> list = new ArrayList<Object>();
                if (nesting >= MAX_NESTING) {
                    // Too deep: skip to the matching bracket without
                    // recursing, so the enclosing structure stays in frame.
                    int open = 1;
                    while (pos < end && open > 0) {
                        byte b = d[pos++];
                        if (b == '[') {
                            open++;
                        } else if (b == ']') {
                            open--;
                        }
                    }
                    return list;
                }
                nesting++;
                try {
                    while (true) {
                        Object o = token();
                        if (o == END_ARRAY || o == EOF) { // NOPMD CompareObjectsWithEquals - sentinel identity
                            return list;
                        }
                        if (o == END_DICT) { // NOPMD CompareObjectsWithEquals - sentinel identity
                            continue;
                        }
                        list.add(o);
                    }
                } finally {
                    nesting--;
                }
            }
            case ']':
                pos++;
                return END_ARRAY;
            case '<':
                if (pos + 1 < end && d[pos + 1] == '<') {
                    pos += 2;
                    return dictionary();
                }
                return hexString();
            case '>':
                pos += pos + 1 < end && d[pos + 1] == '>' ? 2 : 1;
                return END_DICT;
            case '{':
            case '}':
                pos++;
                return new Op(String.valueOf((char) c));
            case ')':
                pos++;
                return SKIP;
            default:
                break;
        }
        if (c >= '0' && c <= '9' || c == '-' || c == '+' || c == '.') {
            Object n = number();
            if (references && n instanceof Integer) {
                int save = pos;
                skipWhitespace();
                int gen = digits();
                if (gen >= 0) {
                    skipWhitespace();
                    if (pos < end && d[pos] == 'R' && (pos + 1 >= end || isEnd(d[pos + 1] & 0xff))) {
                        pos++;
                        return new PdfRef(((Integer) n).intValue(), gen);
                    }
                }
                pos = save;
            }
            return n;
        }
        int start = pos;
        while (pos < end && !isEnd(d[pos] & 0xff)) {
            pos++;
        }
        if (pos == start) {
            pos++;
            return SKIP;
        }
        String word = ascii(start, pos);
        if ("true".equals(word)) {
            return Boolean.TRUE;
        }
        if ("false".equals(word)) {
            return Boolean.FALSE;
        }
        if ("null".equals(word)) {
            return null;
        }
        return new Op(word);
    }

    private static boolean isEnd(int c) {
        return PdfFormat.isWhitespace(c) || PdfFormat.isDelimiter(c);
    }

    private int digits() {
        int start = pos;
        int v = 0;
        while (pos < end && d[pos] >= '0' && d[pos] <= '9') {
            v = v * 10 + (d[pos] - '0');
            pos++;
        }
        return pos == start ? -1 : v;
    }

    private PdfDict dictionary() {
        PdfDict dict = new PdfDict();
        if (nesting >= MAX_NESTING) {
            return dict;
        }
        nesting++;
        try {
            return fillDictionary(dict);
        } finally {
            nesting--;
        }
    }

    private PdfDict fillDictionary(PdfDict dict) {
        while (true) {
            Object k = token();
            if (k == END_DICT || k == EOF) {
                return dict;
            }
            if (!(k instanceof PdfName)) {
                continue;
            }
            Object v = token();
            if (v == END_DICT || v == EOF) {
                dict.put(((PdfName) k).value, null);
                return dict;
            }
            if (v == END_ARRAY) {
                continue;
            }
            dict.put(((PdfName) k).value, v);
        }
    }

    private Object number() {
        int start = pos;
        boolean real = false;
        if (d[pos] == '-' || d[pos] == '+') {
            pos++;
        }
        while (pos < end) {
            int c = d[pos];
            if (c == '.') {
                real = true;
            } else if (c < '0' || c > '9') {
                // Tolerate a stray minus inside a number, as readers do.
                if (c != '-') {
                    break;
                }
            }
            pos++;
        }
        String s = ascii(start, pos);
        if (!real) {
            long v = parseLong(s);
            if (v >= Integer.MIN_VALUE && v <= Integer.MAX_VALUE) {
                return Integer.valueOf((int) v);
            }
            return Double.valueOf(v);
        }
        return Double.valueOf(parseReal(s));
    }

    private static long parseLong(String s) {
        long v = 0;
        boolean neg = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '-') {
                neg = i == 0 || neg;
            } else if (c >= '0' && c <= '9') {
                v = v * 10 + (c - '0');
                if (v > 0xffffffffffL) {
                    break;
                }
            }
        }
        return neg ? -v : v;
    }

    private static double parseReal(String s) {
        double v = 0;
        double scale = 0;
        boolean neg = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '-') {
                neg = i == 0 || neg;
            } else if (c == '.') {
                if (scale == 0) {
                    scale = 1;
                }
            } else if (c >= '0' && c <= '9') {
                v = v * 10 + (c - '0');
                if (scale != 0) {
                    scale *= 10;
                }
            }
        }
        if (scale > 1) {
            v /= scale;
        }
        return neg ? -v : v;
    }

    private PdfName name() {
        pos++;
        StringBuilder sb = new StringBuilder();
        while (pos < end) {
            int c = d[pos] & 0xff;
            if (isEnd(c)) {
                break;
            }
            if (c == '#' && pos + 2 < end && hex(d[pos + 1]) >= 0 && hex(d[pos + 2]) >= 0) {
                sb.append((char) (hex(d[pos + 1]) * 16 + hex(d[pos + 2])));
                pos += 3;
            } else {
                sb.append((char) c);
                pos++;
            }
        }
        return new PdfName(sb.toString());
    }

    private static int hex(int c) {
        if (c >= '0' && c <= '9') {
            return c - '0';
        }
        if (c >= 'a' && c <= 'f') {
            return c - 'a' + 10;
        }
        if (c >= 'A' && c <= 'F') {
            return c - 'A' + 10;
        }
        return -1;
    }

    private PdfString hexString() {
        pos++;
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        int hi = -1;
        while (pos < end) {
            int c = d[pos++] & 0xff;
            if (c == '>') {
                break;
            }
            int h = hex(c);
            if (h < 0) {
                continue;
            }
            if (hi < 0) {
                hi = h;
            } else {
                bo.write(hi * 16 + h);
                hi = -1;
            }
        }
        if (hi >= 0) {
            bo.write(hi * 16);
        }
        return new PdfString(bo.toByteArray());
    }

    private PdfString literal() {
        pos++;
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        int depth = 1;
        while (pos < end) {
            int c = d[pos++] & 0xff;
            if (c == '(') {
                depth++;
            } else if (c == ')') {
                if (--depth == 0) { // NOPMD AssignmentInOperand - counter bumped where it is tested
                    break;
                }
            } else if (c == '\\' && pos < end) {
                c = d[pos++] & 0xff;
                switch (c) {
                    case 'n':
                        c = '\n';
                        break;
                    case 'r':
                        c = '\r';
                        break;
                    case 't':
                        c = '\t';
                        break;
                    case 'b':
                        c = '\b';
                        break;
                    case 'f':
                        c = '\f';
                        break;
                    case '\r':
                        if (pos < end && d[pos] == '\n') {
                            pos++;
                        }
                        continue;
                    case '\n':
                        continue;
                    default:
                        if (c >= '0' && c <= '7') {
                            int v = c - '0';
                            for (int k = 0; k < 2 && pos < end && d[pos] >= '0' && d[pos] <= '7'; k++) {
                                v = v * 8 + (d[pos++] - '0');
                            }
                            c = v & 0xff;
                        }
                }
            } else if (c == '\r') {
                // An unescaped end of line in a literal reads as one LF.
                if (pos < end && d[pos] == '\n') {
                    pos++;
                }
                c = '\n';
            }
            bo.write(c);
        }
        return new PdfString(bo.toByteArray());
    }

    /// After a `BI` operator: reads the inline image's dictionary and data
    /// up to `EI`, returning a stream whose keys are the abbreviated ones as
    /// written.
    public PdfStream inlineImage() {
        PdfStream s = new PdfStream();
        while (true) {
            Object k = next();
            if (k == EOF) {
                s.raw = new byte[0];
                return s;
            }
            if (k instanceof Op && "ID".equals(((Op) k).name)) {
                break;
            }
            if (k instanceof PdfName) {
                s.put(((PdfName) k).value, next());
            }
        }
        if (pos < end && PdfFormat.isWhitespace(d[pos] & 0xff)) {
            pos++;
        }
        int start = pos;
        int stop = end;
        for (int i = start; i + 1 < end; i++) {
            if (d[i] == 'E' && d[i + 1] == 'I' && (i == start || PdfFormat.isWhitespace(d[i - 1] & 0xff))
                    && (i + 2 >= end || isEnd(d[i + 2] & 0xff))) {
                stop = i;
                pos = i + 2;
                break;
            }
        }
        if (stop == end) {
            pos = end;
        }
        int dataEnd = stop;
        if (dataEnd > start && PdfFormat.isWhitespace(d[dataEnd - 1] & 0xff)) {
            dataEnd--;
        }
        s.raw = new byte[dataEnd - start];
        System.arraycopy(d, start, s.raw, 0, s.raw.length);
        return s;
    }

    /// The ASCII text of `d[from, to)`.
    public String ascii(int from, int to) {
        StringBuilder sb = new StringBuilder(to - from);
        for (int i = from; i < to; i++) {
            sb.append((char) (d[i] & 0xff));
        }
        return sb.toString();
    }
}
