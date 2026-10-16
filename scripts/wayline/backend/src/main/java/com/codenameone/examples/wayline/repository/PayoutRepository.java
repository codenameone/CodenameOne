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
import com.codename1.orm.repository.Param;
import com.codename1.orm.repository.Query;
import com.codename1.orm.repository.Repository;
import com.codenameone.examples.wayline.domain.Payout;

import java.util.List;

/// The payouts made to drivers.
@Repository(entity = Payout.class, key = String.class)
public interface PayoutRepository extends CrudRepository<Payout, String> {
    /// The last hundred payouts made to a driver, newest first.
    List<Payout> findTop100ByDriverOrderByCreatedAtDescIdAsc(String driver);

    /// Everything ever paid out to a driver. A sum over no rows is nothing at
    /// all to a database, hence the zero to fall back on.
    @Query("select coalesce(sum(p.amountCents), 0) from Payout p where p.driver = :driver")
    Long paidOut(@Param("driver") String driver);
}
