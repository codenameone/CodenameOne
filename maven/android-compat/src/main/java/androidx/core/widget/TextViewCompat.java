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
package androidx.core.widget;

import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.widget.TextView;

/// Static helpers over `TextView`.
public final class TextViewCompat {

    public static final int AUTO_SIZE_TEXT_TYPE_NONE = 0;
    public static final int AUTO_SIZE_TEXT_TYPE_UNIFORM = 1;

    private TextViewCompat() {
    }

    public static void setTextAppearance(TextView textView, int resId) {
        textView.setTextAppearance(resId);
    }

    public static void setCompoundDrawablesRelative(TextView textView, Drawable start, Drawable top, Drawable end,
                                                    Drawable bottom) {
        textView.setCompoundDrawablesRelative(start, top, end, bottom);
    }

    public static void setCompoundDrawablesRelativeWithIntrinsicBounds(TextView textView, int start, int top,
                                                                       int end, int bottom) {
        textView.setCompoundDrawablesRelativeWithIntrinsicBounds(start, top, end, bottom);
    }

    public static Drawable[] getCompoundDrawablesRelative(TextView textView) {
        return textView.getCompoundDrawablesRelative();
    }

    public static void setCompoundDrawableTintList(TextView textView, ColorStateList tint) {
        textView.setCompoundDrawableTintList(tint);
    }

    public static int getMaxLines(TextView textView) {
        return textView.getMaxLines();
    }

    public static void setLineHeight(TextView textView, int lineHeight) {
        textView.setLineHeight(lineHeight);
    }

    /// Recorded; text is laid out at its own size.
    public static void setAutoSizeTextTypeWithDefaults(TextView textView, int autoSizeTextType) {
    }

    public static void setAutoSizeTextTypeUniformWithConfiguration(TextView textView, int min, int max,
                                                                   int granularity, int unit) {
    }
}
