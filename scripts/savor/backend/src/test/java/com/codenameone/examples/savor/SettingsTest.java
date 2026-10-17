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

import com.codename1.backend.annotations.Autowired;
import com.codename1.backend.test.BackendTest;
import com.codename1.backend.test.MockMvc;
import com.codenameone.examples.savor.account.Accounts;
import com.codenameone.examples.savor.account.DemoAccounts;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static com.codename1.backend.test.MockMvcRequestBuilders.get;
import static com.codename1.backend.test.MockMvcRequestBuilders.post;
import static com.codename1.backend.test.MockMvcRequestBuilders.put;
import static com.codename1.backend.test.MockMvcResultMatchers.jsonPath;
import static com.codename1.backend.test.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertEquals;

/// What someone changes about their own account: how the app behaves for them,
/// who they are, their password, and whether they have an account at all.
@BackendTest
class SettingsTest {
    @Autowired
    private MockMvc mvc;
    @Autowired
    private Accounts accounts;

    @Test
    void preferencesAreSavedAndReadBack() throws Exception {
        String who = People.customer(accounts, "prefers");
        mvc.perform(People.asCustomer(get("/api/account/preferences"), who))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.language").value("en"))
                .andExpect(jsonPath("$.theme").value("system"))
                .andExpect(jsonPath("$.units").value("km"))
                .andExpect(jsonPath("$.notifyDeliveryUpdates").value(true))
                .andExpect(jsonPath("$.courierGender").value("any"))
                .andExpect(jsonPath("$.defaultPaymentMethodId").value("cash"));

        String card = (String) Trips.addCard(mvc, who, Trips.VISA).get("id");
        String wanted = "{\"language\":\"he\",\"theme\":\"dark\",\"units\":\"mi\","
                + "\"notifyDeliveryUpdates\":false,\"notifyReceipts\":true,\"notifyPromotions\":true,"
                + "\"courierGender\":\"women\",\"quietDelivery\":true,\"accessibleVehicle\":true,"
                + "\"petFriendly\":true,\"defaultPaymentMethodId\":\"cash\","
                + "\"defaultTipPercent\":15,\"shareTripsWith\":\"+1 (555) 010-2222\"}";
        mvc.perform(People.asCustomer(Trips.body(put("/api/account/preferences"), wanted), who))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.language").value("he"));
        Map saved = Trips.json(mvc, People.asCustomer(get("/api/account/preferences"), who));
        assertEquals("he", saved.get("language"));
        assertEquals("dark", saved.get("theme"));
        assertEquals("mi", saved.get("units"));
        assertEquals(Boolean.FALSE, saved.get("notifyDeliveryUpdates"));
        assertEquals(Boolean.TRUE, saved.get("notifyReceipts"));
        assertEquals(Boolean.TRUE, saved.get("notifyPromotions"));
        assertEquals("women", saved.get("courierGender"));
        assertEquals(Boolean.TRUE, saved.get("quietDelivery"));
        assertEquals(Boolean.TRUE, saved.get("accessibleVehicle"));
        assertEquals(Boolean.TRUE, saved.get("petFriendly"));
        assertEquals(15L, Trips.number(saved, "defaultTipPercent"));
        // Stored in the one form a phone number has.
        assertEquals("+15550102222", saved.get("shareTripsWith"));
        // The default way to pay is the mark on the card itself: saving it
        // here and reading the cards agree.
        assertEquals("cash", saved.get("defaultPaymentMethodId"));
        mvc.perform(People.asCustomer(get("/api/payments/methods"), who))
                .andExpect(jsonPath("$[0].id").value(card))
                .andExpect(jsonPath("$[0].isDefault").value(false))
                .andExpect(jsonPath("$[1].isDefault").value(true));
        mvc.perform(People.asCustomer(get("/api/me"), who))
                .andExpect(jsonPath("$.language").value("he"));

        // Each is one of the values it can take, or it is refused whole.
        String[] wrong = {"{\"language\":\"xx\"}", "{\"theme\":\"neon\"}", "{\"units\":\"ft\"}",
            "{\"courierGender\":\"men\"}", "{\"defaultTipPercent\":12}",
            "{\"shareTripsWith\":\"not a number\"}",
            "{\"defaultPaymentMethodId\":\"someoneelsescard\"}"};
        for (int iter = 0; iter < wrong.length; iter++) {
            int answered = Trips.statusOf(mvc, People.asCustomer(Trips.body(
                    put("/api/account/preferences"), wrong[iter]), who));
            assertEquals(iter == wrong.length - 1 ? 404 : 400, answered, wrong[iter]);
        }
        mvc.perform(People.asCustomer(get("/api/account/preferences"), who))
                .andExpect(jsonPath("$.language").value("he"))
                .andExpect(jsonPath("$.theme").value("dark"));
    }

    @Test
    void aProfileIsSavedAndReadBack() throws Exception {
        String who = People.customer(accounts, "profiled");
        mvc.perform(People.asCustomer(get("/api/account/profile"), who))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("profiled"))
                .andExpect(jsonPath("$.gender").value("unspecified"));
        mvc.perform(People.asCustomer(Trips.body(put("/api/account/profile"),
                        "{\"name\":\"Pat Profiled\",\"gender\":\"nonbinary\","
                                + "\"emergencyContactName\":\"Sam\","
                                + "\"emergencyContactPhone\":\"+1 555 010 3333\","
                                + "\"homeAddress\":\"1 Home St\",\"workAddress\":\"2 Work Ave\"}"),
                        who))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Pat Profiled"))
                .andExpect(jsonPath("$.gender").value("nonbinary"));
        mvc.perform(People.asCustomer(get("/api/account/profile"), who))
                .andExpect(jsonPath("$.name").value("Pat Profiled"))
                .andExpect(jsonPath("$.emergencyContactName").value("Sam"))
                .andExpect(jsonPath("$.emergencyContactPhone").value("+15550103333"))
                .andExpect(jsonPath("$.homeAddress").value("1 Home St"))
                .andExpect(jsonPath("$.workAddress").value("2 Work Ave"));
        mvc.perform(People.asCustomer(Trips.body(put("/api/account/profile"),
                        "{\"name\":\"\",\"gender\":\"female\"}"), who))
                .andExpect(status().isBadRequest());
        mvc.perform(People.asCustomer(Trips.body(put("/api/account/profile"),
                        "{\"name\":\"Pat\",\"gender\":\"other\"}"), who))
                .andExpect(status().isBadRequest());
    }

    @Test
    void aPasswordIsChangedByWhoeverKnowsTheOldOne() throws Exception {
        String who = People.customer(accounts, "rekeyed");
        mvc.perform(People.asCustomer(Trips.body(post("/api/account/password"),
                        "{\"current\":\"not-the-password\",\"replacement\":\"another-long-one\"}"),
                        who))
                .andExpect(status().isForbidden());
        mvc.perform(People.asCustomer(Trips.body(post("/api/account/password"),
                        "{\"current\":\"" + People.PASSWORD + "\",\"replacement\":\"short\"}"), who))
                .andExpect(status().isBadRequest());
        mvc.perform(People.asCustomer(Trips.body(post("/api/account/password"),
                        "{\"current\":\"" + People.PASSWORD
                                + "\",\"replacement\":\"another-long-one\"}"), who))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(who));
        assertEquals(null, SignIn.session(mvc, who, People.PASSWORD));
        assertEquals(false, SignIn.session(mvc, who, "another-long-one") == null);
    }

    @Test
    void aChangedPasswordSignsTheOtherDevicesOut() throws Exception {
        String who = People.customer(accounts, "two-devices");
        Map phone = SignIn.tokens(mvc, who, People.PASSWORD);
        Map tablet = SignIn.tokens(mvc, who, People.PASSWORD);

        mvc.perform(Trips.body(post("/api/account/password"),
                        "{\"current\":\"" + People.PASSWORD
                                + "\",\"replacement\":\"another-long-one\"}")
                        .header("Host", SignIn.HOST)
                        .header("Authorization", "Bearer " + phone.get("access_token")))
                .andExpect(status().isOk());

        // The tablet's refresh token asked for no password, and would have
        // gone on working for whoever held the tablet.
        mvc.perform(SignIn.formPost("/oauth2/token", "grant_type", "refresh_token",
                        "refresh_token", (String) tablet.get("refresh_token"),
                        "client_id", SecurityConfig.CLIENT_ID).header("Host", SignIn.HOST))
                .andExpect(status().isBadRequest());
        // The device the password was changed on stays signed in.
        mvc.perform(SignIn.formPost("/oauth2/token", "grant_type", "refresh_token",
                        "refresh_token", (String) phone.get("refresh_token"),
                        "client_id", SecurityConfig.CLIENT_ID).header("Host", SignIn.HOST))
                .andExpect(status().isOk());
    }

    @Test
    void anAccountIsClosedForGood() throws Exception {
        // Warsaw, where nobody drives.
        String who = People.customer(accounts, "leaving");
        String token = SignIn.token(mvc, who, People.PASSWORD);
        Trips.addCard(mvc, who, Trips.VISA);
        mvc.perform(People.asCustomer(Trips.body(put("/api/onboarding/application"),
                "{\"legalName\":\"Lee Leaving\"}"), who)).andExpect(status().isOk());

        // Not with a delivery under way: somebody may be onboarding to it.
        String delivery = (String) Trips.json(mvc, People.asCustomer(Trips.body(post("/api/deliveries"),
                People.delivery(52.2297, 21.0122, 52.2400, 21.0300)), who)).get("id");
        String closure = "{\"password\":\"" + People.PASSWORD + "\"}";
        mvc.perform(People.asCustomer(Trips.body(post("/api/account/delete"), closure), who))
                .andExpect(status().isConflict());
        mvc.perform(People.asCustomer(post("/api/deliveries/" + delivery + "/cancel"), who))
                .andExpect(status().isOk());

        // Not on the strength of a token: the password is asked for again.
        mvc.perform(People.asCustomer(post("/api/account/delete"), who))
                .andExpect(status().isBadRequest());
        mvc.perform(People.asCustomer(Trips.body(post("/api/account/delete"), "{}"), who))
                .andExpect(status().isForbidden());
        mvc.perform(People.asCustomer(Trips.body(post("/api/account/delete"),
                        "{\"password\":\"not-the-password\"}"), who))
                .andExpect(status().isForbidden());
        mvc.perform(People.asCustomer(get("/api/me"), who)).andExpect(status().isOk());

        mvc.perform(People.asCustomer(Trips.body(post("/api/account/delete"), closure), who))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(who));
        // Gone: the token, the password, and the account an admin would look up.
        mvc.perform(get("/api/me").header("Host", SignIn.HOST)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
        assertEquals(null, SignIn.session(mvc, who, People.PASSWORD));
        mvc.perform(People.asAdmin(get("/api/admin/users/" + who), DemoAccounts.ADMIN))
                .andExpect(status().isNotFound());
        mvc.perform(People.asAdmin(get("/api/admin/applications/" + who), DemoAccounts.ADMIN))
                .andExpect(status().isNotFound());
        // And the address is retired with it. The deliveries the account took are
        // kept under it, so whoever registered it next would be handed them.
        mvc.perform(post("/api/account/register").contentType("application/json")
                        .content("{\"email\":\"" + who + "\",\"password\":\"" + People.PASSWORD
                                + "\",\"displayName\":\"Somebody else\","
                                + "\"phone\":\"+15550112\"}"))
                .andExpect(status().isConflict());

        // An admin's account is not closed this way.
        mvc.perform(People.asAdmin(Trips.body(post("/api/account/delete"),
                        "{\"password\":\"" + DemoAccounts.PASSWORD + "\"}"), DemoAccounts.ADMIN))
                .andExpect(status().isConflict());
    }
}
