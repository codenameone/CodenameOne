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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Builds the AndroidManifest fragments injected when an app uses
 * {@code com.codename1.health} on Android -- Health Connect permissions,
 * the provider {@code <queries>} entry, and the permissions-rationale
 * activity that Health Connect refuses to show its consent dialog without.
 *
 * <p>Extracted into a pure static helper so the per-type permission mapping
 * is unit-testable and so the BuildDaemon copy stays trivially diffable --
 * <b>keep this file in sync with
 * {@code com.codename1.build.daemon.HealthManifestFragments}</b>.
 * {@link #FRAGMENT_VERSION} is logged during the build so a stale daemon is
 * visible in the log rather than silently producing an APK with no health
 * permissions.</p>
 *
 * <p><b>Why the permissions come from build hints rather than the bytecode
 * scanner.</b> Health Connect permissions are per data type, and the type
 * an app uses is expressed as a reference to a {@code HealthDataType}
 * constant -- that is, a field read. The scanner's {@code visitFieldInsn}
 * is an empty override, so field reads are invisible to it; only type and
 * method references are recorded. The set therefore cannot be inferred and
 * is declared through {@code android.health.read} and
 * {@code android.health.write}. That also matches Google Play policy, which
 * requires declaring exactly the types you use rather than requesting a
 * superset.</p>
 *
 * <p><b>Duplicate suppression uses quote-delimited tokens</b>, and the
 * hazard is worse here than for Bluetooth: {@code READ_HEART_RATE} is a
 * strict prefix of {@code READ_HEART_RATE_VARIABILITY},
 * {@code READ_EXERCISE} of {@code READ_EXERCISE_ROUTE}, and
 * {@code READ_HEALTH_DATA} of {@code READ_HEALTH_DATA_IN_BACKGROUND}. A
 * plain {@code contains()} would silently drop permissions an app
 * declared.</p>
 */
final class HealthManifestFragments {

    /**
     * Bumped whenever the emitted fragments change. Logged by the Android
     * builder so a BuildDaemon running an older copy of this class is
     * apparent from the build log.
     */
    static final String FRAGMENT_VERSION = "health-1";

    /** The Health Connect provider package. */
    static final String PROVIDER_PACKAGE =
            "com.google.android.apps.healthdata";

    /** Fully qualified name of the rationale activity in the Android port. */
    static final String RATIONALE_ACTIVITY =
            "com.codename1.health.HealthPermissionsRationaleActivity";

    /**
     * Portable data-type token to Health Connect permission suffix.
     *
     * <p>Keys are the {@code HealthDataType.getId()} values from the core
     * framework, which are also the tokens developers write in the
     * {@code android.health.read} / {@code android.health.write} build
     * hints. Types with no Health Connect equivalent are deliberately
     * absent, so declaring one is an error naming the unknown token rather
     * than a silently missing permission.</p>
     *
     * <p><b>Keep in sync with {@code com.codename1.health.HealthDataType}.</b>
     * The builder cannot depend on the core jar, so this table is a
     * duplicate; {@code HealthManifestFragmentsTest} pins the token set
     * against a golden list so a core-side addition that is not mirrored
     * here fails CI.</p>
     */
    private static final Map<String, String> PERMISSION_SUFFIX =
            new LinkedHashMap<String, String>();

    static {
        PERMISSION_SUFFIX.put("steps", "STEPS");
        PERMISSION_SUFFIX.put("distance_walking_running", "DISTANCE");
        PERMISSION_SUFFIX.put("distance_cycling", "DISTANCE");
        PERMISSION_SUFFIX.put("distance_swimming", "DISTANCE");
        PERMISSION_SUFFIX.put("flights_climbed", "FLOORS_CLIMBED");
        PERMISSION_SUFFIX.put("elevation_gained", "ELEVATION_GAINED");
        PERMISSION_SUFFIX.put("active_energy", "ACTIVE_CALORIES_BURNED");
        PERMISSION_SUFFIX.put("basal_energy", "BASAL_METABOLIC_RATE");
        PERMISSION_SUFFIX.put("exercise_time", "EXERCISE");
        PERMISSION_SUFFIX.put("wheelchair_pushes", "WHEELCHAIR_PUSHES");
        PERMISSION_SUFFIX.put("heart_rate", "HEART_RATE");
        PERMISSION_SUFFIX.put("resting_heart_rate", "RESTING_HEART_RATE");
        PERMISSION_SUFFIX.put("walking_heart_rate_average", "HEART_RATE");
        PERMISSION_SUFFIX.put("heart_rate_variability_sdnn",
                "HEART_RATE_VARIABILITY");
        PERMISSION_SUFFIX.put("oxygen_saturation", "OXYGEN_SATURATION");
        PERMISSION_SUFFIX.put("respiratory_rate", "RESPIRATORY_RATE");
        PERMISSION_SUFFIX.put("body_temperature", "BODY_TEMPERATURE");
        PERMISSION_SUFFIX.put("basal_body_temperature",
                "BASAL_BODY_TEMPERATURE");
        PERMISSION_SUFFIX.put("vo2_max", "VO2_MAX");
        PERMISSION_SUFFIX.put("blood_pressure", "BLOOD_PRESSURE");
        PERMISSION_SUFFIX.put("blood_glucose", "BLOOD_GLUCOSE");
        PERMISSION_SUFFIX.put("body_mass", "WEIGHT");
        PERMISSION_SUFFIX.put("lean_body_mass", "LEAN_BODY_MASS");
        PERMISSION_SUFFIX.put("bone_mass", "BONE_MASS");
        PERMISSION_SUFFIX.put("body_fat_percentage", "BODY_FAT");
        PERMISSION_SUFFIX.put("body_mass_index", "BODY_WATER_MASS");
        PERMISSION_SUFFIX.put("height", "HEIGHT");
        PERMISSION_SUFFIX.put("waist_circumference", "BODY_MEASUREMENTS");
        PERMISSION_SUFFIX.put("power", "POWER");
        PERMISSION_SUFFIX.put("speed", "SPEED");
        PERMISSION_SUFFIX.put("cycling_cadence", "CYCLING_PEDALING_CADENCE");
        PERMISSION_SUFFIX.put("running_cadence", "STEPS_CADENCE");
        PERMISSION_SUFFIX.put("hydration", "HYDRATION");
        PERMISSION_SUFFIX.put("dietary_energy", "NUTRITION");
        PERMISSION_SUFFIX.put("nutrition", "NUTRITION");
        PERMISSION_SUFFIX.put("sleep", "SLEEP");
        PERMISSION_SUFFIX.put("workout", "EXERCISE");
        PERMISSION_SUFFIX.put("mindful_session", "MINDFULNESS");
        PERMISSION_SUFFIX.put("menstruation_flow", "MENSTRUATION");
        PERMISSION_SUFFIX.put("intermenstrual_bleeding",
                "INTERMENSTRUAL_BLEEDING");
    }

    private HealthManifestFragments() {
    }

    /** Every data-type token this builder understands. */
    static Set<String> knownTokens() {
        return Collections.unmodifiableSet(
                new TreeSet<String>(PERMISSION_SUFFIX.keySet()));
    }

    /**
     * Splits a comma-separated build-hint value into tokens, trimming and
     * dropping blanks. Tolerates whitespace and trailing commas, since the
     * value is hand-written in a properties file.
     */
    static List<String> parseTypeList(String hintValue) {
        List<String> out = new ArrayList<String>();
        if (hintValue == null) {
            return out;
        }
        String[] parts = hintValue.split(",");
        for (int i = 0; i < parts.length; i++) {
            String t = parts[i].trim();
            if (t.length() > 0 && !out.contains(t)) {
                out.add(t);
            }
        }
        return out;
    }

    /**
     * Returns the tokens in {@code tokens} that this builder does not
     * recognise, so the caller can fail with a message naming them.
     */
    static List<String> unknownTokens(List<String> tokens) {
        List<String> out = new ArrayList<String>();
        for (int i = 0; i < tokens.size(); i++) {
            if (!PERMISSION_SUFFIX.containsKey(tokens.get(i))) {
                out.add(tokens.get(i));
            }
        }
        return out;
    }

    /**
     * Name of the generated string resource carrying the privacy policy
     * URL.
     *
     * <p>Shared with {@code HealthPermissionsRationaleActivity}, which
     * resolves it by name at runtime. A rename on one side alone leaves the
     * rationale screen blank, so the name lives here rather than being
     * spelled out in two places.</p>
     */
    static final String POLICY_URL_RESOURCE = "cn1_health_privacy_policy";

    /**
     * The Health Connect permission suffix for one token, or {@code null}
     * when the token is unknown. Exposed so the Kotlin bridge's own copy of
     * this table can be pinned against it -- see
     * {@code HealthBridgeTokenTableTest}.
     */
    static String permissionSuffix(String token) {
        return PERMISSION_SUFFIX.get(token);
    }

    /**
     * The full Health Connect permission name for one token, or
     * {@code null} when the token is unknown.
     */
    static String permissionFor(String token, boolean write) {
        String suffix = PERMISSION_SUFFIX.get(token);
        if (suffix == null) {
            return null;
        }
        return "android.permission.health." + (write ? "WRITE_" : "READ_")
                + suffix;
    }

    /**
     * Returns {@code xPermissions} with the health permissions appended.
     *
     * <p>Several tokens map to one Health Connect permission -- the three
     * distance types all map to {@code READ_DISTANCE} -- so the emitted set
     * is deduplicated. Permissions the developer already declared through
     * {@code android.xpermissions} are left alone.</p>
     *
     * @param xPermissions      current accumulated manifest fragment
     * @param readTokens        {@code android.health.read} tokens
     * @param writeTokens       {@code android.health.write} tokens
     * @param backgroundRead    {@code android.health.background} hint
     * @param history           {@code android.health.history} hint
     * @param workoutSessions   workout API usage detected
     * @param targetSdkVersion  the app's target SDK
     */
    static String injectPermissions(String xPermissions,
            List<String> readTokens, List<String> writeTokens,
            boolean backgroundRead, boolean history, boolean workoutSessions,
            int targetSdkVersion) {
        StringBuilder sb = new StringBuilder(
                xPermissions == null ? "" : xPermissions);
        Set<String> emitted = new TreeSet<String>();
        for (int i = 0; i < readTokens.size(); i++) {
            addPermission(sb, emitted, permissionFor(readTokens.get(i),
                    false));
        }
        for (int i = 0; i < writeTokens.size(); i++) {
            addPermission(sb, emitted, permissionFor(writeTokens.get(i),
                    true));
        }
        if (backgroundRead) {
            addPermission(sb, emitted,
                    "android.permission.health.READ_HEALTH_DATA_IN_BACKGROUND");
        }
        if (history) {
            addPermission(sb, emitted,
                    "android.permission.health.READ_HEALTH_DATA_HISTORY");
        }
        if (workoutSessions) {
            // Recording an exercise session on a phone means staying alive
            // in the foreground service; ACTIVITY_RECOGNITION additionally
            // gates step and exercise detection from API 29.
            addPermission(sb, emitted,
                    "android.permission.ACTIVITY_RECOGNITION");
            addPermission(sb, emitted,
                    "android.permission.FOREGROUND_SERVICE");
            if (targetSdkVersion >= 34) {
                addPermission(sb, emitted,
                        "android.permission.FOREGROUND_SERVICE_HEALTH");
            }
        }
        return sb.toString();
    }

    private static void addPermission(StringBuilder sb, Set<String> emitted,
            String name) {
        if (name == null || emitted.contains(name)) {
            return;
        }
        emitted.add(name);
        // Quote-delimited so that READ_HEART_RATE does not suppress
        // READ_HEART_RATE_VARIABILITY -- see the class documentation.
        if (sb.indexOf("\"" + name + "\"") >= 0) {
            return;
        }
        sb.append("    <uses-permission android:name=\"").append(name)
                .append("\" />\n");
    }

    /**
     * Returns {@code xQueries} with the Health Connect provider declared.
     *
     * <p>Without this the provider is invisible to package visibility on
     * API 30+, and every Health Connect call fails as though the app were
     * not installed. The Android builder only emits {@code <queries>} at
     * {@code targetSdkVersion >= 30}, which is why it asserts that
     * separately.</p>
     */
    static String injectQueries(String xQueries) {
        String entry = "        <package android:name=\"" + PROVIDER_PACKAGE
                + "\" />\n";
        if (xQueries != null && xQueries.contains(PROVIDER_PACKAGE)) {
            return xQueries;
        }
        return (xQueries == null ? "" : xQueries) + entry;
    }

    /**
     * Returns {@code xApplication} with the health application entries
     * appended: the permissions-rationale activity, the API 34+
     * view-permission-usage alias, and -- when workouts are used -- the
     * foreground service that keeps a recording session alive.
     *
     * <p>The rationale activity is not optional. Health Connect will not
     * present its consent dialog to an app that does not declare one, so
     * omitting it produces a permission request that silently never
     * appears.</p>
     */
    static String injectApplicationEntries(String xApplication,
            boolean workoutSessions, int targetSdkVersion) {
        StringBuilder sb = new StringBuilder(
                xApplication == null ? "" : xApplication);
        if (sb.indexOf(RATIONALE_ACTIVITY) < 0) {
            sb.append("        <activity android:name=\"")
                    .append(RATIONALE_ACTIVITY)
                    .append("\"\n")
                    .append("                  android:exported=\"true\"\n")
                    .append("                  android:theme=")
                    .append("\"@android:style/Theme.NoDisplay\">\n")
                    .append("            <intent-filter>\n")
                    .append("                <action android:name=")
                    .append("\"androidx.health.ACTION_SHOW_PERMISSIONS")
                    .append("_RATIONALE\" />\n")
                    .append("            </intent-filter>\n")
                    .append("        </activity>\n");
            if (targetSdkVersion >= 34) {
                // API 34 routes the rationale through the platform
                // permission-usage intent instead of the AndroidX action.
                sb.append("        <activity-alias android:name=")
                        .append("\"ViewPermissionUsageActivity\"\n")
                        .append("                        android:exported=")
                        .append("\"true\"\n")
                        .append("                        ")
                        .append("android:targetActivity=\"")
                        .append(RATIONALE_ACTIVITY).append("\"\n")
                        .append("                        ")
                        .append("android:permission=")
                        .append("\"android.permission.START_VIEW_PERMISSION")
                        .append("_USAGE\">\n")
                        .append("            <intent-filter>\n")
                        .append("                <action android:name=")
                        .append("\"android.intent.action.VIEW_PERMISSION")
                        .append("_USAGE\" />\n")
                        .append("                <category android:name=")
                        .append("\"android.intent.category.HEALTH")
                        .append("_PERMISSIONS\" />\n")
                        .append("            </intent-filter>\n")
                        .append("        </activity-alias>\n");
            }
        }
        if (workoutSessions
                && sb.indexOf("com.codename1.health.HealthWorkoutService") < 0) {
            sb.append("        <service android:name=")
                    .append("\"com.codename1.health.HealthWorkoutService\"\n")
                    .append("                 android:exported=\"false\"\n")
                    .append("                 android:foregroundServiceType=")
                    .append("\"health\" />\n");
        }
        return sb.toString();
    }

    /**
     * ProGuard keep rules for the health classes that are reached by name
     * or by the platform rather than by ordinary Java references.
     *
     * <p>The generated background-listener factory constructs listeners
     * with a direct {@code new}, so the listener classes themselves survive
     * shrinking; the keep rules exist so that their <i>names</i> stay
     * stable under obfuscation, since a subscription persists the name it
     * was registered under and must still match after an app update.</p>
     */
    static String proguardKeepRules(List<String> listenerClassNames) {
        StringBuilder sb = new StringBuilder();
        sb.append("-keep class ").append(RATIONALE_ACTIVITY)
                .append(" { *; }\n");
        sb.append("-keep class com.codename1.health.CN1HealthConnectBridge")
                .append(" { *; }\n");
        sb.append("-keep interface ")
                .append("com.codename1.impl.android.HealthConnectDelegate")
                .append(" { *; }\n");
        List<String> sorted = new ArrayList<String>(listenerClassNames);
        Collections.sort(sorted);
        for (int i = 0; i < sorted.size(); i++) {
            sb.append("-keepnames class ").append(sorted.get(i))
                    .append("\n");
        }
        return sb.toString();
    }

    /** Convenience for callers holding an array. */
    static List<String> asList(String... values) {
        return new ArrayList<String>(Arrays.asList(values));
    }
}
