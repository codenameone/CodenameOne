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
    /// actually uses is declared.
    private interface BrowserWebSocket extends JSObject {
        void send(String message);
        void send(ArrayBuffer data);
        void close(int code, String reason);
        int getReadyState();
    }

    @JSFunctor
    private interface OpenCallback extends JSObject {
        void call();
    }

    @JSFunctor
    private interface TextCallback extends JSObject {
        void call(String message);
    }

    @JSFunctor
    private interface BinaryCallback extends JSObject {
        void call(ArrayBuffer data);
    }

    @JSFunctor
    private interface CloseCallback extends JSObject {
        void call(int code, String reason);
    }

    @JSFunctor
    private interface ErrorCallback extends JSObject {
        void call(String message);
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
        ws = createNative(
                getUrl(),
                new OpenCallback() {
                    @Override
                    public void call() {
                        state = WebSocketState.OPEN;
                        sink().onConnect();
                    }
                },
                new TextCallback() {
                    @Override
                    public void call(String message) {
                        sink().onTextMessage(message);
                    }
                },
                new BinaryCallback() {
                    @Override
                    public void call(ArrayBuffer data) {
                        sink().onBinaryMessage(toByteArray(data));
                    }
                },
                new CloseCallback() {
                    @Override
                    public void call(int code, String reason) {
                        state = WebSocketState.CLOSED;
                        sink().onClose(code, reason == null ? "" : reason);
                    }
                },
                new ErrorCallback() {
                    @Override
                    public void call(String message) {
                        WebSocketState prev = state;
                        state = WebSocketState.CLOSED;
                        if (prev != WebSocketState.CLOSED) {
                            sink().onError(new RuntimeException(message == null ? "WebSocket error" : message));
                        }
                    }
                }
        );
    }

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

    @JSBody(
            params = {"url", "onOpen", "onText", "onBinary", "onClose", "onError"},
            script =
                    "var w = new WebSocket(url);\n" +
                    "w.binaryType = 'arraybuffer';\n" +
                    "w.onopen = function() { onOpen(); };\n" +
                    "w.onmessage = function(e) {\n" +
                    "  if (typeof e.data === 'string') { onText(e.data); }\n" +
                    "  else { onBinary(e.data); }\n" +
                    "};\n" +
                    "w.onclose = function(e) { onClose(e.code|0, e.reason || ''); };\n" +
                    "w.onerror = function() { onError('WebSocket error'); };\n" +
                    "return w;"
    )
    private static native BrowserWebSocket createNative(
            String url,
            OpenCallback onOpen,
            TextCallback onText,
            BinaryCallback onBinary,
            CloseCallback onClose,
            ErrorCallback onError);
}
