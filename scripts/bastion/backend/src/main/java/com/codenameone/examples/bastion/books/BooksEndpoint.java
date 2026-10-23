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

import com.codename1.backend.annotations.Component;
import com.codenameone.examples.bastion.Refused;
import com.codenameone.examples.bastion.api.BalanceSheetDto;
import com.codenameone.examples.bastion.api.BooksApiServer;
import com.codenameone.examples.bastion.api.ChartAccountDto;
import com.codenameone.examples.bastion.api.IncomeStatementDto;
import com.codenameone.examples.bastion.api.JournalEntryDto;
import com.codenameone.examples.bastion.api.LedgerLineDto;
import com.codenameone.examples.bastion.api.PeriodDto;
import com.codenameone.examples.bastion.api.ReconciliationDto;
import com.codenameone.examples.bastion.api.TAccountDto;
import com.codenameone.examples.bastion.api.TrialBalanceDto;
import com.codenameone.examples.bastion.api.VolumeDayDto;
import com.codenameone.examples.bastion.audit.Audit;

import java.util.List;

/// [com.codenameone.examples.bastion.api.BooksApi], served: read-only, staff
/// only, and every read on the audit chain.
@Component
public class BooksEndpoint implements BooksApiServer {
    private final Books books;
    private final Audit audit;

    public BooksEndpoint(Books books, Audit audit) {
        this.books = books;
        this.audit = audit;
    }

    @Override
    public List<ChartAccountDto> chart() throws Exception {
        Staff.name();
        // The chart is the bank's structure, not anybody's data: not audited.
        return Chart.all();
    }

    @Override
    public List<JournalEntryDto> journal(String from, String to, String limit) throws Exception {
        long f = from(from);
        long t = to(to);
        int n = clamp(limit, 50, 200);
        Staff.read(audit, "journal", f + ".." + t + " limit " + n);
        return books.journal(f, t, n);
    }

    @Override
    public JournalEntryDto entry(String id) throws Exception {
        Staff.read(audit, id, "journal entry");
        return books.entry(id);
    }

    @Override
    public List<LedgerLineDto> generalLedger(String code, String asset, String from, String to)
            throws Exception {
        long f = from(from);
        long t = to(to);
        Staff.read(audit, "ledger:" + code + ":" + asset, f + ".." + t);
        return books.generalLedger(code, asset, f, t);
    }

    @Override
    public TAccountDto tAccount(String id, String from, String to) throws Exception {
        long f = from(from);
        long t = to(to);
        Staff.read(audit, id, "t-account " + f + ".." + t);
        return books.tAccount(id, f, t);
    }

    @Override
    public TrialBalanceDto trialBalance(String asOf) throws Exception {
        long t = to(asOf);
        Staff.read(audit, "trial-balance", "as of " + t);
        return books.trialBalance(t);
    }

    @Override
    public BalanceSheetDto balanceSheet(String asOf) throws Exception {
        long t = to(asOf);
        Staff.read(audit, "balance-sheet", "as of " + t);
        return books.balanceSheet(t);
    }

    @Override
    public IncomeStatementDto incomeStatement(String from, String to) throws Exception {
        long f = from(from);
        long t = to(to);
        Staff.read(audit, "income-statement", f + ".." + t);
        return books.incomeStatement(f, t);
    }

    @Override
    public ReconciliationDto reconciliation() throws Exception {
        String who = Staff.name();
        ReconciliationDto result = books.reconcile();
        // What was found is on the record too, as with the ledger check.
        audit.append(who, "RECONCILED", "ledger", result.clean() ? "clean"
                : result.breaks().size() + " breaks");
        return result;
    }

    @Override
    public List<PeriodDto> periods() throws Exception {
        Staff.name();
        return books.periods();
    }

    @Override
    public List<VolumeDayDto> volume(String days) throws Exception {
        int d = clamp(days, 30, 366);
        Staff.read(audit, "volume", d + " days");
        return books.volume(d);
    }

    /// Query values arrive as text and are optional: absent or empty is the
    /// default, anything that is not a number a 400.
    public static long number(String text, long fallback) {
        if (text == null || text.trim().length() == 0) {
            return fallback;
        }
        try {
            return Long.parseLong(text.trim());
        } catch (NumberFormatException notANumber) {
            throw Refused.with(400, "not-number", "That is not a number");
        }
    }

    static long from(String from) {
        long f = number(from, 0L);
        return f < 0 ? 0L : f;
    }

    static long to(String to) {
        long t = number(to, 0L);
        return t <= 0 ? System.currentTimeMillis() : t;
    }

    public static int clamp(String value, int fallback, int max) {
        long v = number(value, fallback);
        if (v <= 0) {
            return fallback;
        }
        return (int) Math.min(v, max);
    }
}
