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

import com.codename1.backend.Crypto;
import com.codename1.backend.Transactions;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/// A server's mail support: its transport, its queue and its defaults.
///
/// Application code injects a [MailSender], which sends through the one its
/// server has. This class is what [Mailers#open] answers, for code that sets
/// one up by hand -- a command line tool, a check.
public final class Mailer {
    /// How many mails a server with no transport remembers.
    private static final int KEPT = 1000;
    private static final String HEX = "0123456789abcdef";

    private final MailTransport transport;
    private final MailQueue queue;
    private final Address defaultFrom;
    private final String domain;
    private final boolean capture;
    private final boolean warn;
    private final AtomicBoolean warned = new AtomicBoolean();
    private final List<Mail> sent = new ArrayList<Mail>();

    Mailer(MailTransport transport, MailQueue queue, Address defaultFrom, String domain,
            boolean warnWhenCaptured) {
        this.transport = transport;
        this.queue = queue;
        this.defaultFrom = defaultFrom;
        this.domain = domain;
        this.capture = transport instanceof MemoryTransport;
        this.warn = warnWhenCaptured;
    }

    /// The transport mails leave through.
    public MailTransport getTransport() {
        return transport;
    }

    private static String hex(byte[] data) {
        StringBuilder sb = new StringBuilder(data.length * 2);
        for (byte b : data) {
            sb.append(HEX.charAt(b >> 4 & 15)).append(HEX.charAt(b & 15));
        }
        return sb.toString();
    }

    private static boolean isHostName(String host) {
        for (int i = 0; i < host.length(); i++) {
            char c = host.charAt(i);
            if (!(c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9'
                    || c == '.' || c == '-')) {
                return false;
            }
        }
        return host.length() > 0;
    }

    private byte[] encode(Mail resolved) throws IOException {
        String id = hex(Crypto.randomBytes(16));
        String host = domain != null ? domain : resolved.from.domain();
        if (!isHostName(host)) {
            // An address may have a domain outside ASCII; a Message-ID may
            // not, and is written a byte a character.
            host = "mail.invalid";
        }
        return MimeWriter.write(resolved, id + "@" + host, "=_cn1_" + id.substring(0, 24),
                System.currentTimeMillis());
    }

    /// Takes `mail` for delivery and returns without waiting for it.
    ///
    /// Inside a database transaction the mail is sent only if the transaction
    /// commits. A failure to deliver is retried and then recorded, never
    /// thrown here; what this throws is a mail that cannot be sent at all --
    /// no sender, a template that does not exist.
    public void send(Mail mail) throws IOException {
        final Mail resolved = mail.resolved(defaultFrom);
        byte[] mime = encode(resolved);
        if (capture) {
            if (Transactions.isActive()) {
                Transactions.registerSynchronization(new Transactions.Synchronization() {
                    @Override
                    public void afterCommit() {
                        keep(resolved);
                    }
                });
            } else {
                keep(resolved);
            }
            return;
        }
        List<String> to = resolved.recipients();
        queue.submit(resolved.from.spec, to.toArray(new String[to.size()]), mime);
    }

    /// Takes several mails; they leave over one connection where the
    /// transport has connections.
    public void send(Mail... mails) throws IOException {
        for (Mail m : mails) {
            send(m);
        }
    }

    /// Sends `mail` on the calling thread and returns when the server has
    /// taken it, or throws why it did not. No queue, no retry and no regard
    /// for a transaction: for a caller that has to know, and has the time to
    /// wait.
    public void sendNow(Mail mail) throws IOException {
        Mail resolved = mail.resolved(defaultFrom);
        byte[] mime = encode(resolved);
        if (capture) {
            keep(resolved);
            return;
        }
        List<String> to = resolved.recipients();
        try {
            MailMetrics.transmit(transport, resolved.from.spec,
                    to.toArray(new String[to.size()]), mime);
        } catch (MailException err) {
            MailMetrics.failed(err.isPermanent());
            throw err;
        } catch (IOException err) {
            MailMetrics.failed(false);
            throw new MailException("The mail could not be sent: " + err.getMessage(), err);
        }
        MailMetrics.sent();
    }

    private void keep(Mail resolved) {
        if (warn && warned.compareAndSet(false, true)) {
            System.err.println("cn1: cn1.mail.url is not set, so mail is kept in memory and "
                    + "not sent; set it to smtp://..., ses://... or log:");
        }
        MailMetrics.sent();
        synchronized (sent) {
            if (sent.size() >= KEPT) {
                sent.remove(0);
            }
            sent.add(resolved);
        }
    }

    /// The mails sent through the `memory:` transport, oldest first: what a
    /// test asserts on. Each has its template rendered and its sender filled
    /// in. Empty for any other transport.
    public List<Mail> sent() {
        synchronized (sent) {
            return new ArrayList<Mail>(sent);
        }
    }

    /// Forgets what [#sent] answers.
    public void clearSent() {
        synchronized (sent) {
            sent.clear();
        }
    }

    /// Mails accepted and neither delivered nor given up on yet.
    public int pending() {
        return queue.depth();
    }

    /// Whether the transport could reach its server the last time it tried.
    public boolean isHealthy() {
        return transport.isHealthy();
    }

    /// The name of the transport mail leaves through, such as `smtp`.
    public String transportName() {
        return transport.getName();
    }

    /// Stops sending. Mails that are due are handed over first, for up to
    /// five seconds; with a data source the rest are sent by the next start.
    public void close() {
        queue.close(5000);
        transport.close();
    }
}
