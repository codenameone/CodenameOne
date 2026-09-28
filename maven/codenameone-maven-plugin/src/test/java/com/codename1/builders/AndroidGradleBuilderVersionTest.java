/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided by
 * Oracle in the LICENSE file that accompanied this code.
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AndroidGradleBuilderVersionTest {

    @Test
    void comparesMinorVersionsInsteadOfOnlyTheMajorVersion() {
        assertTrue(AndroidGradleBuilder.compareVersions("8.1", "8.13") < 0);
        assertEquals(0, AndroidGradleBuilder.compareVersions("8.13", "8.13"));
        assertTrue(AndroidGradleBuilder.compareVersions("8.13.2", "8.13") > 0);
    }

    @Test
    void gradleVersionHintSelectsGradle9OnlyFromNineUp() throws Exception {
        // Absent, blank or below 9: the default Gradle 8 build, as before the hint was read.
        assertNull(AndroidGradleBuilder.requestedGradle9Version(null));
        assertNull(AndroidGradleBuilder.requestedGradle9Version(""));
        assertNull(AndroidGradleBuilder.requestedGradle9Version("  "));
        assertNull(AndroidGradleBuilder.requestedGradle9Version("8.1"));
        assertNull(AndroidGradleBuilder.requestedGradle9Version("6.5"));
        // The bare major is the measured pairing; an explicit release is honoured as given.
        assertEquals(AndroidGradleBuilder.GRADLE_9_VERSION,
                AndroidGradleBuilder.requestedGradle9Version("9"));
        assertEquals(AndroidGradleBuilder.GRADLE_9_VERSION,
                AndroidGradleBuilder.requestedGradle9Version(" 9 "));
        assertEquals("9.6.0", AndroidGradleBuilder.requestedGradle9Version("9.6.0"));
        // Padded: Gradle 9 publishes every release as major.minor.patch, and 9.6 is the floor
        // itself rather than something below 9.6.0.
        assertEquals("9.9.0", AndroidGradleBuilder.requestedGradle9Version("9.9"));
        assertEquals("9.6.0", AndroidGradleBuilder.requestedGradle9Version("9.6"));
    }

    @Test
    void builtInKotlinKeepsItsCompilerUnlessANewerOneIsAskedFor() {
        // Floors the bundled 2.2.10 already meets -- Health Connect's 1.9.x, legacy cn1lib
        // values -- must not put an older kotlin-gradle-plugin beside AGP 9's.
        assertFalse(AndroidGradleBuilder.kotlinOverridesBuiltIn(""));
        assertFalse(AndroidGradleBuilder.kotlinOverridesBuiltIn(null));
        assertFalse(AndroidGradleBuilder.kotlinOverridesBuiltIn("1.9.22"));
        assertFalse(AndroidGradleBuilder.kotlinOverridesBuiltIn("1.7.22"));
        assertFalse(AndroidGradleBuilder.kotlinOverridesBuiltIn(
                AndroidGradleBuilder.AGP_9_BUILT_IN_KOTLIN_VERSION));
        assertFalse(AndroidGradleBuilder.kotlinOverridesBuiltIn("2.2"));
        // A newer Kotlin is exactly what the classpath entry is for.
        assertTrue(AndroidGradleBuilder.kotlinOverridesBuiltIn("2.3.0"));
        assertTrue(AndroidGradleBuilder.kotlinOverridesBuiltIn("2.2.20"));
        // A qualified release is judged by its numeric part: an old RC is still a legacy
        // floor, a newer beta is still a newer compiler.
        assertFalse(AndroidGradleBuilder.kotlinOverridesBuiltIn("1.9.22-RC2"));
        assertTrue(AndroidGradleBuilder.kotlinOverridesBuiltIn("2.3.0-Beta1"));
        // A Gradle variable lands in a single-quoted coordinate Groovy never interpolates, so
        // it is not an override: the built-in compiler is used.
        assertFalse(AndroidGradleBuilder.kotlinOverridesBuiltIn("$kotlinVersion"));
    }

    @Test
    void agp9UsesSdkKeepsOnlyWhatTheMergerStillAccepts() {
        // AGP 9 fails the merge on all three SDK versions in <uses-sdk>, max included.
        String hint = "tools:overrideLibrary=\"androidx.car.app\" android:minSdkVersion=\"21\""
                + " android:targetSdkVersion='35' android:maxSdkVersion = \"34\"";
        assertEquals("    <uses-sdk tools:overrideLibrary=\"androidx.car.app\" />\n",
                AndroidGradleBuilder.agp9UsesSdk(hint));
        // Nothing left means no element at all.
        assertEquals("", AndroidGradleBuilder.agp9UsesSdk("android:minSdkVersion=\"21\""));
        assertEquals("", AndroidGradleBuilder.agp9UsesSdk(""));
        assertEquals("", AndroidGradleBuilder.agp9UsesSdk(null));
        // maxSdkVersion moves to defaultConfig rather than vanishing.
        assertEquals("34", AndroidGradleBuilder.xmanifestMaxSdkVersion(hint));
        assertNull(AndroidGradleBuilder.xmanifestMaxSdkVersion("tools:overrideLibrary=\"x\""));
    }

    @Test
    void aMaxSdkVersionWithNoNumberToMoveIsRefusedNotDropped() {
        String placeholder = AndroidGradleBuilder.agp9MaxSdkRefusal(
                "tools:overrideLibrary=\"x\" android:maxSdkVersion=\"${maxSdk}\"");
        assertTrue(placeholder != null && placeholder.contains("${maxSdk}"), String.valueOf(placeholder));
        assertTrue(AndroidGradleBuilder.agp9MaxSdkRefusal("android:maxSdkVersion='@integer/max'") != null);
        // A literal moves to defaultConfig; no attribute means nothing to refuse.
        assertNull(AndroidGradleBuilder.agp9MaxSdkRefusal("android:maxSdkVersion = \"34\""));
        assertNull(AndroidGradleBuilder.agp9MaxSdkRefusal("tools:overrideLibrary=\"x\""));
        assertNull(AndroidGradleBuilder.agp9MaxSdkRefusal(null));
    }

    @Test
    void aGoogleServicesPinAgp9CannotApplyIsRefusedByName() {
        String old = AndroidGradleBuilder.agp9GoogleServicesRefusal(
                "\n    classpath 'com.google.gms:google-services:4.3.15'\n");
        assertTrue(old != null && old.contains("4.3.15"), String.valueOf(old));
        assertNull(AndroidGradleBuilder.agp9GoogleServicesRefusal(
                "classpath 'com.google.gms:google-services:4.4.0'"));
        assertNull(AndroidGradleBuilder.agp9GoogleServicesRefusal(
                "classpath 'com.google.gms:google-services:4.5.0'"));
        // Gradle resolves duplicates to the highest, so a cn1lib's old pin beside the
        // project's new one is fine; two old ones are not.
        assertNull(AndroidGradleBuilder.agp9GoogleServicesRefusal(
                "classpath 'com.google.gms:google-services:4.3.15'\n"
                + "classpath 'com.google.gms:google-services:4.5.0'"));
        String twoOld = AndroidGradleBuilder.agp9GoogleServicesRefusal(
                "classpath 'com.google.gms:google-services:4.3.15'\n"
                + "classpath 'com.google.gms:google-services:4.3.10'");
        assertTrue(twoOld != null && twoOld.contains("4.3.15"), String.valueOf(twoOld));
        // Nothing pinned, or nothing readable: nothing to refuse.
        assertNull(AndroidGradleBuilder.agp9GoogleServicesRefusal(""));
        assertNull(AndroidGradleBuilder.agp9GoogleServicesRefusal(null));
        assertNull(AndroidGradleBuilder.agp9GoogleServicesRefusal(
                "classpath \"com.google.gms:google-services:$gsVersion\""));
    }

    @Test
    void gradleVersionHintRefusesWhatTheAndroidGradlePluginCannotRunOn() {
        // Below AGP 9.4.1's own floor: refused before the build instead of inside Gradle.
        BuildException old = assertThrows(BuildException.class,
                () -> AndroidGradleBuilder.requestedGradle9Version("9.1.0"));
        assertTrue(old.getMessage().contains(AndroidGradleBuilder.GRADLE_9_MIN_VERSION),
                old.getMessage());
        BuildException future = assertThrows(BuildException.class,
                () -> AndroidGradleBuilder.requestedGradle9Version("10.0"));
        assertTrue(future.getMessage().contains("not supported"), future.getMessage());
        BuildException garbage = assertThrows(BuildException.class,
                () -> AndroidGradleBuilder.requestedGradle9Version("latest"));
        assertTrue(garbage.getMessage().contains("not a Gradle version"), garbage.getMessage());
    }

    @Test
    void theGradle9FloorIsNotAboveTheDefaultPairing() {
        assertTrue(AndroidGradleBuilder.compareVersions(AndroidGradleBuilder.GRADLE_9_VERSION,
                AndroidGradleBuilder.GRADLE_9_MIN_VERSION) >= 0);
    }

    @Test
    void renameHardeningRejectsEveryValueThatLeavesR8Off() {
        // R8 renames only when android.enableProguard is exactly "true"; every other value leaves it
        // off. A rename profile must be rejected for all of them, not only the literal "false".
        for (String off : new String[] {"false", "off", "0", "no", "False", "OFF", "", "yes"}) {
            assertTrue(AndroidGradleBuilder.r8RenameRequiredButDisabled(true, off),
                    "rename requested + enableProguard=" + off + " must be rejected");
        }
        // Exactly "true" enables R8, so a rename profile is fine.
        assertFalse(AndroidGradleBuilder.r8RenameRequiredButDisabled(true, "true"));
        // When rename is not requested, R8 being off is irrelevant.
        assertFalse(AndroidGradleBuilder.r8RenameRequiredButDisabled(false, "off"));
        assertFalse(AndroidGradleBuilder.r8RenameRequiredButDisabled(false, "true"));
    }

    @Test
    void renameHardeningNeedsAReleaseVariantNotJustEnableProguard() {
        // R8 minifyEnabled lives in the release buildType, so a debug-only build never renames even
        // with the default android.enableProguard=true.
        BuildRequest debugOnly = new BuildRequest();
        debugOnly.setCertificate(new byte[] {1, 2, 3});
        debugOnly.putArgument("android.release", "false");
        debugOnly.putArgument("android.debug", "true");
        assertFalse(AndroidGradleBuilder.androidReleaseVariantBuilt(debugOnly),
                "android.release=false + debug builds only assembleDebug");

        // A default (release) build with a certificate does produce a release variant.
        BuildRequest release = new BuildRequest();
        release.setCertificate(new byte[] {1, 2, 3});
        assertTrue(AndroidGradleBuilder.androidReleaseVariantBuilt(release));

        // Neither explicitly selected falls back to building both (release included).
        BuildRequest both = new BuildRequest();
        both.setCertificate(new byte[] {1, 2, 3});
        both.putArgument("android.release", "false");
        both.putArgument("android.debug", "false");
        assertTrue(AndroidGradleBuilder.androidReleaseVariantBuilt(both));

        // No signing certificate means only assembleDebug runs, so no release variant.
        BuildRequest noCert = new BuildRequest();
        noCert.putArgument("android.release", "true");
        assertFalse(AndroidGradleBuilder.androidReleaseVariantBuilt(noCert));
    }

    @Test
    void forcedOffLocalBuildDoesNotRequireR8() {
        // harden.allowUnhardenedLocalBuild takes the escape hatch: hardenSourceJar returns the original
        // jar stamped cn1.hardened=false, so the R8-rename enforcement must NOT fire even though the level
        // still reads aggressive -- otherwise a local build with R8 off or no release cert is rejected
        // despite opting out of hardening.
        BuildRequest forcedOff = new BuildRequest();
        forcedOff.putArgument("harden.level", "aggressive");
        forcedOff.putArgument("cn1.hardened", "false");
        assertFalse(AndroidGradleBuilder.androidRenameHardeningActive(forcedOff, true, true),
                "cn1.hardened=false (forced-off escape hatch) must not require R8");

        // A build that actually hardened (verified output) with a rename profile DOES require R8.
        BuildRequest hardened = new BuildRequest();
        hardened.putArgument("harden.level", "aggressive");
        hardened.putArgument("cn1.hardened", "true");
        assertTrue(AndroidGradleBuilder.androidRenameHardeningActive(hardened, true, true),
                "a verified hardened rename profile requires R8");

        // Even with cn1.hardened=true, harden.level=off or rename opted out needs no R8.
        BuildRequest offLevel = new BuildRequest();
        offLevel.putArgument("harden.level", "off");
        offLevel.putArgument("cn1.hardened", "true");
        assertFalse(AndroidGradleBuilder.androidRenameHardeningActive(offLevel, true, true));
        assertFalse(AndroidGradleBuilder.androidRenameHardeningActive(hardened, true, false),
                "rename opted out needs no R8");
        assertFalse(AndroidGradleBuilder.androidRenameHardeningActive(hardened, false, true),
                "harden.and.enabled=false needs no R8");
    }

    @Test
    void typedPushAutoDetectsBothAndroidProviderConfigurations() {
        assertTrue(AndroidGradleBuilder.usesFcmPush(3, "auto", true));
        assertFalse(AndroidGradleBuilder.usesFcmPush(3, "auto", false));
        assertTrue(AndroidGradleBuilder.usesHuaweiPush(3, "auto", true));
        assertFalse(AndroidGradleBuilder.usesHuaweiPush(3, "auto", false));
        assertTrue(AndroidGradleBuilder.usesFcmPush(1, "fcm", false));
        assertFalse(AndroidGradleBuilder.usesHuaweiPush(1, "huawei", true));
    }

    @Test
    void typedPushReplaysColdStartMessagesBeforeTheListenerIsInstalled() {
        String typedReplay = AndroidGradleBuilder.pendingPushReplayCode(3);
        assertTrue(typedReplay.contains("AndroidImplementation.firePendingPushes(new PushCallback()"));
        assertTrue(typedReplay.contains("CodenameOneImplementation.getPushCallback()"));
        assertTrue(typedReplay.contains("PushClient.dispatch(value)"));

        String legacyReplay = AndroidGradleBuilder.pendingPushReplayCode(1);
        assertTrue(legacyReplay.contains("AndroidImplementation.firePendingPushes("
                + "com.codename1.impl.CodenameOneImplementation.getPushCallback(), this)"));
        assertFalse(legacyReplay.contains("PushClient.dispatch"));
    }
}
