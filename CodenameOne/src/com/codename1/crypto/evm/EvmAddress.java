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
package com.codename1.crypto.evm;

import com.codename1.crypto.encoding.Hex;
import com.codename1.crypto.encoding.Rlp;
import com.codename1.security.Hash;
import com.codename1.security.Secp256k1;

/// A 20-byte address on Ethereum or any EVM chain (Polygon, Arbitrum, Base, BNB Chain...).
///
/// An address is the last 20 bytes of the Keccak-256 of the 64-byte public key. EIP-55
/// spells it in mixed case so the case is a checksum: [#parse] rejects a mixed-case address
/// whose case is wrong, which catches a mistyped character before funds go to an address
/// nobody holds. An all-lowercase or all-uppercase address carries no checksum and is
/// accepted as written, as every wallet does.
@com.codename1.impl.SharedWithBackend
public final class EvmAddress {
    private final byte[] bytes;

    private EvmAddress(byte[] bytes) {
        this.bytes = bytes;
    }

    /// Wraps 20 address bytes.
    public static EvmAddress fromBytes(byte[] address) {
        if (address == null || address.length != 20) {
            throw new IllegalArgumentException("an address is 20 bytes");
        }
        byte[] copy = new byte[20];
        System.arraycopy(address, 0, copy, 0, 20);
        return new EvmAddress(copy);
    }

    /// The address of a secp256k1 public key in any SEC 1 form, or the raw 64 bytes.
    public static EvmAddress fromPublicKey(byte[] publicKey) {
        byte[] raw;
        if (publicKey.length == 64) {
            raw = publicKey;
        } else {
            byte[] full = Secp256k1.convertPublicKey(publicKey, false);
            raw = new byte[64];
            System.arraycopy(full, 1, raw, 0, 64);
        }
        byte[] hash = Hash.keccak256(raw);
        byte[] out = new byte[20];
        System.arraycopy(hash, 12, out, 0, 20);
        return new EvmAddress(out);
    }

    /// The address a private key controls.
    public static EvmAddress fromPrivateKey(byte[] privateKey) {
        return fromPublicKey(Secp256k1.publicKey(privateKey, false));
    }

    /// The address of a contract deployed with `CREATE` by `deployer` at `nonce`.
    public static EvmAddress contractAddress(EvmAddress deployer, long nonce) {
        java.util.List<Object> fields = new java.util.ArrayList<Object>();
        fields.add(deployer.bytes);
        fields.add(Rlp.longBytes(nonce));
        byte[] hash = Hash.keccak256(Rlp.encode(fields));
        byte[] out = new byte[20];
        System.arraycopy(hash, 12, out, 0, 20);
        return new EvmAddress(out);
    }

    /// The address of a contract deployed with `CREATE2`.
    public static EvmAddress create2Address(EvmAddress deployer, byte[] salt32, byte[] initCodeHash) {
        if (salt32.length != 32 || initCodeHash.length != 32) {
            throw new IllegalArgumentException("salt and init code hash are 32 bytes");
        }
        byte[] data = new byte[85];
        data[0] = (byte) 0xff;
        System.arraycopy(deployer.bytes, 0, data, 1, 20);
        System.arraycopy(salt32, 0, data, 21, 32);
        System.arraycopy(initCodeHash, 0, data, 53, 32);
        byte[] hash = Hash.keccak256(data);
        byte[] out = new byte[20];
        System.arraycopy(hash, 12, out, 0, 20);
        return new EvmAddress(out);
    }

    /// Parses `0x` and 40 hex digits, checking the EIP-55 checksum when the text is in mixed
    /// case.
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: for a malformed address or a wrong checksum
    public static EvmAddress parse(String text) {
        if (text == null || text.length() != 42 || text.charAt(0) != '0'
                || (text.charAt(1) != 'x' && text.charAt(1) != 'X')) {
            throw new IllegalArgumentException("an address is 0x and 40 hex digits");
        }
        String hex = text.substring(2);
        EvmAddress out = new EvmAddress(Hex.decode(hex));
        boolean lower = false;
        boolean upper = false;
        for (int iter = 0; iter < 40; iter++) {
            char c = hex.charAt(iter);
            if (c >= 'a' && c <= 'f') {
                lower = true;
            } else if (c >= 'A' && c <= 'F') {
                upper = true;
            }
        }
        if (lower && upper && !out.toChecksumString().substring(2).equals(hex)) {
            throw new IllegalArgumentException("EIP-55 checksum mismatch: " + text);
        }
        return out;
    }

    /// Tells whether [#parse] accepts the text.
    public static boolean isValid(String text) {
        try {
            parse(text);
            return true;
        } catch (IllegalArgumentException invalid) {
            return false;
        }
    }

    /// A copy of the 20 bytes.
    public byte[] toBytes() {
        byte[] out = new byte[20];
        System.arraycopy(bytes, 0, out, 0, 20);
        return out;
    }

    /// The EIP-55 mixed-case form, which is how an address should be shown.
    public String toChecksumString() {
        String hex = Hex.encode(bytes);
        byte[] hash = Hash.keccak256(ascii(hex));
        StringBuilder out = new StringBuilder("0x");
        for (int iter = 0; iter < 40; iter++) {
            char c = hex.charAt(iter);
            int nibble = (hash[iter >> 1] >> ((iter & 1) == 0 ? 4 : 0)) & 0xf;
            out.append(c >= 'a' && nibble >= 8 ? (char) (c - 32) : c);
        }
        return out.toString();
    }

    /// The lowercase `0x` form.
    public String toLowerCaseString() {
        return "0x" + Hex.encode(bytes);
    }

    /// The EIP-55 form.
    @Override
    public String toString() {
        return toChecksumString();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof EvmAddress)) {
            return false;
        }
        byte[] other = ((EvmAddress) o).bytes;
        for (int iter = 0; iter < 20; iter++) {
            if (other[iter] != bytes[iter]) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        return ((bytes[16] & 0xff) << 24) | ((bytes[17] & 0xff) << 16) | ((bytes[18] & 0xff) << 8)
                | (bytes[19] & 0xff);
    }

    static byte[] ascii(String text) {
        byte[] out = new byte[text.length()];
        for (int iter = 0; iter < out.length; iter++) {
            out[iter] = (byte) text.charAt(iter);
        }
        return out;
    }
}
