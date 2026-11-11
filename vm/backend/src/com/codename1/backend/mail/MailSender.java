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
import java.util.List;

/// Sends mail through the server's transport: what an application injects,
/// as it would Spring's `JavaMailSender`.
///
/// ```java
/// @Service
/// public class Signup {
///     private final MailSender mail;
///     public Signup(MailSender mail) { this.mail = mail; }
///
///     @Transactional
///     public void register(Account account) throws IOException {
///         accounts.save(account);
///         mail.send(Mail.to(account.getEmail())
///                 .subject("Welcome")
///                 .template("mail/welcome", new Model().addAttribute("name", account.getName()))
///                 .build());
///     }
/// }
/// ```
///
/// [#send] returns at once. The mail is encoded on the calling thread and
/// handed to the server's mail thread, which delivers it, tries again when
/// the mail server is unreachable and gives up when it refuses. Inside a
/// database transaction the mail is written to an outbox table in that
/// transaction and sent after the commit, so a rollback sends nothing and a
/// crash between the commit and the send sends it when the server is back.
///
/// Where it goes is `cn1.mail.url`; see [Mailers]. Without one, mail is kept
/// in memory and [#sent] answers it, which is how a test reads what its code
/// sent.
public final class MailSender {
    /// Takes `mail` for delivery and returns without waiting for it.
    public void send(Mail mail) throws IOException {
        Mailers.current().send(mail);
    }

    /// Takes several mails; they leave over one connection where the
    /// transport has connections.
    public void send(Mail... mails) throws IOException {
        Mailers.current().send(mails);
    }

    /// Sends `mail` on the calling thread and returns when the mail server
    /// has taken it, or throws a [MailException] saying why it did not. No
    /// queue, no retry, and no regard for a transaction.
    public void sendNow(Mail mail) throws IOException {
        Mailers.current().sendNow(mail);
    }

    /// The mails sent while the server has no transport, oldest first, each
    /// with its template rendered and its sender filled in.
    public List<Mail> sent() throws IOException {
        return Mailers.current().sent();
    }

    /// Forgets what [#sent] answers; a test calls it before it acts.
    public void clearSent() throws IOException {
        Mailers.current().clearSent();
    }

    /// The sender of the calling thread's server, for code no bean is
    /// injected into.
    public static MailSender current() {
        return new MailSender();
    }
}
