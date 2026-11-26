/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.bazaar.content;

import java.util.ArrayList;
import java.util.List;

/// The rich text of pages, policies and blog posts: an allow-list sanitizer
/// and the block model the storefront draws.
///
/// [#parse] reads merchant HTML into [Block]s -- paragraphs, two heading
/// levels, quotes and lists -- made of [Run]s of text that may be bold,
/// italic or a link. Everything else is dropped: any other tag (its text
/// kept), every attribute but a link's `href`, and the whole content of
/// `script`, `style`, `iframe`, `object`, `embed`, `svg`, `math`,
/// `template`, `noscript`, `textarea` and `select`. A link keeps only an
/// `http:`, `https:` or `mailto:` address or a path on this site; a
/// `javascript:` or `data:` one (however it is spelled, encoded or padded)
/// loses its link and keeps its words.
///
/// [#sanitize] writes the model back as HTML -- what is stored -- so a
/// stored body is already clean, and the storefront parses it again to draw
/// it with the compiled templates, which print text and never markup. There
/// is no way for a body to put markup on a page except through this model.
public final class RichText {
    /// The longest body accepted, in characters.
    public static final int MAX = 50000;
    private static final int MAX_BLOCKS = 2000;
    private static final String[] DROP_CONTENT = {"script", "style", "iframe", "object", "embed",
        "svg", "math", "template", "noscript", "textarea", "select", "title", "head"};

    private RichText() {
    }

    /// A stretch of text with one look.
    public static final class Run {
        public String text = "";
        public String href = "";
        public boolean link;
        public boolean plain;
        public boolean br;
        public boolean bold;
        public boolean italic;
        /// The classes the storefront styles it with: `rt-b`, `rt-i`.
        public String css = "";
    }

    /// One list item.
    public static final class Item {
        public final List<Run> runs = new ArrayList<Run>();
    }

    /// A paragraph, heading, quote or list.
    public static final class Block {
        public String tag = "p";
        public boolean p;
        public boolean h2;
        public boolean h3;
        public boolean quote;
        public boolean ul;
        public boolean ol;
        public final List<Run> runs = new ArrayList<Run>();
        public final List<Item> items = new ArrayList<Item>();

        Block(String tag) {
            this.tag = tag;
            p = "p".equals(tag);
            h2 = "h2".equals(tag);
            h3 = "h3".equals(tag);
            quote = "blockquote".equals(tag);
            ul = "ul".equals(tag);
            ol = "ol".equals(tag);
        }

        boolean list() {
            return ul || ol;
        }

        boolean empty() {
            return list() ? items.isEmpty() : runs.isEmpty();
        }
    }

    /// The clean HTML of `html`.
    public static String sanitize(String html) {
        return toHtml(parse(html));
    }

    /// The words of `html` alone, whitespace collapsed: an excerpt or a feed summary.
    public static String plainText(String html) {
        List<Block> blocks = parse(html);
        StringBuilder out = new StringBuilder();
        for (int b = 0; b < blocks.size(); b++) {
            Block block = blocks.get(b);
            if (block.list()) {
                for (int i = 0; i < block.items.size(); i++) {
                    words(out, block.items.get(i).runs);
                }
            } else {
                words(out, block.runs);
            }
        }
        return out.toString().trim();
    }

    private static void words(StringBuilder out, List<Run> runs) {
        for (int iter = 0; iter < runs.size(); iter++) {
            if (out.length() > 0 && out.charAt(out.length() - 1) != ' ') {
                out.append(' ');
            }
            out.append(runs.get(iter).text.trim());
        }
    }

    /// The blocks of `html`; see the class comment.
    public static List<Block> parse(String html) {
        Parser p = new Parser();
        String in = html == null ? "" : html;
        if (in.length() > MAX) {
            in = in.substring(0, MAX);
        }
        p.run(in);
        return p.blocks;
    }

    private static final class Parser {
        final List<Block> blocks = new ArrayList<Block>();
        Block block;
        Block list;
        Item item;
        int bold;
        int italic;
        String href;

        void run(String in) {
            int at = 0;
            int n = in.length();
            while (at < n && blocks.size() < MAX_BLOCKS) {
                char c = in.charAt(at);
                if (c != '<') {
                    int lt = in.indexOf('<', at);
                    int end = lt < 0 ? n : lt;
                    text(decode(in.substring(at, end)));
                    at = end;
                    continue;
                }
                if (in.startsWith("<!--", at)) {
                    int close = in.indexOf("-->", at + 4);
                    at = close < 0 ? n : close + 3;
                    continue;
                }
                int gt = tagEnd(in, at + 1);
                if (gt < 0) {
                    // An unterminated tag: the rest is not markup we keep.
                    break;
                }
                String inside = in.substring(at + 1, gt);
                at = gt + 1;
                if (inside.length() == 0 || inside.charAt(0) == '!' || inside.charAt(0) == '?') {
                    continue;
                }
                boolean closing = inside.charAt(0) == '/';
                String name = tagName(inside, closing ? 1 : 0);
                if (name.length() == 0) {
                    continue;
                }
                if (!closing && dropsContent(name)) {
                    at = skipTo(in, at, name);
                    continue;
                }
                if (closing) {
                    close(name);
                } else {
                    open(name, inside);
                }
            }
            finish();
        }

        void open(String name, String inside) {
            if ("p".equals(name) || "div".equals(name)) {
                if (item == null) {
                    start("p");
                }
            } else if ("h1".equals(name) || "h2".equals(name)) {
                start("h2");
            } else if ("h3".equals(name) || "h4".equals(name) || "h5".equals(name)
                    || "h6".equals(name)) {
                start("h3");
            } else if ("blockquote".equals(name)) {
                start("blockquote");
            } else if ("ul".equals(name) || "ol".equals(name)) {
                if (list == null) {
                    endBlock();
                    list = new Block(name);
                }
            } else if ("li".equals(name)) {
                if (list == null) {
                    endBlock();
                    list = new Block("ul");
                }
                item = new Item();
                list.items.add(item);
            } else if ("br".equals(name)) {
                Run r = new Run();
                r.br = true;
                target().add(r);
            } else if ("strong".equals(name) || "b".equals(name)) {
                bold++;
            } else if ("em".equals(name) || "i".equals(name)) {
                italic++;
            } else if ("a".equals(name)) {
                href = safeHref(attribute(inside, "href"));
            }
        }

        void close(String name) {
            if ("p".equals(name) || "div".equals(name) || "h1".equals(name) || "h2".equals(name)
                    || "h3".equals(name) || "h4".equals(name) || "h5".equals(name)
                    || "h6".equals(name) || "blockquote".equals(name)) {
                if (item == null) {
                    endBlock();
                }
            } else if ("li".equals(name)) {
                item = null;
            } else if ("ul".equals(name) || "ol".equals(name)) {
                endList();
            } else if ("strong".equals(name) || "b".equals(name)) {
                bold = Math.max(0, bold - 1);
            } else if ("em".equals(name) || "i".equals(name)) {
                italic = Math.max(0, italic - 1);
            } else if ("a".equals(name)) {
                href = null;
            }
        }

        void start(String tag) {
            if (list != null) {
                // A heading or paragraph inside a list item stays in the item.
                if (item != null) {
                    return;
                }
                endList();
            }
            endBlock();
            block = new Block(tag);
        }

        List<Run> target() {
            if (list != null) {
                if (item == null) {
                    item = new Item();
                    list.items.add(item);
                }
                return item.runs;
            }
            if (block == null) {
                block = new Block("p");
            }
            return block.runs;
        }

        void text(String raw) {
            String t = collapse(raw);
            if (t.length() == 0) {
                return;
            }
            if (t.trim().length() == 0 && (list != null ? item == null : block == null)) {
                return;
            }
            List<Run> runs = target();
            Run r = new Run();
            r.text = t;
            r.bold = bold > 0;
            r.italic = italic > 0;
            r.css = (r.bold ? "rt-b" : "") + (r.bold && r.italic ? " " : "")
                    + (r.italic ? "rt-i" : "");
            if (href != null) {
                r.link = true;
                r.href = href;
            } else {
                r.plain = true;
            }
            runs.add(r);
        }

        void endBlock() {
            if (block != null && !block.empty()) {
                trimEnds(block.runs);
                blocks.add(block);
            }
            block = null;
        }

        void endList() {
            if (list != null) {
                for (int iter = list.items.size() - 1; iter >= 0; iter--) {
                    trimEnds(list.items.get(iter).runs);
                    if (list.items.get(iter).runs.isEmpty()) {
                        list.items.remove(iter);
                    }
                }
                if (!list.empty()) {
                    blocks.add(list);
                }
            }
            list = null;
            item = null;
        }

        void finish() {
            endList();
            endBlock();
        }
    }

    private static void trimEnds(List<Run> runs) {
        while (!runs.isEmpty() && runs.get(0).br) {
            runs.remove(0);
        }
        while (!runs.isEmpty() && runs.get(runs.size() - 1).br) {
            runs.remove(runs.size() - 1);
        }
        if (!runs.isEmpty()) {
            Run first = runs.get(0);
            first.text = trimStart(first.text);
            Run last = runs.get(runs.size() - 1);
            last.text = trimEnd(last.text);
            if (first.text.length() == 0 && !first.br) {
                runs.remove(0);
            }
            if (!runs.isEmpty()) {
                Run tail = runs.get(runs.size() - 1);
                if (tail.text.length() == 0 && !tail.br) {
                    runs.remove(runs.size() - 1);
                }
            }
        }
    }

    private static String trimStart(String s) {
        int i = 0;
        while (i < s.length() && s.charAt(i) == ' ') {
            i++;
        }
        return s.substring(i);
    }

    private static String trimEnd(String s) {
        int i = s.length();
        while (i > 0 && s.charAt(i - 1) == ' ') {
            i--;
        }
        return s.substring(0, i);
    }

    /// Whitespace runs as one space; control characters gone.
    public static String collapse(String s) {
        StringBuilder out = new StringBuilder(s.length());
        boolean space = false;
        for (int iter = 0; iter < s.length(); iter++) {
            char c = s.charAt(iter);
            if (c == ' ' || c == '\t' || c == '\n' || c == '\r' || c == '\f' || c == '\u00a0') {
                if (!space) {
                    out.append(' ');
                }
                space = true;
            } else if (c < 0x20 || c == 0x7f || c == '\ufffe' || c == '\uffff') {
                continue;
            } else {
                out.append(c);
                space = false;
            }
        }
        return out.toString();
    }

    private static boolean dropsContent(String name) {
        for (int iter = 0; iter < DROP_CONTENT.length; iter++) {
            if (DROP_CONTENT[iter].equals(name)) {
                return true;
            }
        }
        return false;
    }

    /// Past the `</name>` that closes a dropped element, or the end.
    private static int skipTo(String in, int from, String name) {
        int at = from;
        while (true) {
            int lt = in.indexOf("</", at);
            if (lt < 0) {
                return in.length();
            }
            String n = tagName(in, lt + 2);
            if (n.equals(name)) {
                int gt = in.indexOf('>', lt);
                return gt < 0 ? in.length() : gt + 1;
            }
            at = lt + 2;
        }
    }

    /// The index of the `>` ending a tag that starts at `from`, past quoted values.
    private static int tagEnd(String in, int from) {
        char quote = 0;
        for (int iter = from; iter < in.length(); iter++) {
            char c = in.charAt(iter);
            if (quote != 0) {
                if (c == quote) {
                    quote = 0;
                }
            } else if (c == '"' || c == '\'') {
                quote = c;
            } else if (c == '>') {
                return iter;
            }
        }
        return -1;
    }

    /// The lower-case ASCII name at `from` (letters and digits), or empty.
    static String tagName(String s, int from) {
        StringBuilder out = new StringBuilder();
        for (int iter = from; iter < s.length() && out.length() < 16; iter++) {
            char c = s.charAt(iter);
            if (c >= 'A' && c <= 'Z') {
                out.append((char) (c + 32));
            } else if (c >= 'a' && c <= 'z' || c >= '0' && c <= '9' && out.length() > 0) {
                out.append(c);
            } else {
                break;
            }
        }
        return out.toString();
    }

    /// The decoded value of attribute `name` in a tag's inside, or null.
    static String attribute(String inside, String name) {
        int at = 0;
        int n = inside.length();
        // Past the tag name.
        while (at < n && !isSpace(inside.charAt(at)) && inside.charAt(at) != '/') {
            at++;
        }
        while (at < n) {
            while (at < n && (isSpace(inside.charAt(at)) || inside.charAt(at) == '/')) {
                at++;
            }
            int start = at;
            while (at < n && !isSpace(inside.charAt(at)) && inside.charAt(at) != '='
                    && inside.charAt(at) != '/') {
                at++;
            }
            String attr = inside.substring(start, at);
            while (at < n && isSpace(inside.charAt(at))) {
                at++;
            }
            String value = "";
            if (at < n && inside.charAt(at) == '=') {
                at++;
                while (at < n && isSpace(inside.charAt(at))) {
                    at++;
                }
                if (at < n && (inside.charAt(at) == '"' || inside.charAt(at) == '\'')) {
                    char q = inside.charAt(at);
                    int close = inside.indexOf(q, at + 1);
                    int end = close < 0 ? n : close;
                    value = inside.substring(at + 1, end);
                    at = close < 0 ? n : close + 1;
                } else {
                    int vs = at;
                    while (at < n && !isSpace(inside.charAt(at))) {
                        at++;
                    }
                    value = inside.substring(vs, at);
                }
            }
            if (attr.length() == 0) {
                at++;
                continue;
            }
            if (attr.equalsIgnoreCase(name)) {
                return decode(value);
            }
        }
        return null;
    }

    private static boolean isSpace(char c) {
        return c == ' ' || c == '\t' || c == '\n' || c == '\r' || c == '\f';
    }

    /// `href` if it is an address a link may keep, else null; see the class comment.
    public static String safeHref(String href) {
        if (href == null) {
            return null;
        }
        // Browsers ignore control characters and spaces inside a scheme
        // ("java\tscript:"), so they are taken out before it is judged.
        StringBuilder squeezed = new StringBuilder(href.length());
        for (int iter = 0; iter < href.length(); iter++) {
            char c = href.charAt(iter);
            if (c > ' ' && c != 0x7f) {
                squeezed.append(c);
            }
        }
        String h = squeezed.toString();
        if (h.length() == 0 || h.length() > 2000) {
            return null;
        }
        if (h.charAt(0) == '/' ) {
            return h.length() > 1 && (h.charAt(1) == '/' || h.charAt(1) == '\\') ? null : h;
        }
        if (h.charAt(0) == '#') {
            return h;
        }
        if (h.regionMatches(true, 0, "https://", 0, 8) && h.length() > 8
                || h.regionMatches(true, 0, "http://", 0, 7) && h.length() > 7
                || h.regionMatches(true, 0, "mailto:", 0, 7) && h.length() > 7) {
            return h;
        }
        return null;
    }

    /// Text with the character references a browser would read, decoded.
    static String decode(String s) {
        if (s.indexOf('&') < 0) {
            return s;
        }
        StringBuilder out = new StringBuilder(s.length());
        int at = 0;
        while (at < s.length()) {
            char c = s.charAt(at);
            if (c != '&') {
                out.append(c);
                at++;
                continue;
            }
            int semi = s.indexOf(';', at);
            if (semi < 0 || semi - at > 10) {
                out.append(c);
                at++;
                continue;
            }
            String ref = s.substring(at + 1, semi);
            int code = -1;
            if (ref.length() > 1 && ref.charAt(0) == '#') {
                code = number(ref.substring(1));
            } else if ("amp".equals(ref)) {
                code = '&';
            } else if ("lt".equals(ref)) {
                code = '<';
            } else if ("gt".equals(ref)) {
                code = '>';
            } else if ("quot".equals(ref)) {
                code = '"';
            } else if ("apos".equals(ref)) {
                code = '\'';
            } else if ("nbsp".equals(ref)) {
                code = 0xa0;
            } else if ("colon".equals(ref)) {
                code = ':';
            } else if ("Tab".equals(ref)) {
                code = '\t';
            } else if ("NewLine".equals(ref)) {
                code = '\n';
            }
            if (code < 0 || code > 0xffff) {
                out.append(c);
                at++;
                continue;
            }
            out.append((char) code);
            at = semi + 1;
        }
        return out.toString();
    }

    private static int number(String digits) {
        boolean hex = digits.length() > 1 && (digits.charAt(0) == 'x' || digits.charAt(0) == 'X');
        int out = 0;
        for (int iter = hex ? 1 : 0; iter < digits.length(); iter++) {
            char c = digits.charAt(iter);
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
            out = out * (hex ? 16 : 10) + d;
            if (out > 0xffff) {
                return -1;
            }
        }
        return out;
    }

    /// The HTML of `blocks`.
    public static String toHtml(List<Block> blocks) {
        StringBuilder out = new StringBuilder();
        for (int b = 0; b < blocks.size(); b++) {
            Block block = blocks.get(b);
            out.append('<').append(block.tag).append('>');
            if (block.list()) {
                for (int i = 0; i < block.items.size(); i++) {
                    out.append("<li>");
                    runs(out, block.items.get(i).runs);
                    out.append("</li>");
                }
            } else {
                runs(out, block.runs);
            }
            out.append("</").append(block.tag).append(">\n");
        }
        return out.toString();
    }

    private static void runs(StringBuilder out, List<Run> runs) {
        for (int iter = 0; iter < runs.size(); iter++) {
            Run r = runs.get(iter);
            if (r.br) {
                out.append("<br>");
                continue;
            }
            if (r.link) {
                out.append("<a href=\"");
                escape(out, r.href);
                out.append("\" rel=\"nofollow noopener\">");
            }
            if (r.bold) {
                out.append("<strong>");
            }
            if (r.italic) {
                out.append("<em>");
            }
            escape(out, r.text);
            if (r.italic) {
                out.append("</em>");
            }
            if (r.bold) {
                out.append("</strong>");
            }
            if (r.link) {
                out.append("</a>");
            }
        }
    }

    /// Escapes text for HTML (or XML) element content and quoted attributes.
    public static void escape(StringBuilder out, String text) {
        for (int iter = 0; iter < text.length(); iter++) {
            char c = text.charAt(iter);
            if (c == '&') {
                out.append("&amp;");
            } else if (c == '<') {
                out.append("&lt;");
            } else if (c == '>') {
                out.append("&gt;");
            } else if (c == '"') {
                out.append("&quot;");
            } else if (c == '\'') {
                out.append("&#39;");
            } else if (c < 0x20 && c != '\t' && c != '\n' && c != '\r' || c == '\ufffe'
                    || c == '\uffff') {
                continue;
            } else {
                out.append(c);
            }
        }
    }
}
