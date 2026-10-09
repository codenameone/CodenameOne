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

import java.util.Map;
import java.util.Set;

/// What an authorization server says of a user: the claims of the `/userinfo`
/// answer and of an ID token, beyond `sub`.
///
/// ```java
/// http.authorizationServer(as -> as.userInfoMapper((username, scopes) -> {
///     Account account = accounts.byName(username);
///     Map<String, Object> claims = new LinkedHashMap<>();
///     if (scopes.contains("email")) {
///         claims.put("email", account.getEmail());
///         claims.put("email_verified", account.isEmailConfirmed());
///     }
///     if (scopes.contains("profile")) {
///         claims.put("name", account.getDisplayName());
///     }
///     return claims;
/// }));
/// ```
///
/// Without one, `profile` yields `preferred_username`, the user's name here,
/// and `email` yields nothing: this server does not know that a user name is
/// an address, let alone a verified one.
public interface OidcUserInfoMapper {
    /// The claims of `username` that `scopes` entitle a client to. `sub` is
    /// added by the server and cannot be replaced.
    Map<String, Object> getClaims(String username, Set<String> scopes);
}
