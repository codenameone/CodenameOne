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
import com.codenameone.examples.bazaar.domain.Subscription;
import java.time.Instant;
import java.util.List;

/// Reads and writes [Subscription] rows.
@Repository(entity = Subscription.class, key = String.class)
public interface SubscriptionRepository extends CrudRepository<Subscription, String> {
    List<Subscription> findByStoreIdAndUsername(String storeId, String username);

    List<Subscription> findByStoreIdOrderByCreatedAtDesc(String storeId);

    List<Subscription> findByStatusAndNextAttemptAtLessThanEqual(String status, Instant now);

    @Query("select s from Subscription s where s.storeId = :storeId and s.status = :status and s.dueAt >= :from and s.dueAt < :to order by s.dueAt asc")
    List<Subscription> due(@Param("storeId") String storeId, @Param("status") String status,
            @Param("from") Instant from, @Param("to") Instant to);

    /// Takes a renewal attempt: moves the attempt time to `lease` only while it is
    /// still `at`, so two job runs cannot both charge one renewal.
    @Modifying
    @Query("update Subscription s set s.nextAttemptAt = :lease where s.id = :id and s.nextAttemptAt = :at and s.status = :status")
    int claim(@Param("id") String id, @Param("at") Instant at, @Param("lease") Instant lease,
            @Param("status") String status);
}
