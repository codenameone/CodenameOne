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

import java.io.File;
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
                .contains("cydia"));
        List<String> got = declare(request("ios.applicationQueriesSchemes", ownSchemes(24)), 27);
        assertEquals(25, got.size());
        assertFalse(got.contains("cydia"));
    }

    /// The single most important property of the order. An app with one slot left must
    /// spend it on the rootless package manager, because rootless is the case that goes
    /// undetected without it. Cydia held this position once, which meant the narrowest
    /// builds kept the obsolete rootful probe and dropped the one this change exists for.
    @Test
    void sileoSurvivesWhenOnlyOneSlotIsLeft() throws Exception {
        List<String> got = declare(request("ios.applicationQueriesSchemes", ownSchemes(24)), 27);
        assertTrue(got.contains("sileo"));
    }

    /// Partial room is used, not discarded: the entries at the front fit and the rest
    /// are reported.
    @Test
    void asManyAsFitAreAdded() throws Exception {
        List<String> got = declare(request("ios.applicationQueriesSchemes", ownSchemes(48)), 26);
        assertEquals(50, got.size());
        assertTrue(got.contains("sileo"));
        assertTrue(got.contains("filza"));
        assertFalse(got.contains("cydia"));
    }

    /// Ours are declared LAST, after every other scheme the builder adds.
    ///
    /// Not a style preference. iOS honours a limited number of entries and ignores the
    /// rest, so whoever claims a slot last is whoever loses it. These schemes are the
    /// lowest priority thing in the array -- a secondary security probe -- while the
    /// others are features the app asked for. Declared beside the jailbreak header, this
    /// ran before the Smart Home block appended com.apple.Home, so a project at the cap
    /// got sileo as its last honoured entry and com.apple.Home as an ignored one, and
    /// SmartHome.openEcosystemApp() then reported Home missing on a device that had it.
    ///
    /// Checked against the source because the invariant is an ordering inside one long
    /// method, which no amount of unit testing the merge itself can pin.
    @Test
    void ourSchemesAreDeclaredAfterEveryOtherSchemeAddition() throws Exception {
        File f = new File("src/main/java/com/codename1/builders/IPhoneBuilder.java");
        assertTrue(f.exists(), "builder source must be readable: " + f.getAbsolutePath());
        String src = new String(java.nio.file.Files.readAllBytes(f.toPath()),
                java.nio.charset.StandardCharsets.UTF_8);

        int ours = src.indexOf(
                "declareApplicationQueriesSchemes(request, JAILBREAK_QUERY_SCHEMES");
        assertTrue(ours > 0, "the jailbreak schemes must be declared somewhere");
        int render = src.indexOf("injectToPlist(tmpFile, resDir, request)");
        assertTrue(ours < render, "declared before the plist is rendered");

        // Every other writer of the hint has to have had its say already. The one
        // inside declareApplicationQueriesSchemes itself is this merge writing its own
        // result, so it is not a competitor.
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(
                "putArgument\\(\"ios\\.applicationQueriesSchemes\"[,\\s]*([^;]*);")
                .matcher(src);
        int others = 0;
        while (m.find()) {
            if (m.group(1).contains("joined.toString()")) {
                continue;
            }
            others++;
            assertTrue(m.start() < ours, "a scheme added at offset " + m.start()
                    + " claims its slot after the jailbreak schemes at " + ours
                    + "; move it above that call or it is the one iOS drops");
        }
        assertTrue(others > 0, "expected to find the Smart Home addition to compare against");
    }

    /// The builder declares the schemes and IOSImplementation probes them, from two
    /// hand-maintained lists. They have to carry the same schemes in the same order:
    /// only as many as fit are declared, taken from the front, so a probe whose scheme
    /// sits further down the runtime list than the builder's is asking about something
    /// that was never declared -- and canOpenURL: answers false for that, which reads
    /// as a clean device.
    @Test
    void theRuntimeProbeListMatchesTheDeclaredOne() throws Exception {
        File f = new File("../../Ports/iOSPort/src/com/codename1/impl/ios/"
                + "IOSImplementation.java");
        assertTrue(f.exists(), "the iOS port must be readable: " + f.getAbsolutePath());
        String src = new String(java.nio.file.Files.readAllBytes(f.toPath()),
                java.nio.charset.StandardCharsets.UTF_8);
        int at = src.indexOf("JAILBREAK_URL_SCHEMES = {");
        assertTrue(at > 0, "JAILBREAK_URL_SCHEMES not found in IOSImplementation");
        String body = src.substring(at, src.indexOf("};", at));
        java.util.List<String> runtime = new java.util.ArrayList<String>();
        java.util.regex.Matcher m =
                java.util.regex.Pattern.compile("\"([a-z0-9]+)://").matcher(body);
        while (m.find()) {
            runtime.add(m.group(1));
        }
        assertEquals(Arrays.asList(IPhoneBuilder.JAILBREAK_QUERY_SCHEMES), runtime);
    }

    /// A project already over the cap on its own is left exactly as it is.
    @Test
    void aProjectAlreadyOverTheCapIsNotTouched() throws Exception {
        BuildRequest r = request("ios.applicationQueriesSchemes", ownSchemes(60));
        assertEquals(60, declare(r, 26).size());
    }

    /// The plist renderer appends fbauth2 and gplus off these two hints AFTER this
    /// merge runs, so a ceiling that ignores them is one the renderer walks straight
    /// past -- and the app is told its schemes fit while shipping a plist where they
    /// do not.
    @Test
    void roomIsLeftForTheSchemesTheRendererAppends() throws Exception {
        List<String> plain = declare(request("ios.applicationQueriesSchemes", ownSchemes(48)), 26);
        assertEquals(50, plain.size());

        List<String> withFacebook = declare(request(
                "ios.applicationQueriesSchemes", ownSchemes(48),
                "facebook.appId", "12345"), 26);
        assertEquals(49, withFacebook.size());

        List<String> withBoth = declare(request(
                "ios.applicationQueriesSchemes", ownSchemes(48),
                "facebook.appId", "12345",
                "ios.gplus.clientId", "abc.apps.googleusercontent.com"), 26);
        assertEquals(48, withBoth.size());
    }
}
