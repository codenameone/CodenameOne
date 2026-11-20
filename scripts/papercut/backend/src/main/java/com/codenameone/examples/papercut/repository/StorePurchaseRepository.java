/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * Use of this source code is governed by the license in the LICENSE file beside
 * this project's pom.xml.
 */
package com.codenameone.examples.papercut.repository;

import com.codename1.orm.repository.CrudRepository;
import com.codename1.orm.repository.Param;
import com.codename1.orm.repository.Query;
import com.codename1.orm.repository.Repository;
import com.codenameone.examples.papercut.domain.StorePurchase;
import java.time.Instant;
import java.util.List;

@Repository(entity = StorePurchase.class, key = String.class)
public interface StorePurchaseRepository extends CrudRepository<StorePurchase, String> {
    @Query("select s from StorePurchase s where s.username = :u order by s.createdAt desc, s.id asc")
    List<StorePurchase> of(@Param("u") String username);

    @Query("select s from StorePurchase s where s.createdAt >= :since")
    List<StorePurchase> since(@Param("since") Instant since);
}
