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

import com.codename1.health.HealthError;
import com.codename1.health.HealthException;
import com.codename1.util.AsyncResource;
import com.codename1.impl.async.EdtResult;

/// Starts and tracks workout recordings.
///
/// Obtain one from [com.codename1.health.Health#getWorkouts()]; it is
/// never null.
///
/// #### Check what the platform will actually do for you
///
/// Two capabilities that are easy to conflate, and that this API keeps
/// separate on purpose:
///
/// - [#isLiveSessionSupported()] -- the OS runs a real session, keeping
///   the app alive while it records.
/// - [#isSensorCollectionSupported()] -- the OS also gathers heart rate
///   and energy into that session by itself.
///
/// **Both are `false` on every platform in this release.** No port
/// implements an OS-owned session yet, so every workout is recorded: the
/// framework keeps the clock and the rollup, and persists what you feed
/// it when the session ends. That is exactly the flow Google documents
/// for Android phones, and it is why these are queryable facts rather
/// than assumptions -- code that branches on them today keeps working
/// unchanged when a port starts answering true.
///
/// Where the second is false, a workout records only what you feed it.
/// Building a UI with a live heart-rate readout without checking would
/// produce an app that works on a watch and shows a permanent dash on a
/// phone.
///
/// ```java
/// WorkoutManager workouts = Health.getInstance().getWorkouts();
/// workouts.startSession(new WorkoutConfiguration()
///         .setActivityType(WorkoutActivityType.RUNNING)
///         .setLocationType(WorkoutLocationType.OUTDOOR))
///     .onResult((session, err) -> {
///         if (err != null) { Log.e(err); return; }
///         // startSession() only builds the session; this starts the
///         // clock and moves it to RUNNING, and until it is called
///         // addSamples() and end() fail with SESSION_STATE.
///         session.start();
///         if (!session.isLive()) {
///             status.setText("Recording - connect a strap for heart rate");
///         }
///     });
/// ```
public class WorkoutManager {

    private WorkoutSession activeSession;

    /// Guards [#activeSession] so the one-at-a-time rule is a rule
    /// rather than a race.
    private final Object sessionLock = new Object();

    /// Whether the operating system provides a real workout session that
    /// keeps the app alive and owns the recording.
    ///
    /// `false` everywhere in this release: no port overrides this, so a
    /// workout is always recorded by the framework rather than owned by
    /// the OS. Health Connect has no such concept at all on phones, and
    /// `androidx.health.services` is Wear OS only; HealthKit does have
    /// `HKWorkoutSession`, but nothing here drives it yet.
    /// [#startSession(WorkoutConfiguration)] works regardless, in
    /// recorded mode.
    public boolean isLiveSessionSupported() {
        return false;
    }

    /// Whether the operating system collects sensor data into the session
    /// on its own, without the app feeding samples in.
    ///
    /// `false` on every platform in this release. It is the OS-owned
    /// session that would do the collecting, and no port runs one yet --
    /// so a workout contains what you fed it through
    /// [WorkoutSession#addSamples(java.util.List)], and nothing else.
    public boolean isSensorCollectionSupported() {
        return false;
    }

    /// Starts a workout.
    ///
    /// Only one session may run at a time; starting another while one is
    /// active fails with [HealthError#SESSION_STATE] rather than silently
    /// abandoning the first, because an abandoned workout is data the user
    /// believed was being recorded.
    public final AsyncResource<WorkoutSession> startSession(
            WorkoutConfiguration configuration) {
        AsyncResource<WorkoutSession> out =
                new EdtResult<WorkoutSession>();
        WorkoutSession session;
        // The check, the creation and the assignment as one step. Read
        // and then written, two callers both found nothing running and
        // both were handed a session they could start: the second
        // assignment only hid the first from getActiveSession(), while
        // both went on collecting and both persisted a workout at the
        // end -- which is the opposite of what the one-at-a-time rule
        // exists to promise.
        synchronized (sessionLock) {
            WorkoutSession existing = activeSession;
            if (existing != null && isRunning(existing)) {
                out.error(new HealthException(HealthError.SESSION_STATE,
                        "a workout is already in progress; end or discard"
                                + " it before starting another"));
                return out;
            }
            session = createSession(
                    configuration == null ? new WorkoutConfiguration()
                            : configuration);
            activeSession = session;
            session.setManager(this);
        }
        out.complete(session);
        return out;
    }

    /// The session currently running or paused, or null.
    public final WorkoutSession getActiveSession() {
        WorkoutSession s;
        synchronized (sessionLock) {
            s = activeSession;
            if (s != null && !isRunning(s)) {
                // Dropped, not just hidden. Reporting null while still
                // holding the reference kept a finished session -- its
                // recording, its listeners and its statistics -- reachable
                // through this manager until another was started, which for
                // an app that runs one workout is the life of the process.
                activeSession = null;
                s = null;
            }
        }
        return s;
    }

    /// Drops `session` if it is the one being held.
    ///
    /// Called by the session itself when it reaches a terminal state. The
    /// identity test matters: a session that ended after another had already
    /// been started must not clear the newer one.
    final void releaseIfCurrent(WorkoutSession session) {
        synchronized (sessionLock) {
            if (activeSession == session) { //NOPMD CompareObjectsWithEquals
                // Identity, not equality: the question is whether this is the
                // instance being held, not whether an equal one is.
                activeSession = null;
            }
        }
    }

    private static boolean isRunning(WorkoutSession s) {
        WorkoutSessionState state = s.getState();
        return state == WorkoutSessionState.NOT_STARTED
                || state == WorkoutSessionState.PREPARING
                || state == WorkoutSessionState.RUNNING
                || state == WorkoutSessionState.PAUSED;
    }

    /// Creates the session object. Ports override to return a session
    /// backed by a real platform workout; the default records in shared
    /// code and writes on end.
    protected WorkoutSession createSession(WorkoutConfiguration config) {
        return new RecordedWorkoutSession(config);
    }
}
