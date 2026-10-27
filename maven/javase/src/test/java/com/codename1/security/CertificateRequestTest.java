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
package com.codename1.security;

import java.io.File;
import java.io.FileOutputStream;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CertificateRequestTest {

    private static byte[] sign(String algorithm, KeyPair keys, byte[] data) {
        try {
            Signature s = Signature.getInstance(algorithm);
            s.initSign(keys.getPrivate());
            s.update(data);
            return s.sign();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /// Reads the TLV at `offset` and answers {tag, valueStart, valueEnd}.
    private static int[] tlv(byte[] der, int offset) {
        int tag = der[offset] & 0xff;
        int first = der[offset + 1] & 0xff;
        int start = offset + 2;
        int length = first;
        if (first >= 0x80) {
            length = 0;
            for (int i = 0; i < (first & 0x7f); i++) {
                length = length << 8 | der[start++] & 0xff;
            }
        }
        return new int[]{tag, start, start + length};
    }

    private void check(String keyAlgorithm, int bits, String signature) throws Exception {
        KeyPairGenerator g = KeyPairGenerator.getInstance(keyAlgorithm);
        g.initialize(bits);
        final KeyPair keys = g.generateKeyPair();
        final CertificateRequest request = new CertificateRequest(keys.getPublic().getEncoded(), keyAlgorithm)
                .commonName("thermostat-42").organization("Example Devices").country("US")
                .dnsName("thermostat-42.local").ipAddress("192.168.1.20");
        assertEquals(signature, request.getSignatureAlgorithm());
        byte[] der = request.sign(data -> sign(signature, keys, data));

        int[] outer = tlv(der, 0);
        assertEquals(0x30, outer[0]);
        assertEquals(der.length, outer[2]);
        int[] info = tlv(der, outer[1]);
        byte[] signed = Arrays.copyOfRange(der, outer[1], info[2]);
        assertArrayEquals(request.getInfo(), signed);
        int[] algorithm = tlv(der, info[2]);
        int[] bitString = tlv(der, algorithm[2]);
        assertEquals(0x03, bitString[0]);
        assertEquals(0, der[bitString[1]]);
        byte[] sig = Arrays.copyOfRange(der, bitString[1] + 1, bitString[2]);

        Signature verify = Signature.getInstance(signature);
        verify.initVerify(keys.getPublic());
        verify.update(signed);
        assertTrue(verify.verify(sig));

        String pem = CertificateRequest.toPem(der);
        assertTrue(pem.startsWith("-----BEGIN CERTIFICATE REQUEST-----\n"));
        // When openssl is installed, have it parse and verify the request too.
        File file = File.createTempFile("cn1-csr", ".pem");
        file.deleteOnExit();
        FileOutputStream out = new FileOutputStream(file);
        out.write(pem.getBytes("US-ASCII"));
        out.close();
        Process p;
        try {
            p = new ProcessBuilder("openssl", "req", "-in", file.getPath(), "-noout", "-verify", "-text")
                    .redirectErrorStream(true).start();
        } catch (java.io.IOException noOpenssl) {
            return;
        }
        String text = new String(readAll(p.getInputStream()), "UTF-8");
        assertEquals(0, p.waitFor(), text);
        assertTrue(text.contains("CN = thermostat-42") || text.contains("CN=thermostat-42"), text);
        assertTrue(text.contains("DNS:thermostat-42.local"), text);
        assertTrue(text.contains("IP Address:192.168.1.20"), text);
    }

    private static byte[] readAll(java.io.InputStream in) throws java.io.IOException {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int n;
        while ((n = in.read(buffer)) > 0) {
            out.write(buffer, 0, n);
        }
        return out.toByteArray();
    }

    @Test
    void ecRequestVerifies() throws Exception {
        check("EC", 256, "SHA256withECDSA");
    }

    @Test
    void rsaRequestVerifies() throws Exception {
        check("RSA", 2048, "SHA256withRSA");
    }

    @Test
    void encodesLongOidArcs() {
        // 1.2.840.113549 -> 2a 86 48 86 f7 0d
        assertArrayEquals(new byte[]{6, 6, 0x2a, (byte) 0x86, 0x48, (byte) 0x86, (byte) 0xf7, 0x0d},
                CertificateRequest.oid(new int[]{1, 2, 840, 113549}));
    }

    @Test
    void rejectsBadInput() {
        byte[] key = {1};
        assertThrows(IllegalArgumentException.class, () -> new CertificateRequest(key, "DSA"));
        CertificateRequest r = new CertificateRequest(key, "EC");
        assertThrows(IllegalArgumentException.class, () -> r.ipAddress("10.0.0.256"));
        assertThrows(IllegalArgumentException.class, () -> r.ipAddress("10.0.0"));
        assertThrows(IllegalArgumentException.class, () -> r.country("USA"));
        assertThrows(IllegalArgumentException.class, () -> r.dnsName("bad name"));
    }
}
