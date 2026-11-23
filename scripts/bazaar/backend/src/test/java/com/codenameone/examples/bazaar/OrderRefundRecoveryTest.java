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

import com.codename1.backend.DataSource;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Autowired;
import com.codename1.backend.annotations.Bean;
import com.codename1.backend.annotations.Primary;
import com.codename1.backend.test.BackendTest;
import com.codename1.backend.test.MockMvc;
import com.codename1.backend.test.TestConfiguration;
import com.codenameone.examples.bazaar.account.Accounts;
import com.codenameone.examples.bazaar.api.OrderDto;
import com.codenameone.examples.bazaar.api.RefundLineDto;
import com.codenameone.examples.bazaar.api.RefundRequestDto;
import com.codenameone.examples.bazaar.cart.Carts;
import com.codenameone.examples.bazaar.checkout.Checkouts;
import com.codenameone.examples.bazaar.inventory.Inventory;
import com.codenameone.examples.bazaar.order.Orders;
import com.codenameone.examples.bazaar.pricing.DemoPricing;
import com.codenameone.examples.bazaar.store.Stores;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// An order refund whose card part the processor did not answer, or refused,
/// or that a crash interrupted: the order's claims stay while the card refund
/// may have been made, and the sweep (Orders.resumeRefunds) finishes the order
/// side exactly once, or releases it, when the processor answers.
@BackendTest
class OrderRefundRecoveryTest {
    @Autowired
    private MockMvc mvc;
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
    private Orders orders;
    @Autowired
    private ScriptedPayments processor;

    @TestConfiguration
    static class Processor {
        @Bean
        @Primary
        ScriptedPayments scriptedPayments() {
            return new ScriptedPayments();
        }
    }

    private Shop shop(String name) throws Exception {
        return new Shop(name, 10, accounts, stores, pricing, db, inventory, carts, checkouts, mvc);
    }

    private static RefundRequestDto mugs(OrderDto o, int quantity) {
        List<RefundLineDto> ls = new ArrayList<RefundLineDto>();
        ls.add(new RefundLineDto(o.lines().get(0).id(), quantity));
        return new RefundRequestDto(ls, 0, true, "Broken");
    }

    private long number(String sql, String arg) throws Exception {
        Map row = db.queryOne(sql, new Object[] {arg});
        return row == null ? -1 : Rows.number(row.get("n"));
    }

    private long events(String orderId, String kind) throws Exception {
        Map row = db.queryOne("SELECT COUNT(*) AS n FROM order_event WHERE order_id = ? "
                + "AND kind = ?", new Object[] {orderId, kind});
        return Rows.number(row.get("n"));
    }

    /// Ages every claim of `orderId` past the sweep's wait and runs it twice.
    private void sweep(String orderId) throws Exception {
        db.execute("UPDATE payment SET refund_claimed_at = 1 WHERE order_id = ? "
                + "AND status = 'REFUNDING'", new Object[] {orderId});
        db.execute("UPDATE order_refund_claim SET claimed_at = 1 WHERE order_id = ?",
                new Object[] {orderId});
        orders.resumeRefunds();
        orders.resumeRefunds();
    }

    @Test
    void anUnansweredRefundIsFinishedOnceByTheSweep() throws Exception {
        processor.reset();
        Shop s = shop("orr-lost");
        OrderDto o = s.buy(People.customer(accounts, "orr-lost-buyer"), 2);
        processor.refundFailures.add(Boolean.TRUE);
        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> orders.refund(s.owner, s.storeId, o.id(), mugs(o, 2)));
        assertEquals(409, e.getStatus());
        assertEquals(1, events(o.id(), "REFUND_PENDING"));
        // The claims stay while the processor may have made it.
        long total = orders.detailOf(o.id()).refundedCents();
        assertTrue(total > 0);
        assertEquals(2, orders.detailOf(o.id()).lines().get(0).refundedQty());
        assertEquals(8, inventory.available(s.variantId, s.locationId));

        sweep(o.id());
        assertEquals(2, processor.keys.size());
        assertEquals(processor.keys.get(0), processor.keys.get(1));
        assertEquals(total, number("SELECT refunded_cents AS n FROM payment WHERE order_id = ?",
                o.id()));
        assertEquals(total, orders.detailOf(o.id()).refundedCents());
        assertEquals(1, number("SELECT COUNT(*) AS n FROM refund WHERE order_id = ?", o.id()));
        assertEquals(1, events(o.id(), "REFUNDED"));
        assertEquals(10, inventory.available(s.variantId, s.locationId));
        assertEquals(0, number("SELECT COUNT(*) AS n FROM order_refund_claim WHERE order_id = ?",
                o.id()));
        processor.reset();
    }

    @Test
    void anotherRefundWaitsWhileOneIsBeingConfirmed() throws Exception {
        processor.reset();
        Shop s = shop("orr-wait");
        OrderDto o = s.buy(People.customer(accounts, "orr-wait-buyer"), 2);
        processor.refundFailures.add(Boolean.TRUE);
        assertThrows(ResponseStatusException.class,
                () -> orders.refund(s.owner, s.storeId, o.id(), mugs(o, 1)));
        long claimed = orders.detailOf(o.id()).refundedCents();
        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> orders.refund(s.owner, s.storeId, o.id(), mugs(o, 1)));
        assertEquals(409, e.getStatus());
        assertEquals(1, processor.keys.size());
        assertEquals(claimed, orders.detailOf(o.id()).refundedCents());
        assertEquals(1, orders.detailOf(o.id()).lines().get(0).refundedQty());
        processor.reset();
    }

    @Test
    void aRefusedRefundGivesItsClaimsBackAndTheRetryUsesANewKey() throws Exception {
        processor.reset();
        Shop s = shop("orr-refused");
        OrderDto o = s.buy(People.customer(accounts, "orr-refused-buyer"), 2);
        processor.refundRefusals.add(Boolean.TRUE);
        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> orders.refund(s.owner, s.storeId, o.id(), mugs(o, 2)));
        assertEquals(402, e.getStatus());
        assertEquals(0, orders.detailOf(o.id()).refundedCents());
        assertEquals(0, orders.detailOf(o.id()).lines().get(0).refundedQty());
        assertEquals(0, number("SELECT COUNT(*) AS n FROM order_refund_claim WHERE order_id = ?",
                o.id()));

        orders.refund(s.owner, s.storeId, o.id(), mugs(o, 2));
        assertEquals(2, processor.keys.size());
        assertNotEquals(processor.keys.get(0), processor.keys.get(1));
        assertTrue(orders.detailOf(o.id()).refundedCents() > 0);
        assertEquals(orders.detailOf(o.id()).refundedCents(), number("SELECT refunded_cents AS n "
                + "FROM payment WHERE order_id = ?", o.id()));
        assertEquals(10, inventory.available(s.variantId, s.locationId));
        processor.reset();
    }

    @Test
    void anOrderClaimNoPaymentClaimFollowedIsReleased() throws Exception {
        processor.reset();
        Shop s = shop("orr-orphan");
        OrderDto o = s.buy(People.customer(accounts, "orr-orphan-buyer"), 2);
        processor.refundFailures.add(Boolean.TRUE);
        assertThrows(ResponseStatusException.class,
                () -> orders.refund(s.owner, s.storeId, o.id(), mugs(o, 2)));
        // As if the server stopped between the order's claim and the
        // payment's: the payment was never claimed.
        db.execute("UPDATE payment SET status = 'PAID', pending_refund_cents = 0 "
                + "WHERE order_id = ?", new Object[] {o.id()});
        sweep(o.id());
        assertEquals(1, processor.keys.size());
        assertEquals(0, orders.detailOf(o.id()).refundedCents());
        assertEquals(0, orders.detailOf(o.id()).lines().get(0).refundedQty());
        assertEquals(1, events(o.id(), "REFUND_RELEASED"));
        orders.refund(s.owner, s.storeId, o.id(), mugs(o, 2));
        assertEquals(10, inventory.available(s.variantId, s.locationId));
        processor.reset();
    }
}
