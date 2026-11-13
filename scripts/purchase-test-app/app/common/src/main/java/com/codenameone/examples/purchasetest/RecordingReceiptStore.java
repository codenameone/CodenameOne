/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codenameone.examples.purchasetest;

import com.codename1.payment.Purchase;
import com.codename1.payment.Receipt;
import com.codename1.payment.ReceiptStore;
import com.codename1.system.NativeLookup;
import com.codename1.util.SuccessCallback;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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
    private final List<Receipt> submitted = new ArrayList<Receipt>();

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
        if (receipt != null) {
            submitted.add(receipt);
            final String sku = receipt.getSku();
            // A synchronization is in progress here, so this only registers
            // the callback: it fires once the receipt was taken off the
            // pending queue and the list below was fetched, which is the
            // state a commerce refresh reads.
            Purchase.getInAppPurchase().synchronizeReceipts(0, new SuccessCallback<Boolean>() {
                public void onSucess(Boolean ok) {
                    CommerceCheck.run(sink, sku);
                }
            });
        }
        callback.onSucess(Boolean.TRUE);
    }

    /**
     * Answers what was submitted, as a receipt server would: with an expiry
     * date. The store's own receipt carries none, and the expiry is what the
     * store-direct entitlement signal (Purchase.isSubscribed) is computed
     * from, so without it no purchase could ever read as entitled here.
     */
    public void fetchReceipts(SuccessCallback<Receipt[]> callback) {
        Receipt[] out = new Receipt[submitted.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = submitted.get(i);
            if (out[i].getExpiryDate() == null) {
                out[i].setExpiryDate(new Date(System.currentTimeMillis() + 24L * 60 * 60 * 1000));
            }
        }
        callback.onSucess(out);
    }
}
