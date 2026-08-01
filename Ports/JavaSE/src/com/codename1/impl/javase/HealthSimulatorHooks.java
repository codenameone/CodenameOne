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
package com.codename1.impl.javase;

import com.codename1.health.Health;
import com.codename1.health.HealthAuthorizationStatus;
import com.codename1.health.HealthError;
import com.codename1.health.HealthSample;
import com.codename1.health.HealthStore;
import com.codename1.impl.health.LocalHealth;
import com.codename1.impl.javase.health.SimulatedHealthStore;
import com.codename1.impl.javase.health.SyntheticHealthData;

import java.util.List;

/// Simulate-menu actions for the health API, also callable from tests as
/// `CN.execute("health:itemN")`.
///
/// Every method is `public static void` with no arguments -- the contract
/// {@code SimulatorHookLoader} enforces -- and runs on the EDT.
public class HealthSimulatorHooks {

    /// A fixed seed so a scripted dataset is identical run to run, which is
    /// what lets a test assert an exact total.
    private static final long DEMO_SEED = 20260101L;

    private HealthSimulatorHooks() {
    }

    /// The simulated store behind the current health entry point, or null
    /// when the port is not using one.
    private static SimulatedHealthStore store() {
        Health h = Health.getInstance();
        HealthStore s = h.getStore();
        if (s instanceof SimulatedHealthStore) {
            return (SimulatedHealthStore) s;
        }
        System.err.println("Health simulation: the active port is not using"
                + " a simulated health store");
        return null;
    }

    /// Grants every read and write, the permissive case.
    public static void grantAllPermissions() {
        SimulatedHealthStore s = store();
        if (s != null) {
            s.setAllReadPermissions(
                    SimulatedHealthStore.ReadAuthScript.GRANTED);
            s.setAllWritePermissions(HealthAuthorizationStatus.AUTHORIZED);
        }
    }

    /// Denies every read and write.
    public static void denyAllPermissions() {
        SimulatedHealthStore s = store();
        if (s != null) {
            s.setAllReadPermissions(
                    SimulatedHealthStore.ReadAuthScript.DENIED_SILENT);
            s.setAllWritePermissions(HealthAuthorizationStatus.DENIED);
        }
    }

    /// **The trap.** Grants writes, silently refuses reads, and emulates
    /// iOS -- so authorization appears to succeed, the status reads
    /// UNKNOWN, and every query returns empty with no error.
    ///
    /// This is the single most useful thing to click before shipping a
    /// health app, because it is the behaviour real users will produce and
    /// the one a permissive simulator never shows you.
    public static void grantWriteDenyReadSilently() {
        SimulatedHealthStore s = store();
        if (s != null) {
            s.setReadAuthorizationPolicy(
                    SimulatedHealthStore.ReadAuthPolicy.IOS_OPAQUE);
            s.setAllReadPermissions(
                    SimulatedHealthStore.ReadAuthScript.DENIED_SILENT);
            s.setAllWritePermissions(HealthAuthorizationStatus.AUTHORIZED);
        }
    }

    /// Emulates Health Connect instead: read refusals fail loudly.
    public static void useAndroidPermissionBehaviour() {
        SimulatedHealthStore s = store();
        if (s != null) {
            s.setReadAuthorizationPolicy(
                    SimulatedHealthStore.ReadAuthPolicy.ANDROID_EXPLICIT);
        }
    }

    /// Emulates HealthKit: read refusals are indistinguishable from having
    /// no data. The default.
    public static void useIosPermissionBehaviour() {
        SimulatedHealthStore s = store();
        if (s != null) {
            s.setReadAuthorizationPolicy(
                    SimulatedHealthStore.ReadAuthPolicy.IOS_OPAQUE);
        }
    }

    /// Loads a week of synthetic steps, heart rate, sleep and weight.
    public static void loadDemoDataset() {
        SimulatedHealthStore s = store();
        if (s == null) {
            return;
        }
        List<HealthSample> samples = new SyntheticHealthData(DEMO_SEED)
                .generateWeek(System.currentTimeMillis());
        s.seed(samples);
        System.out.println("Health simulation: loaded " + samples.size()
                + " synthetic samples");
    }

    /// Removes every stored sample.
    public static void clearAllData() {
        SimulatedHealthStore s = store();
        if (s != null) {
            s.clear();
        }
    }

    /// Clears scripted permissions, faults and availability, leaving data
    /// alone.
    public static void resetScripts() {
        SimulatedHealthStore s = store();
        if (s != null) {
            s.resetScripts();
        }
    }

    /// Makes the store report itself unavailable, as a missing or disabled
    /// Health Connect provider would.
    public static void setHealthUnavailable() {
        SimulatedHealthStore s = store();
        if (s != null) {
            s.setAvailable(false);
        }
    }

    /// Makes the store available again.
    public static void setHealthAvailable() {
        SimulatedHealthStore s = store();
        if (s != null) {
            s.setAvailable(true);
        }
    }

    /// Fails the next query once, then recovers -- so a test can assert
    /// that the app retries rather than wedging.
    public static void primeQueryFailure() {
        SimulatedHealthStore s = store();
        if (s != null) {
            s.failNext("query", HealthError.DATABASE_INACCESSIBLE,
                    "simulated locked-device read failure");
        }
    }

    /// Fails the next write once, then recovers.
    public static void primeSaveFailure() {
        SimulatedHealthStore s = store();
        if (s != null) {
            s.failNext("save", HealthError.UNAUTHORIZED,
                    "simulated write authorization failure");
        }
    }

    /// Creates the health entry point the JavaSE port installs.
    static Health createSimulatedHealth() {
        return new LocalHealth(new SimulatedHealthStore());
    }
}
