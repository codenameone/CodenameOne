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

    void reset();
}
