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
import com.codename1.orm.repository.PageRequest;
import com.codename1.orm.repository.Param;
import com.codename1.orm.repository.Query;
import com.codename1.orm.repository.Repository;
import com.codenameone.examples.bazaar.domain.Payment;

import java.time.Instant;
import java.util.List;

/// What each order was charged, keyed by the order's id.
@Repository(entity = Payment.class, key = String.class)
public interface PaymentRepository extends CrudRepository<Payment, String> {
    /// A customer's payments, newest first.
    List<Payment> findByUsernameOrderByCreatedAtDesc(String username);

    /// The most recent payments of everyone, newest first.
    @Query("select p from Payment p order by p.createdAt desc")
    List<Payment> newestFirst(PageRequest page);

    /// Every payment made since `since`, for the admin's charts.
    List<Payment> findByCreatedAtGreaterThanEqual(Instant since);

    /// Payments in one status, the oldest first.
    List<Payment> findByStatusOrderByCreatedAtAsc(String status, PageRequest page);

    /// Whether a customer has a payment in this status on this card.
    boolean existsByUsernameAndMethodIdAndStatus(String username, String methodId, String status);

    /// Whether a customer has a payment in this status on any method.
    boolean existsByUsernameAndStatus(String username, String status);

    /// Moves a payment from one status to another; the conditional update is
    /// what lets only one of two concurrent callers act on it.
    ///
    /// @return 1 for the one caller that moved it
    @Modifying
    @Query("update Payment p set p.status = :to where p.orderId = :id and p.status = :from")
    int claim(@Param("id") String orderId, @Param("from") String from, @Param("to") String to);

    /// Claims a payment for a refund of `amount`, as [#claim] does, keeping
    /// what is asked for and when so an interrupted refund can be finished.
    ///
    /// @return 1 for the one caller that moved it
    @Modifying
    @Query("update Payment p set p.status = 'REFUNDING', p.pendingRefundCents = :amount, "
            + "p.refundClaimedAt = :at where p.orderId = :id and p.status = :from")
    int claimRefund(@Param("id") String orderId, @Param("from") String from,
            @Param("amount") long amount, @Param("at") Instant at);

    /// Refunds claimed before `before` and still under way.
    List<Payment> findByStatusAndRefundClaimedAtLessThan(String status, Instant before);

    /// Records how a pending refund ended.
    ///
    /// @return 1 for the one caller that recorded it
    @Modifying
    @Query("update Payment p set p.status = :status, p.refundedAt = :at "
            + "where p.orderId = :id and p.status = 'REFUND_PENDING'")
    int endRefund(@Param("id") String orderId, @Param("status") String status,
            @Param("at") Instant at);
}
