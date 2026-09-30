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

import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Switching one preprocessor define must never switch another whose name it prefixes.
 *
 * <p>CN1Crypto.h carries {@code //#define CN1_INCLUDE_CRYPTO} and
 * {@code //#define CN1_INCLUDE_CRYPTO_GCM} on consecutive lines, and the builder turned the first
 * on with a plain {@code String.replace}. That uncommented both, so every application that used
 * any crypto API linked CommonCrypto's private AES-GCM SPI whatever {@code ios.crypto.gcm} said,
 * and App Store Connect rejected the upload for non-public symbols.
 */
public class ExecutorDefineMarkerTest {

    private static final String CRYPTO_HEADER =
            "//#define CN1_INCLUDE_CRYPTO\n//#define CN1_INCLUDE_CRYPTO_GCM\n";

    @Test
    public void enablingADefineLeavesTheLongerNameAlone() {
        String out = Executor.replaceMarker(CRYPTO_HEADER,
                "//#define CN1_INCLUDE_CRYPTO", "#define CN1_INCLUDE_CRYPTO");
        assertEquals("#define CN1_INCLUDE_CRYPTO\n//#define CN1_INCLUDE_CRYPTO_GCM\n", out);
    }

    @Test
    public void theLongerNameIsStillReachableByItsOwnMarker() {
        String out = Executor.replaceMarker(CRYPTO_HEADER,
                "//#define CN1_INCLUDE_CRYPTO_GCM", "#define CN1_INCLUDE_CRYPTO_GCM");
        assertEquals("//#define CN1_INCLUDE_CRYPTO\n#define CN1_INCLUDE_CRYPTO_GCM\n", out);
    }

    @Test
    public void disablingADefineLeavesTheLongerNameAlone() {
        // The push toggles run the other way, turning a live define off.
        String out = Executor.replaceMarker("#define INCLUDE_CN1_PUSH\n#define INCLUDE_CN1_PUSH2\n",
                "#define INCLUDE_CN1_PUSH", "");
        assertEquals("\n#define INCLUDE_CN1_PUSH2\n", out);
    }

    @Test
    public void aNameFollowedByAValueOrCommentStillMatches() {
        assertEquals("#define CN1_X 1\n",
                Executor.replaceMarker("//#define CN1_X 1\n", "//#define CN1_X", "#define CN1_X"));
        assertEquals("#define CN1_X// on\r\n",
                Executor.replaceMarker("//#define CN1_X// on\r\n", "//#define CN1_X", "#define CN1_X"));
    }

    @Test
    public void markersThatAreNotDefinesKeepPlainReplacement() {
        // Placeholders and code fragments are substituted exactly as String.replace would.
        assertEquals("aXYZb", Executor.replaceMarker("a@PH@b", "@PH@", "XYZ"));
        assertEquals("//ONE_TWO", Executor.replaceMarker("//ONE_TWO", "//ONE", "//ONE"));
        assertEquals("$1\\x", Executor.replaceMarker("#define A", "#define A", "$1\\x"));
    }

    @Test
    public void theShippedCryptoHeaderEnablesOnlyWhatWasAsked() throws Exception {
        File header = new File("../../Ports/iOSPort/nativeSources/CN1Crypto.h");
        if (!header.isFile()) {
            // The plugin can be built from a source bundle without the ports beside it.
            return;
        }
        String text = new String(Files.readAllBytes(header.toPath()), StandardCharsets.UTF_8);
        assertTrue("CN1Crypto.h no longer carries the GCM switch this test guards",
                text.contains("//#define CN1_INCLUDE_CRYPTO_GCM"));
        String out = Executor.replaceMarker(text,
                "//#define CN1_INCLUDE_CRYPTO", "#define CN1_INCLUDE_CRYPTO");
        assertTrue(out.contains("//#define CN1_INCLUDE_CRYPTO_GCM"));
        assertFalse(out.contains("//#define CN1_INCLUDE_CRYPTO\n"));
    }
}
