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
import com.codename1.orm.repository.Param;
import com.codename1.orm.repository.Query;
import com.codename1.orm.repository.Repository;
import com.codenameone.examples.pulse.domain.Booking;

import java.time.Instant;
import java.util.List;

@Repository(entity = Booking.class, key = String.class)
public interface BookingRepository extends CrudRepository<Booking, String> {
    Booking findFirstBySessionIdAndMemberId(String sessionId, String memberId);

    Booking findFirstByIdAndAccount(String id, String account);

    List<Booking> findBySessionIdOrderByCreatedAtAsc(String sessionId);

    List<Booking> findBySessionIdAndStateOrderByQueuePositionAsc(String sessionId, String state);

    List<Booking> findByAccountAndStateIn(String account, List<String> states);

    List<Booking> findBySessionIdIn(List<String> sessionIds);

    List<Booking> findByMemberIdAndStateOrderByCheckedInAtDesc(String memberId, String state);

    long countByMemberIdAndState(String memberId, String state);

    long countByAccountAndState(String account, String state);

    /// The last place in the queue handed out, so the next is one more.
    @Query("select max(b.queuePosition) from Booking b where b.sessionId = :session")
    Long lastPosition(@Param("session") String sessionId);

    /// The member's place in a class starting between two times, for the door.
    @Query("select b from Booking b where b.memberId = :member and b.state = 'BOOKED'"
            + " and b.sessionId in (select s.id from ClassSession s where s.startsAt >= :from"
            + " and s.startsAt < :to)")
    List<Booking> bookedBetween(@Param("member") String memberId, @Param("from") Instant from,
            @Param("to") Instant to);

    /// Which class types an account's household has been to, for nudges.
    @Query("select distinct s.classTypeId from ClassSession s where s.id in (select b.sessionId"
            + " from Booking b where b.account = :account and b.state = 'CHECKED_IN')")
    List<String> typesAttended(@Param("account") String account);

    /// Accounts that have checked in to a class of this type.
    @Query("select distinct b.account from Booking b where b.state = 'CHECKED_IN' and b.sessionId"
            + " in (select s.id from ClassSession s where s.classTypeId = :type)")
    List<String> accountsWhoTook(@Param("type") String classTypeId);

    int deleteByAccount(String account);
}
