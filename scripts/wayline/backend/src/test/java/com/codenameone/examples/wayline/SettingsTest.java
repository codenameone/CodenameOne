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

import com.codename1.backend.annotations.Autowired;
import com.codename1.backend.test.BackendTest;
import com.codename1.backend.test.MockMvc;
import com.codenameone.examples.wayline.account.Accounts;
import com.codenameone.examples.wayline.account.DemoAccounts;
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
        String who = People.rider(accounts, "prefers");
        mvc.perform(People.asRider(get("/api/account/preferences"), who))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.language").value("en"))
                .andExpect(jsonPath("$.theme").value("system"))
                .andExpect(jsonPath("$.units").value("km"))
                .andExpect(jsonPath("$.notifyRideUpdates").value(true))
                .andExpect(jsonPath("$.driverGender").value("any"))
                .andExpect(jsonPath("$.defaultPaymentMethodId").value("cash"));

        String card = (String) Trips.addCard(mvc, who, Trips.VISA).get("id");
        String wanted = "{\"language\":\"he\",\"theme\":\"dark\",\"units\":\"mi\","
                + "\"notifyRideUpdates\":false,\"notifyReceipts\":true,\"notifyPromotions\":true,"
                + "\"driverGender\":\"women\",\"quietRide\":true,\"accessibleVehicle\":true,"
                + "\"petFriendly\":true,\"defaultPaymentMethodId\":\"cash\","
                + "\"defaultTipPercent\":15,\"shareTripsWith\":\"+1 (555) 010-2222\"}";
        mvc.perform(People.asRider(Trips.body(put("/api/account/preferences"), wanted), who))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.language").value("he"));
        Map saved = Trips.json(mvc, People.asRider(get("/api/account/preferences"), who));
        assertEquals("he", saved.get("language"));
        assertEquals("dark", saved.get("theme"));
        assertEquals("mi", saved.get("units"));
        assertEquals(Boolean.FALSE, saved.get("notifyRideUpdates"));
        assertEquals(Boolean.TRUE, saved.get("notifyReceipts"));
        assertEquals(Boolean.TRUE, saved.get("notifyPromotions"));
        assertEquals("women", saved.get("driverGender"));
        assertEquals(Boolean.TRUE, saved.get("quietRide"));
        assertEquals(Boolean.TRUE, saved.get("accessibleVehicle"));
        assertEquals(Boolean.TRUE, saved.get("petFriendly"));
        assertEquals(15L, Trips.number(saved, "defaultTipPercent"));
        // Stored in the one form a phone number has.
        assertEquals("+15550102222", saved.get("shareTripsWith"));
        // The default way to pay is the mark on the card itself: saving it
        // here and reading the cards agree.
        assertEquals("cash", saved.get("defaultPaymentMethodId"));
        mvc.perform(People.asRider(get("/api/payments/methods"), who))
                .andExpect(jsonPath("$[0].id").value(card))
                .andExpect(jsonPath("$[0].isDefault").value(false))
                .andExpect(jsonPath("$[1].isDefault").value(true));
        mvc.perform(People.asRider(get("/api/me"), who))
                .andExpect(jsonPath("$.language").value("he"));

        // Each is one of the values it can take, or it is refused whole.
        String[] wrong = {"{\"language\":\"xx\"}", "{\"theme\":\"neon\"}", "{\"units\":\"ft\"}",
            "{\"driverGender\":\"men\"}", "{\"defaultTipPercent\":12}",
            "{\"shareTripsWith\":\"not a number\"}",
            "{\"defaultPaymentMethodId\":\"someoneelsescard\"}"};
        for (int iter = 0; iter < wrong.length; iter++) {
            int answered = Trips.statusOf(mvc, People.asRider(Trips.body(
                    put("/api/account/preferences"), wrong[iter]), who));
            assertEquals(iter == wrong.length - 1 ? 404 : 400, answered, wrong[iter]);
        }
        mvc.perform(People.asRider(get("/api/account/preferences"), who))
                .andExpect(jsonPath("$.language").value("he"))
                .andExpect(jsonPath("$.theme").value("dark"));
    }

    @Test
    void aProfileIsSavedAndReadBack() throws Exception {
        String who = People.rider(accounts, "profiled");
        mvc.perform(People.asRider(get("/api/account/profile"), who))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("profiled"))
                .andExpect(jsonPath("$.gender").value("unspecified"));
        mvc.perform(People.asRider(Trips.body(put("/api/account/profile"),
                        "{\"name\":\"Pat Profiled\",\"gender\":\"nonbinary\","
                                + "\"emergencyContactName\":\"Sam\","
                                + "\"emergencyContactPhone\":\"+1 555 010 3333\","
                                + "\"homeAddress\":\"1 Home St\",\"workAddress\":\"2 Work Ave\"}"),
                        who))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Pat Profiled"))
                .andExpect(jsonPath("$.gender").value("nonbinary"));
        mvc.perform(People.asRider(get("/api/account/profile"), who))
                .andExpect(jsonPath("$.name").value("Pat Profiled"))
                .andExpect(jsonPath("$.emergencyContactName").value("Sam"))
                .andExpect(jsonPath("$.emergencyContactPhone").value("+15550103333"))
                .andExpect(jsonPath("$.homeAddress").value("1 Home St"))
                .andExpect(jsonPath("$.workAddress").value("2 Work Ave"));
        mvc.perform(People.asRider(Trips.body(put("/api/account/profile"),
                        "{\"name\":\"\",\"gender\":\"female\"}"), who))
                .andExpect(status().isBadRequest());
        mvc.perform(People.asRider(Trips.body(put("/api/account/profile"),
                        "{\"name\":\"Pat\",\"gender\":\"other\"}"), who))
                .andExpect(status().isBadRequest());
    }

    @Test
    void aPasswordIsChangedByWhoeverKnowsTheOldOne() throws Exception {
        String who = People.rider(accounts, "rekeyed");
        mvc.perform(People.asRider(Trips.body(post("/api/account/password"),
                        "{\"current\":\"not-the-password\",\"replacement\":\"another-long-one\"}"),
                        who))
                .andExpect(status().isForbidden());
        mvc.perform(People.asRider(Trips.body(post("/api/account/password"),
                        "{\"current\":\"" + People.PASSWORD + "\",\"replacement\":\"short\"}"), who))
                .andExpect(status().isBadRequest());
        mvc.perform(People.asRider(Trips.body(post("/api/account/password"),
                        "{\"current\":\"" + People.PASSWORD
                                + "\",\"replacement\":\"another-long-one\"}"), who))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(who));
        assertEquals(null, SignIn.session(mvc, who, People.PASSWORD));
        assertEquals(false, SignIn.session(mvc, who, "another-long-one") == null);
    }

    @Test
    void anAccountIsClosedForGood() throws Exception {
        // Warsaw, where nobody drives.
        String who = People.rider(accounts, "leaving");
        String token = SignIn.token(mvc, who, People.PASSWORD);
        Trips.addCard(mvc, who, Trips.VISA);
        mvc.perform(People.asRider(Trips.body(put("/api/driving/application"),
                "{\"legalName\":\"Lee Leaving\"}"), who)).andExpect(status().isOk());

        // Not with a ride under way: somebody may be driving to it.
        String ride = (String) Trips.json(mvc, People.asRider(Trips.body(post("/api/rides"),
                People.ride(52.2297, 21.0122, 52.2400, 21.0300)), who)).get("id");
        mvc.perform(People.asRider(post("/api/account/delete"), who))
                .andExpect(status().isConflict());
        mvc.perform(People.asRider(post("/api/rides/" + ride + "/cancel"), who))
                .andExpect(status().isOk());

        mvc.perform(People.asRider(post("/api/account/delete"), who))
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
        // And the address is free for whoever wants an account with it next,
        // who starts with nothing of the last one's.
        String again = People.rider(accounts, "leaving");
        assertEquals(who, again);
        mvc.perform(People.asRider(get("/api/payments/methods"), again))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value("cash"));
        mvc.perform(People.asRider(get("/api/driving/application"), again))
                .andExpect(jsonPath("$.status").value("none"));

        // An admin's account is not closed this way.
        mvc.perform(People.asAdmin(post("/api/account/delete"), DemoAccounts.ADMIN))
                .andExpect(status().isConflict());
    }
}
