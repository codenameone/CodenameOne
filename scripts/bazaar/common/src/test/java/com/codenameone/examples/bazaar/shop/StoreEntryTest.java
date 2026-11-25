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
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

/// Which store's cart the shopper sees. The shop walkthrough lost its cart:
/// a guest filled a cart at Fennel & Fig from a search, signed in, and opened
/// a product of another store, which made that store the current one; the
/// Cart section showed only the current store's (empty) cart, with the
/// merged Fennel & Fig cart still on the server.
class StoreEntryTest {
    @Test
    void anEmptyCurrentCartGivesWayToTheOneLastFilled() {
        // Remembered cart still to be fetched (-1): go and look.
        assertEquals("fennel", ShopState.cartStore("northwind", 0, "fennel", -1));
        // ...and it has lines: show it.
        assertEquals("fennel", ShopState.cartStore("northwind", 0, "fennel", 2));
        assertEquals("fennel", ShopState.cartStore(null, 0, "fennel", -1));
        assertEquals("northwind", ShopState.cartStore("northwind", 0, "", -1));
        assertEquals("northwind", ShopState.cartStore("northwind", 0, null, -1));
    }

    @Test
    void theCurrentStoresCartWinsWhenItHasLines() {
        assertEquals("northwind", ShopState.cartStore("northwind", 1, "fennel", -1));
        assertEquals("northwind", ShopState.cartStore("northwind", 3, "fennel", 2));
    }

    @Test
    void anEmptyRememberedCartDoesNotPullTheShopperBack() {
        // Ordered at Fennel & Fig (its cart now empty), browsing Northwind.
        assertEquals("northwind", ShopState.cartStore("northwind", 0, "fennel", 0));
        assertNull(ShopState.cartStore(null, 0, "fennel", 0));
        assertNull(ShopState.cartStore(null, 0, "", -1));
        assertEquals("fennel", ShopState.cartStore("fennel", 0, "fennel", -1));
    }

    @Test
    void aStoreNoListingShowedIsLookedUpNotIgnored() {
        assertEquals(ShopState.ENTER_LOOKUP, ShopState.entering(null, "fennel", false));
        assertEquals(ShopState.ENTER_LOOKUP, ShopState.entering("northwind", "fennel", false));
    }

    @Test
    void aKnownStoreIsEnteredAndTheCurrentOneKept() {
        assertEquals(ShopState.ENTER_SWITCH, ShopState.entering("northwind", "fennel", true));
        assertEquals(ShopState.ENTER_SWITCH, ShopState.entering(null, "fennel", true));
        assertEquals(ShopState.ENTER_STAY, ShopState.entering("fennel", "fennel", false));
    }

    @Test
    void noStoreNamedChangesNothing() {
        assertEquals(ShopState.ENTER_NONE, ShopState.entering("northwind", null, true));
        assertEquals(ShopState.ENTER_NONE, ShopState.entering("northwind", "", false));
    }
}
