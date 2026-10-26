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
import com.codename1.crypto.hd.Mnemonic;
import com.codename1.security.Secp256k1;
import com.codename1.security.vault.Vault;
import com.codename1.util.AsyncResource;

/// A [Signer] whose mnemonic is sealed in a [Vault], so it is at rest only as ciphertext under
/// the user's password, and on a remembered device under the platform key store (which on iOS
/// and Android can require biometrics to release). It signs only while the vault is unlocked:
/// the first signature after an unlock reads the mnemonic and derives the seed, and the seed is
/// wiped as soon as a call finds the vault locked again.
///
/// ```java
/// Vault vault = Vault.named("wallet");
/// vault.unlockWithPassword(password).get();
/// VaultSigner.store(vault, "main", Mnemonic.generate(256), "").get();
/// Wallet wallet = new Wallet(new VaultSigner(vault, "main"));
/// ```
///
/// Client only: the backend keeps keys in a key service, not a user vault. A mnemonic passes
/// through a `String` on its way to the seed derivation, which the garbage collector, not this
/// class, eventually clears.
public final class VaultSigner implements Signer {
    private static final String MNEMONIC = ".mnemonic";
    private static final String PASSPHRASE = ".passphrase";

    private final Vault vault;
    private final String name;
    private SeedSigner seed;

    /// A signer over the mnemonic stored in a vault under a name by [#store].
    public VaultSigner(Vault vault, String name) {
        if (vault == null || name == null) {
            throw new IllegalArgumentException("vault and name are required");
        }
        this.vault = vault;
        this.name = name;
    }

    /// Seals a mnemonic and its BIP-39 passphrase into an unlocked vault under a name.
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: for a mnemonic whose words or checksum are wrong
    public static AsyncResource<Boolean> store(final Vault vault, final String name, String mnemonic,
            String passphrase) {
        if (!Mnemonic.isValid(mnemonic)) {
            throw new IllegalArgumentException("not a valid BIP-39 mnemonic");
        }
        final char[] pass = (passphrase == null ? "" : passphrase).toCharArray();
        final AsyncResource<Boolean> out = new AsyncResource<Boolean>();
        vault.putSecret(name + MNEMONIC, mnemonic.toCharArray()).ready(new com.codename1.util.SuccessCallback<Boolean>() {
            @Override
            public void onSucess(Boolean ok) {
                vault.putSecret(name + PASSPHRASE, pass).onResult(new com.codename1.util.AsyncResult<Boolean>() {
                    @Override
                    public void onReady(Boolean value, Throwable error) {
                        if (error != null) {
                            out.error(error);
                        } else {
                            out.complete(value);
                        }
                    }
                });
            }
        }).except(new com.codename1.util.SuccessCallback<Throwable>() {
            @Override
            public void onSucess(Throwable t) {
                out.error(t);
            }
        });
        return out;
    }

    /// The stored mnemonic, for the user to write down. The caller should clear the array.
    public AsyncResource<char[]> revealMnemonic() {
        return vault.getSecret(name + MNEMONIC);
    }

    /// Wipes the derived seed now; the next signature reads it from the vault again.
    public void forget() {
        if (seed != null) {
            seed.destroy();
            seed = null;
        }
    }

    private SeedSigner seed() {
        if (!vault.isUnlocked()) {
            forget();
            throw new IllegalStateException("the vault is locked");
        }
        if (seed == null) {
            char[] words = vault.getSecret(name + MNEMONIC).get();
            char[] pass = vault.getSecret(name + PASSPHRASE).get();
            try {
                seed = SeedSigner.fromMnemonic(new String(words), new String(pass));
            } finally {
                clear(words);
                clear(pass);
            }
        }
        return seed;
    }

    private static void clear(char[] c) {
        for (int i = 0; i < c.length; i++) {
            c[i] = ' ';
        }
    }

    @Override
    public int getMasterFingerprint() {
        return seed().getMasterFingerprint();
    }

    @Override
    public byte[] getSecp256k1PublicKey(DerivationPath path) {
        return seed().getSecp256k1PublicKey(path);
    }

    @Override
    public Secp256k1.EcdsaSignature signSecp256k1(DerivationPath path, byte[] hash32) {
        return seed().signSecp256k1(path, hash32);
    }

    @Override
    public byte[] signTaproot(DerivationPath path, byte[] sighash, byte[] merkleRoot) {
        return seed().signTaproot(path, sighash, merkleRoot);
    }

    @Override
    public byte[] getEd25519PublicKey(DerivationPath path) {
        return seed().getEd25519PublicKey(path);
    }

    @Override
    public byte[] signEd25519(DerivationPath path, byte[] message) {
        return seed().signEd25519(path, message);
    }
}
