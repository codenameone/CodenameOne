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
/// SAML 2.0 Web Browser SSO for this server as a service provider, shaped like
/// Spring Security's SAML module: [RelyingPartyRegistration] describes an
/// identity provider, [RelyingPartyRegistrations] reads one from metadata or
/// the configuration, and `http.saml2Login(...)` wires the sign-in. The
/// response is judged by [Saml2ResponseValidator]. XML signatures are checked
/// by this package's own strict reader and exclusive canonicalization, which
/// accept only the shapes SAML signing produces. Not supported: encrypted
/// assertions and Single Logout.
package com.codename1.backend.security.saml2;
