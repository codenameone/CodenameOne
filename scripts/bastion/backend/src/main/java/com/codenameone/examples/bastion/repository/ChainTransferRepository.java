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
import com.codename1.orm.repository.Modifying;
import com.codename1.orm.repository.Param;
import com.codename1.orm.repository.Query;
import com.codename1.orm.repository.Repository;
import com.codenameone.examples.bastion.domain.ChainTransfer;

import java.util.List;

@Repository(entity = ChainTransfer.class, key = String.class)
public interface ChainTransferRepository extends CrudRepository<ChainTransfer, String> {
    List<ChainTransfer> findByStatusOrderByCreatedAtAsc(String status);

    List<ChainTransfer> findByOwnerOrderByCreatedAtDesc(String owner);

    List<ChainTransfer> findByDirectionAndTxHash(String direction, String txHash);

    /// Records a signature only over the state it was made from. Two workers
    /// that both signed one QUEUED row get 1 and 0: the second signature is
    /// dropped before it is ever broadcast, so one withdrawal can never
    /// become two transactions.
    @Modifying
    @Query("update ChainTransfer t set t.status = :next, t.nonce = :nonce, t.raw = :raw, "
            + "t.txHash = :txHash, t.amount = :amount, t.updatedAt = :now "
            + "where t.id = :id and t.status = :expected")
    int signed(@Param("id") String id, @Param("expected") String expected,
            @Param("next") String next, @Param("nonce") long nonce, @Param("raw") String raw,
            @Param("txHash") String txHash, @Param("amount") long amount, @Param("now") Instant now);

    /// Moves a row from `expected` to `next`, or answers 0 if another worker
    /// moved it first.
    @Modifying
    @Query("update ChainTransfer t set t.status = :next, t.updatedAt = :now "
            + "where t.id = :id and t.status = :expected")
    int advance(@Param("id") String id, @Param("expected") String expected,
            @Param("next") String next, @Param("now") Instant now);
}
