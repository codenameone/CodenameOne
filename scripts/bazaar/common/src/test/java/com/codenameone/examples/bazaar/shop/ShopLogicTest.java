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
package com.codenameone.examples.bazaar.shop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.codenameone.examples.bazaar.api.AddressDto;
import com.codenameone.examples.bazaar.ui.ArtPalette;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/// The storefront's pure logic: money, saved addresses, placeholder art
/// colours and search toggles. Nothing here opens a display.
class ShopLogicTest {
    @Test
    void moneyKeepsTheSignUnderOneUnit() {
        assertEquals("-$0.50", Money.format(-50, "USD", "USD"));
        assertEquals("$12.05", Money.format(1205, null, "USD"));
        assertEquals("12.40 EUR", Money.format(1240, "EUR", "USD"));
        assertEquals("$10.00 - $14.00", Money.range(1000, 1400, "USD", "USD"));
        assertEquals("$10.00", Money.range(1000, 1000, "USD", "USD"));
    }

    @Test
    void percentOffIsGuarded() {
        assertEquals(25, Money.percentOff(750, 1000));
        assertEquals(0, Money.percentOff(750, 0));
        assertEquals(0, Money.percentOff(1200, 1000));
    }

    @Test
    void parsesTypedPrices() {
        assertEquals(1200, Money.parse("12"));
        assertEquals(1250, Money.parse("$12.5"));
        assertEquals(1250, Money.parse(" 12.50 "));
        assertEquals(-1, Money.parse("abc"));
        assertEquals(-1, Money.parse(""));
        assertEquals(-1, Money.parse(null));
    }

    @Test
    void filterPrefillKeepsCents() {
        assertEquals("12", Money.typed(1200));
        assertEquals("12.50", Money.typed(1250));
        assertEquals("0.05", Money.typed(5));
        assertEquals("-12.50", Money.typed(-1250));
        assertEquals("-0.05", Money.typed(-5));
        assertEquals("-12", Money.typed(-1200));
        assertEquals(1250, Money.parse(Money.typed(1250)));
        assertEquals(1205, Money.parse(Money.typed(1205)));
    }

    @Test
    void addressesRoundTripAndDedupe() {
        AddressDto a = new AddressDto("Dana Levi", "12 Main St", "", "Springfield", "IL", "62701",
                "US", "555", 0, 0);
        AddressDto b = new AddressDto("Sam Roe", "1 Elm", "Apt 2", "Shelbyville", "", "1",
                "US", "", 0, 0);
        List<AddressDto> list = Addresses.remember(new ArrayList<>(), a);
        list = Addresses.remember(list, b);
        list = Addresses.remember(list, a);
        assertEquals(2, list.size());
        assertTrue(Addresses.same(a, list.get(0)));
        List<AddressDto> back = Addresses.decode(Addresses.encode(list));
        assertEquals(2, back.size());
        assertTrue(Addresses.same(b, back.get(1)));
        assertEquals("Apt 2", back.get(1).line2());
        assertEquals("Dana Levi, 12 Main St, Springfield, 62701", Addresses.summary(a));
        assertEquals("a b", Addresses.clean(" a\tb "));
    }

    @Test
    void artIsDeterministic() {
        assertEquals(ArtPalette.hash("p-1"), ArtPalette.hash("p-1"));
        assertTrue(ArtPalette.hash("anything") >= 0);
        int[] one = ArtPalette.colors("p-1", false);
        int[] two = ArtPalette.colors("p-1", false);
        assertEquals(one[0], two[0]);
        assertEquals("CM", ArtPalette.initials("ceramic mug"));
        assertEquals("", ArtPalette.initials(null));
        assertTrue(ArtPalette.typeWord("Coffee Mug", null) >= 0);
        assertEquals(-1, ArtPalette.typeWord("zzz", "qqq"));
    }

    @Test
    void togglesASingleValueFilter() {
        assertEquals("red", SearchQuery.toggle(null, "red"));
        assertNull(SearchQuery.toggle("red", "red"));
        assertFalse("blue".equals(SearchQuery.toggle("blue", "red")));
    }
}
