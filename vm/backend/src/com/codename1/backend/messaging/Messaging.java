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
package com.codename1.backend.messaging;

import com.codename1.backend.Config;
import com.codename1.backend.DataSource;
import com.codename1.impl.backend.BackendAccess;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/// Opens the [MessageTransport] a configuration names, and finds the one of
/// the calling thread's server.
///
/// | Setting | Default | |
/// |---|---|---|
/// | `cn1.messaging.url` | see below | `memory:`, `db:`, `redis://...`, `amqp://...`, `kafka://...`, `sqs://...`, `pubsub://...` |
/// | `cn1.messaging.lease` | 60000 | milliseconds before a message a consumer took and never settled is delivered again |
/// | `cn1.messaging.timeout` | 5000 | milliseconds a broker connection or reply may take |
/// | `cn1.messaging.db.poll` | 500 | milliseconds between polls of an empty `db:` queue |
///
/// The retry settings are [RetryPolicy]'s. Without `cn1.messaging.url`,
/// Spring Boot's `spring.rabbitmq.host` (or `.addresses`) selects AMQP and
/// `spring.kafka.bootstrap-servers` Kafka; otherwise a server with a data
/// source queues in its database and one without queues in memory.
///
/// A broker transport is compiled into an application only when its
/// configuration names it: the build registers that one transport's factory
/// with [#register], so the protocols an application does not use add nothing
/// to its size.
public final class Messaging {
    private static final String RESOURCE = "cn1.messaging";
    private static final String DATA_SOURCE = "cn1.messaging.ds";
    private static final Map<String, TransportFactory> FACTORIES =
            new HashMap<String, TransportFactory>();

    private Messaging() {
    }

    /// Opens a broker transport for a URL of its scheme.
    public interface TransportFactory {
        /// Opens the transport `url` names; the caller closes it.
        MessageTransport open(String url, Config config, RetryPolicy retry) throws IOException;
    }

    /// Makes `scheme` (`redis`, `amqp`, `kafka`, ...) open through `factory`.
    /// Called by the generated wiring, never by application code.
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

    /// Gives the calling thread's server's transport its data source: the
    /// `db:` queue and the outbox live there. Called by the generated wiring
    /// before any bean can send.
    public static void bindDataSource(DataSource dataSource) throws IOException {
        DataSourceHolder h = holder();
        synchronized (h) {
            h.dataSource = dataSource;
        }
    }

    /// The data source of the calling thread's server, or null.
    static DataSource dataSource() throws IOException {
        DataSourceHolder h = holder();
        synchronized (h) {
            return h.dataSource;
        }
    }

    /// The transport of the calling thread's server: opened from its
    /// configuration the first time it is asked for, closed when the server
    /// stops.
    public static MessageTransport current() throws IOException {
        final DataSource ds = dataSource();
        return (MessageTransport) BackendAccess.get().serverResource(RESOURCE,
                new BackendAccess.ResourceFactory() {
                    @Override
                    public Object open(Config config) throws IOException {
                        return Messaging.open(config, ds);
                    }
                });
    }

    /// The consumer group a listener declared, or -- when it declared none --
    /// the application's name (`spring.application.name`, then
    /// `cn1.application.name`), or `default`.
    public static String group(Config config, String declared) throws IOException {
        if (declared != null && declared.trim().length() > 0) {
            return declared.trim();
        }
        if (config != null) {
            String name = config.get("spring.application.name");
            if (name == null || name.trim().length() == 0) {
                name = config.get("cn1.application.name");
            }
            if (name != null && name.trim().length() > 0) {
                return name.trim();
            }
        }
        return "default";
    }

    /// The URL `config` names, or null when it names none.
    public static String url(Config config) throws IOException {
        if (config == null) {
            return null;
        }
        String url = config.get("cn1.messaging.url");
        if (url != null && url.trim().length() > 0) {
            return url.trim();
        }
        String addresses = config.get("spring.rabbitmq.addresses");
        if (addresses != null && addresses.trim().length() > 0) {
            String a = addresses.trim();
            return a.indexOf("://") >= 0 ? a : "amqp://" + a;
        }
        String host = config.get("spring.rabbitmq.host");
        if (host != null && host.trim().length() > 0) {
            StringBuilder sb = new StringBuilder("amqp://");
            String user = config.get("spring.rabbitmq.username");
            String password = config.get("spring.rabbitmq.password");
            if (user != null) {
                sb.append(user).append(':').append(password == null ? "" : password).append('@');
            }
            sb.append(host.trim()).append(':').append(config.getInt("spring.rabbitmq.port", 5672));
            String vhost = config.get("spring.rabbitmq.virtual-host");
            if (vhost != null && vhost.length() > 0 && !"/".equals(vhost)) {
                sb.append('/').append(vhost);
            }
            return sb.toString();
        }
        String kafka = config.get("spring.kafka.bootstrap-servers");
        if (kafka != null && kafka.trim().length() > 0) {
            return "kafka://" + kafka.trim();
        }
        return null;
    }

    /// Opens the transport `config` names; the caller closes it.
    ///
    /// - `dataSource`: the server's, or null; where `db:` queues and where an
    ///   outbox is kept
    public static MessageTransport open(Config config, DataSource dataSource) throws IOException {
        String url = url(config);
        RetryPolicy retry = RetryPolicy.of(config);
        if (url == null) {
            url = dataSource != null ? "db:" : "memory:";
        }
        if (url.startsWith("memory:")) {
            return new MemoryTransport(retry);
        }
        if (url.startsWith("db:")) {
            if (dataSource == null) {
                throw new IOException("cn1.messaging.url=db: needs a data source; set "
                        + "cn1.datasource.url or use memory: for development");
            }
            return new DatabaseTransport(dataSource, retry,
                    config == null ? 60000 : config.getInt("cn1.messaging.lease", 60000),
                    config == null ? 500 : config.getInt("cn1.messaging.db.poll", 500));
        }
        int colon = url.indexOf(':');
        String scheme = colon < 0 ? url : url.substring(0, colon);
        TransportFactory f;
        synchronized (FACTORIES) {
            f = FACTORIES.get(scheme);
            if (f == null && scheme.endsWith("s")) {
                // amqps, rediss: the TLS form of the same transport.
                f = FACTORIES.get(scheme.substring(0, scheme.length() - 1));
            }
        }
        if (f == null) {
            throw new IOException("cn1.messaging.url \"" + url + "\" names a transport this "
                    + "application was not built with; set cn1.messaging.url in "
                    + "application.properties so the build includes it");
        }
        MessageTransport t = f.open(url, config, retry); // NOPMD CloseResource - returned, or wrapped and returned
        return dataSource == null ? t : new OutboxTransport(t, dataSource);
    }
}
