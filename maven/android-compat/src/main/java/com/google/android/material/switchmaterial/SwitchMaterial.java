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
package com.google.android.material.switchmaterial;

import android.content.Context;
import android.content.res.ColorStateList;
import android.util.AttributeSet;

import androidx.appcompat.widget.SwitchCompat;

import com.google.android.material.R;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.internal.MaterialAttrs;

/// The MaterialComponents switch: AppCompat's switch in the theme's
/// secondary color.
public class SwitchMaterial extends SwitchCompat {

    private boolean mUseMaterialThemeColors;

    public SwitchMaterial(Context context) {
        this(context, null);
    }

    public SwitchMaterial(Context context, AttributeSet attrs) {
        this(context, attrs, androidx.appcompat.R.attr.switchStyle);
    }

    public SwitchMaterial(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setUseMaterialThemeColors(true);
    }

    public void setUseMaterialThemeColors(boolean useMaterialThemeColors) {
        mUseMaterialThemeColors = useMaterialThemeColors;
        if (!useMaterialThemeColors) {
            return;
        }
        Context c = getContext();
        int secondary = MaterialColors.getColor(c, R.attr.colorSecondary, 0xff03dac6);
        int surface = MaterialColors.getColor(c, R.attr.colorSurface, 0xffffffff);
        int onSurface = MaterialAttrs.onSurface(c);
        int[][] states = {{android.R.attr.state_enabled, android.R.attr.state_checked},
            {android.R.attr.state_enabled}, {}};
        setThumbTintList(new ColorStateList(states, new int[] {secondary,
            MaterialColors.layer(surface, onSurface, MaterialColors.ALPHA_DISABLED_LOW), surface}));
        setTrackTintList(new ColorStateList(states, new int[] {MaterialColors.withAlpha(secondary, 0.54f),
            MaterialColors.withAlpha(onSurface, 0.38f), MaterialColors.withAlpha(onSurface, 0.12f)}));
    }

    public boolean isUseMaterialThemeColors() {
        return mUseMaterialThemeColors;
    }
}
