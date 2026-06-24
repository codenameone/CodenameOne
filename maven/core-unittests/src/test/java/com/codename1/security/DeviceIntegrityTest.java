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
package com.codename1.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pure-logic coverage for the {@link DeviceIntegrity} accessibility-service
 * allow-list evaluation ({@code containsUntrustedService}), which backs
 * {@link DeviceIntegrity#hasUntrustedAccessibilityService(String...)}. Exercises
 * the {@code package/.Service} component-id parsing and the allow-list matching
 * without needing a platform {@code Display} implementation.
 */
class DeviceIntegrityTest {

    private static final String TALKBACK = "com.google.android.marvin.talkback/.TalkBackService";
    private static final String MALWARE = "com.evil.overlay/.SpyService";

    @Test
    void noEnabledServicesIsTrusted() {
        assertFalse(DeviceIntegrity.containsUntrustedService(null, new String[0]));
        assertFalse(DeviceIntegrity.containsUntrustedService(new String[0], new String[0]));
        assertFalse(DeviceIntegrity.containsUntrustedService(new String[0],
                new String[] { "com.google.android.marvin.talkback" }));
    }

    @Test
    void anyEnabledServiceIsUntrustedWhenAllowListEmpty() {
        // With no allow-list, every enabled accessibility service is suspicious.
        assertTrue(DeviceIntegrity.containsUntrustedService(new String[] { TALKBACK }, new String[0]));
        assertTrue(DeviceIntegrity.containsUntrustedService(new String[] { TALKBACK }, null));
    }

    @Test
    void allowedPackageIsTrusted() {
        // The component id package is matched, ignoring the "/.Service" suffix.
        assertFalse(DeviceIntegrity.containsUntrustedService(
                new String[] { TALKBACK },
                new String[] { "com.google.android.marvin.talkback" }));
    }

    @Test
    void untrustedServiceAlongsideTrustedTripsTheGuard() {
        assertTrue(DeviceIntegrity.containsUntrustedService(
                new String[] { TALKBACK, MALWARE },
                new String[] { "com.google.android.marvin.talkback" }));
    }

    @Test
    void componentIdWithoutSlashIsTreatedAsPackage() {
        // Some ids may be a bare package with no "/.Service" portion.
        assertFalse(DeviceIntegrity.containsUntrustedService(
                new String[] { "com.trusted.reader" },
                new String[] { "com.trusted.reader" }));
        assertTrue(DeviceIntegrity.containsUntrustedService(
                new String[] { "com.trusted.reader" },
                new String[] { "com.other.pkg" }));
    }

    @Test
    void nullAndEmptyEntriesAreSkipped() {
        // Stray null / empty component ids must not count as untrusted services.
        assertFalse(DeviceIntegrity.containsUntrustedService(
                new String[] { null, "", TALKBACK },
                new String[] { "com.google.android.marvin.talkback" }));
        assertTrue(DeviceIntegrity.containsUntrustedService(
                new String[] { null, "", MALWARE },
                new String[] { "com.google.android.marvin.talkback" }));
    }
}
