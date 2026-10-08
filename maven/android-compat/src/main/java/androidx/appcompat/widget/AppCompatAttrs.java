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
package androidx.appcompat.widget;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.R;

/// Applies the attributes AppCompat widgets add to the framework's, from the
/// application namespace: background, image and button tints, `srcCompat`,
/// and `textAllCaps`/`fontFamily` on text.
final class AppCompatAttrs {

    private AppCompatAttrs() {
    }

    /// AppCompat widgets take their default style from AppCompat's theme
    /// attribute. Under a theme that does not define it (a framework theme)
    /// the framework's attribute is used, so the widget is still styled.
    static int defStyle(Context context, int appCompatAttr, int frameworkAttr) {
        TypedValue tv = new TypedValue();
        if (context.getTheme().resolveAttribute(appCompatAttr, tv, true)) {
            return appCompatAttr;
        }
        return frameworkAttr;
    }

    static void background(View v, AttributeSet attrs, int defStyleAttr) {
        TypedArray a = v.getContext().obtainStyledAttributes(attrs, R.styleable.ViewBackgroundHelper, defStyleAttr, 0);
        try {
            if (a.hasValue(R.styleable.ViewBackgroundHelper_backgroundTint)) {
                ColorStateList tint = a.getColorStateList(R.styleable.ViewBackgroundHelper_backgroundTint);
                if (tint != null) {
                    v.setBackgroundTintList(tint);
                }
            }
            if (a.hasValue(R.styleable.ViewBackgroundHelper_backgroundTintMode)) {
                v.setBackgroundTintMode(PorterDuff.intToMode(
                        a.getInt(R.styleable.ViewBackgroundHelper_backgroundTintMode, -1)));
            }
        } finally {
            a.recycle();
        }
    }

    static void image(ImageView v, AttributeSet attrs, int defStyleAttr) {
        TypedArray a = v.getContext().obtainStyledAttributes(attrs, R.styleable.AppCompatImageView, defStyleAttr, 0);
        try {
            int src = a.getResourceId(R.styleable.AppCompatImageView_srcCompat, 0);
            if (src != 0) {
                v.setImageDrawable(v.getContext().getDrawable(src));
            }
            if (a.hasValue(R.styleable.AppCompatImageView_tint)) {
                v.setImageTintList(a.getColorStateList(R.styleable.AppCompatImageView_tint));
            }
            if (a.hasValue(R.styleable.AppCompatImageView_tintMode)) {
                v.setImageTintMode(PorterDuff.intToMode(a.getInt(R.styleable.AppCompatImageView_tintMode, -1)));
            }
        } finally {
            a.recycle();
        }
    }

    static void compound(CompoundButton v, AttributeSet attrs, int defStyleAttr) {
        TypedArray a = v.getContext().obtainStyledAttributes(attrs, R.styleable.CompoundButton, defStyleAttr, 0);
        try {
            int button = a.getResourceId(R.styleable.CompoundButton_buttonCompat, 0);
            if (button != 0) {
                v.setButtonDrawable(button);
            }
            if (a.hasValue(R.styleable.CompoundButton_buttonTint)) {
                v.setButtonTintList(a.getColorStateList(R.styleable.CompoundButton_buttonTint));
            }
            if (a.hasValue(R.styleable.CompoundButton_buttonTintMode)) {
                v.setButtonTintMode(PorterDuff.intToMode(a.getInt(R.styleable.CompoundButton_buttonTintMode, -1)));
            }
        } finally {
            a.recycle();
        }
    }

    static void text(TextView v, AttributeSet attrs, int defStyleAttr) {
        TypedArray a = v.getContext().obtainStyledAttributes(attrs, R.styleable.AppCompatTextView, defStyleAttr, 0);
        try {
            if (a.hasValue(R.styleable.AppCompatTextView_textAllCaps)) {
                v.setAllCaps(a.getBoolean(R.styleable.AppCompatTextView_textAllCaps, false));
            }
            if (a.hasValue(R.styleable.AppCompatTextView_fontFamily)) {
                int font = a.getResourceId(R.styleable.AppCompatTextView_fontFamily, 0);
                Typeface tf = null;
                if (font != 0) {
                    tf = v.getContext().getResources().getFont(font);
                } else {
                    String family = a.getString(R.styleable.AppCompatTextView_fontFamily);
                    if (family != null) {
                        tf = Typeface.create(family, Typeface.NORMAL);
                    }
                }
                if (tf != null) {
                    v.setTypeface(tf);
                }
            }
            if (a.hasValue(R.styleable.AppCompatTextView_lineHeight)) {
                v.setLineHeight(a.getDimensionPixelSize(R.styleable.AppCompatTextView_lineHeight, 0));
            }
            if (a.hasValue(R.styleable.AppCompatTextView_drawableTint)) {
                v.setCompoundDrawableTintList(a.getColorStateList(R.styleable.AppCompatTextView_drawableTint));
            }
        } finally {
            a.recycle();
        }
    }
}
