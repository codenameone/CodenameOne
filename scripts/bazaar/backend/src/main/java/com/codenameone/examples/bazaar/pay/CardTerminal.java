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
package com.codenameone.examples.bazaar.pay;

import java.io.IOException;

/// A card reader service driven from the server, the way Stripe Terminal's
/// server-driven integration works: the server pairs a reader by the code it
/// shows, opens a payment intent for a card that is present, hands the intent
/// to the reader, and then asks (or is told by a webhook) how it went. The
/// register never talks to the processor; it asks this server.
///
/// [SimulatedTerminal] is the reader of development and the tests;
/// [StripeTerminal] calls Stripe. Which one runs follows
/// `bazaar.payments.provider`, so a card taken on the reader is refunded by
/// the same processor the [PaymentProvider] talks to.
public interface CardTerminal {
    /// The intent is still at the reader, or the cardholder has not finished.
    String PENDING = "PENDING";
    String SUCCEEDED = "SUCCEEDED";
    String FAILED = "FAILED";
    String CANCELED = "CANCELED";

    /// `simulated` or `stripe`.
    String name();

    /// Pairs a reader by the registration code it displays.
    ///
    /// @param label the name the store gives it
    /// - `IOException`: when the code is not a reader's, or the processor
    ///   could not be asked
    Reader register(String registrationCode, String label) throws IOException;

    /// Unpairs a reader. One that is already gone is unpaired.
    void remove(String readerRef) throws IOException;

    /// Opens a payment intent for a card present at a reader, captured as
    /// soon as the card is approved.
    ///
    /// @param key names this intent and no other; asked twice with one key,
    ///     the processor opens one intent
    /// @return the processor's reference for the intent
    String createIntent(String key, long amountCents, String currency, String description)
            throws IOException;

    /// Shows the intent on the reader, which then asks for the card.
    void process(String readerRef, String intentRef) throws IOException;

    /// How the intent stands at the reader now.
    Outcome status(String readerRef, String intentRef) throws IOException;

    /// Takes whatever the reader is showing off it. An intent not yet paid is
    /// then left unpaid.
    void cancel(String readerRef, String intentRef) throws IOException;

    /// A reader as the processor knows it.
    final class Reader {
        public final String reference;
        public final String serial;

        public Reader(String reference, String serial) {
            this.reference = reference;
            this.serial = serial == null ? "" : serial;
        }
    }

    /// Where a payment at a reader stands: one of [#PENDING], [#SUCCEEDED],
    /// [#FAILED] or [#CANCELED], with the reason of a failure written for the
    /// cashier and, once paid, the card as a person names it (`Visa 4242`).
    final class Outcome {
        public final String status;
        public final String message;
        public final String cardLabel;

        public Outcome(String status, String message, String cardLabel) {
            this.status = status;
            this.message = message == null ? "" : message;
            this.cardLabel = cardLabel == null ? "" : cardLabel;
        }
    }
}
