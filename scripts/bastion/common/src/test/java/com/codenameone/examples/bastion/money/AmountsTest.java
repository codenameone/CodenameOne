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
package com.codenameone.examples.bastion.money;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// Amounts go to text and back exactly, without a `double` anywhere.
class AmountsTest {
    @Test
    void formatsWithTheAssetsDecimals() {
        assertEquals("$1,234.56", Amounts.format(123456, "USD"));
        assertEquals("$0.05", Amounts.format(5, "USD"));
        assertEquals("-$12.00", Amounts.format(-1200, "USD"));
        assertEquals("1,000,000.00 EUR", Amounts.format(100000000, "EUR"));
        assertEquals("0.015 BTC", Amounts.format(1500000, "BTC"));
        assertEquals("1.00 BTC", Amounts.format(100000000, "BTC"));
        assertEquals("0.000000001 ETH", Amounts.format(1, "ETH"));
        assertEquals("-9,223,372,036,854,775,808", Amounts.format(Long.MIN_VALUE, 0, 0));
    }

    @Test
    void parsesExactlyOrRefuses() {
        assertEquals(1050, Amounts.parse("10.5", "USD"));
        assertEquals(120050, Amounts.parse("1,200.50", "USD"));
        assertEquals(50, Amounts.parse(".5", "USD"));
        assertEquals(1200, Amounts.parse(" 12 ", "USD"));
        assertEquals(1, Amounts.parse("0.00000001", "BTC"));
        assertEquals(Amounts.INVALID, Amounts.parse("0.001", "USD"));
        assertEquals(Amounts.INVALID, Amounts.parse("-5", "USD"));
        assertEquals(Amounts.INVALID, Amounts.parse("1.2.3", "USD"));
        assertEquals(Amounts.INVALID, Amounts.parse("1e5", "USD"));
        assertEquals(Amounts.INVALID, Amounts.parse(".", "USD"));
        assertEquals(Amounts.INVALID, Amounts.parse("1.5,0", "USD"));
        assertEquals(Amounts.INVALID, Amounts.parse("99999999999999999999", "USD"));
        assertEquals(Amounts.INVALID, Amounts.parse("92233720368547758.08", "USD"));
        assertEquals(Long.MAX_VALUE, Amounts.parse("92233720368547758.07", "USD"));
    }

    @Test
    void roundTripsThroughTheFieldForm() {
        assertEquals("1.2345", Amounts.plain(123450000, "BTC"));
        assertEquals(123450000, Amounts.parse(Amounts.plain(123450000, "BTC"), "BTC"));
        assertEquals("1234.50", Amounts.plain(123450, "USD"));
    }

    @Test
    void valuesWithoutOverflowRoundingDown() {
        // 1.5 BTC at $60,000.00 is $90,000.00.
        assertEquals(9000000, Amounts.value(150000000, 8, 6000000));
        // One satoshi at $60,000.00 is 0.06 cents: rounds down to 0.
        assertEquals(0, Amounts.value(1, 8, 6000000));
        // 10 million BTC at $60,000.00 -- q * p would overflow a long.
        assertEquals(6000000L * 10000000L, Amounts.value(1000000000000000L, 8, 6000000));
    }
}
