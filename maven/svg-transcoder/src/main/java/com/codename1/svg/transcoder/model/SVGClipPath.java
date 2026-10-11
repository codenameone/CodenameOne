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

/// Definition node holding the children of a &lt;clipPath&gt; element. The
/// transcoder takes the first shape child (rect, circle, ellipse, path, ...)
/// and emits it as the clip outline. Multi-shape and nested
/// `clip-path-on-clipPath-child` forms are not supported in this
/// implementation -- they are silently flattened to the first child shape.
public final class SVGClipPath extends SVGGroup {
    private boolean objectBoundingBox;
    private boolean mask;
    private boolean alphaMask;

    /// Whether this was written as a `<mask>`: its content is painted and
    /// decides how much of the masked element shows, where a clip path only
    /// lends its outline.
    public boolean isMask() { return mask; }
    public void setMask(boolean mask) { this.mask = mask; }

    /// Whether a mask goes by the opacity of its content (`mask-type:
    /// alpha`) and not by how light it is.
    private boolean linearLuminance;
    /// Whether the luminance of a mask is worked out on linear light
    /// (`color-interpolation="linearRGB"`) and not on the colours as written.
    public boolean isLinearLuminance() { return linearLuminance; }
    public void setLinearLuminance(boolean linearLuminance) { this.linearLuminance = linearLuminance; }
    public boolean isAlphaMask() { return alphaMask; }
    public void setAlphaMask(boolean alphaMask) { this.alphaMask = alphaMask; }

    private boolean regionUserSpace;
    private float regionX = -0.1f;
    private float regionY = -0.1f;
    private float regionWidth = 1.2f;
    private float regionHeight = 1.2f;

    /// Whether the region of a mask is in the coordinates of the element it
    /// masks (`maskUnits="userSpaceOnUse"`) and not in fractions of that
    /// element's bounding box, which is what it is when nothing says.
    public boolean isRegionUserSpace() { return regionUserSpace; }
    public void setRegionUserSpace(boolean regionUserSpace) { this.regionUserSpace = regionUserSpace; }

    /// The region of a mask, outside which nothing of the masked element
    /// shows. Left alone it is the bounding box and a tenth more each way.
    public float getRegionX() { return regionX; }
    public float getRegionY() { return regionY; }
    public float getRegionWidth() { return regionWidth; }
    public float getRegionHeight() { return regionHeight; }
    public void setRegion(float x, float y, float width, float height) {
        this.regionX = x;
        this.regionY = y;
        this.regionWidth = width;
        this.regionHeight = height;
    }

    /// Whether the outline is given in fractions of the bounding box of the
    /// element it clips (`clipPathUnits="objectBoundingBox"`).
    public boolean isObjectBoundingBox() { return objectBoundingBox; }
    public void setObjectBoundingBox(boolean objectBoundingBox) { this.objectBoundingBox = objectBoundingBox; }
}
