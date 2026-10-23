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
package com.codenameone.examples.bastion;

import com.codename1.backend.DataSource;
import com.codename1.backend.Json;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Autowired;
import com.codename1.backend.test.BackendTest;
import com.codename1.backend.test.MockMvc;
import com.codename1.backend.test.MockRequestBuilder;
import com.codename1.backend.test.MockResponse;
import com.codenameone.examples.bastion.account.DemoAccounts;
import com.codenameone.examples.bastion.api.AssetTotalDto;
import com.codenameone.examples.bastion.api.BreakDto;
import com.codenameone.examples.bastion.api.ChainReportDto;
import com.codenameone.examples.bastion.api.JournalEntryDto;
import com.codenameone.examples.bastion.api.JournalLegDto;
import com.codenameone.examples.bastion.api.LedgerLineDto;
import com.codenameone.examples.bastion.api.ReconciliationDto;
import com.codenameone.examples.bastion.api.TrialBalanceDto;
import com.codenameone.examples.bastion.api.TrialBalanceLineDto;
import com.codenameone.examples.bastion.api.VolumeDayDto;
import com.codenameone.examples.bastion.audit.Audit;
import com.codenameone.examples.bastion.api.ApprovalDto;
import com.codenameone.examples.bastion.api.TAccountDto;
import com.codenameone.examples.bastion.books.Approvals;
import com.codenameone.examples.bastion.books.AuditTrailEndpoint;
import com.codenameone.examples.bastion.books.Books;
import com.codenameone.examples.bastion.domain.AuditEvent;
import com.codenameone.examples.bastion.domain.JournalEntry;
import com.codenameone.examples.bastion.domain.LedgerAccount;
import com.codenameone.examples.bastion.ledger.AccountKinds;
import com.codenameone.examples.bastion.ledger.Leg;
import com.codenameone.examples.bastion.ledger.Ledger;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.codename1.backend.test.MockMvcRequestBuilders.get;
import static com.codename1.backend.test.MockMvcRequestBuilders.post;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The accounting invariants: the trial balance balances, a closed period
/// is final, a reversal nets to zero, reconciliation finds a break, the
/// audit chain names its first broken link, an auditor cannot move money and
/// an adjustment above the threshold needs a second administrator.
@BackendTest
class BooksTest {
    @Autowired
    private Ledger ledger;
    @Autowired
    private Books books;
    @Autowired
    private Audit audit;
    @Autowired
    private DataSource db;
    @Autowired
    private MockMvc mvc;
    @Autowired
    private Approvals approvals;

    private static String key() {
        return "test-" + TestKeys.random();
    }

    private LedgerAccount funded(String owner, long cents) throws Exception {
        LedgerAccount account = ledger.open(owner, "USD", AccountKinds.CHECKING, "Checking");
        LedgerAccount cash = ledger.system("USD", AccountKinds.SYS_CASH);
        ledger.post(key(), "DEPOSIT", owner, "seed", Arrays.asList(
                new Leg(cash.getId(), -cents), new Leg(account.getId(), cents)));
        return account;
    }

    /// A maker is an account with a profile; one without waits for a checker.
    private String maker(String username) throws Exception {
        db.execute("INSERT INTO profile (username, display_name, created_at) VALUES (?, ?, ?)",
                new Object[] {username, username, Long.valueOf(System.currentTimeMillis())});
        return username;
    }

    private long balance(LedgerAccount a) throws Exception {
        return ledger.owned(a.getOwner(), a.getId()).getBalance();
    }

    @Test
    void theDemoHistoryBalancesInEveryAsset() throws Exception {
        TrialBalanceDto tb = books.trialBalance(System.currentTimeMillis());
        assertTrue(tb.balanced());
        boolean usd = false;
        for (AssetTotalDto t : tb.totals()) {
            assertEquals(t.debits(), t.credits(), t.asset());
            usd |= "USD".equals(t.asset()) && t.debits() > 0;
        }
        assertTrue(usd, "the demo history posts USD");
        boolean liability = false;
        for (TrialBalanceLineDto line : tb.lines()) {
            // Customer deposits are the bank's liabilities: credit balances.
            if ("2000".equals(line.code())) {
                liability = true;
                assertEquals(0L, line.debit());
            }
        }
        assertTrue(liability);
        assertTrue(books.balanceSheet(System.currentTimeMillis()).balanced());
        // A week ago the books balanced too: every entry is balanced at its date.
        assertTrue(books.trialBalance(System.currentTimeMillis() - 7 * Books.DAY).balanced());
        assertFalse(books.journal(0, System.currentTimeMillis(), 50).isEmpty());
    }

    @Test
    void aReversalNetsToZeroAndHappensOnce() throws Exception {
        LedgerAccount alice = funded("books-a1", 10000);
        LedgerAccount bob = funded("books-b1", 0 + 1);
        JournalEntry paid = ledger.post(key(), "TRANSFER", "books-a1", "rent", Arrays.asList(
                new Leg(alice.getId(), -2500), new Leg(bob.getId(), 2500)));
        JournalEntry back = ledger.reverse(key(), paid.getId(), "admin", "wrong payee");
        assertEquals(10000, balance(alice));
        assertEquals(1, balance(bob));
        JournalEntryDto original = books.entry(paid.getId());
        JournalEntryDto reversal = books.entry(back.getId());
        assertEquals(back.getId(), original.reversedBy());
        assertEquals(paid.getId(), reversal.reverses());
        long net = 0;
        for (JournalLegDto leg : original.legs()) {
            net += leg.credit() - leg.debit();
        }
        for (JournalLegDto leg : reversal.legs()) {
            net += leg.credit() - leg.debit();
        }
        assertEquals(0L, net);
        ResponseStatusException twice = assertThrows(ResponseStatusException.class,
                () -> ledger.reverse(key(), paid.getId(), "admin", ""));
        assertEquals(409, twice.getStatus());
        ResponseStatusException ofReversal = assertThrows(ResponseStatusException.class,
                () -> ledger.reverse(key(), back.getId(), "admin", ""));
        assertEquals(409, ofReversal.getStatus());
    }

    @Test
    void reconciliationFindsAnInjectedBreak() throws Exception {
        LedgerAccount alice = funded("books-a2", 5000);
        assertTrue(books.reconcile().clean(), String.valueOf(books.reconcile().breaks()));
        db.execute("UPDATE ledger_account SET balance = balance + 7 WHERE id = ?",
                new Object[] {alice.getId()});
        try {
            ReconciliationDto r = books.reconcile();
            assertFalse(r.clean());
            boolean cached = false;
            boolean control = false;
            for (BreakDto b : r.breaks()) {
                cached |= "CACHED_BALANCE".equals(b.type()) && alice.getId().equals(b.reference())
                        && b.difference() == 7;
                control |= "CONTROL".equals(b.type()) && "2000".equals(b.reference())
                        && b.difference() == 7;
            }
            assertTrue(cached && control, String.valueOf(r.breaks()));
        } finally {
            db.execute("UPDATE ledger_account SET balance = balance - 7 WHERE id = ?",
                    new Object[] {alice.getId()});
        }
        assertTrue(books.reconcile().clean());
    }

    @Test
    void escrowMovesAreNotVolume() throws Exception {
        LedgerAccount alice = funded("books-v1", 5000);
        LedgerAccount hold = ledger.open("books-v1", "USD", AccountKinds.HOLD, "Hold");
        long before = todayEntries();
        ledger.post(key(), "ORDER_HOLD", "books-v1", "BTC-USD BUY", Arrays.asList(
                new Leg(alice.getId(), -700), new Leg(hold.getId(), 700)));
        ledger.post(key(), "ORDER_RELEASE", "books-v1", "BTC-USD BUY", Arrays.asList(
                new Leg(hold.getId(), -700), new Leg(alice.getId(), 700)));
        assertEquals(before, todayEntries());
        LedgerAccount cash = ledger.system("USD", AccountKinds.SYS_CASH);
        ledger.post(key(), "DEPOSIT", "books-v1", "real", Arrays.asList(
                new Leg(cash.getId(), -10), new Leg(alice.getId(), 10)));
        assertEquals(before + 1, todayEntries());
    }

    private long todayEntries() throws Exception {
        long count = 0;
        List<VolumeDayDto> days = books.volume(1);
        for (VolumeDayDto d : days) {
            if ("USD".equals(d.asset())) {
                count += d.entries();
            }
        }
        return count;
    }

    @Test
    void theFirstBrokenLinkIsNamed() throws Exception {
        audit.append("books-test", "TEST", "one", "first");
        AuditEvent victim = audit.append("books-test", "TEST", "two", "second");
        audit.append("books-test", "TEST", "three", "third");
        db.execute("UPDATE audit_event SET detail = ? WHERE seq = ?",
                new Object[] {"rewritten", Long.valueOf(victim.getSeq())});
        try {
            ChainReportDto report = audit.report();
            assertFalse(report.intact());
            assertEquals(victim.getSeq(), report.firstBrokenSeq());
        } finally {
            db.execute("UPDATE audit_event SET detail = ? WHERE seq = ?",
                    new Object[] {"second", Long.valueOf(victim.getSeq())});
        }
        ChainReportDto again = audit.report();
        assertTrue(again.intact(), String.valueOf(again.problems()));
        assertEquals(0L, again.firstBrokenSeq());
    }

    @Test
    void aClosedPeriodIsFinal() throws Exception {
        LedgerAccount alice = funded("books-a3", 1000);
        LedgerAccount cash = ledger.system("USD", AccountKinds.SYS_CASH);
        long through = System.currentTimeMillis() - 60000L;
        books.close("books-admin", through);
        ResponseStatusException closed = assertThrows(ResponseStatusException.class,
                () -> ledger.postAt(key(), "DEPOSIT", "books-a3", "late", Arrays.asList(
                        new Leg(cash.getId(), -10), new Leg(alice.getId(), 10)), through - 1));
        assertEquals(409, closed.getStatus());
        assertEquals(1000, balance(alice));
        ResponseStatusException again = assertThrows(ResponseStatusException.class,
                () -> books.close("books-admin", through));
        assertEquals(400, again.getStatus());
        // Revenue and expense are zero at the close; their result is retained.
        for (TrialBalanceLineDto line : books.trialBalance(through).lines()) {
            if (line.code().startsWith("4") || line.code().startsWith("5")) {
                assertEquals(0L, line.debit() + line.credit(), line.code() + " " + line.asset());
            }
        }
        assertTrue(books.trialBalance(through).balanced());
        assertTrue(books.balanceSheet(through).balanced());
        // The income statement still shows what the closed period earned.
        assertFalse(books.incomeStatement(0, through).lines().isEmpty());
        // The open period takes postings as before.
        ledger.post(key(), "DEPOSIT", "books-a3", "on time", Arrays.asList(
                new Leg(cash.getId(), -10), new Leg(alice.getId(), 10)));
        assertEquals(1010, balance(alice));
    }

    // ------------------------------------------------------------------ HTTP

    private MockResponse send(String token, MockRequestBuilder request) throws Exception {
        return mvc.perform(request.header("Host", SignIn.HOST)
                .header("Authorization", "Bearer " + token)).andReturn().getResponse();
    }

    private static MockRequestBuilder json(String path, String body) {
        return post(path).contentType("application/json").content(body);
    }

    @Test
    void anAuditorReadsEverythingAndMovesNothing() throws Exception {
        String auditor = SignIn.token(mvc, DemoAccounts.AUDITOR, DemoAccounts.PASSWORD);
        for (String path : new String[] {"/api/books/trial-balance", "/api/books/balance-sheet",
            "/api/books/income-statement", "/api/books/journal?limit=5", "/api/books/chart",
            "/api/books/reconciliation", "/api/books/volume?days=7", "/api/audit/chain",
            "/api/audit/access-log", "/api/books/ledger?code=2000&asset=USD",
            "/api/audit/evidence/trial-balance?format=csv"}) {
            MockResponse answer = send(auditor, get(path));
            assertEquals(200, answer.getStatus(), path + " " + answer.getContentAsString());
        }
        assertEquals(403, send(auditor, json("/api/admin/adjustments",
                "{\"accountId\":\"x\",\"amount\":1,\"memo\":\"\",\"password\":\""
                + DemoAccounts.PASSWORD + "\"}").header("Idempotency-Key", key())).getStatus());
        assertEquals(403, send(auditor, json("/api/transfers", "{\"fromAccountId\":\"x\","
                + "\"toAccountId\":\"y\",\"amount\":1}").header("Idempotency-Key", key()))
                .getStatus());
        assertEquals(403, send(auditor, get("/api/accounts")).getStatus());
        // The reads above are on the record, under the auditor's name.
        MockResponse log = send(auditor, get("/api/audit/access-log?limit=50"));
        assertTrue(log.getContentAsString().contains(DemoAccounts.AUDITOR));
        assertTrue(log.getContentAsString().contains("STAFF_READ"));
        // And customers see none of it.
        String customer = SignIn.token(mvc, DemoAccounts.CUSTOMER, DemoAccounts.PASSWORD);
        assertEquals(403, send(customer, get("/api/books/trial-balance")).getStatus());
        assertEquals(403, send(customer, get("/api/audit/chain")).getStatus());
    }

    @Test
    void aLargeAdjustmentNeedsASecondAdministrator() throws Exception {
        LedgerAccount target = funded("books-c1", 100);
        String admin = SignIn.token(mvc, DemoAccounts.ADMIN, DemoAccounts.PASSWORD);
        String second = SignIn.token(mvc, DemoAccounts.SECOND_ADMIN, DemoAccounts.PASSWORD);
        String body = "{\"accountId\":\"" + target.getId() + "\",\"amount\":500000,"
                + "\"memo\":\"goodwill\",\"password\":\"" + DemoAccounts.PASSWORD + "\"}";

        MockResponse wrong = send(admin, json("/api/admin/adjustments", body.replace(
                DemoAccounts.PASSWORD, "not-the-password")).header("Idempotency-Key", key()));
        assertEquals(403, wrong.getStatus());

        MockResponse made = send(admin, json("/api/admin/adjustments", body)
                .header("Idempotency-Key", key()));
        assertEquals(200, made.getStatus(), made.getContentAsString());
        Map approval = Json.parseObject(made.getContentAsString());
        assertEquals("PENDING", approval.get("status"));
        assertEquals(100, balance(target));
        String approve = "/api/admin/approvals/" + approval.get("id") + "/approve";
        String decision = "{\"password\":\"" + DemoAccounts.PASSWORD + "\",\"reason\":\"ok\"}";

        assertEquals(403, send(admin, json(approve, decision)).getStatus());
        assertEquals(100, balance(target));
        // The refusal is on the record: it is not rolled back with the request.
        assertEquals(1, books.rows("SELECT seq FROM audit_event WHERE action = ? AND subject = ?",
                new Object[] {"APPROVAL_REFUSED", approval.get("id")}).size());

        MockResponse approved = send(second, json(approve, decision));
        assertEquals(200, approved.getStatus(), approved.getContentAsString());
        assertEquals("EXECUTED", Json.parseObject(approved.getContentAsString()).get("status"));
        assertEquals(500100, balance(target));
        assertEquals(409, send(second, json(approve, decision)).getStatus());

        // Below the threshold it posts at once, by one administrator.
        MockResponse small = send(admin, json("/api/admin/adjustments", body.replace("500000",
                "-50")).header("Idempotency-Key", key()));
        assertEquals("EXECUTED", Json.parseObject(small.getContentAsString()).get("status"));
        assertEquals(500050, balance(target));
        List trail = (List) Json.parse(send(admin, get("/api/audit/trail?subject="
                + approval.get("id"))).getContentAsString());
        assertTrue(trail.size() >= 2, String.valueOf(trail));
    }

    @Test
    void customersGetTheirChartData() throws Exception {
        String customer = SignIn.token(mvc, DemoAccounts.CUSTOMER, DemoAccounts.PASSWORD);
        List accounts = (List) Json.parse(send(customer, get("/api/accounts"))
                .getContentAsString());
        String id = (String) ((Map) accounts.get(0)).get("id");
        List history = (List) Json.parse(send(customer, get("/api/accounts/" + id
                + "/balance-history?days=14")).getContentAsString());
        assertEquals(14, history.size());
        List candles = (List) Json.parse(send(customer, get(
                "/api/exchange/markets/BTC-USD/candles?interval=3600000&limit=240"))
                .getContentAsString());
        assertTrue(candles.size() > 24, "candles: " + candles.size());
        List flow = (List) Json.parse(send(customer, get("/api/insights/cashflow?days=10"))
                .getContentAsString());
        assertEquals(10, flow.size());
        MockResponse allocation = send(customer, get("/api/insights/allocation"));
        assertEquals(200, allocation.getStatus(), allocation.getContentAsString());
        assertEquals(404, send(customer, get("/api/accounts/nope/balance-history")).getStatus());
    }

    @Test
    void anUnpricedAdjustmentWaitsForASecondAdministrator() throws Exception {
        db.execute("INSERT INTO asset (code, name, kind, decimals, enabled) VALUES "
                + "('TSTU', 'Unpriced test coin', 'CRYPTO', 8, 1)", new Object[0]);
        LedgerAccount coin = ledger.open("books-u1", "TSTU", AccountKinds.WALLET, "Coins");
        // No market prices TSTU: the request cannot be valued, so it is not
        // known to be small, and waits.
        ApprovalDto a = approvals.adjust(maker("books-admin-u"), key(), coin.getId(), 1L, "unpriced");
        assertEquals(Approvals.PENDING, a.status());
        List<Map> rows = books.rows("SELECT detail FROM audit_event WHERE subject = ?",
                new Object[] {a.id()});
        assertTrue(String.valueOf(rows).contains("unpriced"), String.valueOf(rows));
    }

    @Test
    void splittingAnAdjustmentUnderTheThresholdStillNeedsAChecker() throws Exception {
        LedgerAccount target = funded("books-s1", 100);
        long under = approvals.threshold() - 1;
        String admin = maker("books-admin-s");
        int executed = 0;
        for (int i = 0; i < 10; i++) {
            ApprovalDto a = approvals.adjust(admin, key(), target.getId(), under,
                    "split " + i);
            executed += Approvals.EXECUTED.equals(a.status()) ? 1 : 0;
        }
        // The first fits the threshold; the rest add up past it in 24 hours.
        assertEquals(1, executed);
        assertEquals(100 + under, balance(target));
    }

    @Test
    void concurrentSplitAdjustmentsStillNeedAChecker() throws Exception {
        final LedgerAccount target = funded("books-c1", 100);
        final long under = approvals.threshold() - 1;
        final String admin = maker("books-admin-c");
        final AtomicInteger executed = new AtomicInteger();
        final AtomicInteger answered = new AtomicInteger();
        Runnable[] calls = new Runnable[8];
        for (int iter = 0; iter < calls.length; iter++) {
            final int n = iter;
            calls[iter] = () -> {
                try {
                    ApprovalDto a = approvals.adjust(admin, key(), target.getId(), under,
                            "race " + n);
                    answered.incrementAndGet();
                    if (Approvals.EXECUTED.equals(a.status())) {
                        executed.incrementAndGet();
                    }
                } catch (Exception busy) {
                    // A busy database rolls the request back: nothing posted,
                    // which the balance below confirms.
                }
            };
        }
        // Holds the window between reading the 24h total and posting open,
        // so the requests really overlap: without it the race is too narrow
        // for a missing lock to show on PostgreSQL.
        Approvals.AFTER_RECENT_TOTAL.set(() -> {
            try {
                Thread.sleep(100);
            } catch (InterruptedException stop) {
                Thread.currentThread().interrupt();
            }
        });
        try {
            Race.together(calls);
        } finally {
            Approvals.AFTER_RECENT_TOTAL.set(null);
        }
        // Each one read the 24h total under the maker's lock, so at most one
        // threshold's worth posted without a checker.
        assertTrue(answered.get() > 0, "no request was answered");
        assertTrue(executed.get() <= 1, executed.get() + " posted without a checker");
        assertEquals(100 + under * executed.get(), balance(target));
        assertEquals(executed.get(), books.rows("SELECT id FROM approval WHERE maker = ? "
                + "AND status = ?", new Object[] {admin, Approvals.EXECUTED}).size());
    }

    @Test
    void aMakerWithNoProfileWaitsForAChecker() throws Exception {
        LedgerAccount target = funded("books-n1", 100);
        ApprovalDto a = approvals.adjust("books-admin-none", key(), target.getId(), 1L, "x");
        assertEquals(Approvals.PENDING, a.status());
    }

    @Test
    void aLongTAccountListsTheLatestAndTotalsEverything() throws Exception {
        LedgerAccount many = funded("books-t1", 0 + 1);
        LedgerAccount cash = ledger.system("USD", AccountKinds.SYS_CASH);
        int extra = Books.LEDGER_LINES + 5;
        for (int i = 0; i < extra; i++) {
            ledger.post(key(), "DEPOSIT", "books-t1", "drip", Arrays.asList(
                    new Leg(cash.getId(), -1), new Leg(many.getId(), 1)));
        }
        TAccountDto t = books.tAccount(many.getId(), 0L, System.currentTimeMillis());
        assertTrue(t.truncated());
        assertEquals(Books.LEDGER_LINES, t.debits().size() + t.credits().size());
        assertEquals(1L + extra, t.totalCredits());
        assertEquals(balance(many), t.closingBalance());
        List<LedgerLineDto> credits = t.credits();
        assertEquals(balance(many), credits.get(credits.size() - 1).balance());
    }

    @Test
    void theJournalEvidenceHoldsEveryEntry() throws Exception {
        String admin = SignIn.token(mvc, DemoAccounts.ADMIN, DemoAccounts.PASSWORD);
        long before = System.currentTimeMillis();
        MockResponse r = send(admin, get("/api/audit/evidence/journal?format=json&asOf="
                + before));
        assertEquals(200, r.getStatus(), r.getContentAsString());
        String content = (String) Json.parseObject(r.getContentAsString()).get("content");
        HashSet<Object> ids = new HashSet<Object>();
        for (Object row : (List) Json.parse(content)) {
            ids.add(((Map) row).get("entry_id"));
        }
        long expected = ((Number) books.rows("SELECT COUNT(*) AS n FROM journal_entry "
                + "WHERE created_at <= ?", new Object[] {Long.valueOf(before)}).get(0)
                .get("n")).longValue();
        assertEquals(expected, ids.size());
    }

    @Test
    void csvCellsThatASpreadsheetWouldRunAreQuoted() {
        Map<String, Object> row = new LinkedHashMap<String, Object>();
        row.put("a", "\tcmd");
        row.put("b", "\r=1");
        row.put("c", "plain");
        String out = AuditTrailEndpoint.csv(Arrays.asList(row));
        assertTrue(out.contains("'\tcmd"), out);
        assertTrue(out.contains("\"'\r=1\""), out);
        assertTrue(out.contains(",plain"), out);
    }

    @Test
    void readingTheClosedPeriodsIsOnTheRecord() throws Exception {
        String auditor = SignIn.token(mvc, DemoAccounts.AUDITOR, DemoAccounts.PASSWORD);
        assertEquals(200, send(auditor, get("/api/books/periods")).getStatus());
        assertFalse(books.rows("SELECT seq FROM audit_event WHERE action = ? AND subject = ? "
                + "AND actor = ?", new Object[] {"STAFF_READ", "periods", DemoAccounts.AUDITOR})
                .isEmpty());
    }
}
