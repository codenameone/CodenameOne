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

/**
 * An {@code <image>} element: a picture from another file, or from a
 * {@code data:} address, placed in a box of the drawing. Read for a renderer
 * that can load one; the class the transcoder generates does not draw it.
 */
public final class SVGImage extends SVGNode {
    private String href;
    private float x, y, width, height;
    private String preserveAspectRatio;

    /** The address of the picture, as written. */
    public String getHref() { return href; }
    public void setHref(String href) { this.href = href; }
    public float getX() { return x; }
    public void setX(float x) { this.x = x; }
    public float getY() { return y; }
    public void setY(float y) { this.y = y; }
    /** The width of the box, or 0 for the picture's own. */
    public float getWidth() { return width; }
    public void setWidth(float width) { this.width = width; }
    /** The height of the box, or 0 for the picture's own. */
    public float getHeight() { return height; }
    public void setHeight(float height) { this.height = height; }
    /** How the picture is fitted into its box, as written; null when the element does not say. */
    public String getPreserveAspectRatio() { return preserveAspectRatio; }
    public void setPreserveAspectRatio(String v) { this.preserveAspectRatio = v; }
}
