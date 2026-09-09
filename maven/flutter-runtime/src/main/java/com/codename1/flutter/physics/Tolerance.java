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
package com.codename1.flutter.physics;

/**
 * The error tolerances a physics simulation settles within — Flutter's
 * {@code Tolerance}: {@code distance}, {@code time} and {@code velocity}
 * thresholds below which a simulation is considered to have come to rest. The
 * home carousel physics compares the fling velocity against
 * {@link #velocity()}.
 */
public class Tolerance {

    /** Flutter's {@code Tolerance.defaultTolerance}. */
    public static final Tolerance defaultTolerance = new Tolerance(1e-3, 1e-3, 1e-3);

    private double distance = 1e-3;
    private double time = 1e-3;
    private double velocity = 1e-3;

    public Tolerance() {
    }

    public Tolerance(double distance, double time, double velocity) {
        this.distance = distance;
        this.time = time;
        this.velocity = velocity;
    }

    public void distance(double v) {
        this.distance = v;
    }

    public void time(double v) {
        this.time = v;
    }

    public void velocity(double v) {
        this.velocity = v;
    }

    public double distance() {
        return distance;
    }

    public double time() {
        return time;
    }

    public double velocity() {
        return velocity;
    }
}
