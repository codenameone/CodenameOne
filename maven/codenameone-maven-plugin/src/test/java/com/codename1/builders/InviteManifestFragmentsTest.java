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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the App Links intent filter injected for the
 * {@code com.codename1.analytics.invite} API, and in particular the
 * quote-delimited duplicate suppression that keeps a developer's own filter
 * for a DIFFERENT host from suppressing ours.
 */
class InviteManifestFragmentsTest {

    private static final String HOST = "cloud.codenameone.com";

    @Test
    void filterCarriesAutoVerifyAndTheInvitePath() {
        String out = InviteManifestFragments.injectAppLinks("", HOST, null);
        // autoVerify is what makes Android open the app rather than showing a
        // disambiguation dialog, and it is the whole point of the filter.
        assertTrue(out.contains("android:autoVerify=\"true\""), out);
        assertTrue(out.contains("android:name=\"android.intent.action.VIEW\""), out);
        assertTrue(out.contains("android:name=\"android.intent.category.BROWSABLE\""), out);
        assertTrue(out.contains("android:scheme=\"https\""), out);
        assertTrue(out.contains("android:host=\"" + HOST + "\""), out);
        assertTrue(out.contains("android:pathPrefix=\"/i/\""), out);
    }

    @Test
    void aDevelopersOwnFilterIsPreservedAndOursIsAppended() {
        String existing = "<intent-filter>"
                + "<action android:name=\"android.intent.action.VIEW\" />"
                + "<data android:scheme=\"myapp\" />"
                + "</intent-filter>";
        String out = InviteManifestFragments.injectAppLinks(existing, HOST, null);
        assertTrue(out.startsWith(existing), "the developer's filter must survive verbatim");
        assertTrue(out.contains("android:host=\"" + HOST + "\""), out);
    }

    @Test
    void anAlreadyDeclaredHostIsNotDeclaredTwice() {
        String existing = InviteManifestFragments.injectAppLinks("", HOST, null);
        String out = InviteManifestFragments.injectAppLinks(existing, HOST, null);
        assertEquals(existing, out, "the host was declared a second time");
    }

    @Test
    void aDifferentHostThatContainsOursDoesNotSuppressIt() {
        // The trap this test exists for: a plain contains(host) check reads
        // android:host="staging.cloud.codenameone.com" as already declaring
        // cloud.codenameone.com, so a developer with a staging filter would
        // silently ship without the production one and every invite link would
        // open the browser.
        String existing = "<intent-filter android:autoVerify=\"true\">"
                + "<data android:scheme=\"https\" "
                + "android:host=\"staging.cloud.codenameone.com\" "
                + "android:pathPrefix=\"/i/\" />"
                + "</intent-filter>";
        assertFalse(InviteManifestFragments.declaresHost(existing, HOST),
                "a longer host must not read as ours");
        String out = InviteManifestFragments.injectAppLinks(existing, HOST, null);
        assertTrue(out.contains("android:host=\"" + HOST + "\""),
                "the production filter was suppressed by a staging one");
    }

    @Test
    void aHostThatIsAPrefixOfOursDoesNotSuppressItEither() {
        String existing = "<data android:host=\"codenameone.com\" />";
        assertFalse(InviteManifestFragments.declaresHost(existing, HOST));
    }

    @Test
    void anUnrelatedPathOnTheSameHostDoesNotSuppressTheInviteFilter() {
        // The host alone used to settle it, so an application that already
        // routed cloud.codenameone.com/account/ never got an invite filter and
        // every invite link opened the browser.
        String existing = "<intent-filter>"
                + "<data android:scheme=\"https\" "
                + "android:host=\"" + HOST + "\" "
                + "android:pathPrefix=\"/account/\" />"
                + "</intent-filter>";
        assertFalse(InviteManifestFragments.declaresInviteLinks(existing, HOST, "acme"),
                "an unrelated path must not read as covering the invite links");
        String out = InviteManifestFragments.injectAppLinks(existing, HOST, "acme");
        assertTrue(out.contains("android:pathPrefix=\"/i/acme/\""),
                "the invite filter was suppressed by an unrelated path");
    }

    @Test
    void aBroaderPrefixOnTheSameHostDoesCoverTheInviteLinks() {
        String existing = "<data android:host=\"" + HOST + "\" "
                + "android:pathPrefix=\"/i/\" />";
        assertTrue(InviteManifestFragments.declaresInviteLinks(existing, HOST, "acme"),
                "/i/ accepts /i/acme/<code> and needs no second filter");
        assertEquals(existing, InviteManifestFragments.injectAppLinks(existing, HOST, "acme"));
    }

    @Test
    void anotherAppsSlugDoesNotCoverOurs() {
        String existing = "<data android:host=\"" + HOST + "\" "
                + "android:pathPrefix=\"/i/other/\" />";
        assertFalse(InviteManifestFragments.declaresInviteLinks(existing, HOST, "acme"));
    }

    @Test
    void aHostFilterWithNoPathAtAllCoversEveryPathOnIt() {
        String existing = "<data android:scheme=\"https\" android:host=\"" + HOST + "\" />";
        assertTrue(InviteManifestFragments.declaresInviteLinks(existing, HOST, "acme"));
    }

    @Test
    void hostAndPathMustCoincideInOneFilter() {
        // Two filters, neither of which opens an invite link: ours on the right
        // host but a different path, and the right path on a different host.
        // Searched across the whole hint value they answered yes between them
        // and suppressed the filter that was actually needed.
        String existing = "<intent-filter>"
                + "<data android:scheme=\"https\" android:host=\"" + HOST + "\" "
                + "android:pathPrefix=\"/account/\" /></intent-filter>"
                + "<intent-filter>"
                + "<data android:scheme=\"https\" android:host=\"other.example.com\" "
                + "android:pathPrefix=\"/i/\" /></intent-filter>";
        assertFalse(InviteManifestFragments.declaresInviteLinks(existing, HOST, "acme"),
                "coverage was claimed by two filters that each fail on their own");
        String out = InviteManifestFragments.injectAppLinks(existing, HOST, "acme");
        assertTrue(out.contains("android:pathPrefix=\"/i/acme/\""),
                "the invite filter was suppressed across two unrelated filters");
    }

    @Test
    void oneFilterThatReallyCoversThemIsStillRecognized() {
        String existing = "<intent-filter>"
                + "<data android:scheme=\"https\" android:host=\"other.example.com\" "
                + "android:pathPrefix=\"/x/\" /></intent-filter>"
                + "<intent-filter>"
                + "<data android:scheme=\"https\" android:host=\"" + HOST + "\" "
                + "android:pathPrefix=\"/i/\" /></intent-filter>";
        assertTrue(InviteManifestFragments.declaresInviteLinks(existing, HOST, "acme"));
        assertEquals(existing, InviteManifestFragments.injectAppLinks(existing, HOST, "acme"));
    }

    @Test
    void anEmptyHostInjectsNothing() {
        assertEquals("", InviteManifestFragments.injectAppLinks("", "", null));
        assertEquals("", InviteManifestFragments.injectAppLinks("", null, null));
        assertEquals("x", InviteManifestFragments.injectAppLinks("x", null, null));
    }

    @Test
    void aSlugScopesTheFilterToThisAppAlone() {
        // The link domain is shared by every invite-enabled app, so a bare /i/
        // prefix makes all of them eligible handlers for every invite url and
        // Android shows a chooser or opens the wrong one. This is the Android
        // twin of the apple-app-site-association collision.
        String out = InviteManifestFragments.injectAppLinks("", HOST, "acme");
        assertTrue(out.contains("android:pathPrefix=\"/i/acme/\""), out);
        assertFalse(out.contains("android:pathPrefix=\"/i/\""), out);
    }

    @Test
    void withoutASlugTheBroadFilterIsStillEmitted() {
        // A filter matching nothing would be worse than a broad one: the app
        // would never open its own links at all.
        String out = InviteManifestFragments.injectAppLinks("", HOST, "");
        assertTrue(out.contains("android:pathPrefix=\"/i/\""), out);
    }

    @Test
    void aCustomHostIsHonoured() {
        String out = InviteManifestFragments.injectAppLinks("", "links.example.com", null);
        assertTrue(out.contains("android:host=\"links.example.com\""), out);
        assertFalse(out.contains(HOST), out);
    }
}
