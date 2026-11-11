/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.backend.mail;

/// Derives the plain text alternative of an HTML body.
///
/// Not a renderer: it drops markup, keeps the words, breaks lines where a
/// block ends and writes a link's address after its text -- a password reset
/// mail whose plain part has no link in it is of no use to the reader who
/// sees that part. A mail whose plain text matters is given one with
/// [Mail.Builder#text].
final class HtmlText {
    private HtmlText() {
    }

    /// Whether the `<` at `at` begins a tag, a comment or a declaration, as
    /// an HTML parser decides it: by the character after it.
    private static boolean opensTag(String html, int at) {
        if (at + 1 >= html.length()) {
            return false;
        }
        char c = html.charAt(at + 1);
        return c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c == '/' || c == '!' || c == '?';
    }

    static String strip(String html) {
        StringBuilder out = new StringBuilder(html.length() / 2 + 16);
        int n = html.length();
        int i = 0;
        String href = null;
        int linkStart = 0;
        while (i < n) {
            char c = html.charAt(i);
            if (c == '<' && !opensTag(html, i)) {
                // "a < b": a sign, not markup. Read as a tag it would take
                // the text after it along, to the next > or to the end.
                out.append(c);
                i++;
            } else if (c == '<') {
                int end = html.indexOf('>', i);
                if (end < 0) {
                    // A tag cut short: nothing of it is text.
                    break;
                }
                if (html.startsWith("<!--", i)) {
                    int close = html.indexOf("-->", i + 4);
                    i = close < 0 ? n : close + 3;
                    continue;
                }
                boolean closing = i + 1 < n && html.charAt(i + 1) == '/';
                String tag = tagName(html, closing ? i + 2 : i + 1, end);
                if (!closing && ("script".equals(tag) || "style".equals(tag)
                        || "head".equals(tag) || "title".equals(tag))) {
                    int close = indexOfClose(html, tag, end);
                    i = close < 0 ? n : close;
                    continue;
                }
                if ("a".equals(tag)) {
                    if (closing) {
                        if (href != null) {
                            String shown = out.substring(linkStart).trim();
                            if (!shown.equals(href)) {
                                out.append(" (").append(href).append(')');
                            }
                            href = null;
                        }
                    } else {
                        href = attribute(html, i, end, "href");
                        linkStart = out.length();
                    }
                } else if ("br".equals(tag) || closing && isBlock(tag)) {
                    out.append('\n');
                    if (closing && ("p".equals(tag) || tag.length() == 2 && tag.charAt(0) == 'h')) {
                        // A paragraph is set off by a blank line.
                        out.append('\n');
                    }
                } else if (!closing && "li".equals(tag)) {
                    out.append("- ");
                }
                i = end + 1;
            } else if (c == '&') {
                i = entity(html, i, out);
            } else if (c == '\n' || c == '\r' || c == '\t') {
                out.append(' ');
                i++;
            } else {
                out.append(c);
                i++;
            }
        }
        return tidy(out);
    }

    private static boolean isBlock(String tag) {
        return "p".equals(tag) || "div".equals(tag) || "tr".equals(tag) || "li".equals(tag)
                || "table".equals(tag) || "ul".equals(tag) || "ol".equals(tag)
                || "blockquote".equals(tag) || "section".equals(tag) || "header".equals(tag)
                || "footer".equals(tag) || tag.length() == 2 && tag.charAt(0) == 'h'
                && tag.charAt(1) >= '1' && tag.charAt(1) <= '6';
    }

    /// The tag's name in lower case; ASCII folded by hand, as a device set to
    /// Turkish folds `TITLE` to something that is not `title`.
    private static String tagName(String html, int from, int end) {
        StringBuilder sb = new StringBuilder(8);
        for (int i = from; i < end; i++) {
            char c = html.charAt(i);
            if (c >= 'A' && c <= 'Z') {
                c = (char) (c + 32);
            }
            if (!(c >= 'a' && c <= 'z' || c >= '0' && c <= '9')) {
                break;
            }
            sb.append(c);
        }
        return sb.toString();
    }

    private static int indexOfClose(String html, String tag, int from) {
        int n = html.length();
        int i = from;
        while (true) {
            i = html.indexOf("</", i);
            if (i < 0) {
                return -1;
            }
            if (html.regionMatches(true, i + 2, tag, 0, tag.length())) {
                int end = html.indexOf('>', i);
                return end < 0 ? n : end + 1;
            }
            i += 2;
        }
    }

    private static String attribute(String html, int from, int end, String name) {
        int i = from;
        while (i < end) {
            int at = indexOfIgnoreCase(html, name, i, end);
            if (at < 0) {
                return null;
            }
            int p = at + name.length();
            char before = html.charAt(at - 1);
            while (p < end && html.charAt(p) == ' ') {
                p++;
            }
            if (before <= ' ' && p < end && html.charAt(p) == '=') {
                p++;
                while (p < end && html.charAt(p) == ' ') {
                    p++;
                }
                if (p >= end) {
                    return null;
                }
                char q = html.charAt(p);
                if (q == '"' || q == '\'') {
                    int close = html.indexOf(q, p + 1);
                    if (close < 0 || close > end) {
                        return null;
                    }
                    return unescape(html.substring(p + 1, close));
                }
                int stop = p;
                while (stop < end && html.charAt(stop) > ' ') {
                    stop++;
                }
                return unescape(html.substring(p, stop));
            }
            i = at + name.length();
        }
        return null;
    }

    private static int indexOfIgnoreCase(String html, String name, int from, int end) {
        for (int i = Math.max(from, 1); i + name.length() <= end; i++) {
            if (html.regionMatches(true, i, name, 0, name.length())) {
                return i;
            }
        }
        return -1;
    }

    private static String unescape(String value) {
        if (value.indexOf('&') < 0) {
            return value;
        }
        StringBuilder sb = new StringBuilder(value.length());
        int i = 0;
        while (i < value.length()) {
            if (value.charAt(i) == '&') {
                i = entity(value, i, sb);
            } else {
                sb.append(value.charAt(i++));
            }
        }
        return sb.toString();
    }

    /// Appends the character the entity at `i` stands for and answers the
    /// index after it; an entity it does not know is copied as it is.
    private static int entity(String html, int i, StringBuilder out) {
        int semi = html.indexOf(';', i);
        if (semi < 0 || semi - i > 9) {
            out.append('&');
            return i + 1;
        }
        String name = html.substring(i + 1, semi);
        if ("amp".equals(name)) {
            out.append('&');
        } else if ("lt".equals(name)) {
            out.append('<');
        } else if ("gt".equals(name)) {
            out.append('>');
        } else if ("quot".equals(name)) {
            out.append('"');
        } else if ("apos".equals(name)) {
            out.append('\'');
        } else if ("nbsp".equals(name)) {
            out.append(' ');
        } else if (name.length() > 1 && name.charAt(0) == '#') {
            int code = number(name);
            if (code <= 0 || code > 0x10FFFF || code >= 0xD800 && code <= 0xDFFF) {
                out.append('&');
                return i + 1;
            }
            if (code >= 0x10000) {
                int v = code - 0x10000;
                out.append((char) (0xD800 + (v >> 10))).append((char) (0xDC00 + (v & 0x3FF)));
            } else {
                out.append((char) code);
            }
        } else {
            out.append('&');
            return i + 1;
        }
        return semi + 1;
    }

    private static int number(String name) {
        boolean hex = name.length() > 2 && (name.charAt(1) == 'x' || name.charAt(1) == 'X');
        int value = 0;
        for (int i = hex ? 2 : 1; i < name.length(); i++) {
            char c = name.charAt(i);
            int d;
            if (c >= '0' && c <= '9') {
                d = c - '0';
            } else if (hex && c >= 'a' && c <= 'f') {
                d = c - 'a' + 10;
            } else if (hex && c >= 'A' && c <= 'F') {
                d = c - 'A' + 10;
            } else {
                return -1;
            }
            value = value * (hex ? 16 : 10) + d;
            if (value > 0x10FFFF) {
                return -1;
            }
        }
        return value;
    }

    /// Runs of spaces become one, spaces around a line break go, and more
    /// than one blank line in a row becomes one.
    private static String tidy(StringBuilder raw) {
        StringBuilder out = new StringBuilder(raw.length());
        int blank = 2;
        int n = raw.length();
        int i = 0;
        while (i < n) {
            int eol = i;
            while (eol < n && raw.charAt(eol) != '\n') {
                eol++;
            }
            int from = i;
            int to = eol;
            while (from < to && raw.charAt(from) == ' ') {
                from++;
            }
            while (to > from && raw.charAt(to - 1) == ' ') {
                to--;
            }
            if (from == to) {
                if (blank == 0) {
                    out.append('\n');
                }
                blank++;
            } else {
                boolean space = false;
                for (int p = from; p < to; p++) {
                    char c = raw.charAt(p);
                    if (c == ' ') {
                        space = true;
                    } else {
                        if (space) {
                            out.append(' ');
                            space = false;
                        }
                        out.append(c);
                    }
                }
                out.append('\n');
                blank = 0;
            }
            i = eol + 1;
        }
        int end = out.length();
        while (end > 0 && out.charAt(end - 1) == '\n') {
            end--;
        }
        out.setLength(end);
        if (end > 0) {
            out.append('\n');
        }
        return out.toString();
    }
}
