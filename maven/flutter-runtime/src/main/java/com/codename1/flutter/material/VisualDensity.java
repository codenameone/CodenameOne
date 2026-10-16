/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.flutter.material;

/**
 * A per-axis density adjustment applied to a component's compactness,
 * mirroring Flutter's {@code VisualDensity}. Values are in abstract density
 * units in the range [-4, 4] where 0 is the un-adjusted baseline.
 */
public class VisualDensity {

    /** The standard, un-adjusted density. */
    public static final VisualDensity standard = new VisualDensity(0.0, 0.0);
    /** A looser density for pointer-first platforms. */
    public static final VisualDensity comfortable = new VisualDensity(-1.0, -1.0);
    /** A tighter density. */
    public static final VisualDensity compact = new VisualDensity(-2.0, -2.0);
    /**
     * The platform-appropriate default (compact on desktop, standard on
     * touch). This pass has no adaptive backend, so it aliases
     * {@link #standard}.
     */
    public static final VisualDensity adaptivePlatformDensity = standard;

    private double horizontal;
    private double vertical;

    public VisualDensity() {
    }

    public VisualDensity(double horizontal, double vertical) {
        this.horizontal = horizontal;
        this.vertical = vertical;
    }

    public void horizontal(double v) {
        this.horizontal = v;
    }

    public void vertical(double v) {
        this.vertical = v;
    }

    public double getHorizontal() {
        return horizontal;
    }

    public double getVertical() {
        return vertical;
    }
}
