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

/// Instructions of the SPL Token program and the Associated Token Account program. Token
/// amounts are raw base units, unsigned 64-bit; a mint's `decimals` says where the point is.
/// Each builder has a form that takes the token program, for mints under Token-2022.
@com.codename1.impl.SharedWithBackend
public final class TokenProgram {
    private TokenProgram() {
    }

    /// `Transfer`: moves tokens between two token accounts; `owner` signs.
    public static Instruction transfer(SolanaAddress source, SolanaAddress destination, SolanaAddress owner,
            long amount) {
        return new Instruction(SolanaAddress.TOKEN_PROGRAM,
                new AccountMeta[] {AccountMeta.writable(source), AccountMeta.writable(destination),
                    AccountMeta.signer(owner)},
                new ShortVec().u8(3).u64(amount).toBytes());
    }

    /// `TransferChecked`: a transfer that also names the mint and its decimals, which wallets
    /// prefer and Token-2022 requires.
    public static Instruction transferChecked(SolanaAddress source, SolanaAddress mint, SolanaAddress destination,
            SolanaAddress owner, long amount, int decimals) {
        return transferChecked(SolanaAddress.TOKEN_PROGRAM, source, mint, destination, owner, amount, decimals);
    }

    /// `TransferChecked` under a given token program.
    public static Instruction transferChecked(SolanaAddress tokenProgram, SolanaAddress source, SolanaAddress mint,
            SolanaAddress destination, SolanaAddress owner, long amount, int decimals) {
        return new Instruction(tokenProgram,
                new AccountMeta[] {AccountMeta.writable(source), AccountMeta.readonly(mint),
                    AccountMeta.writable(destination), AccountMeta.signer(owner)},
                new ShortVec().u8(12).u64(amount).u8(decimals).toBytes());
    }

    /// `CloseAccount`: closes an empty token account and returns its rent to `destination`.
    public static Instruction closeAccount(SolanaAddress account, SolanaAddress destination, SolanaAddress owner) {
        return new Instruction(SolanaAddress.TOKEN_PROGRAM,
                new AccountMeta[] {AccountMeta.writable(account), AccountMeta.writable(destination),
                    AccountMeta.signer(owner)},
                new byte[] {9});
    }

    /// Creates the associated token account of `wallet` for `mint`, paid by `payer`. The
    /// idempotent form succeeds when the account already exists, which is what a sender
    /// wants before a first transfer to a recipient.
    public static Instruction createAssociatedTokenAccount(SolanaAddress payer, SolanaAddress wallet,
            SolanaAddress mint, boolean idempotent) {
        return createAssociatedTokenAccount(payer, wallet, mint, SolanaAddress.TOKEN_PROGRAM, idempotent);
    }

    /// Creates an associated token account under a given token program.
    public static Instruction createAssociatedTokenAccount(SolanaAddress payer, SolanaAddress wallet,
            SolanaAddress mint, SolanaAddress tokenProgram, boolean idempotent) {
        SolanaAddress ata = SolanaAddress.associatedTokenAddress(wallet, mint, tokenProgram);
        return new Instruction(SolanaAddress.ASSOCIATED_TOKEN_PROGRAM,
                new AccountMeta[] {AccountMeta.signerWritable(payer), AccountMeta.writable(ata),
                    AccountMeta.readonly(wallet), AccountMeta.readonly(mint),
                    AccountMeta.readonly(SolanaAddress.SYSTEM_PROGRAM), AccountMeta.readonly(tokenProgram)},
                idempotent ? new byte[] {1} : new byte[0]);
    }
}
