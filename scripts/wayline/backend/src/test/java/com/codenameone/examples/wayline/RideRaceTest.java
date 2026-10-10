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
import com.codename1.backend.test.BackendTest;
import com.codename1.backend.test.MockMvc;
import com.codenameone.examples.wayline.account.Accounts;
import com.codenameone.examples.wayline.api.RideDto;
import com.codenameone.examples.wayline.ride.Rides;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static com.codename1.backend.test.MockMvcRequestBuilders.get;
import static com.codename1.backend.test.MockMvcRequestBuilders.post;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// A ride answered from several places at the same moment.
///
/// One statement decides each change of state, and the number of rows it
/// changed is the answer. These tests are the ones that would notice that
/// becoming a read followed by a write: they send the same answer from many
/// threads at once and count how many were told it was theirs.
///
/// The threads call the service and not the HTTP layer. What is being tested
/// is the transaction each call opens, and the service is where it opens.
@BackendTest
class RideRaceTest {
    private static final int THREADS = 8;
    /// The outcomes of one call: the ride was taken, or the status it was
    /// refused with.
    private static final int TAKEN = 200;

    @Autowired
    private MockMvc mvc;
    @Autowired
    private Accounts accounts;
    @Autowired
    private Rides rides;
    @Autowired
    private DataSource db;

    /// Runs `calls` at the same moment, each on a thread of its own, and waits
    /// for them all.
    private static void together(Runnable[] calls) throws Exception {
        // A count and a flag, polled: the class library a compiled server
        // links against has the atomics and no latch.
        final AtomicInteger ready = new AtomicInteger();
        final AtomicBoolean go = new AtomicBoolean();
        Thread[] threads = new Thread[calls.length];
        for (int iter = 0; iter < calls.length; iter++) {
            final Runnable call = calls[iter];
            threads[iter] = new Thread(new Runnable() {
                @Override
                public void run() {
                    ready.incrementAndGet();
                    long giveUp = System.currentTimeMillis() + 30000L;
                    while (!go.get()) {
                        if (System.currentTimeMillis() > giveUp) {
                            return;
                        }
                        Thread.yield();
                    }
                    call.run();
                }
            }, "race-" + iter);
            threads[iter].start();
        }
        long giveUp = System.currentTimeMillis() + 30000L;
        while (ready.get() < calls.length) {
            assertTrue(System.currentTimeMillis() < giveUp, "the threads started");
            Thread.sleep(1L);
        }
        go.set(true);
        for (int iter = 0; iter < threads.length; iter++) {
            threads[iter].join(60000L);
            assertTrue(!threads[iter].isAlive(), "thread " + iter + " finished");
        }
    }

    /// A call of `accept` that writes what came of it into `outcomes[slot]`:
    /// [#TAKEN], the status of the refusal, or -1 for anything else.
    private Runnable accept(final String driver, final String id, final int[] outcomes,
            final Throwable[] unexpected, final int slot) {
        return new Runnable() {
            @Override
            public void run() {
                try {
                    RideDto ride = rides.accept(driver, id);
                    outcomes[slot] = "ACCEPTED".equals(ride.state) ? TAKEN : -1;
                } catch (ResponseStatusException refused) {
                    outcomes[slot] = refused.getStatus();
                } catch (Throwable err) {
                    outcomes[slot] = -1;
                    unexpected[slot] = err;
                }
            }
        };
    }

    private static int count(int[] outcomes, int wanted) {
        int found = 0;
        for (int iter = 0; iter < outcomes.length; iter++) {
            if (outcomes[iter] == wanted) {
                found++;
            }
        }
        return found;
    }

    private static void noneUnexpected(Throwable[] unexpected) {
        for (int iter = 0; iter < unexpected.length; iter++) {
            if (unexpected[iter] != null) {
                throw new AssertionError("call " + iter + " failed", unexpected[iter]);
            }
        }
    }

    private long events(String id, String state) throws Exception {
        Map row = db.queryOne("SELECT COUNT(*) AS n FROM wl_ride_event WHERE ride_id = ? "
                + "AND state = ?", new Object[] {id, state});
        return ((Number) row.get("n")).longValue();
    }

    /// A ride offered to `driver`, who is on line beside the pickup.
    private String offered(String rider, String driver, double lat, double lng) throws Exception {
        Trips.online(mvc, driver, lat, lng);
        Map asked = Trips.json(mvc, People.asRider(Trips.body(post("/api/rides"),
                People.ride(lat + 0.0002, lng + 0.0002, lat + 0.02, lng + 0.02)), rider));
        assertEquals("OFFERED", asked.get("state"));
        String id = (String) asked.get("id");
        assertEquals(id, Trips.json(mvc, People.asDriver(get("/api/driver/active"), driver))
                .get("id"));
        return id;
    }

    @Test
    void anOfferAcceptedEightTimesAtOnceIsTakenOnce() throws Exception {
        // Warsaw.
        String rider = People.rider(accounts, "waw-rider");
        String driver = People.driver(accounts, "waw-driver");
        String id = offered(rider, driver, 52.2297, 21.0122);

        int[] outcomes = new int[THREADS];
        Throwable[] unexpected = new Throwable[THREADS];
        Runnable[] calls = new Runnable[THREADS];
        for (int iter = 0; iter < THREADS; iter++) {
            calls[iter] = accept(driver, id, outcomes, unexpected, iter);
        }
        together(calls);

        noneUnexpected(unexpected);
        assertEquals(1, count(outcomes, TAKEN), "one call took the ride");
        assertEquals(THREADS - 1, count(outcomes, 409), "and every other was told it was taken");
        // What happened is on record once, too.
        assertEquals(1L, events(id, "ACCEPTED"));
        assertEquals("ACCEPTED", Trips.json(mvc, People.asRider(get("/api/rides/" + id), rider))
                .get("state"));
    }

    @Test
    void anOfferThatLapsedIsNotTakenWhileItIsBeingTakenBack() throws Exception {
        // Athens.
        String rider = People.rider(accounts, "ath-rider");
        final String driver = People.driver(accounts, "ath-driver");
        final String id = offered(rider, driver, 37.9838, 23.7275);

        // Time is moved on by hand: the offer lapsed, and nothing has swept it
        // up yet. Taking it is refused already.
        db.execute("UPDATE wl_ride SET offer_expires_at = 1 WHERE id = ?", new Object[] {id});
        int[] alone = new int[1];
        Throwable[] none = new Throwable[1];
        accept(driver, id, alone, none, 0).run();
        noneUnexpected(none);
        if (alone[0] != 404) {
            // 404 is the sweep having run between the two lines above: the
            // offer is gone and so, to this driver, is the ride.
            assertEquals(409, alone[0]);
        }

        // Now the sweep and the driver's answers at the same moment.
        int[] outcomes = new int[THREADS];
        Throwable[] unexpected = new Throwable[THREADS + 1];
        Runnable[] calls = new Runnable[THREADS + 1];
        for (int iter = 0; iter < THREADS; iter++) {
            calls[iter] = accept(driver, id, outcomes, unexpected, iter);
        }
        calls[THREADS] = new Runnable() {
            @Override
            public void run() {
                try {
                    rides.tick();
                } catch (Throwable err) {
                    unexpected[THREADS] = err;
                }
            }
        };
        together(calls);

        noneUnexpected(unexpected);
        assertEquals(0, count(outcomes, TAKEN), "nobody took a lapsed offer");
        // Refused as lapsed before the sweep, and as no longer theirs after it.
        assertEquals(THREADS, count(outcomes, 409) + count(outcomes, 404));
        assertEquals(0L, events(id, "ACCEPTED"));
        // The only driver in reach has let it go by, so it waits.
        assertEquals("REQUESTED", Trips.json(mvc, People.asRider(get("/api/rides/" + id), rider))
                .get("state"));
        assertEquals("NONE", Trips.json(mvc, People.asDriver(get("/api/driver/active"), driver))
                .get("state"));
        mvc.perform(People.asRider(post("/api/rides/" + id + "/cancel"), rider));
    }

    @Test
    void anOfferTakenBackIsNoLongerTheDriversToTake() throws Exception {
        // Brussels.
        String rider = People.rider(accounts, "bru-rider");
        String driver = People.driver(accounts, "bru-driver");
        String id = offered(rider, driver, 50.8503, 4.3517);

        // The offer lapses and the sweep takes it back. The ride is no longer
        // this driver's, and a ride that is not theirs is answered as no ride
        // at all: 404, as it is for any ride of somebody else's.
        db.execute("UPDATE wl_ride SET offer_expires_at = 1 WHERE id = ?", new Object[] {id});
        rides.tick();
        int[] outcome = new int[1];
        Throwable[] none = new Throwable[1];
        accept(driver, id, outcome, none, 0).run();
        noneUnexpected(none);
        assertEquals(404, outcome[0]);
        assertEquals(0L, events(id, "ACCEPTED"));
        assertEquals("REQUESTED", Trips.json(mvc, People.asRider(get("/api/rides/" + id), rider))
                .get("state"));
        mvc.perform(People.asRider(post("/api/rides/" + id + "/cancel"), rider));
    }
}
