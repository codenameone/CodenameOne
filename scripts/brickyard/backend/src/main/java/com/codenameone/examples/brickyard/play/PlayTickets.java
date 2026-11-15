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
import com.codenameone.examples.brickyard.Clock;
import com.codenameone.examples.brickyard.Ids;
import com.codenameone.examples.brickyard.domain.PlayTicket;
import com.codenameone.examples.brickyard.repository.PlayTicketRepository;
import java.io.IOException;

/// Single-use tickets for the game socket. A browser cannot put a bearer
/// token on a WebSocket, so the authenticated join answers a short-lived
/// ticket the socket presents in its query instead.
///
/// They are rows, not memory: the join may be answered by one instance and
/// the socket opened on the instance that runs the room.
@Component
public class PlayTickets {
    public static final int TICKET_SECONDS = 30;

    /// Who a ticket seats, and where.
    public static final class Seat {
        public final String username;
        public final String serverId;

        Seat(String username, String serverId) {
            this.username = username;
            this.serverId = serverId;
        }
    }

    private final PlayTicketRepository tickets;

    public PlayTickets(PlayTicketRepository tickets) {
        this.tickets = tickets;
    }

    public String issue(String username, String serverId) throws IOException {
        PlayTicket t = new PlayTicket();
        t.setId(Ids.next() + Ids.next());
        t.setUsername(username);
        t.setServerId(serverId);
        t.setExpires(Clock.now() + TICKET_SECONDS * 1000L);
        tickets.insert(t);
        return t.getId();
    }

    /// The seat `ticket` grants, once; null when unknown or expired. The
    /// delete is what makes it single use: of two instances redeeming the
    /// same ticket at once, only one deletes the row.
    public Seat redeem(String ticket) {
        if (ticket == null || ticket.length() == 0 || ticket.length() > 64) {
            return null;
        }
        PlayTicket t = tickets.findById(ticket);
        if (t == null || tickets.redeem(ticket) != 1 || t.getExpires() < Clock.now()) {
            return null;
        }
        return new Seat(t.getUsername(), t.getServerId());
    }

    @Scheduled(fixedDelay = 60000, initialDelay = 60000)
    public void sweep() {
        tickets.expire(Clock.now());
    }
}
