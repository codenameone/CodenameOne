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

import com.codename1.appkit.server.push.PushSender;
import com.codename1.appkit.server.push.RecordingPushSender;
import com.codename1.backend.DataSource;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Autowired;
import com.codename1.backend.test.BackendTest;
import com.codename1.backend.test.MockMvc;
import com.codenameone.examples.bazaar.account.Accounts;
import com.codenameone.examples.bazaar.api.BoardColumnDto;
import com.codenameone.examples.bazaar.api.BoardMoveDto;
import com.codenameone.examples.bazaar.api.FulfilLineDto;
import com.codenameone.examples.bazaar.api.FulfillmentDto;
import com.codenameone.examples.bazaar.api.LabelRequestDto;
import com.codenameone.examples.bazaar.api.NewFulfillmentDto;
import com.codenameone.examples.bazaar.api.OrderDto;
import com.codenameone.examples.bazaar.api.PackDto;
import com.codenameone.examples.bazaar.api.RateQuoteDto;
import com.codenameone.examples.bazaar.api.ReturnDto;
import com.codenameone.examples.bazaar.api.ReturnReceiveDto;
import com.codenameone.examples.bazaar.api.ReturnRefundDto;
import com.codenameone.examples.bazaar.api.ReturnRequestDto;
import com.codenameone.examples.bazaar.api.TrackingEventDto;
import com.codenameone.examples.bazaar.api.UnfulfilledDto;
import com.codenameone.examples.bazaar.cart.Carts;
import com.codenameone.examples.bazaar.checkout.Checkouts;
import com.codenameone.examples.bazaar.domain.InventoryLevel;
import com.codenameone.examples.bazaar.domain.OrderLine;
import com.codenameone.examples.bazaar.fulfil.Fulfillments;
import com.codenameone.examples.bazaar.inventory.Inventory;
import com.codenameone.examples.bazaar.order.Orders;
import com.codenameone.examples.bazaar.pricing.DemoPricing;
import com.codenameone.examples.bazaar.repository.OrderLineRepository;
import com.codenameone.examples.bazaar.returns.Returns;
import com.codenameone.examples.bazaar.store.Stores;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.codename1.backend.test.MockMvcRequestBuilders.post;
import static com.codename1.backend.test.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/// Sending orders out: split shipments, stock and payment on shipping,
/// cancelling, the board, simulated tracking and returns.
@BackendTest
class FulfilmentTest {
    @Autowired
    private Accounts accounts;
    @Autowired
    private Stores stores;
    @Autowired
    private Inventory inventory;
    @Autowired
    private DemoPricing pricing;
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
    private OrderLineRepository lines;
    @Autowired
    private PushSender sender;
    @Autowired
    private MockMvc mvc;
    @Autowired
    private DataSource db;

    private Shop shop(String name, int stock) throws Exception {
        return new Shop(name, stock, accounts, stores, pricing, db, inventory, carts, checkouts,
                mvc);
    }

    private String lineOf(OrderDto o) {
        return o.lines() == null ? lineId(o.id()) : o.lines().get(0).id();
    }

    private String lineId(String orderId) {
        List<OrderLine> ls = lines.findByOrderId(orderId);
        return ls.get(0).getId();
    }

    private FulfillmentDto fulfil(Shop s, OrderDto o, int qty, String method) throws Exception {
        List<FulfilLineDto> ls = new ArrayList<FulfilLineDto>();
        ls.add(new FulfilLineDto(lineOf(o), null, null, qty));
        return fulfillments.create(s.owner, s.storeId, o.id(),
                new NewFulfillmentDto(s.locationId, method, ls));
    }

    private FulfillmentDto shipWithLabel(Shop s, String fulfillmentId) throws Exception {
        fulfillments.pack(s.owner, s.storeId, fulfillmentId, new PackDto(0));
        List<RateQuoteDto> rates = fulfillments.rates(s.owner, s.storeId, fulfillmentId);
        assertEquals(3, rates.size());
        FulfillmentDto labelled = fulfillments.buyLabel(s.owner, s.storeId, fulfillmentId,
                new LabelRequestDto(rates.get(0).service()));
        assertTrue(labelled.trackingNumber().startsWith("SIM"), labelled.trackingNumber());
        assertEquals(rates.get(0).cents(), labelled.labelCents());
        return fulfillments.ship(s.owner, s.storeId, fulfillmentId);
    }

    private InventoryLevel level(Shop s) throws Exception {
        return inventory.level(s.storeId, s.variantId, s.locationId);
    }

    private long count(String sql, Object... args) throws Exception {
        Map row = db.queryOne(sql, args);
        return ((Number) row.get("n")).longValue();
    }

    private void expect(int code, Attempt a) throws Exception {
        try {
            a.run();
            fail("expected " + code);
        } catch (ResponseStatusException e) {
            assertEquals(code, e.getStatus(), e.getMessage());
        }
    }

    interface Attempt {
        void run() throws Exception;
    }

    @Test
    void splitShipmentsMoveStockAndCaptureOnce() throws Exception {
        Shop s = shop("split-shop", 5);
        String user = People.customer(accounts, "split-buyer");
        OrderDto o = s.buy(user, 3);
        assertEquals(Orders.AUTHORIZED, o.financial());
        assertEquals(5, level(s).getOnHand());
        assertEquals(3, level(s).getCommitted());

        FulfillmentDto first = fulfil(s, o, 2, "SHIP");
        assertEquals(Fulfillments.PENDING, first.status());
        assertEquals(700, first.weightGrams());
        // Only one unit is left to put in a parcel.
        expect(409, () -> fulfil(s, o, 2, "SHIP"));
        FulfillmentDto second = fulfil(s, o, 1, "SHIP");
        List<UnfulfilledDto> queue = fulfillments.queue(s.owner, s.storeId, null);
        for (UnfulfilledDto u : queue) {
            assertFalse(u.orderId().equals(o.id()), "everything is assigned");
        }
        // No label, no shipping.
        expect(409, () -> fulfillments.ship(s.owner, s.storeId, first.id()));

        FulfillmentDto shipped = shipWithLabel(s, first.id());
        assertEquals(Fulfillments.SHIPPED, shipped.status());
        OrderDto now = orders.detailOf(o.id());
        assertEquals(Orders.PARTIAL, now.fulfilment());
        assertEquals(Orders.PAID, now.financial());
        assertEquals(3, level(s).getOnHand());
        assertEquals(1, level(s).getCommitted());
        // Shipping twice is refused.
        expect(409, () -> fulfillments.ship(s.owner, s.storeId, first.id()));

        shipWithLabel(s, second.id());
        now = orders.detailOf(o.id());
        assertEquals(Orders.FULFILLED, now.fulfilment());
        assertEquals(2, level(s).getOnHand());
        assertEquals(0, level(s).getCommitted());
        assertEquals(1, count("SELECT COUNT(*) AS n FROM order_event WHERE order_id = ? "
                + "AND kind = 'CAPTURED'", o.id()), "the card is charged once");
        assertEquals(2, count("SELECT COUNT(*) AS n FROM inventory_move WHERE variant_id = ? "
                + "AND reason = 'SALE'", s.variantId));
    }

    @Test
    void cancellingAFulfilmentPutsItsUnitsBackInTheQueue() throws Exception {
        Shop s = shop("cancel-ful-shop", 5);
        String user = People.customer(accounts, "cancel-ful-buyer");
        OrderDto o = s.buy(user, 2);
        FulfillmentDto f = fulfil(s, o, 2, "SHIP");
        fulfillments.pack(s.owner, s.storeId, f.id(), new PackDto(900));
        assertEquals(900, fulfillments.detail(f.id()).weightGrams());
        FulfillmentDto cancelled = fulfillments.cancel(s.owner, s.storeId, f.id());
        assertEquals(Fulfillments.CANCELLED, cancelled.status());
        expect(409, () -> fulfillments.ship(s.owner, s.storeId, f.id()));
        expect(409, () -> fulfillments.cancel(s.owner, s.storeId, f.id()));
        int open = 0;
        for (UnfulfilledDto u : fulfillments.queue(s.owner, s.storeId, s.locationId)) {
            if (u.orderId().equals(o.id())) {
                open = u.lines().get(0).open();
            }
        }
        assertEquals(2, open);
        assertEquals(5, level(s).getOnHand());
        assertEquals(2, level(s).getCommitted(), "still promised to the order");
        // Someone who is not staff of the store sees nothing.
        String stranger = People.customer(accounts, "cancel-ful-stranger");
        expect(404, () -> fulfillments.queue(stranger, s.storeId, null));
    }

    @Test
    void boardMovesDoWhatTheColumnMeans() throws Exception {
        Shop s = shop("board-shop", 5);
        String user = People.customer(accounts, "board-buyer");
        OrderDto o = s.buy(user, 1);
        assertEquals(Fulfillments.COLUMN_TO_PACK, columnOf(fulfillments.board(s.owner,
                s.storeId), o.id()));
        expect(409, () -> fulfillments.moveCard(s.owner, s.storeId,
                new BoardMoveDto(o.id(), Fulfillments.COLUMN_SHIPPED)));
        List<BoardColumnDto> b = fulfillments.moveCard(s.owner, s.storeId,
                new BoardMoveDto(o.id(), Fulfillments.COLUMN_PACKED));
        assertEquals(Fulfillments.COLUMN_PACKED, columnOf(b, o.id()));
        expect(409, () -> fulfillments.moveCard(s.owner, s.storeId,
                new BoardMoveDto(o.id(), Fulfillments.COLUMN_TO_PACK)));
        b = fulfillments.moveCard(s.owner, s.storeId,
                new BoardMoveDto(o.id(), Fulfillments.COLUMN_SHIPPED));
        assertEquals(Fulfillments.COLUMN_SHIPPED, columnOf(b, o.id()));
        OrderDto now = orders.detailOf(o.id());
        assertEquals(Orders.FULFILLED, now.fulfilment());
        assertEquals(Orders.PAID, now.financial());
        assertTrue(now.fulfillments().get(0).trackingNumber().startsWith("SIM"),
                "the cheapest label was bought");
        b = fulfillments.moveCard(s.owner, s.storeId,
                new BoardMoveDto(o.id(), Fulfillments.COLUMN_DELIVERED));
        assertEquals(Fulfillments.COLUMN_DELIVERED, columnOf(b, o.id()));
        assertEquals(4, b.size());
        assertEquals("To pack", b.get(0).title());
    }

    private static String columnOf(List<BoardColumnDto> board, String orderId) {
        for (BoardColumnDto c : board) {
            for (int iter = 0; iter < c.cards().size(); iter++) {
                if (c.cards().get(iter).orderId().equals(orderId)) {
                    return c.id();
                }
            }
        }
        return null;
    }

    @Test
    void simulatedTrackingWalksTheParcelToTheDoor() throws Exception {
        Shop s = shop("track-shop", 5);
        String user = People.customer(accounts, "track-buyer");
        String token = "track-token-" + Ids.next();
        mvc.perform(People.asCustomer(Rest.body(post("/api/account/push"),
                "{\"provider\":\"fcm\",\"token\":\"" + token + "\",\"platform\":\"and\"}"), user))
                .andExpect(status().isOk());
        OrderDto o = s.buy(user, 1);
        FulfillmentDto f = shipWithLabel(s, fulfil(s, o, 1, "SHIP").id());
        RecordingPushSender sent = (RecordingPushSender) sender;
        assertTrue(titles(sent, token).contains("Your order has shipped"), titles(sent, token));

        long now = System.currentTimeMillis();
        fulfillments.advance(now + 1000);
        assertEquals(Fulfillments.SHIPPED, fulfillments.detail(f.id()).status());
        fulfillments.advance(now + 160000);
        assertEquals(Fulfillments.IN_TRANSIT, fulfillments.detail(f.id()).status());
        fulfillments.advance(now + 250000);
        assertEquals(Fulfillments.OUT_FOR_DELIVERY, fulfillments.detail(f.id()).status());
        fulfillments.advance(now + 3600000);
        FulfillmentDto done = fulfillments.detail(f.id());
        assertEquals(Fulfillments.DELIVERED, done.status());
        fulfillments.advance(now + 7200000);
        List<String> kinds = new ArrayList<String>();
        for (TrackingEventDto e : fulfillments.detail(f.id()).events()) {
            kinds.add(e.status());
        }
        assertEquals("[LABEL_CREATED, SHIPPED, IN_TRANSIT, IN_TRANSIT, OUT_FOR_DELIVERY, "
                + "DELIVERED]", kinds.toString(), "each scan once, in order");
        assertEquals("Albany", done.events().get(done.events().size() - 1).place());
        assertTrue(titles(sent, token).contains("Out for delivery"), titles(sent, token));
        assertTrue(titles(sent, token).contains("Delivered"), titles(sent, token));

        // The shopper sees it; another shopper does not; anyone with the number
        // sees the scans without the order.
        assertEquals(1, fulfillments.tracking(user, o.id()).size());
        String other = People.customer(accounts, "track-other");
        expect(404, () -> fulfillments.tracking(other, o.id()));
        FulfillmentDto pub = fulfillments.track(f.trackingNumber());
        assertEquals("", pub.orderId());
        assertEquals(Fulfillments.DELIVERED, pub.status());
        assertEquals(null, pub.lines());
        Map page = Rest.json(mvc, com.codename1.backend.test.MockMvcRequestBuilders.get(
                "/api/shop/tracking/" + f.trackingNumber()));
        assertEquals("DELIVERED", page.get("status"));
    }

    private static String titles(RecordingPushSender sent, String token) {
        StringBuilder out = new StringBuilder();
        for (RecordingPushSender.Sent each : sent.sentTo(token)) {
            out.append(each.message().get("title")).append('|');
        }
        return out.toString();
    }

    @Test
    void pickupIsHandedOverAndCharged() throws Exception {
        Shop s = shop("pickup-shop", 5);
        String user = People.customer(accounts, "pickup-buyer");
        OrderDto o = s.buy(user, 1);
        FulfillmentDto f = fulfil(s, o, 1, "PICKUP");
        assertEquals("PICKUP", f.method());
        expect(409, () -> fulfillments.ship(s.owner, s.storeId, f.id()));
        assertEquals(Fulfillments.PACKED, fulfillments.pickupReady(s.owner, s.storeId,
                f.id()).status());
        assertEquals(Fulfillments.PICKED_UP, fulfillments.pickedUp(s.owner, s.storeId,
                f.id()).status());
        OrderDto now = orders.detailOf(o.id());
        assertEquals(Orders.FULFILLED, now.fulfilment());
        assertEquals(Orders.PAID, now.financial());
        assertEquals(4, level(s).getOnHand());
    }

    @Test
    void returnIsRefundedAndRestocked() throws Exception {
        Shop s = shop("return-shop", 5);
        String user = People.customer(accounts, "return-buyer");
        OrderDto o = s.buy(user, 2);
        String line = lineOf(o);
        List<FulfilLineDto> one = new ArrayList<FulfilLineDto>();
        one.add(new FulfilLineDto(line, null, null, 1));
        // Nothing arrived yet.
        expect(409, () -> returns.request(user, o.id(), new ReturnRequestDto("Too big", "",
                one)));
        FulfillmentDto f = fulfil(s, o, 2, "LOCAL");
        fulfillments.ship(s.owner, s.storeId, f.id());
        assertEquals(Fulfillments.DELIVERED, fulfillments.delivered(s.owner, s.storeId,
                f.id()).status());
        assertEquals(3, level(s).getOnHand());

        List<FulfilLineDto> three = new ArrayList<FulfilLineDto>();
        three.add(new FulfilLineDto(line, null, null, 3));
        expect(409, () -> returns.request(user, o.id(), new ReturnRequestDto("", "", three)));
        String other = People.customer(accounts, "return-other");
        expect(404, () -> returns.request(other, o.id(), new ReturnRequestDto("", "", one)));

        ReturnDto r = returns.request(user, o.id(), new ReturnRequestDto("Too big", "Sorry",
                one));
        assertEquals(Returns.REQUESTED, r.status());
        expect(409, () -> returns.receive(s.owner, s.storeId, r.id(), new ReturnReceiveDto(true)));
        assertEquals(Returns.APPROVED, returns.approve(s.owner, s.storeId, r.id()).status());
        assertEquals(Returns.RECEIVED, returns.receive(s.owner, s.storeId, r.id(),
                new ReturnReceiveDto(true)).status());
        assertEquals(4, level(s).getOnHand(), "restocked");
        assertEquals(1, count("SELECT COUNT(*) AS n FROM inventory_move WHERE variant_id = ? "
                + "AND reason = 'RETURN'", s.variantId));

        ReturnDto refunded = returns.refund(s.owner, s.storeId, r.id(), new ReturnRefundDto(0));
        assertEquals(Returns.REFUNDED, refunded.status());
        OrderDto now = orders.detailOf(o.id());
        // One $10.00 mug plus its 4% New York tax.
        assertEquals(1040, now.refundedCents());
        assertEquals(1040, refunded.refundedCents());
        assertEquals(Orders.PARTIALLY_REFUNDED, now.financial());
        assertEquals(4, level(s).getOnHand(), "the refund does not restock twice");
        expect(409, () -> returns.refund(s.owner, s.storeId, r.id(), new ReturnRefundDto(0)));
        assertEquals(1, returns.list(s.owner, s.storeId).size());
        assertEquals(1, returns.mine(user, o.id()).size());
        // The other unit can still come back; the refunded one cannot again.
        expect(409, () -> returns.request(user, o.id(), new ReturnRequestDto("", "",
                twoOf(line))));
        assertNotNull(returns.request(user, o.id(), new ReturnRequestDto("", "", one)));
    }

    private static List<FulfilLineDto> twoOf(String line) {
        List<FulfilLineDto> out = new ArrayList<FulfilLineDto>();
        out.add(new FulfilLineDto(line, null, null, 2));
        return out;
    }
}
