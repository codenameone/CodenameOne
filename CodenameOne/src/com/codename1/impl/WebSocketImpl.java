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

package com.codename1.impl;

import com.codename1.io.WebSocketState;

/// Platform contract behind the public `com.codename1.io.WebSocket` facade.
/// Each port that supports WebSocket subclasses this and is returned from
/// `CodenameOneImplementation.createWebSocketImpl(String)`.
///
/// Subclasses call `sink().onConnect()`, `sink().onTextMessage(...)`, etc.
/// from whichever thread the native layer fires the event on. The sink is
/// set exactly once, by the facade, before `connect(int)` is called.
public abstract class WebSocketImpl {
    private final String url;
    private WebSocketEventSink sink;
    private String[] requestedSubprotocols;
    // Written by the port's connect/handshake thread, read by the user's
    // connect handler -- volatile to publish the value across threads.
    @SuppressWarnings("PMD.AvoidUsingVolatile")
    private volatile String selectedSubprotocol;

    protected WebSocketImpl(String url) {
        this.url = url;
    }

    public final String getUrl() {
        return url;
    }

    public final void setEventSink(WebSocketEventSink sink) {
        this.sink = sink;
    }

    protected final WebSocketEventSink sink() {
        return sink;
    }

    /// Sets the subprotocols (RFC 6455 `Sec-WebSocket-Protocol`) the client
    /// offers, in preference order. Called by the public facade before
    /// `connect(int)`; ports include them in the handshake. Null or empty
    /// means "no subprotocol negotiation".
    public final void setRequestedSubprotocols(String[] protocols) {
        this.requestedSubprotocols = protocols;
    }

    /// The subprotocols the client offered, or null when none were set.
    /// Ports read this while building the handshake.
    protected final String[] requestedSubprotocols() {
        return requestedSubprotocols;
    }

    /// Records the subprotocol the server selected. Ports call this once
    /// the handshake completes, before firing `sink().onConnect()`, so the
    /// value is visible to the user's connect handler.
    protected final void setSelectedSubprotocol(String protocol) {
        this.selectedSubprotocol = protocol;
    }

    /// The subprotocol the server selected, or null when none was
    /// negotiated (or the connection has not completed yet).
    public String getSelectedSubprotocol() {
        return selectedSubprotocol;
    }

    /// Initiate the connection. May return immediately and complete
    /// asynchronously; success is signalled via `sink().onConnect()`.
    ///
    /// @param connectTimeoutMs handshake timeout in milliseconds, or 0 for
    /// the platform default.
    public abstract void connect(int connectTimeoutMs);

    /// Close the connection. Idempotent -- calling on an already-closed
    /// connection is a no-op.
    public abstract void close();

    public abstract void sendText(String message);

    public abstract void sendBinary(byte[] message);

    public abstract WebSocketState getReadyState();
}
