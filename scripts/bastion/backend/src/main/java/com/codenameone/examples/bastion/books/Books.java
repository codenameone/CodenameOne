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
import com.codenameone.examples.bastion.Refused;
import com.codenameone.examples.bastion.api.AssetTotalDto;
import com.codenameone.examples.bastion.api.BalanceSheetDto;
import com.codenameone.examples.bastion.api.BreakDto;
import com.codenameone.examples.bastion.api.ControlDto;
import com.codenameone.examples.bastion.api.IncomeStatementDto;
import com.codenameone.examples.bastion.api.JournalEntryDto;
import com.codenameone.examples.bastion.api.JournalLegDto;
import com.codenameone.examples.bastion.api.LedgerLineDto;
import com.codenameone.examples.bastion.api.PeriodDto;
import com.codenameone.examples.bastion.api.ReconciliationDto;
import com.codenameone.examples.bastion.api.StatementLineDto;
import com.codenameone.examples.bastion.api.StatementTotalDto;
import com.codenameone.examples.bastion.api.TAccountDto;
import com.codenameone.examples.bastion.api.TrialBalanceDto;
import com.codenameone.examples.bastion.api.TrialBalanceLineDto;
import com.codenameone.examples.bastion.api.VolumeDayDto;
import com.codenameone.examples.bastion.audit.Audit;
import com.codenameone.examples.bastion.domain.JournalEntry;
import com.codenameone.examples.bastion.domain.LedgerAccount;
import com.codenameone.examples.bastion.ledger.AccountKinds;
import com.codenameone.examples.bastion.ledger.Leg;
import com.codenameone.examples.bastion.ledger.Ledger;
import com.codenameone.examples.bastion.money.Money;
import com.codenameone.examples.bastion.repository.JournalEntryRepository;
import com.codenameone.examples.bastion.repository.LedgerAccountRepository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/// The accounting reports over the ledger, and period close.
///
/// Everything here is derived from the postings at read time -- the journal,
/// the general ledger, trial balance, balance sheet and income statement --
/// so no report can disagree with the ledger it reports on. The SQL is plain
/// SUM/GROUP BY that SQLite, PostgreSQL and MySQL all read alike; anything
/// calendar-shaped (days) is bucketed here in Java, because the three spell
/// date arithmetic three ways.
///
/// Statements are per asset: a ledger in five currencies balances five times,
/// and nothing converts one into another inside the books. The USD figures
/// beside each line are a valuation at the current index price, for reading,
/// never posted.
@Component
public class Books {
    public static final long DAY = 86400000L;

    private final DataSource db;
    private final Ledger ledger;
    private final Money money;
    private final LedgerAccountRepository accounts;
    private final JournalEntryRepository entries;
    private final Audit audit;

    public Books(DataSource db, Ledger ledger, Money money, LedgerAccountRepository accounts,
            JournalEntryRepository entries, Audit audit) {
        this.audit = audit;
        this.db = db;
        this.ledger = ledger;
        this.money = money;
        this.accounts = accounts;
        this.entries = entries;
    }

    // ---------------------------------------------------------------- journal

    @Transactional(readOnly = true)
    public List<JournalEntryDto> journal(long from, long to, int limit) throws IOException {
        List<Map> heads = rows("SELECT id, created_at, kind, initiator, memo FROM journal_entry "
                + "WHERE created_at >= ? AND created_at <= ? ORDER BY created_at DESC, id DESC "
                + "LIMIT " + limit, new Object[] {Long.valueOf(from), Long.valueOf(to)});
        return withLegs(heads);
    }

    @Transactional(readOnly = true)
    public JournalEntryDto entry(String id) throws IOException {
        List<Map> heads = rows("SELECT id, created_at, kind, initiator, memo FROM journal_entry "
                + "WHERE id = ?", new Object[] {id});
        if (heads.isEmpty()) {
            throw Refused.with(404, "no-such-entry", "No such journal entry");
        }
        return withLegs(heads).get(0);
    }

    private List<JournalEntryDto> withLegs(List<Map> heads) throws IOException {
        List<JournalEntryDto> out = new ArrayList<JournalEntryDto>();
        if (heads.isEmpty()) {
            return out;
        }
        StringBuilder in = new StringBuilder();
        Object[] ids = new Object[heads.size()];
        for (int i = 0; i < heads.size(); i++) {
            in.append(i == 0 ? "?" : ", ?");
            ids[i] = str(heads.get(i), "id");
        }
        Map<String, List<JournalLegDto>> legs = new LinkedHashMap<String, List<JournalLegDto>>();
        for (Map r : rows("SELECT p.entry_id AS entry_id, p.account_id AS account_id, "
                + "p.amount AS amount, a.kind AS kind, a.asset AS asset, a.owner AS owner, "
                + "a.name AS name FROM posting p JOIN ledger_account a ON a.id = p.account_id "
                + "WHERE p.entry_id IN (" + in + ") ORDER BY p.amount ASC, p.id ASC", ids)) {
            String entry = str(r, "entry_id");
            List<JournalLegDto> list = legs.get(entry);
            if (list == null) {
                list = new ArrayList<JournalLegDto>();
                legs.put(entry, list);
            }
            long amount = num(r, "amount");
            String kind = str(r, "kind");
            list.add(new JournalLegDto(str(r, "account_id"), Chart.code(kind), accountName(r),
                    str(r, "owner"), str(r, "asset"), Chart.debit(amount), Chart.credit(amount)));
        }
        Map<String, String> reverses = new LinkedHashMap<String, String>();
        Map<String, String> reversedBy = new LinkedHashMap<String, String>();
        for (Map r : rows("SELECT entry_id, reversed_by FROM reversal WHERE entry_id IN (" + in
                + ") OR reversed_by IN (" + in + ")", concat(ids, ids))) {
            reverses.put(str(r, "reversed_by"), str(r, "entry_id"));
            reversedBy.put(str(r, "entry_id"), str(r, "reversed_by"));
        }
        for (Map h : heads) {
            String id = str(h, "id");
            List<JournalLegDto> list = legs.get(id);
            out.add(new JournalEntryDto(id, num(h, "created_at"), str(h, "kind"),
                    str(h, "initiator"), str(h, "memo"), orEmpty(reverses.get(id)),
                    orEmpty(reversedBy.get(id)),
                    list == null ? new ArrayList<JournalLegDto>() : list));
        }
        return out;
    }

    // ------------------------------------------------------- ledgers, T-account

    /// Every posting under chart `code` in `asset` between `from` and `to`,
    /// with the running balance on the code's normal side, starting from the
    /// balance brought forward at `from`. At most 2000 lines.
    @Transactional(readOnly = true)
    public List<LedgerLineDto> generalLedger(String code, String asset, long from, long to)
            throws IOException {
        String kind = Chart.kindOf(code);
        if (kind == null) {
            throw Refused.with(404, "no-such-account", "No such account");
        }
        ledger.asset(asset);
        long opening = number(first(rows("SELECT COALESCE(SUM(p.amount), 0) AS t FROM posting p "
                + "JOIN ledger_account a ON a.id = p.account_id WHERE a.kind = ? AND a.asset = ? "
                + "AND p.created_at < ?", new Object[] {kind, asset, Long.valueOf(from)}), "t"));
        return lines(rows("SELECT p.entry_id AS entry_id, p.account_id AS account_id, "
                + "p.amount AS amount, p.created_at AS created_at, a.kind AS kind, "
                + "a.asset AS asset, a.owner AS owner, e.kind AS ekind, e.memo AS memo "
                + "FROM posting p JOIN ledger_account a ON a.id = p.account_id "
                + "JOIN journal_entry e ON e.id = p.entry_id WHERE a.kind = ? AND a.asset = ? "
                + "AND p.created_at >= ? AND p.created_at <= ? ORDER BY p.created_at ASC, p.id ASC "
                + "LIMIT 2000", new Object[] {kind, asset, Long.valueOf(from), Long.valueOf(to)}),
                kind, opening);
    }

    @Transactional(readOnly = true)
    public TAccountDto tAccount(String accountId, long from, long to) throws IOException {
        LedgerAccount account = accountId == null ? null : accounts.findById(accountId);
        if (account == null) {
            throw Refused.with(404, "no-such-account", "No such account");
        }
        String kind = account.getKind();
        long opening = number(first(rows("SELECT COALESCE(SUM(amount), 0) AS t FROM posting "
                + "WHERE account_id = ? AND created_at < ?",
                new Object[] {accountId, Long.valueOf(from)}), "t"));
        List<LedgerLineDto> all = lines(rows("SELECT p.entry_id AS entry_id, "
                + "p.account_id AS account_id, p.amount AS amount, p.created_at AS created_at, "
                + "a.kind AS kind, a.asset AS asset, a.owner AS owner, e.kind AS ekind, "
                + "e.memo AS memo FROM posting p JOIN ledger_account a ON a.id = p.account_id "
                + "JOIN journal_entry e ON e.id = p.entry_id WHERE p.account_id = ? "
                + "AND p.created_at >= ? AND p.created_at <= ? ORDER BY p.created_at ASC, p.id ASC "
                + "LIMIT 2000", new Object[] {accountId, Long.valueOf(from), Long.valueOf(to)}),
                kind, opening);
        List<LedgerLineDto> debits = new ArrayList<LedgerLineDto>();
        List<LedgerLineDto> credits = new ArrayList<LedgerLineDto>();
        long dr = 0;
        long cr = 0;
        long closing = Chart.normal(kind, opening);
        for (LedgerLineDto line : all) {
            if (line.debit() > 0) {
                debits.add(line);
                dr += line.debit();
            } else {
                credits.add(line);
                cr += line.credit();
            }
            closing = line.balance();
        }
        String cls = Chart.classOf(kind);
        return new TAccountDto(accountId, Chart.code(kind), Chart.name(kind) + " - "
                + account.getName(), cls, Chart.normalOf(cls), account.getAsset(), from, to,
                Chart.normal(kind, opening), debits, credits, dr, cr, closing);
    }

    private static List<LedgerLineDto> lines(List<Map> rows, String kind, long openingNet) {
        List<LedgerLineDto> out = new ArrayList<LedgerLineDto>();
        long net = openingNet;
        for (Map r : rows) {
            long amount = num(r, "amount");
            net += amount;
            out.add(new LedgerLineDto(str(r, "entry_id"), num(r, "created_at"), str(r, "ekind"),
                    str(r, "memo"), str(r, "account_id"), str(r, "owner"), Chart.code(kind),
                    str(r, "asset"), Chart.debit(amount), Chart.credit(amount),
                    Chart.normal(kind, net)));
        }
        return out;
    }

    // ------------------------------------------------------------ statements

    /// Net balance per chart code and asset as of `asOf`, in the debit or
    /// credit column. Customer accounts appear as their control accounts --
    /// one line for all customers' checking -- as a general-ledger trial
    /// balance does.
    @Transactional(readOnly = true)
    public TrialBalanceDto trialBalance(long asOf) throws IOException {
        Map<String, long[]> totals = new TreeMap<String, long[]>();
        List<TrialBalanceLineDto> lines = new ArrayList<TrialBalanceLineDto>();
        for (Map r : balances(asOf)) {
            String kind = str(r, "kind");
            String asset = str(r, "asset");
            long net = num(r, "net");
            long[] t = totals.get(asset);
            if (t == null) {
                t = new long[2];
                totals.put(asset, t);
            }
            t[0] += Chart.debit(net);
            t[1] += Chart.credit(net);
            lines.add(new TrialBalanceLineDto(Chart.code(kind), Chart.name(kind),
                    Chart.classOf(kind), asset, Chart.debit(net), Chart.credit(net)));
        }
        sortLines(lines);
        List<AssetTotalDto> sums = new ArrayList<AssetTotalDto>();
        boolean balanced = true;
        for (Map.Entry<String, long[]> t : totals.entrySet()) {
            boolean ok = t.getValue()[0] == t.getValue()[1];
            balanced &= ok;
            sums.add(new AssetTotalDto(t.getKey(), t.getValue()[0], t.getValue()[1], ok));
        }
        return new TrialBalanceDto(asOf, lines, sums, balanced);
    }

    /// Assets = liabilities + equity in every asset, equity including the
    /// period's earnings not yet closed into retained earnings.
    @Transactional(readOnly = true)
    public BalanceSheetDto balanceSheet(long asOf) throws IOException {
        List<StatementLineDto> lines = new ArrayList<StatementLineDto>();
        Map<String, long[]> perAsset = new TreeMap<String, long[]>();
        long totalUsd = 0;
        for (Map r : balances(asOf)) {
            String kind = str(r, "kind");
            String asset = str(r, "asset");
            long net = num(r, "net");
            long[] t = perAsset.get(asset);
            if (t == null) {
                // assets, liabilities, equity, current earnings
                t = new long[4];
                perAsset.put(asset, t);
            }
            String cls = Chart.classOf(kind);
            long amount = Chart.normal(kind, net);
            if (Chart.REVENUE.equals(cls)) {
                t[3] += amount;
                continue;
            }
            if (Chart.EXPENSE.equals(cls)) {
                t[3] -= amount;
                continue;
            }
            String section = Chart.ASSET.equals(cls) ? "ASSETS"
                    : Chart.LIABILITY.equals(cls) ? "LIABILITIES" : "EQUITY";
            t[Chart.ASSET.equals(cls) ? 0 : Chart.LIABILITY.equals(cls) ? 1 : 2] += amount;
            long usd = usd(asset, amount);
            if (Chart.ASSET.equals(cls)) {
                totalUsd += usd;
            }
            lines.add(new StatementLineDto(section, Chart.code(kind), Chart.name(kind), asset,
                    amount, usd));
        }
        List<StatementTotalDto> totals = new ArrayList<StatementTotalDto>();
        boolean balanced = true;
        for (Map.Entry<String, long[]> e : perAsset.entrySet()) {
            String asset = e.getKey();
            long[] t = e.getValue();
            if (t[3] != 0) {
                lines.add(new StatementLineDto("EQUITY", "3900", "Current period earnings",
                        asset, t[3], usd(asset, t[3])));
            }
            totals.add(new StatementTotalDto(asset, "ASSETS", t[0], usd(asset, t[0])));
            totals.add(new StatementTotalDto(asset, "LIABILITIES", t[1], usd(asset, t[1])));
            totals.add(new StatementTotalDto(asset, "EQUITY", t[2] + t[3],
                    usd(asset, t[2] + t[3])));
            totals.add(new StatementTotalDto(asset, "CURRENT_EARNINGS", t[3], usd(asset, t[3])));
            balanced &= t[0] == t[1] + t[2] + t[3];
        }
        sortStatement(lines);
        return new BalanceSheetDto(asOf, lines, totals, totalUsd, balanced);
    }

    /// Revenue and expenses between `from` and `to`, closing entries left
    /// out: they move the period's result to retained earnings, and counting
    /// them would show every closed period as having earned nothing.
    @Transactional(readOnly = true)
    public IncomeStatementDto incomeStatement(long from, long to) throws IOException {
        List<StatementLineDto> lines = new ArrayList<StatementLineDto>();
        Map<String, long[]> perAsset = new TreeMap<String, long[]>();
        long netUsd = 0;
        for (Map r : rows("SELECT a.kind AS kind, a.asset AS asset, SUM(p.amount) AS net "
                + "FROM posting p JOIN ledger_account a ON a.id = p.account_id "
                + "JOIN journal_entry e ON e.id = p.entry_id WHERE p.created_at >= ? "
                + "AND p.created_at <= ? AND e.kind <> ? GROUP BY a.kind, a.asset",
                new Object[] {Long.valueOf(from), Long.valueOf(to), Ledger.CLOSING})) {
            String kind = str(r, "kind");
            if (!Chart.isNominal(kind)) {
                continue;
            }
            String asset = str(r, "asset");
            long amount = Chart.normal(kind, num(r, "net"));
            boolean revenue = Chart.REVENUE.equals(Chart.classOf(kind));
            long[] t = perAsset.get(asset);
            if (t == null) {
                t = new long[2];
                perAsset.put(asset, t);
            }
            t[revenue ? 0 : 1] += amount;
            lines.add(new StatementLineDto(revenue ? "REVENUE" : "EXPENSES", Chart.code(kind),
                    Chart.name(kind), asset, amount, usd(asset, amount)));
        }
        List<StatementTotalDto> totals = new ArrayList<StatementTotalDto>();
        for (Map.Entry<String, long[]> e : perAsset.entrySet()) {
            String asset = e.getKey();
            long[] t = e.getValue();
            totals.add(new StatementTotalDto(asset, "REVENUE", t[0], usd(asset, t[0])));
            totals.add(new StatementTotalDto(asset, "EXPENSES", t[1], usd(asset, t[1])));
            long net = t[0] - t[1];
            long usd = usd(asset, net);
            netUsd += usd;
            totals.add(new StatementTotalDto(asset, "NET_INCOME", net, usd));
        }
        sortStatement(lines);
        return new IncomeStatementDto(from, to, lines, totals, netUsd);
    }

    private List<Map> balances(long asOf) throws IOException {
        return rows("SELECT a.kind AS kind, a.asset AS asset, SUM(p.amount) AS net "
                + "FROM posting p JOIN ledger_account a ON a.id = p.account_id "
                + "WHERE p.created_at <= ? GROUP BY a.kind, a.asset",
                new Object[] {Long.valueOf(asOf)});
    }

    // -------------------------------------------------------- reconciliation

    /// Reconciles the books three ways and reports every break:
    ///
    /// - each account's kept balance against the sum of its postings;
    /// - each customer control account (chart 2000-2200) as the general
    ///   ledger has it, against the sum of the individual customer accounts'
    ///   kept balances -- the sub-ledger;
    /// - each entry's debits against its credits, per asset.
    @Transactional(readOnly = true)
    public ReconciliationDto reconcile() throws IOException {
        List<BreakDto> breaks = new ArrayList<BreakDto>();
        Map<String, long[]> controls = new TreeMap<String, long[]>();
        for (Map r : rows("SELECT a.id AS id, a.kind AS kind, a.asset AS asset, "
                + "a.balance AS balance, COALESCE(SUM(p.amount), 0) AS total "
                + "FROM ledger_account a LEFT JOIN posting p ON p.account_id = a.id "
                + "GROUP BY a.id, a.kind, a.asset, a.balance", new Object[0])) {
            String kind = str(r, "kind");
            String asset = str(r, "asset");
            long kept = num(r, "balance");
            long posted = num(r, "total");
            if (kept != posted) {
                breaks.add(new BreakDto("CACHED_BALANCE", asset, str(r, "id"), posted, kept,
                        kept - posted));
            }
            if (AccountKinds.isCustomer(kind)) {
                String key = asset + "|" + Chart.code(kind);
                long[] c = controls.get(key);
                if (c == null) {
                    c = new long[2];
                    controls.put(key, c);
                }
                c[0] += posted;
                c[1] += kept;
            }
        }
        List<ControlDto> out = new ArrayList<ControlDto>();
        for (Map.Entry<String, long[]> c : controls.entrySet()) {
            String key = c.getKey();
            int bar = key.indexOf('|');
            String asset = key.substring(0, bar);
            String code = key.substring(bar + 1);
            long control = c.getValue()[0];
            long sub = c.getValue()[1];
            out.add(new ControlDto(asset, code, Chart.name(Chart.kindOf(code)), control, sub,
                    sub - control));
            if (control != sub) {
                breaks.add(new BreakDto("CONTROL", asset, code, control, sub, sub - control));
            }
        }
        for (Map r : rows("SELECT entry_id, asset, SUM(amount) AS total FROM posting "
                + "GROUP BY entry_id, asset HAVING SUM(amount) <> 0", new Object[0])) {
            long total = num(r, "total");
            breaks.add(new BreakDto("UNBALANCED_ENTRY", str(r, "asset"), str(r, "entry_id"), 0L,
                    total, total));
        }
        return new ReconciliationDto(System.currentTimeMillis(), breaks.isEmpty(), out, breaks);
    }

    // ---------------------------------------------------------------- volume

    @Transactional(readOnly = true)
    public List<VolumeDayDto> volume(int days) throws IOException {
        long today = day(System.currentTimeMillis());
        long start = today - (days - 1) * DAY;
        Map<String, long[]> buckets = new TreeMap<String, long[]>();
        Map<String, String> lastEntry = new LinkedHashMap<String, String>();
        for (Map r : rows("SELECT entry_id, asset, amount, created_at FROM posting "
                + "WHERE created_at >= ? AND amount > 0 ORDER BY entry_id",
                new Object[] {Long.valueOf(start)})) {
            long d = day(num(r, "created_at"));
            String asset = str(r, "asset");
            String key = zeroPad(d) + "|" + asset;
            long[] b = buckets.get(key);
            if (b == null) {
                b = new long[2];
                buckets.put(key, b);
            }
            String entry = str(r, "entry_id");
            if (!entry.equals(lastEntry.get(key))) {
                b[0]++;
                lastEntry.put(key, entry);
            }
            b[1] += num(r, "amount");
        }
        List<VolumeDayDto> out = new ArrayList<VolumeDayDto>();
        for (Map.Entry<String, long[]> b : buckets.entrySet()) {
            String key = b.getKey();
            int bar = key.indexOf('|');
            out.add(new VolumeDayDto(Long.parseLong(key.substring(0, bar)),
                    key.substring(bar + 1), (int) b.getValue()[0], b.getValue()[1]));
        }
        return out;
    }

    // ---------------------------------------------------------- period close

    @Transactional(readOnly = true)
    public List<PeriodDto> periods() throws IOException {
        List<PeriodDto> out = new ArrayList<PeriodDto>();
        for (Map r : rows("SELECT seq, closed_through, closed_by, closed_at, entries "
                + "FROM period_close ORDER BY seq DESC", new Object[0])) {
            out.add(period(r));
        }
        return out;
    }

    /// Closes the books through `through`: for each asset, one closing entry
    /// that brings every revenue and expense account to zero against
    /// retained earnings, dated `through`; then the period row, after which
    /// nothing may be posted at or before `through`. The row's sequence is
    /// its key, so two closes racing cannot both commit.
    ///
    /// Audited in the same transaction, so a close and its record commit
    /// together.
    @Transactional(rollbackFor = Exception.class)
    public PeriodDto close(String admin, long through) throws IOException {
        long last = ledger.closedThrough();
        if (through <= last || through >= System.currentTimeMillis()) {
            throw Refused.with(400, "period-end-past-last-close", "A period must end in the past, after the last one closed");
        }
        long seq = number(first(rows("SELECT COALESCE(MAX(seq), 0) AS s FROM period_close",
                new Object[0]), "s")) + 1;
        Map<String, List<Leg>> perAsset = new TreeMap<String, List<Leg>>();
        Map<String, Long> retained = new TreeMap<String, Long>();
        for (Map r : rows("SELECT a.id AS id, a.kind AS kind, a.asset AS asset, "
                + "SUM(p.amount) AS net FROM posting p JOIN ledger_account a "
                + "ON a.id = p.account_id WHERE p.created_at <= ? GROUP BY a.id, a.kind, a.asset",
                new Object[] {Long.valueOf(through)})) {
            String kind = str(r, "kind");
            long net = num(r, "net");
            if (!Chart.isNominal(kind) || net == 0) {
                continue;
            }
            String asset = str(r, "asset");
            List<Leg> legs = perAsset.get(asset);
            if (legs == null) {
                legs = new ArrayList<Leg>();
                perAsset.put(asset, legs);
            }
            legs.add(new Leg(str(r, "id"), -net));
            Long sum = retained.get(asset);
            retained.put(asset, Long.valueOf((sum == null ? 0L : sum.longValue()) + net));
        }
        List<String> posted = new ArrayList<String>();
        for (Map.Entry<String, List<Leg>> e : perAsset.entrySet()) {
            String asset = e.getKey();
            List<Leg> legs = e.getValue();
            long toRetained = retained.get(asset).longValue();
            if (toRetained != 0) {
                LedgerAccount re = ledger.system(asset, AccountKinds.SYS_RETAINED);
                legs.add(new Leg(re.getId(), toRetained));
            }
            if (legs.size() < 2) {
                continue;
            }
            JournalEntry entry = ledger.postClosing("period-close:" + seq + ":" + asset, admin,
                    "Closing entry, period " + seq + " (" + asset + ")", legs, through);
            posted.add(entry.getId());
        }
        StringBuilder list = new StringBuilder();
        for (String id : posted) {
            if (list.length() + id.length() + 1 > 500) {
                break;
            }
            list.append(list.length() == 0 ? "" : ",").append(id);
        }
        long now = System.currentTimeMillis();
        db.execute("INSERT INTO period_close (seq, closed_through, closed_by, closed_at, entries) "
                + "VALUES (?, ?, ?, ?, ?)", new Object[] {Long.valueOf(seq), Long.valueOf(through),
                admin, Long.valueOf(now), list.toString()});
        audit.append(admin, "PERIOD_CLOSED", "period:" + seq, "through " + through + ", "
                + posted.size() + " closing entries " + list);
        return new PeriodDto(seq, through, admin, now, posted);
    }

    private static PeriodDto period(Map r) {
        List<String> ids = new ArrayList<String>();
        String list = str(r, "entries");
        int at = 0;
        while (list.length() > 0 && at <= list.length()) {
            int comma = list.indexOf(',', at);
            int end = comma < 0 ? list.length() : comma;
            if (end > at) {
                ids.add(list.substring(at, end));
            }
            at = end + 1;
        }
        return new PeriodDto(num(r, "seq"), num(r, "closed_through"), str(r, "closed_by"),
                num(r, "closed_at"), ids);
    }

    // --------------------------------------------------------------- helpers

    /// `amount` of `asset` in US cents at the index price, keeping its sign.
    public long usd(String asset, long amount) throws IOException {
        if (amount == 0) {
            return 0L;
        }
        long v;
        try {
            v = money.usdValue(asset, amount < 0 ? -amount : amount);
        } catch (RuntimeException unpriced) {
            // An asset with no market has no USD value to show; the line in
            // its own units is still exact.
            return 0L;
        }
        return amount < 0 ? -v : v;
    }

    /// The UTC midnight `ms` falls on.
    public static long day(long ms) {
        long d = ms / DAY;
        if (ms < 0 && d * DAY != ms) {
            d--;
        }
        return d * DAY;
    }

    private static String zeroPad(long v) {
        String s = Long.toString(v);
        StringBuilder b = new StringBuilder();
        for (int i = s.length(); i < 20; i++) {
            b.append('0');
        }
        return b.append(s).toString();
    }

    private static String accountName(Map r) {
        String owner = str(r, "owner");
        String kind = str(r, "kind");
        return AccountKinds.BANK.equals(owner) ? Chart.name(kind)
                : Chart.name(kind) + " - " + owner;
    }

    private static void sortLines(List<TrialBalanceLineDto> lines) {
        Collections.sort(lines, (a, b) -> {
            int c = a.asset().compareTo(b.asset());
            return c != 0 ? c : a.code().compareTo(b.code());
        });
    }

    private static void sortStatement(List<StatementLineDto> lines) {
        Collections.sort(lines, (a, b) -> {
            int c = a.asset().compareTo(b.asset());
            return c != 0 ? c : a.code().compareTo(b.code());
        });
    }

    private static Object[] concat(Object[] a, Object[] b) {
        Object[] out = new Object[a.length + b.length];
        System.arraycopy(a, 0, out, 0, a.length);
        System.arraycopy(b, 0, out, a.length, b.length);
        return out;
    }

    public List<Map> rows(String sql, Object[] params) throws IOException {
        List raw = db.query(sql, params);
        List<Map> out = new ArrayList<Map>();
        for (int i = 0; raw != null && i < raw.size(); i++) {
            Object o = raw.get(i);
            if (o instanceof Map) {
                out.add((Map) o);
            }
        }
        return out;
    }

    private static Map first(List<Map> rows) {
        return rows.isEmpty() ? new LinkedHashMap() : rows.get(0);
    }

    private static Object first(List<Map> rows, String key) {
        return first(rows).get(key);
    }

    public static String str(Map r, String key) {
        Object v = value(r, key);
        return v == null ? "" : String.valueOf(v);
    }

    public static long num(Map r, String key) {
        return number(value(r, key));
    }

    /// A column by name whatever case the driver gave it.
    private static Object value(Map r, String key) {
        Object v = r.get(key);
        if (v == null && !r.containsKey(key)) {
            for (Object k : r.keySet()) {
                if (k instanceof String && ((String) k).equalsIgnoreCase(key)) {
                    return r.get(k);
                }
            }
        }
        return v;
    }

    static long number(Object value) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value == null) {
            return 0L;
        }
        String s = String.valueOf(value).trim();
        int dot = s.indexOf('.');
        return s.length() == 0 ? 0L : Long.parseLong(dot < 0 ? s : s.substring(0, dot));
    }

    private static String orEmpty(String s) {
        return s == null ? "" : s;
    }
}
