package com.codenameone.examples.purchasetest;

import java.util.ArrayList;
import java.util.List;

/** Desktop/simulator impl of {@link PurchaseTestSink}; records in memory. */
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
                if (i > 0) sb.append(',');
                sb.append(SUBMITTED.get(i));
            }
            return sb.toString();
        }
    }

    public void reset() {
        synchronized (SUBMITTED) { SUBMITTED.clear(); }
    }

    public boolean isSupported() {
        return true;
    }
}
