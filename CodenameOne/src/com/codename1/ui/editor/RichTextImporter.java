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

package com.codename1.ui.editor;

import com.codename1.ui.RichTextFormat;

import java.util.ArrayList;
import java.util.List;

/// Parses supported rich-text interchange formats into the shared editor model. HTML delegates to the
/// framework HTML/XML parser; Markdown and AsciiDoc are parsed directly so neither format is rewritten
/// through HTML on input or output.
public final class RichTextImporter {
    private RichTextImporter() {
    }

    /// Parses rich text directly into the editor model. Markdown and AsciiDoc use native model
    /// builders so their structure does not make an intermediate trip through HTML.
    public static HtmlImporter.Result parse(String content, RichTextFormat format) {
        String value = content == null ? "" : content;
        if (format == RichTextFormat.HTML) {
            return HtmlImporter.parse(value);
        }
        if (format == RichTextFormat.MARKDOWN) {
            return parseLightweightMarkup(value, false);
        }
        if (format == RichTextFormat.ASCIIDOC) {
            return parseLightweightMarkup(value, true);
        }
        if (format == RichTextFormat.RTF) {
            return parseRtf(value);
        }
        ModelBuilder builder = new ModelBuilder();
        List<String> plainLines = lines(value);
        for (int i = 0; i < plainLines.size(); i++) {
            builder.startBlock(RichBlocks.PARAGRAPH, RichBlocks.LIST_NONE);
            builder.append(plainLines.get(i), TextStyle.DEFAULT, null);
        }
        return builder.result(false);
    }

    /// Converts HTML produced by a browser fallback into another rich-text format through the shared
    /// document model. Pure editors do not use this path for Markdown or AsciiDoc input.
    public static String fromHtml(String html, RichTextFormat format) {
        HtmlImporter.Result result = HtmlImporter.parse(html == null ? "" : html);
        EditorDocument doc = new EditorDocument(result.getText());
        InlineStyles inline = new InlineStyles(doc.length());
        for (int i = 0; i < doc.length() && i < result.getStyles().size(); i++) {
            inline.setAt(i, result.getStyles().get(i));
        }
        RichBlocks blocks = new RichBlocks(doc.getLineCount());
        for (int i = 0; i < blocks.count() && i < result.getBlocks().size(); i++) {
            RichBlocks.BlockAttr source = result.getBlocks().get(i);
            RichBlocks.BlockAttr target = blocks.get(i);
            target.type = source.type;
            target.align = source.align;
            target.listType = source.listType;
            target.indent = source.indent;
        }
        return RichTextSerializer.serialize(doc, inline, blocks, result.getLinks(),
                result.getImageSources(), format);
    }

    /// Converts between non-HTML formats through the native rich-text model. This preserves source
    /// mode in browser fallbacks without using HTML as an intermediate representation.
    public static String convert(String content, RichTextFormat sourceFormat, RichTextFormat targetFormat) {
        RichTextFormat source = sourceFormat == null ? RichTextFormat.PLAIN_TEXT : sourceFormat;
        RichTextFormat target = targetFormat == null ? RichTextFormat.PLAIN_TEXT : targetFormat;
        if (source == target) return content == null ? "" : content;
        HtmlImporter.Result result = parse(content, source);
        EditorDocument doc = new EditorDocument(result.getText());
        InlineStyles inline = new InlineStyles(doc.length());
        for (int i = 0; i < doc.length() && i < result.getStyles().size(); i++) {
            inline.setAt(i, result.getStyles().get(i));
        }
        RichBlocks blocks = new RichBlocks(doc.getLineCount());
        for (int i = 0; i < blocks.count() && i < result.getBlocks().size(); i++) {
            RichBlocks.BlockAttr sourceBlock = result.getBlocks().get(i);
            RichBlocks.BlockAttr targetBlock = blocks.get(i);
            targetBlock.type = sourceBlock.type;
            targetBlock.align = sourceBlock.align;
            targetBlock.listType = sourceBlock.listType;
            targetBlock.indent = sourceBlock.indent;
        }
        return RichTextSerializer.serialize(doc, inline, blocks, result.getLinks(),
                result.getImageSources(), target);
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
        for (String line : lines) {
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

    private static HtmlImporter.Result parseLightweightMarkup(String source, boolean asciidoc) {
        ModelBuilder out = new ModelBuilder();
        List<String> sourceLines = lines(source);
        boolean literal = false;
        for (int lineIndex = 0; lineIndex < sourceLines.size(); lineIndex++) {
            String line = sourceLines.get(lineIndex);
            String trimmed = line.trim();
            boolean fence = asciidoc ? "----".equals(trimmed) || "....".equals(trimmed)
                    : line.startsWith("```");
            if (fence) {
                literal = !literal;
                continue;
            }
            if (asciidoc && line.startsWith("[source")) {
                continue;
            }
            if (literal) {
                out.startBlock(RichBlocks.PRE, RichBlocks.LIST_NONE);
                out.append(line, TextStyle.DEFAULT, null);
                continue;
            }

            int type = RichBlocks.PARAGRAPH;
            int listType = RichBlocks.LIST_NONE;
            String inline = line;
            int heading = headingLevel(line, asciidoc ? '=' : '#');
            if (heading > 0 && heading <= 6 && line.length() > heading && line.charAt(heading) == ' ') {
                type = RichBlocks.H1 + heading - 1;
                inline = line.substring(heading + 1);
            } else if (line.startsWith("> ")) {
                type = RichBlocks.BLOCKQUOTE;
                inline = line.substring(2);
            } else {
                String list = asciidoc ? asciiDocList(line) : markdownList(line);
                if (list != null) {
                    listType = "ol".equals(list) ? RichBlocks.LIST_ORDERED : RichBlocks.LIST_UNORDERED;
                    int offset = asciidoc ? 2 : (listType == RichBlocks.LIST_ORDERED
                            ? orderedListOffset(line) : 2);
                    inline = line.substring(Math.min(offset, line.length())).trim();
                }
            }
            out.startBlock(type, listType);
            if (asciidoc) {
                appendAsciiDocInline(out, inline, TextStyle.DEFAULT, null);
            } else {
                appendMarkdownInline(out, inline, TextStyle.DEFAULT, null);
            }
        }
        return out.result(true);
    }

    private static void appendMarkdownInline(ModelBuilder out, String value, TextStyle style, String href) {
        int i = 0;
        while (i < value.length()) {
            if (value.charAt(i) == '\\' && i + 1 < value.length()) {
                out.append(String.valueOf(value.charAt(i + 1)), style, href);
                i += 2;
                continue;
            }
            if (value.startsWith("[![", i)) {
                int labelEnd = value.indexOf(']', i + 3);
                int imageEnd = labelEnd >= 0 && labelEnd + 1 < value.length()
                        && value.charAt(labelEnd + 1) == '(' ? value.indexOf(')', labelEnd + 2) : -1;
                int outerEnd = imageEnd >= 0 && imageEnd + 1 < value.length()
                        && value.charAt(imageEnd + 1) == ']' ? imageEnd + 1 : -1;
                int linkEnd = outerEnd >= 0 && outerEnd + 1 < value.length()
                        && value.charAt(outerEnd + 1) == '(' ? value.indexOf(')', outerEnd + 2) : -1;
                if (linkEnd > 0) {
                    out.appendImage(value.substring(labelEnd + 2, imageEnd),
                            value.substring(outerEnd + 2, linkEnd));
                    i = linkEnd + 1;
                    continue;
                }
            }
            if (value.startsWith("![", i)) {
                int labelEnd = value.indexOf(']', i + 2);
                int urlEnd = labelEnd >= 0 && labelEnd + 1 < value.length()
                        && value.charAt(labelEnd + 1) == '(' ? value.indexOf(')', labelEnd + 2) : -1;
                if (urlEnd > 0) {
                    out.appendImage(value.substring(labelEnd + 2, urlEnd), href);
                    i = urlEnd + 1;
                    continue;
                }
            }
            if (value.charAt(i) == '[') {
                int labelEnd = value.indexOf(']', i + 1);
                int urlEnd = labelEnd >= 0 && labelEnd + 1 < value.length()
                        && value.charAt(labelEnd + 1) == '(' ? value.indexOf(')', labelEnd + 2) : -1;
                if (urlEnd > 0) {
                    appendMarkdownInline(out, value.substring(i + 1, labelEnd), style,
                            value.substring(labelEnd + 2, urlEnd));
                    i = urlEnd + 1;
                    continue;
                }
            }
            if (value.charAt(i) == '`') {
                int end = value.indexOf('`', i + 1);
                if (end > i + 1) {
                    out.append(value.substring(i + 1, end), style.withMonospace(true), href);
                    i = end + 1;
                    continue;
                }
            }
            String marker = markerAt(value, i);
            if (marker != null) {
                int end = value.indexOf(marker, i + marker.length());
                if (end > i + marker.length()) {
                    TextStyle nested = style;
                    if ("~~".equals(marker)) {
                        nested = nested.withStrike(true);
                    } else if (marker.length() == 2) {
                        nested = nested.withBold(true);
                    } else {
                        nested = nested.withItalic(true);
                    }
                    appendMarkdownInline(out, value.substring(i + marker.length(), end), nested, href);
                    i = end + marker.length();
                    continue;
                }
            }
            int next = nextMarkdownSpecial(value, i + 1);
            out.append(value.substring(i, next), style, href);
            i = next;
        }
    }

    private static int nextMarkdownSpecial(String value, int from) {
        int i = from;
        while (i < value.length()) {
            char c = value.charAt(i);
            if (c == '\\' || c == '[' || c == '!' || c == '*' || c == '_' || c == '~' || c == '`') {
                break;
            }
            i++;
        }
        return i;
    }

    private static void appendAsciiDocInline(ModelBuilder out, String value, TextStyle style, String href) {
        int i = 0;
        while (i < value.length()) {
            if (value.charAt(i) == '\\' && i + 1 < value.length()) {
                out.append(String.valueOf(value.charAt(i + 1)), style, href);
                i += 2;
                continue;
            }
            if (value.startsWith("image:", i)) {
                int open = value.indexOf('[', i + 6);
                int close = open < 0 ? -1 : value.indexOf(']', open + 1);
                if (open > i + 6 && close >= 0) {
                    out.appendImage(value.substring(i + 6, open), href);
                    i = close + 1;
                    continue;
                }
            }
            if (value.startsWith("http://", i) || value.startsWith("https://", i)) {
                int open = value.indexOf('[', i);
                int close = open < 0 ? -1 : findMatchingBracket(value, open);
                if (open > i && close > open) {
                    String target = value.substring(i, open);
                    String label = value.substring(open + 1, close);
                    if (label.startsWith("image:") && label.endsWith("[]")) {
                        out.appendImage(label.substring(6, label.length() - 2), target);
                        i = close + 1;
                        continue;
                    }
                    appendAsciiDocInline(out, label, style, target);
                    i = close + 1;
                    continue;
                }
            }
            if (value.startsWith("[line-through]#", i) || value.startsWith("[underline]#", i)) {
                boolean strike = value.startsWith("[line-through]#", i);
                int prefix = strike ? 15 : 12;
                int end = value.indexOf('#', i + prefix);
                if (end > i + prefix) {
                    TextStyle nested = strike ? style.withStrike(true) : style.withUnderline(true);
                    appendAsciiDocInline(out, value.substring(i + prefix, end), nested, href);
                    i = end + 1;
                    continue;
                }
            }
            char marker = value.charAt(i);
            if (marker == '*' || marker == '_' || marker == '+') {
                int end = value.indexOf(marker, i + 1);
                if (end > i + 1) {
                    TextStyle nested = marker == '*' ? style.withBold(true)
                            : (marker == '_' ? style.withItalic(true) : style.withMonospace(true));
                    appendAsciiDocInline(out, value.substring(i + 1, end), nested, href);
                    i = end + 1;
                    continue;
                }
            }
            int next = i + 1;
            while (next < value.length()) {
                char c = value.charAt(next);
                if (c == '\\' || c == '*' || c == '_' || c == '+'
                        || value.startsWith("[line-through]#", next)
                        || value.startsWith("[underline]#", next) || value.startsWith("http://", next)
                        || value.startsWith("https://", next) || value.startsWith("image:", next)) {
                    break;
                }
                next++;
            }
            out.append(value.substring(i, next), style, href);
            i = next;
        }
    }

    private static int findMatchingBracket(String value, int open) {
        int depth = 0;
        for (int i = open; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '\\') {
                i++;
            } else if (c == '[') {
                depth++;
            } else if (c == ']' && --depth == 0) {
                return i;
            }
        }
        return -1;
    }

    private static final class ModelBuilder {
        private final StringBuilder text = new StringBuilder();
        private final List<TextStyle> styles = new ArrayList<TextStyle>();
        private final List<RichBlocks.BlockAttr> blocks = new ArrayList<RichBlocks.BlockAttr>();
        private final List<String> links = new ArrayList<String>();
        private final List<String> imageSources = new ArrayList<String>();

        void startBlock(int type, int listType) {
            if (!blocks.isEmpty()) {
                append("\n", TextStyle.DEFAULT, null);
            }
            RichBlocks.BlockAttr block = new RichBlocks.BlockAttr();
            block.type = type;
            block.listType = listType;
            blocks.add(block);
        }

        void append(String value, TextStyle style, String href) {
            for (int i = 0; i < value.length(); i++) {
                text.append(value.charAt(i));
                styles.add(style == null ? TextStyle.DEFAULT : style);
                links.add(href);
                imageSources.add(null);
            }
        }

        void appendImage(String source, String href) {
            text.append('\uFFFC');
            styles.add(TextStyle.DEFAULT);
            links.add(href);
            imageSources.add(source);
        }

        HtmlImporter.Result result(boolean blockContent) {
            if (blocks.isEmpty()) {
                blocks.add(new RichBlocks.BlockAttr());
            }
            return new HtmlImporter.Result(text.toString(), styles, blocks, links, imageSources,
                    blockContent);
        }
    }

    private static String asciiDocToHtml(String source) {
        List<String> lines = lines(source);
        StringBuilder out = new StringBuilder();
        String list = null;
        boolean literal = false;
        StringBuilder code = new StringBuilder();
        for (String line : lines) {
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
                    if (!state.skip) {
                        if (skipFallback <= 0) {
                            syncRtfStyle(out, rendered, state);
                            appendEscaped(out, next);
                        }
                        skipFallback--;
                    }
                    continue;
                }
                if (next == '\'') {
                    i++;
                    if (i + 1 < source.length()) {
                        int value = hex(source.charAt(i), source.charAt(i + 1));
                        i += 2;
                        if (!state.skip && value >= 0) {
                            if (skipFallback <= 0) {
                                syncRtfStyle(out, rendered, state);
                                appendEscaped(out, (char) value);
                            }
                            skipFallback--;
                        }
                    }
                    continue;
                }
                if (!isAsciiLetter(next)) {
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
                while (i < source.length() && isAsciiLetter(source.charAt(i))) {
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

    private static HtmlImporter.Result parseRtf(String source) {
        ModelBuilder out = new ModelBuilder();
        out.startBlock(RichBlocks.PARAGRAPH, RichBlocks.LIST_NONE);
        RtfState state = new RtfState();
        List<RtfState> stack = new ArrayList<RtfState>();
        int skipFallback = 0;
        for (int i = 0; i < source.length();) {
            char c = source.charAt(i++);
            if (c == '{') {
                stack.add(state.copy());
                continue;
            }
            if (c == '}') {
                if (!stack.isEmpty()) state = stack.remove(stack.size() - 1);
                continue;
            }
            if (c == '\\') {
                if (i >= source.length()) break;
                char next = source.charAt(i);
                if (next == '\\' || next == '{' || next == '}') {
                    i++;
                    if (!state.skip && skipFallback-- <= 0) {
                        out.append(String.valueOf(next), rtfStyle(state), null);
                    }
                    continue;
                }
                if (next == '\'') {
                    i++;
                    if (i + 1 < source.length()) {
                        int value = hex(source.charAt(i), source.charAt(i + 1));
                        i += 2;
                        if (!state.skip && value >= 0 && skipFallback-- <= 0) {
                            out.append(String.valueOf((char) value), rtfStyle(state), null);
                        }
                    }
                    continue;
                }
                if (!isAsciiLetter(next)) {
                    i++;
                    if (next == '*') {
                        state.skip = true;
                    } else if (!state.skip && (next == '~' || next == '_' || next == '-')) {
                        out.append(next == '~' ? " " : "-", rtfStyle(state), null);
                    }
                    continue;
                }
                int wordStart = i;
                while (i < source.length() && isAsciiLetter(source.charAt(i))) {
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
                } else if (("par".equals(word) || "line".equals(word)) && !state.skip) {
                    out.startBlock(RichBlocks.PARAGRAPH, RichBlocks.LIST_NONE);
                } else if ("tab".equals(word) && !state.skip) {
                    out.append("\t", rtfStyle(state), null);
                } else if ("uc".equals(word) && hasNumber) {
                    state.unicodeFallback = Math.max(0, number);
                } else if ("u".equals(word) && hasNumber && !state.skip) {
                    int unicode = number < 0 ? number + 65536 : number;
                    out.append(String.valueOf((char) unicode), rtfStyle(state), null);
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
            out.append(String.valueOf(c), rtfStyle(state), null);
        }
        return out.result(true);
    }

    private static TextStyle rtfStyle(RtfState state) {
        return TextStyle.DEFAULT.withBold(state.bold).withItalic(state.italic)
                .withUnderline(state.underline).withStrike(state.strike);
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

    private static boolean isAsciiLetter(char value) {
        return value >= 'A' && value <= 'Z' || value >= 'a' && value <= 'z';
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
