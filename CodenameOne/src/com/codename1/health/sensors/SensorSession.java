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
package com.codename1.health.sensors;

import com.codename1.health.BloodPressureSample;
import com.codename1.health.HealthDataType;
import com.codename1.health.HealthError;
import com.codename1.health.Health;
import com.codename1.health.workout.WorkoutSession;
import com.codename1.health.HealthException;
import com.codename1.health.HealthQuantity;
import com.codename1.health.HealthSample;
import com.codename1.health.HealthStore;
import com.codename1.health.HealthWriteResult;
import com.codename1.health.HealthUnit;
import com.codename1.health.QuantitySample;
import com.codename1.health.RecordingMethod;
import com.codename1.health.SeriesSample;
import com.codename1.ui.Display;
import com.codename1.util.AsyncResource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// A live connection to one health sensor, decoding its notifications into
/// health samples.
///
/// Obtained from
/// [HealthSensors#connect(HealthSensor,HealthSensorProfile,SensorSessionOptions)].
/// All listener callbacks arrive on the EDT.
///
/// #### Derived values
///
/// Speed and cadence sensors transmit cumulative counters rather than
/// rates, so this class differences consecutive notifications -- handling
/// the counter and event-timer wraps that would otherwise produce spikes
/// of tens of thousands of rpm. See [CumulativeCounterTracker].
public class SensorSession {

    private final String sensorId;
    private final HealthSensorProfile profile;
    private final SensorSessionOptions options;
    private final List<HealthSample> pendingWrites =
            new ArrayList<HealthSample>();
    private java.util.Timer flushTimer;
    private final List<SensorSampleListener> listeners =
            new ArrayList<SensorSampleListener>();
    private final Map<String, HealthSample> latest =
            new HashMap<String, HealthSample>();

    private final CumulativeCounterTracker wheelTracker =
            new CumulativeCounterTracker(0x100000000L, 1024);
    private final CumulativeCounterTracker crankTracker =
            new CumulativeCounterTracker(0x10000L, 1024);

    /// Volatile because it is written on the EDT and read from the
    /// flush timer's thread and from store callbacks. Without it those
    /// threads could go on seeing a running session after it had ended,
    /// re-arm the flush, and issue a write from a session nobody holds --
    /// which is exactly the "issued one more write" failure, and exactly
    /// why it appeared on a CI machine and not on a developer's.
    private volatile SensorSessionState state =
            SensorSessionState.CONNECTING;
    /// Volatile, all three: they are set as notifications decode -- on
    /// whichever thread the transport delivers on -- and read by the app
    /// through the getters, and `streamed` also decides whether a
    /// reconnect is worth attempting. Same reason as [#state], found by
    /// going through the package for the rest of this shape rather than
    /// waiting for each one to be reported.
    private volatile boolean streamed;
    private volatile Integer batteryPercent;
    private volatile int bodySensorLocation = -1;

    /// Ports and [HealthSensors] construct sessions.
    protected SensorSession(String sensorId, HealthSensorProfile profile,
            SensorSessionOptions options) {
        this.sensorId = sensorId;
        this.profile = profile;
        this.options = options == null ? new SensorSessionOptions() : options;
    }

    /// The stable identifier of the connected device -- see
    /// [HealthSensor#getId()].
    public final String getSensorId() {
        return sensorId;
    }

    /// The profile this session is speaking.
    public final HealthSensorProfile getProfile() {
        return profile;
    }

    /// The options this session was created with.
    public final SensorSessionOptions getOptions() {
        return options;
    }

    /// The current lifecycle state.
    public final SensorSessionState getState() {
        return state;
    }

    /// Registers a listener for measurements and state changes.
    public final void addListener(SensorSampleListener listener) {
        if (listener == null) {
            return;
        }
        synchronized (listeners) {
            if (!listeners.contains(listener)) {
                listeners.add(listener);
            }
        }
    }

    /// Removes a previously registered listener.
    public final void removeListener(SensorSampleListener listener) {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }

    /// The most recent sample of `type`, or `null` when none has arrived
    /// or the last one is older than
    /// [SensorSessionOptions#getStaleSampleMillis()].
    ///
    /// Returning null rather than a stale value is deliberate: a UI bound
    /// to this shows a dash when the strap falls off, instead of
    /// continuing to display the wearer's last heart rate as though it
    /// were current.
    public final HealthSample getLatest(HealthDataType type) {
        if (type == null) {
            return null;
        }
        HealthSample s;
        synchronized (latest) {
            s = latest.get(type.getId());
        }
        if (s == null) {
            return null;
        }
        long age = System.currentTimeMillis() - s.getEndMillis();
        return age > options.getStaleSampleMillis() ? null : s;
    }

    /// The device's battery level as a percentage, or `null` when it does
    /// not report one.
    public final Integer getBatteryPercent() {
        return batteryPercent;
    }

    /// Where a heart-rate sensor is worn, as a [BodySensorLocation]
    /// constant, or `-1` when unreported.
    public final int getBodySensorLocation() {
        return bodySensorLocation;
    }

    /// Resets a heart-rate strap's accumulated energy-expended counter, by
    /// writing `0x01` to the Heart Rate Control Point (`0x2A39`).
    ///
    /// Fails with [HealthError#NOT_SUPPORTED] on other profiles and on
    /// straps that do not expose the control point.
    public AsyncResource<Boolean> resetEnergyExpended() {
        AsyncResource<Boolean> out = new AsyncResource<Boolean>();
        out.error(new HealthException(HealthError.NOT_SUPPORTED,
                "this session does not support resetting energy expended"));
        return out;
    }

    /// Asks a glucose meter to replay stored records, delivering each one
    /// through the normal sample listeners, and resolving with how many
    /// were retrieved.
    ///
    /// **Not implemented in this release: this always fails with
    /// [HealthError#NOT_SUPPORTED], on the glucose profile as well as any
    /// other.** Replay runs over the Record Access Control Point, a
    /// stateful request/response protocol on a second characteristic with
    /// its own operators, filters and abort semantics, and shipping it
    /// untested against real meters would be worse than saying plainly
    /// that it is absent. [GlucoseRecordFilter] and the rest of the
    /// vocabulary are here so the call site does not have to change when
    /// it lands.
    ///
    /// Live glucose notifications are unaffected and work today.
    public AsyncResource<Integer> requestStoredRecords(
            GlucoseRecordFilter filter) {
        AsyncResource<Integer> out = new AsyncResource<Integer>();
        out.error(new HealthException(HealthError.NOT_SUPPORTED,
                "replaying stored glucose records is not implemented in"
                        + " this release; live measurements still arrive"
                        + " through the sample listeners"));
        return out;
    }

    /// Disconnects and stops delivering measurements. Idempotent.
    public void stop() {
        setState(SensorSessionState.STOPPED);
    }

    /// Drops this session from [HealthSensors#getActiveSessions()].
    ///
    /// Without it a stopped or failed session stayed in the registry for
    /// the manager's lifetime, so the list documented as "connected or
    /// reconnecting" filled up with dead sessions and kept their
    /// peripherals and listeners alive with them.
    protected final void forgetFromManager() {
        Health.getInstance().getSensors().sessionEnded(this);
    }

    // ------------------------------------------------------------------
    // decoding -- shared by every transport
    // ------------------------------------------------------------------

    /// Decodes one raw characteristic value and emits the resulting
    /// samples. Called by the transport when a notification arrives.
    ///
    /// A malformed payload emits nothing and reports
    /// [HealthError#INVALID_DATA] rather than throwing: one misbehaving
    /// device must not take down the app.
    protected final void onMeasurement(byte[] value, long receivedAtMillis) {
        List<HealthSample> samples = decode(value, receivedAtMillis);
        if (samples == null) {
            fireError(new HealthException(HealthError.INVALID_DATA,
                    profile.getName() + " sensor sent a malformed "
                            + "measurement"));
            return;
        }
        for (HealthSample s : samples) {
            synchronized (latest) {
                latest.put(s.getType().getId(), s);
            }
            fireSample(s);
        }
        route(samples);
    }

    /// Sends decoded samples wherever the options said they should go.
    ///
    /// Both destinations were configurable and neither was read, so an
    /// attached workout never saw a single reading and a write-through
    /// session persisted nothing while reporting success.
    private void route(List<HealthSample> samples) {
        if (samples.isEmpty()) {
            return;
        }
        WorkoutSession workout = options.getWorkoutSession();
        if (workout != null) {
            workout.addSamples(samples);
        }
        if (!options.isWriteToStore()) {
            return;
        }
        // Batched rather than written per notification: a strap notifies
        // about once a second, and a store round trip per reading would
        // spend more time in the platform than in the app.
        boolean startTimer = false;
        synchronized (pendingWrites) {
            startTimer = pendingWrites.isEmpty();
            pendingWrites.addAll(samples);
        }
        if (startTimer) {
            // Armed when the batch opens rather than checked when the next
            // measurement arrives. A scale or a blood-pressure cuff sends
            // one reading and then stays quiet, so a deadline that only
            // advances on the next notification never fires and the reading
            // is written only if the app happens to call stop() -- losing it
            // outright if the process exits first.
            scheduleFlush(options.getStoreBatchMillis());
        }
    }

    private void scheduleFlush(int delayMillis) {
        cancelFlush();
        // CLDC's Timer has no named/daemon constructor.
        java.util.Timer t = new java.util.Timer();
        synchronized (pendingWrites) {
            flushTimer = t;
        }
        t.schedule(new FlushTask(this), Math.max(1, delayMillis));
    }

    private void cancelFlush() {
        java.util.Timer t;
        synchronized (pendingWrites) {
            t = flushTimer;
            flushTimer = null;
        }
        if (t != null) {
            t.cancel();
        }
    }

    /// Named rather than anonymous so the timer holds no synthetic
    /// reference beyond the session it flushes.
    private static final class FlushTask extends java.util.TimerTask {
        private final SensorSession session;

        FlushTask(SensorSession session) {
            this.session = session;
        }

        @Override
        public void run() {
            session.flushIfRunning();
        }
    }

    /// True once the session has ended. Guarded by `pendingWrites`, not
    /// by being volatile, because the point is not visibility but
    /// atomicity: a timer tick has to decide whether to claim the buffer
    /// in the same breath as the session decides it has stopped.
    private boolean flushingStopped;

    /// The timer's flush: claims the buffer only while the session runs.
    ///
    /// Checking the state at the top of the tick was not enough. The
    /// check passed, the session ended, and the write went out anyway --
    /// one write from a session nobody holds a handle to, every time the
    /// end landed inside that gap. The check and the claim are one
    /// critical section now, and ending the session takes the same lock,
    /// so a batch is either claimed while the session is still running or
    /// never claimed at all.
    private void flushIfRunning() {
        cancelFlush();
        List<HealthSample> batch;
        synchronized (pendingWrites) {
            if (flushingStopped || pendingWrites.isEmpty()) {
                return;
            }
            batch = new ArrayList<HealthSample>(pendingWrites);
            pendingWrites.clear();
        }
        writeBatch(batch);
    }

    /// Writes anything still buffered. Called when the session stops so a
    /// short ride does not lose its last partial batch.
    protected final void flushPendingWrites() {
        cancelFlush();
        List<HealthSample> batch;
        synchronized (pendingWrites) {
            if (pendingWrites.isEmpty()) {
                return;
            }
            batch = new ArrayList<HealthSample>(pendingWrites);
            pendingWrites.clear();
        }
        writeBatch(batch);
    }

    /// Sends a claimed batch to the store.
    ///
    /// The samples stay accounted for until the store confirms them.
    /// Removing them from the buffer and ignoring the result meant a
    /// revoked permission or an unavailable provider silently discarded a
    /// scale or cuff reading the caller explicitly asked to persist.
    private void writeBatch(List<HealthSample> batch) {
        Health.getInstance().getStore().write(batch)
                .onResult(new WriteBack(this, batch));
    }

    /// Puts a failed batch back and tells the app it failed.
    private static final class WriteBack
            implements com.codename1.util.AsyncResult<HealthWriteResult> {
        private final SensorSession session;
        private final List<HealthSample> batch;

        WriteBack(SensorSession session, List<HealthSample> batch) {
            this.session = session;
            this.batch = batch;
        }

        /// The tail of `batch` that the failed write did not commit.
        ///
        /// [HealthException#getPartialResult()] names the records that
        /// went in, and the store writes chunks in order, so the
        /// committed ones are a prefix. Counted in records rather than
        /// samples because a series is one sample and many records.
        ///
        /// A series straddling the boundary is retried whole and may
        /// duplicate the part of it that landed. That is the same trade
        /// the write path already makes for one sample, rather than the
        /// alternative of dropping measurements that were never stored.
        private static List<HealthSample> uncommitted(
                List<HealthSample> batch, Throwable error) {
            if (!(error instanceof HealthException)) {
                return batch;
            }
            HealthWriteResult partial =
                    ((HealthException) error).getPartialResult();
            if (partial == null) {
                return batch;
            }
            int committed = partial.getSampleIds().size();
            if (committed <= 0) {
                return batch;
            }
            int records = 0;
            int from = 0;
            for (int i = 0; i < batch.size(); i++) {
                HealthSample s = batch.get(i);
                records += s instanceof SeriesSample
                        ? ((SeriesSample) s).size() : 1;
                if (records > committed) {
                    break;
                }
                from = i + 1;
            }
            return new ArrayList<HealthSample>(
                    batch.subList(from, batch.size()));
        }

        @Override
        public void onReady(HealthWriteResult value, Throwable error) {
            if (error == null) {
                return;
            }
            // Samples of a type the store will never accept are dropped
            // rather than requeued. An Android cycling-power or cadence
            // session produces types Health Connect can read but not
            // write, so every batch failed validation and the retry below
            // resent the identical batch for as long as the session
            // streamed -- an error every storeBatchMillis, for ever, and
            // a buffer that only grew. Retrying is right for a store that
            // is busy or locked; it is pointless for one that has said
            // no, and the answer does not change with time.
            // Whatever a partly-successful write already committed is
            // not sent again. A buffered batch larger than the platform's
            // chunk can fail on a later chunk with the earlier ones
            // already in the store, and requeuing the whole batch wrote
            // those a second time -- duplicate records in the user's
            // health data and every aggregate over them inflated, for as
            // long as the session kept streaming.
            List<HealthSample> outstanding = uncommitted(batch, error);
            List<HealthSample> retryable = new ArrayList<HealthSample>(
                    outstanding.size());
            List<HealthDataType> refused = new ArrayList<HealthDataType>();
            HealthStore store = Health.getInstance().getStore();
            for (HealthSample s : outstanding) {
                if (store.isWritable(s.getType())) {
                    retryable.add(s);
                } else if (!refused.contains(s.getType())) {
                    refused.add(s.getType());
                }
            }
            if (!refused.isEmpty()) {
                com.codename1.io.Log.p("CN1 Health: this platform cannot"
                        + " write " + refused + ", so those sensor samples"
                        + " are dropped rather than retried. Turn"
                        + " setWriteToStore off for this session, or route"
                        + " the readings to a workout instead.");
            }
            if (retryable.isEmpty() || session.isTerminal()) {
                // Nothing is requeued once the session has ended. It
                // could never be flushed by the session itself -- the
                // re-arm below is guarded and the timer is cancelled --
                // so it only sat there waiting for something else to
                // drain it, and something else did: endSession() flushes
                // unconditionally and has more than one caller, so the
                // failed final write refilled the buffer and the next
                // teardown wrote it again. That is the "issued one more
                // write" failure, and it survived both the state check
                // and making the claim atomic because neither of them is
                // on this path.
                session.fireError(asHealthException(error));
                return;
            }
            synchronized (session.pendingWrites) {
                session.pendingWrites.addAll(0, retryable);
            }
            // Re-armed explicitly, but only while the session is still
            // running. Rescheduling from a session that has ended --
            // where a permanent failure like a revoked permission or an
            // unavailable store lands -- retried forever, keeping the
            // dead session alive and firing store writes and errors after
            // shutdown.
            //
            // Both terminal states, not just STOPPED: the reconnect
            // ladder retires a session as FAILED, which passed a
            // stopped-only check and left exactly the session nobody
            // holds a handle to retrying on a timer.
            if (!session.isTerminal()) {
                session.scheduleFlush(session.getOptions()
                        .getStoreBatchMillis());
            }
            session.fireError(asHealthException(error));
        }

        private static HealthException asHealthException(Throwable error) {
            return error instanceof HealthException
                    ? (HealthException) error
                    : new HealthException(HealthError.UNKNOWN,
                            "could not persist sensor samples", error);
        }
    }

    /// Whether this session has ended, however it ended.
    ///
    /// `STOPPED` is the caller asking; `FAILED` is the framework giving
    /// up. Nothing may be scheduled from either -- the session is gone
    /// from the manager's registry, so a timer armed here is one nobody
    /// can cancel.
    final boolean isTerminal() {
        SensorSessionState s = getState();
        return s == SensorSessionState.STOPPED
                || s == SensorSessionState.FAILED;
    }

    /// Decodes a payload into zero or more samples, or `null` when the
    /// payload is malformed.
    ///
    /// A single notification can yield several samples: a cycling power
    /// meter reports power and crank data in one packet, from which both
    /// power and cadence are derived.
    @SuppressWarnings("PMD.CompareObjectsWithEquals")
    private List<HealthSample> decode(byte[] value, long at) {
        List<HealthSample> out = new ArrayList<HealthSample>(2);
        if (profile == HealthSensorProfile.HEART_RATE) {
            HeartRateMeasurement m = HeartRateMeasurement.parse(value);
            if (m == null) {
                return null;
            }
            // Only the rate becomes a sample. RR intervals are decoded by
            // the parser and have no health data type to be a sample of --
            // they are the input to HRV, not a measurement of it, and
            // inventing a type for them would put a number in the store
            // that no platform can read back. An app that needs them
            // subscribes to 0x2A37 through com.codename1.bluetooth.le and
            // calls HeartRateMeasurement.parse itself; both are public,
            // and SensorSessionOptions#setWriteToStore documents the
            // route. See HeartRateMeasurement#getRrIntervalCount().
            out.add(quantity(HealthDataType.HEART_RATE, m.getHeartRate(),
                    HealthUnit.COUNT_PER_MINUTE, at));
            return out;
        }
        if (profile == HealthSensorProfile.CYCLING_POWER) {
            CyclingPowerMeasurement m = CyclingPowerMeasurement.parse(value);
            if (m == null) {
                return null;
            }
            out.add(quantity(HealthDataType.POWER,
                    m.getInstantaneousPowerWatts(), HealthUnit.WATT, at));
            if (m.hasCrankData()) {
                double rpm = crankTracker.update(m.getCrankRevolutions(),
                        m.getLastCrankEventTime());
                if (!Double.isNaN(rpm)) {
                    out.add(quantity(HealthDataType.CYCLING_CADENCE, rpm,
                            HealthUnit.COUNT_PER_MINUTE, at));
                }
            }
            return out;
        }
        if (profile == HealthSensorProfile.CYCLING_SPEED_CADENCE) {
            CscMeasurement m = CscMeasurement.parse(value);
            if (m == null) {
                return null;
            }
            if (m.hasCrankData()) {
                double rpm = crankTracker.update(m.getCrankRevolutions(),
                        m.getLastCrankEventTime());
                if (!Double.isNaN(rpm)) {
                    out.add(quantity(HealthDataType.CYCLING_CADENCE, rpm,
                            HealthUnit.COUNT_PER_MINUTE, at));
                }
            }
            if (m.hasWheelData()) {
                // The wheel rate is deliberately not published. It is not
                // cadence -- cadence is the crank, reported in the block
                // below -- and a combination sensor sending both would
                // otherwise emit two CYCLING_CADENCE samples per
                // notification with very different values. Turning it into
                // speed or distance needs a wheel circumference we do not
                // have, and guessing the tyre size scales every distance
                // the app ever reports.
                wheelTracker.update(m.getWheelRevolutions(),
                        m.getLastWheelEventTime());
            }
            return out;
        }
        if (profile == HealthSensorProfile.RUNNING_SPEED_CADENCE) {
            RscMeasurement m = RscMeasurement.parse(value);
            if (m == null) {
                return null;
            }
            out.add(quantity(HealthDataType.SPEED,
                    m.getSpeedMetersPerSecond(), HealthUnit.METER_PER_SECOND,
                    at));
            out.add(quantity(HealthDataType.RUNNING_CADENCE,
                    m.getCadenceStepsPerMinute(), HealthUnit.COUNT_PER_MINUTE,
                    at));
            return out;
        }
        if (profile == HealthSensorProfile.HEALTH_THERMOMETER) {
            TemperatureMeasurement m = TemperatureMeasurement.parse(value);
            if (m == null) {
                return null;
            }
            out.add(quantity(HealthDataType.BODY_TEMPERATURE, m.getCelsius(),
                    HealthUnit.DEGREE_CELSIUS,
                    m.getTimestampMillis() > 0 ? m.getTimestampMillis() : at));
            return out;
        }
        if (profile == HealthSensorProfile.WEIGHT_SCALE) {
            WeightMeasurement m = WeightMeasurement.parse(value);
            if (m == null) {
                return null;
            }
            out.add(quantity(HealthDataType.BODY_MASS, m.getWeightKg(),
                    HealthUnit.KILOGRAM,
                    m.getTimestampMillis() > 0 ? m.getTimestampMillis() : at));
            return out;
        }
        if (profile == HealthSensorProfile.BLOOD_PRESSURE) {
            BloodPressureMeasurement m =
                    BloodPressureMeasurement.parse(value);
            if (m == null) {
                return null;
            }
            long when = m.getTimestampMillis() > 0
                    ? m.getTimestampMillis() : at;
            BloodPressureSample s = BloodPressureSample.create(
                    m.getSystolicMmHg(), m.getDiastolicMmHg(), when);
            if (m.hasPulse()) {
                s.setPulse(new HealthQuantity(m.getPulseBpm(),
                        HealthUnit.COUNT_PER_MINUTE));
            }
            s.setRecordingMethod(RecordingMethod.AUTOMATIC);
            out.add(s);
            return out;
        }
        if (profile == HealthSensorProfile.GLUCOSE) {
            GlucoseMeasurement m = GlucoseMeasurement.parse(value);
            if (m == null) {
                return null;
            }
            if (!m.hasConcentration() || m.isControlSolution()) {
                // A failed test, or a calibration against control fluid.
                // Neither is the user's blood glucose and neither belongs
                // in their record.
                return out;
            }
            long when = m.getTimestampMillis() > 0
                    ? m.getTimestampMillis() : at;
            out.add(quantity(HealthDataType.BLOOD_GLUCOSE,
                    m.getMillimolesPerLiter(), HealthUnit.MILLIMOLE_PER_LITER,
                    when));
            return out;
        }
        return out;
    }

    private static QuantitySample quantity(HealthDataType type, double value,
            HealthUnit unit, long at) {
        QuantitySample s = QuantitySample.create(type,
                new HealthQuantity(value, unit), at);
        s.setRecordingMethod(RecordingMethod.AUTOMATIC);
        return s;
    }

    // ------------------------------------------------------------------
    // event plumbing
    // ------------------------------------------------------------------

    /// Records the device's battery level. Called by the transport.
    protected final void setBatteryPercent(Integer percent) {
        this.batteryPercent = percent;
    }

    /// Records where a heart-rate sensor is worn. Called by the transport.
    protected final void setBodySensorLocation(int location) {
        this.bodySensorLocation = location;
    }

    /// Forgets the cumulative-counter baselines, so the next notification
    /// establishes a new one. Called by the transport on reconnect: a
    /// sensor that power-cycled restarts its counters from zero.
    protected final void resetCounters() {
        wheelTracker.reset();
        crankTracker.reset();
    }

    /// Moves the session to a new state and notifies listeners.
    /// True once this session has actually streamed.
    ///
    /// A failed initial connect also publishes DISCONNECTED, and
    /// reconnecting from that resurrects a session the caller was already
    /// told had failed.
    protected final boolean hasStreamed() {
        return streamed;
    }

    protected final void setState(SensorSessionState newState) {
        if (newState == SensorSessionState.STREAMING) {
            streamed = true;
        }
        if (newState == null || newState == state) {
            return;
        }
        state = newState;
        if (isTerminal()) {
            // Nothing may be scheduled from a terminal state, and that
            // includes what was scheduled just before reaching one: the
            // session is gone from the manager's registry, so a timer
            // left armed is one nobody can cancel.
            //
            // Under the buffer's lock so a timer tick cannot be midway
            // through claiming it -- see flushIfRunning. The explicit
            // flush endSession() performs next does not consult this
            // flag, so a short ride still keeps its final partial batch.
            synchronized (pendingWrites) {
                flushingStopped = true;
            }
            cancelFlush();
        }
        Object[] snapshot = listenerSnapshot();
        if (snapshot != null) {
            Display.getInstance().callSerially(
                    makeStateRunnable(this, snapshot, newState));
        }
    }

    /// Delivers a sample to listeners on the EDT.
    protected final void fireSample(HealthSample sample) {
        Object[] snapshot = listenerSnapshot();
        if (snapshot != null) {
            Display.getInstance().callSerially(
                    makeSampleRunnable(this, snapshot, sample));
        }
    }

    /// Delivers an error to listeners on the EDT.
    protected final void fireError(HealthException error) {
        Object[] snapshot = listenerSnapshot();
        if (snapshot != null) {
            Display.getInstance().callSerially(
                    makeErrorRunnable(this, snapshot, error));
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

    // The dispatch runnables are built in static methods so they carry no
    // synthetic reference to the enclosing session (SpotBugs
    // SIC_INNER_SHOULD_BE_STATIC_ANON).

    private static Runnable makeSampleRunnable(final SensorSession session,
            final Object[] listeners, final HealthSample sample) {
        return new Runnable() {
            @Override
            public void run() {
                for (Object listener : listeners) {
                    ((SensorSampleListener) listener)
                            .sensorSample(session, sample);
                }
            }
        };
    }

    private static Runnable makeStateRunnable(final SensorSession session,
            final Object[] listeners, final SensorSessionState state) {
        return new Runnable() {
            @Override
            public void run() {
                for (Object listener : listeners) {
                    ((SensorSampleListener) listener)
                            .sensorStateChanged(session, state);
                }
            }
        };
    }

    private static Runnable makeErrorRunnable(final SensorSession session,
            final Object[] listeners, final HealthException error) {
        return new Runnable() {
            @Override
            public void run() {
                for (Object listener : listeners) {
                    ((SensorSampleListener) listener)
                            .sensorError(session, error);
                }
            }
        };
    }

    @Override
    public String toString() {
        return "SensorSession[" + profile.getName() + " " + sensorId + " "
                + state + "]";
    }
}
