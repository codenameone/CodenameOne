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

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/// The Java prep of a GPU batch, which the JavaScript port hands to the WebGPU kernels, must
/// write word for word the records the native hosts' C prep writes. `crypto-gpu/vectors.txt`
/// is the C prep's output (`scripts/crypto-gpu/vectors.sh 48`): one line per entry with the
/// kernel, the entry, the prep's verdict, the record and what the C kernel answers for it.
class CryptoGpuRecordTest {
    private static final int C_ECDSA = 2;
    private static final int C_SCHNORR = 3;

    private static byte[] hex(String s) {
        return "-".equals(s) ? new byte[0] : Hash.fromHex(s);
    }

    private static List<String[]> vectors() throws Exception {
        InputStream in = CryptoGpuRecordTest.class.getResourceAsStream("/crypto-gpu/vectors.txt");
        assertNotNull(in, "crypto-gpu/vectors.txt");
        List<String[]> out = new ArrayList<String[]>();
        BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.US_ASCII));
        try {
            for (String line = r.readLine(); line != null; line = r.readLine()) {
                if (!line.trim().isEmpty()) {
                    out.add(line.trim().split(" "));
                }
            }
        } finally {
            r.close();
        }
        return out;
    }

    @Test
    void recordsMatchTheNativePrep() throws Exception {
        int pending = 0;
        int rejected = 0;
        for (String[] v : vectors()) {
            int kernel = Integer.parseInt(v[0]);
            int algorithm = kernel == C_ECDSA ? CryptoEngine.BATCH_SECP256K1
                    : kernel == C_SCHNORR ? CryptoEngine.BATCH_SCHNORR : CryptoEngine.BATCH_ED25519;
            byte[] msg = hex(v[1]);
            byte[] sig = hex(v[2]);
            byte[] key = hex(v[3]);
            int cPrep = Integer.parseInt(v[4]);
            // a 1 placed in front shows the record starts at its offset, not at 0
            int[] rec = new int[1 + CryptoEngine.gpuRecordWords(algorithm)];
            int prep = CryptoEngine.gpuRecord(algorithm, msg, 0, msg.length, sig, 0, key, 0, key.length, rec, 1);
            String what = String.join(" ", v[0], v[1], v[2], v[3]);
            if (cPrep == 2) {
                assertEquals(2, prep, what);
                StringBuilder words = new StringBuilder();
                for (int i = 1; i < rec.length; i++) {
                    words.append(String.format("%08x", rec[i]));
                }
                assertEquals(v[5], words.toString(), what);
                pending++;
            } else {
                // 0xff is the C prep leaving a key it cannot parse to the single verify, which
                // calls it invalid
                assertEquals(0, prep, what);
                rejected++;
            }
        }
        assertTrue(pending > 100 && rejected >= 10, pending + " pending, " + rejected + " rejected");
    }

    @Test
    void theKernelAnswerIsTheSingleVerifyAnswer() throws Exception {
        for (String[] v : vectors()) {
            if (!"2".equals(v[4])) {
                continue;
            }
            int kernel = Integer.parseInt(v[0]);
            byte[] msg = hex(v[1]);
            byte[] sig = hex(v[2]);
            byte[] key = hex(v[3]);
            boolean single = kernel == C_ECDSA
                    ? Secp256k1.verify(msg, Secp256k1.EcdsaSignature.fromCompact(sig, 0), key)
                    : kernel == C_SCHNORR ? Schnorr.verify(msg, sig, key) : Ed25519.verify(msg, sig, key);
            assertEquals("1".equals(v[6]), single, String.join(" ", v[0], v[1], v[2], v[3]));
        }
    }
}
