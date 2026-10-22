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

/// A STUN or TURN server a [PeerConnection] may use to find a path to the
/// other side.
///
/// A STUN server only tells a device its public address, so it needs no
/// credentials. A TURN server relays the media when no direct path exists --
/// behind a carrier NAT or a strict firewall, which is common on mobile
/// networks -- and is normally given short-lived credentials by the app's
/// own server.
public final class IceServer {
    private final String[] urls;
    private final String username;
    private final String credential;

    /// #### Parameters
    ///
    /// - `urls`: one or more `stun:`, `turn:` or `turns:` URLs for the same
    ///   server
    ///
    /// - `username`: the TURN user name, or null for STUN
    ///
    /// - `credential`: the TURN password, or null for STUN
    public IceServer(String[] urls, String username, String credential) {
        if (urls == null || urls.length == 0) {
            throw new IllegalArgumentException("An ICE server needs at least one URL");
        }
        this.urls = new String[urls.length];
        System.arraycopy(urls, 0, this.urls, 0, urls.length);
        this.username = username;
        this.credential = credential;
    }

    /// A server with no credentials, such as a public STUN server.
    public static IceServer stun(String url) {
        return new IceServer(new String[] {url}, null, null);
    }

    public String[] getUrls() {
        String[] out = new String[urls.length];
        System.arraycopy(urls, 0, out, 0, urls.length);
        return out;
    }

    public String getUsername() {
        return username;
    }

    public String getCredential() {
        return credential;
    }
}
