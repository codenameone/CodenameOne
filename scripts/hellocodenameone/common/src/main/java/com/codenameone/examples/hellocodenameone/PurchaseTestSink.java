package com.codenameone.examples.hellocodenameone;

import com.codename1.system.NativeInterface;

/**
 * Test-only sink used by the iOS Purchase e2e test (StoreKitTest /
 * SKTestSession).  The hosted XCTest cannot read CN1 Java state directly, so
 * {@link RecordingReceiptStore} forwards every submitted receipt's
 * transactionId through this native interface; the iOS implementation persists
 * it where the in-process XCTest can read it back (NSUserDefaults).
 *
 * Implemented natively per platform so it works regardless of which target the
 * sample is built for; only the iOS implementation is exercised by the test.
 */
public interface PurchaseTestSink extends NativeInterface {
    /** Record that a receipt with the given transactionId was submitted. */
    void recordSubmittedReceipt(String transactionId);

    /** Comma separated list of recorded transactionIds (most useful for diagnostics). */
    String recordedSubmissions();

    /** Clear all recorded submissions. */
    void reset();
}
