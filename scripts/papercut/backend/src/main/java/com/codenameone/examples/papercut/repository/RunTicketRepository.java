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
import com.codenameone.examples.papercut.domain.RunTicket;

/// Outstanding run tickets.
@Repository(entity = RunTicket.class, key = String.class)
public interface RunTicketRepository extends CrudRepository<RunTicket, String> {
    /// Marks a ticket used, once: answers 1 for the request that wins and 0 for
    /// a second submit racing it.
    @Modifying
    @Query("update RunTicket t set t.used = true where t.token = :token and t.used = false")
    int use(@Param("token") String token);

    int deleteByExpiresAtLessThan(Instant before);
}
