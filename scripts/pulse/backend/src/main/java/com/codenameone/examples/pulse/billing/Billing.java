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
package com.codenameone.examples.pulse.billing;

import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Scheduled;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.pulse.Ids;
import com.codenameone.examples.pulse.account.Accounts;
import com.codenameone.examples.pulse.api.FreezeDto;
import com.codenameone.examples.pulse.api.InvoiceDto;
import com.codenameone.examples.pulse.api.JoinDto;
import com.codenameone.examples.pulse.api.MembershipDto;
import com.codenameone.examples.pulse.api.PlanDto;
import com.codenameone.examples.pulse.domain.Credit;
import com.codenameone.examples.pulse.domain.Invoice;
import com.codenameone.examples.pulse.domain.Member;
import com.codenameone.examples.pulse.domain.Membership;
import com.codenameone.examples.pulse.domain.MembershipPlan;
import com.codenameone.examples.pulse.domain.Profile;
import com.codenameone.examples.pulse.pay.PaymentDeclinedException;
import com.codenameone.examples.pulse.pay.Payments;
import com.codenameone.examples.pulse.repository.CreditRepository;
import com.codenameone.examples.pulse.repository.InvoiceRepository;
import com.codenameone.examples.pulse.repository.MemberRepository;
import com.codenameone.examples.pulse.repository.MembershipPlanRepository;
import com.codenameone.examples.pulse.repository.MembershipRepository;
import com.codenameone.examples.pulse.repository.ProfileRepository;
import com.codenameone.examples.pulse.studio.Household;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/// Memberships and what they cost: joining, renewing, freezing, cancelling,
/// and getting paid when a card says no.
///
/// A charge is three steps, and only the middle one talks to the processor:
/// a transaction writes the invoice and claims it ([InvoiceRepository#claim]),
/// the card is charged with no transaction open, and a second transaction
/// records what happened. The invoice id is the processor's idempotency key,
/// so a server that dies between the second and third step charges the same
/// invoice again on the next run and is answered the first charge, not a
/// second one.
///
/// The dunning ladder: a failed renewal is retried after [#RETRY_DAYS] days
/// each, and the membership stays usable through the first retries -- most
/// failures are a card that expired, mended within a day. From the
/// [#PAST_DUE_AFTER]th failure the membership is past due and the door says
/// so; after the last retry the invoice waits for the member to pay it by hand.
@Component
public class Billing {
    /// Membership states, beside [Household#CURRENT]'s.
    public static final String ACTIVE = "ACTIVE";
    public static final String FROZEN = "FROZEN";
    public static final String PAST_DUE = "PAST_DUE";
    public static final String PENDING = "PENDING";
    public static final String CANCELLED = "CANCELLED";
    public static final String EXPIRED = "EXPIRED";
    /// Invoice states. `CHARGING` is held only while one attempt is under way.
    public static final String OPEN = "OPEN";
    public static final String CHARGING = "CHARGING";
    public static final String PAID = "PAID";
    public static final String FAILED = "FAILED";
    public static final String VOID = "VOID";
    /// Days from each failed attempt to the next.
    static final int[] RETRY_DAYS = {1, 3, 7};
    /// The failure from which a membership is past due.
    static final int PAST_DUE_AFTER = 2;
    /// What a referrer is given when the member they brought pays for the
    /// first time, in minor units.
    static final long REFERRAL_CENTS = 2000L;
    /// A freeze is a week at least and three months at most.
    static final int FREEZE_MIN_DAYS = 7;
    static final int FREEZE_MAX_DAYS = 90;
    /// How long a class pack or a drop-in can be used.
    static final int PACK_MONTHS = 12;
    /// The `nextAttemptAt` of an invoice no job will try again.
    static final Instant NEVER = Instant.ofEpochMilli(4102444800000L);
    private static final long DAY = 86400000L;
    private static final List<String> LIVE = Arrays.asList(ACTIVE, FROZEN, PAST_DUE, PENDING);

    private final MembershipRepository memberships;
    private final MembershipPlanRepository plans;
    private final InvoiceRepository invoices;
    private final CreditRepository credits;
    private final MemberRepository members;
    private final ProfileRepository profiles;
    private final Payments payments;
    private final Household household;

    public Billing(MembershipRepository memberships, MembershipPlanRepository plans,
            InvoiceRepository invoices, CreditRepository credits, MemberRepository members,
            ProfileRepository profiles, Payments payments, Household household) {
        this.memberships = memberships;
        this.plans = plans;
        this.invoices = invoices;
        this.credits = credits;
        this.members = members;
        this.profiles = profiles;
        this.payments = payments;
        this.household = household;
    }

    // ------------------------------------------------------------- plans

    @Transactional(readOnly = true)
    public List<PlanDto> plans(boolean all) {
        List<MembershipPlan> rows = all ? plans.findAllByOrderByPriceCentsAsc()
                : plans.findByActiveTrueOrderByPriceCentsAsc();
        List<PlanDto> out = new ArrayList<PlanDto>();
        for (int iter = 0; iter < rows.size(); iter++) {
            out.add(plan(rows.get(iter)));
        }
        return out;
    }

    public static PlanDto plan(MembershipPlan row) {
        return new PlanDto(row.getId(), row.getName(), row.getDescription(), row.getKind(),
                row.getPriceCents(), row.getCurrency(), row.getPeriodMonths(),
                row.getClassCredits(), row.getMinAge(), row.getMaxAge(), row.isOffPeakOnly(),
                row.isActive());
    }

    // ------------------------------------------------------------- memberships

    @Transactional(readOnly = true)
    public List<MembershipDto> memberships(String account) {
        List<Membership> rows = memberships.findByAccountOrderByStartedAtDesc(account);
        List<MembershipDto> out = new ArrayList<MembershipDto>();
        for (int iter = 0; iter < rows.size(); iter++) {
            out.add(toDto(rows.get(iter)));
        }
        return out;
    }

    /// Puts a member of the caller's household on a plan and takes the first
    /// payment. A member already on a plan moves to the new one, and what is
    /// left of the old period comes off the first invoice ([#prorate]).
    ///
    /// Paid in cash, the membership is pending until the desk takes the money.
    /// Declined, nothing is kept but the declined invoice: a member never
    /// holds a membership they did not pay for.
    public MembershipDto join(String account, JoinDto request) throws IOException {
        String[] method = payments.choose(account, request == null ? null : request.methodId());
        String[] made = open(account, request, method[0]);
        if (!Payments.CASH.equals(method[0])) {
            try {
                collect(made[1], true);
            } catch (ResponseStatusException declined) {
                abandon(made[0], made[1]);
                throw declined;
            }
        }
        return membership(made[0]);
    }

    @Transactional(readOnly = true)
    MembershipDto membership(String id) {
        return toDto(memberships.findById(id));
    }

    @Transactional(readOnly = true)
    InvoiceDto invoice(String id) {
        return toDto(invoices.findById(id));
    }

    @Transactional(readOnly = true)
    Invoice owedBy(String account, String id) {
        return id == null || id.length() > 32 ? null : invoices.findFirstByIdAndAccount(id, account);
    }

    /// Undoes a join whose first payment was refused: the membership never
    /// starts, and its invoice is kept, void, as the record of the attempt.
    /// A previous plan the join had ended stays ended -- the member chose to
    /// leave it -- but its unused time stays on the account as credit.
    @Transactional(rollbackFor = IOException.class)
    void abandon(String membershipId, String invoiceId) {
        Membership row = memberships.findById(membershipId);
        row.setState(CANCELLED);
        row.setCurrentPeriodEnd(row.getStartedAt());
        row.setUpdatedAt(Instant.now());
        Invoice bill = invoices.findById(invoiceId);
        if (!PAID.equals(bill.getStatus())) {
            bill.setStatus(VOID);
            bill.setNextAttemptAt(NEVER);
            release(bill);
        }
    }

    /// Writes the membership and its first invoice.
    ///
    /// @return the membership's id and the invoice's
    @Transactional(rollbackFor = IOException.class)
    String[] open(String account, JoinDto request, String methodId) throws IOException {
        if (request == null || request.planId() == null) {
            throw new ResponseStatusException(400, "Choose a plan");
        }
        // No member named is the account holder joining for themselves.
        Member member = request.memberId() == null
                ? members.findFirstByAccountAndRelation(account, Accounts.SELF)
                : household.mine(account, request.memberId());
        if (member == null) {
            throw new ResponseStatusException(404, "No such member");
        }
        MembershipPlan plan = plans.findById(request.planId());
        if (plan == null || !plan.isActive()) {
            throw new ResponseStatusException(404, "No such plan");
        }
        int age = Accounts.age(member.getBirthDate(), LocalDate.now(ZoneOffset.UTC));
        if (plan.getMinAge() > 0 && age < plan.getMinAge()
                || plan.getMaxAge() > 0 && age > plan.getMaxAge()) {
            throw new ResponseStatusException(409, member.getName() + " is not of an age for "
                    + plan.getName());
        }
        if (!Household.signed(member)) {
            throw new ResponseStatusException(409, "Sign the waiver for " + member.getName()
                    + " first");
        }
        Instant now = Instant.now();
        long credit = 0;
        Membership old = household.current(member.getId());
        if (old != null) {
            if (PAST_DUE.equals(old.getState()) || PENDING.equals(old.getState())) {
                throw new ResponseStatusException(409, "Settle what is owed on "
                        + member.getName() + "'s membership first");
            }
            if (old.getPlanId().equals(plan.getId())) {
                throw new ResponseStatusException(409, member.getName() + " is on that plan");
            }
            MembershipPlan was = plans.findById(old.getPlanId());
            credit = was == null || Household.counted(was) ? 0
                    : prorate(was.getPriceCents(), old.getCurrentPeriodEnd(), was.getPeriodMonths(),
                            now);
            old.setState(CANCELLED);
            old.setCurrentPeriodEnd(now);
            old.setUpdatedAt(now);
        }
        Membership row = new Membership();
        row.setId(Ids.next());
        row.setMemberId(member.getId());
        row.setAccount(account);
        row.setPlanId(plan.getId());
        row.setState(Payments.CASH.equals(methodId) ? PENDING : ACTIVE);
        row.setStartedAt(now);
        row.setCurrentPeriodEnd(plusMonths(now, Household.counted(plan) ? PACK_MONTHS
                : plan.getPeriodMonths()));
        row.setCreditsLeft(plan.getClassCredits());
        row.setUpdatedAt(now);
        memberships.insert(row);
        if (credit > 0) {
            give(account, credit, "Unused time on a previous plan", now);
        }
        Invoice bill = invoice(row, plan, member, now, row.getCurrentPeriodEnd(), methodId);
        return new String[] {row.getId(), bill.getId()};
    }

    /// The share of a period's price that has not been used yet, by the
    /// millisecond, rounded down to the cent: a member never gets back more
    /// than they paid.
    public static long prorate(long priceCents, Instant periodEnd, int periodMonths,
            Instant now) {
        Instant start = plusMonths(periodEnd, -periodMonths);
        long whole = periodEnd.toEpochMilli() - start.toEpochMilli();
        long left = periodEnd.toEpochMilli() - now.toEpochMilli();
        if (whole <= 0 || left <= 0) {
            return 0;
        }
        if (left >= whole) {
            return priceCents;
        }
        // Multiplying first loses nothing for any real price: a period is
        // under 2^35 ms, a price under 2^27 cents.
        return priceCents * left / whole;
    }

    /// Freezes a membership between two dates: no billing and no classes
    /// meanwhile, and its period is moved on by the frozen days when it
    /// ends. Starts at once when `from` is today.
    @Transactional(rollbackFor = IOException.class)
    public MembershipDto freeze(String account, String id, FreezeDto request) {
        Membership row = owned(account, id);
        if (request == null) {
            throw new ResponseStatusException(400, "Choose the dates");
        }
        LocalDate from = date(request.from());
        LocalDate until = date(request.until());
        freeze(row, from, until, LocalDate.now(ZoneOffset.UTC));
        return toDto(row);
    }

    /// The freeze itself, also used by an away period that freezes billing.
    public void freeze(Membership row, LocalDate from, LocalDate until, LocalDate today) {
        MembershipPlan plan = plans.findById(row.getPlanId());
        if (plan != null && Household.counted(plan)) {
            throw new ResponseStatusException(409, "A class pack cannot be frozen");
        }
        if (!ACTIVE.equals(row.getState()) && !FROZEN.equals(row.getState())) {
            throw new ResponseStatusException(409, "Only an active membership can be frozen");
        }
        long days = until.toEpochDay() - from.toEpochDay();
        if (from.isBefore(today) || days < FREEZE_MIN_DAYS || days > FREEZE_MAX_DAYS) {
            throw new ResponseStatusException(400, "A freeze starts today or later and lasts "
                    + FREEZE_MIN_DAYS + " to " + FREEZE_MAX_DAYS + " days");
        }
        row.setFrozenFrom(start(from));
        row.setFrozenUntil(start(until));
        if (!from.isAfter(today)) {
            row.setState(FROZEN);
        }
        row.setUpdatedAt(Instant.now());
    }

    /// Ends a freeze now, or withdraws one that has not started. The period
    /// moves on by the days it was frozen.
    @Transactional(rollbackFor = IOException.class)
    public MembershipDto unfreeze(String account, String id) {
        Membership row = owned(account, id);
        if (row.getFrozenUntil().equals(Instant.EPOCH)) {
            throw new ResponseStatusException(409, "That membership is not frozen");
        }
        thaw(row, Instant.now());
        return toDto(row);
    }

    private static void thaw(Membership row, Instant now) {
        if (FROZEN.equals(row.getState())) {
            long frozen = Math.max(0, Math.min(now.toEpochMilli(), row.getFrozenUntil().toEpochMilli())
                    - row.getFrozenFrom().toEpochMilli());
            row.setCurrentPeriodEnd(row.getCurrentPeriodEnd().plusMillis(frozen));
            row.setState(ACTIVE);
        }
        row.setFrozenFrom(Instant.EPOCH);
        row.setFrozenUntil(Instant.EPOCH);
        row.setUpdatedAt(now);
    }

    /// Cancels at the end of the period paid for, or takes that back when
    /// asked again. A pending membership, never paid, ends at once.
    @Transactional(rollbackFor = IOException.class)
    public MembershipDto cancel(String account, String id) throws IOException {
        Membership row = owned(account, id);
        if (PENDING.equals(row.getState())) {
            row.setState(CANCELLED);
            List<Invoice> open = invoices.findByAccountOrderByCreatedAtDesc(account);
            for (int iter = 0; iter < open.size(); iter++) {
                Invoice bill = open.get(iter);
                if (bill.getMembershipId().equals(row.getId()) && OPEN.equals(bill.getStatus())) {
                    bill.setStatus(VOID);
                    bill.setFailure("Cancelled before it was paid");
                    release(bill);
                }
            }
        } else {
            row.setCancelAtPeriodEnd(!row.isCancelAtPeriodEnd());
        }
        row.setUpdatedAt(Instant.now());
        return toDto(row);
    }

    private Membership owned(String account, String id) {
        Membership row = id == null || id.length() > 32 ? null
                : memberships.findFirstByIdAndAccount(id, account);
        if (row == null || !LIVE.contains(row.getState())) {
            throw new ResponseStatusException(404, "No such membership");
        }
        return row;
    }

    // ------------------------------------------------------------- invoices

    @Transactional(readOnly = true)
    public List<InvoiceDto> invoices(String account) {
        return toDtos(invoices.findByAccountOrderByCreatedAtDesc(account));
    }

    /// Every invoice, or those in one state, for the owner.
    @Transactional(readOnly = true)
    public List<InvoiceDto> all(String status) {
        return toDtos(status == null || status.length() == 0 ? invoices.findAllByOrderByCreatedAtDesc()
                : invoices.findByStatusOrderByCreatedAtDesc(status));
    }

    /// A member paying an invoice that is owed, with the card they choose.
    public InvoiceDto pay(String account, String id, String methodId) throws IOException {
        Invoice bill = owedBy(account, id);
        if (bill == null) {
            throw new ResponseStatusException(404, "No such invoice");
        }
        String[] method = payments.choose(account, methodId);
        if (Payments.CASH.equals(method[0])) {
            throw new ResponseStatusException(400, "Pay cash at the front desk");
        }
        reroute(bill.getId(), method[0]);
        collect(bill.getId(), true);
        return invoice(bill.getId());
    }

    @Transactional(rollbackFor = IOException.class)
    void reroute(String id, String methodId) {
        Invoice bill = invoices.findById(id);
        if (!OPEN.equals(bill.getStatus()) && !FAILED.equals(bill.getStatus())) {
            throw new ResponseStatusException(409, "That invoice is not owed");
        }
        bill.setMethodId(methodId);
    }

    /// Voids an invoice that is owed, for the owner: what was taken off it
    /// in credit goes back to the member, and a membership waiting on it is
    /// put right.
    @Transactional(rollbackFor = IOException.class)
    public InvoiceDto voidInvoice(String id, String reason) {
        Invoice bill = id == null ? null : invoices.findById(id);
        if (bill == null) {
            throw new ResponseStatusException(404, "No such invoice");
        }
        if (!OPEN.equals(bill.getStatus()) && !FAILED.equals(bill.getStatus())) {
            throw new ResponseStatusException(409, "Only an invoice still owed can be voided");
        }
        bill.setStatus(VOID);
        bill.setFailure(reason == null || reason.trim().length() == 0 ? "Voided"
                : cut(reason.trim(), 200));
        bill.setNextAttemptAt(NEVER);
        release(bill);
        Membership row = memberships.findById(bill.getMembershipId());
        if (row != null && (PAST_DUE.equals(row.getState()) || PENDING.equals(row.getState()))
                && !owes(row)) {
            row.setState(ACTIVE);
            row.setUpdatedAt(Instant.now());
        }
        return toDto(bill);
    }

    /// Charges an invoice once. `interactive` is a member waiting on the
    /// answer: a decline is then theirs to see, as a 402, rather than a
    /// retry scheduled behind their back.
    void collect(String invoiceId, boolean interactive) throws IOException {
        String[] job = claim(invoiceId);
        if (job == null) {
            if (interactive) {
                throw new ResponseStatusException(409, "That invoice is already being paid");
            }
            return;
        }
        long amount = Long.parseLong(job[3]);
        String reference;
        try {
            reference = amount <= 0 ? "credit" : payments.charge(invoiceId, job[0], job[1], amount,
                    job[2], job[4]);
        } catch (PaymentDeclinedException declined) {
            failed(invoiceId, declined.getMessage());
            if (interactive) {
                throw new ResponseStatusException(402, declined.getMessage());
            }
            return;
        } catch (IOException unreachable) {
            // Not a decline: the processor was not reached, or said nothing
            // usable. The invoice goes back to owed without counting the
            // attempt as a failure, and the next run tries it again.
            reopen(invoiceId);
            throw unreachable;
        }
        paid(invoiceId, reference);
    }

    /// Claims an invoice for one attempt.
    ///
    /// @return the account, method, currency, amount due and description,
    ///   or null when it is not this caller's to charge
    @Transactional(rollbackFor = IOException.class)
    String[] claim(String id) {
        if (invoices.claim(id) != 1) {
            return null;
        }
        Invoice bill = invoices.findById(id);
        return new String[] {bill.getAccount(), bill.getMethodId(), bill.getCurrency(),
            String.valueOf(bill.getAmountCents() - bill.getCreditCents()), bill.getDescription()};
    }

    @Transactional(rollbackFor = IOException.class)
    void paid(String id, String reference) throws IOException {
        Instant now = Instant.now();
        Invoice bill = invoices.findById(id);
        bill.setStatus(PAID);
        bill.setChargeRef(reference);
        bill.setPaidAt(now);
        bill.setFailure("");
        bill.setNextAttemptAt(NEVER);
        Membership row = memberships.findById(bill.getMembershipId());
        if (row != null && (PAST_DUE.equals(row.getState()) || PENDING.equals(row.getState()))
                && !owes(row)) {
            row.setState(ACTIVE);
            row.setUpdatedAt(now);
        }
        referral(bill.getAccount(), now);
    }

    @Transactional(rollbackFor = IOException.class)
    void failed(String id, String why) {
        Instant now = Instant.now();
        Invoice bill = invoices.findById(id);
        int attempt = bill.getAttempts();
        bill.setStatus(FAILED);
        bill.setFailure(cut(why == null ? "Declined" : why, 200));
        bill.setNextAttemptAt(attempt <= RETRY_DAYS.length
                ? now.plusMillis(RETRY_DAYS[attempt - 1] * DAY) : NEVER);
        Membership row = memberships.findById(bill.getMembershipId());
        if (row != null && attempt >= PAST_DUE_AFTER && ACTIVE.equals(row.getState())) {
            row.setState(PAST_DUE);
            row.setUpdatedAt(now);
        }
    }

    @Transactional(rollbackFor = IOException.class)
    void reopen(String id) {
        Invoice bill = invoices.findById(id);
        bill.setStatus(bill.getAttempts() > 1 ? FAILED : OPEN);
        bill.setAttempts(bill.getAttempts() - 1);
    }

    private boolean owes(Membership row) {
        List<Invoice> mine = invoices.findByAccountOrderByCreatedAtDesc(row.getAccount());
        for (int iter = 0; iter < mine.size(); iter++) {
            Invoice bill = mine.get(iter);
            if (bill.getMembershipId().equals(row.getId()) && (FAILED.equals(bill.getStatus())
                    || OPEN.equals(bill.getStatus()) || CHARGING.equals(bill.getStatus()))) {
                return true;
            }
        }
        return false;
    }

    /// The first payment of an account somebody referred earns that somebody
    /// a credit, once.
    private void referral(String account, Instant now) throws IOException {
        Profile who = profiles.findById(account);
        if (who == null || who.getReferredBy() == null || who.getReferredBy().length() == 0
                || invoices.countByAccountAndStatus(account, PAID) != 1) {
            return;
        }
        String reason = "Referral: " + account;
        if (profiles.findById(who.getReferredBy()) != null
                && !credits.existsByAccountAndReason(who.getReferredBy(), reason)) {
            give(who.getReferredBy(), REFERRAL_CENTS, reason, now);
        }
    }

    // ------------------------------------------------------------- jobs

    /// Renews what is due, starts and ends freezes, and retries what failed.
    /// Each membership and invoice is its own transaction, so one that
    /// fails is logged and left for the next run without holding up the rest.
    @Scheduled(fixedDelay = 600000, initialDelay = 60000, lock = "pulse-billing")
    public void run() {
        Instant now = Instant.now();
        List<String> due = renewals(now);
        for (int iter = 0; iter < due.size(); iter++) {
            try {
                String bill = renew(due.get(iter), now);
                if (bill != null) {
                    collect(bill, false);
                }
            } catch (IOException | RuntimeException err) {
                System.err.println("[billing] " + err);
            }
        }
        List<String> retries = retries(now);
        for (int iter = 0; iter < retries.size(); iter++) {
            try {
                collect(retries.get(iter), false);
            } catch (IOException | RuntimeException err) {
                System.err.println("[billing] " + err);
            }
        }
    }

    /// Memberships whose period has ended, and those whose freeze starts or
    /// ends today, by id.
    @Transactional(readOnly = true)
    List<String> renewals(Instant now) {
        List<String> out = new ArrayList<String>();
        List<Membership> rows = memberships.findByStateIn(Arrays.asList(ACTIVE, FROZEN, PAST_DUE));
        for (int iter = 0; iter < rows.size(); iter++) {
            Membership row = rows.get(iter);
            boolean freezeDue = !row.getFrozenUntil().equals(Instant.EPOCH)
                    && (ACTIVE.equals(row.getState()) && !row.getFrozenFrom().isAfter(now)
                    || FROZEN.equals(row.getState()) && !row.getFrozenUntil().isAfter(now));
            if (freezeDue || !row.getCurrentPeriodEnd().isAfter(now)) {
                out.add(row.getId());
            }
        }
        return out;
    }

    @Transactional(readOnly = true)
    List<String> retries(Instant now) {
        List<Invoice> rows = invoices.findByStatusInAndNextAttemptAtLessThanEqual(
                Arrays.asList(FAILED), now);
        List<String> out = new ArrayList<String>();
        for (int iter = 0; iter < rows.size(); iter++) {
            out.add(rows.get(iter).getId());
        }
        return out;
    }

    /// Moves one membership on.
    ///
    /// @return the id of an invoice to charge, or null
    @Transactional(rollbackFor = IOException.class)
    String renew(String id, Instant now) throws IOException {
        Membership row = memberships.findById(id);
        if (row == null) {
            return null;
        }
        if (ACTIVE.equals(row.getState()) && !row.getFrozenUntil().equals(Instant.EPOCH)
                && !row.getFrozenFrom().isAfter(now)) {
            row.setState(FROZEN);
            row.setUpdatedAt(now);
        }
        if (FROZEN.equals(row.getState()) && !row.getFrozenUntil().isAfter(now)) {
            thaw(row, now);
        }
        if (row.getCurrentPeriodEnd().isAfter(now) || FROZEN.equals(row.getState())) {
            // A frozen period is extended when the freeze ends, not billed.
            return null;
        }
        MembershipPlan plan = plans.findById(row.getPlanId());
        if (plan == null || Household.counted(plan)) {
            row.setState(EXPIRED);
            row.setUpdatedAt(now);
            return null;
        }
        if (row.isCancelAtPeriodEnd() || PAST_DUE.equals(row.getState())) {
            // A member who has not paid the last period is not billed a
            // new one on top: the membership lapses, the debt stays owed.
            row.setState(row.isCancelAtPeriodEnd() ? CANCELLED : EXPIRED);
            row.setUpdatedAt(now);
            return null;
        }
        Instant start = row.getCurrentPeriodEnd();
        if (invoices.existsByMembershipIdAndPeriodStart(row.getId(), start)) {
            return null;
        }
        Member member = members.findById(row.getMemberId());
        String method = payments.defaultMethod(row.getAccount());
        Instant end = plusMonths(start, plan.getPeriodMonths());
        row.setCurrentPeriodEnd(end);
        row.setUpdatedAt(now);
        Invoice bill = invoice(row, plan, member, start, end, method == null ? "" : method);
        return Payments.CASH.equals(bill.getMethodId()) ? null : bill.getId();
    }

    // ------------------------------------------------------------- internals

    private Invoice invoice(Membership row, MembershipPlan plan, Member member, Instant start,
            Instant end, String methodId) throws IOException {
        Invoice bill = new Invoice();
        bill.setId(Ids.next());
        bill.setAccount(row.getAccount());
        bill.setMembershipId(row.getId());
        bill.setMemberId(row.getMemberId());
        bill.setDescription(cut(plan.getName() + " - " + (member == null ? "" : member.getName()),
                200));
        bill.setAmountCents(plan.getPriceCents());
        bill.setCurrency(plan.getCurrency());
        bill.setStatus(OPEN);
        bill.setPeriodStart(start);
        bill.setPeriodEnd(end);
        bill.setMethodId(methodId);
        bill.setNextAttemptAt(NEVER);
        bill.setCreatedAt(Instant.now());
        invoices.insert(bill);
        apply(bill);
        return bill;
    }

    /// Takes the account's unspent credit off an invoice, oldest first. A
    /// credit larger than what is left is split, so the rest stays to spend.
    private void apply(Invoice bill) throws IOException {
        List<Credit> open = credits.findByAccountAndInvoiceIdOrderByCreatedAtAsc(bill.getAccount(),
                "");
        long left = bill.getAmountCents();
        long used = 0;
        for (int iter = 0; iter < open.size() && left > 0; iter++) {
            Credit row = open.get(iter);
            if (row.getAmountCents() > left) {
                Credit rest = new Credit();
                rest.setId(Ids.next());
                rest.setAccount(row.getAccount());
                rest.setAmountCents(row.getAmountCents() - left);
                rest.setReason(row.getReason());
                rest.setCreatedAt(row.getCreatedAt());
                credits.insert(rest);
                row.setAmountCents(left);
            }
            row.setInvoiceId(bill.getId());
            left -= row.getAmountCents();
            used += row.getAmountCents();
        }
        bill.setCreditCents(used);
    }

    /// Gives back the credit an invoice that will not be paid had taken.
    private void release(Invoice bill) {
        List<Credit> used = credits.findByAccountAndInvoiceIdOrderByCreatedAtAsc(bill.getAccount(),
                bill.getId());
        for (int iter = 0; iter < used.size(); iter++) {
            used.get(iter).setInvoiceId("");
        }
        bill.setCreditCents(0);
    }

    private void give(String account, long cents, String reason, Instant now) throws IOException {
        Credit row = new Credit();
        row.setId(Ids.next());
        row.setAccount(account);
        row.setAmountCents(cents);
        row.setReason(cut(reason, 200));
        row.setCreatedAt(now);
        credits.insert(row);
    }

    public MembershipDto toDto(Membership row) {
        MembershipPlan plan = plans.findById(row.getPlanId());
        Member member = members.findById(row.getMemberId());
        return new MembershipDto(row.getId(), row.getMemberId(),
                member == null ? "" : member.getName(), row.getPlanId(),
                plan == null ? "" : plan.getName(), plan == null ? "" : plan.getKind(),
                row.getState(), row.getStartedAt().toEpochMilli(),
                row.getCurrentPeriodEnd().toEpochMilli(), row.getCreditsLeft(),
                row.getFrozenFrom().toEpochMilli(), row.getFrozenUntil().toEpochMilli(),
                row.isCancelAtPeriodEnd());
    }

    private List<InvoiceDto> toDtos(List<Invoice> rows) {
        List<InvoiceDto> out = new ArrayList<InvoiceDto>();
        for (int iter = 0; iter < rows.size(); iter++) {
            out.add(toDto(rows.get(iter)));
        }
        return out;
    }

    public InvoiceDto toDto(Invoice row) {
        Member member = members.findById(row.getMemberId());
        String label = Payments.CASH.equals(row.getMethodId()) ? "Cash"
                : row.getMethodId().length() == 0 ? "" : "Card";
        return new InvoiceDto(row.getId(), member == null ? "" : member.getName(),
                row.getDescription(), row.getAmountCents(), row.getCreditCents(),
                row.getCurrency(), row.getStatus(), row.getPeriodStart().toEpochMilli(),
                row.getPeriodEnd().toEpochMilli(), row.getAttempts(),
                NEVER.equals(row.getNextAttemptAt()) ? 0 : row.getNextAttemptAt().toEpochMilli(),
                label, row.getFailure(), row.getCreatedAt().toEpochMilli(),
                row.getPaidAt().toEpochMilli());
    }

    /// The same instant some calendar months on, in UTC; the 31st of a month
    /// lands on the last day of a shorter one.
    public static Instant plusMonths(Instant at, int months) {
        return at.atZone(ZoneOffset.UTC).plusMonths(months).toInstant();
    }

    /// A `yyyy-MM-dd` from the client, or 400.
    public static LocalDate date(String text) {
        try {
            return LocalDate.parse(text);
        } catch (RuntimeException bad) {
            throw new ResponseStatusException(400, "Dates are written yyyy-mm-dd");
        }
    }

    private static Instant start(LocalDate day) {
        return Instant.ofEpochMilli(day.toEpochDay() * DAY);
    }

    private static String cut(String text, int max) {
        return text.length() > max ? text.substring(0, max) : text;
    }
}
