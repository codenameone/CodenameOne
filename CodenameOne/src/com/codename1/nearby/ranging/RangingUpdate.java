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

/// One measurement of where the peer is, delivered to
/// [RangingListener#updated] on the EDT.
///
/// Every field except the timestamp is optional, and they drop out
/// independently: a peer directly behind the phone commonly reports a
/// distance with no direction, and a peer at the edge of range reports
/// neither. Guard each read with its `has` method rather than assuming a
/// sentinel value.
///
/// ```java
/// public void updated(RangingUpdate u) {
///     if (u.hasDistance()) {
///         label.setText(String.format("%.1f m", u.getDistance(RangingUnit.METERS)));
///     }
///     if (u.hasDirection()) {
///         arrow.setAngle(u.getAzimuth());
///     }
/// }
/// ```
public final class RangingUpdate {

    private final boolean hasDistance;
    private final double distanceMeters;
    private final boolean hasDirection;
    private final double azimuth;
    private final boolean hasElevation;
    private final double elevation;
    private final float[] vector;
    private final long timestamp;

    /// Ports construct these; application code receives them through
    /// [RangingListener].
    ///
    /// #### Parameters
    ///
    /// - `hasDistance`: whether this update carries a distance
    /// - `distanceMeters`: the distance in meters, ignored when
    ///   `hasDistance` is false
    /// - `hasDirection`: whether this update carries an azimuth
    /// - `azimuth`: horizontal angle in degrees, ignored when `hasDirection`
    ///   is false
    /// - `hasElevation`: whether this update carries an elevation
    /// - `elevation`: vertical angle in degrees, ignored when `hasElevation`
    ///   is false
    /// - `vector`: the platform's raw unit direction vector, or `null`
    /// - `timestamp`: `System.currentTimeMillis()` when the port received
    ///   the measurement
    public RangingUpdate(boolean hasDistance, double distanceMeters,
            boolean hasDirection, double azimuth,
            boolean hasElevation, double elevation,
            float[] vector, long timestamp) {
        this.hasDistance = hasDistance;
        this.distanceMeters = distanceMeters;
        this.hasDirection = hasDirection;
        this.azimuth = azimuth;
        this.hasElevation = hasElevation;
        this.elevation = elevation;
        this.vector = vector == null ? null : new float[] {
            vector[0], vector[1], vector[2]
        };
        this.timestamp = timestamp;
    }

    /// `true` when this update carries a distance measurement.
    public boolean hasDistance() {
        return hasDistance;
    }

    /// The straight-line distance to the peer, in the unit you name.
    ///
    /// Undefined when [#hasDistance()] is `false` -- check first. There is
    /// no zero-argument form on purpose; see [RangingUnit].
    ///
    /// #### Parameters
    ///
    /// - `unit`: the unit to read the distance in
    ///
    /// #### Returns
    ///
    /// the distance expressed in `unit`
    public double getDistance(RangingUnit unit) {
        return unit.fromMeters(distanceMeters);
    }

    /// `true` when this update carries a horizontal direction.
    public boolean hasDirection() {
        return hasDirection;
    }

    /// The horizontal angle to the peer in degrees, in the range -180 to
    /// 180. Zero is straight ahead -- out of the top of a phone held
    /// upright -- and positive is to the right.
    ///
    /// Undefined when [#hasDirection()] is `false`.
    ///
    /// Android reports this angle directly. On iOS the platform reports a
    /// unit direction vector instead and the port converts it with
    /// `atan2(x, -z)`, which is the same convention; [#getDirectionVector()]
    /// still hands back the untouched vector for code that wants it.
    public double getAzimuth() {
        return azimuth;
    }

    /// `true` when this update carries a vertical direction.
    public boolean hasElevation() {
        return hasElevation;
    }

    /// The vertical angle to the peer in degrees, in the range -90 to 90,
    /// where positive is above the device.
    ///
    /// Undefined when [#hasElevation()] is `false`. Fewer devices report
    /// elevation than azimuth, so this drops out on its own.
    public double getElevation() {
        return elevation;
    }

    /// The platform's raw unit direction vector as `{x, y, z}` -- x to the
    /// right, y up, z toward the user, so the forward direction is negative
    /// z. iOS only; `null` everywhere else and `null` on iOS whenever
    /// [#hasDirection()] is `false`.
    ///
    /// Prefer [#getAzimuth()] and [#getElevation()], which are derived from
    /// this on iOS and reported natively on Android, so they work on both.
    /// A fresh copy is returned each call.
    public float[] getDirectionVector() {
        return vector == null ? null : new float[] {
            vector[0], vector[1], vector[2]
        };
    }

    /// `System.currentTimeMillis()` at the moment the port received this
    /// measurement. The platforms disagree on what clock their own
    /// timestamps use -- Android reports elapsed realtime nanoseconds and
    /// iOS reports nothing at all -- so this is stamped on arrival rather
    /// than translated, and is comparable only with other values from this
    /// same clock.
    public long getTimestamp() {
        return timestamp;
    }

    public String toString() {
        StringBuilder b = new StringBuilder("RangingUpdate[");
        if (hasDistance) {
            b.append("distance=").append(distanceMeters).append("m");
        } else {
            b.append("distance=none");
        }
        if (hasDirection) {
            b.append(", azimuth=").append(azimuth);
        }
        if (hasElevation) {
            b.append(", elevation=").append(elevation);
        }
        return b.append(']').toString();
    }
}
