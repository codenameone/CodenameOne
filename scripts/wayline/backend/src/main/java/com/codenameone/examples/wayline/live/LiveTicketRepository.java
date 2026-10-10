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

import com.codename1.backend.annotations.Component;
import com.codename1.orm.session.Session;
import com.codenameone.examples.wayline.domain.LiveTicket;

/// Where live-channel tickets are kept. Only the hash of a ticket is stored,
/// and it is the row's key.
@Component
public class LiveTicketRepository {
    private final Session session;

    public LiveTicketRepository(Session session) {
        this.session = session;
    }

    public void add(LiveTicket ticket) {
        session.persist(ticket);
    }

    /// The ticket with this hash, or null.
    public LiveTicket find(String hash) {
        return session.find(LiveTicket.class, hash);
    }

    /// Deletes a ticket and answers how many rows that removed: 1 for the one
    /// caller that used it, 0 for anyone presenting it after. A statement and
    /// not `session.remove`, because that count is the whole point.
    public int delete(String hash) {
        return session.createQuery("delete from LiveTicket t where t.ticketHash = :hash")
                .setParameter("hash", hash).executeUpdate();
    }

    /// Deletes the tickets that went stale before `now`.
    public int deleteExpired(long now) {
        return session.createQuery("delete from LiveTicket t where t.expiresAt < :now")
                .setParameter("now", Long.valueOf(now)).executeUpdate();
    }

    public int deleteOf(String username) {
        return session.createQuery("delete from LiveTicket t where t.username = :username")
                .setParameter("username", username).executeUpdate();
    }
}
