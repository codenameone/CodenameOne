package com.codenameone.examples.hellocodenameone;

import android.content.Context;
import android.content.Intent;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class DeviceRunnerInstrumentationTest {
    @Test
    public void launchMainActivityAndWaitForDeviceRunner() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        Intent intent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
        assertNotNull("Launch intent not found for package " + context.getPackageName(), intent);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);

        boolean finished = waitForDeviceRunner();
        assertTrue("DeviceRunner did not emit completion marker within timeout", finished);
    }

    private boolean waitForDeviceRunner() throws Exception {
        final long timeoutMs = 300_000L;
        final String endMarker = "CN1SS:SUITE:FINISHED";

        Process logcat = new ProcessBuilder("logcat", "-v", "brief")
                .redirectErrorStream(true)
                .start();

        long deadline = System.currentTimeMillis() + timeoutMs;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(logcat.getInputStream()))) {
            String line;
            while (System.currentTimeMillis() < deadline) {
                if (reader.ready() && (line = reader.readLine()) != null) {
                    if (line.contains(endMarker)) {
                        return true;
                    }
                } else {
                    Thread.sleep(200);
                }
            }
        } finally {
            logcat.destroyForcibly();
        }
        return false;
    }
}
