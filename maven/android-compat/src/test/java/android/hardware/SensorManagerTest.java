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
package android.hardware;

import com.codename1.androidcompat.testing.AndroidTestSupport;
import com.codename1.compat.testing.MainThreadRule;
import com.codename1.sensors.MotionSensorManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class SensorManagerTest {

    @Rule
    public final MainThreadRule mainThread = new MainThreadRule();

    static final class FakeSensors extends MotionSensorManager {
        boolean magnetometer = true;

        @Override
        protected boolean isNativeSensorSupported(int type) {
            return type != TYPE_MAGNETOMETER || magnetometer;
        }

        @Override
        protected void startNativeSensor(int type) {
        }

        @Override
        protected void stopNativeSensor(int type) {
        }

        @Override
        protected boolean readNativeSensor(int type, float[] out) {
            return false;
        }
    }

    private FakeSensors fake;

    @Before
    public void setUp() {
        AndroidTestSupport.context();
        fake = new FakeSensors();
        SensorManager.override = fake;
    }

    @After
    public void tearDown() {
        SensorManager.override = null;
    }

    @Test
    public void accelerometerEventsCarryTheReading() {
        SensorManager sm = SensorManager.getInstance();
        Sensor acc = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        assertNotNull(acc);
        final List<float[]> seen = new ArrayList<float[]>();
        SensorEventListener l = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                seen.add(event.values.clone());
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
            }
        };
        assertTrue(sm.registerListener(l, acc, SensorManager.SENSOR_DELAY_FASTEST));
        sm.feed(MotionSensorManager.TYPE_ACCELEROMETER, 1f, 2f, 9.8f);
        MainThreadRule.drain();
        sm.unregisterListener(l);
        sm.feed(MotionSensorManager.TYPE_ACCELEROMETER, 5f, 5f, 5f);
        MainThreadRule.drain();
        assertEquals(1, seen.size());
        assertEquals(9.8f, seen.get(0)[2], 1e-6f);
    }

    @Test
    public void aCompassNeedsAMagnetometer() {
        fake.magnetometer = false;
        assertNull(SensorManager.getInstance().getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR));
    }

    @Test
    public void rotationMatrixAndOrientationOfAFlatDeviceFacingNorth() {
        float[] r = new float[9];
        float[] gravity = {0f, 0f, 9.81f};
        float[] magnetic = {0f, 22f, -40f};
        assertTrue(SensorManager.getRotationMatrix(r, null, gravity, magnetic));
        float[] o = SensorManager.getOrientation(r, new float[3]);
        assertEquals(0f, o[0], 1e-4f);
        assertEquals(0f, o[1], 1e-4f);
        assertEquals(0f, o[2], 1e-4f);
        assertEquals(1f, r[0], 1e-5f);
        assertEquals(1f, r[4], 1e-5f);
        assertEquals(1f, r[8], 1e-5f);
    }
}
