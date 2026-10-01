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

import static org.junit.jupiter.api.Assertions.assertEquals;
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
     * The purpose string that ships is the one the renderer will use.
     *
     * <p>{@code ios.plistInject} wins in the Info.plist renderer -- a generated value is emitted
     * only for a key the fragment does not declare -- so validating the direct hint approved a
     * disclosure the plist then dropped. An app with a perfectly good
     * {@code ios.NSHomeKitUsageDescription} shipped the fragment's {@code <false/>} and was
     * terminated the moment it touched HomeKit.</p>
     */
    @Test
    public void thePurposeStringValidatedIsTheOneThatShips() {
        BuildRequest hintOnly = new BuildRequest();
        hintOnly.putArgument("ios.NSHomeKitUsageDescription", "control your lights");
        assertEquals("control your lights",
                IPhoneBuilder.effectivePurposeString(hintOnly, "ios.NSHomeKitUsageDescription"),
                "with no fragment the hint is what ships");

        BuildRequest overridden = new BuildRequest();
        overridden.putArgument("ios.NSHomeKitUsageDescription", "control your lights");
        overridden.putArgument("ios.plistInject",
                "<key>NSHomeKitUsageDescription</key><false/>");
        assertEquals("false",
                IPhoneBuilder.effectivePurposeString(overridden, "ios.NSHomeKitUsageDescription"),
                "the fragment wins in the renderer, so it has to win here");

        BuildRequest injected = new BuildRequest();
        injected.putArgument("ios.plistInject",
                "<key>NSHomeKitUsageDescription</key><string>see your home</string>");
        assertEquals("see your home",
                IPhoneBuilder.effectivePurposeString(injected, "ios.NSHomeKitUsageDescription"),
                "and a fragment that declares a real string is a real disclosure");

        BuildRequest blank = new BuildRequest();
        blank.putArgument("ios.plistInject",
                "<key>NSHomeKitUsageDescription</key><string>   </string>");
        assertEquals("false",
                IPhoneBuilder.effectivePurposeString(blank, "ios.NSHomeKitUsageDescription"),
                "an empty purpose string is no string at all to iOS");
    }

    /**
     * An add-device request that expires without a screen frees the bridge.
     *
     * <p>A CommissioningRequest timeout fails the waiting caller in the framework, which knows
     * nothing about the Android delegate's parked callback. When Play services answers neither
     * listener -- no IntentSender, no failure -- nothing else ever cleared it, so every later
     * commission() was refused with BUSY for the life of the process while no screen was in front
     * of the user. The reclaim is deliberately limited to a request that never launched: once the
     * sheet is up the answer is the user's to give, and it arrives through the activity result.</p>
     */
    @Test
    public void anExpiredUnlaunchedRequestStopsBlockingTheBridge() throws Exception {
        File f = new File("src/main/resources/com/codename1/builders/home/"
                + "MatterCommissioningBridge.javas");
        assertTrue(f.exists(), "the injected bridge must be readable: " + f.getAbsolutePath());
        String bridge = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);

        assertTrue(bridge.contains("if (pendingCommission != null && expiredWithNoScreen()) {"),
                "the expiry has to be reclaimed BEFORE the BUSY refusal, or it never runs");
        assertTrue(bridge.contains("commissionDeadline > 0 && !commissionLaunched"),
                "a request with no limit never expires, and a launched one is the user's");
        assertTrue(bridge.contains("commissionLaunched = true;"),
                "the flag has to be set once the screen is really up");
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
     * Neither scanner treats the setup-payload parser as using smart home.
     *
     * <p>SetupPayload parses the string on an accessory's sticker and
     * checksums it, in pure Java. An app that scans a code and says "that is
     * not a Matter code" before deciding whether to commission anything is
     * doing only that -- and on the strength of the package prefix alone it
     * would be made to declare a HomeKit purpose string, carry two restricted
     * entitlements, own an app group and ship a generated extension on iOS,
     * and take a Play Services AAR plus Bluetooth permissions on Android. The
     * build fails for want of the purpose string, or codesigning fails for
     * want of the entitlement on the App ID.</p>
     */
    @Test
    public void neitherScannerCountsTheSetupPayloadParser() throws Exception {
        String ios = source("IPhoneBuilder");
        String android = source("AndroidGradleBuilder");
        for (String src : new String[] {ios, android}) {
            assertTrue(src.contains("isSmartHomeSetupPayload"),
                    "each scanner must exempt the pure-Java payload parser");
            assertTrue(src.contains(
                    "\"com/codename1/home/commissioning/SetupPayload\".equals(cls)"),
                    "the exemption must name the parser exactly");
            assertTrue(src.contains(
                    "&& !isSmartHomeSetupPayload(cls)"),
                    "the exemption must guard the package check itself, so it"
                            + " reaches usesHomeCommissioning too");
        }
    }

    /**
     * The catalog names no Android dependency for smart home either.
     *
     * <p>Same startsWith problem as the iOS frameworks: the prefix covers the
     * pure-Java payload parser, so play-services-home named here would land
     * in an app that only validates a scanned code. AndroidGradleBuilder adds
     * it inside the usesSmartHome gate, beside the delegate that imports
     * it.</p>
     */
    @Test
    public void theCatalogLeavesTheAndroidDependencyToTheBuilder()
            throws Exception {
        PlatformFeatureCatalog.Accumulator reader =
                new PlatformFeatureCatalog.Accumulator();
        reader.consume("com/codename1/home/SmartHome");
        assertFalse(reader.hits().isEmpty(), "the entry must still match");
        for (PlatformFeatureCatalog.Entry hit : reader.hits()) {
            assertTrue(hit.androidGradleDeps().isEmpty(),
                    "the smart home entry must name no Android dependency: "
                            + hit.androidGradleDeps());
        }
        assertEquals(0, reader.minimumAndroidSdk(),
                "and no Android floor either -- the builder raises it inside"
                        + " the gate that knows the parser does not need it");
        String android = source("AndroidGradleBuilder");
        assertTrue(android.contains("play-services-home"),
                "the builder must be the one that adds the dependency");
    }

    /**
     * Asking whether commissioning is possible is not commissioning.
     *
     * <p>An app that shows an "add accessory" button only where the platform
     * has one calls Commissioner.isSupported() or getStyle(). Both are class
     * references in the commissioning package, and on the package prefix
     * alone they bought MatterSupport, the restricted setup-payload
     * entitlement, an app group, a generated extension target and a raised
     * deployment floor -- for a question whose answer may well be "no".</p>
     */
    @Test
    public void aCapabilityProbeIsNotCommissioning() throws Exception {
        assertTrue(SmartHomeManifestFragments.isCommissioningCapabilityType(
                "com/codename1/home/commissioning/Commissioner"));
        assertTrue(SmartHomeManifestFragments.isCommissioningCapabilityType(
                "com/codename1/home/commissioning/CommissioningStyle"));
        assertFalse(SmartHomeManifestFragments.isCommissioningCapabilityType(
                "com/codename1/home/commissioning/CommissioningRequest"),
                "building a request is intent to commission");

        assertFalse(SmartHomeManifestFragments
                .isCommissioningCall("isSupported"));
        assertFalse(SmartHomeManifestFragments.isCommissioningCall("getStyle"));
        // The documented fallback for a platform with no commissioning of its
        // own: it opens the ecosystem's app and nothing else, which is why
        // the same call on SmartHome is availability-only.
        assertFalse(SmartHomeManifestFragments
                .isCommissioningCall("openEcosystemApp"));
        assertTrue(SmartHomeManifestFragments.isCommissioningCall("commission"),
                "and everything else counts, including a method added later");

        for (String builder : new String[] {"IPhoneBuilder",
                "AndroidGradleBuilder"}) {
            String src = source(builder);
            assertTrue(src.contains("isCommissioningCapabilityType"),
                    builder + " must exempt the capability types");
        }
        assertTrue(source("IPhoneBuilder").contains("isCommissioningCall"),
                "and decide Commissioner by the method, as it does SmartHome");
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
     * The Android delegate is injected after the scan that decides whether to.
     *
     * <p>usesSmartHome is set by a callback the class scan runs. Injected
     * above that call it is always false, so the delegate never lands and the
     * whole Android API reports itself unsupported on a device that supports
     * it -- with nothing in the build log to say why. Ordering is the entire
     * bug, so ordering is what this checks.</p>
     */
    @Test
    public void theAndroidDelegateIsInjectedAfterTheScanThatDecidesIt()
            throws Exception {
        String src = source("AndroidGradleBuilder");
        int scan = src.indexOf("scanClassesForPermissions(dummyClassesDir");
        int inject = src.indexOf("SmartHomeInjector.injectAndroid");
        assertTrue(scan > 0, "the scan call must exist");
        assertTrue(inject > 0, "the injection must exist");
        assertTrue(inject > scan,
                "the injection must follow the scan that sets usesSmartHome;"
                        + " before it the flag is always false");
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
     *
     * <p>Raised by the builder rather than the catalog, and for the same
     * reason the dependency itself moved there: the catalog prefix also
     * covers the pure-Java payload parser, and an app that only validates a
     * scanned code should not have its minimum SDK raised for an AAR it never
     * gets. The floor travels with the dependency, inside the one gate that
     * knows the difference.</p>
     */
    @Test
    public void theBuilderRaisesTheAndroidFloorInsideTheGate()
            throws Exception {
        String android = source("AndroidGradleBuilder");
        assertTrue(android.contains(
                "SmartHomeManifestFragments.MINIMUM_SDK"),
                "the builder must raise the floor from the shared constant");
        assertEquals(21, SmartHomeManifestFragments.MINIMUM_SDK,
                "the floor is what play-services-home needs");
        assertGatedBy(android, "\"com.google.android.gms:play-services-home:\"",
                "if (usesSmartHome)");
        // The keyword itself is chosen where `compile` is known, further
        // down: a legacy project without AndroidX cannot evaluate a
        // build.gradle that says implementation.
        assertTrue(android.contains("\"    \" + compile + \" '\""),
                "the dependency line must use the selected configuration");
        int gate = android.indexOf("if (usesSmartHome) {");
        int dependency = android.indexOf(
                "\"com.google.android.gms:play-services-home:\"");
        int floor = android.indexOf(
                "SmartHomeManifestFragments.MINIMUM_SDK)", gate);
        assertTrue(gate > 0 && dependency > gate && floor > dependency,
                "the dependency and the floor both belong inside the gate,"
                        + " in that order: gate=" + gate + " dependency="
                        + dependency + " floor=" + floor);
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
