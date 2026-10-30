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
package com.codenameone.examples.pulse.health;

import com.codename1.health.AggregateMetric;
import com.codename1.health.Health;
import com.codename1.health.HealthAccess;
import com.codename1.health.HealthDataType;
import com.codename1.health.HealthException;
import com.codename1.health.HealthQuantity;
import com.codename1.health.HealthSample;
import com.codename1.health.HealthStore;
import com.codename1.health.HealthTimeRange;
import com.codename1.health.HealthUnit;
import com.codename1.health.QuantitySample;
import com.codename1.health.SampleQuery;
import com.codename1.health.WorkoutActivityType;
import com.codename1.health.WorkoutSample;
import com.codename1.health.sensors.HealthSensor;
import com.codename1.health.sensors.HealthSensorProfile;
import com.codename1.health.sensors.HealthSensors;
import com.codename1.health.sensors.SensorDiscoveryListener;
import com.codename1.health.sensors.SensorScan;
import com.codename1.health.sensors.SensorScanSettings;
import com.codename1.health.sensors.SensorSampleListener;
import com.codename1.health.sensors.SensorSession;
import com.codename1.health.sensors.SensorSessionOptions;
import com.codename1.health.sensors.SensorSessionState;
import com.codename1.health.workout.WorkoutConfiguration;
import com.codename1.health.workout.WorkoutEvent;
import com.codename1.health.workout.WorkoutSession;
import com.codename1.health.workout.WorkoutSessionListener;
import com.codename1.health.workout.WorkoutSessionState;
import com.codenameone.examples.pulse.api.HeartRateDto;
import com.codenameone.examples.pulse.api.SessionDto;
import com.codenameone.examples.pulse.api.WorkoutDto;
import com.codenameone.examples.pulse.net.Api;
import com.codenameone.examples.pulse.net.Net;
import com.codenameone.examples.pulse.ui.Ui;
import java.util.ArrayList;
import java.util.List;

/// Pulse and the phone's health store (HealthKit, Health Connect), and a
/// Bluetooth heart rate strap.
///
/// Two things happen here. Workouts the member recorded elsewhere -- a run,
/// a ride -- are read from the store and posted to the studio, so a friend's
/// feed and the member's own week include them. And during a class the member
/// booked, a workout is recorded: the heart rate goes to the studio every few
/// seconds, which is what the trainer's live view shows, and when the class
/// ends it is written to the store as a workout and posted like the others.
///
/// Everything arrives on the EDT, so nothing here is guarded.
public final class Workouts {
    /// Where a posted workout came from.
    public static final String SOURCE_HEALTH = "HEALTH";
    public static final String SOURCE_CLASS = "CLASS";
    /// The heart rate is sent no more often than this.
    private static final long SEND_EVERY = 5000L;

    /// Told how a class workout is going.
    public interface Progress {
        /// A new heart rate reading, in beats per minute.
        void beat(int bpm);

        /// The workout ended and was posted, or failed with `error`.
        void ended(WorkoutDto posted, String error);
    }

    private static WorkoutSession recording;
    private static SessionDto recordingClass;
    private static SensorScan scan;
    private static SensorSession strap;
    private static Progress progress;
    private static boolean shared;
    private static long lastSent;
    private static int beats;
    private static long beatTotal;
    private static int maxBeat;

    private Workouts() {
    }

    /// Whether the phone has a health store Pulse can use.
    public static boolean storeAvailable() {
        return Health.getInstance().isSupported();
    }

    /// Whether a strap can be paired, which needs no health store.
    public static boolean strapsAvailable() {
        HealthSensors sensors = Health.getInstance().getSensors();
        return sensors != null && sensors.isSupported();
    }

    /// The class being recorded, or null.
    public static SessionDto recording() {
        return recordingClass;
    }

    /// Reads the last two weeks of workouts from the store and posts those
    /// the studio does not have yet. `done` gets how many were posted, or -1
    /// with nothing posted when the store could not be read.
    public static void importRecent(final boolean share, final Net.Ok<Integer> done) {
        if (!storeAvailable()) {
            done.got(Integer.valueOf(-1));
            return;
        }
        final HealthStore store = Health.getInstance().getStore();
        store.requestAuthorization(HealthAccess.read(HealthDataType.WORKOUT),
                HealthAccess.read(HealthDataType.HEART_RATE)).onResult((asked, refused) -> {
                    if (refused != null) {
                        done.got(Integer.valueOf(-1));
                        return;
                    }
                    SampleQuery query = new SampleQuery().addType(HealthDataType.WORKOUT)
                            .setTimeRange(HealthTimeRange.lastDays(14)).setLimit(30)
                            .setSortDescending(true);
                    store.readSamples(query).onResult((samples, failed) -> {
                        if (failed != null) {
                            done.got(Integer.valueOf(-1));
                            return;
                        }
                        Api.engagement().workouts(Net.to(known ->
                                post(fresh(samples, known), share, 0, done),
                                (status, message) -> done.got(Integer.valueOf(-1))));
                    });
                });
    }

    /// The workouts of `samples` that start at no time a posted one does.
    private static List<WorkoutDto> fresh(List<HealthSample> samples, List<WorkoutDto> known) {
        List<WorkoutDto> out = new ArrayList<WorkoutDto>();
        for (int iter = 0; samples != null && iter < samples.size(); iter++) {
            HealthSample sample = samples.get(iter);
            if (!(sample instanceof WorkoutSample)) {
                continue;
            }
            WorkoutSample workout = (WorkoutSample) sample;
            boolean seen = false;
            for (int k = 0; known != null && k < known.size(); k++) {
                if (Math.abs(known.get(k).startedAt() - workout.getStartMillis()) < 60000L) {
                    seen = true;
                    break;
                }
            }
            if (!seen) {
                out.add(dto(workout, null, SOURCE_HEALTH, 0, 0));
            }
        }
        return out;
    }

    private static void post(final List<WorkoutDto> workouts, final boolean share,
            final int index, final Net.Ok<Integer> done) {
        if (index >= workouts.size()) {
            done.got(Integer.valueOf(index));
            return;
        }
        WorkoutDto workout = workouts.get(index);
        Api.engagement().addWorkout(shared(workout, share), Net.to(posted ->
                post(workouts, share, index + 1, done), (status, message) ->
                done.got(Integer.valueOf(index))));
    }

    private static WorkoutDto shared(WorkoutDto workout, boolean share) {
        return new WorkoutDto(workout.id(), workout.sessionId(), workout.activity(),
                workout.startedAt(), workout.minutes(), workout.avgHr(), workout.maxHr(),
                workout.calories(), workout.zones(), workout.source(), share);
    }

    private static WorkoutDto dto(WorkoutSample workout, String sessionId, String source,
            int avg, int max) {
        long millis = workout.getActiveDurationMillis() > 0 ? workout.getActiveDurationMillis()
                : workout.getDurationMillis();
        HealthQuantity energy = workout.getTotalEnergy();
        int calories = energy == null ? 0 : (int) Math.round(energy.getValue(
                HealthUnit.KILOCALORIE));
        return new WorkoutDto(null, sessionId, activityName(workout.getActivityType()),
                workout.getStartMillis(), (int) (millis / 60000L), avg, max, calories, null,
                source, false);
    }

    private static String activityName(WorkoutActivityType type) {
        return type == null ? "OTHER" : type.name();
    }

    /// The kind of workout a class of `category` is.
    static WorkoutActivityType activity(String category) {
        String kind = category == null ? "" : category;
        if (kind.equalsIgnoreCase("cycling") || kind.equalsIgnoreCase("spin")) {
            return WorkoutActivityType.CYCLING;
        }
        if (kind.equalsIgnoreCase("yoga")) {
            return WorkoutActivityType.YOGA;
        }
        if (kind.equalsIgnoreCase("pilates")) {
            return WorkoutActivityType.PILATES;
        }
        if (kind.equalsIgnoreCase("strength")) {
            return WorkoutActivityType.STRENGTH_TRAINING;
        }
        if (kind.equalsIgnoreCase("hiit") || kind.equalsIgnoreCase("cardio")) {
            return WorkoutActivityType.HIGH_INTENSITY_INTERVAL_TRAINING;
        }
        if (kind.equalsIgnoreCase("dance")) {
            return WorkoutActivityType.DANCE;
        }
        return WorkoutActivityType.FUNCTIONAL_TRAINING;
    }

    /// Records `session` as a workout, streaming the heart rate to the
    /// studio while it runs. A strap is used when one is in range; otherwise
    /// the heart rate is whatever the platform's workout collects (a paired
    /// watch). Without a store, a strap alone still feeds the live view.
    public static void startClass(final SessionDto session, boolean share, Progress listener) {
        if (recordingClass != null) {
            return;
        }
        recordingClass = session;
        progress = listener;
        shared = share;
        beats = 0;
        beatTotal = 0;
        maxBeat = 0;
        lastSent = 0;
        if (!storeAvailable()) {
            pairStrap(null);
            return;
        }
        Health.getInstance().getStore().requestAuthorization(
                HealthAccess.write(HealthDataType.WORKOUT),
                HealthAccess.read(HealthDataType.HEART_RATE),
                HealthAccess.write(HealthDataType.HEART_RATE)).onResult((asked, refused) -> {
                    WorkoutConfiguration config = new WorkoutConfiguration()
                            .setActivityType(activity(session.category()))
                            .setTitle(session.name())
                            .addCollectedType(HealthDataType.HEART_RATE);
                    Health.getInstance().getWorkouts().startSession(config)
                            .onResult((workout, failed) -> {
                                if (failed != null || workout == null) {
                                    pairStrap(null);
                                    return;
                                }
                                recording = workout;
                                workout.addListener(WORKOUT_LISTENER);
                                workout.start();
                                pairStrap(workout);
                            });
                });
    }

    /// Looks for a heart rate strap and, on the first found, feeds it into
    /// `workout` (which may be null when there is no store).
    private static void pairStrap(final WorkoutSession workout) {
        if (!strapsAvailable()) {
            return;
        }
        final HealthSensors sensors = Health.getInstance().getSensors();
        scan = sensors.startScan(new SensorScanSettings().addProfile(HealthSensorProfile.HEART_RATE)
                .setTimeoutMillis(20000), new SensorDiscoveryListener() {
                    @Override
                    public void sensorDiscovered(HealthSensor sensor) {
                        if (strap != null || recordingClass == null) {
                            return;
                        }
                        stopScan();
                        SensorSessionOptions options = new SensorSessionOptions()
                                .setAutoReconnect(true);
                        if (workout != null) {
                            options.setWorkoutSession(workout);
                        }
                        sensors.connect(sensor, HealthSensorProfile.HEART_RATE, options)
                                .onResult((connected, failed) -> {
                                    if (failed != null || connected == null) {
                                        return;
                                    }
                                    if (recordingClass == null) {
                                        connected.stop();
                                        return;
                                    }
                                    strap = connected;
                                    connected.addListener(STRAP_LISTENER);
                                });
                    }

                    @Override
                    public void scanFailed(HealthException error) {
                        scan = null;
                    }
                });
    }

    private static void stopScan() {
        if (scan != null) {
            scan.stop();
            scan = null;
        }
    }

    /// A reading, from a strap or the workout. Sent on to the studio at most
    /// every [#SEND_EVERY] milliseconds.
    static void beat(int bpm) {
        if (recordingClass == null || bpm <= 0) {
            return;
        }
        beats++;
        beatTotal += bpm;
        maxBeat = Math.max(maxBeat, bpm);
        if (progress != null) {
            progress.beat(bpm);
        }
        long now = System.currentTimeMillis();
        if (now - lastSent < SEND_EVERY) {
            return;
        }
        lastSent = now;
        Api.engagement().heartRate(new HeartRateDto(recordingClass.id(), bpm),
                Net.to(sent -> { }, (status, message) -> { }));
    }

    /// Ends the class workout: writes it to the store and posts it.
    public static void stopClass() {
        final SessionDto session = recordingClass;
        if (session == null) {
            return;
        }
        stopScan();
        if (strap != null) {
            strap.removeListener(STRAP_LISTENER);
            strap.stop();
            strap = null;
        }
        final int avg = beats == 0 ? 0 : (int) (beatTotal / beats);
        final int max = maxBeat;
        final WorkoutSession workout = recording;
        recording = null;
        if (workout == null) {
            finish(new WorkoutDto(null, session.id(), activityName(activity(session.category())),
                    session.startsAt(), (int) ((Math.min(System.currentTimeMillis(),
                    session.endsAt()) - session.startsAt()) / 60000L), avg, max, 0, null,
                    SOURCE_CLASS, shared));
            return;
        }
        workout.removeListener(WORKOUT_LISTENER);
        workout.end().onResult((written, failed) -> {
            if (failed != null || written == null) {
                finish(new WorkoutDto(null, session.id(), activityName(activity(
                        session.category())), workout.getStartedAtMillis(),
                        (int) (workout.getElapsedMillis() / 60000L), avg, max, 0, null,
                        SOURCE_CLASS, shared));
                return;
            }
            finish(shared(dto(written, session.id(), SOURCE_CLASS, avg, max), shared));
        });
    }

    private static void finish(WorkoutDto workout) {
        final Progress told = progress;
        recordingClass = null;
        progress = null;
        Api.engagement().addWorkout(workout, Net.to(posted -> {
            if (told != null) {
                told.ended(posted, null);
            }
        }, (status, message) -> {
            if (told != null) {
                told.ended(null, message);
            } else {
                Ui.fail(status, message);
            }
        }));
    }

    /// The class booked for the member that is on now, starting within a
    /// quarter of an hour or under way, or null.
    public static SessionDto classNow(List<SessionDto> booked, long at) {
        for (int iter = 0; booked != null && iter < booked.size(); iter++) {
            SessionDto session = booked.get(iter);
            if (session != null && session.startsAt() - 15 * 60000L <= at
                    && session.endsAt() > at) {
                return session;
            }
        }
        return null;
    }

    private static final SensorSampleListener STRAP_LISTENER = new SensorSampleListener() {
        @Override
        public void sensorSample(SensorSession session, HealthSample sample) {
            if (sample instanceof QuantitySample
                    && HealthDataType.HEART_RATE.equals(sample.getType())) {
                beat((int) Math.round(((QuantitySample) sample).getValue(
                        HealthUnit.COUNT_PER_MINUTE)));
            }
        }

        @Override
        public void sensorStateChanged(SensorSession session, SensorSessionState state) {
        }

        @Override
        public void sensorError(SensorSession session, HealthException error) {
        }
    };

    private static final WorkoutSessionListener WORKOUT_LISTENER = new WorkoutSessionListener() {
        @Override
        public void workoutStateChanged(WorkoutSession session, WorkoutSessionState state) {
        }

        @Override
        public void workoutStatisticsUpdated(WorkoutSession session, HealthDataType type) {
            // A strap reports each beat itself. Without one the platform
            // collects the heart rate (a paired watch) and keeps running
            // statistics rather than the latest beat, so the running average
            // is what the live view gets.
            if (strap != null || !HealthDataType.HEART_RATE.equals(type)) {
                return;
            }
            HealthQuantity average = session.getStatistic(HealthDataType.HEART_RATE,
                    AggregateMetric.AVERAGE);
            if (average != null) {
                beat((int) Math.round(average.getValue(HealthUnit.COUNT_PER_MINUTE)));
            }
        }

        @Override
        public void workoutEvent(WorkoutSession session, WorkoutEvent event) {
        }

        @Override
        public void workoutFailed(WorkoutSession session, HealthException error) {
        }
    };
}
