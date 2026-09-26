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
package com.codename1.flutter.widgets;

/**
 * An immutable snapshot of a scrollable's extents — Flutter's {@code FixedScrollMetrics}.
 *
 * <p>Unlike a live {@link ScrollPosition} this describes a moment rather than tracking
 * one, which is what makes the physics testable: {@code applyPhysicsToUserOffset} is a
 * pure function of the metrics you hand it, so it can be sampled exactly without a
 * scrollable, a viewport or a frame.</p>
 */
public class FixedScrollMetrics extends ScrollMetrics {

    private com.codename1.flutter.AxisDirection axisDirection =
            com.codename1.flutter.AxisDirection.down;
    private double devicePixelRatio;

    public FixedScrollMetrics() {
        // The named setters below stand in for Dart's named parameters; until one is
        // called the extents are simply absent, as they are on a scrollable that has not
        // laid out yet.
    }

    public void minScrollExtent(double v) {
        this.minScrollExtent = v;
        this.hasContentDimensions = true;
    }

    public void maxScrollExtent(double v) {
        this.maxScrollExtent = v;
        this.hasContentDimensions = true;
    }

    public void pixels(double v) {
        this.pixels = v;
        this.hasPixels = true;
    }

    public void viewportDimension(double v) {
        this.viewportDimension = v;
        this.hasViewportDimension = true;
    }

    public void axisDirection(com.codename1.flutter.AxisDirection v) {
        this.axisDirection = v;
    }

    public void devicePixelRatio(double v) {
        this.devicePixelRatio = v;
    }

    public com.codename1.flutter.AxisDirection getAxisDirection() {
        return axisDirection;
    }

    public double getDevicePixelRatio() {
        return devicePixelRatio;
    }
}
