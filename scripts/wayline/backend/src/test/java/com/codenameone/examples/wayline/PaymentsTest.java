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
import com.codename1.backend.annotations.Autowired;
import com.codename1.backend.test.BackendTest;
import com.codename1.backend.test.MockMvc;
import com.codename1.backend.test.MockRequestBuilder;
import com.codenameone.examples.wayline.account.Accounts;
import com.codenameone.examples.wayline.account.DemoAccounts;
import com.codenameone.examples.wayline.pay.Payments;
import com.codenameone.examples.wayline.ride.Rides;
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
    @Autowired
    private Rides rides;
    @Autowired
    private Payments payments;
    @Autowired
    private DataSource db;

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
        // The older call reports the same share, without the tip.
        mvc.perform(People.asDriver(get("/api/driver/earnings"), driver))
                .andExpect(jsonPath("$.totalCents").value((int) (earned - 300L)));

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

        // The refund took the fare and the tip back, so neither is earned any
        // more: not in the day's totals, and not in the balance, which now owes
        // what was cashed out. The trip was still driven.
        summary = Trips.json(mvc, People.asDriver(get("/api/driver/earnings/summary"), driver));
        assertEquals(0L, Trips.number(summary, "today"));
        assertEquals(0L, Trips.number(summary, "month"));
        assertEquals(0L, Trips.number((Map) ((List) summary.get("daily")).get(13), "amount"));
        assertEquals(1L, Trips.number((Map) ((List) summary.get("daily")).get(13), "trips"));
        assertEquals(0L, Trips.number(summary, "tips"));
        assertEquals(-earned, Trips.number(summary, "balance"));
        mvc.perform(People.asDriver(post("/api/driver/payouts"), driver))
                .andExpect(status().isConflict());
        Trips.offline(mvc, driver);
    }

    @Test
    void aRideIsNotRefundedWhileItsTipIsBeingCharged() throws Exception {
        // Oslo.
        String rider = People.rider(accounts, "osl-rider");
        String driver = People.driver(accounts, "osl-driver");
        Trips.addCard(mvc, rider, Trips.VISA);
        Trips.online(mvc, driver, 59.9139, 10.7522);
        String id = (String) Trips.complete(mvc, rider, driver, People.ride(59.9139, 10.7522,
                59.9239, 10.7622)).get("id");

        // A tip as it stands between being claimed and its charge coming back:
        // on the ride, and with no charge recorded for it.
        db.execute("UPDATE wl_ride SET tip_cents = 300 WHERE id = ?", new Object[] {id});
        mvc.perform(admin(Trips.body(post("/api/admin/rides/" + id + "/refund"),
                        "{\"reason\":\"Too soon\"}")))
                .andExpect(status().isConflict());
        // Refused whole: the ride is still paid, so the refund can be asked
        // for again and the tip's charge has a paid ride to land on.
        mvc.perform(People.asRider(get("/api/rides/" + id), rider))
                .andExpect(jsonPath("$.paymentStatus").value("PAID"));
        // Nor is it the driver's to cash out: nobody knows it was charged.
        Map before = Trips.json(mvc, People.asDriver(get("/api/driver/earnings/summary"),
                driver));
        assertEquals(0L, Trips.number(before, "tips"));
        long fareOnly = Trips.number(before, "balance");
        // Nor earned today: the day's total is what the balance grew by.
        assertEquals(fareOnly, Trips.number(before, "today"));
        assertEquals(fareOnly, Trips.number(before, "month"));

        // The charge came back.
        db.execute("UPDATE wl_payment SET tip_ref = 'sim_ch_tip' WHERE ride_id = ?",
                new Object[] {id});
        Map after = Trips.json(mvc, People.asDriver(get("/api/driver/earnings/summary"),
                driver));
        assertEquals(300L, Trips.number(after, "tips"));
        assertEquals(fareOnly + 300L, Trips.number(after, "balance"));
        assertEquals(fareOnly + 300L, Trips.number(after, "today"));
        mvc.perform(admin(Trips.body(post("/api/admin/rides/" + id + "/refund"),
                        "{\"reason\":\"Now\"}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("refunded"));
        Trips.offline(mvc, driver);
    }

    @Test
    void aRefundTheServerNeverRecordedIsAskedForAgain() throws Exception {
        // Bergen.
        String rider = People.rider(accounts, "bgo-rider");
        String driver = People.driver(accounts, "bgo-driver");
        Trips.addCard(mvc, rider, Trips.VISA);
        Trips.online(mvc, driver, 60.3913, 5.3221);
        String id = (String) Trips.complete(mvc, rider, driver, People.ride(60.3913, 5.3221,
                60.4013, 5.3321)).get("id");

        // A refund as a server that stopped half way leaves it: the ride
        // marked, the processor perhaps asked, and nothing written down.
        db.execute("UPDATE wl_ride SET payment_status = 'REFUNDED' WHERE id = ?",
                new Object[] {id});
        mvc.perform(admin(Trips.body(post("/api/admin/rides/" + id + "/refund"),
                        "{\"reason\":\"Again\"}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("refunded"));
        // And once it is recorded, it is refunded once like any other.
        mvc.perform(admin(Trips.body(post("/api/admin/rides/" + id + "/refund"),
                        "{\"reason\":\"Twice\"}")))
                .andExpect(status().isConflict());
        Trips.offline(mvc, driver);
    }

    @Test
    void aRefundTheProcessorHasNotFinishedIsRecordedOnlyOnceItEnds() throws Exception {
        // Gothenburg.
        String rider = People.rider(accounts, "got-rider");
        String driver = People.driver(accounts, "got-driver");
        String card = (String) Trips.addCard(mvc, rider, Trips.SLOW_REFUND).get("id");
        String method = "\"paymentMethodId\":\"" + card + "\"";
        Trips.online(mvc, driver, 57.7089, 11.9746);
        String id = (String) Trips.complete(mvc, rider, driver, Trips.ride(57.7089, 11.9746,
                57.7189, 11.9846, method)).get("id");

        // Taken, and not yet money back: pending, and not to be asked for twice.
        mvc.perform(admin(Trips.body(post("/api/admin/rides/" + id + "/refund"),
                        "{\"reason\":\"Slow\"}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("refund_pending"));
        mvc.perform(People.asRider(get("/api/rides/" + id), rider))
                .andExpect(jsonPath("$.paymentStatus").value("REFUND_PENDING"));
        mvc.perform(admin(Trips.body(post("/api/admin/rides/" + id + "/refund"),
                        "{\"reason\":\"Again\"}")))
                .andExpect(status().isConflict());
        // The processor has finished it by the time it is asked.
        payments.settleRefunds();
        assertEquals("refunded", Trips.receipt(mvc, rider, id).get("status"));

        // And one the processor then fails is recorded as failed, never as
        // refunded, and is not tried again from here.
        db.execute("UPDATE wl_payment_method SET token = 'sim_pm_lost_card' WHERE id = ?",
                new Object[] {card});
        String lost = (String) Trips.complete(mvc, rider, driver, Trips.ride(57.7089, 11.9746,
                57.7189, 11.9846, method)).get("id");
        mvc.perform(admin(Trips.body(post("/api/admin/rides/" + lost + "/refund"),
                        "{\"reason\":\"Lost\"}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("refund_pending"));
        payments.settleRefunds();
        assertEquals("refund_failed", Trips.receipt(mvc, rider, lost).get("status"));
        mvc.perform(People.asRider(get("/api/rides/" + lost), rider))
                .andExpect(jsonPath("$.paymentStatus").value("REFUND_FAILED"));
        mvc.perform(admin(Trips.body(post("/api/admin/rides/" + lost + "/refund"),
                        "{\"reason\":\"Again\"}")))
                .andExpect(status().isConflict());
        Trips.offline(mvc, driver);
    }

    @Test
    void aRideLeftCompletedAndUnpaidIsSettledLater() throws Exception {
        // Lisbon.
        String rider = People.rider(accounts, "lis-rider");
        String driver = People.driver(accounts, "lis-driver");
        Trips.addCard(mvc, rider, Trips.VISA);
        Trips.online(mvc, driver, 38.7223, -9.1393);
        Map done = Trips.complete(mvc, rider, driver, People.ride(38.7223, -9.1393, 38.7323,
                -9.1293));
        String id = (String) done.get("id");
        long total = Trips.number(done, "fareCents") + Trips.number(done, "serviceFee");

        // The server stopped after the ride completed and before its payment
        // was written.
        db.execute("DELETE FROM wl_payment WHERE ride_id = ?", new Object[] {id});
        db.execute("UPDATE wl_ride SET payment_status = 'PENDING' WHERE id = ?",
                new Object[] {id});

        // Completed a moment ago, it may still be at the processor: left alone.
        rides.settleLeftBehind();
        mvc.perform(People.asRider(get("/api/rides/" + id), rider))
                .andExpect(jsonPath("$.paymentStatus").value("PENDING"));

        db.execute("UPDATE wl_ride SET updated_at = ? WHERE id = ?",
                new Object[] {Long.valueOf(System.currentTimeMillis() - 5L * 60L * 1000L), id});
        rides.settleLeftBehind();
        mvc.perform(People.asRider(get("/api/rides/" + id), rider))
                .andExpect(jsonPath("$.paymentStatus").value("PAID"));
        Map receipt = Trips.receipt(mvc, rider, id);
        assertEquals("paid", receipt.get("status"));
        assertEquals(total, Trips.number(receipt, "total"));

        // And once: a second pass finds nothing left to settle.
        rides.settleLeftBehind();
        assertEquals(total, Trips.number(Trips.receipt(mvc, rider, id), "total"));
        Trips.offline(mvc, driver);
    }

    @Test
    void aChargeNobodyCanVouchForIsLeftPendingAndAskedForAgain() throws Exception {
        // Prague.
        String rider = People.rider(accounts, "prg-rider");
        String driver = People.driver(accounts, "prg-driver");
        String card = (String) Trips.addCard(mvc, rider, Trips.NO_ANSWER_WHEN_CHARGED).get("id");
        Trips.online(mvc, driver, 50.0755, 14.4378);
        Map done = Trips.complete(mvc, rider, driver, Trips.ride(50.0755, 14.4378, 50.0855,
                14.4478, "\"paymentMethodId\":\"" + card + "\""));
        String id = (String) done.get("id");
        long total = Trips.number(done, "fareCents") + Trips.number(done, "serviceFee");

        // The processor failed without saying whether it took the money: not
        // paid, and not failed either.
        assertEquals("COMPLETED", done.get("state"));
        assertEquals("PENDING", done.get("paymentStatus"));
        // Not money yet, so not the driver's to cash out.
        assertEquals(0L, Trips.number(Trips.json(mvc, People.asDriver(
                get("/api/driver/earnings/summary"), driver)), "balance"));
        // The card is what the ride will be asked about with, so it stays,
        // and so does the account it belongs to.
        mvc.perform(People.asRider(post("/api/payments/methods/" + card + "/remove"), rider))
                .andExpect(status().isConflict());
        mvc.perform(People.asRider(Trips.body(post("/api/account/delete"),
                        "{\"password\":\"" + People.PASSWORD + "\"}"), rider))
                .andExpect(status().isConflict());
        db.execute("UPDATE wl_ride SET updated_at = ? WHERE id = ?",
                new Object[] {Long.valueOf(System.currentTimeMillis() - 5L * 60L * 1000L), id});
        rides.settleLeftBehind();
        mvc.perform(People.asRider(get("/api/rides/" + id), rider))
                .andExpect(jsonPath("$.paymentStatus").value("PENDING"));

        // The processor answers again, and the same ride is asked about.
        db.execute("UPDATE wl_payment_method SET token = 'sim_pm_recovered' WHERE id = ?",
                new Object[] {card});
        rides.settleLeftBehind();
        mvc.perform(People.asRider(get("/api/rides/" + id), rider))
                .andExpect(jsonPath("$.paymentStatus").value("PAID"));
        assertEquals(total, Trips.number(Trips.receipt(mvc, rider, id), "total"));
        // Paid now, and the driver's share is in the balance.
        assertEquals(Trips.number(done, "fareCents") - Math.round(
                Trips.number(done, "fareCents") * 20d / 100d), Trips.number(Trips.json(mvc,
                People.asDriver(get("/api/driver/earnings/summary"), driver)), "balance"));
        // Settled, and the card can go.
        mvc.perform(People.asRider(post("/api/payments/methods/" + card + "/remove"), rider))
                .andExpect(status().isOk());
        Trips.offline(mvc, driver);
    }

    @Test
    void aTipNobodyCanVouchForStaysOnTheRideUntilItIsAskedForAgain() throws Exception {
        // Vienna.
        String rider = People.rider(accounts, "vie-rider");
        String driver = People.driver(accounts, "vie-driver");
        String card = (String) Trips.addCard(mvc, rider, Trips.VISA).get("id");
        Trips.online(mvc, driver, 48.2082, 16.3738);
        String id = (String) Trips.complete(mvc, rider, driver, People.ride(48.2082, 16.3738,
                48.2182, 16.3838)).get("id");

        // The processor stops answering, and a tip is asked for.
        db.execute("UPDATE wl_payment_method SET token = 'sim_pm_fault_now' WHERE id = ?",
                new Object[] {card});
        assertEquals(502, Trips.statusOf(mvc, People.asRider(Trips.body(
                post("/api/rides/" + id + "/tip"), "{\"amount\":300}"), rider)));
        // It may have been charged, so it stays on the ride, which cannot be
        // refunded around it, and cannot be swapped for another amount.
        assertEquals(300L, Trips.number(Trips.receipt(mvc, rider, id), "tip"));
        mvc.perform(admin(Trips.body(post("/api/admin/rides/" + id + "/refund"),
                        "{\"reason\":\"Too soon\"}")))
                .andExpect(status().isConflict());
        mvc.perform(People.asRider(Trips.body(post("/api/rides/" + id + "/tip"),
                        "{\"amount\":500}"), rider))
                .andExpect(status().isConflict());

        // Asked for again with the same amount once the processor is back.
        db.execute("UPDATE wl_payment_method SET token = 'sim_pm_recovered' WHERE id = ?",
                new Object[] {card});
        mvc.perform(People.asRider(Trips.body(post("/api/rides/" + id + "/tip"),
                        "{\"amount\":300}"), rider))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tip").value(300));
        mvc.perform(admin(Trips.body(post("/api/admin/rides/" + id + "/refund"),
                        "{\"reason\":\"Now\"}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("refunded"));
        Trips.offline(mvc, driver);
    }

    @Test
    void theCardLimitHoldsForSetupsStartedWhileThereWasRoom() throws Exception {
        String rider = People.rider(accounts, "card-collector");
        for (int iter = 0; iter < 7; iter++) {
            Trips.addCard(mvc, rider, Trips.VISA);
        }
        // Two setups started with room for one card.
        Map first = Trips.json(mvc, People.asRider(post("/api/payments/methods/setup"), rider));
        Map second = Trips.json(mvc, People.asRider(post("/api/payments/methods/setup"), rider));
        mvc.perform(People.asRider(Trips.body(post("/api/payments/methods/setup/"
                        + first.get("id") + "/complete"), Trips.card(Trips.MASTERCARD)), rider))
                .andExpect(status().isOk());
        mvc.perform(People.asRider(Trips.body(post("/api/payments/methods/setup/"
                        + second.get("id") + "/complete"), Trips.card(Trips.MASTERCARD)), rider))
                .andExpect(status().isConflict());
        // Cash and eight cards.
        mvc.perform(People.asRider(get("/api/payments/methods"), rider))
                .andExpect(jsonPath("$.length()").value(9));
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
