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
import com.codenameone.examples.bazaar.domain.SaleEvent;

import java.time.Instant;
import java.util.List;

/// Reads and writes [SaleEvent] rows.
@Repository(entity = SaleEvent.class, key = String.class)
public interface SaleEventRepository extends CrudRepository<SaleEvent, String> {
    List<SaleEvent> findByStoreIdOrderByStartsAtAsc(String storeId);

    List<SaleEvent> findByStatusAndStartsAtLessThanEqual(String status, Instant now);

    List<SaleEvent> findByStatusAndEndsAtLessThanEqual(String status, Instant now);

    List<SaleEvent> findByStoreIdAndStatus(String storeId, String status);

    List<SaleEvent> findByStatus(String status);

    @Modifying
    @Query("update SaleEvent s set s.status = :to where s.id = :id and s.status = :from")
    int move(@Param("id") String id, @Param("from") String from, @Param("to") String to);

    /// Counts `qty` more sold if the cap allows it (a cap of 0 is none).
    @Modifying
    @Query("update SaleEvent s set s.sold = s.sold + :qty where s.id = :id and s.status = 'RUNNING' "
            + "and (s.stockCap = 0 or s.sold + :qty <= s.stockCap)")
    int sellIf(@Param("id") String id, @Param("qty") int qty);

    @Modifying
    @Query("update SaleEvent s set s.sold = s.sold - :qty where s.id = :id and s.sold >= :qty")
    int unsell(@Param("id") String id, @Param("qty") int qty);
}
