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
package android.view;

/// Touch slop and timeouts. Distances scale with the device density as on
/// Android.
public class ViewConfiguration {

    private static final int TOUCH_SLOP_DP = 8;
    private final float density;

    private ViewConfiguration(float density) {
        this.density = density;
    }

    public static ViewConfiguration get(android.content.Context context) {
        return new ViewConfiguration(context.getResources().getDisplayMetrics().density);
    }

    public int getScaledTouchSlop() {
        return (int) (TOUCH_SLOP_DP * density + 0.5f);
    }

    public int getScaledPagingTouchSlop() {
        return getScaledTouchSlop() * 2;
    }

    public int getScaledDoubleTapSlop() {
        return (int) (100 * density + 0.5f);
    }

    public int getScaledMinimumFlingVelocity() {
        return (int) (50 * density + 0.5f);
    }

    public int getScaledMaximumFlingVelocity() {
        return (int) (8000 * density + 0.5f);
    }

    public int getScaledOverscrollDistance() {
        return 0;
    }

    public int getScaledOverflingDistance() {
        return (int) (6 * density + 0.5f);
    }

    public int getScaledEdgeSlop() {
        return (int) (12 * density + 0.5f);
    }

    public int getScaledWindowTouchSlop() {
        return (int) (16 * density + 0.5f);
    }

    public int getScaledScrollBarSize() {
        return (int) (4 * density + 0.5f);
    }

    public boolean hasPermanentMenuKey() {
        return false;
    }

    public static int getTapTimeout() {
        return 100;
    }

    public static int getLongPressTimeout() {
        return 400;
    }

    public static int getDoubleTapTimeout() {
        return 300;
    }

    public static int getPressedStateDuration() {
        return 64;
    }

    public static int getJumpTapTimeout() {
        return 500;
    }

    public static int getKeyRepeatTimeout() {
        return getLongPressTimeout();
    }

    public static int getScrollBarFadeDuration() {
        return 250;
    }

    public static int getScrollDefaultDelay() {
        return 300;
    }

    public static float getScrollFriction() {
        return 0.015f;
    }

    @Deprecated
    public static int getTouchSlop() {
        return TOUCH_SLOP_DP;
    }
}
