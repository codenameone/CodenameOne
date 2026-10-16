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

import com.codename1.backend.Crypto;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Scheduled;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.wayline.Ids;
import com.codenameone.examples.wayline.api.TicketDto;
import com.codenameone.examples.wayline.domain.LiveTicket;
import com.codenameone.examples.wayline.repository.LiveTicketRepository;

import java.io.IOException;

/// Tickets for the live channel.
///
/// A WebSocket handshake is one request the app cannot attach its bearer token
/// to on every platform -- a browser's WebSocket takes no headers at all -- and a
/// token in the address would end up in every access log on the way. So the app
/// asks for a ticket over the authenticated API and presents that instead: it
/// is random, it names nothing, it is good for one connection, and it is stale
/// in half a minute.
@Component
public class LiveTickets {
    static final int TICKET_SECONDS = 30;

    private final LiveTicketRepository tickets;

    public LiveTickets(LiveTicketRepository tickets) {
        this.tickets = tickets;
    }

    @Transactional
    public TicketDto issue(String username) throws IOException {
        String ticket = Ids.hex(Crypto.randomBytes(24));
        LiveTicket row = new LiveTicket();
        row.setTicketHash(Ids.sha256(ticket));
        row.setUsername(username);
        row.setExpiresAt(System.currentTimeMillis() + TICKET_SECONDS * 1000L);
        tickets.insert(row);
        return new TicketDto(ticket, TICKET_SECONDS);
    }

    /// The account a ticket was issued to, or null when it is unknown, used or
    /// stale. Redeeming it uses it up: the row is deleted, and only the
    /// connection whose delete removed it is let in, so a ticket presented twice
    /// at the same moment still opens one connection.
    @Transactional
    public String redeem(String ticket) throws IOException {
        if (ticket == null || ticket.length() == 0 || ticket.length() > 128) {
            return null;
        }
        String hash = Ids.sha256(ticket);
        LiveTicket row = tickets.findById(hash);
        if (row == null) {
            return null;
        }
        String username = row.getUsername();
        long expiresAt = row.getExpiresAt();
        // One delete statement: the count it answers is what says this
        // connection, and not another presenting the same ticket, used it.
        int removed = tickets.deleteByTicketHash(hash);
        if (removed != 1 || expiresAt < System.currentTimeMillis()) {
            return null;
        }
        return username;
    }

    /// Clears the tickets nobody came back for.
    @Scheduled(fixedDelay = 60000, initialDelay = 60000)
    @Transactional
    public void sweep() throws IOException {
        tickets.deleteByExpiresAtLessThan(System.currentTimeMillis());
    }

    /// Forgets the tickets of an account that is being closed.
    @Transactional
    public void forget(String username) {
        tickets.deleteByUsername(username);
    }
}
