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
package com.codenameone.examples.morsel.repository;

import com.codename1.orm.repository.CrudRepository;
import com.codename1.orm.repository.Modifying;
import com.codename1.orm.repository.PageRequest;
import com.codename1.orm.repository.Param;
import com.codename1.orm.repository.Query;
import com.codename1.orm.repository.Repository;
import com.codenameone.examples.morsel.domain.DeliveryActivity;

import java.util.List;

/// The Live Activities customers follow their deliveries in, by delivery.
@Repository(entity = DeliveryActivity.class, key = String.class)
public interface DeliveryActivityRepository extends CrudRepository<DeliveryActivity, String> {
    int deleteByDeliveryId(String deliveryId);

    int deleteByUsername(String username);

    /// Records that a state went out, unless one went out after `before`:
    /// 1 for the caller who gets to send, 0 for anyone else at the same moment.
    @Modifying
    @Query("update DeliveryActivity a set a.sentAt = :now "
            + "where a.deliveryId = :deliveryId and a.sentAt < :before")
    int claim(@Param("deliveryId") String deliveryId, @Param("now") long now,
            @Param("before") long before);

    /// Records that the delivery ended at `now`, unless that is already recorded.
    @Modifying
    @Query("update DeliveryActivity a set a.endedAt = :now "
            + "where a.deliveryId = :deliveryId and a.endedAt = 0")
    int markEnded(@Param("deliveryId") String deliveryId, @Param("now") long now);

    /// The activities whose delivery ended before `before` and that are still
    /// here, because the phone was not told: oldest first.
    @Query("select a from DeliveryActivity a where a.endedAt > 0 and a.endedAt < :before "
            + "order by a.endedAt")
    List<DeliveryActivity> leftBehind(@Param("before") long before, PageRequest page);

    /// Forgets the activities whose delivery ended before `before`.
    @Modifying
    @Query("delete from DeliveryActivity a where a.endedAt > 0 and a.endedAt < :before")
    int dropEndedBefore(@Param("before") long before);
}
