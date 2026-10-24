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
package com.codenameone.examples.parley.repository;

import com.codename1.orm.repository.CrudRepository;
import com.codename1.orm.repository.Repository;
import com.codenameone.examples.parley.domain.LiveTicket;

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
