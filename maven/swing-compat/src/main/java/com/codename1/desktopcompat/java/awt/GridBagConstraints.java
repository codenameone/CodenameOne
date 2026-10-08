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
package com.codename1.desktopcompat.java.awt;

/// The placement of one component in a [GridBagLayout]: its cell, how many
/// cells it spans, how it shares spare space and how it sits inside its
/// display area.
public class GridBagConstraints implements Cloneable {

    public static final int RELATIVE = -1;

    public static final int REMAINDER = 0;

    public static final int NONE = 0;

    public static final int BOTH = 1;

    public static final int HORIZONTAL = 2;

    public static final int VERTICAL = 3;

    public static final int CENTER = 10;

    public static final int NORTH = 11;

    public static final int NORTHEAST = 12;

    public static final int EAST = 13;

    public static final int SOUTHEAST = 14;

    public static final int SOUTH = 15;

    public static final int SOUTHWEST = 16;

    public static final int WEST = 17;

    public static final int NORTHWEST = 18;

    public static final int PAGE_START = 19;

    public static final int PAGE_END = 20;

    public static final int LINE_START = 21;

    public static final int LINE_END = 22;

    public static final int FIRST_LINE_START = 23;

    public static final int FIRST_LINE_END = 24;

    public static final int LAST_LINE_START = 25;

    public static final int LAST_LINE_END = 26;

    public static final int BASELINE = 0x100;

    public static final int BASELINE_LEADING = 0x200;

    public static final int BASELINE_TRAILING = 0x300;

    public static final int ABOVE_BASELINE = 0x400;

    public static final int ABOVE_BASELINE_LEADING = 0x500;

    public static final int ABOVE_BASELINE_TRAILING = 0x600;

    public static final int BELOW_BASELINE = 0x700;

    public static final int BELOW_BASELINE_LEADING = 0x800;

    public static final int BELOW_BASELINE_TRAILING = 0x900;

    public int gridx;

    public int gridy;

    public int gridwidth;

    public int gridheight;

    public double weightx;

    public double weighty;

    public int anchor;

    public int fill;

    public Insets insets;

    public int ipadx;

    public int ipady;

    /// The resolved cell and span, written by the layout on each pass.
    int tempX;
    int tempY;
    int tempWidth;
    int tempHeight;
    /// The component's own size as last measured by the layout.
    int minWidth;
    int minHeight;

    public GridBagConstraints() {
        gridx = RELATIVE;
        gridy = RELATIVE;
        gridwidth = 1;
        gridheight = 1;
        anchor = CENTER;
        fill = NONE;
        insets = new Insets(0, 0, 0, 0);
    }

    public GridBagConstraints(int gridx, int gridy, int gridwidth, int gridheight, double weightx, double weighty,
            int anchor, int fill, Insets insets, int ipadx, int ipady) {
        this.gridx = gridx;
        this.gridy = gridy;
        this.gridwidth = gridwidth;
        this.gridheight = gridheight;
        this.weightx = weightx;
        this.weighty = weighty;
        this.anchor = anchor;
        this.fill = fill;
        this.insets = insets;
        this.ipadx = ipadx;
        this.ipady = ipady;
    }

    /// A copy of these constraints with its own `insets` object. Built field
    /// by field, since `Object.clone()` is not available on a device.
    @Override
    public Object clone() {
        Insets in = insets == null ? null : new Insets(insets.top, insets.left, insets.bottom, insets.right);
        GridBagConstraints c = new GridBagConstraints(gridx, gridy, gridwidth, gridheight, weightx, weighty,
                anchor, fill, in, ipadx, ipady);
        c.tempX = tempX;
        c.tempY = tempY;
        c.tempWidth = tempWidth;
        c.tempHeight = tempHeight;
        c.minWidth = minWidth;
        c.minHeight = minHeight;
        return c;
    }
}
