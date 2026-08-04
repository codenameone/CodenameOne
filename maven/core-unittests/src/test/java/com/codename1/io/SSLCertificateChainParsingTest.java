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
package com.codename1.io;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Parsing of the richer, per-certificate form of the platform certificate list.
 * Getting the grouping wrong would attribute a public key to the wrong
 * certificate, which is a pin that silently checks the wrong thing.
 */
class SSLCertificateChainParsingTest {

    @Test
    void groupsEntriesByChainDelimiter() {
        ConnectionRequest.SSLCertificate[] certs = ConnectionRequest.parseGroupedCertificates(
                new String[] {
                    "CHAIN:0",
                    "SHA-256:leafcert",
                    "SHA1:leafsha1",
                    "SPKI-SHA-256:leafspki",
                    "CHAIN:1",
                    "SHA-256:issuercert",
                    "SHA1:issuersha1",
                    "SPKI-SHA-256:issuerspki"
                });

        assertEquals(2, certs.length);

        assertEquals(0, certs[0].getChainIndex());
        assertTrue(certs[0].isLeaf());
        assertEquals("leafcert", certs[0].getCertificteUniqueKey());
        assertEquals("SHA-256", certs[0].getCertificteAlgorithm());
        assertEquals("leafspki", certs[0].getPublicKeyDigest());
        assertEquals("SHA-256", certs[0].getPublicKeyDigestAlgorithm());

        assertEquals(1, certs[1].getChainIndex());
        assertFalse(certs[1].isLeaf());
        assertEquals("issuercert", certs[1].getCertificteUniqueKey());
        assertEquals("issuerspki", certs[1].getPublicKeyDigest());
    }

    @Test
    void nonTypoAliasesReturnTheSameValues() {
        ConnectionRequest.SSLCertificate[] certs = ConnectionRequest.parseGroupedCertificates(
                new String[] {"CHAIN:0", "SHA-256:abc"});
        assertEquals(certs[0].getCertificteUniqueKey(), certs[0].getFingerprint());
        assertEquals(certs[0].getCertificteAlgorithm(), certs[0].getFingerprintAlgorithm());
    }

    /**
     * A port may report digests without grouping. Treat what arrives as the leaf
     * rather than dropping it.
     */
    @Test
    void entriesBeforeAnyDelimiterBecomeTheLeaf() {
        ConnectionRequest.SSLCertificate[] certs = ConnectionRequest.parseGroupedCertificates(
                new String[] {"SHA-256:abc", "SPKI-SHA-256:def"});
        assertEquals(1, certs.length);
        assertEquals(0, certs[0].getChainIndex());
        assertEquals("abc", certs[0].getCertificteUniqueKey());
        assertEquals("def", certs[0].getPublicKeyDigest());
    }

    @Test
    void certificateWithoutASpkiEntryReportsNullRatherThanGuessing() {
        ConnectionRequest.SSLCertificate[] certs = ConnectionRequest.parseGroupedCertificates(
                new String[] {"CHAIN:0", "SHA-256:abc", "SHA1:def"});
        assertEquals(1, certs.length);
        assertNull(certs[0].getPublicKeyDigest());
        assertNull(certs[0].getPublicKeyDigestAlgorithm());
    }

    /**
     * The first fingerprint wins, so the SHA1 entry that follows SHA-256 does not
     * overwrite it. Pinning a SHA1 fingerprint by accident would be a downgrade.
     */
    @Test
    void firstFingerprintWinsWithinACertificateGroup() {
        ConnectionRequest.SSLCertificate[] certs = ConnectionRequest.parseGroupedCertificates(
                new String[] {"CHAIN:0", "SHA-256:strong", "SHA1:weak"});
        assertEquals("SHA-256", certs[0].getCertificteAlgorithm());
        assertEquals("strong", certs[0].getCertificteUniqueKey());
    }

    @Test
    void malformedEntriesAreSkippedWithoutFailingTheChain() {
        ConnectionRequest.SSLCertificate[] certs = ConnectionRequest.parseGroupedCertificates(
                new String[] {null, "no-colon-here", "CHAIN:0", "SHA-256:abc"});
        assertEquals(1, certs.length);
        assertEquals("abc", certs[0].getCertificteUniqueKey());
    }

    @Test
    void unparseableChainIndexFallsBackToPositionalOrder() {
        ConnectionRequest.SSLCertificate[] certs = ConnectionRequest.parseGroupedCertificates(
                new String[] {"CHAIN:x", "SHA-256:a", "CHAIN:y", "SHA-256:b"});
        assertEquals(2, certs.length);
        assertEquals(0, certs[0].getChainIndex());
        assertEquals(1, certs[1].getChainIndex());
    }

    @Test
    void emptyInputYieldsEmptyChain() {
        assertEquals(0, ConnectionRequest.parseGroupedCertificates(new String[0]).length);
    }

    /**
     * Base64 digests contain + / and = but never a comma, which matters because
     * the iOS port ships the whole chain as one comma-joined string.
     */
    @Test
    void base64DigestsSurviveTheCommaJoinedTransport() {
        String spki = "o+c2M5zOnK96U55rTfy2G9krOHRxnMwJ3esntXNFMdc=";
        assertFalse(spki.contains(","));
        ConnectionRequest.SSLCertificate[] certs = ConnectionRequest.parseGroupedCertificates(
                new String[] {"CHAIN:0", "SPKI-SHA-256:" + spki});
        assertEquals(spki, certs[0].getPublicKeyDigest());
    }
}
