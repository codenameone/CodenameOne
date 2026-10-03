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
package com.google.android.material.internal;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.view.View;

import com.google.android.material.R;
import com.google.android.material.color.MaterialColors;

/// What every Material widget needs from its theme: its default style,
/// whether the theme is Material 3, dimensions in pixels and the disabled
/// treatment of colors.
public final class MaterialAttrs {

    private MaterialAttrs() {
    }

    /// `attr` when the theme defines it, else 0: a widget then takes its
    /// style from `defStyleRes` alone (Material Components refuses a
    /// non-Material theme outright; the runtime styles the widget instead).
    public static int defStyleAttr(Context context, int attr) {
        TypedValue tv = new TypedValue();
        return context.getTheme().resolveAttribute(attr, tv, true) ? attr : 0;
    }

    public static boolean isMaterial3(Context context) {
        TypedValue tv = new TypedValue();
        return context.getTheme().resolveAttribute(R.attr.isMaterial3Theme, tv, true) && tv.data != 0;
    }

    public static float dp(Context context, float dp) {
        return dp * context.getResources().getDisplayMetrics().density;
    }

    public static int dpi(Context context, float dp) {
        return Math.round(dp(context, dp));
    }

    public static int themeColor(Context context, int attr, int defaultValue) {
        return MaterialColors.getColor(context, attr, defaultValue);
    }

    public static int onSurface(Context context) {
        return MaterialColors.getColor(context, R.attr.colorOnSurface, 0xff1d1b20);
    }

    /// The list at `index`, or the theme's color for `fallbackAttr`.
    public static ColorStateList colorStateList(Context context, TypedArray a, int index, int fallbackAttr) {
        if (a.hasValue(index)) {
            ColorStateList csl = a.getColorStateList(index);
            if (csl != null) {
                return csl;
            }
        }
        if (fallbackAttr == 0) {
            return null;
        }
        return MaterialColors.getColorStateListOrNull(context, fallbackAttr);
    }

    /// A copy of `d` tinted with `tint`, or `d` when either is null.
    public static Drawable tinted(Drawable d, ColorStateList tint) {
        if (d == null || tint == null) {
            return d;
        }
        Drawable m = d.mutate();
        m.setTintList(tint);
        return m;
    }

    /// Whether the views's state set holds `state`.
    public static boolean hasState(View v, int state) {
        for (int s : v.getDrawableState()) {
            if (s == state) {
                return true;
            }
        }
        return false;
    }
}
