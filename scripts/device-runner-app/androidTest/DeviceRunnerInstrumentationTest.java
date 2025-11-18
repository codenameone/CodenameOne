package com.codenameone.examples.hellocodenameone;

import android.app.UiAutomation;
import android.content.Context;
import android.content.Intent;
import android.os.ParcelFileDescriptor;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.BufferedReader;
import java.io.FileInputStream;
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

        long deadline = System.currentTimeMillis() + timeoutMs;
        UiAutomation automation = InstrumentationRegistry.getInstrumentation().getUiAutomation();
        ParcelFileDescriptor pfd = automation.executeShellCommand("logcat -v brief");
        try (FileInputStream fis = new FileInputStream(pfd.getFileDescriptor());
             BufferedReader reader = new BufferedReader(new InputStreamReader(fis))) {
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
            try {
                pfd.close();
            } catch (Exception ignored) {
            }
        }
        return false;
    }
}
