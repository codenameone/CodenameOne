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

import java.util.ArrayList;
import java.util.List;

public final class SVGLinearGradient extends SVGNode {
    private float x1 = 0f, y1 = 0f, x2 = 1f, y2 = 0f;
    private boolean userSpace;
    private SVGTransform gradientTransform;
    private String href;
    private final List<SVGGradientStop> stops = new ArrayList<SVGGradientStop>();

    public float getX1() { return x1; }
    public void setX1(float v) { this.x1 = v; }
    public float getY1() { return y1; }
    public void setY1(float v) { this.y1 = v; }
    public float getX2() { return x2; }
    public void setX2(float v) { this.x2 = v; }
    public float getY2() { return y2; }
    public void setY2(float v) { this.y2 = v; }

    /** The gradientTransform, or null. Only a renderer that reads the model applies it. */
    public SVGTransform getGradientTransform() { return gradientTransform; }
    public void setGradientTransform(SVGTransform t) { this.gradientTransform = t; }

    public boolean isUserSpace() { return userSpace; }
    public void setUserSpace(boolean userSpace) { this.userSpace = userSpace; }

    private String spreadMethod;

    /** The spreadMethod attribute as written (pad, reflect or repeat), or null. Only a renderer that reads the model applies it. */
    public String getSpreadMethod() { return spreadMethod; }
    public void setSpreadMethod(String v) { this.spreadMethod = v; }

    public String getHref() { return href; }
    public void setHref(String href) { this.href = href; }

    public List<SVGGradientStop> getStops() { return stops; }
}
