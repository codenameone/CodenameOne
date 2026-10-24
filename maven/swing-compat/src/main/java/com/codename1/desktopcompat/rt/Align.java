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
package com.codename1.desktopcompat.rt;

/// Turns the Swing alignment and text position constants into the ones a
/// Codename One label takes.
public final class Align {

    private static final int SWING_CENTER = 0;
    private static final int SWING_TOP = 1;
    private static final int SWING_LEFT = 2;
    private static final int SWING_BOTTOM = 3;
    private static final int SWING_RIGHT = 4;
    private static final int SWING_LEADING = 10;
    private static final int SWING_TRAILING = 11;

    private Align() {
    }

    /// A horizontal alignment: `LEADING` is left and `TRAILING` right.
    public static int horizontal(int swing) {
        switch (swing) {
            case SWING_CENTER:
                return com.codename1.ui.Component.CENTER;
            case SWING_RIGHT:
            case SWING_TRAILING:
                return com.codename1.ui.Component.RIGHT;
            default:
                return com.codename1.ui.Component.LEFT;
        }
    }

    /// A vertical alignment.
    public static int vertical(int swing) {
        switch (swing) {
            case SWING_TOP:
                return com.codename1.ui.Component.TOP;
            case SWING_BOTTOM:
                return com.codename1.ui.Component.BOTTOM;
            default:
                return com.codename1.ui.Component.CENTER;
        }
    }

    /// Where the text goes relative to the icon. Codename One has four
    /// places where Swing has nine: text that is horizontally centered
    /// goes above or below the icon as the vertical position says, and
    /// any other text goes beside it.
    public static int textPosition(int horizontal, int vertical) {
        if (horizontal == SWING_CENTER) {
            if (vertical == SWING_TOP) {
                return com.codename1.ui.Component.TOP;
            }
            if (vertical == SWING_BOTTOM) {
                return com.codename1.ui.Component.BOTTOM;
            }
            return com.codename1.ui.Component.RIGHT;
        }
        if (horizontal == SWING_LEFT || horizontal == SWING_LEADING) {
            return com.codename1.ui.Component.LEFT;
        }
        return com.codename1.ui.Component.RIGHT;
    }
}
