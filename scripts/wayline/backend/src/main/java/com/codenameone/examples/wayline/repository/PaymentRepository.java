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
import com.codename1.orm.repository.PageRequest;
import com.codename1.orm.repository.Param;
import com.codename1.orm.repository.Query;
import com.codename1.orm.repository.Repository;
import com.codenameone.examples.wayline.domain.Payment;

import java.util.List;

/// What each ride was charged, keyed by the ride.
@Repository(entity = Payment.class, key = String.class)
public interface PaymentRepository extends CrudRepository<Payment, String> {
    /// The payments of several rides, in no order.
    List<Payment> findByRideIdIn(List<String> rideIds);

    /// Payments whose refund the processor took and has not finished, the
    /// oldest first.
    List<Payment> findByStatusOrderByCreatedAtAsc(String status, PageRequest page);

    /// Records how a pending refund ended.
    ///
    /// @return 1 for the one caller that recorded it
    @Modifying
    @Query("update Payment p set p.status = :status, p.refundedAt = :at "
            + "where p.rideId = :id and p.status = 'REFUND_PENDING'")
    int endRefund(@Param("id") String rideId, @Param("status") String status,
            @Param("at") long at);
}
