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
/// A second factor at sign-in: one-time codes from an authenticator app, and
/// the recovery codes that stand in for a lost phone.
///
/// `http.mfa(...)` turns it on for a chain. A user who has enrolled -- see
/// [com.codename1.backend.security.mfa.TotpService] -- is not signed in by
/// their password alone: the request stays anonymous, and they are sent to a
/// page that asks for the code.
///
/// The secret an authenticator app shares with the server is kept by a
/// [com.codename1.backend.security.mfa.TotpRepository], in memory or sealed in
/// the server's database; recovery codes are kept as hashes by a
/// [com.codename1.backend.security.mfa.RecoveryCodeRepository].
package com.codename1.backend.security.mfa;
