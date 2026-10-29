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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/// A call to an on-chain program: the program, the accounts it reads and writes, and its
/// opaque instruction data. [SystemProgram], [TokenProgram] and [ComputeBudget] build the
/// common ones.
@com.codename1.impl.SharedWithBackend
public final class Instruction {
    private final SolanaAddress programId;
    private final List<AccountMeta> accounts;
    private final byte[] data;

    /// An instruction.
    public Instruction(SolanaAddress programId, List<AccountMeta> accounts, byte[] data) {
        if (programId == null) {
            throw new IllegalArgumentException("programId");
        }
        this.programId = programId;
        this.accounts = Collections.unmodifiableList(new ArrayList<AccountMeta>(
                accounts == null ? Collections.<AccountMeta>emptyList() : accounts));
        this.data = data == null ? new byte[0] : ShortVec.copy(data);
    }

    /// An instruction from an account array.
    public Instruction(SolanaAddress programId, AccountMeta[] accounts, byte[] data) {
        this(programId, java.util.Arrays.asList(accounts), data);
    }

    /// The program invoked.
    public SolanaAddress getProgramId() {
        return programId;
    }

    /// The accounts, in the order the program expects them.
    public List<AccountMeta> getAccounts() {
        return accounts;
    }

    /// The instruction data, a copy.
    public byte[] getData() {
        return ShortVec.copy(data);
    }

    byte[] rawData() {
        return data;
    }
}
