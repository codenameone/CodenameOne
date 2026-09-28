/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Codename One in the LICENSE file that accompanied this code.
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
package com.codename1.ui;

import com.codename1.util.MathUtil;

/// The free response of a damped harmonic oscillator -- the curve a
/// CASpringAnimation follows -- in closed form.
///
/// `x'' + 2 * zeta * omega * x' + omega^2 * x = 0`, starting at displacement `x0`
/// with velocity `v0`; the spring rests at 0. `omega` is the natural angular
/// frequency (rad/s, `sqrt(stiffness / mass)`) and `zeta` the damping ratio
/// (`damping / (2 * sqrt(stiffness * mass))`): under 1 it oscillates, 1 is
/// critical, over 1 it creeps back without crossing.
///
/// A spring animation from `a` to `b` is `b + displacement(omega, zeta, a - b, v0, t)`.
final class DampedSpring {
    private DampedSpring() {
    }

    /// Displacement from rest at time `t` (seconds).
    static double displacement(double omega, double zeta, double x0, double v0, double t) {
        if (t <= 0) {
            return x0;
        }
        double decay = MathUtil.exp(-zeta * omega * t);
        if (zeta < 1) {
            double wd = omega * Math.sqrt(1 - zeta * zeta);
            double b = (v0 + zeta * omega * x0) / wd;
            return decay * (x0 * Math.cos(wd * t) + b * Math.sin(wd * t));
        }
        if (zeta == 1) {
            return decay * (x0 + (v0 + omega * x0) * t);
        }
        double s = omega * Math.sqrt(zeta * zeta - 1);
        double r1 = -zeta * omega + s;
        double r2 = -zeta * omega - s;
        double c2 = (v0 - r1 * x0) / (r2 - r1);
        double c1 = x0 - c2;
        return c1 * MathUtil.exp(r1 * t) + c2 * MathUtil.exp(r2 * t);
    }

    /// Velocity at time `t` (units per second).
    static double velocity(double omega, double zeta, double x0, double v0, double t) {
        if (t <= 0) {
            return v0;
        }
        double decay = MathUtil.exp(-zeta * omega * t);
        if (zeta < 1) {
            double wd = omega * Math.sqrt(1 - zeta * zeta);
            double b = (v0 + zeta * omega * x0) / wd;
            double c = Math.cos(wd * t);
            double s = Math.sin(wd * t);
            return decay * ((b * wd - zeta * omega * x0) * c - (x0 * wd + zeta * omega * b) * s);
        }
        if (zeta == 1) {
            double k = v0 + omega * x0;
            return decay * (k - omega * (x0 + k * t));
        }
        double s = omega * Math.sqrt(zeta * zeta - 1);
        double r1 = -zeta * omega + s;
        double r2 = -zeta * omega - s;
        double c2 = (v0 - r1 * x0) / (r2 - r1);
        double c1 = x0 - c2;
        return c1 * r1 * MathUtil.exp(r1 * t) + c2 * r2 * MathUtil.exp(r2 * t);
    }
}
