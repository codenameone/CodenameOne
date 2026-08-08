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
package com.codename1.maven;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** The Check-1 truth table: local targets, invalid levels, on-device-debug, and the escape hatch. */
public class HardeningPreflightTest {

    @Test
    public void offIsAlwaysOk() {
        assertFalse(HardeningPreflight.check("off", "ios-source", false, false).isFailed());
        assertFalse(HardeningPreflight.check(null, "local-javascript", false, false).isFailed());
        assertFalse(HardeningPreflight.check("", "android-source", false, false).isFailed());
    }

    @Test
    public void invalidLevelFails() {
        HardeningPreflight.Result r = HardeningPreflight.check("stanadrd", "ios-device", false, false);
        assertTrue(r.isFailed());
        assertTrue(r.getMessage().contains("Invalid harden.level"));
    }

    @Test
    public void cloudTargetWithValidLevelIsOk() {
        assertFalse(HardeningPreflight.check("standard", "ios-device", false, false).isFailed());
        assertFalse(HardeningPreflight.check("aggressive", "android-device", false, false).isFailed());
    }

    @Test
    public void localTargetWithHardeningFailsUnlessAllowed() {
        HardeningPreflight.Result blocked =
                HardeningPreflight.check("standard", "local-javascript", false, false);
        assertTrue(blocked.isFailed());
        assertTrue(blocked.getMessage().contains("build server"));

        HardeningPreflight.Result allowed =
                HardeningPreflight.check("standard", "local-javascript", true, false);
        assertFalse(allowed.isFailed());
        assertTrue(allowed.isForceOff());
        assertTrue(allowed.getMessage().contains("NOT"));
    }

    @Test
    public void sourceTargetsAreLocal() {
        assertTrue(HardeningPreflight.isLocalOrSourceTarget("ios-source"));
        assertTrue(HardeningPreflight.isLocalOrSourceTarget("android-source"));
        assertTrue(HardeningPreflight.isLocalOrSourceTarget("mac-source"));
        assertTrue(HardeningPreflight.isLocalOrSourceTarget("local-windows-device"));
        assertFalse(HardeningPreflight.isLocalOrSourceTarget("ios-device"));
        assertFalse(HardeningPreflight.isLocalOrSourceTarget("android-device"));
    }

    @Test
    public void onDeviceDebugWithHardeningFails() {
        HardeningPreflight.Result r = HardeningPreflight.check("standard", "android-device", false, true);
        assertTrue(r.isFailed());
        assertTrue(r.getMessage().contains("on-device-debug"));
    }

    @Test
    public void levelWithEveryTransformOverriddenOffRequestsNothing() {
        // standard defaults to rename + constant-string encryption; overriding all of them off leaves
        // nothing for the engine to do (SKIPPED_NOT_REQUESTED), so the mojo must treat it as off and
        // not reject a local/source build for a "hardening" it isn't asking for.
        java.util.Properties p = new java.util.Properties();
        p.setProperty("codename1.arg.harden.rename", "false");
        p.setProperty("codename1.arg.harden.strings", "off");
        p.setProperty("codename1.arg.harden.controlFlow", "false");
        assertFalse(CN1BuildMojo.hardeningRequestsAnyTransform(p, "standard"));
        assertFalse(CN1BuildMojo.hardeningRequestsAnyTransform(p, "aggressive"));
        assertFalse(CN1BuildMojo.hardeningRequestsAnyTransform(p, "paranoid"));
    }

    @Test
    public void aRemainingTransformStillCounts() {
        // Any single transform left on means hardening IS requested.
        java.util.Properties strings = new java.util.Properties();
        strings.setProperty("codename1.arg.harden.rename", "false");
        strings.setProperty("codename1.arg.harden.strings", "all");
        strings.setProperty("codename1.arg.harden.controlFlow", "false");
        assertTrue(CN1BuildMojo.hardeningRequestsAnyTransform(strings, "standard"));

        java.util.Properties rename = new java.util.Properties();
        rename.setProperty("codename1.arg.harden.strings", "off");
        rename.setProperty("codename1.arg.harden.controlFlow", "false");
        // rename unset -> defaults on at standard.
        assertTrue(CN1BuildMojo.hardeningRequestsAnyTransform(rename, "standard"));
    }

    @Test
    public void defaultsAtStandardRequestHardening() {
        // No overrides at a non-off level requests hardening (rename + constant strings by default).
        assertTrue(CN1BuildMojo.hardeningRequestsAnyTransform(new java.util.Properties(), "standard"));
        // off requests nothing regardless of overrides.
        assertFalse(CN1BuildMojo.hardeningRequestsAnyTransform(new java.util.Properties(), "off"));
    }

    @Test
    public void invalidLevelIsNotReducedToOff() {
        // A misspelled level must NOT be silently rewritten to off by the no-transform reduction: it
        // has to reach HardeningPreflight.check() so the client-side invalid-level validation fires.
        java.util.Properties allOff = new java.util.Properties();
        allOff.setProperty("codename1.arg.harden.rename", "false");
        allOff.setProperty("codename1.arg.harden.strings", "off");
        allOff.setProperty("codename1.arg.harden.controlFlow", "false");
        assertFalse(CN1BuildMojo.hardeningReducesToOff(allOff, "stanadrd"));
        assertFalse(CN1BuildMojo.hardeningReducesToOff(new java.util.Properties(), "stanadrd"));
        // The invalid level still fails the preflight, unchanged.
        assertTrue(HardeningPreflight.check("stanadrd", "ios-device", false, false).isFailed());
    }

    @Test
    public void validLevelWithEveryTransformOffReducesToOff() {
        java.util.Properties allOff = new java.util.Properties();
        allOff.setProperty("codename1.arg.harden.rename", "false");
        allOff.setProperty("codename1.arg.harden.strings", "off");
        allOff.setProperty("codename1.arg.harden.controlFlow", "false");
        assertTrue(CN1BuildMojo.hardeningReducesToOff(allOff, "standard"));
        // A real request or plain off is not "reduced" (off has nothing to reduce).
        assertFalse(CN1BuildMojo.hardeningReducesToOff(new java.util.Properties(), "standard"));
        assertFalse(CN1BuildMojo.hardeningReducesToOff(allOff, "off"));
    }

    @Test
    public void controlFlowOnlyReducesToOffOnParparVMTargets() {
        // aggressive with rename+strings off leaves only control flow, which the engine SKIPS on the
        // ParparVM native ports. So on iOS this reduces to off (must not be rejected), while on a
        // JVM-bytecode target (JavaSE/Android) control flow really runs and it does NOT reduce to off.
        java.util.Properties cfOnly = new java.util.Properties();
        cfOnly.setProperty("codename1.arg.harden.rename", "false");
        cfOnly.setProperty("codename1.arg.harden.strings", "off");
        // controlFlow unset -> on by default at aggressive.
        assertTrue(CN1BuildMojo.hardeningReducesToOff(cfOnly, "aggressive", "ios"),
                "control flow is skipped on iOS, so nothing runs");
        assertTrue(CN1BuildMojo.hardeningReducesToOff(cfOnly, "aggressive", "win"));
        assertFalse(CN1BuildMojo.hardeningReducesToOff(cfOnly, "aggressive", "javase"),
                "control flow really runs on JavaSE");
        assertFalse(CN1BuildMojo.hardeningReducesToOff(cfOnly, "aggressive", "and"),
                "control flow really runs on Android");
    }

    @Test
    public void stringOnlyReducesToOffOnJavaScript() {
        // standard with rename+controlFlow off leaves only string encryption, which the engine SKIPS on
        // JavaScript (the native bridge). So on JS this reduces to off, but on iOS strings really run.
        java.util.Properties strOnly = new java.util.Properties();
        strOnly.setProperty("codename1.arg.harden.rename", "false");
        strOnly.setProperty("codename1.arg.harden.controlFlow", "false");
        // strings unset -> constant-string encryption on by default at standard.
        assertTrue(CN1BuildMojo.hardeningReducesToOff(strOnly, "standard", "javascript"),
                "string encryption is skipped on JavaScript, so nothing runs");
        assertFalse(CN1BuildMojo.hardeningReducesToOff(strOnly, "standard", "ios"),
                "string encryption really runs on iOS");
    }
}
