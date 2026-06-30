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
package com.codename1.impl.javase;

import com.codename1.sensors.MotionSensorManager;

/**
 * Simulator implementation of the motion sensors. It synthesizes accelerometer,
 * gyroscope and magnetometer readings from a simulated device orientation (set
 * through the "Simulate &gt; Motion / Gesture Simulation" window) plus transient
 * bursts used to reproduce the shake, pick up and free fall gestures. The core
 * derives gravity, linear acceleration and orientation from these readings, so
 * every gesture can be exercised from the desktop without hardware.
 *
 * @author Codename One
 */
public class JavaSEMotionSensorManager extends MotionSensorManager {
    static final int TRANSIENT_NONE = 0;
    static final int TRANSIENT_SHAKE = 1;
    static final int TRANSIENT_FREE_FALL = 2;
    static final int TRANSIENT_PICK_UP = 3;

    private static volatile double simRollRad;
    private static volatile double simPitchRad;
    private static volatile int transientType;
    private static volatile long transientStart;
    private static volatile long transientDuration;

    /**
     * Sets the simulated device orientation.
     *
     * @param pitchDegrees forward/backward tilt in degrees
     * @param rollDegrees left/right tilt in degrees
     */
    public static void setOrientation(double pitchDegrees, double rollDegrees) {
        simPitchRad = Math.toRadians(pitchDegrees);
        simRollRad = Math.toRadians(rollDegrees);
    }

    /**
     * Injects a short oscillating burst so the core recognizes a shake.
     */
    public static void triggerShake() {
        transientType = TRANSIENT_SHAKE;
        transientStart = System.currentTimeMillis();
        transientDuration = 700;
    }

    /**
     * Drops the simulated acceleration to near zero so the core recognizes free
     * fall.
     */
    public static void triggerFreeFall() {
        transientType = TRANSIENT_FREE_FALL;
        transientStart = System.currentTimeMillis();
        transientDuration = 400;
    }

    /**
     * Injects a short movement burst so the core recognizes a pick up. The
     * device must have been at rest and roughly flat beforehand.
     */
    public static void triggerPickUp() {
        transientType = TRANSIENT_PICK_UP;
        transientStart = System.currentTimeMillis();
        transientDuration = 350;
    }

    @Override
    protected boolean isNativeSensorSupported(int type) {
        return type == TYPE_ACCELEROMETER || type == TYPE_GYROSCOPE || type == TYPE_MAGNETOMETER;
    }

    @Override
    protected void startNativeSensor(int type) {
    }

    @Override
    protected void stopNativeSensor(int type) {
    }

    @Override
    protected boolean readNativeSensor(int type, float[] out) {
        switch (type) {
            case TYPE_ACCELEROMETER:
                readAccelerometer(out);
                return true;
            case TYPE_GYROSCOPE:
                out[0] = 0;
                out[1] = 0;
                out[2] = 0;
                return true;
            case TYPE_MAGNETOMETER:
                // A constant northward field so the orientation azimuth resolves.
                out[0] = 0;
                out[1] = 30;
                out[2] = 0;
                return true;
            default:
                return false;
        }
    }

    private void readAccelerometer(float[] out) {
        double g = STANDARD_GRAVITY;
        double roll = simRollRad;
        double pitch = simPitchRad;
        double gx = g * Math.sin(roll);
        double gy = -g * Math.sin(pitch);
        double gz = g * Math.cos(roll) * Math.cos(pitch);

        int tt = transientType;
        if (tt != TRANSIENT_NONE) {
            long elapsed = System.currentTimeMillis() - transientStart;
            if (elapsed < transientDuration) {
                double t = elapsed / 1000.0;
                switch (tt) {
                    case TRANSIENT_SHAKE: {
                        double w = 2 * Math.PI * 8;
                        gx += Math.sin(w * t) * 22;
                        gy += Math.cos(w * t) * 22;
                        gz += Math.sin(2 * w * t) * 10;
                        break;
                    }
                    case TRANSIENT_FREE_FALL:
                        gx = 0;
                        gy = 0;
                        gz = 0;
                        break;
                    case TRANSIENT_PICK_UP:
                        gx += 4;
                        gz += 4;
                        break;
                    default:
                        break;
                }
            } else {
                transientType = TRANSIENT_NONE;
            }
        }

        out[0] = (float) gx;
        out[1] = (float) gy;
        out[2] = (float) gz;
    }
}
