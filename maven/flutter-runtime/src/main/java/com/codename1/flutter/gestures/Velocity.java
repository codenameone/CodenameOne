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
package com.codename1.flutter.gestures;

import com.codename1.flutter.Offset;

/**
 * A 2-D velocity in logical pixels per second — Flutter's {@code Velocity}.
 * Carried by {@link DragEndDetails} so fling handlers can read
 * {@code velocity.pixelsPerSecond}.
 */
public final class Velocity {

    /** {@code Velocity.zero} — no motion. */
    public static final Velocity zero = new Velocity(Offset.zero);

    private final Offset pixelsPerSecond;

    public Velocity() {
        this(Offset.zero);
    }

    public Velocity(Offset pixelsPerSecond) {
        this.pixelsPerSecond = pixelsPerSecond == null ? Offset.zero : pixelsPerSecond;
    }

    /** Dart's static {@code Velocity.zero} getter. */
    public static Velocity zero() {
        return zero;
    }

    public Offset pixelsPerSecond() {
        return pixelsPerSecond;
    }

    /**
     * Dart's {@code Velocity.clampMagnitude(min, max)} — returns a velocity with
     * the same direction but magnitude clamped to {@code [minValue, maxValue]}.
     */
    public Velocity clampMagnitude(double minValue, double maxValue) {
        double valueSquared = pixelsPerSecond.distanceSquared();
        if (valueSquared > maxValue * maxValue) {
            return new Velocity(pixelsPerSecond.$div(pixelsPerSecond.distance()).$times(maxValue));
        }
        if (valueSquared < minValue * minValue) {
            return new Velocity(pixelsPerSecond.$div(pixelsPerSecond.distance()).$times(minValue));
        }
        return this;
    }

    @Override
    public String toString() {
        return "Velocity(" + pixelsPerSecond + ")";
    }
}
