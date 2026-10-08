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
package com.codename1.backend.security.apikey;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// API keys held in memory: for tests, and for a server whose few keys come
/// from its configuration. They are gone when the process ends, and each
/// instance of a server that runs as several has its own.
public final class InMemoryApiKeyRepository implements ApiKeyRepository {
    private final Map<String, ApiKey> byHash = new LinkedHashMap<String, ApiKey>();

    public InMemoryApiKeyRepository(ApiKey... keys) {
        if (keys != null) {
            for (ApiKey key : keys) {
                save(key);
            }
        }
    }

    /// Adds a key, or replaces the one with the same hash.
    public synchronized void save(ApiKey key) {
        if (key == null) {
            throw new IllegalArgumentException("key cannot be null");
        }
        byHash.put(key.getHash(), key);
    }

    /// Withdraws the key with this id.
    ///
    /// @return whether there was one
    public synchronized boolean revoke(String id) {
        for (Map.Entry<String, ApiKey> entry : byHash.entrySet()) {
            if (entry.getValue().getId().equals(id)) {
                entry.setValue(entry.getValue().revoke());
                return true;
            }
        }
        return false;
    }

    @Override
    public synchronized ApiKey findByHash(String hash) {
        return hash == null ? null : byHash.get(hash);
    }

    /// Every key, in the order they were added.
    public synchronized List<ApiKey> findAll() {
        return new ArrayList<ApiKey>(byHash.values());
    }
}
