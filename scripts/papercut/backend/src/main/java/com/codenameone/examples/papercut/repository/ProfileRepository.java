/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * Use of this source code is governed by the license in the LICENSE file beside
 * this project's pom.xml.
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
