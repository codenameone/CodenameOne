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
package com.codename1.ui.plaf;

import com.codename1.junit.UITestBase;
import com.codename1.testing.TestCodenameOneImplementation;
import com.codename1.ui.Font;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import java.util.Hashtable;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class UIManagerLargeTextScaleTest extends UITestBase {
    @Test
    public void testLargeTextScaleFlagAppliesToThemeFonts() {
        TestCodenameOneImplementation impl = implementation;
        impl.setLargerTextEnabled(true);
        impl.setLargerTextScale(1.5f);

        UIManager manager = UIManager.getInstance();
        manager.setUseLargerTextScale(true);

        Font baseFont = Font.createTrueTypeFont(Font.NATIVE_MAIN_REGULAR, Font.NATIVE_MAIN_REGULAR)
                .derive(20f, Font.STYLE_PLAIN);
        Hashtable theme = new Hashtable();
        theme.put("Button.font", baseFont);

        manager.setThemeProps(theme);

        Font scaledFont = manager.getComponentStyle("Button").getFont();
        assertEquals(30f, scaledFont.getPixelSize(), 0.01f);
    }

    @Test
    public void testThemeConstantEnablesLargeTextScaling() {
        TestCodenameOneImplementation impl = implementation;
        impl.setLargerTextEnabled(true);
        impl.setLargerTextScale(1.25f);

        UIManager manager = UIManager.getInstance();
        manager.setUseLargerTextScale(false);

        Font baseFont = Font.createTrueTypeFont(Font.NATIVE_MAIN_REGULAR, Font.NATIVE_MAIN_REGULAR)
                .derive(16f, Font.STYLE_PLAIN);
        Hashtable theme = new Hashtable();
        theme.put("@useLargerTextScaleBool", "true");
        theme.put("Label.font", baseFont);

        manager.setThemeProps(theme);

        Font scaledFont = manager.getComponentStyle("Label").getFont();
        assertEquals(20f, scaledFont.getPixelSize(), 0.01f);
    }

    @Test
    public void testLargeTextScaleDisabledByDefault() {
        TestCodenameOneImplementation impl = implementation;
        impl.setLargerTextEnabled(true);
        impl.setLargerTextScale(1.5f);

        UIManager manager = UIManager.getInstance();
        manager.setUseLargerTextScale(false);

        Font baseFont = Font.createTrueTypeFont(Font.NATIVE_MAIN_REGULAR, Font.NATIVE_MAIN_REGULAR)
                .derive(12f, Font.STYLE_PLAIN);
        Hashtable theme = new Hashtable();
        theme.put("Title.font", baseFont);

        manager.setThemeProps(theme);

        Font scaledFont = manager.getComponentStyle("Title").getFont();
        assertEquals(12f, scaledFont.getPixelSize(), 0.01f);
    }

    /// Walking the scale up and back down via repeated [UIManager#refreshTheme]
    /// calls (the path the simulator's Larger Text menu takes) must always
    /// derive sizes from the original installed font, not from the
    /// previously-scaled font. Without the rollback in
    /// [UIManager#applyLargerTextScaleToThemeFonts] each step compounded the
    /// previous one: XL -> XXL over-scaled, and a return to scale 1.0 never
    /// shrank the fonts back. Regression cover for issue #4963.
    @Test
    public void testRepeatedScaleChangesDoNotCompound() {
        TestCodenameOneImplementation impl = implementation;
        UIManager manager = UIManager.getInstance();
        manager.setUseLargerTextScale(true);

        Font baseFont = Font.createTrueTypeFont(Font.NATIVE_MAIN_REGULAR, Font.NATIVE_MAIN_REGULAR)
                .derive(20f, Font.STYLE_PLAIN);
        Hashtable theme = new Hashtable();
        theme.put("Button.font", baseFont);

        impl.setLargerTextEnabled(false);
        impl.setLargerTextScale(1.0f);
        manager.setThemeProps(theme);
        assertEquals(20f, manager.getComponentStyle("Button").getFont().getPixelSize(), 0.01f);

        // First bump: 1.0 -> 1.5 should yield 30.
        impl.setLargerTextEnabled(true);
        impl.setLargerTextScale(1.5f);
        manager.refreshTheme();
        assertEquals(30f, manager.getComponentStyle("Button").getFont().getPixelSize(), 0.01f);

        // Second bump: 1.5 -> 2.0 must yield 40 (20 * 2.0), not 60 (30 * 2.0).
        impl.setLargerTextScale(2.0f);
        manager.refreshTheme();
        assertEquals(40f, manager.getComponentStyle("Button").getFont().getPixelSize(), 0.01f);

        // Step back down: 2.0 -> 1.25 must yield 25, not some compounded value.
        impl.setLargerTextScale(1.25f);
        manager.refreshTheme();
        assertEquals(25f, manager.getComponentStyle("Button").getFont().getPixelSize(), 0.01f);

        // Return to default scale: fonts must shrink all the way back to 20.
        impl.setLargerTextEnabled(false);
        impl.setLargerTextScale(1.0f);
        manager.refreshTheme();
        assertEquals(20f, manager.getComponentStyle("Button").getFont().getPixelSize(), 0.01f);
    }

    /// Calling [UIManager#refreshTheme] alone rebuilds the theme cache but
    /// does not push the rebuilt styles down to components that already
    /// resolved their styles. The simulator's Larger Text menu therefore has
    /// to follow `UIManager.refreshTheme()` with `Form.refreshTheme(true)`
    /// (see `JavaSEPort.refreshThemeOnly`) so the components on screen
    /// actually pick up the new fonts. This test pins that contract by
    /// verifying an existing Label's resolved font tracks the scale through
    /// a full up-and-back-down cycle. Issue #4963.
    @Test
    public void testFormRefreshThemePropagatesScaleChange() {
        TestCodenameOneImplementation impl = implementation;
        UIManager manager = UIManager.getInstance();
        manager.setUseLargerTextScale(true);

        Font baseFont = Font.createTrueTypeFont(Font.NATIVE_MAIN_REGULAR, Font.NATIVE_MAIN_REGULAR)
                .derive(18f, Font.STYLE_PLAIN);
        Hashtable theme = new Hashtable();
        theme.put("Label.font", baseFont);

        impl.setLargerTextEnabled(false);
        impl.setLargerTextScale(1.0f);
        manager.setThemeProps(theme);

        Form form = new Form();
        Label label = new Label("Hello");
        form.addComponent(label);
        // Force the style chain to be resolved so the label is holding a
        // Style instance from the pre-refresh theme.
        assertEquals(18f, label.getStyle().getFont().getPixelSize(), 0.01f);

        impl.setLargerTextEnabled(true);
        impl.setLargerTextScale(1.5f);
        manager.refreshTheme();
        form.refreshTheme(true);
        assertEquals(27f, label.getStyle().getFont().getPixelSize(), 0.01f);

        impl.setLargerTextScale(2.0f);
        manager.refreshTheme();
        form.refreshTheme(true);
        assertEquals(36f, label.getStyle().getFont().getPixelSize(), 0.01f);

        impl.setLargerTextEnabled(false);
        impl.setLargerTextScale(1.0f);
        manager.refreshTheme();
        form.refreshTheme(true);
        assertEquals(18f, label.getStyle().getFont().getPixelSize(), 0.01f);
    }


    /// A theme with `@includeNativeBool: true` -- what every project generated
    /// by the archetype or Initializr ships -- makes `buildTheme` install the
    /// native theme first, and that re-enters the whole
    /// `setThemeProps -> buildTheme` path with only the native theme's entries
    /// in themeProps. The larger-text pass used to run on that inner build too:
    /// it could not see any of the app theme's font keys, so its rollback left
    /// them alone and then cleared the bookkeeping that remembered their
    /// unscaled originals. The outer build then re-scaled fonts that were
    /// already scaled, with nothing left to roll back -- so simply toggling
    /// Dark/Light Mode in the simulator (which calls `refreshTheme`) grew every
    /// font by the scale factor again, and again, on each toggle. Regression
    /// cover for issue #5565.
    @Test
    public void testRefreshWithNativeThemeDoesNotCompound() {
        TestCodenameOneImplementation impl = implementation;
        UIManager manager = UIManager.getInstance();
        manager.setUseLargerTextScale(true);
        impl.setLargerTextEnabled(true);
        impl.setLargerTextScale(1.5f);

        Font nativeFont = Font.createTrueTypeFont(Font.NATIVE_MAIN_REGULAR, Font.NATIVE_MAIN_REGULAR)
                .derive(10f, Font.STYLE_PLAIN);
        Hashtable nativeTheme = new Hashtable();
        nativeTheme.put("Label.font", nativeFont);
        impl.setNativeTheme(nativeTheme);

        Font appFont = Font.createTrueTypeFont(Font.NATIVE_MAIN_REGULAR, Font.NATIVE_MAIN_REGULAR)
                .derive(20f, Font.STYLE_PLAIN);
        Hashtable theme = new Hashtable();
        theme.put("@includeNativeBool", "true");
        theme.put("Title.font", appFont);
        manager.setThemeProps(theme);

        assertEquals(30f, manager.getComponentStyle("Title").getFont().getPixelSize(), 0.01f);
        assertEquals(15f, manager.getComponentStyle("Label").getFont().getPixelSize(), 0.01f);

        // Every refresh (the Dark/Light Mode menu does one per click) has to be
        // idempotent -- the size must stay at original * scale, forever.
        for (int iter = 0; iter < 5; iter++) {
            manager.refreshTheme();
            assertEquals(30f, manager.getComponentStyle("Title").getFont().getPixelSize(), 0.01f,
                    "app theme font must not compound on refresh " + iter);
            assertEquals(15f, manager.getComponentStyle("Label").getFont().getPixelSize(), 0.01f,
                    "native theme font must not compound on refresh " + iter);
        }

        // And the scale must still be reversible after those refreshes.
        impl.setLargerTextEnabled(false);
        impl.setLargerTextScale(1.0f);
        manager.refreshTheme();
        assertEquals(20f, manager.getComponentStyle("Title").getFont().getPixelSize(), 0.01f);
        assertEquals(10f, manager.getComponentStyle("Label").getFont().getPixelSize(), 0.01f);
    }

    /// The same layered install with the scale left at its default must be a
    /// no-op: repeated refreshes may not touch the fonts at all. Issue #5565.
    @Test
    public void testRefreshWithNativeThemeKeepsUnscaledFonts() {
        TestCodenameOneImplementation impl = implementation;
        UIManager manager = UIManager.getInstance();
        manager.setUseLargerTextScale(true);
        impl.setLargerTextEnabled(false);
        impl.setLargerTextScale(1.0f);

        Font nativeFont = Font.createTrueTypeFont(Font.NATIVE_MAIN_REGULAR, Font.NATIVE_MAIN_REGULAR)
                .derive(11f, Font.STYLE_PLAIN);
        Hashtable nativeTheme = new Hashtable();
        nativeTheme.put("Label.font", nativeFont);
        impl.setNativeTheme(nativeTheme);

        Font appFont = Font.createTrueTypeFont(Font.NATIVE_MAIN_REGULAR, Font.NATIVE_MAIN_REGULAR)
                .derive(22f, Font.STYLE_PLAIN);
        Hashtable theme = new Hashtable();
        theme.put("@includeNativeBool", "true");
        theme.put("Title.font", appFont);
        manager.setThemeProps(theme);

        for (int iter = 0; iter < 5; iter++) {
            manager.refreshTheme();
            assertEquals(22f, manager.getComponentStyle("Title").getFont().getPixelSize(), 0.01f);
            assertEquals(11f, manager.getComponentStyle("Label").getFont().getPixelSize(), 0.01f);
        }
    }

}
