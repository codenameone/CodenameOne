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
 * A glyph in the CN1 material design icon font, identified by its codepoint
 * (one of the {@code FontImage.MATERIAL_*} char constants).
 */
public final class IconData {

    private final char codePoint;
    private String fontFamily;

    public IconData(char codePoint) {
        this.codePoint = codePoint;
    }

    /**
     * Convenience overload for the transpiler, which emits every Dart {@code int}
     * codepoint literal as a Java {@code long}. Gallery-font codepoints live in the
     * BMP private-use area, so the narrowing to {@code char} is lossless in practice.
     */
    public IconData(long codePoint) {
        this((char) codePoint);
    }

    public char codePoint() {
        return codePoint;
    }

    /** The named icon font this glyph belongs to (Dart's {@code IconData.fontFamily}). */
    public String fontFamily() {
        return fontFamily;
    }

    public void fontFamily(String v) {
        this.fontFamily = v;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof IconData && ((IconData) o).codePoint == codePoint;
    }

    @Override
    public int hashCode() {
        return codePoint;
    }
}
