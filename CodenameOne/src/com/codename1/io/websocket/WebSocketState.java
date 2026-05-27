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

/// Connection lifecycle state of a [WebSocket]. Mirrors the WebSocket
/// protocol state machine from RFC 6455.
public enum WebSocketState {
    /// An opening handshake is being performed (RFC 6455 section 4).
    CONNECTING,

    /// The connection is established and usable (the opening handshake
    /// has succeeded).
    OPEN,

    /// A closing handshake is being performed (RFC 6455 section 7).
    CLOSING,

    /// The connection is closed.
    CLOSED
}
