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
package com.codename1.crypto.wallet;

import com.codename1.crypto.hd.DerivationPath;
import com.codename1.security.Secp256k1;

/// Everything that touches a wallet's private keys, behind one interface. The keys are
/// addressed by derivation path and never leave the implementation: it answers public keys
/// and signatures. [SeedSigner] derives them from a seed in memory; a product with real money
/// implements this over a hardware device, a vault the user unlocks, or a key service that
/// holds the keys and logs every signature.
///
/// An implementation that cannot do one kind of signature (a cloud KMS with secp256k1 and no
/// Ed25519, say) throws `UnsupportedOperationException` from it.
@com.codename1.impl.SharedWithBackend
public interface Signer {
    /// The fingerprint of the BIP-32 master key, which PSBTs record to say whose keys they want.
    int getMasterFingerprint();

    /// The compressed 33-byte secp256k1 public key at a BIP-32 path.
    byte[] getSecp256k1PublicKey(DerivationPath path);

    /// An ECDSA signature over a 32-byte hash with the secp256k1 key at a path: RFC 6979
    /// deterministic, low-S, with its recovery id.
    Secp256k1.EcdsaSignature signSecp256k1(DerivationPath path, byte[] hash32);

    /// A BIP-340 signature over a 32-byte Taproot sighash with the secp256k1 key at a path,
    /// tweaked by BIP-341 for `merkleRoot` (`null` for a key-path-only output).
    byte[] signTaproot(DerivationPath path, byte[] sighash, byte[] merkleRoot);

    /// The 32-byte Ed25519 public key at a SLIP-10 path; every index is hardened.
    byte[] getEd25519PublicKey(DerivationPath path);

    /// An Ed25519 signature over a message with the SLIP-10 key at a path.
    byte[] signEd25519(DerivationPath path, byte[] message);
}
