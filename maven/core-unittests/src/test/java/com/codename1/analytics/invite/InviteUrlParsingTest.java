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
package com.codename1.analytics.invite;

import com.codename1.io.Preferences;
import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import org.junit.jupiter.api.AfterEach;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class InviteUrlParsingTest extends UITestBase {

    @AfterEach
    void cleanUp() {
        InviteTestSupport.tearDown();
    }

    @FormTest
    void recognisesTheSluggedAndBareLinkForms() {
        InviteTestSupport.freshInstall();
        assertEquals("ABC123",
                Invites.extractCode("https://cloud.codenameone.com/i/acme/ABC123"));
        // The slug is remembered so later invites mint the precise form, which
        // is what keeps two enrolled apps on one device from claiming each
        // other's links.
        assertEquals("acme", Preferences.get(Invites.PREF_SLUG, ""));

        InviteTestSupport.freshInstall();
        assertEquals("ABC123",
                Invites.extractCode("https://cloud.codenameone.com/i/ABC123"));
    }

    @FormTest
    void ignoresAForeignHostEvenWhenThePathMatches() {
        InviteTestSupport.freshInstall();
        assertNull(Invites.extractCode("https://evil.example.com/i/acme/ABC123"));
        // A prefix of our host is not our host. Matching on startsWith here
        // would accept a look-alike domain.
        assertNull(Invites.extractCode("https://cloud.codenameone.com.evil.test/i/ABC123"));
        assertNull(Invites.extractCode("https://staging.cloud.codenameone.com/i/ABC123"));
    }

    @FormTest
    void hostComparisonIsCaseInsensitiveWithoutCaseFolding() {
        InviteTestSupport.freshInstall();
        assertEquals("ABC123",
                Invites.extractCode("https://CLOUD.CodenameOne.COM/i/ABC123"));
    }

    @FormTest
    void readsTheCodeOutOfAReferrerQueryString() {
        InviteTestSupport.freshInstall();
        assertEquals("ABC123", Invites.codeFromQuery(
                "utm_source=cn1_invite&utm_medium=referral&cn1_invite=ABC123"));
        assertEquals("ABC123", Invites.codeFromQuery("cn1_invite=ABC123"));
        assertNull(Invites.codeFromQuery("utm_source=cn1_invite&utm_medium=referral"));
        assertNull(Invites.codeFromQuery(""));
        assertNull(Invites.codeFromQuery(null));
    }

    @FormTest
    void theReferrerKeyIsMatchedExactlyAndNeverCaseFolded() {
        InviteTestSupport.freshInstall();
        // String.toLowerCase is locale sensitive and has no root-locale
        // overload in this runtime, so under a Turkish default locale the 'i'
        // in "invite" folds to a dotless i and a folded comparison silently
        // stops matching. The key is therefore compared with equals, and a
        // differently cased key is simply not our key.
        assertNull(Invites.codeFromQuery("CN1_INVITE=ABC123"));
        assertNull(Invites.codeFromQuery("Cn1_Invite=ABC123"));
    }

    @FormTest
    void valueIsSplitOnTheFirstEqualsOnly() {
        InviteTestSupport.freshInstall();
        assertEquals("a=b", Invites.codeFromQuery("cn1_invite=a%3Db"));
    }
}
