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

import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Autowired;
import com.codename1.backend.test.BackendTest;
import com.codenameone.examples.bastion.api.BookDto;
import com.codenameone.examples.bastion.api.NewOrderDto;
import com.codenameone.examples.bastion.api.OrderDto;
import com.codenameone.examples.bastion.audit.Audit;
import com.codenameone.examples.bastion.domain.LedgerAccount;
import com.codenameone.examples.bastion.exchange.Exchange;
import com.codenameone.examples.bastion.exchange.Markets;
import com.codenameone.examples.bastion.exchange.ExchangeEndpoint;
import com.codenameone.examples.bastion.ledger.AccountKinds;
import com.codenameone.examples.bastion.ledger.Ledger;
import com.codenameone.examples.bastion.ledger.Leg;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/// The matching engine against the ledger. Each test trades on a market of
/// its own (`T1-USD` .. `T9-USD`, configured in application-test.properties,
/// all BTC/USD at a lot of 1,000 satoshi) so one test's resting orders never
/// meet another's.
///
/// Prices are cents per whole bitcoin: 6,000,000 is 60,000.00. A quantity of
/// 100,000 satoshi (0.001 BTC) at that price is 6,000 cents, and the 0.2%
/// taker fee on it is 12.
@BackendTest
class ExchangeTest {
    static final long PRICE = 6000000L;
    static final long QTY = 100000L;

    @Autowired
    private Exchange exchange;

    @Autowired
    private ExchangeEndpoint endpoint;

    @Autowired
    private Ledger ledger;

    @Autowired
    private Markets markets;

    @Autowired
    private Audit audit;


    private final List<String> owners = Collections.synchronizedList(new ArrayList<String>());

    /// A customer with `cents` in a checking account and `sats` in a wallet.
    private String trader(long cents, long sats) throws Exception {
        String owner = "x-" + TestKeys.random().substring(0, 12) + "@bastion.example";
        owners.add(owner);
        LedgerAccount usd = ledger.open(owner, "USD", AccountKinds.CHECKING, "c");
        LedgerAccount btc = ledger.open(owner, "BTC", AccountKinds.WALLET, "w");
        if (cents > 0) {
            fund(usd, cents);
        }
        if (sats > 0) {
            fund(btc, sats);
        }
        return owner;
    }

    private void fund(LedgerAccount to, long amount) throws Exception {
        LedgerAccount cash = ledger.system(to.getAsset(), AccountKinds.SYS_CASH);
        ledger.post(key(), "DEPOSIT", to.getOwner(), "", Arrays.asList(
                new Leg(cash.getId(), -amount), new Leg(to.getId(), amount)));
    }

    private long balance(String owner, String asset, String kind) throws Exception {
        for (LedgerAccount a : ledger.accountsOf(owner)) {
            if (a.getAsset().equals(asset) && a.getKind().equals(kind)) {
                return a.getBalance();
            }
        }
        return 0L;
    }

    private OrderDto limit(String owner, String market, String side, long price, long qty)
            throws Exception {
        return exchange.place(owner, key(), new NewOrderDto(market, side, Exchange.LIMIT, price,
                qty, 0L));
    }

    private String status(OrderDto o) throws Exception {
        return exchange.order(o.id()).getStatus();
    }

    private static String key() {
        return TestKeys.random();
    }

    private void booksBalance() throws Exception {
        List<String> all = new ArrayList<String>(owners);
        all.add(AccountKinds.BANK);
        assertEquals(new ArrayList<String>(), ledger.verify(all));
        assertEquals(new ArrayList<String>(), audit.verify());
        for (String owner : all) {
            // What sits in a HOLD account is exactly what open orders hold.
            assertEquals(exchange.held(owner, Exchange.BUY),
                    balance(owner, "USD", AccountKinds.HOLD), owner + " USD hold");
            assertEquals(exchange.held(owner, Exchange.SELL),
                    balance(owner, "BTC", AccountKinds.HOLD), owner + " BTC hold");
        }
    }

    @Test
    void aLimitPriceOutsideTheCollarIsRefused() throws Exception {
        String buyer = trader(10000000, 0);
        // 10% either side of the 60,000.00 index.
        assertStatus(400, () -> limit(buyer, "T10-USD", Exchange.BUY, 6600001L, QTY));
        assertStatus(400, () -> limit(buyer, "T10-USD", Exchange.SELL, 5399999L, QTY));
        assertEquals(Exchange.OPEN, status(limit(buyer, "T10-USD", Exchange.BUY, 6600000L,
                QTY)));
        booksBalance();
    }

    @Test
    void aMarketOrderStopsAtTheCollar() throws Exception {
        String seller = trader(0, 2 * QTY);
        String buyer = trader(10000000, 0);
        OrderDto near = limit(seller, "T11-USD", Exchange.SELL, 6000000L, QTY);
        OrderDto far = limit(seller, "T11-USD", Exchange.SELL, 6500000L, QTY);
        // The index falls to 58,000.00: the collar now ends at 63,800.00, and
        // the ask at 65,000.00 is past it.
        markets.index("T11-USD", 5800000L);
        try {
            OrderDto buy = exchange.place(buyer, key(), new NewOrderDto("T11-USD", Exchange.BUY,
                    Exchange.MARKET, 0L, 0L, 5000000L));
            assertEquals(QTY, buy.filled(), "only the ask inside the collar");
            assertEquals(Exchange.FILLED, status(near));
            assertEquals(Exchange.OPEN, status(far));
        } finally {
            markets.index("T11-USD", 6000000L);
        }
        booksBalance();
    }

    @Test
    void aLimitOrderRestsThenFills() throws Exception {
        String seller = trader(0, QTY);
        String buyer = trader(100000, 0);
        OrderDto ask = limit(seller, "T1-USD", Exchange.SELL, PRICE, QTY);
        assertEquals(Exchange.OPEN, ask.status());
        assertEquals(0, balance(seller, "BTC", AccountKinds.WALLET));
        assertEquals(QTY, balance(seller, "BTC", AccountKinds.HOLD));
        BookDto book = endpoint.book("T1-USD");
        assertEquals(1, book.asks().size());
        assertEquals(QTY, book.asks().get(0).quantity());

        OrderDto bid = limit(buyer, "T1-USD", Exchange.BUY, PRICE, QTY);
        assertEquals(Exchange.FILLED, bid.status());
        assertEquals(Exchange.FILLED, status(ask));
        assertEquals(100000 - 6000 - 12, balance(buyer, "USD", AccountKinds.CHECKING));
        assertEquals(QTY, balance(buyer, "BTC", AccountKinds.WALLET));
        assertEquals(6000, balance(seller, "USD", AccountKinds.CHECKING));
        assertEquals(0, balance(seller, "BTC", AccountKinds.HOLD));
        assertEquals(0, balance(buyer, "USD", AccountKinds.HOLD));
        assertTrue(endpoint.book("T1-USD").asks().isEmpty());
        booksBalance();
    }

    @Test
    void aPartialFillLeavesTheRestResting() throws Exception {
        String seller = trader(0, 3 * QTY);
        String buyer = trader(100000, 0);
        OrderDto ask = limit(seller, "T2-USD", Exchange.SELL, PRICE, 3 * QTY);
        // Bids above the ask: trades at the ask, and the difference comes back.
        OrderDto bid = limit(buyer, "T2-USD", Exchange.BUY, 6100000L, QTY);
        assertEquals(Exchange.FILLED, bid.status());
        assertEquals(100000 - 6012, balance(buyer, "USD", AccountKinds.CHECKING));
        assertEquals(0, balance(buyer, "USD", AccountKinds.HOLD));
        assertEquals(Exchange.OPEN, status(ask));
        assertEquals(QTY, exchange.order(ask.id()).getFilled());
        assertEquals(2 * QTY, balance(seller, "BTC", AccountKinds.HOLD));
        assertEquals(2 * QTY, endpoint.book("T2-USD").asks().get(0).quantity());
        booksBalance();
    }

    @Test
    void priceThenTimePriority() throws Exception {
        String first = trader(0, QTY);
        String second = trader(0, QTY);
        String better = trader(0, QTY);
        String buyer = trader(100000, 0);
        OrderDto a = limit(first, "T3-USD", Exchange.SELL, PRICE, QTY);
        OrderDto b = limit(second, "T3-USD", Exchange.SELL, PRICE, QTY);
        OrderDto c = limit(better, "T3-USD", Exchange.SELL, PRICE - 10000L, QTY);
        limit(buyer, "T3-USD", Exchange.BUY, PRICE, 2 * QTY);
        // The better price first though it came last, then the older of the
        // two at the same price.
        assertEquals(Exchange.FILLED, status(c));
        assertEquals(Exchange.FILLED, status(a));
        assertEquals(Exchange.OPEN, status(b));
        assertEquals(0, exchange.order(b.id()).getFilled());
        assertEquals(5990, balance(better, "USD", AccountKinds.CHECKING));
        booksBalance();
    }

    @Test
    void cancellingReleasesTheHold() throws Exception {
        String buyer = trader(100000, 0);
        OrderDto bid = limit(buyer, "T4-USD", Exchange.BUY, 5500000L, QTY);
        // 5,000 at the limit and the most fee it could pay, 10.
        assertEquals(5511, balance(buyer, "USD", AccountKinds.HOLD));
        assertEquals(100000 - 5511, balance(buyer, "USD", AccountKinds.CHECKING));
        String stranger = trader(0, 0);
        assertStatus(404, () -> exchange.cancel(stranger, bid.id()));
        OrderDto cancelled = exchange.cancel(buyer, bid.id());
        assertEquals(Exchange.CANCELLED, cancelled.status());
        assertEquals(100000, balance(buyer, "USD", AccountKinds.CHECKING));
        assertEquals(0, balance(buyer, "USD", AccountKinds.HOLD));
        assertStatus(409, () -> exchange.cancel(buyer, bid.id()));
        booksBalance();
    }

    @Test
    void aMarketBuyLeavesNoHold() throws Exception {
        String seller = trader(0, 2 * QTY);
        String buyer = trader(100000, 0);
        limit(seller, "T5-USD", Exchange.SELL, PRICE, QTY);
        limit(seller, "T5-USD", Exchange.SELL, 6100000L, QTY);
        OrderDto buy = exchange.place(buyer, key(), new NewOrderDto("T5-USD", Exchange.BUY,
                Exchange.MARKET, 0L, 0L, 10000L));
        assertEquals(Exchange.FILLED, buy.status());
        assertEquals(0, balance(buyer, "USD", AccountKinds.HOLD));
        long spent = 100000 - balance(buyer, "USD", AccountKinds.CHECKING);
        long bought = balance(buyer, "BTC", AccountKinds.WALLET);
        assertTrue(spent <= 10000 && spent > 9900, "spent " + spent);
        // All of the first level, then whole lots of the second.
        assertTrue(bought > QTY && bought < 2 * QTY && bought % 1000 == 0, "bought " + bought);
        // The rest of the book, then a market buy into an empty book: it buys
        // nothing and keeps nothing.
        String late = trader(50000, 0);
        exchange.place(late, key(), new NewOrderDto("T5-USD", Exchange.BUY, Exchange.MARKET,
                0L, 0L, 40000L));
        assertEquals(0, balance(late, "USD", AccountKinds.HOLD));
        assertTrue(endpoint.book("T5-USD").asks().isEmpty());
        String last = trader(50000, 0);
        OrderDto none = exchange.place(last, key(), new NewOrderDto("T5-USD", Exchange.BUY,
                Exchange.MARKET, 0L, 0L, 40000L));
        assertEquals(50000, balance(last, "USD", AccountKinds.CHECKING));
        assertEquals(0, balance(last, "USD", AccountKinds.HOLD));
        assertEquals(Exchange.CANCELLED, none.status());
        booksBalance();
    }

    @Test
    void anOrderNeverTradesWithItsOwner() throws Exception {
        String a = trader(100000, QTY);
        OrderDto ask = limit(a, "T6-USD", Exchange.SELL, PRICE, QTY);
        OrderDto bid = limit(a, "T6-USD", Exchange.BUY, PRICE, QTY);
        assertEquals(Exchange.CANCELLED, status(ask));
        assertEquals(Exchange.OPEN, bid.status());
        assertEquals(0, bid.filled());
        assertEquals(QTY, balance(a, "BTC", AccountKinds.WALLET));
        assertTrue(endpoint.book("T6-USD").asks().isEmpty());
        booksBalance();
    }

    @Test
    void anOrderNeedsTheFunds() throws Exception {
        String poor = trader(1000, 500);
        assertStatus(409, () -> limit(poor, "T7-USD", Exchange.BUY, PRICE, QTY));
        assertStatus(409, () -> limit(poor, "T7-USD", Exchange.SELL, PRICE, 1000L));
        assertStatus(400, () -> limit(poor, "T7-USD", Exchange.SELL, PRICE, 1500L));
        assertStatus(400, () -> limit(poor, "T7-USD", Exchange.BUY, Long.MAX_VALUE / 2, 1000000L));
        assertEquals(1000, balance(poor, "USD", AccountKinds.CHECKING));
        assertEquals(500, balance(poor, "BTC", AccountKinds.WALLET));
        assertTrue(exchange.open(poor).isEmpty());
        booksBalance();
    }

    @Test
    void aRetriedOrderIsPlacedOnce() throws Exception {
        String buyer = trader(100000, 0);
        String k = key();
        NewOrderDto order = new NewOrderDto("T8-USD", Exchange.BUY, Exchange.LIMIT, 5500000L,
                QTY, 0L);
        OrderDto first = exchange.place(buyer, k, order);
        OrderDto again = exchange.place(buyer, k, order);
        assertEquals(first.id(), again.id());
        assertEquals(5511, balance(buyer, "USD", AccountKinds.HOLD));
        assertStatus(422, () -> exchange.place(buyer, k, new NewOrderDto("T8-USD", Exchange.BUY,
                Exchange.LIMIT, 5500000L, 2 * QTY, 0L)));
        booksBalance();
    }

    /// Many customers placing and cancelling at once on one market. Whatever
    /// interleaving the threads find, the books must balance after it: every
    /// asset sums to zero, every balance is its postings, every hold is what
    /// its open orders say, and the audit chain is whole.
    @Test
    void aTakerSellersFeeNeverExceedsTheProceeds() throws Exception {
        // One lot at 1,000.00 USD a coin is a single cent; at 950.00 it is
        // 0.95 of one, which rounds down to nothing alone. The taker's fee is
        // on its running total, so a fill can pay a cent of fee out of a cent
        // of proceeds -- but never more than the fill brought in.
        String buyer = trader(100000, 0);
        String seller = trader(0, 6 * 1000L);
        limit(buyer, "T9-USD", Exchange.BUY, 100000L, 1000L);
        limit(buyer, "T9-USD", Exchange.BUY, 100000L, 1000L);
        limit(buyer, "T9-USD", Exchange.BUY, 95000L, 1000L);
        limit(buyer, "T9-USD", Exchange.BUY, 95000L, 1000L);
        limit(buyer, "T9-USD", Exchange.BUY, 95000L, 1000L);
        limit(buyer, "T9-USD", Exchange.BUY, 95000L, 1000L);
        OrderDto sell = limit(seller, "T9-USD", Exchange.SELL, 95000L, 6 * 1000L);
        assertEquals(Exchange.FILLED, sell.status());
        // 2 + 4 x 0.95 = 5.8, so 5 cents of proceeds, a cent of fee on them.
        assertEquals(4, balance(seller, "USD", AccountKinds.CHECKING));
        assertTrue(exchange.order(sell.id()).getFee() <= 5L);
        booksBalance();
    }

    @Test
    void theBooksBalanceAfterAnOrderStorm() throws Exception {
        final String[] traders = new String[6];
        for (int i = 0; i < traders.length; i++) {
            traders[i] = trader(5000000, 5000000);
        }
        final List<Throwable> unexpected = Collections.synchronizedList(new ArrayList<Throwable>());
        final AtomicInteger placed = new AtomicInteger();
        Runnable[] calls = new Runnable[8];
        for (int t = 0; t < calls.length; t++) {
            final long seed = 1000L + t;
            calls[t] = () -> {
                Random r = new Random(seed);
                List<String> mine = new ArrayList<String>();
                for (int n = 0; n < 25; n++) {
                    String owner = traders[r.nextInt(traders.length)];
                    try {
                        if (!mine.isEmpty() && r.nextInt(4) == 0) {
                            String id = mine.remove(r.nextInt(mine.size()));
                            exchange.cancel(exchange.order(id).getOwner(), id);
                            continue;
                        }
                        boolean buy = r.nextBoolean();
                        String side = buy ? Exchange.BUY : Exchange.SELL;
                        long qty = (1 + r.nextInt(50)) * 1000L;
                        OrderDto o;
                        if (r.nextInt(5) == 0) {
                            o = exchange.place(owner, key(), new NewOrderDto("STORM-USD", side,
                                    Exchange.MARKET, 0L, buy ? 0L : qty,
                                    buy ? 1000L + r.nextInt(5000) : 0L));
                        } else {
                            long price = PRICE + (r.nextInt(21) - 10) * 1000L + r.nextInt(7);
                            o = limit(owner, "STORM-USD", side, price, qty);
                        }
                        placed.incrementAndGet();
                        if (Exchange.OPEN.equals(o.status())) {
                            mine.add(o.id());
                        }
                    } catch (ResponseStatusException refused) {
                        // 409: out of funds, or the order filled before its
                        // cancel. Either refusal moved nothing.
                        if (refused.getStatus() != 409) {
                            unexpected.add(refused);
                        }
                    } catch (Throwable busy) {
                        // A database too busy to take the write: the
                        // transaction rolled back whole, which the checks
                        // below confirm.
                        String text = String.valueOf(busy);
                        if (text.indexOf("busy") < 0 && text.indexOf("locked") < 0
                                && text.indexOf("BUSY") < 0 && text.indexOf("LOCKED") < 0) {
                            unexpected.add(busy);
                        }
                    }
                }
            };
        }
        Race.together(calls);
        if (!unexpected.isEmpty()) {
            fail("unexpected: " + unexpected.get(0), unexpected.get(0));
        }
        assertTrue(placed.get() > 50, placed.get() + " orders placed");
        booksBalance();
    }

    private interface Call {
        void run() throws Exception;
    }

    private static void assertStatus(int status, Call call) throws Exception {
        try {
            call.run();
        } catch (ResponseStatusException answered) {
            assertEquals(status, answered.getStatus(), answered.getReason());
            return;
        }
        fail("expected " + status);
    }
}
