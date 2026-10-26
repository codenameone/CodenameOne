/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.bastion.net;

import com.codenameone.examples.bastion.AppConfig;
import com.codename1.io.ConnectionRequest;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/// What the network guard lets through.
class GuardTest {
    @Test
    void cleartextAndUnlistedHostsAreRefused() {
        withDevelopment(() -> {
            assertNull(Guard.refusal("http://localhost:8080/api/me"));
            assertNull(Guard.refusal("http://127.0.0.1:8080/login"));
        });
        assertNull(Guard.refusal("https://api.bastion.example/api/me"));
        assertNull(Guard.refusal("https://API.Bastion.Example:443/api/me"));
        assertNotNull(Guard.refusal("http://api.bastion.example/api/me"));
        assertNotNull(Guard.refusal("https://evil.example/api/me"));
        // The user-info trick: the host is what follows the '@'.
        assertNotNull(Guard.refusal("https://api.bastion.example@evil.example/"));
        assertNotNull(Guard.refusal("https://localhost.evil.example/"));
        assertNotNull(Guard.refusal("ftp://api.bastion.example/"));
    }

    @Test
    void hostOf() {
        assertEquals("api.bastion.example", AppConfig.hostOf("https://api.bastion.example/x?y"));
        assertEquals("[::1]", AppConfig.hostOf("http://[::1]:8080/"));
        assertNull(AppConfig.hostOf("javascript:alert(1)"));
    }

    /// The host is whatever follows the LAST '@' of the authority: user info
    /// can carry a ':' and look like a host and port of its own.
    @Test
    void userInfoCannotPassForTheHost() {
        assertEquals("evil.example", AppConfig.hostOf("http://localhost:1@evil.example/"));
        assertEquals("evil.example", AppConfig.hostOf("http://localhost@evil.example:8080/x"));
        assertEquals("evil.example", AppConfig.hostOf("https://a@b@evil.example/"));
        // A backslash ends the authority, as a browser reads it.
        assertEquals("localhost", AppConfig.hostOf("http://localhost\\@evil.example/"));
        assertEquals("localhost", AppConfig.hostOf("http://u:p@localhost:8080/?next=@evil.example"));
        assertEquals("[::1]", AppConfig.hostOf("http://user@[::1]:8080/"));
        assertNull(AppConfig.hostOf("http://@/"));
        withDevelopment(() -> {
            assertNotNull(Guard.refusal("http://localhost:1@evil.example/"));
            assertNotNull(Guard.refusal("http://127.0.0.1:8080@evil.example/api"));
        });
    }

    /// The development exemption exists only in a development run: an
    /// installed app handed a loopback address does not get cleartext.
    @Test
    void cleartextToLoopbackOnlyInDevelopment() {
        String was = System.clearProperty("bastion.server.url");
        try {
            assertFalse(AppConfig.developmentAllowed());
            assertNotNull(Guard.refusal("http://localhost:8080/api/me"));
            assertNotNull(Guard.refusal("http://10.0.2.2:8080/api/me"));
            assertEquals(AppConfig.RELEASE_SERVER, AppConfig.serverUrl());
        } finally {
            if (was != null) {
                System.setProperty("bastion.server.url", was);
            }
        }
        withDevelopment(() -> {
            assertNull(Guard.refusal("http://localhost:8080/api/me"));
            // The emulator's alias for the host is another machine.
            assertNotNull(Guard.refusal("http://10.0.2.2:8080/api/me"));
        });
    }

    /// A pinned host that presents no chain is refused, not let through.
    @Test
    void anEmptyChainFailsClosedForAPinnedHost() {
        ConnectionRequest request = new ConnectionRequest();
        request.setUrl("https://api.bastion.example/api/me");
        Guard guard = new Guard();
        assertThrows(IOException.class, () -> guard.checkCertificates(request,
                new ConnectionRequest.SSLCertificate[0]));
        assertThrows(IOException.class, () -> guard.checkCertificates(request, null));
    }

    private static void withDevelopment(Runnable check) {
        String was = System.getProperty("bastion.server.url");
        System.setProperty("bastion.server.url", "http://localhost:8080");
        try {
            check.run();
        } finally {
            if (was == null) {
                System.clearProperty("bastion.server.url");
            } else {
                System.setProperty("bastion.server.url", was);
            }
        }
    }
}
