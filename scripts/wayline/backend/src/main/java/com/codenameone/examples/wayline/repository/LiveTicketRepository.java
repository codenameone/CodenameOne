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
package com.codenameone.examples.wayline.repository;

import com.codename1.orm.repository.CrudRepository;
import com.codename1.orm.repository.Repository;
import com.codenameone.examples.wayline.domain.LiveTicket;

/// The tickets that open a live connection. Only the hash of a ticket is
/// stored, and it is the row's key.
@Repository(entity = LiveTicket.class, key = String.class)
public interface LiveTicketRepository extends CrudRepository<LiveTicket, String> {
    /// Deletes a ticket and answers how many rows that removed: 1 for the one
    /// caller that used it, 0 for anyone presenting it after. A statement and
    /// not a read followed by a removal, because that count is the whole
    /// point.
    int deleteByTicketHash(String ticketHash);

    /// Deletes the tickets that went stale before `now`.
    int deleteByExpiresAtLessThan(long now);

    int deleteByUsername(String username);
}
