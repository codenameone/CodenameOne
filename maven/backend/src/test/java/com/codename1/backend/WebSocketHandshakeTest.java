/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.backend;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.security.MessageDigest;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The opening handshake.
 *
 * The accept value is cross-checked against the JDK's own SHA-1 and base64, which
 * is a check this module can make and the translated arm cannot -- there is no
 * MessageDigest there. That is the point of putting it here: if
 * {@link Crypto#sha1} or {@link Base64} ever disagrees with the rest of the world,
 * this is where it shows up, rather than as a handshake every browser rejects.
 */
class WebSocketHandshakeTest {

    @Test
    @DisplayName("RFC 6455 section 1.3's own worked example")
    void rfcVector() {
        assertEquals("s3pPLMBiTxaQ9kYGzzhZRbK+xOo=",
                WebSocketHandshake.accept("dGhlIHNhbXBsZSBub25jZQ=="));
    }

    @Test
    @DisplayName("the accept value agrees with the JDK for arbitrary keys")
    void agreesWithTheJdk() throws Exception {
        Random random = new Random(20260921L);
        for(int trial = 0 ; trial < 500 ; trial++) {
            byte[] nonce = new byte[16];
            random.nextBytes(nonce);
            String key = java.util.Base64.getEncoder().encodeToString(nonce);
            byte[] digest = MessageDigest.getInstance("SHA-1")
                    .digest((key + WebSocketHandshake.GUID).getBytes("ISO-8859-1"));
            assertEquals(java.util.Base64.getEncoder().encodeToString(digest),
                    WebSocketHandshake.accept(key), "for key " + key);
            assertTrue(WebSocketHandshake.keyIsWellFormed(key));
        }
    }

    @Test
    @DisplayName("a key that is not sixteen base64 bytes is refused")
    void malformedKeys() {
        // The accept value is computed either way, so a malformed key otherwise
        // produces a well-formed 101 for a handshake the peer thinks failed.
        assertFalse(WebSocketHandshake.keyIsWellFormed(null));
        assertFalse(WebSocketHandshake.keyIsWellFormed(""));
        assertFalse(WebSocketHandshake.keyIsWellFormed("abc"));
        assertFalse(WebSocketHandshake.keyIsWellFormed("dGhlIHNhbXBsZSBub25jZQ"), "unpadded");
        assertFalse(WebSocketHandshake.keyIsWellFormed("!!!!!!!!!!!!!!!!!!!!!!!!"), "not base64");
        assertFalse(WebSocketHandshake.keyIsWellFormed(
                java.util.Base64.getEncoder().encodeToString(new byte[15])), "fifteen bytes");
        assertFalse(WebSocketHandshake.keyIsWellFormed(
                java.util.Base64.getEncoder().encodeToString(new byte[17])), "seventeen bytes");
        assertTrue(WebSocketHandshake.keyIsWellFormed(
                java.util.Base64.getEncoder().encodeToString(new byte[16])));
    }

    @Test
    @DisplayName("the server's preference order decides, not the client's")
    void subprotocolPrefersTheServerOrder() {
        String[] supported = {"v2.chat", "v1.chat"};
        // The client lists v1 first; taking the client's order would let a peer
        // select a deprecated protocol over a current one by listing it first.
        assertEquals("v2.chat", WebSocketHandshake.selectSubprotocol("v1.chat, v2.chat", supported));
        assertEquals("v1.chat", WebSocketHandshake.selectSubprotocol("v1.chat", supported));
    }

    @Test
    @DisplayName("no overlap answers null, which is a 101 with no header rather than a refusal")
    void subprotocolNoOverlap() {
        String[] supported = {"v1.chat"};
        assertNull(WebSocketHandshake.selectSubprotocol("soap, wamp", supported));
        assertNull(WebSocketHandshake.selectSubprotocol(null, supported));
        assertNull(WebSocketHandshake.selectSubprotocol("v1.chat", new String[0]));
        assertNull(WebSocketHandshake.selectSubprotocol("v1.chat", null));
    }

    @Test
    @DisplayName("subprotocol names are case sensitive, unlike every other list here")
    void subprotocolIsCaseSensitive() {
        assertNull(WebSocketHandshake.selectSubprotocol("V1.CHAT", new String[]{"v1.chat"}));
        assertEquals("v1.chat", WebSocketHandshake.selectSubprotocol("v1.chat", new String[]{"v1.chat"}));
    }

    @Test
    @DisplayName("a whole token, never a substring, and surrounding space is ignored")
    void subprotocolTokenisation() {
        assertEquals("v1.chat",
                WebSocketHandshake.selectSubprotocol("  v1.chat  ", new String[]{"v1.chat"}));
        assertEquals("v1.chat",
                WebSocketHandshake.selectSubprotocol("a,\tv1.chat , b", new String[]{"v1.chat"}));
        assertNull(WebSocketHandshake.selectSubprotocol("xv1.chaty", new String[]{"v1.chat"}));
        assertNull(WebSocketHandshake.selectSubprotocol("v1.cha", new String[]{"v1.chat"}));
    }

    @Test
    @DisplayName("nothing that could start a new header line can be selected")
    void headerInjectionIsImpossible() {
        // The 101 is not written by writeHeadAndBody, so it does not pass through
        // the response-splitting guard the rest of the server has. This is what
        // stands in for it.
        assertFalse(WebSocketHandshake.isToken("x\r\nSet-Cookie: admin=1"));
        assertFalse(WebSocketHandshake.isToken("a b"));
        assertFalse(WebSocketHandshake.isToken("a:b"));
        assertFalse(WebSocketHandshake.isToken("a\nb"));
        assertFalse(WebSocketHandshake.isToken("a\rb"));
        assertFalse(WebSocketHandshake.isToken(""));
        assertFalse(WebSocketHandshake.isToken(null));
        assertTrue(WebSocketHandshake.isToken("graphql-transport-ws"));
        assertTrue(WebSocketHandshake.isToken("v1.chat"));

        // Even if the server's own list holds something unsafe -- a typo in an
        // application's configuration -- it is never echoed.
        assertNull(WebSocketHandshake.selectSubprotocol("bad\r\nx", new String[]{"bad\r\nx"}));
    }
}
