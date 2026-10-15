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
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Autowired;
import com.codename1.backend.annotations.Bean;
import com.codename1.backend.annotations.Primary;
import com.codename1.backend.test.BackendTest;
import com.codename1.backend.test.MockMvc;
import com.codename1.backend.test.TestConfiguration;
import com.codenameone.examples.wayline.account.Accounts;
import com.codenameone.examples.wayline.pay.Payments;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;

import static com.codename1.backend.test.MockMvcRequestBuilders.get;
import static com.codename1.backend.test.MockMvcResultMatchers.jsonPath;
import static org.junit.jupiter.api.Assertions.assertEquals;

/// Paying, against a processor that fails when a test says so: what is left
/// behind when a request to it fails part way, alone or overlapping another.
/// The service is called directly, on the test's thread, so the request a
/// test lines up inside another runs exactly where it is put.
@BackendTest
class ProcessorFaultsTest {
    @Autowired
    private MockMvc mvc;
    @Autowired
    private Accounts accounts;
    @Autowired
    private Payments payments;
    @Autowired
    private ScriptedPayments processor;
    @Autowired
    private DataSource db;

    @TestConfiguration
    static class Processor {
        @Bean
        @Primary
        ScriptedPayments scriptedPayments() {
            return new ScriptedPayments();
        }
    }

    /// The status `call` was refused with, or 200 when it was not.
    private static int refusal(Runnable call) {
        try {
            call.run();
            return 200;
        } catch (ResponseStatusException refused) {
            return refused.getStatus();
        }
    }

    private Runnable refund(final String rideId) {
        return new Runnable() {
            @Override
            public void run() {
                try {
                    payments.refund(rideId, "Overlapping");
                } catch (IOException err) {
                    throw new IllegalStateException(err);
                }
            }
        };
    }

    private Runnable remove(final String rider, final String card) {
        return new Runnable() {
            @Override
            public void run() {
                try {
                    payments.remove(rider, card);
                } catch (IOException err) {
                    throw new IllegalStateException(err);
                }
            }
        };
    }

    private long balance(String driver) throws Exception {
        return Trips.number(Trips.json(mvc, People.asDriver(get("/api/driver/earnings/summary"),
                driver)), "balance");
    }

    @Test
    void aRemovedCardIsDetachedAtTheProcessorFirst() throws Exception {
        processor.reset();
        String rider = People.rider(accounts, "card-detacher");
        String card = (String) Trips.addCard(mvc, rider, Trips.VISA).get("id");
        String token = (String) db.queryOne("SELECT token FROM payment_method WHERE id = ?",
                new Object[] {card}).get("token");

        // A processor that cannot be reached keeps the card: deleted here
        // alone, it would stay chargeable there with nothing pointing at it.
        processor.detachFails = true;
        assertEquals(502, refusal(remove(rider, card)));
        mvc.perform(People.asRider(get("/api/payments/methods"), rider))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(card));
        assertEquals(0, processor.detached.size());

        processor.detachFails = false;
        assertEquals(200, refusal(remove(rider, card)));
        assertEquals(1, processor.detached.size());
        assertEquals(token, processor.detached.get(0));
        mvc.perform(People.asRider(get("/api/payments/methods"), rider))
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void aRefundThatFailsDoesNotUndoOneThatOverlappedIt() throws Exception {
        processor.reset();
        // Riga.
        String rider = People.rider(accounts, "rix-rider");
        String driver = People.driver(accounts, "rix-driver");
        Trips.addCard(mvc, rider, Trips.VISA);
        Trips.online(mvc, driver, 56.9496, 24.1052);
        String id = (String) Trips.complete(mvc, rider, driver, People.ride(56.9496, 24.1052,
                56.9596, 24.1152)).get("id");

        // A refund whose processor fails, and while it is at the processor a
        // second one -- let through as one that may have been cut short --
        // that succeeds and is recorded. The second answers first.
        processor.duringRefund = refund(id);
        processor.refundFailures.add(Boolean.FALSE);
        processor.refundFailures.add(Boolean.TRUE);
        assertEquals(502, refusal(refund(id)));
        // The failure gives back nothing: the payment was refunded, so the
        // ride is, and its fare is not the driver's to cash out.
        mvc.perform(People.asRider(get("/api/rides/" + id), rider))
                .andExpect(jsonPath("$.paymentStatus").value("REFUNDED"));
        assertEquals("refunded", Trips.receipt(mvc, rider, id).get("status"));
        assertEquals(0L, balance(driver));

        // The other way round: the overlapping refund fails and gives its
        // claim back while the first is still at the processor, and the first
        // then succeeds.
        final String next = (String) Trips.complete(mvc, rider, driver, People.ride(56.9496,
                24.1052, 56.9596, 24.1152)).get("id");
        processor.duringRefund = new Runnable() {
            @Override
            public void run() {
                assertEquals(502, refusal(refund(next)));
            }
        };
        processor.refundFailures.add(Boolean.TRUE);
        processor.refundFailures.add(Boolean.FALSE);
        assertEquals(200, refusal(refund(next)));
        Map ride = Trips.json(mvc, People.asRider(get("/api/rides/" + next), rider));
        assertEquals("REFUNDED", ride.get("paymentStatus"));
        assertEquals(0L, balance(driver));
        Trips.offline(mvc, driver);
    }
}
