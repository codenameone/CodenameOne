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
        Method m = IPhoneBuilder.class.getDeclaredMethod(
                "declareApplicationQueriesSchemes", BuildRequest.class,
                String[].class, String.class);
        m.setAccessible(true);
        m.invoke(new IPhoneBuilder(), r, IPhoneBuilder.JAILBREAK_QUERY_SCHEMES, "why");
        String hint = r.getArg("ios.applicationQueriesSchemes", "");
        return hint.length() == 0 ? java.util.Collections.<String>emptyList()
                : Arrays.asList(hint.split(","));
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
}
