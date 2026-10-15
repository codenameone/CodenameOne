/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
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
