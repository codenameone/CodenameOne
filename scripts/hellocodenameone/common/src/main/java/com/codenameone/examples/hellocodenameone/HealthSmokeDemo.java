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
package com.codenameone.examples.hellocodenameone;

import com.codename1.health.Health;
import com.codename1.health.HealthAccess;
import com.codename1.health.HealthChangeBatch;
import com.codename1.health.HealthBackgroundListener;
import com.codename1.health.HealthDataType;
import com.codename1.health.HealthStore;
import com.codename1.health.HealthTimeRange;
import com.codename1.health.SampleQuery;
import com.codename1.health.sensors.HealthSensorProfile;
import com.codename1.health.sensors.HealthSensors;
import com.codename1.io.Log;

/**
 * Exercises the health API from main sources so the build server's class
 * scanner detects it and produces a real health-enabled build.
 *
 * <p>This is what turns the CI smoke build into a genuine test of the
 * native layers: referencing {@code com.codename1.health} outside the
 * sensors subpackage is what makes IPhoneBuilder link HealthKit, flip
 * {@code CN1_INCLUDE_HEALTH} and demand the privacy strings, and what makes
 * AndroidGradleBuilder inject the Health Connect dependency, the manifest
 * fragments and the Kotlin bridge. Without a main-source reference the
 * scanner sees nothing and both bridges silently go untested.</p>
 *
 * <p>Nothing here opens a real health session; it only touches the API
 * surface so the code paths compile and link on device.</p>
 */
public class HealthSmokeDemo {

    /**
     * Registered by the build-generated factory. Present so the builder's
     * {@code implementsInterface} detection has something to find and the
     * generated bindings class is actually produced and compiled.
     */
    public static class SmokeBackgroundListener
            implements HealthBackgroundListener {
        public void healthDataChanged(HealthChangeBatch batch) {
            Log.p("health background batch: " + batch.getAdded().size());
        }
    }

    private HealthSmokeDemo() {
    }

    /** Touches the store surface without requiring any granted access. */
    public static void probeStore() {
        Health health = Health.getInstance();
        Log.p("health availability: " + health.getAvailability());
        HealthStore store = health.getStore();
        Log.p("health supported: " + store.isSupported());
        Log.p("steps supported: "
                + store.isTypeSupported(HealthDataType.STEPS));
        Log.p("push delivery: " + store.isPushDelivery());
        Log.p("read auth: "
                + store.getReadAuthorizationStatus(HealthDataType.STEPS));
        for (String problem : health.getConfigurationProblems()) {
            Log.p("health configuration problem: " + problem);
        }
    }

    /** Issues a bounded read, which must fail cleanly where unsupported. */
    public static void probeRead() {
        SampleQuery q = new SampleQuery()
                .addType(HealthDataType.STEPS)
                .setTimeRange(HealthTimeRange.lastHours(1))
                .setLimit(10);
        Health.getInstance().getStore().readSamples(q)
                .onResult((samples, err) -> {
                    if (err != null) {
                        Log.p("health read failed as expected: " + err);
                    } else {
                        Log.p("health read returned " + samples.size());
                    }
                });
    }

    /** Touches the authorization path without presenting a sheet. */
    public static void probeAuthorizationSurface() {
        HealthAccess[] access =
                HealthAccess.readWrite(HealthDataType.STEPS);
        Log.p("health access pair: " + access.length);
        Health.getInstance().getStore()
                .getAuthorizationRequestStatus(access)
                .onResult((status, err) ->
                        Log.p("health request status: " + status));
    }

    /** Touches the BLE sensor layer, which needs no health store. */
    public static void probeSensors() {
        HealthSensors sensors = Health.getInstance().getSensors();
        Log.p("health sensors supported: " + sensors.isSupported());
        Log.p("known sensor profiles: "
                + HealthSensorProfile.values().size());
    }
}
