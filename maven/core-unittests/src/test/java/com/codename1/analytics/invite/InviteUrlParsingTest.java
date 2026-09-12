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
import java.util.Map;
import com.codename1.junit.EdtTest;
import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    @EdtTest
    void aFragmentIsNotPartOfTheCode() {
        // An App Link commonly arrives with the fragment still attached, and it
        // is not part of the path -- so this claimed a code called
        // "ABC123#section", which exists nowhere.
        assertTrue(Invites.handleUrl("https://cloud.codenameone.com/i/acme/ABC123#section"));
        Map<String, String> pending = InviteStore.read(InviteStore.PENDING);
        assertNotNull(pending);
        assertEquals("ABC123", InviteStore.get(pending, "code", null));
    }

    @Test
    @EdtTest
    void aFragmentAfterAQueryIsAlsoStripped() {
        assertTrue(Invites.handleUrl(
                "https://cloud.codenameone.com/i/acme/ABC124?utm_source=x#top"));
        Map<String, String> pending = InviteStore.read(InviteStore.PENDING);
        assertEquals("ABC124", InviteStore.get(pending, "code", null));
    }

    @Test
    @EdtTest
    void aFragmentOnAQueryStyleLinkIsAlsoStripped() {
        // The query branch runs first, so stripping on the path branch alone
        // left it parsing "?cn1_invite=ABC125#section" and claiming a code with
        // the fragment glued to it.
        assertTrue(Invites.handleUrl(
                "https://cloud.codenameone.com/i/acme?cn1_invite=ABC125#section"));
        Map<String, String> pending = InviteStore.read(InviteStore.PENDING);
        assertEquals("ABC125", InviteStore.get(pending, "code", null));
    }

    @FormTest
    void aForeignUrlCarryingTheKeyIsNotAnInvite() {
        // The query form used to be read BEFORE the host was checked and
        // returned the moment it found the key, so any deep link the
        // application handles for any other domain -- a partner site, a
        // campaign page -- was accepted and claimed. That hands a fresh
        // install, or a last-touch re-attribution, to whoever wrote a url this
        // app happens to open.
        assertNull(Invites.extractCode(
                        "https://partner.example.com/promo?cn1_invite=STOLEN1"),
                "a url on somebody else's host was accepted as an invite");
        // Our own host in the query form is still an invite.
        assertEquals("MINE123", Invites.extractCode(
                "https://cloud.codenameone.com/anything?cn1_invite=MINE123"));
    }

    @FormTest
    void anotherAppsSlugOnTheSharedHostIsNotOurInvite() {
        // One domain serves every enrolled app, which is why the path carries
        // a slug. A build whose App Links filter claims /i/ broadly is handed
        // /i/other-app/CODE as readily as its own, and this took the last
        // component regardless -- claiming a stranger's invite, and
        // remembering their slug as its own so later mints advertised their
        // links.
        Invites.reset();
        Preferences.set(Invites.PREF_SLUG, "acme");

        assertNull(Invites.extractCode("https://cloud.codenameone.com/i/other-app/THEIRS1"),
                "an invite belonging to another app on the shared host was claimed");
        assertEquals("acme", Preferences.get(Invites.PREF_SLUG, ""),
                "the foreign slug was remembered, so later invites mint their links");
        assertEquals("OURS123",
                Invites.extractCode("https://cloud.codenameone.com/i/acme/OURS123"),
                "our own slugged invite stopped being recognised");
    }

    @FormTest
    void aRoutedInviteUrlIsNotHandledTwice() {
        // handleUrl() is the documented route for an app that handles its own
        // deep links, and on Android it runs from the dispatch that setting
        // AppArg fires -- with a checkForInvite() queued behind it as the
        // fallback for apps with no router. The argument stayed set, so that
        // queued check read the same url and handled it again: invite_opened
        // twice on a resolved install, and on a pending one a duplicate claim
        // whose epoch bump discarded the answer to the first.
        Invites.reset();
        com.codename1.ui.Display d = com.codename1.ui.Display.getInstance();
        d.setProperty("AppArg", "https://cloud.codenameone.com/i/ROUTED1");

        assertTrue(Invites.handleUrl("https://cloud.codenameone.com/i/ROUTED1"),
                "the fixture url was not recognised as an invite");

        assertNull(d.getProperty("AppArg", null),
                "the routed url was left in AppArg, so the queued checkForInvite() "
                        + "handles the same invite a second time");
    }

    @FormTest
    void anUnrelatedAppArgIsLeftAlone() {
        // An application may pass any string to handleUrl(). Clearing a launch
        // argument that is not the one being handled is not ours to do.
        Invites.reset();
        com.codename1.ui.Display d = com.codename1.ui.Display.getInstance();
        d.setProperty("AppArg", "myapp://somewhere/else");

        Invites.handleUrl("https://cloud.codenameone.com/i/OTHER1");

        assertEquals("myapp://somewhere/else", d.getProperty("AppArg", null),
                "an unrelated launch argument was cleared");
    }
}
