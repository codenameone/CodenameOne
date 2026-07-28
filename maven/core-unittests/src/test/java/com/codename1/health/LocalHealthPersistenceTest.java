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
import com.codename1.impl.health.StoredHealthStore;
import com.codename1.io.Storage;
import com.codename1.junit.UITestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

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
}
