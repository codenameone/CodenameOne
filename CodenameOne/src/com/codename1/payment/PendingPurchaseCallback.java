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
package com.codename1.payment;

/// Callback interface that the main class may implement to be notified of pending purchases.
/// Pending purchases are purchases that have been completed, and a payment callback from
/// the native platform has been received, but the state of the purchase is "pending".  This
/// occurs if the user has opted to pay with cash on Android, or on iOS and macOS (StoreKit 2)
/// when the purchase waits for Ask to Buy approval or Strong Customer Authentication.  When the
/// payment completes, the `PurchaseCallback#itemPurchased(String)` callback is called, possibly
/// on a later launch of the app.  A declined request calls nothing further.
public interface PendingPurchaseCallback extends PurchaseCallback {
    void itemPurchasePending(String sku);
}
