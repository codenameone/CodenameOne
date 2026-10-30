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
package com.codenameone.examples.bastion.repository;

import java.time.Instant;
import com.codename1.orm.repository.CrudRepository;
import com.codename1.orm.repository.Param;
import com.codename1.orm.repository.Query;
import com.codename1.orm.repository.Repository;
import com.codenameone.examples.bastion.domain.Transfer;


/// Movements as the limits count them. Only ever inserted.
@Repository(entity = Transfer.class, key = String.class)
public interface TransferRepository extends CrudRepository<Transfer, String> {
    /// What `owner` has sent out of the bank or to someone else in `asset`
    /// since `since`; null when nothing.
    @Query("select sum(t.amount) from Transfer t where t.owner = :owner and t.asset = :asset "
            + "and t.outgoing = TRUE and t.createdAt >= :since")
    Long sentSince(@Param("owner") String owner, @Param("asset") String asset,
            @Param("since") Instant since);

    /// What has gone through a payee or a card since `since`; null when nothing.
    @Query("select sum(t.amount) from Transfer t where t.via = :via and t.createdAt >= :since")
    Long throughSince(@Param("via") String via, @Param("since") Instant since);

    /// What has gone through a payee in `asset` since `since`; null when nothing.
    @Query("select sum(t.amount) from Transfer t where t.via = :via and t.asset = :asset "
            + "and t.createdAt >= :since")
    Long throughSinceIn(@Param("via") String via, @Param("asset") String asset,
            @Param("since") Instant since);
}
