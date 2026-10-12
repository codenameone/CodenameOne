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

import com.codename1.svg.transcoder.parser.SVGTransform;

/// A `<pattern>`: a tile that is drawn over and over to fill a shape.
///
/// Its children are the tile's content. It is registered as a definition
/// and never part of the drawing. Only a renderer that reads the model
/// paints with one; the class the transcoder generates does not.
///
/// What the element leaves out it takes from the pattern its `href` names,
/// so [#isGiven(String)] says which of these were written: `x`, `y`,
/// `width`, `height`, `patternUnits`, `patternContentUnits`,
/// `patternTransform`, `viewBox` and `preserveAspectRatio`.
public final class SVGPattern extends SVGGroup {
    private float x;
    private float y;
    private float width;
    private float height;
    private boolean userSpace;
    private boolean contentObjectBoundingBox;
    private SVGTransform patternTransform;
    private float viewBoxX;
    private float viewBoxY;
    private float viewBoxWidth;
    private float viewBoxHeight;
    private String preserveAspectRatio;
    private String href;

    /// The tile, in fractions of the filled shape's bounding box unless
    /// [#isUserSpace()].
    public float getX() { return x; }
    public void setX(float x) { this.x = x; }
    public float getY() { return y; }
    public void setY(float y) { this.y = y; }
    public float getWidth() { return width; }
    public void setWidth(float width) { this.width = width; }
    public float getHeight() { return height; }
    public void setHeight(float height) { this.height = height; }

    /// Whether the tile is given in the coordinates of the filled shape
    /// (`patternUnits="userSpaceOnUse"`).
    public boolean isUserSpace() { return userSpace; }
    public void setUserSpace(boolean userSpace) { this.userSpace = userSpace; }

    /// Whether the content is drawn in fractions of the filled shape's
    /// bounding box (`patternContentUnits="objectBoundingBox"`). A view box
    /// takes its place.
    public boolean isContentObjectBoundingBox() { return contentObjectBoundingBox; }
    public void setContentObjectBoundingBox(boolean v) { this.contentObjectBoundingBox = v; }

    /// The patternTransform, or null.
    public SVGTransform getPatternTransform() { return patternTransform; }
    public void setPatternTransform(SVGTransform t) { this.patternTransform = t; }

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

    /// The id of the pattern this one is derived from, or null.
    public String getHref() { return href; }
    public void setHref(String href) { this.href = href; }
}
