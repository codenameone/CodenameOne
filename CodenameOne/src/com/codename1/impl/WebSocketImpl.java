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
