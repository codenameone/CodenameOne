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
package android.graphics;

/// A 3x3 affine transform, stored row-major as Android exposes it through
/// [#getValues(float[])]: `[scaleX, skewX, transX, skewY, scaleY, transY, 0, 0, 1]`.
public class Matrix {

    public static final int MSCALE_X = 0;
    public static final int MSKEW_X = 1;
    public static final int MTRANS_X = 2;
    public static final int MSKEW_Y = 3;
    public static final int MSCALE_Y = 4;
    public static final int MTRANS_Y = 5;
    public static final int MPERSP_0 = 6;
    public static final int MPERSP_1 = 7;
    public static final int MPERSP_2 = 8;

    public enum ScaleToFit { FILL, START, CENTER, END }

    public static final Matrix IDENTITY_MATRIX = new Matrix();

    private final float[] m = {1, 0, 0, 0, 1, 0, 0, 0, 1};

    public Matrix() {
    }

    public Matrix(Matrix src) {
        set(src);
    }

    public boolean isIdentity() {
        return m[0] == 1 && m[1] == 0 && m[2] == 0 && m[3] == 0 && m[4] == 1 && m[5] == 0;
    }

    public boolean isAffine() {
        return true;
    }

    public boolean rectStaysRect() {
        return (m[1] == 0 && m[3] == 0) || (m[0] == 0 && m[4] == 0);
    }

    public void set(Matrix src) {
        if (src == null) {
            reset();
        } else {
            System.arraycopy(src.m, 0, m, 0, 9);
        }
    }

    public void reset() {
        m[0] = 1;
        m[1] = 0;
        m[2] = 0;
        m[3] = 0;
        m[4] = 1;
        m[5] = 0;
        m[6] = 0;
        m[7] = 0;
        m[8] = 1;
    }

    public void getValues(float[] values) {
        System.arraycopy(m, 0, values, 0, 9);
    }

    public void setValues(float[] values) {
        System.arraycopy(values, 0, m, 0, 9);
    }

    public void setTranslate(float dx, float dy) {
        reset();
        m[2] = dx;
        m[5] = dy;
    }

    public void setScale(float sx, float sy) {
        reset();
        m[0] = sx;
        m[4] = sy;
    }

    public void setScale(float sx, float sy, float px, float py) {
        reset();
        m[0] = sx;
        m[4] = sy;
        m[2] = px - sx * px;
        m[5] = py - sy * py;
    }

    public void setRotate(float degrees) {
        setRotate(degrees, 0, 0);
    }

    public void setRotate(float degrees, float px, float py) {
        double r = Math.toRadians(degrees);
        float c = (float) Math.cos(r);
        float s = (float) Math.sin(r);
        setSinCos(s, c, px, py);
    }

    public void setSinCos(float sinValue, float cosValue, float px, float py) {
        m[0] = cosValue;
        m[1] = -sinValue;
        m[3] = sinValue;
        m[4] = cosValue;
        m[2] = px - cosValue * px + sinValue * py;
        m[5] = py - sinValue * px - cosValue * py;
        m[6] = 0;
        m[7] = 0;
        m[8] = 1;
    }

    public void setSkew(float kx, float ky) {
        reset();
        m[1] = kx;
        m[3] = ky;
    }

    /// this = a * b
    public boolean setConcat(Matrix a, Matrix b) {
        float[] r = multiply(a.m, b.m);
        System.arraycopy(r, 0, m, 0, 9);
        return true;
    }

    private static float[] multiply(float[] a, float[] b) {
        float[] r = new float[9];
        r[0] = a[0] * b[0] + a[1] * b[3];
        r[1] = a[0] * b[1] + a[1] * b[4];
        r[2] = a[0] * b[2] + a[1] * b[5] + a[2];
        r[3] = a[3] * b[0] + a[4] * b[3];
        r[4] = a[3] * b[1] + a[4] * b[4];
        r[5] = a[3] * b[2] + a[4] * b[5] + a[5];
        r[8] = 1;
        return r;
    }

    private void pre(Matrix other) {
        float[] r = multiply(m, other.m);
        System.arraycopy(r, 0, m, 0, 9);
    }

    private void post(Matrix other) {
        float[] r = multiply(other.m, m);
        System.arraycopy(r, 0, m, 0, 9);
    }

    public boolean preTranslate(float dx, float dy) {
        Matrix t = new Matrix();
        t.setTranslate(dx, dy);
        pre(t);
        return true;
    }

    public boolean postTranslate(float dx, float dy) {
        m[2] += dx;
        m[5] += dy;
        return true;
    }

    public boolean preScale(float sx, float sy) {
        Matrix t = new Matrix();
        t.setScale(sx, sy);
        pre(t);
        return true;
    }

    public boolean preScale(float sx, float sy, float px, float py) {
        Matrix t = new Matrix();
        t.setScale(sx, sy, px, py);
        pre(t);
        return true;
    }

    public boolean postScale(float sx, float sy) {
        Matrix t = new Matrix();
        t.setScale(sx, sy);
        post(t);
        return true;
    }

    public boolean postScale(float sx, float sy, float px, float py) {
        Matrix t = new Matrix();
        t.setScale(sx, sy, px, py);
        post(t);
        return true;
    }

    public boolean preRotate(float degrees) {
        Matrix t = new Matrix();
        t.setRotate(degrees);
        pre(t);
        return true;
    }

    public boolean preRotate(float degrees, float px, float py) {
        Matrix t = new Matrix();
        t.setRotate(degrees, px, py);
        pre(t);
        return true;
    }

    public boolean postRotate(float degrees) {
        Matrix t = new Matrix();
        t.setRotate(degrees);
        post(t);
        return true;
    }

    public boolean postRotate(float degrees, float px, float py) {
        Matrix t = new Matrix();
        t.setRotate(degrees, px, py);
        post(t);
        return true;
    }

    public boolean preConcat(Matrix other) {
        pre(other);
        return true;
    }

    public boolean postConcat(Matrix other) {
        post(other);
        return true;
    }

    public boolean invert(Matrix inverse) {
        float det = m[0] * m[4] - m[1] * m[3];
        if (det == 0) {
            return false;
        }
        float a = m[4] / det;
        float b = -m[1] / det;
        float d = -m[3] / det;
        float e = m[0] / det;
        float c = -(a * m[2] + b * m[5]);
        float f = -(d * m[2] + e * m[5]);
        if (inverse != null) {
            inverse.m[0] = a;
            inverse.m[1] = b;
            inverse.m[2] = c;
            inverse.m[3] = d;
            inverse.m[4] = e;
            inverse.m[5] = f;
            inverse.m[6] = 0;
            inverse.m[7] = 0;
            inverse.m[8] = 1;
        }
        return true;
    }

    public void mapPoints(float[] pts) {
        mapPoints(pts, 0, pts, 0, pts.length / 2);
    }

    public void mapPoints(float[] dst, float[] src) {
        mapPoints(dst, 0, src, 0, src.length / 2);
    }

    public void mapPoints(float[] dst, int dstIndex, float[] src, int srcIndex, int pointCount) {
        for (int i = 0; i < pointCount; i++) {
            float x = src[srcIndex + i * 2];
            float y = src[srcIndex + i * 2 + 1];
            dst[dstIndex + i * 2] = m[0] * x + m[1] * y + m[2];
            dst[dstIndex + i * 2 + 1] = m[3] * x + m[4] * y + m[5];
        }
    }

    public void mapVectors(float[] vecs) {
        for (int i = 0; i + 1 < vecs.length; i += 2) {
            float x = vecs[i];
            float y = vecs[i + 1];
            vecs[i] = m[0] * x + m[1] * y;
            vecs[i + 1] = m[3] * x + m[4] * y;
        }
    }

    public boolean mapRect(RectF rect) {
        return mapRect(rect, rect);
    }

    public boolean mapRect(RectF dst, RectF src) {
        float[] p = {src.left, src.top, src.right, src.top, src.right, src.bottom, src.left, src.bottom};
        mapPoints(p);
        float l = Math.min(Math.min(p[0], p[2]), Math.min(p[4], p[6]));
        float r = Math.max(Math.max(p[0], p[2]), Math.max(p[4], p[6]));
        float t = Math.min(Math.min(p[1], p[3]), Math.min(p[5], p[7]));
        float b = Math.max(Math.max(p[1], p[3]), Math.max(p[5], p[7]));
        dst.set(l, t, r, b);
        return rectStaysRect();
    }

    public float mapRadius(float radius) {
        float sx = (float) Math.sqrt(m[0] * m[0] + m[3] * m[3]);
        float sy = (float) Math.sqrt(m[1] * m[1] + m[4] * m[4]);
        return radius * (float) Math.sqrt(sx * sy);
    }

    public boolean setRectToRect(RectF src, RectF dst, ScaleToFit stf) {
        if (src.isEmpty()) {
            reset();
            return false;
        }
        float sx = dst.width() / src.width();
        float sy = dst.height() / src.height();
        float tx = dst.left - src.left * sx;
        float ty = dst.top - src.top * sy;
        if (stf != ScaleToFit.FILL) {
            float s = Math.min(sx, sy);
            sx = s;
            sy = s;
            float dw = dst.width() - src.width() * s;
            float dh = dst.height() - src.height() * s;
            tx = dst.left - src.left * s;
            ty = dst.top - src.top * s;
            if (stf == ScaleToFit.CENTER) {
                tx += dw / 2;
                ty += dh / 2;
            } else if (stf == ScaleToFit.END) {
                tx += dw;
                ty += dh;
            }
        }
        reset();
        m[0] = sx;
        m[4] = sy;
        m[2] = tx;
        m[5] = ty;
        return true;
    }

    /// Translation and scale only, when there is no rotation or skew.
    public boolean isScaleTranslate() {
        return m[1] == 0 && m[3] == 0;
    }

    float[] raw() {
        return m;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Matrix)) {
            return false;
        }
        float[] a = ((Matrix) o).m;
        for (int i = 0; i < 9; i++) {
            if (a[i] != m[i]) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        int h = 0;
        for (float f : m) {
            h = h * 31 + Float.floatToIntBits(f);
        }
        return h;
    }

    @Override
    public String toString() {
        return "Matrix{[" + m[0] + ", " + m[1] + ", " + m[2] + "][" + m[3] + ", " + m[4] + ", " + m[5] + "]}";
    }
}
