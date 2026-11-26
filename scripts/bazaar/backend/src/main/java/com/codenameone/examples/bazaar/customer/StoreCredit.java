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
package com.codenameone.examples.bazaar.customer;

import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Scheduled;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.bazaar.Ids;
import com.codenameone.examples.bazaar.api.CreditGrantDto;
import com.codenameone.examples.bazaar.cart.Codes;
import com.codenameone.examples.bazaar.domain.AuditEvent;
import com.codenameone.examples.bazaar.domain.Customer;
import com.codenameone.examples.bazaar.domain.StoreCreditGrant;
import com.codenameone.examples.bazaar.domain.StoreCreditUse;
import com.codenameone.examples.bazaar.repository.AuditEventRepository;
import com.codenameone.examples.bazaar.repository.CustomerRepository;
import com.codenameone.examples.bazaar.repository.StoreCreditGrantRepository;
import com.codenameone.examples.bazaar.repository.StoreCreditUseRepository;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/// Store credit as a ledger of grants, each with its own expiry.
///
/// Every change to a customer's credit comes through here: staff adjusting
/// it, a return refunded to credit, a layaway cancelled, loyalty points
/// redeemed at the register, and the checkout and the register spending it.
/// `Customer.storeCreditCents` stays the sum of what the grants have left,
/// so the screens that show a balance read one column.
///
/// Spending takes from the grant that lapses first ([CreditMath]) and records
/// what each order took from each grant, so a cancellation or refund puts it
/// back on the same grants, expiry and all. [#sweep] zeroes grants that
/// lapsed. Every method joins the caller's transaction.
@Component
public class StoreCredit {
    /// The longest a grant may run: ten years.
    static final long MAX_LIFE_MILLIS = 3650L * 86400000L;

    private final StoreCreditGrantRepository grants;
    private final StoreCreditUseRepository uses;
    private final CustomerRepository customers;
    private final AuditEventRepository audits;

    public StoreCredit(StoreCreditGrantRepository grants, StoreCreditUseRepository uses,
            CustomerRepository customers, AuditEventRepository audits) {
        this.grants = grants;
        this.uses = uses;
        this.customers = customers;
        this.audits = audits;
    }

    /// Gives a customer `cents` of credit that lapses at `expiresAt` (null or
    /// `Instant.EPOCH` for never).
    @Transactional(rollbackFor = IOException.class)
    public StoreCreditGrant grant(String storeId, String customerId, long cents, Instant expiresAt,
            String reason) throws IOException {
        if (cents <= 0) {
            throw new ResponseStatusException(400, "Credit is a positive amount");
        }
        Instant now = Instant.now();
        Instant until = expiresAt == null ? Instant.EPOCH : expiresAt;
        if (until.toEpochMilli() > 0 && (!until.isAfter(now)
                || until.toEpochMilli() - now.toEpochMilli() > MAX_LIFE_MILLIS)) {
            throw new ResponseStatusException(400, "Credit expires in the future, within ten years");
        }
        StoreCreditGrant g = new StoreCreditGrant();
        g.setId(Ids.next());
        g.setStoreId(storeId);
        g.setCustomerId(customerId);
        g.setAmountCents(cents);
        g.setRemainingCents(cents);
        g.setExpiresAt(until);
        g.setReason(Codes.cut(reason == null ? "" : reason, 200));
        g.setCreatedAt(now);
        grants.insert(g);
        customers.addCredit(customerId, cents);
        return g;
    }

    /// What `customer` can spend now: the grants that have not lapsed.
    @Transactional(readOnly = true)
    public long available(Customer customer) {
        if (customer == null) {
            return 0;
        }
        List<StoreCreditGrant> mine = grants.findByCustomerId(customer.getId());
        long[][] cols = columns(mine);
        long usable = CreditMath.available(cols[0], cols[1], System.currentTimeMillis())
                + untracked(customer, mine);
        return Math.max(0, Math.min(usable, customer.getStoreCreditCents()));
    }

    /// Spends `cents` of a customer's credit for `orderId` (empty for an
    /// adjustment by staff), from the grants that lapse first.
    ///
    /// @return false, taking nothing, when the customer holds less
    @Transactional(rollbackFor = IOException.class)
    public boolean take(String customerId, long cents, String orderId) throws IOException {
        if (cents <= 0) {
            return true;
        }
        List<StoreCreditGrant> mine = grants.findByCustomerId(customerId);
        long loose = untracked(customers.findById(customerId), mine);
        if (loose > 0) {
            // A balance no grant accounts for (written before grants existed,
            // or straight into the table) becomes a grant that never lapses,
            // so it can be spent and given back like any other.
            StoreCreditGrant g = new StoreCreditGrant();
            g.setId(Ids.next());
            g.setStoreId(customers.findById(customerId).getStoreId());
            g.setCustomerId(customerId);
            g.setAmountCents(loose);
            g.setRemainingCents(loose);
            g.setExpiresAt(Instant.EPOCH);
            g.setReason("Balance");
            g.setCreatedAt(Instant.EPOCH);
            grants.insert(g);
            mine = grants.findByCustomerId(customerId);
        }
        long[][] cols = columns(mine);
        long[] parts = CreditMath.plan(cols[0], cols[1], cols[2], System.currentTimeMillis(), cents);
        if (parts == null) {
            return false;
        }
        if (customers.debitCreditIf(customerId, cents) != 1) {
            return false;
        }
        Instant now = Instant.now();
        for (int iter = 0; iter < parts.length; iter++) {
            if (parts[iter] == 0) {
                continue;
            }
            StoreCreditGrant g = mine.get(iter);
            if (grants.takeIf(g.getId(), parts[iter]) != 1) {
                throw new ResponseStatusException(409, "The store credit changed; try again");
            }
            StoreCreditUse u = new StoreCreditUse();
            u.setId(Ids.next());
            u.setGrantId(g.getId());
            u.setCustomerId(customerId);
            u.setOrderId(orderId == null ? "" : orderId);
            u.setAmountCents(parts[iter]);
            u.setCreatedAt(now);
            uses.insert(u);
        }
        return true;
    }

    /// The part of the customer's cached balance that no grant holds.
    private static long untracked(Customer c, List<StoreCreditGrant> mine) {
        if (c == null) {
            return 0;
        }
        long held = 0;
        for (int iter = 0; iter < mine.size(); iter++) {
            held += mine.get(iter).getRemainingCents();
        }
        return Math.max(0, c.getStoreCreditCents() - held);
    }

    /// What `orderId` took from credit and has not had back.
    @Transactional(readOnly = true)
    public long spentOn(String orderId) {
        List<StoreCreditUse> taken = uses.findByOrderId(orderId);
        long sum = 0;
        for (int iter = 0; iter < taken.size(); iter++) {
            sum += taken.get(iter).getAmountCents() - taken.get(iter).getReturnedCents();
        }
        return sum;
    }

    /// Puts back up to `maxCents` of what `orderId` took, on the grants it came
    /// from (a grant that lapsed meanwhile gets it back and lapses again at
    /// the next sweep). Answers how much went back.
    @Transactional(rollbackFor = IOException.class)
    public long restore(String orderId, long maxCents) throws IOException {
        List<StoreCreditUse> taken = uses.findByOrderId(orderId);
        long left = maxCents;
        long back = 0;
        for (int iter = taken.size() - 1; iter >= 0 && left > 0; iter--) {
            StoreCreditUse u = taken.get(iter);
            long open = u.getAmountCents() - u.getReturnedCents();
            long give = Math.min(open, left);
            if (give <= 0) {
                continue;
            }
            grants.giveBack(u.getGrantId(), give);
            customers.addCredit(u.getCustomerId(), give);
            u.setReturnedCents(u.getReturnedCents() + give);
            uses.save(u);
            left -= give;
            back += give;
        }
        return back;
    }

    /// A customer's grants, the ones that lapse first first.
    @Transactional(readOnly = true)
    public List<CreditGrantDto> grantsOf(String customerId) {
        List<StoreCreditGrant> mine = grants.findByCustomerId(customerId);
        long[][] cols = columns(mine);
        int[] order = CreditMath.order(cols[1], cols[2]);
        List<CreditGrantDto> out = new ArrayList<CreditGrantDto>();
        for (int k = 0; k < order.length; k++) {
            StoreCreditGrant g = mine.get(order[k]);
            out.add(new CreditGrantDto(g.getId(), g.getAmountCents(), g.getRemainingCents(),
                    g.getExpiresAt().toEpochMilli(), g.getReason(),
                    g.getCreatedAt().toEpochMilli()));
        }
        return out;
    }

    /// Moves every grant and use of `from` to `to` (a merge); the balance
    /// moves with them.
    @Transactional(rollbackFor = IOException.class)
    public void moveAll(String from, String to, long balance) throws IOException {
        grants.moveAll(from, to);
        uses.moveAll(from, to);
        if (balance > 0) {
            customers.addCredit(to, balance);
        }
    }

    @Scheduled(fixedDelay = 3600000, initialDelay = 300000, lock = "bazaar-credit-expiry")
    public void expireJob() throws IOException {
        sweep(System.currentTimeMillis());
    }

    /// Zeroes every grant that lapsed by `now` and takes what it had left off
    /// its customer's balance; answers how many cents lapsed. Public so that
    /// tests can run the clock forward.
    @Transactional(rollbackFor = IOException.class)
    public long sweep(long now) throws IOException {
        List<StoreCreditGrant> lapsed = grants.lapsed(Instant.EPOCH, Instant.ofEpochMilli(now));
        long total = 0;
        for (int iter = 0; iter < lapsed.size(); iter++) {
            StoreCreditGrant g = lapsed.get(iter);
            long left = g.getRemainingCents();
            if (left <= 0 || grants.takeIf(g.getId(), left) != 1) {
                continue;
            }
            Customer c = customers.findById(g.getCustomerId());
            if (c != null) {
                c.setStoreCreditCents(Math.max(0, c.getStoreCreditCents() - left));
                c.setUpdatedAt(Instant.ofEpochMilli(now));
                customers.save(c);
            }
            AuditEvent e = new AuditEvent();
            e.setId(Ids.next());
            e.setStoreId(g.getStoreId());
            e.setUsername("system");
            e.setAction("STORE_CREDIT_EXPIRED");
            e.setSubject(g.getCustomerId());
            e.setDetail(Codes.cut(left + " cents lapsed: " + g.getReason(), 500));
            e.setCreatedAt(Instant.ofEpochMilli(now));
            audits.insert(e);
            total += left;
        }
        return total;
    }

    /// remaining, expiry (0 for never) and creation time, column by column.
    private static long[][] columns(List<StoreCreditGrant> rows) {
        long[] remaining = new long[rows.size()];
        long[] expires = new long[rows.size()];
        long[] created = new long[rows.size()];
        for (int iter = 0; iter < rows.size(); iter++) {
            StoreCreditGrant g = rows.get(iter);
            remaining[iter] = g.getRemainingCents();
            expires[iter] = g.getExpiresAt().toEpochMilli();
            created[iter] = g.getCreatedAt().toEpochMilli();
        }
        return new long[][] {remaining, expires, created};
    }
}
