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

/// Prices as a page shows them. Amounts are minor units (cents).
final class Money {
    private Money() {
    }

    /// `1250, USD` -> `$12.50`; a currency without a symbol here is spelled
    /// out in front: `CHF 12.50`.
    static String format(long cents, String currency) {
        String symbol = symbol(currency);
        String amount = plain(Math.abs(cents));
        String sign = cents < 0 ? "-" : "";
        return symbol.length() > 0 ? sign + symbol + amount : sign + currency + " " + amount;
    }

    /// `1250` -> `12.50`: what structured data wants for a price.
    static String plain(long cents) {
        long a = Math.abs(cents);
        long frac = a % 100;
        return (cents < 0 ? "-" : "") + (a / 100) + "." + (frac < 10 ? "0" : "") + frac;
    }

    private static String symbol(String currency) {
        if ("USD".equalsIgnoreCase(currency) || "CAD".equalsIgnoreCase(currency)
                || "AUD".equalsIgnoreCase(currency) || "NZD".equalsIgnoreCase(currency)) {
            return "$";
        }
        if ("EUR".equalsIgnoreCase(currency)) {
            return "\u20ac";
        }
        if ("GBP".equalsIgnoreCase(currency)) {
            return "\u00a3";
        }
        if ("ILS".equalsIgnoreCase(currency)) {
            return "\u20aa";
        }
        if ("JPY".equalsIgnoreCase(currency)) {
            return "\u00a5";
        }
        return "";
    }
}
