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

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Bonjour service type MultipeerConnectivity registers under.
 *
 * <p>This is a parity test in disguise. The same fold is implemented twice --
 * here, to write {@code NSBonjourServices} into the Info.plist, and in
 * {@code cn1nbServiceType} in CN1Nearby.m, to register the service at runtime
 * -- and iOS refuses a browse whose registered type is not one the plist
 * declared. The refusal is a silent "no peers found" rather than an error, so
 * a divergence between the two would present as a transport that simply never
 * works on iOS with nothing in any log to explain it.</p>
 *
 * <p>The rule being enforced: 1 to 15 characters, lowercase ASCII letters,
 * digits and hyphens, no leading or trailing hyphen and no two adjacent.
 * MultipeerConnectivity raises on anything else, which on a device is a crash
 * rather than an error an app can show.</p>
 */
class NearbyBonjourServiceTypeTest {

    private static BuildRequest request(String packageName, String hint) {
        BuildRequest r = new BuildRequest();
        r.setPackageName(packageName);
        if (hint != null) {
            r.putArgument("ios.nearby.serviceType", hint);
        }
        return r;
    }

    /** The single folded type, asserting the hint produced exactly one. */
    private static String only(BuildRequest request) {
        List<String> all = IPhoneBuilder.bonjourServiceTypes(request);
        assertEquals(1, all.size(), "expected one service type, got " + all);
        return all.get(0);
    }

    private static void assertLegal(String type) {
        assertTrue(type.length() >= 1 && type.length() <= 15,
                "1 to 15 characters, got " + type.length() + " in " + type);
        assertTrue(type.matches("[a-z0-9-]+"),
                "lowercase letters, digits and hyphens only: " + type);
        assertTrue(!type.startsWith("-") && !type.endsWith("-"),
                "no leading or trailing hyphen: " + type);
        assertTrue(type.indexOf("--") < 0, "no adjacent hyphens: " + type);
    }

    @Test
    void anExplicitHintIsUsedAsGiven() {
        assertEquals("chat", only(
                request("com.example.app", "chat")));
    }

    @Test
    void aReverseDnsPackageIsFoldedRatherThanRejected() {
        // Legal on Android and illegal here, which is exactly the case a
        // cross-platform app hits by writing the obvious thing.
        String type = only(
                request("com.example.chat", null));
        assertLegal(type);
        // Sixteen characters folded, fifteen allowed -- so even this
        // unremarkable package name is truncated, which is why the builder
        // logs the derived type and the guide tells you to set
        // ios.nearby.serviceType yourself.
        assertEquals("com-example-cha", type);
    }

    @Test
    void anOverlongPackageIsTruncatedToTheLimit() {
        String type = only(
                request("com.example.someverylongapplicationname", null));
        assertLegal(type);
        assertEquals(15, type.length());
    }

    @Test
    void aTruncationThatLandsOnAHyphenDoesNotLeaveOne() {
        // "ab.cdefghijklm.x" folds to "ab-cdefghijklm-" at fifteen, and a
        // trailing hyphen is one of the things that makes the framework raise.
        String type = only(
                request("ab.cdefghijklm.x", null));
        assertLegal(type);
    }

    @Test
    void runsOfIllegalCharactersCollapseToOneHyphen() {
        String type = only(
                request("com...example___app", null));
        assertLegal(type);
        assertEquals("com-example-app", type);
    }

    @Test
    void uppercaseIsLowered() {
        assertEquals("mychat", only(
                request("com.example.app", "MyChat")));
    }

    @Test
    void somethingWithNoUsableCharactersFallsBackRatherThanRaising() {
        assertEquals("cn1-nearby", only(
                request("...", null)));
        assertEquals("cn1-nearby", only(
                request(null, null)));
    }

    @Test
    void ablankHintFallsBackToThePackageRatherThanToTheDefault() {
        assertEquals("com-example-app", only(
                request("com.example.app", "   ")));
    }

    @Test
    void everyFoldIsLegal() {
        String[] inputs = {
            "a", "A", "com.example.app", "-leading", "trailing-",
            "com.example.a-very-long-name-indeed", "1.2.3", "_", "--",
            "MiXeD.CaSe.Name", "x.y", "com.example.APP"
        };
        for (String in : inputs) {
            assertLegal(only(request(in, null)));
            assertLegal(only(request("p", in)));
        }
    }

    @Test
    void aCommaSeparatedHintDeclaresEveryServiceTheAppUses() {
        // The point of the list: iOS browses only what the plist declared, and
        // the build cannot see the strings an app passes to startAdvertising.
        List<String> types = IPhoneBuilder.bonjourServiceTypes(
                request("com.example.app", "chat, files , telemetry"));
        assertEquals(3, types.size());
        assertEquals("chat", types.get(0));
        assertEquals("files", types.get(1));
        assertEquals("telemetry", types.get(2));
        for (String t : types) {
            assertLegal(t);
        }
    }

    @Test
    void idsThatFoldToTheSameTypeAreDeclaredOnce() {
        List<String> types = IPhoneBuilder.bonjourServiceTypes(
                request("com.example.app", "chat,chat,Chat"));
        assertEquals(1, types.size());
        assertEquals("chat", types.get(0));
    }

    @Test
    void theFoldIsTheSameOneTheRuntimeApplies() {
        // CN1Nearby.m folds the service id an app passes at runtime and then
        // checks the result against NSBonjourServices. If these two folds ever
        // disagree the app browses a type the plist does not declare, and iOS
        // answers with silence rather than an error -- so the build-side fold
        // is exposed on its own and pinned here.
        assertEquals("chat", IPhoneBuilder.foldBonjourServiceType("chat"));
        assertEquals("com-example-cha",
                IPhoneBuilder.foldBonjourServiceType("com.example.chat"));
        assertEquals("mychat", IPhoneBuilder.foldBonjourServiceType("MyChat"));
        assertEquals("", IPhoneBuilder.foldBonjourServiceType("..."));
        assertEquals("", IPhoneBuilder.foldBonjourServiceType(null));
    }

    @Test
    void everyDeclaredTypeIsLegalWhateverTheHintSays() {
        String[] hints = {
            "a,b,c", "com.example.app,chat", "-,--,x", "A,B",
            "com.example.a-very-long-name-indeed,y", ",,,", "1.2.3"
        };
        for (String hint : hints) {
            List<String> types = IPhoneBuilder.bonjourServiceTypes(
                    request("com.example.app", hint));
            assertTrue(!types.isEmpty(), "never empty for hint " + hint);
            for (String t : types) {
                assertLegal(t);
            }
        }
    }

    @Test
    void anAllDigitIdIsGivenALetterRatherThanLeftIllegal() {
        // Apple requires at least one ASCII LETTER, not merely one legal
        // character. "123" folded to "123", which reads as legal and makes
        // MCNearbyServiceAdvertiser raise rather than fail -- so the app
        // crashed instead of failing to advertise.
        String folded = IPhoneBuilder.foldBonjourServiceType("123");
        assertTrue(hasLetter(folded), folded);
        assertTrue(folded.contains("123"), folded);
        assertTrue(folded.length() <= 15, folded);
    }

    @Test
    void aDigitsAndPunctuationIdAlsoGetsALetter() {
        String folded = IPhoneBuilder.foldBonjourServiceType("12.34.56");
        assertTrue(hasLetter(folded), folded);
        assertTrue(folded.length() <= 15, folded);
        assertFalse(folded.startsWith("-"), folded);
        assertFalse(folded.endsWith("-"), folded);
    }

    @Test
    void anIdThatAlreadyHasALetterIsNotPrefixed() {
        assertEquals("chat", IPhoneBuilder.foldBonjourServiceType("chat"));
        assertEquals("a1", IPhoneBuilder.foldBonjourServiceType("a1"));
    }

    private static boolean hasLetter(String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= 'a' && c <= 'z') {
                return true;
            }
        }
        return false;
    }
}
