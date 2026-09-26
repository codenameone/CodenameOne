/*
 * Copyright (c) 2008, 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
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
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores
 * CA 94065 USA or visit www.oracle.com if you need additional information or
 * have any questions.
 */
package com.codename1.ui.animations;

import com.codename1.ui.plaf.UIManager;
import com.codename1.util.MathUtil;

/// Abstracts the notion of physical motion over time from a numeric location to
/// another. This class can be subclassed to implement any motion equation for
/// appropriate physics effects.
///
/// This class relies on [AnimationTime.now()][AnimationTime#now()] to provide
/// transitions between coordinates, allowing the underlying clock to be
/// overridden for deterministic playback or custom animation pacing. The motion can be subclassed to provide every
/// type of motion feel from parabolic motion to spline and linear motion. The default
/// implementation provides a simple algorithm giving the feel of acceleration and
/// deceleration.
///
/// @author Shai Almog
public class Motion {
    // package protected for the resource editor
    static final int LINEAR = 0;
    static final int SPLINE = 1;
    private static final int FRICTION = 2;
    private static final int DECELERATION = 3;
    private static final int CUBIC = 4;
    private static final int COLOR_LINEAR = 5;
    private static final int EXPONENTIAL_DECAY = 6;
    private static final int CRITICAL_DAMPED_SPRING = 7;
    private static final int THREE_POINT_CUBIC = 8;
    private static boolean slowMotion;
    private final int[] previousLastReturnedValue = new int[3];
    private final long[] previousLastReturnedValueTime = new long[3];
    int motionType;
    private int sourceValue;
    private int destinationValue;
    private int targetPosition;
    private int duration;
    private long startTime;
    private double initVelocity;
    private double friction;
    private int lastReturnedValue;
    private long currentMotionTime = -1;
    private long previousCurrentMotionTime = -1;
    /// The joint and the second segment's control points of a three-point cubic.
    private float midX;
    private float midY;
    private float q0;
    private float q1;
    private float q2;
    private float q3;

    private float p0;
    private float p1;
    private float p2;
    private float p3;

    /// Construct a point/destination motion
    ///
    /// #### Parameters
    ///
    /// - `sourceValue`: starting value
    ///
    /// - `destinationValue`: destination value
    ///
    /// - `duration`: motion duration
    protected Motion(int sourceValue, int destinationValue, int duration) {
        this.sourceValue = sourceValue;
        this.destinationValue = destinationValue;
        this.duration = duration;
        lastReturnedValue = sourceValue;
        if (slowMotion) {
            this.duration *= 50;
        }
        previousLastReturnedValue[0] = -1;
        previousLastReturnedValueTime[0] = -1;
    }

    /// Construct a velocity motion
    ///
    /// #### Parameters
    ///
    /// - `sourceValue`: starting value
    ///
    /// - `initVelocity`: initial velocity
    ///
    /// - `friction`: degree of friction
    protected Motion(int sourceValue, float initVelocity, float friction) {
        this.sourceValue = sourceValue;
        this.initVelocity = initVelocity;
        this.friction = friction;
        duration = (int) ((Math.abs(initVelocity)) / friction);
        previousLastReturnedValue[0] = -1;
        previousLastReturnedValueTime[0] = -1;
    }

    protected Motion(int sourceValue, double initVelocity, double friction) {
        this.sourceValue = sourceValue;
        this.initVelocity = initVelocity;
        this.friction = friction;
        duration = (int) ((Math.abs(initVelocity)) / friction);
        previousLastReturnedValue[0] = -1;
        previousLastReturnedValueTime[0] = -1;
    }

    /// Allows debugging motion behavior by slowing motions down 50 fold, doesn't apply to friction motion
    ///
    /// #### Returns
    ///
    /// the slowMotion
    public static boolean isSlowMotion() {
        return slowMotion;
    }

    /// Allows debugging motion behavior by slowing motions down 50 fold, doesn't apply to friction motion
    ///
    /// #### Parameters
    ///
    /// - `aSlowMotion`: the slowMotion to set
    public static void setSlowMotion(boolean aSlowMotion) {
        slowMotion = aSlowMotion;
    }

    /// Creates a standard Cubic Bezier motion to implement functions such as ease-in/out etc.
    ///
    /// #### Parameters
    ///
    /// - `sourceValue`: starting value
    ///
    /// - `destinationValue`: destination value
    ///
    /// - `duration`: motion duration
    ///
    /// - `p0`: argument to the bezier function
    ///
    /// - `p1`: argument to the bezier function
    ///
    /// - `p2`: argument to the bezier function
    ///
    /// - `p3`: argument to the bezier function
    ///
    /// #### Returns
    ///
    /// Motion instance
    public static Motion createCubicBezierMotion(int sourceValue, int destinationValue, int duration,
                                                 float p0, float p1, float p2, float p3) {
        Motion m = new Motion(sourceValue, destinationValue, duration);
        m.motionType = CUBIC;
        m.p0 = p0;
        m.p1 = p1;
        m.p2 = p2;
        m.p3 = p3;
        return m;
    }

    /// A curve made of TWO cubic beziers joined at a point, which a single cubic cannot
    /// express.
    ///
    /// A plain `cubic-bezier` is monotonic in a way some motion is not: it cannot
    /// accelerate hard, ease, and then ease out again, because it has only two control
    /// points to spend. Curves that do this are specified as a pair of beziers meeting at
    /// a midpoint, each with its own controls, and the joint is where the character of
    /// the motion changes.
    ///
    /// The segments are evaluated in their own normalized space and rescaled, so each
    /// half is an ordinary CSS cubic-bezier and the two meet exactly at the midpoint.
    ///
    /// #### Parameters
    ///
    /// - `sourceValue`: the initial value
    ///
    /// - `destinationValue`: the value at the end of the motion
    ///
    /// - `duration`: the motion duration in milliseconds
    ///
    /// - `a1X`, `a1Y`, `b1X`, `b1Y`: control points of the first segment
    ///
    /// - `midX`, `midY`: the point the two segments meet at
    ///
    /// - `a2X`, `a2Y`, `b2X`, `b2Y`: control points of the second segment
    ///
    /// #### Returns
    ///
    /// Motion instance
    public static Motion createThreePointCubicMotion(int sourceValue, int destinationValue,
            int duration, float a1X, float a1Y, float b1X, float b1Y,
            float midX, float midY, float a2X, float a2Y, float b2X, float b2Y) {
        Motion m = new Motion(sourceValue, destinationValue, duration);
        m.motionType = THREE_POINT_CUBIC;
        m.p0 = a1X;
        m.p1 = a1Y;
        m.p2 = b1X;
        m.p3 = b1Y;
        m.midX = midX;
        m.midY = midY;
        m.q0 = a2X;
        m.q1 = a2Y;
        m.q2 = b2X;
        m.q3 = b2Y;
        return m;
    }

    /// Equivalent to createCubicBezierMotion with 0, 0.42, 0.58, 1.0 as arguments.
    ///
    /// #### Parameters
    ///
    /// - `sourceValue`: starting value
    ///
    /// - `destinationValue`: destination value
    ///
    /// - `duration`: motion duration
    ///
    /// #### Returns
    ///
    /// Motion instance
    public static Motion createEaseInOutMotion(int sourceValue, int destinationValue, int duration) {
        return createCubicBezierMotion(sourceValue, destinationValue, duration, 0, 0.42f, 0.58f, 1);
    }

    /// Equivalent to createCubicBezierMotion with 0f, 0.25f, 0.25f, 1 as arguments.
    ///
    /// #### Parameters
    ///
    /// - `sourceValue`: starting value
    ///
    /// - `destinationValue`: destination value
    ///
    /// - `duration`: motion duration
    ///
    /// #### Returns
    ///
    /// Motion instance
    public static Motion createEaseMotion(int sourceValue, int destinationValue, int duration) {
        return createCubicBezierMotion(sourceValue, destinationValue, duration, 0f, 0.25f, 0.25f, 1.0f);
    }

    /// Equivalent to createCubicBezierMotion with 0f, 0.42f, 1f, 1f as arguments.
    ///
    /// #### Parameters
    ///
    /// - `sourceValue`: starting value
    ///
    /// - `destinationValue`: destination value
    ///
    /// - `duration`: motion duration
    ///
    /// #### Returns
    ///
    /// Motion instance
    public static Motion createEaseInMotion(int sourceValue, int destinationValue, int duration) {
        return createCubicBezierMotion(sourceValue, destinationValue, duration, 0f, 0.42f, 1f, 1f);
    }

    /// Equivalent to createCubicBezierMotion with 0f, 0f, 0.58f, 1.0f as arguments.
    ///
    /// #### Parameters
    ///
    /// - `sourceValue`: starting value
    ///
    /// - `destinationValue`: destination value
    ///
    /// - `duration`: motion duration
    ///
    /// #### Returns
    ///
    /// Motion instance
    public static Motion createEaseOutMotion(int sourceValue, int destinationValue, int duration) {
        return createCubicBezierMotion(sourceValue, destinationValue, duration, 0f, 0f, 0.58f, 1.0f);
    }

    /// Creates a linear motion starting from source value all the way to destination value
    ///
    /// #### Parameters
    ///
    /// - `sourceValue`: the number from which we are starting (usually indicating animation start position)
    ///
    /// - `destinationValue`: the number to which we are heading (usually indicating animation destination)
    ///
    /// - `duration`: @param duration         the length in milliseconds of the motion (time it takes to get from sourceValue to
    /// destinationValue)
    ///
    /// #### Returns
    ///
    /// new motion object
    public static Motion createLinearMotion(int sourceValue, int destinationValue, int duration) {
        Motion l = new Motion(sourceValue, destinationValue, duration);
        l.motionType = LINEAR;
        return l;
    }

    /// Creates a linear motion starting from source value all the way to destination value for a color value.
    /// Unlike a regular linear motion a color linear motion is shifted based on channels where red, green & blue
    /// get shifted separately.
    ///
    /// #### Parameters
    ///
    /// - `sourceValue`: the color from which we are starting
    ///
    /// - `destinationValue`: the destination color
    ///
    /// - `duration`: @param duration         the length in milliseconds of the motion (time it takes to get from sourceValue to
    /// destinationValue)
    ///
    /// #### Returns
    ///
    /// new motion object
    public static Motion createLinearColorMotion(int sourceValue, int destinationValue, int duration) {
        Motion l = new Motion(sourceValue, destinationValue, duration);
        l.motionType = COLOR_LINEAR;
        return l;
    }

    /// Creates a spline motion starting from source value all the way to destination value
    ///
    /// #### Parameters
    ///
    /// - `sourceValue`: the number from which we are starting (usually indicating animation start position)
    ///
    /// - `destinationValue`: the number to which we are heading (usually indicating animation destination)
    ///
    /// - `duration`: @param duration         the length in milliseconds of the motion (time it takes to get from sourceValue to
    /// destinationValue)
    ///
    /// #### Returns
    ///
    /// new motion object
    public static Motion createSplineMotion(int sourceValue, int destinationValue, int duration) {
        Motion spline = new Motion(sourceValue, destinationValue, duration);
        spline.motionType = SPLINE;
        return spline;
    }

    /// Creates a deceleration motion starting from source value all the way to destination value
    ///
    /// #### Parameters
    ///
    /// - `sourceValue`: the number from which we are starting (usually indicating animation start position)
    ///
    /// - `destinationValue`: the number to which we are heading (usually indicating animation destination)
    ///
    /// - `duration`: @param duration         the length in milliseconds of the motion (time it takes to get from sourceValue to
    /// destinationValue)
    ///
    /// #### Returns
    ///
    /// new motion object
    public static Motion createDecelerationMotion(int sourceValue, int destinationValue, int duration) {
        Motion deceleration = new Motion(sourceValue, destinationValue, duration);
        deceleration.motionType = DECELERATION;
        return deceleration;
    }

    /// Creates a critically-damped spring motion from source to destination. This is the
    /// envelope of a second-order critically damped system step response:
    /// `x(t) = dst - (dst - src) * (1 + w*t) * e^(-w*t)` where w is chosen so the residual
    /// at t=duration is about 2%. Produces a quick initial approach with a soft settling
    /// tail, closer in feel to the iOS rubber-band snap-back than the quadratic
    /// `createDecelerationMotion` curve.
    ///
    /// #### Parameters
    ///
    /// - `sourceValue`: the number from which we are starting
    ///
    /// - `destinationValue`: the number to which we are heading
    ///
    /// - `duration`: the length in milliseconds of the motion
    ///
    /// #### Returns
    ///
    /// new motion object
    public static Motion createCriticalDampedSpringMotion(int sourceValue, int destinationValue, int duration) {
        Motion m = new Motion(sourceValue, destinationValue, duration);
        m.motionType = CRITICAL_DAMPED_SPRING;
        return m;
    }

    /// Creates a deceleration motion starting from the current position of another motion.
    ///
    /// #### Parameters
    ///
    /// - `motion`: the number from which we are starting (usually indicating animation start position)
    ///
    /// - `maxDestinationValue`: The farthest position to allow motion to go.
    ///
    /// - `maxDuration`: The longest that the duration is allowed to proceed for.
    ///
    /// #### Returns
    ///
    /// new motion object
    public static Motion createDecelerationMotionFrom(Motion motion, int maxDestinationValue, int maxDuration) {
        return createDecelerationMotion(
                motion.lastReturnedValue,
                motion.destinationValue < motion.sourceValue
                        ? Math.min(motion.destinationValue, maxDestinationValue)
                        : Math.max(motion.destinationValue, maxDestinationValue),
                (int) Math.min(maxDuration, motion.duration - (AnimationTime.now() - motion.startTime))
        );
    }

    /// Creates a friction motion starting from source with initial speed and the friction
    ///
    /// #### Parameters
    ///
    /// - `sourceValue`: the number from which we are starting (usually indicating animation start position)
    ///
    /// - `maxValue`: the maximum value for the friction
    ///
    /// - `initVelocity`: the starting velocity
    ///
    /// - `friction`: the motion friction
    ///
    /// #### Returns
    ///
    /// new motion object
    public static Motion createFrictionMotion(int sourceValue, int maxValue, float initVelocity, float friction) {
        Motion frictionMotion = new Motion(sourceValue, initVelocity, friction);
        frictionMotion.destinationValue = maxValue;
        frictionMotion.motionType = FRICTION;
        return frictionMotion;
    }

    public static Motion createExponentialDecayMotion(int sourceValue, int maxValue, double initVelocity, double timeConstant) {
        Motion decayMotion = new Motion(sourceValue, initVelocity, timeConstant);
        decayMotion.destinationValue = maxValue;
        decayMotion.targetPosition = sourceValue + (int) (initVelocity * (double) UIManager.getInstance().getThemeConstant("DecayMotionScaleFactorInt", 950));
        decayMotion.motionType = EXPONENTIAL_DECAY;
        decayMotion.duration = (int) (6 * timeConstant);
        return decayMotion;

    }

    /// Sends the motion to the end time instantly which is useful for flushing an animation
    public void finish() {
        if (!isFinished()) {
            startTime = AnimationTime.now() - duration;
            currentMotionTime = -1;
            previousCurrentMotionTime = -1;
        }
    }

    /// Sets the start time to the current time
    public void start() {
        startTime = AnimationTime.now();
    }

    /// Returns the current time within the motion relative to start time
    ///
    /// #### Returns
    ///
    /// long value representing AnimationTime.now() - startTime
    public long getCurrentMotionTime() {
        if (currentMotionTime < 0) {
            return AnimationTime.now() - startTime;
        }
        return currentMotionTime;
    }

    /// Allows overriding the getCurrentMotionTime method value with a manual value
    /// to provide full developer control over animation speed/position.
    ///
    /// #### Parameters
    ///
    /// - `currentMotionTime`: the time in milliseconds for the motion.
    public void setCurrentMotionTime(long currentMotionTime) {
        this.previousCurrentMotionTime = this.currentMotionTime;
        this.currentMotionTime = currentMotionTime;

        // workaround allowing the motion to be restarted when manually setting the current time
        if (lastReturnedValue == destinationValue) {
            lastReturnedValue = sourceValue;
        }
    }

    public boolean isDecayMotion() {
        return motionType == EXPONENTIAL_DECAY;
    }

    /// Returns true if the motion has run its course and has finished meaning the current
    /// time is greater than startTime + duration.
    ///
    /// #### Returns
    ///
    /// true if AnimationTime.now() > duration + startTime or the last returned value is the destination value
    public boolean isFinished() {
        return getCurrentMotionTime() > duration || destinationValue == lastReturnedValue || (EXPONENTIAL_DECAY == motionType && previousLastReturnedValue[0] == lastReturnedValue);
    }

    private int getSplineValue() {
        //make sure we reach the destination value.
        if (isFinished()) {
            return destinationValue;
        }
        float totalTime = duration;
        float currentTime = (int) getCurrentMotionTime();
        if (currentMotionTime > -1) {
            currentTime -= startTime;
            totalTime -= startTime;
        }
        currentTime = Math.min(currentTime, totalTime);
        int p = Math.abs(destinationValue - sourceValue);
        float centerTime = totalTime / 2;
        float l = p / (centerTime * centerTime);
        int x;
        if (sourceValue < destinationValue) {
            if (currentTime > centerTime) {
                x = sourceValue + (int) (l * (-centerTime * centerTime + 2 * centerTime * currentTime -
                        currentTime * currentTime / 2));
            } else {
                x = sourceValue + (int) (l * currentTime * currentTime / 2);
            }
        } else {
            currentTime = totalTime - currentTime;
            if (currentTime > centerTime) {
                x = destinationValue + (int) (l * (-centerTime * centerTime + 2 * centerTime * currentTime -
                        currentTime * currentTime / 2));
            } else {
                x = destinationValue + (int) (l * currentTime * currentTime / 2);
            }
        }
        return x;
    }

    private int getThreePointCubicValue() {
        if (isFinished()) {
            return destinationValue;
        }
        float totalTime = duration;
        float currentTime = Math.min((int) getCurrentMotionTime(), (int) totalTime);
        if (currentTime < 0f) {
            currentTime = 0f;
        }
        float t = currentTime / totalTime;

        // Each segment is solved in its OWN normalized space: the controls are expressed
        // relative to the segment's start and divided by its extent, so the solver below
        // sees an ordinary cubic-bezier from (0,0) to (1,1). The result is then scaled
        // back, which is what makes the two halves meet exactly at the midpoint instead
        // of stepping there.
        boolean first = t < midX;
        float scaleX = first ? midX : 1f - midX;
        float scaleY = first ? midY : 1f - midY;
        float value;
        if (scaleX <= 0f) {
            // A segment with no duration is only ever reached at its end.
            value = first ? midY : 1f;
        } else if (scaleY <= 0f) {
            // No vertical extent -- the midpoint lies on the top or bottom edge -- so the
            // Y axis cannot be normalized. Substituting linear progress here made the
            // value run toward t and then jump back at the join. X still normalizes, so
            // solve for the curve parameter as usual and evaluate this segment's Y cubic
            // from its real endpoints and controls.
            float scaledT = (t - (first ? 0f : midX)) / scaleX;
            float x1 = first ? p0 / scaleX : (q0 - midX) / scaleX;
            float x2 = first ? p2 / scaleX : (q2 - midX) / scaleX;
            float u = solveBezierForT(scaledT, x1, x2);
            float y0 = first ? 0f : midY;
            float y3 = first ? midY : 1f;
            float c1 = first ? p1 : q1;
            float c2 = first ? p3 : q3;
            float inv = 1f - u;
            value = inv * inv * inv * y0 + 3f * inv * inv * u * c1 + 3f * inv * u * u * c2 + u * u * u * y3;
        } else {
            float scaledT = (t - (first ? 0f : midX)) / scaleX;
            float x1;
            float y1;
            float x2;
            float y2;
            if (first) {
                x1 = p0 / scaleX;
                y1 = p1 / scaleY;
                x2 = p2 / scaleX;
                y2 = p3 / scaleY;
            } else {
                x1 = (q0 - midX) / scaleX;
                y1 = (q1 - midY) / scaleY;
                x2 = (q2 - midX) / scaleX;
                y2 = (q3 - midY) / scaleY;
            }
            float u = solveBezierForT(scaledT, x1, x2);
            value = bezierAxis(u, y1, y2) * scaleY + (first ? 0f : midY);
        }

        float dis = Math.abs(destinationValue - sourceValue);
        if (destinationValue > sourceValue) {
            return sourceValue + (int) (value * dis);
        }
        return sourceValue - (int) (value * dis);
    }

    private int getCubicValue() {
        //make sure we reach the destination value.
        if (isFinished()) {
            return destinationValue;
        }
        // getCurrentMotionTime() is already motion-relative -- it returns
        // currentMotionTime when set or (AnimationTime.now() - startTime)
        // otherwise. The previous implementation then subtracted startTime
        // twice in two duplicated `if (currentMotionTime > -1)` blocks,
        // which produced nonsense for any motion driven by
        // setCurrentMotionTime(). Just clamp to [0, duration]. See #1524.
        float totalTime = duration;
        float currentTime = Math.min((int) getCurrentMotionTime(), (int) totalTime);
        if (currentTime < 0f) {
            currentTime = 0f;
        }
        float dis = Math.abs(destinationValue - sourceValue);
        float t = currentTime / totalTime;

        // CSS-style cubic-bezier(x1, y1, x2, y2): the four parameters are the
        // X and Y coordinates of the two control points. P0=(0,0) and
        // P3=(1,1) are implicit. The previous implementation just plugged
        // the four floats into a 1D Bernstein basis polynomial directly,
        // which is not the same curve -- e.g. cubic-bezier(0, 0, 0.75, 1)
        // returned 0.41 at t=0.5 instead of the CSS-correct ~0.62. See
        // #1524. Implement the standard solver: find u such that Bx(u) = t,
        // then evaluate By(u).
        float x1 = p0;
        float y1 = p1;
        float x2 = p2;
        float y2 = p3;
        float u = solveBezierForT(t, x1, x2);
        float value = bezierAxis(u, y1, y2);

        int current;
        if (destinationValue > sourceValue) {
            current = sourceValue + (int) (value * dis);
        } else {
            current = sourceValue - (int) (value * dis);
        }
        return current;
    }

    /// Evaluates one coordinate of the implicit CSS cubic-bezier curve
    /// (P0=0, P3=1, with control coordinates {@code c1} and {@code c2}) at
    /// parameter {@code u} in [0,1]. The Bernstein-basis expansion of
    /// {@code (1-u)^3*0 + 3*(1-u)^2*u*c1 + 3*(1-u)*u^2*c2 + u^3*1}.
    private static float bezierAxis(float u, float c1, float c2) {
        float omu = 1f - u;
        return 3f * omu * omu * u * c1
                + 3f * omu * u * u * c2
                + u * u * u;
    }

    /// Derivative dBx/du of {@link #bezierAxis(float, float, float)}.
    private static float bezierAxisDerivative(float u, float c1, float c2) {
        float omu = 1f - u;
        return 3f * omu * omu * c1
                + 6f * omu * u * (c2 - c1)
                + 3f * u * u * (1f - c2);
    }

    /// Solves {@code Bx(u) = t} for {@code u} using a few Newton-Raphson
    /// iterations and falls back to bisection if Newton-Raphson stalls
    /// (e.g. for nearly-degenerate control points whose derivative is 0).
    /// Same general technique CSS engines use for cubic-bezier timing
    /// functions.
    private static float solveBezierForT(float t, float x1, float x2) {
        if (t <= 0f) {
            return 0f;
        }
        if (t >= 1f) {
            return 1f;
        }
        // The CSS cubic-bezier domain requires x1 and x2 in [0,1] for a
        // monotonic x(u); guard so a caller passing values outside that
        // range still gets a sane curve by clamping.
        float cx1 = Math.max(0f, Math.min(1f, x1));
        float cx2 = Math.max(0f, Math.min(1f, x2));

        float u = t;
        for (int i = 0; i < 8; i++) {
            float bxAtU = bezierAxis(u, cx1, cx2);
            float diff = bxAtU - t;
            if (Math.abs(diff) < 1e-6f) {
                return u;
            }
            float deriv = bezierAxisDerivative(u, cx1, cx2);
            if (deriv == 0f) {
                break;
            }
            u -= diff / deriv;
            if (u < 0f) {
                u = 0f;
            } else if (u > 1f) {
                u = 1f;
            }
        }

        // Bisection fallback - guaranteed to converge given Bx is
        // monotonically non-decreasing on [0,1] when 0 <= cx1, cx2 <= 1.
        float lo = 0f;
        float hi = 1f;
        u = t;
        for (int i = 0; i < 16; i++) {
            float bxAtU = bezierAxis(u, cx1, cx2);
            if (Math.abs(bxAtU - t) < 1e-5f) {
                return u;
            }
            if (bxAtU < t) {
                lo = u;
            } else {
                hi = u;
            }
            u = 0.5f * (lo + hi);
        }
        return u;
    }

    /// Returns the value for the motion for the current clock time.
    /// The value is dependent on the Motion type.
    ///
    /// #### Returns
    ///
    /// a value that is relative to the source value
    public int getValue() {
        if (currentMotionTime > -1 && startTime > getCurrentMotionTime()) {
            return sourceValue;
        }

        previousLastReturnedValue[0] = previousLastReturnedValue[1];
        previousLastReturnedValueTime[0] = previousLastReturnedValueTime[1];
        previousLastReturnedValue[1] = previousLastReturnedValue[2];
        previousLastReturnedValueTime[1] = previousLastReturnedValueTime[2];
        previousLastReturnedValue[2] = lastReturnedValue;
        previousLastReturnedValueTime[2] = previousCurrentMotionTime;
        if (previousCurrentMotionTime < 0) {
            previousCurrentMotionTime = getCurrentMotionTime();
        }
        switch (motionType) {
            case SPLINE:
                lastReturnedValue = getSplineValue();
                break;
            case CUBIC:
                lastReturnedValue = getCubicValue();
                break;
            case THREE_POINT_CUBIC:
                lastReturnedValue = getThreePointCubicValue();
                break;
            case FRICTION:
                lastReturnedValue = getFriction();
                break;
            case DECELERATION:
                lastReturnedValue = getRubber();
                break;
            case COLOR_LINEAR:
                lastReturnedValue = getColorLinear();
                break;
            case EXPONENTIAL_DECAY:
                lastReturnedValue = getExponentialDecay();
                break;
            case CRITICAL_DAMPED_SPRING:
                lastReturnedValue = getCriticalDampedSpring();
                break;
            default:
                lastReturnedValue = getLinear();
                break;
        }
        return lastReturnedValue;
    }

    /// Gets an approximation of the current velocity in pixels per millisecond.
    ///
    /// NOTE: If `#countAvailableVelocitySamplingPoints()`  0
    ///
    /// #### Returns
    ///
    /// Current velocity in pixels per millisecond.
    ///
    public double getVelocity() {
        final long localCurrentMotionTime = getCurrentMotionTime();
        final int lastReturnedValueLocal = lastReturnedValue;
        double velocity = 0;
        boolean firstIteration = true;
        for (int i = 2; i >= 0; i--) {
            final long t = previousLastReturnedValueTime[i];
            if (t <= 0 || localCurrentMotionTime == t) {
                break;
            }
            final int valueAtT = previousLastReturnedValue[i];
            final double spotVelocity = (lastReturnedValueLocal - valueAtT) / (double) (localCurrentMotionTime - t);
            velocity = firstIteration ? spotVelocity : (velocity + spotVelocity) / 2.0;
            firstIteration = false;
        }

        return velocity;
    }

    /// Gets the number of sampling points that can be used by `#getVelocity()`.  A minimum of 2 sampling
    /// points are required for the result of `#getVelocity()` to have any meaning.
    ///
    /// #### Returns
    ///
    /// The number of sampling points that can be used by `#getVelocity()`.
    ///
    public int countAvailableVelocitySamplingPoints() {
        int count = 1;
        final long localCurrentMotionTime = getCurrentMotionTime();
        for (int i = 2; i >= 0; i--) {
            final long t = previousLastReturnedValueTime[i];
            if (t <= 0 || localCurrentMotionTime == t) {
                break;
            }
            count++;
        }

        return count;
    }

    private int getLinear() {
        //make sure we reach the destination value.
        if (isFinished()) {
            return destinationValue;
        }
        float totalTime = duration;
        float currentTime = (int) getCurrentMotionTime();
        if (currentMotionTime > -1) {
            currentTime -= startTime;
            totalTime -= startTime;
        }
        int dis = destinationValue - sourceValue;
        int val = (int) (sourceValue + (currentTime / totalTime * dis));

        if (destinationValue < sourceValue) {
            return Math.max(destinationValue, val);
        } else {
            return Math.min(destinationValue, val);
        }
    }

    private int getColorLinear() {
        if (isFinished()) {
            return destinationValue;
        }
        float totalTime = duration;
        float currentTime = (int) getCurrentMotionTime();
        if (currentMotionTime > -1) {
            currentTime -= startTime;
            totalTime -= startTime;
        }

        int sourceR = (sourceValue >> 16) & 0xff;
        int destR = (destinationValue >> 16) & 0xff;
        int sourceG = (sourceValue >> 8) & 0xff;
        int destG = (destinationValue >> 8) & 0xff;
        int sourceB = sourceValue & 0xff;
        int destB = destinationValue & 0xff;

        int disR = destR - sourceR;
        int disG = destG - sourceG;
        int disB = destB - sourceB;
        int valR = (int) (sourceR + (currentTime / totalTime * disR));
        int valG = (int) (sourceG + (currentTime / totalTime * disG));
        int valB = (int) (sourceB + (currentTime / totalTime * disB));

        if (destR < sourceR) {
            valR = Math.max(destR, valR);
        } else {
            valR = Math.min(destR, valR);
        }

        if (destG < sourceG) {
            valG = Math.max(destG, valG);
        } else {
            valG = Math.min(destG, valG);
        }

        if (destB < sourceB) {
            valB = Math.max(destB, valB);
        } else {
            valB = Math.min(destB, valB);
        }
        return (((valR) << 16) & 0xff0000) | (((valG) << 8) & 0xff00) | (valB & 0xff);
    }

    private int getFriction() {
        int time = (int) getCurrentMotionTime();
        int retVal = 0;

        retVal = (int) ((Math.abs(initVelocity) * time) - (friction * (((double) time * time) / 2)));
        if (initVelocity < 0) {
            retVal *= -1;
        }
        retVal += sourceValue;
        if (destinationValue > sourceValue) {
            return Math.min(retVal, destinationValue);
        } else {
            return Math.max(retVal, destinationValue);
        }
    }

    private int getExponentialDecay() {
        double elapsed = getCurrentMotionTime();
        double timeConstant = friction;
        double amplitude = targetPosition - sourceValue;
        int position = (int) Math.round(targetPosition - amplitude * MathUtil.exp(-elapsed / timeConstant));
        if (destinationValue > sourceValue) {
            return Math.min(position, destinationValue);
        } else {
            return Math.max(position, destinationValue);
        }
    }

    private int getRubber() {
        if (isFinished()) {
            return destinationValue;
        }
        float totalTime = duration;
        float currentTime = (int) getCurrentMotionTime();
        if (currentMotionTime > -1) {
            currentTime -= startTime;
            totalTime -= startTime;
        }
        currentTime = Math.min(currentTime, totalTime);
        int p = Math.abs(destinationValue - sourceValue);
        float centerTime = totalTime / 2;
        float l = p / (centerTime * centerTime);
        int x;
        int dis = (int) (l * (-centerTime * centerTime + 2 * centerTime * currentTime -
                currentTime * currentTime / 2));

        if (sourceValue < destinationValue) {
            x = Math.max(sourceValue, sourceValue + dis);
            x = Math.min(destinationValue, x);

        } else {
            x = Math.min(sourceValue, sourceValue - dis);
            x = Math.max(destinationValue, x);
        }
        return x;
    }

    // Critically damped second-order step response: produces a fast initial approach
    // with a soft tail, no overshoot. Omega*duration = 5.83 so residual is ~2% at t=duration.
    private static final double CRITICAL_DAMPED_OMEGA_T = 5.83d;

    private int getCriticalDampedSpring() {
        if (isFinished()) {
            return destinationValue;
        }
        float totalTime = duration;
        float currentTime = (int) getCurrentMotionTime();
        if (currentMotionTime > -1) {
            currentTime -= startTime;
            totalTime -= startTime;
        }
        if (totalTime <= 0) {
            return destinationValue;
        }
        if (currentTime > totalTime) {
            currentTime = totalTime;
        }
        double omegaT = CRITICAL_DAMPED_OMEGA_T * (currentTime / (double) totalTime);
        double residual = (1.0d + omegaT) * MathUtil.exp(-omegaT);
        return (int) Math.round(destinationValue - (destinationValue - sourceValue) * residual);
    }

    /// The number from which we are starting (usually indicating animation start position)
    ///
    /// #### Returns
    ///
    /// the source value
    public int getSourceValue() {
        return sourceValue;
    }

    /// The number from which we are starting (usually indicating animation start position)
    ///
    /// #### Parameters
    ///
    /// - `sourceValue`: the source value
    public void setSourceValue(int sourceValue) {
        this.sourceValue = sourceValue;
    }

    /// The number to which we will reach when the motion is finished
    ///
    /// #### Returns
    ///
    /// the source value
    public int getDestinationValue() {
        return destinationValue;
    }

    /// The value of System.currentTimemillis() when motion was started
    ///
    /// #### Returns
    ///
    /// the start time
    protected long getStartTime() {
        return startTime;
    }

    /// Sets the start time of the motion
    ///
    /// #### Parameters
    ///
    /// - `startTime`: the starting time
    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    /// Returns the animation duration
    ///
    /// #### Returns
    ///
    /// animation duration in milliseconds
    public int getDuration() {
        return duration;
    }
}
