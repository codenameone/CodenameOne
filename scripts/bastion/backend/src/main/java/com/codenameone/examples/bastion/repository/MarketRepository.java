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
import com.codenameone.examples.bastion.domain.Market;

import java.util.List;

@Repository(entity = Market.class, key = String.class)
public interface MarketRepository extends CrudRepository<Market, String> {
    List<Market> findByBaseAndQuoteOrderByIdAsc(String base, String quote);

    /// Writes the row without changing it, holding it until the transaction
    /// ends: the market's lock, as audit_head is the audit chain's.
    @Modifying
    @Query("update Market m set m.seq = m.seq where m.id = :id")
    int hold(@Param("id") String id);

    @Modifying
    @Query("update Market m set m.seq = m.seq + 1 where m.id = :id")
    int advance(@Param("id") String id);

    @Modifying
    @Query("update Market m set m.lastPrice = :price where m.id = :id")
    int traded(@Param("id") String id, @Param("price") long price);

    @Modifying
    @Query("update Market m set m.indexPrice = :price where m.id = :id")
    int index(@Param("id") String id, @Param("price") long price);
}
