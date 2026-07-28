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

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The Health Connect manifest fragments. The prefix hazards get dedicated
 * cases because a loose duplicate check silently drops a permission, which
 * surfaces only as a runtime SecurityException on a user's device.
 */
class HealthManifestFragmentsTest {

    private static List<String> list(String... values) {
        List<String> out = new ArrayList<String>();
        for (String v : values) {
            out.add(v);
        }
        return out;
    }

    private static int count(String haystack, String needle) {
        int n = 0;
        int i = haystack.indexOf(needle);
        while (i >= 0) {
            n++;
            i = haystack.indexOf(needle, i + needle.length());
        }
        return n;
    }

    @Test
    void readTokensBecomeReadPermissions() {
        String out = HealthManifestFragments.injectPermissions("",
                list("steps", "heart_rate"), list(), false, false, false, 34);
        assertTrue(out.contains("android.permission.health.READ_STEPS"));
        assertTrue(out.contains("android.permission.health.READ_HEART_RATE"));
        assertFalse(out.contains("WRITE_STEPS"));
    }

    @Test
    void writeTokensBecomeWritePermissions() {
        String out = HealthManifestFragments.injectPermissions("",
                list(), list("steps"), false, false, false, 34);
        assertTrue(out.contains("android.permission.health.WRITE_STEPS"));
        assertFalse(out.contains("READ_STEPS"));
    }

    /**
     * READ_HEART_RATE is a strict prefix of READ_HEART_RATE_VARIABILITY.
     * A substring-based duplicate check would emit only one of them.
     */
    @Test
    void heartRatePrefixDoesNotSuppressHeartRateVariability() {
        String out = HealthManifestFragments.injectPermissions("",
                list("heart_rate", "heart_rate_variability_sdnn"),
                list(), false, false, false, 34);
        assertTrue(out.contains(
                "\"android.permission.health.READ_HEART_RATE\""));
        assertTrue(out.contains(
                "\"android.permission.health.READ_HEART_RATE_VARIABILITY\""));
    }

    /** READ_EXERCISE is a strict prefix of READ_EXERCISE_ROUTE. */
    @Test
    void exercisePrefixDoesNotSuppressLongerPermissions() {
        String out = HealthManifestFragments.injectPermissions("",
                list("workout"), list(), false, false, false, 34);
        assertTrue(out.contains("\"android.permission.health.READ_EXERCISE\""));
    }

    /**
     * READ_HEALTH_DATA is a strict prefix of
     * READ_HEALTH_DATA_IN_BACKGROUND and READ_HEALTH_DATA_HISTORY.
     */
    @Test
    void backgroundAndHistoryPermissionsCoexist() {
        String out = HealthManifestFragments.injectPermissions("",
                list("steps"), list(), true, true, false, 34);
        assertTrue(out.contains(
                "READ_HEALTH_DATA_IN_BACKGROUND"));
        assertTrue(out.contains("READ_HEALTH_DATA_HISTORY"));
    }

    /**
     * A developer who already declared a permission through
     * android.xpermissions must not get a duplicate.
     */
    @Test
    void alreadyDeclaredPermissionIsNotDuplicated() {
        String existing = "    <uses-permission android:name="
                + "\"android.permission.health.READ_STEPS\" />\n";
        String out = HealthManifestFragments.injectPermissions(existing,
                list("steps"), list(), false, false, false, 34);
        assertEquals(1, count(out,
                "\"android.permission.health.READ_STEPS\""));
    }

    /**
     * Three distance tokens map to one Health Connect permission, so the
     * emitted set must be deduplicated.
     */
    @Test
    void tokensSharingAPermissionEmitItOnce() {
        String out = HealthManifestFragments.injectPermissions("",
                list("distance_walking_running", "distance_cycling",
                        "distance_swimming"),
                list(), false, false, false, 34);
        assertEquals(1, count(out,
                "\"android.permission.health.READ_DISTANCE\""));
    }

    @Test
    void workoutSessionsAddForegroundServiceAndActivityRecognition() {
        String out = HealthManifestFragments.injectPermissions("",
                list("workout"), list(), false, false, true, 34);
        assertTrue(out.contains("android.permission.ACTIVITY_RECOGNITION"));
        // No foreground-service permissions: this release ships no
        // foreground service, and requesting permissions the app cannot
        // use invites a Play review question with no good answer.
        assertFalse(out.contains("android.permission.FOREGROUND_SERVICE"));
        assertFalse(out.contains(
                "android.permission.FOREGROUND_SERVICE_HEALTH"));
    }

    @Test
    void foregroundServicePermissionsAreNeverRequested() {
        String out = HealthManifestFragments.injectPermissions("",
                list("workout"), list(), false, false, true, 33);
        assertFalse(out.contains("android.permission.FOREGROUND_SERVICE"));
        assertFalse(out.contains(
                "android.permission.FOREGROUND_SERVICE_HEALTH"));
    }

    @Test
    void queriesDeclareTheProviderExactlyOnce() {
        String once = HealthManifestFragments.injectQueries("");
        assertTrue(once.contains(HealthManifestFragments.PROVIDER_PACKAGE));
        String twice = HealthManifestFragments.injectQueries(once);
        assertEquals(1, count(twice,
                HealthManifestFragments.PROVIDER_PACKAGE));
    }

    /**
     * Health Connect refuses to show its consent dialog to an app with no
     * rationale activity, so omitting this produces a permission request
     * that silently never appears.
     */
    @Test
    void rationaleActivityIsAlwaysDeclared() {
        String out = HealthManifestFragments.injectApplicationEntries("",
                false, 34);
        assertTrue(out.contains(
                HealthManifestFragments.RATIONALE_ACTIVITY));
        assertTrue(out.contains(
                "androidx.health.ACTION_SHOW_PERMISSIONS_RATIONALE"));
    }

    @Test
    void permissionUsageAliasIsApi34Only() {
        String modern = HealthManifestFragments.injectApplicationEntries("",
                false, 34);
        assertTrue(modern.contains("ViewPermissionUsageActivity"));
        String older = HealthManifestFragments.injectApplicationEntries("",
                false, 33);
        assertFalse(older.contains("ViewPermissionUsageActivity"));
    }

    /**
     * No workout service is declared, in either configuration.
     *
     * The manifest used to name a class that does not exist and was never
     * started, so it advertised a keepalive the app never had. Declaring a
     * missing service is worse than declaring none: it reads as protection
     * in a review of the manifest.
     */
    @Test
    void noWorkoutServiceIsDeclared() {
        assertFalse(HealthManifestFragments.injectApplicationEntries("",
                false, 34).contains("HealthWorkoutService"));
        String withWorkouts = HealthManifestFragments
                .injectApplicationEntries("", true, 34);
        assertFalse(withWorkouts.contains("HealthWorkoutService"));
        assertFalse(withWorkouts.contains("foregroundServiceType"));
    }

    /**
     * The activity name legitimately appears twice in a single emission --
     * once as the activity's own name and once as the alias's
     * targetActivity -- so idempotence is that a second pass adds nothing,
     * not that the name occurs once.
     */
    @Test
    void applicationEntriesAreNotDuplicated() {
        String once = HealthManifestFragments.injectApplicationEntries("",
                true, 34);
        String twice = HealthManifestFragments.injectApplicationEntries(once,
                true, 34);
        assertEquals(once, twice, "a second pass must add nothing");
        assertEquals(1, count(twice, "<activity android:name="));
        assertEquals(0, count(twice, "<service android:name="),
                "no service is declared -- see noWorkoutServiceIsDeclared");
    }

    @Test
    void unknownTokensAreReportedRatherThanSilentlySkipped() {
        List<String> unknown = HealthManifestFragments.unknownTokens(
                list("steps", "telepathy"));
        assertEquals(1, unknown.size());
        assertEquals("telepathy", unknown.get(0));
        assertNull(HealthManifestFragments.permissionFor("telepathy", false));
    }

    @Test
    void typeListParsingToleratesWhitespaceAndBlanks() {
        List<String> parsed = HealthManifestFragments.parseTypeList(
                " steps , heart_rate ,, ");
        assertEquals(2, parsed.size());
        assertEquals("steps", parsed.get(0));
        assertEquals("heart_rate", parsed.get(1));
        assertTrue(HealthManifestFragments.parseTypeList(null).isEmpty());
    }

    /**
     * The token table duplicates HealthDataType, which the builder cannot
     * depend on. This golden list is what makes a core-side addition that
     * was not mirrored here fail CI rather than silently produce an app
     * missing a permission.
     */
    @Test
    void tokenSetMatchesTheGoldenList() {
        String[] golden = {
            "active_energy", "basal_body_temperature", "basal_energy",
            "blood_glucose", "blood_pressure", "body_fat_percentage",
            "body_mass", "body_temperature", "bone_mass",
            "cycling_cadence", "dietary_energy", "distance_cycling",
            "distance_swimming", "distance_walking_running",
            "elevation_gained", "exercise_time", "flights_climbed",
            "heart_rate", "heart_rate_variability_sdnn", "height",
            "hydration", "intermenstrual_bleeding", "lean_body_mass",
            "menstruation_flow", "mindful_session", "nutrition",
            "oxygen_saturation", "power", "respiratory_rate",
            "resting_heart_rate", "running_cadence", "sleep", "speed",
            "steps", "vo2_max", "waist_circumference",
            "walking_heart_rate_average", "wheelchair_pushes", "workout"
        };
        List<String> expected = list(golden);
        List<String> actual = new ArrayList<String>(
                HealthManifestFragments.knownTokens());
        assertEquals(expected, actual,
                "HealthDataType and this builder table have diverged; add"
                        + " the new token here and to the golden list");
    }

    /**
     * Health Connect has no BMI permission or record -- BMI is derived from
     * weight and height, not stored. The table used to map it onto
     * BODY_WATER_MASS, which is an unrelated body-composition datum: the
     * app asked users for sensitive data it had no use for, still could not
     * read either BMI input, and declared the wrong thing to Play. Leaving
     * the token out makes the builder reject it with the list of tokens
     * that do work.
     */
    @Test
    void bodyMassIndexIsNotOfferedOnAndroid() {
        assertFalse(HealthManifestFragments.knownTokens()
                .contains("body_mass_index"));
        assertNull(HealthManifestFragments.permissionSuffix(
                "body_mass_index"));
        assertEquals(list(new String[] { "body_mass_index" }),
                HealthManifestFragments.unknownTokens(
                        list(new String[] { "steps", "body_mass_index" })),
                "the builder must reject it rather than silently declaring"
                        + " an unrelated permission");
    }

    @Test
    void keepRulesCoverGeneratedAndPlatformReachedClasses() {
        String rules = HealthManifestFragments.proguardKeepRules(
                list("com.example.StepWatcher"));
        assertTrue(rules.contains(
                HealthManifestFragments.RATIONALE_ACTIVITY));
        assertTrue(rules.contains("CN1HealthConnectBridge"));
        assertTrue(rules.contains("HealthConnectDelegate"));
        assertTrue(rules.contains("-keepnames class com.example.StepWatcher"));
    }

    /**
     * The rationale activity resolves this resource by name at runtime, so
     * the two spellings have to agree. When they drift the screen a user
     * reaches by asking why the app wants their health data comes up blank,
     * and nothing fails until then.
     */
    @Test
    public void policyUrlResourceMatchesTheRationaleActivity() {
        assertEquals("cn1_health_privacy_policy",
                HealthManifestFragments.POLICY_URL_RESOURCE);
    }

    /**
     * The read and write directions are classified apart.
     *
     * <p>Health Connect permissions are directional. Collapsing both into a
     * single "uses health data" flag let an app that only reads satisfy the
     * build check with an {@code android.health.write} declaration alone;
     * the manifest then carried no read permission and every read failed at
     * runtime with nothing in the build log to explain it.</p>
     */
    @Test
    void storeCallsAreClassifiedByDirection() {
        for (String read : new String[] {"readSamples", "readWorkouts",
                "aggregate", "subscribe", "hasAnyData", "drainChanges"}) {
            assertTrue(HealthManifestFragments.isReadCall(read),
                    read + " reads");
            assertFalse(HealthManifestFragments.isWriteCall(read),
                    read + " does not write");
        }
        for (String write : new String[] {"write", "writeAll", "delete",
                "deleteByRange"}) {
            assertTrue(HealthManifestFragments.isWriteCall(write),
                    write + " writes");
            assertFalse(HealthManifestFragments.isReadCall(write),
                    write + " does not read");
        }
        // A delete needs the write permission, not a third kind.
        assertTrue(HealthManifestFragments.isWriteCall("delete"));
        // Neither direction, so neither hint is demanded.
        assertFalse(HealthManifestFragments.isReadCall("isSupported"));
        assertFalse(HealthManifestFragments.isWriteCall("isSupported"));
        assertFalse(HealthManifestFragments.isReadCall(null));
        assertFalse(HealthManifestFragments.isWriteCall(null));
    }

    /**
     * A `kotlin-gradle-plugin` the app declares for itself is visible to
     * the build.
     *
     * <p>The Gradle generator skips adding its own plugin line when
     * `android.topDependency` already declares one, so raising
     * `requireKotlinStdlib` to the Health Connect floor changed nothing
     * and the bridge was compiled by whatever compiler the app pinned.
     * The build has to read what was actually declared.</p>
     */
    @Test
    void declaredKotlinPluginVersionIsReadFromTopDependency() {
        assertEquals("1.7.22",
                HealthManifestFragments.declaredKotlinPluginVersion(
                        "classpath 'org.jetbrains.kotlin:"
                        + "kotlin-gradle-plugin:1.7.22'\n"));
        assertEquals("1.9.22",
                HealthManifestFragments.declaredKotlinPluginVersion(
                        "classpath \"org.jetbrains.kotlin:"
                        + "kotlin-gradle-plugin:1.9.22\"\n"));
        // The qualifier is dropped: the caller parses each segment as an
        // int, so leaving it on would throw on a version that is fine.
        assertEquals("2.0.0",
                HealthManifestFragments.declaredKotlinPluginVersion(
                        "classpath 'org.jetbrains.kotlin:"
                        + "kotlin-gradle-plugin:2.0.0-Beta1'"));
        // No declaration at all, so nothing to enforce against.
        assertNull(HealthManifestFragments.declaredKotlinPluginVersion(
                "classpath 'com.google.gms:google-services:4.3.15'"));
        assertNull(HealthManifestFragments.declaredKotlinPluginVersion(""));
        assertNull(HealthManifestFragments.declaredKotlinPluginVersion(null));
    }

    /**
     * {@code requestAuthorization} counts as both directions.
     *
     * <p>Its {@code HealthAccess} list is built at runtime and is opaque
     * to bytecode scanning -- the iOS builder already demands both purpose
     * strings for exactly that reason. Matching neither classifier let an
     * app that asks for write access while declaring only
     * {@code android.health.read} pass the build and ship a manifest
     * without the permission it had just requested.</p>
     */
    @Test
    void requestAuthorizationCountsAsBothDirections() {
        assertTrue(HealthManifestFragments.isReadCall(
                "requestAuthorization"));
        assertTrue(HealthManifestFragments.isWriteCall(
                "requestAuthorization"));
    }
}
