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
package com.google.android.material.floatingactionbutton;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import com.google.android.material.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.internal.MaterialAttrs;

/// A floating action button with a label: a [MaterialButton] in the
/// extended FAB style that can [#shrink()] to its icon and [#extend()] back.
public class ExtendedFloatingActionButton extends MaterialButton {

    private boolean mExtended = true;
    private CharSequence mLabel;

    public ExtendedFloatingActionButton(Context context) {
        this(context, null);
    }

    public ExtendedFloatingActionButton(Context context, AttributeSet attrs) {
        this(context, attrs, MaterialAttrs.defStyleAttr(context, R.attr.extendedFloatingActionButtonStyle));
    }

    public ExtendedFloatingActionButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr, MaterialAttrs.isMaterial3(context)
                ? R.style.Widget_Material3_ExtendedFloatingActionButton_Icon_Primary
                : R.style.Widget_MaterialComponents_ExtendedFloatingActionButton_Icon);
        setGravity(android.view.Gravity.CENTER_VERTICAL | android.view.Gravity.START);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (!mExtended) {
            int h = getMeasuredHeight();
            setMeasuredDimension(h, h);
        }
    }

    public boolean isExtended() {
        return mExtended;
    }

    /// Hides the label, leaving a square button with the icon.
    public void shrink() {
        if (!mExtended) {
            return;
        }
        mExtended = false;
        mLabel = getText();
        setText("");
        int pad = (getMinimumHeight() - getIconSize()) / 2;
        setIconPadding(0);
        super.setPadding(Math.max(0, pad), getPaddingTop(), Math.max(0, pad), getPaddingBottom());
        requestLayout();
    }

    /// Shows the label again.
    public void extend() {
        if (mExtended) {
            return;
        }
        mExtended = true;
        setText(mLabel);
        setIconPadding(MaterialAttrs.dpi(getContext(), 12));
        super.setPadding(MaterialAttrs.dpi(getContext(), 16), getPaddingTop(), MaterialAttrs.dpi(getContext(), 20),
                getPaddingBottom());
        requestLayout();
    }

    public void setExtended(boolean extended) {
        if (extended) {
            extend();
        } else {
            shrink();
        }
    }

    public void show() {
        setVisibility(View.VISIBLE);
        setAlpha(0f);
        animate().alpha(1f).setDuration(150).start();
    }

    public void hide() {
        animate().alpha(0f).setDuration(100).withEndAction(new Runnable() {
            @Override
            public void run() {
                setVisibility(View.INVISIBLE);
                setAlpha(1f);
            }
        }).start();
    }
}
