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
package com.codename1.android.rescompiler;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/// An XML element as read from a resource file, before any value in it is
/// encoded: namespaces folded to the short keys the compiler uses, every
/// element carrying the line it started on for diagnostics.
public final class RawNode {

    public static final String NS_ANDROID = "http://schemas.android.com/apk/res/android";
    public static final String NS_RES_AUTO = "http://schemas.android.com/apk/res-auto";
    public static final String NS_RES_PREFIX = "http://schemas.android.com/apk/res/";
    public static final String NS_TOOLS = "http://schemas.android.com/tools";

    /// Namespace keys: none, the framework, the package being compiled.
    public static final int NS_NONE = 0;
    public static final int NS_KEY_ANDROID = 1;
    public static final int NS_KEY_APP = 2;

    public static final class Attr {
        public final int ns;
        public final String name;
        public final String value;

        Attr(int ns, String name, String value) {
            this.ns = ns;
            this.name = name;
            this.value = value;
        }
    }

    public final String tag;
    public final int line;
    public final List<Attr> attrs = new ArrayList<Attr>();
    public final List<RawNode> children = new ArrayList<RawNode>();
    /// Character data directly inside this element, in document order and
    /// interleaved with the children's: `text[i]` precedes `children[i]`.
    final List<StringBuilder> textRuns = new ArrayList<StringBuilder>();

    RawNode(String tag, int line) {
        this.tag = tag;
        this.line = line;
        textRuns.add(new StringBuilder());
    }

    /// The value of the attribute `name` in namespace `ns`, or null.
    public String attr(int ns, String name) {
        for (Attr a : attrs) {
            if (a.ns == ns && a.name.equals(name)) {
                return a.value;
            }
        }
        return null;
    }

    /// The element's own text, excluding children.
    public String ownText() {
        StringBuilder sb = new StringBuilder();
        for (StringBuilder r : textRuns) {
            sb.append(r);
        }
        return sb.toString();
    }

    /// All text inside the element, markup removed: what a `<string>` with
    /// `<b>` or `<xliff:g>` spans means as plain text.
    public String innerText() {
        StringBuilder sb = new StringBuilder();
        appendInner(sb);
        return sb.toString();
    }

    private void appendInner(StringBuilder sb) {
        for (int i = 0; i < textRuns.size(); i++) {
            sb.append(textRuns.get(i));
            if (i < children.size()) {
                children.get(i).appendInner(sb);
            }
        }
    }

    public static RawNode parse(File f) throws IOException {
        InputStream in = new FileInputStream(f);
        try {
            return parse(in, f.getPath());
        } finally {
            in.close();
        }
    }

    /// Refuses DOCTYPE declarations where the parser supports that; answers
    /// whether it does.
    private static boolean disallowDoctype(SAXParserFactory spf) {
        try {
            spf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            return true;
        } catch (ParserConfigurationException | SAXException e) {
            return false;
        }
    }

    public static RawNode parse(InputStream in, String systemId) throws IOException {
        SAXParserFactory spf = SAXParserFactory.newInstance();
        spf.setNamespaceAware(true);
        // A parser without the feature still parses; resource files have no DTDs.
        disallowDoctype(spf);
        Handler h = new Handler();
        try {
            SAXParser p = spf.newSAXParser();
            InputSource src = new InputSource(in);
            src.setSystemId(systemId);
            p.parse(src, h);
        } catch (ParserConfigurationException e) {
            throw new IOException(e);
        } catch (SAXException e) {
            throw new IOException("Malformed XML in " + systemId + ": " + e.getMessage(), e);
        }
        return h.root;
    }

    static int nsKey(String uri) {
        if (uri == null || uri.length() == 0) {
            return NS_NONE;
        }
        if (uri.equals(NS_ANDROID)) {
            return NS_KEY_ANDROID;
        }
        if (uri.equals(NS_RES_AUTO) || uri.startsWith(NS_RES_PREFIX)) {
            return NS_KEY_APP;
        }
        return -1;
    }

    private static final class Handler extends DefaultHandler {
        RawNode root;
        final Deque<RawNode> stack = new ArrayDeque<RawNode>();
        Locator locator;

        @Override
        public void setDocumentLocator(Locator locator) {
            this.locator = locator;
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes atts) {
            // An element in a non-resource namespace (<xliff:g>, <aapt:attr>)
            // keeps its qualified name so callers can recognise it.
            String tag = nsKey(uri) == NS_NONE || nsKey(uri) == NS_KEY_ANDROID ? localName : qName;
            RawNode n = new RawNode(tag, locator == null ? 0 : locator.getLineNumber());
            for (int i = 0; i < atts.getLength(); i++) {
                int key = nsKey(atts.getURI(i));
                if (key < 0) {
                    // tools:, xmlns and anything else that is not a resource
                    // attribute is a build-time annotation; drop it.
                    continue;
                }
                n.attrs.add(new Attr(key, atts.getLocalName(i), atts.getValue(i)));
            }
            if (stack.isEmpty()) {
                root = n;
            } else {
                RawNode parent = stack.peek();
                parent.children.add(n);
                parent.textRuns.add(new StringBuilder());
            }
            stack.push(n);
        }

        @Override
        public void endElement(String uri, String localName, String qName) {
            stack.pop();
        }

        @Override
        public void characters(char[] ch, int start, int length) {
            if (!stack.isEmpty()) {
                RawNode n = stack.peek();
                n.textRuns.get(n.textRuns.size() - 1).append(ch, start, length);
            }
        }
    }
}
