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
package com.codename1.impl.mac;

import com.codename1.impl.ios.IOSImplementation;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MacNativeFontModeTest {
    private final MacImplementation implementation = new MacImplementation();

    @After
    public void restoreDefaultMode() {
        IOSImplementation.setIosMode("auto");
    }

    @Test
    public void aquaModesUseAppKitAliases() {
        String[] modes = {null, "auto", "aqua", "native", "AQUA"};
        for (String mode : modes) {
            IOSImplementation.setIosMode(mode);
            assertEquals(mode, "native:MainRegular", implementation.nativeFontName("native:MainRegular"));
            assertEquals(mode, "native:ItalicRegular", implementation.nativeFontName("native:ItalicRegular"));
        }
    }

    @Test
    public void iosStyleModesKeepTheirHistoricalFontMapping() {
        String[] modes = {"modern", "liquid", "ios7", "flat", "material", "MODERN"};
        for (String mode : modes) {
            IOSImplementation.setIosMode(mode);
            assertEquals(mode, "HelveticaNeue-Medium", implementation.nativeFontName("native:MainRegular"));
            assertEquals(mode, "HelveticaNeue-MediumItalic", implementation.nativeFontName("native:ItalicRegular"));
        }
    }

    @Test
    public void namedApplicationFontsAreNotRemapped() {
        for (String mode : new String[] {"aqua", "modern"}) {
            IOSImplementation.setIosMode(mode);
            assertEquals(mode, "Material Icons", implementation.nativeFontName("Material Icons"));
            assertEquals(mode, "HelveticaNeue-Medium", implementation.nativeFontName("HelveticaNeue-Medium"));
            assertEquals(mode, null, implementation.nativeFontName(null));
        }
    }
}
