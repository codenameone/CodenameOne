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
package com.codenameone.examples.pulse.repository;

import com.codename1.orm.repository.CrudRepository;
import com.codename1.orm.repository.Modifying;
import com.codename1.orm.repository.Param;
import com.codename1.orm.repository.Query;
import com.codename1.orm.repository.Repository;
import com.codenameone.examples.pulse.domain.Shift;

import java.time.Instant;
import java.util.List;

@Repository(entity = Shift.class, key = String.class)
public interface ShiftRepository extends CrudRepository<Shift, String> {
    List<Shift> findByStaffAndEndsAtGreaterThanOrderByStartsAtAsc(String staff, Instant after);

    List<Shift> findByLocationIdAndStateAndEndsAtGreaterThanOrderByStartsAtAsc(String locationId,
            String state, Instant after);

    List<Shift> findByLocationIdAndEndsAtGreaterThanOrderByStartsAtAsc(String locationId,
            Instant after);

    /// Hands an offered shift to `taker`. 1 when it did; 0 when somebody took
    /// it first or it was withdrawn.
    @Modifying
    @Query("update Shift s set s.staff = :taker, s.state = 'ASSIGNED' where s.id = :id"
            + " and s.state = 'SWAP_REQUESTED' and s.staff <> :taker")
    int take(@Param("id") String id, @Param("taker") String taker);

    int deleteByStaff(String staff);
}
