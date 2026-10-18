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

import com.codename1.backend.Json;
import com.codename1.backend.test.MockMvc;
import com.codename1.backend.test.MockRequestBuilder;

import com.codenameone.examples.savor.account.DemoAccounts;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.codename1.backend.test.MockMvcRequestBuilders.get;
import static com.codename1.backend.test.MockMvcRequestBuilders.post;
import static com.codename1.backend.test.MockMvcResultMatchers.status;

/// The steps several tests take on the way to what they are about: a courier
/// going on line, a card being saved, an order being cooked and carried to the door.
public final class Trips {
    public static final String VISA = "4242424242424242";
    public static final String MASTERCARD = "5555555555554444";
    /// Refused when it is saved.
    public static final String DECLINED = "4000000000000002";
    /// Saved without complaint, and refused when it is charged.
    public static final String FAILS_WHEN_CHARGED = "4000000000000341";
    /// Saved without complaint, and charging it fails without an answer.
    public static final String NO_ANSWER_WHEN_CHARGED = "4000000000000119";
    /// Charged without complaint; a refund of it is pending at first.
    public static final String SLOW_REFUND = "4000000000007726";

    private Trips() {
    }

    public static MockRequestBuilder body(MockRequestBuilder request, String json) {
        return request.contentType("application/json").content(json);
    }

    /// The object a request that succeeds answers with.
    public static Map json(MockMvc mvc, MockRequestBuilder request) throws Exception {
        return Json.parseObject(mvc.perform(request).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
    }

    /// The list a request that succeeds answers with.
    public static List list(MockMvc mvc, MockRequestBuilder request) throws Exception {
        return (List) Json.parse(mvc.perform(request).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
    }

    /// The status a request is answered with, whatever it is.
    public static int statusOf(MockMvc mvc, MockRequestBuilder request) throws Exception {
        return mvc.perform(request).andReturn().getResponse().getStatus();
    }

    public static long number(Map object, String field) {
        return ((Number) object.get(field)).longValue();
    }

    /// A restaurant of a test's own, its merchant and the one dish on its menu.
    public static final class Kitchen {
        public final String merchant;
        public final String id;
        public final String dish;
        public final double lat;
        public final double lng;

        Kitchen(String merchant, String id, String dish, double lat, double lng) {
            this.merchant = merchant;
            this.id = id;
            this.dish = dish;
            this.lat = lat;
            this.lng = lng;
        }
    }

    /// Where an order is cooked and where it goes.
    public static final class Trip {
        public final double fromLat;
        public final double fromLng;
        public final double toLat;
        public final double toLng;
        /// Further members of the cart, as JSON without the braces, or "".
        public final String extra;

        Trip(double fromLat, double fromLng, double toLat, double toLng, String extra) {
            this.fromLat = fromLat;
            this.fromLng = fromLng;
            this.toLat = toLat;
            this.toLng = toLng;
            this.extra = extra;
        }
    }

    /// A dish of this price, cooked at the first point and eaten at the second.
    public static final long DISH_CENTS = 1800;

    private static final Map<String, Kitchen> KITCHENS = new HashMap<>();
    /// The application the kitchens above were opened in. Classes whose
    /// contexts differ (one injects the database, another does not) each start
    /// the application afresh, with an empty in-memory database, and a compiled
    /// run keeps this class's statics across all of them in one process, so a
    /// kitchen remembered from an application that has stopped names a
    /// restaurant the running one never had. Each running application has its
    /// own MockMvc, which is what tells them apart.
    private static MockMvc kitchensOf;
    private static int kitchens;

    public static Trip trip(double fromLat, double fromLng, double toLat, double toLng) {
        return new Trip(fromLat, fromLng, toLat, toLng, "");
    }

    /// A trip whose cart says more: `extra` is further members of it, as JSON,
    /// without the braces.
    public static Trip trip(double fromLat, double fromLng, double toLat, double toLng, String extra) {
        return new Trip(fromLat, fromLng, toLat, toLng, extra);
    }

    /// The restaurant at a point, opened, approved and given a menu the first
    /// time it is asked for, and kept for as long as the application it was
    /// opened in runs.
    public static synchronized Kitchen kitchen(MockMvc mvc, double lat, double lng) throws Exception {
        if (kitchensOf != mvc) {
            KITCHENS.clear();
            kitchensOf = mvc;
        }
        String key = lat + "," + lng;
        Kitchen known = KITCHENS.get(key);
        if (known != null) {
            return known;
        }
        kitchens++;
        String merchant = "kitchen" + kitchens + "@test.example";
        mvc.perform(body(post("/api/account/register"), "{\"email\":\"" + merchant
                + "\",\"password\":\"" + People.PASSWORD + "\",\"displayName\":\"Kitchen "
                + kitchens + "\",\"phone\":\"+15550113\"}")).andExpect(status().isOk());
        String id = (String) json(mvc, People.asCustomer(body(post("/api/restaurants/application"),
                "{\"name\":\"Kitchen " + kitchens + "\",\"cuisine\":\"Pizza\",\"description\":\"Test\","
                + "\"address\":\"Here\",\"lat\":" + lat + ",\"lng\":" + lng + ",\"opensAt\":0,"
                + "\"closesAt\":0,\"prepMinutes\":10,\"minimumCents\":0,\"priceLevel\":2,"
                + "\"photoId\":\"\"}"), merchant)).get("id");
        mvc.perform(People.asAdmin(post("/api/admin/restaurants/" + id + "/approve"), DemoAccounts.ADMIN))
                .andExpect(status().isOk());
        String section = (String) json(mvc, People.asMerchant(body(post("/api/merchant/sections"),
                "{\"name\":\"Mains\",\"dishes\":[]}"), merchant)).get("id");
        String dish = (String) json(mvc, People.asMerchant(body(post("/api/merchant/dishes"),
                "{\"sectionId\":\"" + section + "\",\"name\":\"Margherita\",\"description\":\"\","
                + "\"priceCents\":" + DISH_CENTS + ",\"photoId\":\"\",\"available\":true,"
                + "\"popular\":false,\"options\":[]}"), merchant)).get("id");
        Kitchen made = new Kitchen(merchant, id, dish, lat, lng);
        KITCHENS.put(key, made);
        return made;
    }

    /// The cart of one dish from the trip's kitchen to its door.
    public static String cart(MockMvc mvc, Trip trip) throws Exception {
        Kitchen at = kitchen(mvc, trip.fromLat, trip.fromLng);
        return "{\"restaurantId\":\"" + at.id + "\",\"lines\":[{\"dishId\":\"" + at.dish
                + "\",\"quantity\":1,\"optionIds\":[]}],\"dropoffLat\":" + trip.toLat
                + ",\"dropoffLng\":" + trip.toLng + ",\"dropoffAddress\":\"There\",\"note\":\"\""
                + (trip.extra.length() == 0 ? "" : "," + trip.extra) + "}";
    }

    /// Places the order and returns it, waiting for its kitchen.
    public static Map place(MockMvc mvc, String customer, Trip trip) throws Exception {
        return json(mvc, People.asCustomer(body(post("/api/orders"), cart(mvc, trip)), customer));
    }

    /// The kitchen takes the order, which sends for a courier, and cooks it.
    public static void cook(MockMvc mvc, Trip trip, String id) throws Exception {
        Kitchen at = kitchen(mvc, trip.fromLat, trip.fromLng);
        mvc.perform(People.asMerchant(body(post("/api/merchant/orders/" + id + "/accept"),
                "{\"prepMinutes\":10}"), at.merchant)).andExpect(status().isOk());
        mvc.perform(People.asMerchant(post("/api/merchant/orders/" + id + "/ready"), at.merchant))
                .andExpect(status().isOk());
    }

    /// Places an order and has its kitchen take it and cook it, so that a
    /// courier is being looked for. Returns its id, which its delivery shares.
    public static String ask(MockMvc mvc, String customer, Trip trip) throws Exception {
        String id = (String) place(mvc, customer, trip).get("id");
        cook(mvc, trip, id);
        return id;
    }

    public static void online(MockMvc mvc, String courier, double lat, double lng) throws Exception {
        mvc.perform(People.asCourier(body(post("/api/courier/status"),
                People.position(true, lat, lng)), courier)).andExpect(status().isOk());
    }

    public static void offline(MockMvc mvc, String courier) throws Exception {
        mvc.perform(People.asCourier(body(post("/api/courier/status"),
                People.position(false, 0, 0)), courier)).andExpect(status().isOk());
    }

    public static String card(String number) {
        // A few years off whenever this runs: a card is refused both when it
        // has expired and when its date is further away than any card's is.
        int year = Integer.parseInt(Days.iso(System.currentTimeMillis()).substring(0, 4)) + 3;
        return "{\"number\":\"" + number + "\",\"expMonth\":12,\"expYear\":" + year
                + ",\"cvc\":\"123\",\"holder\":\"A Customer\"}";
    }

    /// Saves a card and returns it as the server describes it.
    public static Map addCard(MockMvc mvc, String customer, String number) throws Exception {
        Map setup = json(mvc, People.asCustomer(post("/api/payments/methods/setup"), customer));
        return json(mvc, People.asCustomer(body(post("/api/payments/methods/setup/" + setup.get("id")
                + "/complete"), card(number)), customer));
    }

    /// Orders and has `courier`, who is on line beside the kitchen, take it to
    /// the door. Returns the delivery as the courier is shown it on completing.
    public static Map complete(MockMvc mvc, String customer, String courier, Trip trip) throws Exception {
        String id = ask(mvc, customer, trip);
        mvc.perform(People.asCourier(post("/api/courier/offers/" + id + "/accept"), courier))
                .andExpect(status().isOk());
        mvc.perform(People.asCourier(post("/api/courier/deliveries/" + id + "/arrived"), courier))
                .andExpect(status().isOk());
        mvc.perform(People.asCourier(post("/api/courier/deliveries/" + id + "/start"), courier))
                .andExpect(status().isOk());
        return json(mvc, People.asCourier(post("/api/courier/deliveries/" + id + "/complete"), courier));
    }

    public static Map receipt(MockMvc mvc, String customer, String orderId) throws Exception {
        return json(mvc, People.asCustomer(get("/api/payments/receipts/" + orderId), customer));
    }
}
