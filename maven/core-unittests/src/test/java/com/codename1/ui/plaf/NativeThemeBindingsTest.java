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
import com.codename1.ui.Button;
import com.codename1.ui.util.Resources;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Hashtable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end check: load the shipped iOS Modern native theme `.res`,
 * verify the var() bindings the CSS compiler emitted survived the
 * round-trip, and that pushing an `@accent-color` override through
 * {@link UIManager#addThemeProps(Hashtable)} retunes the accent-bearing
 * Button and RaisedButton properties without touching them directly.
 *
 * Loads the `.res` straight from the repo's `Themes/` build output
 * (next to where `scripts/build-native-themes.sh` writes it). When
 * that file is absent the test is silently skipped rather than failed
 * - the runtime-side binding logic is already covered by
 * {@link UIManagerThemeBindingsTest}, so this test only fires when a
 * fresh native theme is sitting on disk.
 */
public class NativeThemeBindingsTest extends UITestBase {

    @Test
    public void iosModernThemeBindingRetunesRaisedButton() throws Exception {
        File themeFile = locateNativeTheme("iOSModernTheme.res");
        if (themeFile == null) {
            return;
        }
        Resources res;
        InputStream stream = new FileInputStream(themeFile);
        try {
            res = Resources.open(stream);
        } finally {
            stream.close();
        }
        String[] themeNames = res.getThemeResourceNames();
        assertNotNull(themeNames);
        Hashtable theme = res.getTheme(themeNames[0]);
        assertNotNull(theme);

        // iOS 26 buttons are Liquid Glass capsules. Only the RaisedButton FILL is
        // accent-bound (it's the opaque prominent pill, retinted like iOS applies
        // tintColor); the regular Button LABEL is a fixed neutral (black on the
        // light glass), NOT accent-bound. The compiler must emit both the baked-in
        // default AND the @cn1-bind entry for the accent fill. Resources.loadTheme
        // stores colours as unpadded hex (Integer.toHexString), so 007aff -> "7aff".
        assertEquals("7aff", theme.get("RaisedButton.bgColor"));
        assertEquals("accent-color", theme.get("@cn1-bind:RaisedButton.bgColor"));
        assertEquals("accent-pressed-color", theme.get("@cn1-bind:RaisedButton.press#bgColor"));
        assertEquals("accent-color-dark", theme.get("@cn1-bind:$DarkRaisedButton.bgColor"));
        // `#Constants { --accent-color: #007aff; }` is exported as a `@accent-color`
        // theme constant so a user app can override it via the same syntax.
        assertEquals("007AFF", theme.get("@accent-color"));
        assertEquals("DialogButton", theme.get("@dlgButtonCommandUIID"));
        assertEquals("DialogCenteredTitle", theme.get("@dlgCenteredTitleUIID"));
        assertEquals("c6c6c8", theme.get("@dlgInvisibleButtons"));
        assertEquals("38383a", theme.get("@dlgInvisibleButtonsDark"));
        assertEquals("true", theme.get("@hideToolbarCommandTextWithIconBool"));
        assertEquals("60", theme.get("@spinnerPerspectiveDarkFadePctInt"));
        assertEquals("0.5", theme.get("@progressTrackThicknessMM"));
        RoundBorder raisedBorder = assertInstanceOf(RoundBorder.class,
                theme.get("RaisedButton.border"));
        assertTrue(raisedBorder.isStrokeGradient(),
                "RaisedButton must retain its Liquid Glass gradient rim");
        RoundBorder flatBorder = assertInstanceOf(RoundBorder.class,
                theme.get("FlatButton.border"));
        assertTrue(flatBorder.isStrokeGradient(),
                "FlatButton must retain its Liquid Glass gradient rim");
        assertFalse(theme.containsKey("@dialogTitleCenterBool"),
                "The native iOS theme must not opt existing dialogs into the centered layout");

        UIManager.getInstance().setThemeProps(theme);

        Button defaultBtn = new Button("default");
        defaultBtn.setUIID("RaisedButton");
        assertEquals(0x007aff, defaultBtn.getUnselectedStyle().getBgColor(),
                "RaisedButton picks up the inlined accent fallback when no override is supplied");

        Hashtable override = new Hashtable();
        override.put("@accent-color", "ff2d95");
        override.put("@accent-disabled-color", "00b894");
        UIManager.getInstance().addThemeProps(override);

        Button retuned = new Button("magenta");
        retuned.setUIID("RaisedButton");
        assertEquals(0xff2d95, retuned.getUnselectedStyle().getBgColor(),
                "@accent-color override retunes the RaisedButton fill");

        Button textButton = new Button("text");
        Button disabledButton = new Button("disabled");
        disabledButton.setUIID("RaisedButton");
        disabledButton.setEnabled(false);
        // The regular Button label is a fixed black (unaffected by the accent
        // override); the disabled RaisedButton is a fixed desaturated taupe pill.
        assertEquals(0x000000, textButton.getUnselectedStyle().getFgColor(),
                "Button label is a fixed neutral, not accent-bound");
        assertEquals(0xb3a8a0, disabledButton.getDisabledStyle().getBgColor(),
                "Disabled RaisedButton is a fixed desaturated pill, not accent-bound");
    }

    @Test
    public void androidMaterialThemeBindingRetunesButton() throws Exception {
        File themeFile = locateNativeTheme("AndroidMaterialTheme.res");
        if (themeFile == null) {
            return;
        }
        Resources res;
        InputStream stream = new FileInputStream(themeFile);
        try {
            res = Resources.open(stream);
        } finally {
            stream.close();
        }
        String[] themeNames = res.getThemeResourceNames();
        assertNotNull(themeNames);
        Hashtable theme = res.getTheme(themeNames[0]);
        assertNotNull(theme);

        // M3 baseline primary is #6750a4 → "6750a4" when stored via
        // Integer.toHexString.
        assertEquals("6750a4", theme.get("Button.bgColor"));
        assertEquals("accent-color", theme.get("@cn1-bind:Button.bgColor"));
        // Native theme.css declares `#Constants { --accent-color: #6750a4; }`
        // and the Flute compiler now exports that as a `@accent-color`
        // theme constant in addition to the parser-internal var() lookup.
        // This is what lets a user app's theme.css redeclare
        // `#Constants { --accent-color: #ff2d95; }` and have it propagate
        // through the runtime binding pass to every UIID bound to
        // --accent-color in this parent theme.
        assertEquals("6750A4", theme.get("@accent-color"));
        assertEquals("DialogButton", theme.get("@dlgButtonCommandUIID"));
        assertEquals("cac4d0", theme.get("@dlgInvisibleButtons"));

        UIManager.getInstance().setThemeProps(theme);

        Button dialogButton = new Button("dialog command");
        dialogButton.setUIID("DialogButton");
        assertEquals(0, dialogButton.getStyle().getHorizontalMargins(),
                "Android dialog command cells must meet the card edges and each other");

        Button defaultBtn = new Button("default");
        defaultBtn.setUIID("Button");
        assertEquals(0x6750a4, defaultBtn.getUnselectedStyle().getBgColor(),
                "Android M3 button bg should pick up the inlined fallback when no override is supplied");

        Hashtable override = new Hashtable();
        override.put("@accent-color", "ff2d95");
        UIManager.getInstance().addThemeProps(override);

        Button retuned = new Button("magenta");
        retuned.setUIID("Button");
        assertEquals(0xff2d95, retuned.getUnselectedStyle().getBgColor(),
                "@accent-color override should retune Button.bgColor on the Android Material 3 theme");
    }

    /// Searches a few well-known relative locations for a freshly-built
    /// native theme `.res`. We surfboard up a few directory levels
    /// because the surefire `user.dir` lands inside the unittests
    /// module while the build output lives at the repo root's
    /// `Themes/`.
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
