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
package com.codename1.backend.security.oauth2.core;

/// One check a token is put to after its signature has verified: is it still
/// in date, is it from the issuer this server trusts, is it meant for this
/// server.
///
/// ```java
/// OAuth2TokenValidator<Jwt> tenant = jwt -> "acme".equals(jwt.getClaimAsString("tenant"))
///         ? OAuth2TokenValidatorResult.success()
///         : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token",
///                 "The token is for another tenant", null));
/// ```
public interface OAuth2TokenValidator<T> {
    /// Whether `token` passes, and why not.
    OAuth2TokenValidatorResult validate(T token);
}
