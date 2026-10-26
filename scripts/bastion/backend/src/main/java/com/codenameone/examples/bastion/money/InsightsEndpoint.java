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
package com.codenameone.examples.bastion.money;

import com.codename1.backend.annotations.Component;
import com.codenameone.examples.bastion.Caller;
import com.codenameone.examples.bastion.api.AllocationDto;
import com.codenameone.examples.bastion.api.BalancePointDto;
import com.codenameone.examples.bastion.api.CandleDto;
import com.codenameone.examples.bastion.api.CashflowDayDto;
import com.codenameone.examples.bastion.api.InsightsApiServer;
import com.codenameone.examples.bastion.books.Books;
import com.codenameone.examples.bastion.books.BooksEndpoint;
import com.codenameone.examples.bastion.domain.LedgerAccount;
import com.codenameone.examples.bastion.exchange.Markets;
import com.codenameone.examples.bastion.ledger.Ledger;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/// [com.codenameone.examples.bastion.api.InsightsApi], served: chart data
/// over the caller's own accounts, derived from the postings, plus market
/// candles from the trade tape.
///
/// Days are UTC and bucketed here, not in SQL, because SQLite, PostgreSQL
/// and MySQL each spell date arithmetic differently (MySQL's `/` is not even
/// integer division).
@Component
public class InsightsEndpoint implements InsightsApiServer {
    static final long MINUTE = 60000L;

    private final Ledger ledger;
    private final Books books;
    private final Markets markets;

    public InsightsEndpoint(Ledger ledger, Books books, Markets markets) {
        this.ledger = ledger;
        this.books = books;
        this.markets = markets;
    }

    @Override
    public List<BalancePointDto> balanceHistory(String id, String days) throws Exception {
        String owner = Devices.customer();
        LedgerAccount account = ledger.owned(owner, id);
        int n = BooksEndpoint.clamp(days, 30, 366);
        long today = Books.day(System.currentTimeMillis());
        long start = today - (n - 1) * Books.DAY;
        long balance = Books.num(books.rows("SELECT COALESCE(SUM(amount), 0) AS t FROM posting "
                + "WHERE account_id = ? AND created_at < ?",
                new Object[] {account.getId(), Long.valueOf(start)}).get(0), "t");
        List<Map> rows = books.rows("SELECT amount, created_at FROM posting WHERE account_id = ? "
                + "AND created_at >= ? ORDER BY created_at ASC, id ASC",
                new Object[] {account.getId(), Long.valueOf(start)});
        List<BalancePointDto> out = new ArrayList<BalancePointDto>();
        int at = 0;
        for (long d = start; d <= today; d += Books.DAY) {
            while (at < rows.size() && Books.num(rows.get(at), "created_at") < d + Books.DAY) {
                balance += Books.num(rows.get(at), "amount");
                at++;
            }
            out.add(new BalancePointDto(d, balance));
        }
        return out;
    }

    @Override
    public List<CashflowDayDto> cashflow(String asset, String days) throws Exception {
        String owner = Devices.customer();
        String code = asset == null || asset.length() == 0 ? "USD" : asset;
        ledger.asset(code);
        int n = BooksEndpoint.clamp(days, 30, 366);
        long today = Books.day(System.currentTimeMillis());
        long start = today - (n - 1) * Books.DAY;
        Map<String, long[]> perEntry = new LinkedHashMap<String, long[]>();
        for (Map r : books.rows("SELECT p.entry_id AS entry_id, p.amount AS amount, "
                + "p.created_at AS created_at FROM posting p JOIN ledger_account a "
                + "ON a.id = p.account_id WHERE a.owner = ? AND a.asset = ? AND p.created_at >= ? "
                + "ORDER BY p.created_at ASC", new Object[] {owner, code, Long.valueOf(start)})) {
            String entry = Books.str(r, "entry_id");
            long[] e = perEntry.get(entry);
            if (e == null) {
                e = new long[] {Books.num(r, "created_at"), 0L};
                perEntry.put(entry, e);
            }
            e[1] += Books.num(r, "amount");
        }
        // Netted per entry over all of the caller's accounts, so a move from
        // checking to savings -- or into an order's hold -- is neither in nor out.
        TreeMap<Long, long[]> perDay = new TreeMap<Long, long[]>();
        for (long d = start; d <= today; d += Books.DAY) {
            perDay.put(Long.valueOf(d), new long[2]);
        }
        for (long[] e : perEntry.values()) {
            long[] day = perDay.get(Long.valueOf(Books.day(e[0])));
            if (day == null || e[1] == 0) {
                continue;
            }
            day[e[1] > 0 ? 0 : 1] += e[1] > 0 ? e[1] : -e[1];
        }
        List<CashflowDayDto> out = new ArrayList<CashflowDayDto>();
        for (Map.Entry<Long, long[]> d : perDay.entrySet()) {
            long[] v = d.getValue();
            out.add(new CashflowDayDto(d.getKey().longValue(), v[0], v[1], v[0] - v[1]));
        }
        return out;
    }

    @Override
    public List<AllocationDto> allocation() throws Exception {
        String owner = Devices.customer();
        TreeMap<String, Long> held = new TreeMap<String, Long>();
        for (LedgerAccount a : ledger.accountsOf(owner)) {
            Long sum = held.get(a.getAsset());
            held.put(a.getAsset(), Long.valueOf((sum == null ? 0L : sum.longValue())
                    + a.getBalance()));
        }
        long total = 0;
        Map<String, Long> usd = new LinkedHashMap<String, Long>();
        for (Map.Entry<String, Long> h : held.entrySet()) {
            long v = books.usd(h.getKey(), h.getValue().longValue());
            usd.put(h.getKey(), Long.valueOf(v));
            total += v;
        }
        List<AllocationDto> out = new ArrayList<AllocationDto>();
        for (Map.Entry<String, Long> h : held.entrySet()) {
            long v = usd.get(h.getKey()).longValue();
            int bps = total <= 0 ? 0 : (int) (v * 10000L / total);
            out.add(new AllocationDto(h.getKey(), h.getValue().longValue(), v, bps));
        }
        return out;
    }

    @Override
    public List<CandleDto> candles(String id, String interval, String limit) throws Exception {
        Caller.name();
        markets.get(id);
        long step = BooksEndpoint.number(interval, 0L);
        step = step <= 0 ? 60 * MINUTE : step;
        if (step < MINUTE || step > Books.DAY) {
            step = step < MINUTE ? MINUTE : Books.DAY;
        }
        int n = BooksEndpoint.clamp(limit, 168, 500);
        long now = System.currentTimeMillis();
        long last = now - now % step;
        long start = last - (n - 1) * step;
        List<Map> before = books.rows("SELECT price FROM trade WHERE market = ? AND created_at < ? "
                + "ORDER BY created_at DESC LIMIT 1", new Object[] {id, Long.valueOf(start)});
        long close = before.isEmpty() ? -1L : Books.num(before.get(0), "price");
        List<Map> trades = books.rows("SELECT price, quantity, buyer_paid, created_at FROM trade "
                + "WHERE market = ? AND created_at >= ? ORDER BY created_at ASC, id ASC",
                new Object[] {id, Long.valueOf(start)});
        List<CandleDto> out = new ArrayList<CandleDto>();
        int at = 0;
        for (long s = start; s <= last; s += step) {
            long open = -1;
            long high = 0;
            long low = 0;
            long volume = 0;
            long quote = 0;
            int count = 0;
            while (at < trades.size() && Books.num(trades.get(at), "created_at") < s + step) {
                Map t = trades.get(at++);
                long price = Books.num(t, "price");
                if (open < 0) {
                    open = price;
                    high = price;
                    low = price;
                }
                high = Math.max(high, price);
                low = Math.min(low, price);
                close = price;
                volume += Books.num(t, "quantity");
                quote += Books.num(t, "buyer_paid");
                count++;
            }
            if (count == 0) {
                if (close < 0) {
                    // Nothing has traded yet: no candle rather than a made-up one.
                    continue;
                }
                out.add(new CandleDto(s, close, close, close, close, 0L, 0L, 0));
            } else {
                out.add(new CandleDto(s, open, high, low, close, volume, quote, count));
            }
        }
        return out;
    }
}
