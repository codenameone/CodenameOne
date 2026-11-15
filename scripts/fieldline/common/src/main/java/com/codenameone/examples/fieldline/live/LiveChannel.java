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
package com.codenameone.examples.fieldline.live;

import com.codename1.io.CharArrayReader;
import com.codename1.io.JSONParser;
import com.codename1.io.WebSocket;
import com.codename1.io.WebSocketState;
import com.codename1.ui.CN;
import com.codenameone.examples.fieldline.net.Api;
import com.codename1.appkit.Net;
import com.codenameone.examples.fieldline.net.Session;
import com.codenameone.examples.fieldline.ui.Nav;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;

/// The WebSocket the server pushes changes down: a job moved on, a new plan
/// was published, a technician moved.
///
/// It only ever says *that* something changed. What the job or the plan now is, the app
/// asks for over the REST API, which stays the one source of truth -- so a
/// frame that is lost, or a socket that is down, costs a few seconds and
/// nothing else. Every screen that listens here also polls, slowly while the
/// socket is up and faster while it is not ([#connected] says which).
///
/// A WebSocket handshake cannot carry the bearer token on every platform, so
/// the connection is let in by a ticket: the app asks for one over the
/// authenticated API, and the server accepts it once, within half a minute.
public final class LiveChannel {
    /// What a screen wants to hear about. All of it arrives on the EDT.
    public interface Listener {
        /// A job changed: its status, or (`PHOTO`) its evidence.
        void jobChanged(String id, String status);

        /// The scheduler published a new plan.
        void scheduleChanged(String runId, String summary);

        /// A technician reported where they are. Dispatchers only.
        void technicianMoved(String techId, double lat, double lng);
    }

    /// A listener that ignores everything, to override the part a screen wants.
    public static class Adapter implements Listener {
        @Override
        public void jobChanged(String id, String status) {
        }

        @Override
        public void scheduleChanged(String runId, String summary) {
        }

        @Override
        public void technicianMoved(String techId, double lat, double lng) {
        }
    }

    private static final int PING_MILLIS = 25000;

    private static WebSocket socket;
    /// Every screen that is listening, by its owner: the home screen and, on
    /// top of it, an open job. A LinkedHashMap so they hear news in the order
    /// they asked.
    private static final Map<Object, Listener> LISTENERS = new LinkedHashMap<Object, Listener>();
    private static final Object HOME = new Object();
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

    /// Connects, and keeps reconnecting until [#stop].
    public static void start() {
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
        LISTENERS.clear();
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

    /// The one screen that is listening: the one on display.
    /// The home screen's listener; a later home screen replaces it.
    public static void listen(Listener current) {
        listen(HOME, current);
    }

    /// A listener of `owner`'s, beside the home screen's. A second call for
    /// the same owner replaces the first.
    public static void listen(Object owner, Listener current) {
        if (current == null) {
            LISTENERS.remove(owner);
        } else {
            LISTENERS.put(owner, current);
        }
    }

    public static void unlisten(Object owner) {
        LISTENERS.remove(owner);
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
        String url = "ws" + base.substring(4) + "/ws/live?ticket=" + ticket;
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
        if (mine != generation || LISTENERS.isEmpty()) {
            return;
        }
        Map<String, Object> json;
        try {
            json = new JSONParser().parseJSON(new CharArrayReader(text.toCharArray()));
        } catch (IOException malformed) {
            return;
        }
        Object type = json.get("type");
        // A copy: a listener may stop listening, or open a screen that starts.
        List<Listener> all = new ArrayList<Listener>(LISTENERS.values());
        for (Listener listener : all) {
            if ("job".equals(type)) {
                listener.jobChanged(text(json, "id"), text(json, "status"));
            } else if ("schedule".equals(type)) {
                listener.scheduleChanged(text(json, "run"), text(json, "summary"));
            } else if ("tech".equals(type)) {
                listener.technicianMoved(text(json, "id"), number(json, "lat"),
                        number(json, "lng"));
            }
        }
    }

    // Tested, never cast and caught: a failed cast does not throw on iOS.
    private static String text(Map<String, Object> json, String key) {
        Object value = json.get(key);
        return value instanceof String ? (String) value : "";
    }

    private static double number(Map<String, Object> json, String key) {
        Object value = json.get(key);
        return value instanceof Number ? ((Number) value).doubleValue() : 0;
    }
}
