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

import com.codename1.flutter.Color;
import com.codename1.ui.Form;
import com.codename1.ui.plaf.Style;

import java.util.HashMap;
import java.util.Map;

/**
 * Converts the ACTIVE {@link ThemeData} into a CN1 UIManager theme overlay
 * for the Flutter* UIID namespace ({@code UIManager.addThemeProps}), plus
 * direct styling of the Flutter-owned Form.
 *
 * <p><b>Theme isolation:</b> the overlay only ever writes keys in the
 * {@code Flutter*} UIID namespace — non-Flutter UIIDs (Label, Button, Form,
 * Toolbar, ...) are never touched globally, so a Flutter subtree embedded in
 * a regular CN1 app can't restyle the host. The Form and Toolbar backgrounds
 * that Flutter DOES own (the runApp Form, the root Scaffold's Toolbar) are
 * styled per-instance ({@link #applyToForm}, AppBarRenderElement) rather
 * than through global theme constants.</p>
 *
 * <p>Prop table (colors are CN1 theme hex strings):</p>
 * <ul>
 *   <li>colorScheme.surface — FlutterScaffold (the root canvas) bgColor, and
 *       Drawer/BottomNavigationBar backgrounds</li>
 *   <li>colorScheme.onSurface — FlutterText/FlutterRichText/FlutterIcon
 *       fgColor, FlutterIconButton fgColor</li>
 *   <li>colorScheme.primary/onPrimary — FlutterElevatedButton bg/fg;
 *       primary — FlutterTextButton/FlutterOutlinedButton fg</li>
 *   <li>colorScheme.surface/onSurface — FlutterAppBar bg/fg: a Material 3 app
 *       bar sits on the surface with an elevation tint rather than a saturated
 *       fill (this is the strip-mode bar; toolbar mode is styled per-instance
 *       by AppBarRenderElement, to the same default)</li>
 * </ul>
 *
 * <p>State-metric invariance (see RenderElement.unifyStateMetrics): the
 * overlay also writes the {@code sel#}/{@code press#}/{@code dis#} variants
 * of every color so a focus/press state change never swaps in a stale
 * base-theme color.</p>
 */
public final class ThemeDataAdapter {

    private ThemeDataAdapter() {
    }

    /**
     * The pure prop table for a theme (headless-testable). Every key is in
     * the Flutter* UIID namespace.
     */
    public static Map<String, Object> themeProps(ThemeData t) {
        ColorScheme cs = t.colorScheme();
        String surface = hex(cs.surface());
        String onSurface = hex(cs.onSurface());
        String primary = hex(cs.primary());
        String onPrimary = hex(cs.onPrimary());
        String inversePrimary = hex(cs.inversePrimary());

        String surfaceVariant = hex(cs.surfaceVariant() != null
                ? cs.surfaceVariant() : cs.surface());
        // Material's disabled tone: onSurface at 38% over the surface.
        String onSurfaceFaded = hex(com.codename1.flutter.Color.alphaBlend(
                new com.codename1.flutter.Color(
                        (0x61L << 24) | (cs.onSurface().rgb() & 0xFFFFFFL)),
                new com.codename1.flutter.Color(
                        0xFF000000L | (cs.surface().rgb() & 0xFFFFFF))));

        Map<String, Object> p = new HashMap<String, Object>();

        // The Flutter canvas (root host container) and full-bleed surfaces.
        bg(p, "FlutterScaffold", surface);
        bg(p, "FlutterDrawer", surface);
        bg(p, "FlutterBottomNavigationBar", surface);

        // Content foregrounds.
        fg(p, "FlutterText", onSurface);
        fg(p, "FlutterRichText", onSurface);
        fg(p, "FlutterIcon", onSurface);
        fg(p, "FlutterListTile", onSurface);

        // Buttons (ButtonRenderElement also styles programmatically from
        // Theme.of; these keep the UIID defaults consistent).
        bg(p, "FlutterElevatedButton", primary);
        fg(p, "FlutterElevatedButton", onPrimary);
        fg(p, "FlutterTextButton", primary);
        fg(p, "FlutterOutlinedButton", primary);
        fg(p, "FlutterIconButton", onSurface);

        // Slider. Codename One paints the Material 3 look -- a thin rounded
        // track with the active part in the accent and a round thumb -- only
        // when the theme asks for it; without the constants it falls back to
        // the legacy full-height fill, which is why the sliders demo came up as
        // flat lavender bars with no thumb at all. The painter takes the
        // inactive track from this style's background, the thumb from its
        // foreground, and the active track from the *Full style.
        bg(p, "FlutterSlider", surfaceVariant);
        fg(p, "FlutterSlider", primary);
        bg(p, "FlutterSliderFull", primary);
        p.put("FlutterSlider.dis#bgColor", onSurfaceFaded);
        p.put("FlutterSlider.dis#fgColor", onSurfaceFaded);
        p.put("FlutterSliderFull.dis#bgColor", onSurfaceFaded);

        // App bar; the Material 3 ThemeData default background is surface
        // (with an elevation tint), title/icons onSurface.
        bg(p, "FlutterAppBar", surface);
        fg(p, "FlutterAppBar", onSurface);

        // Switch: the CN1 Switch paints the thumb from the fgColor and the
        // track from the bgColor, picking the selected style when ON and the
        // unselected style when OFF (Switch.java:410/575 vs 431/634). Mirror
        // Material 3: ON => primary track + onPrimary (white) thumb;
        // OFF => a muted container track + outline (grey) thumb.
        String onTrack = primary;
        String onThumb = onPrimary;
        String offTrack = hex(new Color(0xFFE7E0EC));
        String offThumb = hex(cs.outline());
        p.put("FlutterSwitch.sel#bgColor", onTrack);
        p.put("FlutterSwitch.sel#fgColor", onThumb);
        p.put("FlutterSwitch.sel#transparency", "255");
        p.put("FlutterSwitch.press#bgColor", onTrack);
        p.put("FlutterSwitch.press#fgColor", onThumb);
        p.put("FlutterSwitch.press#transparency", "255");
        p.put("FlutterSwitch.bgColor", offTrack);
        p.put("FlutterSwitch.fgColor", offThumb);
        p.put("FlutterSwitch.transparency", "255");

        return p;
    }

    /**
     * Installs the theme's prop table as a UIManager overlay. Safe headless
     * (logs and returns).
     */
    public static void install(ThemeData t) {
        try {
            java.util.Hashtable<String, Object> h =
                    new java.util.Hashtable<String, Object>(themeProps(t));
            com.codename1.ui.plaf.UIManager.getInstance().addThemeProps(h);
        } catch (Throwable err) {
            log("could not install ThemeData overlay: " + err);
        }
    }

    /**
     * Styles the Flutter-owned Form per-instance: the form and content pane
     * backgrounds become colorScheme.surface. Instance styling (not theme
     * constants) keeps non-Flutter UIIDs untouched globally.
     */
    public static void applyToForm(Form f, ThemeData t) {
        if (f == null) {
            return;
        }
        try {
            int surface = t.colorScheme().surface().rgb();
            paintSolid(f.getAllStyles(), surface);
            paintSolid(f.getContentPane().getAllStyles(), surface);
        } catch (Throwable err) {
            log("could not style the Form from ThemeData: " + err);
        }
    }

    /**
     * Solid-color background: BACKGROUND_NONE drops any theme background
     * image/gradient that would otherwise paint OVER the bgColor.
     */
    public static void paintSolid(Style s, int rgb) {
        s.setBackgroundType(Style.BACKGROUND_NONE);
        s.setBgColor(rgb);
        s.setBgTransparency(255);
    }

    /**
     * Paints a Flutter {@link Color}, <b>honoring its alpha</b>.
     *
     * <p>{@code Colors.transparent} is {@code 0x00000000} — alpha 0 over black —
     * and the gallery uses it for app bars and scaffolds that should show what
     * is behind them. Painting only the RGB word turns every one of those into
     * an opaque black band, so the alpha has to carry through: fully
     * transparent means paint nothing at all.</p>
     */
    public static void paintColor(Style s, Color c) {
        if (c == null) {
            return;
        }
        int alpha = c.alpha();
        if (alpha <= 0) {
            s.setBackgroundType(Style.BACKGROUND_NONE);
            s.setBgTransparency(0);
            return;
        }
        s.setBackgroundType(Style.BACKGROUND_NONE);
        s.setBgColor(c.rgb());
        s.setBgTransparency(alpha);
    }

    /**
     * CN1 theme hex string for a color's 24-bit RGB portion.
     */
    public static String hex(Color c) {
        String s = Integer.toHexString(c.rgb());
        while (s.length() < 6) {
            s = "0" + s;
        }
        return s;
    }

    private static void bg(Map<String, Object> p, String uiid, String color) {
        for (String state : STATES) {
            p.put(uiid + "." + state + "bgColor", color);
            p.put(uiid + "." + state + "transparency", "255");
        }
    }

    private static void fg(Map<String, Object> p, String uiid, String color) {
        for (String state : STATES) {
            p.put(uiid + "." + state + "fgColor", color);
        }
    }

    private static final String[] STATES = {"", "sel#", "press#", "dis#"};

    private static void log(String msg) {
        try {
            com.codename1.io.Log.p("Flutter runtime: " + msg);
        } catch (Throwable t) {
            // headless: Log has no storage backend
        }
    }
}
