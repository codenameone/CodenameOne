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
import com.codename1.health.HealthUnit;
import com.codename1.health.QuantitySample;
import com.codename1.health.RecordingMethod;
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
    private long pendingSince;
    private final List<SensorSampleListener> listeners =
            new ArrayList<SensorSampleListener>();
    private final Map<String, HealthSample> latest =
            new HashMap<String, HealthSample>();

    private final CumulativeCounterTracker wheelTracker =
            new CumulativeCounterTracker(0x100000000L, 1024);
    private final CumulativeCounterTracker crankTracker =
            new CumulativeCounterTracker(0x10000L, 1024);

    private SensorSessionState state = SensorSessionState.CONNECTING;
    private Integer batteryPercent;
    private int bodySensorLocation = -1;

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
    /// Fails with [HealthError#NOT_SUPPORTED] on other profiles.
    public AsyncResource<Integer> requestStoredRecords(
            GlucoseRecordFilter filter) {
        AsyncResource<Integer> out = new AsyncResource<Integer>();
        out.error(new HealthException(HealthError.NOT_SUPPORTED,
                "stored records are only available on the glucose profile"));
        return out;
    }

    /// Disconnects and stops delivering measurements. Idempotent.
    public void stop() {
        setState(SensorSessionState.STOPPED);
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
        List<HealthSample> batch = null;
        long now = System.currentTimeMillis();
        synchronized (pendingWrites) {
            if (pendingWrites.isEmpty()) {
                pendingSince = now;
            }
            pendingWrites.addAll(samples);
            if (now - pendingSince >= options.getStoreBatchMillis()) {
                batch = new ArrayList<HealthSample>(pendingWrites);
                pendingWrites.clear();
            }
        }
        if (batch != null) {
            Health.getInstance().getStore().write(batch);
        }
    }

    /// Writes anything still buffered. Called when the session stops so a
    /// short ride does not lose its last partial batch.
    protected final void flushPendingWrites() {
        List<HealthSample> batch;
        synchronized (pendingWrites) {
            if (pendingWrites.isEmpty()) {
                return;
            }
            batch = new ArrayList<HealthSample>(pendingWrites);
            pendingWrites.clear();
        }
        Health.getInstance().getStore().write(batch);
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
    protected final void setState(SensorSessionState newState) {
        if (newState == null || newState == state) {
            return;
        }
        state = newState;
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
