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

/// A secret key that lives in the `AndroidKeyStore`. Like the platform's, it
/// does not export its bytes: [#getEncoded()] and [#getFormat()] answer null,
/// and only [Cipher] reads the material, through the package-private
/// [#material()].
///
/// It implements `javax.crypto.SecretKey` so the `(SecretKey) ks.getKey(..)`
/// cast application code writes always holds -- see the note on
/// ParparVM's unchecked `CHECKCAST` in the repository's guidance.
final class AndroidKeyStoreSecretKey implements javax.crypto.SecretKey {
    private static final long serialVersionUID = 1L;

    private final String alias;
    private final String algorithm;
    private final int purposes;
    private final String[] blockModes;
    private final String[] paddings;
    private final boolean randomizedEncryptionRequired;
    private final long created;
    private final byte[] material;

    AndroidKeyStoreSecretKey(String alias, String algorithm, int purposes, String[] blockModes,
                             String[] paddings, boolean randomizedEncryptionRequired, long created,
                             byte[] material) {
        this.alias = alias;
        this.algorithm = algorithm;
        this.purposes = purposes;
        this.blockModes = blockModes;
        this.paddings = paddings;
        this.randomizedEncryptionRequired = randomizedEncryptionRequired;
        this.created = created;
        this.material = material;
    }

    @Override
    public String getAlgorithm() {
        return algorithm;
    }

    /// Null, as on Android: the key material never leaves the key store.
    @Override
    public String getFormat() {
        return null;
    }

    /// Null, as on Android: the key material never leaves the key store.
    @Override
    @SuppressWarnings("PMD.ReturnEmptyCollectionRatherThanNull")
    public byte[] getEncoded() {
        return null;
    }

    String alias() {
        return alias;
    }

    int purposes() {
        return purposes;
    }

    boolean randomizedEncryptionRequired() {
        return randomizedEncryptionRequired;
    }

    long created() {
        return created;
    }

    String[] blockModes() {
        return copy(blockModes);
    }

    String[] paddings() {
        return copy(paddings);
    }

    /// True when the key was generated without a block-mode restriction or
    /// with `mode` among its allowed ones.
    boolean allowsBlockMode(String mode) {
        return allows(blockModes, mode);
    }

    boolean allowsPadding(String padding) {
        if (allows(paddings, padding)) {
            return true;
        }
        // Android spells PKCS#7 for AES; the JDK name for the same padding is
        // PKCS5, and Android's own provider accepts both.
        return "PKCS5Padding".equalsIgnoreCase(padding) && allows(paddings, "PKCS7Padding");
    }

    private static boolean allows(String[] set, String value) {
        if (set.length == 0) {
            return true;
        }
        for (int i = 0; i < set.length; i++) {
            if (set[i].equalsIgnoreCase(value)) {
                return true;
            }
        }
        return false;
    }

    byte[] material() {
        byte[] out = new byte[material.length];
        System.arraycopy(material, 0, out, 0, material.length);
        return out;
    }

    private static String[] copy(String[] a) {
        String[] out = new String[a.length];
        System.arraycopy(a, 0, out, 0, a.length);
        return out;
    }
}
