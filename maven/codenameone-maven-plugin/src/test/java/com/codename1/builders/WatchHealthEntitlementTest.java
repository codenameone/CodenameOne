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

import com.codename1.builders.BuildRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Who gets the HealthKit entitlement on the watch bundle.
 *
 * <p>The watch app is signed on its own App ID. Entitling it for HealthKit
 * when it never touches health data makes codesigning fail against a
 * profile without the capability -- and failing to entitle a watch app
 * that does use health makes its authorization request be refused at
 * runtime. The phone's privacy strings answer this only when the watch
 * runs the phone's code.</p>
 */
public class WatchHealthEntitlementTest {

    private static WatchNativeBuilder builder(String watchMain,
            String phoneMain, String healthHint, String workoutHint) {
        BuildRequest r = new BuildRequest();
        r.setMainClass(phoneMain);
        if (watchMain != null) {
            r.putArgument("watchMain", watchMain);
        } else {
            r.putArgument("watchNative.enabled", "true");
        }
        if (healthHint != null) {
            r.putArgument("watchNative.health", healthHint);
        }
        if (workoutHint != null) {
            r.putArgument("watchNative.health.workoutProcessing", workoutHint);
        }
        WatchNativeBuilder b = new WatchNativeBuilder(null);
        b.parseHints(r);
        return b;
    }

    private static BuildRequest entitlementRequest(String backgroundDelivery,
            String recalibrate) {
        BuildRequest r = new BuildRequest();
        r.setMainClass("com.acme.MyApp");
        if (backgroundDelivery != null) {
            r.putArgument("ios.health.backgroundDelivery", backgroundDelivery);
        }
        if (recalibrate != null) {
            r.putArgument("ios.health.recalibrateEstimates", recalibrate);
        }
        return r;
    }

    private static final String RECALIBRATE =
            "com.apple.developer.healthkit.recalibrate-estimates";
    private static final String BACKGROUND =
            "com.apple.developer.healthkit.background-delivery";

    /**
     * The watch is signed with its own entitlements and inherits nothing,
     * so a capability the phone was granted has to be repeated here.
     * Recalibration is performed by shared workout code, on the target
     * where it is most likely to run at all.
     */
    @Test
    void recalibrationReachesTheWatchEntitlements() {
        String plist = WatchNativeBuilder.watchEntitlementsPlist(
                entitlementRequest(null, "true"), null);
        assertTrue(plist.contains(RECALIBRATE),
                "the watch must carry the capability the hint asked for:\n"
                        + plist);
        assertTrue(plist.contains("com.apple.developer.healthkit</key>"),
                "and the base entitlement is always there");
    }

    @Test
    void theWatchAsksForNoCapabilityItWasNotGiven() {
        String plist = WatchNativeBuilder.watchEntitlementsPlist(
                entitlementRequest(null, null), null);
        assertFalse(plist.contains(RECALIBRATE),
                "an unrequested capability must not be claimed:\n" + plist);
        assertFalse(plist.contains(BACKGROUND), plist);
    }

    /**
     * The canonical entitlement key counts as well as the short hint.
     *
     * <p>The short hint promotes into the {@code ios.entitlements.*}
     * namespace for the phone, and a project can set the canonical key
     * directly instead -- the phone honours both, so a watch reading only
     * the short one signed without a capability the build had granted.</p>
     */
    @Test
    void theCanonicalEntitlementKeyReachesTheWatch() {
        BuildRequest r = new BuildRequest();
        r.setMainClass("com.acme.MyApp");
        r.putArgument("ios.entitlements.com.apple.developer.healthkit"
                + ".recalibrate-estimates", "true");
        assertTrue(WatchNativeBuilder.watchEntitlementsPlist(r, null)
                .contains(RECALIBRATE),
                "the canonical spelling must be honoured here too");

        BuildRequest bg = new BuildRequest();
        bg.setMainClass("com.acme.MyApp");
        bg.putArgument("ios.entitlements.com.apple.developer.healthkit"
                + ".background-delivery", "true");
        assertTrue(WatchNativeBuilder.watchEntitlementsPlist(bg, null)
                .contains(BACKGROUND),
                "and background delivery has the same two spellings");
    }

    /**
     * Workout processing does not ask for background delivery.
     *
     * <p>A workout keeps running through
     * {@code WKBackgroundModes=workout-processing} in the watch
     * Info.plist. The HealthKit sub-entitlement covers HealthKit
     * *delivering updates* to a suspended app, which a workout app need
     * not ask for -- and claiming it put a capability in the signature
     * that a profile carrying only base HealthKit does not have, so
     * entitlement validation refused the build over something nothing
     * used.</p>
     */
    @Test
    void workoutProcessingDoesNotImplyBackgroundDelivery() {
        assertFalse(WatchNativeBuilder.watchEntitlementsPlist(
                entitlementRequest(null, null), "true").contains(BACKGROUND),
                "a workout does not need HealthKit background delivery");
        assertTrue(WatchNativeBuilder.watchEntitlementsPlist(
                entitlementRequest("true", null), "true").contains(BACKGROUND),
                "and the explicit hint still grants it");
    }


    /**
     * A watch that records workouts needs the *update* string, not just
     * one of the two.
     *
     * <p>A workout writes: it saves the session and the child samples the
     * app fed it, and reads nothing. A watch declaring only the share
     * string passed this check and was refused the moment it asked to
     * save the workout -- which is the failure the check exists to
     * prevent, arriving from the other direction.</p>
     */
    @Test
    void aWatchWorkoutNeedsTheUpdateString() {
        assertTrue(WatchNativeBuilder.needsPurposeString(true, "Reads", null,
                true),
                "the share string alone does not let a workout be saved");
        assertFalse(WatchNativeBuilder.needsPurposeString(true, null,
                "Writes", true),
                "and the update string alone is exactly what it needs");
        assertFalse(WatchNativeBuilder.needsPurposeString(false, null, null,
                true),
                "a watch that does not use HealthKit needs nothing");
    }


    /**
     * The string has to match the direction the scan actually saw.
     *
     * <p>Apple wants the disclosure for the operation performed. A watch whose code only reads and
     * declares only {@code NSHealthUpdateUsageDescription} is refused at authorization exactly as
     * if it had declared nothing -- and the reverse holds. Collapsing the detected usage to one
     * boolean accepted the unrelated string and emitted an entitled bundle that fails at runtime,
     * which is the failure this gate exists to prevent, reached by a third route.</p>
     */
    @Test
    void aDetectedDirectionRequiresItsOwnString() {
        // Reads: only the share string will do.
        assertTrue(WatchNativeBuilder.needsPurposeString(true, null, "Writes", false, true, false),
                "a read-only watch is not disclosed by the update string");
        assertFalse(WatchNativeBuilder.needsPurposeString(true, "Reads", null, false, true, false));

        // Writes: only the update string will do.
        assertTrue(WatchNativeBuilder.needsPurposeString(true, "Reads", null, false, false, true),
                "a write-only watch is not disclosed by the share string");
        assertFalse(WatchNativeBuilder.needsPurposeString(true, null, "Writes", false, false, true));

        // Both directions detected: both strings are required.
        assertTrue(WatchNativeBuilder.needsPurposeString(true, "Reads", null, false, true, true));
        assertTrue(WatchNativeBuilder.needsPurposeString(true, null, "Writes", false, true, true));
        assertFalse(WatchNativeBuilder.needsPurposeString(true, "Reads", "Writes", false,
                true, true));

        // Nothing detected -- watchNative.health alone, which says nothing about direction. Either
        // string remains evidence that somebody thought about it, as before.
        assertFalse(WatchNativeBuilder.needsPurposeString(true, null, "Writes", false,
                false, false));
        assertFalse(WatchNativeBuilder.needsPurposeString(true, "Reads", null, false,
                false, false));
        assertTrue(WatchNativeBuilder.needsPurposeString(true, null, null, false, false, false));

        // Still nothing to disclose when the watch does not use HealthKit at all.
        assertFalse(WatchNativeBuilder.needsPurposeString(false, null, null, false, true, true));
    }

    /** The message names the string that is actually missing, not both of them. */
    @Test
    void theErrorNamesTheStringTheWatchNeeds() {
        assertTrue(WatchNativeBuilder.missingPurposeStrings(null, "Writes", true, false)
                .contains("NSHealthShareUsageDescription"));
        assertFalse(WatchNativeBuilder.missingPurposeStrings(null, "Writes", true, false)
                .contains("NSHealthUpdateUsageDescription"),
                "naming the string it already supplied is the wrong remedy");
        assertTrue(WatchNativeBuilder.missingPurposeStrings("Reads", null, false, true)
                .contains("NSHealthUpdateUsageDescription"));
        // Direction unknown: both are named, either will do.
        String either = WatchNativeBuilder.missingPurposeStrings(null, null, false, false);
        assertTrue(either.contains("NSHealthShareUsageDescription"), either);
        assertTrue(either.contains("NSHealthUpdateUsageDescription"), either);
    }


    /**
     * The watch inherits the app-wide answer, and the hint is how you disagree with it.
     *
     * <p>This used to be decided per translation root by a class walk in IPhoneBuilder. The walk
     * was deleted: what it protected against -- a phone-only native's Objective-C compiled for
     * watchOS -- happens regardless of what any stub names, because ParparVM copies every
     * non-class file on the classpath into its output verbatim. A native that cannot build for
     * watchOS is guarded with TARGET_OS_WATCH in its own source, as the port guards its own.</p>
     */
    @Test
    void theWatchFollowsTheAppWideScan() {
        assertTrue(builder("com.acme.WatchApp", "com.acme.MyApp", null, null)
                .watchUsesHealth(true),
                "an app that uses HealthKit entitles the watch too");
        assertFalse(builder("com.acme.WatchApp", "com.acme.MyApp", null, null)
                .watchUsesHealth(false),
                "and one that does not, does not");
    }

    /** The explicit hint settles it in both directions. */
    @Test
    void theExplicitHintWins() {
        assertTrue(builder("com.acme.WatchApp", "com.acme.MyApp", "true", null)
                .watchUsesHealth(false));
        assertFalse(builder(null, "com.acme.MyApp", "false", null)
                .watchUsesHealth(true),
                "an explicit false wins over any reachability answer");
    }

    /** A workout session is HealthKit, so it implies the entitlement. */
    @Test
    void workoutProcessingImpliesHealth() {
        assertTrue(builder("com.acme.WatchApp", "com.acme.MyApp", null, "true")
                .watchUsesHealth(false));
    }

    /**
     * An entitled watch must have a purpose string of its own.
     *
     * <p>{@code watchNative.health=true} on a watch with no phone
     * HealthKit hints entitled and signed the bundle while its Info.plist
     * carried neither purpose string. The build passed and the watch was
     * refused the moment it asked for authorization -- and this build
     * never invents a purpose string, so it has to stop instead.</p>
     */
    @Test
    void anEntitledWatchNeedsAPurposeString() {
        assertTrue(WatchNativeBuilder.needsPurposeString(true, null, null,
                false));
        assertFalse(WatchNativeBuilder.needsPurposeString(true, "Reads", null,
                false));
        assertFalse(WatchNativeBuilder.needsPurposeString(true, null, "Writes",
                false));
        // Not entitled, so nothing to disclose.
        assertFalse(WatchNativeBuilder.needsPurposeString(false, null, null, false));
    }

    /**
     * A whitespace-only purpose string is no purpose string.
     *
     * <p>The phone builder already trims these. Here a blank hint emitted
     * an empty {@code NSHealthShareUsageDescription} and satisfied the
     * check that exists to stop an entitled watch shipping without a
     * disclosure.</p>
     */
    @Test
    void aBlankPurposeStringCountsAsMissing() {
        assertNull(WatchNativeBuilder.trimToNull("   "));
        assertNull(WatchNativeBuilder.trimToNull(""));
        assertNull(WatchNativeBuilder.trimToNull(null));
        assertEquals("Reads workouts",
                WatchNativeBuilder.trimToNull("  Reads workouts  "));
        assertTrue(WatchNativeBuilder.needsPurposeString(true,
                WatchNativeBuilder.trimToNull("   "),
                WatchNativeBuilder.trimToNull(null), false));
    }
}
