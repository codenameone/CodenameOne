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
package com.codenameone.examples.skirmish.repository;

import com.codename1.orm.repository.CrudRepository;
import com.codename1.orm.repository.PageRequest;
import com.codename1.orm.repository.Param;
import com.codename1.orm.repository.Query;
import com.codename1.orm.repository.Repository;
import com.codenameone.examples.skirmish.domain.Flag;
import java.util.List;

@Repository(entity = Flag.class, key = String.class)
public interface FlagRepository extends CrudRepository<Flag, String> {
    @Query("select f from Flag f where f.pending = :pending order by f.createdAt desc, f.id asc")
    List<Flag> inState(@Param("pending") boolean pending, PageRequest page);

    @Query("select f from Flag f where f.username = :u order by f.createdAt desc, f.id asc")
    List<Flag> of(@Param("u") String username, PageRequest page);

    long countByPendingTrue();

    long countByUsernameAndPendingTrue(String username);

    long countByReporterAndMatchId(String reporter, String matchId);
}
