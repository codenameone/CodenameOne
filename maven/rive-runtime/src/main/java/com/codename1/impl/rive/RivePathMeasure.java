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

/// A minimal port of `PathMeasure`/`ContourMeasure`: measures the contours of
/// a [RivePath] by flattening cubics into fixed steps, and answers position
/// and tangent at a distance. Arrays are reused between [#build(RivePath)]
/// calls so a per frame rebuild does not allocate once warmed up.
public final class RivePathMeasure {
    private static final int CUBIC_STEPS = 16;
    private float[] xs = new float[64];
    private float[] ys = new float[64];
    private float[] cum = new float[64];
    private int count;
    private int[] contourStart = new int[4];
    private int[] contourEnd = new int[4];
    private float[] contourLength = new float[4];
    private boolean[] contourClosed = new boolean[4];
    private boolean pendingClosed;
    private int contours;
    private float length;
    private float startOfContourDist;
    private int contourStartIndex = -1;

    public float getLength() {
        return length;
    }

    private void addPoint(float x, float y) {
        if (count == xs.length) {
            int n = count * 2;
            float[] nx = new float[n];
            float[] ny = new float[n];
            float[] nc = new float[n];
            System.arraycopy(xs, 0, nx, 0, count);
            System.arraycopy(ys, 0, ny, 0, count);
            System.arraycopy(cum, 0, nc, 0, count);
            xs = nx;
            ys = ny;
            cum = nc;
        }
        float d = startOfContourDist;
        if (count > 0 && contourStartIndex >= 0 && count > contourStartIndex) {
            float dx = x - xs[count - 1];
            float dy = y - ys[count - 1];
            d = cum[count - 1] + (float) Math.sqrt(dx * dx + dy * dy);
        }
        xs[count] = x;
        ys[count] = y;
        cum[count] = d;
        count++;
    }

    private void beginContour() {
        contourStartIndex = count;
        startOfContourDist = 0;
        pendingClosed = false;
    }

    private void endContour() {
        if (contourStartIndex < 0) {
            return;
        }
        int n = count - contourStartIndex;
        float len = n > 0 ? cum[count - 1] : 0f;
        if (n >= 2 && len > 0f) {
            if (contours == contourStart.length) {
                int m = contours * 2;
                int[] a = new int[m];
                int[] b = new int[m];
                float[] c = new float[m];
                boolean[] cl = new boolean[m];
                System.arraycopy(contourClosed, 0, cl, 0, contours);
                contourClosed = cl;
                System.arraycopy(contourStart, 0, a, 0, contours);
                System.arraycopy(contourEnd, 0, b, 0, contours);
                System.arraycopy(contourLength, 0, c, 0, contours);
                contourStart = a;
                contourEnd = b;
                contourLength = c;
            }
            contourStart[contours] = contourStartIndex;
            contourEnd[contours] = count - 1;
            contourLength[contours] = len;
            contourClosed[contours] = pendingClosed;
            contours++;
            length += len;
        } else {
            count = contourStartIndex;
        }
        contourStartIndex = -1;
    }

    /// Measures `path` (already in the space the caller wants answers in).
    public void build(RivePath path) {
        count = 0;
        contours = 0;
        length = 0;
        contourStartIndex = -1;
        float[] p = path.points;
        int pi = 0;
        float sx = 0;
        float sy = 0;
        float cx = 0;
        float cy = 0;
        for (int v = 0; v < path.verbCount; v++) {
            byte verb = path.verbs[v];
            if (verb == RivePath.MOVE) {
                endContour();
                beginContour();
                sx = p[pi];
                sy = p[pi + 1];
                pi += 2;
                cx = sx;
                cy = sy;
                addPoint(cx, cy);
            } else if (verb == RivePath.LINE) {
                cx = p[pi];
                cy = p[pi + 1];
                pi += 2;
                addPoint(cx, cy);
            } else if (verb == RivePath.CUBIC) {
                float x1 = p[pi];
                float y1 = p[pi + 1];
                float x2 = p[pi + 2];
                float y2 = p[pi + 3];
                float x3 = p[pi + 4];
                float y3 = p[pi + 5];
                pi += 6;
                for (int s = 1; s <= CUBIC_STEPS; s++) {
                    float t = s / (float) CUBIC_STEPS;
                    float u = 1 - t;
                    float b0 = u * u * u;
                    float b1 = 3 * u * u * t;
                    float b2 = 3 * u * t * t;
                    float b3 = t * t * t;
                    addPoint(b0 * cx + b1 * x1 + b2 * x2 + b3 * x3, b0 * cy + b1 * y1 + b2 * y2 + b3 * y3);
                }
                cx = x3;
                cy = y3;
            } else if (verb == RivePath.CLOSE) {
                if (Float.floatToIntBits(cx) != Float.floatToIntBits(sx) || Float.floatToIntBits(cy) != Float.floatToIntBits(sy)) {
                    addPoint(sx, sy);
                }
                cx = sx;
                cy = sy;
                pendingClosed = true;
            }
        }
        endContour();
    }

    /// Port of `PathMeasure::atPercentage`; writes `{x, y, tanX, tanY}`.
    public void atPercentage(float percentage, float[] out) {
        float in = percentage % 1.0f;
        if (in < 0.0f) {
            in += 1.0f;
        }
        if (percentage != 0.0f && in == 0.0f) {
            in = 1.0f;
        }
        atDistance(length * in, out);
    }

    /// Port of `PathMeasure::atDistance`; writes `{x, y, tanX, tanY}`.
    public void atDistance(float distance, float[] out) {
        out[0] = 0;
        out[1] = 0;
        out[2] = 1;
        out[3] = 0;
        float current = distance;
        for (int i = 0; i < contours; i++) {
            float len = contourLength[i];
            if (current - len <= 0 || i + 1 == contours) {
                posTan(i, current, out);
                return;
            }
            current -= len;
        }
    }

    private void posTan(int contour, float d, float[] out) {
        int s = contourStart[contour];
        int e = contourEnd[contour];
        float len = contourLength[contour];
        if (d < 0) {
            d = 0;
        } else if (d > len) {
            d = len;
        }
        int lo = s;
        int hi = e;
        while (hi - lo > 1) {
            int mid = (lo + hi) >>> 1;
            if (cum[mid] < d) {
                lo = mid;
            } else {
                hi = mid;
            }
        }
        float seg = cum[hi] - cum[lo];
        float t = seg > 0 ? (d - cum[lo]) / seg : 0;
        float dx = xs[hi] - xs[lo];
        float dy = ys[hi] - ys[lo];
        out[0] = xs[lo] + dx * t;
        out[1] = ys[lo] + dy * t;
        float l = (float) Math.sqrt(dx * dx + dy * dy);
        if (l > 0) {
            out[2] = dx / l;
            out[3] = dy / l;
        }
    }

    /// Number of measured (non-empty) contours.
    public int getContourCount() {
        return contours;
    }

    public float getContourLength(int contour) {
        return contourLength[contour];
    }

    public boolean isContourClosed(int contour) {
        return contourClosed[contour];
    }

    /// Port of `PathMeasure::isClosed`: a single closed contour.
    public boolean isClosed() {
        return contours == 1 && contourClosed[0];
    }

    private final float[] segTmp = new float[4];

    /// Port of `ContourMeasure::getSegment` over the flattened contour:
    /// appends the part of `contour` between `startD` and `endD` to `out`,
    /// starting with a move when `startWithMove`, else a line.
    public void getContourSegment(int contour, float startD, float endD, RivePath out, boolean startWithMove) {
        float len = contourLength[contour];
        if (startD < 0) {
            startD = 0;
        }
        if (endD > len) {
            endD = len;
        }
        if (startD > endD) {
            return;
        }
        posTan(contour, startD, segTmp);
        if (startWithMove) {
            out.moveTo(segTmp[0], segTmp[1]);
        } else {
            out.lineTo(segTmp[0], segTmp[1]);
        }
        int s = contourStart[contour];
        int e = contourEnd[contour];
        for (int i = s; i <= e; i++) {
            if (cum[i] > startD && cum[i] < endD) {
                out.lineTo(xs[i], ys[i]);
            }
        }
        posTan(contour, endD, segTmp);
        out.lineTo(segTmp[0], segTmp[1]);
    }

    /// Port of `PathMeasure::getSegment`: the span `[startD, endD]` of the
    /// whole path, each contour it crosses starting with a move.
    public void getSegment(float startD, float endD, RivePath out, boolean startWithMove) {
        float base = 0;
        boolean move = startWithMove;
        for (int i = 0; i < contours; i++) {
            float len = contourLength[i];
            float a = startD - base;
            float b = endD - base;
            if (b > 0 && a < len) {
                getContourSegment(i, a, b, out, move);
                move = true;
            }
            base += len;
        }
    }
}
