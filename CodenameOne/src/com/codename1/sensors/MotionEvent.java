/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.sensors;

/// A single immutable reading from a motion sensor delivered to a
/// [MotionSensorListener]. The meaning of the three axis values depends on the
/// sensor type that produced the event (see [MotionSensorManager] for the
/// constants and their units):
///
/// - For the acceleration sensors (`TYPE_ACCELEROMETER`,
///   `TYPE_LINEAR_ACCELERATION`, `TYPE_GRAVITY`) `x`, `y` and `z` are in meters
///   per second squared.
/// - For `TYPE_GYROSCOPE` they are angular velocities in radians per second.
/// - For `TYPE_MAGNETOMETER` they are the magnetic field in microtesla.
/// - For `TYPE_ORIENTATION` `x` is the azimuth, `y` the pitch and `z` the roll,
///   all expressed in radians. Use [#getAzimuth()], [#getPitch()] and
///   [#getRoll()] for readable access.
///
/// All ports normalize their readings to the axis convention of a device held
/// upright in portrait: `x` points to the right, `y` points up and `z` points
/// out of the screen towards the user.
public final class MotionEvent {
    private final int type;
    private final float x;
    private final float y;
    private final float z;
    private final long timestamp;

    /// Creates a new immutable motion event. Application code does not normally
    /// construct these; they are delivered by the framework.
    ///
    /// #### Parameters
    ///
    /// - `type`: one of the `MotionSensorManager.TYPE_*` constants
    /// - `x`: the first axis value
    /// - `y`: the second axis value
    /// - `z`: the third axis value
    /// - `timestamp`: the event time in milliseconds (see `System.currentTimeMillis()`)
    public MotionEvent(int type, float x, float y, float z, long timestamp) {
        this.type = type;
        this.x = x;
        this.y = y;
        this.z = z;
        this.timestamp = timestamp;
    }

    /// The sensor type that produced this event, one of the
    /// `MotionSensorManager.TYPE_*` constants.
    public int getType() {
        return type;
    }

    /// The value of the first (x) axis. See the class documentation for the
    /// unit which depends on the sensor type.
    public float getX() {
        return x;
    }

    /// The value of the second (y) axis. See the class documentation for the
    /// unit which depends on the sensor type.
    public float getY() {
        return y;
    }

    /// The value of the third (z) axis. See the class documentation for the
    /// unit which depends on the sensor type.
    public float getZ() {
        return z;
    }

    /// The azimuth (rotation around the z axis) in radians. Only meaningful for
    /// `TYPE_ORIENTATION` events; equivalent to [#getX()].
    public float getAzimuth() {
        return x;
    }

    /// The pitch (rotation around the x axis) in radians. Only meaningful for
    /// `TYPE_ORIENTATION` events; equivalent to [#getY()].
    public float getPitch() {
        return y;
    }

    /// The roll (rotation around the y axis) in radians. Only meaningful for
    /// `TYPE_ORIENTATION` events; equivalent to [#getZ()].
    public float getRoll() {
        return z;
    }

    /// The Euclidean magnitude of the three axis values. For the acceleration
    /// sensors this is the total acceleration in meters per second squared.
    public double getMagnitude() {
        return Math.sqrt(((double) x) * x + ((double) y) * y + ((double) z) * z);
    }

    /// The time at which this reading was sampled, in milliseconds, compatible
    /// with `System.currentTimeMillis()`.
    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "MotionEvent[type=" + type + ", x=" + x + ", y=" + y + ", z=" + z + "]";
    }
}
