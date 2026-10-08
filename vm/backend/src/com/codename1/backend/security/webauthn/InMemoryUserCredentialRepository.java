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

import com.codename1.backend.Base64Url;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// Credentials kept in this process: gone when it stops, and unknown to any
/// other server. For a test or a demonstration; a deployment keeps them in
/// the database with [JdbcUserCredentialRepository].
public final class InMemoryUserCredentialRepository implements UserCredentialRepository {
    private final Map<String, CredentialRecord> records =
            new LinkedHashMap<String, CredentialRecord>();

    @Override
    public synchronized boolean save(CredentialRecord record) {
        String key = Base64Url.encode(record.getCredentialId());
        if (records.containsKey(key)) {
            return false;
        }
        records.put(key, record);
        return true;
    }

    @Override
    public synchronized CredentialRecord findByCredentialId(byte[] credentialId) {
        return credentialId == null ? null : records.get(Base64Url.encode(credentialId));
    }

    @Override
    public synchronized List<CredentialRecord> findByUserId(byte[] userEntityUserId) {
        List<CredentialRecord> out = new ArrayList<CredentialRecord>();
        if (userEntityUserId == null) {
            return out;
        }
        String owner = Base64Url.encode(userEntityUserId);
        for (CredentialRecord record : records.values()) {
            if (owner.equals(Base64Url.encode(record.getUserEntityUserId()))) {
                // Oldest first; those of one moment in the order they came.
                int at = out.size();
                while (at > 0 && out.get(at - 1).getCreated() > record.getCreated()) {
                    at--;
                }
                out.add(at, record);
            }
        }
        return out;
    }

    @Override
    public synchronized boolean advance(byte[] credentialId, long signatureCount,
                                        boolean uvInitialized, boolean backupState,
                                        long lastUsed) {
        String key = Base64Url.encode(credentialId);
        CredentialRecord record = records.get(key);
        if (record == null) {
            return false;
        }
        long stored = record.getSignatureCount();
        if (!(signatureCount > stored || (signatureCount == 0 && stored == 0))) {
            return false;
        }
        records.put(key, CredentialRecord.from(record).signatureCount(signatureCount)
                .uvInitialized(uvInitialized).backupState(backupState).lastUsed(lastUsed)
                .build());
        return true;
    }

    @Override
    public synchronized boolean delete(byte[] credentialId) {
        return credentialId != null && records.remove(Base64Url.encode(credentialId)) != null;
    }
}
