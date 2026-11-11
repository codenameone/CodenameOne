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

import com.codename1.backend.Config;
import com.codename1.backend.DataSource;
import com.codename1.backend.messaging.RetryPolicy;
import com.codename1.impl.backend.BackendAccess;
import com.codename1.impl.backend.MailRuntime;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/// Opens the [Mailer] a configuration names, and finds the one of the calling
/// thread's server.
///
/// | Setting | Default | |
/// |---|---|---|
/// | `cn1.mail.url` | `memory:` | `smtp://host:587`, `smtps://host:465`, `ses://region`, `log:`, `memory:` |
/// | `cn1.mail.from` | | the sender of a mail that names none: `Shop <no-reply@example.com>` |
/// | `cn1.mail.domain` | the sender's | the domain a Message-ID is made in |
/// | `cn1.mail.username`, `cn1.mail.password` | | credentials, when the URL carries none |
/// | `cn1.mail.retry.max-attempts` | 6 | attempts before a mail is given up on |
/// | `cn1.mail.retry.backoff` | 30000 | milliseconds before the second attempt |
/// | `cn1.mail.retry.multiplier` | 4 | what each later wait is multiplied by |
/// | `cn1.mail.retry.max-backoff` | 3600000 | the longest wait |
/// | `cn1.mail.outbox.sweep` | 10000 | milliseconds between looks for mails a stopped server left |
///
/// Each transport documents its own settings. Without `cn1.mail.url`, Spring
/// Boot's `spring.mail.host` selects SMTP, with `spring.mail.port`,
/// `spring.mail.username` and `spring.mail.password`.
///
/// A transport is compiled into an application only when its configuration
/// names it: the build registers that one transport's factory with
/// [#register], so SMTP adds nothing to an application that sends through an
/// API, and neither adds anything to one that sends no mail.
public final class Mailers {
    private static final String RESOURCE = "cn1.mail";
    private static final String DATA_SOURCE = "cn1.mail.ds";
    private static final Map<String, TransportFactory> FACTORIES =
            new HashMap<String, TransportFactory>();

    private Mailers() {
    }

    /// Opens a transport for a URL of its scheme.
    public interface TransportFactory {
        /// Opens the transport `url` names; the caller closes it.
        MailTransport open(String url, Config config) throws IOException;
    }

    /// Makes `scheme` (`smtp`, `ses`) open through `factory`. Called by the
    /// generated wiring, never by application code.
    public static void register(String scheme, TransportFactory factory) {
        synchronized (FACTORIES) {
            FACTORIES.put(scheme, factory);
        }
    }

    private static final class DataSourceHolder {
        DataSource dataSource;
    }

    private static DataSourceHolder holder() throws IOException {
        return (DataSourceHolder) BackendAccess.get().serverResource(DATA_SOURCE,
                new BackendAccess.ResourceFactory() {
                    @Override
                    public Object open(Config config) {
                        return new DataSourceHolder();
                    }
                });
    }

    /// Gives the calling thread's server's mail support its data source,
    /// where the outbox is kept. Called by the generated wiring before any
    /// bean can send.
    public static void bindDataSource(DataSource dataSource) throws IOException {
        DataSourceHolder h = holder();
        synchronized (h) {
            h.dataSource = dataSource;
        }
    }

    /// The mail support of the calling thread's server: opened from its
    /// configuration the first time it is asked for, closed when the server
    /// stops.
    public static Mailer current() throws IOException {
        DataSourceHolder h = holder();
        final DataSource ds;
        synchronized (h) {
            ds = h.dataSource;
        }
        Object mailer = BackendAccess.get().serverResource(RESOURCE,
                new BackendAccess.ResourceFactory() {
                    @Override
                    public Object open(Config config) throws IOException {
                        return new Closer(Mailers.open(config, ds));
                    }
                });
        return ((Closer) mailer).mailer;
    }

    /// What a server keeps: it closes its resources through `close()`.
    private static final class Closer implements java.io.Closeable {
        final Mailer mailer;
        private final MailRuntime.Probe probe;

        Closer(Mailer mailer) {
            this.mailer = mailer;
            this.probe = new State(mailer);
            MailRuntime.opened(probe);
            MailMetrics.outbox();
        }

        @Override
        public void close() {
            MailRuntime.closed(probe);
            mailer.close();
        }
    }

    /// What the health report and the outbox gauge read of an open mailer.
    private static final class State implements MailRuntime.Probe {
        private final Mailer mailer;

        State(Mailer mailer) {
            this.mailer = mailer;
        }

        @Override
        public String transport() {
            return mailer.transportName();
        }

        @Override
        public boolean isHealthy() {
            return mailer.isHealthy();
        }

        @Override
        public int pending() {
            return mailer.pending();
        }
    }

    /// Whether `config` names a transport that sends.
    public static boolean isConfigured(Config config) throws IOException {
        String url = url(config);
        return url != null && !url.startsWith("memory:");
    }

    /// The URL `config` names, or null when it names none.
    public static String url(Config config) throws IOException {
        if (config == null) {
            return null;
        }
        String url = config.get("cn1.mail.url");
        if (url != null && url.trim().length() > 0) {
            return url.trim();
        }
        String host = config.get("spring.mail.host");
        if (host != null && host.trim().length() > 0) {
            int port = config.getInt("spring.mail.port", 25);
            return (port == 465 ? "smtps://" : "smtp://") + host.trim() + ":" + port;
        }
        return null;
    }

    /// Opens the mail support `config` names; the caller closes it.
    ///
    /// - `dataSource`: the server's, or null; where the outbox is kept
    public static Mailer open(Config config, DataSource dataSource) throws IOException {
        String url = url(config);
        boolean unset = url == null;
        if (unset) {
            url = "memory:";
        }
        MailTransport transport; // NOPMD CloseResource - owned by the Mailer returned
        if (url.startsWith("memory:")) {
            transport = new MemoryTransport();
        } else if (url.startsWith("log:")) {
            transport = new LogTransport();
        } else {
            int colon = url.indexOf(':');
            String scheme = colon < 0 ? url : url.substring(0, colon);
            TransportFactory f;
            synchronized (FACTORIES) {
                f = FACTORIES.get(scheme);
                if (f == null && scheme.endsWith("s")) {
                    // smtps: the TLS form of the same transport.
                    f = FACTORIES.get(scheme.substring(0, scheme.length() - 1));
                }
            }
            if (f == null) {
                throw new IOException("cn1.mail.url \"" + scheme + ":...\" names a transport "
                        + "this application was not built with; set cn1.mail.url in "
                        + "application.properties so the build includes it");
            }
            transport = f.open(url, config);
        }
        String from = config == null ? null : config.get("cn1.mail.from");
        Address sender = null;
        if (from != null && from.trim().length() > 0) {
            try {
                sender = Address.parse(from);
            } catch (IllegalArgumentException err) {
                transport.close();
                throw new IOException("cn1.mail.from: " + err.getMessage(), err);
            }
        }
        String domain = config == null ? null : config.get("cn1.mail.domain");
        if (domain != null && domain.trim().length() == 0) {
            domain = null;
        }
        RetryPolicy retry = new RetryPolicy(
                config == null ? 6 : config.getInt("cn1.mail.retry.max-attempts", 6),
                config == null ? 30000 : config.getInt("cn1.mail.retry.backoff", 30000),
                config == null ? 4 : config.getInt("cn1.mail.retry.multiplier", 4),
                config == null ? 3600000 : config.getInt("cn1.mail.retry.max-backoff", 3600000));
        // A transport that sends nothing gets no outbox. Every server on a
        // database sweeps the one table, and one with no mail server -- a
        // developer's, a test run -- would take the rows of the others, call
        // them sent and delete them.
        boolean sends = !(transport instanceof MemoryTransport
                || transport instanceof LogTransport);
        MailQueue queue = new MailQueue(transport, sends ? dataSource : null, retry,
                config == null ? 10000 : config.getInt("cn1.mail.outbox.sweep", 10000));
        return new Mailer(transport, queue, sender, domain == null ? null : domain.trim(), unset);
    }
}
