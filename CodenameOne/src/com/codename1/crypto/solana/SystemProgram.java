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

/// Instructions of the System Program. Amounts are lamports, 10^9 to the SOL, and are
/// written as unsigned 64-bit values.
@com.codename1.impl.SharedWithBackend
public final class SystemProgram {
    /// Lamports in one SOL.
    public static final long LAMPORTS_PER_SOL = 1000000000L;

    private static final SolanaAddress RECENT_BLOCKHASHES =
            SolanaAddress.parse("SysvarRecentB1ockHashes11111111111111111111");

    private SystemProgram() {
    }

    /// Moves lamports between two system accounts; `from` signs.
    public static Instruction transfer(SolanaAddress from, SolanaAddress to, long lamports) {
        return new Instruction(SolanaAddress.SYSTEM_PROGRAM,
                new AccountMeta[] {AccountMeta.signerWritable(from), AccountMeta.writable(to)},
                new ShortVec().u32(2).u64(lamports).toBytes());
    }

    /// Creates an account owned by a program, funded with `lamports` and sized `space` bytes;
    /// both the payer and the new account sign.
    public static Instruction createAccount(SolanaAddress payer, SolanaAddress newAccount, long lamports,
            long space, SolanaAddress owner) {
        return new Instruction(SolanaAddress.SYSTEM_PROGRAM,
                new AccountMeta[] {AccountMeta.signerWritable(payer), AccountMeta.signerWritable(newAccount)},
                new ShortVec().u32(0).u64(lamports).u64(space).bytes(owner.raw()).toBytes());
    }

    /// Assigns an account to a program.
    public static Instruction assign(SolanaAddress account, SolanaAddress owner) {
        return new Instruction(SolanaAddress.SYSTEM_PROGRAM,
                new AccountMeta[] {AccountMeta.signerWritable(account)},
                new ShortVec().u32(1).bytes(owner.raw()).toBytes());
    }

    /// Allocates space in an account.
    public static Instruction allocate(SolanaAddress account, long space) {
        return new Instruction(SolanaAddress.SYSTEM_PROGRAM,
                new AccountMeta[] {AccountMeta.signerWritable(account)},
                new ShortVec().u32(8).u64(space).toBytes());
    }

    /// Advances a durable nonce. It must be the first instruction of a transaction whose
    /// recent blockhash is the nonce value.
    public static Instruction advanceNonceAccount(SolanaAddress nonceAccount, SolanaAddress authority) {
        return new Instruction(SolanaAddress.SYSTEM_PROGRAM,
                new AccountMeta[] {AccountMeta.writable(nonceAccount), AccountMeta.readonly(RECENT_BLOCKHASHES),
                    AccountMeta.signer(authority)},
                new ShortVec().u32(4).toBytes());
    }
}
