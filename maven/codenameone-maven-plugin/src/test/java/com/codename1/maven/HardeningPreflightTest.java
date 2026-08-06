/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
}
