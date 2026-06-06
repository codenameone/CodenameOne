package com.codenameone.examples.hellocodenameone;

import java.util.ArrayList;
import java.util.List;

/**
 * Desktop/simulator implementation of {@link PurchaseTestSink}. Records in
 * memory; only the iOS implementation is exercised by the StoreKitTest harness.
 */
public class PurchaseTestSinkImpl {
    private static final List<String> SUBMITTED = new ArrayList<String>();

    public void recordSubmittedReceipt(String transactionId) {
        synchronized (SUBMITTED) {
            SUBMITTED.add(transactionId == null ? "<null>" : transactionId);
        }
    }

    public String recordedSubmissions() {
        synchronized (SUBMITTED) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < SUBMITTED.size(); i++) {
                if (i > 0) {
                    sb.append(',');
                }
                sb.append(SUBMITTED.get(i));
            }
            return sb.toString();
        }
    }

    public void reset() {
        synchronized (SUBMITTED) {
            SUBMITTED.clear();
        }
    }

    public boolean isSupported() {
        return true;
    }
}
