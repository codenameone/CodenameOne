/*
 * Copyright (c) 2026 Codename One and contributors.
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * You may use this file only in compliance with that license.
 * The license notice for this subtree is available in Ports/JavaScriptPort/LICENSE.md.
 */

package com.codename1.teavm.geom;

/// Pure-Java 4x4 matrix math for the JavaScript port's perspective / camera
/// transforms. Storage and operation conventions mirror OpenGL ES
/// (column-major 16-element arrays, post-multiply composition) so the results
/// match the Android port's CN1Matrix4f (which delegates to
/// {@code android.opengl.Matrix}). This keeps the JS port's perspective output
/// pixel-comparable to the native goldens.
///
/// A matrix index is {@code col * 4 + row}; the affine embedding maps a 2D
/// point via {@code x' = m[0]*x + m[4]*y + m[12]}, {@code y' = m[1]*x + m[5]*y + m[13]}.
/// All methods are static and allocation-light; there is no JS interop here so
/// ParparVM translates the arithmetic directly.
public final class JSMatrix4 {
    private JSMatrix4() {
    }

    public static double[] identity() {
        double[] m = new double[16];
        m[0] = m[5] = m[10] = m[15] = 1;
        return m;
    }

    /// Embed a 2D affine (m00,m10,m01,m11,m02,m12) as a 4x4 matrix.
    public static double[] fromAffine(double m00, double m10, double m01,
                                      double m11, double m02, double m12) {
        double[] m = new double[16];
        m[0] = m00; m[1] = m10;
        m[4] = m01; m[5] = m11;
        m[10] = 1;
        m[12] = m02; m[13] = m12;
        m[15] = 1;
        return m;
    }

    /// result = lhs * rhs (column-major).
    public static double[] multiplyMM(double[] lhs, double[] rhs) {
        double[] r = new double[16];
        for (int c = 0; c < 4; c++) {
            for (int row = 0; row < 4; row++) {
                double sum = 0;
                for (int k = 0; k < 4; k++) {
                    sum += lhs[k * 4 + row] * rhs[c * 4 + k];
                }
                r[c * 4 + row] = sum;
            }
        }
        return r;
    }

    /// result = m * v (4-vector).
    public static double[] multiplyMV(double[] m, double[] v) {
        double[] r = new double[4];
        for (int row = 0; row < 4; row++) {
            r[row] = m[row] * v[0] + m[4 + row] * v[1]
                    + m[8 + row] * v[2] + m[12 + row] * v[3];
        }
        return r;
    }

    /// In-place post-multiply by a translation: m = m * T(x,y,z).
    public static void translateInPlace(double[] m, double x, double y, double z) {
        for (int i = 0; i < 4; i++) {
            m[12 + i] += m[i] * x + m[4 + i] * y + m[8 + i] * z;
        }
    }

    /// In-place post-multiply by a scale: m = m * S(x,y,z).
    public static void scaleInPlace(double[] m, double x, double y, double z) {
        for (int i = 0; i < 4; i++) {
            m[i] *= x;
            m[4 + i] *= y;
            m[8 + i] *= z;
        }
    }

    /// Rotation matrix about an arbitrary axis (Rodrigues), angle in radians.
    public static double[] setRotate(double angleRad, double x, double y, double z) {
        double[] m = new double[16];
        m[15] = 1;
        double s = Math.sin(angleRad);
        double c = Math.cos(angleRad);
        double len = Math.sqrt(x * x + y * y + z * z);
        if (len != 1 && len != 0) {
            double inv = 1.0 / len;
            x *= inv; y *= inv; z *= inv;
        }
        double nc = 1 - c;
        double xy = x * y, yz = y * z, zx = z * x;
        double xs = x * s, ys = y * s, zs = z * s;
        m[0] = x * x * nc + c;  m[4] = xy * nc - zs;   m[8] = zx * nc + ys;
        m[1] = xy * nc + zs;    m[5] = y * y * nc + c; m[9] = yz * nc - xs;
        m[2] = zx * nc - ys;    m[6] = yz * nc + xs;   m[10] = z * z * nc + c;
        return m;
    }

    /// Perspective projection. {@code fovyRad} is the vertical field of view in
    /// radians (matching {@code com.codename1.ui.Transform.makePerspective}).
    public static double[] perspective(double fovyRad, double aspect,
                                       double zNear, double zFar) {
        double[] m = new double[16];
        double f = 1.0 / Math.tan(fovyRad / 2.0);
        double rangeReciprocal = 1.0 / (zNear - zFar);
        m[0] = f / aspect;
        m[5] = f;
        m[10] = (zFar + zNear) * rangeReciprocal;
        m[11] = -1;
        m[14] = 2.0 * zFar * zNear * rangeReciprocal;
        return m;
    }

    public static double[] ortho(double left, double right, double bottom,
                                 double top, double near, double far) {
        double[] m = new double[16];
        double rw = 1.0 / (right - left);
        double rh = 1.0 / (top - bottom);
        double rd = 1.0 / (far - near);
        m[0] = 2.0 * rw;
        m[5] = 2.0 * rh;
        m[10] = -2.0 * rd;
        m[12] = -(right + left) * rw;
        m[13] = -(top + bottom) * rh;
        m[14] = -(far + near) * rd;
        m[15] = 1;
        return m;
    }

    /// Camera / look-at transform (mirrors {@code Matrix.setLookAtM}).
    public static double[] lookAt(double eyeX, double eyeY, double eyeZ,
                                  double centerX, double centerY, double centerZ,
                                  double upX, double upY, double upZ) {
        double fx = centerX - eyeX;
        double fy = centerY - eyeY;
        double fz = centerZ - eyeZ;
        double rlf = 1.0 / Math.sqrt(fx * fx + fy * fy + fz * fz);
        fx *= rlf; fy *= rlf; fz *= rlf;
        // s = f x up
        double sx = fy * upZ - fz * upY;
        double sy = fz * upX - fx * upZ;
        double sz = fx * upY - fy * upX;
        double rls = 1.0 / Math.sqrt(sx * sx + sy * sy + sz * sz);
        sx *= rls; sy *= rls; sz *= rls;
        // u = s x f
        double ux = sy * fz - sz * fy;
        double uy = sz * fx - sx * fz;
        double uz = sx * fy - sy * fx;
        double[] m = new double[16];
        m[0] = sx;  m[4] = sy;  m[8] = sz;
        m[1] = ux;  m[5] = uy;  m[9] = uz;
        m[2] = -fx; m[6] = -fy; m[10] = -fz;
        m[15] = 1;
        translateInPlace(m, -eyeX, -eyeY, -eyeZ);
        return m;
    }

    public static boolean isIdentity(double[] m) {
        for (int i = 0; i < 16; i++) {
            double expected = (i % 5 == 0) ? 1.0 : 0.0;
            if (m[i] != expected) {
                return false;
            }
        }
        return true;
    }
}
