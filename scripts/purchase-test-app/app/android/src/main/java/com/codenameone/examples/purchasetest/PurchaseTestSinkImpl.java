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

    public void recordCommerceResult(String result) {
        // Android asserts on the CN1SS:IAP:COMMERCE logcat line the app prints.
    }

    public void reset() {
        synchronized (SUBMITTED) { SUBMITTED.clear(); }
    }

    public boolean isSupported() {
        return true;
    }
}
