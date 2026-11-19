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

/// A regular polygon inscribed in its bounds.
public class Polygon extends PolygonBase {
    protected int vertexCount() {
        return mPoints;
    }

    @Override
    protected void buildVertices() {
        float hw = mWidth / 2;
        float hh = mHeight / 2;
        float ox = -mOriginX * mWidth + hw;
        float oy = -mOriginY * mHeight + hh;
        int n = vertexCount();
        double angle = -Math.PI / 2;
        double inc = 2 * Math.PI / n;
        for (int i = 0; i < n; i++) {
            float w = vertexWidth(i, hw);
            float h = vertexHeight(i, hh);
            Rectangle.set(straight(i), ox + (float) Math.cos(angle) * w, oy + (float) Math.sin(angle) * h, mCornerRadius);
            angle += inc;
        }
        truncate(n);
    }

    protected float vertexWidth(int index, float halfWidth) {
        return halfWidth;
    }

    protected float vertexHeight(int index, float halfHeight) {
        return halfHeight;
    }

    @Override
    protected void pointsChanged() {
        markPathDirty();
    }

    @Override
    protected void cornerRadiusChanged() {
        markPathDirty();
    }
}
