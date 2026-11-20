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
import com.codename1.orm.repository.Query;
import com.codename1.orm.repository.Param;
import com.codename1.orm.repository.Modifying;
import com.codename1.orm.repository.Repository;
import com.codenameone.examples.bazaar.domain.Discount;
import java.util.List;

/// Reads and writes [Discount] rows.
@Repository(entity = Discount.class, key = String.class)
public interface DiscountRepository extends CrudRepository<Discount, String> {
    List<Discount> findByStoreIdOrderByCreatedAtDesc(String storeId);

    List<Discount> findByStoreIdAndCode(String storeId, String code);

    List<Discount> findByStoreIdAndAutomatic(String storeId, boolean automatic);

    /// Counts one use of a discount if it has uses left (a `usageLimit` of 0
    /// is no limit). The conditional update is what keeps two checkouts from
    /// both taking the last use.
    ///
    /// @return 1 when the use was counted
    @Modifying
    @Query("update Discount d set d.usedCount = d.usedCount + 1 where d.id = :id "
            + "and (d.usageLimit = 0 or d.usedCount < d.usageLimit)")
    int useIf(@Param("id") String id);

    /// Gives back a use [#useIf] counted, for an order that did not go through.
    @Modifying
    @Query("update Discount d set d.usedCount = d.usedCount - 1 where d.id = :id and d.usedCount > 0")
    int unuse(@Param("id") String id);
}
