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
package com.codenameone.examples.slate.repository;

import com.codename1.orm.repository.CrudRepository;
import com.codename1.orm.repository.Modifying;
import com.codename1.orm.repository.Param;
import com.codename1.orm.repository.Query;
import com.codename1.orm.repository.Repository;
import com.codenameone.examples.slate.domain.BusyBlock;

import java.time.Instant;
import java.util.List;

@Repository(entity = BusyBlock.class, key = String.class)
public interface BusyBlockRepository extends CrudRepository<BusyBlock, String> {
    @Query("select b from BusyBlock b where b.username in :users and b.startsAt < :to"
            + " and b.endsAt > :from")
    List<BusyBlock> overlapping(@Param("users") List<String> usernames,
            @Param("from") Instant from, @Param("to") Instant to);

    int deleteByLinkId(String linkId);

    /// What a link said about one stretch, which a new reading of the same
    /// stretch replaces. Busy time that only starts inside the stretch is
    /// replaced; time that began before it was read with an earlier stretch.
    @Modifying
    @Query("delete from BusyBlock b where b.linkId = :link and b.startsAt >= :from"
            + " and b.startsAt < :to")
    int clear(@Param("link") String linkId, @Param("from") Instant from, @Param("to") Instant to);

    int deleteByUsername(String username);
}
