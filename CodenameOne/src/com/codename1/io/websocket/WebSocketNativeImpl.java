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
 *
 * Originally written by Steve Hannah for the cn1-websockets cn1lib, MIT
 * licensed. Moved into Codename One core with a contributor-grant retained.
 */
package com.codename1.io.websocket;

import com.codename1.system.NativeInterface;

/// Per-platform native interface backing [WebSocket]. Application code does
/// not call this directly -- subclass [WebSocket] instead and the framework
/// wires up the right per-platform implementation through `NativeLookup`.
///
/// Implementations live outside core:
///
/// - iOS: a `URLSessionWebSocketTask` wrapper in `ios/src/main/objectivec/`
/// - Android: an `OkHttp` `WebSocket` client (or the JDK `java.net.http.WebSocket`
///   on API 33+) in `android/src/main/java/`
/// - JavaScript: a thin `window.WebSocket` wrapper in `javascript/src/main/javascript/`
/// - Desktop / simulator: a Tyrus or `java.net.http.WebSocket` adapter
///
/// Each implementation receives a numeric ID from the framework, opens its
/// underlying connection, and dispatches inbound events back to the Java
/// side via the static callbacks on [WebSocket] (`openReceived`,
/// `messageReceived`, `closeReceived`, `errorReceived`).
public interface WebSocketNativeImpl extends NativeInterface {
    /// Sets the URL for the connection. Called before `#connect()`.
    void setUrl(String url);

    /// Assigns a per-instance identifier the framework uses to route inbound
    /// events back to the correct Java-side `WebSocket` instance.
    void setId(int id);

    /// Returns the identifier set via `#setId(int)`.
    int getId();

    /// Sends a binary frame. Called only when `#getReadyState()` is
    /// `WebSocketState#OPEN`.
    void sendBytes(byte[] message);

    /// Sends a UTF-8 text frame. Called only when `#getReadyState()` is
    /// `WebSocketState#OPEN`.
    void sendString(String message);

    /// Initiates the closing handshake. Subsequent reads will continue to
    /// fire until the server confirms the close.
    void close();

    /// Initiates the opening handshake. Returns immediately; the framework
    /// expects an `openReceived` callback (or `errorReceived`) asynchronously.
    void connect();

    /// Returns the current state as a small int:
    /// 0 = `CONNECTING`, 1 = `OPEN`, 2 = `CLOSING`, anything else = `CLOSED`.
    /// The Java-side translates this back to a [WebSocketState].
    int getReadyState();
}
