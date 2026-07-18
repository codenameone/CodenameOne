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
        assertEquals(ThemeDataAdapter.hex(t.colorScheme().inversePrimary()), p.get("FlutterAppBar.bgColor"));
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
}
