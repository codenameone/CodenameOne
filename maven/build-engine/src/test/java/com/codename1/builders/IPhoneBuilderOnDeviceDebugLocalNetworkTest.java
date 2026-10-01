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

/// Deciding whether an on-device-debug proxy host is the local network.
///
/// The on-device-debug app dials OUT to a proxy on the developer's machine.
/// For the native simulator that is the host's own loopback and no privacy
/// declaration applies; for a physical iPhone it is an address on the Wi-Fi
/// the two share, which since iOS 14 is consent-gated local-network access
/// and terminates an app that reaches it with no purpose string. The build
/// injects `NSLocalNetworkUsageDescription` for the second case and not the
/// first, so this predicate is what decides whether a debugging session on a
/// real device can connect at all.
///
/// The asymmetry is deliberate and is what the cases below pin: anything not
/// recognisably loopback is treated as the local network. A spare purpose
/// string costs one prompt in a build that is debug-only by construction; a
/// missing one costs a session that cannot start, with the proxy still waiting
/// and nothing on the device to say why.
class IPhoneBuilderOnDeviceDebugLocalNetworkTest {

    @Test
    void loopbackNeedsNoLocalNetworkDeclaration() {
        assertTrue(IPhoneBuilder.isLoopbackDebugProxyHost("127.0.0.1"),
                "the simulator's default proxy host is loopback");
        assertTrue(IPhoneBuilder.isLoopbackDebugProxyHost("localhost"),
                "the name for it is loopback too");
        assertTrue(IPhoneBuilder.isLoopbackDebugProxyHost("LOCALHOST"),
                "a host name is not case sensitive");
        assertTrue(IPhoneBuilder.isLoopbackDebugProxyHost("  127.0.0.1  "),
                "a hint value carries whatever spacing the properties file had");
        // Loopback is 127.0.0.1 by convention and the whole 127/8 block by rule,
        // and a developer who binds a proxy to another address in it is still on
        // loopback.
        assertTrue(IPhoneBuilder.isLoopbackDebugProxyHost("127.0.0.53"),
                "the whole 127/8 block is loopback, not just 127.0.0.1");
        assertTrue(IPhoneBuilder.isLoopbackDebugProxyHost("::1"),
                "IPv6 loopback");
        assertTrue(IPhoneBuilder.isLoopbackDebugProxyHost("[::1]"),
                "a literal IPv6 address in a host position is bracketed");
    }

    @Test
    void aLanAddressIsTheLocalNetwork() {
        assertFalse(IPhoneBuilder.isLoopbackDebugProxyHost("192.168.1.42"),
                "the address a physical iPhone has to dial");
        assertFalse(IPhoneBuilder.isLoopbackDebugProxyHost("10.0.0.7"), "a LAN address");
        assertFalse(IPhoneBuilder.isLoopbackDebugProxyHost("172.16.4.9"), "a LAN address");
        assertFalse(IPhoneBuilder.isLoopbackDebugProxyHost("my-laptop.local"),
                "a Bonjour name resolves on the local network, which is the point");
    }

    @Test
    void onlyAnAddressCountsAsLoopbackNotAnythingSpelledLikeOne() {
        // A host NAME may begin with digits, so "starts with 127." is not the
        // question -- these all resolve wherever DNS says, which is not loopback,
        // and reading them as loopback would withhold the declaration from a real
        // device.
        assertFalse(IPhoneBuilder.isLoopbackDebugProxyHost("127.0.0.1.example.com"),
                "a name that merely begins with the loopback address is not it");
        assertFalse(IPhoneBuilder.isLoopbackDebugProxyHost("127.example.com"),
                "nor is a name whose first label is 127");
        assertFalse(IPhoneBuilder.isLoopbackDebugProxyHost("127.0.0"),
                "a truncated address is not an address");
        assertFalse(IPhoneBuilder.isLoopbackDebugProxyHost("127.0.0.256"),
                "nor is one whose octet is out of range");
        assertFalse(IPhoneBuilder.isLoopbackDebugProxyHost("127.0.0.x"),
                "nor is one that is not numeric");
    }

    @Test
    void anAbsentHostIsTreatedAsTheLocalNetwork() {
        // Never reached through the build, which defaults the hint to 127.0.0.1,
        // but the direction matters if it ever is: unknown resolves towards
        // declaring, because that is the answer that keeps a session working.
        assertFalse(IPhoneBuilder.isLoopbackDebugProxyHost(null),
                "an unknown host must not be assumed to be loopback");
        assertFalse(IPhoneBuilder.isLoopbackDebugProxyHost(""),
                "nor an empty one");
    }
}
