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

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Currency;
import java.util.Locale;

/// java.util.Currency and the currency members of java.text: the same codes
/// give the same numeric codes, fraction digits and symbols, and a currency
/// format writes the same text.
///
/// Only currencies and countries whose ISO 4217 data has been stable for
/// years are compared, because the HotSpot reference may be a later JDK
/// than the Java 17 tables JavaAPI was generated from.
public final class CurrencyChecks {
    private CurrencyChecks() {
    }

    private static final Locale US = new Locale("en", "US");
    private static final String SIGN = String.valueOf((char) 0xA4);

    private static final String[] CODES = {
        "USD", "EUR", "JPY", "GBP", "CHF", "CAD", "AUD", "INR", "ILS", "BHD", "KWD", "CLF", "XAU", "XXX",
        "BRL", "MXN", "SEK", "NOK", "DKK", "PLN", "ZAR", "KRW", "CNY", "HKD", "SGD", "NZD", "TND",
    };

    private static final String[] COUNTRIES = {
        "US", "DE", "FR", "JP", "GB", "CH", "CA", "AU", "IN", "IL", "BH", "BR", "MX", "SE", "IT", "ES",
        "NL", "IE", "KR", "CN", "ZA", "NZ", "SG", "HK",
    };

    public static void run() {
        Bulk.check("Currency.codesNumericAndDigits", () -> {
            for (String code : CODES) {
                Currency c = Currency.getInstance(code);
                p(code + " code", c.getCurrencyCode());
                p(code + " numeric", c.getNumericCode());
                p(code + " numeric string", c.getNumericCodeAsString());
                p(code + " digits", c.getDefaultFractionDigits());
                p(code + " toString", c.toString());
            }
        });

        // Newer than some HotSpot references, so judged on ParparVM alone.
        Bulk.cn1Only("Currency.knowsTheIso4217AmendmentsAfterJava17", () -> {
            Bulk.expectEquals(Integer.valueOf(924), Integer.valueOf(Currency.getInstance("ZWG").getNumericCode()), "ZWG");
            Bulk.expectEquals("ZWG", Currency.getInstance(new Locale("", "ZW")).getCurrencyCode(), "Zimbabwe");
            Bulk.expectEquals("XCG", Currency.getInstance(new Locale("", "CW")).getCurrencyCode(), "Curacao");
            Bulk.expectEquals("XCG", Currency.getInstance(new Locale("", "SX")).getCurrencyCode(), "Sint Maarten");
            Bulk.expectEquals("ANG", Currency.getInstance("ANG").getCurrencyCode(), "ANG still known");
        });

        Bulk.check("Currency.isOneInstancePerCode", () -> {
            p("same", Currency.getInstance("EUR") == Currency.getInstance("EUR"));
            p("by locale", Currency.getInstance(Locale.GERMANY) == Currency.getInstance("EUR"));
            p("in set", Currency.getAvailableCurrencies().contains(Currency.getInstance("JPY")));
        });

        Bulk.check("Currency.countryCurrency", () -> {
            for (String country : COUNTRIES) {
                p(country, Currency.getInstance(new Locale("", country)).getCurrencyCode());
            }
            p("antarctica", String.valueOf(Currency.getInstance(new Locale("", "AQ"))));
        });

        Bulk.check("Currency.rejectsBadInput", () -> {
            Bulk.pThrown("unknown code", IllegalArgumentException.class, () -> Currency.getInstance("ABC"));
            Bulk.pThrown("lower case", IllegalArgumentException.class, () -> Currency.getInstance("usd"));
            Bulk.pThrown("no country", IllegalArgumentException.class, () -> Currency.getInstance(Locale.ENGLISH));
            Bulk.pThrown("null code", NullPointerException.class, () -> Currency.getInstance((String) null));
        });

        Bulk.check("Currency.symbolsInUsEnglish", () -> {
            String[] codes = {"USD", "EUR", "JPY", "GBP", "CAD", "AUD", "INR", "ILS", "CHF"};
            for (String code : codes) {
                p(code, Currency.getInstance(code).getSymbol(US));
            }
            p("USD in UK", Currency.getInstance("USD").getSymbol(Locale.UK));
            p("GBP in UK", Currency.getInstance("GBP").getSymbol(Locale.UK));
            p("CAD in Canada", Currency.getInstance("CAD").getSymbol(Locale.CANADA));
        });

        Bulk.check("Currency.displayNamesInEnglish", () -> {
            for (String code : new String[] {"USD", "EUR", "JPY", "GBP", "CHF"}) {
                p(code, Currency.getInstance(code).getDisplayName(Locale.ENGLISH));
            }
        });

        Bulk.check("Currency.formatInUsEnglish", () -> {
            NumberFormat f = NumberFormat.getCurrencyInstance(US);
            p("currency", f.getCurrency().getCurrencyCode());
            p("dollars", f.format(1234.5));
            f.setCurrency(Currency.getInstance("EUR"));
            p("euros", f.format(1234.5));
            p("euro currency", f.getCurrency().getCurrencyCode());
            DecimalFormat intl = new DecimalFormat(SIGN + SIGN + " #,##0.00", DecimalFormatSymbols.getInstance(US));
            intl.setCurrency(Currency.getInstance("GBP"));
            p("international", intl.format(-7.25));
        });

        Bulk.check("Currency.symbolsFollowTheCurrency", () -> {
            DecimalFormatSymbols s = DecimalFormatSymbols.getInstance(US);
            p("default", s.getCurrency().getCurrencyCode());
            p("default intl", s.getInternationalCurrencySymbol());
            s.setCurrency(Currency.getInstance("JPY"));
            p("symbol", s.getCurrencySymbol());
            p("intl", s.getInternationalCurrencySymbol());
            s.setInternationalCurrencySymbol("CHF");
            p("by code", s.getCurrency().getCurrencyCode());
            s.setInternationalCurrencySymbol("not a code");
            p("by text", String.valueOf(s.getCurrency()));
        });
    }

    private static void p(String key, Object value) {
        Bulk.p(key, value);
    }

    private static void p(String key, int value) {
        Bulk.p(key, value);
    }

    private static void p(String key, boolean value) {
        Bulk.p(key, value);
    }
}
