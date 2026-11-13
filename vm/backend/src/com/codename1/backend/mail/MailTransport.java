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
package com.codename1.backend.mail;

import java.io.IOException;

/// One way of handing an encoded mail to something that will deliver it: an
/// SMTP server, a provider's API.
///
/// A transport is given the envelope and the bytes and nothing else. It does
/// not queue, retry or keep anything; the server's [Mailer] does that around
/// it, the same way for every transport. It is called from the mail thread
/// and from any thread that calls [MailSender#sendNow], so it is safe to call
/// from several at once.
public abstract class MailTransport {
    /// What this transport is: `smtp`, `ses`, `memory`, `log`.
    public abstract String getName();

    /// Hands one mail over, returning when the other side has taken it.
    ///
    /// Throws a [MailException] whose [MailException#isPermanent] says
    /// whether trying again could help; any other IOException is taken to be
    /// a failure of the connection, and is retried.
    ///
    /// - `from`: the address a bounce goes to, without a display name
    /// - `recipients`: the addresses to deliver to, without display names
    /// - `mime`: the whole message, CRLF line endings, not dot-stuffed
    public abstract void send(String from, String[] recipients, byte[] mime) throws IOException;

    /// Whether the transport could reach its server the last time it tried.
    /// One that has not tried yet is healthy.
    public boolean isHealthy() {
        return true;
    }

    /// Closes connections it holds. Never throws.
    public abstract void close();
}
