/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
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
package com.codename1.maps.vector;

/// A numeric style property that may vary with zoom. It is either a constant
/// or a set of `(zoom, value)` stops linearly interpolated between, mirroring
/// the most common form of a MapLibre GL "interpolate" expression. Used for
/// line widths and text sizes.
final class ZoomValue {

    private final double base;
    private final double[] stopZooms;
    private final double[] stopValues;

    private ZoomValue(double base, double[] zooms, double[] values) {
        this.base = base;
        this.stopZooms = zooms;
        this.stopValues = values;
    }

    /// A value that does not change with zoom.
    static ZoomValue constant(double value) {
        return new ZoomValue(value, null, null);
    }

    /// A value interpolated between the supplied ascending zoom stops.
    static ZoomValue stops(double[] zooms, double[] values) {
        if (zooms == null || values == null || zooms.length != values.length || zooms.length == 0) {
            return constant(0);
        }
        return new ZoomValue(values[0], zooms, values);
    }

    /// Evaluates the property at `zoom`, clamping outside the stop range.
    double eval(double zoom) {
        if (stopZooms == null) {
            return base;
        }
        if (zoom <= stopZooms[0]) {
            return stopValues[0];
        }
        int last = stopZooms.length - 1;
        if (zoom >= stopZooms[last]) {
            return stopValues[last];
        }
        for (int i = 0; i < last; i++) {
            double z0 = stopZooms[i];
            double z1 = stopZooms[i + 1];
            if (zoom >= z0 && zoom <= z1) {
                double t = (zoom - z0) / (z1 - z0);
                return stopValues[i] + t * (stopValues[i + 1] - stopValues[i]);
            }
        }
        return base;
    }
}
