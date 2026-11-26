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
package com.codenameone.examples.bazaar.web;

import java.util.ArrayList;
import java.util.List;

/// The checkout page: what was typed, the choices it leads to and every
/// amount, with the reason next to each field the server refused.
public final class CheckoutView {
    public String action = "";
    public String codesAction = "";
    public String placeAction = "";
    /// The bot guard's timing token for the place form (BotGuard).
    public String fillToken = "";
    /// The estimated import duties, or empty.
    public String duty = "";
    /// Which tax exemption certificate applies, or empty.
    public String exemption = "";
    /// That the order is charged in the store's currency, for a shopper who
    /// browsed another market's prices; or empty.
    public String marketNote = "";
    /// A company buyer on net terms may place the cart on account instead:
    /// what that means in words, and where the button posts.
    public boolean netTerms;
    public String termsLabel = "";
    public String termsAction = "";
    public String cartUrl = "";
    public boolean empty = true;
    public boolean signedIn;
    public String key = "";

    public String email = "";
    public String name = "";
    public String line1 = "";
    public String line2 = "";
    public String city = "";
    public String region = "";
    public String postal = "";
    public String phone = "";
    public String note = "";
    public List<Option> countries = new ArrayList<Option>();
    public List<Option> addresses = new ArrayList<Option>();
    public List<Option> rates = new ArrayList<Option>();
    public List<Option> methods = new ArrayList<Option>();
    /// Whether a card can be typed here (see `Payments.takesTypedCards`).
    public boolean typedCards;
    /// Whether the typed card is the chosen method.
    public boolean typedChosen = true;

    public List<CartLineView> lines = new ArrayList<CartLineView>();
    public String subtotal = "";
    public String discount = "";
    public String shipping = "";
    public String tax = "";
    public boolean taxInclusive;
    public String total = "";
    public String giftCard = "";
    public String due = "";
    public boolean nothingDue;
    public String discountCode = "";
    public String giftCardCode = "";
    /// What store credit pays ("-$5.00"), or empty.
    public String storeCredit = "";
    /// The signed-in shopper's credit at the store, or empty for none.
    public String creditAvailable = "";
    public boolean usingCredit;

    /// Problems with the order as a whole, shown first, in an alert.
    public List<String> problems = new ArrayList<String>();
    public String emailError = "";
    public String nameError = "";
    public String line1Error = "";
    public String cityError = "";
    public String countryError = "";
    public String rateError = "";
    public String codeError = "";
    public String cardError = "";
    /// Whether any error is shown, for the summary at the top.
    public boolean invalid;

    /// The tips on offer, "none" first; empty with tips off.
    public List<Option> tips = new ArrayList<Option>();
    /// The store's own questions ([Field]).
    public List<Field> fields = new ArrayList<Field>();
    public String fieldError = "";
    /// The payment method types beside the card, for this currency and
    /// country; empty when only cards are taken.
    public List<Option> payTypes = new ArrayList<Option>();

    /// One of the store's checkout fields, named `f<n>` in the form.
    public static final class Field {
        public String name = "";
        public String label = "";
        public String kind = "";
        public boolean required;
        public String value = "";
        public boolean checked;
        public List<Option> choices = new ArrayList<Option>();
        /// Which of the three kinds it is.
        public boolean text;
        public boolean checkbox;
        public boolean select;
    }

    /// A choice of a select or a radio group.
    public static final class Option {
        public String value = "";
        public String label = "";
        public String detail = "";
        public boolean chosen;

        public Option() {
        }

        public Option(String value, String label, String detail, boolean chosen) {
            this.value = value;
            this.label = label;
            this.detail = detail;
            this.chosen = chosen;
        }
    }
}
