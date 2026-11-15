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
package com.codenameone.examples.brickyard.play;

import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Scheduled;
import com.codenameone.examples.brickyard.Ids;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/// Single-use tickets for the game socket. A browser cannot put a bearer
/// token on a WebSocket, so the authenticated join answers a short-lived
/// ticket the socket presents in its query instead.
///
/// They live in memory: a ticket is only useful to the instance that runs
/// the room it seats, so one instance is the whole story for this sample.
@Component
public class PlayTickets {
    public static final int TICKET_SECONDS = 30;

    /// Who a ticket seats, and where.
    public static final class Seat {
        public final String username;
        public final String serverId;
        final long expires;

        Seat(String username, String serverId, long expires) {
            this.username = username;
            this.serverId = serverId;
            this.expires = expires;
        }
    }

    private final Map<String, Seat> tickets = new ConcurrentHashMap<String, Seat>();

    public String issue(String username, String serverId) throws IOException {
        String ticket = Ids.next() + Ids.next();
        tickets.put(ticket, new Seat(username, serverId, System.currentTimeMillis() + TICKET_SECONDS * 1000L));
        return ticket;
    }

    /// The seat `ticket` grants, once; null when unknown or expired.
    public Seat redeem(String ticket) {
        if (ticket == null || ticket.length() == 0 || ticket.length() > 128) {
            return null;
        }
        Seat s = tickets.remove(ticket);
        return s == null || s.expires < System.currentTimeMillis() ? null : s;
    }

    @Scheduled(fixedDelay = 60000, initialDelay = 60000)
    public void sweep() {
        long now = System.currentTimeMillis();
        for (Map.Entry<String, Seat> e : tickets.entrySet()) {
            if (e.getValue().expires < now) {
                tickets.remove(e.getKey());
            }
        }
    }
}
