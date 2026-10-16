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
package com.codenameone.examples.wayline.repository;

import com.codename1.orm.repository.CrudRepository;
import com.codename1.orm.repository.Modifying;
import com.codename1.orm.repository.Param;
import com.codename1.orm.repository.Query;
import com.codename1.orm.repository.Repository;
import com.codenameone.examples.wayline.domain.Pricing;

/// The admin's prices: one row, whose key is [#ONLY]. A migration creates it,
/// with an `updatedAt` of 0 while the prices are still the settings'.
@Repository(entity = Pricing.class, key = Integer.class)
public interface PricingRepository extends CrudRepository<Pricing, Integer> {
    /// The key of the one row there is.
    Integer ONLY = Integer.valueOf(1);

    /// Writes new prices over the one row, in one statement that reads
    /// nothing first: two admins saving together each write theirs, and the
    /// later one is what stays.
    ///
    /// @return 1, or 0 when the row is missing
    @Modifying
    @Query("update Pricing p set p.baseCents = :base, p.perKmCents = :perKm, "
            + "p.perMinuteCents = :perMinute, p.minimumCents = :minimum, "
            + "p.feePercent = :fee, p.commissionPercent = :commission, p.surge = :surge, "
            + "p.comfort = :comfort, p.xl = :xl, p.updatedAt = :at, p.updatedBy = :by "
            + "where p.id = :id")
    int change(@Param("id") int id, @Param("base") long base, @Param("perKm") long perKm,
            @Param("perMinute") long perMinute, @Param("minimum") long minimum,
            @Param("fee") double fee, @Param("commission") double commission,
            @Param("surge") double surge, @Param("comfort") double comfort,
            @Param("xl") double xl, @Param("at") long at, @Param("by") String by);
}
