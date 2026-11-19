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
package com.codename1.ui.chart.layout;

import com.codename1.ui.geom.GeneralPath;

/// Path builders shared by the radial and flow charts.
///
/// Angles are in degrees, measured clockwise from twelve o'clock, which is how people read a
/// dial; screen y grows downwards.
public final class Shapes {
    private Shapes() {
    }

    /// The x coordinate of a point at `deg` on a circle.
    ///
    /// #### Parameters
    ///
    /// - `cx`: the centre x
    ///
    /// - `r`: the radius
    ///
    /// - `deg`: the angle, clockwise from twelve o'clock
    ///
    /// #### Returns
    ///
    /// the x coordinate
    public static float px(float cx, float r, double deg) {
        return (float) (cx + r * Math.sin(Math.toRadians(deg)));
    }

    /// The y coordinate of a point at `deg` on a circle.
    ///
    /// #### Parameters
    ///
    /// - `cy`: the centre y
    ///
    /// - `r`: the radius
    ///
    /// - `deg`: the angle, clockwise from twelve o'clock
    ///
    /// #### Returns
    ///
    /// the y coordinate
    public static float py(float cy, float r, double deg) {
        return (float) (cy - r * Math.cos(Math.toRadians(deg)));
    }

    /// An annular sector, or a pie wedge when `inner` is 0.
    ///
    /// #### Parameters
    ///
    /// - `cx`: the centre x
    ///
    /// - `cy`: the centre y
    ///
    /// - `outer`: the outer radius
    ///
    /// - `inner`: the inner radius, 0 for a wedge
    ///
    /// - `start`: the start angle in degrees
    ///
    /// - `sweep`: the sweep in degrees, positive clockwise
    ///
    /// #### Returns
    ///
    /// a closed path
    public static GeneralPath sector(float cx, float cy, float outer, float inner, double start, double sweep) {
        return sector(new GeneralPath(), cx, cy, outer, inner, start, sweep);
    }

    /// The same sector written into a path the caller reuses, so painting allocates nothing.
    /// The path is reset first.
    ///
    /// #### Parameters
    ///
    /// - `p`: the path to reset and fill
    ///
    /// - `cx`: the centre x
    ///
    /// - `cy`: the centre y
    ///
    /// - `outer`: the outer radius
    ///
    /// - `inner`: the inner radius, 0 for a wedge
    ///
    /// - `start`: the start angle in degrees
    ///
    /// - `sweep`: the sweep in degrees, positive clockwise
    ///
    /// #### Returns
    ///
    /// `p`, closed
    public static GeneralPath sector(GeneralPath p, float cx, float cy, float outer, float inner, double start,
            double sweep) {
        p.reset();
        // a non-finite sweep would ask for Integer.MAX_VALUE steps and hang the EDT; past a
        // full turn a sector looks the same, so the sweep is clamped to one turn either way
        double sw = sweep;
        if (!(Math.abs(sw) <= 360)) { //NOPMD LogicInversion - NaN fails every comparison
            sw = sw < -360 ? -360 : sw > 360 ? 360 : 0;
        }
        double st = Math.abs(start) < 1e9 ? start : 0;
        int steps = Math.max(2, Math.min(360 / 4 + 1, (int) Math.ceil(Math.abs(sw) / 4)));
        for (int i = 0; i <= steps; i++) {
            double a = st + sw * i / steps;
            if (i == 0) {
                p.moveTo(px(cx, outer, a), py(cy, outer, a));
            } else {
                p.lineTo(px(cx, outer, a), py(cy, outer, a));
            }
        }
        if (inner <= 0) {
            p.lineTo(cx, cy);
        } else {
            for (int i = steps; i >= 0; i--) {
                double a = st + sw * i / steps;
                p.lineTo(px(cx, inner, a), py(cy, inner, a));
            }
        }
        p.closePath();
        return p;
    }

    /// A band joining a vertical span on one side to a vertical span on the other with
    /// S-shaped edges, the link of a sankey diagram.
    ///
    /// #### Parameters
    ///
    /// - `x0`: the left x
    ///
    /// - `top0`: the band's top at the left
    ///
    /// - `bottom0`: the band's bottom at the left
    ///
    /// - `x1`: the right x
    ///
    /// - `top1`: the band's top at the right
    ///
    /// - `bottom1`: the band's bottom at the right
    ///
    /// #### Returns
    ///
    /// a closed path
    public static GeneralPath ribbon(float x0, float top0, float bottom0, float x1, float top1, float bottom1) {
        GeneralPath p = new GeneralPath();
        float mx = (x0 + x1) / 2;
        p.moveTo(x0, top0);
        p.curveTo(mx, top0, mx, top1, x1, top1);
        p.lineTo(x1, bottom1);
        p.curveTo(mx, bottom1, mx, bottom0, x0, bottom0);
        p.closePath();
        return p;
    }
}
