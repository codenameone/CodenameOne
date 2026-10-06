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
/// Passkeys: signing in with a credential an authenticator holds, as the Web
/// Authentication specification defines it.
///
/// `http.webAuthn(...)` turns them on for a chain and serves the two
/// ceremonies -- registering a passkey for a user who is signed in, and
/// signing in with one. Behind those endpoints is
/// [com.codename1.backend.security.webauthn.WebAuthnRelyingPartyOperations],
/// which makes the options a client starts a ceremony from and verifies what
/// the authenticator answered; an application that serves the ceremonies at
/// addresses of its own uses it directly.
///
/// What is kept is a
/// [com.codename1.backend.security.webauthn.CredentialRecord] for each
/// passkey, in a
/// [com.codename1.backend.security.webauthn.UserCredentialRepository], and
/// for each user the handle authenticators know them by, in a
/// [com.codename1.backend.security.webauthn.PublicKeyCredentialUserEntityRepository]:
/// both in memory or in the server's database. Neither holds a secret.
///
/// The options and the answers travel as the specification's JSON forms,
/// byte strings in base64url, which is what a browser's
/// `PublicKeyCredential.parseCreationOptionsFromJSON` and `toJSON()`, and the
/// Codename One client's `com.codename1.io.webauthn.WebAuthnClient`, read and
/// write.
package com.codename1.backend.security.webauthn;
