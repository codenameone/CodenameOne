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
package com.codename1.androidcompat.runtime;

/// A compiled resource XML element: a layout node, a drawable, a menu item.
/// Attribute values were typed by the build, so nothing is parsed from text at
/// runtime.
public final class XmlNode {

    public static final int NS_NONE = 0;
    public static final int NS_ANDROID = 1;
    public static final int NS_APP = 2;

    public static final String ANDROID_NS_URI = "http://schemas.android.com/apk/res/android";
    public static final String APP_NS_URI = "http://schemas.android.com/apk/res-auto";

    public final String tag;
    public final int line;
    public final int[] attrIds;
    public final byte[] namespaces;
    public final String[] names;
    public final ResValue[] values;
    public final String text;
    public final XmlNode[] children;
    /// The resource file this node belongs to, for diagnostics.
    String source;

    public XmlNode(String tag, int line, int[] attrIds, byte[] namespaces, String[] names, ResValue[] values,
                   String text, XmlNode[] children) {
        this.tag = tag;
        this.line = line;
        this.attrIds = attrIds;
        this.namespaces = namespaces;
        this.names = names;
        this.values = values;
        this.text = text;
        this.children = children;
    }

    public String getSource() {
        return source;
    }

    /// Index of the attribute `name` in namespace key `ns`, or -1.
    public int indexOf(int ns, String name) {
        for (int i = 0; i < names.length; i++) {
            if (namespaces[i] == ns && names[i].equals(name)) {
                return i;
            }
        }
        return -1;
    }

    /// Index of the attribute with resource id `attrId`, or -1.
    public int indexOfAttr(int attrId) {
        if (attrId == 0) {
            return -1;
        }
        for (int i = 0; i < attrIds.length; i++) {
            if (attrIds[i] == attrId) {
                return i;
            }
        }
        return -1;
    }

    public ResValue value(int ns, String name) {
        int i = indexOf(ns, name);
        return i < 0 ? null : values[i];
    }

    public ResValue valueForAttr(int attrId) {
        int i = indexOfAttr(attrId);
        return i < 0 ? null : values[i];
    }

    void setSource(String s) {
        source = s;
        for (XmlNode c : children) {
            c.setSource(s);
        }
    }

    public static int namespaceKey(String uri) {
        if (uri == null || uri.length() == 0) {
            return NS_NONE;
        }
        if (uri.equals(ANDROID_NS_URI)) {
            return NS_ANDROID;
        }
        return NS_APP;
    }
}
