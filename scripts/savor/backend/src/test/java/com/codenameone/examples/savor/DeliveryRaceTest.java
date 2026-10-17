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

import com.codename1.backend.DataSource;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Autowired;
import com.codename1.backend.test.BackendTest;
import com.codename1.backend.test.MockMvc;
import com.codenameone.examples.savor.account.Accounts;
import com.codenameone.examples.savor.api.DeliveryDto;
import com.codenameone.examples.savor.api.DeliveryState;
import com.codenameone.examples.savor.delivery.Deliveries;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static com.codename1.backend.test.MockMvcRequestBuilders.get;
import static com.codename1.backend.test.MockMvcRequestBuilders.post;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// A delivery answered from several places at the same moment.
///
/// One statement decides each change of state, and the number of rows it
/// changed is the answer. These tests are the ones that would notice that
/// becoming a read followed by a write: they send the same answer from many
/// threads at once and count how many were told it was theirs.
///
/// The threads call the service and not the HTTP layer. What is being tested
/// is the transaction each call opens, and the service is where it opens.
@BackendTest
class DeliveryRaceTest {
    private static final int THREADS = 8;
    /// The outcomes of one call: the delivery was taken, or the status it was
    /// refused with.
    private static final int TAKEN = 200;

    @Autowired
    private MockMvc mvc;
    @Autowired
    private Accounts accounts;
    @Autowired
    private Deliveries deliveries;
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
    private Runnable accept(final String courier, final String id, final int[] outcomes,
            final Throwable[] unexpected, final int slot) {
        return new Runnable() {
            @Override
            public void run() {
                try {
                    DeliveryDto delivery = deliveries.accept(courier, id);
                    outcomes[slot] = DeliveryState.ACCEPTED == delivery.state() ? TAKEN : -1;
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
        Map row = db.queryOne("SELECT COUNT(*) AS n FROM delivery_event WHERE delivery_id = ? "
                + "AND state = ?", new Object[] {id, state});
        return ((Number) row.get("n")).longValue();
    }

    /// A delivery offered to `courier`, who is on line beside the pickup.
    private String offered(String customer, String courier, double lat, double lng) throws Exception {
        Trips.online(mvc, courier, lat, lng);
        Map asked = Trips.json(mvc, People.asCustomer(Trips.body(post("/api/deliveries"),
                People.delivery(lat + 0.0002, lng + 0.0002, lat + 0.02, lng + 0.02)), customer));
        assertEquals("OFFERED", asked.get("state"));
        String id = (String) asked.get("id");
        assertEquals(id, Trips.json(mvc, People.asCourier(get("/api/courier/active"), courier))
                .get("id"));
        return id;
    }

    @Test
    void anOfferAcceptedEightTimesAtOnceIsTakenOnce() throws Exception {
        // Warsaw.
        String customer = People.customer(accounts, "waw-customer");
        String courier = People.courier(accounts, "waw-courier");
        String id = offered(customer, courier, 52.2297, 21.0122);

        int[] outcomes = new int[THREADS];
        Throwable[] unexpected = new Throwable[THREADS];
        Runnable[] calls = new Runnable[THREADS];
        for (int iter = 0; iter < THREADS; iter++) {
            calls[iter] = accept(courier, id, outcomes, unexpected, iter);
        }
        together(calls);

        noneUnexpected(unexpected);
        assertEquals(1, count(outcomes, TAKEN), "one call took the delivery");
        assertEquals(THREADS - 1, count(outcomes, 409), "and every other was told it was taken");
        // What happened is on record once, too.
        assertEquals(1L, events(id, "ACCEPTED"));
        assertEquals("ACCEPTED", Trips.json(mvc, People.asCustomer(get("/api/deliveries/" + id), customer))
                .get("state"));
    }

    @Test
    void anOfferThatLapsedIsNotTakenWhileItIsBeingTakenBack() throws Exception {
        // Athens.
        String customer = People.customer(accounts, "ath-customer");
        final String courier = People.courier(accounts, "ath-courier");
        final String id = offered(customer, courier, 37.9838, 23.7275);

        // Time is moved on by hand: the offer lapsed, and nothing has swept it
        // up yet. Taking it is refused already.
        db.execute("UPDATE delivery SET offer_expires_at = 1 WHERE id = ?", new Object[] {id});
        int[] alone = new int[1];
        Throwable[] none = new Throwable[1];
        accept(courier, id, alone, none, 0).run();
        noneUnexpected(none);
        if (alone[0] != 404) {
            // 404 is the sweep having run between the two lines above: the
            // offer is gone and so, to this courier, is the delivery.
            assertEquals(409, alone[0]);
        }

        // Now the sweep and the courier's answers at the same moment.
        int[] outcomes = new int[THREADS];
        Throwable[] unexpected = new Throwable[THREADS + 1];
        Runnable[] calls = new Runnable[THREADS + 1];
        for (int iter = 0; iter < THREADS; iter++) {
            calls[iter] = accept(courier, id, outcomes, unexpected, iter);
        }
        calls[THREADS] = new Runnable() {
            @Override
            public void run() {
                try {
                    deliveries.tick();
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
        // The only courier in reach has let it go by, so it waits.
        assertEquals("REQUESTED", Trips.json(mvc, People.asCustomer(get("/api/deliveries/" + id), customer))
                .get("state"));
        assertEquals("NONE", Trips.json(mvc, People.asCourier(get("/api/courier/active"), courier))
                .get("state"));
        mvc.perform(People.asCustomer(post("/api/deliveries/" + id + "/cancel"), customer));
    }

    @Test
    void anOfferTakenBackIsNoLongerTheCouriersToTake() throws Exception {
        // Brussels.
        String customer = People.customer(accounts, "bru-customer");
        String courier = People.courier(accounts, "bru-courier");
        String id = offered(customer, courier, 50.8503, 4.3517);

        // The offer lapses and the sweep takes it back. The delivery is no longer
        // this courier's, and a delivery that is not theirs is answered as no delivery
        // at all: 404, as it is for any delivery of somebody else's.
        db.execute("UPDATE delivery SET offer_expires_at = 1 WHERE id = ?", new Object[] {id});
        deliveries.tick();
        int[] outcome = new int[1];
        Throwable[] none = new Throwable[1];
        accept(courier, id, outcome, none, 0).run();
        noneUnexpected(none);
        assertEquals(404, outcome[0]);
        assertEquals(0L, events(id, "ACCEPTED"));
        assertEquals("REQUESTED", Trips.json(mvc, People.asCustomer(get("/api/deliveries/" + id), customer))
                .get("state"));
        mvc.perform(People.asCustomer(post("/api/deliveries/" + id + "/cancel"), customer));
    }
}
