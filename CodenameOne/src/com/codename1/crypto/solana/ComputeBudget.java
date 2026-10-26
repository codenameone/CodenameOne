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

/// Compute Budget and Memo instructions, the two most transactions add beside their payload.
@com.codename1.impl.SharedWithBackend
public final class ComputeBudget {
    private ComputeBudget() {
    }

    /// Caps the compute units the transaction may use.
    public static Instruction setComputeUnitLimit(int units) {
        return new Instruction(SolanaAddress.COMPUTE_BUDGET_PROGRAM, new AccountMeta[0],
                new ShortVec().u8(2).u32(units & 0xffffffffL).toBytes());
    }

    /// Sets the priority fee, in micro-lamports per compute unit.
    public static Instruction setComputeUnitPrice(long microLamports) {
        return new Instruction(SolanaAddress.COMPUTE_BUDGET_PROGRAM, new AccountMeta[0],
                new ShortVec().u8(3).u64(microLamports).toBytes());
    }

    /// A Memo program instruction carrying UTF-8 text; each listed signer must sign.
    public static Instruction memo(String text, SolanaAddress... signers) {
        AccountMeta[] metas = new AccountMeta[signers.length];
        for (int i = 0; i < signers.length; i++) {
            metas[i] = AccountMeta.signer(signers[i]);
        }
        return new Instruction(SolanaAddress.MEMO_PROGRAM, metas, SolanaAddress.utf8(text));
    }
}
