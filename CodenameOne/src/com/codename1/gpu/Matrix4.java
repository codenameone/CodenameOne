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

/// Portable column-major 4x4 float matrix math used by the 3D API. Every
/// operation works on plain `float[16]` arrays so it behaves identically on
/// every platform without relying on native transform support. The layout
/// matches OpenGL/Metal column-major convention: element `m[c * 4 + r]` is
/// column `c`, row `r`.
public final class Matrix4 {
    private Matrix4() {
    }

    /// Allocates a new identity matrix.
    public static float[] identity() {
        float[] m = new float[16];
        setIdentity(m);
        return m;
    }

    /// Resets the supplied matrix to the identity matrix.
    public static void setIdentity(float[] m) {
        for (int i = 0; i < 16; i++) {
            m[i] = 0.0f;
        }
        m[0] = 1.0f;
        m[5] = 1.0f;
        m[10] = 1.0f;
        m[15] = 1.0f;
    }

    /// Copies the contents of `src` into `dst`. Both arrays must hold 16 floats.
    public static void copy(float[] src, float[] dst) {
        for (int i = 0; i < 16; i++) {
            dst[i] = src[i];
        }
    }

    /// Multiplies `a * b` and stores the result in `dst`. `dst` may not alias
    /// `a` or `b`.
    public static void multiply(float[] a, float[] b, float[] dst) {
        for (int c = 0; c < 4; c++) {
            int cb = c * 4;
            for (int r = 0; r < 4; r++) {
                dst[cb + r] = a[r] * b[cb]
                        + a[4 + r] * b[cb + 1]
                        + a[8 + r] * b[cb + 2]
                        + a[12 + r] * b[cb + 3];
            }
        }
    }

    /// Builds a perspective projection matrix. `fovYRadians` is the vertical
    /// field of view in radians, `aspect` the width/height ratio.
    public static float[] perspective(float fovYRadians, float aspect, float near, float far) {
        float[] m = new float[16];
        float f = (float) (1.0 / Math.tan(fovYRadians / 2.0));
        m[0] = f / aspect;
        m[5] = f;
        m[10] = (far + near) / (near - far);
        m[11] = -1.0f;
        m[14] = (2.0f * far * near) / (near - far);
        return m;
    }

    /// Builds an orthographic projection matrix.
    public static float[] ortho(float left, float right, float bottom, float top, float near, float far) {
        float[] m = new float[16];
        m[0] = 2.0f / (right - left);
        m[5] = 2.0f / (top - bottom);
        m[10] = -2.0f / (far - near);
        m[12] = -(right + left) / (right - left);
        m[13] = -(top + bottom) / (top - bottom);
        m[14] = -(far + near) / (far - near);
        m[15] = 1.0f;
        return m;
    }

    /// Builds a right-handed look-at view matrix from eye, target and up vectors.
    public static float[] lookAt(float eyeX, float eyeY, float eyeZ,
                                 float centerX, float centerY, float centerZ,
                                 float upX, float upY, float upZ) {
        float fx = centerX - eyeX;
        float fy = centerY - eyeY;
        float fz = centerZ - eyeZ;
        float rlf = 1.0f / length(fx, fy, fz);
        fx *= rlf;
        fy *= rlf;
        fz *= rlf;

        float sx = fy * upZ - fz * upY;
        float sy = fz * upX - fx * upZ;
        float sz = fx * upY - fy * upX;
        float rls = 1.0f / length(sx, sy, sz);
        sx *= rls;
        sy *= rls;
        sz *= rls;

        float ux = sy * fz - sz * fy;
        float uy = sz * fx - sx * fz;
        float uz = sx * fy - sy * fx;

        float[] m = new float[16];
        m[0] = sx;
        m[4] = sy;
        m[8] = sz;
        m[1] = ux;
        m[5] = uy;
        m[9] = uz;
        m[2] = -fx;
        m[6] = -fy;
        m[10] = -fz;
        m[12] = -(sx * eyeX + sy * eyeY + sz * eyeZ);
        m[13] = -(ux * eyeX + uy * eyeY + uz * eyeZ);
        m[14] = fx * eyeX + fy * eyeY + fz * eyeZ;
        m[15] = 1.0f;
        return m;
    }

    /// Returns a translation matrix.
    public static float[] translation(float x, float y, float z) {
        float[] m = identity();
        m[12] = x;
        m[13] = y;
        m[14] = z;
        return m;
    }

    /// Returns a scale matrix.
    public static float[] scaling(float x, float y, float z) {
        float[] m = new float[16];
        m[0] = x;
        m[5] = y;
        m[10] = z;
        m[15] = 1.0f;
        return m;
    }

    /// Returns a rotation matrix around an arbitrary axis. `angleRadians` is the
    /// rotation angle, `(x, y, z)` the rotation axis (need not be normalized).
    public static float[] rotation(float angleRadians, float x, float y, float z) {
        float len = length(x, y, z);
        if (len != 0.0f) {
            float inv = 1.0f / len;
            x *= inv;
            y *= inv;
            z *= inv;
        }
        float c = (float) Math.cos(angleRadians);
        float s = (float) Math.sin(angleRadians);
        float omc = 1.0f - c;
        float[] m = new float[16];
        m[0] = x * x * omc + c;
        m[1] = y * x * omc + z * s;
        m[2] = z * x * omc - y * s;
        m[4] = x * y * omc - z * s;
        m[5] = y * y * omc + c;
        m[6] = z * y * omc + x * s;
        m[8] = x * z * omc + y * s;
        m[9] = y * z * omc - x * s;
        m[10] = z * z * omc + c;
        m[15] = 1.0f;
        return m;
    }

    /// Computes the transpose of the upper-left 3x3 of the inverse of `m`,
    /// expanded to a 4x4. This is the correct matrix for transforming normals.
    /// Returns the identity when `m` is not invertible.
    public static float[] normalMatrix(float[] m) {
        float[] inv = new float[16];
        if (!invert(m, inv)) {
            return identity();
        }
        float[] out = identity();
        out[0] = inv[0];
        out[1] = inv[4];
        out[2] = inv[8];
        out[4] = inv[1];
        out[5] = inv[5];
        out[6] = inv[9];
        out[8] = inv[2];
        out[9] = inv[6];
        out[10] = inv[10];
        return out;
    }

    /// Inverts `m` into `dst`. Returns false (leaving `dst` untouched) when the
    /// matrix is singular.
    public static boolean invert(float[] m, float[] dst) {
        float[] inv = new float[16];
        inv[0] = m[5] * m[10] * m[15] - m[5] * m[11] * m[14] - m[9] * m[6] * m[15]
                + m[9] * m[7] * m[14] + m[13] * m[6] * m[11] - m[13] * m[7] * m[10];
        inv[4] = -m[4] * m[10] * m[15] + m[4] * m[11] * m[14] + m[8] * m[6] * m[15]
                - m[8] * m[7] * m[14] - m[12] * m[6] * m[11] + m[12] * m[7] * m[10];
        inv[8] = m[4] * m[9] * m[15] - m[4] * m[11] * m[13] - m[8] * m[5] * m[15]
                + m[8] * m[7] * m[13] + m[12] * m[5] * m[11] - m[12] * m[7] * m[9];
        inv[12] = -m[4] * m[9] * m[14] + m[4] * m[10] * m[13] + m[8] * m[5] * m[14]
                - m[8] * m[6] * m[13] - m[12] * m[5] * m[10] + m[12] * m[6] * m[9];
        inv[1] = -m[1] * m[10] * m[15] + m[1] * m[11] * m[14] + m[9] * m[2] * m[15]
                - m[9] * m[3] * m[14] - m[13] * m[2] * m[11] + m[13] * m[3] * m[10];
        inv[5] = m[0] * m[10] * m[15] - m[0] * m[11] * m[14] - m[8] * m[2] * m[15]
                + m[8] * m[3] * m[14] + m[12] * m[2] * m[11] - m[12] * m[3] * m[10];
        inv[9] = -m[0] * m[9] * m[15] + m[0] * m[11] * m[13] + m[8] * m[1] * m[15]
                - m[8] * m[3] * m[13] - m[12] * m[1] * m[11] + m[12] * m[3] * m[9];
        inv[13] = m[0] * m[9] * m[14] - m[0] * m[10] * m[13] - m[8] * m[1] * m[14]
                + m[8] * m[2] * m[13] + m[12] * m[1] * m[10] - m[12] * m[2] * m[9];
        inv[2] = m[1] * m[6] * m[15] - m[1] * m[7] * m[14] - m[5] * m[2] * m[15]
                + m[5] * m[3] * m[14] + m[13] * m[2] * m[7] - m[13] * m[3] * m[6];
        inv[6] = -m[0] * m[6] * m[15] + m[0] * m[7] * m[14] + m[4] * m[2] * m[15]
                - m[4] * m[3] * m[14] - m[12] * m[2] * m[7] + m[12] * m[3] * m[6];
        inv[10] = m[0] * m[5] * m[15] - m[0] * m[7] * m[13] - m[4] * m[1] * m[15]
                + m[4] * m[3] * m[13] + m[12] * m[1] * m[7] - m[12] * m[3] * m[5];
        inv[14] = -m[0] * m[5] * m[14] + m[0] * m[6] * m[13] + m[4] * m[1] * m[14]
                - m[4] * m[2] * m[13] - m[12] * m[1] * m[6] + m[12] * m[2] * m[5];
        inv[3] = -m[1] * m[6] * m[11] + m[1] * m[7] * m[10] + m[5] * m[2] * m[11]
                - m[5] * m[3] * m[10] - m[9] * m[2] * m[7] + m[9] * m[3] * m[6];
        inv[7] = m[0] * m[6] * m[11] - m[0] * m[7] * m[10] - m[4] * m[2] * m[11]
                + m[4] * m[3] * m[10] + m[8] * m[2] * m[7] - m[8] * m[3] * m[6];
        inv[11] = -m[0] * m[5] * m[11] + m[0] * m[7] * m[9] + m[4] * m[1] * m[11]
                - m[4] * m[3] * m[9] - m[8] * m[1] * m[7] + m[8] * m[3] * m[5];
        inv[15] = m[0] * m[5] * m[10] - m[0] * m[6] * m[9] - m[4] * m[1] * m[10]
                + m[4] * m[2] * m[9] + m[8] * m[1] * m[6] - m[8] * m[2] * m[5];

        float det = m[0] * inv[0] + m[1] * inv[4] + m[2] * inv[8] + m[3] * inv[12];
        if (det == 0.0f) {
            return false;
        }
        float invDet = 1.0f / det;
        for (int i = 0; i < 16; i++) {
            dst[i] = inv[i] * invDet;
        }
        return true;
    }

    private static float length(float x, float y, float z) {
        return (float) Math.sqrt(x * x + y * y + z * z);
    }
}
