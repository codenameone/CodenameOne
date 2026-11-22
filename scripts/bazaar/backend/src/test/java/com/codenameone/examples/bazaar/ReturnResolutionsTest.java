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
import com.codename1.backend.test.BackendTest;
import com.codename1.backend.test.MockMvc;
import com.codenameone.examples.bazaar.account.Accounts;
import com.codenameone.examples.bazaar.api.FulfilLineDto;
import com.codenameone.examples.bazaar.api.GiftCardDto;
import com.codenameone.examples.bazaar.api.RefundLineDto;
import com.codenameone.examples.bazaar.api.RefundRequestDto;
import com.codenameone.examples.bazaar.api.NewFulfillmentDto;
import com.codenameone.examples.bazaar.api.OrderDto;
import com.codenameone.examples.bazaar.api.OrderSettingsDto;
import com.codenameone.examples.bazaar.api.ReturnQuoteDto;
import com.codenameone.examples.bazaar.api.ReturnReceiveDto;
import com.codenameone.examples.bazaar.api.ReturnRefundDto;
import com.codenameone.examples.bazaar.api.SelfReturnDto;
import com.codenameone.examples.bazaar.cart.Carts;
import com.codenameone.examples.bazaar.checkout.Checkouts;
import com.codenameone.examples.bazaar.fulfil.Fulfillments;
import com.codenameone.examples.bazaar.giftcard.GiftCards;
import com.codenameone.examples.bazaar.inventory.Inventory;
import com.codenameone.examples.bazaar.order.OrderSettings;
import com.codenameone.examples.bazaar.order.Orders;
import com.codenameone.examples.bazaar.pricing.DemoPricing;
import com.codenameone.examples.bazaar.returns.Resolutions;
import com.codenameone.examples.bazaar.returns.Returns;
import com.codenameone.examples.bazaar.returns.SelfReturns;
import com.codenameone.examples.bazaar.store.StoreAccess;
import com.codenameone.examples.bazaar.store.Stores;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// A received return settled end to end by each resolution the shopper can
/// choose ([Resolutions]): store credit lands on the customer and nothing
/// goes back to the card; an exchange makes a replacement order paid with
/// the goods' value, mailing a link for a dearer variant's difference or
/// refunding what a cheaper one leaves over.
@BackendTest
class ReturnResolutionsTest {
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
    private Fulfillments fulfillments;
    @Autowired
    private Returns returns;
    @Autowired
    private SelfReturns selfReturns;
    @Autowired
    private OrderSettings settings;
    @Autowired
    private GiftCards giftCards;

    private Shop shop(String name) throws Exception {
        return new Shop(name, 10, accounts, stores, pricing, db, inventory, carts, checkouts, mvc);
    }

    /// Return rules with a flat 200 cent fee, and a delivered order of two
    /// 1000 cent mugs by `user`.
    private OrderDto delivered(Shop s, String user) throws Exception {
        settings.save(s.owner, s.storeId, new OrderSettingsDto(false, 30, 0, 200, "", ""));
        OrderDto o = s.buy(user, 2);
        List<FulfilLineDto> ls = new ArrayList<FulfilLineDto>();
        ls.add(new FulfilLineDto(o.lines().get(0).id(), null, null, 2));
        String fid = fulfillments.create(s.owner, s.storeId, o.id(),
                new NewFulfillmentDto(s.locationId, "LOCAL", ls)).id();
        fulfillments.ship(s.owner, s.storeId, fid);
        fulfillments.delivered(s.owner, s.storeId, fid);
        return o;
    }

    private static List<FulfilLineDto> oneMug(OrderDto o) {
        List<FulfilLineDto> ls = new ArrayList<FulfilLineDto>();
        ls.add(new FulfilLineDto(o.lines().get(0).id(), null, null, 1));
        return ls;
    }

    /// Approves, receives (restocking) and refunds return `id`, as staff.
    private void settle(Shop s, String id) throws Exception {
        returns.approve(s.owner, s.storeId, id);
        returns.receive(s.owner, s.storeId, id, new ReturnReceiveDto(true));
        returns.refund(s.owner, s.storeId, id, new ReturnRefundDto(0));
    }

    /// Another variant of the shop's mug, in stock.
    private String variant(Shop s, String title, long priceCents) throws Exception {
        String id = Ids.next();
        db.execute("INSERT INTO variant (id, product_id, store_id, title, option1, option2, "
                + "option3, sku, barcode, price_cents, compare_at_cents, cost_cents, weight_grams, "
                + "taxable, track_inventory, allow_backorder, position) VALUES "
                + "(?, ?, ?, ?, ?, '', '', ?, '', ?, 0, 0, 350, 1, 1, 0, 1)",
                new Object[] {id, s.productId, s.storeId, title, title, s.sku + "-" + title,
                    Long.valueOf(priceCents)});
        inventory.restock(s.storeId, id, s.locationId, 5, "opening", "test", Inventory.RESTOCK);
        return id;
    }

    private String text(String sql, String column, String arg) throws Exception {
        Map row = db.queryOne(sql, new Object[] {arg});
        return row == null ? "" : Rows.text(row.get(column));
    }

    private long count(String sql, String arg) throws Exception {
        Map row = db.queryOne(sql, new Object[] {arg});
        return row == null ? -1 : Rows.number(row.get("n"));
    }

    private long credit(String customerId) throws Exception {
        Map row = db.queryOne("SELECT store_credit_cents FROM customer WHERE id = ?",
                new Object[] {customerId});
        return Rows.number(row.get("store_credit_cents"));
    }

    @Test
    void storeCreditLandsOnTheCustomerAndNothingOnTheCard() throws Exception {
        Shop s = shop("rr-credit");
        String user = People.customer(accounts, "rr-credit-buyer");
        OrderDto o = delivered(s, user);
        ReturnQuoteDto q = selfReturns.request(user, o.id(), new SelfReturnDto("CHANGED_MIND",
                "", Resolutions.STORE_CREDIT, null, oneMug(o)));
        // The mug's price and its share of the order's tax.
        assertTrue(q.valueCents() >= 1000, "value " + q.valueCents());
        assertEquals(200, q.feeCents());
        long owed = q.valueCents() - q.feeCents();
        String customer = text("SELECT customer_id FROM store_order WHERE id = ?",
                "customer_id", o.id());
        assertTrue(customer.length() > 0);
        long before = credit(customer);
        settle(s, q.returnId());
        assertEquals(before + owed, credit(customer));
        // The order records the return, but the card keeps every cent: the
        // payment row refunds nothing.
        assertEquals(owed, orders.detailOf(o.id()).refundedCents());
        assertEquals(0, count("SELECT COALESCE(SUM(refunded_cents), 0) AS n FROM payment "
                + "WHERE order_id = ?", o.id()));
        assertEquals(Returns.REFUNDED, returns.mine(user, o.id()).get(0).status());
        // A second settle is refused and cannot pay the credit twice.
        ResponseStatusException again = assertThrows(ResponseStatusException.class,
                () -> returns.refund(s.owner, s.storeId, q.returnId(), new ReturnRefundDto(0)));
        assertEquals(409, again.getStatus());
        assertEquals(before + owed, credit(customer));
    }

    @Test
    void aDearerExchangeIsPartPaidAndMailsALinkForTheDifference() throws Exception {
        Shop s = shop("rr-dear");
        String red = variant(s, "Red", 1500);
        String user = People.customer(accounts, "rr-dear-buyer");
        OrderDto o = delivered(s, user);
        ReturnQuoteDto q = selfReturns.request(user, o.id(), new SelfReturnDto("CHANGED_MIND",
                "", Resolutions.EXCHANGE, red, oneMug(o)));
        assertEquals(1500, q.exchangeCents());
        assertEquals(1500 - (q.valueCents() - q.feeCents()), q.differenceCents());
        settle(s, q.returnId());
        String replacement = text("SELECT replacement_order_id FROM return_request WHERE id = ?",
                "replacement_order_id", q.returnId());
        assertTrue(replacement.length() > 0);
        OrderDto r = orders.detailOf(replacement);
        assertEquals(red, r.lines().get(0).variantId());
        assertEquals(user, r.username());
        assertNotEquals(Orders.PAID, r.financial());
        // The goods' value paid toward the replacement, not back to the card.
        assertEquals(0, orders.detailOf(o.id()).refundedCents());
        assertEquals(1, count("SELECT COUNT(*) AS n FROM pay_link WHERE order_id = ?",
                replacement));
    }

    @Test
    void aCheaperExchangeIsPaidInFullAndRefundsTheRest() throws Exception {
        Shop s = shop("rr-cheap");
        String green = variant(s, "Green", 300);
        String user = People.customer(accounts, "rr-cheap-buyer");
        OrderDto o = delivered(s, user);
        ReturnQuoteDto q = selfReturns.request(user, o.id(), new SelfReturnDto("CHANGED_MIND",
                "", Resolutions.EXCHANGE, green, oneMug(o)));
        long owed = q.valueCents() - q.feeCents();
        settle(s, q.returnId());
        String replacement = text("SELECT replacement_order_id FROM return_request WHERE id = ?",
                "replacement_order_id", q.returnId());
        OrderDto r = orders.detailOf(replacement);
        assertEquals(Orders.PAID, r.financial());
        // The replacement took its total out of what the goods were worth
        // after the fee, and the rest went back to the card.
        assertEquals(owed - r.totalCents(), orders.detailOf(o.id()).refundedCents());
        assertEquals(0, count("SELECT COUNT(*) AS n FROM pay_link WHERE order_id = ?",
                replacement));
    }

    private static RefundRequestDto mugs(OrderDto o, int... quantities) {
        List<RefundLineDto> ls = new ArrayList<RefundLineDto>();
        for (int q : quantities) {
            ls.add(new RefundLineDto(o.lines().get(0).id(), q));
        }
        return new RefundRequestDto(ls, 0, true, "");
    }

    @Test
    void aLineNamedTwiceInOneRefundIsRefused() throws Exception {
        Shop s = shop("rr-dup");
        OrderDto o = s.buy(People.customer(accounts, "rr-dup-buyer"), 2);
        // Two mugs sold; 2 + 2 used to pass as two separate "2 of 2 left".
        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> orders.refund(s.owner, s.storeId, o.id(), mugs(o, 2, 2)));
        assertEquals(400, e.getStatus());
        assertEquals(0, orders.detailOf(o.id()).refundedCents());
        assertEquals(0, orders.detailOf(o.id()).lines().get(0).refundedQty());
    }

    @Test
    void refundsRacingForOneOrderNeverPayMoreThanItTook() throws Exception {
        Shop s = shop("rr-race");
        OrderDto o = s.buy(People.customer(accounts, "rr-race-buyer"), 2);
        long total = orders.detailOf(o.id()).totalCents();
        // Four refunds of the whole order at once: together they may pay
        // no more than the order took (a late one is capped at what is left).
        Thread[] runs = new Thread[4];
        final int[] ok = new int[1];
        for (int iter = 0; iter < runs.length; iter++) {
            runs[iter] = new Thread(() -> {
                try {
                    orders.refund(s.owner, s.storeId, o.id(), mugs(o, 2));
                    synchronized (ok) {
                        ok[0]++;
                    }
                } catch (Exception refused) {
                    // 400 or 409: someone else got there first.
                }
            });
            runs[iter].start();
        }
        for (Thread r : runs) {
            r.join();
        }
        assertTrue(ok[0] >= 1);
        assertTrue(orders.detailOf(o.id()).refundedCents() <= total);
        assertTrue(count("SELECT COALESCE(SUM(refunded_cents), 0) AS n FROM payment "
                + "WHERE order_id = ?", o.id()) <= total);
    }

    @Test
    void aGiftCardGetsBackWhatItPaidWhenTheRefundIsLarger() throws Exception {
        Shop s = shop("rr-gift");
        OrderDto o = s.buy(People.customer(accounts, "rr-gift-buyer"), 2);
        GiftCardDto card = giftCards.issue(s.owner, s.storeId, new GiftCardDto(null, null, null,
                2500, 0, true, "", 0, "", 0, "", "", "", 0, 0));
        assertTrue(giftCards.redeem(card.id(), o.id(), 500));
        // No card charge to take the refund: all of it asks the gift card,
        // which paid 500 of the order. It used to get nothing at all.
        db.execute("UPDATE payment SET refunded_cents = amount_cents WHERE order_id = ?",
                new Object[] {o.id()});
        db.execute("UPDATE store_order SET financial = ? WHERE id = ?",
                new Object[] {Orders.PAID, o.id()});
        long[] parts = orders.refundParts(s.owner, s.storeId, o.id(), mugs(o, 1), false,
                StoreAccess.MANAGERS);
        assertEquals(500, parts[2]);
        assertEquals(2500, count("SELECT balance_cents AS n FROM gift_card WHERE id = ?",
                card.id()));
    }

    @Test
    void storeCreditOnAGuestOrderIsRefundedInstead() throws Exception {
        Shop s = shop("rr-guest");
        String user = People.customer(accounts, "rr-guest-buyer");
        OrderDto o = delivered(s, user);
        ReturnQuoteDto q = selfReturns.request(user, o.id(), new SelfReturnDto("CHANGED_MIND",
                "", Resolutions.STORE_CREDIT, null, oneMug(o)));
        db.execute("UPDATE store_order SET customer_id = '' WHERE id = ?", new Object[] {o.id()});
        settle(s, q.returnId());
        long owed = q.valueCents() - q.feeCents();
        // Nobody to credit, so the money goes back to the card.
        assertEquals(owed, count("SELECT COALESCE(SUM(refunded_cents), 0) AS n FROM payment "
                + "WHERE order_id = ?", o.id()));
    }
}
