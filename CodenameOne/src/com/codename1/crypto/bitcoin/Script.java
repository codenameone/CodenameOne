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

import com.codename1.security.Hash;

/// A Bitcoin script: the locking `scriptPubKey` of an output, or the `scriptSig`, redeem and
/// witness scripts that unlock one. Immutable.
///
/// The standard output templates have factories ([#p2pkh], [#p2wpkh], [#p2tr]...) and
/// matching tests; [#builder] writes anything else with minimal pushes.
@com.codename1.impl.SharedWithBackend
public final class Script {
    /// `OP_0`, also the empty push.
    public static final int OP_0 = 0x00;
    /// `OP_PUSHDATA1`.
    public static final int OP_PUSHDATA1 = 0x4c;
    /// `OP_PUSHDATA2`.
    public static final int OP_PUSHDATA2 = 0x4d;
    /// `OP_PUSHDATA4`.
    public static final int OP_PUSHDATA4 = 0x4e;
    /// `OP_1NEGATE`.
    public static final int OP_1NEGATE = 0x4f;
    /// `OP_1`; `OP_2`..`OP_16` follow it.
    public static final int OP_1 = 0x51;
    /// `OP_16`.
    public static final int OP_16 = 0x60;
    /// `OP_RETURN`.
    public static final int OP_RETURN = 0x6a;
    /// `OP_DUP`.
    public static final int OP_DUP = 0x76;
    /// `OP_EQUAL`.
    public static final int OP_EQUAL = 0x87;
    /// `OP_EQUALVERIFY`.
    public static final int OP_EQUALVERIFY = 0x88;
    /// `OP_HASH160`.
    public static final int OP_HASH160 = 0xa9;
    /// `OP_CHECKSIG`.
    public static final int OP_CHECKSIG = 0xac;
    /// `OP_CHECKMULTISIG`.
    public static final int OP_CHECKMULTISIG = 0xae;
    /// `OP_CHECKLOCKTIMEVERIFY`.
    public static final int OP_CHECKLOCKTIMEVERIFY = 0xb1;
    /// `OP_CHECKSEQUENCEVERIFY`.
    public static final int OP_CHECKSEQUENCEVERIFY = 0xb2;
    /// `OP_CHECKSIGADD`, the Tapscript multisig opcode.
    public static final int OP_CHECKSIGADD = 0xba;

    private static final Script EMPTY = new Script(new byte[0]);

    private final byte[] program;

    private Script(byte[] program) {
        this.program = program;
    }

    /// The empty script.
    public static Script empty() {
        return EMPTY;
    }

    /// Wraps serialized script bytes.
    public static Script of(byte[] program) {
        return new Script(Wire.copy(program));
    }

    /// Wraps script bytes given in hex.
    public static Script fromHex(String hex) {
        return new Script(Hash.fromHex(hex));
    }

    /// `OP_DUP OP_HASH160 <hash> OP_EQUALVERIFY OP_CHECKSIG`.
    public static Script p2pkh(byte[] pubKeyHash20) {
        check(pubKeyHash20, 20);
        return builder().op(OP_DUP).op(OP_HASH160).push(pubKeyHash20).op(OP_EQUALVERIFY).op(OP_CHECKSIG).build();
    }

    /// `OP_HASH160 <hash> OP_EQUAL`.
    public static Script p2sh(byte[] scriptHash20) {
        check(scriptHash20, 20);
        return builder().op(OP_HASH160).push(scriptHash20).op(OP_EQUAL).build();
    }

    /// `OP_0 <hash>`, native SegWit v0 key hash.
    public static Script p2wpkh(byte[] pubKeyHash20) {
        check(pubKeyHash20, 20);
        return builder().op(OP_0).push(pubKeyHash20).build();
    }

    /// `OP_0 <sha256(witnessScript)>`, native SegWit v0 script hash.
    public static Script p2wsh(byte[] scriptHash32) {
        check(scriptHash32, 32);
        return builder().op(OP_0).push(scriptHash32).build();
    }

    /// `OP_1 <x-only key>`, Taproot.
    public static Script p2tr(byte[] outputKey32) {
        check(outputKey32, 32);
        return builder().op(OP_1).push(outputKey32).build();
    }

    /// A witness output of any version (0 to 16) and program.
    public static Script witness(int version, byte[] program) {
        if (version < 0 || version > 16 || program.length < 2 || program.length > 40) {
            throw new IllegalArgumentException("bad witness program");
        }
        return builder().op(version == 0 ? OP_0 : OP_1 + version - 1).push(program).build();
    }

    /// `OP_RETURN <data>`, a provably unspendable data carrier.
    public static Script opReturn(byte[] data) {
        return builder().op(OP_RETURN).push(data).build();
    }

    /// `m <pubkey>... n OP_CHECKMULTISIG`, the bare multisig redeem or witness script.
    public static Script multisig(int m, byte[][] publicKeys) {
        if (m < 1 || m > publicKeys.length || publicKeys.length > 16) {
            throw new IllegalArgumentException("multisig takes 1 <= m <= n <= 16");
        }
        Builder b = builder().number(m);
        for (byte[] key : publicKeys) {
            b.push(key);
        }
        return b.number(publicKeys.length).op(OP_CHECKMULTISIG).build();
    }

    /// A builder that appends opcodes and minimal pushes.
    public static Builder builder() {
        return new Builder();
    }

    /// Appends opcodes and pushes to a script.
    public static final class Builder {
        private final Wire out = new Wire();

        Builder() {
        }

        /// Appends one opcode.
        public Builder op(int opcode) {
            if (opcode < 0 || opcode > 0xff) {
                throw new IllegalArgumentException("bad opcode");
            }
            out.u8(opcode);
            return this;
        }

        /// Appends a data push with the shortest push opcode for its length.
        public Builder push(byte[] data) {
            int n = data.length;
            if (n < OP_PUSHDATA1) {
                out.u8(n);
            } else if (n <= 0xff) {
                out.u8(OP_PUSHDATA1).u8(n);
            } else if (n <= 0xffff) {
                out.u8(OP_PUSHDATA2).u8(n & 0xff).u8(n >>> 8);
            } else {
                out.u8(OP_PUSHDATA4).u32(n);
            }
            out.bytes(data);
            return this;
        }

        /// Appends a number as script arithmetic reads it: `OP_0`, `OP_1NEGATE`, `OP_1`..`OP_16`,
        /// or a minimal little-endian sign-magnitude push.
        public Builder number(long value) {
            if (value == 0) {
                return op(OP_0);
            }
            if (value == -1) {
                return op(OP_1NEGATE);
            }
            if (value >= 1 && value <= 16) {
                return op(OP_1 + (int) value - 1);
            }
            return push(scriptNumber(value));
        }

        /// The script.
        public Script build() {
            return new Script(out.toBytes());
        }
    }

    static byte[] scriptNumber(long value) {
        boolean negative = value < 0;
        long abs = negative ? -value : value;
        Wire w = new Wire();
        while (abs != 0) {
            w.u8((int) (abs & 0xff));
            abs >>>= 8;
        }
        byte[] b = w.toBytes();
        if ((b[b.length - 1] & 0x80) != 0) {
            byte[] ext = new byte[b.length + 1];
            System.arraycopy(b, 0, ext, 0, b.length);
            ext[b.length] = (byte) (negative ? 0x80 : 0);
            return ext;
        }
        if (negative) {
            b[b.length - 1] |= (byte) 0x80;
        }
        return b;
    }

    // ---------------------------------------------------------------- classification

    /// Tells whether this is a P2PKH output.
    public boolean isP2pkh() {
        return program.length == 25 && at(0) == OP_DUP && at(1) == OP_HASH160 && at(2) == 20
                && at(23) == OP_EQUALVERIFY && at(24) == OP_CHECKSIG;
    }

    /// Tells whether this is a P2SH output.
    public boolean isP2sh() {
        return program.length == 23 && at(0) == OP_HASH160 && at(1) == 20 && at(22) == OP_EQUAL;
    }

    /// Tells whether this is a P2WPKH output.
    public boolean isP2wpkh() {
        return getWitnessVersion() == 0 && program.length == 22;
    }

    /// Tells whether this is a P2WSH output.
    public boolean isP2wsh() {
        return getWitnessVersion() == 0 && program.length == 34;
    }

    /// Tells whether this is a Taproot output.
    public boolean isP2tr() {
        return getWitnessVersion() == 1 && program.length == 34;
    }

    /// The witness version of a SegWit output, or -1 when this is not one.
    public int getWitnessVersion() {
        if (program.length < 4 || program.length > 42 || at(1) != program.length - 2) {
            return -1;
        }
        int v = at(0);
        if (v == OP_0) {
            return 0;
        }
        if (v >= OP_1 && v <= OP_16) {
            return v - OP_1 + 1;
        }
        return -1;
    }

    /// The witness program of a SegWit output, or `null`.
    public byte[] getWitnessProgram() {
        if (getWitnessVersion() < 0) {
            return null;
        }
        byte[] out = new byte[program.length - 2];
        System.arraycopy(program, 2, out, 0, out.length);
        return out;
    }

    /// The 20-byte hash a P2PKH or P2SH output commits to, the 20 or 32-byte program of a
    /// SegWit output, or `null` for any other script.
    public byte[] getDestinationHash() {
        if (isP2pkh()) {
            return slice(3, 20);
        }
        if (isP2sh()) {
            return slice(2, 20);
        }
        return getWitnessProgram();
    }

    private byte[] slice(int from, int n) {
        byte[] out = new byte[n];
        System.arraycopy(program, from, out, 0, n);
        return out;
    }

    private int at(int i) {
        return program[i] & 0xff;
    }

    /// A copy of the serialized script.
    public byte[] toBytes() {
        return Wire.copy(program);
    }

    /// The script length in bytes.
    public int length() {
        return program.length;
    }

    byte[] raw() {
        return program;
    }

    /// The serialized script in hex.
    public String toHex() {
        return Hash.toHex(program);
    }

    @Override
    public String toString() {
        return toHex();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Script)) {
            return false;
        }
        byte[] other = ((Script) o).program;
        if (other.length != program.length) {
            return false;
        }
        for (int iter = 0; iter < other.length; iter++) {
            if (other[iter] != program[iter]) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        int h = 1;
        for (byte programByte : program) {
            h = h * 31 + programByte;
        }
        return h;
    }

    private static void check(byte[] b, int n) {
        if (b == null || b.length != n) {
            throw new IllegalArgumentException("expected " + n + " bytes");
        }
    }
}
