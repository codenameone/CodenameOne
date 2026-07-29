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
package com.codename1.impl.ios;

import com.codename1.health.Health;
import com.codename1.health.HealthAvailability;
import com.codename1.health.HealthSample;
import com.codename1.health.SamplePage;
import com.codename1.health.HealthStore;
import com.codename1.ui.Display;
import com.codename1.util.AsyncResource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// The HealthKit-backed health entry point.
///
/// Native results arrive on a dedicated serial GCD queue and are matched
/// back to their caller through a request-id registry, the same shape
/// [IOSBluetooth] uses.
public final class IOSHealth extends Health {

    private static final Map<Integer, Integer> LIMITS =
            new HashMap<Integer, Integer>();
    private static final Map<Integer, AsyncResource> REQUESTS =
            new HashMap<Integer, AsyncResource>();
    private static int nextRequestId = 1;

    private final IOSNative nativeInstance;
    private final IOSHealthStore store;

    IOSHealth(IOSNative nativeInstance) {
        this.nativeInstance = nativeInstance;
        this.store = new IOSHealthStore(nativeInstance, this);
    }

    public boolean isSupported() {
        return nativeInstance.hkIsAvailable();
    }

    public HealthAvailability getAvailability() {
        // HealthKit is part of the OS: it is either present or the device
        // is an iPad too old to have it. There is no separate provider to
        // install, unlike Android.
        return nativeInstance.hkIsAvailable()
                ? HealthAvailability.AVAILABLE
                : HealthAvailability.NOT_SUPPORTED;
    }

    /// The HealthKit-backed store.
    ///
    /// This is where the missing-privacy-string diagnostic is thrown,
    /// rather than on the way into the facade: reaching the facade is
    /// also how an app gets to [#getSensors()], which is pure Bluetooth
    /// LE and needs no HealthKit at all. Throwing earlier made that
    /// supported path unusable on iOS without declaring disclosures the
    /// app does not need.
    ///
    /// Still thrown rather than reported: a missing usage description is
    /// a developer bug that gets the app rejected from the App Store, so
    /// it must not be swallowed into an AsyncResource error nobody reads.
    /// [#getConfigurationProblems()] answers the same question without
    /// throwing, for a diagnostics screen.
    public HealthStore getStore() {
        return store;
    }

    /// Recorded workouts, which persist through the store.
    public com.codename1.health.workout.WorkoutManager getWorkouts() {
        requireUsageStrings();
        return super.getWorkouts();
    }

    /// Throws when the app declares no HealthKit purpose string.
    ///
    /// Called from the operations that read or write, not from
    /// [#getStore()]. An app that only asks the store what it supports --
    /// isTypeSupported, isWritable, getSupportedTypes -- reads nothing,
    /// and the iOS builder deliberately lets such an app build with no
    /// purpose string because there is no truthful text to demand of it.
    /// Throwing on the way to the store made that build fail at runtime
    /// instead, which is the failure the builder's exemption exists to
    /// avoid.
    void requireUsageStrings() {
        if (!nativeInstance.checkHealthShareUsage()
                && !nativeInstance.checkHealthUpdateUsage()) {
            throw new com.codename1.health.HealthConfigurationException(
                    MISSING_USAGE_MESSAGE);
        }
    }

    public AsyncResource<Boolean> openHealthSettings() {
        AsyncResource<Boolean> out = new AsyncResource<Boolean>();
        // iOS has no deep link to an app's Health permissions; the closest
        // is the app's own Settings page. Reporting false rather than
        // opening something misleading lets the caller say so.
        out.complete(Boolean.FALSE);
        return out;
    }

    public List<String> getConfigurationProblems() {
        List<String> problems = new ArrayList<String>();
        if (!nativeInstance.checkHealthShareUsage()
                && !nativeInstance.checkHealthUpdateUsage()) {
            problems.add(MISSING_USAGE_MESSAGE);
        }
        return problems;
    }

    /// The message used both by [#getConfigurationProblems()] and by the
    /// build-hint diagnostic thrown from [#getStore()], so a developer
    /// sees the same wording from a diagnostics screen and from the
    /// exception.
    static final String MISSING_USAGE_MESSAGE =
            "This app uses com.codename1.health but declares no HealthKit "
            + "privacy strings. Add the ios.NSHealthShareUsageDescription "
            + "build hint (to read health data) and/or "
            + "ios.NSHealthUpdateUsageDescription (to write it). The text "
            + "must describe your app's specific use -- Codename One does "
            + "not inject a placeholder, because Apple reviews it against "
            + "your app's behaviour and rejects generic copy.";

    // ------------------------------------------------------------------
    // request registry
    // ------------------------------------------------------------------

    static synchronized int takeId(AsyncResource resource) {
        int id = nextRequestId++;
        REQUESTS.put(Integer.valueOf(id), resource);
        // Every cleanup path used to sit inside a native callback, so a
        // native call that never called back -- the shared timeout fires
        // and completes the resource instead -- left its entry, and its
        // LIMITS entry, in the registry for the life of the process.
        // Completion is the one event that always happens, whichever side
        // produced it.
        resource.onResult(new Forget(id));
        return id;
    }

    /// Drops a request's registry state once its resource is done,
    /// whatever finished it.
    ///
    /// Built as a named static class so it carries no synthetic reference
    /// to anything (SpotBugs `SIC_INNER_SHOULD_BE_STATIC_ANON`).
    private static final class Forget
            implements com.codename1.util.AsyncResult<Object> {
        private final int requestId;

        Forget(int requestId) {
            this.requestId = requestId;
        }

        @Override
        public void onReady(Object value, Throwable error) {
            forget(requestId);
        }
    }

    /// Removes a request's registry entries. Safe to call twice: the
    /// native callback path takes the resource out first, and this runs
    /// afterwards on the completion it caused.
    static synchronized void forget(int requestId) {
        REQUESTS.remove(Integer.valueOf(requestId));
        LIMITS.remove(Integer.valueOf(requestId));
    }

    /// Registers a read whose page must report truncation.
    ///
    /// HealthKit has no continuation token, so the only honest thing a
    /// capped read can say is "there was more". The requested limit is
    /// remembered here and compared against what came back.
    static synchronized int takeId(AsyncResource resource, int limit) {
        int id = takeId(resource);
        LIMITS.put(Integer.valueOf(id), Integer.valueOf(limit));
        return id;
    }

    static synchronized AsyncResource take(int requestId) {
        return REQUESTS.remove(Integer.valueOf(requestId));
    }

    static synchronized int takeLimit(int requestId) {
        Integer v = LIMITS.remove(Integer.valueOf(requestId));
        return v == null ? Integer.MAX_VALUE : v.intValue();
    }

    // ---- Callbacks invoked from native code (do not rename) ----

    static void nativeHkAuthorizationResult(int requestId, boolean granted,
            int errorCode, String message) {
        AsyncResource r = take(requestId);
        if (r == null) {
            return;
        }
        if (errorCode >= 0) {
            // HealthKit failed the request outright rather than the user
            // declining. Completing with `false` and no error would let the
            // caller carry on as though the sheet had simply been answered.
            failOnEdt(r, IOSHealthStore.toException(errorCode, message));
            return;
        }
        // granted reflects only that the sheet completed. HealthKit will
        // not say what the user chose for reads, so neither do we.
        completeOnEdt(r, Boolean.valueOf(granted));
    }

    static void nativeHkSamples(int requestId, String tsv) {
        AsyncResource r = take(requestId);
        int limit = takeLimit(requestId);
        if (r == null) {
            return;
        }
        SamplePage page = com.codename1.impl.health.HealthWire
                .decodeSamplePage(tsv);
        if (limit != Integer.MAX_VALUE && page.size() > limit) {
            // One more came back than was asked for, which is how a capped
            // read learns there is more. Trim to the limit and say so.
            List<HealthSample> kept = new ArrayList<HealthSample>(
                    page.getSamples().subList(0, limit));
            page = new SamplePage(kept, null, true);
        }
        completeOnEdt(r, page);
    }

    static void nativeHkSaveResult(int requestId, String uuids) {
        AsyncResource r = take(requestId);
        if (r == null) {
            return;
        }
        completeOnEdt(r, com.codename1.impl.health.HealthWire
                .decodeWriteResult(uuids));
    }

    static void nativeHkRequestError(int requestId, int errorCode,
            String message) {
        AsyncResource r = take(requestId);
        // Dropped here too, not only on the success path: a read that
        // failed -- the locked-device case, which is exactly when
        // background queries run -- otherwise left its entry in the static
        // map for the life of the process.
        takeLimit(requestId);
        if (r == null) {
            return;
        }
        failOnEdt(r, IOSHealthStore.toException(errorCode, message));
    }

    /// Completes on the EDT.
    ///
    /// These callbacks arrive on CN1Health.m's own GCD health queue, and
    /// AsyncResource runs its callbacks on whatever thread completes it.
    /// Completing directly would hand every application handler a
    /// background thread, breaking the guarantee HealthStore makes that
    /// results arrive on the EDT -- and any handler touching a Component
    /// would race the renderer rather than fail loudly.
    private static void completeOnEdt(AsyncResource r, Object value) {
        Display.getInstance().callSerially(new Complete(r, value));
    }

    private static void failOnEdt(AsyncResource r, Throwable error) {
        Display.getInstance().callSerially(new Fail(r, error));
    }

    /// Named rather than anonymous so SpotBugs does not see an inner class
    /// holding the enclosing reference, and so the translator keeps it.
    private static final class Complete implements Runnable {
        private final AsyncResource resource;
        private final Object value;

        Complete(AsyncResource resource, Object value) {
            this.resource = resource;
            this.value = value;
        }

        public void run() {
            resource.complete(value);
        }
    }

    private static final class Fail implements Runnable {
        private final AsyncResource resource;
        private final Throwable error;

        Fail(AsyncResource resource, Throwable error) {
            this.resource = resource;
            this.error = error;
        }

        public void run() {
            resource.error(error);
        }
    }

    static {
        // ---- do not remove: defeats ParparVM dead-code elimination ----
        // These are only ever called from C, so nothing in the Java graph
        // references them and the translator would otherwise strip them.
        nativeHkAuthorizationResult(-1, false, -1, null);
        nativeHkSamples(-1, null);
        nativeHkSaveResult(-1, null);
        nativeHkRequestError(-1, 0, null);
    }
}
