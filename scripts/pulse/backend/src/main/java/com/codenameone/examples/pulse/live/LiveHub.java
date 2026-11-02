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
package com.codenameone.examples.pulse.live;

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
/// Four frames, each a small JSON object:
///
/// - `{"type":"session","id":...,"booked":...,"waitlisted":...,"state":...}` -- a
///   class filled up, emptied or was cancelled. Sent to everyone connected, so an
///   open timetable shows the places left without being asked.
/// - `{"type":"booking","id":...,"state":...}` -- one of an account's places
///   changed: a waitlist turned into a place, a class was cancelled. Sent to that
///   account, which then reads the booking from the API. The frame carries no
///   more than that on purpose: the API is the one account of a booking, and a
///   client that missed a frame is put right by its next read.
/// - `{"type":"heartrate","sessionId":...,"memberId":...,"bpm":...,"zone":...}` --
///   a member sharing their heart rate during a class. Sent to the class's
///   trainer only.
/// - `{"type":"checkin","locationId":...,"name":...,"admitted":...}` -- someone
///   came through a door. Sent to staff.
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
    private final List<WebSocketSession> staff = new ArrayList<WebSocketSession>();

    synchronized void add(String username, boolean isStaff, WebSocketSession session) {
        List<WebSocketSession> open = byUser.get(username);
        if (open == null) {
            open = new ArrayList<WebSocketSession>();
            byUser.put(username, open);
        }
        open.add(session);
        if (isStaff) {
            staff.add(session);
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
        staff.remove(session);
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
                staff.removeAll(open);
            }
        }
        for (int iter = 0; open != null && iter < open.size(); iter++) {
            open.get(iter).close(4403, "suspended");
        }
    }

    /// Tells everyone connected how full a class is now.
    public void sessionChanged(String id, int booked, int waitlisted, String state) {
        Map<String, Object> frame = new LinkedHashMap<String, Object>();
        frame.put("type", "session");
        frame.put("id", id);
        frame.put("booked", Integer.valueOf(booked));
        frame.put("waitlisted", Integer.valueOf(waitlisted));
        frame.put("state", state);
        List<WebSocketSession> to = new ArrayList<WebSocketSession>();
        synchronized (this) {
            for (List<WebSocketSession> open : byUser.values()) {
                to.addAll(open);
            }
        }
        send(to, Json.write(frame));
    }

    /// Tells an account that one of its bookings changed.
    public void bookingChanged(String account, String id, String state) {
        Map<String, Object> frame = new LinkedHashMap<String, Object>();
        frame.put("type", "booking");
        frame.put("id", id);
        frame.put("state", state);
        List<WebSocketSession> to = new ArrayList<WebSocketSession>();
        synchronized (this) {
            collect(to, account);
        }
        send(to, Json.write(frame));
    }

    /// Hands a member's heart rate to the trainer of the class they are in.
    public void heartRate(String trainer, String sessionId, String memberId, int bpm, int zone) {
        Map<String, Object> frame = new LinkedHashMap<String, Object>();
        frame.put("type", "heartrate");
        frame.put("sessionId", sessionId);
        frame.put("memberId", memberId);
        frame.put("bpm", Integer.valueOf(bpm));
        frame.put("zone", Integer.valueOf(zone));
        List<WebSocketSession> to = new ArrayList<WebSocketSession>();
        synchronized (this) {
            collect(to, trainer);
        }
        send(to, Json.write(frame));
    }

    /// Tells the staff connected that someone came through a door.
    public void checkedIn(String locationId, String name, boolean admitted) {
        List<WebSocketSession> to;
        synchronized (this) {
            if (staff.isEmpty()) {
                return;
            }
            to = new ArrayList<WebSocketSession>(staff);
        }
        Map<String, Object> frame = new LinkedHashMap<String, Object>();
        frame.put("type", "checkin");
        frame.put("locationId", locationId);
        frame.put("name", name);
        frame.put("admitted", Boolean.valueOf(admitted));
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
