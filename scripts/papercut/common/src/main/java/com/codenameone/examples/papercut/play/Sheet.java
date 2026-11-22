/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.papercut.play;

/// The page the game is played on, as a grid of springs.
///
/// Every crossing of the ruled lines is a point mass tied to where it was
/// printed. A burst pushes the points out and an inkwell pulls them in, and
/// the springs bring them back, so the lines ripple and pucker the way a sheet
/// of paper would if you could poke it. [ArenaView] draws the horizontal
/// lines blue and one column red, which is all it takes to read as a page of
/// a school notebook.
///
/// The grid is plain arithmetic with no UI in it, so it runs in tests too.
public final class Sheet {
    public final int cols;
    public final int rows;
    public final float spacing;
    /// Displacement from the printed position, per point, row major.
    final float[] dx;
    final float[] dy;
    private final float[] vx;
    private final float[] vy;

    private static final float STIFFNESS = 38f;
    private static final float DAMPING = 5.5f;
    /// Neighbours pull toward each other too, which is what makes a poke
    /// spread out as a ripple instead of staying a dent.
    private static final float COUPLING = 16f;
    private static final float MAX_OFFSET = 70f;

    public Sheet(float width, float height, float spacing) {
        this.spacing = spacing;
        cols = (int) (width / spacing) + 1;
        rows = (int) (height / spacing) + 1;
        int n = cols * rows;
        dx = new float[n];
        dy = new float[n];
        vx = new float[n];
        vy = new float[n];
    }

    /// Pushes every point within `radius` of (x, y) away from it, or toward
    /// it when `strength` is negative. The push fades linearly to the edge.
    public void poke(float x, float y, float radius, float strength) {
        int c0 = Math.max(0, (int) ((x - radius) / spacing));
        int c1 = Math.min(cols - 1, (int) ((x + radius) / spacing) + 1);
        int r0 = Math.max(0, (int) ((y - radius) / spacing));
        int r1 = Math.min(rows - 1, (int) ((y + radius) / spacing) + 1);
        float r2 = radius * radius;
        for (int r = r0; r <= r1; r++) {
            for (int c = c0; c <= c1; c++) {
                int i = r * cols + c;
                float px = c * spacing + dx[i] - x;
                float py = r * spacing + dy[i] - y;
                float d2 = px * px + py * py;
                if (d2 >= r2 || d2 < 0.0001f) {
                    continue;
                }
                float d = (float) Math.sqrt(d2);
                float f = strength * (1 - d / radius);
                vx[i] += px / d * f;
                vy[i] += py / d * f;
            }
        }
    }

    public void update(float dt) {
        int n = cols * rows;
        for (int i = 0; i < n; i++) {
            int c = i % cols;
            float ax = -STIFFNESS * dx[i] - DAMPING * vx[i];
            float ay = -STIFFNESS * dy[i] - DAMPING * vy[i];
            float sx = 0;
            float sy = 0;
            int k = 0;
            if (c > 0) {
                sx += dx[i - 1];
                sy += dy[i - 1];
                k++;
            }
            if (c < cols - 1) {
                sx += dx[i + 1];
                sy += dy[i + 1];
                k++;
            }
            if (i >= cols) {
                sx += dx[i - cols];
                sy += dy[i - cols];
                k++;
            }
            if (i + cols < n) {
                sx += dx[i + cols];
                sy += dy[i + cols];
                k++;
            }
            if (k > 0) {
                ax += COUPLING * (sx / k - dx[i]);
                ay += COUPLING * (sy / k - dy[i]);
            }
            vx[i] += ax * dt;
            vy[i] += ay * dt;
        }
        for (int i = 0; i < n; i++) {
            dx[i] = clamp(dx[i] + vx[i] * dt);
            dy[i] = clamp(dy[i] + vy[i] * dt);
        }
    }

    private static float clamp(float v) {
        return v > MAX_OFFSET ? MAX_OFFSET : v < -MAX_OFFSET ? -MAX_OFFSET : v;
    }

    /// The point's position on the sheet now.
    public float x(int col, int row) {
        return col * spacing + dx[row * cols + col];
    }

    public float y(int col, int row) {
        return row * spacing + dy[row * cols + col];
    }

    public void reset() {
        for (int i = 0; i < dx.length; i++) {
            dx[i] = 0;
            dy[i] = 0;
            vx[i] = 0;
            vy[i] = 0;
        }
    }
}
