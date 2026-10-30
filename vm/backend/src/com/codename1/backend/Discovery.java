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
package com.codename1.backend;

import com.codename1.impl.MdnsEngine;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/// Finds and is found on the local network over multicast DNS (Bonjour,
/// DNS-SD): the same records the client's `BonjourBrowser` and
/// `BonjourPublisher` read and write, so a phone app discovers a device's
/// backend with no address configured, and a device finds a gateway the same
/// way.
///
/// ```java
/// Discovery.Advertisement ad = Discovery.advertise("Hall Thermostat", "_thermo._tcp", 8080,
///         Collections.singletonMap("api", "/v1"));
/// Discovery.browse("_gateway._tcp", new Discovery.Listener() {
///     public void found(Discovery.Service s) { connect(s.getHost(), s.getPort()); }
///     public void lost(Discovery.Service s) { }
/// });
/// ```
///
/// The protocol runs on one daemon thread, and listeners are called on it: a
/// server has no event dispatch thread, so a listener that does real work
/// hands it to a task. The thread exits when nothing is advertised or browsed.
/// A name another host already uses gets a numeric suffix, as Bonjour does;
/// [Advertisement#getName()] says which name was taken. IPv4 only.
public final class Discovery {
    private static final AtomicReference<MdnsEngine> ENGINE = new AtomicReference<MdnsEngine>();

    private Discovery() {
    }

    private static MdnsEngine engine() {
        MdnsEngine e = ENGINE.get();
        if (e == null) {
            ENGINE.compareAndSet(null, new BackendMdns());
            e = ENGINE.get();
        }
        return e;
    }

    /// Advertises a service on `port`. `type` is `_name._tcp` or `_name._udp`;
    /// `txt` may be null.
    public static Advertisement advertise(String name, String type, int port, Map<String, String> txt) {
        if (name == null || name.length() == 0 || port <= 0 || port > 65535) {
            throw new IllegalArgumentException("A service needs a name and a port in 1..65535");
        }
        MdnsEngine e = engine();
        return new Advertisement(e, e.publish(name, type, port, txt));
    }

    /// Browses for a type until [Browse#close()].
    public static Browse browse(String type, final Listener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("listener");
        }
        MdnsEngine e = engine();
        return new Browse(e, e.browse(type, new MdnsEngine.Listener() {
            @Override
            public void resolved(Object browse, String name, String type, String host, int port,
                    Map<String, String> txt) {
                listener.found(new Service(name, type, host, port, txt));
            }

            @Override
            public void lost(Object browse, String name, String type) {
                listener.lost(new Service(name, type, null, 0, null));
            }

            @Override
            public void failed(Object browse, String message) {
                listener.failed(message);
            }
        }));
    }

    /// Receives what a [#browse(String, Listener)] finds.
    public interface Listener {
        /// A service appeared, or changed its address, port or TXT record.
        void found(Service service);

        /// A service said goodbye or its records expired. Only the name and
        /// type are set.
        void lost(Service service);

        /// The socket could not be opened; nothing more will be reported. The
        /// default does nothing.
        default void failed(String message) {
        }
    }

    /// A service seen on the network.
    public static final class Service {
        private final String name;
        private final String type;
        private final String host;
        private final int port;
        private final Map<String, String> txt;

        Service(String name, String type, String host, int port, Map<String, String> txt) {
            this.name = name;
            this.type = type;
            this.host = host;
            this.port = port;
            this.txt = Collections.unmodifiableMap(
                    txt == null ? new HashMap<String, String>() : new HashMap<String, String>(txt));
        }

        /// The instance name, `Hall Thermostat`.
        public String getName() {
            return name;
        }

        /// The type, `_thermo._tcp.`.
        public String getType() {
            return type;
        }

        /// The IPv4 address, null for a lost service.
        public String getHost() {
            return host;
        }

        /// The port, 0 for a lost service.
        public int getPort() {
            return port;
        }

        /// The TXT record's keys and values; never null.
        public Map<String, String> getTxt() {
            return txt;
        }

        @Override
        public String toString() {
            return name + " " + type + " " + host + ":" + port;
        }
    }

    /// A running advertisement.
    public static final class Advertisement implements AutoCloseable {
        private final MdnsEngine engine;
        private final Object handle;

        Advertisement(MdnsEngine engine, Object handle) {
            this.engine = engine;
            this.handle = handle;
        }

        /// The name taken, which has a suffix when another host had the one
        /// asked for.
        public String getName() {
            return engine.publishedName(handle);
        }

        /// Withdraws the service; browsers see it go at once.
        @Override
        public void close() {
            engine.unpublish(handle);
        }
    }

    /// A running browse.
    public static final class Browse implements AutoCloseable {
        private final MdnsEngine engine;
        private final Object handle;

        Browse(MdnsEngine engine, Object handle) {
            this.engine = engine;
            this.handle = handle;
        }

        /// Stops the browse; its listener is not called again.
        @Override
        public void close() {
            engine.stopBrowse(handle);
        }
    }
}
