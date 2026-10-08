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

import com.codename1.backend.DataSource;
import com.codename1.backend.Json;
import com.codename1.backend.security.Clock;
import com.codename1.backend.security.oauth2.core.AuthorizationGrantType;
import com.codename1.backend.security.oauth2.core.ClientAuthenticationMethod;
import com.codename1.backend.security.oauth2.core.OAuth2Parameters;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// Clients kept in the server's database, in the
/// `cn1_oauth2_registered_client` table of
/// [com.codename1.backend.security.SecuritySchema].
///
/// ```java
/// @Bean
/// RegisteredClientRepository clients(DataSource dataSource) {
///     return new JdbcRegisteredClientRepository(dataSource);
/// }
/// ```
public final class JdbcRegisteredClientRepository implements RegisteredClientRepository {
    private static final String COLUMNS = "id, client_id, client_secret, client_name, "
            + "authentication_methods, grant_types, redirect_uris, scopes, settings";
    private final DataSource dataSource;
    private Clock clock = Clock.SYSTEM;

    public JdbcRegisteredClientRepository(DataSource dataSource) {
        if (dataSource == null) {
            throw new IllegalArgumentException("dataSource cannot be null");
        }
        this.dataSource = dataSource;
    }

    /// The clock a client's creation time is read from; for tests.
    public void setClock(Clock clock) {
        this.clock = clock;
    }

    @Override
    public void save(RegisteredClient client) {
        List<String> methods = new ArrayList<String>();
        for (ClientAuthenticationMethod method : client.getClientAuthenticationMethods()) {
            methods.add(method.getValue());
        }
        List<String> grants = new ArrayList<String>();
        for (AuthorizationGrantType grant : client.getAuthorizationGrantTypes()) {
            grants.add(grant.getValue());
        }
        Map<String, Object> settings = new LinkedHashMap<String, Object>();
        settings.put("requireProofKey",
                Boolean.valueOf(client.getClientSettings().isRequireProofKey()));
        TokenSettings t = client.getTokenSettings();
        settings.put("authorizationCodeTimeToLive", Long.valueOf(t.getAuthorizationCodeTimeToLive()));
        settings.put("accessTokenTimeToLive", Long.valueOf(t.getAccessTokenTimeToLive()));
        settings.put("idTokenTimeToLive", Long.valueOf(t.getIdTokenTimeToLive()));
        settings.put("refreshTokenTimeToLive", Long.valueOf(t.getRefreshTokenTimeToLive()));
        settings.put("deviceCodeTimeToLive", Long.valueOf(t.getDeviceCodeTimeToLive()));
        settings.put("reuseRefreshTokens", Boolean.valueOf(t.isReuseRefreshTokens()));
        if (!client.getResources().isEmpty()) {
            settings.put("resources", new ArrayList<Object>(client.getResources()));
        }
        Object[] values = {client.getClientId(),
            client.getClientSecret() == null ? "" : client.getClientSecret(),
            client.getClientName(), OAuth2Parameters.scopes(methods),
            OAuth2Parameters.scopes(grants), lines(client.getRedirectUris()),
            OAuth2Parameters.scopes(client.getScopes()), Json.write(settings)};
        try {
            Object[] update = new Object[values.length + 1];
            System.arraycopy(values, 0, update, 0, values.length);
            update[values.length] = client.getId();
            if (dataSource.execute("UPDATE cn1_oauth2_registered_client SET client_id = ?, "
                    + "client_secret = ?, client_name = ?, authentication_methods = ?, "
                    + "grant_types = ?, redirect_uris = ?, scopes = ?, settings = ? WHERE id = ?",
                    update) == 0 && dataSource.queryOne("SELECT id FROM "
                    + "cn1_oauth2_registered_client WHERE id = ?",
                        new Object[] {client.getId()}) == null) {
                // An update that changes nothing counts no row on MySQL, so the
                // client is asked for before it is taken to be new.
                Object[] insert = new Object[values.length + 2];
                insert[0] = client.getId();
                System.arraycopy(values, 0, insert, 1, values.length);
                insert[values.length + 1] = Long.valueOf(clock.currentTimeMillis());
                dataSource.execute("INSERT INTO cn1_oauth2_registered_client (" + COLUMNS
                        + ", created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", insert);
            }
        } catch (IOException err) {
            throw new IllegalStateException("The client " + client.getClientId()
                    + " could not be stored: " + err.getMessage(), err);
        }
    }

    /// One address a line: an address cannot contain a line break, and may
    /// contain anything else.
    private static String lines(Iterable<String> values) {
        StringBuilder sb = new StringBuilder();
        for (String value : values) {
            sb.append(sb.length() == 0 ? "" : "\n").append(value);
        }
        return sb.toString();
    }

    @Override
    public RegisteredClient findById(String id) {
        return find("id", id);
    }

    @Override
    public RegisteredClient findByClientId(String clientId) {
        return find("client_id", clientId);
    }

    private RegisteredClient find(String column, String value) {
        if (value == null) {
            return null;
        }
        Map row;
        try {
            row = dataSource.queryOne("SELECT " + COLUMNS + " FROM cn1_oauth2_registered_client "
                    + "WHERE " + column + " = ?", new Object[] {value});
        } catch (IOException err) {
            // A store that cannot be asked knows no client: the request is
            // refused as one from an unknown client is.
            System.err.println("cn1: the registered clients could not be read: "
                    + err.getMessage());
            return null;
        }
        if (row == null) {
            return null;
        }
        try {
            return decode(row);
        } catch (IOException malformed) {
            System.err.println("cn1: the registered client settings cannot be read; the client is refused");
            return null;
        } catch (IllegalArgumentException malformed) {
            System.err.println("cn1: the registered client is invalid; the client is refused");
            return null;
        }
    }

    private static RegisteredClient decode(Map row) throws IOException {
        RegisteredClient.Builder b = RegisteredClient.withId(String.valueOf(row.get("id")))
                .clientId(String.valueOf(row.get("client_id")))
                .clientSecret(String.valueOf(row.get("client_secret")))
                .clientName(String.valueOf(row.get("client_name")));
        for (String method : OAuth2Parameters.scopes(String.valueOf(
                row.get("authentication_methods")))) {
            b.clientAuthenticationMethod(new ClientAuthenticationMethod(method));
        }
        for (String grant : OAuth2Parameters.scopes(String.valueOf(row.get("grant_types")))) {
            b.authorizationGrantType(new AuthorizationGrantType(grant));
        }
        String uris = String.valueOf(row.get("redirect_uris"));
        int start = 0;
        while (start < uris.length()) {
            int end = uris.indexOf('\n', start);
            end = end < 0 ? uris.length() : end;
            if (end > start) {
                b.redirectUri(uris.substring(start, end));
            }
            start = end + 1;
        }
        for (String scope : OAuth2Parameters.scopes(String.valueOf(row.get("scopes")))) {
            b.scope(scope);
        }
        Map settings = Json.parseObject(String.valueOf(row.get("settings")));
        b.clientSettings(ClientSettings.builder().requireProofKey(flag(settings, "requireProofKey")).build());
        TokenSettings.Builder t = TokenSettings.builder();
        t.authorizationCodeTimeToLive(seconds(settings, "authorizationCodeTimeToLive", 300));
        t.accessTokenTimeToLive(seconds(settings, "accessTokenTimeToLive", 300));
        t.idTokenTimeToLive(seconds(settings, "idTokenTimeToLive", 1800));
        t.refreshTokenTimeToLive(seconds(settings, "refreshTokenTimeToLive", 3600));
        t.deviceCodeTimeToLive(seconds(settings, "deviceCodeTimeToLive", 300));
        t.reuseRefreshTokens(flag(settings, "reuseRefreshTokens"));
        b.tokenSettings(t.build());
        if (settings.containsKey("resources")) {
            Object resources = settings.get("resources");
            if (!(resources instanceof List)) {
                throw new IllegalArgumentException("resources must be an array");
            }
            for (Object resource : (List) resources) {
                if (!(resource instanceof String)) {
                    throw new IllegalArgumentException("resources must contain strings");
                }
                b.resource((String) resource);
            }
        }
        return b.build();
    }

    private static boolean flag(Map settings, String name) {
        Object value = settings.get(name);
        if (!(value instanceof Boolean)) {
            throw new IllegalArgumentException(name + " must be a boolean");
        }
        return ((Boolean) value).booleanValue();
    }

    private static long seconds(Map settings, String name, long fallback) {
        if (!settings.containsKey(name)) {
            return fallback;
        }
        Object value = settings.get(name);
        if (!(value instanceof Number)) {
            throw new IllegalArgumentException(name + " must be a positive integer");
        }
        Number number = (Number) value;
        long seconds = number.longValue();
        if (seconds <= 0 || number.doubleValue() != (double) seconds) {
            throw new IllegalArgumentException(name + " must be a positive integer");
        }
        return seconds;
    }
}
