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
