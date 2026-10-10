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

import java.util.HashMap;
import java.util.Map;

/** Top-level &lt;svg&gt; document. */
public final class SVGDocument extends SVGGroup {
    private float viewBoxX;
    private float viewBoxY;
    private float viewBoxWidth;
    private float viewBoxHeight;
    private float width;
    private float height;
    private final Map<String, SVGNode> definitions = new HashMap<String, SVGNode>();

    public float getViewBoxX() { return viewBoxX; }
    public void setViewBoxX(float v) { this.viewBoxX = v; }

    public float getViewBoxY() { return viewBoxY; }
    public void setViewBoxY(float v) { this.viewBoxY = v; }

    public float getViewBoxWidth() { return viewBoxWidth; }
    public void setViewBoxWidth(float v) { this.viewBoxWidth = v; }

    public float getViewBoxHeight() { return viewBoxHeight; }
    public void setViewBoxHeight(float v) { this.viewBoxHeight = v; }

    public float getWidth() { return width; }
    public void setWidth(float w) { this.width = w; }

    public float getHeight() { return height; }
    public void setHeight(float h) { this.height = h; }

    public Map<String, SVGNode> getDefinitions() { return definitions; }

    private boolean viewBoxDeclared;
    private String preserveAspectRatio;

    /** Whether the document has a viewBox attribute of its own, as opposed to the one derived from its size. */
    public boolean isViewBoxDeclared() { return viewBoxDeclared; }
    public void setViewBoxDeclared(boolean v) { this.viewBoxDeclared = v; }

    /** The preserveAspectRatio attribute as written, or null. */
    public String getPreserveAspectRatio() { return preserveAspectRatio; }
    public void setPreserveAspectRatio(String v) { this.preserveAspectRatio = v; }
}
