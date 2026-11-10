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
// Not final: icon packs declare their own subclasses (`class MyIcons extends IconData`
// with `const MyIcons(int cp) : super(cp, fontFamily: ...)`), as Flutter allows.
public class IconData {

    private char codePoint;
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

    /**
     * For subclasses (icon packs): {@code const MyIcons(int cp) : super(cp, fontFamily: ...)}
     * reaches here, the transpiler then assigning each argument through its setter.
     */
    protected IconData() {
        this((char) 0);
    }

    private String fontPackage;
    private dart.core.DartList<String> fontFamilyFallback;

    /** Flutter's {@code IconData.codePoint}, as the int Dart sees. */
    private boolean matchTextDirection;

    /** Flutter's {@code IconData.matchTextDirection}: mirror the glyph in right-to-left text. */
    public boolean matchTextDirection() {
        return matchTextDirection;
    }

    public void matchTextDirection(boolean v) {
        this.matchTextDirection = v;
    }

    public long codePoint() {
        return codePoint;
    }

    public void codePoint(long v) {
        this.codePoint = (char) v;
    }

    /** Flutter's {@code IconData.fontPackage}: the package bundling the font, or null. */
    public String fontPackage() {
        return fontPackage;
    }

    public void fontPackage(String v) {
        this.fontPackage = v;
    }

    public char codePointChar() {
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
        if (!(o instanceof IconData)) {
            return false;
        }
        IconData d = (IconData) o;
        // Flutter's IconData.== compares the code point, the font and the direction flag.
        return d.codePoint == codePoint && same(d.fontFamily, fontFamily) && same(d.fontPackage, fontPackage)
                && d.matchTextDirection == matchTextDirection;
    }

    private static boolean same(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    @Override
    public int hashCode() {
        return codePoint * 31 + (fontFamily == null ? 0 : fontFamily.hashCode());
    }

    /// IconData.fontFamilyFallback: the fallback font families given to the constructor.
    public dart.core.DartList<String> fontFamilyFallback() {
        return fontFamilyFallback;
    }

    public void fontFamilyFallback(dart.core.DartList<String> v) {
        this.fontFamilyFallback = v;
    }
}
