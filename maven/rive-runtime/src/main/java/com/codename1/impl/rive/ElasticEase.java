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

/// Elastic easing equations with an amplitude and period.
public final class ElasticEase {
    private static final float TWO_PI = (float) (2.0 * Math.PI);
    private float amplitude;
    private float period;
    private float s;

    /// Sets the amplitude and period, recomputing the phase shift.
    public void set(float amplitude, float period) {
        this.amplitude = amplitude;
        this.period = period;
        s = amplitude < 1.0f ? period / 4.0f
                : (float) (period / TWO_PI * com.codename1.util.MathUtil.asin(1.0f / amplitude));
    }

    private float actualAmplitude(float time) {
        if (amplitude < 1.0f) {
            float t = Math.abs(s);
            float absTime = Math.abs(time);
            if (absTime < t) {
                float l = absTime / t;
                return amplitude * l + (1.0f - l);
            }
        }
        return amplitude;
    }

    private static float pow2(float e) {
        return (float) com.codename1.util.MathUtil.pow(2.0, e);
    }

    public float easeOut(float factor) {
        float time = factor;
        return (float) (actualAmplitude(time) * pow2(10.0f * -time) * Math.sin((time - s) * TWO_PI / period)) + 1.0f;
    }

    public float easeIn(float factor) {
        float time = factor - 1.0f;
        return (float) -(actualAmplitude(time) * pow2(10.0f * time) * Math.sin((-time - s) * TWO_PI / period));
    }

    public float easeInOut(float factor) {
        float time = factor * 2.0f - 1.0f;
        float a = actualAmplitude(time);
        if (time < 0.0f) {
            return (float) (-0.5f * a * pow2(10.0f * time) * Math.sin((-time - s) * TWO_PI / period));
        }
        return (float) (0.5f * (a * pow2(10.0f * -time) * Math.sin((time - s) * TWO_PI / period))) + 1.0f;
    }
}
