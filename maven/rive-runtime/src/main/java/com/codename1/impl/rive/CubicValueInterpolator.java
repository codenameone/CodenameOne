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

/// A cubic bezier whose y control points are values, not factors.
public class CubicValueInterpolator extends CubicValueInterpolatorBase {
    private float d;
    private float valueTo;
    private float a;
    private float b;
    private float c;

    public CubicValueInterpolator() {
        computeParameters();
    }

    private void computeParameters() {
        float y1 = d;
        float y2 = getY1();
        float y3 = getY2();
        float y4 = valueTo;
        a = y4 + 3 * (y2 - y3) - y1;
        b = 3 * (y3 - y2 * 2 + y1);
        c = 3 * (y2 - y1);
    }

    @Override
    public void initialize() {
        super.initialize();
        computeParameters();
    }

    @Override
    protected void y1Changed() {
        computeParameters();
    }

    @Override
    protected void y2Changed() {
        computeParameters();
    }

    @Override
    public float transformValue(float valueFrom, float valueTo, float factor) {
        if (Float.floatToIntBits(d) != Float.floatToIntBits(valueFrom)
                || Float.floatToIntBits(this.valueTo) != Float.floatToIntBits(valueTo)) {
            d = valueFrom;
            this.valueTo = valueTo;
            computeParameters();
        }
        float t = solver.getT(factor);
        return ((a * t + b) * t + c) * t + d;
    }
}
