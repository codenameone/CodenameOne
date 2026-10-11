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

import java.util.ArrayList;
import java.util.List;

public final class SVGRadialGradient extends SVGNode {
    private float cx = 0.5f, cy = 0.5f, r = 0.5f;
    private Float fx, fy;
    private boolean userSpace;
    private String href;
    private final List<SVGGradientStop> stops = new ArrayList<SVGGradientStop>();

    public float getCx() { return cx; }
    public void setCx(float cx) { this.cx = cx; }
    public float getCy() { return cy; }
    public void setCy(float cy) { this.cy = cy; }
    public float getR() { return r; }
    public void setR(float r) { this.r = r; }

    /** The focal point, where the first colour starts; null for the centre. */
    public Float getFx() { return fx; }
    public void setFx(Float fx) { this.fx = fx; }
    public Float getFy() { return fy; }
    public void setFy(Float fy) { this.fy = fy; }

    public boolean isUserSpace() { return userSpace; }
    public void setUserSpace(boolean userSpace) { this.userSpace = userSpace; }

    public String getHref() { return href; }
    public void setHref(String href) { this.href = href; }

    public List<SVGGradientStop> getStops() { return stops; }
}
