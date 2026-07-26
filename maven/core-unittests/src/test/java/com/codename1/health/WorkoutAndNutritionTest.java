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
import com.codename1.health.workout.WorkoutConfiguration;
import com.codename1.health.workout.WorkoutEvent;
import com.codename1.health.workout.WorkoutManager;
import com.codename1.health.workout.WorkoutSession;
import com.codename1.health.workout.WorkoutSessionState;
import com.codename1.junit.UITestBase;
import com.codename1.util.AsyncResource;
import com.codename1.util.SuccessCallback;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Workout recording and nutrition logging. */
class WorkoutAndNutritionTest extends UITestBase {

    private static Throwable errorOf(AsyncResource<?> r) {
        final Throwable[] err = new Throwable[1];
        r.except(new SuccessCallback<Throwable>() {
            public void onSucess(Throwable t) {
                err[0] = t;
            }
        });
        return err[0];
    }

    private static WorkoutSession startedSession() {
        WorkoutManager m = new WorkoutManager();
        WorkoutSession s = m.startSession(new WorkoutConfiguration()
                .setActivityType(WorkoutActivityType.RUNNING)).get();
        s.start();
        return s;
    }

    // ---- workouts ----

    @Test
    void aFreshManagerReportsNoLiveSupportAndRecordsInstead() {
        WorkoutManager m = new WorkoutManager();
        assertFalse(m.isLiveSessionSupported());
        assertFalse(m.isSensorCollectionSupported());
        WorkoutSession s = m.startSession(new WorkoutConfiguration()).get();
        assertNotNull(s);
        assertFalse(s.isLive(),
                "a recorded session must say so, so UI does not promise a"
                        + " heart rate that will never arrive");
    }

    @Test
    void stateMachineFollowsTheLifecycle() {
        WorkoutManager m = new WorkoutManager();
        WorkoutSession s = m.startSession(new WorkoutConfiguration()).get();
        assertEquals(WorkoutSessionState.NOT_STARTED, s.getState());
        s.start();
        assertEquals(WorkoutSessionState.RUNNING, s.getState());
        s.pause();
        assertEquals(WorkoutSessionState.PAUSED, s.getState());
        s.resume();
        assertEquals(WorkoutSessionState.RUNNING, s.getState());
    }

    /**
     * Illegal transitions fail with a typed error rather than throwing or
     * silently doing nothing.
     */
    @Test
    void illegalTransitionsFailWithSessionState() {
        WorkoutManager m = new WorkoutManager();
        WorkoutSession s = m.startSession(new WorkoutConfiguration()).get();

        Throwable err = errorOf(s.pause());
        assertNotNull(err, "pausing a session that never started");
        assertEquals(HealthError.SESSION_STATE,
                ((HealthException) err).getError());

        err = errorOf(s.end());
        assertNotNull(err, "ending a session that never started");
        assertEquals(HealthError.SESSION_STATE,
                ((HealthException) err).getError());
    }

    @Test
    void onlyOneSessionMayRunAtATime() {
        WorkoutManager m = new WorkoutManager();
        m.startSession(new WorkoutConfiguration()).get();
        Throwable err = errorOf(m.startSession(new WorkoutConfiguration()));
        assertNotNull(err, "an abandoned workout is data the user believed"
                + " was being recorded");
        assertEquals(HealthError.SESSION_STATE,
                ((HealthException) err).getError());
    }

    @Test
    void discardFreesTheManagerForANewSession() {
        WorkoutManager m = new WorkoutManager();
        WorkoutSession s = m.startSession(new WorkoutConfiguration()).get();
        s.discard();
        assertNull(m.getActiveSession());
        assertNull(errorOf(m.startSession(new WorkoutConfiguration())));
    }

    /**
     * A statistic nothing was fed for is null, not zero -- showing "0 bpm"
     * would be a claim the app cannot support.
     */
    @Test
    void unfedStatisticsAreNullRatherThanZero() {
        WorkoutSession s = startedSession();
        assertNull(s.getStatistic(HealthDataType.HEART_RATE,
                AggregateMetric.AVERAGE));
        assertNull(s.getStatistic(HealthDataType.ACTIVE_ENERGY,
                AggregateMetric.TOTAL));
    }

    @Test
    void fedSamplesRollUpIntoStatistics() {
        WorkoutSession s = startedSession();
        List<HealthSample> samples = new ArrayList<HealthSample>();
        samples.add(QuantitySample.create(HealthDataType.HEART_RATE,
                new HealthQuantity(60, HealthUnit.COUNT_PER_MINUTE), 1000L));
        samples.add(QuantitySample.create(HealthDataType.HEART_RATE,
                new HealthQuantity(80, HealthUnit.COUNT_PER_MINUTE), 2000L));
        s.addSamples(samples);

        assertEquals(70.0, s.getStatistic(HealthDataType.HEART_RATE,
                AggregateMetric.AVERAGE)
                .getValue(HealthUnit.COUNT_PER_MINUTE), 1e-9);
        assertEquals(60.0, s.getStatistic(HealthDataType.HEART_RATE,
                AggregateMetric.MINIMUM)
                .getValue(HealthUnit.COUNT_PER_MINUTE), 1e-9);
        assertEquals(80.0, s.getStatistic(HealthDataType.HEART_RATE,
                AggregateMetric.MAXIMUM)
                .getValue(HealthUnit.COUNT_PER_MINUTE), 1e-9);
        assertEquals(80.0, s.getStatistic(HealthDataType.HEART_RATE,
                AggregateMetric.LATEST)
                .getValue(HealthUnit.COUNT_PER_MINUTE), 1e-9);
    }

    @Test
    void cumulativeSamplesTotalRatherThanAverage() {
        WorkoutSession s = startedSession();
        List<HealthSample> samples = new ArrayList<HealthSample>();
        samples.add(QuantitySample.create(HealthDataType.ACTIVE_ENERGY,
                new HealthQuantity(100, HealthUnit.KILOCALORIE),
                1000L, 2000L));
        samples.add(QuantitySample.create(HealthDataType.ACTIVE_ENERGY,
                new HealthQuantity(50, HealthUnit.KILOCALORIE),
                2000L, 3000L));
        s.addSamples(samples);

        assertEquals(150.0, s.getStatistic(HealthDataType.ACTIVE_ENERGY,
                AggregateMetric.TOTAL).getValue(HealthUnit.KILOCALORIE),
                1e-9);
    }

    @Test
    void eventsAreRecordedInOrder() {
        WorkoutSession s = startedSession();
        s.addEvent(WorkoutEvent.lap(1000L));
        s.addEvent(WorkoutEvent.marker(2000L, "hill"));
        List<WorkoutEvent> events = s.getEvents();
        // start() records nothing, but pause/resume do; only ours here.
        assertEquals(2, events.size());
        assertEquals(WorkoutEvent.Kind.LAP, events.get(0).getKind());
        assertEquals("hill", events.get(1).getLabel());
    }

    @Test
    void pausingRecordsAnEventAndStopsTheElapsedClock() throws Exception {
        WorkoutSession s = startedSession();
        s.pause();
        long afterPause = s.getElapsedMillis();
        Thread.sleep(30);
        assertEquals(afterPause, s.getElapsedMillis(),
                "the elapsed clock must not advance while paused");
        boolean sawPause = false;
        for (WorkoutEvent e : s.getEvents()) {
            if (e.getKind() == WorkoutEvent.Kind.PAUSE) {
                sawPause = true;
            }
        }
        assertTrue(sawPause);
    }

    @Test
    void configurationCarriesTheActivityAndLocation() {
        WorkoutConfiguration c = new WorkoutConfiguration()
                .setActivityType(WorkoutActivityType.CYCLING)
                .setLocationType(
                        com.codename1.health.workout.WorkoutLocationType
                                .OUTDOOR)
                .setKeepAliveInBackground(true)
                .setTitle("Evening ride");
        assertEquals(WorkoutActivityType.CYCLING, c.getActivityType());
        assertTrue(c.isKeepAliveInBackground());
        assertEquals("Evening ride", c.getTitle());
    }

    @Test
    void workoutSampleTotalsAreNullUntilSet() {
        WorkoutSample w = WorkoutSample.create(WorkoutActivityType.RUNNING,
                1000L, 5000L);
        assertNull(w.getTotalEnergy(),
                "an unmeasured energy total must not read as 0 kcal");
        assertNull(w.getTotalDistance());
        assertEquals(4000L, w.getActiveDurationMillis(),
                "active duration falls back to the wall duration");
    }

    // ---- nutrition ----

    @Test
    void nutritionSampleIsSparse() {
        NutritionSample lunch = NutritionSample.create(1000L, 2000L)
                .setFoodName("Chicken salad")
                .setMealType(NutritionSample.MEAL_LUNCH)
                .setNutrient(Nutrient.ENERGY, 420)
                .setNutrient(Nutrient.PROTEIN, 35);

        assertEquals(2, lunch.getNutrientCount());
        assertTrue(lunch.hasNutrient(Nutrient.ENERGY));
        assertFalse(lunch.hasNutrient(Nutrient.SODIUM));
        assertEquals(NutritionSample.MEAL_LUNCH, lunch.getMealType());
        assertEquals("Chicken salad", lunch.getFoodName());
    }

    /**
     * An unmeasured nutrient is null, not zero -- the distinction matters
     * to someone managing their sodium.
     */
    @Test
    void unmeasuredNutrientsReadAsNull() {
        NutritionSample s = NutritionSample.create(1000L, 2000L)
                .setNutrient(Nutrient.ENERGY, 100);
        assertNull(s.getNutrient(Nutrient.SODIUM));
        assertNotNull(s.getNutrient(Nutrient.ENERGY));
    }

    @Test
    void nutrientAmountsUseTheNutrientsOwnUnit() {
        NutritionSample s = NutritionSample.create(1000L, 2000L)
                .setNutrient(Nutrient.SODIUM, 610);
        assertSame(HealthUnit.MILLIGRAM,
                s.getNutrient(Nutrient.SODIUM).getUnit());
        assertEquals(610, s.getNutrient(Nutrient.SODIUM)
                .getValue(HealthUnit.MILLIGRAM), 1e-9);
    }

    @Test
    void nutrientAmountsConvertFromAnExplicitUnit() {
        NutritionSample s = NutritionSample.create(1000L, 2000L)
                .setNutrient(Nutrient.SODIUM, 1.0, HealthUnit.GRAM);
        assertEquals(1000.0, s.getNutrient(Nutrient.SODIUM)
                .getValue(HealthUnit.MILLIGRAM), 1e-6);
    }

    @Test
    void wrongDimensionNutrientUnitIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> NutritionSample.create(1000L, 2000L)
                        .setNutrient(Nutrient.PROTEIN, 5, HealthUnit.LITER));
    }

    @Test
    void nutrientLookupRoundTripsAndIsTotal() {
        for (Nutrient n : Nutrient.values()) {
            assertSame(n, Nutrient.forId(n.getId()));
            assertNotNull(n.getUnit());
        }
        assertNull(Nutrient.forId("unobtainium"));
        assertNull(Nutrient.forId(null));
    }

    @Test
    void instantaneousNutritionStillSpansAnInterval() {
        NutritionSample s = NutritionSample.create(1000L);
        assertFalse(s.isInstantaneous(),
                "nutrition is an interval-only type");
        assertSame(HealthDataType.NUTRITION, s.getType());
    }
}
