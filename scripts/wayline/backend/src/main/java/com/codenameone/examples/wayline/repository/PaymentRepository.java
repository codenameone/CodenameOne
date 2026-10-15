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
