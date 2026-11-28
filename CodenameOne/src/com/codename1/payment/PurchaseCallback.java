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

/// Callback interface that the main class must implement in order for in-app-purchasing
/// to work. Once the main class implements this interface the methods within it
/// are invoked to indicate the various purchase states.
public interface PurchaseCallback {
    /// Indicates a the given SKU was purchased by a user. When purchasing multiple
    /// SKU's at once multiple calls to this method will be performed.
    ///
    /// #### Parameters
    ///
    /// - `sku`: the sku purchased
    void itemPurchased(String sku);

    /// Callback indicating a the given SKU purchase failed
    ///
    /// #### Parameters
    ///
    /// - `sku`: the id
    void itemPurchaseError(String sku, String errorMessage);

    /// Invoked if a refund was granted for a purchase.
    ///
    /// Called on iOS and macOS (StoreKit 2) when Apple revokes a purchase: a refund, or a
    /// Family Sharing purchase the organizer withdrew. Other platforms do not call it, so treat
    /// it as a prompt to recompute ownership from the receipts (see `Purchase#getReceipts()`),
    /// which is the signal that works everywhere.
    ///
    /// #### Parameters
    ///
    /// - `sku`: the sku purchased
    void itemRefunded(String sku);

    /// Invoked when a subscription SKU is started
    ///
    /// #### Parameters
    ///
    /// - `sku`: the sku purchased
    ///
    /// #### Deprecated
    ///
    /// @deprecated Subscriptions should be handled directly in the associated store (iOS/Google Play, etc..).  See https://www.codenameone.com/blog/intro-to-in-app-purchase.html for a guide
    /// into how to set up in-app purchase for all scenarios including subscriptions.
    void subscriptionStarted(String sku);

    /// Invoked when a subscription SKU is canceled
    ///
    /// #### Parameters
    ///
    /// - `sku`: the sku purchased
    ///
    /// #### Deprecated
    ///
    /// @deprecated Subscriptions should be handled directly in the associated store (iOS/Google Play, etc..).  See https://www.codenameone.com/blog/intro-to-in-app-purchase.html for a guide
    /// into how to set up in-app purchase for all scenarios including subscriptions.
    void subscriptionCanceled(String sku);

    /// Indicates that a manual payment has failed
    ///
    /// #### Parameters
    ///
    /// - `paymentCode`: the transaction id of the payment
    void paymentFailed(String paymentCode, String failureReason);

    /// Indicates that a manual payment has passed
    ///
    /// #### Parameters
    ///
    /// - `paymentCode`: the transaction id of the payment
    void paymentSucceeded(String paymentCode, double amount, String currency);
}
