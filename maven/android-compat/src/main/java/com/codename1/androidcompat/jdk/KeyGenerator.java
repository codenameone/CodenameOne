/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.androidcompat.jdk;

import android.security.keystore.KeyGenParameterSpec;

import java.io.IOException;

/// `javax.crypto.KeyGenerator` for the Codename One runtime: AES and HMAC
/// keys from `com.codename1.security.SecureRandom`.
///
/// With the `AndroidKeyStore` provider the generator must be initialised with
/// a [KeyGenParameterSpec], and [#generateKey()] stores the key under the
/// spec's alias (see [AndroidKeyStoreEntries]) and answers a key whose bytes
/// cannot be exported, as on Android. Without it the key is a plain
/// [SecretKeySpec].
public class KeyGenerator {
    private static final String ANDROID_KEY_STORE = "AndroidKeyStore";

    private final String algorithm;
    private final boolean keyStore;
    private int keySizeBits;
    private KeyGenParameterSpec spec;

    protected KeyGenerator(String algorithm, boolean keyStore) {
        this.algorithm = algorithm;
        this.keyStore = keyStore;
        this.keySizeBits = defaultSize(algorithm);
    }

    private static String canonical(String algorithm) {
        if (algorithm == null) {
            return null;
        }
        if ("AES".equalsIgnoreCase(algorithm)) {
            return "AES";
        }
        if ("HmacSHA256".equalsIgnoreCase(algorithm)) {
            return "HmacSHA256";
        }
        if ("HmacSHA512".equalsIgnoreCase(algorithm)) {
            return "HmacSHA512";
        }
        return null;
    }

    private static int defaultSize(String algorithm) {
        if ("HmacSHA256".equals(algorithm)) {
            return 256;
        }
        if ("HmacSHA512".equals(algorithm)) {
            return 512;
        }
        return 128;
    }

    public static KeyGenerator getInstance(String algorithm)
            throws java.security.NoSuchAlgorithmException {
        String c = canonical(algorithm);
        if (c == null) {
            throw new java.security.NoSuchAlgorithmException(algorithm + " KeyGenerator not available");
        }
        return new KeyGenerator(c, false);
    }

    public static KeyGenerator getInstance(String algorithm, String provider)
            throws java.security.NoSuchAlgorithmException, java.security.NoSuchProviderException {
        if (provider == null || provider.length() == 0) {
            throw new IllegalArgumentException("missing provider");
        }
        if (!ANDROID_KEY_STORE.equals(provider)) {
            throw new java.security.NoSuchProviderException("no such provider: " + provider);
        }
        String c = canonical(algorithm);
        if (c == null) {
            throw new java.security.NoSuchAlgorithmException(algorithm + " KeyGenerator not available");
        }
        return new KeyGenerator(c, true);
    }

    public final String getAlgorithm() {
        return algorithm;
    }

    public final void init(java.security.SecureRandom random) {
        // Randomness always comes from com.codename1.security.SecureRandom.
    }

    public final void init(int keysize) {
        init(keysize, null);
    }

    public final void init(int keysize, java.security.SecureRandom random) {
        if ("AES".equals(algorithm) && keysize != 128 && keysize != 192 && keysize != 256) {
            throw new IllegalArgumentException("Unsupported AES key size: " + keysize);
        }
        if (keysize <= 0 || keysize % 8 != 0 || keysize > 512) {
            throw new IllegalArgumentException("Unsupported key size: " + keysize);
        }
        keySizeBits = keysize;
    }

    public final void init(java.security.spec.AlgorithmParameterSpec params)
            throws java.security.InvalidAlgorithmParameterException {
        init(params, null);
    }

    public final void init(java.security.spec.AlgorithmParameterSpec params,
                           java.security.SecureRandom random)
            throws java.security.InvalidAlgorithmParameterException {
        if (!(params instanceof KeyGenParameterSpec)) {
            throw new java.security.InvalidAlgorithmParameterException(
                    "Unsupported params: KeyGenParameterSpec required");
        }
        KeyGenParameterSpec s = (KeyGenParameterSpec) params;
        int size = s.getKeySize() < 0 ? defaultSize(algorithm) : s.getKeySize();
        try {
            init(size, random);
        } catch (IllegalArgumentException e) {
            throw new java.security.InvalidAlgorithmParameterException(e.getMessage());
        }
        if (s.getPurposes() == 0) {
            throw new java.security.InvalidAlgorithmParameterException("No purposes specified");
        }
        spec = s;
    }

    public final javax.crypto.SecretKey generateKey() {
        byte[] material = com.codename1.security.SecureRandom.bytes(keySizeBits / 8);
        if (!keyStore) {
            return new SecretKeySpec(material, algorithm);
        }
        if (spec == null) {
            throw new IllegalStateException("Not initialized");
        }
        AndroidKeyStoreSecretKey k = new AndroidKeyStoreSecretKey(spec.getKeystoreAlias(), algorithm,
                spec.getPurposes(), spec.getBlockModes(), spec.getEncryptionPaddings(),
                spec.isRandomizedEncryptionRequired(), System.currentTimeMillis(), material);
        try {
            AndroidKeyStoreEntries.write(k);
        } catch (IOException e) {
            // Android's provider reports a failed key store write as a
            // ProviderException, an unchecked exception.
            throw new IllegalStateException("Failed to store key " + spec.getKeystoreAlias() + ": " + e.getMessage());
        }
        return k;
    }
}
