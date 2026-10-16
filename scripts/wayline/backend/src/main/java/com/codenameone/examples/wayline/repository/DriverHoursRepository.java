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
package com.codenameone.examples.wayline.repository;

import com.codename1.orm.repository.CrudRepository;
import com.codename1.orm.repository.Modifying;
import com.codename1.orm.repository.Param;
import com.codename1.orm.repository.Query;
import com.codename1.orm.repository.Repository;
import com.codename1.orm.session.Identifier;
import com.codenameone.examples.wayline.domain.DriverHours;

/// The time each driver spent on line, a row for each day, keyed by
/// `Identifier.of(username, dayNumber)`.
@Repository(entity = DriverHours.class, key = Identifier.class)
public interface DriverHoursRepository extends CrudRepository<DriverHours, Identifier> {
    /// Adds time on line to a day that already has a row. The addition is the
    /// database's, so reports that overlap are both counted.
    ///
    /// @return 1, or 0 when the day has no row yet
    @Modifying
    @Query("update DriverHours h set h.millis = h.millis + :millis "
            + "where h.username = :username and h.dayNumber = :day")
    int addMillis(@Param("username") String username, @Param("day") long dayNumber,
            @Param("millis") long millis);

    int deleteByUsername(String username);
}
