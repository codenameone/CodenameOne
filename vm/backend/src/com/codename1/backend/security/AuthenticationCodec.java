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
package com.codename1.backend.security;

import java.util.List;
import java.util.Map;

/// Keeps one kind of [Authentication] in the HTTP session, and makes it again
/// on the next request.
///
/// A session stores plain values -- text, numbers, truth values, lists and
/// maps of them -- because the database session store writes JSON and any
/// server may take the client's next request. By default
/// [HttpSessionSecurityContextRepository] therefore keeps a name and a list of
/// authorities, and a later request sees a
/// [UsernamePasswordAuthenticationToken] whatever signed the user in. A codec
/// is how a way of signing in keeps more: which provider the user came
/// through and what it said of them, which passkey they used.
///
/// Each mechanism that has a kind of its own registers its codec from its own
/// configurer, so the repository names none of them and a server carries only
/// the codecs of what its chains declare. An application with an
/// authentication of its own does the same:
///
/// ```java
/// http.authenticationCodec(new AuthenticationCodec() {
///     public String getKind() {
///         return "badge";
///     }
///
///     public Map<String, Object> encode(Authentication authentication) {
///         if (!(authentication instanceof BadgeAuthentication)) {
///             return null;
///         }
///         Map<String, Object> kept = new HashMap<String, Object>();
///         kept.put("door", ((BadgeAuthentication) authentication).getDoor());
///         return kept;
///     }
///
///     public Authentication decode(String name, List<GrantedAuthority> authorities, Map stored) {
///         Object door = stored.get("door");
///         return door instanceof String
///                 ? new BadgeAuthentication(name, authorities, (String) door) : null;
///     }
/// });
/// ```
///
/// What [#decode] is handed has been through the session store: a number that
/// went in as an `Integer` may come back a `Long`, and nothing in it is to be
/// cast without `instanceof`.
public interface AuthenticationCodec {
    /// The name this kind is stored under: short, and never reused for another.
    String getKind();

    /// What to keep of `authentication` beside its name and authorities, as
    /// plain values; null when it is not of this codec's kind. A map that
    /// holds anything a session cannot store is not kept, and the
    /// authentication is then stored as a name and authorities alone.
    Map<String, Object> encode(Authentication authentication);

    /// The authentication [#encode] kept, for a later request.
    ///
    /// @param name its name, as stored
    /// @param authorities its authorities, as stored
    /// @param stored what [#encode] returned, after the session store
    /// @return the authentication, already authenticated; or null when
    /// `stored` is not something this codec can read, and the user is then the
    /// name and the authorities alone
    Authentication decode(String name, List<GrantedAuthority> authorities, Map stored);
}
