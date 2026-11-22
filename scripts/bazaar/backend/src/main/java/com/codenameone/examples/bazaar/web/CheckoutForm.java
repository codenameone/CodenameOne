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

/// The fields the checkout form posts: one form, so a refresh of the rates, a
/// code applied and the order placed all carry what was typed so far. Strings
/// only, bound and then checked by [CheckoutPages]; binding never reaches the
/// database by itself.
///
/// The card's number and security code are read once, by the payment
/// provider, and never written back into a page.
public class CheckoutForm {
    public String email = "";
    public String addressId = "";
    public String name = "";
    public String line1 = "";
    public String line2 = "";
    public String city = "";
    public String region = "";
    public String postal = "";
    public String country = "";
    public String phone = "";
    public String rateId = "";
    public String note = "";
    /// `apply-discount`, `remove-discount`, `apply-gift` or `remove-gift`.
    public String codeAction = "";
    public String discountCode = "";
    public String giftCardCode = "";
    /// A saved method of a signed-in shopper, or empty for the typed card.
    public String methodId = "";
    public String cardNumber = "";
    /// `MM/YY`.
    public String cardExpiry = "";
    public String cardCvc = "";
    /// Names this checkout's order: a second submit of the same page finds
    /// the order the first one placed.
    public String idempotencyKey = "";
    /// The tip preset chosen, 0 for none.
    public String tip = "";
    /// The payment method type, empty or `card` for a card.
    public String payType = "";
    /// The store's checkout fields, in their order.
    public String f0 = "";
    public String f1 = "";
    public String f2 = "";
    public String f3 = "";
    public String f4 = "";
    public String f5 = "";
    public String f6 = "";
    public String f7 = "";
    public String f8 = "";
    public String f9 = "";

    /// The answer to field `n`.
    public String field(int n) {
        switch (n) {
            case 0: return f0;
            case 1: return f1;
            case 2: return f2;
            case 3: return f3;
            case 4: return f4;
            case 5: return f5;
            case 6: return f6;
            case 7: return f7;
            case 8: return f8;
            case 9: return f9;
            default: return "";
        }
    }
}
