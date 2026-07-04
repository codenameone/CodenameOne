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
package com.codename1.vr;

import com.codename1.gpu.Quaternion;
import com.codename1.sensors.MotionEvent;
import com.codename1.sensors.MotionSensor;
import com.codename1.sensors.MotionSensorListener;
import com.codename1.sensors.MotionSensorManager;

/// Tracks the device orientation for VR by feeding the motion sensors
/// (gyroscope, accelerometer and magnetometer through
/// `com.codename1.sensors.MotionSensorManager`) into an `OrientationFilter`.
///
/// The gyroscope drives the orientation; without one the tracker falls back
/// to accelerometer tilt (stable pitch and roll, no reliable yaw). Sensor
/// hardware runs only between `#start()` and `#stop()` - the sensors API
/// reference-counts listeners, so a stopped tracker costs no battery.
///
/// `#getOrientation(float[])` is thread safe: sensor events arrive on the
/// EDT while the render thread reads the latest snapshot.
public final class HeadTracker {
    private final MotionSensorManager manager;
    private final OrientationFilter filter = new OrientationFilter();
    private final Object lock = new Object();
    private final float[] snapshot = Quaternion.identity();

    private MotionSensor gyro;
    private MotionSensor accel;
    private MotionSensor mag;
    private MotionSensorListener gyroListener;
    private MotionSensorListener accelListener;
    private MotionSensorListener magListener;

    private long lastTimestamp;
    private float ax;
    private float ay;
    private float az;
    private boolean hasAccel;
    private float mx = Float.NaN;
    private float my = Float.NaN;
    private float mz = Float.NaN;
    private boolean started;

    /// Creates a tracker over the platform motion sensors.
    public HeadTracker() {
        this(MotionSensorManager.getInstance());
    }

    /// Test hook: creates a tracker over the supplied manager.
    HeadTracker(MotionSensorManager manager) {
        this.manager = manager;
    }

    /// True when the device has the sensors head tracking needs: a gyroscope,
    /// or at least an accelerometer for tilt-only tracking.
    public static boolean isSupported() {
        MotionSensorManager m = MotionSensorManager.getInstance();
        return m.isSensorSupported(MotionSensorManager.TYPE_GYROSCOPE)
                || m.isSensorSupported(MotionSensorManager.TYPE_ACCELEROMETER);
    }

    /// The fusion filter behind this tracker, exposed to tune the
    /// gyro/reference blend. Configure before `#start()`.
    public OrientationFilter getFilter() {
        return filter;
    }

    /// Starts the sensors and orientation updates. Idempotent.
    public void start() {
        if (started) {
            return;
        }
        started = true;
        lastTimestamp = 0;
        boolean hasGyro = manager.isSensorSupported(MotionSensorManager.TYPE_GYROSCOPE);
        if (hasGyro) {
            gyro = manager.getSensor(MotionSensorManager.TYPE_GYROSCOPE);
            gyroListener = new MotionSensorListener() {
                @Override public void motionReceived(MotionEvent evt) {
                    step(evt.getX(), evt.getY(), evt.getZ(), evt.getTimestamp());
                }
            };
            gyro.addListener(gyroListener);
        }
        if (manager.isSensorSupported(MotionSensorManager.TYPE_ACCELEROMETER)) {
            accel = manager.getSensor(MotionSensorManager.TYPE_ACCELEROMETER);
            final boolean drivesFilter = !hasGyro;
            accelListener = new MotionSensorListener() {
                @Override public void motionReceived(MotionEvent evt) {
                    synchronized (lock) {
                        ax = evt.getX();
                        ay = evt.getY();
                        az = evt.getZ();
                        hasAccel = true;
                    }
                    if (drivesFilter) {
                        // No gyroscope: the accelerometer cadence drives the
                        // filter with zero rotation rates, converging tilt.
                        step(0f, 0f, 0f, evt.getTimestamp());
                    }
                }
            };
            accel.addListener(accelListener);
        }
        if (manager.isSensorSupported(MotionSensorManager.TYPE_MAGNETOMETER)) {
            mag = manager.getSensor(MotionSensorManager.TYPE_MAGNETOMETER);
            magListener = new MotionSensorListener() {
                @Override public void motionReceived(MotionEvent evt) {
                    synchronized (lock) {
                        mx = evt.getX();
                        my = evt.getY();
                        mz = evt.getZ();
                    }
                }
            };
            mag.addListener(magListener);
        }
    }

    /// Stops the sensors. Idempotent; the last orientation remains readable.
    public void stop() {
        if (!started) {
            return;
        }
        started = false;
        if (gyro != null && gyroListener != null) {
            gyro.removeListener(gyroListener);
        }
        if (accel != null && accelListener != null) {
            accel.removeListener(accelListener);
        }
        if (mag != null && magListener != null) {
            mag.removeListener(magListener);
        }
        gyroListener = null;
        accelListener = null;
        magListener = null;
    }

    /// True between `#start()` and `#stop()`.
    public boolean isStarted() {
        return started;
    }

    /// Rotates the orientation so the current view direction becomes
    /// "straight ahead", keeping pitch and roll.
    public void recenter() {
        synchronized (lock) {
            filter.recenterYaw();
            filter.getOrientation(snapshot);
        }
    }

    /// Copies the latest orientation quaternion into `quatOut4` as
    /// `{x, y, z, w}`. Thread safe; intended to be read from the render
    /// thread.
    public void getOrientation(float[] quatOut4) {
        synchronized (lock) {
            Quaternion.copy(snapshot, quatOut4);
        }
    }

    private void step(float gx, float gy, float gz, long timestampMillis) {
        synchronized (lock) {
            if (lastTimestamp != 0 && timestampMillis > lastTimestamp) {
                float dt = (timestampMillis - lastTimestamp) / 1000f;
                float sax = hasAccel ? ax : 0f;
                float say = hasAccel ? ay : 0f;
                float saz = hasAccel ? az : 0f;
                filter.update(gx, gy, gz, sax, say, saz, mx, my, mz, dt);
                filter.getOrientation(snapshot);
            }
            lastTimestamp = timestampMillis;
        }
    }
}
