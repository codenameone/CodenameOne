/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package android.widget;

import com.codename1.util.MathUtil;

/// The deceleration curve Android's scrollers share: the spline that maps
/// fling time to distance (`SplineOverScroller`), the physics that size a
/// fling from its initial velocity, and the viscous-fluid curve of
/// programmatic scrolls.
final class ScrollerMath {

    static final float INFLEXION = 0.35f;
    static final float START_TENSION = 0.5f;
    static final float END_TENSION = 1.0f;
    static final float P1 = START_TENSION * INFLEXION;
    static final float P2 = 1.0f - END_TENSION * (1.0f - INFLEXION);
    static final float DECELERATION_RATE = (float) (MathUtil.log(0.78) / MathUtil.log(0.9));
    static final float GRAVITY_EARTH = 9.80665f;
    static final int NB_SAMPLES = 100;
    static final float[] SPLINE_POSITION = new float[NB_SAMPLES + 1];
    static final float[] SPLINE_TIME = new float[NB_SAMPLES + 1];

    static {
        float xMin = 0.0f;
        float yMin = 0.0f;
        for (int i = 0; i < NB_SAMPLES; i++) {
            final float alpha = (float) i / NB_SAMPLES;
            float xMax = 1.0f;
            float x;
            float tx;
            float coef;
            while (true) {
                x = xMin + (xMax - xMin) / 2.0f;
                coef = 3.0f * x * (1.0f - x);
                tx = coef * ((1.0f - x) * P1 + x * P2) + x * x * x;
                if (Math.abs(tx - alpha) < 1E-5) {
                    break;
                }
                if (tx > alpha) {
                    xMax = x;
                } else {
                    xMin = x;
                }
            }
            SPLINE_POSITION[i] = coef * ((1.0f - x) * START_TENSION + x) + x * x * x;
            float yMax = 1.0f;
            float y;
            float dy;
            while (true) {
                y = yMin + (yMax - yMin) / 2.0f;
                coef = 3.0f * y * (1.0f - y);
                dy = coef * ((1.0f - y) * START_TENSION + y) + y * y * y;
                if (Math.abs(dy - alpha) < 1E-5) {
                    break;
                }
                if (dy > alpha) {
                    yMax = y;
                } else {
                    yMin = y;
                }
            }
            SPLINE_TIME[i] = coef * ((1.0f - y) * P1 + y * P2) + y * y * y;
        }
        SPLINE_POSITION[NB_SAMPLES] = 1.0f;
        SPLINE_TIME[NB_SAMPLES] = 1.0f;
    }

    private ScrollerMath() {
    }

    /// The physical coefficient for a screen of `density` (px per dp).
    static float physicalCoeff(float density) {
        float ppi = density * 160.0f;
        return GRAVITY_EARTH * 39.37f * ppi * 0.84f;
    }

    static double splineDeceleration(float velocity, float friction, float coeff) {
        return MathUtil.log(INFLEXION * Math.abs(velocity) / (friction * coeff));
    }

    static int splineFlingDuration(float velocity, float friction, float coeff) {
        final double l = splineDeceleration(velocity, friction, coeff);
        final double decelMinusOne = DECELERATION_RATE - 1.0;
        return (int) (1000.0 * MathUtil.exp(l / decelMinusOne));
    }

    static double splineFlingDistance(float velocity, float friction, float coeff) {
        final double l = splineDeceleration(velocity, friction, coeff);
        final double decelMinusOne = DECELERATION_RATE - 1.0;
        return friction * coeff * MathUtil.exp(DECELERATION_RATE / decelMinusOne * l);
    }

    private static final float VISCOUS_FLUID_SCALE = 8.0f;
    private static final float VISCOUS_FLUID_NORMALIZE;
    private static final float VISCOUS_FLUID_OFFSET;

    static {
        VISCOUS_FLUID_NORMALIZE = 1.0f / viscousFluidRaw(1.0f);
        VISCOUS_FLUID_OFFSET = 1.0f - VISCOUS_FLUID_NORMALIZE * viscousFluidRaw(1.0f);
    }

    private static float viscousFluidRaw(float x) {
        x *= VISCOUS_FLUID_SCALE;
        if (x < 1.0f) {
            x -= (1.0f - (float) MathUtil.exp(-x));
        } else {
            float start = 0.36787944117f;
            x = 1.0f - (float) MathUtil.exp(1.0f - x);
            x = start + x * (1.0f - start);
        }
        return x;
    }

    /// Android's `Scroller.ViscousFluidInterpolator`.
    static float viscousFluid(float input) {
        float interpolated = VISCOUS_FLUID_NORMALIZE * viscousFluidRaw(input);
        if (interpolated > 0) {
            return interpolated + VISCOUS_FLUID_OFFSET;
        }
        return interpolated;
    }

    /// One axis of a scroller: a programmatic scroll or a spline fling,
    /// ported from `OverScroller.SplineOverScroller` without the overshoot.
    static final class Axis {
        static final int SCROLL = 0;
        static final int FLING = 1;

        int start;
        int current;
        int finalPos;
        float currVelocity;
        long startTime;
        int duration;
        int splineDuration;
        int splineDistance;
        boolean finished = true;
        int mode;
        int min;
        int max;
        float friction = android.view.ViewConfiguration.getScrollFriction();
        final float coeff;
        android.view.animation.Interpolator interpolator;

        Axis(float density) {
            coeff = physicalCoeff(density);
        }

        static long now() {
            return android.os.SystemClock.uptimeMillis();
        }

        void startScroll(int s, int distance, int dur) {
            finished = false;
            mode = SCROLL;
            start = s;
            current = s;
            finalPos = s + distance;
            startTime = now();
            duration = dur;
            currVelocity = 0;
        }

        void fling(int s, int velocity, int mn, int mx) {
            min = mn;
            max = mx;
            finished = false;
            mode = FLING;
            current = s;
            start = s;
            currVelocity = velocity;
            startTime = now();
            duration = 0;
            splineDuration = 0;
            if (s > mx || s < mn) {
                springBack(s, mn, mx);
                return;
            }
            double totalDistance = 0.0;
            if (velocity != 0) {
                duration = splineDuration = splineFlingDuration(velocity, friction, coeff);
                totalDistance = splineFlingDistance(velocity, friction, coeff);
            }
            splineDistance = (int) (totalDistance * (velocity < 0 ? -1 : 1));
            finalPos = s + splineDistance;
            if (finalPos < mn) {
                adjustDuration(start, finalPos, mn);
                finalPos = mn;
            }
            if (finalPos > mx) {
                adjustDuration(start, finalPos, mx);
                finalPos = mx;
            }
            if (duration <= 0) {
                finished = true;
                current = finalPos;
            }
        }

        private void adjustDuration(int s, int oldFinal, int newFinal) {
            final int oldDistance = oldFinal - s;
            final int newDistance = newFinal - s;
            if (oldDistance == 0) {
                return;
            }
            final float x = Math.abs((float) newDistance / oldDistance);
            final int index = (int) (NB_SAMPLES * x);
            if (index < NB_SAMPLES) {
                final float xInf = (float) index / NB_SAMPLES;
                final float xSup = (float) (index + 1) / NB_SAMPLES;
                final float tInf = SPLINE_TIME[index];
                final float tSup = SPLINE_TIME[index + 1];
                final float timeCoef = tInf + (x - xInf) / (xSup - xInf) * (tSup - tInf);
                duration = (int) (duration * timeCoef);
            }
        }

        boolean springBack(int s, int mn, int mx) {
            finished = true;
            start = s;
            current = s;
            finalPos = s;
            currVelocity = 0;
            if (s < mn) {
                startScroll(s, mn - s, 250);
            } else if (s > mx) {
                startScroll(s, mx - s, 250);
            }
            return !finished;
        }

        /// Advances to the current time; false once finished.
        boolean update() {
            if (finished) {
                return false;
            }
            final long elapsed = now() - startTime;
            if (elapsed >= duration) {
                current = finalPos;
                currVelocity = 0;
                finished = true;
                return false;
            }
            if (mode == SCROLL) {
                float t = elapsed / (float) duration;
                float e = interpolator == null ? viscousFluid(t) : interpolator.getInterpolation(t);
                current = start + Math.round(e * (finalPos - start));
                return true;
            }
            final float t = (float) elapsed / splineDuration;
            final int index = (int) (NB_SAMPLES * t);
            float distanceCoef = 1.f;
            float velocityCoef = 0.f;
            if (index < NB_SAMPLES) {
                final float tInf = (float) index / NB_SAMPLES;
                final float tSup = (float) (index + 1) / NB_SAMPLES;
                final float dInf = SPLINE_POSITION[index];
                final float dSup = SPLINE_POSITION[index + 1];
                velocityCoef = (dSup - dInf) / (tSup - tInf);
                distanceCoef = dInf + (t - tInf) * velocityCoef;
            }
            currVelocity = velocityCoef * splineDistance / splineDuration * 1000.0f;
            current = start + Math.round(distanceCoef * splineDistance);
            if (current < min && mode == FLING && splineDistance < 0) {
                current = min;
            }
            if (current > max && mode == FLING && splineDistance > 0) {
                current = max;
            }
            return true;
        }

        void finish() {
            current = finalPos;
            finished = true;
            currVelocity = 0;
        }
    }
}
