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
package com.codename1.backend.security.oauth2.server.authorization;

import java.util.LinkedHashMap;
import java.util.Map;

/// Clients given when the server starts, and kept in this process.
public final class InMemoryRegisteredClientRepository implements RegisteredClientRepository {
    private final Map<String, RegisteredClient> byId = new LinkedHashMap<String, RegisteredClient>();

    public InMemoryRegisteredClientRepository(RegisteredClient... registrations) {
        for (RegisteredClient client : registrations) {
            save(client);
        }
    }

    @Override
    public synchronized void save(RegisteredClient registeredClient) {
        if (registeredClient == null) {
            throw new IllegalArgumentException("registeredClient cannot be null");
        }
        for (RegisteredClient other : byId.values()) {
            if (other.getClientId().equals(registeredClient.getClientId())
                    && !other.getId().equals(registeredClient.getId())) {
                throw new IllegalArgumentException("Registered client must be unique. Found "
                        + "duplicate client identifier: " + registeredClient.getClientId());
            }
        }
        byId.put(registeredClient.getId(), registeredClient);
    }

    @Override
    public synchronized RegisteredClient findById(String id) {
        return byId.get(id);
    }

    @Override
    public synchronized RegisteredClient findByClientId(String clientId) {
        for (RegisteredClient client : byId.values()) {
            if (client.getClientId().equals(clientId)) {
                return client;
            }
        }
        return null;
    }
}
