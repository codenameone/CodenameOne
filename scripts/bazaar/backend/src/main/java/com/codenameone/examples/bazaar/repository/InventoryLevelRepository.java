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
import java.time.Instant;
import com.codename1.orm.repository.Repository;
import com.codenameone.examples.bazaar.domain.InventoryLevel;
import java.util.List;

/// Reads and writes [InventoryLevel] rows.
@Repository(entity = InventoryLevel.class, key = String.class)
public interface InventoryLevelRepository extends CrudRepository<InventoryLevel, String> {
    InventoryLevel findByVariantIdAndLocationId(String variantId, String locationId);

    List<InventoryLevel> findByVariantId(String variantId);

    List<InventoryLevel> findByVariantIdIn(List<String> variantIds);

    List<InventoryLevel> findByLocationId(String locationId);

    List<InventoryLevel> findByStoreId(String storeId);

    /// Commits `qty` units if the row is still at `version` and that many are
    /// available. A bulk update: it flushes and clears the session, so an
    /// entity loaded before it is stale afterwards and must be read again.
    ///
    /// @return 1 when reserved, 0 when the row moved on or the units are not there
    @Modifying(clearAutomatically = true)
    @Query("update InventoryLevel l set l.committed = l.committed + :qty, l.version = l.version + 1, "
            + "l.updatedAt = :at where l.id = :id and l.version = :version "
            + "and l.onHand - l.committed >= :qty")
    int reserveIf(@Param("id") String id, @Param("version") long version,
            @Param("qty") int qty, @Param("at") Instant at);

    /// Moves the three counters by these amounts, whatever they hold now.
    @Modifying(clearAutomatically = true)
    @Query("update InventoryLevel l set l.onHand = l.onHand + :onHand, "
            + "l.committed = l.committed + :committed, l.incoming = l.incoming + :incoming, "
            + "l.version = l.version + 1, l.updatedAt = :at where l.id = :id")
    int apply(@Param("id") String id, @Param("onHand") int onHand,
            @Param("committed") int committed, @Param("incoming") int incoming,
            @Param("at") Instant at);

    /// Sets on hand to a counted figure if the row is still at `version`.
    @Modifying(clearAutomatically = true)
    @Query("update InventoryLevel l set l.onHand = :onHand, l.version = l.version + 1, "
            + "l.updatedAt = :at where l.id = :id and l.version = :version")
    int setOnHandIf(@Param("id") String id, @Param("version") long version,
            @Param("onHand") int onHand, @Param("at") Instant at);

    @Modifying(clearAutomatically = true)
    @Query("update InventoryLevel l set l.reorderPoint = :point where l.id = :id")
    int setReorderPoint(@Param("id") String id, @Param("point") int point);
}
