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

import com.codename1.build.shared.PlatformFeatureCatalog;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The two platform scanners classify smart-home usage the same way.
 *
 * <p>They answer different questions -- one picks a Play services dependency
 * and a queries entry, the other HomeKit, an entitlement and an Xcode target
 * -- but they read the same bytecode and must agree on <em>what the app is
 * doing</em>. The health scanners drifted apart three times on their branch,
 * each time because a rule was fixed on one side and never mirrored. This
 * fails the build instead.</p>
 *
 * <p>It matches on source text, which is crude; the rules live in anonymous
 * visitor callbacks with no seam to call. Crude and load-bearing beats
 * absent -- the same trade {@link HealthScannerParityTest} makes.</p>
 */
public class SmartHomeScannerParityTest {

    private static String source(String simpleName) throws Exception {
        File f = new File("src/main/java/com/codename1/builders/"
                + simpleName + ".java");
        assertTrue(f.exists(), "scanner source must be readable: "
                + f.getAbsolutePath());
        return new String(Files.readAllBytes(f.toPath()),
                StandardCharsets.UTF_8);
    }

    /**
     * Both gate the whole feature on the same package prefix. An app that
     * uses smart home has to be recognized as such on both platforms, or one
     * of them silently ships without the native support and the API reports
     * itself unsupported on a device that supports it.
     */
    @Test
    public void bothScannersGateOnTheSamePackage() throws Exception {
        String ios = source("IPhoneBuilder");
        String android = source("AndroidGradleBuilder");
        assertTrue(ios.contains("com/codename1/home/"),
                "the iOS scanner must recognize com.codename1.home");
        assertTrue(android.contains("com/codename1/home/"),
                "the Android scanner must recognize com.codename1.home");
    }

    /**
     * Commissioning is tracked separately on both sides, because it costs an
     * entire generated Xcode target on iOS and a Play services dependency on
     * Android. If one side stopped distinguishing it, an app that only reads
     * its lights would start carrying the expensive half.
     */
    @Test
    public void bothScannersSeparateCommissioning() throws Exception {
        String ios = source("IPhoneBuilder");
        String android = source("AndroidGradleBuilder");
        assertTrue(ios.contains("com/codename1/home/commissioning/"),
                "the iOS scanner must separate the commissioning package");
        assertTrue(android.contains("com/codename1/home/commissioning/"),
                "the Android scanner must separate the commissioning package");
    }

    /**
     * The entitlement gate is iOS-only, and it must read the shared
     * classifier rather than a private copy.
     *
     * <p>Android has no entitlement to earn, so it deliberately does not make
     * this distinction -- but the classifier lives in a shared helper so the
     * iOS side cannot drift into an inline list that nobody can test.</p>
     */
    @Test
    public void theEntitlementGateUsesTheSharedClassifier() throws Exception {
        String ios = source("IPhoneBuilder");
        assertTrue(ios.contains(
                "SmartHomeManifestFragments.isAccessoryDataCall"),
                "the HomeKit entitlement decision must go through the shared,"
                        + " tested classifier rather than an inline list");
    }

    /**
     * The entitlement is only injected for an app that touches the home.
     *
     * <p>This is the one that costs a real build if it regresses: the HomeKit
     * entitlement has to be granted on the App ID, so an app that merely
     * asked whether smart home exists would fail codesigning for a capability
     * it never wanted -- and the failure surfaces as an opaque codesign error
     * minutes into a cloud build.</p>
     */
    @Test
    public void theEntitlementIsGatedOnAccessoryDataNotOnMereUsage()
            throws Exception {
        String ios = source("IPhoneBuilder");
        int inject = ios.indexOf(
                "ios.entitlements.com.apple.developer.homekit\",\n"
                + "                        \"true\"");
        assertTrue(inject > 0,
                "the HomeKit entitlement injection must be present");
        String before = ios.substring(Math.max(0, inject - 400), inject);
        assertTrue(before.contains("usesHomeAccessoryData"),
                "the injection must be guarded by usesHomeAccessoryData, not"
                        + " by usesSmartHome: " + before);
    }

    /**
     * A build that never mentions smart home must be unchanged. Asserted
     * through the catalog, which is the one place a dependency could leak in
     * unconditionally.
     */
    @Test
    public void nothingIsAddedToABuildThatNeverMentionsSmartHome() {
        PlatformFeatureCatalog.Accumulator empty =
                new PlatformFeatureCatalog.Accumulator();
        empty.consume("com/example/MyApplication");
        empty.consume("com/codename1/ui/Form");
        assertTrue(empty.hits().isEmpty(),
                "an app that never referenced a catalogued package must match"
                        + " nothing: " + empty.hits());
        assertFalse(empty.iosFrameworks().contains("HomeKit"));
    }

    /**
     * The catalog names no iOS framework for smart home, deliberately.
     *
     * <p>A framework named there is linked for real -- IPhoneBuilder appends
     * every matched entry's list to ios.add_libs -- and the catalog matches
     * on a prefix, which cannot see the one thing that decides whether
     * MatterSupport belongs in a build: the ios.home.commissioning=false
     * opt-out. The builder links both frameworks under gates that can, so a
     * second copy of the decision here could only ever disagree with it.</p>
     */
    @Test
    public void theCatalogLeavesIosFrameworkLinkageToTheBuilder() {
        PlatformFeatureCatalog.Accumulator reader =
                new PlatformFeatureCatalog.Accumulator();
        reader.consume("com/codename1/home/SmartHome");
        assertTrue(reader.iosFrameworks().isEmpty(),
                reader.iosFrameworks().toString());

        PlatformFeatureCatalog.Accumulator commissioner =
                new PlatformFeatureCatalog.Accumulator();
        commissioner.consume("com/codename1/home/commissioning/Commissioner");
        assertTrue(commissioner.iosFrameworks().isEmpty(),
                commissioner.iosFrameworks().toString());
        assertFalse(commissioner.hits().isEmpty(),
                "the entry itself must still match, so the deployment-target"
                        + " floor survives: " + commissioner.hits());
    }

    /**
     * The gate that does the linking, checked where it lives. HomeKit hangs
     * off the plain smart-home scan; MatterSupport hangs off
     * matterExtensionEnabled, which is what honours the opt-out.
     */
    @Test
    public void theBuilderLinksEachFrameworkUnderItsOwnGate()
            throws Exception {
        String src = source("IPhoneBuilder");
        assertGatedBy(src, "String hk = \"HomeKit.framework\"",
                "if (usesSmartHome)");
        assertGatedBy(src, "String ms = \"MatterSupport.framework\"",
                "if (matterExtensionEnabled)");
    }

    private static void assertGatedBy(String src, String needle, String gate) {
        int at = src.indexOf(needle);
        assertTrue(at > 0, "expected the linkage " + needle);
        // Wide enough to clear the comment that explains each gate. The
        // window is a heuristic either way; what it must not do is pass
        // because the gate happened to be near.
        String before = src.substring(Math.max(0, at - 1200), at);
        assertTrue(before.contains(gate),
                needle + " must be linked under " + gate + ", got: " + before);
    }


    /**
     * Play services home needs API 21, and letting Gradle's manifest merger
     * discover that produces an error naming a transitive dependency the
     * developer never wrote down.
     */
    @Test
    public void theCatalogRaisesTheAndroidFloor() {
        PlatformFeatureCatalog.Accumulator acc =
                new PlatformFeatureCatalog.Accumulator();
        acc.consume("com/codename1/home/SmartHome");
        assertTrue(acc.minimumAndroidSdk() >= 21,
                "expected the Play services home floor, got "
                        + acc.minimumAndroidSdk());
    }

    /**
     * No placeholder privacy string, for the same reason the health entry
     * injects none: Apple reviews this text against what the app does, iOS
     * terminates an app that reaches HomeKit without one, and a generic
     * string would fail review while not even keeping the app alive.
     */
    @Test
    public void theCatalogInjectsNoHomeKitPurposeString() {
        for (PlatformFeatureCatalog.Entry entry
                : PlatformFeatureCatalog.matchesFor(
                        "com/codename1/home/SmartHome")) {
            for (String[] plist : entry.iosPlistEntries()) {
                assertFalse("NSHomeKitUsageDescription".equals(plist[0]),
                        "the catalog must not default the HomeKit purpose"
                                + " string; IPhoneBuilder fails the build with"
                                + " an actionable message instead");
            }
        }
    }
}
