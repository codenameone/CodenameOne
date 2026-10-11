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
    public boolean isAlphaMask() { return alphaMask; }
    public void setAlphaMask(boolean alphaMask) { this.alphaMask = alphaMask; }

    /// Whether the outline is given in fractions of the bounding box of the
    /// element it clips (`clipPathUnits="objectBoundingBox"`).
    public boolean isObjectBoundingBox() { return objectBoundingBox; }
    public void setObjectBoundingBox(boolean objectBoundingBox) { this.objectBoundingBox = objectBoundingBox; }
}
