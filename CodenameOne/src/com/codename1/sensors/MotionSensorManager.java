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

import com.codename1.ui.Display;
import com.codename1.util.MathUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// Cross platform access to the device motion sensors (accelerometer,
/// gyroscope, magnetometer and the derived gravity, linear acceleration and
/// orientation) together with high level gesture detection (shake, flip, tilt,
/// pick up and free fall).
///
/// Obtain the singleton through [#getInstance()]. The instance is always non
/// `null`; on a device or port without motion sensors every sensor simply
/// reports as unsupported and no events are delivered.
///
/// #### Reading a sensor
///
/// ```java
/// MotionSensorManager m = MotionSensorManager.getInstance();
/// MotionSensor accel = m.getSensor(MotionSensorManager.TYPE_ACCELEROMETER);
/// if (accel != null) {
///     accel.addListener(new MotionSensorListener() {
///         public void motionReceived(MotionEvent evt) {
///             // evt.getX(), evt.getY(), evt.getZ() are in m/s^2
///         }
///     });
/// }
/// ```
///
/// #### Listening for a gesture
///
/// ```java
/// MotionSensorManager.getInstance().addGestureListener(GestureEvent.TYPE_SHAKE, new GestureListener() {
///     public void gestureDetected(GestureEvent evt) {
///         Dialog.show("Shaken", "You shook the device", "OK", null);
///     }
/// });
/// ```
///
/// All callbacks are delivered on the EDT. Sampling is reference counted so the
/// hardware sensors are only powered while at least one listener (sensor or
/// gesture) is registered; remember to remove your listeners to save battery.
///
/// **iOS note:** access to the motion hardware requires the build argument
/// `ios.NSMotionUsageDescription` describing why the app reads motion data.
public abstract class MotionSensorManager {

    /// Accelerometer including the effect of gravity, in meters per second
    /// squared.
    public static final int TYPE_ACCELEROMETER = 1;

    /// Acceleration with the effect of gravity removed, in meters per second
    /// squared. Derived in the core from the accelerometer when the platform
    /// does not provide it natively.
    public static final int TYPE_LINEAR_ACCELERATION = 2;

    /// The gravity vector in meters per second squared. Derived in the core
    /// from the accelerometer when the platform does not provide it natively.
    public static final int TYPE_GRAVITY = 3;

    /// Rate of rotation around the three axes in radians per second.
    public static final int TYPE_GYROSCOPE = 4;

    /// The ambient magnetic field in microtesla.
    public static final int TYPE_MAGNETOMETER = 5;

    /// Device orientation as azimuth, pitch and roll in radians. Derived in the
    /// core from the gravity vector (and the magnetometer for the azimuth).
    public static final int TYPE_ORIENTATION = 6;

    /// Standard earth gravity in meters per second squared, the unit used by the
    /// acceleration sensors.
    public static final double STANDARD_GRAVITY = 9.80665;

    private static final int MAX_TYPE = 6;
    private static final float GRAVITY_FILTER_ALPHA = 0.8f;

    private static MotionSensorManager unsupportedInstance;

    private final Object lock = new Object();
    private final Map<Integer, MotionSensor> sensors = new HashMap<Integer, MotionSensor>();
    private final Map<Integer, ArrayList<GestureListener>> gestureListeners =
            new HashMap<Integer, ArrayList<GestureListener>>();
    private final GestureEngine engine = new GestureEngine();

    private final boolean[] nativeStarted = new boolean[MAX_TYPE + 1];
    private final float[] readBuffer = new float[3];
    private final float[] gravityEstimate = new float[3];
    private boolean gravityInitialized;
    private boolean threadRunning;
    private int gestureListenerCount;
    private int samplingInterval = 50;

    private final Runnable pollRunnable = new Runnable() {
        @Override
        public void run() {
            pollLoop();
        }
    };

    /// Returns the shared motion sensor manager. Never `null`: when the active
    /// port does not provide motion sensors a no-op manager is returned whose
    /// sensors all report as unsupported.
    public static MotionSensorManager getInstance() {
        MotionSensorManager m = Display.getInstance().getMotionSensorManager();
        if (m != null) {
            return m;
        }
        synchronized (MotionSensorManager.class) {
            if (unsupportedInstance == null) {
                unsupportedInstance = new UnsupportedMotionSensorManager();
            }
            return unsupportedInstance;
        }
    }

    // ------------------------------------------------------------------
    // Hooks implemented by the platform ports
    // ------------------------------------------------------------------

    /// Whether the named hardware sensor is present on this device. Only the
    /// hardware backed types (`TYPE_ACCELEROMETER`, `TYPE_GYROSCOPE`,
    /// `TYPE_MAGNETOMETER` and, where the OS exposes them, `TYPE_GRAVITY` and
    /// `TYPE_LINEAR_ACCELERATION`) are queried here; derived types are resolved
    /// by [#isSensorSupported(int)].
    ///
    /// #### Parameters
    ///
    /// - `type`: one of the `TYPE_*` constants
    protected abstract boolean isNativeSensorSupported(int type);

    /// Starts the named hardware sensor. Called when the first listener that
    /// needs it is registered.
    protected abstract void startNativeSensor(int type);

    /// Stops the named hardware sensor. Called when the last listener that
    /// needed it is removed.
    protected abstract void stopNativeSensor(int type);

    /// Reads the latest value of the named hardware sensor.
    ///
    /// #### Parameters
    ///
    /// - `type`: one of the `TYPE_*` constants
    /// - `out`: a three element array to receive the x, y and z values
    ///
    /// #### Returns
    ///
    /// `true` if a value was written, `false` if no reading is available yet
    protected abstract boolean readNativeSensor(int type, float[] out);

    // ------------------------------------------------------------------
    // Public API
    // ------------------------------------------------------------------

    /// Whether the given sensor type can deliver readings on this device. Takes
    /// into account values that the core derives from other sensors, so for
    /// example `TYPE_ORIENTATION` is supported whenever the accelerometer is.
    ///
    /// #### Parameters
    ///
    /// - `type`: one of the `TYPE_*` constants
    public boolean isSensorSupported(int type) {
        switch (type) {
            case TYPE_ACCELEROMETER:
            case TYPE_GYROSCOPE:
            case TYPE_MAGNETOMETER:
                return isNativeSensorSupported(type);
            case TYPE_GRAVITY:
            case TYPE_LINEAR_ACCELERATION:
                return isNativeSensorSupported(type) || isNativeSensorSupported(TYPE_ACCELEROMETER);
            case TYPE_ORIENTATION:
                return isNativeSensorSupported(TYPE_GRAVITY) || isNativeSensorSupported(TYPE_ACCELEROMETER);
            default:
                return false;
        }
    }

    /// Returns the sensor object for the given type, or `null` if the type is
    /// not supported on this device. The same instance is returned for repeated
    /// calls with the same type.
    ///
    /// #### Parameters
    ///
    /// - `type`: one of the `TYPE_*` constants
    public MotionSensor getSensor(int type) {
        if (!isSensorSupported(type)) {
            return null;
        }
        Integer key = Integer.valueOf(type);
        synchronized (lock) {
            MotionSensor s = sensors.get(key);
            if (s == null) {
                s = new MotionSensor(this, type);
                sensors.put(key, s);
            }
            return s;
        }
    }

    /// Registers a listener for a specific gesture. The accelerometer is powered
    /// automatically while at least one gesture listener is registered.
    ///
    /// #### Parameters
    ///
    /// - `gestureType`: one of the `GestureEvent.TYPE_*` constants
    /// - `l`: the listener to invoke when the gesture occurs
    public void addGestureListener(int gestureType, GestureListener l) {
        if (l == null) {
            return;
        }
        Integer key = Integer.valueOf(gestureType);
        synchronized (lock) {
            ArrayList<GestureListener> list = gestureListeners.get(key);
            if (list == null) {
                list = new ArrayList<GestureListener>();
                gestureListeners.put(key, list);
            }
            if (!list.contains(l)) {
                list.add(l);
                gestureListenerCount++;
            }
        }
        ensureRunning();
    }

    /// Removes a previously registered gesture listener.
    ///
    /// #### Parameters
    ///
    /// - `gestureType`: the gesture the listener was registered for
    /// - `l`: the listener to remove
    public void removeGestureListener(int gestureType, GestureListener l) {
        Integer key = Integer.valueOf(gestureType);
        synchronized (lock) {
            ArrayList<GestureListener> list = gestureListeners.get(key);
            if (list != null && list.remove(l)) {
                gestureListenerCount--;
                if (list.isEmpty()) {
                    gestureListeners.remove(key);
                }
            }
        }
    }

    /// The requested time between sensor samples in milliseconds. Defaults to
    /// 50ms (20Hz). The actual rate the hardware delivers may differ.
    public int getSamplingInterval() {
        return samplingInterval;
    }

    /// Sets the requested time between sensor samples in milliseconds. Smaller
    /// values give more responsive gestures at a higher battery cost. Values are
    /// clamped to the range 10ms..1000ms.
    ///
    /// #### Parameters
    ///
    /// - `millis`: the requested interval
    public void setSamplingInterval(int millis) {
        if (millis < 10) {
            millis = 10;
        }
        if (millis > 1000) {
            millis = 1000;
        }
        synchronized (lock) {
            samplingInterval = millis;
        }
    }

    /// Sets the linear acceleration spike, in meters per second squared, above
    /// which jolts are counted towards a shake. Larger values require a more
    /// vigorous shake. The default is 12.0.
    public void setShakeThreshold(double threshold) {
        engine.shakeThreshold = threshold;
    }

    /// Sets the tilt angle, in radians, at which the tilt gestures fire. The
    /// default is roughly 0.6 (about 34 degrees).
    public void setTiltThreshold(double radians) {
        engine.tiltEnterAngle = radians;
        engine.tiltExitAngle = radians * 0.5;
    }

    /// Sets the total acceleration, in meters per second squared, below which
    /// the device is considered to be in free fall. The default is 3.0.
    public void setFreeFallThreshold(double threshold) {
        engine.freeFallThreshold = threshold;
    }

    /// Stops every sensor and removes all registered listeners. Mostly useful
    /// for tests and for an explicit shutdown.
    public void stop() {
        synchronized (lock) {
            sensors.clear();
            gestureListeners.clear();
            gestureListenerCount = 0;
            for (int t = 1; t <= MAX_TYPE; t++) {
                if (nativeStarted[t]) {
                    stopNativeSensor(t);
                    nativeStarted[t] = false;
                }
            }
            threadRunning = false;
        }
    }

    // ------------------------------------------------------------------
    // Internal plumbing used by MotionSensor
    // ------------------------------------------------------------------

    void sensorActivityChanged() {
        ensureRunning();
    }

    void runOnEventThread(Runnable r) {
        Display.getInstance().callSerially(r);
    }

    // ------------------------------------------------------------------
    // Sampling loop
    // ------------------------------------------------------------------

    private void ensureRunning() {
        synchronized (lock) {
            if (threadRunning) {
                return;
            }
            boolean[] needed = computeNeeded();
            if (!anyNeeded(needed)) {
                return;
            }
            threadRunning = true;
            gravityInitialized = false;
            engine.reset();
            Display.getInstance().startThread(pollRunnable, "CN1 Motion Sensors").start();
        }
    }

    private void pollLoop() {
        while (true) {
            int sleep;
            synchronized (lock) {
                if (!threadRunning) {
                    return;
                }
                if (!tick()) {
                    return;
                }
                sleep = samplingInterval;
            }
            try {
                Thread.sleep(sleep);
            } catch (InterruptedException ignored) {
                // resume sampling on the next iteration
            }
        }
    }

    // Runs one sampling cycle. Caller holds lock. Returns false to stop the loop.
    private boolean tick() {
        boolean[] needed = computeNeeded();
        applyNativeDiff(needed);
        if (!anyNeeded(needed)) {
            threadRunning = false;
            return false;
        }

        long time = System.currentTimeMillis();

        boolean accelOk = needed[TYPE_ACCELEROMETER] && read(TYPE_ACCELEROMETER);
        float ax = readBuffer[0];
        float ay = readBuffer[1];
        float az = readBuffer[2];

        boolean nativeGravityOk = needed[TYPE_GRAVITY] && read(TYPE_GRAVITY);
        float ngx = readBuffer[0];
        float ngy = readBuffer[1];
        float ngz = readBuffer[2];

        updateGravityEstimate(accelOk, ax, ay, az, nativeGravityOk, ngx, ngy, ngz);

        dispatchSensors(accelOk, ax, ay, az, nativeGravityOk, ngx, ngy, ngz, time);

        if (gestureListenerCount > 0 && accelOk && gravityInitialized) {
            List<GestureEvent> events = engine.onSample(ax, ay, az,
                    gravityEstimate[0], gravityEstimate[1], gravityEstimate[2], time);
            for (GestureEvent event : events) {
                dispatchGesture(event);
            }
        }
        return true;
    }

    private void dispatchSensors(boolean accelOk, float ax, float ay, float az,
                                 boolean nativeGravityOk, float ngx, float ngy, float ngz,
                                 long time) {
        for (MotionSensor s : sensors.values()) {
            if (!s.hasListeners()) {
                continue;
            }
            int type = s.getType();
            switch (type) {
                case TYPE_ACCELEROMETER:
                    if (accelOk) {
                        s.dispatch(ax, ay, az, time);
                    }
                    break;
                case TYPE_GRAVITY:
                    if (nativeGravityOk) {
                        s.dispatch(ngx, ngy, ngz, time);
                    } else if (gravityInitialized) {
                        s.dispatch(gravityEstimate[0], gravityEstimate[1], gravityEstimate[2], time);
                    }
                    break;
                case TYPE_LINEAR_ACCELERATION:
                    if (isNativeSensorSupported(TYPE_LINEAR_ACCELERATION) && read(TYPE_LINEAR_ACCELERATION)) {
                        s.dispatch(readBuffer[0], readBuffer[1], readBuffer[2], time);
                    } else if (accelOk && gravityInitialized) {
                        s.dispatch(ax - gravityEstimate[0], ay - gravityEstimate[1],
                                az - gravityEstimate[2], time);
                    }
                    break;
                case TYPE_GYROSCOPE:
                    if (read(TYPE_GYROSCOPE)) {
                        s.dispatch(readBuffer[0], readBuffer[1], readBuffer[2], time);
                    }
                    break;
                case TYPE_MAGNETOMETER:
                    if (read(TYPE_MAGNETOMETER)) {
                        s.dispatch(readBuffer[0], readBuffer[1], readBuffer[2], time);
                    }
                    break;
                case TYPE_ORIENTATION:
                    if (gravityInitialized) {
                        float[] o = computeOrientation();
                        s.dispatch(o[0], o[1], o[2], time);
                    }
                    break;
                default:
                    break;
            }
        }
    }

    private void updateGravityEstimate(boolean accelOk, float ax, float ay, float az,
                                       boolean nativeGravityOk, float ngx, float ngy, float ngz) {
        if (nativeGravityOk) {
            gravityEstimate[0] = ngx;
            gravityEstimate[1] = ngy;
            gravityEstimate[2] = ngz;
            gravityInitialized = true;
        } else if (accelOk) {
            if (!gravityInitialized) {
                gravityEstimate[0] = ax;
                gravityEstimate[1] = ay;
                gravityEstimate[2] = az;
                gravityInitialized = true;
            } else {
                gravityEstimate[0] = GRAVITY_FILTER_ALPHA * gravityEstimate[0] + (1 - GRAVITY_FILTER_ALPHA) * ax;
                gravityEstimate[1] = GRAVITY_FILTER_ALPHA * gravityEstimate[1] + (1 - GRAVITY_FILTER_ALPHA) * ay;
                gravityEstimate[2] = GRAVITY_FILTER_ALPHA * gravityEstimate[2] + (1 - GRAVITY_FILTER_ALPHA) * az;
            }
        }
    }

    // Computes azimuth, pitch and roll (radians). Pitch and roll come directly
    // from the gravity estimate; the azimuth (compass heading) needs the
    // magnetometer and follows the rotation matrix derivation used by Android's
    // SensorManager.getRotationMatrix / getOrientation.
    private float[] computeOrientation() {
        double gx = gravityEstimate[0];
        double gy = gravityEstimate[1];
        double gz = gravityEstimate[2];

        float azimuth = 0;
        if (isNativeSensorSupported(TYPE_MAGNETOMETER) && read(TYPE_MAGNETOMETER)) {
            double ex = readBuffer[0];
            double ey = readBuffer[1];
            double ez = readBuffer[2];
            // H = E x A : the device "east" axis
            double hx = ey * gz - ez * gy;
            double hy = ez * gx - ex * gz;
            double hz = ex * gy - ey * gx;
            double normH = Math.sqrt(hx * hx + hy * hy + hz * hz);
            double normA = Math.sqrt(gx * gx + gy * gy + gz * gz);
            if (normH >= 0.1 && normA >= 0.1) {
                hx /= normH;
                hy /= normH;
                hz /= normH;
                double axn = gx / normA;
                double azn = gz / normA;
                // M = A x H : the device "north" axis; we only need its y term
                double my = azn * hx - axn * hz;
                azimuth = (float) MathUtil.atan2(hy, my);
            }
        }

        double horizontal = Math.sqrt(gx * gx + gz * gz);
        float roll = (float) MathUtil.atan2(gx, gz);
        float pitch = (float) MathUtil.atan2(-gy, horizontal);
        return new float[]{azimuth, pitch, roll};
    }

    private void dispatchGesture(GestureEvent evt) {
        Integer key = Integer.valueOf(evt.getType());
        ArrayList<GestureListener> list = gestureListeners.get(key);
        if (list == null || list.isEmpty()) {
            return;
        }
        GestureListener[] targets = new GestureListener[list.size()];
        list.toArray(targets);
        runOnEventThread(new DispatchGesture(targets, evt));
    }

    // Delivers a gesture to a snapshot of listeners on the EDT. A named static
    // class (rather than an anonymous one) since it only needs the captured
    // values, not the enclosing manager.
    private static final class DispatchGesture implements Runnable {
        private final GestureListener[] targets;
        private final GestureEvent event;

        DispatchGesture(GestureListener[] targets, GestureEvent event) {
            this.targets = targets;
            this.event = event;
        }

        @Override
        public void run() {
            for (GestureListener target : targets) {
                target.gestureDetected(event);
            }
        }
    }

    private boolean read(int type) {
        return readNativeSensor(type, readBuffer);
    }

    private boolean[] computeNeeded() {
        boolean[] needed = new boolean[MAX_TYPE + 1];
        for (MotionSensor s : sensors.values()) {
            if (s.hasListeners()) {
                addSources(needed, s.getType());
            }
        }
        if (gestureListenerCount > 0) {
            need(needed, TYPE_ACCELEROMETER);
        }
        return needed;
    }

    private void addSources(boolean[] needed, int type) {
        switch (type) {
            case TYPE_ACCELEROMETER:
                need(needed, TYPE_ACCELEROMETER);
                break;
            case TYPE_GYROSCOPE:
                need(needed, TYPE_GYROSCOPE);
                break;
            case TYPE_MAGNETOMETER:
                need(needed, TYPE_MAGNETOMETER);
                break;
            case TYPE_GRAVITY:
                if (isNativeSensorSupported(TYPE_GRAVITY)) {
                    need(needed, TYPE_GRAVITY);
                } else {
                    need(needed, TYPE_ACCELEROMETER);
                }
                break;
            case TYPE_LINEAR_ACCELERATION:
                if (isNativeSensorSupported(TYPE_LINEAR_ACCELERATION)) {
                    need(needed, TYPE_LINEAR_ACCELERATION);
                } else {
                    need(needed, TYPE_ACCELEROMETER);
                }
                break;
            case TYPE_ORIENTATION:
                if (isNativeSensorSupported(TYPE_GRAVITY)) {
                    need(needed, TYPE_GRAVITY);
                } else {
                    need(needed, TYPE_ACCELEROMETER);
                }
                need(needed, TYPE_MAGNETOMETER);
                break;
            default:
                break;
        }
    }

    private void need(boolean[] needed, int type) {
        if (isNativeSensorSupported(type)) {
            needed[type] = true;
        }
    }

    private void applyNativeDiff(boolean[] needed) {
        for (int t = 1; t <= MAX_TYPE; t++) {
            if (needed[t] && !nativeStarted[t]) {
                startNativeSensor(t);
                nativeStarted[t] = true;
            } else if (!needed[t] && nativeStarted[t]) {
                stopNativeSensor(t);
                nativeStarted[t] = false;
            }
        }
    }

    private boolean anyNeeded(boolean[] needed) {
        for (int t = 1; t <= MAX_TYPE; t++) {
            if (needed[t]) {
                return true;
            }
        }
        return false;
    }
}
