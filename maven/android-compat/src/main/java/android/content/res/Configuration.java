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
package android.content.res;

import java.util.Locale;

/// The device configuration as an application sees it.
public final class Configuration {

    public static final int ORIENTATION_UNDEFINED = 0;
    public static final int ORIENTATION_PORTRAIT = 1;
    public static final int ORIENTATION_LANDSCAPE = 2;

    public static final int UI_MODE_TYPE_MASK = 0x0f;
    public static final int UI_MODE_TYPE_UNDEFINED = 0x00;
    public static final int UI_MODE_TYPE_NORMAL = 0x01;
    public static final int UI_MODE_NIGHT_MASK = 0x30;
    public static final int UI_MODE_NIGHT_UNDEFINED = 0x00;
    public static final int UI_MODE_NIGHT_NO = 0x10;
    public static final int UI_MODE_NIGHT_YES = 0x20;

    public static final int SCREENLAYOUT_SIZE_MASK = 0x0f;
    public static final int SCREENLAYOUT_SIZE_SMALL = 0x01;
    public static final int SCREENLAYOUT_SIZE_NORMAL = 0x02;
    public static final int SCREENLAYOUT_SIZE_LARGE = 0x03;
    public static final int SCREENLAYOUT_SIZE_XLARGE = 0x04;
    public static final int SCREENLAYOUT_LAYOUTDIR_MASK = 0xC0;
    public static final int SCREENLAYOUT_LAYOUTDIR_LTR = 0x40;
    public static final int SCREENLAYOUT_LAYOUTDIR_RTL = 0x80;

    public static final int KEYBOARD_NOKEYS = 1;
    public static final int KEYBOARD_QWERTY = 2;
    public static final int KEYBOARDHIDDEN_NO = 1;
    public static final int KEYBOARDHIDDEN_YES = 2;
    public static final int NAVIGATION_NONAV = 1;
    public static final int TOUCHSCREEN_FINGER = 3;

    public static final int DENSITY_DPI_UNDEFINED = 0;

    public float fontScale = 1f;
    public Locale locale;
    public int orientation;
    public int uiMode;
    public int screenWidthDp;
    public int screenHeightDp;
    public int smallestScreenWidthDp;
    public int densityDpi;
    public int screenLayout;
    public int keyboard = KEYBOARD_NOKEYS;
    public int keyboardHidden = KEYBOARDHIDDEN_YES;
    public int hardKeyboardHidden = KEYBOARDHIDDEN_YES;
    public int navigation = NAVIGATION_NONAV;
    public int touchscreen = TOUCHSCREEN_FINGER;
    public int mcc;
    public int mnc;

    public Configuration() {
    }

    public Configuration(Configuration o) {
        setTo(o);
    }

    public void setTo(Configuration o) {
        fontScale = o.fontScale;
        locale = o.locale;
        orientation = o.orientation;
        uiMode = o.uiMode;
        screenWidthDp = o.screenWidthDp;
        screenHeightDp = o.screenHeightDp;
        smallestScreenWidthDp = o.smallestScreenWidthDp;
        densityDpi = o.densityDpi;
        screenLayout = o.screenLayout;
        keyboard = o.keyboard;
        keyboardHidden = o.keyboardHidden;
        hardKeyboardHidden = o.hardKeyboardHidden;
        navigation = o.navigation;
        touchscreen = o.touchscreen;
    }

    public int getLayoutDirection() {
        return (screenLayout & SCREENLAYOUT_LAYOUTDIR_MASK) == SCREENLAYOUT_LAYOUTDIR_RTL ? 1 : 0;
    }

    public void setLocale(Locale l) {
        locale = l;
    }

    public boolean isNightModeActive() {
        return (uiMode & UI_MODE_NIGHT_MASK) == UI_MODE_NIGHT_YES;
    }

    public boolean isLayoutSizeAtLeast(int size) {
        return (screenLayout & SCREENLAYOUT_SIZE_MASK) >= size;
    }

    /// Bit mask of what differs, using `ActivityInfo.CONFIG_*` values.
    public int diff(Configuration o) {
        int d = 0;
        if (o.orientation != orientation) {
            d |= 0x0080;
        }
        if (o.uiMode != uiMode) {
            d |= 0x0200;
        }
        if (o.screenWidthDp != screenWidthDp || o.screenHeightDp != screenHeightDp) {
            d |= 0x0400;
        }
        if (o.smallestScreenWidthDp != smallestScreenWidthDp) {
            d |= 0x0800;
        }
        if (o.densityDpi != densityDpi) {
            d |= 0x1000;
        }
        if (o.locale != locale && (o.locale == null || locale == null
                || !o.locale.getLanguage().equals(locale.getLanguage())
                || !o.locale.getCountry().equals(locale.getCountry()))) {
            d |= 0x0004;
        }
        if ((o.screenLayout & SCREENLAYOUT_LAYOUTDIR_MASK) != (screenLayout & SCREENLAYOUT_LAYOUTDIR_MASK)) {
            d |= 0x2000;
        }
        if ((o.screenLayout & ~SCREENLAYOUT_LAYOUTDIR_MASK) != (screenLayout & ~SCREENLAYOUT_LAYOUTDIR_MASK)) {
            d |= 0x0100;
        }
        if (Float.compare(o.fontScale, fontScale) != 0) {
            d |= 0x40000000;
        }
        if (o.keyboardHidden != keyboardHidden || o.hardKeyboardHidden != hardKeyboardHidden) {
            d |= 0x0020;
        }
        return d;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Configuration && diff((Configuration) o) == 0
                && Float.compare(((Configuration) o).fontScale, fontScale) == 0;
    }

    @Override
    public int hashCode() {
        return orientation * 31 + uiMode * 17 + screenWidthDp * 7 + screenHeightDp + densityDpi;
    }

    @Override
    public String toString() {
        return "{" + fontScale + " " + (locale == null ? "?" : locale.getLanguage() + "_" + locale.getCountry())
                + " sw" + smallestScreenWidthDp + "dp w" + screenWidthDp + "dp h" + screenHeightDp + "dp "
                + densityDpi + "dpi " + (orientation == ORIENTATION_LANDSCAPE ? "land" : "port")
                + ((uiMode & UI_MODE_NIGHT_MASK) == UI_MODE_NIGHT_YES ? " night" : "") + "}";
    }
}
