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
package com.codenameone.examples.savor;

import com.codename1.backend.Base64;
import com.codename1.backend.DataSource;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Autowired;
import com.codename1.backend.test.BackendTest;
import com.codename1.backend.test.MockMvc;
import com.codenameone.examples.savor.account.Accounts;
import com.codenameone.examples.savor.account.DemoAccounts;
import com.codenameone.examples.savor.api.DeliveryDto;
import com.codenameone.examples.savor.api.PhotoDto;
import com.codenameone.examples.savor.catalog.Catalog;
import com.codenameone.examples.savor.catalog.Restaurants;
import com.codenameone.examples.savor.order.Orders;
import com.codenameone.examples.savor.order.Quotes;
import org.junit.jupiter.api.Test;

import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/// What a merchant may store, see and leave behind.
@BackendTest
class MerchantGuardsTest {
    @Autowired
    private MockMvc mvc;
    @Autowired
    private Accounts accounts;
    @Autowired
    private Restaurants restaurants;
    @Autowired
    private Orders orders;
    @Autowired
    private Catalog catalog;
    @Autowired
    private DataSource db;

    /// A PNG's signature and header chunk declaring `width` by `height`.
    private static String png(int width, int height) {
        byte[] bytes = new byte[33];
        byte[] start = {(byte) 0x89, 'P', 'N', 'G', 13, 10, 26, 10, 0, 0, 0, 13, 'I', 'H', 'D', 'R'};
        System.arraycopy(start, 0, bytes, 0, start.length);
        int[] sides = {width, height};
        for (int side = 0; side < 2; side++) {
            for (int b = 0; b < 4; b++) {
                bytes[16 + side * 4 + b] = (byte) (sides[side] >>> (24 - b * 8));
            }
        }
        return Base64.encode(bytes);
    }

    private static int statusOf(ThrowingCall call) throws Exception {
        try {
            call.run();
        } catch (ResponseStatusException refused) {
            return refused.getStatus();
        }
        return 200;
    }

    private interface ThrowingCall {
        void run() throws Exception;
    }

    @Test
    void photosAreCheckedAndPruned() throws Exception {
        Trips.Kitchen kitchen = Trips.kitchen(mvc, -41.2865, 174.7762);
        assertEquals(413, statusOf(() -> restaurants.upload(kitchen.merchant,
                new PhotoDto("", "image/png", png(100000, 100000)))));
        String old = restaurants.upload(kitchen.merchant, new PhotoDto("", "image/png", png(8, 8))).id();
        db.execute("UPDATE photo SET created_at = ? WHERE id = ?", new Object[] {1000L, old});
        String fresh = restaurants.upload(kitchen.merchant,
                new PhotoDto("", "image/png", png(8, 8))).id();
        // Named by nothing and past its grace: gone. The new one stays.
        assertEquals(404, statusOf(() -> catalog.photo(old)));
        assertEquals(fresh, catalog.photo(fresh).id());
    }

    @Test
    void theOnlyMerchantCannotCloseTheAccount() throws Exception {
        Trips.Kitchen kitchen = Trips.kitchen(mvc, -41.2965, 174.7862);
        assertEquals(409, statusOf(() -> accounts.close(kitchen.merchant, People.PASSWORD)));
    }

    @Test
    void aMerchantIsNotShownTheCustomersSignIn() throws Exception {
        String customer = People.customer(accounts, "wlg-guard-customer");
        String courier = People.courier(accounts, "wlg-guard-courier");
        Trips.Trip trip = Trips.trip(-41.3065, 174.7962, -41.3000, 174.8000);
        Trips.online(mvc, courier, trip.fromLat, trip.fromLng);
        String id = (String) Trips.complete(mvc, customer, courier, trip).get("id");
        Trips.Kitchen kitchen = Trips.kitchen(mvc, trip.fromLat, trip.fromLng);
        DeliveryDto merchant = orders.get(kitchen.merchant, false, id).delivery();
        if (merchant == null) {
            fail("The order has no delivery");
        }
        assertNull(merchant.customerUsername());
        assertEquals(customer, orders.get(DemoAccounts.ADMIN, true, id).delivery().customerUsername());
    }

    @Test
    void aRestaurantWithNoStaffLeftIsNotListedAgain() throws Exception {
        Trips.Kitchen kitchen = Trips.kitchen(mvc, -41.3165, 174.8062);
        restaurants.suspend(DemoAccounts.ADMIN, kitchen.id, true, "Closing down");
        // What closing the only merchant's account leaves behind.
        db.execute("DELETE FROM restaurant_staff WHERE restaurant_id = ?", new Object[] {kitchen.id});
        assertEquals(409, statusOf(() -> restaurants.suspend(DemoAccounts.ADMIN, kitchen.id, false, "")));
    }

    @Test
    void refundedOrdersAreNotCountedAsSales() throws Exception {
        String customer = People.customer(accounts, "wlg-sales-customer");
        String courier = People.courier(accounts, "wlg-sales-courier");
        Trips.Trip trip = Trips.trip(-41.3265, 174.8162, -41.3200, 174.8200);
        Trips.online(mvc, courier, trip.fromLat, trip.fromLng);
        String id = (String) Trips.complete(mvc, customer, courier, trip).get("id");
        Trips.Kitchen kitchen = Trips.kitchen(mvc, trip.fromLat, trip.fromLng);
        long before = restaurants.summary(kitchen.merchant).salesTodayCents();
        assertTrue(before > 0L, "the paid order counts");
        db.execute("UPDATE customer_order SET payment_status = ? WHERE id = ?", new Object[] {"REFUNDED", id});
        assertEquals(0L, restaurants.summary(kitchen.merchant).salesTodayCents());
    }

    @Test
    void aFavoriteOutOfRangeIsNotListed() throws Exception {
        String customer = People.customer(accounts, "wlg-far-customer");
        Trips.Kitchen kitchen = Trips.kitchen(mvc, -41.3365, 174.8262);
        catalog.favorite(customer, kitchen.id, true);
        assertEquals(1, catalog.favorites(customer, kitchen.lat, kitchen.lng).size());
        // A degree of latitude is over 100 km: far past where anything delivers.
        assertEquals(0, catalog.favorites(customer, kitchen.lat + 1d, kitchen.lng).size());
    }

    @Test
    void openingHoursFollowTheConfiguredZone() {
        assertEquals(0, Quotes.minuteOfDay(0L, TimeZone.getTimeZone("UTC")));
        assertEquals(120, Quotes.minuteOfDay(0L, TimeZone.getTimeZone("GMT+2")));
        assertEquals(23 * 60, Quotes.minuteOfDay(0L, TimeZone.getTimeZone("GMT-01:00")));
    }
}
