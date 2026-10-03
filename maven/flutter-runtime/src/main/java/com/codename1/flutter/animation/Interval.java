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
package com.codename1.flutter.animation;

/**
 * A curve that is 0 until {@code begin}, 1 after {@code end}, and applies an
 * inner {@code curve} across [begin, end] — Flutter's {@code Interval}. Used to
 * stagger sub-animations off a single controller.
 */
public class Interval extends Curve {

    private final double begin;
    private final double end;
    private Curve curve = Curves.linear;

    public Interval(double begin, double end) {
        this.begin = begin;
        this.end = end;
    }

    /** Named-parameter setter for the Dart {@code curve:} argument. */
    public void curve(Curve v) {
        this.curve = v == null ? Curves.linear : v;
    }

    @Override
    protected double transformInternal(double t) {
        double span = end - begin;
        double p = span <= 0.0 ? (t < begin ? 0.0 : 1.0) : (t - begin) / span;
        if (p < 0.0) {
            p = 0.0;
        } else if (p > 1.0) {
            p = 1.0;
        }
        return curve.transform(p);
    }
}
