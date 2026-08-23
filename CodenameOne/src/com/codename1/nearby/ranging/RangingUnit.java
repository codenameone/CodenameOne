/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
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
package com.codename1.nearby.ranging;

/// The unit a distance is read in.
///
/// There is deliberately no zero-argument distance getter on
/// [RangingUpdate]: the caller always names the unit. Both platforms report
/// meters natively, so a bare `getDistance()` would have been correct on
/// every device and still wrong in every app that displayed it as feet.
public enum RangingUnit {
    /// Meters, the unit both platforms measure in.
    METERS(1.0),

    /// International feet, 0.3048 m exactly.
    FEET(0.3048),

    /// Centimeters.
    CENTIMETERS(0.01),

    /// International inches, 0.0254 m exactly.
    INCHES(0.0254);

    private final double metersPerUnit;

    private RangingUnit(double metersPerUnit) {
        this.metersPerUnit = metersPerUnit;
    }

    /// Converts a distance expressed in meters into this unit.
    ///
    /// #### Parameters
    ///
    /// - `meters`: the distance in meters
    ///
    /// #### Returns
    ///
    /// the same distance expressed in this unit
    public double fromMeters(double meters) {
        return meters / metersPerUnit;
    }

    /// Converts a distance expressed in this unit into meters.
    ///
    /// #### Parameters
    ///
    /// - `value`: the distance in this unit
    ///
    /// #### Returns
    ///
    /// the same distance in meters
    public double toMeters(double value) {
        return value * metersPerUnit;
    }
}
