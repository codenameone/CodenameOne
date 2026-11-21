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
import com.codenameone.examples.bazaar.domain.StockAlert;

import java.util.List;

/// Reads and writes [StockAlert] rows.
@Repository(entity = StockAlert.class, key = String.class)
public interface StockAlertRepository extends CrudRepository<StockAlert, String> {
    List<StockAlert> findByStoreIdOrderByCreatedAtDesc(String storeId);

    List<StockAlert> findByVariantIdAndStatus(String variantId, String status);

    List<StockAlert> findByStatus(String status);

    List<StockAlert> findByVariantIdAndEmailAndChannel(String variantId, String email, String channel);

    @Modifying
    @Query("update StockAlert a set a.status = :to where a.id = :id and a.status = :from")
    int move(@Param("id") String id, @Param("from") String from, @Param("to") String to);

    @Modifying
    @Query("update StockAlert a set a.status = 'READY' where a.variantId = :variantId "
            + "and a.status = 'WAITING'")
    int ready(@Param("variantId") String variantId);
}
