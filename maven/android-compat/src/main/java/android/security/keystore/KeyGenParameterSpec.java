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
package android.security.keystore;

/// Android's `KeyGenParameterSpec`: what `KeyGenerator.init` is told about a
/// key that lives in the `AndroidKeyStore`.
///
/// It implements the JDK's `AlgorithmParameterSpec` by that name, because the
/// application passes it to `javax.crypto.KeyGenerator`; the remap step points
/// both at the runtime's shim. The user-authentication settings are recorded
/// and answered back but never enforced -- see
/// `com.codename1.androidcompat.jdk.KeyStore` for where keys are kept.
public final class KeyGenParameterSpec implements java.security.spec.AlgorithmParameterSpec {
    private final String keystoreAlias;
    private final int purposes;
    private final int keySize;
    private final String[] blockModes;
    private final String[] encryptionPaddings;
    private final String[] digests;
    private final boolean randomizedEncryptionRequired;
    private final boolean userAuthenticationRequired;
    private final int userAuthenticationValidityDurationSeconds;
    private final boolean strongBoxBacked;
    private final boolean invalidatedByBiometricEnrollment;

    KeyGenParameterSpec(Builder b) {
        keystoreAlias = b.keystoreAlias;
        purposes = b.purposes;
        keySize = b.keySize;
        blockModes = copy(b.blockModes);
        encryptionPaddings = copy(b.encryptionPaddings);
        digests = copy(b.digests);
        randomizedEncryptionRequired = b.randomizedEncryptionRequired;
        userAuthenticationRequired = b.userAuthenticationRequired;
        userAuthenticationValidityDurationSeconds = b.userAuthenticationValidityDurationSeconds;
        strongBoxBacked = b.strongBoxBacked;
        invalidatedByBiometricEnrollment = b.invalidatedByBiometricEnrollment;
    }

    static String[] copy(String[] a) {
        if (a == null) {
            return new String[0];
        }
        String[] out = new String[a.length];
        System.arraycopy(a, 0, out, 0, a.length);
        return out;
    }

    public String getKeystoreAlias() {
        return keystoreAlias;
    }

    public int getPurposes() {
        return purposes;
    }

    /// The requested size in bits, or -1 when the spec does not say.
    public int getKeySize() {
        return keySize;
    }

    public String[] getBlockModes() {
        return copy(blockModes);
    }

    public String[] getEncryptionPaddings() {
        return copy(encryptionPaddings);
    }

    public String[] getDigests() {
        return copy(digests);
    }

    public boolean isDigestsSpecified() {
        return digests.length > 0;
    }

    public boolean isRandomizedEncryptionRequired() {
        return randomizedEncryptionRequired;
    }

    public boolean isUserAuthenticationRequired() {
        return userAuthenticationRequired;
    }

    public int getUserAuthenticationValidityDurationSeconds() {
        return userAuthenticationValidityDurationSeconds;
    }

    public boolean isStrongBoxBacked() {
        return strongBoxBacked;
    }

    public boolean isInvalidatedByBiometricEnrollment() {
        return invalidatedByBiometricEnrollment;
    }

    /// Android's `KeyGenParameterSpec.Builder`.
    public static final class Builder {
        private final String keystoreAlias;
        private final int purposes;
        private int keySize = -1;
        private String[] blockModes;
        private String[] encryptionPaddings;
        private String[] digests;
        private boolean randomizedEncryptionRequired = true;
        private boolean userAuthenticationRequired;
        private int userAuthenticationValidityDurationSeconds;
        private boolean strongBoxBacked;
        private boolean invalidatedByBiometricEnrollment = true;

        public Builder(String keystoreAlias, int purposes) {
            if (keystoreAlias == null) {
                throw new NullPointerException("keystoreAlias == null");
            }
            if (keystoreAlias.length() == 0) {
                throw new IllegalArgumentException("keystoreAlias must not be empty");
            }
            this.keystoreAlias = keystoreAlias;
            this.purposes = purposes;
        }

        public Builder setKeySize(int keySize) {
            if (keySize < 0) {
                throw new IllegalArgumentException("keySize < 0");
            }
            this.keySize = keySize;
            return this;
        }

        public Builder setBlockModes(String... blockModes) {
            this.blockModes = copy(blockModes);
            return this;
        }

        public Builder setEncryptionPaddings(String... paddings) {
            this.encryptionPaddings = copy(paddings);
            return this;
        }

        public Builder setDigests(String... digests) {
            this.digests = copy(digests);
            return this;
        }

        public Builder setRandomizedEncryptionRequired(boolean required) {
            this.randomizedEncryptionRequired = required;
            return this;
        }

        public Builder setUserAuthenticationRequired(boolean required) {
            this.userAuthenticationRequired = required;
            return this;
        }

        public Builder setUserAuthenticationValidityDurationSeconds(int seconds) {
            this.userAuthenticationValidityDurationSeconds = seconds;
            return this;
        }

        public Builder setUserAuthenticationParameters(int timeoutSeconds, int type) {
            this.userAuthenticationValidityDurationSeconds = timeoutSeconds;
            return this;
        }

        public Builder setIsStrongBoxBacked(boolean strongBox) {
            this.strongBoxBacked = strongBox;
            return this;
        }

        public Builder setInvalidatedByBiometricEnrollment(boolean invalidate) {
            this.invalidatedByBiometricEnrollment = invalidate;
            return this;
        }

        public Builder setUnlockedDeviceRequired(boolean required) {
            return this;
        }

        public KeyGenParameterSpec build() {
            return new KeyGenParameterSpec(this);
        }
    }
}
