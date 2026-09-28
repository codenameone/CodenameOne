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
import static org.junit.jupiter.api.Assertions.assertTrue;

class AndroidGradleBuilderVersionTest {

    @Test
    void comparesMinorVersionsInsteadOfOnlyTheMajorVersion() {
        assertTrue(AndroidGradleBuilder.compareVersions("8.1", "8.13") < 0);
        assertEquals(0, AndroidGradleBuilder.compareVersions("8.13", "8.13"));
        assertTrue(AndroidGradleBuilder.compareVersions("8.13.2", "8.13") > 0);
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
