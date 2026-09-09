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
package com.codename1.flutter.fonts;

import com.codename1.flutter.FontWeight;
import com.codename1.flutter.TextStyle;
import com.codename1.flutter.material.TextTheme;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * The google_fonts shim must NAME the family it was asked for. Painting is a
 * separate question (it needs a bundled face and a Display); what is pinned here
 * is that the name survives the call at all, because it used to be dropped and
 * every study then rendered in the platform typeface.
 */
class GoogleFontsFamilyTest {

    @Test
    void aStyleCarriesItsFamily() {
        TextStyle s = GoogleFonts.workSans(16, FontWeight.w500, null, null, null,
                null, null, null, null);
        assertEquals("WorkSans", s.fontFamily());
        assertEquals(FontWeight.w500, s.fontWeight());
        assertEquals(16.0, s.fontSize().doubleValue());
    }

    @Test
    void eachHelperNamesItsOwnFamily() {
        assertEquals("Eczar", GoogleFonts.eczar(12, null, null, null, null, null, null, null, null)
                .fontFamily());
        assertEquals("LibreFranklin", GoogleFonts.libreFranklin(12, null, null, null, null, null,
                null, null, null).fontFamily());
        assertEquals("RobotoCondensed", GoogleFonts.robotoCondensed(12, null, null, null, null,
                null, null, null, null).fontFamily());
        assertEquals("Montserrat", GoogleFonts.montserrat(12, null, null, null, null, null, null,
                null, null).fontFamily());
    }

    @Test
    void aBaseStyleKeepsWhatTheCallDidNotOverride() {
        TextStyle base = new TextStyle();
        base.letterSpacing(2.5);
        TextStyle s = GoogleFonts.oswald(0, null, null, null, null, base, null, null, null);
        assertEquals("Oswald", s.fontFamily());
        assertEquals(2.5, s.getLetterSpacing().doubleValue());
    }

    @Test
    void aTextThemeIsRepointedAtTheFamily() {
        TextTheme themed = GoogleFonts.workSansTextTheme(new TextTheme());
        assertEquals("WorkSans", themed.bodyMedium().fontFamily());
        assertEquals("WorkSans", themed.displayLarge().fontFamily());
        assertEquals("WorkSans", themed.labelSmall().fontFamily());
    }

    @Test
    void anUnnamedFamilyResolvesToNothingRatherThanThrowing() {
        FontResolver.clearCache();
        assertNull(FontResolver.resolve(null, FontWeight.w400, false));
        assertNull(FontResolver.resolve("", FontWeight.w400, false));
        // No face is bundled with the tests, so a real name is a miss too --
        // and a miss must be a null, not an exception, or every Text on a
        // platform without the font would fail to build.
        assertNull(FontResolver.resolve("NoSuchFaceAnywhere", FontWeight.w700, false));
    }
}
