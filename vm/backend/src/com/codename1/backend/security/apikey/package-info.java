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
/// API keys: long-lived secrets a program presents instead of signing in.
///
/// A key is generated once and shown once. What is stored is its SHA-256, so a
/// copy of the table is not a set of working keys, and a request is
/// authenticated by hashing what it presents and looking that up.
///
/// ```java
/// GeneratedApiKey made = new ApiKeyGenerator().generate("ci-bot", "deploy", "read");
/// repository.save(made.getApiKey());          // the hash, the owner, the scopes
/// show(made.getPlaintext());                  // cn1_Zm9v...; never available again
/// ```
///
/// Turned on with `http.apiKey(...)`; see
/// [com.codename1.backend.security.ApiKeyConfigurer].
package com.codename1.backend.security.apikey;
