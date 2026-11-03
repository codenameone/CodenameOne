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
import com.codename1.backend.annotations.Autowired;
import com.codename1.backend.test.BackendTest;
import com.codenameone.examples.pulse.People;
import com.codenameone.examples.pulse.account.Accounts;
import com.codenameone.examples.pulse.api.CardDto;
import com.codenameone.examples.pulse.api.JoinDto;
import com.codenameone.examples.pulse.api.MembershipDto;
import com.codenameone.examples.pulse.domain.Invoice;
import com.codenameone.examples.pulse.domain.Member;
import com.codenameone.examples.pulse.domain.Membership;
import com.codenameone.examples.pulse.pay.Payments;
import com.codenameone.examples.pulse.repository.InvoiceRepository;
import com.codenameone.examples.pulse.repository.MemberRepository;
import com.codenameone.examples.pulse.repository.MembershipRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Getting paid: joining, renewing, the dunning ladder and proration.
@BackendTest
class BillingTest {
    private static final String GOOD = "4242424242424242";
    private static final String FAILS_LATER = "4000000000000341";
    private static final long DAY = 86400000L;

    @Autowired
    private Accounts accounts;
    @Autowired
    private MemberRepository members;
    @Autowired
    private MembershipRepository memberships;
    @Autowired
    private InvoiceRepository invoices;
    @Autowired
    private Payments payments;
    @Autowired
    private Billing billing;

    private String card(String account, String number) throws Exception {
        String setup = payments.startSetup(account, "Card Holder").id();
        return payments.completeSetup(account, setup, new CardDto(number, 12, 2035, "123",
                "Card Holder")).id();
    }

    private String signed(String name) throws Exception {
        String account = People.account(accounts, name, false);
        Member self = People.self(members, account);
        self.setWaiverAt(Instant.now());
        People.tx(() -> { members.save(self); return null; });
        return account;
    }

    @Test
    void theUnusedPartOfAPeriodIsGivenBackToTheCent() {
        Instant end = Instant.parse("2026-02-01T00:00:00Z");
        // Half of January's 31 days left.
        assertEquals(1500, Billing.prorate(3000, end, 1, Instant.parse("2026-01-16T12:00:00Z")));
        assertEquals(3000, Billing.prorate(3000, end, 1, Instant.parse("2025-12-20T00:00:00Z")));
        assertEquals(0, Billing.prorate(3000, end, 1, end));
        // Rounded down: never more back than was paid for.
        assertEquals(96, Billing.prorate(100, end, 1, Instant.parse("2026-01-02T00:00:00Z")));
        // An annual plan.
        assertEquals(5999, Billing.prorate(11999, Instant.parse("2027-01-01T00:00:00Z"), 12,
                Instant.parse("2026-07-02T12:00:00Z")));
    }

    @Test
    void joiningChargesTheFirstPeriod() throws Exception {
        String account = signed("Joiner");
        String good = card(account, GOOD);
        MembershipDto joined = billing.join(account, new JoinDto(null, "plan-unlimited", good));
        assertEquals(Billing.ACTIVE, joined.state());
        List<Invoice> bills = People.tx(() -> invoices.findByAccountOrderByCreatedAtDesc(account));
        assertEquals(1, bills.size());
        assertEquals(Billing.PAID, bills.get(0).getStatus());

        // A second join on the same plan is refused; switching plans credits
        // the unused time against the new one.
        ResponseStatusException again = assertThrows(ResponseStatusException.class,
                () -> billing.join(account, new JoinDto(null, "plan-unlimited", good)));
        assertEquals(409, again.getStatus());
        billing.join(account, new JoinDto(null, "plan-annual", good));
        Invoice second = People.tx(() -> invoices.findByAccountOrderByCreatedAtDesc(account)).get(0);
        assertTrue(second.getCreditCents() > 0, "credit for the unused month");
        assertEquals(Billing.PAID, second.getStatus());
    }

    @Test
    void aDeclinedJoinLeavesNoMembership() throws Exception {
        String account = signed("Declined");
        String bad = card(account, FAILS_LATER);
        ResponseStatusException declined = assertThrows(ResponseStatusException.class,
                () -> billing.join(account, new JoinDto(null, "plan-unlimited", bad)));
        assertEquals(402, declined.getStatus());
        List<MembershipDto> held = billing.memberships(account);
        assertEquals(1, held.size());
        assertEquals(Billing.CANCELLED, held.get(0).state());
        assertEquals(Billing.VOID, People.tx(() -> invoices.findByAccountOrderByCreatedAtDesc(account)).get(0)
                .getStatus());
    }

    @Test
    void aFailingRenewalClimbsTheLadderAndIsPaidByHand() throws Exception {
        String account = signed("Ladder");
        card(account, FAILS_LATER);
        Member self = People.self(members, account);
        Membership row = People.membership(account, self.getId(), "plan-unlimited", 0);
        row.setCurrentPeriodEnd(Instant.now().minusMillis(60000L));
        People.tx(() -> { memberships.insert(row); return null; });

        String bill = billing.renew(row.getId(), Instant.now());
        assertTrue(bill != null);
        billing.collect(bill, false);
        Invoice first = People.tx(() -> invoices.findById(bill));
        assertEquals(Billing.FAILED, first.getStatus());
        assertEquals(1, first.getAttempts());
        long wait = first.getNextAttemptAt().toEpochMilli() - System.currentTimeMillis();
        assertTrue(wait > DAY - 60000L && wait <= DAY, "a day to the first retry");
        // One failure is a grace period: the member can still come in.
        assertEquals(Billing.ACTIVE, People.tx(() -> memberships.findById(row.getId())).getState());

        // Renewing again does not bill the same period twice.
        assertNull(billing.renew(row.getId(), Instant.now()));

        billing.collect(bill, false);
        assertEquals(Billing.PAST_DUE, People.tx(() -> memberships.findById(row.getId())).getState());
        billing.collect(bill, false);
        billing.collect(bill, false);
        Invoice last = People.tx(() -> invoices.findById(bill));
        assertEquals(4, last.getAttempts());
        assertEquals(Billing.NEVER, last.getNextAttemptAt(), "no more retries");

        String good = card(account, GOOD);
        billing.pay(account, bill, good);
        assertEquals(Billing.PAID, People.tx(() -> invoices.findById(bill)).getStatus());
        assertEquals(Billing.ACTIVE, People.tx(() -> memberships.findById(row.getId())).getState());
    }

    @Test
    void aFrozenMembershipIsNotBilledAndThawsLonger() throws Exception {
        String account = signed("Frozen");
        card(account, GOOD);
        Member self = People.self(members, account);
        Membership row = People.membership(account, self.getId(), "plan-unlimited", 0);
        Instant now = Instant.now();
        row.setState(Billing.FROZEN);
        row.setFrozenFrom(now.minusMillis(10L * DAY));
        row.setFrozenUntil(now.plusMillis(5L * DAY));
        row.setCurrentPeriodEnd(now.minusMillis(60000L));
        People.tx(() -> { memberships.insert(row); return null; });
        assertNull(billing.renew(row.getId(), now));
        assertTrue(People.tx(() -> invoices.findByAccountOrderByCreatedAtDesc(account)).isEmpty());

        MembershipDto thawed = billing.unfreeze(account, row.getId());
        assertEquals(Billing.ACTIVE, thawed.state());
        // Ten frozen days are added to the period.
        assertTrue(thawed.currentPeriodEnd() > now.plusMillis(9L * DAY).toEpochMilli());
    }
}
