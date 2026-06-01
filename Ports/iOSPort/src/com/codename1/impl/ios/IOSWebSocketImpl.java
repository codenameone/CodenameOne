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
package com.codename1.impl.ios;

import com.codename1.impl.WebSocketImpl;
import com.codename1.io.WebSocketState;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * iOS WebSocket implementation backed by NSURLSessionWebSocketTask. The
 * heavy lifting happens in Ports/iOSPort/nativeSources/WebSocketImpl.{h,m};
 * this class is a thin Java adapter that:
 *
 *  - assigns each connection a unique int id and registers itself in a
 *    static map so the native side can route incoming events back to the
 *    right instance via the fireXxx(int, ...) static methods;
 *  - delegates lifecycle / send / state calls to IOSNative.
 */
class IOSWebSocketImpl extends WebSocketImpl {

    private static final AtomicInteger nextId = new AtomicInteger(1);
    private static final Map<Integer, IOSWebSocketImpl> registry =
            new HashMap<Integer, IOSWebSocketImpl>();

    private final int connectionId;
    private long nativePtr;
    private volatile WebSocketState state = WebSocketState.CONNECTING;

    IOSWebSocketImpl(String url) {
        super(url);
        this.connectionId = nextId.getAndIncrement();
        synchronized (registry) {
            registry.put(Integer.valueOf(connectionId), this);
        }
        this.nativePtr = IOSImplementation.nativeInstance.createWebSocketNative(connectionId, url);
    }

    @Override
    public void connect(int connectTimeoutMs) {
        IOSImplementation.nativeInstance.connectWebSocketNative(nativePtr, connectTimeoutMs);
    }

    @Override
    public void close() {
        if (state == WebSocketState.CLOSED || state == WebSocketState.CLOSING) {
            return;
        }
        state = WebSocketState.CLOSING;
        IOSImplementation.nativeInstance.closeWebSocketNative(nativePtr);
    }

    @Override
    public void sendText(String message) {
        if (state != WebSocketState.OPEN) {
            throw new IllegalStateException("WebSocket is not open");
        }
        IOSImplementation.nativeInstance.sendWebSocketTextNative(nativePtr, message);
    }

    @Override
    public void sendBinary(byte[] message) {
        if (state != WebSocketState.OPEN) {
            throw new IllegalStateException("WebSocket is not open");
        }
        IOSImplementation.nativeInstance.sendWebSocketBinaryNative(nativePtr, message);
    }

    @Override
    public WebSocketState getReadyState() {
        return state;
    }

    /* ---------- static dispatch from native code ---------- */

    public static void fireConnect(int connectionId) {
        IOSWebSocketImpl ws = lookup(connectionId);
        if (ws == null) {
            return;
        }
        ws.state = WebSocketState.OPEN;
        ws.sink().onConnect();
    }

    public static void fireTextMessage(int connectionId, String message) {
        IOSWebSocketImpl ws = lookup(connectionId);
        if (ws != null) {
            ws.sink().onTextMessage(message);
        }
    }

    public static void fireBinaryMessage(int connectionId, byte[] message) {
        IOSWebSocketImpl ws = lookup(connectionId);
        if (ws != null) {
            ws.sink().onBinaryMessage(message);
        }
    }

    public static void fireClose(int connectionId, int code, String reason) {
        IOSWebSocketImpl ws = lookup(connectionId);
        if (ws == null) {
            return;
        }
        ws.state = WebSocketState.CLOSED;
        ws.releaseNative();
        ws.sink().onClose(code, reason == null ? "" : reason);
    }

    public static void fireError(int connectionId, String message) {
        IOSWebSocketImpl ws = lookup(connectionId);
        if (ws == null) {
            return;
        }
        WebSocketState prev = ws.state;
        ws.state = WebSocketState.CLOSED;
        ws.releaseNative();
        if (prev != WebSocketState.CLOSED) {
            ws.sink().onError(new RuntimeException(message == null ? "WebSocket error" : message));
        }
    }

    private static IOSWebSocketImpl lookup(int connectionId) {
        synchronized (registry) {
            return registry.get(Integer.valueOf(connectionId));
        }
    }

    private void releaseNative() {
        synchronized (registry) {
            registry.remove(Integer.valueOf(connectionId));
        }
        long ptr = nativePtr;
        nativePtr = 0L;
        if (ptr != 0L) {
            IOSImplementation.nativeInstance.releaseWebSocketNative(ptr);
        }
    }
}
