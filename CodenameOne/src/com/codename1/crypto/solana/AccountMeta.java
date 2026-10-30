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

/// One account an instruction touches, with whether it must sign and whether it is written.
@com.codename1.impl.SharedWithBackend
public final class AccountMeta {
    private final SolanaAddress address;
    private final boolean signer;
    private final boolean writable;

    /// An account reference.
    public AccountMeta(SolanaAddress address, boolean signer, boolean writable) {
        if (address == null) {
            throw new IllegalArgumentException("address");
        }
        this.address = address;
        this.signer = signer;
        this.writable = writable;
    }

    /// A writable account that signs.
    public static AccountMeta signerWritable(SolanaAddress address) {
        return new AccountMeta(address, true, true);
    }

    /// A read-only account that signs.
    public static AccountMeta signer(SolanaAddress address) {
        return new AccountMeta(address, true, false);
    }

    /// A writable account that does not sign.
    public static AccountMeta writable(SolanaAddress address) {
        return new AccountMeta(address, false, true);
    }

    /// A read-only account that does not sign.
    public static AccountMeta readonly(SolanaAddress address) {
        return new AccountMeta(address, false, false);
    }

    /// The account.
    public SolanaAddress getAddress() {
        return address;
    }

    /// Whether the account's key must sign the transaction.
    public boolean isSigner() {
        return signer;
    }

    /// Whether the instruction writes the account.
    public boolean isWritable() {
        return writable;
    }
}
