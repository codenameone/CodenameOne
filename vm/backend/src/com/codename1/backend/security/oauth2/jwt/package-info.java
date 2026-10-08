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
/// JSON Web Tokens signed with a public key algorithm or a shared secret:
/// [com.codename1.backend.security.oauth2.jwt.JwtDecoder] verifies one and
/// reads its claims, [com.codename1.backend.security.oauth2.jwt.JwtEncoder]
/// makes one.
///
/// ```java
/// JwtDecoder decoder = JwtDecoders.fromIssuerLocation("https://accounts.example.com");
/// Jwt jwt = decoder.decode(token);       // BadJwtException says why not
/// String user = jwt.getSubject();
/// ```
///
/// Which algorithm verifies a token is settled by the key the decoder holds and
/// the algorithms it was told to accept, and the token's own `alg` header has
/// to agree with both. A token that says `none`, or says `HS256` to a decoder
/// that holds an RSA public key, is refused before anything is computed.
///
/// `com.codename1.backend.Jwt` is not part of this: it issues and checks HS256
/// tokens between a server and itself, and stays as it is.
package com.codename1.backend.security.oauth2.jwt;
