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
import com.codename1.crypto.encoding.Bech32;
import com.codename1.security.Hash;
import com.codename1.security.Secp256k1;

/// A Bitcoin address of any standard type, tied to its network.
///
/// | Type | Starts with (mainnet) | Built from |
/// |---|---|---|
/// | [#P2PKH] | `1` | [#p2pkh] |
/// | [#P2SH] | `3` | [#p2sh], [#p2shP2wpkh] (BIP-49) |
/// | [#P2WPKH] | `bc1q` | [#p2wpkh] (BIP-84) |
/// | [#P2WSH] | `bc1q` | [#p2wsh] |
/// | [#P2TR] | `bc1p` | [#p2tr] (BIP-86) |
///
/// [#parse] reads any of them, refusing an address of another network and a Bech32 address
/// with the wrong checksum variant; [#getScriptPubKey] is what a transaction pays to.
@com.codename1.impl.SharedWithBackend
public final class BitcoinAddress {
    /// Pay to public key hash.
    public static final int P2PKH = 0;
    /// Pay to script hash.
    public static final int P2SH = 1;
    /// Native SegWit v0 key hash.
    public static final int P2WPKH = 2;
    /// Native SegWit v0 script hash.
    public static final int P2WSH = 3;
    /// Taproot, SegWit v1.
    public static final int P2TR = 4;
    /// A SegWit version this library knows no spending rules for (v2 to v16).
    public static final int WITNESS_UNKNOWN = 5;

    private final int type;
    private final BitcoinNetwork network;
    private final Script scriptPubKey;
    private final String text;

    private BitcoinAddress(int type, BitcoinNetwork network, Script scriptPubKey, String text) {
        this.type = type;
        this.network = network;
        this.scriptPubKey = scriptPubKey;
        this.text = text;
    }

    /// A legacy address of a public key (compressed or not; the form changes the address).
    public static BitcoinAddress p2pkh(byte[] publicKey, BitcoinNetwork network) {
        return fromScript(Script.p2pkh(Hash.hash160(publicKey)), network);
    }

    /// A native SegWit address of a compressed public key.
    public static BitcoinAddress p2wpkh(byte[] compressedPublicKey, BitcoinNetwork network) {
        requireCompressed(compressedPublicKey);
        return fromScript(Script.p2wpkh(Hash.hash160(compressedPublicKey)), network);
    }

    /// The BIP-49 nested SegWit address: P2WPKH wrapped in P2SH.
    public static BitcoinAddress p2shP2wpkh(byte[] compressedPublicKey, BitcoinNetwork network) {
        requireCompressed(compressedPublicKey);
        return p2sh(Script.p2wpkh(Hash.hash160(compressedPublicKey)), network);
    }

    /// The P2SH address of a redeem script.
    public static BitcoinAddress p2sh(Script redeemScript, BitcoinNetwork network) {
        return fromScript(Script.p2sh(Hash.hash160(redeemScript.raw())), network);
    }

    /// The P2WSH address of a witness script.
    public static BitcoinAddress p2wsh(Script witnessScript, BitcoinNetwork network) {
        return fromScript(Script.p2wsh(Hash.sha256(witnessScript.raw())), network);
    }

    /// The Taproot address of an x-only internal key with no script tree, as BIP-86 wallets
    /// derive it; a 33-byte compressed key is accepted and reduced to its x coordinate.
    public static BitcoinAddress p2tr(byte[] internalKey, BitcoinNetwork network) {
        return p2tr(internalKey, null, network);
    }

    /// The Taproot address of an internal key committing to a script tree's Merkle root.
    public static BitcoinAddress p2tr(byte[] internalKey, byte[] merkleRoot, BitcoinNetwork network) {
        byte[] x = internalKey;
        if (internalKey.length == 33) {
            x = new byte[32];
            System.arraycopy(internalKey, 1, x, 0, 32);
        }
        return fromScript(Taproot.outputScript(x, merkleRoot), network);
    }

    /// The address that a standard output script pays to.
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: for a script with no address form (bare multisig,
    ///   `OP_RETURN`)
    public static BitcoinAddress fromScript(Script script, BitcoinNetwork network) {
        if (script.isP2pkh()) {
            return new BitcoinAddress(P2PKH, network, script,
                    base58(network.getP2pkhVersion(), script.getDestinationHash()));
        }
        if (script.isP2sh()) {
            return new BitcoinAddress(P2SH, network, script,
                    base58(network.getP2shVersion(), script.getDestinationHash()));
        }
        int version = script.getWitnessVersion();
        if (version < 0) {
            throw new IllegalArgumentException("script has no address form");
        }
        byte[] program = script.getWitnessProgram();
        int type;
        if (version == 0 && program.length == 20) {
            type = P2WPKH;
        } else if (version == 0 && program.length == 32) {
            type = P2WSH;
        } else if (version == 0) {
            throw new IllegalArgumentException("bad v0 witness program length");
        } else if (version == 1 && program.length == 32) {
            type = P2TR;
        } else {
            type = WITNESS_UNKNOWN;
        }
        return new BitcoinAddress(type, network, script,
                Bech32.encodeSegwitAddress(network.getBech32Hrp(), version, program));
    }

    /// Parses an address of the given network.
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: for a malformed address, a bad checksum or an address
    ///   of another network
    public static BitcoinAddress parse(String address, BitcoinNetwork network) {
        if (address == null || address.length() == 0) {
            throw new IllegalArgumentException("empty address");
        }
        String hrp = network.getBech32Hrp();
        if (address.length() > hrp.length() && address.regionMatches(true, 0, hrp + "1", 0, hrp.length() + 1)) {
            Bech32.SegwitAddress s = Bech32.decodeSegwitAddress(hrp, address);
            return fromScript(Script.witness(s.getVersion(), s.getProgram()), network);
        }
        byte[] payload;
        try {
            payload = Base58Check.decode(address);
        } catch (IllegalArgumentException bad) {
            throw new IllegalArgumentException("not a " + network + " address: " + address);
        }
        if (payload.length != 21) {
            throw new IllegalArgumentException("not a " + network + " address: " + address);
        }
        byte[] hash = new byte[20];
        System.arraycopy(payload, 1, hash, 0, 20);
        int version = payload[0] & 0xff;
        if (version == network.getP2pkhVersion()) {
            return fromScript(Script.p2pkh(hash), network);
        }
        if (version == network.getP2shVersion()) {
            return fromScript(Script.p2sh(hash), network);
        }
        throw new IllegalArgumentException("not a " + network + " address: " + address);
    }

    /// Tells whether [#parse] accepts the address for the network.
    public static boolean isValid(String address, BitcoinNetwork network) {
        try {
            parse(address, network);
            return true;
        } catch (IllegalArgumentException invalid) {
            return false;
        }
    }

    /// The address type, [#P2PKH] to [#WITNESS_UNKNOWN].
    public int getType() {
        return type;
    }

    /// The network.
    public BitcoinNetwork getNetwork() {
        return network;
    }

    /// The output script that pays this address.
    public Script getScriptPubKey() {
        return scriptPubKey;
    }

    /// The address text. Bech32 addresses are lowercase.
    @Override
    public String toString() {
        return text;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof BitcoinAddress && ((BitcoinAddress) o).text.equals(text)
                && ((BitcoinAddress) o).network == network;
    }

    @Override
    public int hashCode() {
        return text.hashCode();
    }

    private static String base58(int version, byte[] hash) {
        byte[] payload = new byte[21];
        payload[0] = (byte) version;
        System.arraycopy(hash, 0, payload, 1, 20);
        return Base58Check.encode(payload);
    }

    private static void requireCompressed(byte[] key) {
        if (key == null || key.length != 33 || !Secp256k1.isValidPublicKey(key)) {
            throw new IllegalArgumentException("SegWit needs a 33-byte compressed public key");
        }
    }
}
