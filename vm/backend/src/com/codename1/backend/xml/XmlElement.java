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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// One element of a small in-memory XML tree, built by [XmlPullParser].
///
/// ```java
/// XmlElement root = XmlElement.parse(bytes);
/// for (XmlElement item : root.children("item")) {
///     String id = item.getAttribute("id");
///     String title = item.child("title").getText();
/// }
/// ```
///
/// The tree keeps elements, attributes and text; comments, processing
/// instructions and the document type are dropped, exactly as the parser drops
/// them. It carries every limit of the parser, including its refusal to expand
/// any entity a document type declares, so it is safe for untrusted input.
public final class XmlElement {
    private final String name;
    private final Map<String, String> attributes;
    private final List<XmlElement> children = new ArrayList<XmlElement>();
    private String text = "";

    private XmlElement(String name, Map<String, String> attributes) {
        this.name = name;
        this.attributes = attributes;
    }

    /// Parses a UTF-8 document and answers its root element.
    ///
    /// #### Throws
    ///
    /// - `XmlException`: when the document is not well formed; see [XmlPullParser].
    public static XmlElement parse(byte[] utf8) throws XmlException {
        return parse(XmlPullParser.of(utf8));
    }

    /// Parses a document held in a string and answers its root element.
    ///
    /// #### Throws
    ///
    /// - `XmlException`: when the document is not well formed; see [XmlPullParser].
    public static XmlElement parse(String document) throws XmlException {
        return parse(XmlPullParser.of(document));
    }

    /// Reads `in` to its end as UTF-8 and answers the root element. The stream is
    /// not closed.
    ///
    /// #### Throws
    ///
    /// - `IOException`: when reading fails, or an `XmlException` when the
    ///   document is not well formed.
    public static XmlElement parse(InputStream in) throws IOException {
        return parse(XmlPullParser.of(in));
    }

    /// Builds the tree from a parser positioned before the root element, reading
    /// it to the end of the document. Configure the parser first, for example
    /// with [XmlPullParser#setMaxDepth(int)].
    ///
    /// #### Throws
    ///
    /// - `XmlException`: when the document is not well formed.
    public static XmlElement parse(XmlPullParser parser) throws XmlException {
        List<XmlElement> open = new ArrayList<XmlElement>();
        XmlElement root = null;
        for (int ev = parser.next(); ev != XmlPullParser.END_DOCUMENT; ev = parser.next()) {
            if (ev == XmlPullParser.START_ELEMENT) {
                int count = parser.getAttributeCount();
                Map<String, String> attrs = count == 0
                        ? Collections.<String, String>emptyMap()
                        : new LinkedHashMap<String, String>();
                for (int i = 0; i < count; i++) {
                    attrs.put(parser.getAttributeName(i), parser.getAttributeValue(i));
                }
                XmlElement e = new XmlElement(parser.getName(), attrs);
                if (open.isEmpty()) {
                    root = e;
                } else {
                    open.get(open.size() - 1).children.add(e);
                }
                open.add(e);
            } else if (ev == XmlPullParser.END_ELEMENT) {
                open.remove(open.size() - 1);
            } else if (ev == XmlPullParser.TEXT && !open.isEmpty()) {
                XmlElement e = open.get(open.size() - 1);
                e.text = e.text.concat(parser.getText());
            }
        }
        if (root == null) {
            throw new XmlException("no root element");
        }
        return root;
    }

    /// The element name as written, prefix included.
    public String getName() {
        return name;
    }

    /// The decoded value of the named attribute, or null when there is none.
    public String getAttribute(String attributeName) {
        return attributes.get(attributeName);
    }

    /// Every attribute, in document order. The map is read-only.
    public Map<String, String> getAttributes() {
        return Collections.unmodifiableMap(attributes);
    }

    /// The child elements in document order. The list is read-only.
    public List<XmlElement> getChildren() {
        return Collections.unmodifiableList(children);
    }

    /// The first child element named `childName`, or null when there is none.
    public XmlElement child(String childName) {
        for (XmlElement c : children) {
            if (c.name.equals(childName)) {
                return c;
            }
        }
        return null;
    }

    /// Every child element named `childName`, in document order; empty when there
    /// is none.
    public List<XmlElement> children(String childName) {
        List<XmlElement> out = new ArrayList<XmlElement>();
        for (XmlElement c : children) {
            if (c.name.equals(childName)) {
                out.add(c);
            }
        }
        return out;
    }

    /// The element's own character data -- every text and CDATA run directly
    /// inside it, concatenated, with whitespace as written. Text inside child
    /// elements is not included.
    public String getText() {
        return text;
    }

    /// The text of the first child named `childName`, trimmed, or null when there
    /// is no such child.
    public String childText(String childName) {
        XmlElement c = child(childName);
        return c == null ? null : c.getText().trim();
    }

    @Override
    public String toString() {
        return "<" + name + "> with " + children.size() + " children";
    }
}
