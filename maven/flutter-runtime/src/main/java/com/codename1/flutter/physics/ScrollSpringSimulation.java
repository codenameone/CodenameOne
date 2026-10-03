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
 * A spring {@link Simulation} that carries a scrollable from a start offset to
 * an end offset under a {@link SpringDescription} — Flutter's
 * {@code ScrollSpringSimulation}. Used by the home carousel's snapping physics
 * to settle onto an item after a fling. The spring is captured for API shape;
 * this pass models the endpoint (the settled position is {@code end}).
 *
 * @see com.codename1.flutter.widgets.ScrollPhysics
 */
public class ScrollSpringSimulation extends Simulation {

    private final Object spring;
    private final double start;
    private final double end;
    private final double velocity;

    public ScrollSpringSimulation(Object spring, double start, double end, double velocity) {
        this.spring = spring;
        this.start = start;
        this.end = end;
        this.velocity = velocity;
    }

    public Object getSpring() {
        return spring;
    }

    public double getStart() {
        return start;
    }

    public double getEnd() {
        return end;
    }

    public double getVelocity() {
        return velocity;
    }

    @Override
    public double x(double time) {
        return end;
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
