/*
 * Copyright (c) 2008-2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.crypto.bitcoin;

import com.codename1.crypto.encoding.Base58Check;
import com.codename1.security.Secp256k1;

/// Wallet Import Format: a private key in Base58Check, the form paper wallets and
/// `dumpprivkey` use. The trailing `0x01` flag says the key's addresses use the compressed
/// public key.
@com.codename1.impl.SharedWithBackend
public final class Wif {
    private final byte[] privateKey;
    private final boolean compressed;
    private final BitcoinNetwork network;

    private Wif(byte[] privateKey, boolean compressed, BitcoinNetwork network) {
        this.privateKey = privateKey;
        this.compressed = compressed;
        this.network = network;
    }

    /// Encodes a private key.
    public static String encode(byte[] privateKey, boolean compressed, BitcoinNetwork network) {
        if (!Secp256k1.isValidPrivateKey(privateKey)) {
            throw new IllegalArgumentException("invalid private key");
        }
        byte[] payload = new byte[compressed ? 34 : 33];
        payload[0] = (byte) network.getWifPrefix();
        System.arraycopy(privateKey, 0, payload, 1, 32);
        if (compressed) {
            payload[33] = 1;
        }
        try {
            return Base58Check.encode(payload);
        } finally {
            wipe(payload);
        }
    }

    /// Decodes a WIF string of the given network.
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: for a malformed key, a bad checksum or another network
    public static Wif decode(String wif, BitcoinNetwork network) {
        byte[] payload = Base58Check.decode(wif);
        try {
            boolean compressed = payload.length == 34 && payload[33] == 1;
            if ((payload.length != 33 && !compressed) || (payload[0] & 0xff) != network.getWifPrefix()) {
                throw new IllegalArgumentException("not a " + network + " WIF key");
            }
            byte[] key = new byte[32];
            System.arraycopy(payload, 1, key, 0, 32);
            if (!Secp256k1.isValidPrivateKey(key)) {
                wipe(key);
                throw new IllegalArgumentException("invalid private key");
            }
            return new Wif(key, compressed, network);
        } finally {
            wipe(payload);
        }
    }

    /// A copy of the private key. Wipe it after use.
    public byte[] getPrivateKey() {
        return Wire.copy(privateKey);
    }

    /// Tells whether the key's addresses use the compressed public key.
    public boolean isCompressed() {
        return compressed;
    }

    /// The public key in the form the flag selects.
    public byte[] getPublicKey() {
        return Secp256k1.publicKey(privateKey, compressed);
    }

    /// The network.
    public BitcoinNetwork getNetwork() {
        return network;
    }

    /// Wipes the private key.
    public void destroy() {
        wipe(privateKey);
    }

    static void wipe(byte[] b) {
        if (b != null) {
            for (int iter = 0; iter < b.length; iter++) {
                b[iter] = 0;
            }
        }
    }
}
