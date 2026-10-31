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
package com.codenameone.examples.pulse.repository;

import com.codename1.orm.repository.CrudRepository;
import com.codename1.orm.repository.Modifying;
import com.codename1.orm.repository.Param;
import com.codename1.orm.repository.Query;
import com.codename1.orm.repository.Repository;
import com.codenameone.examples.pulse.domain.Invoice;

import java.time.Instant;
import java.util.List;

@Repository(entity = Invoice.class, key = String.class)
public interface InvoiceRepository extends CrudRepository<Invoice, String> {
    List<Invoice> findByAccountOrderByCreatedAtDesc(String account);
    List<Invoice> findByMemberIdOrderByCreatedAtDesc(String memberId);

    Invoice findFirstByIdAndAccount(String id, String account);

    @Query("select i from Invoice i order by i.createdAt desc")
    List<Invoice> findAllByOrderByCreatedAtDesc();

    List<Invoice> findByStatusOrderByCreatedAtDesc(String status);

    List<Invoice> findByStatusInAndNextAttemptAtLessThanEqual(List<String> statuses, Instant due);

    List<Invoice> findByStatusAndPaidAtGreaterThanEqual(String status, Instant since);

    long countByAccountAndStatus(String account, String status);

    boolean existsByMembershipIdAndPeriodStart(String membershipId, Instant periodStart);

    /// Whether an account has a charge that may be under way at the processor:
    /// one claimed for charging (CHARGING) and not yet settled.
    boolean existsByAccountAndStatus(String account, String status);

    boolean existsByAccountAndMethodIdAndStatus(String account, String methodId, String status);

    /// Claims an invoice for one charge attempt. 1 when this caller has it; 0
    /// when another got there first or it is no longer owed. What stops two
    /// renewal runs, or a run and a member paying by hand, from both charging.
    @Modifying
    @Query("update Invoice i set i.status = 'CHARGING', i.attempts = i.attempts + 1"
            + " where i.id = :id and (i.status = 'OPEN' or i.status = 'FAILED')")
    int claim(@Param("id") String id);

    @Query("select sum(i.amountCents - i.creditCents) from Invoice i where i.account = :account"
            + " and i.status = 'PAID'")
    Long spent(@Param("account") String account);
}
