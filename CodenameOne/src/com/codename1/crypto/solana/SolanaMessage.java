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

import com.codename1.crypto.encoding.Base58;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/// The part of a transaction that is signed: the accounts, a recent blockhash and the
/// compiled instructions. Both wire versions are read and written: the legacy format and v0.
/// [#compile] orders accounts the way the Solana SDK does: the fee payer first, then the
/// writable signers, the read-only signers, the writable and the read-only non-signers, each
/// group in order of first appearance, with every program after the accounts named before it.
///
/// A v0 message read from the wire may name accounts through address lookup tables; those
/// are kept as they are so the message re-serializes unchanged, but only its static keys can be
/// resolved here.
@com.codename1.impl.SharedWithBackend
public final class SolanaMessage {
    /// The version of a legacy message.
    public static final int LEGACY = -1;

    private final int version;
    private final int numRequiredSignatures;
    private final int numReadonlySigned;
    private final int numReadonlyUnsigned;
    private final List<SolanaAddress> accountKeys;
    private final byte[] recentBlockhash;
    private final List<Compiled> instructions;
    private final List<Lookup> lookups;

    private SolanaMessage(int version, int numRequiredSignatures, int numReadonlySigned, int numReadonlyUnsigned,
            List<SolanaAddress> accountKeys, byte[] recentBlockhash, List<Compiled> instructions,
            List<Lookup> lookups) {
        this.version = version;
        this.numRequiredSignatures = numRequiredSignatures;
        this.numReadonlySigned = numReadonlySigned;
        this.numReadonlyUnsigned = numReadonlyUnsigned;
        this.accountKeys = Collections.unmodifiableList(accountKeys);
        this.recentBlockhash = recentBlockhash;
        this.instructions = instructions;
        this.lookups = lookups;
    }

    /// A legacy message.
    ///
    /// #### Parameters
    ///
    /// - `payer`: the fee payer, which signs
    /// - `instructions`: the instructions, run in order
    /// - `recentBlockhash`: a Base58 blockhash from `getLatestBlockhash`, or a durable nonce
    public static SolanaMessage compile(SolanaAddress payer, List<Instruction> instructions, String recentBlockhash) {
        return compile(LEGACY, payer, instructions, recentBlockhash);
    }

    /// A v0 message with no address lookup tables.
    public static SolanaMessage compileV0(SolanaAddress payer, List<Instruction> instructions,
            String recentBlockhash) {
        return compile(0, payer, instructions, recentBlockhash);
    }

    private static SolanaMessage compile(int version, SolanaAddress payer, List<Instruction> instructions,
            String recentBlockhash) {
        byte[] hash = Base58.decode(recentBlockhash);
        if (hash.length != 32) {
            throw new IllegalArgumentException("a blockhash is 32 bytes");
        }
        List<SolanaAddress> order = new ArrayList<SolanaAddress>();
        List<boolean[]> flags = new ArrayList<boolean[]>();
        note(order, flags, payer, true, true);
        for (int i = 0; i < instructions.size(); i++) {
            Instruction ix = instructions.get(i);
            note(order, flags, ix.getProgramId(), false, false);
            List<AccountMeta> metas = ix.getAccounts();
            for (int j = 0; j < metas.size(); j++) {
                AccountMeta m = metas.get(j);
                note(order, flags, m.getAddress(), m.isSigner(), m.isWritable());
            }
        }
        List<SolanaAddress> keys = new ArrayList<SolanaAddress>();
        int[] counts = new int[4];
        for (int group = 0; group < 4; group++) {
            boolean signer = group < 2;
            boolean writable = group % 2 == 0;
            for (int i = 0; i < order.size(); i++) {
                boolean[] f = flags.get(i);
                if (f[0] == signer && f[1] == writable) {
                    keys.add(order.get(i));
                    counts[group]++;
                }
            }
        }
        if (keys.size() > 256) {
            throw new IllegalArgumentException("a message names at most 256 accounts");
        }
        List<Compiled> compiled = new ArrayList<Compiled>();
        for (int i = 0; i < instructions.size(); i++) {
            Instruction ix = instructions.get(i);
            List<AccountMeta> metas = ix.getAccounts();
            int[] idx = new int[metas.size()];
            for (int j = 0; j < idx.length; j++) {
                idx[j] = keys.indexOf(metas.get(j).getAddress());
            }
            compiled.add(new Compiled(keys.indexOf(ix.getProgramId()), idx, ix.rawData()));
        }
        return new SolanaMessage(version, counts[0] + counts[1], counts[1], counts[3], keys, hash, compiled,
                new ArrayList<Lookup>());
    }

    private static void note(List<SolanaAddress> order, List<boolean[]> flags, SolanaAddress a, boolean signer,
            boolean writable) {
        int i = order.indexOf(a);
        if (i < 0) {
            order.add(a);
            flags.add(new boolean[] {signer, writable});
        } else {
            boolean[] f = flags.get(i);
            f[0] |= signer;
            f[1] |= writable;
        }
    }

    /// The wire form, the bytes every signer signs.
    public byte[] serialize() {
        ShortVec w = new ShortVec();
        if (version != LEGACY) {
            w.u8(0x80 | version);
        }
        w.u8(numRequiredSignatures).u8(numReadonlySigned).u8(numReadonlyUnsigned);
        w.length(accountKeys.size());
        for (int i = 0; i < accountKeys.size(); i++) {
            w.bytes(accountKeys.get(i).raw());
        }
        w.bytes(recentBlockhash);
        w.length(instructions.size());
        for (int i = 0; i < instructions.size(); i++) {
            Compiled c = instructions.get(i);
            w.u8(c.program).length(c.accounts.length);
            for (int j = 0; j < c.accounts.length; j++) {
                w.u8(c.accounts[j]);
            }
            w.length(c.data.length).bytes(c.data);
        }
        if (version != LEGACY) {
            w.length(lookups.size());
            for (int i = 0; i < lookups.size(); i++) {
                Lookup l = lookups.get(i);
                w.bytes(l.table);
                w.length(l.writable.length).bytes(l.writable);
                w.length(l.readonly.length).bytes(l.readonly);
            }
        }
        return w.toBytes();
    }

    /// Reads a message, legacy or v0.
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: for malformed data or an unknown version
    public static SolanaMessage parse(byte[] data) {
        ShortVec.Reader r = new ShortVec.Reader(data);
        SolanaMessage m = read(r);
        if (r.hasMore()) {
            throw new IllegalArgumentException("trailing data after the message");
        }
        return m;
    }

    static SolanaMessage read(ShortVec.Reader r) {
        int version = LEGACY;
        if ((r.peek() & 0x80) != 0) {
            version = r.u8() & 0x7f;
            if (version != 0) {
                throw new IllegalArgumentException("unsupported message version " + version);
            }
        }
        int sigs = r.u8();
        int roSigned = r.u8();
        int roUnsigned = r.u8();
        int n = r.length();
        if (sigs > n || roSigned > sigs || roUnsigned > n - sigs) {
            throw new IllegalArgumentException("inconsistent message header");
        }
        List<SolanaAddress> keys = new ArrayList<SolanaAddress>();
        for (int i = 0; i < n; i++) {
            keys.add(SolanaAddress.fromBytes(r.read(32)));
        }
        byte[] hash = r.read(32);
        int nIx = r.length();
        List<Compiled> ixs = new ArrayList<Compiled>();
        for (int i = 0; i < nIx; i++) {
            int program = r.u8();
            int[] accounts = new int[r.length()];
            for (int j = 0; j < accounts.length; j++) {
                accounts[j] = r.u8();
            }
            ixs.add(new Compiled(program, accounts, r.read(r.length())));
        }
        List<Lookup> lookups = new ArrayList<Lookup>();
        if (version != LEGACY) {
            int nl = r.length();
            for (int i = 0; i < nl; i++) {
                byte[] table = r.read(32);
                byte[] wr = r.read(r.length());
                lookups.add(new Lookup(table, wr, r.read(r.length())));
            }
        }
        return new SolanaMessage(version, sigs, roSigned, roUnsigned, keys, hash, ixs, lookups);
    }

    /// [#LEGACY] or 0.
    public int getVersion() {
        return version;
    }

    /// The accounts named directly in the message; signers come first.
    public List<SolanaAddress> getAccountKeys() {
        return accountKeys;
    }

    /// The fee payer, the first account.
    public SolanaAddress getFeePayer() {
        return accountKeys.get(0);
    }

    /// The Base58 recent blockhash.
    public String getRecentBlockhash() {
        return Base58.encode(recentBlockhash);
    }

    /// How many signatures the transaction carries; the first this many accounts sign.
    public int getNumRequiredSignatures() {
        return numRequiredSignatures;
    }

    /// Whether the account at an index signs.
    public boolean isSigner(int index) {
        return index < numRequiredSignatures;
    }

    /// Whether the message marks the account at an index writable.
    public boolean isWritable(int index) {
        if (index < numRequiredSignatures) {
            return index < numRequiredSignatures - numReadonlySigned;
        }
        return index < accountKeys.size() - numReadonlyUnsigned;
    }

    /// Whether the message loads accounts through address lookup tables.
    public boolean hasAddressLookups() {
        return !lookups.isEmpty();
    }

    /// The instructions with their accounts resolved.
    ///
    /// #### Throws
    ///
    /// - `IllegalStateException`: when an instruction names an account from a lookup table
    public List<Instruction> getInstructions() {
        List<Instruction> out = new ArrayList<Instruction>();
        for (int i = 0; i < instructions.size(); i++) {
            Compiled c = instructions.get(i);
            List<AccountMeta> metas = new ArrayList<AccountMeta>();
            for (int j = 0; j < c.accounts.length; j++) {
                int k = c.accounts[j];
                metas.add(new AccountMeta(key(k), isSigner(k), isWritable(k)));
            }
            out.add(new Instruction(key(c.program), metas, c.data));
        }
        return out;
    }

    private SolanaAddress key(int index) {
        if (index >= accountKeys.size()) {
            throw new IllegalStateException("account " + index + " comes from an address lookup table");
        }
        return accountKeys.get(index);
    }

    private static final class Compiled {
        final int program;
        final int[] accounts;
        final byte[] data;

        Compiled(int program, int[] accounts, byte[] data) {
            this.program = program;
            this.accounts = accounts;
            this.data = data;
        }
    }

    private static final class Lookup {
        final byte[] table;
        final byte[] writable;
        final byte[] readonly;

        Lookup(byte[] table, byte[] writable, byte[] readonly) {
            this.table = table;
            this.writable = writable;
            this.readonly = readonly;
        }
    }
}
