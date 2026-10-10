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

import com.codename1.backend.Crypto;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Scheduled;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.wayline.Ids;
import com.codenameone.examples.wayline.api.TicketDto;
import com.codenameone.examples.wayline.domain.LiveTicket;

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
        row.ticketHash = Ids.sha256(ticket);
        row.username = username;
        row.expiresAt = System.currentTimeMillis() + TICKET_SECONDS * 1000L;
        tickets.add(row);
        TicketDto dto = new TicketDto();
        dto.ticket = ticket;
        dto.expiresInSeconds = TICKET_SECONDS;
        return dto;
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
        LiveTicket row = tickets.find(hash);
        if (row == null) {
            return null;
        }
        String username = row.username;
        long expiresAt = row.expiresAt;
        // One delete statement: the count it answers is what says this
        // connection, and not another presenting the same ticket, used it.
        int removed = tickets.delete(hash);
        if (removed != 1 || expiresAt < System.currentTimeMillis()) {
            return null;
        }
        return username;
    }

    /// Clears the tickets nobody came back for.
    @Scheduled(fixedDelay = 60000, initialDelay = 60000)
    @Transactional
    public void sweep() throws IOException {
        tickets.deleteExpired(System.currentTimeMillis());
    }

    /// Forgets the tickets of an account that is being closed.
    @Transactional
    public void forget(String username) {
        tickets.deleteOf(username);
    }
}
