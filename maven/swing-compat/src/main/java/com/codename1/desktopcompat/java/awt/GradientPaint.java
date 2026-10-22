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
package com.codename1.desktopcompat.java.awt;

import com.codename1.desktopcompat.java.awt.geom.Point2D;

/// A linear gradient between two colors anchored at two points in user
/// space; beyond the points it holds the end colors, or repeats back and
/// forth when cyclic.
public class GradientPaint implements Paint {

    private final float x1;
    private final float y1;
    private final float x2;
    private final float y2;
    private final Color color1;
    private final Color color2;
    private final boolean cyclic;

    public GradientPaint(float x1, float y1, Color color1, float x2, float y2, Color color2) {
        this(x1, y1, color1, x2, y2, color2, false);
    }

    public GradientPaint(Point2D pt1, Color color1, Point2D pt2, Color color2) {
        this((float) pt1.getX(), (float) pt1.getY(), color1, (float) pt2.getX(), (float) pt2.getY(), color2, false);
    }

    public GradientPaint(float x1, float y1, Color color1, float x2, float y2, Color color2, boolean cyclic) {
        if (color1 == null || color2 == null) {
            throw new NullPointerException("Colors cannot be null");
        }
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
        this.color1 = color1;
        this.color2 = color2;
        this.cyclic = cyclic;
    }

    public GradientPaint(Point2D pt1, Color color1, Point2D pt2, Color color2, boolean cyclic) {
        this((float) pt1.getX(), (float) pt1.getY(), color1, (float) pt2.getX(), (float) pt2.getY(), color2, cyclic);
    }

    public Point2D getPoint1() {
        return new Point2D.Float(x1, y1);
    }

    public Color getColor1() {
        return color1;
    }

    public Point2D getPoint2() {
        return new Point2D.Float(x2, y2);
    }

    public Color getColor2() {
        return color2;
    }

    public boolean isCyclic() {
        return cyclic;
    }

    @Override
    public int getTransparency() {
        return (color1.getAlpha() & color2.getAlpha()) == 0xff ? OPAQUE : TRANSLUCENT;
    }
}
