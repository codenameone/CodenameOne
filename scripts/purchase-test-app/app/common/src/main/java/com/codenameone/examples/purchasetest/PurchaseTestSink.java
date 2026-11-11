package com.codenameone.examples.purchasetest;

import com.codename1.system.NativeInterface;

/**
 * Test-only sink: {@link RecordingReceiptStore} forwards each submitted
 * receipt's transactionId here, and the iOS implementation persists it to
 * NSUserDefaults where the hosted XCTest can read it back. Implemented per
 * platform so the app builds everywhere; only iOS is exercised by the test.
 */
public interface PurchaseTestSink extends NativeInterface {
    void recordSubmittedReceipt(String transactionId);

    String recordedSubmissions();

    /**
     * Records the outcome of the commerce check the app runs once a purchase
     * has synchronized: "OK ..." or "FAIL ...". The iOS implementation stores
     * it where the hosted XCTest reads it back.
     */
    void recordCommerceResult(String result);

    void reset();
}
