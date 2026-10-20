/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.webrtc;

import java.util.ArrayList;
import java.util.List;

/// How a [PeerConnection] looks for a path to the other side.
public final class PeerConnectionConfig {
    private final List<IceServer> servers = new ArrayList<IceServer>();
    private boolean relayOnly;

    /// Adds a STUN or TURN server.
    public PeerConnectionConfig addIceServer(IceServer server) {
        if (server == null) {
            throw new IllegalArgumentException("null ICE server");
        }
        servers.add(server);
        return this;
    }

    /// Uses only paths through a TURN relay. The other side then never learns
    /// this device's own addresses, at the cost of every call going through
    /// the relay. Off by default.
    public PeerConnectionConfig setRelayOnly(boolean relayOnly) {
        this.relayOnly = relayOnly;
        return this;
    }

    public boolean isRelayOnly() {
        return relayOnly;
    }

    public List<IceServer> getIceServers() {
        return new ArrayList<IceServer>(servers);
    }

    /// The servers in the form [com.codename1.webrtc.spi.WebRtcBridge] takes.
    String wire() {
        StringBuilder out = new StringBuilder();
        for (int iter = 0; iter < servers.size(); iter++) {
            IceServer each = servers.get(iter);
            if (iter > 0) {
                out.append('\n');
            }
            String[] urls = each.getUrls();
            for (int u = 0; u < urls.length; u++) {
                if (u > 0) {
                    out.append(',');
                }
                out.append(clean(urls[u]));
            }
            out.append('\t').append(clean(each.getUsername()));
            out.append('\t').append(clean(each.getCredential()));
        }
        return out.toString();
    }

    // A tab, comma or line break inside a value would split it in the wire
    // form. None is legal in a URL, and a TURN credential is base64 or hex.
    private static String clean(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder out = new StringBuilder(value.length());
        for (int iter = 0; iter < value.length(); iter++) {
            char c = value.charAt(iter);
            if (c != '\t' && c != '\n' && c != '\r') {
                out.append(c);
            }
        }
        return out.toString();
    }
}
