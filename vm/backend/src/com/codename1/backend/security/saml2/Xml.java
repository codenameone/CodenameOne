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
package com.codename1.backend.security.saml2;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/// The XML SAML is written in, read strictly enough to verify a signature over.
///
/// This is not the framework's `com.codename1.xml.XMLParser`, and must not
/// become it. That parser forgives HTML: it folds case, trims text and drops
/// the whitespace between tags. A signature is over the exact canonical bytes,
/// and every difference between the document the signature check reads and
/// the one the application reads is a way to sign one thing and log in as
/// another. So this reader keeps what canonicalization needs and refuses
/// what SAML never contains:
///
/// - No document type declaration at all, so no entity of any kind beyond the
///   five XML predefines and character references: no XXE, no expansion bomb.
/// - Processing instructions only before the root; none inside it.
/// - Comments are dropped and the text on either side of one is joined. A
///   NameID of `admin@corp.example<!---->.evil.example` is therefore the one
///   string it signs, never its first half.
/// - Every prefix must be declared; an attribute may appear once.
/// - Every `ID` attribute in the document is unique, so no element can stand
///   in for another that has its ID.
/// - Depth and size are bounded.
final class Xml {
    static final String XMLNS = "http://www.w3.org/2000/xmlns/";
    static final String XML = "http://www.w3.org/XML/1998/namespace";

    /// The deepest an element may sit. A SAML response is under ten deep.
    static final int MAX_DEPTH = 48;

    /// The largest document read, in characters.
    static final int MAX_LENGTH = 1024 * 1024;

    private Xml() {
    }

    /// A node: an [Element] or a [Text].
    abstract static class Node { // NOPMD AbstractClassWithoutAnyMethod - the common type of Element and Text, never made itself
        Element parent;
    }

    /// Character data, with references resolved and line ends normalized.
    static final class Text extends Node {
        final StringBuilder value = new StringBuilder(); // NOPMD AvoidStringBufferField - built once while parsing, bounded by MAX_LENGTH
    }

    /// One attribute, never a namespace declaration.
    static final class Attribute {
        final String prefix;
        final String localName;
        final String namespace;
        final String value;

        Attribute(String prefix, String localName, String namespace, String value) {
            this.prefix = prefix;
            this.localName = localName;
            this.namespace = namespace;
            this.value = value;
        }

        String qualifiedName() {
            return prefix.length() == 0 ? localName : prefix + ":" + localName;
        }
    }

    /// An element, its attributes, the namespaces it declares and its children.
    static final class Element extends Node {
        final String prefix;
        final String localName;
        String namespace;
        final List<Attribute> attributes = new ArrayList<Attribute>();
        /// Prefix to namespace, as declared on this element; "" is the default.
        final Map<String, String> declared = new LinkedHashMap<String, String>();
        final List<Node> children = new ArrayList<Node>();

        Element(String prefix, String localName) {
            this.prefix = prefix;
            this.localName = localName;
        }

        boolean is(String ns, String name) {
            return localName.equals(name) && ns.equals(namespace);
        }

        /// The value of the unprefixed attribute `name`, or null.
        String attribute(String name) {
            for (Attribute a : attributes) {
                if (a.namespace.length() == 0 && a.localName.equals(name)) {
                    return a.value;
                }
            }
            return null;
        }

        /// The namespace `p` names here, or null when it is not declared.
        String lookup(String p) {
            if ("xml".equals(p)) {
                return XML;
            }
            for (Element e = this; e != null; e = e.parent) {
                String ns = e.declared.get(p);
                if (ns != null) {
                    return ns;
                }
            }
            return p.length() == 0 ? "" : null;
        }

        /// The child elements named so, in order.
        List<Element> children(String ns, String name) {
            List<Element> out = new ArrayList<Element>();
            for (Node n : children) {
                if (n instanceof Element && ((Element) n).is(ns, name)) {
                    out.add((Element) n);
                }
            }
            return out;
        }

        /// The one child element named so; null when there is none.
        ///
        /// @throws Saml2AuthenticationException when there are several
        Element child(String ns, String name) {
            List<Element> found = children(ns, name);
            if (found.size() > 1) {
                throw Saml2AuthenticationException.malformed("More than one " + name
                        + " in " + localName);
            }
            return found.isEmpty() ? null : found.get(0);
        }

        List<Element> elements() {
            List<Element> out = new ArrayList<Element>();
            for (Node n : children) {
                if (n instanceof Element) {
                    out.add((Element) n);
                }
            }
            return out;
        }

        /// All text directly inside this element, joined.
        String text() {
            StringBuilder out = new StringBuilder();
            for (Node n : children) {
                if (n instanceof Text) {
                    out.append(((Text) n).value);
                }
            }
            return out.toString();
        }

        /// The text, which must be all this element holds.
        ///
        /// @throws Saml2AuthenticationException when it has child elements
        String textOnly() {
            for (Node n : children) {
                if (n instanceof Element) {
                    throw Saml2AuthenticationException.malformed(localName
                            + " holds elements where text was expected");
                }
            }
            return text();
        }
    }

    /// Reads `data`, UTF-8 encoded.
    ///
    /// @throws Saml2AuthenticationException when it is not XML this reader takes
    static Element parse(byte[] data) {
        String s;
        try {
            s = new String(data, "UTF-8");
        } catch (UnsupportedEncodingException err) {
            throw Saml2AuthenticationException.malformed("No UTF-8 decoder", err);
        }
        return parse(s);
    }

    static Element parse(String document) {
        if (document.length() > MAX_LENGTH) {
            throw Saml2AuthenticationException.malformed("The document is too large");
        }
        return new Parser(document).document();
    }

    private static final class Parser {
        private final String s;
        private int pos;
        private final Set<String> ids = new HashSet<String>();

        Parser(String document) {
            String d = document;
            if (d.length() > 0 && d.charAt(0) == '\uFEFF') {
                d = d.substring(1);
            }
            this.s = normalizeLineEnds(d);
        }

        private static String normalizeLineEnds(String in) {
            if (in.indexOf('\r') < 0) {
                return in;
            }
            StringBuilder out = new StringBuilder(in.length());
            for (int i = 0; i < in.length(); i++) {
                char c = in.charAt(i);
                if (c == '\r') {
                    out.append('\n');
                    if (i + 1 < in.length() && in.charAt(i + 1) == '\n') {
                        i++;
                    }
                } else {
                    out.append(c);
                }
            }
            return out.toString();
        }

        private Saml2AuthenticationException bad(String why) {
            return Saml2AuthenticationException.malformed("Malformed XML at " + pos + ": " + why);
        }

        Element document() {
            prolog();
            if (pos >= s.length() || s.charAt(pos) != '<') {
                throw bad("no root element");
            }
            Element root = element(null, 0);
            // Only whitespace and comments may follow the root.
            while (pos < s.length()) {
                if (isSpace(s.charAt(pos))) {
                    pos++;
                } else if (s.startsWith("<!--", pos)) {
                    comment();
                } else {
                    throw bad("content after the root element");
                }
            }
            return root;
        }

        private void prolog() {
            while (pos < s.length()) {
                char c = s.charAt(pos);
                if (isSpace(c)) {
                    pos++;
                } else if (s.startsWith("<?", pos)) {
                    int end = s.indexOf("?>", pos + 2);
                    if (end < 0) {
                        throw bad("unterminated processing instruction");
                    }
                    pos = end + 2;
                } else if (s.startsWith("<!--", pos)) {
                    comment();
                } else if (s.startsWith("<!", pos)) {
                    throw bad("a document type declaration is not accepted");
                } else {
                    return;
                }
            }
        }

        private void comment() {
            int end = s.indexOf("-->", pos + 4);
            if (end < 0) {
                throw bad("unterminated comment");
            }
            pos = end + 3;
        }

        private Element element(Element parent, int depth) {
            if (depth > MAX_DEPTH) {
                throw bad("nested too deeply");
            }
            pos++;
            String qname = name();
            List<String[]> raw = new ArrayList<String[]>();
            boolean empty;
            while (true) {
                skipSpace();
                if (pos >= s.length()) {
                    throw bad("unterminated start tag");
                }
                char c = s.charAt(pos);
                if (c == '>') {
                    pos++;
                    empty = false;
                    break;
                }
                if (c == '/') {
                    if (pos + 1 >= s.length() || s.charAt(pos + 1) != '>') {
                        throw bad("stray / in a start tag");
                    }
                    pos += 2;
                    empty = true;
                    break;
                }
                int before = pos;
                String an = name();
                if (before == pos) {
                    throw bad("expected an attribute");
                }
                skipSpace();
                expect('=');
                skipSpace();
                raw.add(new String[] {an, attributeValue()});
            }
            int colon = qname.indexOf(':');
            Element e = new Element(colon < 0 ? "" : qname.substring(0, colon),
                    colon < 0 ? qname : qname.substring(colon + 1));
            e.parent = parent;
            Set<String> seen = new HashSet<String>();
            for (String[] pair : raw) {
                String an = pair[0];
                if (!seen.add(an)) {
                    throw bad("attribute " + an + " appears twice");
                }
                if ("xmlns".equals(an)) {
                    e.declared.put("", pair[1]);
                } else if (an.startsWith("xmlns:")) {
                    String p = an.substring(6);
                    if (p.length() == 0 || pair[1].length() == 0) {
                        throw bad("an empty namespace declaration");
                    }
                    e.declared.put(p, pair[1]);
                }
            }
            e.namespace = e.lookup(e.prefix);
            if (e.namespace == null) {
                throw bad("undeclared prefix " + e.prefix);
            }
            Set<String> expanded = new HashSet<String>();
            for (String[] pair : raw) {
                String an = pair[0];
                if ("xmlns".equals(an) || an.startsWith("xmlns:")) {
                    continue;
                }
                int ac = an.indexOf(':');
                String ap = ac < 0 ? "" : an.substring(0, ac);
                String local = ac < 0 ? an : an.substring(ac + 1);
                String ns = ap.length() == 0 ? "" : e.lookup(ap);
                if (ns == null) {
                    throw bad("undeclared prefix " + ap);
                }
                if (!expanded.add(ns + " " + local)) {
                    throw bad("attribute " + local + " appears twice");
                }
                Attribute a = new Attribute(ap, local, ns, pair[1]);
                e.attributes.add(a);
                if (ns.length() == 0 && "ID".equals(local) && !ids.add(a.value)) {
                    throw bad("the ID " + a.value + " is used twice");
                }
            }
            if (empty) {
                return e;
            }
            Text text = null;
            while (true) {
                if (pos >= s.length()) {
                    throw bad("unterminated element " + qname);
                }
                char c = s.charAt(pos);
                if (c == '<') {
                    if (s.startsWith("</", pos)) {
                        pos += 2;
                        String closing = name();
                        if (!closing.equals(qname)) {
                            throw bad("</" + closing + "> closes <" + qname + ">");
                        }
                        skipSpace();
                        expect('>');
                        return e;
                    }
                    if (s.startsWith("<!--", pos)) {
                        // Dropped, and the text around it stays one node.
                        comment();
                        continue;
                    }
                    if (s.startsWith("<![CDATA[", pos)) {
                        int end = s.indexOf("]]>", pos + 9);
                        if (end < 0) {
                            throw bad("unterminated CDATA");
                        }
                        text = text(e, text);
                        text.value.append(s, pos + 9, end);
                        pos = end + 3;
                        continue;
                    }
                    if (s.startsWith("<?", pos) || s.startsWith("<!", pos)) {
                        throw bad("markup SAML never contains");
                    }
                    e.children.add(element(e, depth + 1));
                    text = null;
                    continue;
                }
                text = text(e, text);
                if (c == '&') {
                    text.value.append(reference());
                } else if (c == '>' && s.startsWith("]]>", pos - 2)
                        && text.value.length() >= 2) {
                    throw bad("]]> in text");
                } else {
                    text.value.append(c);
                    pos++;
                }
            }
        }

        private static Text text(Element e, Text current) {
            if (current != null) {
                return current;
            }
            Text t = new Text();
            t.parent = e;
            e.children.add(t);
            return t;
        }

        private String attributeValue() {
            if (pos >= s.length()) {
                throw bad("expected a quoted value");
            }
            char quote = s.charAt(pos);
            if (quote != '"' && quote != '\'') {
                throw bad("expected a quoted value");
            }
            pos++;
            StringBuilder out = new StringBuilder();
            while (true) {
                if (pos >= s.length()) {
                    throw bad("unterminated attribute value");
                }
                char c = s.charAt(pos);
                if (c == quote) {
                    pos++;
                    return out.toString();
                }
                if (c == '<') {
                    throw bad("< in an attribute value");
                }
                if (c == '&') {
                    out.append(reference());
                } else {
                    // Attribute-value normalization: a literal tab or line end
                    // is a space; one written as a reference is kept.
                    out.append(c == '\t' || c == '\n' ? ' ' : c);
                    pos++;
                }
            }
        }

        private String reference() {
            int end = s.indexOf(';', pos);
            if (end < 0 || end - pos > 12) {
                throw bad("unterminated reference");
            }
            String name = s.substring(pos + 1, end);
            pos = end + 1;
            if ("lt".equals(name)) {
                return "<";
            }
            if ("gt".equals(name)) {
                return ">";
            }
            if ("amp".equals(name)) {
                return "&";
            }
            if ("quot".equals(name)) {
                return "\"";
            }
            if ("apos".equals(name)) {
                return "'";
            }
            if (name.length() > 1 && name.charAt(0) == '#') {
                int code;
                try {
                    code = name.charAt(1) == 'x'
                            ? Integer.parseInt(name.substring(2), 16)
                            : Integer.parseInt(name.substring(1), 10);
                } catch (NumberFormatException err) {
                    Saml2AuthenticationException refused = bad("bad character reference");
                    refused.initCause(err);
                    throw refused;
                }
                if (code == 0 || code > 0x10FFFF || (code >= 0xD800 && code <= 0xDFFF)
                        || (code < 0x20 && code != 9 && code != 10 && code != 13)) {
                    throw bad("a character XML does not allow");
                }
                if (code < 0x10000) {
                    return String.valueOf((char) code);
                }
                int v = code - 0x10000;
                return new String(new char[] {(char) (0xD800 + (v >> 10)),
                    (char) (0xDC00 + (v & 0x3FF))});
            }
            throw bad("the entity " + name + " is not defined");
        }

        private String name() {
            int start = pos;
            while (pos < s.length()) {
                char c = s.charAt(pos);
                if (isSpace(c) || c == '>' || c == '/' || c == '=' || c == '<' || c == '"'
                        || c == '\'' || c == '&') {
                    break;
                }
                pos++;
            }
            if (start == pos) {
                throw bad("expected a name");
            }
            String n = s.substring(start, pos);
            int colon = n.indexOf(':');
            if (colon == 0 || colon == n.length() - 1 || n.indexOf(':', colon + 1) >= 0) {
                throw bad("the name " + n + " is not namespace well-formed");
            }
            return n;
        }

        private void expect(char c) {
            if (pos >= s.length() || s.charAt(pos) != c) {
                throw bad("expected " + c);
            }
            pos++;
        }

        private void skipSpace() {
            while (pos < s.length() && isSpace(s.charAt(pos))) {
                pos++;
            }
        }

        private static boolean isSpace(char c) {
            return c == ' ' || c == '\t' || c == '\n';
        }
    }

    /// Escapes text for a document this side writes.
    static String escape(String value) {
        StringBuilder out = new StringBuilder(value.length() + 8);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '&':
                    out.append("&amp;");
                    break;
                case '<':
                    out.append("&lt;");
                    break;
                case '>':
                    out.append("&gt;");
                    break;
                case '"':
                    out.append("&quot;");
                    break;
                default:
                    out.append(c);
            }
        }
        return out.toString();
    }
}
