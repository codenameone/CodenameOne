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
import com.codename1.orm.repository.Param;
import com.codename1.orm.repository.Query;
import com.codename1.orm.repository.Repository;
import com.codenameone.examples.pulse.domain.CheckIn;

import java.time.Instant;
import java.util.List;

@Repository(entity = CheckIn.class, key = String.class)
public interface CheckInRepository extends CrudRepository<CheckIn, String> {
    long countByMemberIdAndAtGreaterThanEqual(String memberId, Instant since);

    long countByAccount(String account);

    long countByAccountAndAtGreaterThanEqual(String account, Instant since);

    long countByLocationIdAndAtGreaterThanEqual(String locationId, Instant since);

    long countByAtGreaterThanEqual(Instant since);
    List<CheckIn> findByAtGreaterThanEqual(Instant since);
    List<CheckIn> findByMemberIdAndAtGreaterThanEqualOrderByAtAsc(String memberId, Instant since);
    /// When each member, not account, last came in: a child and their parent
    /// share an account.
    @Query("select c.memberId, max(c.at) from CheckIn c group by c.memberId")
    List<Object[]> lastVisitsByMember();

    List<CheckIn> findByAccountAndAtGreaterThanEqualOrderByAtAsc(String account, Instant since);

    List<CheckIn> findByLocationIdAndAtGreaterThanEqualOrderByAtDesc(String locationId,
            Instant since);

    /// When each account last came in.
    @Query("select c.account, max(c.at) from CheckIn c group by c.account")
    List<Object[]> lastVisits();

    @Query("select max(c.at) from CheckIn c where c.account = :account")
    Instant lastVisit(@Param("account") String account);

    int deleteByAccount(String account);
}
