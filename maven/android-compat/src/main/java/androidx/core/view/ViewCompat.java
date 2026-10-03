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

import android.content.res.ColorStateList;
import android.graphics.PorterDuff;
import android.view.View;
import android.view.WindowInsets;

/// Static helpers over `View` that older Android releases lacked. Most
/// forward to the current API; window insets, which Codename One applies
/// itself, are delivered once and are always zero.
public final class ViewCompat {

    public static final int IMPORTANT_FOR_ACCESSIBILITY_AUTO = 0;
    public static final int IMPORTANT_FOR_ACCESSIBILITY_YES = 1;
    public static final int IMPORTANT_FOR_ACCESSIBILITY_NO = 2;
    public static final int IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS = 4;
    public static final int LAYOUT_DIRECTION_LTR = View.LAYOUT_DIRECTION_LTR;
    public static final int LAYOUT_DIRECTION_RTL = View.LAYOUT_DIRECTION_RTL;
    public static final int OVER_SCROLL_ALWAYS = 0;
    public static final int OVER_SCROLL_IF_CONTENT_SCROLLS = 1;
    public static final int OVER_SCROLL_NEVER = 2;

    private ViewCompat() {
    }

    public static int generateViewId() {
        return View.generateViewId();
    }

    public static void setElevation(View view, float elevation) {
        view.setElevation(elevation);
    }

    public static float getElevation(View view) {
        return view.getElevation();
    }

    public static void setTranslationZ(View view, float z) {
        view.setTranslationZ(z);
    }

    public static void setBackgroundTintList(View view, ColorStateList tint) {
        view.setBackgroundTintList(tint);
    }

    public static ColorStateList getBackgroundTintList(View view) {
        return view.getBackgroundTintList();
    }

    public static void setBackgroundTintMode(View view, PorterDuff.Mode mode) {
        view.setBackgroundTintMode(mode);
    }

    public static boolean isAttachedToWindow(View view) {
        return view.isAttachedToWindow();
    }

    public static boolean isLaidOut(View view) {
        return view.getWidth() > 0 || view.getHeight() > 0;
    }

    public static int getLayoutDirection(View view) {
        return view.getLayoutDirection();
    }

    public static void setLayoutDirection(View view, int direction) {
        view.setLayoutDirection(direction);
    }

    public static int getPaddingStart(View view) {
        return view.getPaddingStart();
    }

    public static int getPaddingEnd(View view) {
        return view.getPaddingEnd();
    }

    public static void setPaddingRelative(View view, int start, int top, int end, int bottom) {
        view.setPaddingRelative(start, top, end, bottom);
    }

    public static int getMinimumWidth(View view) {
        return view.getMinimumWidth();
    }

    public static int getMinimumHeight(View view) {
        return view.getMinimumHeight();
    }

    public static void postOnAnimation(View view, Runnable action) {
        view.postOnAnimation(action);
    }

    public static void postOnAnimationDelayed(View view, Runnable action, long delayMillis) {
        view.postDelayed(action, delayMillis);
    }

    public static void postInvalidateOnAnimation(View view) {
        view.postInvalidateOnAnimation();
    }

    public static void setImportantForAccessibility(View view, int mode) {
    }

    public static void setAccessibilityDelegate(View view, Object delegate) {
    }

    public static void setTooltipText(View view, CharSequence text) {
        view.setTooltipText(text);
    }

    public static void setTransitionName(View view, String name) {
        view.setTransitionName(name);
    }

    public static String getTransitionName(View view) {
        return view.getTransitionName();
    }

    public static void setOnApplyWindowInsetsListener(final View view, final OnApplyWindowInsetsListener listener) {
        if (listener == null) {
            view.setOnApplyWindowInsetsListener(null);
            return;
        }
        view.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
            @Override
            public WindowInsets onApplyWindowInsets(View v, WindowInsets insets) {
                return listener.onApplyWindowInsets(v, WindowInsetsCompat.toWindowInsetsCompat(insets)).toWindowInsets();
            }
        });
        // Codename One lays the tree out in the safe area already: the
        // listener hears the (zero) insets once, as Android delivers them on
        // the first layout pass.
        view.post(new Runnable() {
            @Override
            public void run() {
                listener.onApplyWindowInsets(view, WindowInsetsCompat.toWindowInsetsCompat(WindowInsets.CONSUMED));
            }
        });
    }

    public static void requestApplyInsets(View view) {
        view.requestApplyInsets();
    }

    public static WindowInsetsCompat getRootWindowInsets(View view) {
        return WindowInsetsCompat.toWindowInsetsCompat(WindowInsets.CONSUMED);
    }

    public static WindowInsetsCompat onApplyWindowInsets(View view, WindowInsetsCompat insets) {
        return insets;
    }

    public static WindowInsetsCompat dispatchApplyWindowInsets(View view, WindowInsetsCompat insets) {
        return insets;
    }

    public static boolean canScrollVertically(View view, int direction) {
        return view.canScrollVertically(direction);
    }

    public static boolean canScrollHorizontally(View view, int direction) {
        return view.canScrollHorizontally(direction);
    }

    public static boolean hasOnClickListeners(View view) {
        return view.hasOnClickListeners();
    }
}
