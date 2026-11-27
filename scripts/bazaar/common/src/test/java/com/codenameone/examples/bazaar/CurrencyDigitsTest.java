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
package com.codenameone.examples.bazaar;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.codenameone.examples.bazaar.merchant.ProductCsv;
import com.codenameone.examples.bazaar.shop.Money;
import org.junit.jupiter.api.Test;

/// The client's money paths in currencies with 0, 2 and 3 decimals: yen,
/// dollars and Bahraini dinars. Amounts are minor units of their own
/// currency, never hundredths. Nothing here opens a display or a bundle.
class CurrencyDigitsTest {
    @Test
    void shopperFormatsInTheCurrencysOwnDecimals() {
        assertEquals("$12.50", Money.format(1250, "USD", "USD"));
        assertEquals("1,250 JPY", Money.format(1250, "JPY", "USD"));
        assertEquals("1.250 BHD", Money.format(1250, "BHD", "USD"));
        assertEquals("-0.005 BHD", Money.format(-5, "BHD", "USD"));
        assertEquals("-5 JPY", Money.format(-5, "JPY", "USD"));
    }

    @Test
    void priceFilterRoundTrips() {
        assertEquals("12.50", Money.typed(1250, "USD"));
        assertEquals("1250", Money.typed(1250, "JPY"));
        assertEquals("1.250", Money.typed(1250, "BHD"));
        assertEquals("0.005", Money.typed(5, "BHD"));
        assertEquals(1250, Money.parse("12.5", "USD"));
        assertEquals(1250, Money.parse("1250", "JPY"));
        assertEquals(1250, Money.parse("1.25", "BHD"));
        assertEquals(1205, Money.parse("1.205", "BHD"));
        // More decimals than the currency has is refused, never cut.
        assertEquals(-1, Money.parse("12.345", "USD"));
        assertEquals(-1, Money.parse("1250.5", "JPY"));
        assertEquals(1250, Money.parse("12.500", "USD"));
        long[] amounts = {0, 5, 1205, 1250, 99999};
        String[] codes = {"USD", "JPY", "BHD"};
        for (String code : codes) {
            for (long a : amounts) {
                assertEquals(a, Money.parse(Money.typed(a, code), code), code + " " + a);
            }
        }
    }

    @Test
    void merchantFieldsRoundTrip() {
        assertEquals("12.50", ProductCsv.major(1250, "USD"));
        assertEquals("1250", ProductCsv.major(1250, "JPY"));
        assertEquals("1.250", ProductCsv.major(1250, "BHD"));
        assertEquals(1250, ProductCsv.cents("12,50", "USD"));
        assertEquals(1250, ProductCsv.cents("1,250", "JPY"));
        assertEquals(1250, ProductCsv.cents("1250", "JPY"));
        assertEquals(1250, ProductCsv.cents("1.25", "BHD"));
        assertEquals(1250, ProductCsv.cents("1,25", "BHD"));
        // A price with more decimals than the currency is not a price: the
        // field keeps its value rather than saving a cut-down one.
        assertEquals(com.codenameone.examples.bazaar.ops.Amounts.NOT_AN_AMOUNT,
                ProductCsv.amount("12.345", "USD"));
        assertEquals(com.codenameone.examples.bazaar.ops.Amounts.NOT_AN_AMOUNT,
                ProductCsv.amount("1250.5", "JPY"));
        assertEquals(1250, ProductCsv.amount("12.500", "USD"));
        assertEquals(0, ProductCsv.cents("12.345", "USD"));
        long[] amounts = {5, 1205, 1250, 99999};
        String[] codes = {"USD", "JPY", "BHD"};
        for (String code : codes) {
            for (long a : amounts) {
                assertEquals(a, ProductCsv.cents(ProductCsv.major(a, code), code), code + " " + a);
                assertEquals(-a, ProductCsv.cents(ProductCsv.major(-a, code), code), code + " -" + a);
            }
        }
        // Percentages and basis points keep two decimals whatever the store sells in.
        assertEquals("12.50", ProductCsv.major(1250));
        assertEquals(1250, ProductCsv.cents("12.5"));
    }

    @Test
    void fareBracketsCountWholeUnits() {
        assertEquals("10_to_20", Telemetry.fare(1250, "USD"));
        assertEquals("50_or_more", Telemetry.fare(1250, "JPY"));
        assertEquals("under_5", Telemetry.fare(1250, "BHD"));
    }
}
