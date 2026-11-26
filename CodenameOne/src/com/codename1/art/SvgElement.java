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
package com.codename1.art;

import com.codename1.impl.xml.XmlScanner;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// A parsed SVG element: its name, attributes, children and text.
final class SvgElement {
    static final int MAX_ELEMENTS = 200000;
    static final int MAX_DEPTH = 256;

    final String name;
    final Map<String, String> attrs = new LinkedHashMap<String, String>();
    final List<SvgElement> children = new ArrayList<SvgElement>();
    final StringBuilder text = new StringBuilder(); //NOPMD AvoidStringBufferField - text content collected while parsing
    SvgElement parent;
    /// Text nodes interleaved with child elements, for text content: a String or an element.
    final List<Object> content = new ArrayList<Object>();

    SvgElement(String name) {
        this.name = name;
    }

    String attr(String n) {
        return attrs.get(n);
    }

    static SvgElement parse(InputStream in) throws IOException {
        XmlScanner x = new XmlScanner(in, new long[]{64L * 1024 * 1024});
        SvgElement root = null;
        SvgElement cur = null;
        int count = 0;
        int depth = 0;
        while (true) {
            int ev = x.next();
            if (ev == XmlScanner.EOF) {
                break;
            }
            if (ev == XmlScanner.START) {
                count++;
                if (count > MAX_ELEMENTS) {
                    throw new IOException("SVG has too many elements");
                }
                depth++;
                if (depth > MAX_DEPTH) {
                    throw new IOException("SVG nests too deeply");
                }
                SvgElement e = new SvgElement(x.name);
                for (int i = 0; i < x.attrCount(); i++) {
                    String n = x.attrName(i);
                    if (n.startsWith("xlink:")) {
                        n = n.substring(6);
                    }
                    e.attrs.put(n, x.attrValue(i));
                }
                if (cur == null) {
                    root = e;
                } else {
                    e.parent = cur;
                    cur.children.add(e);
                    cur.content.add(e);
                }
                cur = e;
            } else if (ev == XmlScanner.END) {
                depth--;
                if (cur != null) {
                    cur = cur.parent;
                }
            } else if (ev == XmlScanner.TEXT && cur != null) {
                cur.text.append(x.text);
                cur.content.add(x.text);
            }
        }
        if (root == null) {
            throw new IOException("Not an SVG document");
        }
        return root;
    }
}
