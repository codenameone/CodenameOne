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
package com.codenameone.examples.fieldline.repository;

import com.codename1.orm.repository.CrudRepository;
import com.codename1.orm.repository.Modifying;
import com.codename1.orm.repository.Param;
import com.codename1.orm.repository.Query;
import com.codename1.orm.repository.Repository;
import com.codenameone.examples.fieldline.domain.Profile;

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
    /// same account. Call it before anything is read: what is read after it is
    /// what the transaction before this one left, while a row read before it
    /// was read before the wait.
    ///
    /// @return 1, or 0 when the account has no profile
    @Modifying
    @Query("update Profile p set p.createdAt = p.createdAt where p.username = :username")
    int hold(@Param("username") String username);

    /// The profiles of these accounts; none for a name that has none.
    List<Profile> findByUsernameIn(List<String> usernames);
}
