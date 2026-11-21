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
package com.codenameone.examples.bazaar.pos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.codenameone.examples.bazaar.api.ParkedCartDto;
import com.codenameone.examples.bazaar.api.PosCatalogDto;
import com.codenameone.examples.bazaar.api.PosItemDto;
import com.codenameone.examples.bazaar.api.PosLineDto;
import com.codenameone.examples.bazaar.api.PosPolicyDto;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/// The cached policy, when a sale, refund or edit needs a manager, the money
/// of a layaway, and a parked cart coming back.
class PosRulesTest {
    private static PosPolicyDto policy(long discountBp, boolean overrides, boolean refunds,
            boolean edits) {
        return new PosPolicyDto(discountBp, overrides, refunds, edits, 1500, 1800, 2000, 2000, 1000);
    }

    @Test
    void thePolicyCacheRoundTripsAndRefusesJunk() {
        PosPolicyDto p = policy(1500, true, false, true);
        assertEquals(p, PosRules.decode(PosRules.encode(p)));
        assertNull(PosRules.decode(""));
        assertNull(PosRules.decode(null));
        assertNull(PosRules.decode("1,2,3"));
        assertNull(PosRules.decode("1,0,0,0,1500,1800,2000,2000,x"));
        assertNull(PosRules.decode("1,0,0,0,1500,1800,2000,2000,1000,7"));
        assertEquals(10000L, PosRules.decode("99999,0,0,0,0,0,0,0,0").discountApprovalBp());
    }

    @Test
    void aSaleNeedsAManagerOnlyPastTheThreshold() {
        PosPolicyDto p = policy(1000, false, false, false);
        long[] list = {1000, 500};
        List<PosLineDto> small = Arrays.asList(new PosLineDto("a", 1, 900, "", true, true),
                new PosLineDto("b", 1, 500, "", true, false));
        assertNull(PosRules.saleApproval(p, "CASHIER", small, list, 0));
        List<PosLineDto> big = Arrays.asList(new PosLineDto("a", 1, 800, "", true, true),
                new PosLineDto("b", 1, 500, "", true, false));
        assertEquals(PosRules.DISCOUNT, PosRules.saleApproval(p, "CASHIER", big, list, 0));
        assertEquals(PosRules.DISCOUNT, PosRules.saleApproval(p, "CASHIER", small, list, 100));
        assertNull(PosRules.saleApproval(p, "MANAGER", big, list, 0));
        assertNull(PosRules.saleApproval(p, "owner", big, list, 0));
        assertNull(PosRules.saleApproval(null, "CASHIER", big, list, 0));
        assertNull(PosRules.saleApproval(p, "CASHIER", Arrays.<PosLineDto>asList(), new long[0],
                500));
    }

    @Test
    void aChangedPriceNeedsAManagerWhenThePolicySays() {
        long[] list = {1000};
        List<PosLineDto> raised = Arrays.asList(new PosLineDto("a", 1, 1200, "", true, true));
        assertEquals(PosRules.PRICE_OVERRIDE, PosRules.saleApproval(policy(0, true, false, false),
                "CASHIER", raised, list, 0));
        assertNull(PosRules.saleApproval(policy(0, false, false, false), "CASHIER", raised, list,
                0));
    }

    @Test
    void refundsAndEditsFollowTheirSwitches() {
        assertTrue(PosRules.refundNeedsApproval(policy(0, false, true, false), "CASHIER"));
        assertFalse(PosRules.refundNeedsApproval(policy(0, false, true, false), "MANAGER"));
        assertFalse(PosRules.refundNeedsApproval(policy(0, false, false, false), "CASHIER"));
        assertTrue(PosRules.editNeedsApproval(policy(0, false, false, true), "CASHIER"));
        assertFalse(PosRules.editNeedsApproval(null, "CASHIER"));
    }

    @Test
    void layawayMoney() {
        assertEquals(2000, PosRules.minimumDeposit(10000, 2000));
        assertEquals(0, PosRules.minimumDeposit(10000, 0));
        assertEquals(0, PosRules.minimumDeposit(0, 2000));
        assertEquals(10000, PosRules.minimumDeposit(10000, 99999));
        assertEquals(7500, PosRules.balance(10000, 2500));
        assertEquals(0, PosRules.balance(10000, 12000));
        assertEquals(25, PosRules.percentPaid(10000, 2500));
        assertEquals(0, PosRules.percentPaid(0, 2500));
        assertEquals(100, PosRules.percentPaid(100, 500));
        assertEquals(7500, PosRules.payment(9000, 10000, 2500));
        assertEquals(0, PosRules.payment(-5, 10000, 2500));
        assertEquals(1500, PosRules.cancelRefund(10000, 2500, 1000));
        assertEquals(0, PosRules.cancelRefund(10000, 500, 1000));
    }

    @Test
    void aParkedCartComesBackWithItsPricesAndLostItemsAsCustom() {
        PosItemDto mug = new PosItemDto("v1", "p1", "Mug", "Default", "", "", 1000, 0, true,
                false, 0, "");
        PosCatalogDto cat = new PosCatalogDto("1", false, "s", "r", "l", "USD", false, "", "",
                Arrays.asList(mug), null, null);
        ParkedCartDto parked = new ParkedCartDto("", "c1", "Ann", "", "", "note",
                Arrays.asList(new PosLineDto("v1", 2, 800, "", true, true),
                        new PosLineDto("gone", 1, 300, "Old thing", false, false),
                        new PosLineDto("", 1, 250, "Wrap", false, false)),
                ParkedCarts.PARKED, 0);
        PosCart cart = new PosCart();
        ParkedPane.restore(cart, parked, cat);
        assertEquals(3, cart.items().size());
        assertEquals(800, cart.items().get(0).unitCents());
        assertTrue(PosCart.custom(cart.items().get(1).item));
        assertEquals(300, cart.items().get(1).unitCents());
        assertEquals("note", cart.note());
        assertEquals(2 * 800 + 300 + 250, cart.beforeOrderDiscount());
        assertEquals(4, ParkedPane.units(parked));
        assertEquals(2150, ParkedPane.total(parked));
    }
}
