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

import com.codename1.backend.HttpServer;

import java.util.Map;
import java.util.Set;

/// What decides who may register a client of their own at the registration
/// endpoint (RFC 7591), and with what. Registration is off until one is given
/// to `clientRegistration(...)` on the authorization server configurer.
///
/// The server parses and checks the request first, and only a request that
/// passes reaches [#validate]. What every self-registered client is, whatever
/// the policy says:
///
/// - public: `token_endpoint_auth_method` is `none` and it has no secret,
///   since anybody can register one and a secret proves nothing about them;
/// - PKCE (S256) is required, and so is the user's consent on every
///   authorization, so that a client nobody vetted is never granted anything
///   silently;
/// - grant types `authorization_code`, optionally with `refresh_token`, and
///   response type `code`;
/// - one to five redirect addresses, each an `https` address, or an `http`
///   one on `127.0.0.1` or `[::1]` -- any port is then accepted, as RFC 8252
///   asks for an application on the user's computer; no fragment, no user
///   information;
/// - scopes out of [#allowedScopes], all of them when the request names none;
/// - no resource servers and no audience of its own choosing.
///
/// A client that [#validate] returns which breaks one of these is not saved,
/// and the request is answered 500: that is a mistake in the policy, not in
/// the request.
public interface ClientRegistrationPolicy {
    /// The scopes a self-registered client may be given. Required and not
    /// empty: there is no safe default for what a stranger's client may ask a
    /// user for.
    Set<String> allowedScopes();

    /// The validate-and-limit hook: refuse the registration by throwing a
    /// [ClientRegistrationException], or return the client to save -- the one
    /// given, or one built from it with less (fewer redirects, fewer scopes,
    /// shorter token lifetimes). It is also the place to count registrations
    /// per address and refuse past a limit.
    ///
    /// @param request the HTTP request
    /// @param metadata the client metadata the request sent, as parsed JSON
    /// @param client the client the server would save
    /// @return the client to save
    RegisteredClient validate(HttpServer.Request request, Map<String, Object> metadata,
            RegisteredClient client);
}
