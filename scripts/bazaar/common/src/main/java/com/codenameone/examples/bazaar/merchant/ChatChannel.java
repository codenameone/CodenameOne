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
package com.codenameone.examples.bazaar.merchant;

import com.codename1.appkit.Net;
import com.codename1.io.CharArrayReader;
import com.codename1.io.JSONParser;
import com.codename1.io.WebSocket;
import com.codename1.io.WebSocketState;
import com.codename1.ui.CN;
import com.codenameone.examples.bazaar.net.Api;
import com.codenameone.examples.bazaar.net.Session;
import java.io.IOException;
import java.util.Map;
import java.util.Timer;

/// The inbox's connection to the store chat socket, `/ws/chat`, as one more
/// member of the store's staff -- the same socket the web inbox uses. It is
/// let in by the single-use ticket the app's live channel takes, and the
/// server checks the store and the inbox rights when it opens and again for
/// every conversation a frame names.
///
/// It is open while the inbox is on screen. A shopper can ring the store only
/// while some member of staff is connected here or on the web inbox; with
/// nobody, the shopper's widget says nobody is available.
///
/// Reconnects with backoff, like the live channel. Each connection opens with
/// a `hello` carrying fresh ICE servers, which is when the inbox asks again
/// for the thread it shows.
final class ChatChannel {
    /// What the inbox hears; every call is on the EDT.
    interface Listener {
        void frame(String type, Map<String, Object> frame);

        void lost();
    }

    private static final int PING_MILLIS = 25000;

    private final String storeId;
    private final Listener listener;
    private WebSocket socket;
    private boolean wanted;
    private boolean open;
    private int attempts;
    private int generation;
    private Timer pinger;

    ChatChannel(String storeId, Listener listener) {
        this.storeId = storeId;
        this.listener = listener;
    }

    void start() {
        if (wanted) {
            return;
        }
        wanted = true;
        attempts = 0;
        connect();
    }

    void stop() {
        wanted = false;
        open = false;
        generation++;
        if (pinger != null) {
            pinger.cancel();
            pinger = null;
        }
        if (socket != null) {
            socket.close();
            socket = null;
        }
    }

    boolean isOpen() {
        return open;
    }

    /// Sends a frame; answers false when there is no connection to send on.
    boolean send(Map<String, Object> frame) {
        if (!open || socket == null || socket.getReadyState() != WebSocketState.OPEN) {
            return false;
        }
        socket.send(JSONParser.mapToJson(frame));
        return true;
    }

    private void connect() {
        if (!wanted || !WebSocket.isSupported()) {
            return;
        }
        final int mine = ++generation;
        Api.account().liveTicket(Net.to(ticket -> {
            if (mine == generation && wanted) {
                connect(ticket.ticket(), mine);
            }
        }, (status, message) -> lost(mine)));
    }

    private void connect(String ticket, final int mine) {
        String base = Session.serverUrl();
        String url = "ws" + base.substring(4) + "/ws/chat?staff=" + ticket + "&store=" + storeId;
        socket = WebSocket.build(url)
                .onConnect(ws -> CN.callSerially(() -> opened(mine)))
                .onTextMessage((ws, text) -> CN.callSerially(() -> frame(text, mine)))
                .onClose((ws, code, reason) -> CN.callSerially(() -> lost(mine)))
                .onError((ws, err) -> CN.callSerially(() -> lost(mine)))
                .connect(10000);
    }

    private void opened(final int mine) {
        if (mine != generation) {
            return;
        }
        open = true;
        attempts = 0;
        if (pinger != null) {
            pinger.cancel();
        }
        pinger = CN.setInterval(PING_MILLIS, () -> CN.callSerially(() -> {
            if (mine == generation) {
                send(ChatFrames.ping());
            }
        }));
    }

    private void lost(int mine) {
        if (mine != generation || !wanted) {
            return;
        }
        open = false;
        final int waiting = ++generation;
        if (pinger != null) {
            pinger.cancel();
            pinger = null;
        }
        if (socket != null) {
            socket.close();
            socket = null;
        }
        listener.lost();
        int delay = 1000 << Math.min(attempts++, 5);
        CN.setTimeout(delay, () -> CN.callSerially(() -> {
            if (waiting == generation) {
                connect();
            }
        }));
    }

    private void frame(String text, int mine) {
        if (mine != generation) {
            return;
        }
        Map<String, Object> json;
        try {
            json = new JSONParser().parseJSON(new CharArrayReader(text.toCharArray()));
        } catch (IOException malformed) {
            return;
        }
        Object type = json.get("type");
        if (type instanceof String) {
            listener.frame((String) type, json);
        }
    }
}
