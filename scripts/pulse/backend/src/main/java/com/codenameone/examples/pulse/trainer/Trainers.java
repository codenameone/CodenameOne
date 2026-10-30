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
package com.codenameone.examples.pulse.trainer;

import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.pulse.Ids;
import com.codenameone.examples.pulse.account.Accounts;
import com.codenameone.examples.pulse.api.AttendanceDto;
import com.codenameone.examples.pulse.api.RosterEntryDto;
import com.codenameone.examples.pulse.api.SessionDto;
import com.codenameone.examples.pulse.api.ShiftDto;
import com.codenameone.examples.pulse.domain.Booking;
import com.codenameone.examples.pulse.domain.CheckIn;
import com.codenameone.examples.pulse.domain.ClassSession;
import com.codenameone.examples.pulse.domain.Member;
import com.codenameone.examples.pulse.domain.Profile;
import com.codenameone.examples.pulse.domain.Shift;
import com.codenameone.examples.pulse.repository.BookingRepository;
import com.codenameone.examples.pulse.repository.CheckInRepository;
import com.codenameone.examples.pulse.repository.ClassSessionRepository;
import com.codenameone.examples.pulse.repository.MemberRepository;
import com.codenameone.examples.pulse.repository.ProfileRepository;
import com.codenameone.examples.pulse.repository.ShiftRepository;
import com.codenameone.examples.pulse.studio.Bookings;
import com.codenameone.examples.pulse.studio.Studio;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/// A trainer's day: the classes they teach, who is coming, who came, and
/// the shifts they work.
@Component
public class Trainers {
    /// The shift states.
    public static final String ASSIGNED = "ASSIGNED";
    public static final String SWAP_REQUESTED = "SWAP_REQUESTED";
    /// Attendance can be corrected this long after a class ends, before the
    /// class is settled and its absentees become no-shows.
    static final long CORRECT_MILLIS = 30L * 60000L;

    private final ClassSessionRepository sessions;
    private final BookingRepository bookings;
    private final MemberRepository members;
    private final CheckInRepository checkIns;
    private final ShiftRepository shifts;
    private final ProfileRepository profiles;
    private final Studio studio;
    private final HeartRates rates;

    public Trainers(ClassSessionRepository sessions, BookingRepository bookings,
            MemberRepository members, CheckInRepository checkIns, ShiftRepository shifts,
            ProfileRepository profiles, Studio studio, HeartRates rates) {
        this.sessions = sessions;
        this.bookings = bookings;
        this.members = members;
        this.checkIns = checkIns;
        this.shifts = shifts;
        this.profiles = profiles;
        this.studio = studio;
        this.rates = rates;
    }

    /// The classes `trainer` teaches from the start of today for a week.
    @Transactional(readOnly = true)
    public List<SessionDto> classes(String trainer) throws IOException {
        Instant from = Instant.ofEpochMilli(System.currentTimeMillis() / 86400000L * 86400000L);
        return studio.describe(trainer,
                sessions.findByTrainerAndStartsAtGreaterThanEqualAndStartsAtLessThanOrderByStartsAtAsc(
                        trainer, from, from.plusMillis(7L * 86400000L)));
    }

    /// Who is booked into a class, who is waiting, who came; with what the
    /// trainer needs to know about each, and their heart rate when it is
    /// being shared.
    @Transactional(readOnly = true)
    public List<RosterEntryDto> roster(String caller, boolean owner, String sessionId) {
        ClassSession session = teaching(caller, owner, sessionId);
        List<Booking> rows = bookings.findBySessionIdOrderByCreatedAtAsc(session.getId());
        List<RosterEntryDto> out = new ArrayList<RosterEntryDto>();
        LocalDate day = LocalDate.ofEpochDay(session.getStartsAt().toEpochMilli() / 86400000L);
        long now = System.currentTimeMillis();
        for (int iter = 0; iter < rows.size(); iter++) {
            Booking row = rows.get(iter);
            if (Bookings.CANCELLED.equals(row.getState())
                    || Bookings.LATE_CANCEL.equals(row.getState())) {
                continue;
            }
            Member member = members.findById(row.getMemberId());
            if (member == null) {
                continue;
            }
            out.add(entry(row, member, day, session.getId(), now));
        }
        return out;
    }

    private RosterEntryDto entry(Booking row, Member member, LocalDate day, String sessionId,
            long now) {
        long before = bookings.countByMemberIdAndState(member.getId(), Bookings.CHECKED_IN);
        boolean first = before == 0 || before == 1 && Bookings.CHECKED_IN.equals(row.getState());
        int[] rate = rates.get(sessionId, member.getId(), now);
        return new RosterEntryDto(row.getId(), member.getId(), member.getName(),
                Accounts.age(member.getBirthDate(), day), row.getState(),
                (int) row.getQueuePosition(), first, member.getMedicalNote(),
                rate == null ? 0 : rate[0], rate == null ? 0 : rate[1]);
    }

    /// Marks someone present or absent, for a member the door did not see:
    /// a child brought in through the side, a phone that was flat.
    @Transactional(rollbackFor = IOException.class)
    public RosterEntryDto attendance(String caller, boolean owner, String sessionId,
            AttendanceDto mark) throws IOException {
        if (mark == null || mark.bookingId() == null) {
            throw new ResponseStatusException(400, "Say who");
        }
        ClassSession session = teaching(caller, owner, sessionId);
        long now = System.currentTimeMillis();
        if (now < session.getStartsAt().toEpochMilli() - 3600000L
                || now > session.getEndsAt().toEpochMilli() + CORRECT_MILLIS
                || !Studio.SCHEDULED.equals(session.getState())) {
            throw new ResponseStatusException(409,
                    "Attendance is taken from an hour before a class to just after it");
        }
        Booking row = bookings.findById(mark.bookingId());
        if (row == null || !row.getSessionId().equals(session.getId())) {
            throw new ResponseStatusException(404, "No such booking");
        }
        boolean here = Bookings.CHECKED_IN.equals(row.getState());
        if (mark.present() && Bookings.BOOKED.equals(row.getState())) {
            row.setState(Bookings.CHECKED_IN);
            row.setCheckedInAt(Instant.ofEpochMilli(now));
            CheckIn visit = new CheckIn();
            visit.setId(Ids.next());
            visit.setMemberId(row.getMemberId());
            visit.setAccount(row.getAccount());
            visit.setLocationId(session.getLocationId());
            visit.setMethod("STAFF");
            visit.setSessionId(session.getId());
            visit.setAt(Instant.ofEpochMilli(now));
            visit.setStaff(caller);
            checkIns.insert(visit);
        } else if (!mark.present() && here) {
            // A mistaken tick. The visit it recorded stays in the door's
            // log; the booking goes back to expected.
            row.setState(Bookings.BOOKED);
            row.setCheckedInAt(Instant.EPOCH);
        } else if (!Bookings.BOOKED.equals(row.getState()) && !here) {
            throw new ResponseStatusException(409, "That person is not booked into the class");
        }
        row.setUpdatedAt(Instant.ofEpochMilli(now));
        Member member = members.findById(row.getMemberId());
        LocalDate day = LocalDate.ofEpochDay(session.getStartsAt().toEpochMilli() / 86400000L);
        return entry(row, member, day, session.getId(), now);
    }

    /// A class the caller teaches, or any class for the owner. Someone
    /// else's is answered as none.
    ClassSession teaching(String caller, boolean owner, String sessionId) {
        ClassSession session = studio.sessionOrNull(sessionId);
        if (session == null || !owner && !caller.equals(session.getTrainer())) {
            throw new ResponseStatusException(404, "No such class");
        }
        return session;
    }

    /// The trainer's shifts still to come, and the ones others offered up.
    @Transactional(readOnly = true)
    public List<ShiftDto> shifts(String trainer) {
        Instant now = Instant.now();
        List<Shift> mine = shifts.findByStaffAndEndsAtGreaterThanOrderByStartsAtAsc(trainer,
                now);
        List<ShiftDto> out = new ArrayList<ShiftDto>();
        String home = "";
        for (int iter = 0; iter < mine.size(); iter++) {
            out.add(dto(mine.get(iter)));
            home = mine.get(iter).getLocationId();
        }
        if (home.length() > 0) {
            List<Shift> offered = shifts.findByLocationIdAndStateAndEndsAtGreaterThanOrderByStartsAtAsc(
                    home, SWAP_REQUESTED, now);
            for (int iter = 0; iter < offered.size(); iter++) {
                if (!offered.get(iter).getStaff().equals(trainer)) {
                    out.add(dto(offered.get(iter)));
                }
            }
        }
        return out;
    }

    /// Offers one of the caller's shifts to the others. Offering it again
    /// withdraws the offer.
    @Transactional(rollbackFor = IOException.class)
    public ShiftDto offer(String trainer, String id) {
        Shift row = id == null ? null : shifts.findById(id);
        if (row == null || !row.getStaff().equals(trainer)) {
            throw new ResponseStatusException(404, "No such shift");
        }
        if (!row.getStartsAt().isAfter(Instant.now())) {
            throw new ResponseStatusException(409, "That shift has started");
        }
        row.setState(SWAP_REQUESTED.equals(row.getState()) ? ASSIGNED : SWAP_REQUESTED);
        return dto(row);
    }

    /// Takes a shift someone else offered. Two trainers tapping Take together
    /// are told apart by one conditional statement ([ShiftRepository#take]).
    @Transactional(rollbackFor = IOException.class)
    public ShiftDto take(String trainer, String id) {
        Shift row = id == null ? null : shifts.findById(id);
        if (row == null) {
            throw new ResponseStatusException(404, "No such shift");
        }
        if (!row.getStartsAt().isAfter(Instant.now())) {
            throw new ResponseStatusException(409, "That shift has started");
        }
        if (shifts.take(id, trainer) != 1) {
            throw new ResponseStatusException(409, "Somebody else took that shift");
        }
        return dto(shifts.findById(id));
    }

    public ShiftDto dto(Shift row) {
        Profile who = profiles.findById(row.getStaff());
        return new ShiftDto(row.getId(), row.getLocationId(), row.getStaff(),
                who == null ? "" : who.getDisplayName(), row.getDuty(),
                row.getStartsAt().toEpochMilli(), row.getEndsAt().toEpochMilli(), row.getState());
    }
}
