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
package com.codenameone.examples.pulse.desk;

import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.pulse.Days;
import com.codenameone.examples.pulse.Ids;
import com.codenameone.examples.pulse.Text;
import com.codenameone.examples.pulse.account.Accounts;
import com.codenameone.examples.pulse.api.CheckInRequestDto;
import com.codenameone.examples.pulse.api.CheckInResultDto;
import com.codenameone.examples.pulse.api.DeskDayDto;
import com.codenameone.examples.pulse.api.MemberSummaryDto;
import com.codenameone.examples.pulse.api.SessionDto;
import com.codenameone.examples.pulse.api.WalkInDto;
import com.codenameone.examples.pulse.domain.Booking;
import com.codenameone.examples.pulse.domain.CheckIn;
import com.codenameone.examples.pulse.domain.ClassSession;
import com.codenameone.examples.pulse.domain.ClassType;
import com.codenameone.examples.pulse.domain.Member;
import com.codenameone.examples.pulse.domain.Membership;
import com.codenameone.examples.pulse.domain.MembershipPlan;
import com.codenameone.examples.pulse.domain.WalkIn;
import com.codenameone.examples.pulse.live.LiveHub;
import com.codenameone.examples.pulse.repository.BookingRepository;
import com.codenameone.examples.pulse.repository.CheckInRepository;
import com.codenameone.examples.pulse.repository.ClassSessionRepository;
import com.codenameone.examples.pulse.repository.ClassTypeRepository;
import com.codenameone.examples.pulse.repository.MemberRepository;
import com.codenameone.examples.pulse.repository.MembershipPlanRepository;
import com.codenameone.examples.pulse.repository.WalkInRepository;
import com.codenameone.examples.pulse.studio.Bookings;
import com.codenameone.examples.pulse.studio.Household;
import com.codenameone.examples.pulse.studio.Passes;
import com.codenameone.examples.pulse.studio.Studio;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/// The front desk and the door: letting people in, finding a member, and
/// taking a walk-in's money.
///
/// A check-in always answers -- admitted, or refused with the reason the desk
/// reads out -- and never fails for a member who is simply not allowed in:
/// a kiosk turns that answer into a green or a red screen, and a refusal is
/// as much an answer as an admission.
@Component
public class Desk {
    /// A booking counts as this visit's from this long before its class...
    static final long EARLY_MILLIS = 60L * 60000L;
    /// ...to this long after it started.
    static final long LATE_MILLIS = 30L * 60000L;
    static final String[] WALK_IN_KINDS = {"GUEST", "DAY_PASS", "TRIAL"};
    static final String[] PAID_BY = {"CASH", "CARD", "NONE"};
    /// The most a walk-in is charged at the desk, in cents. A typo guard.
    static final long MOST_CENTS = 50000L;

    private final Passes passes;
    private final MemberRepository members;
    private final Household household;
    private final MembershipPlanRepository plans;
    private final BookingRepository bookings;
    private final ClassSessionRepository sessions;
    private final ClassTypeRepository types;
    private final CheckInRepository checkIns;
    private final WalkInRepository walkIns;
    private final Accounts accounts;
    private final Studio studio;
    private final LiveHub live;

    public Desk(Passes passes, MemberRepository members, Household household,
            MembershipPlanRepository plans, BookingRepository bookings,
            ClassSessionRepository sessions, ClassTypeRepository types,
            CheckInRepository checkIns, WalkInRepository walkIns, Accounts accounts,
            Studio studio, LiveHub live) {
        this.passes = passes;
        this.members = members;
        this.household = household;
        this.plans = plans;
        this.bookings = bookings;
        this.sessions = sessions;
        this.types = types;
        this.checkIns = checkIns;
        this.walkIns = walkIns;
        this.accounts = accounts;
        this.studio = studio;
        this.live = live;
    }

    /// Lets someone in, or says why not. `staff` is who is at the desk, or
    /// the kiosk's account.
    public CheckInResultDto checkIn(String staff, CheckInRequestDto request) throws IOException {
        if (request == null || request.locationId() == null
                || request.locationId().length() == 0) {
            throw new ResponseStatusException(400, "Say which door");
        }
        CheckInResultDto result;
        try {
            result = admit(staff, request);
        } catch (ResponseStatusException answered) {
            throw answered;
        } catch (RuntimeException raced) {
            // The same code shown at two doors at once: both found it unused
            // and the second insert of it failed. That is a replay.
            result = refused(REPLAYED, null);
        }
        live.checkedIn(request.locationId(), result.memberName(), result.admitted());
        return result;
    }

    private static final String REPLAYED = "That pass was already used. Refresh it and try again";

    @Transactional(rollbackFor = IOException.class)
    CheckInResultDto admit(String staff, CheckInRequestDto request) throws IOException {
        Instant now = Instant.now();
        String method;
        String memberId;
        if (request.code() != null && request.code().length() > 0) {
            method = "QR";
            memberId = passes.redeem(request.code().trim(), now.toEpochMilli());
            if (memberId == null) {
                return refused("That pass is not valid. Refresh it and try again", null);
            }
            if (memberId.length() == 0) {
                return refused(REPLAYED, null);
            }
        } else if (request.nfcId() != null && request.nfcId().length() > 0) {
            method = "NFC";
            memberId = passes.byNfc(request.nfcId().trim());
            if (memberId == null) {
                return refused("That card is not one of ours", null);
            }
        } else {
            // Looked up by name at the desk, which only staff can do; a
            // kiosk has no lookup.
            method = "STAFF";
            memberId = request.memberId();
        }
        Member member = memberId == null || memberId.length() > 32 ? null
                : members.findById(memberId);
        if (member == null) {
            return refused("No such member", null);
        }
        if (!accounts.enabled(member.getAccount())) {
            return refused("This account is on hold. Please see the desk", member);
        }
        if (!member.getWaiverAt().isAfter(Instant.EPOCH)) {
            return refused("The waiver has not been signed", member);
        }
        Membership membership = household.current(member.getId());
        MembershipPlan plan = membership == null ? null : plans.findById(membership.getPlanId());
        if (membership == null) {
            return refused("No membership. A day pass is available at the desk", member);
        }
        if ("PAST_DUE".equals(membership.getState())) {
            return refused("A payment is overdue. Please see the desk", member);
        }
        if ("FROZEN".equals(membership.getState()) && membership.getFrozenUntil().isAfter(now)) {
            return refused("The membership is frozen", member);
        }
        // The class they are here for, if they booked one: it is marked as
        // attended, and a pack's class was already spent on booking it.
        List<Booking> booked = bookings.bookedBetween(member.getId(),
                now.minusMillis(LATE_MILLIS), now.plusMillis(EARLY_MILLIS));
        Booking booking = null;
        ClassSession session = null;
        for (int iter = 0; iter < booked.size() && booking == null; iter++) {
            ClassSession candidate = sessions.findById(booked.get(iter).getSessionId());
            if (candidate != null && candidate.getLocationId().equals(request.locationId())) {
                booking = booked.get(iter);
                session = candidate;
            }
        }
        if (booking == null && plan != null && Household.counted(plan)) {
            return refused("A class pack is for booked classes. Book one first", member);
        }
        if (booking != null) {
            booking.setState(Bookings.CHECKED_IN);
            booking.setCheckedInAt(now);
            booking.setUpdatedAt(now);
        }
        CheckIn row = new CheckIn();
        row.setId(Ids.next());
        row.setMemberId(member.getId());
        row.setAccount(member.getAccount());
        row.setLocationId(request.locationId());
        row.setMethod(method);
        row.setSessionId(session == null ? "" : session.getId());
        row.setAt(now);
        row.setStaff(staff);
        checkIns.insert(row);
        ClassType type = session == null ? null : types.findById(session.getClassTypeId());
        return new CheckInResultDto(true, "", member.getId(), member.getName(),
                Accounts.age(member.getBirthDate(), LocalDate.now()),
                plan == null ? "" : plan.getName(), membership.getState(),
                type == null ? "" : type.getName(),
                session == null ? 0L : session.getStartsAt().toEpochMilli(),
                (int) checkIns.countByMemberIdAndAtGreaterThanEqual(member.getId(),
                        monthStart(now)));
    }

    private CheckInResultDto refused(String reason, Member member) {
        Membership membership = member == null ? null : household.current(member.getId());
        MembershipPlan plan = membership == null ? null : plans.findById(membership.getPlanId());
        return new CheckInResultDto(false, reason, member == null ? "" : member.getId(),
                member == null ? "" : member.getName(),
                member == null ? 0 : Accounts.age(member.getBirthDate(), LocalDate.now()),
                plan == null ? "" : plan.getName(),
                membership == null ? "NONE" : membership.getState(), "", 0L, 0);
    }

    /// Members whose name or account contains `query`, for the desk.
    @Transactional(readOnly = true)
    public List<MemberSummaryDto> find(String query) {
        String text = query == null ? "" : query.trim();
        if (text.length() < 2) {
            throw new ResponseStatusException(400, "Type at least two letters");
        }
        if (text.length() > 60 || text.indexOf('%') >= 0 || text.indexOf('_') >= 0) {
            throw new ResponseStatusException(400, "That is not a name");
        }
        List<Member> rows = members.search("%" + text + "%");
        List<MemberSummaryDto> out = new ArrayList<MemberSummaryDto>();
        LocalDate today = LocalDate.now();
        for (int iter = 0; iter < rows.size() && out.size() < 25; iter++) {
            Member row = rows.get(iter);
            Membership membership = household.current(row.getId());
            MembershipPlan plan = membership == null ? null
                    : plans.findById(membership.getPlanId());
            Instant last = checkIns.lastVisit(row.getAccount());
            out.add(new MemberSummaryDto(row.getId(), row.getAccount(), row.getName(),
                    Accounts.age(row.getBirthDate(), today), row.getRelation(),
                    membership == null ? "NONE" : membership.getState(),
                    plan == null ? "" : plan.getName(), last == null ? 0L : last.toEpochMilli()));
        }
        return out;
    }

    /// Records someone who came in without a membership: a guest, a day
    /// pass or a free trial. Their details are what the owner's campaigns
    /// follow up on; the money, if any, was taken at the desk.
    @Transactional(rollbackFor = IOException.class)
    public WalkInDto walkIn(String staff, WalkInDto walkIn) throws IOException {
        if (walkIn == null) {
            throw new ResponseStatusException(400, "Nothing to record");
        }
        String name = Text.required(walkIn.name(), 120, "their name");
        String email = walkIn.email() == null || walkIn.email().trim().length() == 0 ? ""
                : Accounts.email(walkIn.email());
        String phone = Text.optional(walkIn.phone(), 32, "their phone");
        String kind = Text.oneOf(walkIn.kind(), WALK_IN_KINDS, WALK_IN_KINDS[1], "the kind");
        String paidBy = Text.oneOf(walkIn.paidBy(), PAID_BY, PAID_BY[0], "how it was paid");
        if (walkIn.amountCents() < 0 || walkIn.amountCents() > MOST_CENTS) {
            throw new ResponseStatusException(400, "That amount is not a day pass");
        }
        if ("NONE".equals(paidBy) != (walkIn.amountCents() == 0)) {
            throw new ResponseStatusException(400, "Say how it was paid, or that it was free");
        }
        String location = Text.required(walkIn.locationId(), 32, "the location");
        WalkIn row = new WalkIn();
        row.setId(Ids.next());
        row.setLocationId(location);
        row.setName(name);
        row.setEmail(email);
        row.setPhone(phone);
        row.setKind(kind);
        row.setAmountCents(walkIn.amountCents());
        row.setPaidBy(paidBy);
        row.setStaff(staff);
        row.setAt(Instant.now());
        walkIns.insert(row);
        return toDto(row);
    }

    static WalkInDto toDto(WalkIn row) {
        return new WalkInDto(row.getId(), row.getLocationId(), row.getName(), row.getEmail(),
                row.getPhone(), row.getKind(), row.getAmountCents(), row.getPaidBy(),
                row.getAt().toEpochMilli());
    }

    /// The desk's day at a location: who came, what was taken, and what is
    /// starting soon.
    @Transactional(readOnly = true)
    public DeskDayDto today(String staff, String locationId) throws IOException {
        if (locationId == null || locationId.length() == 0) {
            throw new ResponseStatusException(400, "Say which location");
        }
        Instant now = Instant.now();
        // The location's day, not UTC's: a studio in New York would otherwise
        // start a new day at seven or eight in the evening.
        ZoneId zone = studio.zone(locationId);
        Instant dayStart = LocalDate.now(zone).atStartOfDay(zone).toInstant();
        List<WalkIn> walked = walkIns.findByLocationIdAndAtGreaterThanEqualOrderByAtDesc(
                locationId, dayStart);
        long cents = 0L;
        for (int iter = 0; iter < walked.size(); iter++) {
            cents += walked.get(iter).getAmountCents();
        }
        List<SessionDto> upcoming = studio.describe(staff,
                sessions.findByLocationIdAndStartsAtGreaterThanEqualAndStartsAtLessThanOrderByStartsAtAsc(
                        locationId, now.minusMillis(LATE_MILLIS), now.plusMillis(3L * 3600000L)));
        List<CheckIn> came = checkIns.findByLocationIdAndAtGreaterThanEqualOrderByAtDesc(
                locationId, dayStart);
        List<CheckInResultDto> recent = new ArrayList<CheckInResultDto>();
        LocalDate today = LocalDate.now(zone);
        for (int iter = 0; iter < came.size() && recent.size() < 12; iter++) {
            CheckIn row = came.get(iter);
            Member member = members.findById(row.getMemberId());
            if (member == null) {
                continue;
            }
            Membership membership = household.current(member.getId());
            MembershipPlan plan = membership == null ? null
                    : plans.findById(membership.getPlanId());
            ClassSession session = row.getSessionId().length() == 0 ? null
                    : sessions.findById(row.getSessionId());
            ClassType type = session == null ? null : types.findById(session.getClassTypeId());
            recent.add(new CheckInResultDto(true, "", member.getId(), member.getName(),
                    Accounts.age(member.getBirthDate(), today),
                    plan == null ? "" : plan.getName(),
                    membership == null ? "NONE" : membership.getState(),
                    type == null ? "" : type.getName(),
                    session == null ? 0L : session.getStartsAt().toEpochMilli(), 0));
        }
        return new DeskDayDto(came.size(), walked.size(), cents, upcoming, recent);
    }

    /// Midnight UTC on the first of `now`'s month.
    static Instant monthStart(Instant now) {
        LocalDate day = LocalDate.ofEpochDay(now.toEpochMilli() / Days.MILLIS);
        return Instant.ofEpochMilli(day.withDayOfMonth(1).toEpochDay() * Days.MILLIS);
    }
}
