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

import com.codenameone.examples.bazaar.api.PaymentTypeDto;

import java.util.ArrayList;
import java.util.List;

/// The payment method types a store may offer beside cards, and where each
/// works: the currencies the processor takes it in and the shopper's
/// countries (billing or delivery) it is offered to. A store turns types on
/// in its checkout settings; a checkout offers the ones on whose rules the
/// store's currency and the shopper's country both pass.
///
/// The names are Stripe's payment method types, so the one the shopper picks
/// goes to the processor unchanged. An **asynchronous** type finishes after
/// the shopper leaves -- a bank debit clears days later, a pay-later
/// provider may still be deciding -- so its order waits as payment pending
/// until the processor's webhook says how it went. Every type here is
/// treated as asynchronous: even the ones that usually answer at once
/// (iDEAL, Bancontact) are settled only by the processor's own word, never
/// by the browser coming back.
public final class PaymentTypes {
    /// Cards, saved or typed, and the wallets (which pay with a card).
    public static final String CARD = "card";

    /// Type, label, currencies (empty for any), countries (empty for any).
    static final String[][] RULES = {
        {"klarna", "Klarna", "AUD,CAD,CHF,CZK,DKK,EUR,GBP,NOK,NZD,PLN,SEK,USD",
            "AT,AU,BE,CA,CH,CZ,DE,DK,ES,FI,FR,GB,GR,IE,IT,NL,NO,NZ,PL,PT,SE,US"},
        {"afterpay_clearpay", "Afterpay / Clearpay", "AUD,CAD,GBP,NZD,USD",
            "AU,CA,GB,NZ,US"},
        {"affirm", "Affirm", "CAD,USD", "CA,US"},
        {"ideal", "iDEAL", "EUR", "NL"},
        {"bancontact", "Bancontact", "EUR", "BE"},
        {"eps", "EPS", "EUR", "AT"},
        {"p24", "Przelewy24", "EUR,PLN", "PL"},
        {"sepa_debit", "SEPA Direct Debit", "EUR",
            "AT,BE,BG,CH,CY,CZ,DE,DK,EE,ES,FI,FR,GB,GR,HR,HU,IE,IS,IT,LI,LT,LU,LV,MT,NL,NO,PL,PT,"
            + "RO,SE,SI,SK"},
        // Manual tenders: no processor is involved. The order is placed
        // payment pending and the merchant marks it paid ([Orders#markPaid])
        // when the cash or the transfer arrives. Empty lists take any
        // currency and any country.
        {"cod", "Cash on delivery", "", ""},
        {"bank_transfer", "Bank transfer", "", ""},
    };

    private PaymentTypes() {
    }

    /// Whether `type` is paid by hand (cash on delivery, bank transfer):
    /// nothing is opened at a processor, and the merchant marks it paid.
    public static boolean manual(String type) {
        return "cod".equals(type) || "bank_transfer".equals(type);
    }

    /// Whether `type` is one a store may turn on.
    public static boolean known(String type) {
        return rule(type) != null;
    }

    /// Every type a store may turn on, in the order the settings list them.
    public static List<PaymentTypeDto> all() {
        List<PaymentTypeDto> out = new ArrayList<PaymentTypeDto>();
        for (int iter = 0; iter < RULES.length; iter++) {
            out.add(new PaymentTypeDto(RULES[iter][0], RULES[iter][1], true));
        }
        return out;
    }

    /// What a shopper reads for `type`.
    public static String label(String type) {
        if (CARD.equals(type)) {
            return "Card";
        }
        String[] r = rule(type);
        return r == null ? type : r[1];
    }

    /// Whether `type` is taken in `currency` from a shopper in `country`
    /// (two letters, upper case). An empty country passes the country rule:
    /// before an address is typed, the currency alone decides.
    public static boolean allowed(String type, String currency, String country) {
        String[] r = rule(type);
        if (r == null) {
            return false;
        }
        return listed(r[2], currency) && (country == null || country.length() == 0
                || listed(r[3], country));
    }

    /// The enabled types (in table order) that `currency` and `country`
    /// allow, after the card, which is always first.
    public static List<PaymentTypeDto> offered(List<String> enabled, String currency,
            String country) {
        List<PaymentTypeDto> out = new ArrayList<PaymentTypeDto>();
        out.add(new PaymentTypeDto(CARD, label(CARD), false));
        for (int iter = 0; iter < RULES.length; iter++) {
            String type = RULES[iter][0];
            if (enabled != null && enabled.contains(type) && allowed(type, currency, country)) {
                out.add(new PaymentTypeDto(type, RULES[iter][1], true));
            }
        }
        return out;
    }

    private static String[] rule(String type) {
        for (int iter = 0; type != null && iter < RULES.length; iter++) {
            if (RULES[iter][0].equals(type)) {
                return RULES[iter];
            }
        }
        return null;
    }

    /// Whether `code` is one of the comma separated `list`; an empty list
    /// holds everything.
    static boolean listed(String list, String code) {
        if (list.length() == 0) {
            return true;
        }
        if (code == null || code.length() == 0) {
            return false;
        }
        int start = 0;
        while (start <= list.length()) {
            int comma = list.indexOf(',', start);
            int end = comma < 0 ? list.length() : comma;
            if (end - start == code.length() && list.regionMatches(true, start, code, 0,
                    code.length())) {
                return true;
            }
            if (comma < 0) {
                return false;
            }
            start = comma + 1;
        }
        return false;
    }
}
