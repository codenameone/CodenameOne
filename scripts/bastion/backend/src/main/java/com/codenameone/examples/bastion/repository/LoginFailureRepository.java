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

import java.time.Instant;
import com.codename1.orm.repository.CrudRepository;
import com.codename1.orm.repository.Modifying;
import com.codename1.orm.repository.Param;
import com.codename1.orm.repository.Query;
import com.codename1.orm.repository.Repository;
import com.codenameone.examples.bastion.domain.LoginFailure;

@Repository(entity = LoginFailure.class, key = String.class)
public interface LoginFailureRepository extends CrudRepository<LoginFailure, String> {
    /// Starts a new window when the old one has run out.
    @Modifying
    @Query("update LoginFailure f set f.failures = 0, f.firstAt = :now "
            + "where f.username = :username and f.firstAt < :cutoff")
    int restartWindow(@Param("username") String username, @Param("now") Instant now,
            @Param("cutoff") Instant cutoff);

    /// One more failure, counted by the database: two at once are two.
    @Modifying
    @Query("update LoginFailure f set f.failures = f.failures + 1 where f.username = :username")
    int increment(@Param("username") String username);

    /// Locks the account when the count has reached `attempts`, and starts
    /// over; 1 when this call locked it.
    @Modifying
    @Query("update LoginFailure f set f.lockedUntil = :until, f.failures = 0, f.firstAt = :now "
            + "where f.username = :username and f.failures >= :attempts")
    int lockIfReached(@Param("username") String username, @Param("attempts") int attempts,
            @Param("now") Instant now, @Param("until") Instant until);
}
