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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Verifies the synthetic sensor readings produced by the simulator motion
 * sensor manager. The values are computed without any UI, so they can be tested
 * directly.
 */
public class JavaSEMotionSensorManagerTest {
    private static final double G = MotionSensorManager.STANDARD_GRAVITY;

    private double magnitude(float[] v) {
        return Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
    }

    @Test
    public void simulatedSensorValues() {
        JavaSEMotionSensorManager m = new JavaSEMotionSensorManager();
        float[] out = new float[3];

        // Raw sensors are simulated; the derived types are resolved by the core.
        Assertions.assertTrue(m.isNativeSensorSupported(MotionSensorManager.TYPE_ACCELEROMETER));
        Assertions.assertTrue(m.isNativeSensorSupported(MotionSensorManager.TYPE_GYROSCOPE));
        Assertions.assertTrue(m.isNativeSensorSupported(MotionSensorManager.TYPE_MAGNETOMETER));
        Assertions.assertFalse(m.isNativeSensorSupported(MotionSensorManager.TYPE_GRAVITY));
        Assertions.assertFalse(m.isNativeSensorSupported(MotionSensorManager.TYPE_ORIENTATION));

        // Flat and face up: gravity reads about +9.81 on the z axis.
        JavaSEMotionSensorManager.setOrientation(0, 0);
        Assertions.assertTrue(m.readNativeSensor(MotionSensorManager.TYPE_ACCELEROMETER, out));
        Assertions.assertEquals(0.0, out[0], 0.01);
        Assertions.assertEquals(0.0, out[1], 0.01);
        Assertions.assertEquals(G, out[2], 0.01);

        // Rolled 90 degrees: gravity shifts onto the x axis.
        JavaSEMotionSensorManager.setOrientation(0, 90);
        m.readNativeSensor(MotionSensorManager.TYPE_ACCELEROMETER, out);
        Assertions.assertEquals(G, out[0], 0.05);
        Assertions.assertEquals(0.0, out[2], 0.05);

        // The magnetometer returns a constant northward field.
        Assertions.assertTrue(m.readNativeSensor(MotionSensorManager.TYPE_MAGNETOMETER, out));
        Assertions.assertEquals(30.0, out[1], 0.01);

        // Shake injects a strong transient acceleration.
        JavaSEMotionSensorManager.setOrientation(0, 0);
        JavaSEMotionSensorManager.triggerShake();
        double peak = 0;
        for (int i = 0; i < 20; i++) {
            m.readNativeSensor(MotionSensorManager.TYPE_ACCELEROMETER, out);
            peak = Math.max(peak, magnitude(out));
        }
        Assertions.assertTrue(peak > 15, "shake should produce a large acceleration, was " + peak);

        // Free fall overrides the reading with near-zero acceleration.
        JavaSEMotionSensorManager.triggerFreeFall();
        m.readNativeSensor(MotionSensorManager.TYPE_ACCELEROMETER, out);
        Assertions.assertTrue(magnitude(out) < 3, "free fall should be near weightless");
    }
}
