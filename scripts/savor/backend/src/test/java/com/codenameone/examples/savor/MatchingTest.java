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

import com.codename1.backend.DataSource;
import com.codename1.backend.annotations.Autowired;
import com.codename1.backend.test.BackendTest;
import com.codename1.backend.test.MockMvc;
import com.codenameone.examples.savor.account.Accounts;
import com.codenameone.examples.savor.account.DemoAccounts;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static com.codename1.backend.test.MockMvcRequestBuilders.get;
import static com.codename1.backend.test.MockMvcRequestBuilders.post;
import static com.codename1.backend.test.MockMvcResultMatchers.jsonPath;
import static com.codename1.backend.test.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertEquals;

/// Which courier an order's delivery is offered to: the free one nearest the
/// kitchen, approved and still somebody.
@BackendTest
class MatchingTest {
    private static final double LAT = 48.2082;
    private static final double LNG = 16.3738;

    @Autowired
    private MockMvc mvc;
    @Autowired
    private Accounts accounts;
    @Autowired
    private DataSource db;

    /// Orders from the kitchen at `lat`, `lng`, has it cooked, and answers the
    /// state its delivery is left in; then calls it off, so the customer and
    /// the courier are free for the next one.
    private String dispatched(String customer, double lat, double lng) throws Exception {
        String id = Trips.ask(mvc, customer, Trips.trip(lat, lng, lat + 0.012, lng + 0.016));
        String state = (String) ((Map) Trips.json(mvc, People.asCustomer(get("/api/orders/" + id),
                customer)).get("delivery")).get("state");
        mvc.perform(People.asAdmin(Trips.body(post("/api/admin/orders/" + id + "/cancel"),
                "{\"reason\":\"Test\"}"), DemoAccounts.ADMIN)).andExpect(status().isOk());
        return state;
    }

    @Test
    void theCourierNearestTheKitchenIsAsked() throws Exception {
        // Vienna: one courier by the door, one by the kitchen.
        String customer = People.customer(accounts, "vie-customer");
        String byTheDoor = People.courier(accounts, "vie-door");
        String byTheKitchen = People.courier(accounts, "vie-kitchen");
        Trips.online(mvc, byTheDoor, LAT + 0.012, LNG + 0.016);
        Trips.online(mvc, byTheKitchen, LAT + 0.001, LNG);

        String id = Trips.ask(mvc, customer, Trips.trip(LAT, LNG, LAT + 0.012, LNG + 0.016));
        mvc.perform(People.asCourier(get("/api/courier/active"), byTheKitchen))
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.state").value("OFFERED"));
        mvc.perform(People.asCourier(get("/api/courier/active"), byTheDoor))
                .andExpect(jsonPath("$.state").value("NONE"));
        mvc.perform(People.asAdmin(Trips.body(post("/api/admin/orders/" + id + "/cancel"),
                "{\"reason\":\"Test\"}"), DemoAccounts.ADMIN)).andExpect(status().isOk());

        // An approval withdrawn takes a courier out of matching at once, on
        // line or not.
        Trips.offline(mvc, byTheDoor);
        assertEquals("OFFERED", dispatched(customer, LAT, LNG));
        db.execute("UPDATE courier_application SET status = 'rejected' WHERE username = ?",
                new Object[] {byTheKitchen});
        assertEquals("REQUESTED", dispatched(customer, LAT, LNG));
        db.execute("UPDATE courier_application SET status = 'approved' WHERE username = ?",
                new Object[] {byTheKitchen});
        Trips.offline(mvc, byTheKitchen);
    }

    @Test
    void aCourierWithNoProfileIsOfferedNothing() throws Exception {
        // Zurich.
        String customer = People.customer(accounts, "zrh-customer");
        String courier = People.courier(accounts, "zrh-courier");
        Trips.online(mvc, courier, 47.3769, 8.5417);
        assertEquals("OFFERED", dispatched(customer, 47.3771, 8.5419));

        // What an account that is half gone leaves behind: on line, approved,
        // and nobody. No order is sent there.
        db.execute("DELETE FROM profile WHERE username = ?", new Object[] {courier});
        assertEquals("REQUESTED", dispatched(customer, 47.3771, 8.5419));
    }

    @Test
    void aCourierWhoGoesOffLineIsOffLine() throws Exception {
        // Two reports close together, so the second counts the time between
        // them as time on line: adding that time is a bulk update, and the
        // courier's row must already be written when it runs. The pause is
        // what makes that time more than nothing: two reports in the same
        // millisecond add no time and run no update.
        String courier = People.courier(accounts, "ldn-courier");
        Trips.online(mvc, courier, 51.5072, -0.1276);
        Thread.sleep(50);
        mvc.perform(People.asCourier(Trips.body(post("/api/courier/status"),
                People.position(false, 0, 0)), courier))
                .andExpect(jsonPath("$.online").value(false));
        mvc.perform(People.asCourier(get("/api/courier/me"), courier))
                .andExpect(jsonPath("$.online").value(false));
        assertEquals("REQUESTED", dispatched(People.customer(accounts, "ldn-customer"),
                51.5074, -0.1278));
    }
}
