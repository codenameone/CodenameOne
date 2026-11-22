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
import com.codenameone.examples.bazaar.api.InventoryStateDto;
import com.codenameone.examples.bazaar.api.LotDto;
import com.codenameone.examples.bazaar.api.OrderDto;
import com.codenameone.examples.bazaar.api.SerialAssignDto;
import com.codenameone.examples.bazaar.api.SerialDto;
import com.codenameone.examples.bazaar.api.TrackedReceiptDto;
import com.codenameone.examples.bazaar.api.TrackingDto;
import com.codenameone.examples.bazaar.api.UnavailableDto;
import com.codenameone.examples.bazaar.cart.Carts;
import com.codenameone.examples.bazaar.checkout.Checkouts;
import com.codenameone.examples.bazaar.inventory.Inventory;
import com.codenameone.examples.bazaar.inventory.InventoryStates;
import com.codenameone.examples.bazaar.inventory.Tracking;
import com.codenameone.examples.bazaar.pricing.DemoPricing;
import com.codenameone.examples.bazaar.store.Stores;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Selling past zero, the unavailable buckets, and serial and lot tracking.
@BackendTest
class InventoryStatesTest {
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
    private MockMvc mvc;
    @Autowired
    private DataSource db;
    @Autowired
    private InventoryStates states;
    @Autowired
    private Tracking tracking;

    private Shop shop(String name, int stock) throws Exception {
        return new Shop(name, stock, accounts, stores, pricing, db, inventory, carts, checkouts,
                mvc);
    }

    private static int refused(Executable call) {
        return assertThrows(ResponseStatusException.class, call).getStatus();
    }

    @Test
    void anOrderPastZeroIsMarkedBackordered() throws Exception {
        Shop s = shop("oversell-shop", 1);
        db.execute("UPDATE variant SET allow_backorder = 1 WHERE id = ?",
                new Object[] {s.variantId});
        OrderDto o = s.buy(People.customer(accounts, "oversell-buyer"), 3);
        Map line = db.queryOne("SELECT backordered_qty FROM order_line WHERE order_id = ?",
                new Object[] {o.id()});
        assertEquals(2L, Rows.number(line.get("backordered_qty")));
        InventoryStateDto st = states.state(s.storeId, s.variantId, s.locationId);
        assertEquals(-2, st.available());
        assertTrue(st.continueSelling());
    }

    @Test
    void anInStockOrderIsNotBackordered() throws Exception {
        Shop s = shop("instock-shop", 5);
        OrderDto o = s.buy(People.customer(accounts, "instock-buyer"), 2);
        Map line = db.queryOne("SELECT backordered_qty FROM order_line WHERE order_id = ?",
                new Object[] {o.id()});
        assertEquals(0L, Rows.number(line.get("backordered_qty")));
    }

    @Test
    void bucketsTakeOnlyAvailableUnits() throws Exception {
        Shop s = shop("bucket-shop", 10);
        assertTrue(inventory.reserve(s.storeId, s.variantId, s.locationId, 4, "o1", "buyer"));
        InventoryStateDto st = states.move(s.owner, s.storeId, new UnavailableDto(s.variantId,
                s.locationId, InventoryStates.DAMAGED, 2));
        assertEquals(2, st.damaged());
        assertEquals(10, st.onHand());
        assertEquals(4, st.available());
        assertEquals(4, st.committed());
        assertEquals(409, refused(() -> states.move(s.owner, s.storeId,
                new UnavailableDto(s.variantId, s.locationId, InventoryStates.SAFETY_STOCK, 5))));
        st = states.move(s.owner, s.storeId, new UnavailableDto(s.variantId, s.locationId,
                InventoryStates.DAMAGED, -1));
        assertEquals(1, st.damaged());
        assertEquals(5, st.available());
        assertEquals(409, refused(() -> states.move(s.owner, s.storeId,
                new UnavailableDto(s.variantId, s.locationId, InventoryStates.DAMAGED, -2))));
        assertEquals(400, refused(() -> states.move(s.owner, s.storeId,
                new UnavailableDto(s.variantId, s.locationId, "LOST", 1))));
        Shop other = shop("bucket-other", 1);
        assertEquals(404, refused(() -> states.move(other.owner, other.storeId,
                new UnavailableDto(s.variantId, s.locationId, InventoryStates.DAMAGED, 1))));
    }

    @Test
    void serialsAreReceivedSoldAndFound() throws Exception {
        Shop s = shop("serial-shop", 5);
        tracking.setMode(s.owner, s.storeId, new TrackingDto(s.variantId, "", "", "",
                Tracking.SERIAL));
        assertEquals(400, refused(() -> tracking.receive(s.owner, s.storeId,
                new TrackedReceiptDto(s.variantId, s.locationId, "PO-1",
                        Arrays.asList("A1", "A2"), "", 0, 3))));
        List<SerialDto> got = tracking.receive(s.owner, s.storeId, new TrackedReceiptDto(
                s.variantId, s.locationId, "PO-1", Arrays.asList("A1", "A2", "A3"), "", 0, 3));
        assertEquals(3, got.size());
        assertEquals(409, refused(() -> tracking.receive(s.owner, s.storeId,
                new TrackedReceiptDto(s.variantId, s.locationId, "PO-2",
                        Collections.singletonList("A1"), "", 0, 1))));
        // Switching away while units are in stock would orphan them.
        assertEquals(409, refused(() -> tracking.setMode(s.owner, s.storeId,
                new TrackingDto(s.variantId, "", "", "", Tracking.NONE))));
        OrderDto o = s.buy(People.customer(accounts, "serial-buyer"), 2);
        String lineId = Rows.text(db.queryOne("SELECT id FROM order_line WHERE order_id = ?",
                new Object[] {o.id()}).get("id"));
        tracking.assign(s.owner, s.storeId, o.id(), new SerialAssignDto(lineId,
                Arrays.asList("A2", "A3")));
        assertEquals(409, refused(() -> tracking.assign(s.owner, s.storeId, o.id(),
                new SerialAssignDto(lineId, Collections.singletonList("A1")))));
        List<SerialDto> found = tracking.find(s.owner, s.storeId, "A3");
        assertEquals(1, found.size());
        assertEquals(o.id(), found.get(0).orderId());
        assertEquals(o.number(), found.get(0).orderNumber());
        assertEquals(Tracking.SOLD, found.get(0).status());
        assertEquals(3, tracking.find(s.owner, s.storeId, "A").size());
        Shop other = shop("serial-other", 1);
        assertEquals(404, refused(() -> tracking.find(other.owner, s.storeId, "A3")));
    }

    @Test
    void lotsShipSoonestExpiringFirst() throws Exception {
        Shop s = shop("lot-shop", 20);
        tracking.setMode(s.owner, s.storeId, new TrackingDto(s.variantId, "", "", "",
                Tracking.LOT));
        long day = 86400000L;
        long now = System.currentTimeMillis();
        tracking.receive(s.owner, s.storeId, new TrackedReceiptDto(s.variantId, s.locationId,
                "PO-1", null, "LATE", now + 90 * day, 5));
        tracking.receive(s.owner, s.storeId, new TrackedReceiptDto(s.variantId, s.locationId,
                "PO-1", null, "NOEXP", 0, 5));
        tracking.receive(s.owner, s.storeId, new TrackedReceiptDto(s.variantId, s.locationId,
                "PO-1", null, "SOON", now + 10 * day, 3));
        List<LotDto> lots = tracking.lots(s.owner, s.storeId, s.variantId);
        assertEquals("SOON", lots.get(0).lotCode());
        assertEquals("LATE", lots.get(1).lotCode());
        assertEquals("NOEXP", lots.get(2).lotCode());
        inventory.recordPosSale(s.storeId, s.variantId, s.locationId, 4, "pos-order", "cashier");
        lots = tracking.lots(s.owner, s.storeId, s.variantId);
        assertEquals(0, lots.get(0).remaining());
        assertEquals(4, lots.get(1).remaining());
        assertEquals(5, lots.get(2).remaining());
        assertEquals(Arrays.asList("SOON x 3", "LATE x 1"), tracking.picks("pos-order"));
    }
}
