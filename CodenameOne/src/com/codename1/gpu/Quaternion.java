/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 */
package com.codename1.gpu;

/// Portable quaternion math used by the 3D, AR and VR APIs. Every operation
/// works on plain `float[4]` arrays laid out as `{x, y, z, w}` so it behaves
/// identically on every platform. A quaternion of this form represents a
/// rotation; `{0, 0, 0, 1}` is the identity (no rotation). Rotation matrices
/// produced by `toMatrix(float[], float[])` use the same column-major layout
/// as `Matrix4`.
public final class Quaternion {
    private Quaternion() {
    }

    /// Allocates a new identity quaternion `{0, 0, 0, 1}`.
    public static float[] identity() {
        return new float[]{0.0f, 0.0f, 0.0f, 1.0f};
    }

    /// Resets the supplied quaternion to the identity rotation.
    public static void setIdentity(float[] q) {
        q[0] = 0.0f;
        q[1] = 0.0f;
        q[2] = 0.0f;
        q[3] = 1.0f;
    }

    /// Copies the contents of `src` into `dst`. Both arrays must hold 4 floats.
    public static void copy(float[] src, float[] dst) {
        dst[0] = src[0];
        dst[1] = src[1];
        dst[2] = src[2];
        dst[3] = src[3];
    }

    /// Multiplies `a * b` (apply `b` first, then `a`) and stores the result in
    /// `dst`. `dst` may alias `a` or `b`.
    public static void multiply(float[] a, float[] b, float[] dst) {
        float ax = a[0];
        float ay = a[1];
        float az = a[2];
        float aw = a[3];
        float bx = b[0];
        float by = b[1];
        float bz = b[2];
        float bw = b[3];
        dst[0] = aw * bx + ax * bw + ay * bz - az * by;
        dst[1] = aw * by - ax * bz + ay * bw + az * bx;
        dst[2] = aw * bz + ax * by - ay * bx + az * bw;
        dst[3] = aw * bw - ax * bx - ay * by - az * bz;
    }

    /// Returns a quaternion representing a rotation of `angleRadians` around the
    /// axis `(x, y, z)`. The axis need not be normalized; a zero axis returns
    /// the identity.
    public static float[] fromAxisAngle(float angleRadians, float x, float y, float z) {
        float[] q = identity();
        setAxisAngle(q, angleRadians, x, y, z);
        return q;
    }

    /// Stores a rotation of `angleRadians` around the axis `(x, y, z)` into `q`.
    /// The axis need not be normalized; a zero axis produces the identity.
    public static void setAxisAngle(float[] q, float angleRadians, float x, float y, float z) {
        float len = (float) Math.sqrt(x * x + y * y + z * z);
        if (len == 0.0f) {
            setIdentity(q);
            return;
        }
        float half = angleRadians * 0.5f;
        float s = (float) Math.sin(half) / len;
        q[0] = x * s;
        q[1] = y * s;
        q[2] = z * s;
        q[3] = (float) Math.cos(half);
    }

    /// Normalizes `q` in place to unit length. A zero quaternion is reset to the
    /// identity.
    public static void normalize(float[] q) {
        float len = (float) Math.sqrt(q[0] * q[0] + q[1] * q[1] + q[2] * q[2] + q[3] * q[3]);
        if (len == 0.0f) {
            setIdentity(q);
            return;
        }
        float inv = 1.0f / len;
        q[0] *= inv;
        q[1] *= inv;
        q[2] *= inv;
        q[3] *= inv;
    }

    /// Stores the conjugate of `q` (the inverse rotation for a unit quaternion)
    /// into `dst`. `dst` may alias `q`.
    public static void conjugate(float[] q, float[] dst) {
        dst[0] = -q[0];
        dst[1] = -q[1];
        dst[2] = -q[2];
        dst[3] = q[3];
    }

    /// Writes the rotation matrix equivalent of the unit quaternion `q` into the
    /// 16 element column-major matrix `dst16`. The result matches
    /// `Matrix4.rotation(float, float, float, float)` for the same axis and
    /// angle.
    public static void toMatrix(float[] q, float[] dst16) {
        float x = q[0];
        float y = q[1];
        float z = q[2];
        float w = q[3];
        float x2 = x + x;
        float y2 = y + y;
        float z2 = z + z;
        float xx = x * x2;
        float yy = y * y2;
        float zz = z * z2;
        float xy = x * y2;
        float xz = x * z2;
        float yz = y * z2;
        float wx = w * x2;
        float wy = w * y2;
        float wz = w * z2;
        dst16[0] = 1.0f - (yy + zz);
        dst16[1] = xy + wz;
        dst16[2] = xz - wy;
        dst16[3] = 0.0f;
        dst16[4] = xy - wz;
        dst16[5] = 1.0f - (xx + zz);
        dst16[6] = yz + wx;
        dst16[7] = 0.0f;
        dst16[8] = xz + wy;
        dst16[9] = yz - wx;
        dst16[10] = 1.0f - (xx + yy);
        dst16[11] = 0.0f;
        dst16[12] = 0.0f;
        dst16[13] = 0.0f;
        dst16[14] = 0.0f;
        dst16[15] = 1.0f;
    }

    /// Rotates the vector stored in `xyzInOut` (3 floats) by the unit quaternion
    /// `q`, writing the result back in place.
    public static void rotateVector(float[] q, float[] xyzInOut) {
        float vx = xyzInOut[0];
        float vy = xyzInOut[1];
        float vz = xyzInOut[2];
        float qx = q[0];
        float qy = q[1];
        float qz = q[2];
        float qw = q[3];
        // t = 2 * cross(q.xyz, v); v' = v + qw * t + cross(q.xyz, t)
        float tx = 2.0f * (qy * vz - qz * vy);
        float ty = 2.0f * (qz * vx - qx * vz);
        float tz = 2.0f * (qx * vy - qy * vx);
        xyzInOut[0] = vx + qw * tx + (qy * tz - qz * ty);
        xyzInOut[1] = vy + qw * ty + (qz * tx - qx * tz);
        xyzInOut[2] = vz + qw * tz + (qx * ty - qy * tx);
    }

    /// Spherically interpolates between the unit quaternions `a` and `b` by the
    /// factor `t` in `[0, 1]`, storing the result in `dst`. Takes the shortest
    /// arc; falls back to linear interpolation when the quaternions are nearly
    /// parallel.
    public static void slerp(float[] a, float[] b, float t, float[] dst) {
        float ax = a[0];
        float ay = a[1];
        float az = a[2];
        float aw = a[3];
        float bx = b[0];
        float by = b[1];
        float bz = b[2];
        float bw = b[3];
        float dot = ax * bx + ay * by + az * bz + aw * bw;
        if (dot < 0.0f) {
            dot = -dot;
            bx = -bx;
            by = -by;
            bz = -bz;
            bw = -bw;
        }
        float wa;
        float wb;
        if (dot > 0.9995f) {
            wa = 1.0f - t;
            wb = t;
        } else {
            float theta = (float) Math.acos(dot);
            float invSin = 1.0f / (float) Math.sin(theta);
            wa = (float) Math.sin((1.0f - t) * theta) * invSin;
            wb = (float) Math.sin(t * theta) * invSin;
        }
        dst[0] = wa * ax + wb * bx;
        dst[1] = wa * ay + wb * by;
        dst[2] = wa * az + wb * bz;
        dst[3] = wa * aw + wb * bw;
        normalize(dst);
    }

    /// Integrates a body-frame angular velocity into the orientation quaternion
    /// `q`, storing the result in `dst`. `gx`, `gy` and `gz` are rotation rates
    /// in radians per second around the body X, Y and Z axes (the convention
    /// used by gyroscope sensors) and `dtSeconds` is the integration interval.
    /// `dst` may alias `q`. The result is normalized.
    public static void integrateGyro(float[] q, float gx, float gy, float gz,
                                     float dtSeconds, float[] dst) {
        float angle = (float) Math.sqrt(gx * gx + gy * gy + gz * gz) * dtSeconds;
        if (angle == 0.0f) {
            if (dst != q) {
                copy(q, dst);
            }
            return;
        }
        float[] delta = fromAxisAngle(angle, gx, gy, gz);
        // Body-frame rates compose on the right of the current orientation.
        multiply(q, delta, dst);
        normalize(dst);
    }
}
