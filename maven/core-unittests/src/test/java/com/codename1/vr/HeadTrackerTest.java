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
package com.codename1.vr;

import com.codename1.junit.UITestBase;
import com.codename1.sensors.FakeMotionSensorManager;
import com.codename1.sensors.MotionSensorManager;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Coverage for {@link HeadTracker}: sensor lifecycle wiring through
 * {@link FakeMotionSensorManager}, gyro-driven orientation updates, the
 * no-gyro accelerometer fallback and thread-safe snapshot reads.
 */
class HeadTrackerTest extends UITestBase {

    private static float yawAngle(float[] q) {
        return 2f * (float) Math.atan2(q[1], q[3]);
    }

    @Test
    void isSupportedReflectsInstalledSensors() {
        implementation.setMotionSensorManager(new FakeMotionSensorManager(
                MotionSensorManager.TYPE_GYROSCOPE));
        assertTrue(HeadTracker.isSupported());

        implementation.setMotionSensorManager(new FakeMotionSensorManager(
                MotionSensorManager.TYPE_ACCELEROMETER));
        assertTrue(HeadTracker.isSupported());

        implementation.setMotionSensorManager(new FakeMotionSensorManager());
        assertFalse(HeadTracker.isSupported());
    }

    @Test
    void eventsOnlyDriveTheOrientationWhileStarted() {
        // Native sensor start/stop happens on the manager's background poll
        // thread, so this test observes the listener wiring instead: events
        // fired before start() or after stop() must not move the orientation.
        FakeMotionSensorManager mgr = new FakeMotionSensorManager(
                MotionSensorManager.TYPE_GYROSCOPE,
                MotionSensorManager.TYPE_ACCELEROMETER,
                MotionSensorManager.TYPE_MAGNETOMETER);
        HeadTracker tracker = new HeadTracker(mgr);
        assertFalse(tracker.isStarted());

        mgr.fire(MotionSensorManager.TYPE_GYROSCOPE, 0f, 1f, 0f, 1000);
        mgr.fire(MotionSensorManager.TYPE_GYROSCOPE, 0f, 1f, 0f, 1100);
        flushSerialCalls();
        float[] q = new float[4];
        tracker.getOrientation(q);
        assertEquals(0f, yawAngle(q), 0f, "events before start are ignored");

        tracker.start();
        tracker.start();
        assertTrue(tracker.isStarted());
        mgr.fire(MotionSensorManager.TYPE_GYROSCOPE, 0f, 1f, 0f, 2000);
        flushSerialCalls();
        mgr.fire(MotionSensorManager.TYPE_GYROSCOPE, 0f, 1f, 0f, 2100);
        flushSerialCalls();
        tracker.getOrientation(q);
        float yawWhileRunning = yawAngle(q);
        assertTrue(yawWhileRunning > 0.05f, "events while started move the orientation");
        // The double start did not register duplicate listeners: one 100ms
        // step at 1 rad/s is 0.1 radians, not 0.2.
        assertEquals(0.1f, yawWhileRunning, 0.01f);

        tracker.stop();
        tracker.stop();
        assertFalse(tracker.isStarted());
        mgr.fire(MotionSensorManager.TYPE_GYROSCOPE, 0f, 1f, 0f, 3000);
        mgr.fire(MotionSensorManager.TYPE_GYROSCOPE, 0f, 1f, 0f, 3100);
        flushSerialCalls();
        tracker.getOrientation(q);
        assertEquals(yawWhileRunning, yawAngle(q), 1e-6f, "events after stop are ignored");
    }

    @Test
    void gyroEventsUpdateTheOrientation() {
        FakeMotionSensorManager mgr = new FakeMotionSensorManager(
                MotionSensorManager.TYPE_GYROSCOPE);
        HeadTracker tracker = new HeadTracker(mgr);
        tracker.start();

        // 1 rad/s around device Y; first event only establishes the clock.
        long t = 1000;
        mgr.fire(MotionSensorManager.TYPE_GYROSCOPE, 0f, 1f, 0f, t);
        flushSerialCalls();
        for (int i = 0; i < 50; i++) {
            t += 10;
            mgr.fire(MotionSensorManager.TYPE_GYROSCOPE, 0f, 1f, 0f, t);
            flushSerialCalls();
        }
        float[] q = new float[4];
        tracker.getOrientation(q);
        // 50 steps of 10ms at 1 rad/s = 0.5 radians of yaw.
        assertEquals(0.5f, yawAngle(q), 0.01f);
        tracker.stop();
    }

    @Test
    void accelerometerFallbackConvergesTiltWithoutGyro() {
        FakeMotionSensorManager mgr = new FakeMotionSensorManager(
                MotionSensorManager.TYPE_ACCELEROMETER);
        HeadTracker tracker = new HeadTracker(mgr);
        tracker.getFilter().setGyroWeight(0.5f);
        tracker.start();

        // Device pitched so gravity reaction reads along device +Z.
        long t = 1000;
        for (int i = 0; i < 200; i++) {
            mgr.fire(MotionSensorManager.TYPE_ACCELEROMETER, 0f, 0f, 9.81f, t);
            flushSerialCalls();
            t += 10;
        }
        float[] q = new float[4];
        tracker.getOrientation(q);
        float[] deviceUp = {0f, 0f, 1f};
        com.codename1.gpu.Quaternion.rotateVector(q, deviceUp);
        assertEquals(1f, deviceUp[1], 0.01f, "device +Z should map to world up");
        tracker.stop();
    }

    @Test
    void recenterZeroesTheYaw() {
        FakeMotionSensorManager mgr = new FakeMotionSensorManager(
                MotionSensorManager.TYPE_GYROSCOPE);
        HeadTracker tracker = new HeadTracker(mgr);
        tracker.start();
        long t = 1000;
        mgr.fire(MotionSensorManager.TYPE_GYROSCOPE, 0f, 2f, 0f, t);
        flushSerialCalls();
        for (int i = 0; i < 30; i++) {
            t += 10;
            mgr.fire(MotionSensorManager.TYPE_GYROSCOPE, 0f, 2f, 0f, t);
            flushSerialCalls();
        }
        float[] q = new float[4];
        tracker.getOrientation(q);
        assertTrue(Math.abs(yawAngle(q)) > 0.1f);
        tracker.recenter();
        tracker.getOrientation(q);
        assertEquals(0f, yawAngle(q), 0.01f);
        tracker.stop();
    }

    @Test
    void snapshotIsReadableFromAnotherThread() throws InterruptedException {
        FakeMotionSensorManager mgr = new FakeMotionSensorManager(
                MotionSensorManager.TYPE_GYROSCOPE);
        final HeadTracker tracker = new HeadTracker(mgr);
        tracker.start();
        mgr.fire(MotionSensorManager.TYPE_GYROSCOPE, 0f, 1f, 0f, 1000);
        flushSerialCalls();
        mgr.fire(MotionSensorManager.TYPE_GYROSCOPE, 0f, 1f, 0f, 1100);
        flushSerialCalls();

        final AtomicReference<float[]> fromThread = new AtomicReference<float[]>();
        Thread render = new Thread(new Runnable() {
            public void run() {
                float[] q = new float[4];
                tracker.getOrientation(q);
                fromThread.set(q);
            }
        }, "fake-render-thread");
        render.start();
        render.join(5000);
        assertNotNull(fromThread.get());
        float len = (float) Math.sqrt(fromThread.get()[0] * fromThread.get()[0]
                + fromThread.get()[1] * fromThread.get()[1]
                + fromThread.get()[2] * fromThread.get()[2]
                + fromThread.get()[3] * fromThread.get()[3]);
        assertEquals(1f, len, 1e-4f);
        tracker.stop();
    }
}
