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
import com.codenameone.examples.savor.delivery.Deliveries;
import com.codenameone.examples.savor.order.Orders;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static com.codename1.backend.test.MockMvcRequestBuilders.get;
import static com.codename1.backend.test.MockMvcRequestBuilders.post;
import static com.codename1.backend.test.MockMvcResultMatchers.jsonPath;
import static com.codename1.backend.test.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// An order from the cart to the stars, and the ways it can go otherwise:
/// the order and its delivery are two machines that move together.
@BackendTest
class OrderFlowTest {
    @Autowired
    private MockMvc mvc;
    @Autowired
    private Accounts accounts;
    @Autowired
    private Deliveries deliveries;
    @Autowired
    private Orders orders;
    @Autowired
    private DataSource db;

    @Test
    void anOrderFromTheCartToTheStars() throws Exception {
        // Lisbon. Each test has a city to itself.
        String customer = People.customer(accounts, "lis-customer");
        String courier = People.courier(accounts, "lis-courier");
        Trips.Trip trip = Trips.trip(38.7223, -9.1393, 38.7369, -9.1427);
        Trips.Kitchen kitchen = Trips.kitchen(mvc, trip.fromLat, trip.fromLng);
        String cart = Trips.cart(mvc, trip);

        Map quote = Trips.json(mvc, People.asCustomer(Trips.body(post("/api/orders/quote"), cart),
                customer));
        assertEquals("USD", quote.get("currency"));
        assertEquals(Trips.DISH_CENTS, Trips.number(quote, "subtotalCents"));
        long total = Trips.number(quote, "totalCents");
        assertTrue(total > Trips.DISH_CENTS, "fees on top of the food: " + total);

        // Placed, the order waits for the kitchen; nobody is looking for a
        // courier yet, and the card is only held.
        Map placed = Trips.place(mvc, customer, trip);
        String id = (String) placed.get("id");
        assertEquals("PLACED", placed.get("state"));
        assertEquals(total, Trips.number(placed, "totalCents"));
        assertEquals(null, placed.get("delivery"));
        mvc.perform(People.asMerchant(get("/api/merchant/orders"), kitchen.merchant))
                .andExpect(jsonPath("$[0].id").value(id));
        // Another kitchen's merchant does not see it, or get to answer it.
        Trips.Kitchen other = Trips.kitchen(mvc, 38.7000, -9.1500);
        mvc.perform(People.asMerchant(Trips.body(post("/api/merchant/orders/" + id + "/accept"),
                "{\"prepMinutes\":10}"), other.merchant)).andExpect(status().isNotFound());
        // A courier cannot pick up what is not cooked, or not even sent for.
        mvc.perform(People.asCourier(post("/api/courier/offers/" + id + "/accept"), courier))
                .andExpect(status().isNotFound());

        // On line beside the kitchen, the courier is offered it the moment
        // the kitchen takes it.
        Trips.online(mvc, courier, 38.7225, -9.1395);
        mvc.perform(People.asMerchant(Trips.body(post("/api/merchant/orders/" + id + "/accept"),
                        "{\"prepMinutes\":12}"), kitchen.merchant))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("ACCEPTED"))
                .andExpect(jsonPath("$.prepMinutes").value(12));
        mvc.perform(People.asCourier(get("/api/courier/active"), courier))
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.state").value("OFFERED"));
        // Too late for the customer to change their mind.
        mvc.perform(People.asCustomer(post("/api/orders/" + id + "/cancel"), customer))
                .andExpect(status().isConflict());
        mvc.perform(People.asCourier(post("/api/courier/offers/" + id + "/accept"), courier))
                .andExpect(jsonPath("$.state").value("ACCEPTED"));
        mvc.perform(People.asCourier(post("/api/courier/deliveries/" + id + "/arrived"), courier))
                .andExpect(jsonPath("$.state").value("ARRIVED"));
        // The food is not ready, so it cannot leave.
        mvc.perform(People.asCourier(post("/api/courier/deliveries/" + id + "/start"), courier))
                .andExpect(status().isConflict());
        mvc.perform(People.asMerchant(post("/api/merchant/orders/" + id + "/ready"), kitchen.merchant))
                .andExpect(jsonPath("$.state").value("READY"));
        mvc.perform(People.asCourier(post("/api/courier/deliveries/" + id + "/start"), courier))
                .andExpect(jsonPath("$.state").value("IN_PROGRESS"));

        // On its way: the customer follows the courier and can call them; the
        // courier can call the customer; the admin is told neither number.
        Map customersView = Trips.json(mvc, People.asCustomer(get("/api/orders/" + id), customer));
        assertEquals("PICKED_UP", customersView.get("state"));
        Map delivery = (Map) customersView.get("delivery");
        assertEquals("lis-courier", delivery.get("courierName"));
        assertEquals("+15550111", delivery.get("courierPhone"));
        Map couriersView = Trips.json(mvc, People.asCourier(get("/api/courier/active"), courier));
        assertEquals("+15550111", couriersView.get("customerPhone"));
        Map adminsView = Trips.json(mvc, People.asAdmin(get("/api/orders/" + id), DemoAccounts.ADMIN));
        assertEquals(null, ((Map) adminsView.get("delivery")).get("courierPhone"));
        // Nobody else sees the order at all.
        String stranger = People.customer(accounts, "lis-stranger");
        mvc.perform(People.asCustomer(get("/api/orders/" + id), stranger))
                .andExpect(status().isNotFound());
        // Not yet something to rate.
        mvc.perform(People.asCustomer(Trips.body(post("/api/orders/" + id + "/rate"), "{\"stars\":5}"),
                customer)).andExpect(status().isConflict());

        mvc.perform(People.asCourier(post("/api/courier/deliveries/" + id + "/complete"), courier))
                .andExpect(jsonPath("$.state").value("COMPLETED"))
                .andExpect(jsonPath("$.paymentStatus").value("PAID"));
        Map delivered = Trips.json(mvc, People.asCustomer(get("/api/orders/" + id), customer));
        assertEquals("DELIVERED", delivered.get("state"));
        assertEquals("PAID", delivered.get("paymentStatus"));
        assertEquals(null, ((Map) delivered.get("delivery")).get("courierPhone"));
        assertEquals(0, Trips.list(mvc, People.asCustomer(get("/api/orders/active"), customer)).size());
        mvc.perform(People.asCustomer(get("/api/orders/history"), customer))
                .andExpect(jsonPath("$[0].id").value(id))
                .andExpect(jsonPath("$[0].state").value("DELIVERED"));

        mvc.perform(People.asCustomer(Trips.body(post("/api/orders/" + id + "/rate"), "{\"stars\":9}"),
                customer)).andExpect(status().isBadRequest());
        mvc.perform(People.asCustomer(Trips.body(post("/api/orders/" + id + "/rate"), "{\"stars\":4}"),
                        customer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stars").value(4));
        mvc.perform(People.asCustomer(Trips.body(post("/api/orders/" + id + "/rate"), "{\"stars\":1}"),
                customer)).andExpect(status().isConflict());

        // The courier is paid the delivery fee and the tip, not the food.
        Map earned = Trips.json(mvc, People.asCourier(get("/api/courier/earnings"), courier));
        assertEquals(Trips.number(delivered, "deliveryFeeCents") + Trips.number(delivered, "tipCents"),
                Trips.number(earned, "totalCents"));
        assertEquals(1L, Trips.number(earned, "deliveries"));
    }

    @Test
    void aKitchenThatSaysNothingTurnsTheOrderDown() throws Exception {
        // Seville.
        String customer = People.customer(accounts, "svq-customer");
        Trips.Trip trip = Trips.trip(37.3891, -5.9845, 37.3772, -5.9869);
        String id = (String) Trips.place(mvc, customer, trip).get("id");
        db.execute("UPDATE customer_order SET placed_at = 1 WHERE id = ?", new Object[] {id});
        orders.sweep();
        mvc.perform(People.asCustomer(get("/api/orders/" + id), customer))
                .andExpect(jsonPath("$.state").value("REJECTED"))
                .andExpect(jsonPath("$.paymentStatus").value("RELEASED"));
    }

    @Test
    void anOfferPassesFromCourierToCourierThenGivesUp() throws Exception {
        // Madrid, with two couriers: one beside the kitchen and one a kilometre off.
        String customer = People.customer(accounts, "mad-customer");
        String near = People.courier(accounts, "mad-near");
        String far = People.courier(accounts, "mad-far");
        Trips.online(mvc, near, 40.4169, -3.7036);
        Trips.online(mvc, far, 40.4260, -3.7036);

        String id = Trips.ask(mvc, customer, Trips.trip(40.4168, -3.7038, 40.4300, -3.6900));
        // The nearer one is asked first, and only that one.
        mvc.perform(People.asCourier(get("/api/courier/active"), near))
                .andExpect(jsonPath("$.id").value(id));
        mvc.perform(People.asCourier(get("/api/courier/active"), far))
                .andExpect(jsonPath("$.state").value("NONE"));
        // An offer is one courier's. Another cannot take it.
        mvc.perform(People.asCourier(post("/api/courier/offers/" + id + "/accept"), far))
                .andExpect(status().isNotFound());

        // Passing on it sends it straight to the other.
        mvc.perform(People.asCourier(post("/api/courier/offers/" + id + "/decline"), near))
                .andExpect(jsonPath("$.state").value("NONE"));
        mvc.perform(People.asCourier(get("/api/courier/active"), far))
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.state").value("OFFERED"));

        // That one says nothing. Time is moved on by hand: the offer lapsed.
        db.execute("UPDATE delivery SET offer_expires_at = 1 WHERE id = ?", new Object[] {id});
        mvc.perform(People.asCourier(post("/api/courier/offers/" + id + "/accept"), far))
                .andExpect(status().isConflict());
        deliveries.tick();
        mvc.perform(People.asCustomer(get("/api/orders/" + id), customer))
                .andExpect(jsonPath("$.state").value("READY"))
                .andExpect(jsonPath("$.delivery.state").value("REQUESTED"));

        // And when the search runs out of time, the order ends and the card
        // is let go.
        db.execute("UPDATE delivery SET search_until = 1 WHERE id = ?", new Object[] {id});
        deliveries.tick();
        mvc.perform(People.asCustomer(get("/api/orders/" + id), customer))
                .andExpect(jsonPath("$.state").value("NO_COURIERS"))
                .andExpect(jsonPath("$.paymentStatus").value("RELEASED"));
        assertEquals(0, Trips.list(mvc, People.asCustomer(get("/api/orders/active"), customer)).size());
    }

    @Test
    void aCourierWhoGivesUpSendsTheOrderOnToTheNext() throws Exception {
        // Rome.
        String customer = People.customer(accounts, "rom-customer");
        String first = People.courier(accounts, "rom-first");
        String second = People.courier(accounts, "rom-second");
        Trips.online(mvc, first, 41.9028, 12.4964);
        String id = Trips.ask(mvc, customer, Trips.trip(41.9030, 12.4960, 41.8902, 12.4922));
        mvc.perform(People.asCourier(post("/api/courier/offers/" + id + "/accept"), first))
                .andExpect(status().isOk());
        Trips.online(mvc, second, 41.9040, 12.4970);
        mvc.perform(People.asCourier(post("/api/courier/deliveries/" + id + "/cancel"), first))
                .andExpect(status().isOk());
        mvc.perform(People.asCourier(get("/api/courier/active"), first))
                .andExpect(jsonPath("$.state").value("NONE"));
        mvc.perform(People.asCourier(get("/api/courier/active"), second))
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.state").value("OFFERED"));
        mvc.perform(People.asCustomer(get("/api/orders/" + id), customer))
                .andExpect(jsonPath("$.state").value("READY"));
    }

    @Test
    void aCustomerCancelsBeforeTheKitchenAnswers() throws Exception {
        // Naples.
        String customer = People.customer(accounts, "nap-customer");
        String stranger = People.customer(accounts, "nap-stranger");
        Trips.Trip trip = Trips.trip(40.8518, 14.2681, 40.8400, 14.2500);
        Trips.Kitchen kitchen = Trips.kitchen(mvc, trip.fromLat, trip.fromLng);
        String id = (String) Trips.place(mvc, customer, trip).get("id");
        // Somebody else's order does not exist, to read or to cancel.
        mvc.perform(People.asCustomer(post("/api/orders/" + id + "/cancel"), stranger))
                .andExpect(status().isNotFound());
        mvc.perform(People.asCustomer(post("/api/orders/" + id + "/cancel"), customer))
                .andExpect(jsonPath("$.state").value("CANCELLED_BY_CUSTOMER"))
                .andExpect(jsonPath("$.paymentStatus").value("RELEASED"));
        mvc.perform(People.asMerchant(Trips.body(post("/api/merchant/orders/" + id + "/accept"),
                "{\"prepMinutes\":10}"), kitchen.merchant)).andExpect(status().isConflict());
    }

    @Test
    void anAdminCancelEndsTheDeliveryToo() throws Exception {
        // Milan.
        String customer = People.customer(accounts, "mil-customer");
        String courier = People.courier(accounts, "mil-courier");
        Trips.online(mvc, courier, 45.4642, 9.1900);
        String id = Trips.ask(mvc, customer, Trips.trip(45.4641, 9.1899, 45.4800, 9.2000));
        mvc.perform(People.asCourier(post("/api/courier/offers/" + id + "/accept"), courier))
                .andExpect(status().isOk());
        mvc.perform(People.asAdmin(Trips.body(post("/api/admin/orders/" + id + "/cancel"),
                        "{\"reason\":\"Kitchen fire\"}"), DemoAccounts.ADMIN))
                .andExpect(jsonPath("$.state").value("CANCELLED_BY_ADMIN"));
        mvc.perform(People.asCourier(get("/api/courier/active"), courier))
                .andExpect(jsonPath("$.state").value("NONE"));
        mvc.perform(People.asCourier(post("/api/courier/deliveries/" + id + "/arrived"), courier))
                .andExpect(status().isConflict());
        // Free again, so the next order goes to the same courier.
        String next = Trips.ask(mvc, customer, Trips.trip(45.4641, 9.1899, 45.4700, 9.1800));
        mvc.perform(People.asCourier(get("/api/courier/active"), courier))
                .andExpect(jsonPath("$.id").value(next));
    }

    @Test
    void anOrderNeedsAProvedPhoneAnOpenKitchenAndADoorInReach() throws Exception {
        // Paris.
        mvc.perform(Trips.body(post("/api/account/register"),
                        "{\"email\":\"unproved@test.example\",\"password\":\"a-long-password\","
                                + "\"displayName\":\"Unproved\",\"phone\":\"+15550112\"}"))
                .andExpect(status().isOk());
        Trips.Trip trip = Trips.trip(48.8566, 2.3522, 48.8606, 2.3376);
        mvc.perform(People.asCustomer(Trips.body(post("/api/orders"), Trips.cart(mvc, trip)),
                "unproved@test.example")).andExpect(status().isForbidden());
        String customer = People.customer(accounts, "par-customer");
        // Further than anyone delivers.
        mvc.perform(People.asCustomer(Trips.body(post("/api/orders/quote"),
                        Trips.cart(mvc, Trips.trip(48.8566, 2.3522, 49.8566, 2.3522))), customer))
                .andExpect(status().isBadRequest());
        // A closed kitchen takes no orders.
        Trips.Kitchen kitchen = Trips.kitchen(mvc, trip.fromLat, trip.fromLng);
        mvc.perform(People.asMerchant(post("/api/merchant/paused?paused=true"), kitchen.merchant))
                .andExpect(status().isOk());
        mvc.perform(People.asCustomer(Trips.body(post("/api/orders"), Trips.cart(mvc, trip)), customer))
                .andExpect(status().isConflict());
        mvc.perform(People.asMerchant(post("/api/merchant/paused?paused=false"), kitchen.merchant))
                .andExpect(status().isOk());
        // Three on the way at once, and no more.
        for (int iter = 0; iter < 3; iter++) {
            Trips.place(mvc, customer, trip);
        }
        mvc.perform(People.asCustomer(Trips.body(post("/api/orders"), Trips.cart(mvc, trip)), customer))
                .andExpect(status().isConflict());
    }
}
