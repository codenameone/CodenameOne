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
package com.codenameone.examples.bazaar.pay;

import com.codename1.backend.DataSource;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Scheduled;
import com.codenameone.examples.bazaar.Days;
import com.codenameone.examples.bazaar.Rows;
import com.codenameone.examples.bazaar.checkout.Checkouts;
import com.codenameone.examples.bazaar.domain.Payment;
import com.codenameone.examples.bazaar.domain.StoreOrder;
import com.codenameone.examples.bazaar.order.Orders;
import com.codenameone.examples.bazaar.repository.StoreOrderRepository;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// Settles the card holds a crash left behind, and holds the processor's
/// records against the orders' once a day.
///
/// **Crash recovery.** Checkout writes a `payment_attempt` row before it asks
/// the processor for a hold and deletes it once the order records the hold
/// ([Payments#authorize] with `journaled`). A row older than [#GRACE_MILLIS]
/// therefore belongs to a checkout that stopped part way, and [#recover]
/// settles it by what the order shows:
///
/// - the order recorded its payment, or was settled some other way: nothing
///   to do;
/// - the payment table has the hold and the order is still waiting: the
///   order is **completed**, as the checkout would have;
/// - the order has no payment, or there is no order: the hold is
///   **voided**. The processor is asked for the hold again with the same
///   idempotency key, which answers the first hold if it was made, and that
///   hold is released; the order's stock, gift card, store credit and
///   discount uses go back.
///
/// **Daily reconciliation.** [#reconcileDay] lists the processor's payments
/// of a UTC day ([PaymentProvider#charges]) and matches them by reference to
/// the payment table: matched, at the processor only, here only, or with a
/// different amount. The counts and the references that differ are kept in
/// `payment_reconciliation`, one row per day.
@Component
public class PaymentRecovery {
    public static final String STARTED = "STARTED";
    static final String RECONCILING = "RECONCILING";
    /// How long a checkout may take between its journal row and its order
    /// recording the hold before the row is taken as left behind.
    public static final long GRACE_MILLIS = 15L * 60000L;
    private static final int BATCH = 100;

    private final DataSource db;
    private final Payments payments;
    private final PaymentProvider provider;
    private final Checkouts checkouts;
    private final StoreOrderRepository orders;

    public PaymentRecovery(DataSource db, Payments payments, PaymentProvider provider,
            Checkouts checkouts, StoreOrderRepository orders) {
        this.db = db;
        this.payments = payments;
        this.provider = provider;
        this.checkouts = checkouts;
        this.orders = orders;
    }

    /// What one pass of [#recover] did.
    public static final class Outcome {
        /// Orders finished with the hold the processor gave.
        public int completed;
        /// Holds released, with their orders taken back.
        public int voided;
        /// Attempts whose orders had recorded the payment after all.
        public int recorded;
        /// Attempts left for the next pass: the processor did not answer.
        public int deferred;
    }

    @Scheduled(fixedDelay = 300000, initialDelay = 60000, lock = "bazaar-payment-recovery")
    public void recoverJob() throws IOException {
        recover(System.currentTimeMillis());
    }

    /// Settles every attempt older than [#GRACE_MILLIS] at `now`.
    public Outcome recover(long now) throws IOException {
        Outcome out = new Outcome();
        List rows = db.query("SELECT * FROM payment_attempt WHERE status = ? AND created_at < ? "
                + "ORDER BY created_at", new Object[] {STARTED, Long.valueOf(now - GRACE_MILLIS)});
        for (int iter = 0; iter < rows.size() && iter < BATCH; iter++) {
            Map row = (Map) rows.get(iter);
            String orderId = Rows.text(row.get("order_id"));
            if (db.execute("UPDATE payment_attempt SET status = ? WHERE order_id = ? AND status = ?",
                    new Object[] {RECONCILING, orderId, STARTED}) != 1) {
                continue;
            }
            boolean settled = false;
            try {
                settle(row, orderId, out);
                settled = true;
            } catch (IOException | RuntimeException failed) {
                System.err.println("payment attempt for order " + orderId + " not settled: "
                        + failed.getMessage());
                out.deferred++;
            }
            if (settled) {
                payments.dropAttempt(orderId);
            } else {
                db.execute("UPDATE payment_attempt SET status = ? WHERE order_id = ?",
                        new Object[] {STARTED, orderId});
            }
        }
        return out;
    }

    private void settle(Map row, String orderId, Outcome out) throws IOException {
        StoreOrder order = orders.findById(orderId);
        Payment payment = payments.paymentOf(orderId);
        if (order != null && !Orders.PENDING.equals(order.getFinancial())) {
            out.recorded++;
            return;
        }
        if (order != null && payment != null) {
            if (Payments.AUTHORIZED.equals(payment.getStatus())
                    && checkouts.completeRecovered(orderId, payment.getProviderRef())) {
                out.completed++;
            } else {
                out.recorded++;
            }
            return;
        }
        if (payment != null) {
            // The payment was recorded and its order is gone: let it go.
            payments.voidOrder(orderId);
            out.voided++;
            return;
        }
        releaseOrphan(row);
        if (order != null) {
            checkouts.abandonRecovered(orderId);
        }
        out.voided++;
    }

    /// Asks for the hold again under its own idempotency key, which answers the
    /// first hold if the processor made one (or makes one now, which is let
    /// go at once), and releases it. A refusal means nothing was ever held.
    private void releaseOrphan(Map row) throws IOException {
        String reference;
        try {
            reference = provider.authorize(Rows.text(row.get("idem_key")),
                    Rows.text(row.get("customer")), Rows.text(row.get("token")),
                    Rows.number(row.get("amount_cents")), Rows.text(row.get("currency")),
                    "Bazaar order");
        } catch (PaymentDeclinedException declined) {
            return;
        }
        if (reference != null && Payments.atProcessor(reference)) {
            provider.release(reference);
        }
    }

    // ------------------------------------------------------ reconciliation

    /// What one day's reconciliation found.
    public static final class DayReport {
        public String day = "";
        public boolean listed;
        public int matched;
        public int providerOnly;
        public int ordersOnly;
        public int mismatched;
        public String details = "";
    }

    /// Reconciles yesterday (UTC) once it is over, if it was not yet.
    @Scheduled(fixedDelay = 3600000, initialDelay = 300000, lock = "bazaar-payment-reconcile")
    public void reconcileJob() throws IOException {
        long yesterday = Days.start(System.currentTimeMillis()) - Days.MILLIS;
        if (db.queryOne("SELECT report_day FROM payment_reconciliation WHERE report_day = ?",
                new Object[] {Days.iso(yesterday)}) == null) {
            reconcileDay(yesterday);
        }
    }

    /// Holds the processor's payments created on the UTC day `dayMillis`
    /// falls in against the payment table's, and keeps the result.
    public DayReport reconcileDay(long dayMillis) throws IOException {
        long from = Days.start(dayMillis);
        long to = from + Days.MILLIS;
        DayReport r = new DayReport();
        r.day = Days.iso(from);
        List<PaymentProvider.ProcessorCharge> listed = provider.charges(from, to);
        StringBuilder details = new StringBuilder();
        if (listed == null) {
            details.append(provider.name()).append(" cannot list its payments");
        } else {
            r.listed = true;
            Map<String, Map> ours = new HashMap<String, Map>();
            List rows = db.query("SELECT order_id, amount_cents, currency, provider_ref FROM payment "
                    + "WHERE created_at >= ? AND created_at < ?",
                    new Object[] {Long.valueOf(from), Long.valueOf(to)});
            for (int iter = 0; iter < rows.size(); iter++) {
                Map row = (Map) rows.get(iter);
                String ref = Rows.text(row.get("provider_ref"));
                if (Payments.atProcessor(ref)) {
                    ours.put(ref, row);
                }
            }
            for (int iter = 0; iter < listed.size(); iter++) {
                PaymentProvider.ProcessorCharge c = listed.get(iter);
                Map row = ours.remove(c.reference);
                if (row == null) {
                    // Wider than the day: a payment recorded a little after
                    // midnight still counts as matched.
                    row = db.queryOne("SELECT order_id, amount_cents, currency FROM payment "
                            + "WHERE provider_ref = ?", new Object[] {c.reference});
                }
                if (row == null) {
                    r.providerOnly++;
                    note(details, "processor only", c.reference);
                } else if (Rows.number(row.get("amount_cents")) != c.amountCents
                        || !Rows.text(row.get("currency")).equalsIgnoreCase(c.currency)) {
                    r.mismatched++;
                    note(details, "amount differs", c.reference);
                } else {
                    r.matched++;
                }
            }
            for (Map row : ours.values()) {
                r.ordersOnly++;
                note(details, "orders only", Rows.text(row.get("provider_ref")));
            }
        }
        r.details = details.toString();
        db.execute("DELETE FROM payment_reconciliation WHERE report_day = ?", new Object[] {r.day});
        db.execute("INSERT INTO payment_reconciliation (report_day, provider, matched, provider_only, "
                + "orders_only, mismatched, details, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                new Object[] {r.day, provider.name(), Integer.valueOf(r.matched),
                    Integer.valueOf(r.providerOnly), Integer.valueOf(r.ordersOnly),
                    Integer.valueOf(r.mismatched), r.details,
                    Long.valueOf(System.currentTimeMillis())});
        return r;
    }

    private static void note(StringBuilder details, String what, String reference) {
        if (details.length() > 8000) {
            return;
        }
        details.append(details.length() == 0 ? "" : "\n").append(what).append(": ")
                .append(reference);
    }
}
