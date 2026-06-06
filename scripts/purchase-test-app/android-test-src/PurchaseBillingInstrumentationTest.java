package com.codenameone.examples.hellocodenameone;

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
 * Injects {@link CN1TestBillingSupport} via the framework test seam
 * ({@link CodenameOneActivity#setBillingSupportTestOverride}), launches the app
 * so the generated stub enables billing and calls {@code initBilling()} on the
 * fake (which synthesizes a purchase through {@code Purchase.postReceipt}), then
 * scrapes logcat for the {@code CN1SS:IAP:SUBMITTED} marker emitted by the app's
 * RecordingReceiptStore. Seeing it proves the receipt reached the store
 * installed on a different Purchase instance at startup.
 */
@RunWith(AndroidJUnit4.class)
public class PurchaseBillingInstrumentationTest {
    private static final String TAG = "PurchaseBillingTest";
    private static final String SUBMITTED_MARKER = "CN1SS:IAP:SUBMITTED " + CN1TestBillingSupport.TEST_TX_ID;

    @After
    public void clearOverride() {
        CodenameOneActivity.setBillingSupportTestOverride(null);
    }

    @Test
    public void purchaseReachesReceiptStore() throws Exception {
        // Inject the fake before the activity resumes so getBillingSupport()
        // returns it instead of the real Play BillingSupport.
        CodenameOneActivity.setBillingSupportTestOverride(new CN1TestBillingSupport());

        Context context = ApplicationProvider.getApplicationContext();
        Intent intent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
        assertTrue("Launch intent not found for package " + context.getPackageName(), intent != null);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);

        boolean submitted = waitForMarker(SUBMITTED_MARKER, 120_000L);
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
                        Log.i(TAG, "Observed submission marker");
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
