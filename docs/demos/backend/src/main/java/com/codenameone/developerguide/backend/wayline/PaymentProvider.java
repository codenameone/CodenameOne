/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codenameone.developerguide.backend.wayline;

import java.io.IOException;

// tag::wayline-payment-provider[]
/// A payment processor. The one place the server touches one.
public interface PaymentProvider {
    /// What the app is told, so it knows how to add a card.
    String name();

    /// Opens the processor's record of a rider and returns its reference.
    String createCustomer(String username, String displayName) throws IOException;

    /// Begins saving a card to `customer`.
    Setup startCardSetup(String customer, String setupId) throws IOException;

    /// Finishes a setup and returns the card it saved.
    Card finishCardSetup(String customer, String reference, CardDto typed) throws IOException;

    /// Charges a saved card with its owner absent. A processor asked twice
    /// with the same `key` charges once.
    String charge(String key, String customer, String token, long amountCents, String currency,
            String description) throws IOException;

    /// Returns `amountCents` of a charge to the card it came from.
    String refund(String chargeReference, long amountCents) throws IOException;

    /// A setup under way.
    final class Setup {
        /// Whether the card is typed into the processor's own page, at `url`.
        public boolean hosted;
        public String url = "";
        public String reference = "";
    }

    /// A saved card: what may be shown of it, and the token it is charged by.
    final class Card {
        public String brand = "";
        public String last4 = "";
        public int expMonth;
        public int expYear;
        public String token = "";
    }
}
// end::wayline-payment-provider[]
