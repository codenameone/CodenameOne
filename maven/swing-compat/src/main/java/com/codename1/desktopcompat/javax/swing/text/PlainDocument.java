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
package com.codename1.desktopcompat.javax.swing.text;

import java.util.Enumeration;
import java.util.Vector;

/// A document of plain text whose structure is its lines.
///
/// With the property `filterNewlines` set to `Boolean.TRUE`, as a single
/// line field sets it, line breaks in inserted text become spaces.
public class PlainDocument extends AbstractDocument {

    public static final String tabSizeAttribute = "tabSize";

    public static final String lineLimitAttribute = "lineLimit";

    private final Root root = new Root();
    private int[] lineStarts;
    private int lineStartsAt = -1;

    public PlainDocument() {
        putProperty(tabSizeAttribute, Integer.valueOf(8));
    }

    /// Applied to every insertion whichever way it arrives -- `insertString`,
    /// `replace` or the filter's bypass -- the way the desktop does it in
    /// its `insertUpdate`.
    @Override
    String cn1FilterInsert(String str) {
        Object filter = getProperty("filterNewlines");
        if (Boolean.TRUE.equals(filter) && str.indexOf('\n') >= 0) {
            StringBuilder filtered = new StringBuilder(str);
            for (int i = 0; i < filtered.length(); i++) {
                if (filtered.charAt(i) == '\n') {
                    filtered.setCharAt(i, ' ');
                }
            }
            return filtered.toString();
        }
        return str;
    }

    /// The offsets the lines start at; always at least one line.
    private int[] lines() {
        if (lineStarts == null || lineStartsAt != cn1ModCount()) {
            CharSequence s = cn1Chars();
            int n = 1;
            for (int i = 0; i < s.length(); i++) {
                if (s.charAt(i) == '\n') {
                    n++;
                }
            }
            int[] starts = new int[n];
            int at = 1;
            for (int i = 0; i < s.length(); i++) {
                if (s.charAt(i) == '\n') {
                    starts[at++] = i + 1;
                }
            }
            lineStarts = starts;
            lineStartsAt = cn1ModCount();
        }
        return lineStarts;
    }

    private int lineOf(int offset) {
        int[] starts = lines();
        int lo = 0;
        int hi = starts.length - 1;
        while (lo < hi) {
            int mid = (lo + hi + 1) >>> 1;
            if (starts[mid] <= offset) {
                lo = mid;
            } else {
                hi = mid - 1;
            }
        }
        return lo;
    }

    @Override
    public Element getDefaultRootElement() {
        return root;
    }

    @Override
    public Element getParagraphElement(int pos) {
        return root.getElement(lineOf(pos));
    }

    /// The attribute set of an element: nothing but its name.
    private static final class Name implements AttributeSet {
        private final String name;

        Name(String name) {
            this.name = name;
        }

        @Override
        public int getAttributeCount() {
            return 1;
        }

        @Override
        public boolean isDefined(Object attrName) {
            return ElementNameAttribute.equals(attrName);
        }

        @Override
        public boolean isEqual(AttributeSet attr) {
            return attr != null && attr.getAttributeCount() == 1
                    && name.equals(attr.getAttribute(ElementNameAttribute));
        }

        @Override
        public AttributeSet copyAttributes() {
            return this;
        }

        @Override
        public Object getAttribute(Object key) {
            return ElementNameAttribute.equals(key) ? name : null;
        }

        @Override
        public Enumeration<?> getAttributeNames() {
            Vector<Object> v = new Vector<Object>();
            v.addElement(ElementNameAttribute);
            return v.elements();
        }

        @Override
        public boolean containsAttribute(Object key, Object value) {
            return ElementNameAttribute.equals(key) && name.equals(value);
        }

        @Override
        public boolean containsAttributes(AttributeSet attributes) {
            return attributes == null || attributes.getAttributeCount() == 0 || isEqual(attributes);
        }

        @Override
        public AttributeSet getResolveParent() {
            return null;
        }
    }

    private final class Root implements Element {
        private final AttributeSet attributes = new Name(ParagraphElementName);

        @Override
        public Document getDocument() {
            return PlainDocument.this;
        }

        @Override
        public Element getParentElement() {
            return null;
        }

        @Override
        public String getName() {
            return ParagraphElementName;
        }

        @Override
        public AttributeSet getAttributes() {
            return attributes;
        }

        @Override
        public int getStartOffset() {
            return 0;
        }

        @Override
        public int getEndOffset() {
            return getLength() + 1;
        }

        @Override
        public int getElementIndex(int offset) {
            return lineOf(offset < 0 ? 0 : offset);
        }

        @Override
        public int getElementCount() {
            return lines().length;
        }

        @Override
        public Element getElement(int index) {
            if (index < 0 || index >= lines().length) {
                return null;
            }
            return new Line(index);
        }

        @Override
        public boolean isLeaf() {
            return false;
        }
    }

    /// One line. Its end is the offset after its line break, and for the
    /// last line one past the end of the text, as on the desktop.
    private final class Line implements Element {
        private final int index;
        private final AttributeSet attributes = new Name(ContentElementName);

        Line(int index) {
            this.index = index;
        }

        @Override
        public Document getDocument() {
            return PlainDocument.this;
        }

        @Override
        public Element getParentElement() {
            return root;
        }

        @Override
        public String getName() {
            return ContentElementName;
        }

        @Override
        public AttributeSet getAttributes() {
            return attributes;
        }

        @Override
        public int getStartOffset() {
            int[] starts = lines();
            return index < starts.length ? starts[index] : getLength();
        }

        @Override
        public int getEndOffset() {
            int[] starts = lines();
            return index + 1 < starts.length ? starts[index + 1] : getLength() + 1;
        }

        @Override
        public int getElementIndex(int offset) {
            return -1;
        }

        @Override
        public int getElementCount() {
            return 0;
        }

        @Override
        public Element getElement(int i) {
            return null;
        }

        @Override
        public boolean isLeaf() {
            return true;
        }
    }
}
