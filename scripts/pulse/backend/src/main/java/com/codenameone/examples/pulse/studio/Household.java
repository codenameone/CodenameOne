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
package com.codenameone.examples.pulse.studio;

import com.codename1.backend.Crypto;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.pulse.Ids;
import com.codenameone.examples.pulse.Text;
import com.codenameone.examples.pulse.account.Accounts;
import com.codenameone.examples.pulse.api.MemberDto;
import com.codenameone.examples.pulse.api.NewMemberDto;
import com.codenameone.examples.pulse.api.PassDto;
import com.codenameone.examples.pulse.domain.Member;
import com.codenameone.examples.pulse.domain.Membership;
import com.codenameone.examples.pulse.domain.MembershipPlan;
import com.codenameone.examples.pulse.domain.PassKey;
import com.codenameone.examples.pulse.repository.MemberRepository;
import com.codenameone.examples.pulse.repository.MembershipPlanRepository;
import com.codenameone.examples.pulse.repository.MembershipRepository;
import com.codenameone.examples.pulse.repository.PassKeyRepository;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/// A household: the account holder and the children they book for.
///
/// Every member of a household is booked, billed and let in on their own --
/// a child has their own membership, their own place in a class and their
/// own door pass -- but only the account holder signs in. A child's age is
/// what decides which classes they can take, so it is read from their birth
/// date on the day of the class, never stored.
@Component
public class Household {
    /// The most people one account books for.
    static final int MOST = 8;
    /// A door pass's code changes this often, in seconds.
    public static final int PERIOD_SECONDS = 30;
    /// And has this many digits.
    public static final int DIGITS = 8;

    /// The states of a membership that still describe someone.
    static final List<String> CURRENT = Arrays.asList("ACTIVE", "FROZEN", "PAST_DUE");

    private final MemberRepository members;
    private final MembershipRepository memberships;
    private final MembershipPlanRepository plans;
    private final PassKeyRepository passes;
    private final Accounts accounts;

    public Household(MemberRepository members, MembershipRepository memberships,
            MembershipPlanRepository plans, PassKeyRepository passes, Accounts accounts) {
        this.members = members;
        this.memberships = memberships;
        this.plans = plans;
        this.passes = passes;
        this.accounts = accounts;
    }

    @Transactional(readOnly = true)
    public List<MemberDto> list(String account) throws IOException {
        List<Member> rows = members.findByAccountOrderByCreatedAtAsc(account);
        List<MemberDto> out = new ArrayList<MemberDto>();
        LocalDate today = LocalDate.now();
        for (int iter = 0; iter < rows.size(); iter++) {
            out.add(toDto(rows.get(iter), today));
        }
        return out;
    }

    /// Adds a child. Answers 400 for one who is old enough for an account of
    /// their own, and 409 once the household is full.
    @Transactional(rollbackFor = IOException.class)
    public MemberDto addChild(String account, NewMemberDto child) throws IOException {
        if (child == null) {
            throw new ResponseStatusException(400, "Nothing to add");
        }
        String name = Text.required(child.name(), 120, "their name");
        LocalDate born = Accounts.birthDate(child.birthDate());
        LocalDate today = LocalDate.now();
        if (Accounts.age(born, today) >= Accounts.ACCOUNT_AGE) {
            throw new ResponseStatusException(400, "At " + Accounts.ACCOUNT_AGE
                    + " they can open an account of their own");
        }
        String note = Text.optional(child.medicalNote(), 500, "the medical note");
        // Held, so two additions at once cannot both see room for one more.
        accounts.hold(account);
        if (members.countByAccount(account) >= MOST) {
            throw new ResponseStatusException(409, "A household has at most " + MOST
                    + " members");
        }
        Member row = new Member();
        row.setId(Ids.next());
        row.setAccount(account);
        row.setName(name);
        row.setBirthDate(born);
        row.setRelation("CHILD");
        row.setMedicalNote(note);
        row.setCreatedAt(Instant.now());
        members.insert(row);
        return toDto(row, today);
    }

    /// Records that the account holder signed the waiver for one of their
    /// household. Signing again is harmless and keeps the first date.
    @Transactional(rollbackFor = IOException.class)
    public MemberDto signWaiver(String account, String memberId) throws IOException {
        Member row = mine(account, memberId);
        if (!signed(row)) {
            row.setWaiverAt(Instant.now());
        }
        return toDto(row, LocalDate.now());
    }

    /// A member's door pass, made the first time it is asked for.
    @Transactional(rollbackFor = IOException.class)
    public PassDto pass(String account, String memberId) throws IOException {
        Member row = mine(account, memberId);
        PassKey key = passes.findById(row.getId());
        if (key == null) {
            key = new PassKey();
            key.setMemberId(row.getId());
            fill(key);
            passes.insert(key);
        }
        return toDto(row, key);
    }

    /// Replaces a pass's secret, for a phone that was lost: the old one's
    /// codes and its NFC id stop opening the door at once.
    @Transactional(rollbackFor = IOException.class)
    public PassDto rotate(String account, String memberId) throws IOException {
        Member row = mine(account, memberId);
        PassKey key = passes.findById(row.getId());
        if (key == null) {
            return pass(account, memberId);
        }
        fill(key);
        return toDto(row, key);
    }

    /// A member of the caller's household, or 404. Someone else's member is
    /// answered the same as none, so ids cannot be probed.
    public Member mine(String account, String memberId) {
        Member row = memberId == null ? null : members.findFirstByIdAndAccount(memberId, account);
        if (row == null) {
            throw new ResponseStatusException(404, "No such member");
        }
        return row;
    }

    /// Whether a plan is a number of classes rather than a period of time:
    /// a pack, or a single drop-in, which is a pack of one.
    public static boolean counted(MembershipPlan plan) {
        return "CLASS_PACK".equals(plan.getKind()) || "DROP_IN".equals(plan.getKind());
    }

    public static boolean signed(Member row) {
        return row.getWaiverAt().isAfter(Instant.EPOCH);
    }

    private static void fill(PassKey key) throws IOException {
        key.setSecret(Ids.hex(Crypto.randomBytes(20)));
        key.setNfcId(Ids.hex(Crypto.randomBytes(8)));
        key.setCreatedAt(Instant.now());
    }

    private static PassDto toDto(Member row, PassKey key) {
        return new PassDto(row.getId(), row.getName(), key.getSecret(), key.getNfcId(),
                PERIOD_SECONDS, DIGITS);
    }

    /// The member as the app shows them, with the membership that lets them
    /// in, if any.
    public MemberDto toDto(Member row, LocalDate today) {
        Membership current = current(row.getId());
        MembershipPlan plan = current == null ? null : plans.findById(current.getPlanId());
        return new MemberDto(row.getId(), row.getName(), row.getBirthDate().toString(),
                Accounts.age(row.getBirthDate(), today), row.getRelation(), signed(row),
                row.getMedicalNote(), current == null ? "NONE" : current.getState(),
                plan == null ? "" : plan.getName());
    }

    /// The membership a member holds now, or null.
    public Membership current(String memberId) {
        List<Membership> held = memberships.findByMemberIdAndStateIn(memberId, CURRENT);
        Membership best = null;
        for (int iter = 0; iter < held.size(); iter++) {
            Membership one = held.get(iter);
            if (best == null || one.getStartedAt().isAfter(best.getStartedAt())) {
                best = one;
            }
        }
        return best;
    }
}
