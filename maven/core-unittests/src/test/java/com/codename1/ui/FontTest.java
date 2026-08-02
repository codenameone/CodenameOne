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
package com.codename1.ui;

import com.codename1.junit.EdtTest;
import com.codename1.junit.UITestBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

class FontTest extends UITestBase {
    @BeforeEach
    void clearCaches() throws Exception {
        getDerivedFontCache().clear();
        setFontReturnedHeight(0f);
        implementation.setTrueTypeSupported(true);
        implementation.setNativeFontSchemeSupported(true);
    }

    @EdtTest
    void testCreateTrueTypeFontCachesByFileNameAndHeight() {
        Font first = Font.createTrueTypeFont("CustomFont", "custom.ttf");
        Font second = Font.createTrueTypeFont("CustomFont", "custom.ttf");
        assertSame(first, second);
        assertTrue(first.isTTFNativeFont());
    }

    @EdtTest
    void testCreateTrueTypeFontRejectsInvalidFileNames() {
        assertThrows(IllegalArgumentException.class, () -> Font.createTrueTypeFont("BadFont", "path/bad.ttf"));
        assertThrows(IllegalArgumentException.class, () -> Font.createTrueTypeFont("BadFont", "badfont.woff"));
    }

    /**
     * OpenType is loadable on every port -- Core Text, Typeface, DirectWrite,
     * FontConfig, java.awt and FontFace all read the SFNT container whether the
     * outlines are glyf or CFF -- so the file name check must not reject it.
     * The check is also case insensitive, since capitalisation says nothing
     * about whether a font will load.
     */
    @EdtTest
    void testCreateTrueTypeFontAcceptsBothFontContainers() {
        assertTrue(Font.isSupportedFontFile("font.ttf"));
        assertTrue(Font.isSupportedFontFile("font.otf"));
        assertTrue(Font.isSupportedFontFile("font.TTF"));
        assertTrue(Font.isSupportedFontFile("font.OTF"));
        assertFalse(Font.isSupportedFontFile("font.woff"));
        assertFalse(Font.isSupportedFontFile(null));
    }

    @EdtTest
    void testCreateTrueTypeFontReturnsNullWhenLoadingFails() {
        Font font = Font.createTrueTypeFont("native:Missing", "native:Missing");
        assertNull(font);
    }

    @EdtTest
    void testDeriveCachesByRequestedSizeAndWeight() {
        Font base = Font.createTrueTypeFont("BaseFont", "base.ttf");
        Font derived1 = base.derive(24f, Font.STYLE_BOLD);
        Font derived2 = base.derive(24f, Font.STYLE_BOLD);
        assertSame(derived1, derived2);
        assertEquals(24f, derived1.getPixelSize(), 0.01f);
        assertTrue(derived1.isTTFNativeFont());
    }

    @EdtTest
    void testDeriveCreatesDistinctFontsForDifferentWeights() {
        Font base = Font.createTrueTypeFont("WeightFont", "weight.ttf");
        Font plain = base.derive(18f, Font.STYLE_PLAIN);
        Font bold = base.derive(18f, Font.STYLE_BOLD);
        assertNotSame(plain, bold);
    }

    @EdtTest
    void testStringWidthHandlesSpecialCases() {
        Font font = Font.getDefaultFont();
        assertEquals(0, font.stringWidth(null));
        assertEquals(0, font.stringWidth(""));
        assertEquals(5, font.stringWidth(" "));
        assertEquals(24, font.stringWidth("abc"));
    }

    @EdtTest
    void testCharsWidthDelegatesToImplementation() {
        Font font = Font.getDefaultFont();
        char[] chars = new char[]{'a', 'b', 'c', 'd'};
        assertEquals(32, font.charsWidth(chars, 0, chars.length));
    }

    @EdtTest
    void testIsTrueTypeFileSupportedDelegatesToImplementation() {
        implementation.setTrueTypeSupported(false);
        assertFalse(Font.isTrueTypeFileSupported());
        implementation.setTrueTypeSupported(true);
        assertTrue(Font.isTrueTypeFileSupported());
    }

    private HashMap<String, Font> getDerivedFontCache() throws Exception {
        Field field = Font.class.getDeclaredField("derivedFontCache");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        HashMap<String, Font> cache = (HashMap<String, Font>) field.get(null);
        return cache;
    }

    private void setFontReturnedHeight(float value) throws Exception {
        Field field = Font.class.getDeclaredField("fontReturnedHeight");
        field.setAccessible(true);
        field.set(null, value);
    }
}
