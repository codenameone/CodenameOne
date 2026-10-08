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
package com.codename1.desktopcompat.rt;

import com.codename1.desktopcompat.java.awt.Color;
import com.codename1.desktopcompat.java.awt.Dimension;
import com.codename1.desktopcompat.java.awt.Font;
import com.codename1.desktopcompat.java.awt.FontMetrics;
import com.codename1.desktopcompat.java.awt.Graphics;
import java.util.ArrayList;

/// The small part of HTML that Swing applications put in labels, buttons,
/// tab titles and tool tips.
///
/// A text is HTML when it starts with `<html>`, in any case. [#parse]
/// turns it into lines of styled runs, [#preferredSize] measures them and
/// [#paint] draws them, so a component needs three calls to show it.
///
/// Understood: `<b>` `<strong>` `<i>` `<em>` `<u>` `<br>` `<p>` `<div>`
/// `<center>` `<font color= size=>` `<big>` `<small>` `<h1>` to `<h6>`
/// `<li>`, the `align` attribute and a `color` in a `style` attribute, and
/// the entities `&amp;` `&lt;` `&gt;` `&nbsp;` `&quot;` `&apos;` and the
/// numeric ones. Every other tag is dropped and its content kept, except
/// `<head>`, `<style>`, `<script>` and `<title>`, whose content goes too.
/// Malformed input never throws: what cannot be read as a tag is text.
///
/// Not supported: images, tables, lists beyond a dash per item, links,
/// style sheets and font faces.
public final class MiniHtml {

    /// The alignment of a line that asked for none.
    public static final int ALIGN_DEFAULT = -1;
    /// Values of [Line#alignment()] and of the `alignment` of [#paint];
    /// the same numbers as the Swing constants `LEFT`, `CENTER`, `RIGHT`.
    public static final int ALIGN_LEFT = 2;
    public static final int ALIGN_CENTER = 0;
    public static final int ALIGN_RIGHT = 4;
    private static final int ALIGN_TRAILING = 11;

    private static final float[] FONT_SIZES = {0.6f, 0.8f, 1f, 1.15f, 1.5f, 2f, 3f};
    private static final float[] HEADING_SIZES = {2f, 1.5f, 1.17f, 1f, 0.83f, 0.67f};

    private static final String[] COLOR_NAMES = {
        "black", "white", "red", "green", "blue", "yellow", "gray", "grey", "silver", "maroon", "navy", "purple",
        "teal", "olive", "lime", "aqua", "cyan", "fuchsia", "magenta", "orange", "pink", "darkgray", "darkgrey",
        "lightgray", "lightgrey", "brown"
    };
    private static final int[] COLOR_VALUES = {
        0x000000, 0xffffff, 0xff0000, 0x008000, 0x0000ff, 0xffff00, 0x808080, 0x808080, 0xc0c0c0, 0x800000,
        0x000080, 0x800080, 0x008080, 0x808000, 0x00ff00, 0x00ffff, 0x00ffff, 0xff00ff, 0xff00ff, 0xffa500,
        0xffc0cb, 0xa9a9a9, 0xa9a9a9, 0xd3d3d3, 0xd3d3d3, 0xa52a2a
    };

    private static final Color LINK = new Color(0x0000ee);

    private MiniHtml() {
    }

    // ------------------------------------------------------------ model

    /// A stretch of text in one style.
    public static final class Run {
        private final String text;
        private final boolean bold;
        private final boolean italic;
        private final boolean underline;
        private final Color color;
        private final float scale;
        private final String href;

        Run(String text, boolean bold, boolean italic, boolean underline, Color color, float scale) {
            this(text, bold, italic, underline, color, scale, null);
        }

        Run(String text, boolean bold, boolean italic, boolean underline, Color color, float scale, String href) {
            this.text = text;
            this.bold = bold;
            this.italic = italic;
            this.underline = underline;
            this.color = color;
            this.scale = scale;
            this.href = href;
        }

        /// What the `href` of the link this text is in says, or `null`
        /// for text that is not in a link.
        public String href() {
            return href;
        }

        public String text() {
            return text;
        }

        public boolean bold() {
            return bold;
        }

        public boolean italic() {
            return italic;
        }

        public boolean underline() {
            return underline;
        }

        /// The color the text asked for, or `null` for the component's.
        public Color color() {
            return color;
        }

        /// The font size as a multiple of the component's.
        public float scale() {
            return scale;
        }

        Run withText(String t) {
            return new Run(t, bold, italic, underline, color, scale, href);
        }

        boolean sameStyle(Run o) {
            return bold == o.bold && italic == o.italic && underline == o.underline && Float.compare(scale, o.scale) == 0
                    && (color == null ? o.color == null : color.equals(o.color))
                    && (href == null ? o.href == null : href.equals(o.href));
        }

        Font font(Font base) {
            int style = base.getStyle() | (bold ? Font.BOLD : 0) | (italic ? Font.ITALIC : 0);
            if (style == base.getStyle() && Float.compare(scale, 1f) == 0) {
                return base;
            }
            return base.deriveFont(style, base.getSize2D() * scale);
        }
    }

    /// One line: runs side by side.
    public static final class Line {
        private final ArrayList<Run> runs = new ArrayList<Run>();
        private int alignment = ALIGN_DEFAULT;
        private boolean gapBefore;

        Line() {
        }

        public int runCount() {
            return runs.size();
        }

        public Run run(int index) {
            return runs.get(index);
        }

        /// [#ALIGN_LEFT], [#ALIGN_CENTER], [#ALIGN_RIGHT] or
        /// [#ALIGN_DEFAULT].
        public int alignment() {
            return alignment;
        }

        /// Whether the line starts a paragraph and is set off from the
        /// line above it.
        public boolean gapBefore() {
            return gapBefore;
        }

        /// The text of the line without its styles.
        public String text() {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < runs.size(); i++) {
                sb.append(runs.get(i).text);
            }
            return sb.toString();
        }

        void add(Run r) {
            if (!runs.isEmpty()) {
                Run last = runs.get(runs.size() - 1);
                if (last.sameStyle(r)) {
                    runs.set(runs.size() - 1, last.withText(last.text + r.text));
                    return;
                }
            }
            runs.add(r);
        }
    }

    /// A parsed text: lines from top to bottom.
    public static final class Document {
        private final ArrayList<Line> lines = new ArrayList<Line>();

        Document() {
        }

        public int lineCount() {
            return lines.size();
        }

        public Line line(int index) {
            return lines.get(index);
        }
    }

    // ------------------------------------------------------------ parsing

    /// Whether Swing would read `s` as HTML: it starts with `<html>`.
    public static boolean isHtml(String s) {
        return s != null && s.length() >= 6 && s.regionMatches(true, 0, "<html>", 0, 6);
    }

    /// The text of `s` without its tags, lines separated by a line feed;
    /// `s` itself when it is not HTML.
    public static String plainText(String s) {
        if (!isHtml(s)) {
            return s;
        }
        Document d = parse(s);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < d.lineCount(); i++) {
            if (i > 0) {
                sb.append('\n');
            }
            sb.append(d.line(i).text());
        }
        return sb.toString();
    }

    /// As [#plainText] on one line: what a widget that shows a single
    /// line of text gets.
    public static String singleLine(String s) {
        String t = plainText(s);
        return t == null ? null : t.replace('\n', ' ');
    }

    /// The style in force at one point of the text.
    private static final class State {
        String tag;
        boolean bold;
        boolean italic;
        boolean underline;
        Color color;
        float scale = 1f;
        int align = ALIGN_DEFAULT;
        String href;

        State copy(String forTag) {
            State s = new State();
            s.tag = forTag;
            s.bold = bold;
            s.italic = italic;
            s.underline = underline;
            s.color = color;
            s.scale = scale;
            s.align = align;
            s.href = href;
            return s;
        }
    }

    /// Builds the lines as the text is read.
    private static final class Builder {
        final Document doc = new Document();
        final ArrayList<State> stack = new ArrayList<State>();
        final StringBuilder text = new StringBuilder();
        Line line = new Line();
        boolean pendingGap;
        boolean spacePending;

        Builder() {
            stack.add(new State());
        }

        State top() {
            return stack.get(stack.size() - 1);
        }

        void flush() {
            if (spacePending && lineHasText()) {
                // The space belongs to the style it was written in.
                text.append(' ');
                spacePending = false;
            }
            if (text.length() > 0) {
                State s = top();
                if (line.runs.isEmpty()) {
                    line.alignment = s.align;
                }
                line.add(new Run(text.toString(), s.bold, s.italic, s.underline, s.color, s.scale, s.href));
                text.setLength(0);
            }
        }

        boolean lineHasText() {
            return text.length() > 0 || !line.runs.isEmpty();
        }

        void character(char c, boolean collapsible) {
            if (collapsible && (c == ' ' || c == '\t' || c == '\n' || c == '\r' || c == '\f')) {
                if (lineHasText()) {
                    spacePending = true;
                }
                return;
            }
            if (spacePending) {
                text.append(' ');
                spacePending = false;
            }
            text.append(c);
        }

        void endLine() {
            spacePending = false;
            flush();
            if (pendingGap && !doc.lines.isEmpty()) {
                line.gapBefore = true;
            }
            pendingGap = false;
            doc.lines.add(line);
            line = new Line();
        }

        /// The start or end of a block: ends the line if it has anything.
        void block(boolean gap) {
            if (lineHasText()) {
                endLine();
            }
            if (gap) {
                pendingGap = true;
            }
        }

        void lineBreak() {
            endLine();
        }

        void open(State s) {
            flush();
            stack.add(s);
        }

        void close(String tag) {
            for (int i = stack.size() - 1; i > 0; i--) {
                if (tag.equals(stack.get(i).tag)) {
                    flush();
                    while (stack.size() > i) {
                        stack.remove(stack.size() - 1);
                    }
                    return;
                }
            }
        }

        Document finish() {
            if (lineHasText()) {
                endLine();
            }
            return doc;
        }
    }

    /// Reads `html` into lines of runs. A text that is not HTML is read
    /// the same way, so its tags, if it has any, are honoured too.
    public static Document parse(String html) {
        Builder b = new Builder();
        if (html == null) {
            return b.doc;
        }
        int n = html.length();
        int i = 0;
        while (i < n) {
            char c = html.charAt(i);
            if (c == '<') {
                int next = tag(html, i, b);
                if (next > i) {
                    i = next;
                    continue;
                }
                b.character(c, false);
                i++;
            } else if (c == '&') {
                i = entity(html, i, b);
            } else {
                b.character(c, true);
                i++;
            }
        }
        return b.finish();
    }

    private static boolean letter(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }

    private static String asciiLower(String s) {
        StringBuilder sb = null;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= 'A' && c <= 'Z') {
                if (sb == null) {
                    sb = new StringBuilder(s);
                }
                sb.setCharAt(i, (char) (c + ('a' - 'A')));
            }
        }
        return sb == null ? s : sb.toString();
    }

    private static int indexOfIgnoreCase(String s, String what, int from) {
        int last = s.length() - what.length();
        for (int i = Math.max(0, from); i <= last; i++) {
            if (s.regionMatches(true, i, what, 0, what.length())) {
                return i;
            }
        }
        return -1;
    }

    /// Reads the entity at `at` and answers the index after it; an
    /// ampersand that starts none is text.
    private static int entity(String s, int at, Builder b) {
        int end = -1;
        int limit = Math.min(s.length(), at + 12);
        for (int i = at + 1; i < limit; i++) {
            char c = s.charAt(i);
            if (c == ';') {
                end = i;
                break;
            }
            if (!(letter(c) || (c >= '0' && c <= '9') || c == '#')) {
                break;
            }
        }
        if (end > at + 1) {
            String name = s.substring(at + 1, end);
            if ("nbsp".equals(name)) {
                b.character(' ', false);
                return end + 1;
            }
            int ch = -1;
            if ("amp".equals(name)) {
                ch = '&';
            } else if ("lt".equals(name)) {
                ch = '<';
            } else if ("gt".equals(name)) {
                ch = '>';
            } else if ("quot".equals(name)) {
                ch = '"';
            } else if ("apos".equals(name)) {
                ch = '\'';
            } else if (name.charAt(0) == '#' && name.length() > 1) {
                try {
                    if (name.charAt(1) == 'x' || name.charAt(1) == 'X') {
                        ch = name.length() > 2 ? Integer.parseInt(name.substring(2), 16) : -1;
                    } else {
                        ch = Integer.parseInt(name.substring(1));
                    }
                } catch (NumberFormatException e) {
                    ch = -1;
                }
                if (ch == 160) {
                    b.character(' ', false);
                    return end + 1;
                }
                if (ch <= 0 || ch > 0xffff) {
                    ch = -1;
                }
            }
            if (ch >= 0) {
                b.character((char) ch, ch == ' ' || ch == '\n' || ch == '\t' || ch == '\r');
                return end + 1;
            }
        }
        b.character('&', false);
        return at + 1;
    }

    /// Reads the tag at `at` and answers the index after it, or `at`
    /// when what is there is not a tag.
    private static int tag(String s, int at, Builder b) {
        int n = s.length();
        if (s.startsWith("<!--", at)) {
            int end = s.indexOf("-->", at + 4);
            return end < 0 ? n : end + 3;
        }
        int p = at + 1;
        boolean closing = false;
        if (p < n && s.charAt(p) == '/') {
            closing = true;
            p++;
        }
        if (p < n && s.charAt(p) == '!') {
            int end = s.indexOf('>', p);
            return end < 0 ? at : end + 1;
        }
        if (p >= n || !letter(s.charAt(p))) {
            return at;
        }
        int end = s.indexOf('>', p);
        if (end < 0) {
            return at;
        }
        int nameEnd = p;
        while (nameEnd < end && (letter(s.charAt(nameEnd)) || (s.charAt(nameEnd) >= '0' && s.charAt(nameEnd) <= '9'))) {
            nameEnd++;
        }
        String name = asciiLower(s.substring(p, nameEnd));
        String attrs = s.substring(nameEnd, end);
        int after = end + 1;
        if (closing) {
            closeTag(name, b);
            return after;
        }
        if ("head".equals(name) || "style".equals(name) || "script".equals(name) || "title".equals(name)) {
            int close = indexOfIgnoreCase(s, "</" + name, after);
            if (close < 0) {
                return n;
            }
            int gt = s.indexOf('>', close);
            return gt < 0 ? n : gt + 1;
        }
        openTag(name, attrs, b);
        return after;
    }

    private static int headingLevel(String name) {
        if (name.length() == 2 && name.charAt(0) == 'h' && name.charAt(1) >= '1' && name.charAt(1) <= '6') {
            return name.charAt(1) - '0';
        }
        return 0;
    }

    private static void openTag(String name, String attrs, Builder b) {
        if ("br".equals(name)) {
            b.lineBreak();
            return;
        }
        if ("html".equals(name) || "body".equals(name)) {
            return;
        }
        State s = b.top().copy(name);
        int heading = headingLevel(name);
        if ("b".equals(name) || "strong".equals(name)) {
            s.bold = true;
        } else if ("i".equals(name) || "em".equals(name)) {
            s.italic = true;
        } else if ("u".equals(name)) {
            s.underline = true;
        } else if ("a".equals(name)) {
            String href = attribute(attrs, "href");
            if (href != null) {
                // A link looks like one, as it does on the desktop.
                s.href = href;
                s.underline = true;
                s.color = LINK;
            }
        } else if ("big".equals(name)) {
            s.scale *= 1.2f;
        } else if ("small".equals(name)) {
            s.scale *= 0.85f;
        } else if ("font".equals(name)) {
            Color c = color(attribute(attrs, "color"));
            if (c != null) {
                s.color = c;
            }
            s.scale = fontSize(attribute(attrs, "size"), s.scale);
        } else if ("center".equals(name)) {
            b.block(false);
            s.align = ALIGN_CENTER;
        } else if ("p".equals(name)) {
            b.block(true);
        } else if ("div".equals(name) || "tr".equals(name) || "ul".equals(name) || "ol".equals(name)) {
            b.block(false);
        } else if ("li".equals(name)) {
            b.block(false);
        } else if (heading > 0) {
            b.block(true);
            s.bold = true;
            s.scale = HEADING_SIZES[heading - 1];
        }
        String align = attribute(attrs, "align");
        if (align != null) {
            if ("center".equalsIgnoreCase(align)) {
                s.align = ALIGN_CENTER;
            } else if ("right".equalsIgnoreCase(align)) {
                s.align = ALIGN_RIGHT;
            } else if ("left".equalsIgnoreCase(align)) {
                s.align = ALIGN_LEFT;
            }
        }
        String style = attribute(attrs, "style");
        if (style != null) {
            Color c = color(styleProperty(style, "color"));
            if (c != null) {
                s.color = c;
            }
        }
        b.open(s);
        if ("li".equals(name)) {
            b.character('-', false);
            b.character(' ', false);
        }
    }

    private static void closeTag(String name, Builder b) {
        if ("html".equals(name) || "body".equals(name) || "br".equals(name)) {
            return;
        }
        boolean heading = headingLevel(name) > 0;
        boolean para = "p".equals(name) || heading;
        boolean block = para || "center".equals(name) || "div".equals(name) || "tr".equals(name)
                || "ul".equals(name) || "ol".equals(name) || "li".equals(name);
        if (block) {
            b.block(false);
        }
        b.close(name);
        if (para) {
            b.pendingGap = true;
        }
    }

    /// The value of an attribute in the text between a tag's name and its
    /// closing bracket, or `null`.
    static String attribute(String attrs, String name) {
        int n = attrs.length();
        int i = 0;
        while (i < n) {
            while (i < n && !letter(attrs.charAt(i))) {
                i++;
            }
            int start = i;
            while (i < n && (letter(attrs.charAt(i)) || attrs.charAt(i) == '-')) {
                i++;
            }
            if (i == start) {
                break;
            }
            boolean match = i - start == name.length() && attrs.regionMatches(true, start, name, 0, name.length());
            while (i < n && attrs.charAt(i) == ' ') {
                i++;
            }
            if (i >= n || attrs.charAt(i) != '=') {
                if (match) {
                    return "";
                }
                continue;
            }
            i++;
            while (i < n && attrs.charAt(i) == ' ') {
                i++;
            }
            String value;
            if (i < n && (attrs.charAt(i) == '"' || attrs.charAt(i) == '\'')) {
                char q = attrs.charAt(i);
                int close = attrs.indexOf(q, i + 1);
                if (close < 0) {
                    close = n;
                }
                value = attrs.substring(i + 1, close);
                i = Math.min(n, close + 1);
            } else {
                int vs = i;
                while (i < n && attrs.charAt(i) != ' ' && attrs.charAt(i) != '\t' && attrs.charAt(i) != '\n') {
                    i++;
                }
                value = attrs.substring(vs, i);
            }
            if (match) {
                return value.trim();
            }
        }
        return null;
    }

    private static String styleProperty(String style, String name) {
        int from = 0;
        while (from < style.length()) {
            int semi = style.indexOf(';', from);
            if (semi < 0) {
                semi = style.length();
            }
            int colon = style.indexOf(':', from);
            if (colon > from && colon < semi && style.substring(from, colon).trim().equalsIgnoreCase(name)) {
                return style.substring(colon + 1, semi).trim();
            }
            from = semi + 1;
        }
        return null;
    }

    private static float fontSize(String size, float current) {
        if (size == null || size.length() == 0) {
            return current;
        }
        try {
            int index;
            char first = size.charAt(0);
            if (first == '+') {
                index = 3 + Integer.parseInt(size.substring(1).trim());
            } else if (first == '-') {
                index = 3 - Integer.parseInt(size.substring(1).trim());
            } else {
                index = Integer.parseInt(size.trim());
            }
            index = Math.max(1, Math.min(FONT_SIZES.length, index));
            return FONT_SIZES[index - 1];
        } catch (NumberFormatException e) {
            return current;
        }
    }

    /// The color an attribute value names: `#rrggbb`, `#rgb`, a bare hex
    /// number of six digits, `rgb(r, g, b)` or one of the HTML color
    /// names. `null` when it names none.
    public static Color color(String value) {
        if (value == null) {
            return null;
        }
        String v = asciiLower(value.trim());
        if (v.length() == 0) {
            return null;
        }
        for (int i = 0; i < COLOR_NAMES.length; i++) {
            if (COLOR_NAMES[i].equals(v)) {
                return new Color(COLOR_VALUES[i]);
            }
        }
        try {
            if (v.startsWith("rgb(") && v.endsWith(")")) {
                String body = v.substring(4, v.length() - 1);
                int c1 = body.indexOf(',');
                int c2 = body.indexOf(',', c1 + 1);
                if (c1 < 0 || c2 < 0) {
                    return null;
                }
                int r = clamp(Integer.parseInt(body.substring(0, c1).trim()));
                int g = clamp(Integer.parseInt(body.substring(c1 + 1, c2).trim()));
                int bl = clamp(Integer.parseInt(body.substring(c2 + 1).trim()));
                return new Color(r, g, bl);
            }
            String hex = v.charAt(0) == '#' ? v.substring(1) : v;
            if (hex.length() == 3) {
                int rgb = Integer.parseInt(hex, 16);
                int r = (rgb >> 8) & 0xf;
                int g = (rgb >> 4) & 0xf;
                int bl = rgb & 0xf;
                return new Color(r * 17, g * 17, bl * 17);
            }
            if (hex.length() == 6) {
                return new Color(Integer.parseInt(hex, 16));
            }
        } catch (NumberFormatException e) {
            return null;
        }
        return null;
    }

    private static int clamp(int v) {
        return v < 0 ? 0 : v > 255 ? 255 : v;
    }

    // ------------------------------------------------------------ measuring

    private static Font base(Font f) {
        return f != null ? f : Fonts.defaultFont();
    }

    private static int lineWidth(Line l, Font base) {
        int w = 0;
        for (int i = 0; i < l.runs.size(); i++) {
            Run r = l.runs.get(i);
            w += Fonts.metrics(r.font(base)).stringWidth(r.text);
        }
        return w;
    }

    private static int lineHeight(Line l, Font base) {
        int h = 0;
        for (int i = 0; i < l.runs.size(); i++) {
            h = Math.max(h, Fonts.metrics(l.runs.get(i).font(base)).getHeight());
        }
        return h > 0 ? h : Fonts.metrics(base).getHeight();
    }

    private static int lineAscent(Line l, Font base) {
        int a = 0;
        for (int i = 0; i < l.runs.size(); i++) {
            a = Math.max(a, Fonts.metrics(l.runs.get(i).font(base)).getAscent());
        }
        return a > 0 ? a : Fonts.metrics(base).getAscent();
    }

    private static int gap(Font base) {
        return Fonts.metrics(base).getHeight() / 2;
    }

    /// The size the lines take in logical pixels when none is wrapped,
    /// with `font` as the component's font.
    public static Dimension preferredSize(Document doc, Font font) {
        Font base = base(font);
        int w = 0;
        int h = 0;
        for (int i = 0; i < doc.lines.size(); i++) {
            Line l = doc.lines.get(i);
            w = Math.max(w, lineWidth(l, base));
            h += lineHeight(l, base);
            if (l.gapBefore) {
                h += gap(base);
            }
        }
        return new Dimension(w, h);
    }

    /// The document with every line wider than `width` broken at spaces.
    /// Answers `doc` itself when nothing has to be broken.
    public static Document wrap(Document doc, Font font, int width) {
        if (width <= 0) {
            return doc;
        }
        Font base = base(font);
        boolean needed = false;
        for (int i = 0; i < doc.lines.size() && !needed; i++) {
            needed = lineWidth(doc.lines.get(i), base) > width;
        }
        if (!needed) {
            return doc;
        }
        Document out = new Document();
        for (int i = 0; i < doc.lines.size(); i++) {
            Line l = doc.lines.get(i);
            if (lineWidth(l, base) <= width) {
                out.lines.add(l);
                continue;
            }
            Line cur = new Line();
            cur.alignment = l.alignment;
            cur.gapBefore = l.gapBefore;
            int curWidth = 0;
            for (int r = 0; r < l.runs.size(); r++) {
                Run run = l.runs.get(r);
                FontMetrics fm = Fonts.metrics(run.font(base));
                String t = run.text;
                int from = 0;
                while (from < t.length()) {
                    int sp = t.indexOf(' ', from);
                    int to = sp < 0 ? t.length() : sp + 1;
                    String chunk = t.substring(from, to);
                    int visible = fm.stringWidth(sp < 0 ? chunk : t.substring(from, sp));
                    if (curWidth + visible > width && !cur.runs.isEmpty()) {
                        out.lines.add(cur);
                        cur = new Line();
                        cur.alignment = l.alignment;
                        curWidth = 0;
                    }
                    cur.add(run.withText(chunk));
                    curWidth += fm.stringWidth(chunk);
                    from = to;
                }
            }
            out.lines.add(cur);
        }
        return out;
    }

    /// The size the lines take when those wider than `width` are broken,
    /// in logical pixels; a `width` of 0 or less breaks none.
    public static Dimension wrappedSize(Document doc, Font font, int width) {
        return preferredSize(wrap(doc, font, width), font);
    }

    /// The link at a point of a text that [#paint] drew with its top left
    /// corner at the origin: the `href` of the run under `(px, py)`, or
    /// `null` when there is no link there. `font`, `width` and
    /// `alignment` are the ones the text was painted with.
    public static String hrefAt(Document doc, Font font, int width, int alignment, int px, int py) {
        Font base = base(font);
        Document d = wrap(doc, base, width);
        int top = 0;
        for (int i = 0; i < d.lines.size(); i++) {
            Line l = d.lines.get(i);
            if (l.gapBefore) {
                top += gap(base);
            }
            int height = lineHeight(l, base);
            if (py >= top && py < top + height) {
                int lx = lineStart(l, base, width, alignment);
                for (int r = 0; r < l.runs.size(); r++) {
                    Run run = l.runs.get(r);
                    int rw = Fonts.metrics(run.font(base)).stringWidth(run.text);
                    if (px >= lx && px < lx + rw) {
                        return run.href;
                    }
                    lx += rw;
                }
                return null;
            }
            top += height;
        }
        return null;
    }

    /// Where a line starts, relative to the left edge of the text.
    private static int lineStart(Line l, Font base, int width, int alignment) {
        int align = l.alignment != ALIGN_DEFAULT ? l.alignment : alignment;
        if (width > 0 && align != ALIGN_LEFT) {
            int lw = lineWidth(l, base);
            if (align == ALIGN_CENTER) {
                return (width - lw) / 2;
            } else if (align == ALIGN_RIGHT || align == ALIGN_TRAILING) {
                return width - lw;
            }
        }
        return 0;
    }

    // ------------------------------------------------------------ painting

    /// Draws the lines with their top left corner at `(x, y)`, in the
    /// font and color `g` has, which are the component's. A line that
    /// names no alignment is placed in `width` by `alignment`; a line
    /// wider than `width` is wrapped. A `width` of 0 or less neither
    /// wraps nor aligns.
    public static void paint(Graphics g, Document doc, int x, int y, int width, int alignment) {
        Font base = base(g.getFont());
        Color baseColor = g.getColor();
        Document d = wrap(doc, base, width);
        int top = y;
        try {
            for (int i = 0; i < d.lines.size(); i++) {
                Line l = d.lines.get(i);
                if (l.gapBefore) {
                    top += gap(base);
                }
                int height = lineHeight(l, base);
                int baseline = top + lineAscent(l, base);
                int lx = x + lineStart(l, base, width, alignment);
                for (int r = 0; r < l.runs.size(); r++) {
                    Run run = l.runs.get(r);
                    Font f = run.font(base);
                    FontMetrics fm = Fonts.metrics(f);
                    int rw = fm.stringWidth(run.text);
                    g.setFont(f);
                    Color c = run.color != null ? run.color : baseColor;
                    if (c != null) {
                        g.setColor(c);
                    }
                    g.drawString(run.text, lx, baseline);
                    if (run.underline) {
                        g.drawLine(lx, baseline + 1, lx + rw - 1, baseline + 1);
                    }
                    lx += rw;
                }
                top += height;
            }
        } finally {
            g.setFont(base);
            if (baseColor != null) {
                g.setColor(baseColor);
            }
        }
    }
}
