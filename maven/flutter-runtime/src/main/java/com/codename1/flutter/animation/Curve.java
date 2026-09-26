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
 * A mapping of the unit interval to itself — Flutter's {@code Curve}. Concrete
 * curves ({@link Cubic}, {@link Interval}, and the {@link Curves} constants)
 * override {@link #transform}. {@code t} is clamped to [0, 1].
 */
public abstract class Curve {

    /** Maps {@code t} (0..1) to an eased value; endpoints are pinned to 0 and 1. */
    public double transform(double t) {
        if (t <= 0.0) {
            return 0.0;
        }
        if (t >= 1.0) {
            return 1.0;
        }
        return transformInternal(t);
    }

    /** The eased value strictly inside (0, 1). */
    protected abstract double transformInternal(double t);

    /** The curve that runs this one in reverse ({@code 1 - curve(1 - t)}). */
    public Curve flipped() {
        return new FlippedCurve(this);
    }
}
