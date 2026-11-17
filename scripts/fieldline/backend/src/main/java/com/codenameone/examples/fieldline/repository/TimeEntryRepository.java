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
import com.codenameone.examples.fieldline.domain.TimeEntry;

import java.util.List;

/// Clocked time.
@Repository(entity = TimeEntry.class, key = String.class)
public interface TimeEntryRepository extends CrudRepository<TimeEntry, String> {
    /// A technician's spans that start inside the span from `from` to `to`,
    /// earliest first.
    @Query("select t from TimeEntry t where t.techId = :tech and t.startAt >= :from and t.startAt < :to order by t.startAt asc")
    List<TimeEntry> ofTechnician(@Param("tech") String techId, @Param("from") long from, @Param("to") long to);

    /// A technician's spans still open, of any kind.
    @Query("select t from TimeEntry t where t.techId = :tech and t.endAt = 0 order by t.startAt asc")
    List<TimeEntry> open(@Param("tech") String techId);
}
