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
import static org.junit.jupiter.api.Assertions.fail;
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
        assertEquals("ABC123xxxxxxxxxxxxxxxx",
                Invites.extractCode("https://cloud.codenameone.com/i/acme/ABC123xxxxxxxxxxxxxxxx"));
        // And the slug is NOT remembered from it.
        //
        // It used to be, on the reasoning that a build with no slug of its own
        // may as well learn one. But the only urls that reach that branch are
        // the ones that CARRY a slug, and a build whose App Links filter
        // claims /i/ broadly is handed another app's link on the host every
        // enrolled app shares -- so the only thing it could learn from was a
        // slug somebody else chose. Every invite minted before the first
        // registration response then advertised their path. The slug comes
        // from the build hint or from a registration response, both of which
        // are ours.
        assertEquals("", Preferences.get(Invites.PREF_SLUG, ""),
                "a slug was learned from an incoming link, so a stranger's link can "
                        + "rewrite the paths this app mints");

        InviteTestSupport.freshInstall();
        assertEquals("ABC123xxxxxxxxxxxxxxxx",
                Invites.extractCode("https://cloud.codenameone.com/i/ABC123xxxxxxxxxxxxxxxx"));
    }

    @FormTest
    void ignoresAForeignHostEvenWhenThePathMatches() {
        InviteTestSupport.freshInstall();
        assertNull(Invites.extractCode("https://evil.example.com/i/acme/ABC123xxxxxxxxxxxxxxxx"));
        // A prefix of our host is not our host. Matching on startsWith here
        // would accept a look-alike domain.
        assertNull(Invites.extractCode("https://cloud.codenameone.com.evil.test/i/ABC123xxxxxxxxxxxxxxxx"));
        assertNull(Invites.extractCode("https://staging.cloud.codenameone.com/i/ABC123xxxxxxxxxxxxxxxx"));
    }

    @FormTest
    void hostComparisonIsCaseInsensitiveWithoutCaseFolding() {
        InviteTestSupport.freshInstall();
        assertEquals("ABC123xxxxxxxxxxxxxxxx",
                Invites.extractCode("https://CLOUD.CodenameOne.COM/i/ABC123xxxxxxxxxxxxxxxx"));
    }

    @FormTest
    void readsTheCodeOutOfAReferrerQueryString() {
        InviteTestSupport.freshInstall();
        assertEquals("ABC123xxxxxxxxxxxxxxxx", Invites.codeFromQuery(
                "utm_source=cn1_invite&utm_medium=referral&cn1_invite=ABC123xxxxxxxxxxxxxxxx"));
        assertEquals("ABC123xxxxxxxxxxxxxxxx", Invites.codeFromQuery("cn1_invite=ABC123xxxxxxxxxxxxxxxx"));
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
        assertTrue(Invites.handleUrl("https://cloud.codenameone.com/i/acme/ABC123xxxxxxxxxxxxxxxx#section"));
        Map<String, String> pending = InviteStore.read(InviteStore.PENDING);
        assertNotNull(pending);
        assertEquals("ABC123xxxxxxxxxxxxxxxx", InviteStore.get(pending, "code", null));
    }

    @Test
    @EdtTest
    void aFragmentAfterAQueryIsAlsoStripped() {
        assertTrue(Invites.handleUrl(
                "https://cloud.codenameone.com/i/acme/ABC124xxxxxxxxxxxxxxxx?utm_source=x#top"));
        Map<String, String> pending = InviteStore.read(InviteStore.PENDING);
        assertEquals("ABC124xxxxxxxxxxxxxxxx", InviteStore.get(pending, "code", null));
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

        assertNull(Invites.extractCode("https://cloud.codenameone.com/i/other-app/THEIRS1xxxxxxxxxxxxxxx"),
                "an invite belonging to another app on the shared host was claimed");
        assertEquals("acme", Preferences.get(Invites.PREF_SLUG, ""),
                "the foreign slug was remembered, so later invites mint their links");
        assertEquals("OURS123xxxxxxxxxxxxxxx",
                Invites.extractCode("https://cloud.codenameone.com/i/acme/OURS123xxxxxxxxxxxxxxx"),
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
        d.setProperty("AppArg", "https://cloud.codenameone.com/i/ROUTED1xxxxxxxxxxxxxxx");

        assertTrue(Invites.handleUrl("https://cloud.codenameone.com/i/ROUTED1xxxxxxxxxxxxxxx"),
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

        Invites.handleUrl("https://cloud.codenameone.com/i/OTHER1xxxxxxxxxxxxxxxx");

        assertEquals("myapp://somewhere/else", d.getProperty("AppArg", null),
                "an unrelated launch argument was cleared");
    }

    @FormTest
    void theLinkBaseMustBeAnHttpsOrigin() {
        // Invite.getUrl() promises an absolute https url, and the generated
        // Android filter and iOS associated domain match an https host with
        // the /i/ path and nothing else.
        Invites.reset();
        // A bare host is what the build hint carries, so it is accepted and
        // read as https.
        Invites.setLinkBase("links.example.com");
        assertEquals("https://links.example.com", Invites.getLinkBase());

        try {
            Invites.setLinkBase("http://links.example.com");
            fail("an http base was accepted, and every link it mints opens the browser");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("https"), expected.getMessage());
        }
        try {
            // Mints /base/i/<code>, which the generated filter -- matching
            // /i/ -- never sees, while the host check stays silent because
            // the host is right.
            Invites.setLinkBase("https://links.example.com/base");
            fail("a base carrying a path was accepted");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("no path"), expected.getMessage());
        }
        // A trailing slash is not a path.
        Invites.setLinkBase("https://links.example.com/");
        assertEquals("https://links.example.com", Invites.getLinkBase());
        Invites.setLinkBase(null);
    }

    @FormTest
    void onlyHttpsUrlsCarryInvites() {
        // An application forwarding its broader deep links here could hand
        // over a custom scheme or plain http on the right host, and a
        // host-only test accepted both -- persisting and claiming a code
        // although nothing the framework mints or the platforms associate is
        // anything but https.
        assertNull(Invites.extractCode("myapp://cloud.codenameone.com/i/SCHEME1xxxxxxxxxxxxxxx"),
                "a custom-scheme url was accepted as an invite");
        assertNull(Invites.extractCode("http://cloud.codenameone.com/i/PLAIN1xxxxxxxxxxxxxxxx"),
                "an http url was accepted as an invite");
        assertEquals("REAL123xxxxxxxxxxxxxxx",
                Invites.extractCode("https://cloud.codenameone.com/i/REAL123xxxxxxxxxxxxxxx"),
                "the https form stopped being recognised");
    }

    /**
     * A same-host url only yields a code if it looks like one.
     *
     * <p>Any nonempty final path component used to be accepted, so
     * {@code /i/<anything>} on the shared host was consumed, written into the
     * PENDING record and put through the claim retries -- and on a fresh
     * install the no-match that came back could settle attribution before the
     * Play referrer or the App Clip handoff had been looked at, which is the
     * answer that actually mattered.</p>
     *
     * <p>What this does and does not buy is worth being exact about: it stops
     * malformed values, not a crafted one. Somebody who supplies 22 url-safe
     * characters still gets a claim and a no-match. The grammar is a filter on
     * accidents and garbage, not an authentication.</p>
     */
    @FormTest
    void aValueThatCannotBeACodeIsNotTreatedAsOne() {
        InviteTestSupport.freshInstall();
        String host = "https://cloud.codenameone.com/i/";

        assertNull(Invites.extractCode(host + "hello"),
                "a short word was accepted as an invite code");
        assertNull(Invites.extractCode(host + "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"),
                "an over-long value was accepted as an invite code");
        assertNull(Invites.extractCode(host + "ABC123xxxxxxxxxxxxxxx!"),
                "a value with a character no code can contain was accepted");
        assertNull(Invites.extractCode(host + "ABC123xxxxxxxxxxxxxxx."),
                "a value with a dot was accepted, and a code is url-safe base64");

        // Exactly the shape the framework mints still works, in both forms.
        assertEquals("ABC123xxxxxxxxxxxxxxxx",
                Invites.extractCode(host + "ABC123xxxxxxxxxxxxxxxx"),
                "a well formed code stopped being recognised");
        assertEquals("ABC123xxxxxxxxxxxxxxxx",
                Invites.extractCode(host + "acme/ABC123xxxxxxxxxxxxxxxx"),
                "a well formed slugged code stopped being recognised");
    }
}
