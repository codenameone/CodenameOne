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
package com.google.android.material.textfield;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.ViewParent;

import androidx.appcompat.widget.AppCompatEditText;

/// The text field inside a [TextInputLayout]. It tells the layout when it
/// gains or loses focus, so the floating label moves at once.
public class TextInputEditText extends AppCompatEditText {

    public TextInputEditText(Context context) {
        this(context, null);
    }

    public TextInputEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public TextInputEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    /// The layout this field sits in, or null.
    public TextInputLayout getTextInputLayout() {
        ViewParent p = getParent();
        while (p != null) {
            if (p instanceof TextInputLayout) {
                return (TextInputLayout) p;
            }
            p = p.getParent();
        }
        return null;
    }

    @Override
    protected void onFocusChanged(boolean gainFocus, int direction, Rect previouslyFocusedRect) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
        TextInputLayout layout = getTextInputLayout();
        if (layout != null) {
            layout.onEditTextStateChanged(true);
        }
    }

    /// The layout's hint, which the layout took from the field to float it.
    @Override
    public CharSequence getHint() {
        TextInputLayout layout = getTextInputLayout();
        if (layout != null && layout.isHintEnabled()) {
            return layout.getHint();
        }
        return super.getHint();
    }
}
