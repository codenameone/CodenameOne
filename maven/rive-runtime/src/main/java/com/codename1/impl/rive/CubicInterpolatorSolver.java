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

/// Solves a cubic bezier's x for its parameter t (sampled table plus
/// Newton-Raphson, falling back to bisection).
public final class CubicInterpolatorSolver {
    private static final int SPLINE_TABLE_SIZE = 11;
    private static final float SAMPLE_STEP_SIZE = 1.0f / (SPLINE_TABLE_SIZE - 1.0f);
    private static final int NEWTON_ITERATIONS = 4;
    private static final float NEWTON_MIN_SLOPE = 0.001f;
    private static final float SUBDIVISION_PRECISION = 0.0000001f;
    private static final int SUBDIVISION_MAX_ITERATIONS = 10;

    private final float[] values = new float[SPLINE_TABLE_SIZE];
    private float x1;
    private float x2;

    /// The bezier value at `aT` for control points `aA1` and `aA2`.
    public static float calcBezier(float aT, float aA1, float aA2) {
        return (((1.0f - 3.0f * aA2 + 3.0f * aA1) * aT + (3.0f * aA2 - 6.0f * aA1)) * aT + (3.0f * aA1)) * aT;
    }

    private static float getSlope(float aT, float aA1, float aA2) {
        return 3.0f * (1.0f - 3.0f * aA2 + 3.0f * aA1) * aT * aT + 2.0f * (3.0f * aA2 - 6.0f * aA1) * aT + (3.0f * aA1);
    }

    /// Samples the curve for x control points `x1` and `x2`.
    public void build(float x1, float x2) {
        this.x1 = x1;
        this.x2 = x2;
        for (int i = 0; i < SPLINE_TABLE_SIZE; ++i) {
            values[i] = calcBezier(i * SAMPLE_STEP_SIZE, x1, x2);
        }
    }

    /// The parameter t whose x is `x`.
    public float getT(float x) {
        float intervalStart = 0.0f;
        int currentSample = 1;
        int lastSample = SPLINE_TABLE_SIZE - 1;
        for (; currentSample != lastSample && values[currentSample] <= x; ++currentSample) {
            intervalStart += SAMPLE_STEP_SIZE;
        }
        --currentSample;
        float dist = (x - values[currentSample]) / (values[currentSample + 1] - values[currentSample]);
        float guessForT = intervalStart + dist * SAMPLE_STEP_SIZE;
        float initialSlope = getSlope(guessForT, x1, x2);
        if (initialSlope >= NEWTON_MIN_SLOPE) {
            for (int i = 0; i < NEWTON_ITERATIONS; ++i) {
                float currentSlope = getSlope(guessForT, x1, x2);
                if (currentSlope == 0.0f) {
                    return guessForT;
                }
                float currentX = calcBezier(guessForT, x1, x2) - x;
                guessForT -= currentX / currentSlope;
            }
            return guessForT;
        } else if (initialSlope == 0.0f) {
            return guessForT;
        }
        float aB = intervalStart + SAMPLE_STEP_SIZE;
        float currentX;
        float currentT;
        int i = 0;
        do {
            currentT = intervalStart + (aB - intervalStart) / 2.0f;
            currentX = calcBezier(currentT, x1, x2) - x;
            if (currentX > 0.0f) {
                aB = currentT;
            } else {
                intervalStart = currentT;
            }
        } while (Math.abs(currentX) > SUBDIVISION_PRECISION && ++i < SUBDIVISION_MAX_ITERATIONS);
        return currentT;
    }
}
