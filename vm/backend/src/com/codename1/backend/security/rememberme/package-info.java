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
/// Remember-me: recognizing a returning user by a cookie, so that closing the
/// browser does not sign them out.
///
/// `http.rememberMe(...)` turns it on for a chain. The cookie holds a series
/// and a token, of which the server keeps only a hash; the token is replaced
/// every time the cookie is used, and a cookie whose series is known but whose
/// token is not is taken as stolen, which signs the user out everywhere. Tokens
/// are kept by a [com.codename1.backend.security.rememberme.PersistentTokenRepository]:
/// in memory, or in the server's database.
///
/// A user recognized this way is authenticated but not *fully*: see
/// [com.codename1.backend.security.RememberMeAuthenticationToken].
package com.codename1.backend.security.rememberme;
