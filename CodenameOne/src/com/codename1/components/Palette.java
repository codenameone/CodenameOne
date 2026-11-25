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
package com.codename1.components;

import com.codename1.ui.Component;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.plaf.UIManager;

/// Colours for the components in this package that paint themselves rather
/// than delegate to a theme border: a range slider's fill, a progress ring, a
/// waveform's played bars. Each is read from the component's own UIID when the
/// theme defines it, and otherwise from a UIID every native theme styles, so an
/// unstyled component follows the app's accent colour (including an
/// `@accent-color` override) and switches with dark mode.
final class Palette {
    private Palette() {
    }

    /// The accent colour: the component's UIID foreground when themed, otherwise
    /// the theme's `SliderFull` background.
    static int accent(Component c, String uiid) {
        UIManager m = c.getUIManager();
        if (uiid != null && m.isThemeStyleDefined(uiid)) {
            return m.getComponentStyle(uiid).getFgColor();
        }
        Style full = m.getComponentStyle("SliderFull");
        return m.isThemeStyleDefined("SliderFull") ? full.getBgColor() : 0x6750a4;
    }

    /// The track colour behind a fill: the UIID's background when themed,
    /// otherwise the theme's `Slider` background, otherwise a faded text colour.
    static int track(Component c, String uiid) {
        UIManager m = c.getUIManager();
        if (uiid != null && m.isThemeStyleDefined(uiid)) {
            return m.getComponentStyle(uiid).getBgColor();
        }
        if (m.isThemeStyleDefined("Slider")) {
            Style s = m.getComponentStyle("Slider");
            if ((s.getBgTransparency() & 0xff) != 0) {
                return s.getBgColor();
            }
        }
        return blend(text(c), background(c), 0.2f);
    }

    /// The text colour of the theme's `Label`.
    static int text(Component c) {
        return c.getUIManager().getComponentStyle("Label").getFgColor();
    }

    /// The form background, used to tell light from dark when mixing colours.
    static int background(Component c) {
        UIManager m = c.getUIManager();
        Style s = m.getComponentStyle("Form");
        if ((s.getBgTransparency() & 0xff) != 0) {
            return s.getBgColor();
        }
        return isDark(text(c)) ? 0xffffff : 0x000000;
    }

    /// Mixes `a` into `b`: 0 answers `b`, 1 answers `a`.
    static int blend(int a, int b, float amount) {
        int r = mix((a >> 16) & 0xff, (b >> 16) & 0xff, amount);
        int g = mix((a >> 8) & 0xff, (b >> 8) & 0xff, amount);
        int bl = mix(a & 0xff, b & 0xff, amount);
        return (r << 16) | (g << 8) | bl;
    }

    private static int mix(int a, int b, float amount) {
        return Math.max(0, Math.min(255, Math.round(b + (a - b) * amount)));
    }

    /// True for a colour dark enough that white text reads better on it.
    static boolean isDark(int rgb) {
        int r = (rgb >> 16) & 0xff;
        int g = (rgb >> 8) & 0xff;
        int b = rgb & 0xff;
        return r * 299 + g * 587 + b * 114 < 128000;
    }
}
