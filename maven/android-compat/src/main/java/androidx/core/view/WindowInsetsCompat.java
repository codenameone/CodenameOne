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
package androidx.core.view;

import android.view.WindowInsets;

import androidx.core.graphics.Insets;

/// The insets of a window. Codename One keeps content out of the status bar,
/// notch and home indicator itself, so an Android view tree is laid out
/// inside the safe area and sees no system insets.
public class WindowInsetsCompat {

    public static final WindowInsetsCompat CONSUMED = new WindowInsetsCompat(WindowInsets.CONSUMED);

    private final WindowInsets insets;

    public WindowInsetsCompat(WindowInsetsCompat src) {
        this(src == null ? WindowInsets.CONSUMED : src.insets);
    }

    private WindowInsetsCompat(WindowInsets insets) {
        this.insets = insets == null ? WindowInsets.CONSUMED : insets;
    }

    public static WindowInsetsCompat toWindowInsetsCompat(WindowInsets insets) {
        return new WindowInsetsCompat(insets);
    }

    public static WindowInsetsCompat toWindowInsetsCompat(WindowInsets insets, android.view.View view) {
        return new WindowInsetsCompat(insets);
    }

    public WindowInsets toWindowInsets() {
        return insets;
    }

    /// Inset types, as bit masks.
    public static final class Type {
        public static final int STATUS_BARS = 1;
        public static final int NAVIGATION_BARS = 1 << 1;
        public static final int CAPTION_BAR = 1 << 2;
        public static final int IME = 1 << 3;
        public static final int SYSTEM_GESTURES = 1 << 4;
        public static final int MANDATORY_SYSTEM_GESTURES = 1 << 5;
        public static final int TAPPABLE_ELEMENT = 1 << 6;
        public static final int DISPLAY_CUTOUT = 1 << 7;

        private Type() {
        }

        public static int statusBars() {
            return STATUS_BARS;
        }

        public static int navigationBars() {
            return NAVIGATION_BARS;
        }

        public static int captionBar() {
            return CAPTION_BAR;
        }

        public static int ime() {
            return IME;
        }

        public static int systemGestures() {
            return SYSTEM_GESTURES;
        }

        public static int mandatorySystemGestures() {
            return MANDATORY_SYSTEM_GESTURES;
        }

        public static int tappableElement() {
            return TAPPABLE_ELEMENT;
        }

        public static int displayCutout() {
            return DISPLAY_CUTOUT;
        }

        public static int systemBars() {
            return STATUS_BARS | NAVIGATION_BARS | CAPTION_BAR;
        }
    }

    public Insets getInsets(int typeMask) {
        if ((typeMask & (Type.STATUS_BARS | Type.NAVIGATION_BARS | Type.CAPTION_BAR)) != 0) {
            return Insets.of(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(),
                    insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
        }
        return Insets.NONE;
    }

    public Insets getInsetsIgnoringVisibility(int typeMask) {
        return getInsets(typeMask);
    }

    public boolean isVisible(int typeMask) {
        return (typeMask & Type.IME) == 0;
    }

    public int getSystemWindowInsetLeft() {
        return insets.getSystemWindowInsetLeft();
    }

    public int getSystemWindowInsetTop() {
        return insets.getSystemWindowInsetTop();
    }

    public int getSystemWindowInsetRight() {
        return insets.getSystemWindowInsetRight();
    }

    public int getSystemWindowInsetBottom() {
        return insets.getSystemWindowInsetBottom();
    }

    public boolean hasSystemWindowInsets() {
        return insets.hasSystemWindowInsets();
    }

    public boolean isConsumed() {
        return insets.isConsumed();
    }

    public WindowInsetsCompat consumeSystemWindowInsets() {
        return new WindowInsetsCompat(insets.consumeSystemWindowInsets());
    }

    public WindowInsetsCompat replaceSystemWindowInsets(int left, int top, int right, int bottom) {
        return new WindowInsetsCompat(insets.replaceSystemWindowInsets(left, top, right, bottom));
    }

    /// Builds insets; every inset this runtime reports is zero.
    public static final class Builder {
        private WindowInsets insets = WindowInsets.CONSUMED;

        public Builder() {
        }

        public Builder(WindowInsetsCompat src) {
            insets = src.insets;
        }

        public Builder setInsets(int typeMask, Insets i) {
            insets = insets.replaceSystemWindowInsets(i.left, i.top, i.right, i.bottom);
            return this;
        }

        public WindowInsetsCompat build() {
            return new WindowInsetsCompat(insets);
        }
    }
}
