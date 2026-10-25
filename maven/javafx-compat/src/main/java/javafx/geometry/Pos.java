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
package javafx.geometry;

/// A vertical and a horizontal position combined.
public enum Pos {
    /// Top, left.
    TOP_LEFT(VPos.TOP, HPos.LEFT),
    /// Top, centered horizontally.
    TOP_CENTER(VPos.TOP, HPos.CENTER),
    /// Top, right.
    TOP_RIGHT(VPos.TOP, HPos.RIGHT),
    /// Centered vertically, left.
    CENTER_LEFT(VPos.CENTER, HPos.LEFT),
    /// Centered both ways.
    CENTER(VPos.CENTER, HPos.CENTER),
    /// Centered vertically, right.
    CENTER_RIGHT(VPos.CENTER, HPos.RIGHT),
    /// Bottom, left.
    BOTTOM_LEFT(VPos.BOTTOM, HPos.LEFT),
    /// Bottom, centered horizontally.
    BOTTOM_CENTER(VPos.BOTTOM, HPos.CENTER),
    /// Bottom, right.
    BOTTOM_RIGHT(VPos.BOTTOM, HPos.RIGHT),
    /// On the baseline, left.
    BASELINE_LEFT(VPos.BASELINE, HPos.LEFT),
    /// On the baseline, centered horizontally.
    BASELINE_CENTER(VPos.BASELINE, HPos.CENTER),
    /// On the baseline, right.
    BASELINE_RIGHT(VPos.BASELINE, HPos.RIGHT);

    private final VPos vpos;
    private final HPos hpos;

    Pos(VPos vpos, HPos hpos) {
        this.vpos = vpos;
        this.hpos = hpos;
    }

    /// Returns the vertical part.
    public VPos getVpos() {
        return vpos;
    }

    /// Returns the horizontal part.
    public HPos getHpos() {
        return hpos;
    }
}
