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
package com.codename1.svg.transcoder.model;

/// A `<marker>`: a small drawing put on the points of a line, an arrowhead
/// for one.
///
/// Its children are what is drawn. It is registered as a definition and
/// never part of the drawing. Only a renderer that reads the model draws
/// markers; the class the transcoder generates does not.
public final class SVGMarker extends SVGGroup {
    private float markerWidth = 3f;
    private float markerHeight = 3f;
    private float refX;
    private float refY;
    private String orient;
    private boolean userSpace;
    private float viewBoxX;
    private float viewBoxY;
    private float viewBoxWidth;
    private float viewBoxHeight;
    private String preserveAspectRatio;
    private boolean overflowVisible;

    /// The size of the box the marker is drawn in.
    public float getMarkerWidth() { return markerWidth; }
    public void setMarkerWidth(float v) { this.markerWidth = v; }
    public float getMarkerHeight() { return markerHeight; }
    public void setMarkerHeight(float v) { this.markerHeight = v; }

    /// The point of the marker's content that is put on the point of the line.
    public float getRefX() { return refX; }
    public void setRefX(float v) { this.refX = v; }
    public float getRefY() { return refY; }
    public void setRefY(float v) { this.refY = v; }

    /// The orient attribute as written: `auto`, `auto-start-reverse`, an
    /// angle, or null.
    public String getOrient() { return orient; }
    public void setOrient(String v) { this.orient = v; }

    /// Whether the marker is as large as it is written
    /// (`markerUnits="userSpaceOnUse"`) and not scaled by the width of the
    /// stroke it is on.
    public boolean isUserSpace() { return userSpace; }
    public void setUserSpace(boolean v) { this.userSpace = v; }

    /// The view box; its width and height are zero when there is none.
    public float getViewBoxX() { return viewBoxX; }
    public float getViewBoxY() { return viewBoxY; }
    public float getViewBoxWidth() { return viewBoxWidth; }
    public float getViewBoxHeight() { return viewBoxHeight; }
    public void setViewBox(float x, float y, float width, float height) {
        this.viewBoxX = x;
        this.viewBoxY = y;
        this.viewBoxWidth = width;
        this.viewBoxHeight = height;
    }

    /// The preserveAspectRatio attribute as written, or null.
    public String getPreserveAspectRatio() { return preserveAspectRatio; }
    public void setPreserveAspectRatio(String v) { this.preserveAspectRatio = v; }

    /// Whether what the marker draws outside its box shows
    /// (`overflow="visible"`); it is cut off otherwise.
    public boolean isOverflowVisible() { return overflowVisible; }
    public void setOverflowVisible(boolean v) { this.overflowVisible = v; }
}
