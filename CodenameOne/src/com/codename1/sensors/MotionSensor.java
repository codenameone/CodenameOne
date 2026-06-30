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

import java.util.ArrayList;

/// Represents a single motion sensor (for example the accelerometer or the
/// gyroscope). Obtain an instance from
/// [MotionSensorManager#getSensor(int)] and start receiving readings by
/// registering a [MotionSensorListener]:
///
/// ```java
/// MotionSensor accel = MotionSensorManager.getInstance().getSensor(MotionSensorManager.TYPE_ACCELEROMETER);
/// if (accel != null) {
///     accel.addListener(new MotionSensorListener() {
///         public void motionReceived(MotionEvent evt) {
///             label.setText("x=" + evt.getX() + " y=" + evt.getY() + " z=" + evt.getZ());
///         }
///     });
/// }
/// ```
///
/// Sampling is reference counted: the underlying hardware sensor is only active
/// while at least one listener is registered, so always remove your listener
/// (typically when the form is no longer showing) to conserve battery.
public class MotionSensor {
    private final MotionSensorManager manager;
    private final int type;
    private final ArrayList<MotionSensorListener> listeners = new ArrayList<MotionSensorListener>();
    private final float[] last = new float[3];
    private boolean hasReading;
    private long lastTimestamp;

    MotionSensor(MotionSensorManager manager, int type) {
        this.manager = manager;
        this.type = type;
    }

    /// The type of this sensor, one of the `MotionSensorManager.TYPE_*`
    /// constants.
    public int getType() {
        return type;
    }

    /// Whether this sensor is available on the current device. A sensor with no
    /// hardware backing can still be obtained but will never deliver readings.
    public boolean isSupported() {
        return manager.isSensorSupported(type);
    }

    /// Registers a listener that will receive readings on the EDT. Registering
    /// the first listener starts the underlying hardware sensor.
    ///
    /// #### Parameters
    ///
    /// - `l`: the listener to add, ignored if `null` or already registered
    public void addListener(MotionSensorListener l) {
        if (l == null) {
            return;
        }
        synchronized (listeners) {
            if (!listeners.contains(l)) {
                listeners.add(l);
            }
        }
        manager.sensorActivityChanged();
    }

    /// Removes a previously registered listener. Removing the last listener
    /// stops the underlying hardware sensor.
    ///
    /// #### Parameters
    ///
    /// - `l`: the listener to remove
    public void removeListener(MotionSensorListener l) {
        synchronized (listeners) {
            listeners.remove(l);
        }
        manager.sensorActivityChanged();
    }

    /// The most recent value of the x axis, or `0` if no reading has arrived yet.
    public float getX() {
        return last[0];
    }

    /// The most recent value of the y axis, or `0` if no reading has arrived yet.
    public float getY() {
        return last[1];
    }

    /// The most recent value of the z axis, or `0` if no reading has arrived yet.
    public float getZ() {
        return last[2];
    }

    /// Whether at least one reading has been received from this sensor.
    public boolean hasReading() {
        return hasReading;
    }

    /// The timestamp of the most recent reading in milliseconds.
    public long getTimestamp() {
        return lastTimestamp;
    }

    boolean hasListeners() {
        synchronized (listeners) {
            return !listeners.isEmpty();
        }
    }

    void dispatch(float x, float y, float z, long timestamp) {
        last[0] = x;
        last[1] = y;
        last[2] = z;
        lastTimestamp = timestamp;
        hasReading = true;
        MotionSensorListener[] snapshot;
        synchronized (listeners) {
            if (listeners.isEmpty()) {
                return;
            }
            snapshot = new MotionSensorListener[listeners.size()];
            listeners.toArray(snapshot);
        }
        manager.runOnEventThread(new DispatchEvent(snapshot, new MotionEvent(type, x, y, z, timestamp)));
    }

    // Delivers a reading to a snapshot of listeners on the EDT. A named static
    // class (rather than an anonymous one) since it only needs the captured
    // values, not the enclosing sensor.
    private static final class DispatchEvent implements Runnable {
        private final MotionSensorListener[] targets;
        private final MotionEvent evt;

        DispatchEvent(MotionSensorListener[] targets, MotionEvent evt) {
            this.targets = targets;
            this.evt = evt;
        }

        @Override
        public void run() {
            for (MotionSensorListener target : targets) {
                target.motionReceived(evt);
            }
        }
    }
}
