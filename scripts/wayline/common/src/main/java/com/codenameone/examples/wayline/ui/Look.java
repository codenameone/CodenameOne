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
package com.codenameone.examples.wayline.ui;

import com.codename1.ui.CN;
import com.codename1.ui.plaf.UIManager;
import com.codenameone.examples.wayline.Prefs;

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
