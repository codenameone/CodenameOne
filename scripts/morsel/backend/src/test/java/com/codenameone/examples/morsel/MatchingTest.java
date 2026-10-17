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
package com.codenameone.examples.morsel;

import com.codename1.backend.DataSource;
import com.codename1.backend.annotations.Autowired;
import com.codename1.backend.test.BackendTest;
import com.codename1.backend.test.MockMvc;
import com.codenameone.examples.morsel.account.Accounts;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static com.codename1.backend.test.MockMvcRequestBuilders.get;
import static com.codename1.backend.test.MockMvcRequestBuilders.post;
import static com.codename1.backend.test.MockMvcResultMatchers.jsonPath;
import static com.codename1.backend.test.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Which courier a delivery is offered to, when the delivery is particular: the kind of
/// car, a woman at the wheel, room for a wheelchair.
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

    private String ask(String customer, String extra) throws Exception {
        return Trips.delivery(LAT, LNG, 48.2200, 16.3900, extra);
    }

    /// Asks for a delivery, checks the state it is left in, and cancels it so the
    /// customer and the courier are free for the next one.
    private void expect(String customer, String extra, String state) throws Exception {
        Map asked = Trips.json(mvc, People.asCustomer(Trips.body(post("/api/deliveries"),
                ask(customer, extra)), customer));
        assertEquals(state, asked.get("state"), extra);
        mvc.perform(People.asCustomer(post("/api/deliveries/" + asked.get("id") + "/cancel"), customer))
                .andExpect(status().isOk());
    }

    @Test
    void aDeliveryGoesToACourierWhoFitsIt() throws Exception {
        // Vienna.
        String customer = People.customer(accounts, "vie-customer");
        String standard = People.courier(accounts, "vie-standard");
        String comfort = People.courier(accounts, "vie-comfort");
        // What an admin's review of the application would have recorded.
        db.execute("UPDATE courier_application SET product = 'comfort' WHERE username = ?",
                new Object[] {comfort});
        Trips.online(mvc, standard, LAT, LNG);
        Trips.online(mvc, comfort, LAT + 0.001, LNG);

        // The quote says what there is of each kind, and prices each.
        Map quote = Trips.json(mvc, People.asCustomer(Trips.body(post("/api/deliveries/quote"),
                People.delivery(LAT, LNG, 48.2200, 16.3900)), customer));
        List options = (List) quote.get("options");
        assertEquals(3, options.size());
        Map first = (Map) options.get(0);
        Map second = (Map) options.get(1);
        Map third = (Map) options.get(2);
        assertEquals("standard", first.get("product"));
        assertEquals("comfort", second.get("product"));
        assertEquals("xl", third.get("product"));
        assertEquals(1L, Trips.number(first, "couriersNearby"));
        assertEquals(1L, Trips.number(second, "couriersNearby"));
        assertEquals(0L, Trips.number(third, "couriersNearby"));
        assertEquals(4L, Trips.number(first, "seats"));
        assertEquals(6L, Trips.number(third, "seats"));
        long fare = Trips.number(first, "fare");
        assertEquals(fare, Trips.number(quote, "amountCents"));
        assertEquals(fare + Trips.number(first, "serviceFee"), Trips.number(first, "total"));
        // Each part of a fare is multiplied and rounded on its own, so the
        // dearer kinds are within a cent or two of the multiple.
        assertTrue(Math.abs(Trips.number(second, "fare") - fare * 1.35d) <= 2d);
        assertTrue(Math.abs(Trips.number(third, "fare") - fare * 1.7d) <= 2d);
        // Asked for a kind, the quote is that kind's.
        mvc.perform(People.asCustomer(Trips.body(post("/api/deliveries/quote"),
                        ask(customer, "\"product\":\"comfort\"")), customer))
                .andExpect(jsonPath("$.amountCents").value((int) Trips.number(second, "fare")))
                .andExpect(jsonPath("$.couriersNearby").value(1));
        mvc.perform(People.asCustomer(get("/api/deliveries/nearby?lat=" + LAT + "&lng=" + LNG), customer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].product").value("standard"))
                .andExpect(jsonPath("$[1].product").value("comfort"));

        // A comfort delivery goes past the nearer, standard car.
        Map asked = Trips.json(mvc, People.asCustomer(Trips.body(post("/api/deliveries"),
                ask(customer, "\"product\":\"comfort\"")), customer));
        String id = (String) asked.get("id");
        assertEquals("OFFERED", asked.get("state"));
        assertEquals("comfort", asked.get("product"));
        assertEquals(Trips.number(second, "fare"), Trips.number(asked, "fareCents"));
        mvc.perform(People.asCourier(get("/api/courier/active"), standard))
                .andExpect(jsonPath("$.state").value("NONE"));
        mvc.perform(People.asCourier(get("/api/courier/active"), comfort))
                .andExpect(jsonPath("$.id").value(id));
        mvc.perform(People.asCustomer(post("/api/deliveries/" + id + "/cancel"), customer))
                .andExpect(status().isOk());
        // Nobody drives an XL here, so one waits; and there is no fourth kind.
        expect(customer, "\"product\":\"xl\"", "REQUESTED");
        mvc.perform(People.asCustomer(Trips.body(post("/api/deliveries"),
                        ask(customer, "\"product\":\"limousine\"")), customer))
                .andExpect(status().isBadRequest());

        // A woman courier, asked for: neither is one until one says so.
        mvc.perform(People.asCustomer(Trips.body(post("/api/deliveries"),
                        ask(customer, "\"courierGender\":\"men\"")), customer))
                .andExpect(status().isBadRequest());
        expect(customer, "\"courierGender\":\"women\"", "REQUESTED");
        db.execute("UPDATE profile SET gender = 'female' WHERE username = ?",
                new Object[] {comfort});
        // Still not for a standard delivery: she drives a comfort car.
        expect(customer, "\"courierGender\":\"women\"", "REQUESTED");
        mvc.perform(People.asCustomer(Trips.body(post("/api/deliveries/quote"),
                        ask(customer, "\"courierGender\":\"women\"")), customer))
                .andExpect(jsonPath("$.couriersNearby").value(0))
                .andExpect(jsonPath("$.options[1].couriersNearby").value(1));
        asked = Trips.json(mvc, People.asCustomer(Trips.body(post("/api/deliveries"),
                ask(customer, "\"courierGender\":\"women\",\"product\":\"comfort\"")), customer));
        id = (String) asked.get("id");
        assertEquals("OFFERED", asked.get("state"));
        assertEquals("women", asked.get("courierGender"));
        mvc.perform(People.asCourier(post("/api/courier/offers/" + id + "/accept"), comfort))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.courierGender").value("women"));
        mvc.perform(People.asCustomer(post("/api/deliveries/" + id + "/cancel"), customer))
                .andExpect(status().isOk());

        // A car that takes a wheelchair.
        expect(customer, "\"accessibleVehicle\":true", "REQUESTED");
        db.execute("UPDATE courier_application SET wheelchair = 1 WHERE username = ?",
                new Object[] {standard});
        expect(customer, "\"accessibleVehicle\":true", "OFFERED");

        // A courier who takes pets: nobody, until one says so.
        expect(customer, "\"petFriendly\":true", "REQUESTED");
        mvc.perform(People.asCustomer(Trips.body(post("/api/deliveries/quote"),
                        ask(customer, "\"petFriendly\":true")), customer))
                .andExpect(jsonPath("$.couriersNearby").value(0));
        db.execute("UPDATE courier_application SET pets = 1 WHERE username = ?",
                new Object[] {standard});
        expect(customer, "\"petFriendly\":true", "OFFERED");

        // An approval withdrawn takes a courier out of matching at once, on
        // line or not.
        expect(customer, "\"product\":\"standard\"", "OFFERED");
        db.execute("UPDATE courier_application SET status = 'rejected' WHERE username = ?",
                new Object[] {standard});
        expect(customer, "\"product\":\"standard\"", "REQUESTED");
        mvc.perform(People.asCustomer(get("/api/deliveries/nearby?lat=" + LAT + "&lng=" + LNG), customer))
                .andExpect(jsonPath("$.length()").value(1));
        db.execute("UPDATE courier_application SET status = 'approved' WHERE username = ?",
                new Object[] {standard});
        Trips.offline(mvc, standard);
        Trips.offline(mvc, comfort);
    }

    @Test
    void aCourierWithNoProfileIsOfferedNothing() throws Exception {
        // Zurich.
        String customer = People.customer(accounts, "zrh-customer");
        String courier = People.courier(accounts, "zrh-courier");
        Trips.online(mvc, courier, 47.3769, 8.5417);
        String delivery = People.delivery(47.3771, 8.5419, 47.3900, 8.5600);
        mvc.perform(People.asCustomer(Trips.body(post("/api/deliveries/quote"), delivery), customer))
                .andExpect(jsonPath("$.couriersNearby").value(1));

        // What an account that is half gone leaves behind: on line, approved,
        // and nobody. Whatever the delivery asks for, it is not sent there.
        db.execute("DELETE FROM profile WHERE username = ?", new Object[] {courier});
        mvc.perform(People.asCustomer(Trips.body(post("/api/deliveries/quote"), delivery), customer))
                .andExpect(jsonPath("$.couriersNearby").value(0));
        Map asked = Trips.json(mvc, People.asCustomer(Trips.body(post("/api/deliveries"), delivery), customer));
        assertEquals("REQUESTED", asked.get("state"));
        mvc.perform(People.asCustomer(post("/api/deliveries/" + asked.get("id") + "/cancel"), customer))
                .andExpect(status().isOk());
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
        String delivery = People.delivery(51.5074, -0.1278, 51.5155, -0.0922);
        mvc.perform(People.asCustomer(Trips.body(post("/api/deliveries/quote"), delivery),
                People.customer(accounts, "ldn-customer")))
                .andExpect(jsonPath("$.couriersNearby").value(0));
    }
}
