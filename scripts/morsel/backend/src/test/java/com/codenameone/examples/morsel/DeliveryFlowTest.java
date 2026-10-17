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
import com.codename1.backend.Json;
import com.codename1.backend.annotations.Autowired;
import com.codename1.backend.test.BackendTest;
import com.codename1.backend.test.MockMvc;
import com.codename1.backend.test.MockRequestBuilder;
import com.codenameone.examples.morsel.account.Accounts;
import com.codenameone.examples.morsel.account.DemoAccounts;
import com.codenameone.examples.morsel.delivery.Deliveries;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static com.codename1.backend.test.MockMvcRequestBuilders.get;
import static com.codename1.backend.test.MockMvcRequestBuilders.post;
import static com.codename1.backend.test.MockMvcResultMatchers.jsonPath;
import static com.codename1.backend.test.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// A delivery from request to rating, and the ways it can go otherwise.
@BackendTest
class DeliveryFlowTest {
    @Autowired
    private MockMvc mvc;
    @Autowired
    private Accounts accounts;
    @Autowired
    private Deliveries deliveries;
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
    void aDeliveryFromRequestToRating() throws Exception {
        // Lisbon. Each test has a city to itself.
        String customer = People.customer(accounts, "lis-customer");
        String courier = People.courier(accounts, "lis-courier");
        String delivery = People.delivery(38.7223, -9.1393, 38.7369, -9.1427);

        // Nobody is on line yet.
        mvc.perform(People.asCustomer(body(post("/api/deliveries/quote"), delivery), customer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.couriersNearby").value(0))
                .andExpect(jsonPath("$.currency").value("USD"));

        mvc.perform(People.asCourier(body(post("/api/courier/status"),
                        People.position(true, 38.7250, -9.1400)), courier))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.online").value(true));
        Map quote = json(People.asCustomer(body(post("/api/deliveries/quote"), delivery), customer));
        assertEquals(1L, ((Number) quote.get("couriersNearby")).longValue());
        long fare = ((Number) quote.get("amountCents")).longValue();
        assertTrue(fare >= 500, "the minimum fare, at least: " + fare);

        // Asking offers it to the courier at once. The customer is waiting, and is
        // not told who was asked.
        Map asked = json(People.asCustomer(body(post("/api/deliveries"), delivery), customer));
        String id = (String) asked.get("id");
        assertEquals("OFFERED", asked.get("state"));
        assertEquals(null, asked.get("courierUsername"));
        assertEquals(fare, ((Number) asked.get("fareCents")).longValue());
        mvc.perform(People.asCustomer(body(post("/api/deliveries"), delivery), customer))
                .andExpect(status().isConflict());

        // The courier sees the offer, with the clock running, and takes it.
        mvc.perform(People.asCourier(get("/api/courier/active"), courier))
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.state").value("OFFERED"))
                .andExpect(jsonPath("$.customerName").value("lis-customer"));
        // Not before it is taken.
        mvc.perform(People.asCourier(post("/api/courier/deliveries/" + id + "/start"), courier))
                .andExpect(status().isConflict());
        mvc.perform(People.asCourier(post("/api/courier/offers/" + id + "/accept"), courier))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("ACCEPTED"));
        mvc.perform(People.asCourier(post("/api/courier/offers/" + id + "/accept"), courier))
                .andExpect(status().isConflict());

        // Now the customer has a courier, a car to look for and a position to follow.
        mvc.perform(People.asCustomer(get("/api/deliveries/active"), customer))
                .andExpect(jsonPath("$.state").value("ACCEPTED"))
                .andExpect(jsonPath("$.courierName").value("lis-courier"))
                .andExpect(jsonPath("$.vehicle").value("Green Fiat 500"))
                .andExpect(jsonPath("$.courierLat").value(38.725));
        // The two of them can now reach each other, and each is told the
        // other's number and not their own. An admin looking at the delivery is
        // told neither.
        Map customersView = json(People.asCustomer(get("/api/deliveries/" + id), customer));
        assertEquals("+15550111", customersView.get("courierPhone"));
        assertEquals(null, customersView.get("customerPhone"));
        Map couriersView = json(People.asCourier(get("/api/courier/active"), courier));
        assertEquals("+15550111", couriersView.get("customerPhone"));
        assertEquals(null, couriersView.get("courierPhone"));
        // A name is the e-mail its account signs in with. Each is given their
        // own and not the other's; the admin is given both.
        assertEquals(customer, customersView.get("customerUsername"));
        assertEquals(null, customersView.get("courierUsername"));
        assertEquals(courier, couriersView.get("courierUsername"));
        assertEquals(null, couriersView.get("customerUsername"));
        Map adminsView = json(People.asAdmin(get("/api/deliveries/" + id), DemoAccounts.ADMIN));
        assertEquals(customer, adminsView.get("customerUsername"));
        assertEquals(courier, adminsView.get("courierUsername"));
        assertEquals(null, adminsView.get("courierPhone"));
        assertEquals(null, adminsView.get("customerPhone"));
        // A courier on a delivery stays on line.
        mvc.perform(People.asCourier(body(post("/api/courier/status"),
                        People.position(false, 38.7250, -9.1400)), courier))
                .andExpect(status().isConflict());

        mvc.perform(People.asCourier(post("/api/courier/deliveries/" + id + "/arrived"), courier))
                .andExpect(jsonPath("$.state").value("ARRIVED"));
        mvc.perform(People.asCourier(post("/api/courier/deliveries/" + id + "/start"), courier))
                .andExpect(jsonPath("$.state").value("IN_PROGRESS"));
        // Too late to cancel.
        mvc.perform(People.asCustomer(post("/api/deliveries/" + id + "/cancel"), customer))
                .andExpect(status().isConflict());
        // And not yet something to rate.
        mvc.perform(People.asCustomer(body(post("/api/deliveries/" + id + "/rate"), "{\"stars\":5}"), customer))
                .andExpect(status().isConflict());
        mvc.perform(People.asCourier(post("/api/courier/deliveries/" + id + "/complete"), courier))
                .andExpect(jsonPath("$.state").value("COMPLETED"))
                .andExpect(jsonPath("$.paymentStatus").value("PAID"));

        mvc.perform(People.asCustomer(get("/api/deliveries/active"), customer))
                .andExpect(jsonPath("$.state").value("NONE"));
        // The delivery is over, and so is the reason to have each other's number.
        assertEquals(null, json(People.asCustomer(get("/api/deliveries/" + id), customer)).get("courierPhone"));
        assertEquals(null, json(People.asCourier(get("/api/deliveries/" + id), courier)).get("customerPhone"));
        // And to know where the courier is, who goes on reporting it for the
        // next delivery: not from the delivery, and not from the history it stays in.
        mvc.perform(People.asCourier(body(post("/api/courier/status"),
                        People.position(true, 38.7300, -9.1500)), courier))
                .andExpect(status().isOk());
        mvc.perform(People.asCustomer(get("/api/deliveries/" + id), customer))
                .andExpect(jsonPath("$.courierName").value("lis-courier"))
                .andExpect(jsonPath("$.courierLat").value(0.0))
                .andExpect(jsonPath("$.courierLng").value(0.0));
        mvc.perform(People.asCustomer(get("/api/deliveries/history"), customer))
                .andExpect(jsonPath("$[0].courierLat").value(0.0))
                .andExpect(jsonPath("$[0].courierLng").value(0.0));
        mvc.perform(People.asCustomer(body(post("/api/deliveries/" + id + "/rate"), "{\"stars\":9}"), customer))
                .andExpect(status().isBadRequest());
        mvc.perform(People.asCustomer(body(post("/api/deliveries/" + id + "/rate"), "{\"stars\":4}"), customer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rating").value(4))
                .andExpect(jsonPath("$.courierRating").value(4.0));
        mvc.perform(People.asCustomer(body(post("/api/deliveries/" + id + "/rate"), "{\"stars\":1}"), customer))
                .andExpect(status().isConflict());

        mvc.perform(People.asCustomer(get("/api/deliveries/history"), customer))
                .andExpect(jsonPath("$[0].id").value(id))
                .andExpect(jsonPath("$[0].state").value("COMPLETED"));
        Map earned = json(People.asCourier(get("/api/courier/earnings"), courier));
        // The courier's share, not the fare: the service keeps a fifth, and
        // the onboarding screen must not show it as earned.
        long share = fare - Math.round(fare * 20d / 100d);
        assertEquals(share, ((Number) earned.get("totalCents")).longValue());
        assertEquals(share, ((Number) earned.get("todayCents")).longValue());
        assertEquals(share, ((Number) earned.get("weekCents")).longValue());
        assertEquals(1L, ((Number) earned.get("deliveries")).longValue());
    }

    @Test
    void anOfferPassesFromCourierToCourierThenGivesUp() throws Exception {
        // Madrid, with two couriers: one beside the pickup and one a kilometre off.
        String customer = People.customer(accounts, "mad-customer");
        String near = People.courier(accounts, "mad-near");
        String far = People.courier(accounts, "mad-far");
        mvc.perform(People.asCourier(body(post("/api/courier/status"),
                People.position(true, 40.4169, -3.7036)), near)).andExpect(status().isOk());
        mvc.perform(People.asCourier(body(post("/api/courier/status"),
                People.position(true, 40.4260, -3.7036)), far)).andExpect(status().isOk());

        Map asked = json(People.asCustomer(body(post("/api/deliveries"),
                People.delivery(40.4168, -3.7038, 40.4300, -3.6900)), customer));
        String id = (String) asked.get("id");
        // The nearer one is asked first, and only that one.
        mvc.perform(People.asCourier(get("/api/courier/active"), near))
                .andExpect(jsonPath("$.id").value(id));
        mvc.perform(People.asCourier(get("/api/courier/active"), far))
                .andExpect(jsonPath("$.state").value("NONE"));
        // An offer is one courier's. Another cannot take it, or learn of it.
        mvc.perform(People.asCourier(post("/api/courier/offers/" + id + "/accept"), far))
                .andExpect(status().isNotFound());

        // Passing on it sends it straight to the other.
        mvc.perform(People.asCourier(post("/api/courier/offers/" + id + "/decline"), near))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("NONE"));
        mvc.perform(People.asCourier(get("/api/courier/active"), far))
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.state").value("OFFERED"));

        // That one says nothing. Time is moved on by hand: the offer lapsed.
        db.execute("UPDATE delivery SET offer_expires_at = 1 WHERE id = ?", new Object[] {id});
        mvc.perform(People.asCourier(post("/api/courier/offers/" + id + "/accept"), far))
                .andExpect(status().isConflict());
        deliveries.tick();
        // Both have passed now, so it waits, offered to nobody.
        mvc.perform(People.asCustomer(get("/api/deliveries/active"), customer))
                .andExpect(jsonPath("$.state").value("REQUESTED"));
        mvc.perform(People.asCourier(get("/api/courier/active"), near))
                .andExpect(jsonPath("$.state").value("NONE"));

        // And when the search runs out of time, the customer is told so.
        db.execute("UPDATE delivery SET search_until = 1 WHERE id = ?", new Object[] {id});
        deliveries.tick();
        mvc.perform(People.asCustomer(get("/api/deliveries/" + id), customer))
                .andExpect(jsonPath("$.state").value("NO_COURIERS"));
        mvc.perform(People.asCustomer(get("/api/deliveries/active"), customer))
                .andExpect(jsonPath("$.state").value("NONE"));
    }

    @Test
    void aCustomerCancelsAndTheCourierIsFree() throws Exception {
        // Rome.
        String customer = People.customer(accounts, "rom-customer");
        String courier = People.courier(accounts, "rom-courier");
        String stranger = People.customer(accounts, "rom-stranger");
        mvc.perform(People.asCourier(body(post("/api/courier/status"),
                People.position(true, 41.9028, 12.4964)), courier)).andExpect(status().isOk());
        String delivery = People.delivery(41.9030, 12.4960, 41.8902, 12.4922);
        String id = (String) json(People.asCustomer(body(post("/api/deliveries"), delivery), customer)).get("id");
        mvc.perform(People.asCourier(post("/api/courier/offers/" + id + "/accept"), courier))
                .andExpect(status().isOk());

        // Somebody else's delivery does not exist, to read or to cancel.
        mvc.perform(People.asCustomer(get("/api/deliveries/" + id), stranger))
                .andExpect(status().isNotFound());
        mvc.perform(People.asCustomer(post("/api/deliveries/" + id + "/cancel"), stranger))
                .andExpect(status().isNotFound());

        mvc.perform(People.asCustomer(post("/api/deliveries/" + id + "/cancel"), customer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("CANCELLED_BY_CUSTOMER"));
        mvc.perform(People.asCourier(get("/api/courier/active"), courier))
                .andExpect(jsonPath("$.state").value("NONE"));
        mvc.perform(People.asCourier(post("/api/courier/deliveries/" + id + "/arrived"), courier))
                .andExpect(status().isConflict());
        // Free again, so the next delivery comes to the same courier.
        String next = (String) json(People.asCustomer(body(post("/api/deliveries"), delivery), customer)).get("id");
        mvc.perform(People.asCourier(get("/api/courier/active"), courier))
                .andExpect(jsonPath("$.id").value(next));
        mvc.perform(People.asCustomer(post("/api/deliveries/" + next + "/cancel"), customer))
                .andExpect(status().isOk());
    }

    @Test
    void aDeliveryNeedsAProvedPhoneAndSomewhereToGo() throws Exception {
        mvc.perform(post("/api/account/register").contentType("application/json")
                        .content("{\"email\":\"unproved@test.example\",\"password\":\"a-long-password\","
                                + "\"displayName\":\"Unproved\",\"phone\":\"+15550112\"}"))
                .andExpect(status().isOk());
        mvc.perform(People.asCustomer(body(post("/api/deliveries"),
                        People.delivery(48.8566, 2.3522, 48.8606, 2.3376)), "unproved@test.example"))
                .andExpect(status().isForbidden());
        String customer = People.customer(accounts, "par-customer");
        // The same point twice, and a point that is not on the planet.
        mvc.perform(People.asCustomer(body(post("/api/deliveries"),
                        People.delivery(48.8566, 2.3522, 48.8566, 2.3522)), customer))
                .andExpect(status().isBadRequest());
        mvc.perform(People.asCustomer(body(post("/api/deliveries/quote"),
                        People.delivery(148.8566, 2.3522, 48.8606, 2.3376)), customer))
                .andExpect(status().isBadRequest());
    }
}
