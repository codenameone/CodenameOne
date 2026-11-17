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
package com.codenameone.examples.fieldline.repository;

import com.codename1.orm.repository.CrudRepository;
import com.codename1.orm.repository.Param;
import com.codename1.orm.repository.Query;
import com.codename1.orm.repository.Repository;
import com.codenameone.examples.fieldline.domain.Job;

import java.util.List;

/// Jobs. Most reads are a span of the board: the jobs due, or planned, in it.
@Repository(entity = Job.class, key = String.class)
public interface JobRepository extends CrudRepository<Job, String> {
    /// The jobs whose window overlaps the span, or that are planned in it.
    @Query("select j from Job j where (j.windowEnd > :from and j.windowStart < :to) "
            + "or (j.plannedStart >= :from and j.plannedStart < :to) "
            + "order by j.plannedStart asc, j.id asc")
    List<Job> inSpan(@Param("from") long from, @Param("to") long to);

    /// One technician's planned jobs in the span, in driving order.
    @Query("select j from Job j where j.techId = :tech and j.plannedStart >= :from "
            + "and j.plannedStart < :to order by j.plannedStart asc")
    List<Job> plannedFor(@Param("tech") String techId, @Param("from") long from, @Param("to") long to);
}
