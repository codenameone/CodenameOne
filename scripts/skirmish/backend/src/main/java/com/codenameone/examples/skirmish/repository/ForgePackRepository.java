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
import com.codenameone.examples.skirmish.domain.ForgePack;
import java.util.List;

@Repository(entity = ForgePack.class, key = String.class)
public interface ForgePackRepository extends CrudRepository<ForgePack, String> {
    @Query("select p from ForgePack p where p.owner = :u order by p.updatedAt desc, p.id asc")
    List<ForgePack> ownedBy(@Param("u") String owner, PageRequest page);

    @Query("select p from ForgePack p where p.status = :s and p.official = :o order by p.name asc, p.id asc")
    List<ForgePack> official(@Param("s") String status, @Param("o") boolean official, PageRequest page);

    /// Published packs every file of which a moderator has approved.
    @Query("select p from ForgePack p where p.status = :s and p.pendingAssets = :pa "
            + "order by p.plays desc, p.id asc")
    List<ForgePack> popular(@Param("s") String status, @Param("pa") boolean pendingAssets, PageRequest page);

    @Query("select p from ForgePack p where p.status = :s and p.pendingAssets = :pa "
            + "order by p.updatedAt desc, p.id asc")
    List<ForgePack> newest(@Param("s") String status, @Param("pa") boolean pendingAssets, PageRequest page);

    /// What a moderator should look at: reported packs and hidden ones.
    @Query("select p from ForgePack p where p.reports > :r or p.status = :s order by p.reports desc, p.id asc")
    List<ForgePack> queue(@Param("r") int minReports, @Param("s") String status, PageRequest page);

    /// Packs still waiting on a file a moderator has not approved.
    @Query("select p from ForgePack p where p.pendingAssets = :pa order by p.id asc")
    List<ForgePack> awaitingAssets(@Param("pa") boolean pendingAssets, PageRequest page);

    long countByOwner(String owner);
}
