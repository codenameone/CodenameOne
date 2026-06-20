/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Codename One in the LICENSE file that accompanied this code.
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
package com.codename1.maps;

import java.util.ArrayList;
import java.util.List;

/// A connected sequence of line segments drawn on a map. Add one through
/// [MapSurface#addPolyline(Polyline)].
public final class Polyline extends MapObject {

    private final List points;
    private int strokeColor = 0x2196f3;
    private int strokeWidth = 4;
    private int strokeAlpha = 255;
    private boolean visible = true;

    /// Creates an empty polyline; append vertices with [#addPoint(LatLng)].
    public Polyline() {
        points = new ArrayList();
    }

    /// Creates a polyline through the supplied vertices (defensively copied).
    public Polyline(LatLng[] pts) {
        points = new ArrayList();
        if (pts != null) {
            for (int i = 0; i < pts.length; i++) {
                points.add(pts[i]);
            }
        }
    }

    /// Appends a vertex.
    public Polyline addPoint(LatLng point) {
        points.add(point);
        return this;
    }

    /// The live list of vertices ([LatLng]).
    public List getPoints() {
        return points;
    }

    /// The stroke color as 0xRRGGBB.
    public int getStrokeColor() {
        return strokeColor;
    }

    /// Sets the stroke color as 0xRRGGBB.
    public Polyline setStrokeColor(int strokeColor) {
        this.strokeColor = strokeColor;
        return this;
    }

    /// The stroke width in pixels.
    public int getStrokeWidth() {
        return strokeWidth;
    }

    /// Sets the stroke width in pixels.
    public Polyline setStrokeWidth(int strokeWidth) {
        this.strokeWidth = strokeWidth;
        return this;
    }

    /// The stroke opacity in [0,255].
    public int getStrokeAlpha() {
        return strokeAlpha;
    }

    /// Sets the stroke opacity in [0,255].
    public Polyline setStrokeAlpha(int strokeAlpha) {
        this.strokeAlpha = strokeAlpha;
        return this;
    }

    /// Whether the polyline is rendered.
    public boolean isVisible() {
        return visible;
    }

    /// Shows or hides the polyline.
    public Polyline setVisible(boolean visible) {
        this.visible = visible;
        return this;
    }
}
