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
