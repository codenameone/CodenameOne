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
package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.sensors.MotionEvent;
import com.codename1.sensors.MotionSensor;
import com.codename1.sensors.MotionSensorListener;
import com.codename1.sensors.MotionSensorManager;
import com.codename1.ui.Display;

/**
 * On-device coverage for the motion sensor API (com.codename1.sensors).
 *
 * <p>The accelerometer is wired to a different native API on every port -- the
 * Android {@code SensorManager} on Android and CoreMotion's {@code
 * CMMotionManager} on iOS -- so a port that forgets to register its manager, or
 * whose native bridge fails to link, would never deliver a reading. Running this
 * on the device is what catches that.
 *
 * <p>The check adapts to the hardware: when the accelerometer is present (the
 * Android emulator exposes one) it registers a listener and asserts that a real
 * reading flows all the way through the sampling thread and the EDT dispatch.
 * When it is absent (the iOS Simulator has no CoreMotion accelerometer) the test
 * instead asserts that the API degrades gracefully -- the manager is still
 * non-null and reports the sensor as unsupported without throwing -- which still
 * exercises the native bridge end to end.
 */
public class MotionSensorDeviceTest extends BaseTest {

    /// Not safe for the runner's silent-timeout retry: it polls the sensors on a Display.startThread worker,
    /// and that worker outlives runTest(). A retry resets the shared
    /// completion state, so a late done() from the first attempt's worker
    /// would complete the second attempt and advance the suite early.
    @Override
    public boolean isRetrySafe() {
        return false;
    }

    @Override
    public boolean shouldTakeScreenshot() {
        return false;
    }

    @Override
    public int getTimeoutMillis() {
        return 30000;
    }

    @Override
    public boolean runTest() {
        final MotionSensorManager manager = MotionSensorManager.getInstance();
        assertBool(manager != null, "MotionSensorManager.getInstance() returned null");

        if (!manager.isSensorSupported(MotionSensorManager.TYPE_ACCELEROMETER)) {
            // No accelerometer on this device (e.g. the iOS Simulator). The API
            // must still be safe to call: getSensor returns null and no events
            // are delivered.
            assertNull(manager.getSensor(MotionSensorManager.TYPE_ACCELEROMETER),
                    "getSensor must return null for an unsupported sensor");
            done();
            return true;
        }

        final MotionSensor accelerometer = manager.getSensor(MotionSensorManager.TYPE_ACCELEROMETER);
        assertBool(accelerometer != null, "accelerometer is supported but getSensor returned null");

        final boolean[] received = {false};
        final MotionSensorListener listener = new MotionSensorListener() {
            @Override
            public void motionReceived(MotionEvent evt) {
                received[0] = true;
            }
        };
        accelerometer.addListener(listener);

        // Wait off the EDT for a reading so the polling thread and the EDT
        // dispatch stay free to deliver it.
        Display.getInstance().startThread(new Runnable() {
            @Override
            public void run() {
                long deadline = System.currentTimeMillis() + 5000;
                while (!received[0] && System.currentTimeMillis() < deadline) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException ignored) {
                        // keep polling
                    }
                }
                accelerometer.removeListener(listener);
                if (!received[0]) {
                    fail("no accelerometer reading was delivered within 5 seconds");
                }
                done();
            }
        }, "MotionSensorDeviceTest").start();
        return true;
    }
}
