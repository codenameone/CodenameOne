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
package com.codename1.builders;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The two platform scanners classify health usage the same way.
 *
 * <p>They answer different questions -- one picks Health Connect and
 * per-type permissions, the other HealthKit and purpose strings -- but
 * they read the same bytecode and must agree on <em>what the app is
 * doing</em>. Three separate defects on this branch were the same shape:
 * a classification rule fixed in the Android scanner and never mirrored,
 * so the iOS build kept the old behaviour until somebody noticed
 * independently. This fails the build instead.</p>
 *
 * <p>It matches on source text rather than behaviour, which is crude; the
 * rules live in anonymous visitor callbacks with no seam to call. Crude
 * and load-bearing beats absent.</p>
 */
public class HealthScannerParityTest {

    private static String source(String simpleName) throws Exception {
        File f = new File("src/main/java/com/codename1/builders/"
                + simpleName + ".java");
        assertTrue(f.exists(), "scanner source must be readable: "
                + f.getAbsolutePath());
        return new String(Files.readAllBytes(f.toPath()),
                StandardCharsets.UTF_8);
    }

    /**
     * Enabling sensor write-through is store use on both platforms. It is
     * the one call inside the sensors package that needs the health
     * stack, and the package-wide exemption hides it by default.
     */
    @Test
    void bothScannersTreatSensorWriteThroughAsStoreUse() throws Exception {
        for (String builder : new String[] {"AndroidGradleBuilder",
                "IPhoneBuilder"}) {
            String src = source(builder);
            assertTrue(src.contains(
                    "com/codename1/health/sensors/SensorSessionOptions"),
                    builder + " must classify setWriteToStore as store use;"
                            + " the sensors exemption hides it otherwise");
            assertTrue(src.contains("setWriteToStore"), builder);
        }
    }

    /**
     * The shared value types are not store use on either platform. A
     * sensor callback names {@code HealthSample} whatever else it does.
     */
    @Test
    void bothScannersExemptTheSharedModelTypes() throws Exception {
        for (String builder : new String[] {"AndroidGradleBuilder",
                "IPhoneBuilder"}) {
            String src = source(builder);
            assertTrue(src.contains("isSharedHealthModel"),
                    builder + " must exempt the health value types, or a"
                            + " BLE-only app is dragged into the health"
                            + " stack by a listener signature");
        }
    }

    /**
     * Obtaining the workout manager is a write and not a read, on both
     * scanners.
     *
     * <p>This test asserted the opposite, on the assumption that a
     * workout reads sensor data. It does not: nothing in
     * {@code com.codename1.health.workout} calls {@code readSamples} or
     * {@code aggregate}, the rollup is computed from the samples the app
     * fed in, and {@code end()} writes them. Demanding the read direction
     * meant a workout-only app could not build without declaring a
     * sensitive read permission it never exercises -- which Play policy
     * asks you not to request and App Review questions on iOS.</p>
     */
    @Test
    void bothScannersTreatWorkoutsAsAWriteOnly() throws Exception {
        for (String builder : new String[] {"AndroidGradleBuilder",
                "IPhoneBuilder"}) {
            String src = source(builder);
            // The classification block, which is the last mention: the
            // earlier ones are the facade condition and its comments.
            int at = src.lastIndexOf("startsWith(\"getWorkouts\")");
            assertTrue(at > 0, builder + " must classify getWorkouts");
            int end = src.indexOf("\n                        }", at);
            if (end < 0) {
                end = Math.min(src.length(), at + 1600);
            }
            String after = src.substring(at, end);
            assertTrue(after.contains("usesHealthWrite = true"),
                    builder + " must set the write direction at"
                            + " getWorkouts");
            assertFalse(after.contains("usesHealthRead = true"),
                    builder + " must not claim a read at getWorkouts; no"
                            + " workout path reads the store");
        }
    }

    /**
     * Naming a store type is not using it, so the class-name branch must
     * not set {@code usesHealthData} -- that flag is what demands
     * {@code android.health.read}/{@code write} hints and fails the build
     * without them.
     *
     * <p>This is a twin-drift guard rather than a rule the reviewer
     * questioned: the BuildDaemon copy of this scanner had already been
     * corrected while the plugin copy still set the flag here, so the same
     * app built locally demanded hints that the cloud build did not. The
     * flag belongs on the real read and write calls, which the
     * {@code usesClassMethod} hook sees.</p>
     */
    @Test
    void namingAStoreTypeDoesNotDemandDirectionHints() throws Exception {
        String src = source("AndroidGradleBuilder");
        int at = src.indexOf("!isSharedHealthModel(cls)");
        assertTrue(at > 0, "the class-name branch must still be there");
        String branch = src.substring(at,
                Math.min(src.length(), at + 200));
        assertFalse(branch.contains("usesHealthData"),
                "the class-name branch must not set usesHealthData; it is"
                        + " set by the read and write call hooks");
    }

    /**
     * Both builders generate background-listener bindings.
     *
     * <p>iOS left {@code implementsInterface} empty on the reasoning that
     * only Health Connect delivery survives process death. Cold launches
     * do too: a restored subscription carries only the listener's class
     * name, and without generated bindings the runtime cannot turn that
     * back into an instance -- so even a manual {@code drainChanges()}
     * after a restart delivered nothing at all.</p>
     */
    @Test
    void bothBuildersGenerateListenerBindings() throws Exception {
        for (String builder : new String[] {"AndroidGradleBuilder",
                "IPhoneBuilder"}) {
            String src = source(builder);
            assertTrue(src.contains("healthScan.implementsInterface("),
                    builder + " must feed the shared listener scan");
            assertTrue(src.contains("healthScan.declaresType("),
                    builder + " must report constructibility, or an"
                            + " abstract declarer gets bound instead of"
                            + " its usable subclass");
            assertTrue(src.contains(
                    "HealthListenerBindings.generate(healthScan.resolve())"),
                    builder + " must generate from the resolved set");
            assertTrue(src.contains("installStatement("),
                    builder + " must install them at startup");
        }
    }

    /**
     * The HealthKit background-delivery entitlement is not inferred from
     * polling calls.
     *
     * <p>Neither {@code subscribe()} nor {@code drainChanges()} registers
     * an {@code HKObserverQuery}, so the entitlement they used to trigger
     * bought nothing while demanding a provisioning-profile capability --
     * and getting that wrong fails codesign with an opaque message.</p>
     */
    @Test
    void backgroundDeliveryEntitlementComesOnlyFromTheHint()
            throws Exception {
        String src = source("IPhoneBuilder");
        assertFalse(src.contains("usesHealthObserver"),
                "the inferred observer flag should be gone");
        assertTrue(src.contains("ios.health.backgroundDelivery"),
                "the explicit hint still turns it on");
    }
}
