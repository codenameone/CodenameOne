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
package com.codenameone.examples.bastion.books;

import com.codename1.backend.DataSource;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codename1.backend.annotations.Value;
import com.codenameone.examples.bastion.Ids;
import com.codenameone.examples.bastion.Refused;
import com.codenameone.examples.bastion.api.ApprovalDto;
import com.codenameone.examples.bastion.api.JournalEntryDto;
import com.codenameone.examples.bastion.api.JournalLegDto;
import com.codenameone.examples.bastion.audit.Audit;
import com.codenameone.examples.bastion.domain.JournalEntry;
import com.codenameone.examples.bastion.domain.LedgerAccount;
import com.codenameone.examples.bastion.ledger.AccountKinds;
import com.codenameone.examples.bastion.ledger.Leg;
import com.codenameone.examples.bastion.ledger.Ledger;
import com.codenameone.examples.bastion.repository.LedgerAccountRepository;
import com.codenameone.examples.bastion.repository.ProfileRepository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/// Maker-checker for the two things an administrator can do to money: a
/// manual adjustment of a customer account and the reversal of an entry.
///
/// What will be posted is fixed when the request is made -- the account,
/// the amount, the entry -- and the request is valued in USD. At or below
/// `bastion.approval.threshold-usd-cents` it posts at once; above it, it waits
/// PENDING until a **different** administrator approves it. The approval
/// claims the row with a conditional update (`WHERE status = 'PENDING'`)
/// before it posts, so two approvals arriving together execute it once.
/// Every step is appended to the audit chain in the transaction that takes it.
@Component
public class Approvals {
    public static final String ADJUSTMENT = "ADJUSTMENT";
    public static final String REVERSAL = "REVERSAL";
    public static final String PENDING = "PENDING";
    public static final String EXECUTED = "EXECUTED";
    public static final String REJECTED = "REJECTED";

    private final DataSource db;
    private final Ledger ledger;
    private final Books books;
    private final Audit audit;
    private final LedgerAccountRepository accounts;
    private final ProfileRepository profiles;
    private final long threshold;

    public Approvals(DataSource db, Ledger ledger, Books books, Audit audit,
            LedgerAccountRepository accounts, ProfileRepository profiles,
            @Value("${bastion.approval.threshold-usd-cents:100000}") String threshold) {
        this.db = db;
        this.ledger = ledger;
        this.books = books;
        this.audit = audit;
        this.accounts = accounts;
        this.profiles = profiles;
        this.threshold = Long.parseLong(threshold.trim());
    }

    public long threshold() {
        return threshold;
    }

    @Transactional(rollbackFor = Exception.class)
    public ApprovalDto adjust(String maker, String idempotencyKey, String accountId, long amount,
            String memo) throws IOException {
        boolean known = holdMaker(maker);
        String key = Ledger.clientKey(maker, idempotencyKey);
        LedgerAccount account = accountId == null ? null : accounts.findById(accountId);
        // Customer money only, and never an order's escrow: the bank's own
        // accounts move by the entries that explain them.
        if (account == null || !AccountKinds.isCustomer(account.getKind())
                || AccountKinds.HOLD.equals(account.getKind())) {
            throw Refused.with(404, "no-such-account", "No such account");
        }
        if (amount == 0) {
            throw Refused.with(400, "amount-cannot-zero", "An amount cannot be zero");
        }
        ApprovalDto earlier = byKey(key);
        if (earlier != null) {
            if (!ADJUSTMENT.equals(earlier.kind()) || !accountId.equals(earlier.accountId())
                    || earlier.amount() != amount) {
                throw Refused.with(422, "idempotency-key-reused", "That Idempotency-Key was already used for a different request");
            }
            return earlier;
        }
        long usd = books.priced(account.getAsset(), amount < 0 ? -amount : amount);
        return create(known, ADJUSTMENT, maker, accountId, "", account.getAsset(), amount, usd,
                clip(memo), key);
    }

    @Transactional(rollbackFor = Exception.class)
    public ApprovalDto reverse(String maker, String idempotencyKey, String entryId, String memo)
            throws IOException {
        boolean known = holdMaker(maker);
        String key = Ledger.clientKey(maker, idempotencyKey);
        JournalEntryDto entry = books.entry(entryId);
        if (Ledger.REVERSAL.equals(entry.kind()) || Ledger.CLOSING.equals(entry.kind())) {
            throw Refused.with(409, "entry-cannot-reversed", "That entry cannot be reversed");
        }
        if (entry.reversedBy().length() > 0) {
            throw Refused.with(409, "entry-already-reversed", "That entry was already reversed");
        }
        ApprovalDto earlier = byKey(key);
        if (earlier != null) {
            if (!REVERSAL.equals(earlier.kind()) || !entryId.equals(earlier.entryId())) {
                throw Refused.with(422, "idempotency-key-reused", "That Idempotency-Key was already used for a different request");
            }
            return earlier;
        }
        // Valued as everything it moves: the debits of every asset.
        // One unpriced leg makes the whole request unpriced (UNPRICED).
        long usd = 0;
        String asset = "";
        for (JournalLegDto leg : entry.legs()) {
            if (leg.debit() > 0) {
                long v = books.priced(leg.asset(), leg.debit());
                usd = v == Books.UNPRICED || usd == Books.UNPRICED ? Books.UNPRICED : usd + v;
                asset = asset.length() == 0 ? leg.asset() : asset;
            }
        }
        return create(known, REVERSAL, maker, "", entryId, asset, 0L, usd, clip(memo), key);
    }

    /// Not transactional on purpose: a maker approving their own request is
    /// refused here, before the approval's transaction opens, so the
    /// `APPROVAL_REFUSED` row commits in a transaction of its own instead of
    /// being rolled back by the refusal that follows it.
    public ApprovalDto approve(String checker, String id) throws IOException {
        ApprovalDto a = get(id);
        if (PENDING.equals(a.status()) && a.maker().equals(checker)) {
            audit.append(checker, "APPROVAL_REFUSED", id, "maker cannot approve own request");
            throw Refused.with(403, "four-eyes-second-administrator", "A second administrator must approve this");
        }
        return approveAsChecker(checker, id);
    }

    @Transactional(rollbackFor = Exception.class)
    ApprovalDto approveAsChecker(String checker, String id) throws IOException {
        ApprovalDto a = get(id);
        if (!PENDING.equals(a.status())) {
            throw Refused.with(409, "approval-not-pending", "That request is no longer waiting for approval");
        }
        if (a.maker().equals(checker)) {
            // Only reachable when the maker changed between the two reads,
            // which cannot happen; kept so this method stands on its own.
            throw Refused.with(403, "four-eyes-second-administrator", "A second administrator must approve this");
        }
        long now = System.currentTimeMillis();
        if (db.execute("UPDATE approval SET status = ?, checker = ?, decided_at = ? "
                + "WHERE id = ? AND status = ?", new Object[] {EXECUTED, checker,
                Long.valueOf(now), id, PENDING}) != 1) {
            throw Refused.with(409, "approval-not-pending", "That request is no longer waiting for approval");
        }
        String entry = execute(a);
        db.execute("UPDATE approval SET result_entry = ? WHERE id = ?", new Object[] {entry, id});
        audit.append(checker, a.kind() + "_APPROVED", id, "maker " + a.maker() + ", entry "
                + entry + ", " + a.usdCents() + " USD cents");
        return get(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public ApprovalDto reject(String checker, String id, String reason) throws IOException {
        ApprovalDto a = get(id);
        if (db.execute("UPDATE approval SET status = ?, checker = ?, decided_at = ? "
                + "WHERE id = ? AND status = ?", new Object[] {REJECTED, checker,
                Long.valueOf(System.currentTimeMillis()), id, PENDING}) != 1) {
            throw Refused.with(409, "approval-not-pending", "That request is no longer waiting for approval");
        }
        audit.append(checker, a.kind() + "_REJECTED", id, clip(reason));
        return get(id);
    }

    @Transactional(readOnly = true)
    public List<ApprovalDto> list() throws IOException {
        List<ApprovalDto> out = new ArrayList<ApprovalDto>();
        for (Map r : books.rows("SELECT * FROM approval ORDER BY created_at DESC LIMIT 200",
                new Object[0])) {
            out.add(dto(r));
        }
        return out;
    }

    @Transactional(readOnly = true)
    public ApprovalDto get(String id) throws IOException {
        List<Map> rows = books.rows("SELECT * FROM approval WHERE id = ?", new Object[] {id});
        if (rows.isEmpty()) {
            throw Refused.with(404, "not-found", "Not found");
        }
        return dto(rows.get(0));
    }

    /// Test seam, null in production: runs between reading the maker's 24h
    /// total and acting on it, so a test can hold that window open long
    /// enough for concurrent requests to overlap. Without it the race is a few
    /// microseconds wide and a missing lock still passes on PostgreSQL.
    public static final AtomicReference<Runnable> AFTER_RECENT_TOTAL =
            new AtomicReference<Runnable>();

    /// Serializes one maker's requests: a no-op write of the maker's profile
    /// row, like `ProfileRepository.hold` for password changes, locks it until
    /// the transaction ends on SQLite, PostgreSQL and MySQL alike. Without it
    /// N concurrent requests just under the threshold all read the same 24h
    /// total and all post without a checker. Taken before the idempotency
    /// lookup too, so two retries of one key cannot both insert.
    /// Answers false when the maker has no profile, so nothing was locked;
    /// [create] then fails closed and the request waits for a checker.
    private boolean holdMaker(String maker) {
        return maker != null && profiles.hold(maker) > 0;
    }

    private ApprovalDto create(boolean known, String kind, String maker, String accountId,
            String entryId, String asset, long amount, long usd, String memo, String key)
            throws IOException {
        String id = Ids.next();
        long now = System.currentTimeMillis();
        // Fail closed: a request that cannot be valued is not known to be
        // small, so it waits for a second administrator.
        boolean unpriced = usd == Books.UNPRICED;
        if (unpriced) {
            usd = 0L;
        }
        // Splitting one large change into many just under the threshold
        // posts nothing alone: what this maker posted without a checker in
        // the last 24 hours counts towards this request.
        long recent = unpriced ? 0L : Books.number(Books.first(books.rows(
                "SELECT COALESCE(SUM(usd_cents), 0) AS t FROM approval WHERE maker = ? "
                + "AND status = ? AND checker = ? AND created_at > ?", new Object[] {maker,
                    EXECUTED, "", Long.valueOf(now - Books.DAY)}), "t"));
        Runnable pause = AFTER_RECENT_TOTAL.get();
        if (pause != null) {
            pause.run();
        }
        // Subtracted, not added: a value saturates at Long.MAX_VALUE.
        boolean waits = !known || unpriced || usd > threshold - recent;
        db.execute("INSERT INTO approval (id, kind, status, maker, checker, account_id, entry_id, "
                + "asset, amount, usd_cents, memo, request_key, result_entry, created_at, "
                + "decided_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                new Object[] {id, kind, waits ? PENDING : EXECUTED, maker, "", accountId, entryId,
                    asset, Long.valueOf(amount), Long.valueOf(usd), memo, key, "",
                    Long.valueOf(now), Long.valueOf(waits ? 0L : now)});
        ApprovalDto a = get(id);
        if (waits) {
            audit.append(maker, kind + "_REQUESTED", id, describe(a) + (!known ? ", maker has no profile"
                    : unpriced ? ", unpriced"
                    : recent > 0 ? ", " + recent + " USD cents posted without a checker in 24h"
                    : "") + ", waits for a second administrator");
            return a;
        }
        String entry = execute(a);
        db.execute("UPDATE approval SET result_entry = ? WHERE id = ?", new Object[] {entry, id});
        audit.append(maker, kind + "_POSTED", id, describe(a) + ", within the "
                + threshold + " USD cent threshold (" + recent + " earlier in 24h), entry " + entry);
        return get(id);
    }

    private String execute(ApprovalDto a) throws IOException {
        if (REVERSAL.equals(a.kind())) {
            return ledger.reverse("rev:" + a.id(), a.entryId(), a.maker(), a.memo()).getId();
        }
        LedgerAccount adjust = ledger.system(a.asset(), AccountKinds.SYS_ADJUST);
        JournalEntry entry = ledger.post("adj:" + a.id(), ADJUSTMENT, a.maker(),
                a.memo().length() == 0 ? "Adjustment" : a.memo(), Arrays.asList(
                        new Leg(a.accountId(), a.amount()), new Leg(adjust.getId(), -a.amount())));
        return entry.getId();
    }

    private ApprovalDto byKey(String key) throws IOException {
        List<Map> rows = books.rows("SELECT * FROM approval WHERE request_key = ?",
                new Object[] {key});
        return rows.isEmpty() ? null : dto(rows.get(0));
    }

    private static String describe(ApprovalDto a) {
        return REVERSAL.equals(a.kind()) ? "reverse " + a.entryId() + " (" + a.usdCents()
                + " USD cents)" : a.amount() + " " + a.asset() + " to " + a.accountId() + " ("
                + a.usdCents() + " USD cents)";
    }

    private static ApprovalDto dto(Map r) {
        return new ApprovalDto(Books.str(r, "id"), Books.str(r, "kind"), Books.str(r, "status"),
                Books.str(r, "maker"), Books.str(r, "checker"), Books.str(r, "account_id"),
                Books.str(r, "entry_id"), Books.str(r, "asset"), Books.num(r, "amount"),
                Books.num(r, "usd_cents"), Books.str(r, "memo"), Books.str(r, "result_entry"),
                Books.num(r, "created_at"), Books.num(r, "decided_at"));
    }

    private static String clip(String s) {
        if (s == null) {
            return "";
        }
        return s.length() <= 120 ? s : s.substring(0, 120);
    }
}
