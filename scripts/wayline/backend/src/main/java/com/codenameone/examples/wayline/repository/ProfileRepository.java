/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codenameone.examples.wayline.repository;

import com.codename1.orm.repository.CrudRepository;
import com.codename1.orm.repository.Modifying;
import com.codename1.orm.repository.Param;
import com.codename1.orm.repository.Query;
import com.codename1.orm.repository.Repository;
import com.codenameone.examples.wayline.domain.Profile;

import java.util.List;

/// Profiles, keyed by the e-mail an account signs in with.
///
/// A repository holds the queries of one kind of entity and nothing else: no
/// rule about who may do what, and no transaction of its own. The build writes
/// its implementation, and every call runs on the session of the transaction
/// its caller opened, so a service method marked `@Transactional` is what
/// makes several of these calls one unit of work. Called outside one, it is
/// refused.
@Repository(entity = Profile.class, key = String.class)
public interface ProfileRepository extends CrudRepository<Profile, String> {
    /// Makes the rest of the transaction wait its turn behind any other that
    /// holds the same account: the row is written, to the value it has, and a
    /// written row is kept by its writer until it commits. For a check and an
    /// insert that must not be interleaved with the same pair made for the
    /// same account. Call it before anything is read: like every bulk
    /// statement it empties the session, and what is read afterwards is what
    /// the transaction before this one left.
    ///
    /// @return 1, or 0 when the account has no profile
    @Modifying
    @Query("update Profile p set p.createdAt = p.createdAt where p.username = :username")
    int hold(@Param("username") String username);

    /// Every profile, newest first.
    @Query("select p from Profile p order by p.createdAt desc, p.username asc")
    List<Profile> newestFirst();

    /// The profiles of these accounts; none for a name that has none.
    List<Profile> findByUsernameIn(List<String> usernames);

    /// The profiles of every account that has a driver's state: the fleet,
    /// and not every account there is.
    @Query("select p from Profile p where p.username in (select d.username from DriverState d)")
    List<Profile> drivers();

    /// The display names of some accounts, as `{username, displayName}`. The
    /// two fields and not the profiles: a list of applications shows nothing
    /// else of them.
    @Query("select p.username, p.displayName from Profile p where p.username in :names")
    List<Object[]> displayNames(@Param("names") List<String> usernames);

    long countByFlaggedTrue();

    /// When each account made since `since` was made.
    @Query("select p.createdAt from Profile p where p.createdAt >= :since")
    List<Long> signUps(@Param("since") long since);
}
