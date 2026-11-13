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
package com.codename1.cli;

import java.util.ArrayList;
import java.util.List;

/// Measuring, cutting and wrapping text by the columns it takes on a terminal
/// rather than by its `char` count: escape sequences take no columns, East
/// Asian wide characters and emoji take two, combining marks none.
final class TextWidth {
    private TextWidth() {
    }

    /// The columns code point `cp` takes: 0, 1 or 2.
    static int codePointWidth(int cp) {
        if (cp == 0 || cp < 32 || (cp >= 0x7f && cp < 0xa0)) {
            return 0;
        }
        if (isZeroWidth(cp)) {
            return 0;
        }
        return isWide(cp) ? 2 : 1;
    }

    private static boolean isZeroWidth(int cp) {
        return (cp >= 0x300 && cp <= 0x36f)
                || (cp >= 0x483 && cp <= 0x489)
                || (cp >= 0x591 && cp <= 0x5bd)
                || (cp >= 0x610 && cp <= 0x61a)
                || (cp >= 0x64b && cp <= 0x65f)
                || (cp >= 0x1ab0 && cp <= 0x1aff)
                || (cp >= 0x1dc0 && cp <= 0x1dff)
                || (cp >= 0x200b && cp <= 0x200f)
                || (cp >= 0x20d0 && cp <= 0x20ff)
                || (cp >= 0xfe00 && cp <= 0xfe0f)
                || (cp >= 0xfe20 && cp <= 0xfe2f)
                || cp == 0xfeff
                || (cp >= 0x1f3fb && cp <= 0x1f3ff)
                || (cp >= 0xe0100 && cp <= 0xe01ef);
    }

    private static boolean isWide(int cp) {
        return (cp >= 0x1100 && cp <= 0x115f)
                || cp == 0x2329 || cp == 0x232a
                || (cp >= 0x231a && cp <= 0x231b)
                || (cp >= 0x23e9 && cp <= 0x23ec)
                || (cp >= 0x25fd && cp <= 0x25fe)
                || (cp >= 0x2614 && cp <= 0x2615)
                || (cp >= 0x2648 && cp <= 0x2653)
                || cp == 0x26a1 || cp == 0x26bd || cp == 0x26be || cp == 0x26c4 || cp == 0x26c5
                || cp == 0x26d4 || cp == 0x26ea || cp == 0x26f5 || cp == 0x26fa || cp == 0x26fd
                || cp == 0x2705 || cp == 0x270a || cp == 0x270b || cp == 0x2728 || cp == 0x274c
                || cp == 0x2753 || cp == 0x2754 || cp == 0x2755 || cp == 0x2757
                || (cp >= 0x2795 && cp <= 0x2797)
                || cp == 0x27b0 || cp == 0x27bf || cp == 0x2b1b || cp == 0x2b1c
                || cp == 0x2b50 || cp == 0x2b55
                || (cp >= 0x2e80 && cp <= 0x303e)
                || (cp >= 0x3041 && cp <= 0x33ff)
                || (cp >= 0x3400 && cp <= 0x4dbf)
                || (cp >= 0x4e00 && cp <= 0x9fff)
                || (cp >= 0xa000 && cp <= 0xa4cf)
                || (cp >= 0xa960 && cp <= 0xa97f)
                || (cp >= 0xac00 && cp <= 0xd7a3)
                || (cp >= 0xf900 && cp <= 0xfaff)
                || (cp >= 0xfe10 && cp <= 0xfe19)
                || (cp >= 0xfe30 && cp <= 0xfe6f)
                || (cp >= 0xff00 && cp <= 0xff60)
                || (cp >= 0xffe0 && cp <= 0xffe6)
                || (cp >= 0x16fe0 && cp <= 0x18aff)
                || (cp >= 0x1b000 && cp <= 0x1b2ff)
                || cp == 0x1f004 || cp == 0x1f0cf || cp == 0x1f18e
                || (cp >= 0x1f191 && cp <= 0x1f19a)
                || (cp >= 0x1f200 && cp <= 0x1f251)
                || (cp >= 0x1f300 && cp <= 0x1f64f)
                || (cp >= 0x1f680 && cp <= 0x1f6ff)
                || (cp >= 0x1f7e0 && cp <= 0x1f7eb)
                || (cp >= 0x1f90c && cp <= 0x1f9ff)
                || (cp >= 0x1fa70 && cp <= 0x1faff)
                || (cp >= 0x20000 && cp <= 0x3fffd);
    }

    /// The length of the escape sequence starting at `i` (which holds ESC).
    static int escapeLength(String s, int i) {
        int n = s.length();
        if (i + 1 >= n) {
            return 1;
        }
        char c = s.charAt(i + 1);
        if (c == '[') {
            int j = i + 2;
            while (j < n) {
                char d = s.charAt(j);
                if (d >= 0x40 && d <= 0x7e) {
                    return j - i + 1;
                }
                j++;
            }
            return n - i;
        }
        if (c == ']') {
            // OSC, a hyperlink: ends at BEL or at ESC backslash.
            int j = i + 2;
            while (j < n) {
                char d = s.charAt(j);
                if (d == 7) {
                    return j - i + 1;
                }
                if (d == 27 && j + 1 < n && s.charAt(j + 1) == '\\') {
                    return j - i + 2;
                }
                j++;
            }
            return n - i;
        }
        return 2;
    }

    /// The columns `s` takes.
    static int width(String s) {
        if (s == null) {
            return 0;
        }
        int w = 0;
        int i = 0;
        int n = s.length();
        while (i < n) {
            char c = s.charAt(i);
            if (c == 27) {
                i += escapeLength(s, i);
                continue;
            }
            int cp = s.codePointAt(i);
            w += codePointWidth(cp);
            i += Character.charCount(cp);
        }
        return w;
    }

    /// `s` without escape sequences.
    static String strip(String s) {
        if (s == null || s.indexOf(27) < 0) {
            return s;
        }
        StringBuilder sb = new StringBuilder(s.length());
        int i = 0;
        int n = s.length();
        while (i < n) {
            char c = s.charAt(i);
            if (c == 27) {
                i += escapeLength(s, i);
                continue;
            }
            sb.append(c);
            i++;
        }
        return sb.toString();
    }

    /// `s` cut to at most `max` columns, keeping escape sequences and ending
    /// with `ellipsis` when anything was cut. A reset is appended after a cut
    /// that left an escape open.
    static String truncate(String s, int max, String ellipsis) {
        return truncate(s, max, ellipsis, true);
    }

    /// [#truncate(String, int, String)], leaving open escapes open when
    /// `close` is false -- for a wrap, whose post-pass closes and reopens
    /// every style in force at a break, the ones from earlier words too.
    private static String truncate(String s, int max, String ellipsis, boolean close) {
        if (width(s) <= max) {
            return s;
        }
        int ellipsisWidth = ellipsis == null ? 0 : width(ellipsis);
        int budget = Math.max(0, max - ellipsisWidth);
        StringBuilder sb = new StringBuilder();
        boolean escapes = false;
        boolean linkOpen = false;
        int w = 0;
        int i = 0;
        int n = s.length();
        while (i < n) {
            char c = s.charAt(i);
            if (c == 27) {
                int len = escapeLength(s, i);
                if (s.startsWith("\u001b]8;", i)) {
                    linkOpen = !hyperlinkCloses(s, i, len);
                }
                sb.append(s, i, i + len);
                escapes = true;
                i += len;
                continue;
            }
            int cp = s.codePointAt(i);
            int cw = codePointWidth(cp);
            if (w + cw > budget) {
                break;
            }
            sb.appendCodePoint(cp);
            w += cw;
            i += Character.charCount(cp);
        }
        if (escapes && close) {
            sb.append("\u001b[0m");
        }
        if (linkOpen && close) {
            // Cut inside a hyperlink: without its closing sequence the link
            // would run on over everything the terminal shows after it.
            sb.append("\u001b]8;;\u001b\\");
        }
        if (ellipsis != null && max >= ellipsisWidth) {
            sb.append(ellipsis);
        }
        return sb.toString();
    }

    /// Whether the OSC 8 sequence at `start` (`len` chars) is the closing
    /// one, `ESC ] 8 ; params ; ST`, whose URI is empty.
    private static boolean hyperlinkCloses(String s, int start, int len) {
        int uri = s.indexOf(';', start + 4);
        if (uri < 0 || uri >= start + len) {
            return true;
        }
        char after = uri + 1 < start + len ? s.charAt(uri + 1) : 7;
        return after == 7 || after == 27;
    }

    /// `s` padded with spaces to `width` columns, on the side `align` says.
    static String pad(String s, int width, Table.Align align) {
        int gap = width - width(s);
        if (gap <= 0) {
            return s;
        }
        StringBuilder sb = new StringBuilder(s.length() + gap);
        if (align == Table.Align.RIGHT) {
            spaces(sb, gap).append(s);
        } else if (align == Table.Align.CENTER) {
            int left = gap / 2;
            spaces(sb, left).append(s);
            spaces(sb, gap - left);
        } else {
            sb.append(s);
            spaces(sb, gap);
        }
        return sb.toString();
    }

    static StringBuilder spaces(StringBuilder sb, int n) {
        for (int i = 0; i < n; i++) {
            sb.append(' ');
        }
        return sb;
    }

    /// `s` split into lines of at most `width` columns: at its own line breaks,
    /// then between words, and inside a word only when the word alone is
    /// wider than a line. Escape sequences stay with the text they style.
    static List<String> wrap(String s, int width) {
        List<String> out = new ArrayList<String>();
        if (width < 1) {
            width = 1;
        }
        String[] paragraphs = splitLines(s == null ? "" : s);
        for (String paragraph : paragraphs) {
            wrapParagraph(paragraph, width, out);
        }
        // A style or link open where a line breaks would color the border
        // and cells after it, and be missing from the line that continues
        // it: close it at the break and open it again on the next line.
        String state = "";
        for (int i = 0; i < out.size(); i++) {
            StringBuilder line = new StringBuilder(state).append(out.get(i));
            state = activeState(line.toString());
            if (state.length() > 0) {
                line.append(closer(state));
            }
            out.set(i, line.toString());
        }
        return out;
    }

    /// The escapes still in force at the end of `s`: the SGR codes since
    /// the last reset, then an open OSC 8 hyperlink.
    static String activeState(String s) {
        StringBuilder sgr = new StringBuilder();
        String link = "";
        int n = s.length();
        int i = 0;
        while (i < n) {
            if (s.charAt(i) != 27) {
                i++;
                continue;
            }
            int len = escapeLength(s, i);
            String seq = s.substring(i, Math.min(n, i + len));
            if (seq.length() > 2 && seq.charAt(1) == '[' && seq.charAt(seq.length() - 1) == 'm') {
                String params = seq.substring(2, seq.length() - 1);
                if (params.length() == 0 || "0".equals(params)) {
                    sgr.setLength(0);
                } else {
                    sgr.append(seq);
                }
            } else if (seq.startsWith("\u001b]8;")) {
                link = hyperlinkCloses(s, i, len) ? "" : seq;
            }
            i += len;
        }
        return sgr.toString() + link;
    }

    /// What ends `state` from [#activeState(String)].
    private static String closer(String state) {
        StringBuilder sb = new StringBuilder();
        if (state.indexOf("\u001b]8;") >= 0) {
            sb.append("\u001b]8;;\u001b\\");
        }
        if (state.indexOf("\u001b[") >= 0) {
            sb.append("\u001b[0m");
        }
        return sb.toString();
    }

    static String[] splitLines(String s) {
        List<String> lines = new ArrayList<String>();
        int start = 0;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == '\n') {
                int end = i > start && s.charAt(i - 1) == '\r' ? i - 1 : i;
                lines.add(s.substring(start, end));
                start = i + 1;
            }
        }
        lines.add(s.substring(start));
        return lines.toArray(new String[lines.size()]);
    }

    private static void wrapParagraph(String text, int width, List<String> out) {
        if (width(text) <= width) {
            out.add(text);
            return;
        }
        StringBuilder line = new StringBuilder();
        int lineWidth = 0;
        int i = 0;
        int n = text.length();
        while (i < n) {
            int start = i;
            while (i < n && text.charAt(i) == ' ') {
                i++;
            }
            int wordStart = i;
            while (i < n && text.charAt(i) != ' ') {
                i++;
            }
            if (wordStart == i) {
                break;
            }
            String word = text.substring(wordStart, i);
            int ww = width(word);
            int gap = lineWidth == 0 ? 0 : wordStart - start;
            if (lineWidth > 0 && lineWidth + gap + ww > width) {
                out.add(line.toString());
                line.setLength(0);
                lineWidth = 0;
                gap = 0;
            }
            if (ww > width) {
                // A word wider than a line is cut wherever the line ends.
                String rest = word;
                while (width(rest) > width - lineWidth) {
                    String head = truncate(rest, width - lineWidth, null, false);
                    if (width(head) == 0) {
                        // Narrower than one character (a wide one in a
                        // one-column cell): it goes on a line of its own,
                        // overflowing, or the cut would make no progress.
                        String after = dropColumns(rest, 1);
                        head = rest.substring(0, rest.length() - after.length());
                    }
                    String plainHead = strip(head);
                    line.append(head);
                    out.add(line.toString());
                    line.setLength(0);
                    lineWidth = 0;
                    rest = dropColumns(rest, width(plainHead));
                }
                line.append(rest);
                lineWidth = width(rest);
                continue;
            }
            spaces(line, gap).append(word);
            lineWidth += gap + ww;
        }
        // A word cut to the last column leaves nothing on the final line.
        if (line.length() > 0) {
            out.add(line.toString());
        }
    }

    /// `s` without its first `columns` columns of text (escapes are dropped
    /// with the text before them).
    private static String dropColumns(String s, int columns) {
        int w = 0;
        int i = 0;
        int n = s.length();
        while (i < n && w < columns) {
            char c = s.charAt(i);
            if (c == 27) {
                i += escapeLength(s, i);
                continue;
            }
            int cp = s.codePointAt(i);
            w += codePointWidth(cp);
            i += Character.charCount(cp);
        }
        return s.substring(i);
    }
}
