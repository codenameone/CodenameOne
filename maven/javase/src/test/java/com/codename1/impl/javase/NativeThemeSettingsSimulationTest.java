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
package com.codename1.impl.javase;

import com.codename1.impl.NativeThemeSettings;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NativeThemeSettingsSimulationTest {
    private JavaSEPort previous;
    private double previousFontScale;

    @org.junit.jupiter.api.BeforeEach
    void savePort() {
        previous = JavaSEPort.instance;
        previousFontScale = JavaSEPort.getFontScale();
        JavaSEPort.setFontScale(1.0);
    }

    @org.junit.jupiter.api.AfterEach
    void restorePort() {
        JavaSEPort.instance = previous;
        JavaSEPort.setFontScale(previousFontScale);
    }

    @Test
    void simulatorSnapshotsAreDefensiveAndDeterministic() {
        JavaSEPort port = new JavaSEPort();
        NativeThemeSettings input = new NativeThemeSettings().color("accent-color", 0x123456).font("Dialog", 19);
        port.setSimulatorNativeThemeSettings(input);
        input.color("accent-color", 0xffffff);
        assertEquals("123456", port.getNativeThemeSettings().getColor("accent-color"));
        port.getNativeThemeSettings().color("accent-color", 0);
        assertEquals("123456", port.getNativeThemeSettings().getColor("accent-color"));
        assertEquals(19f, port.getNativeThemeSettings().getFontSize());
    }

    @Test
    void selectedUiFontPreservesRequestedSizeAndStyle() {
        JavaSEPort port = new JavaSEPort();
        java.awt.Font font = (java.awt.Font) port.loadNativeThemeFont("Serif", "native:MainBold", 23,
                java.awt.Font.BOLD | java.awt.Font.ITALIC);
        assertEquals("Serif", font.getFamily());
        assertEquals(23f, font.getSize2D());
        assertTrue(font.isBold());
        assertTrue(font.isItalic());
    }
    @Test
    void inheritedFontUsesPortScalingAndRetinaCorrection() {
        JavaSEPort port = new JavaSEPort();
        for (double scale : new double[] {1.0, 1.5, 0.5}) {
            JavaSEPort.setFontScale(scale);
            int style = com.codename1.ui.Font.STYLE_BOLD | com.codename1.ui.Font.STYLE_ITALIC;
            java.awt.Font inherited = (java.awt.Font) port.loadNativeThemeFont("Serif", "native:MainBold", 24, style);
            java.awt.Font ordinary = (java.awt.Font) port.deriveTrueTypeFont(new java.awt.Font("Serif", 0, 1), 24, style);
            assertEquals(ordinary, inherited, "Inherited UI fonts must use the same physical scaling");
            assertEquals(scale == 1.5 ? 36f : 24f, inherited.getSize2D());
            assertTrue(inherited.isBold());
            assertTrue(inherited.isItalic());
        }
    }

    @Test
    void gnomeFontUsesFamilyPointSizeAndDesktopDpi() {
        NativeThemeSettings settings = new NativeThemeSettings();
        JavaSEPort.readDesktopThemeFont(settings, null, "Noto Sans Bold Italic 10.5", 144 * 1024);
        assertEquals("Noto Sans", settings.getFontFamily());
        assertEquals(21f, settings.getFontSize());
        JavaSEPort.readDesktopThemeFont(settings, null, "DejaVu Sans 12", null);
        assertEquals("DejaVu Sans", settings.getFontFamily());
        assertEquals(16f, settings.getFontSize());
        JavaSEPort.readDesktopThemeFont(settings, null, "Noto Sans 17px", 144 * 1024);
        assertEquals(17f, settings.getFontSize());
    }

    @Test
    void desktopFontFallbackIgnoresMalformedValuesAndPrefersWindowsFont() {
        for (Object value : new Object[] {null, 12, "Sans", "Sans -2", "Sans NaN", "Sans 0"}) {
            NativeThemeSettings settings = new NativeThemeSettings();
            JavaSEPort.readDesktopThemeFont(settings, null, value, null);
            assertNull(settings.getFontFamily());
        }
        NativeThemeSettings settings = new NativeThemeSettings();
        JavaSEPort.readDesktopThemeFont(settings, new java.awt.Font("Dialog", 0, 18), "Sans 12", null);
        assertEquals("Dialog", settings.getFontFamily());
        assertEquals(18f, settings.getFontSize());
    }
    @Test
    void desktopSettingsNotificationInvalidatesCachedAppearance() throws Exception {
        java.lang.reflect.Field resolved = JavaSEPort.class.getDeclaredField("osDarkModeResolved");
        java.lang.reflect.Field appearance = JavaSEPort.class.getDeclaredField("osDarkMode");
        resolved.setAccessible(true);
        appearance.setAccessible(true);
        Object previousResolved = resolved.get(null);
        Object previousAppearance = appearance.get(null);
        try {
            for (Boolean oldAppearance : new Boolean[] {Boolean.TRUE, Boolean.FALSE}) {
                resolved.set(null, true);
                appearance.set(null, oldAppearance);
                JavaSEPort.desktopThemeSettingsChanged();
                assertEquals(false, resolved.get(null));
                assertNull(appearance.get(null));
            }
        } finally {
            resolved.set(null, previousResolved);
            appearance.set(null, previousAppearance);
        }
    }
}
