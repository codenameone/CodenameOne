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
package com.codename1.art;

import com.codename1.ui.geom.GeneralPath;

import java.util.ArrayList;
import java.util.List;

/// A vector shape: a path with any number of fills and strokes, painted in order.
public class ShapeNode extends ArtNode {
    private GeneralPath path;
    private final List<ArtPaint> fills = new ArrayList<ArtPaint>();
    private final List<StrokeStyle> strokes = new ArrayList<StrokeStyle>();

    /// Creates a shape.
    ///
    /// #### Parameters
    ///
    /// - `path`: its outline, in the shape's space
    public ShapeNode(GeneralPath path) {
        this.path = path == null ? new GeneralPath() : path;
    }

    /// A rectangle, optionally with rounded corners.
    ///
    /// #### Parameters
    ///
    /// - `x`: left
    /// - `y`: top
    /// - `w`: width
    /// - `h`: height
    /// - `rx`: corner radius; 0 for square corners
    public static ShapeNode rect(double x, double y, double w, double h, double rx) {
        return new ShapeNode(Shapes.roundRect(x, y, w, h, rx, rx));
    }

    /// An ellipse.
    ///
    /// #### Parameters
    ///
    /// - `cx`: center x
    /// - `cy`: center y
    /// - `rx`: horizontal radius
    /// - `ry`: vertical radius
    public static ShapeNode ellipse(double cx, double cy, double rx, double ry) {
        return new ShapeNode(Shapes.ellipse(cx, cy, rx, ry));
    }

    /// A straight line.
    ///
    /// #### Parameters
    ///
    /// - `x1`: start x
    /// - `y1`: start y
    /// - `x2`: end x
    /// - `y2`: end y
    public static ShapeNode line(double x1, double y1, double x2, double y2) {
        GeneralPath p = new GeneralPath();
        p.moveTo(x1, y1);
        p.lineTo(x2, y2);
        return new ShapeNode(p);
    }

    /// The outline.
    public GeneralPath getPath() {
        return path;
    }

    /// Replaces the outline.
    ///
    /// #### Parameters
    ///
    /// - `path`: the new outline
    public void setPath(GeneralPath path) {
        this.path = path == null ? new GeneralPath() : path;
    }

    /// The fills, painted first to last. The list is live.
    public List<ArtPaint> getFills() {
        return fills;
    }

    /// The strokes, painted after the fills. The list is live.
    public List<StrokeStyle> getStrokes() {
        return strokes;
    }

    /// Adds a fill and returns this shape.
    ///
    /// #### Parameters
    ///
    /// - `paint`: the fill
    public ShapeNode fill(ArtPaint paint) {
        fills.add(paint);
        return this;
    }

    /// Adds a stroke and returns this shape.
    ///
    /// #### Parameters
    ///
    /// - `stroke`: the stroke
    public ShapeNode stroke(StrokeStyle stroke) {
        strokes.add(stroke);
        return this;
    }

    @Override
    public double[] getLocalBounds() {
        float[] b = path.getBounds2D();
        if (b[2] <= 0 && b[3] <= 0 && strokes.isEmpty()) {
            return null;
        }
        double pad = 0;
        for (StrokeStyle s : strokes) {
            if (s.isEnabled()) {
                double w = s.getWidth();
                if (s.getJoin() == StrokeStyle.JOIN_MITER) {
                    w *= Math.max(1, s.getMiterLimit()) / 2 + 0.5;
                }
                if (s.getAlignment() == StrokeStyle.ALIGN_OUTSIDE) {
                    w *= 2;
                }
                pad = Math.max(pad, w / 2);
            }
        }
        return new double[]{b[0] - pad, b[1] - pad, b[2] + pad * 2, b[3] + pad * 2};
    }

    @Override
    public ArtNode copy() {
        ShapeNode s = new ShapeNode(new GeneralPath(path));
        for (int i = 0; i < fills.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed: no iterator allocation
            s.fills.add(fills.get(i).copy());
        }
        for (int i = 0; i < strokes.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed: no iterator allocation
            s.strokes.add(strokes.get(i).copy());
        }
        return copyBase(s);
    }
}
