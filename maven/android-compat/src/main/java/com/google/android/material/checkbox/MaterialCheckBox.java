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
package com.google.android.material.checkbox;

import android.content.Context;
import android.content.res.ColorStateList;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatCheckBox;

import com.google.android.material.R;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.internal.MaterialAttrs;

/// A check box in the theme's colors: the primary (Material 3) or secondary
/// (MaterialComponents) color when checked, a muted one when not.
public class MaterialCheckBox extends AppCompatCheckBox {

    private boolean mUseMaterialThemeColors;

    public MaterialCheckBox(Context context) {
        this(context, null);
    }

    public MaterialCheckBox(Context context, AttributeSet attrs) {
        this(context, attrs, androidx.appcompat.R.attr.checkboxStyle);
    }

    public MaterialCheckBox(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setUseMaterialThemeColors(true);
    }

    public void setUseMaterialThemeColors(boolean useMaterialThemeColors) {
        mUseMaterialThemeColors = useMaterialThemeColors;
        if (useMaterialThemeColors) {
            setButtonTintList(materialColors(getContext()));
        }
    }

    public boolean isUseMaterialThemeColors() {
        return mUseMaterialThemeColors;
    }

    static ColorStateList materialColors(Context c) {
        boolean m3 = MaterialAttrs.isMaterial3(c);
        int checked = MaterialColors.getColor(c, m3 ? R.attr.colorPrimary : R.attr.colorSecondary, 0xff6750a4);
        int onSurface = MaterialAttrs.onSurface(c);
        int unchecked = m3 ? MaterialColors.getColor(c, R.attr.colorOnSurfaceVariant, 0xff49454f)
                : MaterialColors.withAlpha(onSurface, 0.54f);
        return new ColorStateList(new int[][] {{-android.R.attr.state_enabled},
            {android.R.attr.state_checked}, {}},
            new int[] {MaterialColors.withAlpha(onSurface, 0.38f), checked, unchecked});
    }
}
