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
package com.codenameone.examples.parley.repository;

import com.codename1.orm.repository.CrudRepository;
import com.codename1.orm.repository.Modifying;
import com.codename1.orm.repository.Param;
import com.codename1.orm.repository.Query;
import com.codename1.orm.repository.Repository;
import com.codenameone.examples.parley.domain.CallRecord;

import java.time.Instant;
import java.util.List;

/// Calls, live and past.
@Repository(entity = CallRecord.class, key = String.class)
public interface CallRecordRepository extends CrudRepository<CallRecord, String> {
    /// Makes the rest of the transaction wait behind any other that holds
    /// the same call: the row is written to the value it has, and a written
    /// row is kept by its writer until it commits. Call it before reading.
    ///
    /// @return 1, or 0 when there is no such call
    @Modifying
    @Query("update CallRecord c set c.startedAt = c.startedAt where c.id = :id")
    int hold(@Param("id") String id);

    @Query("select c from CallRecord c where c.caller = :username or c.callee = :username order by c.startedAt desc")
    List<CallRecord> historyOf(@Param("username") String username);

    @Query("select c from CallRecord c where (c.caller = :username or c.callee = :username) and c.endedAt = :none")
    List<CallRecord> liveOf(@Param("username") String username, @Param("none") Instant none);

    @Query("select c from CallRecord c where c.endedAt = :none and c.startedAt < :before")
    List<CallRecord> openSince(@Param("before") Instant before, @Param("none") Instant none);

    long countByStartedAtGreaterThanEqual(Instant since);

    @Query("select c.startedAt from CallRecord c where c.startedAt >= :since")
    List<Instant> startedSince(@Param("since") Instant since);

    @Modifying
    @Query("delete from CallRecord c where c.caller = :username or c.callee = :username")
    int deleteOf(@Param("username") String username);
}
