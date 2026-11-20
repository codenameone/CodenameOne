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
import com.codenameone.examples.bazaar.account.Accounts;
import com.codenameone.examples.bazaar.api.LocationDto;
import com.codenameone.examples.bazaar.domain.Store;
import com.codenameone.examples.bazaar.inventory.Inventory;
import com.codenameone.examples.bazaar.store.Stores;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The stock ledger under the calls checkout, fulfilment and the register
/// make: reservations that never oversell, and levels that always equal the
/// sum of their moves.
@BackendTest
class InventoryTest {
    @Autowired
    private Accounts accounts;
    @Autowired
    private Stores stores;
    @Autowired
    private Inventory inventory;
    @Autowired
    private DataSource db;

    /// A store with one location and one tracked variant, returned as
    /// `{storeId, locationId, variantId}`.
    private String[] shop(String name, boolean backorder) throws Exception {
        String owner = People.customer(accounts, name);
        Store store = stores.create(owner, name, name, "USD");
        LocationDto location = stores.addLocation(owner, store.getId(),
                new LocationDto(null, "Back room", "WAREHOUSE", "", true, true, 0));
        String variantId = Ids.next();
        db.execute("INSERT INTO variant (id, product_id, store_id, title, option1, option2, "
                + "option3, sku, barcode, price_cents, compare_at_cents, cost_cents, weight_grams, "
                + "taxable, track_inventory, allow_backorder, position) VALUES "
                + "(?, ?, ?, 'One', '', '', '', ?, '', 1000, 0, 400, 100, 1, 1, ?, 0)",
                new Object[] {variantId, Ids.next(), store.getId(), name + "-SKU",
                    Integer.valueOf(backorder ? 1 : 0)});
        return new String[] {store.getId(), location.id(), variantId};
    }

    private long level(String variantId, String locationId, String column) throws Exception {
        Map row = db.queryOne("SELECT " + column + " AS n FROM inventory_level WHERE variant_id = ? "
                + "AND location_id = ?", new Object[] {variantId, locationId});
        return ((Number) row.get("n")).longValue();
    }

    private long moved(String variantId, String locationId, String column) throws Exception {
        Map row = db.queryOne("SELECT COALESCE(SUM(" + column + "), 0) AS n FROM inventory_move "
                + "WHERE variant_id = ? AND location_id = ?", new Object[] {variantId, locationId});
        return ((Number) row.get("n")).longValue();
    }

    private void ledgerHolds(String variantId, String locationId) throws Exception {
        assertEquals(level(variantId, locationId, "on_hand"),
                moved(variantId, locationId, "on_hand_delta"));
        assertEquals(level(variantId, locationId, "committed"),
                moved(variantId, locationId, "committed_delta"));
    }

    @Test
    void concurrentReservationsNeverOversell() throws Exception {
        final String[] s = shop("race-shop", false);
        inventory.restock(s[0], s[2], s[1], 5, "opening", "test", Inventory.RESTOCK);
        final int[] won = new int[1];
        final List<Throwable> failures = new ArrayList<Throwable>();
        Thread[] threads = new Thread[20];
        for (int iter = 0; iter < threads.length; iter++) {
            final String reference = "order-" + iter;
            threads[iter] = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        boolean ok = inventory.reserve(s[0], s[2], s[1], 1, reference, "buyer");
                        synchronized (won) {
                            if (ok) {
                                won[0]++;
                            }
                        }
                    } catch (Throwable failed) {
                        synchronized (failures) {
                            failures.add(failed);
                        }
                    }
                }
            });
        }
        for (int iter = 0; iter < threads.length; iter++) {
            threads[iter].start();
        }
        for (int iter = 0; iter < threads.length; iter++) {
            threads[iter].join();
        }
        assertTrue(failures.isEmpty(), "failures: " + failures);
        assertEquals(5, won[0]);
        assertEquals(0, inventory.available(s[2], s[1]));
        assertEquals(5L, level(s[2], s[1], "committed"));
        ledgerHolds(s[2], s[1]);
    }

    @Test
    void theLedgerFollowsEveryChange() throws Exception {
        String[] s = shop("ledger-shop", false);
        inventory.restock(s[0], s[2], s[1], 10, "opening", "test", Inventory.RESTOCK);
        assertTrue(inventory.reserve(s[0], s[2], s[1], 4, "o1", "buyer"));
        assertFalse(inventory.reserve(s[0], s[2], s[1], 7, "o2", "buyer"));
        inventory.ship(s[0], s[2], s[1], 3, "o1", "packer");
        inventory.release(s[0], s[2], s[1], 1, "o1", "packer");
        inventory.restock(s[0], s[2], s[1], 1, "r1", "packer", Inventory.RETURN);
        assertEquals(8, inventory.available(s[2], s[1]));
        // The register sells what it holds, whatever the server thinks.
        inventory.recordPosSale(s[0], s[2], s[1], 12, "pos-1", "cashier");
        assertEquals(-4, inventory.available(s[2], s[1]));
        // A count of 3 against -4 on hand is a COUNT move of 7.
        assertEquals(7, inventory.count(s[0], s[2], s[1], 3, "count", "packer"));
        assertEquals(3, inventory.available(s[2], s[1]));
        assertEquals(s[1], inventory.pickLocation(s[0], s[2], 1));
        ledgerHolds(s[2], s[1]);
    }

    @Test
    void backordersMayGoBelowZero() throws Exception {
        String[] s = shop("backorder-shop", true);
        assertTrue(inventory.reserve(s[0], s[2], s[1], 3, "o1", "buyer"));
        assertEquals(-3, inventory.available(s[2], s[1]));
        ledgerHolds(s[2], s[1]);
    }
}
