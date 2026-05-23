/*
 * Copyright (c) 2008, 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Codename One in the LICENSE file that accompanied this code.
 */
package com.codename1.io.bonjour;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/// A Bonjour / mDNS service discovered by `BonjourBrowser` or registered by
/// `BonjourPublisher`. Immutable.
public final class BonjourService {
    private final String name;
    private final String type;
    private final String host;
    private final int port;
    private final Map<String, String> txt;

    public BonjourService(String name, String type, String host, int port,
                          Map<String, String> txt) {
        this.name = name;
        this.type = type;
        this.host = host;
        this.port = port;
        if (txt == null) {
            this.txt = Collections.unmodifiableMap(new HashMap<String, String>());
        } else {
            this.txt = Collections.unmodifiableMap(new HashMap<String, String>(txt));
        }
    }

    /// User-visible service name. May include a numeric suffix added by the
    /// platform to resolve name collisions on the subnet.
    public String getName() {
        return name;
    }

    /// mDNS service type (e.g. `_http._tcp.`).
    public String getType() {
        return type;
    }

    /// Resolved host. Either a dotted-quad IPv4, an IPv6 literal in square
    /// brackets, or a `.local.` hostname. `null` if the service is announced
    /// but the address has not been resolved yet.
    public String getHost() {
        return host;
    }

    /// Service port.
    public int getPort() {
        return port;
    }

    /// TXT-record metadata. Always non-null; may be empty.
    public Map<String, String> getTxt() {
        return txt;
    }

    @Override
    public String toString() {
        return name + " (" + type + ") " + host + ":" + port;
    }
}
