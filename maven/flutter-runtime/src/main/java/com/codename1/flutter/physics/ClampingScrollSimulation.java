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
 * A friction {@link Simulation} clamped to a scroll range — Flutter's
 * {@code ClampingScrollSimulation}. Models the deceleration of a fling on the
 * Android-style clamping scroll physics. This pass captures the parameters and
 * reports the resting {@code position}.
 */
public class ClampingScrollSimulation extends Simulation {

    private double position;
    private double velocity;
    private Double friction;

    public ClampingScrollSimulation() {
    }

    public void position(double v) {
        this.position = v;
    }

    public void velocity(double v) {
        this.velocity = v;
    }

    public void friction(Double v) {
        this.friction = v;
    }

    public double getPosition() {
        return position;
    }

    public double getVelocity() {
        return velocity;
    }

    public Double getFriction() {
        return friction;
    }

    @Override
    public double x(double time) {
        return position;
    }

    @Override
    public double dx(double time) {
        return 0.0;
    }

    @Override
    public boolean isDone(double time) {
        return true;
    }
}
