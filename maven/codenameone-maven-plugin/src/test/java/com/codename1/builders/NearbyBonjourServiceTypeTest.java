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
        assertEquals("chat", IPhoneBuilder.bonjourServiceType(
                request("com.example.app", "chat")));
    }

    @Test
    void aReverseDnsPackageIsFoldedRatherThanRejected() {
        // Legal on Android and illegal here, which is exactly the case a
        // cross-platform app hits by writing the obvious thing.
        String type = IPhoneBuilder.bonjourServiceType(
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
        String type = IPhoneBuilder.bonjourServiceType(
                request("com.example.someverylongapplicationname", null));
        assertLegal(type);
        assertEquals(15, type.length());
    }

    @Test
    void aTruncationThatLandsOnAHyphenDoesNotLeaveOne() {
        // "ab.cdefghijklm.x" folds to "ab-cdefghijklm-" at fifteen, and a
        // trailing hyphen is one of the things that makes the framework raise.
        String type = IPhoneBuilder.bonjourServiceType(
                request("ab.cdefghijklm.x", null));
        assertLegal(type);
    }

    @Test
    void runsOfIllegalCharactersCollapseToOneHyphen() {
        String type = IPhoneBuilder.bonjourServiceType(
                request("com...example___app", null));
        assertLegal(type);
        assertEquals("com-example-app", type);
    }

    @Test
    void uppercaseIsLowered() {
        assertEquals("mychat", IPhoneBuilder.bonjourServiceType(
                request("com.example.app", "MyChat")));
    }

    @Test
    void somethingWithNoUsableCharactersFallsBackRatherThanRaising() {
        assertEquals("cn1-nearby", IPhoneBuilder.bonjourServiceType(
                request("...", null)));
        assertEquals("cn1-nearby", IPhoneBuilder.bonjourServiceType(
                request(null, null)));
    }

    @Test
    void ablankHintFallsBackToThePackageRatherThanToTheDefault() {
        assertEquals("com-example-app", IPhoneBuilder.bonjourServiceType(
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
            assertLegal(IPhoneBuilder.bonjourServiceType(request(in, null)));
            assertLegal(IPhoneBuilder.bonjourServiceType(request("p", in)));
        }
    }
}
