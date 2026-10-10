/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codenameone.examples.wayline;

import com.codename1.backend.DataSource;
import com.codename1.backend.annotations.Autowired;
import com.codename1.backend.test.BackendTest;
import com.codename1.backend.test.MockMvc;
import com.codenameone.examples.wayline.account.Accounts;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static com.codename1.backend.test.MockMvcRequestBuilders.get;
import static com.codename1.backend.test.MockMvcRequestBuilders.post;
import static com.codename1.backend.test.MockMvcResultMatchers.jsonPath;
import static com.codename1.backend.test.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Which driver a ride is offered to, when the ride is particular: the kind of
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

    private String ask(String rider, String extra) throws Exception {
        return Trips.ride(LAT, LNG, 48.2200, 16.3900, extra);
    }

    /// Asks for a ride, checks the state it is left in, and cancels it so the
    /// rider and the driver are free for the next one.
    private void expect(String rider, String extra, String state) throws Exception {
        Map asked = Trips.json(mvc, People.asRider(Trips.body(post("/api/rides"),
                ask(rider, extra)), rider));
        assertEquals(state, asked.get("state"), extra);
        mvc.perform(People.asRider(post("/api/rides/" + asked.get("id") + "/cancel"), rider))
                .andExpect(status().isOk());
    }

    @Test
    void aRideGoesToADriverWhoFitsIt() throws Exception {
        // Vienna.
        String rider = People.rider(accounts, "vie-rider");
        String standard = People.driver(accounts, "vie-standard");
        String comfort = People.driver(accounts, "vie-comfort");
        // What an admin's review of the application would have recorded.
        db.execute("UPDATE wl_driver_application SET product = 'comfort' WHERE username = ?",
                new Object[] {comfort});
        Trips.online(mvc, standard, LAT, LNG);
        Trips.online(mvc, comfort, LAT + 0.001, LNG);

        // The quote says what there is of each kind, and prices each.
        Map quote = Trips.json(mvc, People.asRider(Trips.body(post("/api/rides/quote"),
                People.ride(LAT, LNG, 48.2200, 16.3900)), rider));
        List options = (List) quote.get("options");
        assertEquals(3, options.size());
        Map first = (Map) options.get(0);
        Map second = (Map) options.get(1);
        Map third = (Map) options.get(2);
        assertEquals("standard", first.get("product"));
        assertEquals("comfort", second.get("product"));
        assertEquals("xl", third.get("product"));
        assertEquals(1L, Trips.number(first, "driversNearby"));
        assertEquals(1L, Trips.number(second, "driversNearby"));
        assertEquals(0L, Trips.number(third, "driversNearby"));
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
        mvc.perform(People.asRider(Trips.body(post("/api/rides/quote"),
                        ask(rider, "\"product\":\"comfort\"")), rider))
                .andExpect(jsonPath("$.amountCents").value((int) Trips.number(second, "fare")))
                .andExpect(jsonPath("$.driversNearby").value(1));
        mvc.perform(People.asRider(get("/api/rides/nearby?lat=" + LAT + "&lng=" + LNG), rider))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].product").value("standard"))
                .andExpect(jsonPath("$[1].product").value("comfort"));

        // A comfort ride goes past the nearer, standard car.
        Map asked = Trips.json(mvc, People.asRider(Trips.body(post("/api/rides"),
                ask(rider, "\"product\":\"comfort\"")), rider));
        String id = (String) asked.get("id");
        assertEquals("OFFERED", asked.get("state"));
        assertEquals("comfort", asked.get("product"));
        assertEquals(Trips.number(second, "fare"), Trips.number(asked, "fareCents"));
        mvc.perform(People.asDriver(get("/api/driver/active"), standard))
                .andExpect(jsonPath("$.state").value("NONE"));
        mvc.perform(People.asDriver(get("/api/driver/active"), comfort))
                .andExpect(jsonPath("$.id").value(id));
        mvc.perform(People.asRider(post("/api/rides/" + id + "/cancel"), rider))
                .andExpect(status().isOk());
        // Nobody drives an XL here, so one waits; and there is no fourth kind.
        expect(rider, "\"product\":\"xl\"", "REQUESTED");
        mvc.perform(People.asRider(Trips.body(post("/api/rides"),
                        ask(rider, "\"product\":\"limousine\"")), rider))
                .andExpect(status().isBadRequest());

        // A woman driver, asked for: neither is one until one says so.
        mvc.perform(People.asRider(Trips.body(post("/api/rides"),
                        ask(rider, "\"driverGender\":\"men\"")), rider))
                .andExpect(status().isBadRequest());
        expect(rider, "\"driverGender\":\"women\"", "REQUESTED");
        db.execute("UPDATE wl_profile SET gender = 'female' WHERE username = ?",
                new Object[] {comfort});
        // Still not for a standard ride: she drives a comfort car.
        expect(rider, "\"driverGender\":\"women\"", "REQUESTED");
        mvc.perform(People.asRider(Trips.body(post("/api/rides/quote"),
                        ask(rider, "\"driverGender\":\"women\"")), rider))
                .andExpect(jsonPath("$.driversNearby").value(0))
                .andExpect(jsonPath("$.options[1].driversNearby").value(1));
        asked = Trips.json(mvc, People.asRider(Trips.body(post("/api/rides"),
                ask(rider, "\"driverGender\":\"women\",\"product\":\"comfort\"")), rider));
        id = (String) asked.get("id");
        assertEquals("OFFERED", asked.get("state"));
        assertEquals("women", asked.get("driverGender"));
        mvc.perform(People.asDriver(post("/api/driver/offers/" + id + "/accept"), comfort))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.driverGender").value("women"));
        mvc.perform(People.asRider(post("/api/rides/" + id + "/cancel"), rider))
                .andExpect(status().isOk());

        // A car that takes a wheelchair.
        expect(rider, "\"accessibleVehicle\":true", "REQUESTED");
        db.execute("UPDATE wl_driver_application SET wheelchair = 1 WHERE username = ?",
                new Object[] {standard});
        expect(rider, "\"accessibleVehicle\":true", "OFFERED");

        // A driver who takes pets: nobody, until one says so.
        expect(rider, "\"petFriendly\":true", "REQUESTED");
        mvc.perform(People.asRider(Trips.body(post("/api/rides/quote"),
                        ask(rider, "\"petFriendly\":true")), rider))
                .andExpect(jsonPath("$.driversNearby").value(0));
        db.execute("UPDATE wl_driver_application SET pets = 1 WHERE username = ?",
                new Object[] {standard});
        expect(rider, "\"petFriendly\":true", "OFFERED");

        // An approval withdrawn takes a driver out of matching at once, on
        // line or not.
        expect(rider, "\"product\":\"standard\"", "OFFERED");
        db.execute("UPDATE wl_driver_application SET status = 'rejected' WHERE username = ?",
                new Object[] {standard});
        expect(rider, "\"product\":\"standard\"", "REQUESTED");
        mvc.perform(People.asRider(get("/api/rides/nearby?lat=" + LAT + "&lng=" + LNG), rider))
                .andExpect(jsonPath("$.length()").value(1));
        db.execute("UPDATE wl_driver_application SET status = 'approved' WHERE username = ?",
                new Object[] {standard});
        Trips.offline(mvc, standard);
        Trips.offline(mvc, comfort);
    }

    @Test
    void aDriverWithNoProfileIsOfferedNothing() throws Exception {
        // Zurich.
        String rider = People.rider(accounts, "zrh-rider");
        String driver = People.driver(accounts, "zrh-driver");
        Trips.online(mvc, driver, 47.3769, 8.5417);
        String ride = People.ride(47.3771, 8.5419, 47.3900, 8.5600);
        mvc.perform(People.asRider(Trips.body(post("/api/rides/quote"), ride), rider))
                .andExpect(jsonPath("$.driversNearby").value(1));

        // What an account that is half gone leaves behind: on line, approved,
        // and nobody. Whatever the ride asks for, it is not sent there.
        db.execute("DELETE FROM wl_profile WHERE username = ?", new Object[] {driver});
        mvc.perform(People.asRider(Trips.body(post("/api/rides/quote"), ride), rider))
                .andExpect(jsonPath("$.driversNearby").value(0));
        Map asked = Trips.json(mvc, People.asRider(Trips.body(post("/api/rides"), ride), rider));
        assertEquals("REQUESTED", asked.get("state"));
        mvc.perform(People.asRider(post("/api/rides/" + asked.get("id") + "/cancel"), rider))
                .andExpect(status().isOk());
    }
}
