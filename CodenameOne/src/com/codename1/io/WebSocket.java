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

package com.codename1.io;

import com.codename1.impl.WebSocketEventSink;
import com.codename1.impl.WebSocketImpl;

/// Client-side WebSocket. Connections are created via [build] and configured
/// with fluent handler setters before being started with [connect]:
///
/// ```
/// WebSocket ws = WebSocket.build("wss://example.com/chat")
///     .onConnect(w        -> w.send("hello"))
///     .onTextMessage((w, m) -> Log.p("recv " + m))
///     .onClose((w, c, r)  -> Log.p("closed " + c + " / " + r))
///     .onError((w, e)     -> Log.e(e))
///     .connect();
/// ```
///
/// Each handler receives the `WebSocket` as its first argument so it can
/// send, query state, or close without capturing an external reference.
///
/// Handlers fire on a background thread. Use
/// `Display.getInstance().callSerially(...)` inside a handler if you need
/// to touch UI from it.
///
/// Use [isSupported] to check at runtime whether the current port supports
/// WebSocket -- older ports return `false` and [build] will throw on them.
public final class WebSocket {

    /// Handler for the connection-established event.
    public interface ConnectHandler {
        void onConnect(WebSocket ws);
    }

    /// Handler for an incoming text frame.
    public interface TextHandler {
        void onText(WebSocket ws, String message);
    }

    /// Handler for an incoming binary frame.
    public interface BinaryHandler {
        void onBinary(WebSocket ws, byte[] message);
    }

    /// Handler for the close event.
    ///
    /// @param statusCode the RFC 6455 close status code (1000 = normal, etc.)
    /// @param reason     the human-readable reason, or empty string
    public interface CloseHandler {
        void onClose(WebSocket ws, int statusCode, String reason);
    }

    /// Handler for transport- or protocol-level errors. The connection is
    /// closed by the time this fires.
    public interface ErrorHandler {
        void onError(WebSocket ws, Exception ex);
    }

    private final WebSocketImpl impl;
    private volatile ConnectHandler connectHandler;
    private volatile TextHandler textHandler;
    private volatile BinaryHandler binaryHandler;
    private volatile CloseHandler closeHandler;
    private volatile ErrorHandler errorHandler;

    private WebSocket(WebSocketImpl impl) {
        this.impl = impl;
        final WebSocket self = this;
        impl.setEventSink(new WebSocketEventSink() {
            public void onConnect() {
                ConnectHandler h = connectHandler;
                if (h != null) {
                    try {
                        h.onConnect(self);
                    } catch (Throwable t) {
                        dispatchError(t);
                    }
                }
            }

            public void onTextMessage(String message) {
                TextHandler h = textHandler;
                if (h != null) {
                    try {
                        h.onText(self, message);
                    } catch (Throwable t) {
                        dispatchError(t);
                    }
                }
            }

            public void onBinaryMessage(byte[] message) {
                BinaryHandler h = binaryHandler;
                if (h != null) {
                    try {
                        h.onBinary(self, message);
                    } catch (Throwable t) {
                        dispatchError(t);
                    }
                }
            }

            public void onClose(int statusCode, String reason) {
                CloseHandler h = closeHandler;
                if (h != null) {
                    try {
                        h.onClose(self, statusCode, reason);
                    } catch (Throwable t) {
                        dispatchError(t);
                    }
                }
            }

            public void onError(Exception ex) {
                ErrorHandler h = errorHandler;
                if (h != null) {
                    try {
                        h.onError(self, ex);
                    } catch (Throwable swallow) {
                        // Last-ditch: a throwable from the user's error
                        // handler has nowhere else to go without
                        // recursing into onError again. Drop it on the
                        // floor.
                        swallow.toString();
                    }
                }
            }

            private void dispatchError(Throwable t) {
                ErrorHandler h = errorHandler;
                if (h != null) {
                    Exception wrapped = (t instanceof Exception) ? (Exception) t : new RuntimeException(t);
                    try {
                        h.onError(self, wrapped);
                    } catch (Throwable swallow) {
                        // Same rationale as onError() above.
                        swallow.toString();
                    }
                }
            }
        });
    }

    /// Whether the current port supports WebSocket.
    public static boolean isSupported() {
        return Util.getImplementation().isWebSocketSupported();
    }

    /// Create an unconnected WebSocket bound to `url`. The URL must use the
    /// `ws://` or `wss://` scheme. Call [connect] to start the handshake.
    ///
    /// @throws RuntimeException if the current port does not support WebSocket.
    public static WebSocket build(String url) {
        WebSocketImpl impl = Util.getImplementation().createWebSocketImpl(url);
        return new WebSocket(impl);
    }

    /// Register a handler for the connection-established event. Returns
    /// `this` for chaining.
    public WebSocket onConnect(ConnectHandler handler) {
        this.connectHandler = handler;
        return this;
    }

    /// Register a handler for incoming text frames. Returns `this` for chaining.
    public WebSocket onTextMessage(TextHandler handler) {
        this.textHandler = handler;
        return this;
    }

    /// Register a handler for incoming binary frames. Returns `this` for chaining.
    public WebSocket onBinaryMessage(BinaryHandler handler) {
        this.binaryHandler = handler;
        return this;
    }

    /// Register a handler for the close event. Returns `this` for chaining.
    public WebSocket onClose(CloseHandler handler) {
        this.closeHandler = handler;
        return this;
    }

    /// Register a handler for transport errors. Returns `this` for chaining.
    public WebSocket onError(ErrorHandler handler) {
        this.errorHandler = handler;
        return this;
    }

    /// Start the handshake using the platform default connect timeout.
    /// Returns `this` for chaining; success is signalled asynchronously
    /// via the registered [ConnectHandler].
    public WebSocket connect() {
        impl.connect(0);
        return this;
    }

    /// Start the handshake with an explicit connect timeout in milliseconds.
    /// `0` means "use platform default".
    public WebSocket connect(int connectTimeoutMs) {
        impl.connect(connectTimeoutMs);
        return this;
    }

    /// Close the connection. Idempotent.
    public void close() {
        impl.close();
    }

    /// Send a text frame. Throws `IllegalStateException` if the connection
    /// is not [WebSocketState#OPEN].
    public void send(String text) {
        impl.sendText(text);
    }

    /// Send a binary frame. Throws `IllegalStateException` if the connection
    /// is not [WebSocketState#OPEN].
    public void send(byte[] binary) {
        impl.sendBinary(binary);
    }

    public WebSocketState getReadyState() {
        return impl.getReadyState();
    }

    public String getUrl() {
        return impl.getUrl();
    }
}
