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

import com.codename1.crypto.encoding.Hex;
import com.codename1.security.Hash;
import com.codename1.security.Schnorr;
import com.codename1.security.Secp256k1;
import com.codename1.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

/// A Bitcoin transaction: inputs spending previous outputs, new outputs, and the witness
/// data SegWit inputs carry.
///
/// Mutable while it is built and signed; [#serialize] writes the network form (with the
/// witness when any input has one) and [#parse] reads it back. The three signature hash
/// algorithms are here -- legacy ([#legacySighash]), SegWit v0 BIP-143
/// ([#segwitV0Sighash]) and Taproot BIP-341 ([#taprootSighash]) -- together with
/// one-call signers for the common single-key inputs:
///
/// ```java
/// BitcoinTransaction tx = new BitcoinTransaction();
/// tx.addInput(prevTxid, 0);
/// tx.addOutput(50000, BitcoinAddress.parse("bc1q...", BitcoinNetwork.MAINNET).getScriptPubKey());
/// tx.signP2wpkh(0, privateKey, 60000);
/// String hex = tx.toHex();   // broadcast
/// ```
@com.codename1.impl.SharedWithBackend
public final class BitcoinTransaction {
    /// Sign every input and output.
    public static final int SIGHASH_ALL = 0x01;
    /// Sign every input and no output.
    public static final int SIGHASH_NONE = 0x02;
    /// Sign every input and the output with the same index.
    public static final int SIGHASH_SINGLE = 0x03;
    /// Combined with another type: sign only this input.
    public static final int SIGHASH_ANYONECANPAY = 0x80;
    /// Taproot's default, which commits like `SIGHASH_ALL` and adds no byte to the signature.
    public static final int SIGHASH_DEFAULT = 0x00;

    /// The sequence that disables relative lock time and replace-by-fee signalling.
    public static final long SEQUENCE_FINAL = 0xffffffffL;
    /// The sequence an input signals BIP-125 replace-by-fee with.
    public static final long SEQUENCE_RBF = 0xfffffffdL;

    private long version = 2;
    private long lockTime;
    private final List<Input> inputs = new ArrayList<Input>();
    private final List<Output> outputs = new ArrayList<Output>();

    /// One input: the outpoint it spends, its unlocking script, sequence and witness.
    public static final class Input {
        private final byte[] prevTxidInternal;
        private final long prevIndex;
        private Script scriptSig = Script.empty();
        private long sequence;
        private List<byte[]> witness = new ArrayList<byte[]>();

        Input(byte[] prevTxidInternal, long prevIndex, long sequence) {
            this.prevTxidInternal = prevTxidInternal;
            this.prevIndex = prevIndex;
            this.sequence = sequence;
        }

        /// The spent transaction's id in display (big-endian) hex, as explorers show it.
        public String getPrevTxid() {
            return Hex.encode(Wire.reverse(prevTxidInternal));
        }

        /// The output index spent.
        public long getPrevIndex() {
            return prevIndex;
        }

        /// The unlocking script.
        public Script getScriptSig() {
            return scriptSig;
        }

        /// Replaces the unlocking script.
        public void setScriptSig(Script scriptSig) {
            this.scriptSig = scriptSig == null ? Script.empty() : scriptSig;
        }

        /// The sequence number.
        public long getSequence() {
            return sequence;
        }

        /// Replaces the sequence number.
        public void setSequence(long sequence) {
            this.sequence = sequence;
        }

        /// The witness stack items, in order.
        public List<byte[]> getWitness() {
            List<byte[]> out = new ArrayList<byte[]>();
            for (int iter = 0; iter < witness.size(); iter++) {
                out.add(Wire.copy(witness.get(iter)));
            }
            return out;
        }

        /// Replaces the witness stack.
        public void setWitness(List<byte[]> items) {
            witness = new ArrayList<byte[]>();
            if (items != null) {
                for (int iter = 0; iter < items.size(); iter++) {
                    witness.add(Wire.copy(items.get(iter)));
                }
            }
        }

        byte[] outpoint() {
            return new Wire().bytes(prevTxidInternal).u32(prevIndex).toBytes();
        }
    }

    /// One output: an amount in satoshis and the script that locks it.
    public static final class Output {
        private final long value;
        private final Script scriptPubKey;

        /// An output of `value` satoshis to `scriptPubKey`.
        public Output(long value, Script scriptPubKey) {
            if (value < 0 || value > 2100000000000000L) {
                throw new IllegalArgumentException("output value out of range");
            }
            this.value = value;
            this.scriptPubKey = scriptPubKey;
        }

        /// The amount in satoshis.
        public long getValue() {
            return value;
        }

        /// The locking script.
        public Script getScriptPubKey() {
            return scriptPubKey;
        }

        byte[] serialize() {
            return new Wire().u64(value).varBytes(scriptPubKey.raw()).toBytes();
        }
    }

    /// An empty version 2 transaction.
    public BitcoinTransaction() {
    }

    /// Adds an input spending output `index` of the transaction `prevTxid` (display hex), with
    /// [#SEQUENCE_RBF]. Returns its index.
    public int addInput(String prevTxid, long index) {
        return addInput(prevTxid, index, SEQUENCE_RBF);
    }

    /// Adds an input with an explicit sequence. Returns its index.
    public int addInput(String prevTxid, long index, long sequence) {
        byte[] txid = Hex.decode(prevTxid);
        if (txid.length != 32) {
            throw new IllegalArgumentException("a txid is 32 bytes");
        }
        inputs.add(new Input(Wire.reverse(txid), index, sequence));
        return inputs.size() - 1;
    }

    /// Adds an output. Returns its index.
    public int addOutput(long value, Script scriptPubKey) {
        outputs.add(new Output(value, scriptPubKey));
        return outputs.size() - 1;
    }

    /// Adds an output paying an address. Returns its index.
    public int addOutput(long value, BitcoinAddress address) {
        return addOutput(value, address.getScriptPubKey());
    }

    /// The inputs, live: changes to an `Input` change the transaction.
    public List<Input> getInputs() {
        return inputs;
    }

    /// The outputs.
    public List<Output> getOutputs() {
        return new ArrayList<Output>(outputs);
    }

    /// The version, 2 by default.
    public long getVersion() {
        return version;
    }

    /// Sets the version.
    public void setVersion(long version) {
        this.version = version;
    }

    /// The lock time.
    public long getLockTime() {
        return lockTime;
    }

    /// Sets the lock time (a block height below 500000000, a Unix time above).
    public void setLockTime(long lockTime) {
        this.lockTime = lockTime;
    }

    // ---------------------------------------------------------------- wire form

    /// Tells whether any input carries witness data.
    public boolean hasWitness() {
        for (int iter = 0; iter < inputs.size(); iter++) {
            if (!inputs.get(iter).witness.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /// The network serialization, with the witness when any input has one.
    public byte[] serialize() {
        return serialize(hasWitness());
    }

    byte[] serialize(boolean withWitness) {
        Wire w = new Wire().u32(version);
        if (withWitness) {
            w.u8(0).u8(1);
        }
        w.varInt(inputs.size());
        for (int iter = 0; iter < inputs.size(); iter++) {
            Input in = inputs.get(iter);
            w.bytes(in.outpoint()).varBytes(in.scriptSig.raw()).u32(in.sequence);
        }
        w.varInt(outputs.size());
        for (int iter = 0; iter < outputs.size(); iter++) {
            w.bytes(outputs.get(iter).serialize());
        }
        if (withWitness) {
            for (int iter = 0; iter < inputs.size(); iter++) {
                List<byte[]> items = inputs.get(iter).witness;
                w.varInt(items.size());
                for (int k = 0; k < items.size(); k++) {
                    w.varBytes(items.get(k));
                }
            }
        }
        return w.u32(lockTime).toBytes();
    }

    /// [#serialize] in hex, the form `sendrawtransaction` and every broadcast API take.
    public String toHex() {
        return Hex.encode(serialize());
    }

    /// The transaction id in display hex: the reversed double SHA-256 of the form without
    /// witness.
    public String getTxid() {
        return Hex.encode(Wire.reverse(Hash.sha256(Hash.sha256(serialize(false)))));
    }

    /// The witness transaction id in display hex.
    public String getWtxid() {
        return Hex.encode(Wire.reverse(Hash.sha256(Hash.sha256(serialize(true)))));
    }

    /// The virtual size in vbytes, which fees are priced in.
    public long getVirtualSize() {
        long base = serialize(false).length;
        long total = serialize().length;
        return (base * 3 + total + 3) / 4;
    }

    /// Reads a serialized transaction, with or without witness.
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: for truncated or trailing data
    public static BitcoinTransaction parse(byte[] raw) {
        return parse(raw, true);
    }

    /// Reads a serialized transaction. With `allowWitness` false the SegWit marker is not
    /// recognized, which is how a PSBT's unsigned transaction is read: a transaction with
    /// no inputs would otherwise begin with the marker byte.
    ///
    /// #### Throws
    ///
    /// - : for truncated or trailing data
    public static BitcoinTransaction parse(byte[] raw, boolean allowWitness) {
        Wire r = new Wire(raw);
        BitcoinTransaction tx = new BitcoinTransaction();
        tx.version = r.readU32();
        boolean witness = false;
        if (allowWitness && r.remaining() >= 2 && r.peek() == 0) {
            r.readU8();
            if (r.readU8() != 1) {
                throw new IllegalArgumentException("bad SegWit flag");
            }
            witness = true;
        }
        long nIn = r.readVarInt();
        checkCount(nIn, r);
        for (long iter = 0; iter < nIn; iter++) {
            byte[] txid = r.read(32);
            long index = r.readU32();
            Script sig = Script.of(r.readVarBytes());
            Input in = new Input(txid, index, r.readU32());
            in.scriptSig = sig;
            tx.inputs.add(in);
        }
        long nOut = r.readVarInt();
        checkCount(nOut, r);
        for (long iter = 0; iter < nOut; iter++) {
            long value = r.readU64();
            tx.outputs.add(new Output(value, Script.of(r.readVarBytes())));
        }
        if (witness) {
            for (int iter = 0; iter < tx.inputs.size(); iter++) {
                long items = r.readVarInt();
                checkCount(items, r);
                List<byte[]> stack = new ArrayList<byte[]>();
                for (long k = 0; k < items; k++) {
                    stack.add(r.readVarBytes());
                }
                tx.inputs.get(iter).witness = stack;
            }
            if (!tx.hasWitness()) {
                throw new IllegalArgumentException("SegWit flag with an empty witness");
            }
        }
        tx.lockTime = r.readU32();
        if (r.hasMore()) {
            throw new IllegalArgumentException("trailing data after the transaction");
        }
        return tx;
    }

    /// [#parse] from hex.
    public static BitcoinTransaction fromHex(String hex) {
        return parse(Hex.decode(hex));
    }

    private static void checkCount(long n, Wire r) {
        if (n < 0 || n > r.remaining()) {
            throw new IllegalArgumentException("count past the end of the data");
        }
    }

    // ---------------------------------------------------------------- signature hashes

    /// The legacy signature hash of input `index` against `scriptCode` (the spent output
    /// script, or the redeem script of P2SH).
    public byte[] legacySighash(int index, Script scriptCode, int sighashType) {
        int base = sighashType & 0x1f;
        if (base == SIGHASH_SINGLE && index >= outputs.size()) {
            // the consensus quirk: hash of the number 1
            byte[] one = new byte[32];
            one[0] = 1;
            return one;
        }
        boolean anyone = (sighashType & SIGHASH_ANYONECANPAY) != 0;
        Wire w = new Wire().u32(version);
        if (anyone) {
            w.varInt(1);
            Input in = inputs.get(index);
            w.bytes(in.outpoint()).varBytes(scriptCode.raw()).u32(in.sequence);
        } else {
            w.varInt(inputs.size());
            for (int iter = 0; iter < inputs.size(); iter++) {
                Input in = inputs.get(iter);
                w.bytes(in.outpoint());
                w.varBytes(iter == index ? scriptCode.raw() : new byte[0]);
                boolean zeroSeq = iter != index && (base == SIGHASH_NONE || base == SIGHASH_SINGLE);
                w.u32(zeroSeq ? 0 : in.sequence);
            }
        }
        if (base == SIGHASH_NONE) {
            w.varInt(0);
        } else if (base == SIGHASH_SINGLE) {
            w.varInt(index + 1);
            for (int iter = 0; iter < index; iter++) {
                w.u64(-1L).varInt(0);
            }
            w.bytes(outputs.get(index).serialize());
        } else {
            w.varInt(outputs.size());
            for (int iter = 0; iter < outputs.size(); iter++) {
                w.bytes(outputs.get(iter).serialize());
            }
        }
        w.u32(lockTime).u32(sighashType & 0xffffffffL);
        return Hash.sha256(Hash.sha256(w.toBytes()));
    }

    /// The BIP-143 signature hash of a SegWit v0 input.
    ///
    /// #### Parameters
    ///
    /// - `scriptCode`: for P2WPKH the P2PKH script of the key hash; for P2WSH the witness
    ///   script
    ///
    /// - `amount`: the value of the spent output in satoshis, which the signature commits to
    public byte[] segwitV0Sighash(int index, Script scriptCode, long amount, int sighashType) {
        int base = sighashType & 0x1f;
        boolean anyone = (sighashType & SIGHASH_ANYONECANPAY) != 0;
        byte[] zero = new byte[32];
        byte[] hashPrevouts = zero;
        byte[] hashSequence = zero;
        byte[] hashOutputs = zero;
        if (!anyone) {
            hashPrevouts = dsha(prevouts());
        }
        if (!anyone && base != SIGHASH_SINGLE && base != SIGHASH_NONE) {
            hashSequence = dsha(sequences());
        }
        if (base != SIGHASH_SINGLE && base != SIGHASH_NONE) {
            hashOutputs = dsha(allOutputs());
        } else if (base == SIGHASH_SINGLE && index < outputs.size()) {
            hashOutputs = dsha(outputs.get(index).serialize());
        }
        Input in = inputs.get(index);
        Wire w = new Wire().u32(version).bytes(hashPrevouts).bytes(hashSequence).bytes(in.outpoint())
                .varBytes(scriptCode.raw()).u64(amount).u32(in.sequence).bytes(hashOutputs).u32(lockTime)
                .u32(sighashType & 0xffffffffL);
        return dsha(w.toBytes());
    }

    /// The BIP-341 signature hash of a Taproot input, key path, no annex.
    ///
    /// #### Parameters
    ///
    /// - `spent`: the output every input spends, in input order -- Taproot signatures commit
    ///   to all their amounts and scripts
    public byte[] taprootSighash(int index, List<Output> spent, int sighashType) {
        return taprootSighash(index, spent, sighashType, null);
    }

    /// The BIP-341 signature hash of a Taproot script-path spend of `leafHash` (BIP-342
    /// extension, no annex, code separator position none); `null` for the key path.
    public byte[] taprootSighash(int index, List<Output> spent, int sighashType, byte[] leafHash) {
        if (spent == null || spent.size() != inputs.size()) {
            throw new IllegalArgumentException("Taproot needs the spent output of every input");
        }
        int base = sighashType & 0x03;
        boolean anyone = (sighashType & SIGHASH_ANYONECANPAY) != 0;
        if (sighashType != SIGHASH_DEFAULT && (base == 0 || (sighashType & ~0x83) != 0)) {
            throw new IllegalArgumentException("bad Taproot sighash type");
        }
        Wire w = new Wire().u8(0).u8(sighashType).u32(version).u32(lockTime);
        if (!anyone) {
            w.bytes(Hash.sha256(prevouts()));
            Wire amounts = new Wire();
            Wire scripts = new Wire();
            for (int iter = 0; iter < spent.size(); iter++) {
                amounts.u64(spent.get(iter).value);
                scripts.varBytes(spent.get(iter).scriptPubKey.raw());
            }
            w.bytes(Hash.sha256(amounts.toBytes()));
            w.bytes(Hash.sha256(scripts.toBytes()));
            w.bytes(Hash.sha256(sequences()));
        }
        if (base != SIGHASH_NONE && base != SIGHASH_SINGLE) {
            w.bytes(Hash.sha256(allOutputs()));
        }
        int spendType = leafHash == null ? 0 : 2;
        w.u8(spendType);
        Input in = inputs.get(index);
        if (anyone) {
            w.bytes(in.outpoint()).u64(spent.get(index).value).varBytes(spent.get(index).scriptPubKey.raw())
                    .u32(in.sequence);
        } else {
            w.u32(index);
        }
        if (base == SIGHASH_SINGLE) {
            if (index >= outputs.size()) {
                throw new IllegalArgumentException("SIGHASH_SINGLE without a matching output");
            }
            w.bytes(Hash.sha256(outputs.get(index).serialize()));
        }
        if (leafHash != null) {
            w.bytes(leafHash).u8(0).u32(0xffffffffL);
        }
        return Taproot.taggedHash("TapSighash", w.toBytes());
    }

    private byte[] prevouts() {
        Wire w = new Wire();
        for (int iter = 0; iter < inputs.size(); iter++) {
            w.bytes(inputs.get(iter).outpoint());
        }
        return w.toBytes();
    }

    private byte[] sequences() {
        Wire w = new Wire();
        for (int iter = 0; iter < inputs.size(); iter++) {
            w.u32(inputs.get(iter).sequence);
        }
        return w.toBytes();
    }

    private byte[] allOutputs() {
        Wire w = new Wire();
        for (int iter = 0; iter < outputs.size(); iter++) {
            w.bytes(outputs.get(iter).serialize());
        }
        return w.toBytes();
    }

    private static byte[] dsha(byte[] b) {
        return Hash.sha256(Hash.sha256(b));
    }

    // ---------------------------------------------------------------- signing

    /// Signs a P2PKH input with `SIGHASH_ALL` and sets its `scriptSig`.
    public void signP2pkh(int index, byte[] privateKey, boolean compressed) {
        byte[] pub = Secp256k1.publicKey(privateKey, compressed);
        Script code = Script.p2pkh(Hash.hash160(pub));
        byte[] sig = ecdsa(legacySighash(index, code, SIGHASH_ALL), privateKey, SIGHASH_ALL);
        inputs.get(index).setScriptSig(Script.builder().push(sig).push(pub).build());
    }

    /// Signs a P2WPKH input with `SIGHASH_ALL` and sets its witness.
    public void signP2wpkh(int index, byte[] privateKey, long amount) {
        byte[] pub = Secp256k1.publicKey(privateKey, true);
        byte[] sig = ecdsa(segwitV0Sighash(index, Script.p2pkh(Hash.hash160(pub)), amount, SIGHASH_ALL), privateKey,
                SIGHASH_ALL);
        Input in = inputs.get(index);
        in.witness = new ArrayList<byte[]>();
        in.witness.add(sig);
        in.witness.add(pub);
        in.setScriptSig(Script.empty());
    }

    /// Signs a BIP-49 P2SH-P2WPKH input with `SIGHASH_ALL`.
    public void signP2shP2wpkh(int index, byte[] privateKey, long amount) {
        signP2wpkh(index, privateKey, amount);
        byte[] pub = Secp256k1.publicKey(privateKey, true);
        inputs.get(index).setScriptSig(Script.builder().push(Script.p2wpkh(Hash.hash160(pub)).raw()).build());
    }

    /// Signs a BIP-86 Taproot key-path input with `SIGHASH_DEFAULT` and sets its witness.
    ///
    /// #### Parameters
    ///
    /// - `privateKey`: the untweaked internal private key
    ///
    /// - `spent`: the output every input spends, in input order
    public void signP2trKeyPath(int index, byte[] privateKey, List<Output> spent) {
        signP2trKeyPath(index, privateKey, null, spent, SIGHASH_DEFAULT);
    }

    /// Signs a Taproot key-path input, committing to a script tree's Merkle root.
    public void signP2trKeyPath(int index, byte[] privateKey, byte[] merkleRoot, List<Output> spent,
            int sighashType) {
        byte[] tweaked = Taproot.tweakPrivateKey(privateKey, merkleRoot);
        try {
            byte[] sig = Schnorr.sign(taprootSighash(index, spent, sighashType), tweaked, SecureRandom.bytes(32));
            if (sighashType != SIGHASH_DEFAULT) {
                byte[] withType = new byte[65];
                System.arraycopy(sig, 0, withType, 0, 64);
                withType[64] = (byte) sighashType;
                sig = withType;
            }
            Input in = inputs.get(index);
            in.witness = new ArrayList<byte[]>();
            in.witness.add(sig);
            in.setScriptSig(Script.empty());
        } finally {
            Wif.wipe(tweaked);
        }
    }

    /// A DER ECDSA signature with the sighash byte appended, as legacy and SegWit v0 inputs
    /// carry it.
    public static byte[] ecdsa(byte[] sighash, byte[] privateKey, int sighashType) {
        Secp256k1.EcdsaSignature sig = Secp256k1.sign(sighash, privateKey);
        byte[] der = Der.encode(sig.r(), sig.s());
        byte[] out = new byte[der.length + 1];
        System.arraycopy(der, 0, out, 0, der.length);
        out[der.length] = (byte) sighashType;
        return out;
    }
}
