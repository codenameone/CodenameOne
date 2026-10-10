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
package com.codename1.flutter.material;

import com.codename1.flutter.Brightness;
import com.codename1.flutter.Color;
import com.codename1.flutter.Colors;
import com.codename1.flutter.ThemeMode;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The M4 theming contract: ThemeData drives the Flutter* UIID overlay, and
 * themeMode + platform brightness pick the effective theme.
 */
public class ThemingTest {

    private ThemeData light() {
        ThemeData t = new ThemeData();
        t.colorScheme(ColorScheme.fromSeed(Colors.deepPurple));
        return t;
    }

    private ThemeData dark() {
        ThemeData t = new ThemeData();
        t.colorScheme(ColorScheme.fromSeed(Colors.deepPurple, Brightness.dark));
        t.brightness(Brightness.dark);
        return t;
    }

    // ------------------------------------------------------------------
    // Prop table
    // ------------------------------------------------------------------

    @Test
    public void colorSchemeLandsOnFlutterUiids() {
        ThemeData t = light();
        Map<String, Object> p = ThemeDataAdapter.themeProps(t);
        assertEquals(ThemeDataAdapter.hex(t.colorScheme().surface()), p.get("FlutterScaffold.bgColor"));
        assertEquals(ThemeDataAdapter.hex(t.colorScheme().onSurface()), p.get("FlutterText.fgColor"));
        assertEquals(ThemeDataAdapter.hex(t.colorScheme().primary()), p.get("FlutterElevatedButton.bgColor"));
        assertEquals(ThemeDataAdapter.hex(t.colorScheme().onPrimary()), p.get("FlutterElevatedButton.fgColor"));
        // M3: the app bar sits on the surface with an elevation tint, not a
        // saturated fill (matches AppBarRenderElement's per-instance default)
        assertEquals(ThemeDataAdapter.hex(t.colorScheme().surface()), p.get("FlutterAppBar.bgColor"));
        assertEquals(ThemeDataAdapter.hex(t.colorScheme().onSurface()), p.get("FlutterAppBar.fgColor"));
    }

    @Test
    public void overlayOnlyTouchesFlutterNamespace() {
        // the overlay must never restyle a host app's own components
        for (String key : ThemeDataAdapter.themeProps(light()).keySet()) {
            String uiid = key.substring(0, key.indexOf('.'));
            // state prefixes (sel#, press#, dis#) may precede the UIID
            int hash = uiid.indexOf('#');
            if (hash >= 0) {
                uiid = uiid.substring(hash + 1);
            }
            assertTrue(uiid.startsWith("Flutter"),
                    "overlay key outside the Flutter namespace: " + key);
        }
    }

    @Test
    public void lightAndDarkProduceDifferentSurfaces() {
        Map<String, Object> l = ThemeDataAdapter.themeProps(light());
        Map<String, Object> d = ThemeDataAdapter.themeProps(dark());
        assertNotEquals(l.get("FlutterScaffold.bgColor"), d.get("FlutterScaffold.bgColor"));
        assertNotEquals(l.get("FlutterText.fgColor"), d.get("FlutterText.fgColor"));
    }

    @Test
    public void darkSurfaceIsDarkerThanItsOnColor() {
        ColorScheme cs = ColorScheme.fromSeed(Colors.deepPurple, Brightness.dark);
        assertTrue(luminance(cs.surface()) < luminance(cs.onSurface()),
                "dark scheme must paint light content on a dark surface");
        ColorScheme lightCs = ColorScheme.fromSeed(Colors.deepPurple, Brightness.light);
        assertTrue(luminance(lightCs.surface()) > luminance(lightCs.onSurface()),
                "light scheme must paint dark content on a light surface");
    }

    private double luminance(Color c) {
        int rgb = c.rgb();
        return 0.2126 * ((rgb >> 16) & 0xFF) + 0.7152 * ((rgb >> 8) & 0xFF) + 0.0722 * (rgb & 0xFF);
    }

    @Test
    public void hexIsSixDigitRgb() {
        String h = ThemeDataAdapter.hex(new Color(0xFF102030));
        assertEquals("102030", h);
    }

    // ------------------------------------------------------------------
    // Effective-theme matrix (themeMode × platform brightness)
    // ------------------------------------------------------------------

    @Test
    public void themeModeDecisionTable() {
        assertTrue(MaterialApp.wantsDark(ThemeMode.dark, Boolean.FALSE), "explicit dark wins");
        assertFalse(MaterialApp.wantsDark(ThemeMode.light, Boolean.TRUE), "explicit light wins");
        assertTrue(MaterialApp.wantsDark(ThemeMode.system, Boolean.TRUE), "system follows platform");
        assertFalse(MaterialApp.wantsDark(ThemeMode.system, Boolean.FALSE), "system follows platform");
        assertFalse(MaterialApp.wantsDark(ThemeMode.system, null), "unknown platform = light");
        assertFalse(MaterialApp.wantsDark(null, null), "default mode = system = light");
        assertTrue(MaterialApp.wantsDark(null, Boolean.TRUE), "default mode follows platform");
    }

    @Test
    public void effectiveThemePicksDarkThemeWhenDark() {
        MaterialApp app = new MaterialApp();
        ThemeData l = light();
        ThemeData d = dark();
        app.theme(l);
        app.darkTheme(d);

        app.themeMode(ThemeMode.light);
        assertEquals(l, app.effectiveTheme());

        app.themeMode(ThemeMode.dark);
        assertEquals(d, app.effectiveTheme(), "dark mode must select darkTheme");
    }

    @Test
    public void effectiveThemeFallsBackToLightThemeWithoutDarkTheme() {
        MaterialApp app = new MaterialApp();
        ThemeData l = light();
        app.theme(l);
        app.themeMode(ThemeMode.dark);
        // no darkTheme supplied — Flutter keeps using theme
        assertEquals(l, app.effectiveTheme());
    }

    @Test
    public void effectiveThemeSynthesizesWhenNoThemeGiven() {
        MaterialApp app = new MaterialApp();
        app.themeMode(ThemeMode.dark);
        ThemeData t = app.effectiveTheme();
        assertNotNull(t);
        assertEquals(Brightness.dark, t.brightness(),
                "a synthesized theme must carry the requested brightness");
    }

    // ------------------------------------------------------------------
    // Alpha
    // ------------------------------------------------------------------

    /**
     * {@code Colors.transparent} is 0x00000000 — alpha 0 over BLACK. Painting
     * only its RGB word turns every "transparent" app bar and scaffold in the
     * gallery into an opaque black band.
     */
    @Test
    public void transparentPaintsNothingRatherThanBlack() {
        com.codename1.ui.plaf.Style s = new com.codename1.ui.plaf.Style();
        ThemeDataAdapter.paintColor(s, new com.codename1.flutter.Color(0x00000000L));
        assertEquals(0, s.getBgTransparency() & 0xFF, "alpha 0 must paint nothing");
    }

    @Test
    public void opaqueColorPaintsFully() {
        com.codename1.ui.plaf.Style s = new com.codename1.ui.plaf.Style();
        ThemeDataAdapter.paintColor(s, new com.codename1.flutter.Color(0xFF2196F3L));
        assertEquals(0x2196F3, s.getBgColor());
        assertEquals(255, s.getBgTransparency() & 0xFF);
    }

    @Test
    public void partialAlphaCarriesThrough() {
        com.codename1.ui.plaf.Style s = new com.codename1.ui.plaf.Style();
        ThemeDataAdapter.paintColor(s, new com.codename1.flutter.Color(0x80FF0000L));
        assertEquals(0xFF0000, s.getBgColor());
        assertEquals(0x80, s.getBgTransparency() & 0xFF);
    }
}
