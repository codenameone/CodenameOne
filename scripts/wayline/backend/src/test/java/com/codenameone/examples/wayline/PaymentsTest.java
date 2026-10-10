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

/// Paying: the cards an account keeps, the charge when a ride completes, the
/// tip after it, the refund an admin can make, and what all of it earns the
/// driver. Against the simulated processor, which is what a server with no
/// key configured runs.
@BackendTest
class PaymentsTest {
    @Autowired
    private MockMvc mvc;
    @Autowired
    private Accounts accounts;

    private MockRequestBuilder admin(MockRequestBuilder request) {
        return People.asAdmin(request, DemoAccounts.ADMIN);
    }

    @Test
    void cardsAreSavedChosenAndRemoved() throws Exception {
        String rider = People.rider(accounts, "card-holder");
        String other = People.rider(accounts, "card-other");
        mvc.perform(People.asRider(get("/api/payments/config"), rider))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider").value("simulated"))
                .andExpect(jsonPath("$.currency").value("USD"));
        // Cash, always, and the default until there is a card.
        mvc.perform(People.asRider(get("/api/payments/methods"), rider))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value("cash"))
                .andExpect(jsonPath("$[0].kind").value("cash"))
                .andExpect(jsonPath("$[0].isDefault").value(true));

        // A card that is refused is not saved, and says why; the same setup
        // then takes a card that is good.
        Map setup = Trips.json(mvc, People.asRider(post("/api/payments/methods/setup"), rider));
        assertEquals(Boolean.FALSE, setup.get("hosted"));
        String complete = "/api/payments/methods/setup/" + setup.get("id") + "/complete";
        mvc.perform(People.asRider(Trips.body(post(complete), Trips.card(Trips.DECLINED)), rider))
                .andExpect(status().is(402));
        mvc.perform(People.asRider(Trips.body(post(complete), Trips.card("4242424242424241")),
                        rider))
                .andExpect(status().is(402));
        // Somebody else cannot finish it for them.
        mvc.perform(People.asRider(Trips.body(post(complete), Trips.card(Trips.VISA)), other))
                .andExpect(status().isNotFound());
        mvc.perform(People.asRider(get("/api/payments/methods"), rider))
                .andExpect(jsonPath("$.length()").value(1));
        Map visa = Trips.json(mvc, People.asRider(Trips.body(post(complete),
                Trips.card(Trips.VISA)), rider));
        assertEquals("card", visa.get("kind"));
        assertEquals("Visa", visa.get("brand"));
        assertEquals("4242", visa.get("last4"));
        assertEquals("Visa 4242", visa.get("label"));
        assertEquals(12L, Trips.number(visa, "expMonth"));
        // The first card saved is the one to pay with.
        assertEquals(Boolean.TRUE, visa.get("isDefault"));
        // A setup is used once.
        mvc.perform(People.asRider(Trips.body(post(complete), Trips.card(Trips.VISA)), rider))
                .andExpect(status().isNotFound());

        Map second = Trips.addCard(mvc, rider, Trips.MASTERCARD);
        assertEquals("Mastercard", second.get("brand"));
        assertEquals(Boolean.FALSE, second.get("isDefault"));
        String visaId = (String) visa.get("id");
        String secondId = (String) second.get("id");
        mvc.perform(People.asRider(post("/api/payments/methods/" + secondId + "/default"), rider))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isDefault").value(true));
        mvc.perform(People.asRider(get("/api/payments/methods"), rider))
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].id").value(visaId))
                .andExpect(jsonPath("$[0].isDefault").value(false))
                .andExpect(jsonPath("$[1].isDefault").value(true))
                .andExpect(jsonPath("$[2].id").value("cash"))
                .andExpect(jsonPath("$[2].isDefault").value(false));

        // A card is its owner's: nobody else sees it, chooses it or removes it.
        mvc.perform(People.asRider(get("/api/payments/methods"), other))
                .andExpect(jsonPath("$.length()").value(1));
        mvc.perform(People.asRider(post("/api/payments/methods/" + visaId + "/default"), other))
                .andExpect(status().isNotFound());
        mvc.perform(People.asRider(post("/api/payments/methods/" + visaId + "/remove"), other))
                .andExpect(status().isNotFound());
        mvc.perform(People.asRider(Trips.body(post("/api/rides"), Trips.ride(52.52, 13.405,
                        52.53, 13.42, "\"paymentMethodId\":\"" + visaId + "\"")), other))
                .andExpect(status().isBadRequest());

        // Removing the default leaves cash as the default; cash stays.
        mvc.perform(People.asRider(post("/api/payments/methods/" + secondId + "/remove"), rider))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(secondId));
        mvc.perform(People.asRider(get("/api/payments/methods"), rider))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[1].id").value("cash"))
                .andExpect(jsonPath("$[1].isDefault").value(true));
        mvc.perform(People.asRider(post("/api/payments/methods/cash/remove"), rider))
                .andExpect(status().isBadRequest());
        mvc.perform(People.asRider(post("/api/payments/methods/cash/default"), rider))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isDefault").value(true));
    }

    @Test
    void aRideIsChargedTippedAndRefundedAndTheDriverIsPaid() throws Exception {
        // Berlin.
        String rider = People.rider(accounts, "ber-rider");
        String driver = People.driver(accounts, "ber-driver");
        String stranger = People.rider(accounts, "ber-stranger");
        Trips.addCard(mvc, rider, Trips.VISA);
        Trips.online(mvc, driver, 52.5200, 13.4050);

        // No method named: the default, which is now the card.
        Map done = Trips.complete(mvc, rider, driver, People.ride(52.5200, 13.4050, 52.5300,
                13.4200));
        String id = (String) done.get("id");
        long fare = Trips.number(done, "fareCents");
        long fee = Trips.number(done, "serviceFee");
        assertEquals("PAID", done.get("paymentStatus"));
        assertEquals("Visa 4242", done.get("paymentMethodLabel"));
        assertEquals(Boolean.FALSE, done.get("cash"));
        assertEquals("standard", done.get("product"));
        assertEquals(Math.round(fare * 10d / 100d), fee);
        assertEquals(fare + fee, Trips.number(done, "total"));
        assertEquals("B", done.get("driverPhotoInitials"));

        // The receipt adds up, part by part.
        Map receipt = Trips.receipt(mvc, rider, id);
        assertEquals("paid", receipt.get("status"));
        assertEquals("Visa 4242", receipt.get("methodLabel"));
        assertEquals(fare, Trips.number(receipt, "baseFare") + Trips.number(receipt,
                "distanceFare") + Trips.number(receipt, "timeFare"));
        assertEquals(fee, Trips.number(receipt, "serviceFee"));
        assertEquals(0L, Trips.number(receipt, "tip"));
        assertEquals(fare + fee, Trips.number(receipt, "total"));
        assertTrue(Trips.number(receipt, "paidAt") > 0L);
        assertEquals("Here", receipt.get("pickupName"));
        assertEquals("ber-driver", receipt.get("driverName"));
        assertEquals("Green Fiat 500", receipt.get("vehicle"));
        // It is the rider's, and nobody else's -- the driver's included.
        mvc.perform(People.asRider(get("/api/payments/receipts/" + id), stranger))
                .andExpect(status().isNotFound());
        mvc.perform(People.asDriver(get("/api/payments/receipts/" + id), driver))
                .andExpect(status().isNotFound());
        mvc.perform(People.asRider(get("/api/payments/receipts"), stranger))
                .andExpect(jsonPath("$.length()").value(0));
        mvc.perform(People.asRider(get("/api/payments/receipts"), rider))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].rideId").value(id));

        // A tip, once, by the rider.
        mvc.perform(People.asRider(Trips.body(post("/api/rides/" + id + "/tip"),
                        "{\"amount\":300}"), stranger))
                .andExpect(status().isNotFound());
        mvc.perform(People.asRider(Trips.body(post("/api/rides/" + id + "/tip"),
                        "{\"amount\":0}"), rider))
                .andExpect(status().isBadRequest());
        Map tipped = Trips.json(mvc, People.asRider(Trips.body(post("/api/rides/" + id + "/tip"),
                "{\"amount\":300}"), rider));
        assertEquals(300L, Trips.number(tipped, "tip"));
        assertEquals(fare + fee + 300L, Trips.number(tipped, "total"));
        mvc.perform(People.asRider(Trips.body(post("/api/rides/" + id + "/tip"),
                        "{\"amount\":300}"), rider))
                .andExpect(status().isConflict());
        mvc.perform(People.asRider(get("/api/rides/" + id), rider))
                .andExpect(jsonPath("$.tip").value(300));

        // The driver earns the fare less the service's fifth, and all the tip.
        long earned = fare - Math.round(fare * 20d / 100d) + 300L;
        Map summary = Trips.json(mvc, People.asDriver(get("/api/driver/earnings/summary"),
                driver));
        assertEquals("USD", summary.get("currency"));
        assertEquals(earned, Trips.number(summary, "today"));
        assertEquals(earned, Trips.number(summary, "week"));
        assertEquals(earned, Trips.number(summary, "month"));
        assertEquals(earned, Trips.number(summary, "balance"));
        assertEquals(1L, Trips.number(summary, "trips"));
        assertEquals(300L, Trips.number(summary, "tips"));
        assertEquals(1d, ((Number) summary.get("acceptanceRate")).doubleValue());
        List daily = (List) summary.get("daily");
        assertEquals(14, daily.size());
        Map today = (Map) daily.get(13);
        assertEquals(Days.iso(System.currentTimeMillis()), today.get("date"));
        assertEquals(earned, Trips.number(today, "amount"));
        assertEquals(1L, Trips.number(today, "trips"));
        assertEquals(0L, Trips.number((Map) daily.get(0), "amount"));
        // What the older call reports is unchanged: the fare.
        mvc.perform(People.asDriver(get("/api/driver/earnings"), driver))
                .andExpect(jsonPath("$.totalCents").value((int) fare));

        // Cashing out needs somewhere to send it, and empties the balance.
        mvc.perform(People.asDriver(get("/api/driver/payouts"), driver))
                .andExpect(jsonPath("$.length()").value(0));
        mvc.perform(People.asDriver(post("/api/driver/payouts"), driver))
                .andExpect(status().isConflict());
        mvc.perform(People.asDriver(get("/api/driver/payout-account"), driver))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountLast4").value(""));
        mvc.perform(People.asDriver(Trips.body(put("/api/driver/payout-account"),
                        "{\"holder\":\"B Driver\",\"bankName\":\"Spree Bank\","
                                + "\"accountLast4\":\"12a4\"}"), driver))
                .andExpect(status().isBadRequest());
        mvc.perform(People.asDriver(Trips.body(put("/api/driver/payout-account"),
                        "{\"holder\":\"B Driver\",\"bankName\":\"Spree Bank\","
                                + "\"accountLast4\":\"1234\"}"), driver))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bankName").value("Spree Bank"));
        Map payout = Trips.json(mvc, People.asDriver(post("/api/driver/payouts"), driver));
        assertEquals(earned, Trips.number(payout, "amount"));
        assertEquals("paid", payout.get("status"));
        assertEquals("Spree Bank ****1234", payout.get("destination"));
        mvc.perform(People.asDriver(post("/api/driver/payouts"), driver))
                .andExpect(status().isConflict());
        mvc.perform(People.asDriver(get("/api/driver/payouts"), driver))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value((String) payout.get("id")));
        summary = Trips.json(mvc, People.asDriver(get("/api/driver/earnings/summary"), driver));
        assertEquals(0L, Trips.number(summary, "balance"));
        assertEquals(earned, Trips.number(summary, "today"));

        // An admin sees every payment, and returns this one, once, saying why.
        List all = Trips.list(mvc, admin(get("/api/admin/payments")));
        boolean listed = false;
        for (int iter = 0; iter < all.size(); iter++) {
            Map row = (Map) all.get(iter);
            if (id.equals(row.get("rideId"))) {
                listed = true;
                assertEquals(rider, row.get("riderUsername"));
            }
        }
        assertTrue(listed, "the admin's list has the ride");
        mvc.perform(admin(Trips.body(post("/api/admin/rides/" + id + "/refund"), "{}")))
                .andExpect(status().isBadRequest());
        mvc.perform(admin(Trips.body(post("/api/admin/rides/" + id + "/refund"),
                        "{\"reason\":\"The car broke down\"}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("refunded"));
        mvc.perform(admin(Trips.body(post("/api/admin/rides/" + id + "/refund"),
                        "{\"reason\":\"Twice\"}")))
                .andExpect(status().isConflict());
        assertEquals("refunded", Trips.receipt(mvc, rider, id).get("status"));
        mvc.perform(People.asRider(get("/api/rides/" + id), rider))
                .andExpect(jsonPath("$.paymentStatus").value("REFUNDED"));
        Trips.offline(mvc, driver);
    }

    @Test
    void cashIsRecordedAndACardThatFailsIsReported() throws Exception {
        // Dublin.
        String rider = People.rider(accounts, "dub-rider");
        String driver = People.driver(accounts, "dub-driver");
        String failing = (String) Trips.addCard(mvc, rider, Trips.FAILS_WHEN_CHARGED).get("id");
        Trips.online(mvc, driver, 53.3498, -6.2603);

        // Cash, asked for by name although a card is the default.
        Map cash = Trips.complete(mvc, rider, driver, Trips.ride(53.3498, -6.2603, 53.3600,
                -6.2500, "\"paymentMethodId\":\"cash\",\"note\":\"Blue door\",\"quietRide\":true"));
        String cashId = (String) cash.get("id");
        assertEquals("PAID", cash.get("paymentStatus"));
        assertEquals("Cash", cash.get("paymentMethodLabel"));
        assertEquals(Boolean.TRUE, cash.get("cash"));
        assertEquals("Blue door", cash.get("note"));
        assertEquals(Boolean.TRUE, cash.get("quietRide"));
        assertEquals("cash", Trips.receipt(mvc, rider, cashId).get("status"));
        // Nothing went to a card, so there is nothing to send back to one.
        mvc.perform(admin(Trips.body(post("/api/admin/rides/" + cashId + "/refund"),
                        "{\"reason\":\"Asked nicely\"}")))
                .andExpect(status().isConflict());
        mvc.perform(People.asRider(Trips.body(post("/api/rides/" + cashId + "/tip"),
                        "{\"amount\":150}"), rider))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tip").value(150))
                .andExpect(jsonPath("$.status").value("cash"));

        // The card that is refused when charged: the ride still ends, and
        // says it was not paid for.
        Map unpaid = Trips.complete(mvc, rider, driver, Trips.ride(53.3498, -6.2603, 53.3600,
                -6.2500, "\"paymentMethodId\":\"" + failing + "\""));
        String unpaidId = (String) unpaid.get("id");
        assertEquals("COMPLETED", unpaid.get("state"));
        assertEquals("FAILED", unpaid.get("paymentStatus"));
        assertEquals("failed", Trips.receipt(mvc, rider, unpaidId).get("status"));
        mvc.perform(People.asRider(Trips.body(post("/api/rides/" + unpaidId + "/tip"),
                        "{\"amount\":150}"), rider))
                .andExpect(status().isConflict());
        mvc.perform(admin(Trips.body(post("/api/admin/rides/" + unpaidId + "/refund"),
                        "{\"reason\":\"Nothing to return\"}")))
                .andExpect(status().isConflict());
        // A card with a ride riding on it is not removed until the ride ends.
        String waiting = (String) Trips.json(mvc, People.asRider(Trips.body(post("/api/rides"),
                Trips.ride(53.3498, -6.2603, 53.3600, -6.2500, "\"paymentMethodId\":\"" + failing
                        + "\"")), rider)).get("id");
        mvc.perform(People.asRider(post("/api/payments/methods/" + failing + "/remove"), rider))
                .andExpect(status().isConflict());
        mvc.perform(People.asRider(post("/api/rides/" + waiting + "/cancel"), rider))
                .andExpect(status().isOk());
        mvc.perform(People.asRider(post("/api/payments/methods/" + failing + "/remove"), rider))
                .andExpect(status().isOk());
        Trips.offline(mvc, driver);
    }

    @Test
    void theHostedSetupEndsOnAPageThatSaysSo() throws Exception {
        String done = mvc.perform(get("/pay/return?result=done")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertTrue(done.indexOf("Card added") > 0, done);
        String cancelled = mvc.perform(get("/pay/return?result=%3Cscript%3E"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        // Nothing from the address is written into the page.
        assertTrue(cancelled.indexOf("No card was added") > 0, cancelled);
        assertEquals(-1, cancelled.indexOf("<script"));
    }
}
