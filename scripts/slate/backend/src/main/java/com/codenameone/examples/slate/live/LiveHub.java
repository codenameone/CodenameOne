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
package com.codenameone.examples.slate.live;

import com.codename1.backend.Json;
import com.codename1.backend.WebSocketSession;
import com.codename1.backend.annotations.Component;

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
/// - `{"type":"slots","service":...,"day":...}` -- a day's starts for a service
///   changed: something was booked, given up or moved. Sent to everyone
///   connected, so an open slot picker drops a start somebody else just took
///   without being asked.
/// - `{"type":"appointment","id":...,"state":...}` -- one of an account's
///   bookings changed. Sent to the client and to every provider on it, who then
///   read the booking from the API. The frame carries no more than that on
///   purpose: the API is the one account of a booking, and a client that missed
///   a frame is put right by its next read.
/// - `{"type":"schedule","username":...}` -- a provider's day changed under
///   them: a calendar sync brought in busy time, an owner moved their hours.
///   Sent to that provider and to the owners.
///
/// The connections are those of this process. Two instances of the server
/// would each know only their own, and a change made on one would not reach a
/// client connected to the other: running more than one needs a channel
/// between them -- a database LISTEN/NOTIFY, a message broker -- feeding the
/// same methods.
@Component
public class LiveHub {
    private final Map<String, List<WebSocketSession>> byUser =
            new HashMap<String, List<WebSocketSession>>();
    private final List<WebSocketSession> owners = new ArrayList<WebSocketSession>();

    synchronized void add(String username, boolean isOwner, WebSocketSession session) {
        List<WebSocketSession> open = byUser.get(username);
        if (open == null) {
            open = new ArrayList<WebSocketSession>();
            byUser.put(username, open);
        }
        open.add(session);
        if (isOwner) {
            owners.add(session);
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
        owners.remove(session);
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
                owners.removeAll(open);
            }
        }
        for (int iter = 0; open != null && iter < open.size(); iter++) {
            open.get(iter).close(4403, "suspended");
        }
    }

    /// Tells everyone connected that a day's starts for a service changed. An
    /// empty service or day means any: a provider's calendar changed, or an
    /// account with bookings on many days was closed.
    public void slotsChanged(String serviceId, String day) {
        Map<String, Object> frame = new LinkedHashMap<String, Object>();
        frame.put("type", "slots");
        frame.put("service", serviceId);
        frame.put("day", day);
        List<WebSocketSession> to = new ArrayList<WebSocketSession>();
        synchronized (this) {
            for (List<WebSocketSession> open : byUser.values()) {
                to.addAll(open);
            }
        }
        send(to, Json.write(frame));
    }

    /// Tells the accounts on a booking that it changed.
    public void appointmentChanged(List<String> accounts, String id, String state) {
        Map<String, Object> frame = new LinkedHashMap<String, Object>();
        frame.put("type", "appointment");
        frame.put("id", id);
        frame.put("state", state);
        List<WebSocketSession> to = new ArrayList<WebSocketSession>();
        synchronized (this) {
            for (int iter = 0; iter < accounts.size(); iter++) {
                collect(to, accounts.get(iter));
            }
        }
        send(to, Json.write(frame));
    }

    /// Tells a provider, and the owners, that the provider's days changed.
    public void scheduleChanged(String username) {
        Map<String, Object> frame = new LinkedHashMap<String, Object>();
        frame.put("type", "schedule");
        frame.put("username", username);
        List<WebSocketSession> to = new ArrayList<WebSocketSession>();
        synchronized (this) {
            collect(to, username);
            for (int iter = 0; iter < owners.size(); iter++) {
                if (!to.contains(owners.get(iter))) {
                    to.add(owners.get(iter));
                }
            }
        }
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
