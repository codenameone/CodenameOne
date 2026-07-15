/*
 * Copyright (c) 2008, 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
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
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores
 * CA 94065 USA or visit www.oracle.com if you need additional information or
 * have any questions.
 */
package com.codename1.ui.editor;

import com.codename1.ui.RichTextFormat;

import java.util.ArrayList;
import java.util.List;

/// Converts supported rich-text interchange formats into the HTML fragment consumed by the shared
/// `HtmlImporter`. Keeping one HTML-to-model mapping avoids separate style/block implementations for
/// RTF, Markdown and AsciiDoc.
public final class RichTextImporter {
    private RichTextImporter() {
    }

    /// Converts content to an HTML fragment. HTML input is returned unchanged and plain text is escaped.
    public static String toHtml(String content, RichTextFormat format) {
        String value = content == null ? "" : content;
        if (format == null || format == RichTextFormat.PLAIN_TEXT) {
            return "<p>" + escape(value).replace("\n", "<br>") + "</p>";
        }
        if (format == RichTextFormat.HTML) {
            return value;
        }
        if (format == RichTextFormat.RTF) {
            return rtfToHtml(value);
        }
        if (format == RichTextFormat.ASCIIDOC) {
            return asciiDocToHtml(value);
        }
        return markdownToHtml(value);
    }

    private static String markdownToHtml(String source) {
        List<String> lines = lines(source);
        StringBuilder out = new StringBuilder();
        String list = null;
        boolean fenced = false;
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.startsWith("```")) {
                if (fenced) {
                    out.append("<pre>").append(escape(code.toString())).append("</pre>");
                    code.setLength(0);
                }
                fenced = !fenced;
                continue;
            }
            if (fenced) {
                if (code.length() > 0) {
                    code.append('\n');
                }
                code.append(line);
                continue;
            }
            String nextList = markdownList(line);
            if (!equals(list, nextList)) {
                closeList(out, list);
                list = nextList;
                if (list != null) {
                    out.append('<').append(list).append('>');
                }
            }
            if (list != null) {
                int offset = "ol".equals(list) ? orderedListOffset(line) : 2;
                out.append("<li>").append(markdownInline(line.substring(offset).trim())).append("</li>");
                continue;
            }
            if (line.trim().length() == 0) {
                continue;
            }
            int heading = headingLevel(line, '#');
            if (heading > 0 && heading <= 6 && line.length() > heading && line.charAt(heading) == ' ') {
                out.append("<h").append(heading).append('>')
                        .append(markdownInline(line.substring(heading + 1))).append("</h").append(heading).append('>');
            } else if (line.startsWith("> ")) {
                out.append("<blockquote>").append(markdownInline(line.substring(2))).append("</blockquote>");
            } else {
                out.append("<p>").append(markdownInline(line)).append("</p>");
            }
        }
        closeList(out, list);
        if (fenced) {
            out.append("<pre>").append(escape(code.toString())).append("</pre>");
        }
        return out.toString();
    }

    private static String asciiDocToHtml(String source) {
        List<String> lines = lines(source);
        StringBuilder out = new StringBuilder();
        String list = null;
        boolean literal = false;
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if ("----".equals(line.trim()) || "....".equals(line.trim())) {
                if (literal) {
                    out.append("<pre>").append(escape(code.toString())).append("</pre>");
                    code.setLength(0);
                }
                literal = !literal;
                continue;
            }
            if (literal) {
                if (code.length() > 0) {
                    code.append('\n');
                }
                code.append(line);
                continue;
            }
            String nextList = asciiDocList(line);
            if (!equals(list, nextList)) {
                closeList(out, list);
                list = nextList;
                if (list != null) {
                    out.append('<').append(list).append('>');
                }
            }
            if (list != null) {
                out.append("<li>").append(asciiDocInline(line.substring(2).trim())).append("</li>");
                continue;
            }
            if (line.trim().length() == 0 || line.startsWith("[source")) {
                continue;
            }
            int heading = headingLevel(line, '=');
            if (heading > 0 && heading <= 6 && line.length() > heading && line.charAt(heading) == ' ') {
                out.append("<h").append(heading).append('>')
                        .append(asciiDocInline(line.substring(heading + 1))).append("</h").append(heading).append('>');
            } else if (line.startsWith("> ")) {
                out.append("<blockquote>").append(asciiDocInline(line.substring(2))).append("</blockquote>");
            } else {
                out.append("<p>").append(asciiDocInline(line)).append("</p>");
            }
        }
        closeList(out, list);
        if (literal) {
            out.append("<pre>").append(escape(code.toString())).append("</pre>");
        }
        return out.toString();
    }

    private static String markdownInline(String value) {
        StringBuilder out = new StringBuilder();
        int i = 0;
        while (i < value.length()) {
            if (value.charAt(i) == '\\' && i + 1 < value.length()) {
                appendEscaped(out, value.charAt(i + 1));
                i += 2;
                continue;
            }
            if (value.startsWith("![", i)) {
                int labelEnd = value.indexOf(']', i + 2);
                int urlEnd = labelEnd >= 0 && labelEnd + 1 < value.length() && value.charAt(labelEnd + 1) == '('
                        ? value.indexOf(')', labelEnd + 2) : -1;
                if (urlEnd > 0) {
                    out.append("<img src=\"").append(escapeAttribute(value.substring(labelEnd + 2, urlEnd))).append("\">");
                    i = urlEnd + 1;
                    continue;
                }
            }
            if (value.charAt(i) == '[') {
                int labelEnd = value.indexOf(']', i + 1);
                int urlEnd = labelEnd >= 0 && labelEnd + 1 < value.length() && value.charAt(labelEnd + 1) == '('
                        ? value.indexOf(')', labelEnd + 2) : -1;
                if (urlEnd > 0) {
                    out.append("<a href=\"").append(escapeAttribute(value.substring(labelEnd + 2, urlEnd))).append("\">")
                            .append(markdownInline(value.substring(i + 1, labelEnd))).append("</a>");
                    i = urlEnd + 1;
                    continue;
                }
            }
            String marker = markerAt(value, i);
            if (marker != null) {
                int end = value.indexOf(marker, i + marker.length());
                if (end > i + marker.length()) {
                    String tag = "~~".equals(marker) ? "s" : (marker.length() == 2 ? "strong" : "em");
                    out.append('<').append(tag).append('>')
                            .append(markdownInline(value.substring(i + marker.length(), end)))
                            .append("</").append(tag).append('>');
                    i = end + marker.length();
                    continue;
                }
            }
            if (value.charAt(i) == '`') {
                int end = value.indexOf('`', i + 1);
                if (end > i) {
                    out.append("<span>").append(escape(value.substring(i + 1, end))).append("</span>");
                    i = end + 1;
                    continue;
                }
            }
            appendEscaped(out, value.charAt(i++));
        }
        return out.toString();
    }

    private static String asciiDocInline(String value) {
        StringBuilder out = new StringBuilder();
        int i = 0;
        while (i < value.length()) {
            if (value.startsWith("http://", i) || value.startsWith("https://", i)) {
                int bracket = value.indexOf('[', i);
                int end = bracket < 0 ? -1 : value.indexOf(']', bracket + 1);
                if (end > bracket) {
                    out.append("<a href=\"").append(escapeAttribute(value.substring(i, bracket))).append("\">")
                            .append(escape(value.substring(bracket + 1, end))).append("</a>");
                    i = end + 1;
                    continue;
                }
            }
            char marker = value.charAt(i);
            if (marker == '*' || marker == '_' || marker == '`') {
                int end = value.indexOf(marker, i + 1);
                if (end > i + 1) {
                    String tag = marker == '*' ? "strong" : (marker == '_' ? "em" : "span");
                    out.append('<').append(tag).append('>').append(escape(value.substring(i + 1, end)))
                            .append("</").append(tag).append('>');
                    i = end + 1;
                    continue;
                }
            }
            appendEscaped(out, value.charAt(i++));
        }
        return out.toString();
    }

    private static String rtfToHtml(String source) {
        RtfState state = new RtfState();
        List<RtfState> stack = new ArrayList<RtfState>();
        RtfState rendered = new RtfState();
        StringBuilder out = new StringBuilder("<p>");
        int skipFallback = 0;
        for (int i = 0; i < source.length();) {
            char c = source.charAt(i++);
            if (c == '{') {
                stack.add(state.copy());
                continue;
            }
            if (c == '}') {
                if (!stack.isEmpty()) {
                    state = stack.remove(stack.size() - 1);
                }
                continue;
            }
            if (c == '\\') {
                if (i >= source.length()) {
                    break;
                }
                char next = source.charAt(i);
                if (next == '\\' || next == '{' || next == '}') {
                    i++;
                    if (!state.skip && skipFallback-- <= 0) {
                        syncRtfStyle(out, rendered, state);
                        appendEscaped(out, next);
                    }
                    continue;
                }
                if (next == '\'') {
                    i++;
                    if (i + 1 < source.length()) {
                        int value = hex(source.charAt(i), source.charAt(i + 1));
                        i += 2;
                        if (!state.skip && value >= 0 && skipFallback-- <= 0) {
                            syncRtfStyle(out, rendered, state);
                            appendEscaped(out, (char) value);
                        }
                    }
                    continue;
                }
                if (!Character.isLetter(next)) {
                    i++;
                    if (next == '*') {
                        state.skip = true;
                    } else if (!state.skip && (next == '~' || next == '_' || next == '-')) {
                        syncRtfStyle(out, rendered, state);
                        appendEscaped(out, next == '~' ? ' ' : '-');
                    }
                    continue;
                }
                int wordStart = i;
                while (i < source.length() && Character.isLetter(source.charAt(i))) {
                    i++;
                }
                String word = source.substring(wordStart, i);
                int sign = 1;
                if (i < source.length() && source.charAt(i) == '-') {
                    sign = -1;
                    i++;
                }
                int numberStart = i;
                while (i < source.length() && Character.isDigit(source.charAt(i))) {
                    i++;
                }
                boolean hasNumber = i > numberStart;
                int number = hasNumber ? sign * Integer.parseInt(source.substring(numberStart, i)) : 1;
                if (i < source.length() && source.charAt(i) == ' ') {
                    i++;
                }
                if (isRtfDestination(word)) {
                    state.skip = true;
                } else if ("b".equals(word)) {
                    state.bold = number != 0;
                } else if ("i".equals(word)) {
                    state.italic = number != 0;
                } else if ("ul".equals(word)) {
                    state.underline = number != 0;
                } else if ("ulnone".equals(word)) {
                    state.underline = false;
                } else if ("strike".equals(word)) {
                    state.strike = number != 0;
                } else if ("plain".equals(word)) {
                    state.bold = state.italic = state.underline = state.strike = false;
                } else if ("par".equals(word) || "line".equals(word)) {
                    if (!state.skip) {
                        closeRtfStyle(out, rendered);
                        out.append("</p><p>");
                    }
                } else if ("tab".equals(word) && !state.skip) {
                    syncRtfStyle(out, rendered, state);
                    out.append("&#9;");
                } else if ("uc".equals(word) && hasNumber) {
                    state.unicodeFallback = Math.max(0, number);
                } else if ("u".equals(word) && hasNumber && !state.skip) {
                    syncRtfStyle(out, rendered, state);
                    int unicode = number < 0 ? number + 65536 : number;
                    appendEscaped(out, (char) unicode);
                    skipFallback = state.unicodeFallback;
                }
                continue;
            }
            if (c == '\r' || c == '\n' || state.skip) {
                continue;
            }
            if (skipFallback > 0) {
                skipFallback--;
                continue;
            }
            syncRtfStyle(out, rendered, state);
            appendEscaped(out, c);
        }
        closeRtfStyle(out, rendered);
        return out.append("</p>").toString();
    }

    private static boolean isRtfDestination(String word) {
        return "fonttbl".equals(word) || "colortbl".equals(word) || "stylesheet".equals(word)
                || "info".equals(word) || "pict".equals(word) || "object".equals(word)
                || "header".equals(word) || "footer".equals(word) || "fldinst".equals(word);
    }

    private static void syncRtfStyle(StringBuilder out, RtfState rendered, RtfState desired) {
        if (rendered.sameStyle(desired)) {
            return;
        }
        closeRtfStyle(out, rendered);
        if (desired.bold) {
            out.append("<strong>");
        }
        if (desired.italic) {
            out.append("<em>");
        }
        if (desired.underline) {
            out.append("<u>");
        }
        if (desired.strike) {
            out.append("<s>");
        }
        rendered.setStyle(desired);
    }

    private static void closeRtfStyle(StringBuilder out, RtfState rendered) {
        if (rendered.strike) {
            out.append("</s>");
        }
        if (rendered.underline) {
            out.append("</u>");
        }
        if (rendered.italic) {
            out.append("</em>");
        }
        if (rendered.bold) {
            out.append("</strong>");
        }
        rendered.bold = rendered.italic = rendered.underline = rendered.strike = false;
    }

    private static final class RtfState {
        boolean bold;
        boolean italic;
        boolean underline;
        boolean strike;
        boolean skip;
        int unicodeFallback = 1;

        RtfState copy() {
            RtfState result = new RtfState();
            result.bold = bold;
            result.italic = italic;
            result.underline = underline;
            result.strike = strike;
            result.skip = skip;
            result.unicodeFallback = unicodeFallback;
            return result;
        }

        boolean sameStyle(RtfState other) {
            return bold == other.bold && italic == other.italic && underline == other.underline
                    && strike == other.strike;
        }

        void setStyle(RtfState other) {
            bold = other.bold;
            italic = other.italic;
            underline = other.underline;
            strike = other.strike;
        }
    }

    private static String markerAt(String value, int offset) {
        if (value.startsWith("**", offset) || value.startsWith("__", offset)) {
            return value.substring(offset, offset + 2);
        }
        if (value.startsWith("~~", offset)) {
            return "~~";
        }
        char c = value.charAt(offset);
        return c == '*' || c == '_' ? String.valueOf(c) : null;
    }

    private static String markdownList(String line) {
        if (line.startsWith("- ") || line.startsWith("* ") || line.startsWith("+ ")) {
            return "ul";
        }
        return orderedListOffset(line) > 0 ? "ol" : null;
    }

    private static int orderedListOffset(String line) {
        int i = 0;
        while (i < line.length() && Character.isDigit(line.charAt(i))) {
            i++;
        }
        return i > 0 && i + 1 < line.length() && line.charAt(i) == '.' && line.charAt(i + 1) == ' '
                ? i + 2 : 0;
    }

    private static String asciiDocList(String line) {
        if (line.startsWith("* ")) {
            return "ul";
        }
        return line.startsWith(". ") ? "ol" : null;
    }

    private static int headingLevel(String line, char marker) {
        int i = 0;
        while (i < line.length() && line.charAt(i) == marker) {
            i++;
        }
        return i;
    }

    private static void closeList(StringBuilder out, String list) {
        if (list != null) {
            out.append("</").append(list).append('>');
        }
    }

    private static boolean equals(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    private static List<String> lines(String source) {
        List<String> result = new ArrayList<String>();
        int start = 0;
        for (int i = 0; i < source.length(); i++) {
            if (source.charAt(i) == '\n') {
                int end = i > start && source.charAt(i - 1) == '\r' ? i - 1 : i;
                result.add(source.substring(start, end));
                start = i + 1;
            }
        }
        result.add(source.substring(start));
        return result;
    }

    private static int hex(char high, char low) {
        int h = Character.digit(high, 16);
        int l = Character.digit(low, 16);
        return h < 0 || l < 0 ? -1 : (h << 4) | l;
    }

    private static String escape(String value) {
        StringBuilder out = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            appendEscaped(out, value.charAt(i));
        }
        return out.toString();
    }

    private static String escapeAttribute(String value) {
        return escape(value).replace("\"", "&quot;");
    }

    private static void appendEscaped(StringBuilder out, char c) {
        if (c == '&') {
            out.append("&amp;");
        } else if (c == '<') {
            out.append("&lt;");
        } else if (c == '>') {
            out.append("&gt;");
        } else if (c == '"') {
            out.append("&quot;");
        } else {
            out.append(c);
        }
    }
}
