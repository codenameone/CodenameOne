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

import android.util.AttributeSet;
import android.util.TypedValue;

/// The attributes of one compiled XML element: what a view constructor gets
/// during inflation. Values were typed at build time; the string accessors
/// answer with the source text, as Android's do.
public final class CompiledAttributeSet implements AttributeSet {

    private final XmlNode node;

    public CompiledAttributeSet(XmlNode node) {
        this.node = node;
    }

    public XmlNode getNode() {
        return node;
    }

    /// The value set for the attribute with resource id `attrId`, or null.
    public ResValue valueForAttr(int attrId) {
        return node.valueForAttr(attrId);
    }

    @Override
    public int getAttributeCount() {
        return node.names.length;
    }

    @Override
    public String getAttributeNamespace(int index) {
        switch (node.namespaces[index]) {
            case XmlNode.NS_ANDROID:
                return XmlNode.ANDROID_NS_URI;
            case XmlNode.NS_APP:
                return XmlNode.APP_NS_URI;
            default:
                return "";
        }
    }

    @Override
    public String getAttributeName(int index) {
        return node.names[index];
    }

    @Override
    public String getAttributeValue(int index) {
        return text(node.values[index]);
    }

    @Override
    public String getAttributeValue(String namespace, String name) {
        int i = node.indexOf(XmlNode.namespaceKey(namespace), name);
        return i < 0 ? null : text(node.values[i]);
    }

    private static String text(ResValue v) {
        if (v.string != null) {
            return v.string;
        }
        CharSequence s = TypedValue.coerceToString(v.type, v.data);
        return s == null ? null : s.toString();
    }

    @Override
    public String getPositionDescription() {
        return (node.getSource() == null ? "Binary XML" : node.getSource()) + " line #" + node.line
                + " <" + node.tag + ">";
    }

    @Override
    public int getAttributeNameResource(int index) {
        return node.attrIds[index];
    }

    private int index(String namespace, String attribute) {
        return node.indexOf(XmlNode.namespaceKey(namespace), attribute);
    }

    @Override
    public int getAttributeListValue(String namespace, String attribute, String[] options, int defaultValue) {
        int i = index(namespace, attribute);
        return i < 0 ? defaultValue : getAttributeListValue(i, options, defaultValue);
    }

    @Override
    public boolean getAttributeBooleanValue(String namespace, String attribute, boolean defaultValue) {
        int i = index(namespace, attribute);
        return i < 0 ? defaultValue : getAttributeBooleanValue(i, defaultValue);
    }

    @Override
    public int getAttributeResourceValue(String namespace, String attribute, int defaultValue) {
        int i = index(namespace, attribute);
        return i < 0 ? defaultValue : getAttributeResourceValue(i, defaultValue);
    }

    @Override
    public int getAttributeIntValue(String namespace, String attribute, int defaultValue) {
        int i = index(namespace, attribute);
        return i < 0 ? defaultValue : getAttributeIntValue(i, defaultValue);
    }

    @Override
    public int getAttributeUnsignedIntValue(String namespace, String attribute, int defaultValue) {
        return getAttributeIntValue(namespace, attribute, defaultValue);
    }

    @Override
    public float getAttributeFloatValue(String namespace, String attribute, float defaultValue) {
        int i = index(namespace, attribute);
        return i < 0 ? defaultValue : getAttributeFloatValue(i, defaultValue);
    }

    @Override
    public int getAttributeListValue(int index, String[] options, int defaultValue) {
        String v = getAttributeValue(index);
        if (v == null || options == null) {
            return defaultValue;
        }
        for (int i = 0; i < options.length; i++) {
            if (v.equals(options[i])) {
                return i;
            }
        }
        return defaultValue;
    }

    @Override
    public boolean getAttributeBooleanValue(int index, boolean defaultValue) {
        ResValue v = node.values[index];
        if (v.type >= TypedValue.TYPE_FIRST_INT && v.type <= TypedValue.TYPE_LAST_INT) {
            return v.data != 0;
        }
        return defaultValue;
    }

    @Override
    public int getAttributeResourceValue(int index, int defaultValue) {
        ResValue v = node.values[index];
        return v.type == TypedValue.TYPE_REFERENCE ? v.data : defaultValue;
    }

    @Override
    public int getAttributeIntValue(int index, int defaultValue) {
        ResValue v = node.values[index];
        if (v.type >= TypedValue.TYPE_FIRST_INT && v.type <= TypedValue.TYPE_LAST_INT) {
            return v.data;
        }
        return defaultValue;
    }

    @Override
    public int getAttributeUnsignedIntValue(int index, int defaultValue) {
        return getAttributeIntValue(index, defaultValue);
    }

    @Override
    public float getAttributeFloatValue(int index, float defaultValue) {
        ResValue v = node.values[index];
        if (v.type == TypedValue.TYPE_FLOAT) {
            return Float.intBitsToFloat(v.data);
        }
        return defaultValue;
    }

    @Override
    public String getIdAttribute() {
        return getAttributeValue(XmlNode.ANDROID_NS_URI, "id");
    }

    @Override
    public String getClassAttribute() {
        return getAttributeValue("", "class");
    }

    @Override
    public int getIdAttributeResourceValue(int defaultValue) {
        return getAttributeResourceValue(XmlNode.ANDROID_NS_URI, "id", defaultValue);
    }

    @Override
    public int getStyleAttribute() {
        int i = node.indexOf(XmlNode.NS_NONE, "style");
        if (i < 0) {
            return 0;
        }
        ResValue v = node.values[i];
        return v.type == TypedValue.TYPE_REFERENCE || v.type == TypedValue.TYPE_ATTRIBUTE ? v.data : 0;
    }

    /// Whether `style=` names a theme attribute (`style="?attr/x"`), which
    /// obtainStyledAttributes resolves through the theme first.
    public boolean isStyleAttributeThemeReference() {
        int i = node.indexOf(XmlNode.NS_NONE, "style");
        return i >= 0 && node.values[i].type == TypedValue.TYPE_ATTRIBUTE;
    }
}
