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

import com.codename1.health.workout.WorkoutSession;

/// How a [SensorSession] should behave: reconnection, where samples go,
/// and how stale a cached reading may be.
public final class SensorSessionOptions {

    private boolean autoReconnect = true;
    private boolean writeToStore;
    private int storeBatchMillis = 60000;
    private WorkoutSession workoutSession;
    private int staleSampleMillis = 10000;

    /// Whether the session re-establishes a dropped link on its own.
    /// Defaults to `true`, which is almost always right -- chest straps
    /// drop out routinely as the wearer moves.
    /// An independent copy of these options.
    ///
    /// A session takes one when it is constructed. The builder is fluent
    /// and a caller can reuse a single instance for a second sensor, and
    /// the session reads these values for as long as it runs -- so
    /// reconfiguring the builder afterwards rerouted a strap's readings
    /// into a different workout, started persisting them, or changed the
    /// reconnect policy of a session already connected.
    ///
    /// The workout session is shared rather than copied: it is a live
    /// object the caller is deliberately pointing at.
    SensorSessionOptions copy() {
        SensorSessionOptions out = new SensorSessionOptions();
        out.autoReconnect = autoReconnect;
        out.writeToStore = writeToStore;
        out.storeBatchMillis = storeBatchMillis;
        out.workoutSession = workoutSession;
        out.staleSampleMillis = staleSampleMillis;
        return out;
    }

    public SensorSessionOptions setAutoReconnect(boolean autoReconnect) {
        this.autoReconnect = autoReconnect;
        return this;
    }

    /// `true` when the session reconnects automatically.
    public boolean isAutoReconnect() {
        return autoReconnect;
    }

    /// Whether received samples are written through to the platform health
    /// store. **Defaults to `false`, and that default is deliberate.**
    ///
    /// During a workout on watchOS -- and on iOS 26 and later -- the
    /// operating system records heart rate into HealthKit itself. A strap
    /// that also writes its own heart-rate samples then produces two
    /// overlapping sets for the same minutes, and every downstream
    /// average, maximum and chart is computed over double-counted data.
    ///
    /// This release drives no live workout session, so that overlap comes
    /// from a workout the user started elsewhere -- in Apple's own Workout
    /// app, say -- rather than from one of yours. The default stays
    /// `false` regardless: turning it on is a decision about somebody
    /// else's data as much as your own.
    ///
    /// If you are recording a workout, attach the session to it with
    /// [#setWorkoutSession(WorkoutSession)] instead: samples land in the
    /// workout's statistics and are persisted once, as part of the
    /// workout, when it ends.
    ///
    /// Turning this on is right when you are logging a standalone
    /// measurement the OS knows nothing about -- a weight from a scale, a
    /// body temperature from a thermometer, a glucose reading from a
    /// meter.
    ///
    /// **Blood pressure is not among them on mobile in this release.** It
    /// is two values in one reading, which HealthKit models as a
    /// correlation and Health Connect as its own record type, and neither
    /// is implemented here -- the sample line this API writes over carries
    /// a single value. A cuff reading routed to the store therefore fails
    /// with [HealthError#TYPE_NOT_SUPPORTED] on both platforms rather than
    /// being quietly dropped, and the local and simulator stores keep it
    /// fine. Read it off the session and persist it yourself until the
    /// correlation paths land.
    public SensorSessionOptions setWriteToStore(boolean writeToStore) {
        this.writeToStore = writeToStore;
        return this;
    }

    /// `true` when samples are written through to the health store.
    public boolean isWriteToStore() {
        return writeToStore;
    }

    /// How long samples are batched before being written to the store.
    /// Defaults to one minute. Writing every notification individually
    /// would mean a store round trip per second per sensor.

    /// How long samples are batched before a store write.
    ///
    /// The [Duration] form of [#setStoreBatchMillis(int)], which is the type the rest of
    /// the framework speaks; the millis form stays for the ports and the
    /// wire format.
    public SensorSessionOptions setStoreBatch(java.time.Duration value) {
        if (value == null) {
            throw new IllegalArgumentException("a duration is required");
        }
        // Range-checked before the narrowing. A duration beyond
        // Integer.MAX_VALUE milliseconds -- about 24.9 days -- wrapped
        // negative, and a negative here does not mean "very long": the scan
        // timeout reads it as disabled and leaves an expensive BLE scan
        // running, and the batch and staleness windows read it as zero and
        // flush every sample or treat every cached one as stale.
        if (value.toMillis() > Integer.MAX_VALUE || value.toMillis() < 0) {
            throw new IllegalArgumentException(
                    "a duration here must be between zero and "
                            + Integer.MAX_VALUE + " milliseconds, got "
                            + value.toMillis());
        }
        return setStoreBatchMillis((int) value.toMillis());
    }

    /// How long samples are batched before a store write, as a [Duration].
    public java.time.Duration getStoreBatch() {
        return java.time.Duration.ofMillis(getStoreBatchMillis());
    }

    public SensorSessionOptions setStoreBatchMillis(int storeBatchMillis) {
        this.storeBatchMillis = storeBatchMillis;
        return this;
    }

    /// The store write-batch interval in milliseconds.
    public int getStoreBatchMillis() {
        return storeBatchMillis;
    }

    /// Attaches this sensor to a workout, so its samples become part of
    /// that workout's statistics and are persisted with it.
    ///
    /// This is the right way to feed a heart-rate strap into a recorded
    /// workout -- see the warning on [#setWriteToStore(boolean)].
    public SensorSessionOptions setWorkoutSession(
            WorkoutSession workoutSession) {
        this.workoutSession = workoutSession;
        return this;
    }

    /// The attached workout, or null.
    public WorkoutSession getWorkoutSession() {
        return workoutSession;
    }

    /// How long a cached reading stays current, for
    /// [SensorSession#getLatest(com.codename1.health.HealthDataType)].
    /// Defaults to ten seconds.
    ///
    /// Past this age `getLatest` returns null rather than a stale value,
    /// so a UI bound to it shows a dash instead of silently displaying the
    /// heart rate from before the strap fell off.

    /// How old a sample may be before it is dropped.
    ///
    /// The [Duration] form of [#setStaleSampleMillis(int)], which is the type the rest of
    /// the framework speaks; the millis form stays for the ports and the
    /// wire format.
    public SensorSessionOptions setStaleSample(java.time.Duration value) {
        if (value == null) {
            throw new IllegalArgumentException("a duration is required");
        }
        // Range-checked before the narrowing. A duration beyond
        // Integer.MAX_VALUE milliseconds -- about 24.9 days -- wrapped
        // negative, and a negative here does not mean "very long": the scan
        // timeout reads it as disabled and leaves an expensive BLE scan
        // running, and the batch and staleness windows read it as zero and
        // flush every sample or treat every cached one as stale.
        if (value.toMillis() > Integer.MAX_VALUE || value.toMillis() < 0) {
            throw new IllegalArgumentException(
                    "a duration here must be between zero and "
                            + Integer.MAX_VALUE + " milliseconds, got "
                            + value.toMillis());
        }
        return setStaleSampleMillis((int) value.toMillis());
    }

    /// How old a sample may be before it is dropped, as a [Duration].
    public java.time.Duration getStaleSample() {
        return java.time.Duration.ofMillis(getStaleSampleMillis());
    }

    public SensorSessionOptions setStaleSampleMillis(int staleSampleMillis) {
        this.staleSampleMillis = staleSampleMillis;
        return this;
    }

    /// The staleness threshold in milliseconds.
    public int getStaleSampleMillis() {
        return staleSampleMillis;
    }
}
