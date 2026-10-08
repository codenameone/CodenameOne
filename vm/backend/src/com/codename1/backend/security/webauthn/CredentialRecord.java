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
package com.codename1.backend.security.webauthn;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/// One registered passkey: what the server keeps of a credential, as the
/// WebAuthn specification's credential record lists it.
///
/// Nothing in it is secret. The public key verifies signatures and makes
/// none, so a copy of this table signs nobody in.
public final class CredentialRecord {
    private final byte[] credentialId;
    private final byte[] userEntityUserId;
    private final long algorithm;
    private final byte[] publicKey;
    private final long signatureCount;
    private final boolean uvInitialized;
    private final boolean backupEligible;
    private final boolean backupState;
    private final List<String> transports;
    private final String label;
    private final long created;
    private final long lastUsed;

    private CredentialRecord(Builder b) {
        if (b.credentialId == null || b.credentialId.length == 0 || b.credentialId.length > 1023) {
            throw new IllegalArgumentException("A credential id is 1 to 1023 bytes");
        }
        if (b.userEntityUserId == null || b.userEntityUserId.length == 0) {
            throw new IllegalArgumentException("A credential belongs to a user");
        }
        if (b.publicKey == null || b.publicKey.length == 0) {
            throw new IllegalArgumentException("A credential has a public key");
        }
        this.credentialId = b.credentialId.clone();
        this.userEntityUserId = b.userEntityUserId.clone();
        this.algorithm = b.algorithm;
        this.publicKey = b.publicKey.clone();
        this.signatureCount = b.signatureCount;
        this.uvInitialized = b.uvInitialized;
        this.backupEligible = b.backupEligible;
        this.backupState = b.backupState;
        this.transports = Collections.unmodifiableList(new ArrayList<String>(b.transports));
        this.label = b.label == null ? "" : b.label;
        this.created = b.created;
        this.lastUsed = b.lastUsed;
    }

    public static Builder builder() {
        return new Builder();
    }

    /// A builder that starts as a copy of `record`.
    public static Builder from(CredentialRecord record) {
        Builder b = new Builder();
        b.credentialId = record.credentialId;
        b.userEntityUserId = record.userEntityUserId;
        b.algorithm = record.algorithm;
        b.publicKey = record.publicKey;
        b.signatureCount = record.signatureCount;
        b.uvInitialized = record.uvInitialized;
        b.backupEligible = record.backupEligible;
        b.backupState = record.backupState;
        b.transports = new ArrayList<String>(record.transports);
        b.label = record.label;
        b.created = record.created;
        b.lastUsed = record.lastUsed;
        return b;
    }

    /// The credential's id, which the authenticator chose.
    public byte[] getCredentialId() {
        return credentialId.clone();
    }

    /// The handle of the user the credential belongs to; see
    /// [PublicKeyCredentialUserEntity#getId].
    public byte[] getUserEntityUserId() {
        return userEntityUserId.clone();
    }

    /// The COSE identifier of the key's algorithm; see [CoseKey].
    public long getAlgorithm() {
        return algorithm;
    }

    /// The public key, as a SubjectPublicKeyInfo in DER.
    public byte[] getPublicKey() {
        return publicKey.clone();
    }

    /// The signature counter of the last ceremony. Zero for an authenticator
    /// that keeps none, as passkeys synced between devices do not.
    public long getSignatureCount() {
        return signatureCount;
    }

    /// Whether the authenticator has ever verified the user with this
    /// credential: a PIN, a fingerprint, a face.
    public boolean isUvInitialized() {
        return uvInitialized;
    }

    /// Whether the credential may be backed up -- synced to the user's other
    /// devices. Settled when it is made and never changed.
    public boolean isBackupEligible() {
        return backupEligible;
    }

    /// Whether it was backed up at its last ceremony.
    public boolean isBackupState() {
        return backupState;
    }

    /// How the authenticator said it can be reached: `internal`, `hybrid`,
    /// `usb`, `nfc`, `ble`. Sent back as a hint at sign-in.
    public List<String> getTransports() {
        return transports;
    }

    /// What the user called it; empty when they named it nothing.
    public String getLabel() {
        return label;
    }

    /// When it was registered, in epoch milliseconds.
    public long getCreated() {
        return created;
    }

    /// When it last signed in, in epoch milliseconds; when it was registered,
    /// until it has.
    public long getLastUsed() {
        return lastUsed;
    }

    /// Builds a [CredentialRecord].
    public static final class Builder {
        private byte[] credentialId;
        private byte[] userEntityUserId;
        private long algorithm = CoseKey.ES256;
        private byte[] publicKey;
        private long signatureCount;
        private boolean uvInitialized;
        private boolean backupEligible;
        private boolean backupState;
        private List<String> transports = new ArrayList<String>();
        private String label;
        private long created;
        private long lastUsed;

        Builder() {
        }

        public Builder credentialId(byte[] credentialId) {
            this.credentialId = credentialId;
            return this;
        }

        public Builder userEntityUserId(byte[] userEntityUserId) {
            this.userEntityUserId = userEntityUserId;
            return this;
        }

        public Builder algorithm(long algorithm) {
            this.algorithm = algorithm;
            return this;
        }

        public Builder publicKey(byte[] publicKey) {
            this.publicKey = publicKey;
            return this;
        }

        public Builder signatureCount(long signatureCount) {
            this.signatureCount = signatureCount;
            return this;
        }

        public Builder uvInitialized(boolean uvInitialized) {
            this.uvInitialized = uvInitialized;
            return this;
        }

        public Builder backupEligible(boolean backupEligible) {
            this.backupEligible = backupEligible;
            return this;
        }

        public Builder backupState(boolean backupState) {
            this.backupState = backupState;
            return this;
        }

        public Builder transports(List<String> transports) {
            this.transports = transports == null ? new ArrayList<String>()
                    : new ArrayList<String>(transports);
            return this;
        }

        public Builder label(String label) {
            this.label = label;
            return this;
        }

        public Builder created(long created) {
            this.created = created;
            return this;
        }

        public Builder lastUsed(long lastUsed) {
            this.lastUsed = lastUsed;
            return this;
        }

        public CredentialRecord build() {
            return new CredentialRecord(this);
        }
    }
}
