/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * Use of this source code is governed by the license in the LICENSE file beside
 * this project's pom.xml.
 */
package com.codenameone.examples.papercut.repository;

import com.codename1.orm.repository.CrudRepository;
import com.codename1.orm.repository.PageRequest;
import com.codename1.orm.repository.Param;
import com.codename1.orm.repository.Query;
import com.codename1.orm.repository.Repository;
import com.codenameone.examples.papercut.domain.AdminAction;
import java.util.List;

@Repository(entity = AdminAction.class, key = String.class)
public interface AdminActionRepository extends CrudRepository<AdminAction, String> {
    @Query("select a from AdminAction a order by a.createdAt desc, a.id asc")
    List<AdminAction> newest(PageRequest page);

    @Query("select a from AdminAction a where a.username = :u order by a.createdAt desc, a.id asc")
    List<AdminAction> of(@Param("u") String username, PageRequest page);
}
