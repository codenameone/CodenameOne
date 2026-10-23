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
package com.codenameone.examples.bastion.account;

import com.codenameone.examples.bastion.Ids;
import com.codenameone.examples.bastion.books.Books;
import com.codenameone.examples.bastion.domain.LedgerAccount;
import com.codenameone.examples.bastion.domain.Market;
import com.codenameone.examples.bastion.exchange.MarketMaker;
import com.codenameone.examples.bastion.exchange.Markets;
import com.codenameone.examples.bastion.ledger.AccountKinds;
import com.codenameone.examples.bastion.ledger.Leg;
import com.codenameone.examples.bastion.ledger.Ledger;
import com.codename1.backend.DataSource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/// Weeks of believable history for a development server, so that every
/// chart, statement and ledger has something to show: payroll, card spending,
/// rent, savings with monthly interest, wire fees, and a trade tape on every
/// configured crypto market between the market maker, the demo customers and
/// two other trading desks.
///
/// Everything is posted through the ledger, dated in the past
/// ([Ledger#postAt]) and in time order, so the books balance at every instant
/// and no balance is ever below zero on any day. The bank's capital comes in
/// first. A pseudo-random generator with a fixed seed makes every fresh
/// database tell the same story. When the history spans more than five
/// weeks, its first four are closed as an accounting period.
final class DemoHistory {
    private static final long DAY = Books.DAY;
    private static final long HOUR = 3600000L;
    private static final String[] MERCHANTS = {
        "Corner Grocer", "Metro Transit", "Blue Bottle Coffee", "City Power & Light",
        "Northside Pharmacy", "Bookshop Annex", "Harbor Fuel", "Lantern Bistro",
        "Streamly Subscription", "Hardware Depot",
    };
    /// Trading desks other than the demo customers, so the tape is not two
    /// people trading with a robot. Like the market maker they are not
    /// usernames anyone can sign in as.
    static final String[] DESKS = {"@desk-north", "@desk-south"};

    private final Ledger ledger;
    private final Markets markets;
    private final DataSource db;
    private final Random random = new Random(20261021L);
    private int seq;

    DemoHistory(Ledger ledger, Markets markets, DataSource db) {
        this.ledger = ledger;
        this.markets = markets;
        this.db = db;
    }

    /// The time the history starts: the demo customers' opening deposits are
    /// dated here too.
    static long start(int days) {
        return Books.day(System.currentTimeMillis()) - days * DAY + 9 * HOUR;
    }

    void seed(int days, String customer, String second) throws IOException {
        long start = start(days);
        LedgerAccount cash = ledger.system("USD", AccountKinds.SYS_CASH);
        LedgerAccount equity = ledger.system("USD", AccountKinds.SYS_EQUITY);
        post("capital", "CAPITAL", AccountKinds.BANK, "Paid-in capital", start - HOUR,
                new Leg(cash.getId(), -100000000L), new Leg(equity.getId(), 100000000L));

        LedgerAccount checking = first(customer, "USD", AccountKinds.CHECKING);
        LedgerAccount savings = ledger.open(customer, "USD", AccountKinds.SAVINGS, "Savings");
        LedgerAccount checking2 = first(second, "USD", AccountKinds.CHECKING);
        LedgerAccount fees = ledger.system("USD", AccountKinds.SYS_FEES);
        LedgerAccount interest = ledger.system("USD", AccountKinds.SYS_INTEREST);

        List<Market> tape = new ArrayList<Market>();
        for (Market m : markets.all()) {
            // The configured markets, not the tests' T1..T9: one per pair.
            if (m.getId().equals(m.getBase() + "-" + m.getQuote())) {
                tape.add(m);
            }
        }
        List<String> traders = new ArrayList<String>(Arrays.asList(customer, second));
        traders.addAll(Arrays.asList(DESKS));
        for (Market m : tape) {
            fund(MarketMaker.OWNER, m, 2000, start);
            for (String trader : traders) {
                fund(trader, m, trader.startsWith("@") ? 40 : 1, start);
            }
        }
        long[] price = new long[tape.size()];
        for (int i = 0; i < tape.size(); i++) {
            price[i] = tape.get(i).getIndexPrice();
        }

        for (int d = 0; d < days; d++) {
            long day = start + d * DAY;
            if (d % 14 == 1) {
                move("payroll", "DEPOSIT", customer, "Payroll - Northwind Ltd", day + HOUR,
                        cash, checking, 420000);
            }
            if (d % 14 == 8) {
                move("payroll", "DEPOSIT", second, "Payroll - Contoso Labs", day + HOUR,
                        cash, checking2, 310000);
            }
            int spends = 1 + random.nextInt(3);
            for (int s = 0; s < spends; s++) {
                long cents = 800 + random.nextInt(11200);
                move("card", "CARD", customer, MERCHANTS[random.nextInt(MERCHANTS.length)],
                        day + (3 + 3 * s) * HOUR + random.nextInt(3600000), checking, cash, cents);
                if (random.nextInt(2) == 0) {
                    move("card", "CARD", second, MERCHANTS[random.nextInt(MERCHANTS.length)],
                            day + (4 + 3 * s) * HOUR, checking2, cash, 500 + random.nextInt(6000));
                }
            }
            if (d % 7 == 3) {
                move("rent", "TRANSFER", customer, "Rent share", day + 10 * HOUR, checking,
                        checking2, 45000);
            }
            if (d % 7 == 5) {
                move("save", "TRANSFER", customer, "To savings", day + 11 * HOUR, checking,
                        savings, 30000);
            }
            if (d % 10 == 6) {
                long at = day + 12 * HOUR;
                post("wire", "WITHDRAWAL", customer, "Wire to brokerage, fee 15.00", at,
                        new Leg(checking.getId(), -21500), new Leg(cash.getId(), 20000),
                        new Leg(fees.getId(), 1500));
            }
            if (d % 30 == 29) {
                long held = Math.max(0L, ledger.owned(customer, savings.getId()).getBalance());
                long paid = Math.max(1L, held * 40 / 10000);
                move("interest", "INTEREST", AccountKinds.BANK, "Savings interest 0.40%",
                        day + 23 * HOUR, interest, savings, paid);
            }
            for (int i = 0; i < tape.size(); i++) {
                Market m = tape.get(i);
                int count = 10 + random.nextInt(10);
                for (int t = 0; t < count; t++) {
                    // A mean-reverting random walk around the index price.
                    long index = m.getIndexPrice();
                    long drift = (index - price[i]) / 20;
                    long shock = price[i] * (random.nextInt(2001) - 1000) / 100000;
                    price[i] = Math.max(index / 2, price[i] + drift + shock);
                    String trader = traders.get(random.nextInt(traders.size()));
                    trade(m, price[i], trader, random.nextBoolean(),
                            day + (t * 24 * HOUR) / count + random.nextInt(600000));
                }
            }
        }
    }

    /// Opens `owner`'s accounts for `m` if missing and deposits `units` whole
    /// coins; a desk or the market maker also gets that many coins' worth of
    /// quote currency as a float.
    private void fund(String owner, Market m, int units, long at) throws IOException {
        LedgerAccount wallet = first(owner, m.getBase(), AccountKinds.WALLET);
        LedgerAccount checking = first(owner, m.getQuote(), AccountKinds.CHECKING);
        long coin = units * Markets.scale(ledger.asset(m.getBase()));
        LedgerAccount chain = ledger.system(m.getBase(), AccountKinds.SYS_CHAIN);
        post("fund-" + m.getBase(), "DEPOSIT", owner, "Coins deposited", at, new Leg(chain.getId(),
                -coin), new Leg(wallet.getId(), coin));
        if (owner.startsWith("@")) {
            long quote = (long) units * m.getIndexPrice();
            LedgerAccount cash = ledger.system(m.getQuote(), AccountKinds.SYS_CASH);
            post("fund-" + m.getQuote(), "DEPOSIT", owner, "Trading float", at,
                    new Leg(cash.getId(), -quote), new Leg(checking.getId(), quote));
        }
    }

    /// One trade between `trader` and the market maker, the trader taking:
    /// the same five legs the matching engine posts, taker fee 0.2%.
    private void trade(Market m, long price, String trader, boolean buys, long at)
            throws IOException {
        long scale = Markets.scale(ledger.asset(m.getBase()));
        long lots = 1 + random.nextInt(trader.startsWith("@") ? 400 : 60);
        long quantity = lots * m.getLot();
        long notional = price * quantity / scale;
        if (notional <= 0) {
            return;
        }
        long fee = Math.max(1L, notional * 20 / 10000);
        LedgerAccount tWallet = first(trader, m.getBase(), AccountKinds.WALLET);
        LedgerAccount tCash = first(trader, m.getQuote(), AccountKinds.CHECKING);
        LedgerAccount mWallet = first(MarketMaker.OWNER, m.getBase(), AccountKinds.WALLET);
        LedgerAccount mCash = first(MarketMaker.OWNER, m.getQuote(), AccountKinds.CHECKING);
        LedgerAccount fees = ledger.system(m.getQuote(), AccountKinds.SYS_FEES);
        List<Leg> legs = buys
                ? Arrays.asList(new Leg(tCash.getId(), -(notional + fee)),
                        new Leg(mCash.getId(), notional), new Leg(fees.getId(), fee),
                        new Leg(mWallet.getId(), -quantity), new Leg(tWallet.getId(), quantity))
                : Arrays.asList(new Leg(tWallet.getId(), -quantity),
                        new Leg(mWallet.getId(), quantity), new Leg(mCash.getId(), -notional),
                        new Leg(tCash.getId(), notional - fee), new Leg(fees.getId(), fee));
        String entry = post("trade", "TRADE", trader, (buys ? "Buy " : "Sell ") + m.getId(), at,
                legs.toArray(new Leg[0]));
        if (entry == null) {
            return;
        }
        // Written straight to the table: this runs outside any transaction,
        // where a repository has no session to write through.
        db.execute("INSERT INTO trade (id, market, buy_order, sell_order, buyer, seller, "
                + "taker_side, price, quantity, buyer_paid, seller_got, fee, entry_id, created_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", new Object[] {Ids.next(),
                    m.getId(), "", "", buys ? trader : MarketMaker.OWNER,
                    buys ? MarketMaker.OWNER : trader, buys ? "BUY" : "SELL", Long.valueOf(price),
                    Long.valueOf(quantity), Long.valueOf(buys ? notional + fee : notional),
                    Long.valueOf(buys ? notional : notional - fee), Long.valueOf(fee), entry,
                    Long.valueOf(at)});
    }

    private void move(String tag, String kind, String initiator, String memo, long at,
            LedgerAccount from, LedgerAccount to, long amount) throws IOException {
        post(tag, kind, initiator, memo, at, new Leg(from.getId(), -amount),
                new Leg(to.getId(), amount));
    }

    /// Posts and answers the entry id, or null when the ledger refused it --
    /// a customer the walk would overdraw simply does not make that payment.
    private String post(String tag, String kind, String initiator, String memo, long at,
            Leg... legs) throws IOException {
        try {
            return ledger.postAt("demo-history:" + tag + ":" + (seq++), kind, initiator, memo,
                    Arrays.asList(legs), at).getId();
        } catch (RuntimeException refused) {
            return null;
        }
    }

    private LedgerAccount first(String owner, String asset, String kind) throws IOException {
        for (LedgerAccount a : ledger.accountsOf(owner)) {
            if (a.getAsset().equals(asset) && a.getKind().equals(kind)) {
                return a;
            }
        }
        return ledger.open(owner, asset, kind, AccountKinds.WALLET.equals(kind)
                ? asset + " wallet" : "Checking");
    }
}
