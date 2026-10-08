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

import android.graphics.Rect;

/// Gravity constants and the placement math views and drawables share.
public class Gravity {

    public static final int NO_GRAVITY = 0x0000;
    public static final int AXIS_SPECIFIED = 0x0001;
    public static final int AXIS_PULL_BEFORE = 0x0002;
    public static final int AXIS_PULL_AFTER = 0x0004;
    public static final int AXIS_CLIP = 0x0008;
    public static final int AXIS_X_SHIFT = 0;
    public static final int AXIS_Y_SHIFT = 4;
    public static final int TOP = (AXIS_PULL_BEFORE | AXIS_SPECIFIED) << AXIS_Y_SHIFT;
    public static final int BOTTOM = (AXIS_PULL_AFTER | AXIS_SPECIFIED) << AXIS_Y_SHIFT;
    public static final int LEFT = (AXIS_PULL_BEFORE | AXIS_SPECIFIED) << AXIS_X_SHIFT;
    public static final int RIGHT = (AXIS_PULL_AFTER | AXIS_SPECIFIED) << AXIS_X_SHIFT;
    public static final int CENTER_VERTICAL = AXIS_SPECIFIED << AXIS_Y_SHIFT;
    public static final int FILL_VERTICAL = TOP | BOTTOM;
    public static final int CENTER_HORIZONTAL = AXIS_SPECIFIED << AXIS_X_SHIFT;
    public static final int FILL_HORIZONTAL = LEFT | RIGHT;
    public static final int CENTER = CENTER_VERTICAL | CENTER_HORIZONTAL;
    public static final int FILL = FILL_VERTICAL | FILL_HORIZONTAL;
    public static final int CLIP_VERTICAL = AXIS_CLIP << AXIS_Y_SHIFT;
    public static final int CLIP_HORIZONTAL = AXIS_CLIP << AXIS_X_SHIFT;
    public static final int RELATIVE_LAYOUT_DIRECTION = 0x00800000;
    public static final int HORIZONTAL_GRAVITY_MASK = (AXIS_SPECIFIED | AXIS_PULL_BEFORE | AXIS_PULL_AFTER) << AXIS_X_SHIFT;
    public static final int VERTICAL_GRAVITY_MASK = (AXIS_SPECIFIED | AXIS_PULL_BEFORE | AXIS_PULL_AFTER) << AXIS_Y_SHIFT;
    public static final int DISPLAY_CLIP_VERTICAL = 0x10000000;
    public static final int DISPLAY_CLIP_HORIZONTAL = 0x01000000;
    public static final int START = RELATIVE_LAYOUT_DIRECTION | LEFT;
    public static final int END = RELATIVE_LAYOUT_DIRECTION | RIGHT;
    public static final int RELATIVE_HORIZONTAL_GRAVITY_MASK = START | END;

    public static void apply(int gravity, int w, int h, Rect container, Rect outRect) {
        apply(gravity, w, h, container, 0, 0, outRect);
    }

    public static void apply(int gravity, int w, int h, Rect container, Rect outRect, int layoutDirection) {
        apply(getAbsoluteGravity(gravity, layoutDirection), w, h, container, 0, 0, outRect);
    }

    public static void apply(int gravity, int w, int h, Rect container, int xAdj, int yAdj, Rect outRect) {
        switch (gravity & ((AXIS_PULL_BEFORE | AXIS_PULL_AFTER) << AXIS_X_SHIFT)) {
            case 0:
                outRect.left = container.left + ((container.right - container.left - w) / 2) + xAdj;
                outRect.right = outRect.left + w;
                if ((gravity & (AXIS_CLIP << AXIS_X_SHIFT)) == (AXIS_CLIP << AXIS_X_SHIFT)) {
                    if (outRect.left < container.left) {
                        outRect.left = container.left;
                    }
                    if (outRect.right > container.right) {
                        outRect.right = container.right;
                    }
                }
                break;
            case AXIS_PULL_BEFORE << AXIS_X_SHIFT:
                outRect.left = container.left + xAdj;
                outRect.right = outRect.left + w;
                if ((gravity & (AXIS_CLIP << AXIS_X_SHIFT)) == (AXIS_CLIP << AXIS_X_SHIFT)) {
                    if (outRect.right > container.right) {
                        outRect.right = container.right;
                    }
                }
                break;
            case AXIS_PULL_AFTER << AXIS_X_SHIFT:
                outRect.right = container.right - xAdj;
                outRect.left = outRect.right - w;
                if ((gravity & (AXIS_CLIP << AXIS_X_SHIFT)) == (AXIS_CLIP << AXIS_X_SHIFT)) {
                    if (outRect.left < container.left) {
                        outRect.left = container.left;
                    }
                }
                break;
            default:
                outRect.left = container.left + xAdj;
                outRect.right = container.right + xAdj;
                break;
        }
        switch (gravity & ((AXIS_PULL_BEFORE | AXIS_PULL_AFTER) << AXIS_Y_SHIFT)) {
            case 0:
                outRect.top = container.top + ((container.bottom - container.top - h) / 2) + yAdj;
                outRect.bottom = outRect.top + h;
                if ((gravity & (AXIS_CLIP << AXIS_Y_SHIFT)) == (AXIS_CLIP << AXIS_Y_SHIFT)) {
                    if (outRect.top < container.top) {
                        outRect.top = container.top;
                    }
                    if (outRect.bottom > container.bottom) {
                        outRect.bottom = container.bottom;
                    }
                }
                break;
            case AXIS_PULL_BEFORE << AXIS_Y_SHIFT:
                outRect.top = container.top + yAdj;
                outRect.bottom = outRect.top + h;
                if ((gravity & (AXIS_CLIP << AXIS_Y_SHIFT)) == (AXIS_CLIP << AXIS_Y_SHIFT)) {
                    if (outRect.bottom > container.bottom) {
                        outRect.bottom = container.bottom;
                    }
                }
                break;
            case AXIS_PULL_AFTER << AXIS_Y_SHIFT:
                outRect.bottom = container.bottom - yAdj;
                outRect.top = outRect.bottom - h;
                if ((gravity & (AXIS_CLIP << AXIS_Y_SHIFT)) == (AXIS_CLIP << AXIS_Y_SHIFT)) {
                    if (outRect.top < container.top) {
                        outRect.top = container.top;
                    }
                }
                break;
            default:
                outRect.top = container.top + yAdj;
                outRect.bottom = container.bottom + yAdj;
                break;
        }
    }

    public static boolean isVertical(int gravity) {
        return gravity > 0 && (gravity & VERTICAL_GRAVITY_MASK) != 0;
    }

    public static boolean isHorizontal(int gravity) {
        return gravity > 0 && (gravity & RELATIVE_HORIZONTAL_GRAVITY_MASK) != 0;
    }

    /// Resolves START/END to LEFT/RIGHT for `layoutDirection` (0 LTR, 1 RTL).
    public static int getAbsoluteGravity(int gravity, int layoutDirection) {
        int result = gravity;
        if ((result & RELATIVE_LAYOUT_DIRECTION) != 0) {
            if ((result & START) == START) {
                result &= ~START;
                result |= layoutDirection == 1 ? RIGHT : LEFT;
            } else if ((result & END) == END) {
                result &= ~END;
                result |= layoutDirection == 1 ? LEFT : RIGHT;
            }
            result &= ~RELATIVE_LAYOUT_DIRECTION;
        }
        return result;
    }
}
