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
package com.codenameone.examples.wayline.live;

import com.codename1.io.CharArrayReader;
import com.codename1.io.JSONParser;
import com.codename1.io.WebSocket;
import com.codename1.io.WebSocketState;
import com.codename1.ui.CN;
import com.codenameone.examples.wayline.net.Api;
import com.codenameone.examples.wayline.net.Net;
import com.codenameone.examples.wayline.net.Session;

import java.io.IOException;
import java.util.Map;
import java.util.Timer;

/// The WebSocket the server pushes changes down: a ride changed state, the
/// driver moved, a car went on or offline.
///
/// It only ever says *that* something changed. What the ride now is, the app
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
        void rideChanged(String id, String state);

        void driverMoved(String rideId, double lat, double lng, double heading);

        void fleetChanged(String username, boolean online, double lat, double lng,
                double heading);
    }

    /// A listener that ignores everything, to override the part a screen wants.
    public static class Adapter implements Listener {
        @Override
        public void rideChanged(String id, String state) {
        }

        @Override
        public void driverMoved(String rideId, double lat, double lng, double heading) {
        }

        @Override
        public void fleetChanged(String username, boolean online, double lat, double lng,
                double heading) {
        }
    }

    private static final int PING_MILLIS = 25000;

    private static WebSocket socket;
    private static Listener listener;
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
        listener = null;
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
    public static void listen(Listener current) {
        listener = current;
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
                open(ticket.ticket, mine);
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
                .onClose((ws, code, reason) -> CN.callSerially(() -> lost(mine)))
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

    private static void lost(int mine) {
        if (mine != generation || !wanted) {
            return;
        }
        connected = false;
        generation++;
        if (socket != null) {
            socket.close();
            socket = null;
        }
        // 1, 2, 4 ... 32 seconds, and then every 32.
        int delay = 1000 << Math.min(attempts++, 5);
        CN.setTimeout(delay, () -> CN.callSerially(LiveChannel::connect));
    }

    private static void frame(String text, int mine) {
        if (mine != generation || listener == null) {
            return;
        }
        Map<String, Object> json;
        try {
            json = new JSONParser().parseJSON(new CharArrayReader(text.toCharArray()));
        } catch (IOException malformed) {
            return;
        }
        Object type = json.get("type");
        if ("ride".equals(type)) {
            listener.rideChanged(text(json, "id"), text(json, "state"));
        } else if ("driver".equals(type)) {
            listener.driverMoved(text(json, "rideId"), number(json, "lat"), number(json, "lng"),
                    number(json, "heading"));
        } else if ("fleet".equals(type)) {
            Object online = json.get("online");
            listener.fleetChanged(text(json, "username"),
                    Boolean.TRUE.equals(online) || "true".equals(online),
                    number(json, "lat"), number(json, "lng"), number(json, "heading"));
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
