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

import com.codename1.backend.Json;
import com.codename1.backend.test.MockMvc;
import com.codename1.backend.test.MockRequestBuilder;

import java.util.List;
import java.util.Map;

import static com.codename1.backend.test.MockMvcRequestBuilders.post;
import static com.codename1.backend.test.MockMvcResultMatchers.status;

/// The steps several tests take on the way to what they are about: a request
/// made and its answer read, a card being saved.
public final class Rest {
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

    private Rest() {
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
        return Rows.number(object.get(field));
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
}
