/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.reel.pay;

import com.codenameone.examples.reel.api.CardDto;

import java.io.IOException;

/// A payment processor. The one place the server touches one.
///
/// The shape is the one real processors share. A customer is a *customer* of the
/// processor; a card is saved to that customer once, in a *setup*, and comes
/// back as a token; from then on the server charges the token with nobody
/// present, which is what lets a delivery be paid for as it ends. The server keeps
/// the token, a brand and four digits. It never keeps a card number, and with
/// a hosted setup it never sees one.
// tag::reel-payment-provider[]
public interface PaymentProvider {
    /// `simulated` or `stripe`: what the app is told, so it knows how to add a
    /// card.
    String name();

    /// Opens the processor's record of a customer and returns its reference.
    String createCustomer(String username, String displayName) throws IOException;

    /// Deletes the processor's record of a customer, and the cards saved to it
    /// with it. A record that is already gone is deleted: this is asked again
    /// when the first answer was lost.
    void forgetCustomer(String customer) throws IOException;

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

    /// Takes a saved card off `customer`, so that it can't be charged again.
    /// A card that is already off, or gone, is detached: this is asked again
    /// when the first answer was lost.
    ///
    /// @param token what [#finishCardSetup] returned as the card's token
    void detachCard(String customer, String token) throws IOException;

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
    /// A processor may take the refund and only later say whether the bank
    /// returned the money: such a refund comes back [Refund#pending], and is
    /// asked about with [#refundStatus] until it has an answer.
    ///
    /// @return the refund as the processor took it
    /// - `IOException`: when the processor could not be asked, or refused the
    ///   refund outright
    Refund refund(String chargeReference, long amountCents) throws IOException;

    /// What became of a refund that was [Refund#pending]: [#REFUND_SUCCEEDED],
    /// [#REFUND_PENDING] while the processor still does not know, or
    /// [#REFUND_FAILED] when the money did not go back.
    String refundStatus(String refundReference) throws IOException;
    /// Holds `amountCents` on a saved card without taking it, for an order
    /// that is paid for only once it arrives. Keyed as [#charge] is.
    ///
    /// @return the processor's reference for the hold, for [#capture] or [#release]
    /// - `PaymentDeclinedException`: when the card was refused
    String authorize(String key, String customer, String token, long amountCents,
            String currency, String description) throws IOException;
    /// Takes `amountCents`, at most what was held, from a hold. A hold that was
    /// already captured is captured: this is asked again when the first answer
    /// was lost.
    void capture(String reference, long amountCents) throws IOException;
    /// Lets a hold go, so the money is the cardholder's again. A hold that is
    /// already let go is released.
    void release(String reference) throws IOException;
    // end::reel-payment-provider[]

    String REFUND_SUCCEEDED = "succeeded";
    String REFUND_PENDING = "pending";
    String REFUND_FAILED = "failed";

    /// A refund the processor took.
    final class Refund {
        public String reference = "";
        /// Whether the money is not yet known to be back on the card.
        public boolean pending;
    }

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
