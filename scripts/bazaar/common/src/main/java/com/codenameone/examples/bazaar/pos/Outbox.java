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
package com.codenameone.examples.bazaar.pos;

import com.codenameone.examples.bazaar.api.PosLineDto;
import com.codenameone.examples.bazaar.api.PosSaleDto;
import com.codenameone.examples.bazaar.api.PosSaleResultDto;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// The sales a register made and what became of them. Pure: storage and
/// JSON are handed in, so the state machine is tested on its own.
///
/// A sale is written here, with the client id the register made for it,
/// before anything else happens -- before the receipt, before the network
/// is tried. Then:
///
/// - PENDING: waiting to go. [#due] picks the ones whose time has come.
/// - SYNCING: in a request ([#sending]). Not stored: a register restarted
///   mid-request finds it PENDING again and sends it again, which is safe,
///   because the server answers a client id it already has as DUPLICATE.
/// - SYNCED / DUPLICATE: the server has it. Both are success.
/// - ERROR: the server refused it (a gift card that ran dry, say). Not
///   retried by itself: a person looks at it and retries ([#retry]).
///
/// A request that never got an answer puts its sales back to PENDING with a
/// later time, doubling from two seconds to at most five minutes.
public final class Outbox {
    public static final String PENDING = "PENDING";
    public static final String SYNCING = "SYNCING";
    public static final String SYNCED = "SYNCED";
    public static final String DUPLICATE = "DUPLICATE";
    public static final String ERROR = "ERROR";
    /// Refused only for want of a manager's approval: waits until a manager
    /// approves it at this register, then goes again under the same client id.
    public static final String APPROVAL = "APPROVAL";

    /// Most sales sent in one request.
    public static final int BATCH = 20;
    /// Finished sales kept for the outbox screen.
    public static final int KEEP_DONE = 200;
    static final String KEY = "pos.outbox";

    /// Where the outbox is kept: one text value under a key.
    public interface Persist {
        String read(String key);

        void write(String key, String value);
    }

    /// A sale to and from text.
    public interface Codec {
        String encode(PosSaleDto sale);

        PosSaleDto decode(String text);
    }

    /// One sale and its fate.
    public static final class Entry {
        /// Replaced, never edited, when a manager re-approves the sale.
        public PosSaleDto sale;
        public String state = PENDING;
        public int attempts;
        public long nextAt;
        public String orderId = "";
        public int number;
        public long totalCents;
        public long changeCents;
        public boolean oversold;
        public String message = "";

        Entry(PosSaleDto sale) {
            this.sale = sale;
        }

        public String clientId() {
            return sale.clientId();
        }

        /// Whether the server does not have it yet.
        public boolean open() {
            return PENDING.equals(state) || SYNCING.equals(state) || ERROR.equals(state)
                    || APPROVAL.equals(state);
        }
    }

    private final Persist persist;
    private final Codec codec;
    private final List<Entry> entries = new ArrayList<Entry>();

    public Outbox(Persist persist, Codec codec) {
        this.persist = persist;
        this.codec = codec;
        load();
    }

    public List<Entry> entries() {
        return new ArrayList<Entry>(entries);
    }

    public Entry find(String clientId) {
        for (int iter = 0; iter < entries.size(); iter++) {
            if (entries.get(iter).clientId().equals(clientId)) {
                return entries.get(iter);
            }
        }
        return null;
    }

    // tag::bazaar-outbox-add[]
    /// Records a sale; stored before this returns.
    public Entry add(PosSaleDto sale) {
        Entry existing = find(sale.clientId());
        if (existing != null) {
            return existing;
        }
        Entry e = new Entry(sale);
        entries.add(e);
        save();
        return e;
    }
    // end::bazaar-outbox-add[]

    /// Waiting or in flight.
    public int pending() {
        int n = 0;
        for (int iter = 0; iter < entries.size(); iter++) {
            String s = entries.get(iter).state;
            if (PENDING.equals(s) || SYNCING.equals(s)) {
                n++;
            }
        }
        return n;
    }

    /// Refused sales, those waiting for an approval among them.
    public int errors() {
        int n = 0;
        for (int iter = 0; iter < entries.size(); iter++) {
            String st = entries.get(iter).state;
            if (ERROR.equals(st) || APPROVAL.equals(st)) {
                n++;
            }
        }
        return n;
    }

    /// Sales waiting for a manager's approval.
    public int awaitingApproval() {
        int n = 0;
        for (int iter = 0; iter < entries.size(); iter++) {
            if (APPROVAL.equals(entries.get(iter).state)) {
                n++;
            }
        }
        return n;
    }

    /// Gives an APPROVAL sale the approval a manager just granted and sends
    /// it again now, under the same client id, so the server still keeps it
    /// once. False when the sale is not waiting for one.
    public boolean reapprove(String clientId, String approvalId) {
        Entry e = find(clientId);
        if (e == null || !APPROVAL.equals(e.state) || approvalId == null
                || approvalId.length() == 0) {
            return false;
        }
        PosSaleDto s = e.sale;
        e.sale = new PosSaleDto(s.clientId(), s.shiftId(), s.customerId(), s.email(),
                s.discountCode(), s.note(), s.createdAt(), s.lines(), s.tenders(),
                s.staffUsername(), s.tipCents(), approvalId);
        e.state = PENDING;
        e.attempts = 0;
        e.nextAt = 0;
        e.message = "";
        save();
        return true;
    }

    /// The PENDING sales whose time has come, oldest first, at most [#BATCH].
    public List<Entry> due(long now) {
        List<Entry> out = new ArrayList<Entry>();
        for (int iter = 0; iter < entries.size() && out.size() < BATCH; iter++) {
            Entry e = entries.get(iter);
            if (PENDING.equals(e.state) && e.nextAt <= now) {
                out.add(e);
            }
        }
        return out;
    }

    /// The earliest time a PENDING sale may go, or -1 when none waits.
    public long nextAttempt() {
        long best = -1;
        for (int iter = 0; iter < entries.size(); iter++) {
            Entry e = entries.get(iter);
            if (PENDING.equals(e.state) && (best < 0 || e.nextAt < best)) {
                best = e.nextAt;
            }
        }
        return best;
    }

    public void sending(List<Entry> batch) {
        for (int iter = 0; iter < batch.size(); iter++) {
            batch.get(iter).state = SYNCING;
        }
    }

    /// The server answered `batch` with `results`, matched by client id. A
    /// sale the answer does not mention goes back to wait.
    public void answered(List<Entry> batch, List<PosSaleResultDto> results, long now) {
        Map<String, PosSaleResultDto> byId = new HashMap<String, PosSaleResultDto>();
        for (int iter = 0; results != null && iter < results.size(); iter++) {
            PosSaleResultDto r = results.get(iter);
            if (r != null && r.clientId() != null) {
                byId.put(r.clientId(), r);
            }
        }
        for (int iter = 0; iter < batch.size(); iter++) {
            Entry e = batch.get(iter);
            PosSaleResultDto r = byId.get(e.clientId());
            if (r == null) {
                backoff(e, now);
                continue;
            }
            // tag::bazaar-outbox-answer[]
            String status = r.status() == null ? "" : r.status();
            if ("OK".equalsIgnoreCase(status)) {
                e.state = SYNCED;
            } else if (DUPLICATE.equalsIgnoreCase(status)) {
                e.state = DUPLICATE;
            } else if (APPROVAL.equalsIgnoreCase(status)) {
                e.state = APPROVAL;
            } else {
                e.state = ERROR;
            }
            // end::bazaar-outbox-answer[]
            e.attempts++;
            e.orderId = r.orderId() == null ? "" : r.orderId();
            e.number = r.number();
            e.totalCents = r.totalCents();
            e.changeCents = r.changeCents();
            e.oversold = r.oversold();
            e.message = r.message() == null ? "" : r.message();
        }
        prune();
        save();
    }

    /// The request for `batch` got no answer: every sale waits longer.
    public void unanswered(List<Entry> batch, long now) {
        for (int iter = 0; iter < batch.size(); iter++) {
            backoff(batch.get(iter), now);
        }
        save();
    }

    /// Sends an ERROR sale again, now.
    public boolean retry(String clientId) {
        Entry e = find(clientId);
        if (e == null || !ERROR.equals(e.state)) {
            return false;
        }
        e.state = PENDING;
        e.attempts = 0;
        e.nextAt = 0;
        save();
        return true;
    }

    /// Every PENDING sale may go now: the network just came back.
    public void wake() {
        for (int iter = 0; iter < entries.size(); iter++) {
            Entry e = entries.get(iter);
            if (PENDING.equals(e.state)) {
                e.nextAt = 0;
            }
        }
    }

    /// Two seconds, doubling per attempt, at most five minutes.
    public static long delay(int attempts) {
        long d = 2000L;
        for (int iter = 1; iter < attempts && d < 300000L; iter++) {
            d *= 2;
        }
        return Math.min(d, 300000L);
    }

    private void backoff(Entry e, long now) {
        e.state = PENDING;
        e.attempts++;
        e.nextAt = now + delay(e.attempts);
    }

    /// Units per variant of sales the server does not have yet, which a
    /// fresh catalog does not count.
    public Map<String, Integer> unsyncedUnits() {
        Map<String, Integer> out = new HashMap<String, Integer>();
        for (int iter = 0; iter < entries.size(); iter++) {
            Entry e = entries.get(iter);
            if (!e.open() || e.sale.lines() == null) {
                continue;
            }
            for (int l = 0; l < e.sale.lines().size(); l++) {
                PosLineDto line = e.sale.lines().get(l);
                Integer before = out.get(line.variantId());
                out.put(line.variantId(), Integer.valueOf(
                        (before == null ? 0 : before.intValue()) + line.quantity()));
            }
        }
        return out;
    }

    private void prune() {
        int done = 0;
        for (int iter = entries.size() - 1; iter >= 0; iter--) {
            if (!entries.get(iter).open()) {
                done++;
                if (done > KEEP_DONE) {
                    entries.remove(iter);
                }
            }
        }
    }

    // --- storage: one line per sale, tab separated, the sale's JSON last.

    void save() {
        StringBuilder out = new StringBuilder();
        for (int iter = 0; iter < entries.size(); iter++) {
            Entry e = entries.get(iter);
            String state = SYNCING.equals(e.state) ? PENDING : e.state;
            out.append(state).append('\t').append(e.attempts).append('\t').append(e.nextAt)
                    .append('\t').append(flat(e.orderId)).append('\t').append(e.number)
                    .append('\t').append(e.totalCents).append('\t').append(e.changeCents)
                    .append('\t').append(e.oversold ? '1' : '0').append('\t')
                    .append(flat(e.message)).append('\t').append(flat(codec.encode(e.sale)))
                    .append('\n');
        }
        persist.write(KEY, out.toString());
    }

    private void load() {
        entries.clear();
        String text = persist.read(KEY);
        if (text == null || text.length() == 0) {
            return;
        }
        int start = 0;
        while (start < text.length()) {
            int end = text.indexOf('\n', start);
            if (end < 0) {
                end = text.length();
            }
            String line = text.substring(start, end);
            start = end + 1;
            String[] f = split(line, 10);
            if (f == null) {
                continue;
            }
            PosSaleDto sale = codec.decode(f[9]);
            if (sale == null || sale.clientId() == null) {
                continue;
            }
            Entry e = new Entry(sale);
            e.state = SYNCING.equals(f[0]) ? PENDING : f[0];
            e.attempts = (int) num(f[1]);
            e.nextAt = num(f[2]);
            e.orderId = f[3];
            e.number = (int) num(f[4]);
            e.totalCents = num(f[5]);
            e.changeCents = num(f[6]);
            e.oversold = "1".equals(f[7]);
            e.message = f[8];
            entries.add(e);
        }
    }

    private static String flat(String s) {
        if (s == null) {
            return "";
        }
        return s.replace('\t', ' ').replace('\n', ' ').replace('\r', ' ');
    }

    private static String[] split(String line, int fields) {
        String[] out = new String[fields];
        int start = 0;
        for (int iter = 0; iter < fields - 1; iter++) {
            int tab = line.indexOf('\t', start);
            if (tab < 0) {
                return null;
            }
            out[iter] = line.substring(start, tab);
            start = tab + 1;
        }
        out[fields - 1] = line.substring(start);
        return out;
    }

    private static long num(String s) {
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException bad) {
            return 0;
        }
    }
}
