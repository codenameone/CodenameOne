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
package com.codenameone.examples.brickyard.repository;

import com.codename1.orm.repository.CrudRepository;
import com.codename1.orm.repository.Param;
import com.codename1.orm.repository.Query;
import com.codename1.orm.repository.Repository;
import com.codenameone.examples.brickyard.domain.Sanction;
import com.codename1.orm.repository.PageRequest;
import java.util.List;

/// Storage for [Sanction].
@Repository(entity = Sanction.class, key = String.class)
public interface SanctionRepository extends CrudRepository<Sanction, String> {
    @Query("select s from Sanction s where s.username = :u order by s.createdAt asc, s.id asc")
    List<Sanction> of(@Param("u") String username);

    /// Sanctions not lifted that end after `now`, the newest first.
    @Query("select s from Sanction s where s.lifted = 0 and s.untilAt > :now "
            + "order by s.createdAt desc, s.id desc")
    List<Sanction> active(@Param("now") long now, PageRequest page);
}
