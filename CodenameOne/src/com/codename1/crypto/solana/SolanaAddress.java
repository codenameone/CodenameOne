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
package com.codename1.crypto.solana;

import java.nio.charset.StandardCharsets;
import com.codename1.crypto.encoding.Base58;
import com.codename1.security.Ed25519;
import com.codename1.security.Hash;

/// A Solana account address: 32 bytes, written in Base58. An address is either an Ed25519
/// public key or a program-derived address (PDA), which by construction is not on the curve
/// and so has no private key.
@com.codename1.impl.SharedWithBackend
public final class SolanaAddress implements Comparable<SolanaAddress> {
    /// The address length in bytes.
    public static final int LENGTH = 32;

    /// The System Program, which creates accounts and moves lamports.
    public static final SolanaAddress SYSTEM_PROGRAM = parse("11111111111111111111111111111111");
    /// The SPL Token program.
    public static final SolanaAddress TOKEN_PROGRAM = parse("TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA");
    /// The SPL Token-2022 program.
    public static final SolanaAddress TOKEN_2022_PROGRAM = parse("TokenzQdBNbLqP5VEhdkAS6EPFLC1PHnBqCXEpPxuEb");
    /// The Associated Token Account program.
    public static final SolanaAddress ASSOCIATED_TOKEN_PROGRAM =
            parse("ATokenGPvbdGVxr1b2hvZbsiqW5xWH25efTNsLJA8knL");
    /// The Compute Budget program.
    public static final SolanaAddress COMPUTE_BUDGET_PROGRAM = parse("ComputeBudget111111111111111111111111111111");
    /// The Memo program.
    public static final SolanaAddress MEMO_PROGRAM = parse("MemoSq4gqABAXKb96qnH8TysNcWxMyWCqXgDLGmfcHr");

    private static final byte[] PDA_MARKER = {
        'P', 'r', 'o', 'g', 'r', 'a', 'm', 'D', 'e', 'r', 'i', 'v', 'e', 'd', 'A', 'd', 'd', 'r', 'e', 's', 's'
    };
    private static final int MAX_SEED_LENGTH = 32;
    private static final int MAX_SEEDS = 16;

    private final byte[] bytes;

    private SolanaAddress(byte[] bytes) {
        this.bytes = bytes;
    }

    /// The address of 32 bytes, an Ed25519 public key or a PDA.
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: unless there are exactly 32 bytes
    public static SolanaAddress fromBytes(byte[] bytes) {
        if (bytes == null || bytes.length != LENGTH) {
            throw new IllegalArgumentException("a Solana address is 32 bytes");
        }
        byte[] copy = new byte[LENGTH];
        System.arraycopy(bytes, 0, copy, 0, LENGTH);
        return new SolanaAddress(copy);
    }

    /// The address of an Ed25519 private key (its 32-byte seed).
    public static SolanaAddress fromPrivateKey(byte[] seed) {
        return new SolanaAddress(Ed25519.publicKey(seed));
    }

    /// Reads a Base58 address.
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: for text that is not Base58 or not 32 bytes
    public static SolanaAddress parse(String base58) {
        byte[] b = Base58.decode(base58);
        if (b.length != LENGTH) {
            throw new IllegalArgumentException("a Solana address is 32 bytes: " + base58);
        }
        return new SolanaAddress(b);
    }

    /// Whether text is a Base58 Solana address.
    public static boolean isValid(String base58) {
        try {
            parse(base58);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /// The address bytes, a copy.
    public byte[] toBytes() {
        byte[] copy = new byte[LENGTH];
        System.arraycopy(bytes, 0, copy, 0, LENGTH);
        return copy;
    }

    byte[] raw() {
        return bytes;
    }

    /// Whether the address is on the Ed25519 curve, which is to say it could have a private key.
    /// A program-derived address answers `false`.
    public boolean isOnCurve() {
        return Ed25519.isOnCurve(bytes);
    }

    /// `createProgramAddress`: the address of seeds under a program, with no bump search.
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: for more than 16 seeds, a seed over 32 bytes, or seeds
    ///   whose hash falls on the curve
    public static SolanaAddress createProgramAddress(byte[][] seeds, SolanaAddress programId) {
        SolanaAddress a = programAddress(seeds, null, programId);
        if (a == null) {
            throw new IllegalArgumentException("the seeds give an address on the curve");
        }
        return a;
    }

    /// `findProgramAddress`: the canonical PDA, searching the bump seed from 255 down.
    public static ProgramAddress findProgramAddress(byte[][] seeds, SolanaAddress programId) {
        byte[] bump = new byte[1];
        for (int b = 255; b >= 0; b--) {
            bump[0] = (byte) b;
            SolanaAddress a = programAddress(seeds, bump, programId);
            if (a != null) {
                return new ProgramAddress(a, b);
            }
        }
        throw new IllegalArgumentException("no viable bump seed");
    }

    private static SolanaAddress programAddress(byte[][] seeds, byte[] bump, SolanaAddress programId) {
        if (seeds.length + (bump == null ? 0 : 1) > MAX_SEEDS) {
            throw new IllegalArgumentException("at most 16 seeds");
        }
        Hash h = Hash.create(Hash.SHA256);
        for (byte[] seed : seeds) {
            if (seed.length > MAX_SEED_LENGTH) {
                throw new IllegalArgumentException("a seed is at most 32 bytes");
            }
            h.update(seed);
        }
        if (bump != null) {
            h.update(bump);
        }
        h.update(programId.bytes);
        h.update(PDA_MARKER);
        byte[] out = h.digest();
        return Ed25519.isOnCurve(out) ? null : new SolanaAddress(out);
    }

    /// `createWithSeed`: the address `SHA-256(base || seed || owner)` that the System
    /// Program's `*WithSeed` instructions use.
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: for a seed over 32 bytes
    public static SolanaAddress createWithSeed(SolanaAddress base, String seed, SolanaAddress owner) {
        byte[] s = seed.getBytes(StandardCharsets.UTF_8);
        if (s.length > MAX_SEED_LENGTH) {
            throw new IllegalArgumentException("a seed is at most 32 bytes");
        }
        Hash h = Hash.create(Hash.SHA256);
        h.update(base.bytes);
        h.update(s);
        h.update(owner.bytes);
        return new SolanaAddress(h.digest());
    }

    /// The associated token account of a wallet for a mint under the SPL Token program.
    public static SolanaAddress associatedTokenAddress(SolanaAddress wallet, SolanaAddress mint) {
        return associatedTokenAddress(wallet, mint, TOKEN_PROGRAM);
    }

    /// The associated token account of a wallet for a mint under a given token program, such as
    /// [#TOKEN_2022_PROGRAM].
    public static SolanaAddress associatedTokenAddress(SolanaAddress wallet, SolanaAddress mint,
            SolanaAddress tokenProgram) {
        return findProgramAddress(new byte[][] {wallet.bytes, tokenProgram.bytes, mint.bytes},
                ASSOCIATED_TOKEN_PROGRAM).getAddress();
    }


    /// The Base58 form.
    @Override
    public String toString() {
        return Base58.encode(bytes);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof SolanaAddress && java.util.Arrays.equals(bytes, ((SolanaAddress) o).bytes);
    }

    @Override
    public int hashCode() {
        return java.util.Arrays.hashCode(bytes);
    }

    /// Orders addresses by their bytes, unsigned.
    public int compareTo(SolanaAddress o) { // NOPMD MissingOverride - Comparable implementation; @Override on it needs Java 6 source
        for (int i = 0; i < LENGTH; i++) {
            int d = (bytes[i] & 0xff) - (o.bytes[i] & 0xff);
            if (d != 0) {
                return d;
            }
        }
        return 0;
    }

    /// A program-derived address with the bump seed that produced it.
    public static final class ProgramAddress {
        private final SolanaAddress address;
        private final int bump;

        ProgramAddress(SolanaAddress address, int bump) {
            this.address = address;
            this.bump = bump;
        }

        /// The address.
        public SolanaAddress getAddress() {
            return address;
        }

        /// The bump seed, 0 to 255, that a program passes back to sign for the address.
        public int getBump() {
            return bump;
        }
    }
}
