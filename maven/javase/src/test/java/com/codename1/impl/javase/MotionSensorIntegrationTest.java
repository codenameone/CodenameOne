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

import com.codename1.sensors.GestureEvent;
import com.codename1.sensors.GestureListener;
import com.codename1.sensors.MotionSensorManager;
import com.codename1.testing.junit.CodenameOneTest;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;

/**
 * End-to-end test of the motion sensor pipeline running inside a real simulator
 * Display. A synthetic accelerometer that oscillates like a shake is fed through
 * the live sampling thread, gesture engine and EDT dispatch; the test passes
 * only if the shake reaches a registered listener. This exercises the whole
 * {@link MotionSensorManager} plumbing (thread start via {@code Display}, source
 * reference counting, derivation and {@code callSerially} dispatch) rather than
 * any single platform port.
 */
@CodenameOneTest
@DisabledIfSystemProperty(named = "java.awt.headless", matches = "true")
public class MotionSensorIntegrationTest {

    // A manager whose accelerometer continuously oscillates, which the engine
    // recognizes as a shake.
    private static final class ShakingManager extends MotionSensorManager {
        @Override
        protected boolean isNativeSensorSupported(int type) {
            return type == TYPE_ACCELEROMETER;
        }

        @Override
        protected void startNativeSensor(int type) {
        }

        @Override
        protected void stopNativeSensor(int type) {
        }

        @Override
        protected boolean readNativeSensor(int type, float[] out) {
            if (type != TYPE_ACCELEROMETER) {
                return false;
            }
            // A single-axis 2Hz oscillation: zero mean (so the gravity low-pass
            // does not absorb it) and passing through zero each half-cycle, so the
            // linear acceleration magnitude rises above and falls below the shake
            // threshold repeatedly.
            double t = System.currentTimeMillis() / 1000.0;
            out[0] = (float) (Math.sin(2 * Math.PI * 2 * t) * 30);
            out[1] = 0;
            out[2] = (float) STANDARD_GRAVITY;
            return true;
        }
    }

    @Test
    public void shakeReachesListenerThroughTheFullStack() throws Exception {
        MotionSensorManager m = new ShakingManager();

        final boolean[] shaken = {false};
        final Object lock = new Object();
        GestureListener listener = new GestureListener() {
            @Override
            public void gestureDetected(GestureEvent evt) {
                synchronized (lock) {
                    shaken[0] = true;
                    lock.notifyAll();
                }
            }
        };

        m.addGestureListener(GestureEvent.TYPE_SHAKE, listener);
        try {
            synchronized (lock) {
                long deadline = System.currentTimeMillis() + 5000;
                while (!shaken[0] && System.currentTimeMillis() < deadline) {
                    lock.wait(250);
                }
            }
        } finally {
            m.removeGestureListener(GestureEvent.TYPE_SHAKE, listener);
            m.stop();
        }

        Assertions.assertTrue(shaken[0],
                "shake gesture should reach the listener through the full pipeline");
    }
}
