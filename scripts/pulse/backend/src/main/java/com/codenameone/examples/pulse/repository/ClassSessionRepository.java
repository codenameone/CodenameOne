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
import com.codenameone.examples.pulse.domain.ClassSession;

import java.time.Instant;
import java.util.List;

/// The timetable.
///
/// The places taken in a class are a counter on its row, and the counter is
/// only ever moved by the two conditional updates below. A booking is a place
/// when [#takePlace] changed a row and a queue entry when it did not: the
/// database decides which, one statement at a time, so two people asking for
/// the last place at once cannot both get it, whatever else is in flight.
@Repository(entity = ClassSession.class, key = String.class)
public interface ClassSessionRepository extends CrudRepository<ClassSession, String> {
    List<ClassSession> findByLocationIdAndStartsAtGreaterThanEqualAndStartsAtLessThanOrderByStartsAtAsc(
            String locationId, Instant from, Instant to);

    List<ClassSession> findByTrainerAndStartsAtGreaterThanEqualAndStartsAtLessThanOrderByStartsAtAsc(
            String trainer, Instant from, Instant to);

    List<ClassSession> findByStartsAtGreaterThanEqualAndStartsAtLessThanOrderByStartsAtAsc(
            Instant from, Instant to);

    List<ClassSession> findByIdIn(List<String> ids);

    boolean existsByTrainerAndStateAndStartsAtGreaterThan(String trainer, String state,
            Instant after);

    /// Classes in a room or with a trainer that overlap `[from, to)`.
    @Query("select count(s) from ClassSession s where s.state = 'SCHEDULED' and s.startsAt < :to"
            + " and s.endsAt > :from and (s.roomId = :room or s.trainer = :trainer)")
    Long clashes(@Param("room") String room, @Param("trainer") String trainer,
            @Param("from") Instant from, @Param("to") Instant to);

    /// Takes a place if there is one. 1 when it did, 0 when the class is full
    /// or no longer on.
    @Modifying
    @Query("update ClassSession s set s.booked = s.booked + 1 where s.id = :id"
            + " and s.booked < s.capacity and s.state = 'SCHEDULED'")
    int takePlace(@Param("id") String id);

    @Modifying
    @Query("update ClassSession s set s.booked = s.booked - 1 where s.id = :id and s.booked > 0")
    int givePlace(@Param("id") String id);

    @Modifying
    @Query("update ClassSession s set s.waitlisted = s.waitlisted + :delta where s.id = :id"
            + " and s.waitlisted + :delta >= 0")
    int moveQueue(@Param("id") String id, @Param("delta") int delta);

    /// Holds the class's row until the transaction ends, so that changes to
    /// its queue are made one at a time.
    @Modifying
    @Query("update ClassSession s set s.waitlisted = s.waitlisted where s.id = :id")
    int hold(@Param("id") String id);
}
