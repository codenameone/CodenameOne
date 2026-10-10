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
package com.codenameone.examples.wayline.pay;

import com.codenameone.examples.wayline.api.CardDto;

import java.io.IOException;

/// A payment processor. The one place the server touches one.
///
/// The shape is the one real processors share. A rider is a *customer* of the
/// processor; a card is saved to that customer once, in a *setup*, and comes
/// back as a token; from then on the server charges the token with nobody
/// present, which is what lets a ride be paid for as it ends. The server keeps
/// the token, a brand and four digits. It never keeps a card number, and with
/// a hosted setup it never sees one.
public interface PaymentProvider {
    /// `simulated` or `stripe`: what the app is told, so it knows how to add a
    /// card.
    String name();

    /// Opens the processor's record of a rider and returns its reference.
    String createCustomer(String username, String displayName) throws IOException;

    /// Begins saving a card to `customer`. `setupId` is the server's own id
    /// for the attempt, for the processor to hand back.
    Setup startCardSetup(String customer, String setupId) throws IOException;

    /// Finishes a setup and returns the card it saved.
    ///
    /// @param reference what [#startCardSetup] returned as the setup's reference
    /// @param typed the card as typed into the app, for a processor whose setup
    ///     is not hosted; ignored by one whose setup is
    /// - `PaymentDeclinedException`: when the card was refused or the setup was
    ///   not finished
    Card finishCardSetup(String customer, String reference, CardDto typed) throws IOException;

    /// Charges a saved card with its owner absent.
    ///
    /// @param key names this charge and no other. A processor asked twice with
    ///     the same key charges once, so a request repeated after a timeout
    ///     cannot take the money twice.
    /// @return the processor's reference for the charge
    /// - `PaymentDeclinedException`: when the card was refused
    /// - `IOException`: when the processor could not be asked
    String charge(String key, String customer, String token, long amountCents, String currency,
            String description) throws IOException;

    /// Returns `amountCents` of a charge to the card it came from.
    ///
    /// @return the processor's reference for the refund
    String refund(String chargeReference, long amountCents) throws IOException;

    /// A setup under way.
    final class Setup {
        /// Whether the card is typed into the processor's own page, at `url`.
        public boolean hosted;
        public String url = "";
        /// What to hand to [PaymentProvider#finishCardSetup].
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
