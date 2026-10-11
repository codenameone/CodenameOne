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

/// An `<svg>` inside the drawing: a box of its own, in which its content is
/// laid out afresh and outside which none of it shows.
///
/// The content is fitted to the box by the view box and
/// `preserveAspectRatio`, as the whole drawing is to an image. Without a
/// view box it is only moved to the box's corner.
///
/// This is not a group, so that the class the transcoder generates, which
/// knows nothing of the box, does not draw the content somewhere it is not.
/// Only a renderer that reads the model draws it.
public final class SVGViewport extends SVGNode {
    private final SVGGroup content = new SVGGroup();
    private float x;
    private float y;
    private float width;
    private float height;
    private float viewBoxX;
    private float viewBoxY;
    private float viewBoxWidth;
    private float viewBoxHeight;
    private String preserveAspectRatio;

    /// What is drawn in the box.
    public SVGGroup getContent() { return content; }

    public float getX() { return x; }
    public void setX(float x) { this.x = x; }
    public float getY() { return y; }
    public void setY(float y) { this.y = y; }
    public float getWidth() { return width; }
    public void setWidth(float width) { this.width = width; }
    public float getHeight() { return height; }
    public void setHeight(float height) { this.height = height; }

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
}
