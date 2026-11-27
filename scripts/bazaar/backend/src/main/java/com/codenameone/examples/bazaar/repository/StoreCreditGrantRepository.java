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
package com.codenameone.examples.bazaar.repository;

import com.codename1.orm.repository.CrudRepository;
import com.codename1.orm.repository.Modifying;
import com.codename1.orm.repository.Param;
import com.codename1.orm.repository.Query;
import com.codename1.orm.repository.Repository;
import com.codenameone.examples.bazaar.domain.StoreCreditGrant;
import java.time.Instant;
import java.util.List;

/// Reads and writes [StoreCreditGrant] rows.
@Repository(entity = StoreCreditGrant.class, key = String.class)
public interface StoreCreditGrantRepository extends CrudRepository<StoreCreditGrant, String> {

    List<StoreCreditGrant> findByCustomerId(String customerId);

    /// Grants with credit left that lapsed by `now` (and lapse at all).
    @Query("select g from StoreCreditGrant g where g.remainingCents > 0 "
            + "and g.expiresAt > :never and g.expiresAt <= :now")
    List<StoreCreditGrant> lapsed(@Param("never") Instant never, @Param("now") Instant now);

    /// Takes `amount` of a grant if that much is left.
    ///
    /// @return 1 when taken
    @Modifying(clearAutomatically = true)
    @Query("update StoreCreditGrant g set g.remainingCents = g.remainingCents - :amount "
            + "where g.id = :id and g.remainingCents >= :amount")
    int takeIf(@Param("id") String id, @Param("amount") long amount);

    /// Puts `amount` back on a grant.
    @Modifying(clearAutomatically = true)
    @Query("update StoreCreditGrant g set g.remainingCents = g.remainingCents + :amount "
            + "where g.id = :id")
    int giveBack(@Param("id") String id, @Param("amount") long amount);

    /// Moves every grant of one customer to another (a merge).
    @Modifying(clearAutomatically = true)
    @Query("update StoreCreditGrant g set g.customerId = :to where g.customerId = :from")
    int moveAll(@Param("from") String from, @Param("to") String to);
}
