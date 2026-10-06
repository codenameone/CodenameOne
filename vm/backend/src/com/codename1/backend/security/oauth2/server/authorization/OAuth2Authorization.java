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

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/// One grant: a user -- or, for the device grant, nobody yet -- having let one
/// client act with some scopes. The authorization code, the refresh tokens and
/// the device and user codes issued under it are kept beside it by the
/// [OAuth2AuthorizationService], as hashes; removing the grant revokes them
/// all.
public final class OAuth2Authorization {
    /// A grant a user has made.
    public static final String ACTIVE = "active";
    /// A device grant the user has not answered.
    public static final String PENDING = "pending";
    /// A device grant the user refused.
    public static final String DENIED = "denied";

    private final String id;
    private final String registeredClientId;
    private final String principalName;
    private final String authorizationGrantType;
    private final Set<String> scopes;
    private final String status;
    private final Map<String, Object> attributes;
    private final long createdAt;
    private final long expiresAt;

    /// @param attributes what else is remembered of the grant, as text, numbers,
    /// lists and maps
    /// @param createdAt epoch milliseconds
    /// @param expiresAt when nothing issued under the grant can be used any
    /// more, in epoch milliseconds
    public OAuth2Authorization(String id, String registeredClientId, String principalName,
                               String authorizationGrantType, Collection<String> scopes,
                               String status, Map<String, Object> attributes, long createdAt,
                               long expiresAt) {
        if (id == null || registeredClientId == null || authorizationGrantType == null
                || status == null) {
            throw new IllegalArgumentException("An authorization needs an id, a client, a grant "
                    + "type and a status");
        }
        this.id = id;
        this.registeredClientId = registeredClientId;
        this.principalName = principalName == null ? "" : principalName;
        this.authorizationGrantType = authorizationGrantType;
        this.scopes = Collections.unmodifiableSet(scopes == null ? new LinkedHashSet<String>()
                : new LinkedHashSet<String>(scopes));
        this.status = status;
        this.attributes = Collections.unmodifiableMap(attributes == null
                ? new LinkedHashMap<String, Object>()
                : new LinkedHashMap<String, Object>(attributes));
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public String getId() {
        return id;
    }

    /// The [RegisteredClient#getId] of the client the grant was made to.
    public String getRegisteredClientId() {
        return registeredClientId;
    }

    /// Who made the grant; empty for a device grant nobody has answered.
    public String getPrincipalName() {
        return principalName;
    }

    public String getAuthorizationGrantType() {
        return authorizationGrantType;
    }

    public Set<String> getScopes() {
        return scopes;
    }

    /// [#ACTIVE], [#PENDING] or [#DENIED].
    public String getStatus() {
        return status;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    /// One attribute, or null.
    public Object getAttribute(String name) {
        return attributes.get(name);
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getExpiresAt() {
        return expiresAt;
    }

    /// This grant, good until `expiresAt`.
    public OAuth2Authorization withExpiresAt(long expiresAt) {
        return new OAuth2Authorization(id, registeredClientId, principalName,
                authorizationGrantType, scopes, status, attributes, createdAt, expiresAt);
    }
}
