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
package com.google.android.material.color;

import android.content.Context;
import android.content.res.ColorStateList;
import android.util.TypedValue;
import android.view.View;

/// Resolves the theme's color roles and blends colors the way Material
/// Components' widgets do.
public final class MaterialColors {

    public static final float ALPHA_FULL = 1.00f;
    public static final float ALPHA_MEDIUM = 0.54f;
    public static final float ALPHA_DISABLED = 0.38f;
    public static final float ALPHA_LOW = 0.32f;
    public static final float ALPHA_DISABLED_LOW = 0.12f;

    private MaterialColors() {
    }

    /// The color the theme of `context` gives `attr`, or `defaultValue`.
    public static int getColor(Context context, int attr, int defaultValue) {
        TypedValue tv = new TypedValue();
        if (!context.getTheme().resolveAttribute(attr, tv, true)) {
            return defaultValue;
        }
        if (tv.type >= TypedValue.TYPE_FIRST_COLOR_INT && tv.type <= TypedValue.TYPE_LAST_COLOR_INT) {
            return tv.data;
        }
        if (tv.resourceId != 0) {
            ColorStateList csl = context.getResources().getColorStateList(tv.resourceId, context.getTheme());
            if (csl != null) {
                return csl.getDefaultColor();
            }
        }
        return defaultValue;
    }

    /// The color the theme gives `attr`; throws when the theme defines
    /// none, as Material Components does.
    public static int getColor(View view, int attr) {
        TypedValue tv = new TypedValue();
        if (!view.getContext().getTheme().resolveAttribute(attr, tv, true)) {
            throw new IllegalArgumentException(view.getClass().getName()
                    + " requires a value for the attribute " + Integer.toHexString(attr)
                    + "; make sure the application theme is a Material Components theme");
        }
        return getColor(view.getContext(), attr, 0);
    }

    public static int getColor(View view, int attr, int defaultValue) {
        return getColor(view.getContext(), attr, defaultValue);
    }

    /// The color state list the theme gives `attr`, or null.
    public static ColorStateList getColorStateListOrNull(Context context, int attr) {
        TypedValue tv = new TypedValue();
        if (!context.getTheme().resolveAttribute(attr, tv, true)) {
            return null;
        }
        if (tv.type >= TypedValue.TYPE_FIRST_COLOR_INT && tv.type <= TypedValue.TYPE_LAST_COLOR_INT) {
            return ColorStateList.valueOf(tv.data);
        }
        if (tv.resourceId != 0) {
            return context.getResources().getColorStateList(tv.resourceId, context.getTheme());
        }
        return null;
    }

    /// `color` with its alpha multiplied by `alpha` (0 to 1).
    public static int compositeARGBWithAlpha(int color, int alpha) {
        int a = ((color >>> 24) * alpha) / 255;
        return (color & 0x00ffffff) | (a << 24);
    }

    /// `overlay` at `overlayAlpha` over `background`.
    public static int layer(int background, int overlay, float overlayAlpha) {
        int a = Math.round(((overlay >>> 24) & 0xff) * overlayAlpha);
        return layer(background, (overlay & 0x00ffffff) | (a << 24));
    }

    /// `overlay` composited over `background` (source-over).
    public static int layer(int background, int overlay) {
        float fa = ((overlay >>> 24) & 0xff) / 255f;
        float ba = ((background >>> 24) & 0xff) / 255f;
        float oa = fa + ba * (1f - fa);
        if (oa <= 0f) {
            return 0;
        }
        int r = blend((overlay >> 16) & 0xff, fa, (background >> 16) & 0xff, ba, oa);
        int g = blend((overlay >> 8) & 0xff, fa, (background >> 8) & 0xff, ba, oa);
        int b = blend(overlay & 0xff, fa, background & 0xff, ba, oa);
        return (Math.round(oa * 255f) << 24) | (r << 16) | (g << 8) | b;
    }

    private static int blend(int fc, float fa, int bc, float ba, float oa) {
        return Math.max(0, Math.min(255, Math.round((fc * fa + bc * ba * (1f - fa)) / oa)));
    }

    /// Whether `color` reads as light (relative luminance above one half).
    public static boolean isColorLight(int color) {
        int r = (color >> 16) & 0xff;
        int g = (color >> 8) & 0xff;
        int b = color & 0xff;
        return (0.2126 * r + 0.7152 * g + 0.0722 * b) / 255.0 > 0.5;
    }

    /// `color` with its alpha replaced by `alpha` (0 to 1) of full.
    public static int withAlpha(int color, float alpha) {
        return (color & 0x00ffffff) | (Math.round(255 * Math.max(0f, Math.min(1f, alpha))) << 24);
    }

    /// A color state list showing `color` normally and the disabled
    /// treatment Material uses (`onSurface` at `disabledAlpha`) when
    /// disabled. A list that already has states is returned as it is.
    public static ColorStateList withDisabled(ColorStateList color, int onSurface, float disabledAlpha) {
        if (color == null || color.isStateful()) {
            return color;
        }
        return new ColorStateList(new int[][] {{-android.R.attr.state_enabled}, {}},
                new int[] {withAlpha(onSurface, disabledAlpha * ((onSurface >>> 24) / 255f)),
                    color.getDefaultColor()});
    }
}
