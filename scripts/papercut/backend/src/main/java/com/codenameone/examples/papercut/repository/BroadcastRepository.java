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
import com.codenameone.examples.papercut.domain.Broadcast;

/// Pushes sent from the console.
@Repository(entity = Broadcast.class, key = String.class)
public interface BroadcastRepository extends CrudRepository<Broadcast, String> {
    @Query("select b from Broadcast b order by b.createdAt desc, b.id asc")
    List<Broadcast> newest(PageRequest page);
}
