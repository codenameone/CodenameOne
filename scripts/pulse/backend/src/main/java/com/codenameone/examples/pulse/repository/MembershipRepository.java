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
import com.codenameone.examples.pulse.domain.Membership;

import java.time.Instant;
import java.util.List;

@Repository(entity = Membership.class, key = String.class)
public interface MembershipRepository extends CrudRepository<Membership, String> {
    List<Membership> findByAccountOrderByStartedAtDesc(String account);

    List<Membership> findByMemberIdAndStateIn(String memberId, List<String> states);

    Membership findFirstByIdAndAccount(String id, String account);

    List<Membership> findByStateInAndCurrentPeriodEndLessThanEqual(List<String> states,
            Instant due);

    List<Membership> findByStateIn(List<String> states);

    long countByStateIn(List<String> states);

    long countByStateAndUpdatedAtGreaterThanEqual(String state, Instant since);

    long countByStartedAtGreaterThanEqual(Instant since);

    /// Spends one class from a pack. 1 when there was one to spend.
    @Modifying
    @Query("update Membership m set m.creditsLeft = m.creditsLeft - 1 where m.id = :id"
            + " and m.creditsLeft > 0")
    int spendCredit(@Param("id") String id);

    @Modifying
    @Query("update Membership m set m.creditsLeft = m.creditsLeft + 1 where m.id = :id")
    int returnCredit(@Param("id") String id);

    int deleteByAccount(String account);
}
