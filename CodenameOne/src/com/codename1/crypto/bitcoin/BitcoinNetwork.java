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

/// The parameters that make an address or key belong to one Bitcoin network.
@com.codename1.impl.SharedWithBackend
public final class BitcoinNetwork {
    /// Bitcoin mainnet: `1...`, `3...`, `bc1...`.
    public static final BitcoinNetwork MAINNET = new BitcoinNetwork("mainnet", 0x00, 0x05, "bc", 0x80, 0x0488B21E,
            0x0488ADE4, 0);
    /// testnet3 and testnet4: `m...`/`n...`, `2...`, `tb1...`.
    public static final BitcoinNetwork TESTNET = new BitcoinNetwork("testnet", 0x6f, 0xc4, "tb", 0xef, 0x043587CF,
            0x04358394, 1);
    /// Signet, which shares testnet's encodings.
    public static final BitcoinNetwork SIGNET = new BitcoinNetwork("signet", 0x6f, 0xc4, "tb", 0xef, 0x043587CF,
            0x04358394, 1);
    /// Regtest: testnet's encodings except for the `bcrt` HRP.
    public static final BitcoinNetwork REGTEST = new BitcoinNetwork("regtest", 0x6f, 0xc4, "bcrt", 0xef,
            0x043587CF, 0x04358394, 1);

    private final String name;
    private final int p2pkhVersion;
    private final int p2shVersion;
    private final String hrp;
    private final int wifPrefix;
    private final int xpubVersion;
    private final int xprvVersion;
    private final int coinType;

    private BitcoinNetwork(String name, int p2pkhVersion, int p2shVersion, String hrp, int wifPrefix,
            int xpubVersion, int xprvVersion, int coinType) {
        this.name = name;
        this.p2pkhVersion = p2pkhVersion;
        this.p2shVersion = p2shVersion;
        this.hrp = hrp;
        this.wifPrefix = wifPrefix;
        this.xpubVersion = xpubVersion;
        this.xprvVersion = xprvVersion;
        this.coinType = coinType;
    }

    /// The network's name.
    public String getName() {
        return name;
    }

    /// The Base58Check version byte of a P2PKH address.
    public int getP2pkhVersion() {
        return p2pkhVersion;
    }

    /// The Base58Check version byte of a P2SH address.
    public int getP2shVersion() {
        return p2shVersion;
    }

    /// The Bech32 human-readable part of a SegWit address.
    public String getBech32Hrp() {
        return hrp;
    }

    /// The first byte of a WIF private key.
    public int getWifPrefix() {
        return wifPrefix;
    }

    /// The BIP-32 public version bytes (`xpub`/`tpub`).
    public int getXpubVersion() {
        return xpubVersion;
    }

    /// The BIP-32 private version bytes (`xprv`/`tprv`).
    public int getXprvVersion() {
        return xprvVersion;
    }

    /// The BIP-44 coin type: 0 on mainnet, 1 on every test network.
    public int getCoinType() {
        return coinType;
    }

    @Override
    public String toString() {
        return name;
    }
}
