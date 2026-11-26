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
import com.codename1.backend.annotations.Autowired;
import com.codename1.backend.test.BackendTest;
import com.codename1.backend.test.MockMvc;
import com.codenameone.examples.bazaar.account.Accounts;
import com.codenameone.examples.bazaar.api.LocationDto;
import com.codenameone.examples.bazaar.store.Stores;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static com.codename1.backend.test.MockMvcRequestBuilders.get;
import static com.codename1.backend.test.MockMvcRequestBuilders.post;
import static org.junit.jupiter.api.Assertions.assertEquals;

/// The stock screens: adjustments, counts, the low-stock list, transfers and
/// purchase orders received in parts -- and the ledger adding up after all of
/// it.
@BackendTest
class StockTest {
    @Autowired
    private MockMvc mvc;
    @Autowired
    private Accounts accounts;
    @Autowired
    private Stores stores;
    @Autowired
    private DataSource db;

    private Map level(Shops shop, String variantId, String locationId) throws Exception {
        List levels = Rest.list(mvc, People.asMerchant(get(shop.base() + "/inventory?product="),
                shop.owner));
        for (int iter = 0; iter < levels.size(); iter++) {
            Map level = (Map) levels.get(iter);
            if (variantId.equals(level.get("variantId"))
                    && locationId.equals(level.get("locationId"))) {
                return level;
            }
        }
        throw new AssertionError("no level");
    }

    private Map send(Shops shop, String path, String json) throws Exception {
        return Rest.json(mvc, People.asMerchant(Rest.body(post(shop.base() + path), json),
                shop.owner));
    }

    private void ledgerHolds(String storeId) throws Exception {
        Map bad = db.queryOne("SELECT COUNT(*) AS n FROM inventory_level l WHERE l.store_id = ? "
                + "AND (l.on_hand <> (SELECT COALESCE(SUM(m.on_hand_delta), 0) FROM inventory_move m "
                + "WHERE m.variant_id = l.variant_id AND m.location_id = l.location_id) "
                + "OR l.committed <> (SELECT COALESCE(SUM(m.committed_delta), 0) FROM "
                + "inventory_move m WHERE m.variant_id = l.variant_id AND m.location_id = "
                + "l.location_id))", new Object[] {storeId});
        assertEquals(0L, Rows.number(bad.get("n")));
    }

    @Test
    void adjustmentsCountsAndTheLowStockList() throws Exception {
        Shops shop = Shops.open(accounts, stores, "count-shop");
        Map product = shop.product(mvc, "Count Candle", "ACTIVE", "Candles", "C", "", 900);
        String v = Shops.variantId(product, 0);
        String at = "\"variantId\":\"" + v + "\",\"locationId\":\"" + shop.locationId + "\"";
        Map level = send(shop, "/inventory/adjust", "{" + at + ",\"delta\":10,"
                + "\"reason\":\"RESTOCK\"}");
        assertEquals(10L, Rest.number(level, "onHand"));
        send(shop, "/inventory/adjust", "{" + at + ",\"delta\":-2,\"reason\":\"DAMAGE\"}");
        level = send(shop, "/inventory/count", "{" + at + ",\"quantity\":5}");
        assertEquals(5L, Rest.number(level, "available"));
        List moves = Rest.list(mvc, People.asMerchant(get(shop.base() + "/inventory/moves?variant="
                + v), shop.owner));
        assertEquals(3, moves.size());
        assertEquals("COUNT", ((Map) moves.get(0)).get("reason"));
        assertEquals(-3L, Rest.number((Map) moves.get(0), "onHandDelta"));

        send(shop, "/inventory/reorder-point", "{" + at + ",\"quantity\":5}");
        List low = Rest.list(mvc, People.asMerchant(get(shop.base() + "/inventory/low"),
                shop.owner));
        assertEquals(1, low.size());
        send(shop, "/inventory/adjust", "{" + at + ",\"delta\":1,\"reason\":\"CORRECTION\"}");
        low = Rest.list(mvc, People.asMerchant(get(shop.base() + "/inventory/low"), shop.owner));
        assertEquals(0, low.size());
        ledgerHolds(shop.storeId);
    }

    @Test
    void transfersMoveStockAndArriveInParts() throws Exception {
        Shops shop = Shops.open(accounts, stores, "transfer-shop");
        LocationDto annex = stores.addLocation(shop.owner, shop.storeId,
                new LocationDto(null, "Annex", "RETAIL", "", false, true, 0));
        Map product = shop.product(mvc, "Moving Box", "ACTIVE", "Boxes", "B", "", 300);
        String v = Shops.variantId(product, 0);
        send(shop, "/inventory/adjust", "{\"variantId\":\"" + v + "\",\"locationId\":\""
                + shop.locationId + "\",\"delta\":8,\"reason\":\"RESTOCK\"}");
        String lines = "\"lines\":[{\"variantId\":\"" + v + "\",\"quantity\":6}]";
        Map transfer = send(shop, "/transfers", "{\"fromLocationId\":\"" + shop.locationId
                + "\",\"toLocationId\":\"" + annex.id() + "\"," + lines + "}");
        assertEquals("DRAFT", transfer.get("status"));
        String path = "/transfers/" + transfer.get("id");
        transfer = send(shop, path + "/send", "{}");
        assertEquals("IN_TRANSIT", transfer.get("status"));
        assertEquals(2L, Rest.number(level(shop, v, shop.locationId), "onHand"));
        assertEquals(6L, Rest.number(level(shop, v, annex.id()), "incoming"));
        String lineId = (String) ((Map) ((List) transfer.get("lines")).get(0)).get("id");
        transfer = send(shop, path + "/receive", "{\"lines\":[{\"lineId\":\"" + lineId
                + "\",\"quantity\":4}]}");
        assertEquals("PARTIAL", transfer.get("status"));
        Map annexLevel = level(shop, v, annex.id());
        assertEquals(4L, Rest.number(annexLevel, "onHand"));
        assertEquals(2L, Rest.number(annexLevel, "incoming"));
        // More than is outstanding is refused.
        assertEquals(400, Rest.statusOf(mvc, People.asMerchant(Rest.body(post(shop.base() + path
                + "/receive"), "{\"lines\":[{\"lineId\":\"" + lineId + "\",\"quantity\":3}]}"),
                shop.owner)));
        // Cancelled, the two that never came go back where they left.
        transfer = send(shop, path + "/cancel", "{}");
        assertEquals("CANCELLED", transfer.get("status"));
        assertEquals(4L, Rest.number(level(shop, v, shop.locationId), "onHand"));
        assertEquals(0L, Rest.number(level(shop, v, annex.id()), "incoming"));
        // Sending more than is there is refused.
        Map big = send(shop, "/transfers", "{\"fromLocationId\":\"" + shop.locationId
                + "\",\"toLocationId\":\"" + annex.id() + "\",\"lines\":[{\"variantId\":\"" + v
                + "\",\"quantity\":50}]}");
        assertEquals(409, Rest.statusOf(mvc, People.asMerchant(Rest.body(post(shop.base()
                + "/transfers/" + big.get("id") + "/send"), "{}"), shop.owner)));
        ledgerHolds(shop.storeId);
    }

    @Test
    void purchaseOrdersAreReceivedInParts() throws Exception {
        Shops shop = Shops.open(accounts, stores, "po-shop");
        Map product = shop.product(mvc, "Ordered Oars", "ACTIVE", "Oars", "O", "", 5000, 6000);
        String v0 = Shops.variantId(product, 0);
        String v1 = Shops.variantId(product, 1);
        Map supplier = send(shop, "/suppliers", "{\"name\":\"Oar Works\",\"email\":"
                + "\"sales@oars.example\"}");
        Map order = send(shop, "/purchase-orders", "{\"supplierId\":\"" + supplier.get("id")
                + "\",\"locationId\":\"" + shop.locationId + "\",\"lines\":[{\"variantId\":\""
                + v0 + "\",\"quantity\":10,\"costCents\":2000},{\"variantId\":\"" + v1
                + "\",\"quantity\":4,\"costCents\":2500}]}");
        assertEquals(30000L, Rest.number(order, "totalCents"));
        assertEquals("Oar Works", order.get("supplierName"));
        String path = "/purchase-orders/" + order.get("id");
        order = send(shop, path + "/order", "{}");
        assertEquals("ORDERED", order.get("status"));
        assertEquals(10L, Rest.number(level(shop, v0, shop.locationId), "incoming"));
        // A supplier with an open order stays.
        assertEquals(409, Rest.statusOf(mvc, People.asMerchant(
                com.codename1.backend.test.MockMvcRequestBuilders.delete(shop.base()
                + "/suppliers/" + supplier.get("id")), shop.owner)));

        List lines = (List) order.get("lines");
        // Lines come back in no promised order; find each by its variant.
        boolean firstIsV0 = v0.equals(((Map) lines.get(0)).get("variantId"));
        String l0 = (String) ((Map) lines.get(firstIsV0 ? 0 : 1)).get("id");
        String l1 = (String) ((Map) lines.get(firstIsV0 ? 1 : 0)).get("id");
        order = send(shop, path + "/receive", "{\"lines\":[{\"lineId\":\"" + l0
                + "\",\"quantity\":6}]}");
        assertEquals("PARTIAL", order.get("status"));
        Map first = level(shop, v0, shop.locationId);
        assertEquals(6L, Rest.number(first, "onHand"));
        assertEquals(4L, Rest.number(first, "incoming"));
        order = send(shop, path + "/receive", "{\"lines\":[{\"lineId\":\"" + l0
                + "\",\"quantity\":4},{\"lineId\":\"" + l1 + "\",\"quantity\":4}]}");
        assertEquals("RECEIVED", order.get("status"));
        assertEquals(10L, Rest.number(level(shop, v0, shop.locationId), "onHand"));
        assertEquals(0L, Rest.number(level(shop, v1, shop.locationId), "incoming"));
        List moves = Rest.list(mvc, People.asMerchant(get(shop.base() + "/inventory/moves"),
                shop.owner));
        assertEquals("RECEIVE", ((Map) moves.get(0)).get("reason"));
        assertEquals(409, Rest.statusOf(mvc, People.asMerchant(Rest.body(post(shop.base() + path
                + "/cancel"), "{}"), shop.owner)));
        ledgerHolds(shop.storeId);
    }
}
