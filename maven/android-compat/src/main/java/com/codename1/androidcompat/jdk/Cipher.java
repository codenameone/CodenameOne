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

import java.io.ByteArrayOutputStream;

/// `javax.crypto.Cipher` for the Codename One runtime: AES in GCM, CBC and
/// ECB over `com.codename1.security.Cipher`.
///
/// The input is buffered and processed in [#doFinal()]; `update` answers an
/// empty array, which the JDK allows. GCM takes only the 128-bit tag the core
/// produces, and a tag or padding that does not verify is reported as the
/// JDK reports it (`AEADBadTagException`, `BadPaddingException`).
public class Cipher {
    public static final int ENCRYPT_MODE = 1;
    public static final int DECRYPT_MODE = 2;
    public static final int WRAP_MODE = 3;
    public static final int UNWRAP_MODE = 4;

    private static final int BLOCK = 16;
    private static final int GCM_TAG_BITS = 128;
    private static final int GCM_IV = 12;

    private final String transformation;
    private final String mode;
    private final String padding;
    private final String coreTransformation;

    private int opmode;
    private byte[] key;
    private byte[] iv;
    private final ByteArrayOutputStream input = new ByteArrayOutputStream();
    private final ByteArrayOutputStream aad = new ByteArrayOutputStream();

    protected Cipher(String transformation, String mode, String padding, String coreTransformation) {
        this.transformation = transformation;
        this.mode = mode;
        this.padding = padding;
        this.coreTransformation = coreTransformation;
    }

    public static final Cipher getInstance(String transformation)
            throws java.security.NoSuchAlgorithmException, javax.crypto.NoSuchPaddingException {
        if (transformation == null || transformation.length() == 0) {
            throw new java.security.NoSuchAlgorithmException("Null or empty transformation");
        }
        String alg = transformation;
        String m = "ECB";
        String p = "PKCS5Padding";
        int s1 = transformation.indexOf('/');
        if (s1 >= 0) {
            int s2 = transformation.indexOf('/', s1 + 1);
            if (s2 < 0 || transformation.indexOf('/', s2 + 1) >= 0) {
                throw new java.security.NoSuchAlgorithmException("Invalid transformation: " + transformation);
            }
            alg = transformation.substring(0, s1).trim();
            m = transformation.substring(s1 + 1, s2).trim();
            p = transformation.substring(s2 + 1).trim();
        }
        if (!"AES".equalsIgnoreCase(alg)) {
            throw new java.security.NoSuchAlgorithmException("Cannot find any provider supporting " + transformation);
        }
        boolean noPad = "NoPadding".equalsIgnoreCase(p);
        boolean pkcs = "PKCS5Padding".equalsIgnoreCase(p) || "PKCS7Padding".equalsIgnoreCase(p);
        String core;
        if ("GCM".equalsIgnoreCase(m)) {
            if (!noPad) {
                throw new javax.crypto.NoSuchPaddingException("GCM takes NoPadding, not " + p);
            }
            m = "GCM";
            core = com.codename1.security.Cipher.AES_GCM;
        } else if ("CBC".equalsIgnoreCase(m)) {
            if (!noPad && !pkcs) {
                throw new javax.crypto.NoSuchPaddingException("Unsupported padding " + p);
            }
            m = "CBC";
            core = noPad ? com.codename1.security.Cipher.AES_CBC : com.codename1.security.Cipher.AES_CBC_PKCS5;
        } else if ("ECB".equalsIgnoreCase(m)) {
            if (!pkcs) {
                throw new javax.crypto.NoSuchPaddingException("Unsupported padding " + p);
            }
            m = "ECB";
            core = com.codename1.security.Cipher.AES_ECB_PKCS5;
        } else {
            throw new java.security.NoSuchAlgorithmException("Cannot find any provider supporting " + transformation);
        }
        return new Cipher(transformation, m, noPad ? "NoPadding" : p, core);
    }

    public static final Cipher getInstance(String transformation, String provider)
            throws java.security.NoSuchAlgorithmException, java.security.NoSuchProviderException,
            javax.crypto.NoSuchPaddingException {
        if (provider == null || provider.length() == 0) {
            throw new IllegalArgumentException("Missing provider");
        }
        return getInstance(transformation);
    }

    public final String getAlgorithm() {
        return transformation;
    }

    public final int getBlockSize() {
        return BLOCK;
    }

    public final int getOutputSize(int inputLen) {
        int total = input.size() + Math.max(inputLen, 0);
        if ("GCM".equals(mode)) {
            return opmode == DECRYPT_MODE ? Math.max(total - GCM_TAG_BITS / 8, 0) : total + GCM_TAG_BITS / 8;
        }
        return opmode == DECRYPT_MODE ? total : (total / BLOCK + 1) * BLOCK;
    }

    public final byte[] getIV() {
        if (iv == null) {
            return null;
        }
        byte[] out = new byte[iv.length];
        System.arraycopy(iv, 0, out, 0, iv.length);
        return out;
    }

    public final void init(int opmode, java.security.Key key) throws java.security.InvalidKeyException {
        try {
            init(opmode, key, (java.security.spec.AlgorithmParameterSpec) null, null);
        } catch (java.security.InvalidAlgorithmParameterException e) {
            // The JDK reports a cipher that needs parameters it was not given
            // as an InvalidKeyException from this overload.
            throw new java.security.InvalidKeyException(e.getMessage());
        }
    }

    public final void init(int opmode, java.security.Key key, java.security.SecureRandom random)
            throws java.security.InvalidKeyException {
        init(opmode, key);
    }

    public final void init(int opmode, java.security.Key key, java.security.spec.AlgorithmParameterSpec params)
            throws java.security.InvalidKeyException, java.security.InvalidAlgorithmParameterException {
        init(opmode, key, params, null);
    }

    public final void init(int opmode, java.security.Key key, java.security.spec.AlgorithmParameterSpec params,
                           java.security.SecureRandom random)
            throws java.security.InvalidKeyException, java.security.InvalidAlgorithmParameterException {
        if (opmode != ENCRYPT_MODE && opmode != DECRYPT_MODE) {
            throw new IllegalArgumentException("Unsupported opmode " + opmode);
        }
        byte[] material = material(opmode, key);
        byte[] newIv = null;
        if (params instanceof javax.crypto.spec.GCMParameterSpec) {
            if (!"GCM".equals(mode)) {
                throw new java.security.InvalidAlgorithmParameterException("GCMParameterSpec for " + mode);
            }
            javax.crypto.spec.GCMParameterSpec g = (javax.crypto.spec.GCMParameterSpec) params;
            if (g.getTLen() != GCM_TAG_BITS) {
                throw new java.security.InvalidAlgorithmParameterException(
                        "Unsupported GCM tag length: " + g.getTLen());
            }
            newIv = g.getIV();
        } else if (params instanceof javax.crypto.spec.IvParameterSpec) {
            if ("ECB".equals(mode)) {
                throw new java.security.InvalidAlgorithmParameterException("ECB takes no IV");
            }
            newIv = ((javax.crypto.spec.IvParameterSpec) params).getIV();
            if ("CBC".equals(mode) && newIv.length != BLOCK) {
                throw new java.security.InvalidAlgorithmParameterException("Wrong IV length: must be 16 bytes long");
            }
        } else if (params != null) {
            throw new java.security.InvalidAlgorithmParameterException("Unsupported parameters");
        }
        if (newIv != null && newIv.length == 0) {
            throw new java.security.InvalidAlgorithmParameterException("Empty IV");
        }
        if (newIv != null && opmode == ENCRYPT_MODE && key instanceof AndroidKeyStoreSecretKey
                && ((AndroidKeyStoreSecretKey) key).randomizedEncryptionRequired()) {
            // Android refuses a caller IV for a key generated with randomized
            // encryption (the default), so code written for it never passes one.
            throw new java.security.InvalidAlgorithmParameterException("Caller-provided IV not permitted");
        }
        if (newIv == null && !"ECB".equals(mode)) {
            if (opmode == DECRYPT_MODE) {
                throw new java.security.InvalidAlgorithmParameterException(
                        "IV required when decrypting. Use IvParameterSpec or AlgorithmParameters to provide it.");
            }
            newIv = com.codename1.security.SecureRandom.bytes("GCM".equals(mode) ? GCM_IV : BLOCK);
        }
        this.opmode = opmode;
        this.key = material;
        this.iv = newIv;
        gcmSpent = false;
        input.reset();
        aad.reset();
    }

    private byte[] material(int opmode, java.security.Key key) throws java.security.InvalidKeyException {
        if (key == null) {
            throw new java.security.InvalidKeyException("No key given");
        }
        if (!"AES".equalsIgnoreCase(key.getAlgorithm())) {
            throw new java.security.InvalidKeyException("Wrong algorithm: AES required");
        }
        byte[] m;
        if (key instanceof AndroidKeyStoreSecretKey) {
            AndroidKeyStoreSecretKey k = (AndroidKeyStoreSecretKey) key;
            int needed = opmode == ENCRYPT_MODE ? 1 : 2;
            if ((k.purposes() & needed) == 0) {
                throw new java.security.InvalidKeyException("Incompatible purpose");
            }
            if (!k.allowsBlockMode(mode)) {
                throw new java.security.InvalidKeyException("Incompatible block mode: " + mode);
            }
            if (!k.allowsPadding(padding)) {
                throw new java.security.InvalidKeyException("Incompatible padding mode: " + padding);
            }
            m = k.material();
        } else {
            m = key.getEncoded();
        }
        if (m == null || (m.length != 16 && m.length != 24 && m.length != 32)) {
            throw new java.security.InvalidKeyException("Invalid AES key length");
        }
        return m;
    }

    private void checkInit() {
        if (key == null) {
            throw new IllegalStateException("Cipher not initialized");
        }
        if (gcmSpent) {
            // A second GCM encryption under the same key and IV leaks the
            // XOR of the plaintexts and lets an attacker forge tags; Android
            // refuses it the same way.
            throw new IllegalStateException("Cipher must be reinitialized after a GCM encryption");
        }
    }

    /// Set by a GCM encryption's doFinal; cleared by init.
    private boolean gcmSpent;

    public final void updateAAD(byte[] src) {
        if (src == null) {
            throw new IllegalArgumentException("src buffer is null");
        }
        updateAAD(src, 0, src.length);
    }

    public final void updateAAD(byte[] src, int offset, int len) {
        checkInit();
        if (!"GCM".equals(mode)) {
            throw new IllegalStateException("AAD is supported only in GCM mode");
        }
        if (input.size() > 0) {
            throw new IllegalStateException("AAD must be supplied before encryption/decryption starts");
        }
        aad.write(src, offset, len);
    }

    public final byte[] update(byte[] in) {
        if (in == null) {
            throw new IllegalArgumentException("Null input buffer");
        }
        return update(in, 0, in.length);
    }

    public final byte[] update(byte[] in, int offset, int len) {
        checkInit();
        if (in == null || offset < 0 || len < 0 || offset + len > in.length) {
            throw new IllegalArgumentException("Bad arguments");
        }
        input.write(in, offset, len);
        return new byte[0];
    }

    public final byte[] doFinal(byte[] in)
            throws javax.crypto.IllegalBlockSizeException, javax.crypto.BadPaddingException {
        if (in == null) {
            throw new IllegalArgumentException("Null input buffer");
        }
        return doFinal(in, 0, in.length);
    }

    public final byte[] doFinal(byte[] in, int offset, int len)
            throws javax.crypto.IllegalBlockSizeException, javax.crypto.BadPaddingException {
        update(in, offset, len);
        return doFinal();
    }

    public final byte[] doFinal()
            throws javax.crypto.IllegalBlockSizeException, javax.crypto.BadPaddingException {
        checkInit();
        byte[] data = input.toByteArray();
        byte[] ad = aad.size() == 0 ? null : aad.toByteArray();
        input.reset();
        aad.reset();
        boolean gcm = "GCM".equals(mode);
        if (!gcm && "NoPadding".equals(padding) && data.length % BLOCK != 0) {
            throw new javax.crypto.IllegalBlockSizeException("Input length not multiple of 16 bytes");
        }
        if (!gcm && opmode == DECRYPT_MODE && data.length % BLOCK != 0) {
            throw new javax.crypto.IllegalBlockSizeException(
                    "Input length must be multiple of 16 when decrypting with padded cipher");
        }
        com.codename1.security.SecretKey k = new com.codename1.security.SecretKey("AES", key);
        if (opmode == ENCRYPT_MODE) {
            gcmSpent = gcm;
            try {
                return com.codename1.security.Cipher.aesEncrypt(coreTransformation, k, iv, ad, data);
            } catch (com.codename1.security.CryptoException e) {
                throw new javax.crypto.IllegalBlockSizeException(e.getMessage());
            }
        }
        if (gcm && data.length < GCM_TAG_BITS / 8) {
            throw new javax.crypto.AEADBadTagException("Input data too short to contain an expected tag length of 16bytes");
        }
        try {
            return com.codename1.security.Cipher.aesDecrypt(coreTransformation, k, iv, ad, data);
        } catch (com.codename1.security.CryptoException e) {
            if (gcm) {
                throw new javax.crypto.AEADBadTagException("Tag mismatch");
            }
            throw new javax.crypto.BadPaddingException("Given final block not properly padded");
        }
    }
}
