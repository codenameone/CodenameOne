/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.splat.net;

import com.codename1.appkit.Net;

import com.codename1.io.CharArrayReader;
import com.codename1.io.JSONParser;
import com.codename1.io.WebSocket;
import com.codename1.io.WebSocketState;
import com.codename1.ui.CN;
import java.io.IOException;
import java.util.Map;
import java.util.Timer;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/// The game socket, `/ws/game` on the server. One is open while the player is
/// queued, playing, or (as an admin) watching.
///
/// Opening it takes a one-time ticket from the REST API, because a browser
/// cannot put a bearer token on a WebSocket. Every message arrives on the EDT,
/// already parsed.
// tag::splat-game-link[]
public final class GameLink {
    /// What a screen does with the socket.
    public interface Listener {
        void message(Map<String, Object> message);

        /// The socket is gone. `code` is the WebSocket close code, or 0 when
        /// it never opened or dropped without one.
        void closed(int code);
    }

    private static final int PING_MILLIS = 20000;
    private final Listener listener;
    private WebSocket socket;
    private Timer pinger;
    private boolean closed;

    private GameLink(Listener listener) {
        this.listener = listener;
    }

    /// Opens a socket; `listener` hears `ready` first, then whatever follows.
    public static GameLink open(Listener listener) {
        final GameLink link = new GameLink(listener);
        if (!WebSocket.isSupported()) {
            CN.callSerially(() -> link.lost(0));
            return link;
        }
        Api.account().gameTicket(Net.to(ticket -> link.connect(ticket.ticket()),
                (status, message) -> link.lost(0)));
        return link;
    }

    private void connect(String ticket) {
        if (closed) {
            return;
        }
        String base = Session.serverUrl();
        String url = "ws" + base.substring(4) + "/ws/game?ticket=" + ticket;
        socket = WebSocket.build(url)
                .onConnect(ws -> CN.callSerially(this::opened))
                .onTextMessage((ws, text) -> {
                    arrived.incrementAndGet();
                    CN.callSerially(() -> frame(text));
                })
                .onClose((ws, code, reason) -> CN.callSerially(() -> lost(code)))
                .onError((ws, err) -> {
                    error.set(String.valueOf(err));
                    CN.callSerially(() -> lost(0));
                })
                .connect(10000);
    }

    private void opened() {
        pinger = CN.setInterval(PING_MILLIS, () -> send("{\"type\":\"ping\"}"));
    }

    /// Frames the socket has delivered and frames the EDT has read, for a test
    /// that has to tell a server that went quiet from a client that stopped
    /// reading. The first is counted on the socket's own thread.
    private final AtomicInteger arrived = new AtomicInteger();
    private int read;
    private String closedBy;
    private int largest;
    private long lastFrameAt;
    /// The socket's last error, written on the socket's own thread.
    private final AtomicReference<String> error = new AtomicReference<String>();

    /// Where the socket stands, for diagnostics.
    public String describe() {
        return "frames arrived " + arrived.get() + ", read " + read + ", largest " + largest + " chars"
                + (closed ? ", closed by " + closedBy : "");
    }

    private void frame(String text) {
        read++;
        lastFrameAt = System.currentTimeMillis();
        largest = Math.max(largest, text.length());
        if (closed) {
            return;
        }
        try {
            listener.message(new JSONParser().parseJSON(new CharArrayReader(text.toCharArray())));
        } catch (IOException malformed) {
            // A frame that is not JSON is not from our server; ignore it.
        }
    }

    private void lost(int code) {
        if (closed) {
            return;
        }
        closedBy = "socket, code " + code + (error.get() == null ? "" : " (" + error.get() + ")") + ", "
                + (System.currentTimeMillis() - lastFrameAt) + " ms after the last frame";
        close();
        listener.closed(code);
    }

    public void send(String json) {
        if (!closed && socket != null && socket.getReadyState() == WebSocketState.OPEN) {
            socket.send(json);
        }
    }

    public void close() {
        if (closedBy == null) {
            closedBy = "client";
        }
        closed = true;
        if (pinger != null) {
            pinger.cancel();
            pinger = null;
        }
        if (socket != null) {
            socket.close();
            socket = null;
        }
    }

    public boolean isClosed() {
        return closed;
    }

    public static String text(Map<String, Object> m, String key) {
        Object v = m.get(key);
        return v == null ? "" : v.toString();
    }

    public static int number(Map<String, Object> m, String key) {
        Object v = m.get(key);
        return v instanceof Number ? ((Number) v).intValue() : 0;
    }
}
// end::splat-game-link[]
