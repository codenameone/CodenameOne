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

/// An elastic easing curve (ease in, out or in-out).
public class ElasticInterpolator extends ElasticInterpolatorBase {
    private final ElasticEase elastic = new ElasticEase();

    public ElasticInterpolator() {
        elastic.set(1.0f, 0.5f);
    }

    @Override
    public void initialize() {
        elastic.set(getAmplitude(), getPeriod() == 0.0f ? 0.5f : getPeriod());
    }

    @Override
    protected void amplitudeChanged() {
        initialize();
    }

    @Override
    protected void periodChanged() {
        initialize();
    }

    @Override
    public float transform(float factor) {
        switch (getEasingValue()) {
            case 0:
                return elastic.easeIn(factor);
            case 1:
                return elastic.easeOut(factor);
            case 2:
                return elastic.easeInOut(factor);
            default:
                return factor;
        }
    }
}
