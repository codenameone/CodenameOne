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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The health privacy-policy hint. Google Play requires the policy and the
 * permissions-rationale screen has to link to it, so a value that passes the
 * build check but cannot be opened on a device is worse than one that fails:
 * the app ships and gets rejected in review instead.
 */
class HealthPolicyUrlTest {

    @Test
    void anAbsoluteHttpsUrlIsAccepted() {
        assertTrue(AndroidGradleBuilder.isHealthPolicyUrl(
                "https://example.com/privacy"));
        assertTrue(AndroidGradleBuilder.isHealthPolicyUrl(
                "https://example.com"));
    }

    @Test
    void theSchemeIsMatchedWithoutRegardToCase() {
        // A hint typed in a settings dialog, not a parsed document.
        assertTrue(AndroidGradleBuilder.isHealthPolicyUrl(
                "HTTPS://Example.com/privacy"));
    }

    @Test
    void nothingAtAllIsRejected() {
        assertFalse(AndroidGradleBuilder.isHealthPolicyUrl(null));
        assertFalse(AndroidGradleBuilder.isHealthPolicyUrl(""));
    }

    @Test
    void aValueWithNoSchemeIsRejected() {
        // The rationale screen hands this to the platform as a link, and a
        // bare host opens nothing.
        assertFalse(AndroidGradleBuilder.isHealthPolicyUrl(
                "example.com/privacy"));
        assertFalse(AndroidGradleBuilder.isHealthPolicyUrl("not-a-url"));
    }

    @Test
    void otherSchemesAreRejected() {
        // http:// is refused as well: cleartext is blocked by default at
        // the API levels a health build targets, so the link would fail on
        // the device rather than merely being insecure.
        assertFalse(AndroidGradleBuilder.isHealthPolicyUrl(
                "ftp://example.com/policy"));
        assertFalse(AndroidGradleBuilder.isHealthPolicyUrl(
                "http://example.com/policy"));
    }

    @Test
    void anHttpsUrlWithNoHostIsRejected() {
        // Parses, has the right scheme, and names no server.
        assertFalse(AndroidGradleBuilder.isHealthPolicyUrl("https:///privacy"));
    }

    @Test
    void aMalformedValueIsRejectedRatherThanThrowing() {
        // The hint reaches this straight from the project properties, so a
        // parse failure has to be an answer rather than an exception with
        // no build-hint name in it.
        assertFalse(AndroidGradleBuilder.isHealthPolicyUrl(
                "https://exa mple.com/privacy"));
        assertFalse(AndroidGradleBuilder.isHealthPolicyUrl("   "));
    }
}
