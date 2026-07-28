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

    /**
     * The stores write whatever they are handed, so a total in the wrong
     * dimension round-tripped intact and only blew up in the caller that
     * did the documented thing and asked for kilocalories.
     */
    @Test
    void workoutTotalsRejectTheWrongDimension() {
        WorkoutSample w = WorkoutSample.create(WorkoutActivityType.RUNNING,
                1767225600000L, 1767225600000L + 60000L);
        assertThrows(IllegalArgumentException.class,
                () -> w.setTotalEnergy(
                        new HealthQuantity(70, HealthUnit.KILOGRAM)));
        assertThrows(IllegalArgumentException.class,
                () -> w.setTotalDistance(
                        new HealthQuantity(500, HealthUnit.KILOCALORIE)));
        assertNull(w.getTotalEnergy());
        assertNull(w.getTotalDistance());
    }

    /**
     * Active duration excludes pauses, so it is a subset of the span and
     * cannot exceed it. Stored unchecked, every pace and average drawn
     * from the workout was wrong in a way that looks like a units bug
     * somewhere else.
     */
    @Test
    void activeDurationCannotExceedTheWorkoutItself() {
        WorkoutSample w = WorkoutSample.create(WorkoutActivityType.RUNNING,
                1767225600000L, 1767225600000L + 60000L);
        assertThrows(IllegalArgumentException.class,
                () -> w.setActiveDurationMillis(60001L));
        // The whole span is legal -- a workout with no pauses.
        w.setActiveDurationMillis(60000L);
        assertEquals(60000L, w.getActiveDurationMillis());
        // And a negative still means "not reported separately", which
        // reads back as the wall duration.
        w.setActiveDurationMillis(-1L);
        assertEquals(60000L, w.getActiveDurationMillis());
    }

    /** Any unit of the right dimension is accepted, and null still means
     *  the total was never measured. */
    @Test
    void workoutTotalsAcceptAnyUnitOfTheRightDimension() {
        WorkoutSample w = WorkoutSample.create(WorkoutActivityType.RUNNING,
                1767225600000L, 1767225600000L + 60000L);
        w.setTotalEnergy(new HealthQuantity(2000, HealthUnit.KILOJOULE));
        w.setTotalDistance(new HealthQuantity(5, HealthUnit.KILOMETER));
        assertEquals(5000,
                w.getTotalDistance().in(HealthUnit.METER).getValue(
                        HealthUnit.METER), 0.001);
        w.setTotalEnergy(null);
        assertNull(w.getTotalEnergy());
    }

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
                .setTitle("Evening ride");
        assertEquals(WorkoutActivityType.CYCLING, c.getActivityType());
        assertEquals("Evening ride", c.getTitle());
        // Keepalive is covered on its own below: it is refused in this
        // release, and this case asserted that it was stored.
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

    /**
     * A recorded workout says what it could not store.
     *
     * <p>Health Connect has no single-value write form for the
     * series-shaped types -- power, speed and both cadences -- which is
     * exactly what a bike or foot pod feeds into a workout. Those samples
     * were dropped on the way to the store while {@code end()} resolved as
     * though nothing had happened. They still cannot be stored, but the
     * workout now names them so the app can keep them itself.</p>
     */
    @Test
    void aRecordedWorkoutReportsSamplesItCouldNotStore() throws Exception {
        final FakeHealthStore store = new FakeHealthStore();
        // The mobile shape: the workout record itself has no write form,
        // and neither do the series-backed sensor types.
        store.unwritable.add(HealthDataType.WORKOUT);
        store.unwritable.add(HealthDataType.POWER);
        implementation.setHealth(new Health() {
            @Override
            public boolean isSupported() {
                return true;
            }

            @Override
            public HealthStore getStore() {
                return store;
            }
        });
        try {
            WorkoutManager workouts = Health.getInstance().getWorkouts();
            WorkoutSession session = workouts.startSession(
                    new WorkoutConfiguration().setActivityType(
                            WorkoutActivityType.CYCLING)).get();
            session.start();
            List<HealthSample> fed = new ArrayList<HealthSample>();
            fed.add(FakeHealthStore.sample(HealthDataType.POWER,
                    1000L, 1000L, 210));
            fed.add(FakeHealthStore.sample(HealthDataType.ACTIVE_ENERGY,
                    1000L, 2000L, 12));
            session.addSamples(fed).get();

            WorkoutSample done = session.end().get();
            assertEquals("power", done.getMetadata().get(
                    WorkoutSample.SAMPLES_NOT_PERSISTED),
                    "the workout must name what it could not store");
            assertEquals("true", done.getMetadata().get(
                    WorkoutSample.WORKOUT_NOT_PERSISTED),
                    "and say that the session record itself was not stored");
            // Nothing writable at all -- the ordinary no-sensor workout on
            // both mobile platforms. That path returns early, and used to
            // come back with neither an id nor any explanation.
            WorkoutSession bare = workouts.startSession(
                    new WorkoutConfiguration().setActivityType(
                            WorkoutActivityType.WALKING)).get();
            bare.start();
            WorkoutSample bareDone = bare.end().get();
            assertEquals("true", bareDone.getMetadata().get(
                    WorkoutSample.WORKOUT_NOT_PERSISTED),
                    "an empty recorded workout still says it was not"
                            + " persisted");
        } finally {
            implementation.setHealth(null);
        }
    }

    /**
     * A series fed to a workout counts toward its statistics, and LATEST
     * follows the timestamp rather than the arrival order.
     *
     * <p>The rollup skipped anything that was not a {@code QuantitySample},
     * so a heart-rate trace left AVERAGE, MINIMUM and MAXIMUM null for
     * data the session was collecting and would go on to persist. And
     * LATEST was replaced by whatever arrived last, so a delayed device
     * reading or an unsorted history batch reported a stale value as the
     * newest.</p>
     */
    @Test
    void seriesSamplesCountAndLatestFollowsTheClock() throws Exception {
        WorkoutSession s = startedSession();

        long[] at = {5000L, 6000L, 7000L};
        double[] bpm = {60, 80, 70};
        List<HealthSample> fed = new ArrayList<HealthSample>();
        fed.add(SeriesSample.create(HealthDataType.HEART_RATE, at[0], at[2],
                at, at, bpm, HealthUnit.COUNT_PER_MINUTE));
        s.addSamples(fed).get();

        assertEquals(60.0, s.getStatistic(HealthDataType.HEART_RATE,
                AggregateMetric.MINIMUM).getValue(
                        HealthUnit.COUNT_PER_MINUTE), 1e-9);
        assertEquals(80.0, s.getStatistic(HealthDataType.HEART_RATE,
                AggregateMetric.MAXIMUM).getValue(
                        HealthUnit.COUNT_PER_MINUTE), 1e-9);
        assertEquals(70.0, s.getStatistic(HealthDataType.HEART_RATE,
                AggregateMetric.LATEST).getValue(
                        HealthUnit.COUNT_PER_MINUTE), 1e-9,
                "the last measurement in the series is the latest");

        // An older reading arriving afterwards must not become "latest".
        List<HealthSample> late = new ArrayList<HealthSample>();
        late.add(FakeHealthStore.sample(HealthDataType.HEART_RATE,
                3000L, 3000L, 55));
        s.addSamples(late).get();
        assertEquals(70.0, s.getStatistic(HealthDataType.HEART_RATE,
                AggregateMetric.LATEST).getValue(
                        HealthUnit.COUNT_PER_MINUTE), 1e-9,
                "a delayed older reading is not the newest one");
        assertEquals(55.0, s.getStatistic(HealthDataType.HEART_RATE,
                AggregateMetric.MINIMUM).getValue(
                        HealthUnit.COUNT_PER_MINUTE), 1e-9,
                "but it still counts everywhere else");
    }

    /**
     * Background keepalive is not implemented anywhere in this release,
     * so the setter refuses it rather than storing a request nothing
     * honours.
     *
     * <p>An accepted-but-ignored flag is the worst of the options here:
     * the app cannot tell it was ignored until a user loses a workout to
     * a suspended process.</p>
     */
    @Test
    void backgroundKeepAliveIsRefusedRatherThanIgnored() {
        WorkoutConfiguration c = new WorkoutConfiguration()
                .setActivityType(WorkoutActivityType.RUNNING);
        assertThrows(IllegalArgumentException.class,
                () -> c.setKeepAliveInBackground(true));
        // False is the default and stays accepted.
        c.setKeepAliveInBackground(false);
        assertFalse(c.isKeepAliveInBackground());
    }
}
