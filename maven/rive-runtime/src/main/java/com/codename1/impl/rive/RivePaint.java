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

/// How a renderer fills or strokes a [RivePath]. One instance per shape paint,
/// updated in place.
public final class RivePaint {
    public static final int GRADIENT_NONE = 0;
    public static final int GRADIENT_LINEAR = 1;
    public static final int GRADIENT_RADIAL = 2;

    public boolean stroke;
    public int color = 0xff000000;
    public float thickness = 1f;
    /// Rive's cap: 0 butt, 1 round, 2 square.
    public int cap;
    /// Rive's join: 0 miter, 1 round, 2 bevel.
    public int join;
    /// Rive's blend mode value (3 = src-over).
    public int blendMode = 3;
    public int gradient = GRADIENT_NONE;
    /// Linear: start and end point. Radial: center and a point on the rim.
    public float startX;
    public float startY;
    public float endX;
    public float endY;
    public int[] stopColors = new int[0];
    public float[] stopPositions = new float[0];
    public int stopCount;
    /// Bumped whenever anything above changes.
    public int version;
}
