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

import com.codename1.system.NativeLookup;
import com.codename1.ui.Display;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/// Client-side WebSocket connection following RFC 6455. Subclass and override
/// the four `onXxx` callbacks, then call `#connect()` to open the handshake.
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
/// Check `#isSupported()` before constructing one -- support requires a
/// per-platform native implementation; platforms without one return `false`.
public abstract class WebSocket {
    private static int nextId = 1;
    private static final Map<Integer, WebSocket> sockets = new HashMap<Integer, WebSocket>();

    private WebSocketNativeImpl impl;
    private final String url;
    private Thread socketThread;
    private boolean connecting;

    /// Failure surfaced by the underlying transport. Carries the
    /// platform-specific error code so callers can branch on
    /// "connection refused" vs "TLS error" vs "protocol violation" when the
    /// platform exposes that detail.
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

    /// Returns `true` if the current platform ships a WebSocket native
    /// implementation. Always check before constructing -- on platforms
    /// without an implementation (or in tests with the native layer
    /// stubbed out) the constructor returns a non-functional instance.
    public static boolean isSupported() {
        try {
            WebSocketNativeImpl impl = (WebSocketNativeImpl) NativeLookup.create(WebSocketNativeImpl.class);
            return impl != null && impl.isSupported();
        } catch (Throwable t) {
            return false;
        }
    }

    public WebSocket(String url) {
        this.url = url;
        impl = (WebSocketNativeImpl) NativeLookup.create(WebSocketNativeImpl.class);
        if (impl != null) {
            int id = nextId++;
            impl.setId(id);
            sockets.put(id, this);
        }
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

    /// Called on the EDT when the transport raises an error. The exception is
    /// usually a `WebSocketException`.
    protected abstract void onError(Exception ex);

    /// Sends a text frame. If the connection isn't open the call is routed
    /// through `#onError` rather than throwing.
    public void send(String message) {
        if (impl != null && getReadyState() == WebSocketState.OPEN) {
            impl.sendString(message);
        } else {
            onError(new IOException(
                    "Attempt to send message while socket is not open: " + getReadyState()));
        }
    }

    /// Sends a binary frame. Same error-routing contract as `#send(String)`.
    public void send(byte[] message) {
        if (impl != null && getReadyState() == WebSocketState.OPEN) {
            impl.sendBytes(message);
        } else {
            onError(new IOException(
                    "Attempt to send message while socket is not open: " + getReadyState()));
        }
    }

    /// Initiates the closing handshake. The `#onClose` callback fires once the
    /// peer acknowledges. Calling `close()` on an already-closed socket is a
    /// no-op.
    public void close() {
        if (impl != null && getReadyState() != WebSocketState.CLOSED) {
            impl.close();
        }
    }

    /// Initiates the opening handshake. May be called from the EDT (in which
    /// case the actual `setUrl` + `connect` happens on a dedicated worker
    /// thread to avoid blocking the UI). Subsequent state transitions are
    /// dispatched back via the `onXxx` callbacks.
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
            case 0: return WebSocketState.CONNECTING;
            case 1: return WebSocketState.OPEN;
            case 2: return WebSocketState.CLOSING;
            default: return WebSocketState.CLOSED;
        }
    }

    /// Utility for the iOS port -- allocates a byte array from Java so the
    /// native side can fill it without crossing the bridge for each byte.
    static byte[] newByteArray(int len) {
        return new byte[len];
    }

    // ---- Native-to-Java callback entry points. Marked @Deprecated only to
    // ---- hint that application code shouldn't call them; the per-platform
    // ---- native impls do, by mangled symbol name.

    /// @deprecated Internal callback for native implementations.
    @Deprecated
    public static void messageReceived(int id, String message) {
        WebSocket socket = sockets.get(id);
        if (socket != null) {
            socket.connecting = false;
            socket.onMessage(message);
        }
    }

    /// @deprecated Internal callback for native implementations.
    @Deprecated
    public static void messageReceived(int id, byte[] message) {
        WebSocket socket = sockets.get(id);
        if (socket != null) {
            socket.onMessage(message);
        }
    }

    /// @deprecated Internal callback for native implementations. Wrapper
    /// around `#messageReceived(int, byte[])` to work around an old issue
    /// with overloaded-method dispatch in the JavaScript port.
    @Deprecated
    public static void messageReceivedBytes(int id, byte[] message) {
        messageReceived(id, message);
    }

    /// @deprecated Internal callback for native implementations.
    @Deprecated
    public static void closeReceived(int id, int statusCode, String reason) {
        WebSocket socket = sockets.remove(id);
        if (socket != null) {
            socket.onClose(statusCode, reason);
        }
    }

    /// @deprecated Internal callback for native implementations.
    @Deprecated
    public static void openReceived(int id) {
        WebSocket socket = sockets.get(id);
        if (socket != null) {
            socket.onOpen();
        }
    }

    /// @deprecated Internal callback for native implementations.
    @Deprecated
    public static void errorReceived(int id, String message, int code) {
        WebSocket socket = sockets.get(id);
        if (socket != null) {
            socket.onError(new WebSocketException(message, code));
        }
    }
}
