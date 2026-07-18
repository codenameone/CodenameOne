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

import java.util.List;

/// Serializes the native rich-text model directly to its requested interchange format.
public final class RichTextSerializer {
    private RichTextSerializer() {
    }

    /// Serializes a native editor document into the requested interchange format.
    public static String serialize(EditorDocument doc, InlineStyles inline, RichBlocks blocks,
            List<String> links, List<String> imageSources, RichTextFormat format) {
        if (format == RichTextFormat.HTML) {
            return HtmlSerializer.serialize(doc, inline, blocks, links, imageSources);
        }
        if (format == RichTextFormat.MARKDOWN) {
            return serializeMarkup(doc, inline, blocks, links, imageSources, false);
        }
        if (format == RichTextFormat.ASCIIDOC) {
            return serializeMarkup(doc, inline, blocks, links, imageSources, true);
        }
        if (format == RichTextFormat.RTF) {
            return serializeRtf(doc, inline);
        }
        return doc.getText().replace("\uFFFC", "");
    }

    private static String serializeMarkup(EditorDocument doc, InlineStyles inline, RichBlocks blocks,
            List<String> links, List<String> imageSources, boolean asciidoc) {
        StringBuilder out = new StringBuilder();
        boolean inPre = false;
        for (int line = 0; line < doc.getLineCount(); line++) {
            RichBlocks.BlockAttr block = blocks.get(line);
            boolean pre = block.type == RichBlocks.PRE;
            if (pre != inPre) {
                if (out.length() > 0 && out.charAt(out.length() - 1) != '\n') {
                    out.append('\n');
                }
                out.append(asciidoc ? "----\n" : "```\n");
                inPre = pre;
            }
            if (line > 0 && out.length() > 0 && out.charAt(out.length() - 1) != '\n') {
                out.append('\n');
            }
            if (!pre) {
                appendBlockPrefix(out, block, asciidoc);
            }
            appendInline(out, doc, inline, links, imageSources, line, asciidoc, pre);
        }
        if (inPre) {
            if (out.length() > 0 && out.charAt(out.length() - 1) != '\n') {
                out.append('\n');
            }
            out.append(asciidoc ? "----" : "```");
        }
        return out.toString();
    }

    private static void appendBlockPrefix(StringBuilder out, RichBlocks.BlockAttr block, boolean asciidoc) {
        for (int i = 0; i < block.indent; i++) {
            out.append("  ");
        }
        if (block.listType == RichBlocks.LIST_ORDERED) {
            out.append(asciidoc ? ". " : "1. ");
        } else if (block.listType == RichBlocks.LIST_UNORDERED) {
            out.append(asciidoc ? "* " : "- ");
        } else if (block.type >= RichBlocks.H1 && block.type <= RichBlocks.H1 + 5) {
            char marker = asciidoc ? '=' : '#';
            int level = block.type - RichBlocks.H1 + 1;
            for (int i = 0; i < level; i++) {
                out.append(marker);
            }
            out.append(' ');
        } else if (block.type == RichBlocks.BLOCKQUOTE) {
            out.append("> ");
        }
    }

    private static void appendInline(StringBuilder out, EditorDocument doc, InlineStyles inline,
            List<String> links, List<String> imageSources, int line, boolean asciidoc, boolean literal) {
        int start = doc.getLineStart(line);
        String text = doc.getLineText(line);
        int column = 0;
        while (column < text.length()) {
            int offset = start + column;
            String source = valueAt(imageSources, offset);
            String href = valueAt(links, offset);
            if (text.charAt(column) == '\uFFFC' && source != null) {
                String image = asciidoc ? "image:" + source + "[]" : "![image](" + source + ")";
                if (href != null) {
                    out.append(asciidoc ? href + "[" + image + "]" : "[" + image + "](" + href + ")");
                } else {
                    out.append(image);
                }
                column++;
                continue;
            }
            TextStyle style = inline.styleAt(offset);
            int end = column + 1;
            while (end < text.length() && text.charAt(end) != '\uFFFC'
                    && style.equals(inline.styleAt(start + end))
                    && equal(href, valueAt(links, start + end))) {
                end++;
            }
            String value = text.substring(column, end);
            if (!literal) {
                if (style.isMonospace()) {
                    value = (asciidoc ? "+" : "`") + escapeInlineCode(value, asciidoc)
                            + (asciidoc ? "+" : "`");
                } else {
                    value = escapeMarkup(value, asciidoc);
                }
                if (style.isStrike()) {
                    value = asciidoc ? "[line-through]#" + value + "#" : "~~" + value + "~~";
                }
                if (style.isUnderline() && asciidoc) {
                    value = "[underline]#" + value + "#";
                }
                if (style.isItalic()) {
                    value = (asciidoc ? "_" : "*") + value + (asciidoc ? "_" : "*");
                }
                if (style.isBold()) {
                    value = (asciidoc ? "*" : "**") + value + (asciidoc ? "*" : "**");
                }
                if (href != null) {
                    value = asciidoc ? href + "[" + value + "]" : "[" + value + "](" + href + ")";
                }
            }
            out.append(value);
            column = end;
        }
    }

    private static String serializeRtf(EditorDocument doc, InlineStyles inline) {
        StringBuilder out = new StringBuilder("{\\rtf1\\ansi ");
        TextStyle active = TextStyle.DEFAULT;
        for (int i = 0; i < doc.length(); i++) {
            char c = doc.charAt(i);
            if (c == '\n') {
                out.append("\\par\n");
                continue;
            }
            TextStyle style = inline.styleAt(i);
            if (!style.equals(active)) {
                appendRtfSwitch(out, "b", active.isBold(), style.isBold());
                appendRtfSwitch(out, "i", active.isItalic(), style.isItalic());
                appendRtfSwitch(out, "ul", active.isUnderline(), style.isUnderline());
                appendRtfSwitch(out, "strike", active.isStrike(), style.isStrike());
                active = style;
            }
            if (c == '\uFFFC') {
                continue;
            }
            if (c == '\\' || c == '{' || c == '}') {
                out.append('\\').append(c);
            } else if (c > 127) {
                // The RTF unicode keyword (backslash-u) takes a SIGNED 16-bit value; values
                // above 32767 (surrogate halves, high BMP chars) must be emitted as N-65536
                // or Word-family consumers reject the document
                int code = c;
                if (code > 32767) {
                    code -= 65536;
                }
                out.append("\\u").append(code).append('?');
            } else {
                out.append(c);
            }
        }
        return out.append('}').toString();
    }

    private static void appendRtfSwitch(StringBuilder out, String name, boolean oldValue, boolean newValue) {
        if (oldValue != newValue) {
            out.append('\\').append(name);
            if (!newValue) {
                out.append('0');
            }
            out.append(' ');
        }
    }

    private static String escapeMarkup(String value, boolean asciidoc) {
        String specials = asciidoc ? "\\*_[#+" : "\\`*_{}[]()#+-.!~>";
        StringBuilder out = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (specials.indexOf(c) >= 0) {
                out.append('\\');
            }
            out.append(c);
        }
        return out.toString();
    }

    private static String escapeInlineCode(String value, boolean asciidoc) {
        char delimiter = asciidoc ? '+' : '`';
        StringBuilder out = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == delimiter || c == '\\') {
                out.append('\\');
            }
            out.append(c);
        }
        return out.toString();
    }

    private static String valueAt(List<String> values, int index) {
        return index >= 0 && index < values.size() ? values.get(index) : null;
    }

    private static boolean equal(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }
}
