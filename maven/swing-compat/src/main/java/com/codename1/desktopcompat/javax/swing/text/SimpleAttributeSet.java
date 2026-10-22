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

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashMap;

/// A set of attributes kept in a table.
///
/// An attribute this set lacks is looked up in its resolve parent, when
/// it has one; the count, the names and [#isDefined] are of this set
/// alone.
public class SimpleAttributeSet implements MutableAttributeSet {

    /// A set with nothing in it, which cannot be changed.
    public static final AttributeSet EMPTY = new EmptyAttributeSet();

    private final LinkedHashMap<Object, Object> table = new LinkedHashMap<Object, Object>(3);

    public SimpleAttributeSet() {
    }

    /// A set with the attributes of `source`.
    public SimpleAttributeSet(AttributeSet source) {
        addAttributes(source);
    }

    public boolean isEmpty() {
        return table.isEmpty();
    }

    @Override
    public int getAttributeCount() {
        return table.size();
    }

    @Override
    public boolean isDefined(Object attrName) {
        return table.containsKey(attrName);
    }

    @Override
    public boolean isEqual(AttributeSet attr) {
        return getAttributeCount() == attr.getAttributeCount() && containsAttributes(attr);
    }

    @Override
    public AttributeSet copyAttributes() {
        return new SimpleAttributeSet(this);
    }

    @Override
    public Enumeration<?> getAttributeNames() {
        return new Names(new ArrayList<Object>(table.keySet()));
    }

    @Override
    public Object getAttribute(Object name) {
        Object value = table.get(name);
        if (value == null) {
            AttributeSet parent = getResolveParent();
            if (parent != null) {
                value = parent.getAttribute(name);
            }
        }
        return value;
    }

    @Override
    public boolean containsAttribute(Object name, Object value) {
        return value.equals(getAttribute(name));
    }

    @Override
    public boolean containsAttributes(AttributeSet attributes) {
        boolean result = true;
        Enumeration<?> names = attributes.getAttributeNames();
        while (result && names.hasMoreElements()) {
            Object name = names.nextElement();
            result = attributes.getAttribute(name).equals(getAttribute(name));
        }
        return result;
    }

    @Override
    public void addAttribute(Object name, Object value) {
        table.put(name, value);
    }

    @Override
    public void addAttributes(AttributeSet attributes) {
        Enumeration<?> names = attributes.getAttributeNames();
        while (names.hasMoreElements()) {
            Object name = names.nextElement();
            addAttribute(name, attributes.getAttribute(name));
        }
    }

    @Override
    public void removeAttribute(Object name) {
        table.remove(name);
    }

    @Override
    public void removeAttributes(Enumeration<?> names) {
        while (names.hasMoreElements()) {
            removeAttribute(names.nextElement());
        }
    }

    @Override
    public void removeAttributes(AttributeSet attributes) {
        if (attributes == this) {
            table.clear();
            return;
        }
        Enumeration<?> names = attributes.getAttributeNames();
        while (names.hasMoreElements()) {
            Object name = names.nextElement();
            Object value = attributes.getAttribute(name);
            if (value.equals(getAttribute(name))) {
                removeAttribute(name);
            }
        }
    }

    @Override
    public AttributeSet getResolveParent() {
        Object parent = table.get(StyleConstants.ResolveAttribute);
        return parent instanceof AttributeSet ? (AttributeSet) parent : null;
    }

    @Override
    public void setResolveParent(AttributeSet parent) {
        addAttribute(StyleConstants.ResolveAttribute, parent);
    }

    @Override
    public int hashCode() {
        return table.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        return obj instanceof AttributeSet && isEqual((AttributeSet) obj);
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();
        for (java.util.Map.Entry<Object, Object> e : table.entrySet()) {
            Object key = e.getKey();
            Object value = e.getValue();
            if (value instanceof AttributeSet) {
                // A resolve parent is named, not written out in full.
                s.append(key).append("=**AttributeSet** ");
            } else {
                s.append(key).append('=').append(value).append(' ');
            }
        }
        return s.toString();
    }

    /// The names of a set, as they were when it was asked.
    static final class Names implements Enumeration<Object> {

        private final ArrayList<Object> names;
        private int next;

        Names(ArrayList<Object> names) {
            this.names = names;
        }

        @Override
        public boolean hasMoreElements() {
            return next < names.size();
        }

        @Override
        public Object nextElement() {
            if (next >= names.size()) {
                throw new java.util.NoSuchElementException();
            }
            return names.get(next++);
        }
    }

    private static final class EmptyAttributeSet implements AttributeSet {

        @Override
        public int getAttributeCount() {
            return 0;
        }

        @Override
        public boolean isDefined(Object attrName) {
            return false;
        }

        @Override
        public boolean isEqual(AttributeSet attr) {
            return attr.getAttributeCount() == 0;
        }

        @Override
        public AttributeSet copyAttributes() {
            return this;
        }

        @Override
        public Object getAttribute(Object key) {
            return null;
        }

        @Override
        public Enumeration<?> getAttributeNames() {
            return new Names(new ArrayList<Object>());
        }

        @Override
        public boolean containsAttribute(Object name, Object value) {
            return false;
        }

        @Override
        public boolean containsAttributes(AttributeSet attributes) {
            return attributes.getAttributeCount() == 0;
        }

        @Override
        public AttributeSet getResolveParent() {
            return null;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            return obj instanceof AttributeSet && ((AttributeSet) obj).getAttributeCount() == 0;
        }

        @Override
        public int hashCode() {
            return 0;
        }
    }
}
