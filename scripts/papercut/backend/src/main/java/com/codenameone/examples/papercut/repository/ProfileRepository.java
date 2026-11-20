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
package com.codenameone.examples.papercut.repository;

import com.codename1.orm.repository.CrudRepository;
import com.codename1.orm.repository.Modifying;
import com.codename1.orm.repository.PageRequest;
import com.codename1.orm.repository.Param;
import com.codename1.orm.repository.Query;
import com.codename1.orm.repository.Repository;
import java.time.Instant;
import java.util.List;
import com.codenameone.examples.papercut.domain.Profile;

/// The players' game data.
@Repository(entity = Profile.class, key = String.class)
public interface ProfileRepository extends CrudRepository<Profile, String> {
    /// Locks the row for the rest of the transaction; answers 0 when there is none.
    @Modifying
    @Query("update Profile p set p.createdAt = p.createdAt where p.username = :username")
    int hold(@Param("username") String username);

    @Query("select p from Profile p where p.username like :q or p.displayName like :q "
            + "order by p.lastSeen desc, p.username asc")
    List<Profile> search(@Param("q") String like, PageRequest page);

    @Query("select p from Profile p where p.displayName = :name order by p.username asc")
    List<Profile> named(@Param("name") String displayName, PageRequest page);

    @Query("select p from Profile p order by p.bestClassic desc, p.username asc")
    List<Profile> byBestClassic(PageRequest page);

    @Query("select count(p) from Profile p where p.lastSeen >= :since")
    Long seenSince(@Param("since") Instant since);

    /// The players a broadcast reaches, a page at a time; banned players are
    /// never sent anything.
    @Query("select p from Profile p where p.banned = false and p.lastSeen < :seen order by p.username asc")
    List<Profile> audience(@Param("seen") Instant seenBefore, PageRequest page);

    /// Suspensions whose time is up, for the sweep that lifts them.
    @Query("select p from Profile p where p.banned = false and p.suspendedUntil > :epoch "
            + "and p.suspendedUntil <= :now")
    List<Profile> expired(@Param("epoch") Instant epoch, @Param("now") Instant now);
}
