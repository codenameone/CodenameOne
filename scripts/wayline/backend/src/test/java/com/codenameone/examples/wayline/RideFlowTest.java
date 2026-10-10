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
import com.codename1.backend.Json;
import com.codename1.backend.annotations.Autowired;
import com.codename1.backend.test.BackendTest;
import com.codename1.backend.test.MockMvc;
import com.codename1.backend.test.MockRequestBuilder;
import com.codenameone.examples.wayline.account.Accounts;
import com.codenameone.examples.wayline.account.DemoAccounts;
import com.codenameone.examples.wayline.ride.Rides;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static com.codename1.backend.test.MockMvcRequestBuilders.get;
import static com.codename1.backend.test.MockMvcRequestBuilders.post;
import static com.codename1.backend.test.MockMvcResultMatchers.jsonPath;
import static com.codename1.backend.test.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// A ride from request to rating, and the ways it can go otherwise.
@BackendTest
class RideFlowTest {
    @Autowired
    private MockMvc mvc;
    @Autowired
    private Accounts accounts;
    @Autowired
    private Rides rides;
    @Autowired
    private DataSource db;

    private Map json(MockRequestBuilder request) throws Exception {
        return Json.parseObject(mvc.perform(request).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
    }

    private static MockRequestBuilder body(MockRequestBuilder request, String json) {
        return request.contentType("application/json").content(json);
    }

    @Test
    void aRideFromRequestToRating() throws Exception {
        // Lisbon. Each test has a city to itself.
        String rider = People.rider(accounts, "lis-rider");
        String driver = People.driver(accounts, "lis-driver");
        String ride = People.ride(38.7223, -9.1393, 38.7369, -9.1427);

        // Nobody is on line yet.
        mvc.perform(People.asRider(body(post("/api/rides/quote"), ride), rider))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.driversNearby").value(0))
                .andExpect(jsonPath("$.currency").value("USD"));

        mvc.perform(People.asDriver(body(post("/api/driver/status"),
                        People.position(true, 38.7250, -9.1400)), driver))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.online").value(true));
        Map quote = json(People.asRider(body(post("/api/rides/quote"), ride), rider));
        assertEquals(1L, ((Number) quote.get("driversNearby")).longValue());
        long fare = ((Number) quote.get("amountCents")).longValue();
        assertTrue(fare >= 500, "the minimum fare, at least: " + fare);

        // Asking offers it to the driver at once. The rider is waiting, and is
        // not told who was asked.
        Map asked = json(People.asRider(body(post("/api/rides"), ride), rider));
        String id = (String) asked.get("id");
        assertEquals("OFFERED", asked.get("state"));
        assertEquals(null, asked.get("driverUsername"));
        assertEquals(fare, ((Number) asked.get("fareCents")).longValue());
        mvc.perform(People.asRider(body(post("/api/rides"), ride), rider))
                .andExpect(status().isConflict());

        // The driver sees the offer, with the clock running, and takes it.
        mvc.perform(People.asDriver(get("/api/driver/active"), driver))
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.state").value("OFFERED"))
                .andExpect(jsonPath("$.riderName").value("lis-rider"));
        // Not before it is taken.
        mvc.perform(People.asDriver(post("/api/driver/rides/" + id + "/start"), driver))
                .andExpect(status().isConflict());
        mvc.perform(People.asDriver(post("/api/driver/offers/" + id + "/accept"), driver))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("ACCEPTED"));
        mvc.perform(People.asDriver(post("/api/driver/offers/" + id + "/accept"), driver))
                .andExpect(status().isConflict());

        // Now the rider has a driver, a car to look for and a position to follow.
        mvc.perform(People.asRider(get("/api/rides/active"), rider))
                .andExpect(jsonPath("$.state").value("ACCEPTED"))
                .andExpect(jsonPath("$.driverName").value("lis-driver"))
                .andExpect(jsonPath("$.vehicle").value("Green Fiat 500"))
                .andExpect(jsonPath("$.driverLat").value(38.725));
        // The two of them can now reach each other, and each is told the
        // other's number and not their own. An admin looking at the ride is
        // told neither.
        Map ridersView = json(People.asRider(get("/api/rides/" + id), rider));
        assertEquals("+15550111", ridersView.get("driverPhone"));
        assertEquals(null, ridersView.get("riderPhone"));
        Map driversView = json(People.asDriver(get("/api/driver/active"), driver));
        assertEquals("+15550111", driversView.get("riderPhone"));
        assertEquals(null, driversView.get("driverPhone"));
        Map adminsView = json(People.asAdmin(get("/api/rides/" + id), DemoAccounts.ADMIN));
        assertEquals(null, adminsView.get("driverPhone"));
        assertEquals(null, adminsView.get("riderPhone"));
        // A driver on a ride stays on line.
        mvc.perform(People.asDriver(body(post("/api/driver/status"),
                        People.position(false, 38.7250, -9.1400)), driver))
                .andExpect(status().isConflict());

        mvc.perform(People.asDriver(post("/api/driver/rides/" + id + "/arrived"), driver))
                .andExpect(jsonPath("$.state").value("ARRIVED"));
        mvc.perform(People.asDriver(post("/api/driver/rides/" + id + "/start"), driver))
                .andExpect(jsonPath("$.state").value("IN_PROGRESS"));
        // Too late to cancel.
        mvc.perform(People.asRider(post("/api/rides/" + id + "/cancel"), rider))
                .andExpect(status().isConflict());
        // And not yet something to rate.
        mvc.perform(People.asRider(body(post("/api/rides/" + id + "/rate"), "{\"stars\":5}"), rider))
                .andExpect(status().isConflict());
        mvc.perform(People.asDriver(post("/api/driver/rides/" + id + "/complete"), driver))
                .andExpect(jsonPath("$.state").value("COMPLETED"))
                .andExpect(jsonPath("$.paymentStatus").value("PAID"));

        mvc.perform(People.asRider(get("/api/rides/active"), rider))
                .andExpect(jsonPath("$.state").value("NONE"));
        // The ride is over, and so is the reason to have each other's number.
        assertEquals(null, json(People.asRider(get("/api/rides/" + id), rider)).get("driverPhone"));
        assertEquals(null, json(People.asDriver(get("/api/rides/" + id), driver)).get("riderPhone"));
        mvc.perform(People.asRider(body(post("/api/rides/" + id + "/rate"), "{\"stars\":9}"), rider))
                .andExpect(status().isBadRequest());
        mvc.perform(People.asRider(body(post("/api/rides/" + id + "/rate"), "{\"stars\":4}"), rider))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rating").value(4))
                .andExpect(jsonPath("$.driverRating").value(4.0));
        mvc.perform(People.asRider(body(post("/api/rides/" + id + "/rate"), "{\"stars\":1}"), rider))
                .andExpect(status().isConflict());

        mvc.perform(People.asRider(get("/api/rides/history"), rider))
                .andExpect(jsonPath("$[0].id").value(id))
                .andExpect(jsonPath("$[0].state").value("COMPLETED"));
        Map earned = json(People.asDriver(get("/api/driver/earnings"), driver));
        assertEquals(fare, ((Number) earned.get("totalCents")).longValue());
        assertEquals(fare, ((Number) earned.get("todayCents")).longValue());
        assertEquals(1L, ((Number) earned.get("rides")).longValue());
    }

    @Test
    void anOfferPassesFromDriverToDriverThenGivesUp() throws Exception {
        // Madrid, with two drivers: one beside the pickup and one a kilometre off.
        String rider = People.rider(accounts, "mad-rider");
        String near = People.driver(accounts, "mad-near");
        String far = People.driver(accounts, "mad-far");
        mvc.perform(People.asDriver(body(post("/api/driver/status"),
                People.position(true, 40.4169, -3.7036)), near)).andExpect(status().isOk());
        mvc.perform(People.asDriver(body(post("/api/driver/status"),
                People.position(true, 40.4260, -3.7036)), far)).andExpect(status().isOk());

        Map asked = json(People.asRider(body(post("/api/rides"),
                People.ride(40.4168, -3.7038, 40.4300, -3.6900)), rider));
        String id = (String) asked.get("id");
        // The nearer one is asked first, and only that one.
        mvc.perform(People.asDriver(get("/api/driver/active"), near))
                .andExpect(jsonPath("$.id").value(id));
        mvc.perform(People.asDriver(get("/api/driver/active"), far))
                .andExpect(jsonPath("$.state").value("NONE"));
        // An offer is one driver's. Another cannot take it, or learn of it.
        mvc.perform(People.asDriver(post("/api/driver/offers/" + id + "/accept"), far))
                .andExpect(status().isNotFound());

        // Passing on it sends it straight to the other.
        mvc.perform(People.asDriver(post("/api/driver/offers/" + id + "/decline"), near))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("NONE"));
        mvc.perform(People.asDriver(get("/api/driver/active"), far))
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.state").value("OFFERED"));

        // That one says nothing. Time is moved on by hand: the offer lapsed.
        db.execute("UPDATE wl_ride SET offer_expires_at = 1 WHERE id = ?", new Object[] {id});
        mvc.perform(People.asDriver(post("/api/driver/offers/" + id + "/accept"), far))
                .andExpect(status().isConflict());
        rides.tick();
        // Both have passed now, so it waits, offered to nobody.
        mvc.perform(People.asRider(get("/api/rides/active"), rider))
                .andExpect(jsonPath("$.state").value("REQUESTED"));
        mvc.perform(People.asDriver(get("/api/driver/active"), near))
                .andExpect(jsonPath("$.state").value("NONE"));

        // And when the search runs out of time, the rider is told so.
        db.execute("UPDATE wl_ride SET search_until = 1 WHERE id = ?", new Object[] {id});
        rides.tick();
        mvc.perform(People.asRider(get("/api/rides/" + id), rider))
                .andExpect(jsonPath("$.state").value("NO_DRIVERS"));
        mvc.perform(People.asRider(get("/api/rides/active"), rider))
                .andExpect(jsonPath("$.state").value("NONE"));
    }

    @Test
    void aRiderCancelsAndTheDriverIsFree() throws Exception {
        // Rome.
        String rider = People.rider(accounts, "rom-rider");
        String driver = People.driver(accounts, "rom-driver");
        String stranger = People.rider(accounts, "rom-stranger");
        mvc.perform(People.asDriver(body(post("/api/driver/status"),
                People.position(true, 41.9028, 12.4964)), driver)).andExpect(status().isOk());
        String ride = People.ride(41.9030, 12.4960, 41.8902, 12.4922);
        String id = (String) json(People.asRider(body(post("/api/rides"), ride), rider)).get("id");
        mvc.perform(People.asDriver(post("/api/driver/offers/" + id + "/accept"), driver))
                .andExpect(status().isOk());

        // Somebody else's ride does not exist, to read or to cancel.
        mvc.perform(People.asRider(get("/api/rides/" + id), stranger))
                .andExpect(status().isNotFound());
        mvc.perform(People.asRider(post("/api/rides/" + id + "/cancel"), stranger))
                .andExpect(status().isNotFound());

        mvc.perform(People.asRider(post("/api/rides/" + id + "/cancel"), rider))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("CANCELLED_BY_RIDER"));
        mvc.perform(People.asDriver(get("/api/driver/active"), driver))
                .andExpect(jsonPath("$.state").value("NONE"));
        mvc.perform(People.asDriver(post("/api/driver/rides/" + id + "/arrived"), driver))
                .andExpect(status().isConflict());
        // Free again, so the next ride comes to the same driver.
        String next = (String) json(People.asRider(body(post("/api/rides"), ride), rider)).get("id");
        mvc.perform(People.asDriver(get("/api/driver/active"), driver))
                .andExpect(jsonPath("$.id").value(next));
        mvc.perform(People.asRider(post("/api/rides/" + next + "/cancel"), rider))
                .andExpect(status().isOk());
    }

    @Test
    void aRideNeedsAProvedPhoneAndSomewhereToGo() throws Exception {
        mvc.perform(post("/api/account/register").contentType("application/json")
                        .content("{\"email\":\"unproved@test.example\",\"password\":\"a-long-password\","
                                + "\"displayName\":\"Unproved\",\"phone\":\"+15550112\"}"))
                .andExpect(status().isOk());
        mvc.perform(People.asRider(body(post("/api/rides"),
                        People.ride(48.8566, 2.3522, 48.8606, 2.3376)), "unproved@test.example"))
                .andExpect(status().isForbidden());
        String rider = People.rider(accounts, "par-rider");
        // The same point twice, and a point that is not on the planet.
        mvc.perform(People.asRider(body(post("/api/rides"),
                        People.ride(48.8566, 2.3522, 48.8566, 2.3522)), rider))
                .andExpect(status().isBadRequest());
        mvc.perform(People.asRider(body(post("/api/rides/quote"),
                        People.ride(148.8566, 2.3522, 48.8606, 2.3376)), rider))
                .andExpect(status().isBadRequest());
    }
}
