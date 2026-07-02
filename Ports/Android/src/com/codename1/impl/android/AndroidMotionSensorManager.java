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
package com.codename1.impl.android;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import com.codename1.sensors.MotionSensorManager;

/**
 * Android implementation of the motion sensors backed by the platform
 * {@link android.hardware.SensorManager}. The hardware backed sensor types are
 * mapped to their Android equivalents; the core derives the orientation from the
 * gravity vector and the magnetometer. Android already reports acceleration in
 * m/s^2, angular velocity in rad/s and the magnetic field in microtesla using
 * the same axis convention the API documents, so no unit conversion is needed.
 *
 * @author Codename One
 */
public class AndroidMotionSensorManager extends MotionSensorManager {
    private static final int MAX_TYPE = 6;

    private final SensorManager sensorManager;
    private final Object lock = new Object();
    private final float[][] cached = new float[MAX_TYPE + 1][3];
    private final boolean[] hasData = new boolean[MAX_TYPE + 1];
    private final SensorEventListener[] listeners = new SensorEventListener[MAX_TYPE + 1];

    public AndroidMotionSensorManager(Context context) {
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
    }

    private int androidType(int type) {
        switch (type) {
            case TYPE_ACCELEROMETER:
                return Sensor.TYPE_ACCELEROMETER;
            case TYPE_LINEAR_ACCELERATION:
                return Sensor.TYPE_LINEAR_ACCELERATION;
            case TYPE_GRAVITY:
                return Sensor.TYPE_GRAVITY;
            case TYPE_GYROSCOPE:
                return Sensor.TYPE_GYROSCOPE;
            case TYPE_MAGNETOMETER:
                return Sensor.TYPE_MAGNETIC_FIELD;
            default:
                return -1;
        }
    }

    @Override
    protected boolean isNativeSensorSupported(int type) {
        if (sensorManager == null) {
            return false;
        }
        int a = androidType(type);
        if (a < 0) {
            return false;
        }
        return sensorManager.getDefaultSensor(a) != null;
    }

    @Override
    protected void startNativeSensor(final int type) {
        if (sensorManager == null) {
            return;
        }
        int a = androidType(type);
        if (a < 0) {
            return;
        }
        Sensor sensor = sensorManager.getDefaultSensor(a);
        if (sensor == null) {
            return;
        }
        SensorEventListener listener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                synchronized (lock) {
                    cached[type][0] = event.values[0];
                    cached[type][1] = event.values[1];
                    cached[type][2] = event.values[2];
                    hasData[type] = true;
                }
            }

            @Override
            public void onAccuracyChanged(Sensor s, int accuracy) {
            }
        };
        synchronized (lock) {
            listeners[type] = listener;
            hasData[type] = false;
        }
        sensorManager.registerListener(listener, sensor, getSamplingInterval() * 1000);
    }

    @Override
    protected void stopNativeSensor(int type) {
        if (sensorManager == null) {
            return;
        }
        SensorEventListener listener;
        synchronized (lock) {
            listener = listeners[type];
            listeners[type] = null;
            hasData[type] = false;
        }
        if (listener != null) {
            sensorManager.unregisterListener(listener);
        }
    }

    @Override
    protected boolean readNativeSensor(int type, float[] out) {
        if (type < 0 || type > MAX_TYPE) {
            return false;
        }
        synchronized (lock) {
            if (!hasData[type]) {
                return false;
            }
            out[0] = cached[type][0];
            out[1] = cached[type][1];
            out[2] = cached[type][2];
            return true;
        }
    }
}
