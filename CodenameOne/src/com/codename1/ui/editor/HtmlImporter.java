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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// Parses an HTML string into the pure rich text editor model (plain text plus per character
/// `TextStyle` and per paragraph `RichBlocks.BlockAttr`). It uses a small, tolerant streaming HTML
/// tokenizer (rather than the framework's `HTMLParser`, which is coupled to an `HTMLComponent`) and maps
/// the common inline and block tags onto the editor model. It handles the HTML that `HtmlSerializer`
/// emits plus common external markup (headings, paragraphs, lists, bold / italic / underline / strike,
/// colored spans and links).
public final class HtmlImporter {
    /// The imported content.
    public static final class Result {
        private final String text;
        private final List<TextStyle> styles;
        private final List<RichBlocks.BlockAttr> blocks;
        private final List<String> links;
        private final List<String> imageSources;
        private final boolean blockContent;

        Result(String text, List<TextStyle> styles, List<RichBlocks.BlockAttr> blocks,
                List<String> links, List<String> imageSources, boolean blockContent) {
            this.text = text;
            this.styles = styles;
            this.blocks = blocks;
            this.links = links;
            this.imageSources = imageSources;
            this.blockContent = blockContent;
        }

        /// The plain text.
        public String getText() {
            return text;
        }

        /// One style per character.
        public List<TextStyle> getStyles() {
            return styles;
        }

        /// One block attribute per paragraph.
        public List<RichBlocks.BlockAttr> getBlocks() {
            return blocks;
        }

        /// One hyperlink target per character, null outside links.
        public List<String> getLinks() {
            return links;
        }

        /// One image source per character, non-null only for object-replacement characters.
        public List<String> getImageSources() {
            return imageSources;
        }

        /// True when the fragment explicitly contained block-level markup.
        public boolean hasBlockContent() {
            return blockContent;
        }
    }

    private final StringBuilder text = new StringBuilder(); // NOPMD - intentional owned buffer
    private final List<TextStyle> styles = new ArrayList<TextStyle>();
    private final List<String> links = new ArrayList<String>();
    private final List<String> imageSources = new ArrayList<String>();
    private final List<RichBlocks.BlockAttr> paras = new ArrayList<RichBlocks.BlockAttr>();
    private boolean freshParagraph = true;
    private boolean lastWasSpace = true;

    private TextStyle current = TextStyle.DEFAULT;
    private final List<TextStyle> styleStack = new ArrayList<TextStyle>();
    private final List<String> linkStack = new ArrayList<String>();
    private String currentHref;
    private boolean blockContent;
    // list context stack: each entry is {listType, indent}
    private final List<int[]> listStack = new ArrayList<int[]>();

    private HtmlImporter() {
    }

    /// Parses HTML into an editor model result.
    public static Result parse(String html) {
        HtmlImporter imp = new HtmlImporter();
        imp.tokenize(html == null ? "" : html);
        if (imp.paras.isEmpty()) {
            imp.paras.add(new RichBlocks.BlockAttr());
        }
        return new Result(imp.text.toString(), imp.styles, imp.paras, imp.links, imp.imageSources,
                imp.blockContent);
    }

    // ---- streaming tokenizer ----

    private void tokenize(String html) {
        int i = 0;
        int n = html.length();
        while (i < n) {
            char c = html.charAt(i);
            if (c == '<') {
                if (html.regionMatches(i, "<!--", 0, 4)) {
                    int end = html.indexOf("-->", i + 4);
                    i = end < 0 ? n : end + 3;
                    continue;
                }
                int gt = html.indexOf('>', i);
                if (gt < 0) {
                    appendText(html.substring(i));
                    break;
                }
                String tagBody = html.substring(i + 1, gt);
                i = gt + 1;
                if (tagBody.length() == 0) {
                    continue;
                }
                if (tagBody.charAt(0) == '/') {
                    endTag(tagBody.substring(1).trim().toLowerCase());
                } else {
                    boolean selfClose = tagBody.endsWith("/");
                    if (selfClose) {
                        tagBody = tagBody.substring(0, tagBody.length() - 1);
                    }
                    parseStartTag(tagBody);
                }
            } else {
                int lt = html.indexOf('<', i);
                if (lt < 0) {
                    lt = n;
                }
                appendText(decodeEntities(html.substring(i, lt)));
                i = lt;
            }
        }
    }

    private void parseStartTag(String tagBody) {
        int sp = 0;
        while (sp < tagBody.length() && !isSpace(tagBody.charAt(sp))) {
            sp++;
        }
        String name = tagBody.substring(0, sp).toLowerCase();
        Map<String, String> attrs = parseAttrs(tagBody.substring(sp));
        startTag(name, attrs);
    }

    private static boolean isSpace(char c) {
        return c == ' ' || c == '\t' || c == '\n' || c == '\r';
    }

    private Map<String, String> parseAttrs(String s) {
        Map<String, String> attrs = new HashMap<String, String>();
        int i = 0;
        int n = s.length();
        while (i < n) {
            while (i < n && isSpace(s.charAt(i))) {
                i++;
            }
            int start = i;
            while (i < n && s.charAt(i) != '=' && !isSpace(s.charAt(i))) {
                i++;
            }
            if (i == start) {
                break;
            }
            String key = s.substring(start, i).toLowerCase();
            while (i < n && isSpace(s.charAt(i))) {
                i++;
            }
            String value = "";
            if (i < n && s.charAt(i) == '=') {
                i++;
                while (i < n && isSpace(s.charAt(i))) {
                    i++;
                }
                if (i < n && (s.charAt(i) == '"' || s.charAt(i) == '\'')) {
                    char q = s.charAt(i);
                    i++;
                    int vs = i;
                    while (i < n && s.charAt(i) != q) {
                        i++;
                    }
                    value = s.substring(vs, i);
                    if (i < n) {
                        i++;
                    }
                } else {
                    int vs = i;
                    while (i < n && !isSpace(s.charAt(i))) {
                        i++;
                    }
                    value = s.substring(vs, i);
                }
            }
            attrs.put(key, decodeEntities(value));
        }
        return attrs;
    }

    // ---- tag handling ----

    private void startTag(String name, Map<String, String> attrs) {
        if ("br".equals(name)) {
            blockContent = true;
            newParagraph(currentBlockCtx());
            return;
        }
        if ("img".equals(name)) {
            appendImage(attrs.get("src"));
            return;
        }
        if ("hr".equals(name)) {
            return;
        }
        if ("ol".equals(name) || "ul".equals(name)) {
            blockContent = true;
            listStack.add(new int[]{"ol".equals(name) ? RichBlocks.LIST_ORDERED : RichBlocks.LIST_UNORDERED, 0});
            return;
        }
        if (isBlockTag(name) || "li".equals(name)) {
            blockContent = true;
            startBlock(blockAttrFor(name, attrs));
            return;
        }
        // inline tag
        if (isInlineTag(name)) {
            styleStack.add(current);
            linkStack.add(currentHref);
            current = applyInlineTag(name, current, attrs);
            if ("a".equals(name)) {
                currentHref = attrs.get("href");
            }
        }
    }

    private void endTag(String name) {
        if ("ol".equals(name) || "ul".equals(name)) {
            if (!listStack.isEmpty()) {
                listStack.remove(listStack.size() - 1);
            }
            return;
        }
        if (isInlineTag(name)) {
            if (!styleStack.isEmpty()) {
                current = styleStack.remove(styleStack.size() - 1);
            }
            if (!linkStack.isEmpty()) {
                currentHref = linkStack.remove(linkStack.size() - 1);
            }
        }
    }

    private static boolean isBlockTag(String tag) {
        return "p".equals(tag) || "div".equals(tag) || "pre".equals(tag) || "blockquote".equals(tag)
                || (tag.length() == 2 && tag.charAt(0) == 'h' && tag.charAt(1) >= '1' && tag.charAt(1) <= '6');
    }

    private static boolean isInlineTag(String tag) {
        return "b".equals(tag) || "strong".equals(tag) || "i".equals(tag) || "em".equals(tag)
                || "u".equals(tag) || "s".equals(tag) || "strike".equals(tag) || "del".equals(tag)
                || "a".equals(tag) || "span".equals(tag) || "font".equals(tag);
    }

    private RichBlocks.BlockAttr currentBlockCtx() {
        RichBlocks.BlockAttr a = new RichBlocks.BlockAttr();
        if (!listStack.isEmpty()) {
            int[] top = listStack.get(listStack.size() - 1);
            a.listType = top[0];
            a.indent = listStack.size() - 1;
        }
        return a;
    }

    private RichBlocks.BlockAttr blockAttrFor(String tag, Map<String, String> attrs) {
        RichBlocks.BlockAttr attr = currentBlockCtx();
        if (tag.length() == 2 && tag.charAt(0) == 'h') {
            attr.type = RichBlocks.H1 + (tag.charAt(1) - '1');
            attr.listType = RichBlocks.LIST_NONE;
        } else if ("pre".equals(tag)) {
            attr.type = RichBlocks.PRE;
            attr.listType = RichBlocks.LIST_NONE;
        } else if ("blockquote".equals(tag)) {
            attr.type = RichBlocks.BLOCKQUOTE;
            attr.listType = RichBlocks.LIST_NONE;
        } else if ("li".equals(tag)) {
            attr.type = RichBlocks.PARAGRAPH;
        } else {
            attr.type = RichBlocks.PARAGRAPH;
            if (!"li".equals(tag)) {
                attr.listType = RichBlocks.LIST_NONE;
            }
        }
        String styleAttr = attrs.get("style");
        if (styleAttr != null) {
            String align = cssValue(styleAttr, "text-align");
            if ("center".equals(align)) {
                attr.align = RichBlocks.ALIGN_CENTER;
            } else if ("right".equals(align)) {
                attr.align = RichBlocks.ALIGN_RIGHT;
            } else if ("left".equals(align)) {
                attr.align = RichBlocks.ALIGN_LEFT;
            }
        }
        String dataIndent = attrs.get("data-indent");
        if (dataIndent != null) {
            try {
                attr.indent = Integer.parseInt(dataIndent.trim());
            } catch (NumberFormatException err) { // NOPMD - malformed input, intentionally ignored
                // ignore
            }
        }
        return attr;
    }

    private TextStyle applyInlineTag(String tag, TextStyle style, Map<String, String> attrs) {
        if ("b".equals(tag) || "strong".equals(tag)) {
            return style.withBold(true);
        }
        if ("i".equals(tag) || "em".equals(tag)) {
            return style.withItalic(true);
        }
        if ("u".equals(tag)) {
            return style.withUnderline(true);
        }
        if ("s".equals(tag) || "strike".equals(tag) || "del".equals(tag)) {
            return style.withStrike(true);
        }
        if ("a".equals(tag)) {
            return style.withUnderline(true).withForeColor(0x1a73e8);
        }
        if ("span".equals(tag) || "font".equals(tag)) {
            TextStyle s = style;
            String styleAttr = attrs.get("style");
            if (styleAttr != null) {
                int color = parseColor(cssValue(styleAttr, "color"));
                if (color >= 0) {
                    s = s.withForeColor(color);
                }
                int bg = parseColor(cssValue(styleAttr, "background-color"));
                if (bg < 0) {
                    bg = parseColor(cssValue(styleAttr, "background"));
                }
                if (bg >= 0) {
                    s = s.withHighlight(bg);
                }
                String fw = cssValue(styleAttr, "font-weight");
                if ("bold".equals(fw) || "700".equals(fw)) {
                    s = s.withBold(true);
                }
                String fs = cssValue(styleAttr, "font-style");
                if ("italic".equals(fs)) {
                    s = s.withItalic(true);
                }
                int level = fontSizeLevel(cssValue(styleAttr, "font-size"));
                if (level > 0) {
                    s = s.withFontSizeLevel(level);
                }
            }
            String colorAttr = attrs.get("color");
            if (colorAttr != null) {
                int color = parseColor(colorAttr);
                if (color >= 0) {
                    s = s.withForeColor(color);
                }
            }
            return s;
        }
        return style;
    }

    // ---- model builders ----

    private void startBlock(RichBlocks.BlockAttr attr) {
        if (!freshParagraph) {
            text.append('\n');
            styles.add(TextStyle.DEFAULT);
            links.add(null);
            imageSources.add(null);
            paras.add(attr);
            freshParagraph = true;
            lastWasSpace = true;
        } else if (paras.isEmpty()) {
            paras.add(attr);
        } else {
            paras.set(paras.size() - 1, attr);
        }
    }

    private void newParagraph(RichBlocks.BlockAttr attr) {
        if (paras.isEmpty()) {
            paras.add(new RichBlocks.BlockAttr());
        }
        text.append('\n');
        styles.add(TextStyle.DEFAULT);
        links.add(null);
        imageSources.add(null);
        paras.add(attr);
        freshParagraph = true;
        lastWasSpace = true;
    }

    private void appendText(String t) {
        if (t == null || t.length() == 0) {
            return;
        }
        if (paras.isEmpty()) {
            paras.add(new RichBlocks.BlockAttr());
        }
        for (int i = 0; i < t.length(); i++) {
            char c = t.charAt(i);
            if (c == ' ' || c == '\n' || c == '\r' || c == '\t') {
                if (lastWasSpace) {
                    continue;
                }
                text.append(' ');
                styles.add(current);
                links.add(currentHref);
                imageSources.add(null);
                lastWasSpace = true;
                freshParagraph = false;
            } else {
                text.append(c);
                styles.add(current);
                links.add(currentHref);
                imageSources.add(null);
                lastWasSpace = false;
                freshParagraph = false;
            }
        }
    }

    private void appendImage(String source) {
        if (paras.isEmpty()) {
            paras.add(new RichBlocks.BlockAttr());
        }
        text.append('\uFFFC');
        styles.add(current);
        links.add(currentHref);
        imageSources.add(source == null ? "" : source);
        lastWasSpace = false;
        freshParagraph = false;
    }

    // ---- entities ----

    private static String decodeEntities(String s) {
        if (s.indexOf('&') < 0) {
            return s;
        }
        StringBuilder sb = new StringBuilder(s.length());
        int i = 0;
        int n = s.length();
        while (i < n) {
            char c = s.charAt(i);
            if (c == '&') {
                int semi = s.indexOf(';', i);
                if (semi > i && semi - i <= 10) {
                    String ent = s.substring(i + 1, semi);
                    String rep = entity(ent);
                    if (rep != null) {
                        sb.append(rep);
                        i = semi + 1;
                        continue;
                    }
                }
            }
            sb.append(c);
            i++;
        }
        return sb.toString();
    }

    // Common named HTML entities (typography and symbols) as name/replacement pairs. Values use unicode
    // escapes to keep the source ASCII only.
    private static final String[] NAMED_ENTITIES = {
        "mdash", "\u2014", "ndash", "\u2013", "hellip", "\u2026",
        "lsquo", "\u2018", "rsquo", "\u2019", "ldquo", "\u201C", "rdquo", "\u201D",
        "laquo", "\u00AB", "raquo", "\u00BB", "bull", "\u2022", "middot", "\u00B7",
        "copy", "\u00A9", "reg", "\u00AE", "trade", "\u2122",
        "deg", "\u00B0", "plusmn", "\u00B1", "times", "\u00D7", "divide", "\u00F7",
        "euro", "\u20AC", "pound", "\u00A3", "cent", "\u00A2", "yen", "\u00A5",
        "sect", "\u00A7", "para", "\u00B6"
    };

    private static String entity(String ent) {
        if ("amp".equals(ent)) {
            return "&";
        }
        if ("lt".equals(ent)) {
            return "<";
        }
        if ("gt".equals(ent)) {
            return ">";
        }
        if ("quot".equals(ent)) {
            return "\"";
        }
        if ("apos".equals(ent)) {
            return "'";
        }
        if ("nbsp".equals(ent)) {
            return " ";
        }
        for (int k = 0; k < NAMED_ENTITIES.length; k += 2) {
            if (NAMED_ENTITIES[k].equals(ent)) {
                return NAMED_ENTITIES[k + 1];
            }
        }
        if (ent.length() > 1 && ent.charAt(0) == '#') {
            try {
                int code;
                if (ent.charAt(1) == 'x' || ent.charAt(1) == 'X') {
                    code = Integer.parseInt(ent.substring(2), 16);
                } else {
                    code = Integer.parseInt(ent.substring(1));
                }
                return String.valueOf((char) code);
            } catch (NumberFormatException err) {
                return null;
            }
        }
        return null;
    }

    // ---- css helpers ----

    private static String cssValue(String styleAttr, String prop) {
        String lower = styleAttr.toLowerCase();
        int idx = lower.indexOf(prop);
        while (idx >= 0) {
            boolean boundary = idx == 0 || lower.charAt(idx - 1) == ';' || lower.charAt(idx - 1) == ' ';
            int colon = lower.indexOf(':', idx);
            if (boundary && colon == idx + prop.length()) {
                int end = lower.indexOf(';', colon);
                if (end < 0) {
                    end = styleAttr.length();
                }
                return styleAttr.substring(colon + 1, end).trim();
            }
            idx = lower.indexOf(prop, idx + 1);
        }
        return null;
    }

    private static int parseColor(String v) {
        if (v == null) {
            return -1;
        }
        v = v.trim();
        if (v.startsWith("#")) {
            String hex = v.substring(1);
            if (hex.length() == 3) {
                char r = hex.charAt(0);
                char g = hex.charAt(1);
                char b = hex.charAt(2);
                hex = "" + r + r + g + g + b + b;
            }
            if (hex.length() >= 6) {
                try {
                    return Integer.parseInt(hex.substring(0, 6), 16);
                } catch (NumberFormatException err) {
                    return -1;
                }
            }
            return -1;
        }
        if (v.startsWith("rgb")) {
            int open = v.indexOf('(');
            int close = v.indexOf(')');
            if (open >= 0 && close > open) {
                String[] parts = split(v.substring(open + 1, close), ',');
                if (parts.length >= 3) {
                    try {
                        int r = Integer.parseInt(parts[0].trim());
                        int g = Integer.parseInt(parts[1].trim());
                        int b = Integer.parseInt(parts[2].trim());
                        return ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
                    } catch (NumberFormatException err) {
                        return -1;
                    }
                }
            }
        }
        return -1;
    }

    private static int fontSizeLevel(String v) {
        if (v == null) {
            return 0;
        }
        v = v.trim();
        if (v.endsWith("%")) {
            try {
                int pct = Integer.parseInt(v.substring(0, v.length() - 1).trim());
                if (pct <= 70) {
                    return 1;
                }
                if (pct <= 85) {
                    return 2;
                }
                if (pct <= 100) {
                    return 3;
                }
                if (pct <= 115) {
                    return 4;
                }
                if (pct <= 150) {
                    return 5;
                }
                if (pct <= 200) {
                    return 6;
                }
                return 7;
            } catch (NumberFormatException err) {
                return 0;
            }
        }
        return 0;
    }

    private static String[] split(String s, char c) {
        List<String> out = new ArrayList<String>();
        int start = 0;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == c) {
                out.add(s.substring(start, i));
                start = i + 1;
            }
        }
        out.add(s.substring(start));
        return out.toArray(new String[out.size()]);
    }
}
