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

import com.codename1.desktopcompat.java.awt.Color;
import com.codename1.desktopcompat.java.awt.Font;
import com.codename1.desktopcompat.javax.swing.event.ChangeEvent;
import com.codename1.desktopcompat.javax.swing.event.ChangeListener;
import com.codename1.desktopcompat.javax.swing.event.EventListenerList;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.EventListener;
import java.util.LinkedHashMap;

/// The named styles of one or more documents.
///
/// A context starts with one style, [#DEFAULT_STYLE], which is what the
/// paragraphs of a `DefaultStyledDocument` resolve their attributes in
/// until they are given another.
public class StyleContext {

    public static final String DEFAULT_STYLE = "default";

    private static StyleContext defaultContext;

    private final LinkedHashMap<String, Style> styles = new LinkedHashMap<String, Style>();

    public StyleContext() {
        addStyle(DEFAULT_STYLE, null);
    }

    /// The context documents share when they are not given one.
    public static final StyleContext getDefaultStyleContext() {
        if (defaultContext == null) {
            defaultContext = new StyleContext();
        }
        return defaultContext;
    }

    /// Makes a style that resolves in `parent`. A style with a name is
    /// kept under it, in place of one that had the name; with `null` for
    /// a name the style is the caller's alone.
    public Style addStyle(String nm, Style parent) {
        Style style = new NamedStyle(nm, parent);
        if (nm != null) {
            styles.put(nm, style);
        }
        return style;
    }

    public void removeStyle(String nm) {
        styles.remove(nm);
    }

    public Style getStyle(String nm) {
        return nm == null ? null : styles.get(nm);
    }

    public Enumeration<?> getStyleNames() {
        return new SimpleAttributeSet.Names(new ArrayList<Object>(styles.keySet()));
    }

    /// The font the attributes in `attr` ask for.
    public Font getFont(AttributeSet attr) {
        int style = Font.PLAIN;
        if (StyleConstants.isBold(attr)) {
            style |= Font.BOLD;
        }
        if (StyleConstants.isItalic(attr)) {
            style |= Font.ITALIC;
        }
        return getFont(StyleConstants.getFontFamily(attr), style, StyleConstants.getFontSize(attr));
    }

    public Font getFont(String family, int style, int size) {
        return new Font(family, style, size);
    }

    public Color getForeground(AttributeSet attr) {
        return StyleConstants.getForeground(attr);
    }

    public Color getBackground(AttributeSet attr) {
        return StyleConstants.getBackground(attr);
    }

    public AttributeSet getEmptySet() {
        return SimpleAttributeSet.EMPTY;
    }

    /// A set of attributes with a name, that tells its listeners when it
    /// changes.
    public class NamedStyle implements Style {

        protected EventListenerList listenerList = new EventListenerList();

        protected transient ChangeEvent changeEvent;

        private final SimpleAttributeSet attributes = new SimpleAttributeSet();

        public NamedStyle(String name, Style parent) {
            if (name != null) {
                setName(name);
            }
            if (parent != null) {
                setResolveParent(parent);
            }
        }

        public NamedStyle(Style parent) {
            this(null, parent);
        }

        public NamedStyle() {
        }

        @Override
        public String toString() {
            return "NamedStyle:" + getName() + " " + attributes;
        }

        @Override
        public String getName() {
            if (isDefined(StyleConstants.NameAttribute)) {
                return String.valueOf(getAttribute(StyleConstants.NameAttribute));
            }
            return null;
        }

        public void setName(String name) {
            if (name != null) {
                addAttribute(StyleConstants.NameAttribute, name);
            }
        }

        @Override
        public void addChangeListener(ChangeListener l) {
            listenerList.add(ChangeListener.class, l);
        }

        @Override
        public void removeChangeListener(ChangeListener l) {
            listenerList.remove(ChangeListener.class, l);
        }

        public ChangeListener[] getChangeListeners() {
            return listenerList.getListeners(ChangeListener.class);
        }

        protected void fireStateChanged() {
            ChangeListener[] ls = getChangeListeners();
            for (int i = ls.length - 1; i >= 0; i--) {
                if (changeEvent == null) {
                    changeEvent = new ChangeEvent(this);
                }
                ls[i].stateChanged(changeEvent);
            }
        }

        public <T extends EventListener> T[] getListeners(Class<T> listenerType) {
            return listenerList.getListeners(listenerType);
        }

        @Override
        public int getAttributeCount() {
            return attributes.getAttributeCount();
        }

        @Override
        public boolean isDefined(Object attrName) {
            return attributes.isDefined(attrName);
        }

        @Override
        public boolean isEqual(AttributeSet attr) {
            return attributes.isEqual(attr);
        }

        @Override
        public AttributeSet copyAttributes() {
            NamedStyle a = new NamedStyle();
            a.attributes.addAttributes(attributes);
            return a;
        }

        @Override
        public Object getAttribute(Object attrName) {
            return attributes.getAttribute(attrName);
        }

        @Override
        public Enumeration<?> getAttributeNames() {
            return attributes.getAttributeNames();
        }

        @Override
        public boolean containsAttribute(Object name, Object value) {
            return attributes.containsAttribute(name, value);
        }

        @Override
        public boolean containsAttributes(AttributeSet attrs) {
            return attributes.containsAttributes(attrs);
        }

        @Override
        public AttributeSet getResolveParent() {
            return attributes.getResolveParent();
        }

        @Override
        public void addAttribute(Object name, Object value) {
            attributes.addAttribute(name, value);
            fireStateChanged();
        }

        @Override
        public void addAttributes(AttributeSet attr) {
            attributes.addAttributes(attr);
            fireStateChanged();
        }

        @Override
        public void removeAttribute(Object name) {
            attributes.removeAttribute(name);
            fireStateChanged();
        }

        @Override
        public void removeAttributes(Enumeration<?> names) {
            attributes.removeAttributes(names);
            fireStateChanged();
        }

        @Override
        public void removeAttributes(AttributeSet attrs) {
            if (attrs == this) {
                attributes.removeAttributes(attributes);
            } else {
                attributes.removeAttributes(attrs);
            }
            fireStateChanged();
        }

        @Override
        public void setResolveParent(AttributeSet parent) {
            if (parent != null) {
                attributes.setResolveParent(parent);
            } else {
                attributes.removeAttribute(StyleConstants.ResolveAttribute);
            }
            fireStateChanged();
        }
    }
}
