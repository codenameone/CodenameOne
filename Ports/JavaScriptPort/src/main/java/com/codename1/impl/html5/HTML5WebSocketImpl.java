/*
 * Copyright (c) 2026 Codename One and contributors.
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * You may use this file only in compliance with that license.
 * The license notice for this subtree is available in Ports/JavaScriptPort/LICENSE.md.
 */
package com.codename1.impl.html5;

import com.codename1.html5.js.JSBody;
import com.codename1.html5.js.JSFunctor;
import com.codename1.html5.js.JSObject;
import com.codename1.html5.js.typedarrays.ArrayBuffer;
import com.codename1.html5.js.typedarrays.Uint8Array;
import com.codename1.impl.WebSocketImpl;
import com.codename1.io.WebSocketState;

/**
 * JavaScript port WebSocket implementation backed by the browser's native
 * `WebSocket` global. Uses TeaVM @JSBody / @JSFunctor for the bridge -- no
 * shared-state map needed since each instance carries its own callback
 * closures into JS.
 */
class HTML5WebSocketImpl extends WebSocketImpl {

    /// JSObject view of a browser WebSocket. Only the surface this class
    /// actually uses is declared. Listeners are attached with addEventListener
    /// passing an @JSFunctor directly (the same pattern HTML5Media uses for its
    /// media events) -- registering them by name inside a @JSBody script's
    /// nested closure instead leaves the @JSFunctor params un-callable
    /// ("onError is not a function") once the event fires asynchronously.
    private interface BrowserWebSocket extends JSObject {
        void send(String message);
        void send(ArrayBuffer data);
        void close(int code, String reason);
        int getReadyState();
        void addEventListener(String type, EventCallback listener);
    }

    /// A single browser event listener. The raw Event object is handed back so
    /// its fields (data / code / reason) are read on the Java side.
    @JSFunctor
    private interface EventCallback extends JSObject {
        void call(JSObject event);
    }

    private BrowserWebSocket ws;
    private volatile WebSocketState state = WebSocketState.CONNECTING;

    HTML5WebSocketImpl(String url) {
        super(url);
    }

    @Override
    public void connect(int connectTimeoutMs) {
        // connectTimeoutMs is unused: browser WebSockets don't expose a
        // per-connect timeout. Users who need one should wrap connect()
        // with their own Display.callSerially-driven timer.
        final BrowserWebSocket w = createSocket(getUrl());
        ws = w;
        w.addEventListener("open", new EventCallback() {
            @Override
            public void call(JSObject event) {
                state = WebSocketState.OPEN;
                sink().onConnect();
            }
        });
        w.addEventListener("message", new EventCallback() {
            @Override
            public void call(JSObject event) {
                if (eventDataIsString(event)) {
                    sink().onTextMessage(eventDataAsString(event));
                } else {
                    sink().onBinaryMessage(toByteArray(eventDataAsBuffer(event)));
                }
            }
        });
        w.addEventListener("close", new EventCallback() {
            @Override
            public void call(JSObject event) {
                state = WebSocketState.CLOSED;
                sink().onClose(eventCode(event), eventReason(event));
            }
        });
        w.addEventListener("error", new EventCallback() {
            @Override
            public void call(JSObject event) {
                WebSocketState prev = state;
                state = WebSocketState.CLOSED;
                if (prev != WebSocketState.CLOSED) {
                    sink().onError(new RuntimeException("WebSocket error"));
                }
            }
        });
    }

    @JSBody(params = {"url"}, script =
            "var w = new WebSocket(url);\n"
            + "w.binaryType = 'arraybuffer';\n"
            + "return w;")
    private static native BrowserWebSocket createSocket(String url);

    @JSBody(params = {"e"}, script = "return (typeof e.data === 'string');")
    private static native boolean eventDataIsString(JSObject e);

    @JSBody(params = {"e"}, script = "return e.data;")
    private static native String eventDataAsString(JSObject e);

    @JSBody(params = {"e"}, script = "return e.data;")
    private static native ArrayBuffer eventDataAsBuffer(JSObject e);

    @JSBody(params = {"e"}, script = "return e.code|0;")
    private static native int eventCode(JSObject e);

    @JSBody(params = {"e"}, script = "return e.reason || '';")
    private static native String eventReason(JSObject e);

    @Override
    public void close() {
        if (state == WebSocketState.CLOSED || state == WebSocketState.CLOSING) {
            return;
        }
        state = WebSocketState.CLOSING;
        if (ws != null) {
            ws.close(1000, "");
        }
    }

    @Override
    public void sendText(String message) {
        if (state != WebSocketState.OPEN) {
            throw new IllegalStateException("WebSocket is not open");
        }
        ws.send(message);
    }

    @Override
    public void sendBinary(byte[] message) {
        if (state != WebSocketState.OPEN) {
            throw new IllegalStateException("WebSocket is not open");
        }
        Uint8Array view = Uint8Array.create(message.length);
        for (int i = 0; i < message.length; i++) {
            view.set(i, (short) (message[i] & 0xFF));
        }
        ws.send(view.getBuffer());
    }

    @Override
    public WebSocketState getReadyState() {
        return state;
    }

    private static byte[] toByteArray(ArrayBuffer data) {
        Uint8Array view = Uint8Array.create(data);
        int len = view.getLength();
        byte[] out = new byte[len];
        for (int i = 0; i < len; i++) {
            out[i] = (byte) view.get(i);
        }
        return out;
    }
}
