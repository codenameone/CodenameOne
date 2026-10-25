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
package com.codenameone.examples.bastion.api;

import com.codename1.annotations.rest.Body;
import com.codename1.annotations.rest.GET;
import com.codename1.annotations.rest.Header;
import com.codename1.annotations.rest.POST;
import com.codename1.annotations.rest.Path;
import com.codename1.annotations.rest.Query;
import com.codename1.annotations.rest.RestClient;
import com.codename1.io.rest.Response;
import com.codename1.util.OnComplete;

import java.util.List;

/// The bank's books, read: for staff only -- an ADMIN or an AUDITOR -- and
/// every call is itself recorded in the audit chain (`STAFF_READ`).
///
/// Amounts are ledger units of the line's asset (cents, satoshi, gwei), as
/// debits and credits. Times are epoch milliseconds; an absent or 0 `asOf` or
/// `to` means now, an absent `from` the beginning.
@RestClient
public interface BooksApi {
    /// Proof of reserves: what the chain holds against what the ledger owes,
    /// for the coin on a real chain.
    @GET("/api/books/custody")
    void custody(OnComplete<Response<CustodyDto>> callback);

    /// The chart of accounts.
    @GET("/api/books/chart")
    void chart(OnComplete<Response<List<ChartAccountDto>>> callback);

    /// Journal entries with all their legs, newest first, at most `limit`
    /// (default 50, at most 200).
    @GET("/api/books/journal")
    void journal(@Query("from") String from, @Query("to") String to, @Query("limit") String limit,
            OnComplete<Response<List<JournalEntryDto>>> callback);

    @GET("/api/books/journal/{id}")
    void entry(@Path("id") String id, OnComplete<Response<JournalEntryDto>> callback);

    /// The general ledger of one chart code in one asset: every posting to
    /// every account under the code, oldest first, with the running balance
    /// (the latest 2000; see [GeneralLedgerDto#truncated]).
    @GET("/api/books/ledger")
    void generalLedger(@Query("code") String code, @Query("asset") String asset,
            @Query("from") String from, @Query("to") String to,
            OnComplete<Response<GeneralLedgerDto>> callback);

    /// One ledger account as a T-account.
    @GET("/api/books/accounts/{id}/t-account")
    void tAccount(@Path("id") String id, @Query("from") String from, @Query("to") String to,
            OnComplete<Response<TAccountDto>> callback);

    @GET("/api/books/trial-balance")
    void trialBalance(@Query("asOf") String asOf, OnComplete<Response<TrialBalanceDto>> callback);

    @GET("/api/books/balance-sheet")
    void balanceSheet(@Query("asOf") String asOf, OnComplete<Response<BalanceSheetDto>> callback);

    /// The profit and loss between `from` and `to`.
    @GET("/api/books/income-statement")
    void incomeStatement(@Query("from") String from, @Query("to") String to,
            OnComplete<Response<IncomeStatementDto>> callback);

    /// Sub-ledgers against their control accounts, kept balances against
    /// postings, and every entry's debits against its credits.
    @GET("/api/books/reconciliation")
    void reconciliation(OnComplete<Response<ReconciliationDto>> callback);

    @GET("/api/books/periods")
    void periods(OnComplete<Response<List<PeriodDto>>> callback);

    /// Entries and debits per UTC day over the last `days` (default 30).
    @GET("/api/books/volume")
    void volume(@Query("days") String days, OnComplete<Response<List<VolumeDayDto>>> callback);
}
