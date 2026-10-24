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
package com.codenameone.examples.bastion.ledger;

import com.codenameone.examples.bastion.Refused;
import com.codename1.backend.DataSource;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.bastion.Ids;
import com.codenameone.examples.bastion.domain.Asset;
import com.codenameone.examples.bastion.domain.JournalEntry;
import com.codenameone.examples.bastion.domain.LedgerAccount;
import com.codenameone.examples.bastion.domain.Posting;
import com.codenameone.examples.bastion.repository.AssetRepository;
import com.codenameone.examples.bastion.repository.JournalEntryRepository;
import com.codenameone.examples.bastion.repository.LedgerAccountRepository;
import com.codenameone.examples.bastion.repository.PostingRepository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// The double-entry ledger every balance in this bank lives in, fiat and
/// crypto alike.
///
/// Three rules, each enforced here and nowhere else:
///
/// 1. **Every entry sums to zero in every asset.** Money is never created or
///    destroyed, only moved; a deposit moves it out of the bank's `SYS_CASH`
///    account, which is why that one may go negative.
/// 2. **A customer's balance never goes below zero**, and a frozen account
///    neither pays nor receives. Each balance change is one conditional
///    `UPDATE ... WHERE balance + delta >= 0`, so the database decides
///    atomically and two concurrent transfers cannot both spend the same
///    dollar. A refusal rolls the whole entry back.
/// 3. **The same idempotency key moves money once.** A retried request gets
///    the entry its first attempt made; a different request with the same
///    key is refused, never answered with somebody else's result.
///
/// Balances are a cache of the postings, kept in the same transaction, and
/// [#verify] recomputes them.
@Component
public class Ledger {
    private final AssetRepository assets;
    private final LedgerAccountRepository accounts;
    private final JournalEntryRepository entries;
    private final PostingRepository postings;
    private final DataSource db;

    public Ledger(AssetRepository assets, LedgerAccountRepository accounts,
            JournalEntryRepository entries, PostingRepository postings, DataSource db) {
        this.db = db;
        this.assets = assets;
        this.accounts = accounts;
        this.entries = entries;
        this.postings = postings;
    }

    /// The asset `code` names, or 400 when there is none or it is disabled.
    @Transactional(readOnly = true)
    public Asset asset(String code) throws IOException {
        Asset asset = code == null ? null : assets.findById(code);
        if (asset == null || !asset.isEnabled()) {
            throw Refused.with(400, "no-such-asset", "No such asset");
        }
        return asset;
    }

    @Transactional(readOnly = true)
    public List<Asset> assets() throws IOException {
        return assets.findByEnabledTrueOrderByCodeAsc();
    }

    /// Opens an account for `owner`. A customer may hold several of a kind.
    @Transactional(rollbackFor = Exception.class)
    public LedgerAccount open(String owner, String assetCode, String kind, String name)
            throws IOException {
        Asset asset = asset(assetCode);
        if (!AccountKinds.isCustomer(kind) && !AccountKinds.isSystem(kind)) {
            throw Refused.with(400, "no-such-kind-account", "No such kind of account");
        }
        if (AccountKinds.isSystem(kind) != AccountKinds.BANK.equals(owner)) {
            throw new IllegalArgumentException("Only the bank holds SYS_ accounts");
        }
        // A hold is either: a buy order holds fiat, a sell order holds coins.
        boolean either = AccountKinds.isSystem(kind) || AccountKinds.HOLD.equals(kind);
        if (!either && "CRYPTO".equals(asset.getKind()) != AccountKinds.WALLET.equals(kind)) {
            throw Refused.with(400, "crypto-held-wallet-fiat-account", "Crypto is held in a wallet, fiat in an account");
        }
        LedgerAccount account = new LedgerAccount();
        account.setId(Ids.next());
        account.setOwner(owner);
        account.setAsset(asset.getCode());
        account.setKind(kind);
        account.setName(name == null ? "" : name);
        account.setCreatedAt(System.currentTimeMillis());
        accounts.insert(account);
        return account;
    }

    /// The bank's account of `kind` in `asset`, opened the first time it is
    /// asked for.
    @Transactional(rollbackFor = Exception.class)
    public LedgerAccount system(String assetCode, String kind) throws IOException {
        LedgerAccount found = accounts.findFirstByOwnerAndAssetAndKind(AccountKinds.BANK,
                assetCode, kind);
        return found != null ? found : open(AccountKinds.BANK, assetCode, kind, kind);
    }

    @Transactional(readOnly = true)
    public List<LedgerAccount> accountsOf(String owner) throws IOException {
        return accounts.findByOwnerOrderByCreatedAtAsc(owner);
    }

    /// `owner`'s account `id`, or 404 -- the same answer whether the account
    /// does not exist or belongs to someone else, so an id cannot be probed.
    @Transactional(readOnly = true)
    public LedgerAccount owned(String owner, String id) throws IOException {
        LedgerAccount account = id == null ? null : accounts.findById(id);
        if (account == null || !account.getOwner().equals(owner)) {
            throw Refused.with(404, "no-such-account", "No such account");
        }
        return account;
    }

    /// `owner`'s account `id` for a customer to move money into or out of:
    /// [#owned], but never an order HOLD. A hold belongs to the customer only
    /// as the exchange's escrow of an open order; moving it out directly
    /// would drain the order's backing while the order still rests on the
    /// book. Same 404 as a missing account.
    @Transactional(readOnly = true)
    public LedgerAccount movable(String owner, String id) throws IOException {
        LedgerAccount account = owned(owner, id);
        if (AccountKinds.HOLD.equals(account.getKind())) {
            throw Refused.with(404, "no-such-account", "No such account");
        }
        return account;
    }

    @Transactional(readOnly = true)
    public List<Posting> history(String accountId) throws IOException {
        return postings.findTop100ByAccountIdOrderByCreatedAtDescIdDesc(accountId);
    }

    /// The entry `id`, or null when there is none.
    @Transactional(readOnly = true)
    public JournalEntry entry(String id) throws IOException {
        return id == null ? null : entries.findById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public void freeze(String accountId, boolean frozen) throws IOException {
        if (accounts.setFrozen(accountId, frozen) != 1) {
            throw Refused.with(404, "no-such-account", "No such account");
        }
    }

    /// The journal key for a client's `Idempotency-Key`. Every journal key
    /// shares one unique index, and the exchange posts its own entries under
    /// system keys (`hold:`, `release:`, `trade:` and the seeding keys). A
    /// client key stored as sent could claim one of those first -- or another
    /// customer's key -- and turn the system's posting into a "retry" of the
    /// client's. So a client key is scoped to its owner and hashed under a
    /// `c:` prefix no system key starts with, and a `:` in it is refused so a
    /// key can never be read as a system one. 66 characters, within the column.
    public static String clientKey(String owner, String key) throws IOException {
        // The client makes base64url keys, which never hold ':'; a key that
        // does is answered like any other malformed one.
        if (key == null || key.length() < 16 || key.length() > 80 || key.indexOf(':') >= 0) {
            throw Refused.with(400, "send-idempotency-key-16-80", "Send an Idempotency-Key of 16 to 80 characters");
        }
        return "c:" + Ids.sha256(owner + "\n" + key);
    }

    /// Moves value. See the class comment for the rules; a refusal is a 409
    /// and moves nothing.
    @Transactional(rollbackFor = Exception.class)
    public JournalEntry post(String idempotencyKey, String kind, String initiator, String memo,
            List<Leg> legs) throws IOException {
        return post(idempotencyKey, kind, initiator, memo, legs, System.currentTimeMillis(),
                false);
    }

    /// [#post] dated `at` instead of now: for the demo's history and for
    /// accruals. A date at or before the end of the last closed period is
    /// refused -- a closed period is final, and a correction to it is posted
    /// in the open period, as a reversal or an adjustment.
    @Transactional(rollbackFor = Exception.class)
    public JournalEntry postAt(String idempotencyKey, String kind, String initiator, String memo,
            List<Leg> legs, long at) throws IOException {
        return post(idempotencyKey, kind, initiator, memo, legs, at, false);
    }

    /// The end of the newest closed period, or 0 when none is closed.
    @Transactional(readOnly = true)
    public long closedThrough() throws IOException {
        List rows = db.query("SELECT MAX(closed_through) AS t FROM period_close", new Object[0]);
        if (rows == null || rows.isEmpty() || !(rows.get(0) instanceof Map)) {
            return 0L;
        }
        return number(((Map) rows.get(0)).get("t"));
    }

    /// Reverses entry `entryId`: posts its mirror image now, in the open
    /// period, and records the pair. An entry is reversed at most once (the
    /// `reversal` table's key decides, so two reversals racing cannot both
    /// commit), and neither a reversal nor a closing entry is reversed --
    /// the first would be undone by reversing it again in the other
    /// direction, the second belongs to a closed period. A reversal that would
    /// overdraw a customer is refused like any other movement.
    @Transactional(rollbackFor = Exception.class)
    public JournalEntry reverse(String idempotencyKey, String entryId, String initiator,
            String memo) throws IOException {
        JournalEntry original = entryId == null ? null : entries.findById(entryId);
        if (original == null) {
            throw Refused.with(404, "no-such-entry", "No such journal entry");
        }
        if (REVERSAL.equals(original.getKind()) || CLOSING.equals(original.getKind())) {
            throw Refused.with(409, "entry-cannot-reversed", "That entry cannot be reversed");
        }
        List<Leg> legs = new ArrayList<Leg>();
        for (Posting p : postings.findByEntryId(entryId)) {
            legs.add(new Leg(p.getAccountId(), -p.getAmount()));
        }
        String earlier = reversedBy(entryId);
        if (earlier != null) {
            // The same request retried is answered with its reversal; any
            // other is a second reversal of one entry.
            JournalEntry retried = entries.findFirstByIdempotencyKey(idempotencyKey);
            if (retried != null && retried.getId().equals(earlier)) {
                return retried;
            }
            throw Refused.with(409, "entry-already-reversed", "That entry was already reversed");
        }
        JournalEntry entry = post(idempotencyKey, REVERSAL, initiator,
                "Reversal of " + entryId + (memo == null || memo.length() == 0 ? "" : ": " + memo),
                legs, System.currentTimeMillis(), false);
        db.execute("INSERT INTO reversal (entry_id, reversed_by, at) VALUES (?, ?, ?)",
                new Object[] {entryId, entry.getId(), Long.valueOf(entry.getCreatedAt())});
        return entry;
    }

    /// The entry that reversed `entryId`, or null.
    @Transactional(readOnly = true)
    public String reversedBy(String entryId) throws IOException {
        List rows = db.query("SELECT reversed_by AS r FROM reversal WHERE entry_id = ?",
                new Object[] {entryId});
        if (rows == null || rows.isEmpty() || !(rows.get(0) instanceof Map)) {
            return null;
        }
        Object r = ((Map) rows.get(0)).get("r");
        return r == null ? null : String.valueOf(r);
    }

    /// Journal kinds the books treat specially.
    public static final String REVERSAL = "REVERSAL";
    public static final String CLOSING = "CLOSING";

    /// Posts a closing entry dated at the end of the period being closed,
    /// which the closed-period rule would otherwise refuse. Only period close
    /// calls this, in the transaction that records the close.
    @Transactional(rollbackFor = Exception.class)
    public JournalEntry postClosing(String idempotencyKey, String initiator, String memo,
            List<Leg> legs, long at) throws IOException {
        return post(idempotencyKey, CLOSING, initiator, memo, legs, at, true);
    }

    private JournalEntry post(String idempotencyKey, String kind, String initiator, String memo,
            List<Leg> legs, long at, boolean closing) throws IOException {
        if (idempotencyKey == null || idempotencyKey.length() < 16
                || idempotencyKey.length() > 80) {
            // 16 characters at least: a key a client can collide with by
            // accident is a key that drops a real payment as a "retry".
            throw Refused.with(400, "send-idempotency-key-16-80", "Send an Idempotency-Key of 16 to 80 characters");
        }
        if (legs == null || legs.size() < 2) {
            throw Refused.with(400, "entry-needs-two-sides", "An entry needs two sides");
        }
        String hash = requestHash(kind, memo, legs);
        JournalEntry earlier = entries.findFirstByIdempotencyKey(idempotencyKey);
        if (earlier != null) {
            if (!earlier.getRequestHash().equals(hash)) {
                throw Refused.with(422, "idempotency-key-reused", "That Idempotency-Key was already used for a different request");
            }
            return earlier;
        }

        if (!closing && at <= closedThrough()) {
            throw Refused.with(409, "period-closed", "That date is in a closed period");
        }

        Map<String, Long> perAsset = new HashMap<String, Long>();
        Map<String, LedgerAccount> touched = new HashMap<String, LedgerAccount>();
        for (Leg leg : legs) {
            if (leg.amount() == 0) {
                throw Refused.with(400, "amount-cannot-zero", "An amount cannot be zero");
            }
            LedgerAccount account = touched.get(leg.accountId());
            if (account == null) {
                account = leg.accountId() == null ? null : accounts.findById(leg.accountId());
                if (account == null) {
                    throw Refused.with(404, "no-such-account", "No such account");
                }
                touched.put(account.getId(), account);
            }
            Long sum = perAsset.get(account.getAsset());
            long next = (sum == null ? 0L : sum.longValue()) + leg.amount();
            // Overflow is the one way a sum of longs can lie about being zero.
            if (sum != null && ((sum.longValue() ^ next) & (leg.amount() ^ next)) < 0) {
                throw Refused.with(400, "amount-too-large", "That amount is too large");
            }
            perAsset.put(account.getAsset(), Long.valueOf(next));
        }
        for (Map.Entry<String, Long> sum : perAsset.entrySet()) {
            if (sum.getValue().longValue() != 0L) {
                throw new IllegalStateException("Unbalanced entry in " + sum.getKey());
            }
        }

        long now = at;
        JournalEntry entry = new JournalEntry();
        entry.setId(Ids.next());
        entry.setIdempotencyKey(idempotencyKey);
        entry.setRequestHash(hash);
        entry.setKind(kind);
        entry.setInitiator(initiator == null ? "" : initiator);
        entry.setMemo(memo == null ? "" : (memo.length() > 140 ? memo.substring(0, 140) : memo));
        entry.setCreatedAt(now);
        // Written first: a concurrent request with the same key fails on the
        // unique index here, before either has touched a balance.
        entries.insert(entry);

        // Balances in a fixed order -- by account id -- so two entries over
        // the same accounts take their row locks in the same order and never
        // deadlock each other.
        List<Leg> ordered = new ArrayList<Leg>(legs);
        Collections.sort(ordered, new Comparator<Leg>() {
            @Override
            public int compare(Leg a, Leg b) {
                return a.accountId().compareTo(b.accountId());
            }
        });
        for (Leg leg : ordered) {
            LedgerAccount account = touched.get(leg.accountId());
            int applied = AccountKinds.isSystem(account.getKind())
                    ? accounts.applyUnguarded(account.getId(), leg.amount())
                    : accounts.applyGuarded(account.getId(), leg.amount());
            if (applied != 1) {
                // Why it was refused, asked of the table: the copy in hand
                // was read before this update and may predate a freeze
                // committed since, which would be answered as no money.
                throw accounts.frozenNow(account.getId()) > 0
                        ? Refused.with(409, "account-frozen", "That account is frozen")
                        : Refused.with(409, "insufficient-funds", "Insufficient funds");
            }
            Posting posting = new Posting();
            posting.setId(Ids.next());
            posting.setEntryId(entry.getId());
            posting.setAccountId(account.getId());
            posting.setAsset(account.getAsset());
            posting.setAmount(leg.amount());
            posting.setCreatedAt(now);
            postings.insert(posting);
        }
        return entry;
    }

    @Transactional(readOnly = true)
    public List<Posting> postingsOf(String entryId) throws IOException {
        return postings.findByEntryId(entryId);
    }

    /// Recomputes every balance from its postings and checks every asset sums
    /// to zero. Empty means the books balance.
    @Transactional(readOnly = true)
    public List<String> verify(List<String> owners) throws IOException {
        List<String> problems = new ArrayList<String>();
        for (Asset asset : assets.findAll()) {
            Long total = postings.assetTotal(asset.getCode());
            if (total != null && total.longValue() != 0L) {
                problems.add(asset.getCode() + " sums to " + total + ", not zero");
            }
        }
        // Balance and postings in ONE statement, so both come from one
        // snapshot. Read separately at READ COMMITTED, a transfer committing
        // between the two reads showed a balance that its postings did not
        // yet (or no longer) explain -- a false alarm on a sound ledger.
        for (String owner : owners) {
            List rows = db.query("SELECT a.id AS id, a.balance AS balance, "
                    + "COALESCE(SUM(p.amount), 0) AS total FROM ledger_account a "
                    + "LEFT JOIN posting p ON p.account_id = a.id WHERE a.owner = ? "
                    + "GROUP BY a.id, a.balance", new Object[] {owner});
            for (Object o : rows) {
                Map row = (Map) o;
                long balance = number(row.get("balance"));
                long expected = number(row.get("total"));
                if (expected != balance) {
                    problems.add("account " + row.get("id") + " holds " + balance
                            + " but its postings say " + expected);
                }
            }
        }
        return problems;
    }

    static long number(Object value) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return value == null ? 0L : Long.parseLong(String.valueOf(value).trim());
    }

    static String requestHash(String kind, String memo, List<Leg> legs) throws IOException {
        StringBuilder b = new StringBuilder();
        b.append(kind).append('|').append(memo == null ? "" : memo).append('|');
        for (Leg leg : legs) {
            b.append(leg.accountId()).append('=').append(leg.amount()).append(';');
        }
        return Ids.sha256(b.toString());
    }
}
