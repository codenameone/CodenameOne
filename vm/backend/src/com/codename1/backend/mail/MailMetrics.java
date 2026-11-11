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

import com.codename1.backend.Span;
import com.codename1.backend.Tracing;
import com.codename1.backend.metrics.Counter;
import com.codename1.backend.metrics.Gauge;
import com.codename1.backend.metrics.Metrics;
import com.codename1.impl.backend.MailRuntime;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

/// The mail counters. Registered the first time a mail is settled, so an
/// application that sends none reports none.
final class MailMetrics {
    private static final class Holder {
        static final Counter SENT = Metrics.counter("cn1.mail.sent",
                "Mails a transport accepted", "{mail}");
        static final Counter FAILED = Metrics.counter("cn1.mail.failed",
                "Mails given up on: refused, or out of attempts", "{mail}");
        static final Counter REFUSED = Metrics.counter("cn1.mail.refused",
                "Mails given up on because the server refused them for good", "{mail}");
        static final Counter RETRIED = Metrics.counter("cn1.mail.retried",
                "Attempts that failed and will be made again", "{attempt}");
    }

    private static final AtomicBoolean OUTBOX = new AtomicBoolean();

    private MailMetrics() {
    }

    /// Registers the outbox gauge, once for the process. It reads whichever
    /// mail support is open, so a server restarted in the same process -- a
    /// test -- reports its own.
    static void outbox() {
        if (!OUTBOX.compareAndSet(false, true)) {
            return;
        }
        Metrics.gauge("cn1.mail.outbox.pending",
                "Mails staged and not yet handed to the transport", "{mail}",
                new Gauge.Source() {
                    @Override
                    public double read() {
                        MailRuntime.Probe probe = MailRuntime.probe();
                        return probe == null ? 0 : probe.pending();
                    }
                });
    }

    /// One hand-over to the transport, under a span when a tracer is
    /// installed. The span says which transport and how many recipients, and
    /// never an address: a trace is read by more people than a mailbox.
    static void transmit(MailTransport transport, String from, String[] recipients, byte[] mime)
            throws IOException {
        Span span = Tracing.startSpan("mail send");
        try {
            if (span.isRecording()) {
                span.setAttribute("mail.transport", transport.getName());
                span.setAttribute("mail.recipients", (long) recipients.length);
                span.setAttribute("mail.size", (long) mime.length);
            }
            transport.send(from, recipients, mime);
        } catch (IOException err) {
            span.recordException(err);
            throw err;
        } catch (RuntimeException err) {
            span.recordException(err);
            throw err;
        } finally {
            span.end();
        }
    }

    static void sent() {
        Holder.SENT.increment();
    }

    static void failed(boolean refused) {
        Holder.FAILED.increment();
        if (refused) {
            Holder.REFUSED.increment();
        }
    }

    static void retried() {
        Holder.RETRIED.increment();
    }
}
