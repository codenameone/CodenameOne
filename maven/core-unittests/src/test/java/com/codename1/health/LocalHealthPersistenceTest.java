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
package com.codename1.health;

import com.codename1.health.nutrition.Nutrient;
import com.codename1.health.nutrition.NutritionSample;
import com.codename1.impl.health.LocalHealthStore;
import com.codename1.impl.health.StoredHealthStore;
import com.codename1.util.AsyncResource;
import com.codename1.io.Storage;
import com.codename1.junit.UITestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The local store on the ports that have no platform health store at all
 * -- Windows, Linux and JavaScript.
 *
 * <p>Those report {@code LOCAL_ONLY}, which says the data is only ever
 * this app's own; it does not say the data is gone next launch. Until this
 * existed the base class's persistence hook was a no-op, so every write on
 * those ports was silently lost on restart while the developer guide
 * promised durability.</p>
 *
 * <p>Each case writes through one store and reads back through a second,
 * which is what a restart actually looks like from the store's point of
 * view.</p>
 */
class LocalHealthPersistenceTest extends UITestBase {

    private static final long T0 = 1767225600000L;
    private static final long MINUTE = 60000L;

    @BeforeEach
    void clearStorage() {
        Storage.getInstance().deleteStorageFile("cn1$health$local");
    }

    private static List<HealthSample> one(HealthSample s) {
        List<HealthSample> out = new ArrayList<HealthSample>();
        out.add(s);
        return out;
    }

    /** Writes through one store, reads back through a fresh one. */
    private static List<HealthSample> roundTrip(HealthSample written,
            HealthDataType type) throws Exception {
        StoredHealthStore first = new StoredHealthStore();
        first.write(one(written)).get();
        StoredHealthStore second = new StoredHealthStore();
        return second.readSamples(new SampleQuery().addType(type)
                .setTimeRange(HealthTimeRange.between(T0 - MINUTE,
                        T0 + 100 * MINUTE))).get();
    }

    /**
     * A write that fails to persist is never visible to a reader.
     *
     * <p>The samples went into the live list, persistence ran, and only
     * then were they removed -- so a read taking the list in between saw
     * records that were about to be rolled back and reported as an
     * error. Health data that never existed is the worst thing this
     * store can hand out.</p>
     *
     * <p>This asserts the end state, which is all a sequential test can
     * reach: the window itself needs a reader running while persistence
     * is in flight. What closes it is that the insert, the persist and
     * the rollback are now one critical section on the list readers
     * take.</p>
     */
    @Test
    void aWriteThatCannotPersistIsNeverVisible() throws Exception {
        final boolean[] refuse = new boolean[] { true };
        LocalHealthStore store = new LocalHealthStore() {
            @Override
            protected boolean persist() {
                return !refuse[0];
            }
        };
        QuantitySample s = QuantitySample.create(HealthDataType.STEPS,
                new HealthQuantity(500, HealthUnit.COUNT), 1000L, 2000L);
        assertNotNull(errorOf(store.write(s)),
                "a store that cannot persist must fail the write");

        assertEquals(0, store.readSamples(new SampleQuery()
                .addType(HealthDataType.STEPS)
                .setTimeRange(HealthTimeRange.between(0L, 100000L)))
                .get().size(),
                "and nothing it rolled back may be readable");

        refuse[0] = false;
        assertNull(errorOf(store.write(s)));
        assertEquals(1, store.readSamples(new SampleQuery()
                .addType(HealthDataType.STEPS)
                .setTimeRange(HealthTimeRange.between(0L, 100000L)))
                .get().size(),
                "a write that does persist is still readable");
    }

    @Test
    void aQuantitySurvivesARestart() throws Exception {
        QuantitySample q = QuantitySample.create(HealthDataType.STEPS,
                new HealthQuantity(1234, HealthUnit.COUNT), T0,
                T0 + MINUTE);
        q.putMetadata("note", "written\tbefore\nthe restart");
        List<HealthSample> back = roundTrip(q, HealthDataType.STEPS);

        assertEquals(1, back.size());
        QuantitySample r = (QuantitySample) back.get(0);
        assertEquals(1234, r.getValue(HealthUnit.COUNT), 0.001);
        assertEquals(T0, r.getStartMillis());
        assertEquals(T0 + MINUTE, r.getEndMillis());
        // Tabs and newlines are the format's own separators, so free text
        // is exactly where an unescaped codec falls apart.
        assertEquals("written\tbefore\nthe restart",
                r.getMetadata().get("note"));
    }

    @Test
    void aCategorySurvivesARestart() throws Exception {
        List<HealthSample> back = roundTrip(
                CategorySample.create(HealthDataType.MENSTRUATION_FLOW, 3,
                        T0, T0 + MINUTE),
                HealthDataType.MENSTRUATION_FLOW);
        assertEquals(1, back.size());
        assertEquals(3, ((CategorySample) back.get(0)).getValue());
    }

    /**
     * A series is one record holding many measurements. The wire format
     * flattens it, which is why persisting through that would have been
     * the wrong reuse: the record would come back as loose points.
     */
    @Test
    void aSeriesKeepsItsRecordAndItsPoints() throws Exception {
        SeriesSample series = SeriesSample.create(HealthDataType.HEART_RATE,
                T0, T0 + 2 * MINUTE,
                new long[] { T0, T0 + MINUTE },
                new long[] { T0, T0 + MINUTE },
                new double[] { 61, 74 }, HealthUnit.COUNT_PER_MINUTE);
        List<HealthSample> back = roundTrip(series,
                HealthDataType.HEART_RATE);

        // Read unflattened, which is what asks for record identity --
        // the default flattens, and a flattened read of this record is two
        // points either way.
        assertEquals(2, back.size(), "flattened, this is two measurements");
        StoredHealthStore third = new StoredHealthStore();
        List<HealthSample> whole = third.readSamples(new SampleQuery()
                .addType(HealthDataType.HEART_RATE)
                .setFlattenSeries(false)
                .setTimeRange(HealthTimeRange.between(T0 - MINUTE,
                        T0 + 100 * MINUTE))).get();
        assertEquals(1, whole.size(), "the record, not its measurements");
        SeriesSample r = (SeriesSample) whole.get(0);
        assertEquals(2, r.size());
        assertEquals(61, r.getSampleValue(0, HealthUnit.COUNT_PER_MINUTE),
                0.001);
        assertEquals(74, r.getSampleValue(1, HealthUnit.COUNT_PER_MINUTE),
                0.001);
        assertEquals(T0 + MINUTE, r.getSampleStartMillis(1));
    }

    @Test
    void aSleepSessionKeepsItsStages() throws Exception {
        SleepSample sleep = SleepSample.create(T0, T0 + 60 * MINUTE);
        sleep.addStage(new SleepStageInterval(SleepStage.LIGHT, T0,
                T0 + 20 * MINUTE));
        sleep.addStage(new SleepStageInterval(SleepStage.DEEP,
                T0 + 20 * MINUTE, T0 + 60 * MINUTE));
        sleep.setTitle("night one");

        List<HealthSample> back = roundTrip(sleep, HealthDataType.SLEEP);
        assertEquals(1, back.size());
        SleepSample r = (SleepSample) back.get(0);
        assertEquals(2, r.getStages().size());
        assertEquals(SleepStage.DEEP, r.getStages().get(1).getStage());
        assertEquals("night one", r.getTitle());
        assertEquals(60 * MINUTE, r.getAsleepDurationMillis());
    }

    @Test
    void aWorkoutKeepsItsTotals() throws Exception {
        WorkoutSample w = WorkoutSample.create(WorkoutActivityType.RUNNING,
                T0, T0 + 30 * MINUTE);
        w.setTotalEnergy(new HealthQuantity(300, HealthUnit.KILOCALORIE));
        w.setTotalDistance(new HealthQuantity(5, HealthUnit.KILOMETER));
        w.setActiveDurationMillis(25 * MINUTE);
        w.setTitle("morning run");

        List<HealthSample> back = roundTrip(w, HealthDataType.WORKOUT);
        assertEquals(1, back.size());
        WorkoutSample r = (WorkoutSample) back.get(0);
        assertEquals(WorkoutActivityType.RUNNING, r.getActivityType());
        assertEquals(300, r.getTotalEnergy().getValue(
                HealthUnit.KILOCALORIE), 0.001);
        assertEquals(5000, r.getTotalDistance().getValue(HealthUnit.METER),
                0.001);
        assertEquals(25 * MINUTE, r.getActiveDurationMillis());
        assertEquals("morning run", r.getTitle());
    }

    /** A workout that never measured its totals reads back without them. */
    @Test
    void aWorkoutWithoutTotalsStaysWithoutThem() throws Exception {
        WorkoutSample w = WorkoutSample.create(WorkoutActivityType.YOGA, T0,
                T0 + 30 * MINUTE);
        List<HealthSample> back = roundTrip(w, HealthDataType.WORKOUT);
        WorkoutSample r = (WorkoutSample) back.get(0);
        assertNull(r.getTotalEnergy());
        assertNull(r.getTotalDistance());
        // Unset, so the getter still answers with the wall duration.
        assertEquals(30 * MINUTE, r.getActiveDurationMillis());
    }

    @Test
    void aNutritionEntryKeepsItsSparseNutrients() throws Exception {
        NutritionSample n = NutritionSample.create(T0);
        n.setNutrient(Nutrient.PROTEIN, 12);
        n.setNutrient(Nutrient.TOTAL_FAT, 3.5);
        n.setMealType(NutritionSample.MEAL_LUNCH);
        n.setFoodName("cheese sandwich");

        List<HealthSample> back = roundTrip(n, HealthDataType.NUTRITION);
        assertEquals(1, back.size());
        NutritionSample r = (NutritionSample) back.get(0);
        assertEquals(2, r.getNutrientCount());
        assertEquals(12, r.getNutrient(Nutrient.PROTEIN)
                .getValue(Nutrient.PROTEIN.getUnit()), 0.001);
        assertEquals(NutritionSample.MEAL_LUNCH, r.getMealType());
        // Carried by the in-memory snapshot path and originally missed
        // here, which is the whole failure mode a codec has: a field
        // nobody looks at until a restart.
        assertEquals("cheese sandwich", r.getFoodName());
    }

    @Test
    void aBloodPressureReadingKeepsBothValues() throws Exception {
        BloodPressureSample bp = BloodPressureSample.create(118, 76, T0);
        bp.setPulse(new HealthQuantity(58, HealthUnit.COUNT_PER_MINUTE));

        List<HealthSample> back = roundTrip(bp,
                HealthDataType.BLOOD_PRESSURE);
        assertEquals(1, back.size());
        BloodPressureSample r = (BloodPressureSample) back.get(0);
        assertEquals(118, r.getSystolic().getValue(
                HealthUnit.MILLIMETER_OF_MERCURY), 0.001);
        assertEquals(76, r.getDiastolic().getValue(
                HealthUnit.MILLIMETER_OF_MERCURY), 0.001);
        assertEquals(58, r.getPulse().getValue(HealthUnit.COUNT_PER_MINUTE),
                0.001);
    }

    /**
     * A restored sample brings its identifier back, so the generator has
     * to clear it. Handing out an id something already holds makes a
     * delete-by-id remove the wrong record.
     */
    @Test
    void identifiersAreNotReusedAfterARestart() throws Exception {
        StoredHealthStore first = new StoredHealthStore();
        first.write(one(QuantitySample.create(HealthDataType.STEPS,
                new HealthQuantity(1, HealthUnit.COUNT), T0,
                T0 + MINUTE))).get();
        List<HealthSample> before = first.readSamples(new SampleQuery()
                .addType(HealthDataType.STEPS)
                .setTimeRange(HealthTimeRange.between(T0 - MINUTE,
                        T0 + 2 * MINUTE))).get();
        String firstId = before.get(0).getId();

        StoredHealthStore second = new StoredHealthStore();
        second.write(one(QuantitySample.create(HealthDataType.STEPS,
                new HealthQuantity(2, HealthUnit.COUNT), T0,
                T0 + MINUTE))).get();
        List<HealthSample> after = second.readSamples(new SampleQuery()
                .addType(HealthDataType.STEPS)
                .setTimeRange(HealthTimeRange.between(T0 - MINUTE,
                        T0 + MINUTE))).get();

        assertEquals(2, after.size());
        assertNotEquals(after.get(0).getId(), after.get(1).getId(),
                "a restored id must not be handed out again");
        assertTrue(after.get(0).getId().equals(firstId)
                        || after.get(1).getId().equals(firstId),
                "the restored sample keeps the id it was written with");
    }

    /** A delete is persisted too, not just the write. */
    @Test
    void aDeleteSurvivesARestart() throws Exception {
        StoredHealthStore first = new StoredHealthStore();
        first.write(one(QuantitySample.create(HealthDataType.STEPS,
                new HealthQuantity(7, HealthUnit.COUNT), T0,
                T0 + MINUTE))).get();
        List<HealthSample> stored = first.readSamples(new SampleQuery()
                .addType(HealthDataType.STEPS)
                .setTimeRange(HealthTimeRange.between(T0 - MINUTE,
                        T0 + MINUTE))).get();
        first.delete(HealthDeleteRequest.byId(HealthDataType.STEPS,
                stored.get(0).getId())).get();

        StoredHealthStore second = new StoredHealthStore();
        assertTrue(second.readSamples(new SampleQuery()
                .addType(HealthDataType.STEPS)
                .setTimeRange(HealthTimeRange.between(T0 - MINUTE,
                        T0 + MINUTE))).get().isEmpty());
    }

    /**
     * A store that cannot be written must not report the write as stored.
     *
     * <p>{@code Storage.writeObject} answers false for a full or
     * unwritable store rather than throwing, and ignoring that let the
     * caller be told a durable write succeeded when the record only ever
     * reached memory -- on the very ports whose whole claim is
     * durability. The change is rolled back so memory and disk agree.</p>
     */
    @Test
    void aFailedPersistFailsTheWriteAndRollsItBack() throws Exception {
        UnwritableStore store = new UnwritableStore();
        AsyncResource<HealthWriteResult> write = store.write(
                one(QuantitySample.create(HealthDataType.STEPS,
                        new HealthQuantity(3, HealthUnit.COUNT), T0,
                        T0 + MINUTE)));

        assertNotNull(errorOf(write), "the write must not look successful");
        assertEquals(HealthError.DATABASE_INACCESSIBLE,
                ((HealthException) errorOf(write)).getError());
        assertTrue(store.readSamples(new SampleQuery()
                .addType(HealthDataType.STEPS)
                .setTimeRange(HealthTimeRange.between(T0 - MINUTE,
                        T0 + 2 * MINUTE))).get().isEmpty(),
                "the rolled-back sample must not linger in memory");
    }

    /** A delete that cannot be persisted puts the records back. */
    @Test
    void aFailedPersistRestoresADelete() throws Exception {
        UnwritableStore store = new UnwritableStore();
        store.allowWrites = true;
        store.write(one(QuantitySample.create(HealthDataType.STEPS,
                new HealthQuantity(3, HealthUnit.COUNT), T0,
                T0 + MINUTE))).get();
        List<HealthSample> stored = store.readSamples(new SampleQuery()
                .addType(HealthDataType.STEPS)
                .setTimeRange(HealthTimeRange.between(T0 - MINUTE,
                        T0 + 2 * MINUTE))).get();
        assertEquals(1, stored.size());

        store.allowWrites = false;
        AsyncResource<Integer> delete = store.delete(
                HealthDeleteRequest.byId(HealthDataType.STEPS,
                        stored.get(0).getId()));

        assertNotNull(errorOf(delete));
        assertEquals(1, store.readSamples(new SampleQuery()
                .addType(HealthDataType.STEPS)
                .setTimeRange(HealthTimeRange.between(T0 - MINUTE,
                        T0 + 2 * MINUTE))).get().size(),
                "the record must come back when the delete cannot be"
                        + " persisted");
    }

    /**
     * An empty string survives a restart, and stays distinct from null.
     *
     * <p>They used to encode identically -- both wrote nothing, and nothing
     * read back as null -- so a key deliberately set to "" was deleted on
     * restore rather than restored empty. {@code putMetadata} treats a null
     * value as a removal and an empty value as a stored presence marker, and
     * the setters and getters for titles, notes and source fields draw the
     * same distinction, so collapsing the two changed data behind the app's
     * back.</p>
     */
    @Test
    void anEmptyStringSurvivesPersistenceAndStaysDistinctFromNull()
            throws Exception {
        StoredHealthStore store = new StoredHealthStore();
        QuantitySample s = QuantitySample.create(HealthDataType.STEPS,
                new HealthQuantity(10, HealthUnit.COUNT), 1000L, 2000L);
        s.putMetadata("marker", "");
        s.putMetadata("named", "value");
        store.write(one(s)).get();

        StoredHealthStore reopened = new StoredHealthStore();
        List<HealthSample> back = reopened.readSamples(new SampleQuery()
                .addType(HealthDataType.STEPS)
                .setTimeRange(HealthTimeRange.between(0L, 10_000L))).get();
        assertEquals(1, back.size());

        Map<String, String> meta = back.get(0).getMetadata();
        assertTrue(meta.containsKey("marker"),
                "an empty value is a stored marker, not a removal");
        assertEquals("", meta.get("marker"));
        assertEquals("value", meta.get("named"));
        assertFalse(meta.containsKey("absent"),
                "and a key never set stays absent");
    }

    /**
     * An unreadable store is not an empty one, and must not be overwritten.
     *
     * <p>{@code Storage.readObject} catches its own failures and answers
     * null, so a corrupt or briefly unreadable entry is indistinguishable
     * from no entry. Restoring empty is right for the second and catastrophic
     * for the first: the next write replaced the same key with only the
     * samples that session had added, so a transient read failure permanently
     * deleted the user's history.</p>
     *
     * <p>The entry is left intact and writes are refused, which
     * {@code persist()} already surfaces to the caller by failing and rolling
     * the change back.</p>
     */
    @Test
    void anUnreadableStoreRefusesWritesRatherThanReplacingItself()
            throws Exception {
        // Something present under the key that the codec cannot decode into
        // a blob -- readObject answers null for it, exists() answers true.
        Storage.getInstance().writeObject("cn1$health$local",
                Integer.valueOf(7));

        StoredHealthStore store = new StoredHealthStore();
        QuantitySample s = QuantitySample.create(HealthDataType.STEPS,
                new HealthQuantity(10, HealthUnit.COUNT), 1000L, 2000L);

        assertNotNull(errorOf(store.write(one(s))),
                "a write must fail while the store cannot be read");
        assertTrue(Storage.getInstance().exists("cn1$health$local"),
                "and the unreadable entry must still be there");
    }

    private static Throwable errorOf(AsyncResource<?> r) {
        // Settled first. Results are delivered on the EDT on every backend
        // now, so an off-EDT caller sees the error queued rather than already
        // attached, and reading it without waiting found nothing.
        HealthAwait.settled(r);
        final Throwable[] err = new Throwable[1];
        r.except(new com.codename1.util.SuccessCallback<Throwable>() {
            public void onSucess(Throwable t) {
                err[0] = t;
            }
        });
        return err[0];
    }

    /** A store whose backing storage refuses to take anything. */
    private static final class UnwritableStore
            extends com.codename1.impl.health.LocalHealthStore {

        private boolean allowWrites;

        @Override
        protected boolean persist() {
            return allowWrites;
        }
    }

    /**
     * A failed save must not take the older records with it.
     *
     * <p>{@code Storage.writeObject} deletes the entry when it fails, so
     * by the time it answers false the previous contents are already
     * gone. Rolling the in-memory change back is not enough on its own --
     * without putting the old blob back, one full disk costs every record
     * the app ever wrote rather than the single write that failed.</p>
     */
    @Test
    void aFailedSaveKeepsTheRecordsThatWereAlreadyStored() throws Exception {
        StoredHealthStore first = new StoredHealthStore();
        first.write(one(QuantitySample.create(HealthDataType.STEPS,
                new HealthQuantity(11, HealthUnit.COUNT), T0,
                T0 + MINUTE))).get();

        // A second store over the same entry whose next save fails the
        // way a full disk does -- Storage deletes the entry and then
        // answers false.
        FailingOnceStore second = new FailingOnceStore();
        AsyncResource<HealthWriteResult> doomed = second.write(
                one(QuantitySample.create(HealthDataType.STEPS,
                        new HealthQuantity(22, HealthUnit.COUNT), T0,
                        T0 + MINUTE)));
        assertNotNull(errorOf(doomed), "the write must be reported failed");

        // Whatever the failure did, the earlier record must still be
        // readable by a fresh store.
        StoredHealthStore third = new StoredHealthStore();
        List<HealthSample> back = third.readSamples(new SampleQuery()
                .addType(HealthDataType.STEPS)
                .setTimeRange(HealthTimeRange.between(T0 - MINUTE,
                        T0 + 2 * MINUTE))).get();
        assertEquals(1, back.size(),
                "the previously stored record must survive a failed save");
        assertEquals(11, ((QuantitySample) back.get(0))
                .getValue(HealthUnit.COUNT), 0.001);
    }

    /**
     * Fails its first save exactly as a full disk does -- deleting the
     * entry first, then answering false -- and writes normally after.
     */
    private static final class FailingOnceStore extends StoredHealthStore {

        private boolean failed;

        @Override
        protected boolean writeBlob(String blob) {
            if (!failed) {
                failed = true;
                Storage.getInstance()
                        .deleteStorageFile("cn1$health$local");
                return false;
            }
            return super.writeBlob(blob);
        }
    }

    /**
     * A mutation and the save it triggers are one transaction.
     *
     * <p>Holding only the sample-list lock released it between changing
     * the list and encoding it, so two concurrent mutations could each
     * snapshot and then write in the opposite order -- the older snapshot
     * landing last. Both callers were told they had succeeded, and a
     * record deleted before the restart was back after it.</p>
     *
     * <p>Driven through a store that stalls inside the save, which is the
     * window the race needs; without the transaction the stalled writer
     * overwrites the delete that completed while it was parked.</p>
     */
    @Test
    void aMutationAndItsSaveAreOneTransaction() throws Exception {
        final StallingStore store = new StallingStore();
        store.write(one(QuantitySample.create(HealthDataType.STEPS,
                new HealthQuantity(1, HealthUnit.COUNT), T0,
                T0 + MINUTE))).get();
        List<HealthSample> stored = store.readSamples(query()).get();
        assertEquals(1, stored.size());
        final String firstId = stored.get(0).getId();

        // A second write that parks inside persist, and a delete of the
        // first record racing it.
        store.stallNextSave = true;
        final Throwable[] failure = new Throwable[1];
        Thread writer = new Thread(new Runnable() {
            public void run() {
                try {
                    store.write(one(QuantitySample.create(
                            HealthDataType.STEPS,
                            new HealthQuantity(2, HealthUnit.COUNT),
                            T0 + MINUTE, T0 + 2 * MINUTE))).get();
                } catch (Throwable t) {
                    failure[0] = t;
                }
            }
        });
        writer.start();
        store.awaitStall();
        final java.util.concurrent.CountDownLatch deleted =
                new java.util.concurrent.CountDownLatch(1);
        Thread deleter = new Thread(new Runnable() {
            public void run() {
                try {
                    store.delete(HealthDeleteRequest.byId(
                            HealthDataType.STEPS, firstId)).get();
                    deleted.countDown();
                } catch (Throwable t) {
                    failure[0] = t;
                }
            }
        });
        deleter.start();
        // Wait for the delete to reach a decisive state rather than for a
        // wall-clock interval: under the transaction it blocks on the
        // same lock the parked write holds, and without it it runs to
        // completion and the parked write then lands its stale snapshot
        // on top. Timing out here would make the test pass for the wrong
        // reason, so the loop below insists on one of the two.
        Thread.State reached = awaitBlockedOrDone(deleter);
        assertNotNull(reached, "the delete neither blocked nor finished");
        store.releaseStall();
        writer.join(10000);
        deleter.join(10000);
        assertNull(failure[0]);

        // Whatever order they ran in, disk and memory must agree.
        List<String> inMemory = new ArrayList<String>();
        for (HealthSample s : store.readSamples(query()).get()) {
            inMemory.add(s.getId());
        }
        List<String> onDisk = new ArrayList<String>();
        for (HealthSample s : new StoredHealthStore()
                .readSamples(query()).get()) {
            onDisk.add(s.getId());
        }
        assertEquals(inMemory, onDisk,
                "the saved store must match the one in memory");
    }

    /**
     * Waits until {@code t} is either blocked on a monitor or finished,
     * and reports which. Returns null if it does neither in time.
     */
    private static Thread.State awaitBlockedOrDone(Thread t)
            throws InterruptedException {
        for (int iter = 0; iter < 500; iter++) {
            Thread.State state = t.getState();
            if (state == Thread.State.BLOCKED
                    || state == Thread.State.TERMINATED) {
                return state;
            }
            Thread.sleep(10);
        }
        return null;
    }

    private static SampleQuery query() {
        return new SampleQuery().addType(HealthDataType.STEPS)
                .setTimeRange(HealthTimeRange.between(T0 - MINUTE,
                        T0 + 10 * MINUTE));
    }

    /** A store whose save can be parked, opening the race window. */
    private static final class StallingStore extends StoredHealthStore {

        private volatile boolean stallNextSave;
        private final java.util.concurrent.CountDownLatch stalled =
                new java.util.concurrent.CountDownLatch(1);
        private final java.util.concurrent.CountDownLatch release =
                new java.util.concurrent.CountDownLatch(1);

        void awaitStall() throws InterruptedException {
            assertTrue(stalled.await(10, java.util.concurrent.TimeUnit
                    .SECONDS), "the writer never reached the save");
        }

        void releaseStall() {
            release.countDown();
        }

        @Override
        protected boolean writeBlob(String blob) {
            if (stallNextSave) {
                stallNextSave = false;
                stalled.countDown();
                try {
                    release.await(10,
                            java.util.concurrent.TimeUnit.SECONDS);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
            return super.writeBlob(blob);
        }
    }
}
