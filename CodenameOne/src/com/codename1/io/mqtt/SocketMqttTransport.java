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

import com.codename1.io.Socket;
import com.codename1.io.SocketConnection;
import com.codename1.ui.CN;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicBoolean;

/// Plain TCP through [Socket]. A thread of the socket layer blocks in `read`
/// and hands each chunk to the EDT; writes are made on the caller's thread.
final class SocketMqttTransport implements MqttTransport {
    private final String host;
    private final int port;
    private final int timeout;
    private final AtomicBoolean closed = new AtomicBoolean();
    private OutputStream out;
    private InputStream in;

    SocketMqttTransport(String host, int port, int timeout) {
        this.host = host;
        this.port = port;
        this.timeout = timeout;
    }

    @Override
    public void open(final Handler h) {
        if (!Socket.isSupported()) {
            h.closed("Sockets are not supported on this platform");
            return;
        }
        Socket.connect(host, port, new SocketConnection(timeout) {
            @Override
            public void connectionError(int errorCode, String message) {
                post(h, null, 0, "Connect failed: " + message);
            }

            @Override
            public void connectionEstablished(InputStream is, OutputStream os) {
                in = is;
                out = os;
                if (closed.get()) {
                    closeStreams();
                    return;
                }
                CN.callSerially(new Runnable() {
                    @Override
                    public void run() {
                        if (!closed.get()) {
                            h.opened();
                        }
                    }
                });
                byte[] chunk = new byte[4096];
                String reason = "Connection closed by the broker";
                try {
                    while (!closed.get()) {
                        int n = is.read(chunk);
                        if (n < 0) {
                            break;
                        }
                        if (n > 0) {
                            byte[] copy = new byte[n];
                            System.arraycopy(chunk, 0, copy, 0, n);
                            post(h, copy, n, null);
                        }
                    }
                } catch (IOException e) {
                    reason = "Read failed: " + e.getMessage();
                }
                post(h, null, 0, reason);
            }
        });
    }

    private void post(final Handler h, final byte[] data, final int len, final String reason) {
        CN.callSerially(new Runnable() {
            @Override
            public void run() {
                if (closed.get()) {
                    return;
                }
                if (reason != null) {
                    h.closed(reason);
                } else {
                    h.received(data, len);
                }
            }
        });
    }

    @Override
    public void send(byte[] data) throws IOException {
        // The connection's own stream, closed by close(); send only borrows it.
        OutputStream o = out; // NOPMD CloseResource
        if (o == null) {
            throw new IOException("Not connected");
        }
        o.write(data);
        o.flush();
    }

    @Override
    public void close() {
        closed.set(true);
        closeStreams();
    }

    private void closeStreams() {
        try {
            if (out != null) {
                out.close();
            }
            if (in != null) {
                in.close();
            }
        } catch (IOException e) {
            // closing; nothing left to report
            out = null;
        }
    }
}
