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
package com.codenameone.examples.dash.repository;

import com.codename1.orm.repository.CrudRepository;
import com.codename1.orm.repository.PageRequest;
import com.codename1.orm.repository.Param;
import com.codename1.orm.repository.Query;
import com.codename1.orm.repository.Repository;
import com.codenameone.examples.dash.domain.Referral;
import java.util.List;

@Repository(entity = Referral.class, key = String.class)
public interface ReferralRepository extends CrudRepository<Referral, String> {
    @Query("select r from Referral r where r.inviter = :u order by r.joinedAt desc, r.invitee asc")
    List<Referral> of(@Param("u") String inviter, PageRequest page);

    @Query("select r from Referral r order by r.joinedAt desc, r.invitee asc")
    List<Referral> newest(PageRequest page);

    long countByInviter(String inviter);

    long countByInviterAndRewardedTrue(String inviter);

    @Query("select count(r) from Referral r where r.state = :s")
    Long inState(@Param("s") String state);
}
