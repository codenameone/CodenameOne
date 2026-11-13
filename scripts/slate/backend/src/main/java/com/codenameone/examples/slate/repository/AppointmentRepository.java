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
package com.codenameone.examples.slate.repository;

import com.codename1.orm.repository.CrudRepository;
import com.codename1.orm.repository.Modifying;
import com.codename1.orm.repository.Param;
import com.codename1.orm.repository.Query;
import com.codename1.orm.repository.Repository;
import com.codenameone.examples.slate.domain.Appointment;

import java.time.Instant;
import java.util.List;

@Repository(entity = Appointment.class, key = String.class)
public interface AppointmentRepository extends CrudRepository<Appointment, String> {
    Appointment findFirstByIdAndClient(String id, String client);

    Appointment findFirstByShareToken(String shareToken);

    @Query("select a from Appointment a where a.client = :client and a.endsAt > :now"
            + " and a.state in ('HELD', 'CONFIRMED') order by a.startsAt asc")
    List<Appointment> upcoming(@Param("client") String client, @Param("now") Instant now);

    @Query("select a from Appointment a where a.client = :client and (a.endsAt <= :now"
            + " or a.state in ('CANCELLED', 'COMPLETED', 'NO_SHOW')) order by a.startsAt desc")
    List<Appointment> history(@Param("client") String client, @Param("now") Instant now);

    /// Appointments that take time, overlapping `[from, to)` with buffers.
    @Query("select a from Appointment a where a.state in ('HELD', 'CONFIRMED', 'COMPLETED',"
            + " 'NO_SHOW') and a.blockStart < :to and a.blockEnd > :from")
    List<Appointment> between(@Param("from") Instant from, @Param("to") Instant to);

    /// A client's appointments that take time, overlapping `[from, to)`.
    @Query("select a from Appointment a where a.client = :client and a.state in ('HELD',"
            + " 'CONFIRMED', 'COMPLETED') and a.startsAt < :to and a.endsAt > :from")
    List<Appointment> clientBetween(@Param("client") String client, @Param("from") Instant from,
            @Param("to") Instant to);

    /// Appointments of a provider overlapping `[from, to)`, cancelled ones
    /// included so the schedule can show what changed.
    @Query("select a from Appointment a where a.id in (select p.appointmentId from"
            + " AppointmentParty p where p.ref = :provider and p.kind = 'PROVIDER')"
            + " and a.blockStart < :to and a.blockEnd > :from order by a.startsAt asc")
    List<Appointment> forProvider(@Param("provider") String provider, @Param("from") Instant from,
            @Param("to") Instant to);

    @Query("select a from Appointment a where a.state = 'HELD' and a.holdExpiresAt < :now")
    List<Appointment> lapsedHolds(@Param("now") Instant now);

    @Query("select a from Appointment a where a.state = 'CONFIRMED' and a.remindedAt = :never"
            + " and a.startsAt > :now and a.startsAt <= :until")
    List<Appointment> toRemind(@Param("now") Instant now, @Param("until") Instant until,
            @Param("never") Instant never);

    @Query("select a from Appointment a where a.state = 'CONFIRMED' and a.paymentState = 'PENDING'"
            + " and a.startsAt <= :until")
    List<Appointment> toAuthorize(@Param("until") Instant until);

    List<Appointment> findByGroupKeyAndStateIn(String groupKey, List<String> states);

    long countByClientAndState(String client, String state);

    long countByClient(String client);

    @Query("select sum(a.depositCents) from Appointment a where a.client = :client"
            + " and a.paymentState = 'CAPTURED'")
    Long spent(@Param("client") String client);

    /// Moves the appointment from one state to another. 1 when it did, 0 when
    /// it was no longer in `from`: what stops a cancel and a confirm, or two
    /// sweeps, from both acting on one appointment.
    @Modifying
    @Query("update Appointment a set a.state = :to, a.updatedAt = :now where a.id = :id"
            + " and a.state = :from")
    int move(@Param("id") String id, @Param("from") String from, @Param("to") String to,
            @Param("now") Instant now);

    @Query("select a from Appointment a where a.startsAt >= :from and a.startsAt < :to")
    List<Appointment> started(@Param("from") Instant from, @Param("to") Instant to);

    boolean existsByClientAndMethodIdAndPaymentStateIn(String client, String methodId,
            List<String> paymentStates);

    boolean existsByClientAndPaymentStateIn(String client, List<String> paymentStates);

    int deleteByClientAndState(String client, String state);
}
