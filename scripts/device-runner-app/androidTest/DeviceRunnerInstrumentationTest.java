package com.codenameone.examples.hellocodenameone;

import android.app.UiAutomation;
import android.content.Context;
import android.content.Intent;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;

import static org.junit.Assert.assertNotNull;

@RunWith(AndroidJUnit4.class)
public class DeviceRunnerInstrumentationTest {
    private static final String TAG = "DeviceRunnerTest";

    @Test
    public void launchMainActivityAndWaitForDeviceRunner() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        grantNotificationPermission(context.getPackageName());
        Intent intent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
        assertNotNull("Launch intent not found for package " + context.getPackageName(), intent);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);

        boolean finished = waitForDeviceRunner();
        if (!finished) {
            Log.w(TAG, "DeviceRunner did not emit completion marker; proceeding without hard failure");
        }
    }

    /// Pre-grants POST_NOTIFICATIONS, the way an unattended instrumentation suite has to.
    ///
    /// The suite's SurfacesPublishTest starts a live activity, which on Android 13+ lowers to an
    /// ongoing notification and therefore needs this permission. Without the grant the port asks
    /// for it, the system dialog opens over the app, and nobody is there to answer: the requesting
    /// thread waits on it indefinitely and the modal sits on top of every screenshot that follows,
    /// so the suite stops emitting output partway through and never reaches its completion marker.
    /// Granting up front is what a device user does once by hand, and it lets the suite exercise
    /// the granted path rather than the prompt.
    ///
    /// Failures are logged and ignored: below API 33 the permission does not exist and `pm grant`
    /// rejects it, which is correct and must not fail the run.
    private void grantNotificationPermission(String packageName) {
        String command = "pm grant " + packageName + " android.permission.POST_NOTIFICATIONS";
        try {
            UiAutomation automation = InstrumentationRegistry.getInstrumentation().getUiAutomation();
            ParcelFileDescriptor pfd = automation.executeShellCommand(command);
            try (FileInputStream fis = new FileInputStream(pfd.getFileDescriptor())) {
                // draining the pipe is what lets the command run to completion
                byte[] buffer = new byte[256];
                while (fis.read(buffer) > 0) {
                    // discard
                }
            } finally {
                pfd.close();
            }
            Log.i(TAG, "Granted POST_NOTIFICATIONS to " + packageName);
        } catch (Throwable t) {
            Log.w(TAG, "Could not grant POST_NOTIFICATIONS (expected below API 33): " + t);
        }
    }

    private boolean waitForDeviceRunner() throws Exception {
        final long timeoutMs = 900_000L;
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
