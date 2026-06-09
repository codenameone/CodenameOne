package com.codenameone.examples.purchasetest;

import android.app.UiAutomation;
import android.content.Context;
import android.content.Intent;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.codename1.impl.android.CodenameOneActivity;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;

import static org.junit.Assert.assertTrue;

/**
 * Android-side e2e guard for the IAP / ReceiptStore bridge (issue #5186).
 *
 * Installs {@link CN1TestBillingSupport} via the framework test seam
 * ({@link CodenameOneActivity#setBillingSupportTestOverride}) and launches the
 * dedicated IAP app. Once the CN1 VM has booted and installed the
 * RecordingReceiptStore (CN1SS:IAP_DIAG), it drives a synthetic purchase through
 * the fake -> Purchase.postReceipt -> receipt-sync -> the installed store, then
 * scrapes logcat for CN1SS:IAP:SUBMITTED. Driving the purchase explicitly after
 * the store is ready keeps the test deterministic (no startup race).
 */
@RunWith(AndroidJUnit4.class)
public class PurchaseBillingInstrumentationTest {
    private static final String TAG = "PurchaseBillingTest";
    private static final String INSTALLED_MARKER = "CN1SS:IAP_DIAG installed=true";
    private static final String SUBMITTED_MARKER = "CN1SS:IAP:SUBMITTED " + CN1TestBillingSupport.TEST_TX_ID;

    @After
    public void clearOverride() {
        CodenameOneActivity.setBillingSupportTestOverride(null);
    }

    @Test
    public void purchaseReachesReceiptStore() throws Exception {
        CN1TestBillingSupport fake = new CN1TestBillingSupport();
        // Demonstrates the injection seam (the app would route billing here);
        // we also drive purchase() directly below so timing is deterministic.
        CodenameOneActivity.setBillingSupportTestOverride(fake);

        Context context = ApplicationProvider.getApplicationContext();
        Intent intent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
        assertTrue("Launch intent not found for package " + context.getPackageName(), intent != null);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);

        // Wait until the CN1 VM has booted and installed the RecordingReceiptStore.
        boolean installed = waitForMarker(INSTALLED_MARKER, 90_000L);
        assertTrue("App did not install the RecordingReceiptStore (no '" + INSTALLED_MARKER
                + "' in logcat) - the VM may not have booted.", installed);

        // Now fire the synthetic purchase through the bridge; the store exists,
        // so submitReceipt runs and logs CN1SS:IAP:SUBMITTED.
        fake.purchase(CN1TestBillingSupport.TEST_SKU);

        boolean submitted = waitForMarker(SUBMITTED_MARKER, 60_000L);
        assertTrue("Did not observe '" + SUBMITTED_MARKER + "' in logcat. The synthetic purchase "
                + "did not flow through Purchase.postReceipt into the installed ReceiptStore.", submitted);
    }

    private boolean waitForMarker(String marker, long timeoutMs) throws Exception {
        long deadline = System.currentTimeMillis() + timeoutMs;
        UiAutomation automation = InstrumentationRegistry.getInstrumentation().getUiAutomation();
        ParcelFileDescriptor pfd = automation.executeShellCommand("logcat -v brief");
        try (FileInputStream fis = new FileInputStream(pfd.getFileDescriptor());
             BufferedReader reader = new BufferedReader(new InputStreamReader(fis))) {
            String line;
            while (System.currentTimeMillis() < deadline) {
                if (reader.ready() && (line = reader.readLine()) != null) {
                    if (line.contains(marker)) {
                        Log.i(TAG, "Observed marker: " + marker);
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
