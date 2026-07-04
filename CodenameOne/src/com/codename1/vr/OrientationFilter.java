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
package com.codename1.vr;

import com.codename1.gpu.Quaternion;

/// Deterministic complementary sensor-fusion filter that turns raw gyroscope,
/// accelerometer and magnetometer readings into a device orientation
/// quaternion. Pure math with no platform dependencies, so the same inputs
/// always produce the same orientation on every platform.
///
/// The gyroscope drives the orientation between samples; the accelerometer
/// slowly corrects accumulated tilt drift toward gravity and the magnetometer
/// (when available) corrects yaw drift toward magnetic north. The blend is
/// controlled by `#setGyroWeight(float)`.
///
/// Conventions: the device frame has X right, Y up and Z toward the user
/// (the `com.codename1.sensors` convention); the world frame is Y up with -Z
/// forward. The output quaternion rotates device-frame vectors into the world
/// frame.
public final class OrientationFilter {
    private final float[] q = Quaternion.identity();
    private float gyroWeight = 0.98f;

    // Scratch buffers, allocated once.
    private final float[] v = new float[3];
    private final float[] correction = new float[4];

    /// Creates a filter at the identity orientation.
    public OrientationFilter() {
    }

    /// Sets the complementary blend coefficient in `(0, 1]`: the fraction of
    /// each update that trusts the integrated gyroscope over the
    /// accelerometer/magnetometer reference. Higher values are smoother but
    /// drift-correct more slowly. Default `0.98`.
    public void setGyroWeight(float w) {
        if (w <= 0f || w > 1f) {
            throw new IllegalArgumentException("gyroWeight must be in (0, 1]");
        }
        this.gyroWeight = w;
    }

    /// The complementary blend coefficient.
    public float getGyroWeight() {
        return gyroWeight;
    }

    /// Advances the filter by one sensor sample.
    ///
    /// #### Parameters
    ///
    /// - `gx`, `gy`, `gz`: gyroscope rotation rates around the device axes in
    ///   radians per second; pass zeros when no gyroscope exists
    ///
    /// - `ax`, `ay`, `az`: accelerometer reading including gravity in meters
    ///   per second squared; pass zeros (or NaN) to skip tilt correction
    ///
    /// - `mx`, `my`, `mz`: magnetometer reading in microtesla; pass NaN to
    ///   skip yaw correction
    ///
    /// - `dtSeconds`: the time since the previous update
    public void update(float gx, float gy, float gz,
                       float ax, float ay, float az,
                       float mx, float my, float mz,
                       float dtSeconds) {
        if (dtSeconds <= 0f) {
            return;
        }
        // 1. Gyro integration: body-frame rates compose on the right.
        Quaternion.integrateGyro(q, gx, gy, gz, dtSeconds, q);

        float blend = 1f - gyroWeight;

        // 2. Tilt correction toward gravity. At rest the accelerometer
        // measures the reaction to gravity along the device's world-up
        // direction, so rotating the normalized reading into the world frame
        // should give (0, 1, 0).
        float alen = (float) Math.sqrt(ax * ax + ay * ay + az * az);
        if (!isNaN(alen) && alen > 1e-6f) {
            v[0] = ax / alen;
            v[1] = ay / alen;
            v[2] = az / alen;
            Quaternion.rotateVector(q, v);
            // Rotation carrying the measured up vector onto world up.
            applyWorldCorrection(v[0], v[1], v[2], 0f, 1f, 0f, blend);
        }

        // 3. Yaw correction toward magnetic north. Only the horizontal
        // component of the field is used, and only the world-Y rotation is
        // corrected so the magnetometer can never disturb pitch or roll.
        if (!isNaN(mx) && !isNaN(my) && !isNaN(mz)) {
            float mlen = (float) Math.sqrt(mx * mx + my * my + mz * mz);
            if (mlen > 1e-6f) {
                v[0] = mx / mlen;
                v[1] = my / mlen;
                v[2] = mz / mlen;
                Quaternion.rotateVector(q, v);
                float hx = v[0];
                float hz = v[2];
                float hlen = (float) Math.sqrt(hx * hx + hz * hz);
                if (hlen > 1e-6f) {
                    // Angle of the horizontal field from world north (-Z). A
                    // yaw drift of epsilon shows up here as -epsilon, so
                    // applying the measured angle directly cancels the drift.
                    float yawError = (float) Math.atan2(hx, -hz);
                    Quaternion.setAxisAngle(correction, yawError * blend, 0f, 1f, 0f);
                    Quaternion.multiply(correction, q, q);
                    Quaternion.normalize(q);
                }
            }
        }
    }

    /// Applies a world-frame rotation moving `from` a fraction of the way
    /// toward `to`.
    private void applyWorldCorrection(float fx, float fy, float fz,
                                      float tx, float ty, float tz, float fraction) {
        float cx = fy * tz - fz * ty;
        float cy = fz * tx - fx * tz;
        float cz = fx * ty - fy * tx;
        float clen = (float) Math.sqrt(cx * cx + cy * cy + cz * cz);
        float dot = fx * tx + fy * ty + fz * tz;
        if (clen < 1e-6f) {
            return;
        }
        float angle = (float) Math.atan2(clen, dot);
        Quaternion.setAxisAngle(correction, angle * fraction, cx, cy, cz);
        Quaternion.multiply(correction, q, q);
        Quaternion.normalize(q);
    }

    private static boolean isNaN(float f) {
        return f != f;
    }

    /// Copies the current orientation quaternion into `quatOut4` as
    /// `{x, y, z, w}`.
    public void getOrientation(float[] quatOut4) {
        Quaternion.copy(q, quatOut4);
    }

    /// The current orientation quaternion as a newly allocated array.
    public float[] getOrientation() {
        float[] out = new float[4];
        getOrientation(out);
        return out;
    }

    /// Resets the filter to the identity orientation.
    public void reset() {
        Quaternion.setIdentity(q);
    }

    /// Rotates the orientation around world up so the current forward
    /// direction becomes the new "straight ahead", keeping pitch and roll.
    /// Use to let the user recenter the view.
    public void recenterYaw() {
        // The device -Z axis in world space is the current forward direction.
        v[0] = 0f;
        v[1] = 0f;
        v[2] = -1f;
        Quaternion.rotateVector(q, v);
        float hlen = (float) Math.sqrt(v[0] * v[0] + v[2] * v[2]);
        if (hlen < 1e-6f) {
            // Looking straight up or down; yaw is undefined.
            return;
        }
        // A device yaw of theta puts the forward vector at atan2 angle
        // -theta, so applying the measured angle directly cancels the yaw.
        float yaw = (float) Math.atan2(v[0], -v[2]);
        Quaternion.setAxisAngle(correction, yaw, 0f, 1f, 0f);
        Quaternion.multiply(correction, q, q);
        Quaternion.normalize(q);
    }
}
