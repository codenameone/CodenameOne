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
import com.codenameone.examples.bazaar.domain.Shift;
import java.util.List;

/// Reads and writes [Shift] rows.
@Repository(entity = Shift.class, key = String.class)
public interface ShiftRepository extends CrudRepository<Shift, String> {
    List<Shift> findByRegisterIdOrderByOpenedAtDesc(String registerId);

    List<Shift> findByStoreIdOrderByOpenedAtDesc(String storeId);

    /// Adds to a shift's running totals in one statement, so a sale and a
    /// cash move landing at once both count. `drawer` is what the cash
    /// expected in the drawer moves by.
    @Modifying(clearAutomatically = true)
    @Query("update Shift s set s.cashSalesCents = s.cashSalesCents + :cash, "
            + "s.cardSalesCents = s.cardSalesCents + :card, "
            + "s.otherSalesCents = s.otherSalesCents + :other, "
            + "s.cashMovesCents = s.cashMovesCents + :moves, "
            + "s.expectedCents = s.expectedCents + :drawer, "
            + "s.salesCount = s.salesCount + :sales where s.id = :id")
    int addTotals(@Param("id") String id, @Param("cash") long cash, @Param("card") long card,
            @Param("other") long other, @Param("moves") long moves, @Param("drawer") long drawer,
            @Param("sales") int sales);
}
