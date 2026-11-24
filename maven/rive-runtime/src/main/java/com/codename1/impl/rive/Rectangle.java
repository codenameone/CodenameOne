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

/// A rectangle with per-corner (or linked) radii.
public class Rectangle extends RectangleBase {
    @Override
    protected void buildVertices() {
        float ox = -mOriginX * mWidth;
        float oy = -mOriginY * mHeight;
        float r = mCornerRadiusTL;
        boolean link = mLinkCornerRadius;
        set(straight(0), ox, oy, r);
        set(straight(1), ox + mWidth, oy, link ? r : mCornerRadiusTR);
        set(straight(2), ox + mWidth, oy + mHeight, link ? r : mCornerRadiusBR);
        set(straight(3), ox, oy + mHeight, link ? r : mCornerRadiusBL);
        truncate(4);
    }

    static void set(StraightVertex v, float x, float y, float r) {
        v.mX = x;
        v.mY = y;
        v.mRadius = r;
    }

    @Override
    protected void cornerRadiusTLChanged() {
        markPathDirty();
    }

    @Override
    protected void cornerRadiusTRChanged() {
        markPathDirty();
    }

    @Override
    protected void cornerRadiusBLChanged() {
        markPathDirty();
    }

    @Override
    protected void cornerRadiusBRChanged() {
        markPathDirty();
    }

    @Override
    protected void linkCornerRadiusChanged() {
        markPathDirty();
    }
}
