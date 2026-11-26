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
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.codename1.backend.DataSource;
import com.codename1.backend.annotations.Autowired;
import com.codename1.backend.test.BackendTest;
import com.codename1.backend.test.MockMvc;
import com.codenameone.examples.bazaar.account.Accounts;
import com.codenameone.examples.bazaar.api.LoyaltyProgramDto;
import com.codenameone.examples.bazaar.api.LoyaltyTierDto;
import com.codenameone.examples.bazaar.api.OrderDto;
import com.codenameone.examples.bazaar.api.RewardsDto;
import com.codenameone.examples.bazaar.api.StoreBalanceDto;
import com.codenameone.examples.bazaar.cart.Carts;
import com.codenameone.examples.bazaar.checkout.Checkouts;
import com.codenameone.examples.bazaar.inventory.Inventory;
import com.codenameone.examples.bazaar.loyalty.Loyalty;
import com.codenameone.examples.bazaar.loyalty.LoyaltyMath;
import com.codenameone.examples.bazaar.order.Orders;
import com.codenameone.examples.bazaar.pricing.DemoPricing;
import com.codenameone.examples.bazaar.store.RewardsEndpoint;
import com.codenameone.examples.bazaar.store.Stores;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/// The rewards on the Account tab: balances kept per store (programmes
/// differ, so they are never summed), the store list read from the shopper's
/// memberships, an order's points read from the ledger, and nothing of
/// anyone else's.
@BackendTest
class RewardsEndpointTest {
    @Autowired
    private Accounts accounts;
    @Autowired
    private Stores stores;
    @Autowired
    private DemoPricing pricing;
    @Autowired
    private DataSource db;
    @Autowired
    private Inventory inventory;
    @Autowired
    private Carts carts;
    @Autowired
    private Checkouts checkouts;
    @Autowired
    private MockMvc mvc;
    @Autowired
    private Loyalty loyalty;
    @Autowired
    private Orders orderService;
    @Autowired
    private RewardsEndpoint rewards;

    private Shop shop(String name, int perDollar) throws Exception {
        Shop s = new Shop(name, 50, accounts, stores, pricing, db, inventory, carts, checkouts,
                mvc);
        program(s, perDollar);
        return s;
    }

    private void program(Shop s, int perDollar) throws Exception {
        List<LoyaltyTierDto> tiers = new ArrayList<LoyaltyTierDto>();
        tiers.add(new LoyaltyTierDto(null, "Base", 0, 0, false, 0, "#999999", 0));
        loyalty.save(s.storeId, new LoyaltyProgramDto(true, perDollar, 100, LoyaltyMath.DOWN,
                100, 500, 100, 365, true, "SPEND", 0, 0, tiers));
    }

    /// An order bought and paid, so the ledger credits its points.
    private OrderDto paid(Shop s, String user, int qty) throws Exception {
        OrderDto o = s.buy(user, qty);
        orderService.capturePayment(o.id(), s.owner);
        return o;
    }

    private static StoreBalanceDto at(RewardsDto r, String storeId) {
        for (StoreBalanceDto b : r.balances()) {
            if (b.storeId().equals(storeId)) {
                return b;
            }
        }
        return null;
    }

    @Test
    void balancesArePerStoreAndOrderPointsComeFromTheLedger() throws Exception {
        Shop a = shop("rw-a", 1);
        Shop b = shop("rw-b", 5);
        Shop c = shop("rw-c", 1);
        String user = People.customer(accounts, "rw-buyer");
        OrderDto fromA = paid(a, user, 2);
        paid(b, user, 1);
        // The only order at C is cancelled; the membership it made stays.
        OrderDto fromC = paid(c, user, 1);
        db.execute("UPDATE store_order SET status = ? WHERE id = ?",
                new Object[] {Orders.CANCELLED, fromC.id()});

        long balanceA = loyalty.me(user, a.storeId).balance();
        long balanceB = loyalty.me(user, b.storeId).balance();
        assertTrue(balanceA > 0 && balanceB > 0, balanceA + " / " + balanceB);
        RewardsDto r = rewards.rewards(user, fromA.id());
        assertEquals(3, r.balances().size(), "one per membership, C's included");
        assertEquals(balanceA, at(r, a.storeId).points());
        assertEquals(balanceB, at(r, b.storeId).points());
        assertTrue(at(r, c.storeId) != null, "a cancelled-only store is still listed");
        assertTrue(at(r, c.storeId).storeName().length() > 0, "labelled with the store");
        assertEquals(r.balances().get(0).points(), r.points(),
                "the headline is the best store's balance, never a sum");
        assertEquals(r.balances().get(0).storeName(), r.storeName());
        assertTrue(r.points() < balanceA + balanceB);
        assertEquals(2, r.orders(), "the cancelled order is not counted");

        // The order's points are what the ledger credited: a programme changed
        // since does not rewrite them.
        long credited = 0;
        for (com.codenameone.examples.bazaar.api.LedgerEntryDto e : loyalty.myLedger(user,
                a.storeId)) {
            if (Loyalty.EARN.equals(e.kind()) && fromA.id().equals(e.orderId())) {
                credited = e.points();
            }
        }
        assertTrue(credited > 0);
        assertEquals(credited, r.orderPoints());
        assertEquals(balanceA, r.orderBalance());
        program(a, 20);
        assertEquals(credited, rewards.rewards(user, fromA.id()).orderPoints());
    }

    @Test
    void anotherShopperSeesOnlyTheirOwn() throws Exception {
        Shop a = shop("rw-own", 1);
        String user = People.customer(accounts, "rw-own-buyer");
        OrderDto o = paid(a, user, 2);
        String other = People.customer(accounts, "rw-own-other");
        RewardsDto theirs = rewards.rewards(other, o.id());
        assertEquals(0, theirs.orderPoints(), "someone else's order earns nothing here");
        assertEquals(0, theirs.orderBalance());
        assertEquals(0, theirs.balances().size());
        assertEquals(0, theirs.points());
        assertEquals(0, theirs.orders());
        assertEquals(0, rewards.rewards(null, o.id()).orderPoints(), "a guest has nothing");
        assertTrue(rewards.rewards(user, o.id()).orderPoints() > 0);
    }
}
