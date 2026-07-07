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
package com.codename1.ar;

import com.codename1.gpu.Quaternion;

/// An immutable rigid transform - a rotation followed by a translation - that
/// positions something in AR world space. Poses use the AR coordinate
/// convention shared by ARKit and ARCore: units are meters, the coordinate
/// system is right-handed with Y pointing up and -Z pointing forward from the
/// initial camera direction.
///
/// The rotation is stored as a unit quaternion `(qx, qy, qz, qw)` compatible
/// with `com.codename1.gpu.Quaternion`; the translation is `(tx, ty, tz)`.
/// `toMatrix(float[])` produces the equivalent column-major 4x4 matrix, ready
/// for `com.codename1.gpu.Matrix4` math.
public final class ARPose {
    /// The identity pose: no rotation, located at the world origin.
    public static final ARPose IDENTITY = new ARPose(0, 0, 0, 0, 0, 0, 1);

    private final float tx;
    private final float ty;
    private final float tz;
    private final float qx;
    private final float qy;
    private final float qz;
    private final float qw;

    /// Creates a pose from a translation and a rotation quaternion. The
    /// quaternion is normalized defensively; a zero quaternion becomes the
    /// identity rotation.
    ///
    /// #### Parameters
    ///
    /// - `tx`, `ty`, `tz`: the translation in meters
    ///
    /// - `qx`, `qy`, `qz`, `qw`: the rotation quaternion
    public ARPose(float tx, float ty, float tz, float qx, float qy, float qz, float qw) {
        this.tx = tx;
        this.ty = ty;
        this.tz = tz;
        float len = (float) Math.sqrt(qx * qx + qy * qy + qz * qz + qw * qw);
        if (len == 0.0f) {
            this.qx = 0;
            this.qy = 0;
            this.qz = 0;
            this.qw = 1;
        } else {
            float inv = 1.0f / len;
            this.qx = qx * inv;
            this.qy = qy * inv;
            this.qz = qz * inv;
            this.qw = qw * inv;
        }
    }

    /// Extracts a pose from a column-major 4x4 transform matrix whose upper
    /// left 3x3 is a pure rotation (no scale or shear).
    ///
    /// #### Parameters
    ///
    /// - `m16`: the column-major matrix, 16 floats
    ///
    /// #### Returns
    ///
    /// the equivalent pose
    public static ARPose fromMatrix(float[] m16) {
        float m00 = m16[0];
        float m10 = m16[1];
        float m20 = m16[2];
        float m01 = m16[4];
        float m11 = m16[5];
        float m21 = m16[6];
        float m02 = m16[8];
        float m12 = m16[9];
        float m22 = m16[10];
        float trace = m00 + m11 + m22;
        float x;
        float y;
        float z;
        float w;
        if (trace > 0.0f) {
            float s = (float) Math.sqrt(trace + 1.0f) * 2.0f;
            w = 0.25f * s;
            x = (m21 - m12) / s;
            y = (m02 - m20) / s;
            z = (m10 - m01) / s;
        } else if (m00 > m11 && m00 > m22) {
            float s = (float) Math.sqrt(1.0f + m00 - m11 - m22) * 2.0f;
            w = (m21 - m12) / s;
            x = 0.25f * s;
            y = (m01 + m10) / s;
            z = (m02 + m20) / s;
        } else if (m11 > m22) {
            float s = (float) Math.sqrt(1.0f + m11 - m00 - m22) * 2.0f;
            w = (m02 - m20) / s;
            x = (m01 + m10) / s;
            y = 0.25f * s;
            z = (m12 + m21) / s;
        } else {
            float s = (float) Math.sqrt(1.0f + m22 - m00 - m11) * 2.0f;
            w = (m10 - m01) / s;
            x = (m02 + m20) / s;
            y = (m12 + m21) / s;
            z = 0.25f * s;
        }
        return new ARPose(m16[12], m16[13], m16[14], x, y, z, w);
    }

    /// The X component of the translation in meters.
    public float getTx() {
        return tx;
    }

    /// The Y component of the translation in meters.
    public float getTy() {
        return ty;
    }

    /// The Z component of the translation in meters.
    public float getTz() {
        return tz;
    }

    /// The X component of the rotation quaternion.
    public float getQx() {
        return qx;
    }

    /// The Y component of the rotation quaternion.
    public float getQy() {
        return qy;
    }

    /// The Z component of the rotation quaternion.
    public float getQz() {
        return qz;
    }

    /// The W component of the rotation quaternion.
    public float getQw() {
        return qw;
    }

    /// Writes this pose as a column-major 4x4 transform matrix into `out16`,
    /// compatible with `com.codename1.gpu.Matrix4`.
    ///
    /// #### Parameters
    ///
    /// - `out16`: the destination array, 16 floats
    public void toMatrix(float[] out16) {
        float[] q = {qx, qy, qz, qw};
        Quaternion.toMatrix(q, out16);
        out16[12] = tx;
        out16[13] = ty;
        out16[14] = tz;
    }

    /// Returns this pose as a newly allocated column-major 4x4 transform
    /// matrix.
    public float[] toMatrix() {
        float[] m = new float[16];
        toMatrix(m);
        return m;
    }

    /// Composes this pose with a pose expressed in this pose's local frame,
    /// returning `this * local`. Use it to convert an offset relative to an
    /// anchor into world space.
    ///
    /// #### Parameters
    ///
    /// - `local`: the pose in this pose's local coordinate frame
    ///
    /// #### Returns
    ///
    /// the composed pose in the frame this pose is expressed in
    public ARPose transform(ARPose local) {
        float[] q = {qx, qy, qz, qw};
        float[] lq = {local.qx, local.qy, local.qz, local.qw};
        float[] rq = new float[4];
        Quaternion.multiply(q, lq, rq);
        float[] t = {local.tx, local.ty, local.tz};
        Quaternion.rotateVector(q, t);
        return new ARPose(tx + t[0], ty + t[1], tz + t[2], rq[0], rq[1], rq[2], rq[3]);
    }

    /// Transforms the point stored in `xyzInOut` (3 floats) from this pose's
    /// local frame to the frame this pose is expressed in, writing the result
    /// back in place.
    ///
    /// #### Parameters
    ///
    /// - `xyzInOut`: the point to transform, modified in place
    public void transformPoint(float[] xyzInOut) {
        float[] q = {qx, qy, qz, qw};
        Quaternion.rotateVector(q, xyzInOut);
        xyzInOut[0] += tx;
        xyzInOut[1] += ty;
        xyzInOut[2] += tz;
    }

    @Override
    public String toString() {
        return "ARPose(t=[" + tx + ", " + ty + ", " + tz + "], q=["
                + qx + ", " + qy + ", " + qz + ", " + qw + "])";
    }
}
