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
package com.codenameone.examples.parley.live;

import com.codename1.io.CharArrayReader;
import com.codename1.io.JSONParser;
import com.codename1.io.WebSocket;
import com.codename1.io.WebSocketState;
import com.codename1.ui.CN;
import com.codenameone.examples.parley.net.Api;
import com.codenameone.examples.parley.net.Net;
import com.codenameone.examples.parley.net.Session;
import com.codenameone.examples.parley.ui.Nav;

import java.io.IOException;
import java.util.Map;
import java.util.Timer;

/// The WebSocket the server pushes down: messages for this device, word that
/// a conversation changed, call signalling.
///
/// Messages arrive here first when the app is open and by sync otherwise;
/// either way they are the same envelopes, and acknowledging them is what
/// removes them from the server, so a frame lost with a dropped socket is
/// fetched again by the next sync. The server holds nothing it can read: the
/// envelopes are ciphertext addressed to this one device.
///
/// A WebSocket handshake cannot carry the bearer token on every platform, so
/// the connection is let in by a ticket: the app asks for one over the
/// authenticated API, and the server accepts it once, within half a minute.
public final class LiveChannel {
    /// Where frames go. All of it arrives on the EDT.
    public interface Handler {
        /// An envelope for this device, in the same shape a sync returns.
        void envelope(Map<String, Object> frame);

        /// A conversation's title, members or settings changed.
        void conversationChanged(String id);

        /// Call signalling: an offer, an answer, a candidate, a hang-up.
        void call(Map<String, Object> frame);

        /// The connection is up: anything sent while it was down is waiting
        /// in the queue now.
        void connected();
    }

    /// Admin screens listen for this: a report came in or a flag changed.
    public interface AdminListener {
        void changed();
    }

    private static Handler handler;
    private static AdminListener admin;
    private static String device;
    private static final int PING_MILLIS = 25000;
    private static WebSocket socket;
    private static boolean wanted;
    private static boolean connected;
    private static int attempts;
    /// Counts the connections made, so that what a closed or replaced one
    /// still has to say -- its `onClose`, a frame already on its way -- is
    /// recognised as stale and dropped.
    private static int generation;
    private static Timer pinger;

    private LiveChannel() {
    }

    /// Connects as `deviceId`, and keeps reconnecting until [#stop].
    public static void start(String deviceId, Handler to) {
        handler = to;
        device = deviceId;
        if (wanted) {
            return;
        }
        wanted = true;
        attempts = 0;
        connect();
    }

    public static void stop() {
        wanted = false;
        connected = false;
        handler = null;
        admin = null;
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

    /// The admin screen on display, or null.
    public static void listenAdmin(AdminListener current) {
        admin = current;
    }

    /// Connects again at once, dropping any connection there is: a push
    /// says something is waiting, and a socket the phone kept while asleep
    /// may be dead without knowing it.
    public static void reconnect() {
        if (!wanted) {
            return;
        }
        connected = false;
        generation++;
        if (socket != null) {
            socket.close();
            socket = null;
        }
        attempts = 0;
        connect();
    }

    public static boolean connected() {
        return connected;
    }

    private static void connect() {
        if (!wanted || !WebSocket.isSupported()) {
            return;
        }
        final int mine = ++generation;
        Api.account().liveTicket(Net.to(ticket -> {
            if (mine == generation && wanted) {
                open(ticket.ticket(), mine);
            }
        }, (status, message) -> lost(mine)));
    }

    private static void open(String ticket, final int mine) {
        String base = Session.serverUrl();
        // http -> ws and https -> wss: the scheme keeps its "s".
        String url = "ws" + base.substring(4) + "/ws/live?ticket=" + ticket + "&device=" + device;
        // The handlers run on the socket's thread, so each hands its news to
        // the EDT, where the rest of the app lives.
        socket = WebSocket.build(url)
                .onConnect(ws -> CN.callSerially(() -> opened(mine)))
                .onTextMessage((ws, text) -> CN.callSerially(() -> frame(text, mine)))
                .onClose((ws, code, reason) -> CN.callSerially(() -> closed(mine, code)))
                .onError((ws, err) -> CN.callSerially(() -> lost(mine)))
                .connect(10000);
    }

    private static void opened(int mine) {
        if (mine != generation) {
            return;
        }
        connected = true;
        attempts = 0;
        if (pinger != null) {
            pinger.cancel();
        }
        // Something has to cross an idle connection now and then, or a proxy
        // on the way decides it is dead.
        pinger = CN.setInterval(PING_MILLIS, () -> {
            if (mine == generation && socket != null
                    && socket.getReadyState() == WebSocketState.OPEN) {
                socket.send("{\"type\":\"ping\"}");
            }
        });
    }

    /// The server closes with this code when the account's roles or standing
    /// changed: an admin withdrew a mode, or blocked the account.
    private static final int CHANGED = 4403;

    private static void closed(int mine, int code) {
        if (code == CHANGED && mine == generation && wanted) {
            // The account the app has cached is no longer the account the
            // server knows: ask again, and go back home if the answer opens
            // another mode, so a withdrawn mode is not left open and refused
            // at every request. Reconnecting below happens as for any loss.
            Session.refreshUser(Nav::refreshed, (status, message) -> { });
        }
        lost(mine);
    }

    private static void lost(int mine) {
        if (mine != generation || !wanted) {
            return;
        }
        connected = false;
        final int waiting = ++generation;
        if (socket != null) {
            socket.close();
            socket = null;
        }
        // 1, 2, 4 ... 32 seconds, and then every 32.
        int delay = 1000 << Math.min(attempts++, 5);
        // Only if nothing has happened to the channel in the meantime. Stopped
        // and started again during the wait, it has a connection of its own
        // by now, and connecting here as well would take that one's place:
        // its callbacks would go unheard and its socket would be left open.
        CN.setTimeout(delay, () -> CN.callSerially(() -> {
            if (waiting == generation) {
                connect();
            }
        }));
    }

    private static void frame(String text, int mine) {
        if (mine != generation || handler == null) {
            return;
        }
        Map<String, Object> json;
        try {
            json = new JSONParser().parseJSON(new CharArrayReader(text.toCharArray()));
        } catch (IOException malformed) {
            return;
        }
        Object type = json.get("type");
        if ("envelope".equals(type)) {
            handler.envelope(json);
        } else if ("conversation".equals(type)) {
            handler.conversationChanged(text(json, "id"));
        } else if ("call".equals(type)) {
            handler.call(json);
        } else if ("ready".equals(type)) {
            handler.connected();
        } else if ("admin".equals(type) && admin != null) {
            admin.changed();
        }
    }

    // Tested, never cast and caught: a failed cast does not throw on iOS.
    private static String text(Map<String, Object> json, String key) {
        Object value = json.get(key);
        return value instanceof String ? (String) value : "";
    }
}
