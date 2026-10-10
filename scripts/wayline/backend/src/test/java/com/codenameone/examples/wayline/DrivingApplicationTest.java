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
import com.codename1.backend.test.MockRequestBuilder;
import com.codenameone.examples.wayline.account.Accounts;
import com.codenameone.examples.wayline.account.DemoAccounts;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static com.codename1.backend.test.MockMvcRequestBuilders.get;
import static com.codename1.backend.test.MockMvcRequestBuilders.post;
import static com.codename1.backend.test.MockMvcRequestBuilders.put;
import static com.codename1.backend.test.MockMvcResultMatchers.jsonPath;
import static com.codename1.backend.test.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Applying to drive: a draft, its documents, handing it in, and an admin's
/// answer either way.
@BackendTest
class DrivingApplicationTest {
    /// The smallest things the server takes for a picture: the first bytes of a
    /// PNG and of a JPEG, in base64.
    private static final String PNG = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAAB";
    private static final String JPEG = "/9j/4AAQSkZJRgABAQAAAQABAAA=";
    private static final String[] KINDS = {"photo_id", "licence", "selfie", "vehicle_registration",
        "insurance"};
    private static final String FORM = "{\"legalName\":\"Ada Applicant\","
            + "\"dateOfBirth\":\"1990-05-17\",\"licenceNumber\":\"L-1234\","
            + "\"licenceExpiry\":\"2999-01-31\",\"vehicleMake\":\"Honda\","
            + "\"vehicleModel\":\"Civic\",\"vehicleYear\":2020,\"vehicleColor\":\"Grey\","
            + "\"vehiclePlate\":\"APP 1\",\"vehicleSeats\":4,\"product\":\"standard\"}";

    @Autowired
    private MockMvc mvc;
    @Autowired
    private Accounts accounts;

    private static String picture(String kind, String data) {
        return "{\"kind\":\"" + kind + "\",\"contentType\":\"image/png\",\"dataBase64\":\"" + data
                + "\"}";
    }

    private MockRequestBuilder admin(MockRequestBuilder request) {
        return People.asAdmin(request, DemoAccounts.ADMIN);
    }

    private void upload(String who, String kind) throws Exception {
        mvc.perform(People.asRider(Trips.body(post("/api/driving/application/documents"),
                picture(kind, PNG)), who)).andExpect(status().isOk());
    }

    @Test
    void anApplicationIsDraftedHandedInRejectedCorrectedAndApproved() throws Exception {
        String who = People.rider(accounts, "applies");
        String token = SignIn.token(mvc, who, People.PASSWORD);

        // Nothing yet, and nothing to hand in.
        mvc.perform(People.asRider(get("/api/driving/application"), who))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("none"))
                .andExpect(jsonPath("$.documents.length()").value(5))
                .andExpect(jsonPath("$.documents[0].status").value("missing"));
        mvc.perform(People.asRider(get("/api/me"), who))
                .andExpect(jsonPath("$.driverStatus").value("none"));

        // Half a form is a draft. It saves, and it cannot be handed in.
        mvc.perform(People.asRider(Trips.body(put("/api/driving/application"),
                        "{\"legalName\":\"Ada Applicant\",\"vehicleMake\":\"Honda\"}"), who))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("draft"))
                .andExpect(jsonPath("$.legalName").value("Ada Applicant"));
        mvc.perform(People.asRider(Trips.body(put("/api/driving/application"),
                        "{\"dateOfBirth\":\"17/05/1990\"}"), who))
                .andExpect(status().isBadRequest());
        mvc.perform(People.asRider(post("/api/driving/application/submit"), who))
                .andExpect(status().isBadRequest());

        // A complete form is still not complete without its documents.
        mvc.perform(People.asRider(Trips.body(put("/api/driving/application"), FORM), who))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vehicleYear").value(2020));
        mvc.perform(People.asRider(post("/api/driving/application/submit"), who))
                .andExpect(status().isBadRequest());

        // A document has to be a picture, of a kind that exists, of a sane size.
        mvc.perform(People.asRider(Trips.body(post("/api/driving/application/documents"),
                        picture("photo_id", "bm90IGEgcGljdHVyZQ==")), who))
                .andExpect(status().isBadRequest());
        mvc.perform(People.asRider(Trips.body(post("/api/driving/application/documents"),
                        picture("passport", PNG)), who))
                .andExpect(status().isBadRequest());
        StringBuilder huge = new StringBuilder(PNG);
        while (huge.length() < 2200000) {
            huge.append("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
        }
        mvc.perform(People.asRider(Trips.body(post("/api/driving/application/documents"),
                        picture("photo_id", huge.toString())), who))
                .andExpect(status().isPayloadTooLarge());

        for (int iter = 0; iter < 4; iter++) {
            upload(who, KINDS[iter]);
        }
        // Four of five.
        mvc.perform(People.asRider(post("/api/driving/application/submit"), who))
                .andExpect(status().isBadRequest());
        mvc.perform(People.asRider(Trips.body(post("/api/driving/application/documents"),
                        picture("insurance", JPEG)), who))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documents[4].kind").value("insurance"))
                .andExpect(jsonPath("$.documents[4].status").value("uploaded"));
        mvc.perform(People.asRider(get("/api/driving/application/documents/insurance"), who))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contentType").value("image/jpeg"))
                .andExpect(jsonPath("$.dataBase64").value(JPEG));

        mvc.perform(People.asRider(post("/api/driving/application/submit"), who))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("pending"));
        // In an admin's hands now: not the applicant's to change.
        mvc.perform(People.asRider(Trips.body(put("/api/driving/application"), FORM), who))
                .andExpect(status().isConflict());
        mvc.perform(People.asRider(Trips.body(post("/api/driving/application/documents"),
                        picture("selfie", PNG)), who))
                .andExpect(status().isConflict());
        // Still not a driver: by the role, and by the token already in hand.
        mvc.perform(People.asRider(get("/api/me"), who))
                .andExpect(jsonPath("$.driver").value(false))
                .andExpect(jsonPath("$.driverStatus").value("pending"));
        mvc.perform(get("/api/driver/me").header("Host", SignIn.HOST)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());

        // The admin finds it among those waiting, and reads a document.
        List waiting = Trips.list(mvc, admin(get("/api/admin/applications?status=pending")));
        boolean found = false;
        for (int iter = 0; iter < waiting.size(); iter++) {
            found |= who.equals(((Map) waiting.get(iter)).get("username"));
        }
        assertTrue(found, "the application is waiting");
        mvc.perform(admin(get("/api/admin/applications?status=nonsense")))
                .andExpect(status().isBadRequest());
        mvc.perform(admin(get("/api/admin/applications/" + who)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("applies"))
                .andExpect(jsonPath("$.licenceNumber").value("L-1234"));
        mvc.perform(admin(get("/api/admin/applications/" + who + "/documents/licence")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dataBase64").value(PNG));
        mvc.perform(admin(get("/api/admin/applications/nobody@test.example")))
                .andExpect(status().isNotFound());

        // Rejected, which needs a reason, and the applicant is told it.
        mvc.perform(admin(Trips.body(post("/api/admin/applications/" + who + "/reject"), "{}")))
                .andExpect(status().isBadRequest());
        mvc.perform(admin(Trips.body(post("/api/admin/applications/" + who + "/reject"),
                        "{\"reason\":\"The licence photo is unreadable\"}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("rejected"))
                .andExpect(jsonPath("$.reviewedBy").value(DemoAccounts.ADMIN));
        mvc.perform(admin(post("/api/admin/applications/" + who + "/approve")))
                .andExpect(status().isConflict());
        mvc.perform(People.asRider(get("/api/driving/application"), who))
                .andExpect(jsonPath("$.status").value("rejected"))
                .andExpect(jsonPath("$.rejectionReason").value("The licence photo is unreadable"));

        // Corrected and handed in again, and this time approved.
        upload(who, "licence");
        mvc.perform(People.asRider(post("/api/driving/application/submit"), who))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("pending"))
                .andExpect(jsonPath("$.rejectionReason").value(""));
        mvc.perform(admin(post("/api/admin/applications/" + who + "/approve")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("approved"))
                .andExpect(jsonPath("$.documents[0].status").value("accepted"));
        mvc.perform(admin(post("/api/admin/applications/" + who + "/approve")))
                .andExpect(status().isConflict());

        // Approval is what grants the role: the same token now drives, in the
        // car the application named.
        mvc.perform(get("/api/driver/me").header("Host", SignIn.HOST)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.approved").value(true))
                .andExpect(jsonPath("$.vehicle").value("Grey Honda Civic"))
                .andExpect(jsonPath("$.plate").value("APP 1"))
                .andExpect(jsonPath("$.product").value("standard"));
        mvc.perform(People.asRider(get("/api/me"), who))
                .andExpect(jsonPath("$.driver").value(true))
                .andExpect(jsonPath("$.rider").value(true))
                .andExpect(jsonPath("$.driverStatus").value("approved"));
        // Both answers are on the account's record.
        Map detail = Trips.json(mvc, admin(get("/api/admin/users/" + who)));
        List events = (List) detail.get("events");
        assertEquals(2, events.size());
        assertEquals("approve", ((Map) events.get(0)).get("action"));
        assertEquals("reject", ((Map) events.get(1)).get("action"));
        assertEquals("The licence photo is unreadable", ((Map) events.get(1)).get("reason"));
    }

    @Test
    void onlyAnApprovedDriverGoesOnLine() throws Exception {
        // Holding the role is not enough. This account is let into the
        // driver's part of the API and has an application nobody has approved.
        String who = People.rider(accounts, "unapproved");
        mvc.perform(People.asRider(Trips.body(put("/api/driving/application"), FORM), who))
                .andExpect(status().isOk());
        mvc.perform(People.asDriver(Trips.body(post("/api/driver/status"),
                        People.position(true, 59.9139, 10.7522)), who))
                .andExpect(status().isForbidden());
        mvc.perform(People.asDriver(get("/api/driver/me"), who))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.approved").value(false))
                .andExpect(jsonPath("$.online").value(false));

        // An admin's switch approves, and withdrawing it takes that back.
        mvc.perform(admin(Trips.body(post("/api/admin/users/" + who + "/roles"),
                        "{\"driver\":true,\"admin\":false}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.driverStatus").value("approved"));
        Trips.online(mvc, who, 59.9139, 10.7522);
        mvc.perform(admin(Trips.body(post("/api/admin/users/" + who + "/roles"),
                        "{\"driver\":false,\"admin\":false}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.driver").value(false))
                .andExpect(jsonPath("$.driverStatus").value("rejected"));
        mvc.perform(People.asDriver(Trips.body(post("/api/driver/status"),
                        People.position(true, 59.9139, 10.7522)), who))
                .andExpect(status().isForbidden());
        mvc.perform(People.asDriver(get("/api/driver/me"), who))
                .andExpect(jsonPath("$.online").value(false));
    }

    @Test
    void anApplicationIsItsOwnersAndTheAdminsOnly() throws Exception {
        String owner = People.rider(accounts, "doc-owner");
        String other = People.rider(accounts, "doc-other");
        upload(owner, "photo_id");
        // There is no address by which one applicant reaches another's: each
        // sees their own, and the other has none.
        mvc.perform(People.asRider(get("/api/driving/application/documents/photo_id"), other))
                .andExpect(status().isNotFound());
        mvc.perform(People.asRider(get("/api/driving/application"), other))
                .andExpect(jsonPath("$.status").value("none"))
                .andExpect(jsonPath("$.username").value(other));
        mvc.perform(People.asRider(get("/api/admin/applications/" + owner
                        + "/documents/photo_id"), other))
                .andExpect(status().isForbidden());
        mvc.perform(People.asDriver(get("/api/admin/applications/" + owner), other))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/driving/application/documents/photo_id"))
                .andExpect(status().isUnauthorized());
        mvc.perform(admin(get("/api/admin/applications/" + owner + "/documents/photo_id")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kind").value("photo_id"));
        mvc.perform(admin(get("/api/admin/applications/" + owner + "/documents/selfie")))
                .andExpect(status().isNotFound());
    }

    @Test
    void theDemoApplicantIsWaitingAndTheDemoDriversAreApproved() throws Exception {
        mvc.perform(admin(get("/api/admin/applications/" + DemoAccounts.APPLICANT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("pending"))
                .andExpect(jsonPath("$.documents[2].status").value("uploaded"));
        mvc.perform(admin(get("/api/admin/applications/" + DemoAccounts.DRIVER)))
                .andExpect(jsonPath("$.status").value("approved"))
                .andExpect(jsonPath("$.vehicleMake").value("Toyota"));
        mvc.perform(admin(get("/api/admin/applications/" + DemoAccounts.XL_DRIVER)))
                .andExpect(jsonPath("$.product").value("xl"))
                .andExpect(jsonPath("$.accessible").value(true));
        // Registering to drive, the way the app asks, opens a draft.
        mvc.perform(post("/api/account/register").contentType("application/json")
                        .content("{\"email\":\"wants@test.example\",\"password\":\"a-long-password\","
                                + "\"displayName\":\"Wants\",\"phone\":\"+15550113\","
                                + "\"wantsToDrive\":true,\"gender\":\"female\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.driver").value(false))
                .andExpect(jsonPath("$.gender").value("female"))
                .andExpect(jsonPath("$.driverStatus").value("draft"));
    }
}
