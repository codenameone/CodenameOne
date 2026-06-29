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
