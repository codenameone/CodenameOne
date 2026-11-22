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

import com.codename1.orm.repository.Repository;
import com.codename1.orm.repository.CrudRepository;
import com.codenameone.examples.bazaar.domain.ParkedCart;
import com.codename1.orm.repository.Modifying;
import com.codename1.orm.repository.Param;
import com.codename1.orm.repository.Query;
import java.util.List;

/// Reads and writes [ParkedCart] rows.
@Repository(entity = ParkedCart.class, key = String.class)
public interface ParkedCartRepository extends CrudRepository<ParkedCart, String> {
    List<ParkedCart> findByRegisterIdAndStatusOrderByCreatedAtAsc(String registerId, String status);

    ParkedCart findByRegisterIdAndClientId(String registerId, String clientId);

    /// Moves a parked cart from `from` to `to` only while it is still at `from`.
    @Modifying
    @Query("update ParkedCart p set p.status = :to, p.resumedAt = :at where p.id = :id and p.status = :from")
    int move(@Param("id") String id, @Param("from") String from, @Param("to") String to,
            @Param("at") java.time.Instant at);

    /// Moves every row of one customer to another (a merge).
    @com.codename1.orm.repository.Modifying(clearAutomatically = true)
    @com.codename1.orm.repository.Query("update ParkedCart x set x.customerId = :to where x.customerId = :from")
    int moveCustomer(@com.codename1.orm.repository.Param("from") String from,
            @com.codename1.orm.repository.Param("to") String to);
}
