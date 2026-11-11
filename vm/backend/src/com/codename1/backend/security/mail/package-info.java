/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
/// What a server does by mail on a user's behalf: resetting a forgotten
/// password and proving that an address is theirs.
///
/// Both mail a link carrying a token from [OneTimeTokenService]: 256 random
/// bits that work once, for one purpose, until they expire. Only the token's
/// SHA-256 is kept, so a copy of the table opens nothing.
///
/// - [PasswordResetService] mails the link and sets the new password;
///   `http.passwordReset(...)` adds the two pages in front of it.
/// - [EmailVerificationService] mails the link and records the address as
///   proved; `http.emailVerification(...)` adds the address the link opens.
///
/// A code mailed as a second factor at sign-in is
/// [com.codename1.backend.security.mfa.EmailCodeService].
///
/// Everything here sends through [com.codename1.backend.mail.MailSender], so
/// it goes wherever `cn1.mail.url` says, and a test reads what was sent from
/// `MailSender.sent()`.
package com.codename1.backend.security.mail;
