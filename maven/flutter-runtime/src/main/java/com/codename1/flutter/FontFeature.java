/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.flutter;

/**
 * dart:ui's {@code FontFeature}: an OpenType feature tag and its value.
 * Recorded on a {@link TextStyle} and not applied -- Codename One's Font
 * selects no OpenType features.
 */
public final class FontFeature {
    private final String feature;
    private final long value;

    public FontFeature(String feature, long value) {
        this.feature = feature;
        this.value = value;
    }

    /** {@code FontFeature(feature)}: the value defaults to 1 (on), as in dart:ui. */
    public FontFeature(String feature) {
        this(feature, 1);
    }

    public static FontFeature enable(String feature) {
        return new FontFeature(feature, 1);
    }

    public static FontFeature disable(String feature) {
        return new FontFeature(feature, 0);
    }

    public static FontFeature tabularFigures() {
        return new FontFeature("tnum", 1);
    }

    public static FontFeature proportionalFigures() {
        return new FontFeature("pnum", 1);
    }

    public static FontFeature superscripts() {
        return new FontFeature("sups", 1);
    }

    public static FontFeature subscripts() {
        return new FontFeature("subs", 1);
    }

    public String feature() {
        return feature;
    }

    public long value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof FontFeature && ((FontFeature) o).feature.equals(feature)
                && ((FontFeature) o).value == value;
    }

    @Override
    public int hashCode() {
        return feature.hashCode() * 31 + (int) value;
    }

    @Override
    public String toString() {
        return "FontFeature('" + feature + "', " + value + ")";
    }
}
