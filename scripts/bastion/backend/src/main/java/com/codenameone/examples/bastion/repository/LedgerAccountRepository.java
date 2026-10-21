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

import com.codename1.orm.repository.CrudRepository;
import com.codename1.orm.repository.Modifying;
import com.codename1.orm.repository.Param;
import com.codename1.orm.repository.Query;
import com.codename1.orm.repository.Repository;
import com.codenameone.examples.bastion.domain.LedgerAccount;

import java.util.List;

/// Balances. The two updates are the only way a balance changes, and both are
/// a single conditional statement: the database checks and applies them
/// atomically, whatever else is running, so no read-modify-write window exists
/// for two transfers to race through.
@Repository(entity = LedgerAccount.class, key = String.class)
public interface LedgerAccountRepository extends CrudRepository<LedgerAccount, String> {
    List<LedgerAccount> findByOwnerOrderByCreatedAtAsc(String owner);

    LedgerAccount findFirstByOwnerAndAssetAndKind(String owner, String asset, String kind);

    /// Adds `delta` (which may be negative) unless that would take the balance
    /// below zero or the account is frozen. Answers 1 when applied, 0 when
    /// refused.
    @Modifying
    @Query("update LedgerAccount a set a.balance = a.balance + :delta where a.id = :id "
            + "and a.frozen = FALSE and a.balance + :delta >= 0")
    int applyGuarded(@Param("id") String id, @Param("delta") long delta);

    /// The bank's own accounts: the other side of money arriving from and
    /// leaving for the outside world, which is why they may go negative. A
    /// negative SYS_CASH balance reads "this much has come in".
    @Modifying
    @Query("update LedgerAccount a set a.balance = a.balance + :delta where a.id = :id")
    int applyUnguarded(@Param("id") String id, @Param("delta") long delta);

    @Modifying
    @Query("update LedgerAccount a set a.frozen = :frozen where a.id = :id")
    int setFrozen(@Param("id") String id, @Param("frozen") boolean frozen);
}
