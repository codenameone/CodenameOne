package com.codenameone.examples.hellocodenameone;

import com.codename1.payment.Receipt;
import com.codename1.payment.ReceiptStore;
import com.codename1.system.NativeLookup;
import com.codename1.util.SuccessCallback;

/**
 * Test ReceiptStore installed by the sample app at startup.  It does no real
 * networking: it immediately reports success and forwards the submitted
 * transactionId to the native {@link PurchaseTestSink} so the iOS StoreKitTest
 * harness can assert, from the hosted XCTest, that a purchase made through the
 * real StoreKit observer reached the store.
 *
 * This is the iOS-level reproduction of issue #5186: the StoreKit observer
 * calls the static Purchase.postReceipt(...), which submits through a freshly
 * constructed Purchase instance.  If submitReceipt fires here, the store
 * installed on a different instance at startup was visible to that fresh
 * instance, i.e. the shared (static) receiptStore fix is working end to end.
 */
public class RecordingReceiptStore implements ReceiptStore {
    private final PurchaseTestSink sink;

    public RecordingReceiptStore() {
        PurchaseTestSink s = NativeLookup.create(PurchaseTestSink.class);
        if (s != null && s.isSupported()) {
            sink = s;
        } else {
            sink = null;
        }
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
