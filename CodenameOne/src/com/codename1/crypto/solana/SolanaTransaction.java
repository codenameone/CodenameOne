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
import com.codename1.security.Ed25519;
import java.util.Base64;

/// A Solana transaction: a [SolanaMessage] and one Ed25519 signature per required signer.
/// A slot whose signer has not signed yet holds 64 zero bytes, which is how a transaction is
/// passed between parties that sign it in turn.
@com.codename1.impl.SharedWithBackend
public final class SolanaTransaction {
    private static final int SIGNATURE_LENGTH = 64;

    private final SolanaMessage message;
    private final byte[] messageBytes;
    private final byte[][] signatures;

    /// An unsigned transaction for a message.
    public SolanaTransaction(SolanaMessage message) {
        this(message, message.serialize(), new byte[message.getNumRequiredSignatures()][SIGNATURE_LENGTH]);
    }

    private SolanaTransaction(SolanaMessage message, byte[] messageBytes, byte[][] signatures) {
        this.message = message;
        this.messageBytes = messageBytes;
        this.signatures = signatures;
    }

    /// The message.
    public SolanaMessage getMessage() {
        return message;
    }

    /// Signs with the 32-byte Ed25519 seed of one of the message's signers.
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: if the key is not one of the message's signers
    public void sign(byte[] privateKey) {
        byte[] pub = Ed25519.publicKey(privateKey);
        int slot = slotOf(SolanaAddress.fromBytes(pub));
        signatures[slot] = Ed25519.sign(privateKey, pub, messageBytes);
    }

    /// Places a signature made elsewhere, for instance by a hardware wallet.
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: if the signer is not required or the signature is invalid
    public void addSignature(SolanaAddress signer, byte[] signature) {
        int slot = slotOf(signer);
        if (signature == null || !Ed25519.verify(messageBytes, signature, signer.raw())) {
            throw new IllegalArgumentException("not a valid signature by " + signer);
        }
        signatures[slot] = ShortVec.copy(signature);
    }

    private int slotOf(SolanaAddress signer) {
        for (int i = 0; i < signatures.length; i++) {
            if (message.getAccountKeys().get(i).equals(signer)) {
                return i;
            }
        }
        throw new IllegalArgumentException(signer + " is not a signer of this transaction");
    }

    /// Whether every required signature is present and valid.
    public boolean isFullySigned() {
        for (int i = 0; i < signatures.length; i++) {
            if (!Ed25519.verify(messageBytes, signatures[i], message.getAccountKeys().get(i).raw())) {
                return false;
            }
        }
        return true;
    }

    /// The signature of a signer slot, a copy; all zeros while unsigned.
    public byte[] getSignature(int index) {
        return ShortVec.copy(signatures[index]);
    }

    /// The transaction id: the fee payer's signature in Base58.
    public String getId() {
        return Base58.encode(signatures[0]);
    }

    /// The bytes that are signed.
    public byte[] getMessageBytes() {
        return ShortVec.copy(messageBytes);
    }

    /// The wire form.
    public byte[] serialize() {
        ShortVec w = new ShortVec();
        w.length(signatures.length);
        for (byte[] signature : signatures) {
            w.bytes(signature);
        }
        w.bytes(messageBytes);
        return w.toBytes();
    }

    /// The Base64 wire form, what `sendTransaction` takes with `encoding: base64`.
    public String toBase64() {
        return Base64.getEncoder().encodeToString(serialize());
    }

    /// Reads a transaction.
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: for malformed data, or a signature count that does not
    ///   match the message
    public static SolanaTransaction parse(byte[] data) {
        ShortVec.Reader r = new ShortVec.Reader(data);
        int n = r.length();
        byte[][] sigs = new byte[n][];
        for (int i = 0; i < n; i++) {
            sigs[i] = r.read(SIGNATURE_LENGTH);
        }
        byte[] rest = r.rest();
        SolanaMessage m = SolanaMessage.parse(rest);
        if (m.getNumRequiredSignatures() != n) {
            throw new IllegalArgumentException("signature count does not match the message");
        }
        return new SolanaTransaction(m, rest, sigs);
    }

    /// Reads a Base64 transaction.
    public static SolanaTransaction fromBase64(String base64) {
        return parse(Base64.getDecoder().decode(base64));
    }
}
