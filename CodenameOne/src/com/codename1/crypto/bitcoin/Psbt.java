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

import com.codename1.crypto.encoding.Base64;
import com.codename1.crypto.hd.DerivationPath;
import com.codename1.security.Hash;
import com.codename1.security.Secp256k1;
import java.util.ArrayList;
import java.util.List;

/// A Partially Signed Bitcoin Transaction (BIP-174, with the BIP-371 Taproot fields): the
/// file hardware wallets, multisig coordinators and watch-only servers pass around until
/// every signer has signed.
///
/// ```java
/// Psbt psbt = Psbt.fromBase64(fromCoordinator);
/// psbt.sign(0, privateKey);           // any input this key can sign
/// psbt.finalizeAll();
/// String hex = psbt.extract().toHex();
/// ```
///
/// [#sign] handles P2PKH, P2WPKH, P2SH-P2WPKH, Taproot key-path, and P2SH/P2WSH multisig
/// inputs; [#finalizeInput] builds the final scripts for the same kinds. Fields this class
/// does not interpret are kept and written back unchanged.
@com.codename1.impl.SharedWithBackend
public final class Psbt {
    static final int IN_NON_WITNESS_UTXO = 0x00;
    static final int IN_WITNESS_UTXO = 0x01;
    static final int IN_PARTIAL_SIG = 0x02;
    static final int IN_SIGHASH_TYPE = 0x03;
    static final int IN_REDEEM_SCRIPT = 0x04;
    static final int IN_WITNESS_SCRIPT = 0x05;
    static final int IN_BIP32_DERIVATION = 0x06;
    static final int IN_FINAL_SCRIPTSIG = 0x07;
    static final int IN_FINAL_SCRIPTWITNESS = 0x08;
    static final int IN_TAP_KEY_SIG = 0x13;
    static final int IN_TAP_BIP32_DERIVATION = 0x16;
    static final int IN_TAP_INTERNAL_KEY = 0x17;
    static final int IN_TAP_MERKLE_ROOT = 0x18;

    private static final byte[] MAGIC = {0x70, 0x73, 0x62, 0x74, (byte) 0xff};

    private final BitcoinTransaction tx;
    private final List<byte[][]> global = new ArrayList<byte[][]>();
    private final List<List<byte[][]>> inputMaps = new ArrayList<List<byte[][]>>();
    private final List<List<byte[][]>> outputMaps = new ArrayList<List<byte[][]>>();

    private Psbt(BitcoinTransaction tx) {
        this.tx = tx;
    }

    /// A PSBT for an unsigned transaction; its scripts and witnesses are cleared.
    public static Psbt fromTransaction(BitcoinTransaction unsigned) {
        BitcoinTransaction copy = BitcoinTransaction.parse(unsigned.serialize(false), false);
        for (int iter = 0; iter < copy.getInputs().size(); iter++) {
            copy.getInputs().get(iter).setScriptSig(Script.empty());
            copy.getInputs().get(iter).setWitness(null);
        }
        Psbt p = new Psbt(copy);
        for (int iter = 0; iter < copy.getInputs().size(); iter++) {
            p.inputMaps.add(new ArrayList<byte[][]>());
        }
        for (int iter = 0; iter < copy.getOutputs().size(); iter++) {
            p.outputMaps.add(new ArrayList<byte[][]>());
        }
        return p;
    }

    /// Reads a binary PSBT.
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: for a malformed PSBT, a duplicate key, or a global
    ///   transaction that is not unsigned
    public static Psbt parse(byte[] data) {
        Wire r = new Wire(data);
        byte[] magic = r.read(5);
        for (int iter = 0; iter < 5; iter++) {
            if (magic[iter] != MAGIC[iter]) {
                throw new IllegalArgumentException("not a PSBT");
            }
        }
        List<byte[][]> global = checkKeys(readMap(r), GLOBAL_KEY_SHAPES);
        byte[] unsigned = null;
        for (byte[][] kv : global) {
            if (kv[0].length == 1 && kv[0][0] == 0) {
                unsigned = kv[1];
            }
        }
        if (unsigned == null) {
            throw new IllegalArgumentException("PSBT without an unsigned transaction");
        }
        BitcoinTransaction tx = BitcoinTransaction.parse(unsigned, false);
        for (int iter = 0; iter < tx.getInputs().size(); iter++) {
            BitcoinTransaction.Input in = tx.getInputs().get(iter);
            if (in.getScriptSig().length() != 0 || !in.getWitness().isEmpty()) {
                throw new IllegalArgumentException("the PSBT's transaction is not unsigned");
            }
        }
        Psbt p = new Psbt(tx);
        for (byte[][] globalItem : global) {
            if (!(globalItem[0].length == 1 && globalItem[0][0] == 0)) {
                p.global.add(globalItem);
            }
        }
        for (int iter = 0; iter < tx.getInputs().size(); iter++) {
            p.inputMaps.add(checkKeys(readMap(r), INPUT_KEY_SHAPES));
        }
        for (int iter = 0; iter < tx.getOutputs().size(); iter++) {
            p.outputMaps.add(checkKeys(readMap(r), OUTPUT_KEY_SHAPES));
        }
        if (r.hasMore()) {
            throw new IllegalArgumentException("trailing data after the PSBT");
        }
        return p;
    }

    /// Reads a Base64 PSBT, the form wallets exchange.
    public static Psbt fromBase64(String base64) {
        return parse(Base64.decode(base64));
    }

    // Key shapes of the typed keys BIP-174 and BIP-371 define, indexed by type: 1 means the
    // key is the type byte alone, 2 a type byte followed by a 33 or 65 byte public key, 3 a
    // type byte followed by a 32 byte x-only key, 0 a type this reader leaves unchecked.
    private static final byte[] GLOBAL_KEY_SHAPES = {1, 0, 1, 0, 0, 0, 0, 0};
    private static final byte[] INPUT_KEY_SHAPES = {1, 1, 2, 1, 1, 1, 2, 1, 1, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 1, 0, 0, 3, 1, 1};
    private static final byte[] OUTPUT_KEY_SHAPES = {1, 1, 2, 0, 0, 1, 0, 3};

    // Rejects a known typed key whose length its type does not allow; an unknown type is kept
    // as it is, which is what lets a newer PSBT pass through this reader unharmed.
    private static List<byte[][]> checkKeys(List<byte[][]> map, byte[] shapes) {
        for (byte[][] mapElem : map) {
            byte[] key = mapElem[0];
            int type = key[0] & 0xff;
            int shape = type < shapes.length ? shapes[type] : 0;
            boolean ok;
            if (shape == 1) {
                ok = key.length == 1;
            } else if (shape == 2) {
                ok = (key.length == 34 && (key[1] == 2 || key[1] == 3)) || (key.length == 66 && key[1] == 4);
            } else if (shape == 3) {
                ok = key.length == 33;
            } else {
                ok = true;
            }
            if (!ok) {
                throw new IllegalArgumentException("malformed PSBT key of type " + type);
            }
        }
        return map;
    }

    private static List<byte[][]> readMap(Wire r) {
        List<byte[][]> out = new ArrayList<byte[][]>();
        while (true) {
            byte[] key = r.readVarBytes();
            if (key.length == 0) {
                return out;
            }
            for (byte[][] outItem : out) {
                if (java.util.Arrays.equals(outItem[0], key)) {
                    throw new IllegalArgumentException("duplicate PSBT key");
                }
            }
            out.add(new byte[][] {key, r.readVarBytes()});
        }
    }

    /// The binary PSBT.
    public byte[] serialize() {
        Wire w = new Wire().bytes(MAGIC);
        w.varBytes(new byte[] {0}).varBytes(tx.serialize(false));
        writeMap(w, global);
        for (List<byte[][]> inputMapsItem : inputMaps) {
            writeMap(w, inputMapsItem);
        }
        for (List<byte[][]> outputMapsItem : outputMaps) {
            writeMap(w, outputMapsItem);
        }
        return w.toBytes();
    }

    /// The Base64 PSBT.
    public String toBase64() {
        return Base64.encode(serialize());
    }

    private static void writeMap(Wire w, List<byte[][]> map) {
        for (byte[][] mapElem : map) {
            w.varBytes(mapElem[0]).varBytes(mapElem[1]);
        }
        w.u8(0);
    }

    /// The unsigned transaction.
    public BitcoinTransaction getTransaction() {
        return BitcoinTransaction.parse(tx.serialize(false), false);
    }

    // ---------------------------------------------------------------- input fields

    /// Records the output a SegWit input spends.
    public void setWitnessUtxo(int input, BitcoinTransaction.Output spent) {
        Wire w = new Wire().u64(spent.getValue()).varBytes(spent.getScriptPubKey().raw());
        put(inputMaps.get(input), new byte[] {IN_WITNESS_UTXO}, w.toBytes());
    }

    /// Records the whole previous transaction of an input, which legacy inputs need.
    public void setNonWitnessUtxo(int input, BitcoinTransaction previous) {
        put(inputMaps.get(input), new byte[] {IN_NON_WITNESS_UTXO}, previous.serialize());
    }

    /// Records the redeem script of a P2SH input.
    public void setRedeemScript(int input, Script redeem) {
        put(inputMaps.get(input), new byte[] {IN_REDEEM_SCRIPT}, redeem.raw());
    }

    /// Records the witness script of a P2WSH input.
    public void setWitnessScript(int input, Script witnessScript) {
        put(inputMaps.get(input), new byte[] {IN_WITNESS_SCRIPT}, witnessScript.raw());
    }

    /// Records the x-only internal key of a Taproot input.
    public void setTapInternalKey(int input, byte[] xOnly) {
        put(inputMaps.get(input), new byte[] {IN_TAP_INTERNAL_KEY}, xOnly);
    }

    /// Records the BIP-32 origin of a key an input needs, so a hardware wallet can find it.
    public void addBip32Derivation(int input, byte[] publicKey, int masterFingerprint, long[] path) {
        Wire w = new Wire();
        w.u8(masterFingerprint >>> 24).u8(masterFingerprint >>> 16).u8(masterFingerprint >>> 8).u8(masterFingerprint);
        for (long pathElem : path) {
            w.u32(pathElem);
        }
        put(inputMaps.get(input), Wire.concat(new byte[] {IN_BIP32_DERIVATION}, publicKey), w.toBytes());
    }

    /// The output an input spends, from its witness or non-witness UTXO, or `null`.
    public BitcoinTransaction.Output getSpentOutput(int input) {
        byte[] witnessUtxo = get(inputMaps.get(input), new byte[] {IN_WITNESS_UTXO});
        if (witnessUtxo != null) {
            Wire r = new Wire(witnessUtxo);
            long value = r.readU64();
            return new BitcoinTransaction.Output(value, Script.of(r.readVarBytes()));
        }
        byte[] full = get(inputMaps.get(input), new byte[] {IN_NON_WITNESS_UTXO});
        if (full != null) {
            BitcoinTransaction prev = BitcoinTransaction.parse(full);
            BitcoinTransaction.Input in = tx.getInputs().get(input);
            if (!prev.getTxid().equals(in.getPrevTxid())) {
                throw new IllegalArgumentException("non-witness UTXO does not match input " + input);
            }
            return prev.getOutputs().get((int) in.getPrevIndex());
        }
        return null;
    }

    /// The BIP-32 origins recorded for an input's keys, both the ECDSA kind and the Taproot
    /// kind (BIP-371, whose public keys are 32-byte x-only). A wallet signs the inputs whose
    /// master fingerprint is its own.
    public List<Derivation> getBip32Derivations(int input) {
        List<Derivation> out = new ArrayList<Derivation>();
        List<byte[][]> map = inputMaps.get(input);
        for (byte[][] mapElem : map) {
            byte[] k = mapElem[0];
            int type = k[0] & 0xff;
            if (type != IN_BIP32_DERIVATION && type != IN_TAP_BIP32_DERIVATION) {
                continue;
            }
            Wire r = new Wire(mapElem[1]);
            if (type == IN_TAP_BIP32_DERIVATION) {
                long leaves = r.readVarInt();
                if (leaves > r.remaining() / 32) {
                    throw new IllegalArgumentException("malformed Taproot derivation");
                }
                for (long leaf = 0; leaf < leaves; leaf++) {
                    r.read(32);
                }
            }
            byte[] fp = r.read(4);
            int fingerprint = ((fp[0] & 0xff) << 24) | ((fp[1] & 0xff) << 16) | ((fp[2] & 0xff) << 8) | (fp[3] & 0xff);
            long[] path = new long[r.remaining() / 4];
            if (r.remaining() % 4 != 0) {
                throw new IllegalArgumentException("malformed BIP-32 derivation");
            }
            for (int iter = 0; iter < path.length; iter++) {
                path[iter] = r.readU32();
            }
            byte[] pub = new byte[k.length - 1];
            System.arraycopy(k, 1, pub, 0, pub.length);
            out.add(new Derivation(pub, fingerprint, DerivationPath.of(path)));
        }
        return out;
    }

    /// Records the Taproot (BIP-371) origin of an input's x-only key, for a key-path spend.
    public void addTapBip32Derivation(int input, byte[] xOnlyKey, int masterFingerprint, long[] path) {
        Wire w = new Wire();
        w.varInt(0);
        w.u8(masterFingerprint >>> 24).u8(masterFingerprint >>> 16).u8(masterFingerprint >>> 8).u8(masterFingerprint);
        for (long pathItem : path) {
            w.u32(pathItem);
        }
        put(inputMaps.get(input), Wire.concat(new byte[] {IN_TAP_BIP32_DERIVATION}, xOnlyKey), w.toBytes());
    }

    /// One key origin: a public key, the fingerprint of the master it descends from and the
    /// path from that master.
    public static final class Derivation {
        private final byte[] publicKey;
        private final int fingerprint;
        private final DerivationPath path;

        Derivation(byte[] publicKey, int fingerprint, DerivationPath path) {
            this.publicKey = publicKey;
            this.fingerprint = fingerprint;
            this.path = path;
        }

        /// The public key, 33 or 65 bytes, or 32 x-only bytes for a Taproot origin.
        public byte[] getPublicKey() {
            return Wire.copy(publicKey);
        }

        /// The master key fingerprint.
        public int getMasterFingerprint() {
            return fingerprint;
        }

        /// The derivation path from the master.
        public DerivationPath getPath() {
            return path;
        }
    }

    /// The signatures collected for an input: `{publicKey, signature}` pairs.
    public List<byte[][]> getPartialSignatures(int input) {
        List<byte[][]> out = new ArrayList<byte[][]>();
        List<byte[][]> map = inputMaps.get(input);
        for (byte[][] mapElem : map) {
            byte[] key = mapElem[0];
            if (key[0] == IN_PARTIAL_SIG && key.length > 1) {
                byte[] pub = new byte[key.length - 1];
                System.arraycopy(key, 1, pub, 0, pub.length);
                out.add(new byte[][] {pub, Wire.copy(mapElem[1])});
            }
        }
        return out;
    }

    // ---------------------------------------------------------------- signing

    /// Signs one input with `privateKey` if the key belongs to it, answering whether it did.
    /// The sighash type is the input's own, or `SIGHASH_ALL` (Taproot: `SIGHASH_DEFAULT`).
    ///
    /// #### Throws
    ///
    /// - `IllegalStateException`: when the input lacks the UTXO (or the scripts) it needs
    public boolean sign(int input, byte[] privateKey) {
        BitcoinKey key = BitcoinKey.fromPrivateKey(privateKey);
        try {
            return sign(input, key);
        } finally {
            key.destroy();
        }
    }

    /// Signs one input with a key that may live elsewhere, such as a wallet or a hardware
    /// device; see [#sign(int, byte[])] for what is signed.
    public boolean sign(int input, BitcoinKey key) {
        List<byte[][]> map = inputMaps.get(input);
        BitcoinTransaction.Output spent = getSpentOutput(input);
        if (spent == null) {
            throw new IllegalStateException("input " + input + " has no UTXO");
        }
        Script spk = spent.getScriptPubKey();
        byte[] typeBytes = get(map, new byte[] {IN_SIGHASH_TYPE});
        int sighash = typeBytes == null ? -1 : (int) new Wire(typeBytes).readU32();
        if (spk.isP2tr()) {
            byte[] merkle = get(map, new byte[] {IN_TAP_MERKLE_ROOT});
            byte[] internal = key.getInternalKey();
            if (!java.util.Arrays.equals(Taproot.outputKey(internal, merkle), spk.getWitnessProgram())) {
                return false;
            }
            int type = sighash < 0 ? BitcoinTransaction.SIGHASH_DEFAULT : sighash;
            List<BitcoinTransaction.Output> all = new ArrayList<BitcoinTransaction.Output>();
            for (int iter = 0; iter < tx.getInputs().size(); iter++) {
                BitcoinTransaction.Output o = getSpentOutput(iter);
                if (o == null) {
                    throw new IllegalStateException("Taproot signing needs the UTXO of every input");
                }
                all.add(o);
            }
            byte[] sig = key.signTaproot(tx.taprootSighash(input, all, type), merkle);
            if (type != BitcoinTransaction.SIGHASH_DEFAULT) {
                sig = Wire.concat(sig, new byte[] {(byte) type});
            }
            put(map, new byte[] {IN_TAP_KEY_SIG}, sig);
            put(map, new byte[] {IN_TAP_INTERNAL_KEY}, internal);
            return true;
        }
        int type = sighash < 0 ? BitcoinTransaction.SIGHASH_ALL : sighash;
        byte[] compressed = key.getPublicKey();
        byte[] uncompressed = key.getUncompressedPublicKey();
        Script redeem = script(map, IN_REDEEM_SCRIPT);
        Script witnessScript = script(map, IN_WITNESS_SCRIPT);
        Script inner = spk;
        if (spk.isP2sh()) {
            if (redeem == null) {
                throw new IllegalStateException("P2SH input " + input + " has no redeem script");
            }
            inner = redeem;
        }
        byte[] pub;
        byte[] sighashBytes;
        if (inner.isP2wpkh()) {
            pub = compressed;
            if (!java.util.Arrays.equals(inner.getWitnessProgram(), Hash.hash160(pub))) {
                return false;
            }
            sighashBytes = tx.segwitV0Sighash(input, Script.p2pkh(Hash.hash160(pub)), spent.getValue(), type);
        } else if (inner.isP2wsh()) {
            if (witnessScript == null) {
                throw new IllegalStateException("P2WSH input " + input + " has no witness script");
            }
            pub = keyIn(witnessScript, compressed, uncompressed);
            if (pub == null) {
                return false;
            }
            sighashBytes = tx.segwitV0Sighash(input, witnessScript, spent.getValue(), type);
        } else if (inner.isP2pkh()) {
            byte[] hash = inner.getDestinationHash();
            if (java.util.Arrays.equals(hash, Hash.hash160(compressed))) {
                pub = compressed;
            } else if (java.util.Arrays.equals(hash, Hash.hash160(uncompressed))) {
                pub = uncompressed;
            } else {
                return false;
            }
            sighashBytes = tx.legacySighash(input, inner, type);
        } else {
            pub = keyIn(inner, compressed, uncompressed);
            if (pub == null) {
                return false;
            }
            sighashBytes = tx.legacySighash(input, inner, type);
        }
        Secp256k1.EcdsaSignature es = key.signEcdsa(sighashBytes);
        put(map, Wire.concat(new byte[] {IN_PARTIAL_SIG}, pub),
                Wire.concat(Der.encode(es.r(), es.s()), new byte[] {(byte) type}));
        return true;
    }

    /// Signs every input the key belongs to, answering how many.
    public int signAll(byte[] privateKey) {
        BitcoinKey key = BitcoinKey.fromPrivateKey(privateKey);
        try {
            return signAll(key);
        } finally {
            key.destroy();
        }
    }

    /// Signs every input a key belongs to, answering how many.
    public int signAll(BitcoinKey key) {
        int n = 0;
        for (int iter = 0; iter < inputMaps.size(); iter++) {
            if (getSpentOutput(iter) != null && sign(iter, key)) {
                n++;
            }
        }
        return n;
    }

    // ---------------------------------------------------------------- finalizing

    /// Builds the final `scriptSig` and witness of an input from its signatures, answering
    /// whether it could. A multisig input finalizes once it holds `m` signatures.
    public boolean finalizeInput(int input) {
        List<byte[][]> map = inputMaps.get(input);
        if (get(map, new byte[] {IN_FINAL_SCRIPTSIG}) != null || get(map, new byte[] {IN_FINAL_SCRIPTWITNESS}) != null) {
            return true;
        }
        BitcoinTransaction.Output spent = getSpentOutput(input);
        if (spent == null) {
            return false;
        }
        Script spk = spent.getScriptPubKey();
        List<byte[]> witness = null;
        Script scriptSig = null;
        if (spk.isP2tr()) {
            byte[] sig = get(map, new byte[] {IN_TAP_KEY_SIG});
            if (sig == null) {
                return false;
            }
            witness = new ArrayList<byte[]>();
            witness.add(sig);
        } else {
            List<byte[][]> sigs = getPartialSignatures(input);
            Script redeem = script(map, IN_REDEEM_SCRIPT);
            Script inner = spk.isP2sh() ? redeem : spk;
            if (inner == null || sigs.isEmpty()) {
                return false;
            }
            if (inner.isP2wpkh()) {
                witness = new ArrayList<byte[]>();
                witness.add(sigs.get(0)[1]);
                witness.add(sigs.get(0)[0]);
            } else if (inner.isP2wsh()) {
                Script ws = script(map, IN_WITNESS_SCRIPT);
                List<byte[]> stack = multisigStack(ws, sigs);
                if (stack == null) {
                    return false;
                }
                stack.add(ws.raw());
                witness = stack;
            } else if (inner.isP2pkh()) {
                scriptSig = Script.builder().push(sigs.get(0)[1]).push(sigs.get(0)[0]).build();
            } else {
                List<byte[]> stack = multisigStack(inner, sigs);
                if (stack == null) {
                    return false;
                }
                Script.Builder b = Script.builder();
                for (byte[] stackItem : stack) {
                    if (stackItem.length == 0) {
                        b.op(Script.OP_0);
                    } else {
                        b.push(stackItem);
                    }
                }
                scriptSig = b.build();
            }
            if (spk.isP2sh()) {
                byte[] pushRedeem = Script.builder().push(redeem.raw()).build().raw();
                scriptSig = Script.of(scriptSig == null ? pushRedeem : Wire.concat(scriptSig.raw(), pushRedeem));
            }
        }
        List<byte[][]> kept = new ArrayList<byte[][]>();
        for (byte[][] mapElem : map) {
            int k = mapElem[0][0] & 0xff;
            if (k == IN_NON_WITNESS_UTXO || k == IN_WITNESS_UTXO || k > IN_TAP_MERKLE_ROOT) {
                kept.add(mapElem);
            }
        }
        map.clear();
        map.addAll(kept);
        if (scriptSig != null) {
            put(map, new byte[] {IN_FINAL_SCRIPTSIG}, scriptSig.raw());
        }
        if (witness != null) {
            Wire w = new Wire().varInt(witness.size());
            for (byte[] witnessItem : witness) {
                w.varBytes(witnessItem);
            }
            put(map, new byte[] {IN_FINAL_SCRIPTWITNESS}, w.toBytes());
        }
        return true;
    }

    /// Finalizes every input, answering whether all of them are final.
    public boolean finalizeAll() {
        boolean all = true;
        for (int iter = 0; iter < inputMaps.size(); iter++) {
            all &= finalizeInput(iter);
        }
        return all;
    }

    /// The signed network transaction.
    ///
    /// #### Throws
    ///
    /// - `IllegalStateException`: when an input is not final
    public BitcoinTransaction extract() {
        BitcoinTransaction out = getTransaction();
        for (int iter = 0; iter < inputMaps.size(); iter++) {
            List<byte[][]> map = inputMaps.get(iter);
            byte[] sig = get(map, new byte[] {IN_FINAL_SCRIPTSIG});
            byte[] wit = get(map, new byte[] {IN_FINAL_SCRIPTWITNESS});
            if (sig == null && wit == null) {
                throw new IllegalStateException("input " + iter + " is not final");
            }
            BitcoinTransaction.Input in = out.getInputs().get(iter);
            if (sig != null) {
                in.setScriptSig(Script.of(sig));
            }
            if (wit != null) {
                Wire r = new Wire(wit);
                long n = r.readVarInt();
                List<byte[]> stack = new ArrayList<byte[]>();
                for (long k = 0; k < n; k++) {
                    stack.add(r.readVarBytes());
                }
                in.setWitness(stack);
            }
        }
        return out;
    }

    // ---------------------------------------------------------------- helpers

    /// The signatures of a bare multisig script, in script key order, preceded by the
    /// `CHECKMULTISIG` dummy; `null` while fewer than `m` are present.
    private static List<byte[]> multisigStack(Script ms, List<byte[][]> sigs) {
        if (ms == null) {
            return null;
        }
        List<byte[]> keys = new ArrayList<byte[]>();
        int m = parseMultisig(ms, keys);
        if (m < 0) {
            return null;
        }
        List<byte[]> stack = new ArrayList<byte[]>();
        stack.add(new byte[0]);
        for (int k = 0; k < keys.size() && stack.size() <= m; k++) {
            for (byte[][] sigsItem : sigs) {
                if (java.util.Arrays.equals(keys.get(k), sigsItem[0])) {
                    stack.add(sigsItem[1]);
                    break;
                }
            }
        }
        return stack.size() == m + 1 ? stack : null;
    }

    /// Reads `m <key>... n OP_CHECKMULTISIG`, answering m, or -1 for any other script.
    static int parseMultisig(Script s, List<byte[]> keys) {
        byte[] p = s.raw();
        if (p.length < 3 || (p[p.length - 1] & 0xff) != Script.OP_CHECKMULTISIG) {
            return -1;
        }
        int m = (p[0] & 0xff) - Script.OP_1 + 1;
        int n = (p[p.length - 2] & 0xff) - Script.OP_1 + 1;
        if (m < 1 || m > 16 || n < m || n > 16) {
            return -1;
        }
        int pos = 1;
        while (pos < p.length - 2) {
            int len = p[pos] & 0xff;
            if ((len != 33 && len != 65) || pos + 1 + len > p.length - 2) {
                return -1;
            }
            byte[] key = new byte[len];
            System.arraycopy(p, pos + 1, key, 0, len);
            keys.add(key);
            pos += 1 + len;
        }
        return keys.size() == n ? m : -1;
    }

    private static byte[] keyIn(Script s, byte[] compressed, byte[] uncompressed) {
        byte[] p = s.raw();
        if (contains(p, compressed)) {
            return compressed;
        }
        if (contains(p, uncompressed)) {
            return uncompressed;
        }
        return null;
    }

    private static boolean contains(byte[] hay, byte[] needle) {
        for (int iter = 0; iter + needle.length <= hay.length; iter++) {
            boolean match = true;
            for (int k = 0; k < needle.length && match; k++) {
                match = hay[iter + k] == needle[k];
            }
            if (match) {
                return true;
            }
        }
        return false;
    }

    private static Script script(List<byte[][]> map, int type) {
        byte[] b = get(map, new byte[] {(byte) type});
        return b == null ? null : Script.of(b);
    }

    private static byte[] get(List<byte[][]> map, byte[] key) {
        for (byte[][] mapItem : map) {
            if (java.util.Arrays.equals(mapItem[0], key)) {
                return mapItem[1];
            }
        }
        return null;
    }

    private static void put(List<byte[][]> map, byte[] key, byte[] value) {
        for (int iter = 0; iter < map.size(); iter++) {
            if (java.util.Arrays.equals(map.get(iter)[0], key)) {
                map.set(iter, new byte[][] {key, Wire.copy(value)});
                return;
            }
        }
        map.add(new byte[][] {key, Wire.copy(value)});
    }
}
