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
package com.codename1.documents;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

/// What setRemoteEndpoint refuses.
///
/// Only the refusals are exercised here: they are decided before the port bridge is asked for,
/// so they need no platform behind them, while accepting an endpoint ends in a bridge this test
/// has no way to provide.
public class DocumentProviderEndpointTest {

    @Test
    void refusesAnEndpointThatIsNotHttps() {
        // The readers send the bearer token as an Authorization header, from outside the app, so
        // an "http://" typo hands the token and the documents it unlocks to the network in the
        // clear wherever cleartext is still allowed.
        assertThrows(IllegalArgumentException.class,
                () -> DocumentProvider.setRemoteEndpoint("http://example.com/docs", "token"));
    }

    @Test
    void refusesCredentialsThatCannotBeWrittenAsUtf8() {
        // Both values are stored as UTF-8 by every bridge. An unpaired surrogate becomes "?", so
        // the endpoint read back is a different URL and the token sent is a different credential.
        assertThrows(IllegalArgumentException.class,
                () -> DocumentProvider.setRemoteEndpoint("https://example.com/\ud800/docs", null));
        assertThrows(IllegalArgumentException.class,
                () -> DocumentProvider.setRemoteEndpoint("https://example.com/docs",
                        "tok\udc00en"));
    }
}
