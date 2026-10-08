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
package android.content.res;

import android.util.StateSet;

/// Colors that vary with view state: the compiled form of a `<selector>` in
/// `res/color/`, or a single color.
public class ColorStateList {

    private final int[][] stateSpecs;
    private final int[] colors;
    private final int defaultColor;

    public ColorStateList(int[][] states, int[] colors) {
        this.stateSpecs = states;
        this.colors = colors;
        int def = colors.length > 0 ? colors[0] : 0xff000000;
        for (int i = 0; i < states.length; i++) {
            if (states[i].length == 0) {
                def = colors[i];
                break;
            }
        }
        this.defaultColor = def;
    }

    public static ColorStateList valueOf(int color) {
        return new ColorStateList(new int[][] {new int[0]}, new int[] {color});
    }

    public int getColorForState(int[] stateSet, int defaultColor) {
        for (int i = 0; i < stateSpecs.length; i++) {
            if (StateSet.stateSetMatches(stateSpecs[i], stateSet)) {
                return colors[i];
            }
        }
        return defaultColor;
    }

    public int getDefaultColor() {
        return defaultColor;
    }

    public boolean isStateful() {
        return stateSpecs.length > 1 || (stateSpecs.length == 1 && stateSpecs[0].length > 0);
    }

    public boolean isOpaque() {
        for (int c : colors) {
            if ((c >>> 24) != 0xff) {
                return false;
            }
        }
        return true;
    }

    public ColorStateList withAlpha(int alpha) {
        int[] c = new int[colors.length];
        for (int i = 0; i < c.length; i++) {
            c[i] = (colors[i] & 0xffffff) | (alpha << 24);
        }
        return new ColorStateList(stateSpecs, c);
    }

    public int[][] getStates() {
        return stateSpecs;
    }

    public int[] getColors() {
        return colors;
    }

    @Override
    public String toString() {
        return "ColorStateList{default=#" + Integer.toHexString(defaultColor) + ", n=" + colors.length + "}";
    }
}
