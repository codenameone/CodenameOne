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
/// Sending email: a [com.codename1.backend.mail.Mail] is built, a
/// [com.codename1.backend.mail.MailSender] takes it, and the server delivers
/// it in the background over the transport `cn1.mail.url` names.
///
/// Mail sent inside a database transaction leaves only if the transaction
/// commits. A mail the server could not hand over is tried again; one it was
/// refused is recorded and not sent twice. With no transport configured, mail
/// is kept in memory, where a test reads it back.
///
/// Receiving mail, signing and encrypting it, and character sets other than
/// UTF-8 are not part of this package.
package com.codename1.backend.mail;
