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
package com.codename1.io.mqtt;

import com.codename1.io.WebSocket;
import com.codename1.ui.CN;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

/// MQTT over WebSocket (the `mqtt` subprotocol, one binary message per write),
/// through [WebSocket]. This is the way to TLS: use a `wss://` URL. A packet may
/// span messages and a message may hold several packets; the client reassembles.
final class WebSocketMqttTransport implements MqttTransport {
    private final String url;
    private final int timeout;
    private final AtomicBoolean closed = new AtomicBoolean();
    private WebSocket socket;

    WebSocketMqttTransport(String url, int timeout) {
        this.url = url;
        this.timeout = timeout;
    }

    public void open(final Handler h) {
        if (!WebSocket.isSupported()) {
            h.closed("WebSockets are not supported on this platform");
            return;
        }
        socket = WebSocket.build(url).subprotocols("mqtt")
                .onConnect(new WebSocket.ConnectHandler() {
                    public void onConnect(WebSocket ws) {
                        CN.callSerially(new Runnable() {
                            public void run() {
                                if (!closed.get()) {
                                    h.opened();
                                }
                            }
                        });
                    }
                })
                .onBinaryMessage(new WebSocket.BinaryHandler() {
                    public void onBinary(WebSocket ws, final byte[] message) {
                        CN.callSerially(new Runnable() {
                            public void run() {
                                if (!closed.get()) {
                                    h.received(message, message.length);
                                }
                            }
                        });
                    }
                })
                .onClose(new WebSocket.CloseHandler() {
                    public void onClose(WebSocket ws, final int statusCode, final String reason) {
                        fail(h, "WebSocket closed (" + statusCode + ") " + reason);
                    }
                })
                .onError(new WebSocket.ErrorHandler() {
                    public void onError(WebSocket ws, Exception ex) {
                        fail(h, "WebSocket error: " + ex);
                    }
                })
                .connect(timeout);
    }

    private void fail(final Handler h, final String reason) {
        CN.callSerially(new Runnable() {
            public void run() {
                if (!closed.get()) {
                    h.closed(reason);
                }
            }
        });
    }

    public void send(byte[] data) throws IOException {
        if (socket == null || closed.get()) {
            throw new IOException("Not connected");
        }
        socket.send(data);
    }

    public void close() {
        closed.set(true);
        if (socket != null) {
            socket.close();
        }
    }
}
