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
package com.codename1.nearby.transport;

import com.codename1.impl.nearby.NearbyRequests;
import com.codename1.nearby.spi.NearbyBridge;

/// An incoming request from another device that wants to connect, delivered
/// to [TransportListener#connectionRequested].
///
/// Named `IncomingConnection` rather than the obvious `ConnectionRequest`
/// because `com.codename1.io.ConnectionRequest` is one of the most widely used
/// classes in the framework, and an app doing both networking and nearby
/// transport -- which is most of them -- would have had to qualify one of the
/// two at every mention.
///
/// Answer it with [#accept()] or [#reject()]. A request that is never
/// answered times out on the far side, so answer every one -- and answer it
/// promptly, because both platforms hold radio resources open meanwhile.
///
/// #### Show the token
///
/// [#getAuthenticationToken()] is a short string both devices compute from
/// the connection, and it is the only defense against a device in the middle
/// pretending to be the one the user meant. Showing it on both screens and
/// asking "do these match?" is what makes the pairing trustworthy; skipping
/// that step is a choice to trust whoever answered first.
public final class IncomingConnection {

    private final Endpoint endpoint;
    private final String authenticationToken;
    private boolean answered;

    /// Ports construct these.
    ///
    /// @hidden not part of the public API.
    ///
    /// #### Parameters
    ///
    /// - `endpoint`: who is asking
    /// - `authenticationToken`: the short comparison string, never null
    public IncomingConnection(Endpoint endpoint, String authenticationToken) {
        this.endpoint = endpoint;
        this.authenticationToken =
                authenticationToken == null ? "" : authenticationToken;
    }

    /// Who is asking.
    public Endpoint getEndpoint() {
        return endpoint;
    }

    /// The short string both devices derive from this connection's key
    /// exchange. Identical on both sides when nothing is in the middle.
    ///
    /// Never null, and **empty on iOS**: MultipeerConnectivity offers no
    /// material to bind a token to, and inventing one from the service name
    /// and display names would produce matching digits at both ends of a
    /// relay. Treat empty as "this platform cannot answer the question".
    public String getAuthenticationToken() {
        return authenticationToken;
    }

    /// Whether [#accept()] or [#reject()] has already been called.
    public boolean isAnswered() {
        return answered;
    }

    /// Accepts the connection. The result arrives as
    /// [TransportListener#connected] or
    /// [TransportListener#connectionFailed], because the far side has to
    /// accept too. Calling this twice, or after [#reject()], does nothing.
    public void accept() {
        if (answered) {
            return;
        }
        answered = true;
        NearbyBridge b = NearbyRequests.bridge();
        if (b != null) {
            b.acceptConnection(NearbyRequests.nextId(), endpoint.getId());
        }
    }

    /// Rejects the connection. Calling this twice, or after [#accept()],
    /// does nothing.
    public void reject() {
        if (answered) {
            return;
        }
        answered = true;
        NearbyBridge b = NearbyRequests.bridge();
        if (b != null) {
            b.rejectConnection(endpoint.getId());
        }
    }

    @Override
    public String toString() {
        return "IncomingConnection[" + endpoint + ", token="
                + authenticationToken + "]";
    }
}
