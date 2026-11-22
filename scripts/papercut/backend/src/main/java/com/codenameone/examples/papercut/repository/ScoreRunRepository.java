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
import com.codenameone.examples.papercut.domain.ScoreRun;

/// Accepted runs.
@Repository(entity = ScoreRun.class, key = String.class)
public interface ScoreRunRepository extends CrudRepository<ScoreRun, String> {
    /// Every standing run of a mode since `since`, best first. A board keeps the
    /// first row it meets per player.
    @Query("select r from ScoreRun r where r.mode = :mode and r.struck = false and r.createdAt >= :since "
            + "order by r.score desc, r.createdAt asc, r.id asc")
    List<ScoreRun> board(@Param("mode") String mode, @Param("since") Instant since);

    @Query("select r from ScoreRun r where r.username = :u and r.struck = false order by r.createdAt asc, r.id asc")
    List<ScoreRun> of(@Param("u") String username);

    @Query("select count(r) from ScoreRun r where r.createdAt >= :since")
    Long since(@Param("since") Instant since);
}
