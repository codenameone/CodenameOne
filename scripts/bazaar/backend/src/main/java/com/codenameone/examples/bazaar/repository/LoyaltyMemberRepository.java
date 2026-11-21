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
import com.codenameone.examples.bazaar.domain.LoyaltyMember;

import java.time.Instant;
import java.util.List;

/// Reads and writes [LoyaltyMember] rows.
@Repository(entity = LoyaltyMember.class, key = String.class)
public interface LoyaltyMemberRepository extends CrudRepository<LoyaltyMember, String> {
    LoyaltyMember findByStoreIdAndCustomerId(String storeId, String customerId);

    LoyaltyMember findByStoreIdAndUsername(String storeId, String username);

    LoyaltyMember findByCardToken(String cardToken);

    LoyaltyMember findByStoreIdAndReferralCode(String storeId, String referralCode);

    List<LoyaltyMember> findByStoreIdOrderByLifetimeDesc(String storeId);

    List<LoyaltyMember> findByStoreIdAndBalanceGreaterThanAndLastActivityAtLessThan(String storeId, long balance, Instant before);

    /// Moves a balance by `points` only if it stays at or above zero.
    @Modifying
    @Query("update LoyaltyMember m set m.balance = m.balance + :points, m.version = m.version + 1 "
            + "where m.id = :id and m.balance + :points >= 0")
    int addIf(@Param("id") String id, @Param("points") long points);

    /// Moves a balance by `points`, which may take it below zero: a reversal
    /// of points already spent is a debt the next purchase pays back.
    @Modifying
    @Query("update LoyaltyMember m set m.balance = m.balance + :points, m.version = m.version + 1 "
            + "where m.id = :id")
    int add(@Param("id") String id, @Param("points") long points);

    @Modifying
    @Query("update LoyaltyMember m set m.referralRewarded = true where m.id = :id "
            + "and m.referralRewarded = false")
    int claimReferral(@Param("id") String id);
}
