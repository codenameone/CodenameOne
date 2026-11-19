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

/// A path as a renderer receives it: verbs and points in growable arrays that
/// are rewound, not reallocated, from frame to frame. `version` changes on
/// every rebuild so a renderer can cache its own form of the path.
public final class RivePath {
    public static final byte MOVE = 0;
    public static final byte LINE = 1;
    public static final byte CUBIC = 2;
    public static final byte CLOSE = 3;

    public static final int FILL_NON_ZERO = 0;
    public static final int FILL_EVEN_ODD = 1;

    public byte[] verbs = new byte[16];
    public float[] points = new float[64];
    public int verbCount;
    public int pointCount;
    public int fillRule;
    public int version;

    public void rewind() {
        verbCount = 0;
        pointCount = 0;
        version++;
    }

    public boolean isEmpty() {
        return verbCount == 0;
    }

    private void verb(byte v, int floats) {
        if (verbCount == verbs.length) {
            byte[] n = new byte[verbs.length * 2];
            System.arraycopy(verbs, 0, n, 0, verbCount);
            verbs = n;
        }
        if (pointCount + floats > points.length) {
            float[] n = new float[Math.max(points.length * 2, pointCount + floats)];
            System.arraycopy(points, 0, n, 0, pointCount);
            points = n;
        }
        verbs[verbCount++] = v;
    }

    public void moveTo(float x, float y) {
        verb(MOVE, 2);
        points[pointCount++] = x;
        points[pointCount++] = y;
    }

    public void lineTo(float x, float y) {
        verb(LINE, 2);
        points[pointCount++] = x;
        points[pointCount++] = y;
    }

    public void cubicTo(float ox, float oy, float ix, float iy, float x, float y) {
        verb(CUBIC, 6);
        points[pointCount++] = ox;
        points[pointCount++] = oy;
        points[pointCount++] = ix;
        points[pointCount++] = iy;
        points[pointCount++] = x;
        points[pointCount++] = y;
    }

    public void close() {
        verb(CLOSE, 0);
    }

    /// Appends `other` with every point mapped through `m` (null for none).
    public void addPath(RivePath other, Mat2D m) {
        int p = 0;
        for (int i = 0; i < other.verbCount; i++) {
            byte v = other.verbs[i];
            int n = v == CUBIC ? 6 : v == CLOSE ? 0 : 2;
            verb(v, n);
            for (int k = 0; k < n; k += 2) {
                float x = other.points[p + k];
                float y = other.points[p + k + 1];
                if (m != null) {
                    points[pointCount++] = m.mapX(x, y);
                    points[pointCount++] = m.mapY(x, y);
                } else {
                    points[pointCount++] = x;
                    points[pointCount++] = y;
                }
            }
            p += n;
        }
    }
}
