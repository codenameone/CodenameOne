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
package com.codename1.backend.xml;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

/// A small streaming XML parser: call [#next()] until it answers
/// [#END_DOCUMENT], reading the element name, attributes and text of each event
/// as it goes.
///
/// ```java
/// XmlPullParser p = XmlPullParser.of(bytes);
/// for (int ev = p.next(); ev != XmlPullParser.END_DOCUMENT; ev = p.next()) {
///     if (ev == XmlPullParser.START_ELEMENT && "item".equals(p.getName())) {
///         String id = p.getAttributeValue("id");
///     }
/// }
/// ```
///
/// What it does:
///
/// - Elements, attributes (single or double quoted) and text. An empty element
///   (`<a/>`) is reported as a start followed immediately by its end.
/// - Text and CDATA sections are reported together as one [#TEXT] event per run
///   of character data between two tags; whitespace is kept as written. Text
///   outside the root element is ignored.
/// - The five predefined entities (`&amp;` `&lt;` `&gt;` `&quot;` `&apos;`) and
///   numeric character references (`&#65;`, `&#x41;`) are decoded, in text and in
///   attribute values.
/// - Namespaces are not resolved: a name is reported as written, prefix included
///   (`plist:dict`, `xmlns:a`).
/// - Comments, processing instructions and the XML declaration are skipped.
///
/// What it refuses, by construction rather than by configuration:
///
/// - **A document type declaration is skipped, never interpreted.** Its internal
///   subset is not read, so an entity it declares is never defined, an external
///   one is never fetched, and a reference to either is an [XmlException]. That
///   makes the parser immune to external entity (XXE) and entity expansion
///   ("billion laughs") attacks: there is nothing to expand.
/// - Elements nested deeper than [#setMaxDepth(int)] (256 by default) are
///   rejected, so a hostile document cannot make a recursive consumer such as
///   [XmlElement] overflow its stack.
/// - A document that is not well formed -- a mismatched end tag, an unclosed
///   element, a second root element, a stray `<` -- is an [XmlException].
///
/// Input is decoded as UTF-8; a leading byte order mark is skipped. The
/// `encoding` of the XML declaration is not consulted.
public final class XmlPullParser {
    /// An element's start tag; [#getName()] and the attribute accessors describe it.
    public static final int START_ELEMENT = 1;
    /// An element's end tag; [#getName()] names the element being closed.
    public static final int END_ELEMENT = 2;
    /// Character data, with entities decoded; read it with [#getText()].
    public static final int TEXT = 3;
    /// The end of the document. Every later call to [#next()] answers this again.
    public static final int END_DOCUMENT = 4;

    private static final int DEFAULT_MAX_DEPTH = 256;

    private final String in;
    private int pos;
    private int maxDepth = DEFAULT_MAX_DEPTH;
    private final List<String> stack = new ArrayList<String>();
    private boolean rootSeen;
    private boolean pendingEnd;
    private boolean done;

    private int event;
    private String name;
    private String text;
    private final List<String> attrNames = new ArrayList<String>();
    private final List<String> attrValues = new ArrayList<String>();

    private XmlPullParser(String document) {
        this.in = document;
        if (document.length() > 0 && document.charAt(0) == (char) 0xfeff) {
            pos = 1;
        }
    }

    /// A parser over a document that is already a string.
    public static XmlPullParser of(String document) {
        if (document == null) {
            throw new IllegalArgumentException("document is null");
        }
        return new XmlPullParser(document);
    }

    /// A parser over UTF-8 encoded bytes.
    public static XmlPullParser of(byte[] utf8) {
        return of(utf8, 0, utf8.length);
    }

    /// A parser over `length` UTF-8 encoded bytes of `data` starting at `offset`.
    public static XmlPullParser of(byte[] data, int offset, int length) {
        try {
            return new XmlPullParser(new String(data, offset, length, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("UTF-8 is not supported", e);
        }
    }

    /// A parser over the whole of `in`, read as UTF-8. The stream is read to its
    /// end but not closed.
    public static XmlPullParser of(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) {
            out.write(buf, 0, n);
        }
        return of(out.toByteArray());
    }

    /// Sets the deepest element nesting accepted; deeper is an [XmlException].
    /// Answers this parser.
    public XmlPullParser setMaxDepth(int maxDepth) {
        if (maxDepth < 1) {
            throw new IllegalArgumentException("maxDepth must be positive");
        }
        this.maxDepth = maxDepth;
        return this;
    }

    /// The current event: one of [#START_ELEMENT], [#END_ELEMENT], [#TEXT] or
    /// [#END_DOCUMENT], or 0 before the first call to [#next()].
    public int getEventType() {
        return event;
    }

    /// The element name of the current start or end event, prefix included, or
    /// null for any other event.
    public String getName() {
        return name;
    }

    /// The decoded character data of the current [#TEXT] event, or null for any
    /// other event.
    public String getText() {
        return text;
    }

    /// The number of elements currently open: 1 inside the root element, 0 before
    /// and after it. After an [#END_ELEMENT] event the closed element no longer
    /// counts.
    public int getDepth() {
        return stack.size();
    }

    /// The number of attributes on the current start element; 0 for any other event.
    public int getAttributeCount() {
        return attrNames.size();
    }

    /// The name of attribute `index` of the current start element, as written.
    public String getAttributeName(int index) {
        return attrNames.get(index);
    }

    /// The decoded value of attribute `index` of the current start element.
    public String getAttributeValue(int index) {
        return attrValues.get(index);
    }

    /// The decoded value of the attribute named `attributeName` (prefix included)
    /// on the current start element, or null when it has none.
    public String getAttributeValue(String attributeName) {
        int count = attrNames.size();
        for (int i = 0; i < count; i++) {
            if (attrNames.get(i).equals(attributeName)) {
                return attrValues.get(i);
            }
        }
        return null;
    }

    /// Advances to the next event and answers its type.
    ///
    /// #### Throws
    ///
    /// - `XmlException`: when the document is not well formed, nests too deeply,
    ///   or references an entity this parser does not define.
    public int next() throws XmlException {
        name = null;
        text = null;
        attrNames.clear();
        attrValues.clear();
        if (pendingEnd) {
            pendingEnd = false;
            name = stack.remove(stack.size() - 1);
            return event = END_ELEMENT;
        }
        if (done) {
            return event = END_DOCUMENT;
        }
        StringBuilder chars = null;
        int len = in.length();
        boolean atTag = false;
        while (!atTag) {
            if (pos >= len) {
                if (!stack.isEmpty()) {
                    throw error("unexpected end of document inside <" + stack.get(stack.size() - 1) + ">");
                }
                if (!rootSeen) {
                    throw error("no root element");
                }
                if (chars != null && !isWhitespace(chars)) {
                    throw error("text outside the root element");
                }
                done = true;
                return event = END_DOCUMENT;
            }
            char c = in.charAt(pos);
            if (c != '<') {
                if (chars == null) {
                    chars = new StringBuilder();
                }
                if (c == '&') {
                    readReference(chars);
                } else {
                    chars.append(c);
                    pos++;
                }
                continue;
            }
            if (in.startsWith("<![CDATA[", pos)) {
                int end = in.indexOf("]]>", pos + 9);
                if (end < 0) {
                    throw error("unterminated CDATA section");
                }
                if (chars == null) {
                    chars = new StringBuilder();
                }
                chars.append(in, pos + 9, end);
                pos = end + 3;
                continue;
            }
            if (in.startsWith("<!--", pos)) {
                skipPast("-->", "comment");
                continue;
            }
            if (in.startsWith("<?", pos)) {
                skipPast("?>", "processing instruction");
                continue;
            }
            if (in.startsWith("<!", pos)) {
                skipDeclaration();
                continue;
            }
            // A tag. Report any text gathered before it first.
            if (chars != null && !stack.isEmpty()) {
                break;
            }
            if (chars != null && !isWhitespace(chars)) {
                throw error("text outside the root element");
            }
            chars = null;
            atTag = true;
        }
        if (atTag) {
            return pos + 1 < len && in.charAt(pos + 1) == '/' ? readEndTag() : readStartTag();
        }
        text = chars.toString();
        return event = TEXT;
    }

    private int readStartTag() throws XmlException {
        pos++;
        String tag = readName();
        if (stack.isEmpty() && rootSeen) {
            throw error("a second root element <" + tag + ">");
        }
        while (true) {
            skipWhitespace();
            if (pos >= in.length()) {
                throw error("unterminated start tag <" + tag + ">");
            }
            char c = in.charAt(pos);
            if (c == '>') {
                pos++;
                break;
            }
            if (c == '/') {
                if (pos + 1 >= in.length() || in.charAt(pos + 1) != '>') {
                    throw error("expected /> in <" + tag + ">");
                }
                pos += 2;
                pendingEnd = true;
                break;
            }
            String attr = readName();
            skipWhitespace();
            expect('=');
            skipWhitespace();
            if (pos >= in.length()) {
                throw error("unterminated attribute " + attr);
            }
            char quote = in.charAt(pos);
            if (quote != '"' && quote != '\'') {
                throw error("attribute " + attr + " is not quoted");
            }
            pos++;
            StringBuilder value = new StringBuilder();
            while (true) {
                if (pos >= in.length()) {
                    throw error("unterminated value of attribute " + attr);
                }
                char v = in.charAt(pos);
                if (v == quote) {
                    pos++;
                    break;
                }
                if (v == '<') {
                    throw error("< in the value of attribute " + attr);
                }
                if (v == '&') {
                    readReference(value);
                } else {
                    value.append(v);
                    pos++;
                }
            }
            if (attrNames.contains(attr)) {
                throw error("duplicate attribute " + attr);
            }
            attrNames.add(attr);
            attrValues.add(value.toString());
        }
        if (stack.size() >= maxDepth) {
            throw error("elements nested deeper than " + maxDepth);
        }
        stack.add(tag);
        rootSeen = true;
        name = tag;
        return event = START_ELEMENT;
    }

    private int readEndTag() throws XmlException {
        pos += 2;
        String tag = readName();
        skipWhitespace();
        expect('>');
        if (stack.isEmpty()) {
            throw error("end tag </" + tag + "> with no open element");
        }
        String open = stack.remove(stack.size() - 1);
        if (!open.equals(tag)) {
            throw error("end tag </" + tag + "> does not match <" + open + ">");
        }
        name = tag;
        return event = END_ELEMENT;
    }

    private void readReference(StringBuilder out) throws XmlException {
        int semi = in.indexOf(';', pos);
        if (semi < 0 || semi - pos > 12) {
            throw error("unterminated entity reference");
        }
        String ref = in.substring(pos + 1, semi);
        pos = semi + 1;
        if ("amp".equals(ref)) {
            out.append('&');
        } else if ("lt".equals(ref)) {
            out.append('<');
        } else if ("gt".equals(ref)) {
            out.append('>');
        } else if ("quot".equals(ref)) {
            out.append('"');
        } else if ("apos".equals(ref)) {
            out.append('\'');
        } else if (ref.length() > 1 && ref.charAt(0) == '#') {
            int code = parseCharRef(ref);
            if (code < 0 || code > 0x10ffff || code == 0 || (code >= 0xd800 && code <= 0xdfff)) {
                throw error("invalid character reference &" + ref + ";");
            }
            if (code >= 0x10000) {
                int v = code - 0x10000;
                out.append((char) (0xd800 + (v >> 10)));
                out.append((char) (0xdc00 + (v & 0x3ff)));
            } else {
                out.append((char) code);
            }
        } else {
            // Deliberately no DTD-declared entities: see the class comment.
            throw error("undefined entity &" + ref + ";");
        }
    }

    private static int parseCharRef(String ref) {
        boolean hex = ref.length() > 2 && (ref.charAt(1) == 'x' || ref.charAt(1) == 'X');
        int start = hex ? 2 : 1;
        if (start >= ref.length()) {
            return -1;
        }
        int value = 0;
        for (int i = start; i < ref.length(); i++) {
            int digit = Character.digit(ref.charAt(i), hex ? 16 : 10);
            if (digit < 0) {
                return -1;
            }
            value = value * (hex ? 16 : 10) + digit;
            if (value > 0x10ffff) {
                return -1;
            }
        }
        return value;
    }

    /// Skips `<!DOCTYPE ...>` (or any other `<!` declaration) without reading
    /// what it declares, honouring quoted strings and a bracketed internal subset.
    private void skipDeclaration() throws XmlException {
        int len = in.length();
        int brackets = 0;
        char quote = 0;
        pos += 2;
        while (pos < len) {
            char c = in.charAt(pos++);
            if (quote != 0) {
                if (c == quote) {
                    quote = 0;
                }
            } else if (c == '"' || c == '\'') {
                quote = c;
            } else if (c == '[') {
                brackets++;
            } else if (c == ']') {
                brackets--;
            } else if (c == '>' && brackets <= 0) {
                return;
            }
        }
        throw error("unterminated declaration");
    }

    private void skipPast(String terminator, String what) throws XmlException {
        int end = in.indexOf(terminator, pos + 2);
        if (end < 0) {
            throw error("unterminated " + what);
        }
        pos = end + terminator.length();
    }

    private String readName() throws XmlException {
        int start = pos;
        int len = in.length();
        while (pos < len) {
            char c = in.charAt(pos);
            if (c == ' ' || c == '\t' || c == '\r' || c == '\n' || c == '>' || c == '/'
                    || c == '=' || c == '<' || c == '"' || c == '\'') {
                break;
            }
            pos++;
        }
        if (pos == start) {
            throw error("expected a name");
        }
        return in.substring(start, pos);
    }

    private void expect(char c) throws XmlException {
        if (pos >= in.length() || in.charAt(pos) != c) {
            throw error("expected '" + c + "'");
        }
        pos++;
    }

    private void skipWhitespace() {
        int len = in.length();
        while (pos < len && isSpace(in.charAt(pos))) {
            pos++;
        }
    }

    private static boolean isSpace(char c) {
        return c == ' ' || c == '\t' || c == '\r' || c == '\n';
    }

    private static boolean isWhitespace(CharSequence s) {
        for (int i = 0; i < s.length(); i++) {
            if (!isSpace(s.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private XmlException error(String message) {
        return new XmlException(message + " at offset " + pos);
    }
}
