/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.parley.ui;

import com.codename1.ui.CN;
import com.codename1.ui.plaf.UIManager;
import com.codenameone.examples.parley.Prefs;

/// Light or dark.
///
/// `theme.css` has both: every rule has its dark twin in the block at the end
/// of the file. Which of the two a component gets is decided when its style is
/// resolved, so a screen built after [#apply()] is in the new look and one
/// already built is not -- the caller rebuilds what is on screen.
public final class Look {
    private Look() {
    }

    /// Makes the look the one chosen in Settings: light, dark, or whichever
    /// the device is in.
    public static void apply() {
        String theme = Prefs.theme();
        CN.setDarkMode(Prefs.THEME_DARK.equals(theme) ? Boolean.TRUE
                : Prefs.THEME_LIGHT.equals(theme) ? Boolean.FALSE : null);
        if (CN.isDesktop()) {
            // On a desktop the platform's theme takes the colours and the font
            // the user chose for the whole machine -- the accent among them.
            // It reaches only what that theme styles, which is what the admin
            // console wears; see Layouts.
            UIManager.getInstance().setUseNativeColors(true);
            UIManager.getInstance().setUseNativeFonts(true);
        }
        UIManager.getInstance().refreshTheme();
    }

    /// Whether the app is drawn dark right now. A platform that cannot say is
    /// taken to be light.
    public static boolean dark() {
        Boolean dark = CN.isDarkMode();
        return dark != null && dark.booleanValue();
    }
}
