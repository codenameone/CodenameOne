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
package com.google.android.material.appbar;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;

import androidx.appcompat.widget.Toolbar;

import com.google.android.material.R;
import com.google.android.material.internal.MaterialAttrs;

/// The Material top app bar: an AppCompat [Toolbar] styled by the Material
/// theme, which tints its navigation icon with `navigationIconTint`.
public class MaterialToolbar extends Toolbar {

    private ColorStateList mNavigationIconTint;
    private boolean mTitleCentered;
    private boolean mSubtitleCentered;

    public MaterialToolbar(Context context) {
        this(context, null);
    }

    public MaterialToolbar(Context context, AttributeSet attrs) {
        this(context, attrs, androidx.appcompat.R.attr.toolbarStyle);
    }

    public MaterialToolbar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.MaterialToolbar, defStyleAttr, 0);
        try {
            mNavigationIconTint = a.getColorStateList(R.styleable.MaterialToolbar_navigationIconTint);
            mTitleCentered = a.getBoolean(R.styleable.MaterialToolbar_titleCentered, false);
            mSubtitleCentered = a.getBoolean(R.styleable.MaterialToolbar_subtitleCentered, false);
        } finally {
            a.recycle();
        }
        if (mNavigationIconTint != null && getNavigationIcon() != null) {
            setNavigationIcon(getNavigationIcon());
        }
        if (mTitleCentered || mSubtitleCentered) {
            com.codename1.androidcompat.runtime.CompatReport.unsupported("MaterialToolbar",
                    "titleCentered and subtitleCentered");
        }
        if (getMinimumHeight() == 0) {
            setMinimumHeight(MaterialAttrs.dpi(context, 64));
        }
    }

    @Override
    public void setNavigationIcon(Drawable icon) {
        super.setNavigationIcon(MaterialAttrs.tinted(icon, mNavigationIconTint));
    }

    public void setNavigationIconTint(int color) {
        mNavigationIconTint = ColorStateList.valueOf(color);
        if (getNavigationIcon() != null) {
            setNavigationIcon(getNavigationIcon());
        }
    }

    public ColorStateList getNavigationIconTint() {
        return mNavigationIconTint;
    }

    public void setTitleCentered(boolean titleCentered) {
        mTitleCentered = titleCentered;
    }

    public boolean isTitleCentered() {
        return mTitleCentered;
    }

    public void setSubtitleCentered(boolean subtitleCentered) {
        mSubtitleCentered = subtitleCentered;
    }

    public boolean isSubtitleCentered() {
        return mSubtitleCentered;
    }
}
