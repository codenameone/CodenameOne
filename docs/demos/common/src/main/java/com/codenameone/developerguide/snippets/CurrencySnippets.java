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
package com.codenameone.developerguide.snippets;

import com.codename1.l10n.L10NManager;

import java.util.Currency;
import java.util.Locale;

/// Snippets for currencies, included by the developer guide's localization
/// section.
public class CurrencySnippets {
    String priceLabel() {
        // tag::currency-l10n[]
        double amount = 19.99;
        String priceCurrency = "EUR";
        L10NManager l10n = L10NManager.getInstance();
        String userCurrency = l10n.getCurrencyCode();     // "USD" for a user in the US
        String price = l10n.formatCurrency(amount, priceCurrency);
        String symbol = l10n.getCurrencySymbol(priceCurrency);
        // end::currency-l10n[]
        return userCurrency + " " + price + " " + symbol;
    }

    String javaCurrency() {
        // tag::currency-java[]
        long minorUnits = 1999;                           // 19.99 in cents
        Currency yen = Currency.getInstance("JPY");
        int digits = yen.getDefaultFractionDigits();       // 0: no sen in prices
        Currency local = Currency.getInstance(new Locale("de", "DE")); // EUR

        java.text.NumberFormat format = java.text.NumberFormat.getCurrencyInstance();
        format.setCurrency(local);
        String text = format.format(minorUnits / 100.0);
        // end::currency-java[]
        return digits + " " + text;
    }
}
