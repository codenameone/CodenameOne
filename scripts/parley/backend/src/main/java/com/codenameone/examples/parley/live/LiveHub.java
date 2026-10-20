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

import com.codename1.backend.Json;
import com.codename1.backend.WebSocketSession;
import com.codename1.backend.annotations.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// The open live connections, by account and by device, and the frames sent
/// over them.
///
/// A frame is a JSON object with a `type`. The server sends `envelope` (a
/// message for this device), `call` (a call signal), `conversation` (a chat
/// changed: members, title, timer) and `admin` (the dashboard should refresh).
/// The app sends `ping` and `typing`; everything else goes over the API, so
/// a lost connection loses nothing that cannot be fetched again.
///
/// Connections are this server's own: a second instance has its own hub, and
/// a device connected there is reached by push instead. That is the limit a
/// real deployment would lift with a shared broker.
@Component
public class LiveHub {
    /// Who a connection belongs to, kept as the session's attachment.
    public record Peer(String username, String device, boolean admin) {
    }

    private final Map<String, List<WebSocketSession>> byUser =
            new HashMap<String, List<WebSocketSession>>();
    private final Map<String, List<WebSocketSession>> byDevice =
            new HashMap<String, List<WebSocketSession>>();
    private final List<WebSocketSession> admins = new ArrayList<WebSocketSession>();

    synchronized void add(Peer peer, WebSocketSession session) {
        put(byUser, peer.username(), session);
        put(byDevice, peer.device(), session);
        if (peer.admin()) {
            admins.add(session);
        }
    }

    synchronized void remove(Peer peer, WebSocketSession session) {
        take(byUser, peer.username(), session);
        take(byDevice, peer.device(), session);
        admins.remove(session);
    }

    /// How many connections are open, for the dashboard.
    public synchronized int connections() {
        int total = 0;
        for (List<WebSocketSession> open : byUser.values()) {
            total += open.size();
        }
        return total;
    }

    /// Whether any device of the account is connected here.
    public synchronized boolean online(String username) {
        return byUser.containsKey(username);
    }

    /// Whether this device is connected here.
    public synchronized boolean online(String username, String device) {
        List<WebSocketSession> open = byDevice.get(device);
        if (open == null) {
            return false;
        }
        for (int iter = 0; iter < open.size(); iter++) {
            Object who = open.get(iter).getAttachment();
            if (who instanceof Peer && ((Peer) who).username().equals(username)) {
                return true;
            }
        }
        return false;
    }

    /// Closes every connection of an account: it was blocked or closed.
    public void drop(String username) {
        List<WebSocketSession> open;
        synchronized (this) {
            open = byUser.remove(username);
            if (open != null) {
                admins.removeAll(open);
                for (List<WebSocketSession> each : byDevice.values()) {
                    each.removeAll(open);
                }
            }
        }
        for (int iter = 0; open != null && iter < open.size(); iter++) {
            open.get(iter).close(4403, "suspended");
        }
    }

    /// Closes the connections of one device: it was unlinked.
    public void dropDevice(String device) {
        List<WebSocketSession> open;
        synchronized (this) {
            open = byDevice.remove(device);
            if (open != null) {
                admins.removeAll(open);
                for (List<WebSocketSession> each : byUser.values()) {
                    each.removeAll(open);
                }
            }
        }
        for (int iter = 0; open != null && iter < open.size(); iter++) {
            open.get(iter).close(4403, "unlinked");
        }
    }

    /// Sends a frame to one device, answering whether it was connected.
    public boolean toDevice(String device, Map<String, Object> frame) {
        List<WebSocketSession> to;
        synchronized (this) {
            List<WebSocketSession> open = byDevice.get(device);
            if (open == null || open.isEmpty()) {
                return false;
            }
            to = new ArrayList<WebSocketSession>(open);
        }
        send(to, Json.write(frame));
        return true;
    }

    /// Sends a frame to every device of these accounts.
    public void toUsers(List<String> usernames, Map<String, Object> frame) {
        List<WebSocketSession> to = new ArrayList<WebSocketSession>();
        synchronized (this) {
            for (int iter = 0; iter < usernames.size(); iter++) {
                List<WebSocketSession> open = byUser.get(usernames.get(iter));
                for (int s = 0; open != null && s < open.size(); s++) {
                    if (!to.contains(open.get(s))) {
                        to.add(open.get(s));
                    }
                }
            }
        }
        send(to, Json.write(frame));
    }

    /// Tells the admins' dashboards something changed.
    public void toAdmins(Map<String, Object> frame) {
        List<WebSocketSession> to;
        synchronized (this) {
            if (admins.isEmpty()) {
                return;
            }
            to = new ArrayList<WebSocketSession>(admins);
        }
        send(to, Json.write(frame));
    }

    private static void put(Map<String, List<WebSocketSession>> map, String key,
            WebSocketSession session) {
        List<WebSocketSession> open = map.get(key);
        if (open == null) {
            open = new ArrayList<WebSocketSession>();
            map.put(key, open);
        }
        open.add(session);
    }

    private static void take(Map<String, List<WebSocketSession>> map, String key,
            WebSocketSession session) {
        List<WebSocketSession> open = map.get(key);
        if (open != null) {
            open.remove(session);
            if (open.isEmpty()) {
                map.remove(key);
            }
        }
    }

    private static void send(List<WebSocketSession> to, String text) {
        for (int iter = 0; iter < to.size(); iter++) {
            WebSocketSession session = to.get(iter);
            try {
                session.sendText(text);
            } catch (IOException gone) {
                session.abort();
            }
        }
    }
}
