/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.sensors;

import com.codename1.util.MathUtil;

import java.util.ArrayList;
import java.util.List;

/// Platform independent gesture recognizer. It is fed a stream of accelerometer
/// samples together with a low pass estimate of the gravity vector and returns
/// the gestures it recognizes. Keeping the recognition here means every port
/// that can produce accelerometer readings gets the full gesture set for free.
///
/// This class is intentionally free of any framework dependency (no EDT, no
/// `Display`) so it can be unit tested with synthetic data. All tuning
/// constants are package visible and surfaced through setters on
/// [MotionSensorManager].
class GestureEngine {

    private static final double G = MotionSensorManager.STANDARD_GRAVITY;
    // Sentinel for "this gesture has not fired yet". Comparisons guard against
    // it explicitly so we never compute time - TIME_NONE (which would overflow).
    private static final long TIME_NONE = Long.MIN_VALUE;

    // ---- shake tuning ----
    double shakeThreshold = 12.0;            // linear acceleration spike, m/s^2
    long shakeWindowMs = 800;                // jolts must fall inside this window
    int shakeJoltsNeeded = 3;                // number of jolts that make a shake
    long shakeCooldownMs = 1000;             // minimum gap between two shakes

    // ---- flip tuning ----
    double flipGravityFraction = 0.72;       // fraction of gravity on z to be flat
    long flipStableMs = 350;                 // orientation must hold this long

    // ---- tilt tuning ----
    double tiltEnterAngle = 0.6;             // ~34 degrees, radians
    double tiltExitAngle = 0.35;             // hysteresis back to flat, radians

    // ---- pick up tuning ----
    double pickUpRestLinear = 0.7;           // max linear accel while at rest
    long pickUpRestMs = 350;                 // time at rest before a pick up counts
    double pickUpMoveLinear = 2.8;           // movement that triggers the pick up
    long pickUpCooldownMs = 1500;

    // ---- free fall tuning ----
    double freeFallThreshold = 3.0;          // total acceleration, m/s^2
    long freeFallMinMs = 80;                 // sustained for this long
    long freeFallCooldownMs = 1500;

    // ---- shake state ----
    private final ArrayList<Long> joltTimes = new ArrayList<Long>();
    private boolean shakeAbove;
    private double shakePeak;
    private long lastShakeTime = Long.MIN_VALUE;

    // ---- flip state ----
    private static final int FACE_UNKNOWN = 0;
    private static final int FACE_UP = 1;
    private static final int FACE_DOWN = 2;
    private int faceState = FACE_UNKNOWN;
    private int faceCandidate = FACE_UNKNOWN;
    private long faceCandidateSince;

    // ---- tilt state ----
    private static final int FLAT = 0;
    private static final int NEG = 1;
    private static final int POS = 2;
    private int rollState = FLAT;
    private int pitchState = FLAT;

    // ---- pick up state ----
    private boolean resting;
    private long restSince = Long.MIN_VALUE;
    private long lastPickUpTime = Long.MIN_VALUE;

    // ---- free fall state ----
    private long freeFallSince = Long.MIN_VALUE;
    private long lastFreeFallTime = Long.MIN_VALUE;

    /// Resets all internal state. Used when sampling is (re)started so that
    /// stale orientation does not produce a spurious gesture on the first sample.
    void reset() {
        joltTimes.clear();
        shakeAbove = false;
        shakePeak = 0;
        lastShakeTime = Long.MIN_VALUE;
        faceState = FACE_UNKNOWN;
        faceCandidate = FACE_UNKNOWN;
        faceCandidateSince = 0;
        rollState = FLAT;
        pitchState = FLAT;
        resting = false;
        restSince = Long.MIN_VALUE;
        lastPickUpTime = Long.MIN_VALUE;
        freeFallSince = Long.MIN_VALUE;
        lastFreeFallTime = Long.MIN_VALUE;
    }

    /// Feeds a single sample to the recognizer.
    ///
    /// #### Parameters
    ///
    /// - `ax`, `ay`, `az`: the raw accelerometer reading in m/s^2 including gravity
    /// - `gx`, `gy`, `gz`: a low pass estimate of the gravity vector in m/s^2
    /// - `time`: the sample time in milliseconds
    ///
    /// #### Returns
    ///
    /// the gestures recognized for this sample, never `null` (usually empty)
    List<GestureEvent> onSample(double ax, double ay, double az,
                                double gx, double gy, double gz, long time) {
        List<GestureEvent> events = new ArrayList<GestureEvent>(2);

        double lx = ax - gx;
        double ly = ay - gy;
        double lz = az - gz;
        double linearMag = Math.sqrt(lx * lx + ly * ly + lz * lz);
        double totalMag = Math.sqrt(ax * ax + ay * ay + az * az);

        detectShake(linearMag, time, events);
        detectFlip(gz, time, events);
        detectTilt(gx, gy, gz, time, events);
        detectPickUp(gz, linearMag, time, events);
        detectFreeFall(totalMag, time, events);

        return events;
    }

    private void detectShake(double linearMag, long time, List<GestureEvent> events) {
        if (linearMag > shakeThreshold) {
            if (!shakeAbove) {
                shakeAbove = true;
                shakePeak = linearMag;
                joltTimes.add(Long.valueOf(time));
            } else if (linearMag > shakePeak) {
                shakePeak = linearMag;
            }
        } else if (linearMag < shakeThreshold * 0.7) {
            shakeAbove = false;
        }

        long windowStart = time - shakeWindowMs;
        while (!joltTimes.isEmpty() && joltTimes.get(0).longValue() < windowStart) {
            joltTimes.remove(0);
        }

        if (joltTimes.size() >= shakeJoltsNeeded
                && (lastShakeTime == TIME_NONE || time - lastShakeTime >= shakeCooldownMs)) {
            lastShakeTime = time;
            double peak = shakePeak;
            joltTimes.clear();
            shakePeak = 0;
            events.add(new GestureEvent(GestureEvent.TYPE_SHAKE, peak, time));
        }
    }

    private void detectFlip(double gz, long time, List<GestureEvent> events) {
        double flat = G * flipGravityFraction;
        int instant;
        if (gz > flat) {
            instant = FACE_UP;
        } else if (gz < -flat) {
            instant = FACE_DOWN;
        } else {
            instant = FACE_UNKNOWN;
        }

        if (instant != faceCandidate) {
            faceCandidate = instant;
            faceCandidateSince = time;
            return;
        }
        if (instant == FACE_UNKNOWN || instant == faceState) {
            return;
        }
        if (time - faceCandidateSince >= flipStableMs) {
            faceState = instant;
            if (instant == FACE_DOWN) {
                events.add(new GestureEvent(GestureEvent.TYPE_FLIP_FACE_DOWN, gz, time));
            } else {
                events.add(new GestureEvent(GestureEvent.TYPE_FLIP_FACE_UP, gz, time));
            }
        }
    }

    private void detectTilt(double gx, double gy, double gz, long time, List<GestureEvent> events) {
        double horizontal = Math.sqrt(gx * gx + gz * gz);
        double roll = MathUtil.atan2(gx, gz);
        double pitch = MathUtil.atan2(-gy, horizontal);

        rollState = tiltAxis(roll, rollState, GestureEvent.TYPE_TILT_RIGHT,
                GestureEvent.TYPE_TILT_LEFT, time, events);
        pitchState = tiltAxis(pitch, pitchState, GestureEvent.TYPE_TILT_BACKWARD,
                GestureEvent.TYPE_TILT_FORWARD, time, events);
    }

    private int tiltAxis(double angle, int state, int posType, int negType,
                         long time, List<GestureEvent> events) {
        if (state == FLAT) {
            if (angle > tiltEnterAngle) {
                events.add(new GestureEvent(posType, angle, time));
                return POS;
            }
            if (angle < -tiltEnterAngle) {
                events.add(new GestureEvent(negType, angle, time));
                return NEG;
            }
            return FLAT;
        }
        if (Math.abs(angle) < tiltExitAngle) {
            return FLAT;
        }
        return state;
    }

    private void detectPickUp(double gz, double linearMag, long time, List<GestureEvent> events) {
        boolean flatFaceUp = gz > G * 0.8;
        if (flatFaceUp && linearMag < pickUpRestLinear) {
            if (restSince == Long.MIN_VALUE) {
                restSince = time;
            } else if (time - restSince >= pickUpRestMs) {
                resting = true;
            }
        } else {
            if (resting && linearMag > pickUpMoveLinear
                    && (lastPickUpTime == TIME_NONE || time - lastPickUpTime >= pickUpCooldownMs)) {
                lastPickUpTime = time;
                events.add(new GestureEvent(GestureEvent.TYPE_PICK_UP, linearMag, time));
            }
            resting = false;
            restSince = Long.MIN_VALUE;
        }
    }

    private void detectFreeFall(double totalMag, long time, List<GestureEvent> events) {
        if (totalMag < freeFallThreshold) {
            if (freeFallSince == Long.MIN_VALUE) {
                freeFallSince = time;
            } else if (time - freeFallSince >= freeFallMinMs
                    && (lastFreeFallTime == TIME_NONE || time - lastFreeFallTime >= freeFallCooldownMs)) {
                lastFreeFallTime = time;
                freeFallSince = Long.MIN_VALUE;
                events.add(new GestureEvent(GestureEvent.TYPE_FREE_FALL, totalMag, time));
            }
        } else {
            freeFallSince = Long.MIN_VALUE;
        }
    }
}
