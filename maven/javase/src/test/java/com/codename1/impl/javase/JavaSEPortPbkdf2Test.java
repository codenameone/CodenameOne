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
package com.codename1.impl.javase;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/// The port's PBKDF2, at the edges JCE does not handle the way the specification does.
///
/// This has to live beside the port. `core-unittests` runs against the base
/// `CodenameOneImplementation`, whose `pbkdf2` answers null, so the native path is never reached
/// there and a test written against `KdfProfile` passes whether or not this method is correct --
/// measured, not assumed.
public class JavaSEPortPbkdf2Test {

    private JavaSEPort originalInstance;

    private JavaSEPort port;

    @BeforeEach
    public void setUp() {
        // The constructor overwrites the global instance, as the other port tests here note.
        originalInstance = JavaSEPort.instance;
        port = new JavaSEPort();
    }

    @AfterEach
    public void tearDown() {
        JavaSEPort.instance = originalInstance;
    }

    @Test
    public void anEmptyPasswordIsDeclinedRatherThanThrown() {
        // javax.crypto.spec.SecretKeySpec rejects a zero-length key with
        // IllegalArgumentException("Empty key"). That is not a GeneralSecurityException, so it
        // walked straight out through this method's catch: inside the vault it killed the worker
        // thread without ever completing the AsyncResource the caller was waiting on, and callers
        // of the public Util.pbkdf2 got a raw throw from an API documented to answer null when it
        // cannot derive. Answering null is that documented contract, and it routes the caller to
        // the portable implementation, which zero-pads a short HMAC key the way RFC 2104 says to.
        assertNull(port.pbkdf2("SHA-256", new byte[0], "salt".getBytes(), 1000, 32),
                "an empty password must be declined, not thrown out of");
        assertNull(port.pbkdf2("SHA-256", null, "salt".getBytes(), 1000, 32),
                "a null password must be declined, not thrown out of");
    }

    @Test
    public void anOrdinaryPasswordStillDerivesTheRfc8018Answer() {
        // The guard above must not have cost the ordinary path. RFC 8018 vector, 4096 iterations.
        byte[] derived = port.pbkdf2("SHA-256", "password".getBytes(), "salt".getBytes(), 4096, 32);
        assertNotNull(derived, "the JavaSE port supplies a native derivation");
        assertEquals(32, derived.length);
        StringBuilder hex = new StringBuilder();
        for (int iter = 0; iter < derived.length; iter++) {
            int v = derived[iter] & 0xff;
            hex.append("0123456789abcdef".charAt(v >>> 4));
            hex.append("0123456789abcdef".charAt(v & 0x0f));
        }
        assertEquals("c5e478d59288c841aa530db6845c4c8d962893a001ce4e11a4963873aa98134a",
                hex.toString());
    }

    @Test
    public void anEmptySaltIsStillDerivable() {
        // The other zero-length input, which JCE does NOT object to -- pinned so the guard above
        // is not later widened into refusing a case that works.
        byte[] derived = port.pbkdf2("SHA-256", "password".getBytes(), new byte[0], 1000, 16);
        assertNotNull(derived);
        assertEquals(16, derived.length);
        assertArrayEquals(derived,
                port.pbkdf2("SHA-256", "password".getBytes(), new byte[0], 1000, 16));
    }
}
