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

/// An ellipse inscribed in its bounds, as four cubic arcs.
public class Ellipse extends EllipseBase {
    private static final float CIRCLE_CONSTANT = 0.552284749831f;

    @Override
    protected void buildVertices() {
        truncate(0);
    }

    @Override
    public void update(int value) {
        super.update(value);
        if (hasDirt(value, DIRT_PATH)) {
            float rx = mWidth / 2f;
            float ry = mHeight / 2f;
            float ox = -mOriginX * mWidth + rx;
            float oy = -mOriginY * mHeight + ry;
            float cx = rx * CIRCLE_CONSTANT;
            float cy = ry * CIRCLE_CONSTANT;
            rawPath.rewind();
            rawPath.moveTo(ox, oy - ry);
            rawPath.cubicTo(ox + cx, oy - ry, ox + rx, oy - cy, ox + rx, oy);
            rawPath.cubicTo(ox + rx, oy + cy, ox + cx, oy + ry, ox, oy + ry);
            rawPath.cubicTo(ox - cx, oy + ry, ox - rx, oy + cy, ox - rx, oy);
            rawPath.cubicTo(ox - rx, oy - cy, ox - cx, oy - ry, ox, oy - ry);
            rawPath.close();
        }
    }
}
