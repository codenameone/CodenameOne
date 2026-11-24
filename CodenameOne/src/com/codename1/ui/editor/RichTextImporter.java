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
import com.codename1.wordprocessing.Document;
import com.codename1.wordprocessing.RichTextDocuments;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/// Parses supported rich-text interchange formats into the shared editor model. HTML delegates to the
/// framework HTML/XML parser; Markdown and RTF are read by `com.codename1.wordprocessing`, the one
/// implementation of each in the framework; AsciiDoc is parsed here.
public final class RichTextImporter {
    private RichTextImporter() {
    }

    /// Parses rich text into the editor model. AsciiDoc is built directly; Markdown and RTF come
    /// through the word processing readers.
    public static HtmlImporter.Result parse(String content, RichTextFormat format) {
        String value = content == null ? "" : content;
        if (format == RichTextFormat.HTML) {
            return HtmlImporter.parse(value);
        }
        if (format == RichTextFormat.MARKDOWN || format == RichTextFormat.RTF) {
            return readDocument(value, format);
        }
        if (format == RichTextFormat.ASCIIDOC) {
            return parseAsciiDoc(value);
        }
        ModelBuilder builder = new ModelBuilder();
        List<String> plainLines = lines(value);
        for (String plainLine : plainLines) {
            builder.startBlock(RichBlocks.PARAGRAPH, RichBlocks.LIST_NONE);
            builder.append(plainLine, TextStyle.DEFAULT, null);
        }
        return builder.result(false);
    }

    /// Converts HTML produced by a browser fallback into another rich-text format through the shared
    /// document model. Pure editors do not use this path for Markdown or AsciiDoc input.
    public static String fromHtml(String html, RichTextFormat format) {
        return serialize(HtmlImporter.parse(html == null ? "" : html), format);
    }

    /// Converts between non-HTML formats through the native rich-text model. This preserves source
    /// mode in browser fallbacks without using HTML as an intermediate representation.
    public static String convert(String content, RichTextFormat sourceFormat, RichTextFormat targetFormat) {
        RichTextFormat source = sourceFormat == null ? RichTextFormat.PLAIN_TEXT : sourceFormat;
        RichTextFormat target = targetFormat == null ? RichTextFormat.PLAIN_TEXT : targetFormat;
        if (source == target) {
            return content == null ? "" : content;
        }
        return serialize(parse(content, source), target);
    }

    private static String serialize(HtmlImporter.Result result, RichTextFormat format) {
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

    /// Converts content to an HTML fragment. HTML input is returned unchanged and plain text is escaped.
    public static String toHtml(String content, RichTextFormat format) {
        String value = content == null ? "" : content;
        if (format == null || format == RichTextFormat.PLAIN_TEXT) {
            return "<p>" + escape(value).replace("\n", "<br>") + "</p>";
        }
        if (format == RichTextFormat.HTML) {
            return value;
        }
        if (format == RichTextFormat.ASCIIDOC) {
            return asciiDocToHtml(value);
        }
        return serialize(readDocument(value, format), RichTextFormat.HTML);
    }

    /// Markdown and RTF are read by `com.codename1.wordprocessing`, the one implementation of
    /// each in the framework, and mapped straight onto the editor model.
    private static HtmlImporter.Result readDocument(String value, RichTextFormat format) {
        try {
            Document d = format == RichTextFormat.RTF ? RichTextDocuments.readRtf(value)
                    : RichTextDocuments.readMarkdown(value);
            return DocumentMapping.toEditor(d);
        } catch (IOException ex) {
            // damaged input still shows what text it has rather than nothing
            return parse(value, RichTextFormat.PLAIN_TEXT);
        }
    }

    private static HtmlImporter.Result parseAsciiDoc(String source) {
        ModelBuilder out = new ModelBuilder();
        List<String> sourceLines = lines(source);
        boolean literal = false;
        for (String line : sourceLines) {
            String trimmed = line.trim();
            boolean fence = "----".equals(trimmed) || "....".equals(trimmed);
            if (fence) {
                literal = !literal;
                continue;
            }
            if (line.startsWith("[source")) {
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
            int heading = headingLevel(line, '=');
            if (heading > 0 && heading <= 6 && line.length() > heading && line.charAt(heading) == ' ') {
                type = RichBlocks.H1 + heading - 1;
                inline = line.substring(heading + 1);
            } else if (line.startsWith("> ")) {
                type = RichBlocks.BLOCKQUOTE;
                inline = line.substring(2);
            } else {
                String list = asciiDocList(line);
                if (list != null) {
                    listType = "ol".equals(list) ? RichBlocks.LIST_ORDERED : RichBlocks.LIST_UNORDERED;
                    inline = line.substring(Math.min(2, line.length())).trim();
                }
            }
            out.startBlock(type, listType);
            appendAsciiDocInline(out, inline, TextStyle.DEFAULT, null);
        }
        return out.result(true);
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
            } else if (c == ']') {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    private static final class ModelBuilder {
        @SuppressWarnings("PMD.AvoidStringBufferField")
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
