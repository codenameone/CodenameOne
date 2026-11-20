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
import com.codenameone.examples.papercut.domain.Flag;

/// Runs held for review.
@Repository(entity = Flag.class, key = String.class)
public interface FlagRepository extends CrudRepository<Flag, String> {
    @Query("select f from Flag f where f.pending = :pending order by f.createdAt desc, f.id asc")
    List<Flag> inState(@Param("pending") boolean pending, PageRequest page);

    long countByPendingTrue();

    long countByUsernameAndPendingTrue(String username);
}
