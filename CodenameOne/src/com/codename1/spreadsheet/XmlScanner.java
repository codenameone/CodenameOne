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
package com.codename1.spreadsheet;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

/// Reads the XML of a spreadsheet part as a sequence of tags and text, without building a
/// tree: a sheet of a hundred thousand cells is a hundred thousand elements.
///
/// It reads what spreadsheet programs write -- elements, attributes, text, CDATA, the five
/// named entities and numeric ones -- and skips comments, processing instructions and a
/// document type. Namespace prefixes are cut from element names, and from attribute names
/// that are asked for without one.
final class XmlScanner {
    static final int EOF = 0;
    static final int START = 1;
    static final int END = 2;
    static final int TEXT = 3;

    /// The characters every part of one file together may unpack to.
    static final long MAX_DOCUMENT = 256L * 1024 * 1024;
    /// The longest text or attribute value, far past the 32,767 characters a cell holds.
    static final int MAX_TEXT = 4 * 1024 * 1024;
    private final long[] budget;
    private int depth;
    private final Reader in;
    private final char[] buffer = new char[4096];
    private int pos;
    private int limit;
    private boolean pendingEnd;

    /// The element's name without a prefix, for START and END.
    String name;
    /// The text, for TEXT.
    String text = "";
    private String[] attrNames = new String[8];
    private String[] attrValues = new String[8];
    private int attrCount;
    private final StringBuilder sb = new StringBuilder(); //NOPMD AvoidStringBufferField - one buffer for every token of one file

    /// Reads a part as UTF-8, the encoding every spreadsheet program writes. The stream is
    /// not closed: it is one entry of an archive that has more.
    XmlScanner(InputStream stream) throws IOException {
        this(stream, new long[] {MAX_DOCUMENT});
    }

    /// Reads a part against what is left of the characters one file may unpack to: `budget`
    /// is shared by every part of the archive.
    XmlScanner(InputStream stream, long[] budget) throws IOException {
        in = new InputStreamReader(stream, "UTF-8");
        this.budget = budget;
    }

    private int read() throws IOException {
        if (pos >= limit) {
            limit = in.read(buffer, 0, buffer.length);
            pos = 0;
            if (limit <= 0) {
                limit = 0;
                return -1;
            }
            // A compressed part unpacks to a thousand times its size, so the size of the file
            // says nothing about what reading it takes.
            budget[0] -= limit;
            if (budget[0] < 0) {
                throw new IOException("The spreadsheet unpacks to more than " + MAX_DOCUMENT + " characters");
            }
            if (sb.length() > MAX_TEXT) {
                throw new IOException("The spreadsheet has a text of more than " + MAX_TEXT + " characters");
            }
        }
        return buffer[pos++];
    }


    /// The next event. An element with no content gives START and then END.
    int next() throws IOException {
        if (pendingEnd) {
            pendingEnd = false;
            depth--;
            return END;
        }
        for (;;) {
            int c = read();
            if (c < 0) {
                if (depth > 0) {
                    // half a part read as if it were whole is rows missing without a word
                    throw new IOException("The spreadsheet ends in the middle of an element");
                }
                return EOF;
            }
            if (c != '<') {
                sb.setLength(0);
                pos--;
                readText();
                text = sb.toString();
                return TEXT;
            }
            c = read();
            if (c == '?') {
                skipUntil("?>");
                continue;
            }
            if (c == '!') {
                c = read();
                if (c == '-') {
                    read();
                    skipUntil("-->");
                    continue;
                }
                if (c == '[') {
                    // <![CDATA[
                    for (int i = 0; i < 6; i++) {
                        read();
                    }
                    sb.setLength(0);
                    readCdata();
                    text = sb.toString();
                    return TEXT;
                }
                skipDeclaration();
                continue;
            }
            if (c == '/') {
                name = readName(read());
                int e = read();
                while (e >= 0 && e != '>') {
                    e = read();
                }
                depth--;
                return END;
            }
            name = readName(c);
            readAttributes();
            depth++;
            return START; //NOPMD AvoidBranchingStatementAsLastInLoop - the loop only repeats past comments and declarations
        }
    }

    private void readText() throws IOException {
        for (;;) {
            int c = read();
            if (c < 0) {
                return;
            }
            if (c == '<') {
                pos--;
                return;
            }
            if (c == '&') {
                entity();
            } else {
                sb.append((char) c);
            }
        }
    }

    private void readCdata() throws IOException {
        int brackets = 0;
        for (;;) {
            int c = read();
            if (c < 0) {
                return;
            }
            if (c == ']') {
                brackets++;
                continue;
            }
            if (c == '>' && brackets >= 2) {
                for (int i = 2; i < brackets; i++) {
                    sb.append(']');
                }
                return;
            }
            for (int i = 0; i < brackets; i++) {
                sb.append(']');
            }
            brackets = 0;
            sb.append((char) c);
        }
    }

    private void entity() throws IOException {
        StringBuilder e = new StringBuilder();
        for (;;) {
            int c = read();
            if (c < 0 || c == ';') {
                break;
            }
            if (c == '<' || c == '&' || c == ' ' || c == '\n' || c == '\r' || c == '\t' || e.length() > 10) {
                // not an entity: an ampersand that should have been escaped. What follows it
                // is read again as what it is, so that a tag after it is still a tag.
                sb.append('&').append(e);
                pos--;
                return;
            }
            e.append((char) c);
        }
        String s = e.toString();
        if ("amp".equals(s)) {
            sb.append('&');
        } else if ("lt".equals(s)) {
            sb.append('<');
        } else if ("gt".equals(s)) {
            sb.append('>');
        } else if ("quot".equals(s)) {
            sb.append('"');
        } else if ("apos".equals(s)) {
            sb.append('\'');
        } else if (s.length() > 1 && s.charAt(0) == '#') {
            try {
                int code = s.charAt(1) == 'x' || s.charAt(1) == 'X' ? Integer.parseInt(s.substring(2), 16)
                        : Integer.parseInt(s.substring(1));
                appendCodePoint(sb, code);
            } catch (NumberFormatException err) {
                sb.append('&').append(s).append(';');
            }
        } else {
            sb.append('&').append(s).append(';');
        }
    }

    static void appendCodePoint(StringBuilder sb, int code) {
        // what XML has no character for: half of a surrogate pair, and the control characters
        if (code < 0x20 && code != 9 && code != 10 && code != 13 || code >= 0xd800 && code <= 0xdfff
                || code == 0xfffe || code == 0xffff) {
            return;
        }
        if (code < 0x10000) {
            sb.append((char) code);
        } else if (code <= 0x10ffff) {
            int v = code - 0x10000;
            sb.append((char) (0xd800 + (v >> 10))).append((char) (0xdc00 + (v & 0x3ff)));
        }
    }

    private void skipUntil(String end) throws IOException {
        int matched = 0;
        int n = end.length();
        for (;;) {
            int c = read();
            if (c < 0) {
                return;
            }
            if (c == end.charAt(matched)) {
                matched++;
                if (matched == n) {
                    return;
                }
            } else if (matched == n - 1 && c == end.charAt(0) && n == 3) {
                // "--->": still two dashes seen
                matched = n - 1;
            } else {
                matched = c == end.charAt(0) ? 1 : 0;
            }
        }
    }

    private void skipDeclaration() throws IOException {
        int depth = 1;
        for (;;) {
            int c = read();
            if (c < 0) {
                return;
            }
            if (c == '<') {
                depth++;
            } else if (c == '>') {
                depth--;
                if (depth == 0) {
                    return;
                }
            }
        }
    }

    private static boolean isSpace(int c) {
        return c == ' ' || c == '\n' || c == '\r' || c == '\t';
    }

    /// Reads a name that starts with a character already read, and answers it without its
    /// prefix. Leaves the character after the name unread.
    private String readName(int first) throws IOException {
        sb.setLength(0);
        int c = first;
        while (c >= 0 && !isSpace(c) && c != '>' && c != '/' && c != '=') {
            if (c == ':') {
                sb.setLength(0);
            } else {
                sb.append((char) c);
            }
            c = read();
        }
        if (c >= 0) {
            pos--;
        }
        return sb.toString();
    }

    private String readQualifiedName(int first) throws IOException {
        sb.setLength(0);
        int c = first;
        while (c >= 0 && !isSpace(c) && c != '>' && c != '/' && c != '=') {
            sb.append((char) c);
            c = read();
        }
        if (c >= 0) {
            pos--;
        }
        return sb.toString();
    }

    private void readAttributes() throws IOException {
        attrCount = 0;
        for (;;) {
            int c = read();
            while (isSpace(c)) {
                c = read();
            }
            if (c < 0 || c == '>') {
                return;
            }
            if (c == '/') {
                c = read();
                pendingEnd = true;
                while (c >= 0 && c != '>') {
                    c = read();
                }
                return;
            }
            String attr = readQualifiedName(c);
            c = read();
            while (isSpace(c)) {
                c = read();
            }
            String value = "";
            if (c == '=') {
                c = read();
                while (isSpace(c)) {
                    c = read();
                }
                if (c == '"' || c == '\'') {
                    int quote = c;
                    sb.setLength(0);
                    for (;;) {
                        c = read();
                        if (c < 0 || c == quote) {
                            break;
                        }
                        if (c == '&') {
                            entity();
                        } else {
                            sb.append((char) c);
                        }
                    }
                    value = sb.toString();
                }
            } else if (c >= 0) {
                pos--;
            }
            if (attrCount == attrNames.length) {
                String[] names = new String[attrCount * 2];
                String[] values = new String[attrCount * 2];
                System.arraycopy(attrNames, 0, names, 0, attrCount);
                System.arraycopy(attrValues, 0, values, 0, attrCount);
                attrNames = names;
                attrValues = values;
            }
            attrNames[attrCount] = attr;
            attrValues[attrCount] = value;
            attrCount++;
        }
    }

    /// The value of an attribute of the element that START was just answered for, null when
    /// it has none of that name. A name with a prefix has to match whole; one without matches
    /// whatever prefix the attribute has, the first such attribute winning.
    String attr(String attribute) {
        boolean qualified = attribute.indexOf(':') >= 0;
        for (int i = 0; i < attrCount; i++) {
            String n = attrNames[i];
            if (n.equals(attribute)) {
                return attrValues[i];
            }
            if (!qualified) {
                int colon = n.indexOf(':');
                if (colon >= 0 && n.length() - colon - 1 == attribute.length() && n.endsWith(attribute)) {
                    return attrValues[i];
                }
            }
        }
        return null;
    }

    double doubleAttr(String attribute, double fallback) {
        String v = attr(attribute);
        if (v == null) {
            return fallback;
        }
        try {
            return Double.parseDouble(v.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    int intAttr(String attribute, int fallback) {
        String v = attr(attribute);
        if (v == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(v.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    boolean boolAttr(String attribute) {
        String v = attr(attribute);
        return "1".equals(v) || "true".equals(v);
    }

    /// Skips to the end of the element that START was just answered for.
    void skipElement() throws IOException {
        int depth = 1;
        while (depth > 0) {
            int e = next();
            if (e == EOF) {
                return;
            }
            if (e == START) {
                depth++;
            } else if (e == END) {
                depth--;
            }
        }
    }

    /// The text inside the element that START was just answered for, to its end tag; text of
    /// nested elements included.
    String elementText() throws IOException {
        StringBuilder out = null;
        String single = "";
        int depth = 1;
        while (depth > 0) {
            int e = next();
            if (e == EOF) {
                break;
            }
            if (e == START) {
                depth++;
            } else if (e == END) {
                depth--;
            } else if (out != null) {
                out.append(text);
            } else if (single.length() == 0) {
                single = text;
            } else {
                out = new StringBuilder(single).append(text);
            }
        }
        return out == null ? single : out.toString();
    }
}
