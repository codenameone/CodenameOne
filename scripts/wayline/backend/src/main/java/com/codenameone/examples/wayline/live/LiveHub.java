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
package com.codenameone.examples.wayline.live;

import com.codename1.backend.Json;
import com.codename1.backend.WebSocketSession;
import com.codename1.backend.annotations.Component;
import com.codenameone.examples.wayline.api.RideState;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// The open live connections, and what is pushed down them.
///
/// Three frames, each a small JSON object:
///
/// - `{"type":"ride","id":...,"state":...}` -- a ride changed. Sent to its
///   rider, its driver and the admins, who then read the ride from the API.
///   The frame carries no more than that on purpose: the API is the one account
///   of a ride, and a client that missed a frame is put right by its next read.
/// - `{"type":"driver","rideId":...,"lat":...,"lng":...,"heading":...}` -- the
///   driver of a ride moved. Sent to its rider.
/// - `{"type":"fleet","username":...,"online":...,"lat":...}` -- a driver's
///   status or position changed. Sent to the admins.
///
/// The connections are those of this process. Two instances of the server
/// would each know only their own, and a change made on one would not reach a
/// client connected to the other: running more than one needs a channel
/// between them -- a database LISTEN/NOTIFY, a message broker -- feeding the
/// same three methods.
@Component
public class LiveHub {
    private final Map<String, List<WebSocketSession>> byUser =
            new HashMap<String, List<WebSocketSession>>();
    private final List<WebSocketSession> admins = new ArrayList<WebSocketSession>();

    synchronized void add(String username, boolean admin, WebSocketSession session) {
        List<WebSocketSession> open = byUser.get(username);
        if (open == null) {
            open = new ArrayList<WebSocketSession>();
            byUser.put(username, open);
        }
        open.add(session);
        if (admin) {
            admins.add(session);
        }
    }

    synchronized void remove(String username, WebSocketSession session) {
        List<WebSocketSession> open = byUser.get(username);
        if (open != null) {
            open.remove(session);
            if (open.isEmpty()) {
                byUser.remove(username);
            }
        }
        admins.remove(session);
    }

    /// How many connections are open, for the tests.
    public synchronized int connections() {
        int total = 0;
        for (List<WebSocketSession> open : byUser.values()) {
            total += open.size();
        }
        return total;
    }

    /// Closes every connection an account has open.
    public void drop(String username) {
        List<WebSocketSession> open;
        synchronized (this) {
            open = byUser.remove(username);
            if (open != null) {
                admins.removeAll(open);
            }
        }
        for (int iter = 0; open != null && iter < open.size(); iter++) {
            open.get(iter).close(4403, "suspended");
        }
    }

    /// Tells a ride's rider and driver, and the admins, that it changed.
    /// `also` is one more account to tell, or empty: the driver an offer was
    /// taken back from is no longer the ride's driver, and still has to hear.
    public void rideChanged(String id, RideState state, String rider, String driver, String also) {
        Map<String, Object> frame = new LinkedHashMap<String, Object>();
        frame.put("type", "ride");
        frame.put("id", id);
        frame.put("state", state.name());
        String text = Json.write(frame);
        List<WebSocketSession> to = new ArrayList<WebSocketSession>();
        synchronized (this) {
            collect(to, rider);
            collect(to, driver);
            collect(to, also);
            for (int iter = 0; iter < admins.size(); iter++) {
                if (!to.contains(admins.get(iter))) {
                    to.add(admins.get(iter));
                }
            }
        }
        send(to, text);
    }

    public void driverMoved(String rider, String rideId, double lat, double lng, double heading) {
        Map<String, Object> frame = new LinkedHashMap<String, Object>();
        frame.put("type", "driver");
        frame.put("rideId", rideId);
        frame.put("lat", Double.valueOf(lat));
        frame.put("lng", Double.valueOf(lng));
        frame.put("heading", Double.valueOf(heading));
        List<WebSocketSession> to = new ArrayList<WebSocketSession>();
        synchronized (this) {
            collect(to, rider);
        }
        send(to, Json.write(frame));
    }

    public void fleetChanged(String username, boolean online, double lat, double lng,
            double heading) {
        List<WebSocketSession> to;
        synchronized (this) {
            if (admins.isEmpty()) {
                return;
            }
            to = new ArrayList<WebSocketSession>(admins);
        }
        Map<String, Object> frame = new LinkedHashMap<String, Object>();
        frame.put("type", "fleet");
        frame.put("username", username);
        frame.put("online", Boolean.valueOf(online));
        frame.put("lat", Double.valueOf(lat));
        frame.put("lng", Double.valueOf(lng));
        frame.put("heading", Double.valueOf(heading));
        send(to, Json.write(frame));
    }

    /// Must be called holding the lock.
    private void collect(List<WebSocketSession> to, String username) {
        List<WebSocketSession> open = username == null ? null : byUser.get(username);
        for (int iter = 0; open != null && iter < open.size(); iter++) {
            if (!to.contains(open.get(iter))) {
                to.add(open.get(iter));
            }
        }
    }

    /// Sends outside the lock: a slow client holds up its own frame, not
    /// everyone's. One that cannot be written to is dropped; its close callback
    /// takes it out of the maps.
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
