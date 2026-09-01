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

/**
 * Which Play Billing Library version an Android build uses, and what that
 * choice requires of the rest of the build.
 *
 * <p><b>Why there is a floor at all.</b> {@code BillingSupport} in the Android
 * port is written against the ProductDetails API. The SKU API it replaced --
 * {@code SkuDetails}, {@code querySkuDetailsAsync},
 * {@code BillingClient.SkuType}, {@code BillingFlowParams.setSkuDetails} and
 * the no-argument {@code enablePendingPurchases()} -- was removed across
 * billing 8 and 9, so one implementation cannot serve both eras. Compiling
 * the port's source against each published release puts the boundary exactly
 * at 8.0.0:</p>
 *
 * <pre>
 * billing 4.0.0 .. 7.1.1   ProductDetails source does not compile
 * billing 8.0.0, 9.1.0     compiles clean
 * </pre>
 *
 * <p>Google's own schedule points the same way. Its deprecation FAQ states
 * that from Aug 31 2026 all new apps and updates must use billing 8 or later,
 * with an extension available to Nov 1 2026, so a build below 8 produces an
 * app Play will not accept an update from. Refusing it with a sentence beats
 * emitting two dozen {@code cannot find symbol} errors against a file the
 * developer never wrote.</p>
 *
 * <p><b>Why minSdk moves with it.</b> The billing AAR declares its own
 * {@code minSdkVersion}, and the manifest merger fails outright when the app
 * declares a lower one. Read from the published AAR manifests:</p>
 *
 * <pre>
 * 4.0.0   14
 * 8.0.0   21
 * 8.1.0, 8.2.1, 8.3.0, 9.0.0, 9.1.0   23
 * </pre>
 *
 * <p>So the floor is not "the 8 line needs 21" -- 8.0.0 alone needs 21 and
 * every release after it needs 23. The build raises {@code minSdk} to match
 * rather than letting the merge fail, which is what the Android Auto
 * dependency in this builder already does for the same reason. A version
 * newer than anything listed here is assumed to need the highest floor known;
 * if a future release raises it again the merger still says so by number,
 * which is a readable failure rather than a silent one.</p>
 *
 * <p>Kept as a pure static helper so the policy is unit-testable without a
 * device build, and so the BuildDaemon copy stays trivially diffable --
 * <b>keep this file in sync with its twin in the other repository</b>.</p>
 */
public class PlayBillingVersions {

    /**
     * The version used when the app names none.
     *
     * <p>8.0.0 rather than the newest published release, deliberately: it is
     * the lowest version Play still accepts, and the only one at or above that
     * line whose AAR is content with {@code minSdkVersion} 21. Defaulting any
     * higher would push every in-app-purchase app to 23 without being asked.
     * An app that wants a newer one sets {@code android.billingclient.version}
     * and gets the matching floor automatically.</p>
     */
    public static final String DEFAULT_VERSION = "8.0.0";

    /** The first version whose API the port's BillingSupport is written for. */
    public static final String MINIMUM_SUPPORTED = "8.0.0";

    private PlayBillingVersions() {
    }

    /**
     * The reason this version cannot be built, or null when it can.
     *
     * <p>An unreadable version is accepted rather than refused: a project may
     * legitimately hold one this cannot parse, and refusing on a parse failure
     * would break a build over the shape of a string rather than over anything
     * that is wrong with it. The compiler is the backstop there.</p>
     */
    public static String refusalFor(String billingClientVersion) {
        String numeric = numeric(billingClientVersion);
        if (numeric == null) {
            return null;
        }
        if (compareVersions(numeric, MINIMUM_SUPPORTED) >= 0) {
            return null;
        }
        return "android.billingclient.version is set to " + billingClientVersion.trim()
                + ", and Codename One's in-app purchase support needs "
                + MINIMUM_SUPPORTED + " or newer. Play Billing removed the SkuDetails "
                + "API the older releases used, so the two cannot be built from one "
                + "source, and Google stopped accepting new apps and updates below "
                + "billing 8 on Aug 31 2026. Remove the build hint to take the "
                + "default of " + DEFAULT_VERSION + ", or set a newer version.";
    }

    /**
     * The {@code minSdkVersion} this billing version's AAR requires, as a
     * string so it drops straight into the builder's existing minSdk
     * arithmetic.
     */
    public static String minimumSdk(String billingClientVersion) {
        String numeric = numeric(billingClientVersion);
        if (numeric == null) {
            // Unknown shape: assume the highest floor that has been seen rather than
            // the lowest. Guessing low turns into a manifest merge failure; guessing
            // high only narrows the device range of an app that is already on a
            // billing release requiring API 23.
            return "23";
        }
        if (compareVersions(numeric, "8.1.0") >= 0) {
            return "23";
        }
        if (compareVersions(numeric, "8.0.0") >= 0) {
            return "21";
        }
        // Below the supported floor the build is refused before this matters; the
        // old AAR's own value is returned so nothing is raised on the way to that
        // refusal.
        return "14";
    }

    private static String numeric(String version) {
        if (version == null) {
            return null;
        }
        return HealthManifestFragments.numericVersionPrefix(version.trim());
    }

    /** Numeric dotted version compare; a missing segment counts as zero. */
    private static int compareVersions(String left, String right) {
        String[] l = left.split("\\.");
        String[] r = right.split("\\.");
        int len = Math.max(l.length, r.length);
        for (int i = 0; i < len; i++) {
            int a = i < l.length ? parse(l[i]) : 0;
            int b = i < r.length ? parse(r[i]) : 0;
            if (a != b) {
                return a < b ? -1 : 1;
            }
        }
        return 0;
    }

    private static int parse(String segment) {
        try {
            return Integer.parseInt(segment);
        } catch (NumberFormatException notANumber) {
            return 0;
        }
    }
}
