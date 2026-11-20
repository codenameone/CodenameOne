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
