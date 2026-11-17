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
/// - `{"type":"job","id":...,"status":...}` -- a job changed. Sent to its
///   technician and the dispatchers, who then read the job from the API. The
///   frame carries no more than that on purpose: the API is the one account of
///   a job, and a client that missed a frame is put right by its next read.
/// - `{"type":"schedule","run":...,"summary":...}` -- a new plan was published.
///   Sent to everyone connected.
/// - `{"type":"tech","id":...,"lat":...,"lng":...}` -- a technician moved.
///   Sent to the dispatchers.
///
/// "Dispatchers" below are the connections opened by an account holding the
/// DISPATCHER role. The connections are those of this process; running more
/// than one instance needs a channel between them (a database LISTEN/NOTIFY, a
/// message broker) feeding the same three methods.
@Component
public class LiveHub {
    private final Map<String, List<WebSocketSession>> byUser =
            new HashMap<String, List<WebSocketSession>>();
    private final List<WebSocketSession> admins = new ArrayList<WebSocketSession>();

    synchronized void add(String username, boolean dispatcher, WebSocketSession session) {
        List<WebSocketSession> open = byUser.get(username);
        if (open == null) {
            open = new ArrayList<WebSocketSession>();
            byUser.put(username, open);
        }
        open.add(session);
        if (dispatcher) {
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

    /// A job changed: its status, its checklist, its evidence. Sent to its
    /// technician and to every dispatcher, who then read the job from the API.
    public void jobChanged(String jobId, String status, String technician) {
        Map<String, Object> frame = new LinkedHashMap<String, Object>();
        frame.put("type", "job");
        frame.put("id", jobId);
        frame.put("status", status);
        List<WebSocketSession> to = new ArrayList<WebSocketSession>();
        synchronized (this) {
            collect(to, technician);
            addAll(to, admins);
        }
        send(to, Json.write(frame));
    }

    /// The scheduler published a new plan. Everybody connected re-reads what
    /// they show: a technician's route may have changed under them.
    public void scheduleChanged(String runId, String summary) {
        Map<String, Object> frame = new LinkedHashMap<String, Object>();
        frame.put("type", "schedule");
        frame.put("run", runId);
        frame.put("summary", summary);
        List<WebSocketSession> to = new ArrayList<WebSocketSession>();
        synchronized (this) {
            for (List<WebSocketSession> open : byUser.values()) {
                addAll(to, open);
            }
        }
        send(to, Json.write(frame));
    }

    /// A technician reported a position. Sent to the dispatchers' map.
    public void technicianMoved(String techId, double lat, double lng) {
        List<WebSocketSession> to;
        synchronized (this) {
            if (admins.isEmpty()) {
                return;
            }
            to = new ArrayList<WebSocketSession>(admins);
        }
        Map<String, Object> frame = new LinkedHashMap<String, Object>();
        frame.put("type", "tech");
        frame.put("id", techId);
        frame.put("lat", Double.valueOf(lat));
        frame.put("lng", Double.valueOf(lng));
        send(to, Json.write(frame));
    }

    private static void addAll(List<WebSocketSession> to, List<WebSocketSession> from) {
        for (int iter = 0; iter < from.size(); iter++) {
            if (!to.contains(from.get(iter))) {
                to.add(from.get(iter));
            }
        }
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
