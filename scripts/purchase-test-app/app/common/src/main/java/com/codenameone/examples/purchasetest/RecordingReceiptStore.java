package com.codenameone.examples.purchasetest;

import com.codename1.payment.Receipt;
import com.codename1.payment.ReceiptStore;
import com.codename1.system.NativeLookup;
import com.codename1.util.SuccessCallback;

/**
 * Test ReceiptStore installed at startup. Does no networking: it reports
 * success immediately and forwards the submitted transactionId to the native
 * {@link PurchaseTestSink} (iOS) plus logs CN1SS:IAP:SUBMITTED so the Android
 * instrumentation test can scrape logcat.
 *
 * iOS-/Android-level guard for #5186: the platform receipt path submits through
 * a freshly constructed Purchase instance, so a recorded submission proves the
 * store installed on a different instance is visible to it.
 */
public class RecordingReceiptStore implements ReceiptStore {
    private final PurchaseTestSink sink;

    public RecordingReceiptStore() {
        PurchaseTestSink s = NativeLookup.create(PurchaseTestSink.class);
        sink = (s != null && s.isSupported()) ? s : null;
    }

    public void submitReceipt(Receipt receipt, SuccessCallback<Boolean> callback) {
        if (receipt != null && sink != null) {
            sink.recordSubmittedReceipt(receipt.getTransactionId());
        }
        System.out.println("CN1SS:IAP:SUBMITTED "
                + (receipt == null ? "null" : receipt.getTransactionId()));
        callback.onSucess(Boolean.TRUE);
    }

    public void fetchReceipts(SuccessCallback<Receipt[]> callback) {
        callback.onSucess(new Receipt[0]);
    }
}
