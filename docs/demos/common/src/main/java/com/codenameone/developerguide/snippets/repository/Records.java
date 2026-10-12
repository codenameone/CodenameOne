/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codenameone.developerguide.snippets.repository;

import com.codename1.orm.repository.*;
import com.codename1.orm.session.LockMode;
import java.util.List;

/** Build-time repository example; no handwritten implementation. */
// tag::generated-repository[]
@Repository(entity=RepositoryRecord.class, key=Long.class)
public interface Records extends CrudRepository<RepositoryRecord, Long> {
    List<RepositoryRecord> findByNameAndScoreGreaterThanOrderByScoreDescIdAsc(String name, long score, PageRequest page);
    boolean existsByName(String name);
    long countByName(String name);
    int deleteByName(String name);
    @Lock(LockMode.PESSIMISTIC_WRITE)
    RepositoryRecord findById(Long id);
    @Query("select sum(case when r.score>=:floor then 1 else 0 end) from com.codenameone.developerguide.snippets.repository.RepositoryRecord r")
    Long qualified(@Param("floor") long floor);
    @Query(value="SELECT COUNT(*) FROM repository_records WHERE name=:name OR name=:name", nativeSql=true)
    Long nativeCount(@Param("name") String name);
    @Modifying
    @Query("update com.codenameone.developerguide.snippets.repository.RepositoryRecord r set r.score=:score where r.name=:name")
    int revise(@Param("name") String name, @Param("score") long score);
}
// end::generated-repository[]
