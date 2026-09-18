/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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

import com.codename1.ui.Button;
import com.codename1.ui.util.Resources;
import com.codename1.junit.UITestBase;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Holds the three desktop native themes -- Windows Fluent, macOS Aqua and GNOME
 * Adwaita -- to the same surface as each other.
 *
 * <p>They are three separate hand-written CSS files by deliberate choice: a shared
 * base would make "what you read is what compiles" false, which is what makes the
 * strictNoCef failure messages actionable. The cost of that choice is drift. A UIID
 * added to Fluent and forgotten in Aqua does not fail anything -- the missing UIID
 * falls back to the blank default style, which is white-on-black and looks like a
 * theme bug reported months later by a user on the one platform nobody tested.
 *
 * <p>So the drift is closed by a test instead of by structure. This asserts the
 * three define the same UIIDs, the same {@code #Constants} keys, and a {@code }
 * counterpart for every UIID that needs one. It is not a fidelity measurement and
 * needs no golden captures, so unlike the fidelity gate it protects the themes from
 * the moment they land.
 *
 * <p>Loads each {@code .res} straight from the repo's {@code Themes/} build output
 * and silently skips when absent, the same convention as
 * {@link NativeThemeBindingsTest} -- the test only fires when a freshly-built native
 * theme is on disk.
 */
public class DesktopNativeThemeParityTest extends UITestBase {

    private static final String[] DESKTOP_THEMES = {
        "WindowsFluentTheme.res",
        "MacOSAquaTheme.res",
        "GnomeAdwaitaTheme.res",
    };

    /// Accent colour each desktop theme declares, as its platform's default:
    /// Windows SystemAccentColor, NSColor.controlAccentColor, Adwaita blue.
    private static final String[][] ACCENTS = {
        {"WindowsFluentTheme.res", "0078d4"},
        {"MacOSAquaTheme.res", "007aff"},
        {"GnomeAdwaitaTheme.res", "3584e4"},
    };

    @Test
    public void desktopThemesDefineTheSameUiids() throws Exception {
        Map<String, TreeSet<String>> byTheme = new LinkedHashMap<String, TreeSet<String>>();
        for (String name : DESKTOP_THEMES) {
            Hashtable theme = loadTheme(name);
            if (theme == null) {
                return;   // not built; skip like the sibling native-theme tests
            }
            byTheme.put(name, uiids(theme, false));
        }
        assertSameSets(byTheme, "UIID");
    }

    @Test
    public void desktopThemesDefineTheSameConstants() throws Exception {
        Map<String, TreeSet<String>> byTheme = new LinkedHashMap<String, TreeSet<String>>();
        for (String name : DESKTOP_THEMES) {
            Hashtable theme = loadTheme(name);
            if (theme == null) {
                return;
            }
            TreeSet<String> constants = new TreeSet<String>();
            for (Object k : theme.keySet()) {
                String key = k.toString();
                // Theme constants are the "@name" keys. The "@cn1-bind:" family is a
                // per-UIID binding record rather than a constant, and is covered by
                // the UIID comparison instead.
                if (key.startsWith("@") && !key.startsWith("@cn1-bind:")) {
                    constants.add(key);
                }
            }
            byTheme.put(name, constants);
        }
        assertSameSets(byTheme, "theme constant");
    }

    @Test
    public void everyDesktopUiidHasADarkCounterpart() throws Exception {
        for (String name : DESKTOP_THEMES) {
            Hashtable theme = loadTheme(name);
            if (theme == null) {
                return;
            }
            TreeSet<String> light = uiids(theme, false);
            TreeSet<String> dark = uiids(theme, true);
            List<String> missing = new ArrayList<String>();
            for (String uiid : light) {
                if (dark.contains(uiid) || !paintsAColour(theme, uiid)) {
                    continue;
                }
                missing.add(uiid);
            }
            if (!missing.isEmpty()) {
                fail(name + " defines " + missing.size() + " UIID(s) that paint a colour in light"
                        + " mode and have no $Dark counterpart, so they keep their LIGHT colours on"
                        + " a dark form: " + missing
                        + ".\n  The usual cause is cn1-derive. It is flattened against the light"
                        + " parent at compile time, so a UIID that derives only in the light block"
                        + " gets a concrete copy of the light colours and no $Dark entry at all --"
                        + " repeat the derive inside the dark block.");
            }
        }
    }

    @Test
    public void desktopAccentColorRetunesTheButton() throws Exception {
        for (String[] row : ACCENTS) {
            Hashtable theme = loadTheme(row[0]);
            if (theme == null) {
                return;
            }
            assertEquals(row[1].toUpperCase(), theme.get("@accent-color"),
                    row[0] + " must export its platform's default accent as a theme constant");

            UIManager.getInstance().setThemeProps(theme);
            Hashtable override = new Hashtable();
            override.put("@accent-color", "ff2d95");
            UIManager.getInstance().addThemeProps(override);

            // Whichever UIID the theme binds to --accent-color must follow the
            // override. Without this the binding pass is inert and an application
            // that retunes the accent silently gets the theme default.
            boolean anyBound = false;
            for (Object k : new TreeSet<Object>(theme.keySet())) {
                String key = k.toString();
                if (!key.startsWith("@cn1-bind:") || !key.endsWith(".bgColor")) {
                    continue;
                }
                if (!"accent-color".equals(theme.get(key))) {
                    continue;
                }
                String uiid = key.substring("@cn1-bind:".length(), key.length() - ".bgColor".length());
                Button b = new Button("x");
                b.setUIID(uiid);
                assertEquals(0xff2d95, b.getUnselectedStyle().getBgColor(),
                        row[0] + ": " + uiid + ".bgColor is bound to --accent-color but did not retune");
                anyBound = true;
            }
            assertTrue(anyBound, row[0] + " binds no UIID background to --accent-color, so nothing"
                    + " follows the OS accent colour");
        }
    }

    /// Whether a UIID actually paints something whose colour is appearance-specific.
    ///
    /// A fully transparent UIID -- a scroll TRACK, a spacer -- has nothing to recolour
    /// for dark mode and legitimately has no $Dark counterpart, so requiring one would
    /// be bookkeeping rather than a defect. A UIID with a foreground colour, or an
    /// opaque background, renders wrong on the other appearance and must have one.
    private static boolean paintsAColour(Hashtable theme, String uiid) {
        if (theme.get(uiid + ".fgColor") != null) {
            return true;
        }
        if (theme.get(uiid + ".bgColor") == null) {
            return false;
        }
        Object transparency = theme.get(uiid + ".transparency");
        if (transparency == null) {
            return true;
        }
        try {
            return Integer.parseInt(transparency.toString().trim()) > 0;
        } catch (NumberFormatException ex) {
            return true;
        }
    }

    private void assertSameSets(Map<String, TreeSet<String>> byTheme, String what) {
        TreeSet<String> union = new TreeSet<String>();
        for (TreeSet<String> s : byTheme.values()) {
            union.addAll(s);
        }
        List<String> problems = new ArrayList<String>();
        for (String entry : union) {
            List<String> absent = new ArrayList<String>();
            for (Map.Entry<String, TreeSet<String>> e : byTheme.entrySet()) {
                if (!e.getValue().contains(entry)) {
                    absent.add(e.getKey());
                }
            }
            if (!absent.isEmpty()) {
                problems.add(entry + " missing from " + absent);
            }
        }
        if (!problems.isEmpty()) {
            fail("The desktop native themes have drifted apart -- " + problems.size() + " "
                    + what + "(s) are not defined by all three. A UIID one theme lacks falls back"
                    + " to the blank default style (white on black), which reads as a theme bug on"
                    + " exactly one platform:\n  " + String.join("\n  ", problems));
        }
    }

    /// The UIID set a theme defines, taken from the plain "<UIID>.<property>" keys.
    /// State-prefixed keys ("sel#", "press#", "dis#", "hover#") name the same UIIDs
    /// and are skipped so a theme that styles one extra state does not read as an
    /// extra UIID.
    private static TreeSet<String> uiids(Hashtable theme, boolean dark) {
        TreeSet<String> out = new TreeSet<String>();
        for (Object k : theme.keySet()) {
            String key = k.toString();
            boolean isDark = key.startsWith("$Dark");
            if (isDark != dark) {
                continue;
            }
            if (isDark) {
                key = key.substring("$Dark".length());
            }
            if (key.startsWith("@") || key.indexOf('#') >= 0) {
                continue;
            }
            int dot = key.lastIndexOf('.');
            if (dot > 0) {
                out.add(key.substring(0, dot));
            }
        }
        return out;
    }

    private static Hashtable loadTheme(String fileName) throws Exception {
        File themeFile = locateNativeTheme(fileName);
        if (themeFile == null) {
            return null;
        }
        Resources res;
        InputStream stream = new FileInputStream(themeFile);
        try {
            res = Resources.open(stream);
        } finally {
            stream.close();
        }
        String[] names = res.getThemeResourceNames();
        assertNotNull(names, fileName + " has no theme resource");
        return res.getTheme(names[0]);
    }

    private static File locateNativeTheme(String fileName) {
        File cwd = new File(".").getAbsoluteFile();
        for (int i = 0; i < 6 && cwd != null; i++) {
            File candidate = new File(cwd, "Themes/" + fileName);
            if (candidate.isFile()) {
                return candidate;
            }
            cwd = cwd.getParentFile();
        }
        return null;
    }
}
