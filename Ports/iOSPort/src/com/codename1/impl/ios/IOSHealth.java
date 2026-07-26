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
import com.codename1.health.HealthStore;
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

    private static final Map<Integer, AsyncResource> REQUESTS =
            new HashMap<Integer, AsyncResource>();
    private static int nextRequestId = 1;

    private final IOSNative nativeInstance;
    private final IOSHealthStore store;

    IOSHealth(IOSNative nativeInstance) {
        this.nativeInstance = nativeInstance;
        this.store = new IOSHealthStore(nativeInstance);
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

    public HealthStore getStore() {
        return store;
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

    /// The message used both here and by the build-hint diagnostic thrown
    /// from [IOSImplementation#getHealth()], so a developer sees the same
    /// wording from a diagnostics screen and from the exception.
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
        return id;
    }

    static synchronized AsyncResource take(int requestId) {
        return REQUESTS.remove(Integer.valueOf(requestId));
    }

    // ---- Callbacks invoked from native code (do not rename) ----

    static void nativeHkAuthorizationResult(int requestId, boolean granted,
            int errorCode, String message) {
        AsyncResource r = take(requestId);
        if (r == null) {
            return;
        }
        // granted reflects only that the sheet completed. HealthKit will
        // not say what the user chose for reads, so neither do we.
        r.complete(Boolean.valueOf(granted));
    }

    static void nativeHkSamples(int requestId, String tsv) {
        AsyncResource r = take(requestId);
        if (r == null) {
            return;
        }
        r.complete(com.codename1.impl.health.HealthWire
                .decodeSamplePage(tsv));
    }

    static void nativeHkSaveResult(int requestId, String uuids) {
        AsyncResource r = take(requestId);
        if (r == null) {
            return;
        }
        r.complete(com.codename1.impl.health.HealthWire
                .decodeWriteResult(uuids));
    }

    static void nativeHkRequestError(int requestId, int errorCode,
            String message) {
        AsyncResource r = take(requestId);
        if (r == null) {
            return;
        }
        r.error(IOSHealthStore.toException(errorCode, message));
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
