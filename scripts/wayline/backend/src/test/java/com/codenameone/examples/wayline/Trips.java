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

import com.codename1.backend.Json;
import com.codename1.backend.test.MockMvc;
import com.codename1.backend.test.MockRequestBuilder;

import java.util.List;
import java.util.Map;

import static com.codename1.backend.test.MockMvcRequestBuilders.get;
import static com.codename1.backend.test.MockMvcRequestBuilders.post;
import static com.codename1.backend.test.MockMvcResultMatchers.status;

/// The steps several tests take on the way to what they are about: a driver
/// going on line, a card being saved, a ride being driven to its end.
final class Trips {
    static final String VISA = "4242424242424242";
    static final String MASTERCARD = "5555555555554444";
    /// Refused when it is saved.
    static final String DECLINED = "4000000000000002";
    /// Saved without complaint, and refused when it is charged.
    static final String FAILS_WHEN_CHARGED = "4000000000000341";

    private Trips() {
    }

    static MockRequestBuilder body(MockRequestBuilder request, String json) {
        return request.contentType("application/json").content(json);
    }

    /// The object a request that succeeds answers with.
    static Map json(MockMvc mvc, MockRequestBuilder request) throws Exception {
        return Json.parseObject(mvc.perform(request).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
    }

    /// The list a request that succeeds answers with.
    static List list(MockMvc mvc, MockRequestBuilder request) throws Exception {
        return (List) Json.parse(mvc.perform(request).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
    }

    /// The status a request is answered with, whatever it is.
    static int statusOf(MockMvc mvc, MockRequestBuilder request) throws Exception {
        return mvc.perform(request).andReturn().getResponse().getStatus();
    }

    static long number(Map object, String field) {
        return ((Number) object.get(field)).longValue();
    }

    /// A ride request with more said about it: `extra` is further members of
    /// the object, as JSON, without the braces.
    static String ride(double fromLat, double fromLng, double toLat, double toLng, String extra) {
        String plain = People.ride(fromLat, fromLng, toLat, toLng);
        return plain.substring(0, plain.length() - 1) + "," + extra + "}";
    }

    static void online(MockMvc mvc, String driver, double lat, double lng) throws Exception {
        mvc.perform(People.asDriver(body(post("/api/driver/status"),
                People.position(true, lat, lng)), driver)).andExpect(status().isOk());
    }

    static void offline(MockMvc mvc, String driver) throws Exception {
        mvc.perform(People.asDriver(body(post("/api/driver/status"),
                People.position(false, 0, 0)), driver)).andExpect(status().isOk());
    }

    static String card(String number) {
        // A few years off whenever this runs: a card is refused both when it
        // has expired and when its date is further away than any card's is.
        int year = Integer.parseInt(Days.iso(System.currentTimeMillis()).substring(0, 4)) + 3;
        return "{\"number\":\"" + number + "\",\"expMonth\":12,\"expYear\":" + year
                + ",\"cvc\":\"123\",\"holder\":\"A Rider\"}";
    }

    /// Saves a card and returns it as the server describes it.
    static Map addCard(MockMvc mvc, String rider, String number) throws Exception {
        Map setup = json(mvc, People.asRider(post("/api/payments/methods/setup"), rider));
        return json(mvc, People.asRider(body(post("/api/payments/methods/setup/" + setup.get("id")
                + "/complete"), card(number)), rider));
    }

    /// Asks for a ride and has `driver`, who is on line beside the pickup, take
    /// it to its end. Returns the ride as the driver is shown it on completing.
    static Map complete(MockMvc mvc, String rider, String driver, String ride) throws Exception {
        String id = (String) json(mvc, People.asRider(body(post("/api/rides"), ride), rider))
                .get("id");
        mvc.perform(People.asDriver(post("/api/driver/offers/" + id + "/accept"), driver))
                .andExpect(status().isOk());
        mvc.perform(People.asDriver(post("/api/driver/rides/" + id + "/arrived"), driver))
                .andExpect(status().isOk());
        mvc.perform(People.asDriver(post("/api/driver/rides/" + id + "/start"), driver))
                .andExpect(status().isOk());
        return json(mvc, People.asDriver(post("/api/driver/rides/" + id + "/complete"), driver));
    }

    static Map receipt(MockMvc mvc, String rider, String rideId) throws Exception {
        return json(mvc, People.asRider(get("/api/payments/receipts/" + rideId), rider));
    }
}
