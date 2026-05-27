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

import com.codename1.io.Util;
import com.codename1.ui.Display;

import java.io.IOException;

/// Client-side WebSocket connection following RFC 6455. Subclass and
/// override the four `onXxx` callbacks, then call `#connect()` to open
/// the handshake.
///
/// ```java
/// WebSocket socket = new WebSocket("wss://example.com/socket") {
///     @Override protected void onOpen()                           { send("hello"); }
///     @Override protected void onMessage(String message)          { Log.p("recv: " + message); }
///     @Override protected void onMessage(byte[] message)          { Log.p("recv " + message.length + " bytes"); }
///     @Override protected void onClose(int statusCode, String reason) { Log.p("closed: " + reason); }
///     @Override protected void onError(Exception ex)              { Log.e(ex); }
/// };
/// socket.connect();
/// ```
///
/// All five `onXxx` callbacks fire on the Codename One EDT, so they may
/// touch UI directly. `send(...)` may be called from any thread.
///
/// Check `#isSupported()` before constructing -- the underlying
/// transport is provided by the per-platform port through
/// [com.codename1.impl.CodenameOneImplementation#createWebSocketImpl].
/// Platforms that don't ship a WebSocket implementation return `false`
/// from `isSupported()` and `new WebSocket(...)` produces a non-functional
/// instance.
public abstract class WebSocket {
    private final WebSocketImpl impl;
    private final String url;
    private Thread socketThread;
    private boolean connecting;

    /// Failure surfaced by the underlying transport. Carries the
    /// platform-specific error code so callers can branch on
    /// "connection refused" vs "TLS error" vs "protocol violation"
    /// when the platform exposes that detail.
    public static class WebSocketException extends IOException {
        private final int code;

        public WebSocketException(String message, int code) {
            super(message);
            this.code = code;
        }

        public int getCode() {
            return code;
        }
    }

    /// Returns `true` when the current platform ships a WebSocket
    /// implementation. Always check before constructing -- on platforms
    /// without one, the constructor returns a non-functional instance.
    public static boolean isSupported() {
        try {
            WebSocketImpl probe = Util.createWebSocketImpl(null);
            return probe != null;
        } catch (Throwable t) {
            return false;
        }
    }

    public WebSocket(String url) {
        this.url = url;
        this.impl = Util.createWebSocketImpl(this);
    }

    /// Called on the EDT when the opening handshake has succeeded.
    protected abstract void onOpen();

    /// Called on the EDT when the connection has closed (either side).
    /// `statusCode` is the WebSocket close code (RFC 6455 section 7.4).
    protected abstract void onClose(int statusCode, String reason);

    /// Called on the EDT when a text frame arrives.
    protected abstract void onMessage(String message);

    /// Called on the EDT when a binary frame arrives.
    protected abstract void onMessage(byte[] message);

    /// Called on the EDT when the transport raises an error. The
    /// exception is usually a [WebSocketException].
    protected abstract void onError(Exception ex);

    /// Sends a text frame. If the connection isn't open the call is
    /// routed through `#onError` rather than throwing.
    public void send(String message) {
        if (impl != null && getReadyState() == WebSocketState.OPEN) {
            impl.sendString(message);
        } else {
            onError(new IOException(
                    "Attempt to send message while socket is not open: " + getReadyState()));
        }
    }

    /// Sends a binary frame. Same error-routing contract as
    /// `#send(String)`.
    public void send(byte[] message) {
        if (impl != null && getReadyState() == WebSocketState.OPEN) {
            impl.sendBytes(message);
        } else {
            onError(new IOException(
                    "Attempt to send message while socket is not open: " + getReadyState()));
        }
    }

    /// Initiates the closing handshake. The `#onClose` callback fires
    /// once the peer acknowledges. Calling `close()` on an
    /// already-closed socket is a no-op.
    public void close() {
        if (impl != null && getReadyState() != WebSocketState.CLOSED) {
            impl.close();
        }
    }

    /// Initiates the opening handshake. May be called from the EDT (in
    /// which case the actual `setUrl` + `connect` happens on a
    /// dedicated worker thread to avoid blocking the UI). Subsequent
    /// state transitions are dispatched back via the `onXxx` callbacks.
    public void connect() {
        if (connecting || getReadyState() != WebSocketState.CLOSED || impl == null) {
            return;
        }
        if (Display.getInstance().isEdt()) {
            socketThread = Display.getInstance().startThread(new Runnable() {
                @Override
                public void run() {
                    connect();
                }
            }, "WebSocket");
            socketThread.start();
        } else {
            connecting = true;
            try {
                impl.setUrl(url);
                impl.connect();
            } finally {
                connecting = false;
            }
        }
    }

    /// Returns the current state of the socket. See [WebSocketState].
    public WebSocketState getReadyState() {
        if (impl == null) {
            return connecting ? WebSocketState.CONNECTING : WebSocketState.CLOSED;
        }
        int state = impl.getReadyState();
        switch (state) {
            case 0:  return WebSocketState.CONNECTING;
            case 1:  return WebSocketState.OPEN;
            case 2:  return WebSocketState.CLOSING;
            default: return WebSocketState.CLOSED;
        }
    }

    // ---- Inbound-event entry points called by the per-platform
    // ---- WebSocketImpl. The platform impl must dispatch on the EDT
    // ---- before calling these so application code never has to.

    /// Routed from [WebSocketImpl] when the underlying transport finishes
    /// the opening handshake.
    public void onOpenReceived() {
        connecting = false;
        onOpen();
    }

    /// Routed from [WebSocketImpl] when a text frame arrives.
    public void onMessageReceived(String message) {
        connecting = false;
        onMessage(message);
    }

    /// Routed from [WebSocketImpl] when a binary frame arrives.
    public void onMessageReceived(byte[] message) {
        onMessage(message);
    }

    /// Routed from [WebSocketImpl] when the connection has closed.
    public void onCloseReceived(int statusCode, String reason) {
        onClose(statusCode, reason);
    }

    /// Routed from [WebSocketImpl] when the transport raises an error.
    public void onErrorReceived(String message, int code) {
        onError(new WebSocketException(message == null ? "" : message, code));
    }
}
