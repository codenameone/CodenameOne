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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * When AES-GCM is compiled into an iOS application.
 *
 * <p>GCM used to call CommonCrypto's private SPI, so it was meant to be opt-in -- yet every
 * application using the crypto API got it anyway, because the builder's prefix-matching replace
 * of {@code //#define CN1_INCLUDE_CRYPTO} also uncommented the GCM line. Fixing that replace
 * silently turned AES-GCM off for those applications ("crypto operation failed with code -5").
 * With GCM now built from public calls it is part of the crypto API; an older port that still
 * calls the SPI keeps the opt-in rule.</p>
 */
public class CryptoGcmDecisionTest {

    private static final boolean PUBLIC = true;
    private static final boolean LEGACY = false;

    @Test
    public void publicGcmIsOnForEveryCryptoUser() {
        assertTrue(IPhoneBuilder.resolveCryptoGcm(PUBLIC, true, false, false, ""));
        assertTrue(IPhoneBuilder.resolveCryptoGcm(PUBLIC, true, true, false, ""));
        assertTrue(IPhoneBuilder.resolveCryptoGcm(PUBLIC, true, false, false, "true"));
    }

    @Test
    public void publicGcmCanStillBeTrimmed() {
        assertFalse(IPhoneBuilder.resolveCryptoGcm(PUBLIC, true, false, false, "false"));
    }

    @Test
    public void theVaultKeepsGcmOverAnExplicitFalse() {
        // An existing ios.crypto.gcm=false must not turn a working vault application into one
        // whose crypto fails once the port it builds against moves to public GCM.
        assertTrue(IPhoneBuilder.resolveCryptoGcm(PUBLIC, true, true, false, "false"));
        assertTrue(IPhoneBuilder.resolveCryptoGcm(LEGACY, true, true, false, "false"));
    }

    @Test
    public void noCryptoMeansNoGcmEitherWay() {
        assertFalse(IPhoneBuilder.resolveCryptoGcm(PUBLIC, false, false, false, "true"));
        assertFalse(IPhoneBuilder.resolveCryptoGcm(LEGACY, false, false, false, "true"));
    }

    @Test
    public void legacyGcmStaysOptIn() {
        assertFalse("private SPI must not be linked for a plain crypto user",
                IPhoneBuilder.resolveCryptoGcm(LEGACY, true, false, false, ""));
        assertTrue(IPhoneBuilder.resolveCryptoGcm(LEGACY, true, true, false, ""));
        assertTrue(IPhoneBuilder.resolveCryptoGcm(LEGACY, true, false, false, "true"));
        assertTrue(IPhoneBuilder.resolveCryptoGcm(LEGACY, true, false, true, ""));
        assertFalse(IPhoneBuilder.resolveCryptoGcm(LEGACY, true, false, true, "false"));
    }

    @Test
    public void anUnscannableVaultStillGetsWorkingCrypto() {
        // A library the permission scan never reads can be the only vault user; with public GCM
        // an unknown answer turns crypto (and so GCM) on rather than shipping the stubs.
        assertTrue(IPhoneBuilder.cryptoApiRequired(PUBLIC, false, false, true, ""));
        assertTrue(IPhoneBuilder.resolveCryptoGcm(PUBLIC, true, false, true, ""));
        // ios.crypto.gcm=false is the developer saying there is no vault.
        assertFalse(IPhoneBuilder.cryptoApiRequired(PUBLIC, false, false, true, "false"));
        // The legacy port stops to ask instead, and only an explicit true enables it.
        assertFalse(IPhoneBuilder.cryptoApiRequired(LEGACY, false, false, true, ""));
        assertTrue(IPhoneBuilder.cryptoApiRequired(LEGACY, false, false, true, "true"));
        // Known answers are unchanged.
        assertTrue(IPhoneBuilder.cryptoApiRequired(PUBLIC, false, true, false, "false"));
        assertFalse(IPhoneBuilder.cryptoApiRequired(PUBLIC, false, false, false, ""));
    }

    @Test
    public void onlyLegacyGcmStopsTheBuildToAsk() {
        assertNotNull(IPhoneBuilder.cryptoGcmUndecidable(LEGACY, false, true, ""));
        assertNull(IPhoneBuilder.cryptoGcmUndecidable(LEGACY, false, true, "false"));
        assertNull(IPhoneBuilder.cryptoGcmUndecidable(LEGACY, true, true, ""));
        assertNull(IPhoneBuilder.cryptoGcmUndecidable(PUBLIC, false, true, ""));
    }

    @Test
    public void theShippedPortDeclaresPublicGcm() throws Exception {
        File header = new File("../../Ports/iOSPort/nativeSources/CN1Crypto.h");
        if (!header.isFile()) {
            // The plugin can be built from a source bundle without the ports beside it.
            return;
        }
        String text = new String(Files.readAllBytes(header.toPath()), StandardCharsets.UTF_8);
        assertTrue("CN1Crypto.h must carry the marker the builders read",
                text.contains(IPhoneBuilder.CRYPTO_GCM_PUBLIC_MARKER));
        File source = new File(header.getParentFile(), "CN1Crypto.m");
        String impl = new String(Files.readAllBytes(source.toPath()), StandardCharsets.UTF_8);
        assertFalse("the marker promises no private CommonCrypto GCM declarations",
                impl.contains("extern CCCryptorStatus CCCryptorGCM"));
        assertEquals(-1, impl.indexOf("CommonCryptorSPI.h>"));
    }
}
