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
import com.codenameone.examples.bazaar.domain.CardPayment;
import java.util.List;

/// Reads and writes [CardPayment] rows.
@Repository(entity = CardPayment.class, key = String.class)
public interface CardPaymentRepository extends CrudRepository<CardPayment, String> {
    CardPayment findByStoreIdAndClientId(String storeId, String clientId);

    List<CardPayment> findByReaderIdAndStatus(String readerId, String status);

    /// Spends a succeeded payment on an order, once: two sales naming the
    /// same payment cannot both have it.
    ///
    /// @return 1 when this order got it
    @Modifying(clearAutomatically = true)
    @Query("update CardPayment p set p.orderId = :orderId "
            + "where p.id = :id and p.status = :status and p.orderId = :none")
    int claim(@Param("id") String id, @Param("orderId") String orderId,
            @Param("status") String status, @Param("none") String none);
}
