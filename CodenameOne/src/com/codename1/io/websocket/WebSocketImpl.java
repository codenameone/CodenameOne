/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * version 2 for more details.
 */
package com.codename1.io.websocket;

/// Per-platform WebSocket implementation backing the public [WebSocket]
/// class. Each port supplies a concrete subclass through
/// [com.codename1.impl.CodenameOneImplementation#createWebSocketImpl];
/// application code never instantiates this directly.
///
/// The contract mirrors a small subset of [WebSocket]'s public surface
/// expressed against the lifecycle of one connection. The implementation
/// is responsible for:
///
/// - Establishing and tearing down the underlying transport (URLSession
///   on iOS, OkHttp on Android, `window.WebSocket` on JavaScript,
///   `java.net.http.WebSocket` on JavaSE).
/// - Dispatching inbound events back to the owning [WebSocket] via its
///   protected callbacks. Implementations should hop to the EDT before
///   calling those callbacks so application code never has to.
///
/// Method names match the legacy `cn1-websockets` cn1lib so existing
/// platform implementations can be ported with minimal churn.
public abstract class WebSocketImpl {

    /// Sets the URL for the connection. Called before [#connect()].
    public abstract void setUrl(String url);

    /// Sends a UTF-8 text frame. Only called when [#getReadyState()] returns
    /// `1` ([WebSocketState#OPEN]).
    public abstract void sendString(String message);

    /// Sends a binary frame. Same gating as [#sendString(String)].
    public abstract void sendBytes(byte[] message);

    /// Initiates the closing handshake. Subsequent reads continue to fire
    /// until the peer confirms; the impl is expected to dispatch
    /// `onClose` via the owning [WebSocket] when the handshake completes.
    public abstract void close();

    /// Initiates the opening handshake. Returns immediately; the impl
    /// must dispatch `onOpen` (or `onError`) asynchronously through the
    /// owning [WebSocket].
    public abstract void connect();

    /// Returns the current state as a small int that maps to
    /// [WebSocketState]:
    /// `0` = `CONNECTING`, `1` = `OPEN`, `2` = `CLOSING`, anything else =
    /// `CLOSED`.
    public abstract int getReadyState();
}
