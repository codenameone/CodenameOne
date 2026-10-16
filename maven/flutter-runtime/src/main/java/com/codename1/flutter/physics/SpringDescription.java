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
 * Structural parameters of a spring — Flutter's {@code SpringDescription}:
 * {@code mass}, {@code stiffness} and {@code damping}. Fed to a
 * {@code ScrollSpringSimulation} to model overscroll / fling settling.
 */
public class SpringDescription {

    private double mass;
    private double stiffness;
    private double damping;

    public SpringDescription() {
    }

    public void mass(double v) {
        this.mass = v;
    }

    public void stiffness(double v) {
        this.stiffness = v;
    }

    public void damping(double v) {
        this.damping = v;
    }

    public double getMass() {
        return mass;
    }

    public double getStiffness() {
        return stiffness;
    }

    public double getDamping() {
        return damping;
    }

    /**
     * {@code SpringDescription.withDampingRatio}: builds a spring from a mass,
     * stiffness and damping ratio (1.0 = critically damped).
     */
    public static SpringDescription withDampingRatio(double mass, double stiffness, double ratio) {
        SpringDescription s = new SpringDescription();
        s.mass(mass);
        s.stiffness(stiffness);
        s.damping(ratio * 2.0 * Math.sqrt(mass * stiffness));
        return s;
    }
}
