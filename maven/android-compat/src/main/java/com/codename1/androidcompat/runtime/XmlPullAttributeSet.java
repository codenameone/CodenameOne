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

import org.xmlpull.v1.XmlPullParser;

/// The attributes of a text pull parser's current element as an
/// AttributeSet (`Xml.asAttributeSet`): values are strings, converted the
/// way Android's XmlUtils converts them.
public final class XmlPullAttributeSet implements AttributeSet {

    private final XmlPullParser parser;

    public XmlPullAttributeSet(XmlPullParser parser) {
        this.parser = parser;
    }

    @Override
    public int getAttributeCount() {
        return parser.getAttributeCount();
    }

    @Override
    public String getAttributeNamespace(int index) {
        return parser.getAttributeNamespace(index);
    }

    @Override
    public String getAttributeName(int index) {
        return parser.getAttributeName(index);
    }

    @Override
    public String getAttributeValue(int index) {
        return parser.getAttributeValue(index);
    }

    @Override
    public String getAttributeValue(String namespace, String name) {
        return parser.getAttributeValue(namespace, name);
    }

    @Override
    public String getPositionDescription() {
        return parser.getPositionDescription();
    }

    @Override
    public int getAttributeNameResource(int index) {
        return 0;
    }

    @Override
    public int getAttributeListValue(String namespace, String attribute, String[] options, int defaultValue) {
        return listValue(getAttributeValue(namespace, attribute), options, defaultValue);
    }

    @Override
    public boolean getAttributeBooleanValue(String namespace, String attribute, boolean defaultValue) {
        return toBoolean(getAttributeValue(namespace, attribute), defaultValue);
    }

    @Override
    public int getAttributeResourceValue(String namespace, String attribute, int defaultValue) {
        return toInt(getAttributeValue(namespace, attribute), defaultValue);
    }

    @Override
    public int getAttributeIntValue(String namespace, String attribute, int defaultValue) {
        return toInt(getAttributeValue(namespace, attribute), defaultValue);
    }

    @Override
    public int getAttributeUnsignedIntValue(String namespace, String attribute, int defaultValue) {
        return toInt(getAttributeValue(namespace, attribute), defaultValue);
    }

    @Override
    public float getAttributeFloatValue(String namespace, String attribute, float defaultValue) {
        return toFloat(getAttributeValue(namespace, attribute), defaultValue);
    }

    @Override
    public int getAttributeListValue(int index, String[] options, int defaultValue) {
        return listValue(getAttributeValue(index), options, defaultValue);
    }

    @Override
    public boolean getAttributeBooleanValue(int index, boolean defaultValue) {
        return toBoolean(getAttributeValue(index), defaultValue);
    }

    @Override
    public int getAttributeResourceValue(int index, int defaultValue) {
        return toInt(getAttributeValue(index), defaultValue);
    }

    @Override
    public int getAttributeIntValue(int index, int defaultValue) {
        return toInt(getAttributeValue(index), defaultValue);
    }

    @Override
    public int getAttributeUnsignedIntValue(int index, int defaultValue) {
        return toInt(getAttributeValue(index), defaultValue);
    }

    @Override
    public float getAttributeFloatValue(int index, float defaultValue) {
        return toFloat(getAttributeValue(index), defaultValue);
    }

    @Override
    public String getIdAttribute() {
        return getAttributeValue(null, "id");
    }

    @Override
    public String getClassAttribute() {
        return getAttributeValue(null, "class");
    }

    @Override
    public int getIdAttributeResourceValue(int defaultValue) {
        return getAttributeResourceValue(null, "id", defaultValue);
    }

    @Override
    public int getStyleAttribute() {
        return getAttributeResourceValue(null, "style", 0);
    }

    private static int listValue(String value, String[] options, int defaultValue) {
        if (value == null || options == null) {
            return defaultValue;
        }
        for (int i = 0; i < options.length; i++) {
            if (value.equals(options[i])) {
                return i;
            }
        }
        return defaultValue;
    }

    private static boolean toBoolean(String value, boolean defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        return value.equals("1") || value.equals("true") || value.equals("TRUE");
    }

    /// XmlUtils.convertValueToInt: decimal, `0x`/`#` hex and a leading
    /// zero for octal, optionally negative.
    static int toInt(String value, int defaultValue) {
        if (value == null || value.length() == 0) {
            return defaultValue;
        }
        String s = value;
        int sign = 1;
        int index = 0;
        int base = 10;
        if (s.charAt(0) == '-') {
            sign = -1;
            index++;
        }
        if (s.startsWith("0x", index) || s.startsWith("0X", index)) {
            index += 2;
            base = 16;
        } else if (s.startsWith("#", index)) {
            index++;
            base = 16;
        } else if (s.startsWith("0", index) && s.length() > index + 1) {
            index++;
            base = 8;
        }
        try {
            return (int) (Long.parseLong(s.substring(index), base) * sign);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static float toFloat(String value, float defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        try {
            return Float.parseFloat(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
