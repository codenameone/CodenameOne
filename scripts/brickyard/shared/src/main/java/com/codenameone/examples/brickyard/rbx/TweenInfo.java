/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codenameone.examples.brickyard.rbx;

import com.codenameone.examples.brickyard.luau.Interp;
import com.codename1.util.MathUtil;

/// How a tween runs: duration, easing, repeats, reversal and delay.
public final class TweenInfo extends RbxType {
    public final double time;
    public final String style;
    public final String direction;
    public final int repeatCount;
    public final boolean reverses;
    public final double delay;

    public TweenInfo(double time, String style, String direction, int repeatCount, boolean reverses, double delay) {
        this.time = time;
        this.style = style;
        this.direction = direction;
        this.repeatCount = repeatCount;
        this.reverses = reverses;
        this.delay = delay;
    }

    @Override
    public String typeName() {
        return "TweenInfo";
    }

    @Override
    public Object get(Interp in, String k) {
        switch (k) {
            case "Time":
                return Args.n(time);
            case "EasingStyle":
                return Enums.item("EasingStyle", style);
            case "EasingDirection":
                return Enums.item("EasingDirection", direction);
            case "RepeatCount":
                return Args.n(repeatCount);
            case "Reverses":
                return Boolean.valueOf(reverses);
            case "DelayTime":
                return Args.n(delay);
            default:
                return super.get(in, k);
        }
    }

    /// The eased value of linear progress `t` in 0..1.
    public double ease(double t) {
        if ("In".equals(direction)) {
            return easeIn(t);
        }
        if ("InOut".equals(direction)) {
            return t < 0.5 ? easeIn(t * 2) / 2 : 1 - easeIn((1 - t) * 2) / 2;
        }
        return 1 - easeIn(1 - t);
    }

    private double easeIn(double t) {
        switch (style) {
            case "Linear":
                return t;
            case "Sine":
                return 1 - Math.cos(t * Math.PI / 2);
            case "Back": {
                double s = 1.70158;
                return t * t * ((s + 1) * t - s);
            }
            case "Quart":
                return t * t * t * t;
            case "Quint":
                return t * t * t * t * t;
            case "Cubic":
                return t * t * t;
            case "Exponential":
                return t == 0 ? 0 : MathUtil.pow(2, 10 * (t - 1));
            case "Circular":
                return 1 - Math.sqrt(1 - t * t);
            case "Bounce":
                return 1 - bounceOut(1 - t);
            case "Elastic":
                return t == 0 || t == 1 ? t
                        : -MathUtil.pow(2, 10 * (t - 1)) * Math.sin((t - 1.075) * 2 * Math.PI / 0.3);
            default:
                return t * t;
        }
    }

    private static double bounceOut(double t) {
        double n = 7.5625;
        double d = 2.75;
        if (t < 1 / d) {
            return n * t * t;
        }
        if (t < 2 / d) {
            t -= 1.5 / d;
            return n * t * t + 0.75;
        }
        if (t < 2.5 / d) {
            t -= 2.25 / d;
            return n * t * t + 0.9375;
        }
        t -= 2.625 / d;
        return n * t * t + 0.984375;
    }
}
