/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Codename One in the LICENSE file that accompanied this code.
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
package com.codename1.payment.paywall;

import com.codename1.payment.CommercePackage;
import com.codename1.payment.CustomerInfo;

/// Receives what happens on a paywall. Every method is called on the EDT.
public interface PaywallListener {
    /// The customer pressed the purchase button and the store was asked to
    /// sell the package.
    ///
    /// #### Parameters
    ///
    /// - `pkg`: the package being bought
    void purchaseStarted(CommercePackage pkg);

    /// The cloud confirmed the purchase: the customer now owns the product
    /// of the package.
    /// A paywall shown by [Paywall#show(Offering, PaywallListener)] has
    /// already returned to the form it was shown over; a [PaywallView] in a
    /// form of the application is still showing.
    ///
    /// #### Parameters
    ///
    /// - `info`: the customer information that includes the purchase
    void purchaseCompleted(CustomerInfo info);

    /// The paywall was closed after a purchase was started and before the
    /// cloud confirmed it.
    void purchaseCancelled();

    /// The customer information changed after the customer asked to restore
    /// purchases.
    /// A paywall shown by [Paywall#show(Offering, PaywallListener)] has
    /// already returned to the form it was shown over.
    ///
    /// #### Parameters
    ///
    /// - `info`: the customer information after the restore
    void restoreCompleted(CustomerInfo info);

    /// The paywall was closed: by the customer, or by itself after a
    /// completed purchase or restore when [Paywall#show(Offering, PaywallListener)]
    /// showed it.
    void closed();
}
