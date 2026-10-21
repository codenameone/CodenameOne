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
package com.codenameone.examples.bastion.repository;

import com.codename1.orm.repository.CrudRepository;
import com.codename1.orm.repository.Modifying;
import com.codename1.orm.repository.Param;
import com.codename1.orm.repository.Query;
import com.codename1.orm.repository.Repository;
import com.codenameone.examples.bastion.domain.Card;

import java.util.List;

/// Cards. Freezing is one conditional update, so it takes effect for the next
/// authorization whatever is in flight.
@Repository(entity = Card.class, key = String.class)
public interface CardRepository extends CrudRepository<Card, String> {
    List<Card> findByOwnerOrderByCreatedAtAsc(String owner);

    Card findFirstByAccountId(String accountId);

    @Modifying
    @Query("update Card c set c.frozen = :frozen where c.id = :id and c.owner = :owner")
    int setFrozen(@Param("id") String id, @Param("owner") String owner,
            @Param("frozen") boolean frozen);

    @Modifying
    @Query("update Card c set c.dailyLimit = :limit where c.id = :id and c.owner = :owner")
    int setLimit(@Param("id") String id, @Param("owner") String owner,
            @Param("limit") long limit);
}
