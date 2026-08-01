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
package com.codename1.impl.javase.health;

import com.codename1.health.HealthAuthorizationStatus;
import com.codename1.health.HealthDataType;
import com.codename1.health.HealthError;
import com.codename1.health.HealthException;
import com.codename1.health.HealthDeleteRequest;
import com.codename1.health.HealthQuantity;
import com.codename1.health.HealthSample;
import com.codename1.health.HealthTimeRange;
import com.codename1.health.HealthUnit;
import com.codename1.health.QuantitySample;
import com.codename1.health.SampleQuery;
import com.codename1.util.AsyncResource;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import com.codename1.util.SuccessCallback;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The single highest-value behaviour the health simulator reproduces.
 *
 * <p>HealthKit deliberately refuses to disclose read authorization: a denied
 * read looks exactly like having no data, so that an app cannot infer what a
 * user is choosing to hide. A developer testing only against a permissive
 * store will not meet this until review or production. These cases pin that
 * the simulator reproduces it faithfully, and that switching to Health
 * Connect's behaviour makes the same script fail loudly instead.</p>
 */
class HealthReadAuthTrapTest {

    private SimulatedHealthStore store;

    @BeforeEach
    void createStore() {
        store = new SimulatedHealthStore();
        QuantitySample s = QuantitySample.create(HealthDataType.HEART_RATE,
                new HealthQuantity(62, HealthUnit.COUNT_PER_MINUTE),
                1_767_225_600_000L);
        List<HealthSample> seed = new ArrayList<HealthSample>();
        seed.add(s);
        store.seed(seed);
    }

    private SampleQuery heartRateQuery() {
        return new SampleQuery().addType(HealthDataType.HEART_RATE)
                .setTimeRange(HealthTimeRange.between(1_767_000_000_000L,
                        1_768_000_000_000L));
    }

    /**
     * Every status that is not AUTHORIZED refuses a write and a delete.
     *
     * <p>The guard listed the statuses that fail -- DENIED and
     * NOT_DETERMINED -- so RESTRICTED, UNKNOWN and NOT_SUPPORTED fell
     * through and mutated the store. A test could script "this device blocks
     * writes" and watch the write succeed, which is worse than not
     * simulating the case at all: the app ships having passed a check that
     * agrees with no real provider.</p>
     */
    @Test
    void onlyAuthorizedMayWriteOrDelete() {
        HealthAuthorizationStatus[] refused = {
            HealthAuthorizationStatus.DENIED,
            HealthAuthorizationStatus.NOT_DETERMINED,
            HealthAuthorizationStatus.RESTRICTED,
            HealthAuthorizationStatus.UNKNOWN,
            HealthAuthorizationStatus.NOT_SUPPORTED,
        };
        for (HealthAuthorizationStatus status : refused) {
            store.setWritePermission(HealthDataType.BODY_MASS, status);

            assertNotNull(errorOf(store.write(oneWeight())),
                    "a write must fail while write access is " + status);
            assertNotNull(errorOf(store.delete(
                    HealthDeleteRequest.byRange(HealthDataType.BODY_MASS,
                            HealthTimeRange.between(0L, 10_000L)))),
                    "a delete must fail while write access is " + status);
        }

        // And the one status that does allow it still does.
        store.setWritePermission(HealthDataType.BODY_MASS,
                HealthAuthorizationStatus.AUTHORIZED);
        assertNull(errorOf(store.write(oneWeight())),
                "AUTHORIZED must still be able to write");
    }

    private static java.util.List<HealthSample> oneWeight() {
        java.util.List<HealthSample> out =
                new java.util.ArrayList<HealthSample>();
        out.add(QuantitySample.create(HealthDataType.BODY_MASS,
                new HealthQuantity(70, HealthUnit.KILOGRAM), 1000L));
        return out;
    }

    /// Waits for `r` to settle, and returns it.
    ///
    /// Results are delivered on the EDT on every backend, so a test running
    /// off it sees the outcome queued rather than already attached. The
    /// assertions here are unchanged; they just have to be made after the
    /// delivery instead of before it.
    private static <T> AsyncResource<T> settled(AsyncResource<T> r) {
        long deadline = System.currentTimeMillis() + 10_000L;
        while (!r.isDone() && System.currentTimeMillis() < deadline) {
            try {
                Thread.sleep(5L);
            } catch (InterruptedException ex) {
                break;
            }
        }
        assertTrue(r.isDone(), "the operation must settle rather than hang");
        return r;
    }

    /// Settles `r` and returns the failure it carries, or null if it
    /// succeeded.
    ///
    /// This used to read the error straight back, because a callback attached
    /// to an already-failed resource fired inline on the registering thread.
    /// Health results are delivered on the EDT whether the listener arrives
    /// before the outcome or after it, so an off-EDT caller gets it queued and
    /// has to wait -- the same wait `settled` makes one step earlier.
    ///
    /// Both sides are registered because most callers ask this of a resource
    /// that succeeded and expect null; only one of the two ever fires, so
    /// waiting on `except` alone would burn the whole limit on every
    /// successful call.
    private static <T> Throwable errorOf(AsyncResource<T> r) {
        settled(r);
        // Atomics because the callback runs on the EDT while the value is read
        // from the test thread.
        final AtomicReference<Throwable> err = new AtomicReference<Throwable>();
        final AtomicBoolean delivered = new AtomicBoolean();
        r.except(new SuccessCallback<Throwable>() {
            public void onSucess(Throwable t) {
                err.set(t);
                delivered.set(true);
            }
        });
        r.ready(new SuccessCallback<T>() {
            public void onSucess(T value) {
                delivered.set(true);
            }
        });
        long deadline = System.currentTimeMillis() + 10_000L;
        while (!delivered.get() && System.currentTimeMillis() < deadline) {
            try {
                Thread.sleep(5L);
            } catch (InterruptedException ex) {
                break;
            }
        }
        assertTrue(delivered.get(), "the outcome must be delivered rather than hang");
        return err.get();
    }

    /** The permissive baseline: data is there and comes back. */
    @Test
    void grantedReadReturnsData() {
        store.setReadPermission(HealthDataType.HEART_RATE,
                SimulatedHealthStore.ReadAuthScript.GRANTED);
        List<HealthSample> read = store.readSamples(heartRateQuery()).get();
        assertEquals(1, read.size());
    }

    /**
     * The trap, end to end: authorization "succeeds", the status is
     * UNKNOWN, and the query returns empty <em>with no error</em>. An app
     * that reports "you denied access" here would be guessing.
     */
    @Test
    void iosDeniedReadIsIndistinguishableFromHavingNoData() {
        store.setReadAuthorizationPolicy(
                SimulatedHealthStore.ReadAuthPolicy.IOS_OPAQUE);
        store.setReadPermission(HealthDataType.HEART_RATE,
                SimulatedHealthStore.ReadAuthScript.DENIED_SILENT);

        AsyncResource<Boolean> auth = store.requestAuthorization(
                com.codename1.health.HealthAccess.read(
                        HealthDataType.HEART_RATE));
        assertEquals(Boolean.TRUE, auth.get(),
                "the authorization flow completes even when nothing was"
                        + " granted -- true means 'the user was asked'");

        assertEquals(HealthAuthorizationStatus.UNKNOWN,
                store.getReadAuthorizationStatus(HealthDataType.HEART_RATE),
                "iOS never discloses read authorization");

        AsyncResource<List<HealthSample>> read =
                store.readSamples(heartRateQuery());
        assertNull(errorOf(read), "a denied read must not surface an error");
        assertTrue(read.get().isEmpty(),
                "a denied read is silently empty");
    }

    /**
     * The other half of the trap: a genuinely empty store produces exactly
     * the same observations, which is why the two cannot be told apart and
     * why the API offers no hasReadPermission.
     */
    @Test
    void grantedButNoDataIsObservationallyIdenticalToDenial() {
        store.setReadAuthorizationPolicy(
                SimulatedHealthStore.ReadAuthPolicy.IOS_OPAQUE);
        store.setReadPermission(HealthDataType.HEART_RATE,
                SimulatedHealthStore.ReadAuthScript.GRANTED_BUT_NO_DATA);

        assertEquals(HealthAuthorizationStatus.UNKNOWN,
                store.getReadAuthorizationStatus(HealthDataType.HEART_RATE));
        AsyncResource<List<HealthSample>> read =
                store.readSamples(heartRateQuery());
        assertNull(errorOf(read));
        assertTrue(read.get().isEmpty());
    }

    /**
     * A type scripted to yield nothing empties itself, not the query.
     *
     * <p>Adding one such type used to blank the whole page, so unrelated
     * steps disappeared alongside the heart rate the script was hiding --
     * and a developer would reasonably read that as a bug in their own
     * code rather than as the script working. Neither platform behaves
     * that way: HealthKit hands back what it will show you and stays quiet
     * about the rest.</p>
     */
    @Test
    void oneEmptyTypeDoesNotEmptyTheOthers() {
        List<HealthSample> seed = new ArrayList<HealthSample>();
        seed.add(QuantitySample.create(HealthDataType.STEPS,
                new HealthQuantity(2500, HealthUnit.COUNT),
                1_767_225_600_000L, 1_767_225_660_000L));
        store.seed(seed);
        store.setReadAuthorizationPolicy(
                SimulatedHealthStore.ReadAuthPolicy.IOS_OPAQUE);
        store.setReadPermission(HealthDataType.HEART_RATE,
                SimulatedHealthStore.ReadAuthScript.GRANTED_BUT_NO_DATA);
        store.setReadPermission(HealthDataType.STEPS,
                SimulatedHealthStore.ReadAuthScript.GRANTED);

        AsyncResource<List<HealthSample>> read = store.readSamples(
                new SampleQuery().addType(HealthDataType.HEART_RATE)
                        .addType(HealthDataType.STEPS)
                        .setTimeRange(HealthTimeRange.between(
                                1_767_000_000_000L, 1_768_000_000_000L)));
        assertNull(errorOf(read));
        List<HealthSample> back = read.get();
        assertEquals(1, back.size(), "the granted type still contributes");
        assertEquals(HealthDataType.STEPS, back.get(0).getType());
    }

    /**
     * A hidden record must not spend the query's limit.
     *
     * <p>The first fix filtered the finished page, which is applied after
     * the shared store has sorted and cut to the limit -- so a hidden
     * record that sorted first ate the only slot and a limit-one query
     * came back empty with a visible record sitting right behind it. The
     * records nobody can see are withheld before the sort now.</p>
     */
    @Test
    void aHiddenRecordDoesNotSpendTheLimit() {
        List<HealthSample> seed = new ArrayList<HealthSample>();
        // The steps sample is older, so a descending read reaches the
        // hidden heart-rate sample first.
        seed.add(QuantitySample.create(HealthDataType.STEPS,
                new HealthQuantity(2500, HealthUnit.COUNT),
                1_767_225_000_000L, 1_767_225_060_000L));
        seed.add(QuantitySample.create(HealthDataType.HEART_RATE,
                new HealthQuantity(70, HealthUnit.COUNT_PER_MINUTE),
                1_767_225_600_000L));
        store.seed(seed);
        store.setReadAuthorizationPolicy(
                SimulatedHealthStore.ReadAuthPolicy.IOS_OPAQUE);
        store.setReadPermission(HealthDataType.HEART_RATE,
                SimulatedHealthStore.ReadAuthScript.GRANTED_BUT_NO_DATA);
        store.setReadPermission(HealthDataType.STEPS,
                SimulatedHealthStore.ReadAuthScript.GRANTED);

        AsyncResource<List<HealthSample>> read = store.readSamples(
                new SampleQuery().addType(HealthDataType.HEART_RATE)
                        .addType(HealthDataType.STEPS)
                        .setSortDescending(true)
                        .setLimit(1)
                        .setTimeRange(HealthTimeRange.between(
                                1_767_000_000_000L, 1_768_000_000_000L)));
        assertNull(errorOf(read));
        List<HealthSample> back = read.get();
        assertEquals(1, back.size(),
                "the visible record should fill the budget");
        assertEquals(HealthDataType.STEPS, back.get(0).getType());
    }

    /** The same rule for aggregates, which take a separate path. */
    @Test
    void oneEmptyTypeDoesNotEmptyTheAggregate() {
        // Both types are cumulative, because a metric applies to every
        // type in the query and TOTAL is not meaningful for a discrete
        // one.
        List<HealthSample> seed = new ArrayList<HealthSample>();
        seed.add(QuantitySample.create(HealthDataType.STEPS,
                new HealthQuantity(2500, HealthUnit.COUNT),
                1_767_225_600_000L, 1_767_225_660_000L));
        seed.add(QuantitySample.create(
                HealthDataType.DISTANCE_WALKING_RUNNING,
                new HealthQuantity(1800, HealthUnit.METER),
                1_767_225_600_000L, 1_767_225_660_000L));
        store.seed(seed);
        store.setReadAuthorizationPolicy(
                SimulatedHealthStore.ReadAuthPolicy.IOS_OPAQUE);
        store.setReadPermission(HealthDataType.DISTANCE_WALKING_RUNNING,
                SimulatedHealthStore.ReadAuthScript.GRANTED_BUT_NO_DATA);
        store.setReadPermission(HealthDataType.STEPS,
                SimulatedHealthStore.ReadAuthScript.GRANTED);

        AsyncResource<List<com.codename1.health.AggregateResult>> agg =
                store.aggregate(new com.codename1.health.AggregateQuery()
                        .addType(HealthDataType.DISTANCE_WALKING_RUNNING)
                        .addType(HealthDataType.STEPS)
                        .addMetric(com.codename1.health.AggregateMetric.TOTAL)
                        .setTimeRange(HealthTimeRange.between(
                                1_767_000_000_000L, 1_768_000_000_000L)));
        assertNull(errorOf(agg));
        List<com.codename1.health.AggregateResult> buckets = agg.get();
        assertFalse(buckets.isEmpty());
        com.codename1.health.HealthQuantity steps = buckets.get(0).get(
                HealthDataType.STEPS,
                com.codename1.health.AggregateMetric.TOTAL);
        assertNotNull(steps, "the granted type still aggregates");
        assertEquals(2500, steps.getValue(HealthUnit.COUNT), 0.001);
        assertNull(buckets.get(0).get(
                HealthDataType.DISTANCE_WALKING_RUNNING,
                com.codename1.health.AggregateMetric.TOTAL),
                "and the hidden type still contributes nothing");
    }

    /** Health Connect answers honestly and fails loudly. */
    @Test
    void androidDeniedReadFailsWithUnauthorized() {
        store.setReadAuthorizationPolicy(
                SimulatedHealthStore.ReadAuthPolicy.ANDROID_EXPLICIT);
        store.setReadPermission(HealthDataType.HEART_RATE,
                SimulatedHealthStore.ReadAuthScript.DENIED_SILENT);

        assertEquals(HealthAuthorizationStatus.DENIED,
                store.getReadAuthorizationStatus(HealthDataType.HEART_RATE),
                "Health Connect read permission is an ordinary grant");

        Throwable err = errorOf(store.readSamples(heartRateQuery()));
        assertNotNull(err);
        assertEquals(HealthError.UNAUTHORIZED,
                ((HealthException) err).getError());
    }

    /**
     * IOS_OPAQUE is the default precisely because it is the surprising
     * behaviour -- a developer must meet it in the simulator, not in
     * review.
     */
    @Test
    void iosOpaqueIsTheDefaultPolicy() {
        assertEquals(SimulatedHealthStore.ReadAuthPolicy.IOS_OPAQUE,
                new SimulatedHealthStore().getReadAuthorizationPolicy());
    }

    /** Write authorization is truthfully reportable on both platforms. */
    @Test
    void writeAuthorizationIsAlwaysAnswerable() {
        store.setWritePermission(HealthDataType.BODY_MASS,
                HealthAuthorizationStatus.DENIED);
        assertEquals(HealthAuthorizationStatus.DENIED,
                store.getWriteAuthorizationStatus(HealthDataType.BODY_MASS));
        store.setWritePermission(HealthDataType.BODY_MASS,
                HealthAuthorizationStatus.AUTHORIZED);
        assertEquals(HealthAuthorizationStatus.AUTHORIZED,
                store.getWriteAuthorizationStatus(HealthDataType.BODY_MASS));
    }

    /** A denied write fails, unlike a denied read. */
    @Test
    void deniedWriteFails() {
        store.setWritePermission(HealthDataType.BODY_MASS,
                HealthAuthorizationStatus.DENIED);
        QuantitySample w = QuantitySample.create(HealthDataType.BODY_MASS,
                new HealthQuantity(70, HealthUnit.KILOGRAM),
                1_767_225_600_000L);
        Throwable err = errorOf(store.write(w));
        assertNotNull(err, "a denied write is reportable and must fail");
        assertEquals(HealthError.UNAUTHORIZED,
                ((HealthException) err).getError());
    }

    /**
     * Every rejection is observable on the calling thread, whichever layer
     * produced it.
     *
     * <p>A store call can be turned down in three places -- the shared
     * unsupported check, query validation, and the backend itself -- and
     * all three must behave the same way, because a caller cannot tell
     * which one answered. Routing one of them through {@code callSerially}
     * made the store's threading depend on <i>why</i> it failed, so the
     * same rejection resolved inline or a cycle later depending on whether
     * an event thread happened to be pumping. This asserts the three
     * together rather than one at a time, which is how the split went
     * unnoticed.</p>
     */
    @Test
    void everyRejectionResolvesOnTheCallingThread() {
        store.setAvailable(false);
        assertTrue(settled(store.readSamples(heartRateQuery())).isDone(),
                "an unsupported store must answer before returning");

        store.setAvailable(true);
        store.setReadPermission(HealthDataType.HEART_RATE,
                SimulatedHealthStore.ReadAuthScript.GRANTED);
        assertTrue(settled(store.readSamples(new SampleQuery())).isDone(),
                "a rejected query must answer before returning");

        store.failNext("query", HealthError.DATABASE_INACCESSIBLE,
                "device locked");
        assertTrue(settled(store.readSamples(heartRateQuery())).isDone(),
                "a backend failure must answer before returning");
    }

    /** Fault injection is one-shot, so a test can assert recovery. */
    @Test
    void primedFailureFiresOnceThenRecovers() {
        store.setReadPermission(HealthDataType.HEART_RATE,
                SimulatedHealthStore.ReadAuthScript.GRANTED);
        store.failNext("query", HealthError.DATABASE_INACCESSIBLE,
                "device locked");

        Throwable err = errorOf(store.readSamples(heartRateQuery()));
        assertNotNull(err);
        assertEquals(HealthError.DATABASE_INACCESSIBLE,
                ((HealthException) err).getError());

        assertEquals(1, store.readSamples(heartRateQuery()).get().size(),
                "the next call must succeed");
    }

    /**
     * An unavailable provider fails every operation with NOT_SUPPORTED
     * rather than pretending the store is simply empty.
     */
    @Test
    void unavailableStoreFailsRatherThanReturningEmpty() {
        store.setAvailable(false);
        assertFalse(store.isSupported());
        Throwable err = errorOf(store.readSamples(heartRateQuery()));
        assertNotNull(err);
        assertEquals(HealthError.NOT_SUPPORTED,
                ((HealthException) err).getError());
    }

    @Test
    void resetScriptsRestoresDefaultsWithoutDiscardingData() {
        store.setAvailable(false);
        store.setAllReadPermissions(
                SimulatedHealthStore.ReadAuthScript.DENIED_ERROR);
        store.resetScripts();

        assertTrue(store.isSupported());
        assertEquals(SimulatedHealthStore.ReadAuthPolicy.IOS_OPAQUE,
                store.getReadAuthorizationPolicy());
        assertEquals(1, store.readSamples(heartRateQuery()).get().size(),
                "resetScripts must not discard seeded data");
    }

    /**
     * A denied write is unauthorized, not unsupported.
     *
     * <p>Both mobile stores answer {@code isWritable} from what the
     * platform can store, independently of what the user has allowed. The
     * simulator folded the scripted grant into that answer, so a test
     * scripting DENIED was refused by the shared layer as
     * TYPE_NOT_SUPPORTED before the authorization path ran at all -- the
     * developer chased "this platform cannot store that" instead of the
     * permission problem they were simulating. NOT_DETERMINED went the
     * other way and wrote successfully, letting an app ship having never
     * exercised its own authorization flow.</p>
     */
    @Test
    void writeAuthorizationIsSeparateFromWritability() {
        QuantitySample w = QuantitySample.create(HealthDataType.BODY_MASS,
                new HealthQuantity(70, HealthUnit.KILOGRAM),
                1_767_225_600_000L);

        store.setWritePermission(HealthDataType.BODY_MASS,
                HealthAuthorizationStatus.DENIED);
        assertTrue(store.isWritable(HealthDataType.BODY_MASS),
                "the platform can still store a body mass");
        assertEquals(HealthError.UNAUTHORIZED,
                ((HealthException) errorOf(store.write(w))).getError());

        store.setWritePermission(HealthDataType.BODY_MASS,
                HealthAuthorizationStatus.NOT_DETERMINED);
        assertEquals(HealthError.UNAUTHORIZED,
                ((HealthException) errorOf(store.write(w))).getError(),
                "a write before the user was asked is refused too");

        store.setWritePermission(HealthDataType.BODY_MASS,
                HealthAuthorizationStatus.AUTHORIZED);
        assertNull(errorOf(store.write(w)), "a granted write succeeds");
    }

    /**
     * A denied read denies the aggregate too.
     *
     * <p>Aggregation reads the local records directly rather than going
     * through the sample path, so a type scripted DENIED_SILENT returned a
     * real total while the matching sample read came back empty. A
     * developer would have drawn a chart from data the simulator was
     * pretending they could not see -- the exact trap this store exists to
     * spring.</p>
     */
    @Test
    void aDeniedReadDeniesTheAggregateToo() {
        store.setReadAuthorizationPolicy(
                SimulatedHealthStore.ReadAuthPolicy.IOS_OPAQUE);
        store.setReadPermission(HealthDataType.HEART_RATE,
                SimulatedHealthStore.ReadAuthScript.DENIED_SILENT);

        AsyncResource<java.util.List<com.codename1.health.AggregateResult>>
                r = store.aggregate(new com.codename1.health.AggregateQuery()
                        .addType(HealthDataType.HEART_RATE)
                        .addMetric(com.codename1.health.AggregateMetric.AVERAGE)
                        .setTimeRange(HealthTimeRange.between(
                                1_767_000_000_000L, 1_768_000_000_000L)));
        assertNull(errorOf(r), "iOS denial stays silent here as well");
        for (com.codename1.health.AggregateResult bucket : r.get()) {
            assertNull(bucket.get(HealthDataType.HEART_RATE,
                    com.codename1.health.AggregateMetric.AVERAGE),
                    "a denied aggregate must not report a total");
        }

        store.setReadAuthorizationPolicy(
                SimulatedHealthStore.ReadAuthPolicy.ANDROID_EXPLICIT);
        Throwable err = errorOf(store.aggregate(
                new com.codename1.health.AggregateQuery()
                        .addType(HealthDataType.HEART_RATE)
                        .addMetric(com.codename1.health.AggregateMetric.AVERAGE)
                        .setTimeRange(HealthTimeRange.between(
                                1_767_000_000_000L, 1_768_000_000_000L))));
        assertNotNull(err, "Health Connect fails loudly here too");
        assertEquals(HealthError.UNAUTHORIZED,
                ((HealthException) err).getError());
    }

    /**
     * Deleting needs write authorization, as it does on Health Connect.
     *
     * <p>Writes honoured the scripted status while deletes went straight
     * through, so a test could delete records it was supposedly not
     * allowed to touch.</p>
     */
    @Test
    void aDeniedWriteDeniesTheDelete() {
        store.setWritePermission(HealthDataType.HEART_RATE,
                HealthAuthorizationStatus.DENIED);
        Throwable err = errorOf(store.delete(
                com.codename1.health.HealthDeleteRequest.byRange(
                        HealthDataType.HEART_RATE,
                        HealthTimeRange.between(1_767_000_000_000L,
                                1_768_000_000_000L))));
        assertNotNull(err, "an unauthorized delete must fail");
        assertEquals(HealthError.UNAUTHORIZED,
                ((HealthException) err).getError());

        store.setWritePermission(HealthDataType.HEART_RATE,
                HealthAuthorizationStatus.AUTHORIZED);
        assertNull(errorOf(store.delete(
                com.codename1.health.HealthDeleteRequest.byRange(
                        HealthDataType.HEART_RATE,
                        HealthTimeRange.between(1_767_000_000_000L,
                                1_768_000_000_000L)))),
                "a granted delete still works");
    }

    /**
     * The simulator's "make health unavailable" action must be visible
     * where the guide tells an app to look -- the facade -- not only when
     * an operation fails.
     */
    @Test
    void simulatedUnavailabilityReachesTheFacade() {
        com.codename1.impl.health.LocalHealth health =
                new com.codename1.impl.health.LocalHealth(store);
        assertTrue(health.isSupported());
        assertEquals(com.codename1.health.HealthAvailability.LOCAL_ONLY,
                health.getAvailability());

        store.setAvailable(false);

        assertFalse(health.isSupported(),
                "an app branching on the facade must see the simulation");
        assertEquals(com.codename1.health.HealthAvailability.NOT_SUPPORTED,
                health.getAvailability());
    }
}
