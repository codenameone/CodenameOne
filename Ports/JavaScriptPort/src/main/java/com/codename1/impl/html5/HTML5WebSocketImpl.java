/*
 * Copyright (c) 2026 Codename One and contributors.
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * You may use this file only in compliance with that license.
 * The license notice for this subtree is available in Ports/JavaScriptPort/LICENSE.md.
 */
package com.codename1.impl.html5;

import com.codename1.html5.js.JSBody;
import com.codename1.html5.js.JSObject;
import com.codename1.html5.js.dom.Event;
import com.codename1.html5.js.dom.EventListener;
import com.codename1.html5.js.typedarrays.ArrayBuffer;
import com.codename1.html5.js.typedarrays.Uint8Array;
import com.codename1.impl.WebSocketImpl;
import com.codename1.io.WebSocketState;

/**
 * JavaScript port WebSocket implementation backed by the browser's native
 * `WebSocket` global.
 *
 * Interop rules that matter here:
 *  - Browser events are delivered through DOM EventListener objects
 *    (handleEvent), the same way HTML5Media wires its media events -- NOT
 *    through @JSFunctor functions. A @JSFunctor handed to addEventListener is
 *    never invoked (the socket opens but no listener fires).
 *  - JSObject interface-method arguments marshal cleanly (a Java String becomes
 *    a JS string, an EventListener stays callable), so send / close /
 *    addEventListener are interface methods.
 *  - @JSBody script params do NOT marshal (a Java String arrives as the
 *    ParparVM String object, "[object Object]"). The only @JSBody here is
 *    createSocket (we need `new WebSocket`); it converts the URL with the
 *    runtime's jvm.toNativeString. Event field readers wrap JS strings with
 *    createJavaString.
 */
class HTML5WebSocketImpl extends WebSocketImpl {

    /// JSObject view of a browser WebSocket. Interface-method calls, so the
    /// String / EventListener arguments marshal cleanly.
    private interface BrowserWebSocket extends JSObject {
        void send(String message);
        void send(ArrayBuffer data);
        void close(int code, String reason);
        int getReadyState();
        void addEventListener(String type, EventListener<Event> listener);
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
        w.addEventListener("open", new EventListener<Event>() {
            @Override
            public void handleEvent(Event evt) {
                state = WebSocketState.OPEN;
                sink().onConnect();
            }
        });
        w.addEventListener("message", new EventListener<Event>() {
            @Override
            public void handleEvent(Event evt) {
                if (eventDataIsString(evt)) {
                    sink().onTextMessage(eventDataAsString(evt));
                } else {
                    sink().onBinaryMessage(toByteArray(eventDataAsBuffer(evt)));
                }
            }
        });
        w.addEventListener("close", new EventListener<Event>() {
            @Override
            public void handleEvent(Event evt) {
                state = WebSocketState.CLOSED;
                sink().onClose(eventCode(evt), eventReason(evt));
            }
        });
        w.addEventListener("error", new EventListener<Event>() {
            @Override
            public void handleEvent(Event evt) {
                WebSocketState prev = state;
                state = WebSocketState.CLOSED;
                if (prev != WebSocketState.CLOSED) {
                    sink().onError(new RuntimeException("WebSocket error"));
                }
            }
        });
    }

    // The only @JSBody: we need `new WebSocket`. The URL is a Java String, which
    // is not auto-marshalled into the script, so convert it with the runtime's
    // jvm.toNativeString (the same helper the generated code uses everywhere).
    @JSBody(params = {"url"}, script =
            "var w = new WebSocket(jvm.toNativeString(url));\n"
            + "w.binaryType = 'arraybuffer';\n"
            + "return w;")
    private static native BrowserWebSocket createSocket(String url);

    @JSBody(params = {"e"}, script = "return (typeof e.data === 'string');")
    private static native boolean eventDataIsString(JSObject e);

    @JSBody(params = {"e"}, script = "return '' + e.data;")
    private static native String eventDataAsString(JSObject e);

    @JSBody(params = {"e"}, script = "return e.data;")
    private static native ArrayBuffer eventDataAsBuffer(JSObject e);

    @JSBody(params = {"e"}, script = "return e.code|0;")
    private static native int eventCode(JSObject e);

    @JSBody(params = {"e"}, script = "return '' + (e.reason || '');")
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
        view.set(message);
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
