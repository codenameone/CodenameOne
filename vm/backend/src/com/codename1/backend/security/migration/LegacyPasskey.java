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
package com.codename1.backend.security.migration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/// A passkey as another relying party stored it.
///
/// What a WebAuthn library keeps is the same everywhere, under different column
/// names: the credential id, the COSE public key the authenticator sent at
/// registration, its signature counter, and the user handle -- the
/// `user.id` the old server registered, which the authenticator returns with
/// every assertion and which therefore cannot change.
///
/// A passkey moves only to a server of the same relying party id (the domain).
/// One registered for `old.example.com` does not answer a challenge for
/// `new.example.com`; that is WebAuthn, not this migration.
public final class LegacyPasskey {
    private final String username;
    private final byte[] userHandle;
    private final byte[] credentialId;
    private final byte[] coseKey;
    private long signatureCount;
    private List<String> transports = Collections.emptyList();
    private String label;
    private boolean backupEligible;
    private boolean backupState;
    private boolean uvInitialized;

    /// @param username the local user it belongs to
    /// @param userHandle the `user.id` it was registered with
    /// @param credentialId its credential id
    /// @param coseKey its public key, CBOR-encoded as the authenticator sent it
    public LegacyPasskey(String username, byte[] userHandle, byte[] credentialId, byte[] coseKey) {
        this.username = username;
        this.userHandle = userHandle == null ? null : userHandle.clone();
        this.credentialId = credentialId == null ? null : credentialId.clone();
        this.coseKey = coseKey == null ? null : coseKey.clone();
    }

    public LegacyPasskey signatureCount(long count) {
        this.signatureCount = count;
        return this;
    }

    public LegacyPasskey transports(List<String> transports) {
        this.transports = transports == null ? Collections.<String>emptyList()
                : new ArrayList<String>(transports);
        return this;
    }

    public LegacyPasskey label(String label) {
        this.label = label;
        return this;
    }

    /// The backup flags the old server recorded (synced passkeys are eligible).
    public LegacyPasskey backup(boolean eligible, boolean state) {
        this.backupEligible = eligible;
        this.backupState = state;
        return this;
    }

    public LegacyPasskey uvInitialized(boolean uvInitialized) {
        this.uvInitialized = uvInitialized;
        return this;
    }

    String getUsername() {
        return username;
    }

    byte[] getUserHandle() {
        return userHandle;
    }

    byte[] getCredentialId() {
        return credentialId;
    }

    byte[] getCoseKey() {
        return coseKey;
    }

    long getSignatureCount() {
        return signatureCount;
    }

    List<String> getTransports() {
        return transports;
    }

    String getLabel() {
        return label;
    }

    boolean isBackupEligible() {
        return backupEligible;
    }

    boolean isBackupState() {
        return backupState;
    }

    boolean isUvInitialized() {
        return uvInitialized;
    }
}
