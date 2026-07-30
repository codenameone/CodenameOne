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
package com.codename1.health.workout;

import com.codename1.health.AggregateMetric;
import com.codename1.health.HealthDataType;
import com.codename1.health.HealthError;
import com.codename1.health.HealthException;
import com.codename1.health.HealthQuantity;
import com.codename1.health.HealthSample;
import com.codename1.health.HealthUnit;
import com.codename1.health.QuantitySample;
import com.codename1.health.SeriesSample;
import com.codename1.health.WorkoutSample;
import com.codename1.ui.Display;
import com.codename1.util.AsyncResource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.codename1.impl.health.OneShot;

/// A workout being recorded.
///
/// Obtained from
/// [WorkoutManager#startSession(WorkoutConfiguration)]. The state machine,
/// the elapsed clock and the rollup of fed samples live here in shared
/// code; ports implement only the `do*` methods.
///
/// #### Live and recorded sessions
///
/// [#isLive()] tells you whether the operating system is running a real
/// workout session -- keeping the app alive and collecting sensor data
/// itself. **It is false everywhere in this release**: the only
/// implementation here is a recorded session, and
/// [WorkoutManager#isLiveSessionSupported()] says so on every platform.
/// `HKWorkoutSession` on watchOS and iOS 26, and the Wear OS exercise
/// client, are what would change that answer; nothing here drives them
/// yet. Do not assume the OS keeps your app alive or collects data for
/// you -- backgrounding can end the process and take the session with it.
///
/// On an Android phone it could not be live in any case, because Health
/// Connect has no such concept: Google's documented approach there is to
/// record the session yourself and write it when it ends, which is exactly
/// what a recorded session does. So a recorded session is not a degraded
/// shim -- it is the platform-correct design -- but it does mean **nothing is collected
/// unless you feed it**, through [#addSamples(List)] or by attaching a
/// Bluetooth sensor with
/// [com.codename1.health.sensors.SensorSessionOptions#setWorkoutSession(WorkoutSession)].
///
/// Check [WorkoutManager#isSensorCollectionSupported()] before building UI
/// that assumes a heart rate will appear on its own.
///
/// #### A killed workout is over
///
/// Sessions are deliberately not restored after the process dies. `end()`
/// was never called, so nothing is written. An app that needs
/// crash-resilient workouts should write partial records as it goes --
/// which is what the recorded path does anyway.
public abstract class WorkoutSession {

    private final WorkoutConfiguration configuration;
    private final List<WorkoutSessionListener> listeners =
            new ArrayList<WorkoutSessionListener>();
    private final Map<String, HealthQuantity> statistics =
            new HashMap<String, HealthQuantity>();
    private final Map<String, Integer> counts = new HashMap<String, Integer>();
    private final List<WorkoutEvent> events = new ArrayList<WorkoutEvent>();

    /// Guards the state and the clocks it is read alongside.
    ///
    /// Samples arrive on a Bluetooth callback thread while the transitions
    /// are driven from the EDT, so reading the state and then acting on it
    /// is two steps with the other thread able to run between them -- and
    /// on a field neither side synchronizes, the sensor thread is not
    /// obliged to observe the transition at all.
    private final Object stateLock = new Object();

    private WorkoutSessionState state = WorkoutSessionState.NOT_STARTED;
    private long startedAtMillis;
    private long endedAtMillis;
    private long accumulatedMillis;
    private long resumedAtMillis;

    /// Ports and the framework construct sessions.
    protected WorkoutSession(WorkoutConfiguration configuration) {
        // A snapshot. The builder is fluent and callers reuse one across
        // workouts, and doEnd reads the activity type and title only when
        // the session ends -- so a run was persisted under whatever the
        // configuration had been changed to since it started.
        this.configuration = configuration == null
                ? new WorkoutConfiguration() : configuration.copy();
    }

    /// The configuration this session was started with.
    ///
    /// A copy, so that reconfiguring what this returns cannot change a
    /// workout already under way -- the same reason the session took a
    /// snapshot of what it was given. Without it the constructor's
    /// snapshot only moved the problem: `getConfiguration().setTitle(..)`
    /// reached the very instance `doEnd` reads when it persists.
    public final WorkoutConfiguration getConfiguration() {
        return configuration.copy();
    }

    /// The session's own copy, which is not handed out.
    final WorkoutConfiguration configuration() {
        return configuration;
    }

    /// The current state.
    public final WorkoutSessionState getState() {
        synchronized (stateLock) {
            return state;
        }
    }

    /// Whether the operating system is running a real workout session --
    /// see the class documentation. `false` means the clock and the saved
    /// record are real but collection is entirely up to you.
    public boolean isLive() {
        return false;
    }

    /// Warms up sensors ahead of [#start()], for apps that show a
    /// countdown. Optional; `start()` works without it.
    public final AsyncResource<Boolean> prepare() {
        AsyncResource<Boolean> out = new OneShot<Boolean>();
        synchronized (stateLock) {
            if (!requireState(out, WorkoutSessionState.NOT_STARTED,
                    "prepare")) {
                return out;
            }
            setState(WorkoutSessionState.PREPARING);
        }
        doPrepare(out);
        return out;
    }

    /// Starts recording.
    public final AsyncResource<Boolean> start() {
        AsyncResource<Boolean> out = new OneShot<Boolean>();
        synchronized (stateLock) {
            if (state != WorkoutSessionState.NOT_STARTED
                    && state != WorkoutSessionState.PREPARING) {
                failState(out, "start");
                return out;
            }
            startedAtMillis = System.currentTimeMillis();
            resumedAtMillis = startedAtMillis;
            accumulatedMillis = 0;
            setState(WorkoutSessionState.RUNNING);
        }
        doStart(out);
        return out;
    }

    /// Pauses recording. The elapsed clock stops.
    public final AsyncResource<Boolean> pause() {
        AsyncResource<Boolean> out = new OneShot<Boolean>();
        synchronized (stateLock) {
            if (!requireState(out, WorkoutSessionState.RUNNING, "pause")) {
                return out;
            }
            accumulatedMillis += System.currentTimeMillis() - resumedAtMillis;
            setState(WorkoutSessionState.PAUSED);
        }
        addEvent(new WorkoutEvent(WorkoutEvent.Kind.PAUSE,
                System.currentTimeMillis(), null));
        doPause(out);
        return out;
    }

    /// Resumes after a pause.
    public final AsyncResource<Boolean> resume() {
        AsyncResource<Boolean> out = new OneShot<Boolean>();
        long resumedAt;
        synchronized (stateLock) {
            if (!requireState(out, WorkoutSessionState.PAUSED, "resume")) {
                return out;
            }
            resumedAtMillis = System.currentTimeMillis();
            resumedAt = resumedAtMillis;
            setState(WorkoutSessionState.RUNNING);
        }
        addEvent(new WorkoutEvent(WorkoutEvent.Kind.RESUME,
                resumedAt, null));
        doResume(out);
        return out;
    }

    /// Ends the workout and writes it to the health store, resolving with
    /// the persisted record.
    public final AsyncResource<WorkoutSample> end() {
        AsyncResource<WorkoutSample> out = new OneShot<WorkoutSample>();
        // The whole claim under one lock, so a sample being accepted on a
        // sensor thread is either already in what doEnd is about to
        // snapshot or is refused. Checked separately, a reading could pass
        // the state check, wait, and then roll itself into totals whose
        // workout had already been written -- reported as accepted while
        // absent from the record, or present in the totals with no sample
        // behind it.
        synchronized (stateLock) {
            if (state != WorkoutSessionState.RUNNING
                    && state != WorkoutSessionState.PAUSED) {
                out.error(new HealthException(HealthError.SESSION_STATE,
                        "cannot end a workout in state " + state));
                return out;
            }
            if (state == WorkoutSessionState.RUNNING) {
                accumulatedMillis +=
                        System.currentTimeMillis() - resumedAtMillis;
            }
            endedAtMillis = System.currentTimeMillis();
            setState(WorkoutSessionState.STOPPED);
        }
        doEnd(out);
        return out;
    }

    /// Abandons the workout without writing anything.
    ///
    /// A no-op once the workout has ended, and once [#end()] has been
    /// called it is too late: the store write is already in flight, and
    /// claiming to have abandoned the session while that write lands --
    /// then having its callback move the state back to `ENDED` -- would
    /// break the one promise in this method's name. Discard before ending,
    /// or delete the samples afterwards through the identifiers the write
    /// reports.
    public final void discard() {
        synchronized (stateLock) {
            if (state == WorkoutSessionState.ENDED
                    || state == WorkoutSessionState.STOPPED) {
                return;
            }
            setState(WorkoutSessionState.FAILED);
        }
        doDiscard();
    }

    /// Time spent recording, excluding pauses.
    public final long getElapsedMillis() {
        synchronized (stateLock) {
            if (state == WorkoutSessionState.RUNNING) {
                return accumulatedMillis
                        + (System.currentTimeMillis() - resumedAtMillis);
            }
            return accumulatedMillis;
        }
    }

    /// When the workout started, epoch millis, or 0 before [#start()].
    public final long getStartedAtMillis() {
        synchronized (stateLock) {
            return startedAtMillis;
        }
    }

    /// When it ended, epoch millis, or 0 while still running.
    public final long getEndedAtMillis() {
        synchronized (stateLock) {
            return endedAtMillis;
        }
    }

    /// A live statistic, or **`null`** when the platform does not compute
    /// it and nothing has been fed in.
    ///
    /// Never fabricates a zero. On a recorded session with no sensor
    /// attached every statistic is null, and showing "0 bpm" instead would
    /// be a claim the app cannot support.
    public final HealthQuantity getStatistic(HealthDataType type,
            AggregateMetric metric) {
        if (type == null || metric == null) {
            return null;
        }
        synchronized (statistics) {
            return statistics.get(key(type, metric));
        }
    }

    /// Feeds samples into the workout. On a recorded session this is the
    /// only way anything is collected.
    public final AsyncResource<Boolean> addSamples(List<HealthSample> samples) {
        AsyncResource<Boolean> out = new OneShot<Boolean>();
        // Held across the acceptance, not just the check. end() claims its
        // transition under this same lock before doEnd takes any snapshot,
        // so a reading arriving from a sensor thread either lands in the
        // workout that is being written or is refused -- never accepted
        // into a record that has already been sealed.
        synchronized (stateLock) {
            return addSamplesLocked(samples, out);
        }
    }

    private AsyncResource<Boolean> addSamplesLocked(
            List<HealthSample> samples, AsyncResource<Boolean> out) {
        if (state != WorkoutSessionState.RUNNING
                && state != WorkoutSessionState.PAUSED) {
            failState(out, "addSamples");
            return out;
        }
        if (state == WorkoutSessionState.PAUSED) {
            // Accepted but dropped. A strap stays connected across a pause
            // and keeps notifying, and the elapsed clock already excludes
            // the paused span -- so rolling these in produced a saved
            // workout whose totals covered time its own duration says did
            // not happen. Callers explicitly feeding history can add it
            // after resuming.
            out.complete(Boolean.TRUE);
            return out;
        }
        if (samples == null || samples.isEmpty()) {
            out.complete(Boolean.TRUE);
            return out;
        }
        // Copied and filtered rather than passed through. rollUp ignores
        // a null, but the list went on to be stored whole, and doEnd
        // dereferences what it stored -- so a single null entry threw out
        // of end() after the session had already moved to STOPPED,
        // leaving the caller with neither a result nor a session it could
        // retry.
        List<HealthSample> kept = new ArrayList<HealthSample>(
                samples.size());
        for (HealthSample sample : samples) {
            if (sample != null) {
                kept.add(sample);
            }
        }
        // Every value resolved before a single total moves. Converting a
        // quantity into its type's canonical unit is the one step here
        // that can fail -- a sample whose unit measures the wrong
        // dimension -- and rolling up as we went left the earlier
        // samples' totals applied while the batch itself was refused, so
        // the workout ended reporting a total with no child sample
        // behind it.
        List<QuantitySample> flat = new ArrayList<QuantitySample>();
        for (HealthSample sample : kept) {
            flatten(sample, flat);
        }
        double[] values = new double[flat.size()];
        for (int i = 0; i < flat.size(); i++) {
            QuantitySample q = flat.get(i);
            try {
                values[i] = q.getValue(q.getType().getCanonicalUnit());
            } catch (RuntimeException incompatible) {
                // Reported, not thrown. addSamples is called from the
                // sensor callback thread as well as the app's own, and an
                // exception escaping there lands in the Bluetooth stack.
                out.error(new HealthException(HealthError.INVALID_ARGUMENT,
                        "a " + q.getType().getId() + " sample is measured"
                                + " in " + q.getQuantity().getUnit()
                                        .getSymbol()
                                + ", which is not a "
                                + q.getType().getCanonicalUnit().getSymbol()
                                + "; the batch was not accepted",
                        incompatible));
                return out;
            }
        }
        // Folded first, announced once. A heart-rate trace can carry
        // tens of thousands of points, and one EDT runnable per
        // measurement floods the queue with callbacks that each report
        // the same final figure by the time they run -- while every one
        // of them holds the listener and the session until it does.
        List<HealthDataType> touched = new ArrayList<HealthDataType>();
        for (int i = 0; i < flat.size(); i++) {
            QuantitySample q = flat.get(i);
            applyRollUp(q, values[i]);
            if (!touched.contains(q.getType())) {
                touched.add(q.getType());
            }
        }
        for (HealthDataType type : touched) {
            fireStatisticsUpdated(type);
        }
        if (kept.isEmpty()) {
            out.complete(Boolean.TRUE);
            return out;
        }
        doAddSamples(kept, out);
        return out;
    }

    /// Records an event -- a lap, a marker, a segment boundary.
    public final AsyncResource<Boolean> addEvent(WorkoutEvent event) {
        AsyncResource<Boolean> out = new OneShot<Boolean>();
        if (event == null) {
            out.complete(Boolean.FALSE);
            return out;
        }
        synchronized (events) {
            events.add(event);
        }
        fireEvent(event);
        out.complete(Boolean.TRUE);
        return out;
    }

    /// Every event recorded so far.
    public final List<WorkoutEvent> getEvents() {
        synchronized (events) {
            return new ArrayList<WorkoutEvent>(events);
        }
    }

    /// Registers a listener for state, statistics and events.
    public final void addListener(WorkoutSessionListener listener) {
        if (listener == null) {
            return;
        }
        synchronized (listeners) {
            if (!listeners.contains(listener)) {
                listeners.add(listener);
            }
        }
    }

    /// Removes a listener.
    public final void removeListener(WorkoutSessionListener listener) {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }

    // ------------------------------------------------------------------
    // statistics rollup
    // ------------------------------------------------------------------

    /// Folds one sample into the running totals, minima, maxima, averages
    /// and latest values. Shared by every port, so a session reports its
    /// statistics the same way wherever it runs -- and would go on doing
    /// so if a live session were feeding it instead of the app.
    /// The measurements `sample` contributes to the rollup, expanded but
    /// not yet converted.
    ///
    /// A heart-rate trace fed in whole is workout data like any other.
    /// Skipping series left AVERAGE, MINIMUM, MAXIMUM and LATEST null or
    /// stale for measurements the session was collecting and would go on
    /// to persist. A sample with no numeric value, or of a type with no
    /// canonical unit, contributes nothing and is dropped here rather
    /// than checked again downstream.
    private static void flatten(HealthSample sample,
            List<QuantitySample> out) {
        if (sample instanceof SeriesSample) {
            SeriesSample series = (SeriesSample) sample;
            for (int i = 0; i < series.size(); i++) {
                flatten(series.toQuantitySample(i), out);
            }
            return;
        }
        if (!(sample instanceof QuantitySample)) {
            return;
        }
        QuantitySample q = (QuantitySample) sample;
        if (q.getType().getCanonicalUnit() == null) {
            return;
        }
        out.add(q);
    }

    /// Folds one already-converted measurement into the rollup. Cannot
    /// fail, which is what lets the batch be all-or-nothing.
    private void applyRollUp(QuantitySample sample, double v) {
        QuantitySample q = sample;
        HealthDataType type = q.getType();
        HealthUnit unit = type.getCanonicalUnit();
        synchronized (statistics) {
            Integer nBox = counts.get(type.getId());
            int n = nBox == null ? 0 : nBox.intValue();
            counts.put(type.getId(), Integer.valueOf(n + 1));

            switch (type.getAggregationStyle()) {
                case CUMULATIVE:
                    put(type, AggregateMetric.TOTAL,
                            valueOf(type, AggregateMetric.TOTAL, unit) + v,
                            unit);
                    break;
                case DISCRETE:
                    double sum = valueOf(type, AggregateMetric.TOTAL, unit) + v;
                    put(type, AggregateMetric.TOTAL, sum, unit);
                    // Weighted by how long each sample covers, matching the
                    // aggregate semantics. An arithmetic mean reported ten
                    // minutes at 60 bpm followed by one minute at 120 as
                    // 90 bpm rather than about 65.
                    double w = Math.max(1, sample.getDurationMillis());
                    double weighted = weightedSum(type) + v * w;
                    double totalWeight = weightOf(type) + w;
                    setWeightedSum(type, weighted);
                    setWeight(type, totalWeight);
                    put(type, AggregateMetric.AVERAGE,
                            totalWeight == 0 ? v : weighted / totalWeight,
                            unit);
                    HealthQuantity min =
                            statistics.get(key(type, AggregateMetric.MINIMUM));
                    if (min == null || v < min.getValue(unit)) {
                        put(type, AggregateMetric.MINIMUM, v, unit);
                    }
                    HealthQuantity max =
                            statistics.get(key(type, AggregateMetric.MAXIMUM));
                    if (max == null || v > max.getValue(unit)) {
                        put(type, AggregateMetric.MAXIMUM, v, unit);
                    }
                    // Latest by timestamp, not by arrival. A delayed
                    // device reading or an unsorted history batch made an
                    // older value overwrite a newer one, so the workout
                    // reported a stale number as the current one.
                    Long seen = latestAt.get(type.getId());
                    if (seen == null
                            || q.getStartMillis() >= seen.longValue()) {
                        latestAt.put(type.getId(),
                                Long.valueOf(q.getStartMillis()));
                        put(type, AggregateMetric.LATEST, v, unit);
                    }
                    break;
                default:
                    break;
            }
        }
    }

    /// When each type's LATEST value was measured, so a sample arriving
    /// late cannot pass itself off as the newest.
    private final Map<String, Long> latestAt = new HashMap<String, Long>();

    private final Map<String, Double> weightedSums =
            new HashMap<String, Double>();
    private final Map<String, Double> weights =
            new HashMap<String, Double>();

    private double weightedSum(HealthDataType type) {
        Double v = weightedSums.get(type.getId());
        return v == null ? 0 : v.doubleValue();
    }

    private void setWeightedSum(HealthDataType type, double v) {
        weightedSums.put(type.getId(), Double.valueOf(v));
    }

    private double weightOf(HealthDataType type) {
        Double v = weights.get(type.getId());
        return v == null ? 0 : v.doubleValue();
    }

    private void setWeight(HealthDataType type, double v) {
        weights.put(type.getId(), Double.valueOf(v));
    }

    private double valueOf(HealthDataType type, AggregateMetric metric,
            HealthUnit unit) {
        HealthQuantity existing = statistics.get(key(type, metric));
        return existing == null ? 0 : existing.getValue(unit);
    }

    private void put(HealthDataType type, AggregateMetric metric, double value,
            HealthUnit unit) {
        statistics.put(key(type, metric), new HealthQuantity(value, unit));
    }

    private static String key(HealthDataType type, AggregateMetric metric) {
        return type.getId() + ' ' + metric.name();
    }

    /// The accumulated statistics, for ports assembling the final record.
    protected final Map<String, HealthQuantity> getStatisticsSnapshot() {
        synchronized (statistics) {
            return new HashMap<String, HealthQuantity>(statistics);
        }
    }

    // ------------------------------------------------------------------
    // port SPI
    // ------------------------------------------------------------------

    /// Warms up sensors. Default completes immediately.
    protected void doPrepare(AsyncResource<Boolean> out) {
        out.complete(Boolean.TRUE);
    }

    /// Starts platform collection.
    protected abstract void doStart(AsyncResource<Boolean> out);

    /// Pauses platform collection.
    protected abstract void doPause(AsyncResource<Boolean> out);

    /// Resumes platform collection.
    protected abstract void doResume(AsyncResource<Boolean> out);

    /// Stops collection and persists the workout.
    protected abstract void doEnd(AsyncResource<WorkoutSample> out);

    /// Abandons the workout without persisting.
    protected abstract void doDiscard();

    /// Hands fed samples to the platform. Default accepts them into the
    /// shared rollup only.
    protected void doAddSamples(List<HealthSample> samples,
            AsyncResource<Boolean> out) {
        out.complete(Boolean.TRUE);
    }

    // ------------------------------------------------------------------
    // event plumbing
    // ------------------------------------------------------------------

    /// Moves to a new state and notifies listeners. Ports call this for
    /// transitions the platform initiated.
    protected final void setState(WorkoutSessionState newState) {
        synchronized (stateLock) {
            if (newState == null || newState == state) {
                return;
            }
            state = newState;
            // Published under the lock, so listeners are told about the
            // transitions in the order they happened. Enqueued outside
            // it, a transition that had already released the lock could
            // hand its notification to the EDT after end() had handed
            // over STOPPED, and an app would watch an ended workout
            // appear to resume while getState() said otherwise.
            //
            // Only the enqueue: callSerially appends and returns, and the
            // runnable runs later on the EDT, so no listener runs while
            // this is held.
            Object[] snapshot = listenerSnapshot();
            if (snapshot != null) {
                Display.getInstance().callSerially(
                        makeStateRunnable(this, snapshot, newState));
            }
        }
    }

    /// Notifies listeners that a statistic changed.
    protected final void fireStatisticsUpdated(HealthDataType type) {
        Object[] snapshot = listenerSnapshot();
        if (snapshot != null) {
            Display.getInstance().callSerially(
                    makeStatsRunnable(this, snapshot, type));
        }
    }

    /// Notifies listeners of an event.
    protected final void fireEvent(WorkoutEvent event) {
        Object[] snapshot = listenerSnapshot();
        if (snapshot != null) {
            Display.getInstance().callSerially(
                    makeEventRunnable(this, snapshot, event));
        }
    }

    /// Reports an unrecoverable failure and moves to
    /// [WorkoutSessionState#FAILED].
    protected final void fireFailed(HealthException error) {
        setState(WorkoutSessionState.FAILED);
        Object[] snapshot = listenerSnapshot();
        if (snapshot != null) {
            Display.getInstance().callSerially(
                    makeFailedRunnable(this, snapshot, error));
        }
    }

    private Object[] listenerSnapshot() {
        synchronized (listeners) {
            if (listeners.isEmpty()) {
                return null;
            }
            return listeners.toArray();
        }
    }

    private boolean requireState(AsyncResource out,
            WorkoutSessionState required, String what) {
        if (state == required) {
            return true;
        }
        failState(out, what);
        return false;
    }

    private void failState(AsyncResource out, String what) {
        out.error(new HealthException(HealthError.SESSION_STATE,
                "cannot " + what + " a workout in state " + state));
    }

    // Dispatch runnables are built in static methods so they carry no
    // synthetic reference to the enclosing session (SpotBugs
    // SIC_INNER_SHOULD_BE_STATIC_ANON).

    private static Runnable makeStateRunnable(final WorkoutSession session,
            final Object[] listeners, final WorkoutSessionState state) {
        return new Runnable() {
            @Override
            public void run() {
                for (Object listener : listeners) {
                    ((WorkoutSessionListener) listener)
                            .workoutStateChanged(session, state);
                }
            }
        };
    }

    private static Runnable makeStatsRunnable(final WorkoutSession session,
            final Object[] listeners, final HealthDataType type) {
        return new Runnable() {
            @Override
            public void run() {
                for (Object listener : listeners) {
                    ((WorkoutSessionListener) listener)
                            .workoutStatisticsUpdated(session, type);
                }
            }
        };
    }

    private static Runnable makeEventRunnable(final WorkoutSession session,
            final Object[] listeners, final WorkoutEvent event) {
        return new Runnable() {
            @Override
            public void run() {
                for (Object listener : listeners) {
                    ((WorkoutSessionListener) listener)
                            .workoutEvent(session, event);
                }
            }
        };
    }

    private static Runnable makeFailedRunnable(final WorkoutSession session,
            final Object[] listeners, final HealthException error) {
        return new Runnable() {
            @Override
            public void run() {
                for (Object listener : listeners) {
                    ((WorkoutSessionListener) listener)
                            .workoutFailed(session, error);
                }
            }
        };
    }
}
