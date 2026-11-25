/*
 * MIT License
 *
 * Copyright (c) 2020 Rive
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 * Java rewrite of rive-runtime for Codename One.
 * Codename One modifications Copyright (c) 2026 Codename One.
 */
package com.codename1.impl.rive;

import com.codename1.util.MathUtil;

/// A 2D affine matrix in Rive's layout `[xx, xy, yx, yy, tx, ty]`, mapping a
/// point to `(xx*x + yx*y + tx, xy*x + yy*y + ty)`. Mutable, so the per-frame
/// update allocates nothing.
public final class Mat2D {
    public final float[] v = {1f, 0f, 0f, 1f, 0f, 0f};

    public Mat2D identity() {
        return set(1f, 0f, 0f, 1f, 0f, 0f);
    }

    public Mat2D set(Mat2D o) {
        System.arraycopy(o.v, 0, v, 0, 6);
        return this;
    }

    public Mat2D set(float xx, float xy, float yx, float yy, float tx, float ty) {
        v[0] = xx;
        v[1] = xy;
        v[2] = yx;
        v[3] = yy;
        v[4] = tx;
        v[5] = ty;
        return this;
    }

    /// Rotation, then non-uniform scale, then translation: how Rive composes a
    /// transform component's local matrix.
    public Mat2D compose(float x, float y, float radians, float scaleX, float scaleY) {
        float c = (float) Math.cos(radians);
        float s = (float) Math.sin(radians);
        return set(c * scaleX, s * scaleX, -s * scaleY, c * scaleY, x, y);
    }

    /// `this = a * b`; safe when `this` is `a` or `b`.
    public Mat2D multiply(Mat2D a, Mat2D b) {
        float[] x = a.v;
        float[] y = b.v;
        return set(x[0] * y[0] + x[2] * y[1],
                x[1] * y[0] + x[3] * y[1],
                x[0] * y[2] + x[2] * y[3],
                x[1] * y[2] + x[3] * y[3],
                x[0] * y[4] + x[2] * y[5] + x[4],
                x[1] * y[4] + x[3] * y[5] + x[5]);
    }

    /// Writes the inverse of `m` here. A singular `m` leaves identity and answers false.
    public boolean invert(Mat2D m) {
        float[] a = m.v;
        float det = a[0] * a[3] - a[1] * a[2];
        if (det == 0f) {
            identity();
            return false;
        }
        float d = 1f / det;
        set(a[3] * d, -a[1] * d, -a[2] * d, a[0] * d,
                (a[2] * a[5] - a[3] * a[4]) * d,
                (a[1] * a[4] - a[0] * a[5]) * d);
        return true;
    }

    public float mapX(float x, float y) {
        return v[0] * x + v[2] * y + v[4];
    }

    public float mapY(float x, float y) {
        return v[1] * x + v[3] * y + v[5];
    }

    public float scaleX() {
        return (float) Math.sqrt(v[0] * v[0] + v[1] * v[1]);
    }

    public float scaleY() {
        float sx = scaleX();
        if (sx == 0f) {
            return (float) Math.sqrt(v[2] * v[2] + v[3] * v[3]);
        }
        return (v[0] * v[3] - v[1] * v[2]) / sx;
    }

    public float rotation() {
        return (float) MathUtil.atan2(v[1], v[0]);
    }

    /// Decomposes into `out = {x, y, rotation, scaleX, scaleY, skew}` the way
    /// Rive's `TransformComponents` does, for constraints that blend transforms.
    public void decompose(float[] out) {
        float m0 = v[0];
        float m1 = v[1];
        float m2 = v[2];
        float m3 = v[3];
        float rotation = (float) MathUtil.atan2(m1, m0);
        float denom = m0 * m0 + m1 * m1;
        float scaleX = (float) Math.sqrt(denom);
        float scaleY = scaleX != 0f ? (m0 * m3 - m2 * m1) / scaleX : 0f;
        float skewX = (float) MathUtil.atan2(m0 * m2 + m1 * m3, denom);
        out[0] = v[4];
        out[1] = v[5];
        out[2] = rotation;
        out[3] = scaleX;
        out[4] = scaleY;
        out[5] = skewX;
    }

    /// The inverse of [#decompose(float[])].
    public Mat2D recompose(float[] c) {
        compose(c[0], c[1], c[2], c[3], c[4]);
        float sk = c[5];
        if (sk != 0f) {
            v[2] = v[0] * sk + v[2];
            v[3] = v[1] * sk + v[3];
        }
        return this;
    }
}
