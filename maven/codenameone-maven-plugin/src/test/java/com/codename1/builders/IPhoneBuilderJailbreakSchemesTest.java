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

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The runtime half of jailbreak detection asks canOpenURL: about the package
/// managers a jailbreak installs, and since iOS 9 that question answers false for
/// any scheme the app has not declared -- with no error, no warning, and a clean
/// verdict on a jailbroken device. So the declaration is not a nicety; it is the
/// difference between the probe working and the probe lying.
///
/// These pin the merge, because every failure mode here is silent.
class IPhoneBuilderJailbreakSchemesTest {

    private static BuildRequest request(String... kv) {
        BuildRequest r = new BuildRequest();
        r.setMainClass("MyApp");
        r.setPackageName("com.example");
        for (int i = 0; i < kv.length; i += 2) {
            r.putArgument(kv[i], kv[i + 1]);
        }
        return r;
    }

    private static List<String> declare(BuildRequest r) throws Exception {
        return declare(r, 26);
    }

    private static List<String> declare(BuildRequest r, int xcodeVersion) throws Exception {
        IPhoneBuilder b = new IPhoneBuilder();
        java.lang.reflect.Field xc = IPhoneBuilder.class.getDeclaredField("xcodeVersion");
        xc.setAccessible(true);
        xc.setInt(b, xcodeVersion);
        Method m = IPhoneBuilder.class.getDeclaredMethod(
                "declareApplicationQueriesSchemes", BuildRequest.class,
                String[].class, String.class);
        m.setAccessible(true);
        m.invoke(b, r, IPhoneBuilder.JAILBREAK_QUERY_SCHEMES, "why");
        String hint = r.getArg("ios.applicationQueriesSchemes", "");
        return hint.length() == 0 ? java.util.Collections.<String>emptyList()
                : Arrays.asList(hint.split(","));
    }

    /// `n` schemes of the project's own, none of them ours.
    private static String ownSchemes(int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append("app").append(i);
        }
        return sb.toString();
    }

    @Test
    void aProjectWithNoSchemesGetsEveryOne() throws Exception {
        List<String> got = declare(request());
        assertEquals(Arrays.asList(IPhoneBuilder.JAILBREAK_QUERY_SCHEMES), got);
    }

    /// Sileo is the one that matters. Cydia was the entire list before, and Cydia
    /// belongs to a rootful jailbreak nobody ships for current iOS -- so on the
    /// rootless device a customer actually reported, the probe was asking after the
    /// single front end guaranteed not to be installed.
    @Test
    void sileoIsDeclared() throws Exception {
        assertTrue(declare(request()).contains("sileo"));
    }

    @Test
    void theProjectsOwnSchemesSurvive() throws Exception {
        List<String> got = declare(request("ios.applicationQueriesSchemes",
                "com.apple.Home,fb-messenger"));
        assertEquals("com.apple.Home", got.get(0));
        assertEquals("fb-messenger", got.get(1));
        assertTrue(got.containsAll(Arrays.asList(IPhoneBuilder.JAILBREAK_QUERY_SCHEMES)));
    }

    @Test
    void aSchemeAlreadyDeclaredIsNotDuplicated() throws Exception {
        List<String> got = declare(request("ios.applicationQueriesSchemes", "sileo,filza"));
        assertEquals(1, java.util.Collections.frequency(got, "sileo"));
        assertEquals(1, java.util.Collections.frequency(got, "filza"));
    }

    /// Entry by entry, not as a substring. A project querying "sileo-installer"
    /// contains the text "sileo", and skipping on that basis would leave the exact
    /// scheme canOpenURL: is asked about undeclared -- the silent failure this
    /// whole merge exists to prevent.
    @Test
    void aLongerSchemeThatContainsOursIsNotMistakenForIt() throws Exception {
        List<String> got = declare(request("ios.applicationQueriesSchemes", "sileo-installer"));
        assertTrue(got.contains("sileo-installer"));
        assertTrue(got.contains("sileo"));
    }

    @Test
    void whitespaceAroundAnExistingEntryStillCounts() throws Exception {
        List<String> got = declare(request("ios.applicationQueriesSchemes", " sileo , zbra "));
        assertEquals(1, java.util.Collections.frequency(got, "sileo"));
        assertEquals(1, java.util.Collections.frequency(got, "zbra"));
    }

    /// A project that declares the array through ios.plistInject owns it. The plist
    /// renderer emits its own LSApplicationQueriesSchemes key for this hint without
    /// reading the injected fragment, so setting the hint as well would put the key
    /// in the plist twice -- and a plist with a duplicate key is not one that
    /// reliably keeps either value.
    @Test
    void anInjectedArrayIsLeftAlone() throws Exception {
        BuildRequest r = request("ios.plistInject",
                "<key>LSApplicationQueriesSchemes</key><array>"
                + "<string>cydia</string><string>sileo</string></array>");
        assertTrue(declare(r).isEmpty());
        assertFalse(r.getArg("ios.applicationQueriesSchemes", "").contains("filza"));
    }

    /// A fragment that merely MENTIONS the key -- in a comment, or inside an
    /// unrelated string -- declares nothing. Reading that as a declaration would
    /// skip the hint and ship a plist with no schemes at all.
    @Test
    void aCommentMentioningTheKeyIsNotADeclaration() throws Exception {
        BuildRequest r = request("ios.plistInject",
                "<!-- we set LSApplicationQueriesSchemes elsewhere -->"
                + "<key>UIFileSharingEnabled</key><true/>");
        assertTrue(declare(r).containsAll(
                Arrays.asList(IPhoneBuilder.JAILBREAK_QUERY_SCHEMES)));
    }

    // ------------------------------------------------------------------
    // The cap
    // ------------------------------------------------------------------

    /// iOS honours at most 50 LSApplicationQueriesSchemes entries for an app linked
    /// against the iOS 15 SDK or later, and 25 from the iOS 27 SDK. Past that,
    /// canOpenURL: answers false -- so appending blindly would not help this probe and
    /// would push the project's own schemes into the ignored region.
    @Test
    void ourSchemesGiveWayRatherThanPushTheProjectPastTheCap() throws Exception {
        BuildRequest r = request("ios.applicationQueriesSchemes", ownSchemes(50));
        List<String> got = declare(r, 26);
        assertEquals(50, got.size());
        assertFalse(got.contains("sileo"));
        // The project keeps every one of its own.
        assertTrue(got.contains("app0"));
        assertTrue(got.contains("app49"));
    }

    @Test
    void theCapTightensForTheIos27Sdk() throws Exception {
        assertTrue(declare(request("ios.applicationQueriesSchemes", ownSchemes(24)), 26)
                .contains("filza"));
        List<String> got = declare(request("ios.applicationQueriesSchemes", ownSchemes(24)), 27);
        assertEquals(25, got.size());
        assertTrue(got.contains("cydia"));
        assertFalse(got.contains("filza"));
    }

    /// Partial room is used, not discarded: the first entries fit and the rest are
    /// reported. Priority order matters here -- cydia and sileo come first.
    @Test
    void asManyAsFitAreAdded() throws Exception {
        List<String> got = declare(request("ios.applicationQueriesSchemes", ownSchemes(48)), 26);
        assertEquals(50, got.size());
        assertTrue(got.contains("cydia"));
        assertTrue(got.contains("sileo"));
        assertFalse(got.contains("zbra"));
    }

    /// A project already over the cap on its own is left exactly as it is.
    @Test
    void aProjectAlreadyOverTheCapIsNotTouched() throws Exception {
        BuildRequest r = request("ios.applicationQueriesSchemes", ownSchemes(60));
        assertEquals(60, declare(r, 26).size());
    }
}
